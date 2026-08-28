# Retro Launcher

A customizable, lightweight retro gaming frontend and launcher built for Android handhelds, TVs, phones, and full Android smartwatches.

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

---

## 🎮 Overview

**Retro Launcher** provides an intuitive, controller-friendly interface for browsing and launching your retro game collection. Built with Kotlin and Jetpack Compose, it features rich layout options, comprehensive emulator profile management (supporting both RetroArch cores and standalone Android emulators), flexible gamepad mapping, and hardware rotary crown support.

---

## ✨ Features

- **Multiple Display Styles**: Choose between Icon Grid, Compact List, Carousel / Accordion, and Fullscreen views for both system selection and ROM lists.
- **Emulator & Core Integration**:
  - Pre-configured RetroArch core mappings (32-bit & 64-bit packages).
  - Standalone emulator launch profiles (PPSSPP, DuckStation, NetherSX2, Citra, Dolphin, Drastic, Yuzu, etc.).
  - Custom intent launch arguments and XML emulator profile import/export.
- **Hardware & Controller Support**:
  - Full Gamepad & D-Pad navigation with configurable button remapping.
  - Dedicated hardware rotary dial / crown navigation for Android watches (supporting `ACTION_SCROLL` across multiple motion axes).
  - Configurable hotkeys (e.g., Favorites toggle, ROM search, App drawer, System Settings).
- **Customizable UI & Scaling**:
  - Custom screen margins, canvas resolution scaling, and corner radius settings.
  - Custom system icons, wallpapers, background music (BGM), and sound effects (SFX).
  - Automatic box art, wheel logo, and screenshot integration.
- **Game Management**:
  - Fast recursive directory scanning with multi-extension filters (`.zip`, `.7z`, `.chd`, `.iso`, `.bin`, `.gba`, etc.).
  - Favorite pinning, completion status tracking, search, and recently played tracking.
- **Configuration Portability**: Export and import your launcher layout and system configurations as shareable XML files.

---

## 🚀 Quick Start Guide

### 1. Storage Permissions
Upon first launch, grant storage permissions (`MANAGE_EXTERNAL_STORAGE` or Media access) so the launcher can scan your ROM folders and display box art.

### 2. Set Up a Game System
1. Open the **System Settings** menu (press the Menu button on the top bar or use your mapped controller button).
2. Tap **Add System** or configure existing systems.
3. Select your ROM folder path and specify the file extensions to scan (e.g., `gba,zip,7z`).
4. Select the default emulator profile or core for that system.

### 3. Launching Games
- Navigate the system menu to choose a console/system.
- Scroll through the game list using touch, gamepad D-pad, or the hardware crown dial.
- Tap or press the primary action button (`A` / `Enter`) to launch the selected title in your emulator.

### 4. Customizing Your Layout
- Long-press any game to view game options, toggle favorites, edit metadata, or assign custom cover art.
- Access **Display Settings** from the top bar to adjust UI style (Grid / Carousel / List), grid column count, font sizes, screen padding, and visual effects.
- Access **Controller Settings** to customize gamepad buttons and navigation shortcuts.

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0 (GNU GPLv3)**. See the [LICENSE](LICENSE) file for details.
