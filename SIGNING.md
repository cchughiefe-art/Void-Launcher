# Void Launcher release signing from Termux

Keep this keystore permanently. Every future update must use the same key.

## 1. Generate the key

Run this interactively so passwords are not saved in shell history:

```sh
pkg install openjdk-17 coreutils
keytool -genkeypair -v \
  -keystore "$HOME/void-launcher-release.jks" \
  -alias voidlauncher \
  -keyalg RSA -keysize 4096 -validity 10000
```

Use `Void Launcher` for the name when prompted. Store both passwords safely. Do not delete or regenerate this file after publishing an APK.

## 2. Create the GitHub secret value

```sh
base64 -w 0 "$HOME/void-launcher-release.jks" > "$HOME/void-launcher-release.jks.b64"
```

Open the GitHub repository, then **Settings → Secrets and variables → Actions → New repository secret**. Add:

| Secret | Value |
| --- | --- |
| `VOID_KEYSTORE_B64` | Entire contents of `~/void-launcher-release.jks.b64` |
| `VOID_KEYSTORE_PASSWORD` | Keystore password entered in `keytool` |
| `VOID_KEY_ALIAS` | `voidlauncher` |
| `VOID_KEY_PASSWORD` | Key password entered in `keytool` |

## 3. Back up the key

Copy `~/void-launcher-release.jks` to at least one private location that only you control. Never upload it to the repository or send it in chat.

## 4. Build the release

Open **Actions → Build Signed Void Launcher Release → Run workflow**. Download the `Void-Launcher-v1.2.1-signed-apk` artifact after it succeeds.
