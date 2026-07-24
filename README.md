## Screenshots
<img width="2560" height="1317" alt="photo_2026-07-24_22-10-14" src="https://github.com/user-attachments/assets/e73a5325-1870-427e-824b-af4b190c2963" />

# focus.js

Focus.js replaces your standard launcher with a minimal, code-editor aesthetic paired with an working terminal.

Incase you want a distraction free launcher and some coding vibes, Give this a try


## What it does

- **JSON Home Screen**: Your home screen renders as a syntax-highlighted code editor showing a real `main.json` structure with your pinned apps and system status.
- **Embedded Terminal**: Tap the button in the bottom right (or run a command) to pull up the shell. You can split-screen it or toggle it to full height.
- **Built-in App Launcher**: Search and launch installed apps directly from the terminal prompt.
- **App Pinning**: Dynamically pin or unpin your favorite apps to update the home screen layout on the fly.
- **Actual Shell Commands**: Navigate your file system with `cd`, inspect folders with `ls`, move files with `mv`, and clean up with `rm`—it tracks working directories properly.

---

## Quick Command Reference

Here are the commands built into the launcher:

| Command | Usage | What it does |
| :--- | :--- | :--- |
| `open` | `open WhatsApp` | Finds and opens the app |
| `pin` | `pin Slack` | Pins the app to your `main.json` home screen |
| `unpin` | `unpin Slack` | Removes it from the home screen |
| `cd` | `cd Downloads` | Moves into subdirectories (supports `cd ..` and `cd ~`) |
| `fullscreen` | `fullscreen` | Expands terminal to 100% height |
| `clear` | `clear` | Wipes terminal logs |
| Shell tools | `ls`, `cp`, `mv`, `rm`, `mkdir` | Runs standard Linux/Android file commands |

---

## Permissions & Android Setup

For file manipulation (`cd`, `rm`, `mv`) across your device's storage, the app requires full storage access on Android 11+:
* Permission: `MANAGE_EXTERNAL_STORAGE`

When you first launch the app, it'll ask for permission to manage files. Grant that, and the terminal file operations will work fine.

