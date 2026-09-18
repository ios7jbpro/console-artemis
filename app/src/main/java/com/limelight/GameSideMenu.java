package com.limelight;

import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.limelight.binding.input.ControllerHandler;
import com.limelight.binding.input.GameInputDevice;

import java.util.ArrayList;
import java.util.List;

/**
 * Console-style side overlay replacing the old AlertDialog quick menu.
 *
 * <p>Right-side panel with "categories" (left column); the focused category's
 * actions are shown in the right column. Navigable with a controller:
 * Up/Down moves, Left/Right switches columns, A/Enter activates,
 * B/Back steps back out or closes. Touch works too (tap rows, tap the
 * dimmed area to close).
 */
public class GameSideMenu implements Game.GameMenuCallbacks {
    private static final int COL_CATEGORIES = 0;
    private static final int COL_ITEMS = 1;

    private static final int COLOR_PANEL_TEXT = 0xFFFFFFFF;
    private static final int COLOR_SUBTEXT = 0xFF9FB3C8;

    private static class MenuEntry {
        final String label;
        final Runnable action;

        MenuEntry(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }
    }

    private static class MenuCategory {
        final String title;
        final List<MenuEntry> items = new ArrayList<>();

        MenuCategory(String title) {
            this.title = title;
        }

        void add(String label, Runnable action) {
            items.add(new MenuEntry(label, action));
        }
    }

    private final Game game;
    private final GameMenu dialogHelper;

    private static final long ANIM_MS = 220;
    private static final long NAV_REPEAT_MS = 180;

    private View overlay;
    private View scrim;
    private LinearLayout panel;
    private LinearLayout categoryColumn;
    private LinearLayout itemColumn;
    private TextView itemHeader;

    // Hat/stick navigation repeat state
    private int lastNavXDir;
    private int lastNavYDir;
    private long lastNavTime;

    private final List<MenuCategory> categories = new ArrayList<>();
    private final List<TextView> categoryViews = new ArrayList<>();
    private final List<TextView> itemViews = new ArrayList<>();
    private int categoryIndex;
    private int itemIndex;
    private int focusedColumn = COL_CATEGORIES;

    public GameSideMenu(Game game) {
        this.game = game;
        this.dialogHelper = new GameMenu(game);

        overlay = game.findViewById(R.id.sideMenuOverlay);
        scrim = overlay.findViewById(R.id.sideMenuScrim);
        panel = overlay.findViewById(R.id.sideMenuPanel);
        categoryColumn = overlay.findViewById(R.id.sideMenuCategories);
        itemColumn = overlay.findViewById(R.id.sideMenuItems);
        itemHeader = overlay.findViewById(R.id.sideMenuItemHeader);
        scrim.setOnClickListener(v -> hideMenu());
        overlay.setVisibility(View.GONE);
    }

    private String getString(int id) {
        return game.getResources().getString(id);
    }

    // Runs an action after closing the overlay. Closing first matters because
    // some actions (e.g. clipboard) bail out while a menu is reported open.
    private Runnable closeFirst(Runnable action) {
        return () -> {
            hideMenu();
            action.run();
        };
    }

