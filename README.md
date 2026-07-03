# Karoo Telegram Sync Utility

> [!IMPORTANT]
> **Core Concept:** This utility mirrors your Telegram **`Music`** chat folder directly to your Android device's standard **`/Music/`** directory.
>
> **⚠️ ATTENTION WARNING:**
> - **Two-way Cleanup:** During sync, the app will automatically delete files inside synced folders if they are removed from the corresponding Telegram channels. It will also delete synced channel folders on the device if they are removed from your Telegram `Music` folder.
> - **Safety of Personal Music:** Any pre-existing music files or folders in your device's `/Music/` directory that were *not* created by this app (meaning they do not contain a `.tg_channel_id` file) are **completely safe** and will not be touched or deleted.

---

## 🚀 Features

- **Zero Background Battery Drain:** The app is built to run only when visible on screen. Tapping **Exit App** completely kills the application process and background Python threads, saving battery.
- **Native Download Speeds:** Custom-built Telethon integration utilizing a native C-compiled `pycryptodome` AES-IGE decryption backend (achieving 10–25 MB/s transfer speeds).
- **Interactive Stop/Cancel Sync:** Features a green **Sync Now** button that changes to a red **Stop Sync** button when active. Tapping it halts the operation immediately and cleans up any partially downloaded files.
- **Clean UI Layout:** Designed specifically to fit the narrow screen bounds of cycling computers, featuring confirmation modal dialogs for Logout and Exit actions.

---

## 📦 Installation

1. Connect your Karoo device to your computer via USB (ensure ADB is enabled in Developer Options).
2. Download the pre-built release package [tg-sync-release.apk](https://github.com/Lemonchic/tg-sync/releases/download/v1.0.0/tg-sync-release.apk) from the official GitHub Release page.
3. Install the APK by running the following command in your terminal:
   ```bash
   adb install -r tg-sync-release.apk
   ```

---

## 📖 How to Use

### Step 1: Organize your Telegram Chats
1. In your Telegram account, create a **Chat Folder** named `Music` (case-insensitive).
2. Add any **private channels, private groups, or public chats** containing audio tracks to this folder.

### Step 2: Log In on the Karoo
1. Open the **Karoo Tg Sync** app on your device.
2. Enter your phone number in the international format (e.g. `+380XXXXXXXXX`) and tap **Submit**.
3. Telegram will send an authorization code to your active Telegram sessions. Enter the code in the input box and tap **Submit**.
4. If you have 2-Factor Authentication (2FA) active, enter your **2FA Password** and tap **Submit**.
5. Once authorized, the session is saved securely. On subsequent launches, the app will open directly to the sync screen.

### Step 3: Run Sync
- **Sync Everything:** Leave the **Chat ID** field empty and tap **SYNC NOW**. It will crawl all chats in your `Music` folder, download new tracks, and automatically clean up orphaned local files.
- **Sync Specific Chat:** Enter the Chat ID/Username (e.g. `@my_music_channel`) and tap **SYNC NOW** to sync only that target chat.
- **Sync Directory:** Synced audio files are placed inside the standard `/Music/` directory of the device, organized by chat title.

---

## 🛠️ Tech Stack
- **Kotlin:** Controls the modern Material Design UI and overlay dialogs.
- **Chaquopy:** Runs the embedded Python interpreter on Android.
- **Telethon:** Handles connection and communication with Telegram's MTProto API.
- **PyCryptodome:** Custom-patched C-backend for high-performance crypto execution.
