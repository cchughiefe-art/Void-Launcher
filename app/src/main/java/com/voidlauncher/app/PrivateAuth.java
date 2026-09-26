package com.voidlauncher.app;

import android.content.SharedPreferences;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

final class PrivateAuth {
    private static final int AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG;
    private static long unlockedUntil;

    private PrivateAuth() {}

    static boolean hasSession() { return System.currentTimeMillis() < unlockedUntil; }

    static void authenticate(AppCompatActivity activity, SharedPreferences prefs, Runnable success) {
        if (!prefs.getBoolean("biometric_private_lock", true) || hasSession()) { success.run(); return; }
        BiometricManager manager = BiometricManager.from(activity);
        if (manager.canAuthenticate(AUTHENTICATORS) != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(activity, "Set up a fingerprint in Android Settings first", Toast.LENGTH_LONG).show();
            return;
        }
        BiometricPrompt prompt = new BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
            new BiometricPrompt.AuthenticationCallback() {
                @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    unlockedUntil = System.currentTimeMillis() + 45_000L;
                    success.run();
                }
                @Override public void onAuthenticationError(int code, CharSequence message) {
                    super.onAuthenticationError(code, message);
                    if (code != BiometricPrompt.ERROR_USER_CANCELED && code != BiometricPrompt.ERROR_NEGATIVE_BUTTON)
                        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                }
            });
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Private Space")
            .setSubtitle("Use your fingerprint")
            .setAllowedAuthenticators(AUTHENTICATORS)
            .setNegativeButtonText("Cancel")
            .build();
        prompt.authenticate(info);
    }
}