    private void buildMenu(GameInputDevice device) {
        categories.clear();

        MenuCategory session = new MenuCategory(getString(R.string.side_menu_cat_session));
        session.add(getString(R.string.game_menu_disconnect), closeFirst(game::disconnect));
        session.add(getString(R.string.game_menu_quit_session), closeFirst(game::quit));
        session.add(getString(R.string.game_menu_rotate_screen), closeFirst(game::rotateScreen));
        categories.add(session);

        MenuCategory input = new MenuCategory(getString(R.string.side_menu_cat_input));
        if (game.allowChangeMouseMode) {
            input.add(getString(R.string.game_menu_select_mouse_mode),
                    () -> {
                        hideMenu();
                        dialogHelper.showSelectMouseMode();
                    });
        }
        if (device != null) {
            for (GameMenu.MenuOption option : device.getGameMenuOptions()) {
                final Runnable runnable = option.getRunnable();
                if (runnable != null) {
                    input.add(option.getLabel(), closeFirst(runnable::run));
                }
            }
        }
        input.add(getString(R.string.game_menu_toggle_keyboard), closeFirst(game::toggleKeyboard));
        input.add(getString(R.string.game_menu_send_keys),
                () -> {
                    hideMenu();
                    dialogHelper.showSpecialKeysMenu();
                });
        input.add(getString(R.string.side_menu_touch_sensitivity), closeFirst(game::switchTouchSensitivity));
        categories.add(input);

        MenuCategory display = new MenuCategory(getString(R.string.side_menu_cat_display));
        display.add(getString(game.isZoomModeEnabled()
                        ? R.string.side_menu_disable_zoom_mode
                        : R.string.side_menu_enable_zoom_mode),
                closeFirst(game::toggleZoomMode));
        display.add(getString(R.string.game_menu_toggle_hud), closeFirst(game::toggleHUD));
        display.add(getString(R.string.game_menu_toggle_floating_button),
                closeFirst(game::toggleFloatingButtonVisibility));
        display.add(getString(R.string.side_menu_special_keys_bar),
                closeFirst(game::toggleKeyboardController));
        if (!game.isOnExternalDisplay()) {
            display.add(getString(R.string.side_menu_virtual_controller),
                    closeFirst(game::toggleVirtualController));
        }
        display.add(getString(R.string.side_menu_full_keyboard), closeFirst(game::toggleFullKeyboard));
        categories.add(display);

        MenuCategory host = new MenuCategory(getString(R.string.side_menu_cat_host));
        host.add(getString(R.string.game_menu_upload_clipboard), closeFirst(() -> game.sendClipboard(true)));
        host.add(getString(R.string.game_menu_fetch_clipboard), closeFirst(() -> game.getClipboard(0)));
        host.add(getString(R.string.game_menu_server_cmd),
                () -> {
                    hideMenu();
                    dialogHelper.showServerCmdMenu();
                });
        host.add(getString(R.string.game_menu_task_manager),
                closeFirst(() -> game.sendKeys(new short[]{
                        com.limelight.binding.input.KeyboardTranslator.VK_LCONTROL,
                        com.limelight.binding.input.KeyboardTranslator.VK_LSHIFT,
                        com.limelight.binding.input.KeyboardTranslator.VK_ESCAPE})));
        categories.add(host);
    }

