package com.limelight;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.limelight.profiles.ProfilesManager;
import com.limelight.profiles.SettingsProfile;
import com.limelight.utils.UiHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProfilesActivity extends AppCompatActivity implements ProfilesManager.ProfileChangeListener {
    private LinearLayout sidebar;
    private LinearLayout details;
    private View emptyState;

    private final List<TextView> sidebarRows = new ArrayList<>();
    private UUID selectedId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profiles);

        sidebar = findViewById(R.id.profileSidebar);
        details = findViewById(R.id.profileDetails);
        emptyState = findViewById(R.id.emptyState);

        // Setup add-profile button
        ImageButton fab = findViewById(R.id.addProfileFab);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditProfileActivity.class);
            startActivity(intent);
        });

        // Register for profile changes
        ProfilesManager.getInstance().addListener(this);

        refreshProfiles();

        UiHelper.notifyNewRootView(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ProfilesManager.getInstance().removeListener(this);
    }

    @Override
    public void onProfilesChanged() {
        runOnUiThread(this::refreshProfiles);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-select in case profiles changed in the editor
        refreshProfiles();
        ensureSidebarFocus();
    }

    private void refreshProfiles() {
        List<SettingsProfile> profiles = ProfilesManager.getInstance().getProfiles();
        SettingsProfile active = ProfilesManager.getInstance().getActive();

        // Drop selection if it no longer exists; default to the active profile
        boolean selectedExists = false;
        for (SettingsProfile profile : profiles) {
            if (profile.getUuid().equals(selectedId)) {
                selectedExists = true;
                break;
            }
        }
        if (!selectedExists) {
            selectedId = active != null ? active.getUuid()
                    : (!profiles.isEmpty() ? profiles.get(0).getUuid() : null);
        }

        rebuildSidebar(profiles);
        renderDetails();

        emptyState.setVisibility(profiles.isEmpty() ? View.VISIBLE : View.GONE);

        ensureSidebarFocus();
    }

    private void rebuildSidebar(List<SettingsProfile> profiles) {
        sidebar.removeAllViews();
        sidebarRows.clear();

        float density = getResources().getDisplayMetrics().density;
        int pad = (int) (12 * density + 0.5f);

        for (int i = 0; i < profiles.size(); i++) {
            final SettingsProfile profile = profiles.get(i);
            TextView row = new TextView(this);
            row.setText(profile.getName());
            row.setTextColor(0xFFFFFFFF);
            row.setTextSize(16);
            row.setPadding(pad, pad, pad, pad);
            row.setSingleLine(true);
            row.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.setFocusable(true);
            row.setFocusableInTouchMode(true);
            row.setClickable(true);
            row.setBackgroundResource(profile.getUuid().equals(selectedId)
                    ? R.drawable.ps_row_selected : R.drawable.ps_tile);
            row.setOnClickListener(v -> {
                selectedId = profile.getUuid();
                markSidebarSelected();
                renderDetails();
            });
            // Like GameSideMenu: moving focus onto a profile previews it
            // immediately, no A-press required. Click is kept for touch.
            row.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && !profile.getUuid().equals(selectedId)) {
                    selectedId = profile.getUuid();
                    markSidebarSelected();
                    renderDetails();
                }
            });
            sidebarRows.add(row);
            sidebar.addView(row);
        }
    }

    private void markSidebarSelected() {
        List<SettingsProfile> profiles = ProfilesManager.getInstance().getProfiles();
        for (int i = 0; i < sidebarRows.size() && i < profiles.size(); i++) {
            sidebarRows.get(i).setBackgroundResource(
                    profiles.get(i).getUuid().equals(selectedId)
                            ? R.drawable.ps_row_selected : R.drawable.ps_tile);
        }
    }

    private SettingsProfile getSelected() {
        if (selectedId == null) {
            return null;
        }
        for (SettingsProfile profile : ProfilesManager.getInstance().getProfiles()) {
            if (profile.getUuid().equals(selectedId)) {
                return profile;
            }
        }
        return null;
    }

    private void renderDetails() {
        details.removeAllViews();

        final SettingsProfile profile = getSelected();
        if (profile == null) {
            return;
        }

        float density = getResources().getDisplayMetrics().density;
        int pad = (int) (12 * density + 0.5f);

        SettingsProfile active = ProfilesManager.getInstance().getActive();
        final boolean isActive = active != null && active.getUuid().equals(profile.getUuid());

        TextView name = new TextView(this);
        name.setText(profile.getName());
        name.setTextColor(0xFFFFFFFF);
        name.setTextSize(24);
        name.setTypeface(name.getTypeface(), android.graphics.Typeface.BOLD);
        details.addView(name);

        TextView meta = new TextView(this);
        meta.setText(DateUtils.getRelativeTimeSpanString(
                profile.getModifiedUtc(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
        meta.setTextColor(0xFF9FB3C8);
        meta.setTextSize(14);
        meta.setPadding(0, 0, 0, pad);
        details.addView(meta);

        TextView status = new TextView(this);
        status.setText(isActive ? R.string.ps_profile_active : R.string.ps_profile_inactive);
        status.setTextColor(0xFFFFFFFF);
        status.setTextSize(16);
        status.setPadding(0, 0, 0, pad);
        details.addView(status);

        details.addView(makeActionButton(
                getString(isActive ? R.string.ps_profile_deactivate : R.string.ps_profile_activate),
                () -> {
                    if (isActive) {
                        ProfilesManager.getInstance().setActive(null);
                        Toast.makeText(this, R.string.profile_manager_deactivated_profile,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        ProfilesManager.getInstance().setActive(profile.getUuid());
                        Toast.makeText(this, getString(R.string.profile_manager_activated_profile,
                                profile.getName()), Toast.LENGTH_SHORT).show();
                    }
                    ProfilesManager.getInstance().save(this);
                }));

        details.addView(makeActionButton(getString(R.string.ps_profile_edit), () -> {
            Intent intent = new Intent(this, EditProfileActivity.class);
            intent.putExtra("profileUuid", profile.getUuid().toString());
            startActivity(intent);
        }));

        details.addView(makeActionButton(getString(R.string.ps_profile_delete), () -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.profile_manager_delete_profile)
                    .setMessage(getString(R.string.profile_manager_confirm_profile_deleteion,
                            profile.getName()))
                    .setPositiveButton(R.string.profile_manager_delete, (dialog, which) -> {
                        ProfilesManager.getInstance().delete(profile.getUuid());
                        ProfilesManager.getInstance().save(this);
                        Toast.makeText(this, getString(R.string.profile_manager_profile_deleted,
                                profile.getName()), Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        }));
    }

    private Button makeActionButton(String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(0xFFFFFFFF);
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setFocusable(true);
        button.setFocusableInTouchMode(true);
        button.setBackgroundResource(R.drawable.ps_tile);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        float density = getResources().getDisplayMetrics().density;
        int margin = (int) (6 * density + 0.5f);
        params.setMargins(0, margin, 0, margin);
        button.setLayoutParams(params);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private void ensureSidebarFocus() {
        if (sidebarRows.isEmpty()) {
            return;
        }
        View focused = getCurrentFocus();
        if (focused != null) {
            android.view.ViewParent parent = focused.getParent();
            while (parent != null) {
                if (parent == sidebar || parent == details) {
                    return;
                }
                parent = parent.getParent();
            }
        }
        // Focus the selected row (or the first one)
        View target = null;
        List<SettingsProfile> profiles = ProfilesManager.getInstance().getProfiles();
        for (int i = 0; i < sidebarRows.size() && i < profiles.size(); i++) {
            if (profiles.get(i).getUuid().equals(selectedId)) {
                target = sidebarRows.get(i);
                break;
            }
        }
        if (target == null) {
            target = sidebarRows.get(0);
        }
        final View focusTarget = target;
        if (!focusTarget.requestFocus()) {
            sidebar.post(() -> focusTarget.requestFocus());
        }
    }
}
