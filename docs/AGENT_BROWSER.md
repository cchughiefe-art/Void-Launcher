# VOID Agent Browser

Agent Browser has its own icon outside Private Space. Returning to the launcher resumes an already enabled connection in the background; it never takes over the home screen or overrides Stop. Android 9+ is required for its separate WebView profile. It does not replace Private Browser.

## Setup
1. Install the signed update over the existing launcher. Do not uninstall the launcher.
2. Open **VOID Agent Browser**, open Settings, and enter your server's existing **device key** once. This is not the GitHub token or controller key.
3. Sign in to X manually in the browser. Mises logins cannot transfer automatically.
4. Tap **Start** and allow the notification. The connection stays enabled until **Stop**; there is no 15-minute timer in this client. Upgrade the relay to v0.9 for matching stop-controlled session semantics.
5. Switch apps. A visible notification provides Open and Stop. For Infinix battery restrictions, use the provided Android app settings button and configure background battery access yourself.

The enabled connection resumes whenever the launcher returns to the foreground, after a normal service-process restart, device reboot after unlock, or app update, if Resume after restart is selected. Stop persists across reboot. Force-stop, revoked permissions, OS limits, network loss, and vendor battery management can still interrupt the connection. The app cannot promise 24/7 operation or launch a UI over Android's restrictions.

## Switching apps for verification codes
Opening Agent Browser starts its browser-host service independently of the relay connection. You can sign in before tapping Start, switch to your messages/email app, and return to the same in-memory WebView. Leaving the screen detaches the view without destroying the started service.

The notification has **Stop agent** (disable remote commands, keep the page) and **Close browser** (disable commands and close the browser host). Android process/renderer death can still lose an unfinished form; the app does not save passwords or verification codes to disk.

Regression check: with the agent connection off, reach a verification-code form, switch to messages/email, return, and verify the page/form has not restarted. Repeat after enabling and then stopping agent access.

## Browser and relay
- Separate :void_agent process and WebView data suffix isolate cookies from Private Browser's cookie clearing.
- Service retains its WebView when the activity closes. Closing the screen does not clear browser login.
- Android Keystore encrypts the saved device key. The key and transient receipts are excluded from the launcher's existing allowlist backup rules.
- Automatic relay discovery uses the existing public void-xmd discovery file. Only the relay endpoint is discovered; no key is published.
- Direct device polling runs once per second, with bounded network timeouts and exponential backoff up to a minute on errors. The server's GitHub queue still adds its own polling and publication latency.
- Public HTTPS navigation, capture, scroll and verified links only. No arbitrary remote JavaScript, forms, posting, wallet operations, downloads, camera, microphone or file access.
- Native evaluateJavascript runs only fixed, app-owned scripts. No JavaScript interface is exposed to webpages.
- Captures include visible page text, links, images and loaded articles. They travel through your server into your private void-x-bot repository. Password/sign-in forms are withheld; cookies, storage and hidden inputs are not captured.
- Site restriction/challenge detection stops the connection; it does not solve or evade challenges.
- A leased command is recorded before execution; a completed receipt is saved atomically before delivery. Interrupted actions are reported as uncertain rather than replayed. Relay lease expiry invalidates stale receipts.
- Stop disables command execution immediately, cancels the page load, clears local pending intent, and attempts to revoke the relay session. If offline, the relay may show stale active state until contacted again, but this phone executes no commands.
- A wake lock is bounded to 30 seconds per active command; there is no permanent wake lock. Background renderer behavior requires a real-device test.

## Validation
Run the signed APK build workflow on this branch. Its APK uses the existing signing configuration. Then test:
1. Start, open a public page through the private queue, verify returned URL and text.
2. Keep the app in the background for more than 15 minutes; issue another read and verify receipt.
3. Open and close Private Browser; Agent Browser's X login should remain.
4. Turn networking off and back on; connection should recover without repeating navigation.
5. Stop, reboot, and verify no connection restarts. Start again, reboot with resume enabled, and verify it resumes after unlock.
6. Force-stop through Android settings; the app must remain stopped until explicitly reopened.
7. Interrupt the process during a command; verify no automatic action replay.
8. Review full text, relevant media and visible replies before using captures to draft X replies.

The X publication workflow is unchanged. Building successfully does not establish device persistence; only the device tests do.