    private int dp(int dp) {
        return (int) (dp * game.getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView makeRow(String text, boolean bold) {
        TextView row = new TextView(game);
        row.setText(text);
        row.setTextColor(COLOR_PANEL_TEXT);
        row.setTextSize(16);
        if (bold) {
            row.setTypeface(row.getTypeface(), android.graphics.Typeface.BOLD);
        }
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setFocusable(false);
        row.setClickable(true);
        return row;
    }

    private void refreshViews() {
        categoryColumn.removeAllViews();
        categoryViews.clear();
        for (int i = 0; i < categories.size(); i++) {
            final int index = i;
            TextView row = makeRow(categories.get(i).title, true);
            row.setOnClickListener(v -> {
                categoryIndex = index;
                itemIndex = 0;
                focusedColumn = COL_ITEMS;
                refreshViews();
            });
            categoryViews.add(row);
            categoryColumn.addView(row);
        }

        itemColumn.removeAllViews();
        itemViews.clear();
        MenuCategory current = categories.get(categoryIndex);
        itemHeader.setText(current.title);
        for (int i = 0; i < current.items.size(); i++) {
            final int index = i;
            TextView row = makeRow(current.items.get(i).label, false);
            row.setOnClickListener(v -> {
                itemIndex = index;
                activateFocused();
            });
            itemViews.add(row);
            itemColumn.addView(row);
        }

        // Blue rounded look shared with the rest of the app: every row is a
        // tile, the active one glows with the light-blue selection border.
        for (int i = 0; i < categoryViews.size(); i++) {
            TextView row = categoryViews.get(i);
            if (i == categoryIndex) {
                row.setBackgroundResource(R.drawable.ps_row_selected);
            } else {
                row.setBackgroundResource(R.drawable.ps_tile);
            }
        }
        for (int i = 0; i < itemViews.size(); i++) {
            TextView row = itemViews.get(i);
            if (i == itemIndex && focusedColumn == COL_ITEMS) {
                row.setBackgroundResource(R.drawable.ps_row_selected);
            } else {
                row.setBackgroundResource(R.drawable.ps_tile);
            }
        }
    }

    private void moveSelection(int delta) {
        if (focusedColumn == COL_CATEGORIES) {
            categoryIndex = (categoryIndex + delta + categories.size()) % categories.size();
            itemIndex = 0;
        } else {
            int count = categories.get(categoryIndex).items.size();
            if (count == 0) {
                return;
            }
            itemIndex = (itemIndex + delta + count) % count;
        }
        refreshViews();
    }

    private void activateFocused() {
        if (focusedColumn == COL_CATEGORIES) {
            if (!categories.get(categoryIndex).items.isEmpty()) {
                focusedColumn = COL_ITEMS;
                refreshViews();
            }
            return;
        }
        List<MenuEntry> items = categories.get(categoryIndex).items;
        if (itemIndex >= 0 && itemIndex < items.size()) {
            items.get(itemIndex).action.run();
        }
    }

    private void onBackKey() {
        if (focusedColumn == COL_ITEMS) {
            focusedColumn = COL_CATEGORIES;
            refreshViews();
        } else {
            hideMenu();
        }
    }

    private static boolean isMenuKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_BUTTON_B:
            case KeyEvent.KEYCODE_BUTTON_X:
            case KeyEvent.KEYCODE_BUTTON_Y:
            case KeyEvent.KEYCODE_BUTTON_L1:
            case KeyEvent.KEYCODE_BUTTON_R1:
            case KeyEvent.KEYCODE_BUTTON_L2:
            case KeyEvent.KEYCODE_BUTTON_R2:
            case KeyEvent.KEYCODE_BUTTON_THUMBL:
            case KeyEvent.KEYCODE_BUTTON_THUMBR:
            case KeyEvent.KEYCODE_BUTTON_START:
            case KeyEvent.KEYCODE_BUTTON_SELECT:
            case KeyEvent.KEYCODE_BUTTON_MODE:
            case KeyEvent.KEYCODE_BUTTON_1:
            case KeyEvent.KEYCODE_BUTTON_2:
            case KeyEvent.KEYCODE_BUTTON_3:
            case KeyEvent.KEYCODE_BUTTON_4:
            case KeyEvent.KEYCODE_BUTTON_5:
            case KeyEvent.KEYCODE_BUTTON_6:
            case KeyEvent.KEYCODE_BUTTON_7:
            case KeyEvent.KEYCODE_BUTTON_8:
            case KeyEvent.KEYCODE_BUTTON_9:
            case KeyEvent.KEYCODE_BUTTON_10:
            case KeyEvent.KEYCODE_BUTTON_11:
            case KeyEvent.KEYCODE_BUTTON_12:
            case KeyEvent.KEYCODE_BUTTON_13:
            case KeyEvent.KEYCODE_BUTTON_14:
            case KeyEvent.KEYCODE_BUTTON_15:
            case KeyEvent.KEYCODE_BUTTON_16:
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_SPACE:
                return true;
            default:
                return false;
        }
    }

