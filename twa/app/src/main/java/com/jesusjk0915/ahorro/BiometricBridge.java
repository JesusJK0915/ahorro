package com.jesusjk0915.ahorro;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

/** Puente entre el WebView y el bloqueo del celular (huella, PIN, patrón o contraseña). */
public class BiometricBridge {
    // Huella del sistema o credencial del dispositivo (PIN/patrón/contraseña).
    private static final int AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_WEAK
                    | BiometricManager.Authenticators.DEVICE_CREDENTIAL;

    private final FragmentActivity activity;
    private final WebView webView;

    public BiometricBridge(FragmentActivity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
    }

    @JavascriptInterface
    public boolean isAvailable() {
        try {
            return BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS)
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
                // Con DEVICE_CREDENTIAL no se permite botón negativo: el propio sistema
                // muestra la pantalla del celular y permite cancelar desde ella.
                BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Autenticación")
                        .setSubtitle("Desbloquea tu celular para continuar")
                        .setAllowedAuthenticators(AUTHENTICATORS)
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
