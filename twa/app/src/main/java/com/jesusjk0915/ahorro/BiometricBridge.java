package com.jesusjk0915.ahorro;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.CancellationSignal;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

/** Puente entre el WebView y el sensor de huella del dispositivo. */
public class BiometricBridge {
    private final Activity activity;
    private final WebView webView;

    public BiometricBridge(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
    }

    @JavascriptInterface
    public boolean isAvailable() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false;
        if (activity.checkSelfPermission(Manifest.permission.USE_BIOMETRIC)
                != PackageManager.PERMISSION_GRANTED) return false;
        try {
            return BiometricManager.from(activity)
                    .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    == BiometricManager.BIOMETRIC_SUCCESS;
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    @SuppressLint("NewApi")
    public void authenticate() {
        // Los métodos @JavascriptInterface corren en el hilo de JS, no en el de UI.
        activity.runOnUiThread(() -> {
            if (!isAvailable()) {
                callback(false);
                return;
            }
            try {
                BiometricPrompt prompt = new BiometricPrompt.Builder(activity)
                        .setTitle("Ahorros")
                        .setSubtitle("Confirma tu huella para entrar")
                        .setNegativeButton("Cancelar", activity.getMainExecutor(), (dialog, which) -> callback(false))
                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                        .build();
                prompt.authenticate(new CancellationSignal(), activity.getMainExecutor(),
                        new BiometricPrompt.AuthenticationCallback() {
                            @Override
                            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                                callback(true);
                            }

                            @Override
                            public void onAuthenticationError(int errorCode, CharSequence errString) {
                                callback(false);
                            }
                        });
            } catch (Exception e) {
                callback(false);
            }
        });
    }

    private void callback(boolean ok) {
        activity.runOnUiThread(() -> webView.evaluateJavascript(
                "window.onBiometricResult && window.onBiometricResult(" + ok + ");", null));
    }
}
