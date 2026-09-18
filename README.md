# Console Artemis

A console-friendly fork of [Artemis Android](https://github.com/ClassicOldSong/moonlight-android) (formerly Moonlight Noir) — an open-source client for [Apollo](https://github.com/ClassicOldSong/Apollo) / [Sunshine](https://github.com/LizardByte/Sunshine).

Stream your PC games to your Android device, at home or over the internet, with a **PS4-inspired UI built for controller and TV / handheld use**.

Upstream base: Artemis Android brings all the Moonlight Noir improvements for desktop/office use (virtual display, server commands, clipboard sync with Apollo, custom resolutions/bitrates, virtual controls, etc.). See [Features from upstream](#features-from-upstream) below.

## What changed in this fork?

PS4-like revamp focused on 10-foot / gamepad navigation:

- **PS4-style home**
  - Horizontal host and game rows with large tiles, top status bar, and footer button hints.
  - Shared `ps` theme (`ps_bg`, `ps_tile`, `ps_row_selected`).
  - Focus-stable refresh — background polling updates rows without stealing gamepad focus.
- **Controller-first navigation**
  - D-pad / stick moves across rows, A / Start launches, B goes back.
  - Designed for landscape TV, handhelds, and foldables.
- **In-stream side menu**
  - Hold **START** to open the `GameSideMenu` overlay instead of mouse emulation.
  - Controller-navigable categories: keyboard, mouse mode, performance overlay, video actions, shortcuts, disconnect, etc.
- **Sidebar settings**
  - Stream Settings, Profiles, and Edit Profile screens use a left category sidebar + right content panel, navigable with a gamepad.
- **Fullscreen immersive everywhere**
  - All activities are landscape-only, edge-to-edge, with system bars hidden (`ArtemisApplication.applyFullscreen`).
- **Host-free UI testing**
  - Built-in **Test mode** adds fake hosts/games so you can try the PS4 layout, side menu, and focus handling without a real Apollo/Sunshine server.

## Quick start

1. Install Apollo (recommended) or Sunshine on your Windows PC.
2. Install this app on your Android device / handheld / TV box.
3. Pair once on the same network, then stream.
4. Navigate with your controller; hold START in-game for options.

Tip: open Test mode hosts if you just want to preview the UI with no PC nearby.

## Downloads

* [Download APK directly](https://github.com/ClassicOldSong/moonlight-android/releases)
* [Use Obtainium](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.limelight.noir%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2FClassicOldSong%2Fmoonlight-android%22%2C%22author%22%3A%22ClassicOldSong%22%2C%22name%22%3A%22Artemis%22%2C%22additionalSettings%22%3A%22%7B%5C%22apkFilterRegEx%5C%22%3A%5C%22nonRoot%5C%22%2C%5C%22matchGroutToUse%5C%22%3A%5C%22%241%5C%22%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22v(.%2B)%5C%22%7D%22%7D) (recommended)

Upstream releases live on ClassicOldSong/moonlight-android; this fork tracks `moonlight-noir`.

## Building

* Install Android Studio and the Android NDK
* Run `git submodule update --init --recursive` from within this repo
* Create `local.properties` with `ndk.dir=` pointing at your NDK directory
* Build the APK with Android Studio or Gradle

## Features from upstream

Artemis Android (Moonlight Noir) adds over stock Moonlight:

1. Custom virtual buttons with import/export
2. Custom resolutions and bitrates
3. Multiple mouse mode switching (normal, multi-touch, touchpad, disabled, local cursor)
4. Optimized virtual gamepad skins and free joystick
5. External monitor mode, Joycon D-pad support
6. Simplified performance overlay, game back menu, custom shortcut commands
7. Easy soft-keyboard switching, portrait mode, display-on-top mode for foldables
8. Virtual touchpad area/sensitivity, device vibration override, gamepad debug page
9. Trackpad tap/scroll, non-QWERTY layouts, quick Meta key, framerate-lock fix
10. Video scale Fit/Fill/Stretch, in-game pan/zoom/rotate, quit-app option, Samsung DeX scroll fixes
11. Virtual Display + Server Command + clipboard sync with Apollo, SBS 3D for external displays

Full history and credits: see upstream repo.

## Notes

This is a UI-focused fork. Streaming, pairing, decoding, and input still follow upstream Artemis / Moonlight Android. Console-artemis only re-skins navigation (PcView/AppView, Profiles, Stream Settings, GameSideMenu) for gamepad use.

Upstream project notice from ClassicOldSong: this line of forks exists because useful fixes were hard to land upstream. Feature PRs are welcome here.

## Authors

* [Cameron Gutman](https://github.com/cgutman)
* [Diego Waxemberg](https://github.com/dwaxemberg)
* [Aaron Neyer](https://github.com/Aaronneyer)
* [Andrew Hennessy](https://github.com/yetanothername)

Moonlight was started by students at [Case Western](http://case.edu) as a project at [MHacks](http://mhacks.org).
