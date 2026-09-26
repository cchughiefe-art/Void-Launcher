# Void Launcher

A small, original Android home-screen launcher built from scratch. It does not depend on Launcher3 or Lawnchair source code.

## Included in v0.4

- Registers as an Android HOME app and can become the default launcher
- Fast alphabetized app drawer and instant search
- Long-press actions: dock, app info and hide
- Automatic starter dock with manual favorites
- Four- or five-column grid
- Offline operation and no analytics, ads or network permission
- Release build configuration with shrinking enabled
- Swipe-up Void Lock with separate private and decoy PINs
- Independent private and decoy docks and hidden-app lists
- Salted, repeatedly hashed PIN storage; plaintext PINs are never saved
- Manual immediate relock
- Decoy allowlist: the decoy never exposes the full installed-app list
- Private settings are unavailable from the decoy profile
- Secure-window protection blocks screenshots and launcher previews in Recents
- Search, drawer and authenticated profile state are scrubbed whenever Void loses focus
- Optional Android Work Profile provisioning; Void remains fully usable without it
- Work apps are visible only after the private Void PIN and never in the decoy
- Correct work-profile app launching through Android's LauncherApps service

The decoy is a launcher profile, not a separate Android user. Android Settings, notifications, Recents and deep links can still reveal activity outside the launcher.

## Build

Open the folder in Android Studio and build `app`.

### Build on GitHub from an Android phone

1. Create a new empty GitHub repository named `void-launcher`.
2. Upload or push everything in this project, including the `.github` folder.
3. Open the repository's **Actions** tab.
4. Select **Build Void Launcher APK**, then tap **Run workflow**.
5. When the run finishes, open it and download the `Void-Launcher-v0.1-test` artifact.

The workflow downloads the Android build tools on GitHub's server, so they do not consume storage on the phone. The test APK is signed automatically with a development certificate and can be installed immediately.

For the public release APK, create a private signing key and configure signing through Android Studio's **Generate Signed Bundle / APK** flow. Never commit the keystore or its passwords. Keep the key permanently because future updates must use the same key.

## Next milestones

Workspace pages, draggable icons, folders, widgets, notification dots, gestures, icon packs, backup/restore and a full visual settings screen.
