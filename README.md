# Retro Launcher

A customizable, lightweight retro gaming frontend and launcher built for Android handhelds, TVs, and phones.

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

---

## 🎮 Overview

**Retro Launcher** provides an intuitive, controller-friendly interface for browsing and launching your retro game collection. Built with Kotlin and Jetpack Compose, it features rich layout options, comprehensive emulator profile management (supporting both RetroArch cores and standalone Android emulators), flexible gamepad mapping, top/bottom bar customization, home screen widgets, icon customization, and RetroArch save thumbnail integration.

---

## ✨ Features

- **Multiple Display Styles**: Choose between Icon Grid, Compact List, Carousel / Accordion, and Fullscreen views for both system selection and ROM lists.
- **Interactive System Main Menu Gestures**:
  - **Single Tap**: Instantly enter a gaming system to view its ROM list, or interact with widgets (open BGM music controls, configure slideshow settings, or launch assigned Android apps).
  - **Double Tap**: Fast interactive shortcuts on widgets (such as double-tapping the Clock or Battery widget to toggle Top Bar or header visibility).
- **Top Bar & Bottom Bar Customization**:
  - Fully customizable Top and Bottom Bars with toggles for visibility, position, colors, opacity, and padding.
  - Configurable bar elements including clock, battery indicator, system/ROM counters, volume shortcuts, settings, and navigation icons.
  - Option to hide or auto-toggle bars using controller combo keys for an immersive fullscreen experience.
- **Customizable Widgets**:
  - **Clock Widget**: Displays the current time and date with customizable formatting and styling.
  - **Battery Status Widget**: Shows real-time battery percentage, charging state, and status icons.
  - **Games Count Widget**: Shows collection statistics including total games, total favorites, and completed games.
  - **Image Slideshow Widget**: Automatically cycles through your game icons, cover art, and screenshots.
  - **BGM Music Widget**: Shows background music track details with quick audio playback controls.
  - **App Launcher Widget**: Direct shortcut to quickly launch any installed Android app from the main menu.
  - **Custom Text Widget**: Displays custom notes, titles, or messages with marquee animation support.
- **Emulator & Core Integration**:
  - Pre-configured RetroArch core mappings (32-bit & 64-bit packages).
  - Standalone emulator launch profiles (PPSSPP, DuckStation, NetherSX2, Citra, Dolphin, Drastic, Yuzu, etc.).
  - Custom intent launch arguments and XML emulator profile import/export.
  - **RetroArch Save Thumbnail Support**: Automatically display in-game save state screenshots/thumbnails alongside your game art.
- **Hardware & Controller Support**:
  - Full Gamepad & D-Pad navigation with configurable button remapping.
  - Configurable keys (e.g., Favorites toggle, App drawer, System Settings).
- **Customizable UI & Theming**:
  - **Icon Customization**: Change custom system icons and individual ROM icons with ease.
  - Custom screen margins, canvas resolution scaling, and corner radius settings.
  - Custom system wallpapers, background music (BGM), and navigation sound effects (SFX).
  - Automatic game icon and screenshot integration (by using slideshow widget).
- **Game Management**:
  - Fast recursive directory scanning with multi-extension filters (`.zip`, `.7z`, `.chd`, `.iso`, `.bin`, `.gba`, etc.).
  - Favorite pinning, completion status tracking, and recently played tracking.

---

## 🚀 Quick Start Guide

### 1. Storage Permissions
Upon first launch, grant storage permissions (`MANAGE_EXTERNAL_STORAGE` or Media access) so the launcher can scan your ROM folders and display box art.

### 2. Set Up a Game System
1. Open the **System Settings** menu (press the Menu button on the top bar or use your mapped controller button).
2. Tap **Add System** or configure existing systems.
3. Select your ROM folder path and specify the file extensions to scan (e.g., `gba,zip,7z`).
4. Select the default emulator profile or core for that system.

### 3. Navigating & Launching Games
- **Single Tap** a system card on the main menu to enter and browse its game collection.
- **Single Tap** on widgets to trigger their respective dialogs (BGM controls, slideshow settings) or launch assigned Android apps.
- **Double Tap** on supported widgets (e.g., Clock / Battery widget) for fast UI toggle shortcuts like showing or hiding the Top Bar.
- Scroll through the game list using touch or gamepad D-pad.
- Tap or press the primary action button (`A` / `Enter`) to launch the selected title in your emulator.

### 4. Customizing Your Layout & Games
- Press detail (or assigned gamepad button) of a game or rom to view game information, toggle favorites, edit metadata, or assign custom cover art.
- Long-press any system or widget card on the main menu to edit its properties, icon, or behavior.
- Customize system and ROM icons directly from the system settings and ROM detail dialogs.
- View real-time RetroArch save thumbnails when browsing your saved progress.
- Access **Display Settings** from the top bar to adjust UI style (Grid / Carousel / List), grid column count, font sizes, screen padding, widgets, top/bottom bar layouts, and visual effects.

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0 (GNU GPLv3)**. See the [LICENSE](LICENSE) file for details.