    /** Routes a key-down to the overlay. Returns true if consumed. */
    public boolean handleMenuKeyDown(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_UP:
                moveSelection(-1);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                moveSelection(1);
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                focusedColumn = COL_CATEGORIES;
                refreshViews();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                focusedColumn = COL_ITEMS;
                refreshViews();
                return true;
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_SPACE:
                activateFocused();
                return true;
            case KeyEvent.KEYCODE_BUTTON_B:
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
                onBackKey();
                return true;
            default:
                // Swallow remaining gamepad input so nothing leaks to the
                // stream (or the START-hold logic) while the menu is open.
                if ((event.getSource()
                        & (InputDevice.SOURCE_GAMEPAD | InputDevice.SOURCE_JOYSTICK)) != 0
                        || ControllerHandler.isGameControllerDevice(event.getDevice())
                        || isMenuKey(event.getKeyCode())) {
                    return true;
                }
                return false;
        }
    }

    /** Consumes the key-up matching anything handleMenuKeyDown would eat. */
    public boolean handleMenuKeyUp(KeyEvent event) {
        if (isMenuKey(event.getKeyCode())) {
            return true;
        }
        if ((event.getSource()
                & (InputDevice.SOURCE_GAMEPAD | InputDevice.SOURCE_JOYSTICK)) != 0
                || ControllerHandler.isGameControllerDevice(event.getDevice())) {
            return true;
        }
        return false;
    }

    /** Routes joystick/hat motion to menu navigation. Returns true if consumed. */
    public boolean handleMenuMotionEvent(MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) == 0) {
            return false;
        }

        // D-pad (hat) takes precedence; fall back to the left stick
        float hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X);
        float hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y);
        float x = Math.abs(hatX) > 0.01f ? hatX : event.getAxisValue(MotionEvent.AXIS_X);
        float y = Math.abs(hatY) > 0.01f ? hatY : event.getAxisValue(MotionEvent.AXIS_Y);

        int xDir = x < -0.5f ? -1 : (x > 0.5f ? 1 : 0);
        int yDir = y < -0.5f ? -1 : (y > 0.5f ? 1 : 0);

        long now = SystemClock.uptimeMillis();
        if (xDir != lastNavXDir || yDir != lastNavYDir) {
            if (xDir != 0 && xDir != lastNavXDir) {
                applyNavDirection(xDir, 0);
            }
            if (yDir != 0 && yDir != lastNavYDir) {
                applyNavDirection(0, yDir);
            }
            lastNavXDir = xDir;
            lastNavYDir = yDir;
            lastNavTime = now;
        } else if ((xDir != 0 || yDir != 0) && now - lastNavTime >= NAV_REPEAT_MS) {
            if (xDir != 0) {
                applyNavDirection(xDir, 0);
            }
            if (yDir != 0) {
                applyNavDirection(0, yDir);
            }
            lastNavTime = now;
        }

        // Swallow all joystick motion while open so nothing leaks to the stream
        return true;
    }

    private void applyNavDirection(int xDir, int yDir) {
        if (xDir < 0) {
            focusedColumn = COL_CATEGORIES;
            refreshViews();
        } else if (xDir > 0) {
            focusedColumn = COL_ITEMS;
            refreshViews();
        }
        if (yDir < 0) {
            moveSelection(-1);
        } else if (yDir > 0) {
            moveSelection(1);
        }
    }

    @Override
    public void showMenu(GameInputDevice device) {
        if (isMenuOpen()) {
            hideMenu();
            return;
        }
        buildMenu(device);
        categoryIndex = 0;
        itemIndex = 0;
        focusedColumn = COL_CATEGORIES;
        lastNavXDir = 0;
        lastNavYDir = 0;
        refreshViews();
        overlay.setVisibility(View.VISIBLE);

        // Slide the panel in from the right while fading the scrim in
        panel.animate().cancel();
        panel.animate().withEndAction(null);
        scrim.setAlpha(0f);
        scrim.animate().cancel();
        scrim.animate().alpha(1f).setDuration(ANIM_MS).start();
        overlay.post(() -> {
            if (!isMenuOpen()) {
                return;
            }
            panel.setTranslationX(panel.getWidth());
            panel.animate().translationX(0).setDuration(ANIM_MS).start();
        });
    }

    @Override
    public void hideMenu() {
        if (overlay == null || overlay.getVisibility() != View.VISIBLE) {
            return;
        }
        lastNavXDir = 0;
        lastNavYDir = 0;
        if (panel.getWidth() == 0) {
            overlay.setVisibility(View.GONE);
            return;
        }

        // Slide the panel out to the right while fading the scrim out
        panel.animate().cancel();
        scrim.animate().cancel();
        scrim.animate().alpha(0f).setDuration(ANIM_MS).start();
        panel.animate().translationX(panel.getWidth()).setDuration(ANIM_MS)
                .withEndAction(() -> {
                    panel.animate().withEndAction(null);
                    overlay.setVisibility(View.GONE);
                }).start();
    }

    @Override
    public boolean isMenuOpen() {
        return overlay != null && overlay.getVisibility() == View.VISIBLE;
    }
}
