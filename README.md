# Void Launcher

An original Android HOME launcher with a Pixel-inspired interface and a hidden, launcher-level Private Space.

## Included in v1.6

- Registers as an Android HOME app and asks to become the default launcher
- Wallpaper-first Pixel-style home, clock, translucent dock, app search and 4/5-column drawer
- Swipe up from the bottom to open the complete Main app drawer
- Swipe inward from either screen edge to open the hidden Private Space
- Private Space has its own app allowlist, dock favorites, hidden apps and wallpaper
- Add or remove apps from Private Space by long-pressing them in the Main drawer
- Fingerprint lock before entering Private Space, Private Browser or Private Gallery
- Private Gallery for imported images, videos and documents
- Private Browser downloads save only to Void's app-internal private storage
- Private Browser keeps no persistent history, cache or cookies
- Automatic return to Main whenever Void loses focus, when enabled
- Screenshots are allowed throughout the launcher
- Easier short swipe-up gesture opens the app drawer from the lower half of the home screen
- Pulling down at the top of the app drawer returns to the home screen
- Full-width edge-to-edge home wallpaper with corrected system-bar spacing
- Opaque Pixel-style app drawer with a pull handle, safer bottom inset and smooth open/close motion
- Dark rounded Settings cards replace the default grey Android buttons
- Unfinished placeholder controls removed from Settings
- Home workspace supports up to 16 pinned apps with labels and long-press actions
- Long-press a Main drawer app to add or remove it from the Home screen
- Settings includes a one-tap Home workspace reset
- Clear and reset controls for Private Space
- Fast PackageManager app discovery, caching, alphabetical sorting and search
- Long-press actions for dock, Home, Private Space, app info, Play Store, sharing, uninstalling and hiding apps
- Uninstall requests use Android's protected system confirmation screen; system apps may only offer Disable
- A–Z or Z–A app-drawer sorting
- Settings controls to reset either dock and unhide every app in Main or Private Space
- Ordered Home workspace: icon positions remain where the user leaves them
- Long-press and drag Home icons to rearrange them, with an optional layout lock
- Create, rename, populate, open and delete Home folders with four-icon previews
- Configurable Home grid, Home icon size, app-drawer icon size and 4–6 app dock capacity
- Ordered Main and Private dock storage instead of unordered favourites
- Native app-provided shortcuts in the long-press menu when Void is the default launcher
- Automatic Home and drawer refresh after apps are installed, removed or updated
- Landscape, tablet and foldable resizing is no longer artificially blocked
- Motion-parallax wallpaper with Reduce Motion control
- Adjustable wallpaper focal point, zoom, dim strength, blur and motion intensity
- Four built-in Pixel-style wallpapers that can be applied separately to Main, Private Space or the lock screen
- On-device subject segmentation creates a transparent foreground layer for people, pets and objects
- Depth wallpaper places the home clock behind the detected subject and moves both layers together with parallax
- Automatic depth generation after choosing a wallpaper, plus manual rebuild controls
- Separate Main and Private Space wallpapers plus an optional lock-screen wallpaper setter
- No Work Profile setup, launcher PIN or storage permission prompt
- No analytics or advertising
- Environment-based release signing with a separate GitHub Actions signed-release workflow

Private Space is a fingerprint-gated launcher area with app-internal file storage, not a separate encrypted Android user/profile. Android Settings, notifications, Recents, deep links and other apps may still reveal installed apps or activity. Use Android's system Private Space when strong OS-level isolation is required.

## Build

Open the folder in Android Studio and build `app`.

### Build on GitHub from an Android phone

1. Push this project, including `.github`, to the repository.
2. Open the repository's **Actions** tab.
3. Select **Build Void Launcher APK**, then tap **Run workflow**.
4. Download the debug test artifact when the run finishes.

### Signed release APK

The signed APK workflow reads the keystore and passwords only from GitHub Actions secrets. Required secrets are `VOID_KEYSTORE_B64`, `VOID_KEYSTORE_PASSWORD`, `VOID_KEY_ALIAS`, and `VOID_KEY_PASSWORD`. Run **Build Signed Void Launcher Release** manually, then download `Void-Launcher-v1.6-signed-apk`.

The GitHub workflow builds and signs a debug APK that can be installed for testing. For public releases, create a private signing key and configure release signing. Never commit the keystore or passwords, and retain the same key for all future updates.

## Next milestones

Workspace pages, widgets, notification dots, icon packs, backup/restore and an encrypted local vault.
