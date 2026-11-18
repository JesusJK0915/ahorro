package com.jesusjk0915.ahorro;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

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
        try {
            return BiometricManager.from(activity)
                    .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    == BiometricManager.BIOMETRIC_SUCCESS;
        } catch (Throwable t) {
            return false;
        }
    }

    @JavascriptInterface
    public void authenticate() {
        // @JavascriptInterface corre en el hilo de JS: mostramos el prompt en el de UI.
        activity.runOnUiThread(() -> {
            if (!isAvailable()) {
                callback(false);
                return;
            }
            try {
                BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder(activity)
                        .setTitle("Ahorros")
                        .setSubtitle("Confirma tu huella para entrar")
                        .setNegativeButtonText("Cancelar")
                        .build();
                BiometricPrompt prompt = new BiometricPrompt(
                        activity,
                        ContextCompat.getMainExecutor(activity),
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
                prompt.authenticate(info);
            } catch (Throwable t) {
                callback(false);
            }
        });
    }

    private void callback(boolean ok) {
        activity.runOnUiThread(() -> webView.evaluateJavascript(
                "window.onBiometricResult && window.onBiometricResult(" + ok + ");", null));
    }
}
