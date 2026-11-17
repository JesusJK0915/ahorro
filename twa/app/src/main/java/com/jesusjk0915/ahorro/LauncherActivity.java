package com.jesusjk0915.ahorro;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class LauncherActivity extends Activity {
    public static final String ACTION_INSTALL_RESULT = "ahorro.INSTALL_RESULT";

    private WebView webView;
    private BroadcastReceiver downloadReceiver;
    private int statusBarInset = -1;
    private boolean pageLoaded = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Receiver dinámico temprano (el estático del Manifest es el respaldo en segundo plano).
        registerDownloadReceiver();

        // Resultado del PackageInstaller.
        Intent incoming = getIntent();
        if (incoming != null && ACTION_INSTALL_RESULT.equals(incoming.getAction())) {
            int status = incoming.getIntExtra(PackageInstaller.EXTRA_STATUS,
                    PackageInstaller.STATUS_FAILURE);
            if (status == PackageInstaller.STATUS_SUCCESS) {
                Toast.makeText(this, "Actualización instalada ✅", Toast.LENGTH_LONG).show();
            } else {
                String msg = incoming.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
                Toast.makeText(this, "No se pudo instalar: " + (msg != null ? msg : ("error " + status)),
                        Toast.LENGTH_LONG).show();
            }
        }

        boolean dark = isDark();
        getWindow().setBackgroundDrawable(new ColorDrawable(surfaceColor()));
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(!dark);

        webView = new WebView(this);
        webView.setBackgroundColor(surfaceColor());

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("ahorro".equals(uri.getScheme()) && "install".equals(uri.getHost())) {
                    String url = uri.getQueryParameter("url");
                    if (url != null) startDownload(url);
                    return true;
                }
                return openExternal(uri);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                pageLoaded = true;
                injectTheme(view);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog,
                                          boolean isUserGesture, Message resultMsg) {
                WebView popup = new WebView(LauncherActivity.this);
                popup.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                        openExternal(request.getUrl());
                        return true;
                    }
                });
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(popup);
                resultMsg.sendToTarget();
                return true;
            }
        });

        // Capturar el alto EXACTO de la barra de estado que reporta el dispositivo
        // (en HyperOS/MIUI puede diferir del resource "status_bar_height").
        ViewCompat.setOnApplyWindowInsetsListener(webView, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            if (top != statusBarInset) {
                statusBarInset = top;
                if (pageLoaded) injectTheme(webView);
            }
            return insets;
        });

        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    /* ---------- Material You: colores del fondo de pantalla ---------- */
    private boolean isDark() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    private int sysColor(String name, int fallback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return fallback;
        int id = getResources().getIdentifier(name, "color", "android");
        if (id == 0) return fallback;
        try {
            return getColor(id);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String hex(int color) {
        return String.format("#%06X", 0xFFFFFF & color);
    }

    private int surfaceColor() {
        boolean dark = isDark();
        return sysColor(dark ? "system_neutral1_900" : "system_neutral1_50",
                dark ? 0xFF0D1313 : 0xFFF5FAFA);
    }

    private void injectTheme(WebView view) {
        int sbh = statusBarInset;
        if (sbh < 0) {
            int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (id > 0) sbh = getResources().getDimensionPixelSize(id);
        }

        boolean dark = isDark();
        String primary = hex(sysColor(dark ? "system_accent1_200" : "system_accent1_500", dark ? 0xFF7FD4D6 : 0xFF00696B));
        String onPrimary = hex(sysColor(dark ? "system_accent1_900" : "system_accent1_0", dark ? 0xFF003738 : 0xFFFFFFFF));
        String primaryContainer = hex(sysColor(dark ? "system_accent1_700" : "system_accent1_100", dark ? 0xFF004F51 : 0xFF9DF1F3));
        String onPrimaryContainer = hex(sysColor(dark ? "system_accent1_100" : "system_accent1_900", dark ? 0xFF9DF1F3 : 0xFF002021));
        String secondaryContainer = hex(sysColor(dark ? "system_accent2_700" : "system_accent2_100", dark ? 0xFF2A3F3F : 0xFFCFE7E7));
        String onSecondaryContainer = hex(sysColor(dark ? "system_accent2_100" : "system_accent2_900", dark ? 0xFFCFE7E7 : 0xFF082020));
        String tertiary = hex(sysColor(dark ? "system_accent3_200" : "system_accent3_500", dark ? 0xFFFFB59D : 0xFF9C4326));
        String surfaceLow = hex(sysColor(dark ? "system_neutral1_800" : "system_neutral1_100", dark ? 0xFF141B1B : 0xFFEEF4F4));
        String onSurface = hex(sysColor(dark ? "system_neutral1_100" : "system_neutral1_900", dark ? 0xFFDFE4E4 : 0xFF171D1D));
        String onSurfaceVariant = hex(sysColor(dark ? "system_neutral2_400" : "system_neutral2_700", dark ? 0xFFBFC8C8 : 0xFF3F4949));
        String surface = hex(surfaceColor());

        String js = "document.documentElement.style.setProperty('--sbh','" + sbh + "px');"
                + "window.__applyWallpaperColors && window.__applyWallpaperColors({"
                + "primary:'" + primary + "',"
                + "onPrimary:'" + onPrimary + "',"
                + "primaryContainer:'" + primaryContainer + "',"
                + "onPrimaryContainer:'" + onPrimaryContainer + "',"
                + "secondaryContainer:'" + secondaryContainer + "',"
                + "onSecondaryContainer:'" + onSecondaryContainer + "',"
                + "tertiary:'" + tertiary + "',"
                + "surface:'" + surface + "',"
                + "surfaceLow:'" + surfaceLow + "',"
                + "onSurface:'" + onSurface + "',"
                + "onSurfaceVariant:'" + onSurfaceVariant + "'"
                + "});";
        view.evaluateJavascript(js, null);
    }

    /* ---------- actualización: descargar e instalar ---------- */
    private void startDownload(String url) {
        try {
            Uri source = Uri.parse(url);
            String scheme = source.getScheme();
            if (scheme == null || (!scheme.equals("https") && !scheme.equals("http"))) return;

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request req = new DownloadManager.Request(source);
            req.setTitle("Ahorros · Actualización");
            req.setDescription("Descargando la nueva versión…");
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "ahorro-update.apk");
            long id = dm.enqueue(req);
            // Guardar el ID para que el receiver (dinámico o del Manifest) sepa qué descarga instalar.
            getSharedPreferences(Installer.PREFS, MODE_PRIVATE).edit()
                    .putLong(Installer.KEY_ID, id).apply();
            Toast.makeText(this, "Descargando actualización…", Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
    }

    private void registerDownloadReceiver() {
        try {
            if (downloadReceiver != null) return;
            downloadReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent intent) {
                    Installer.handle(c, intent);
                }
            };
            IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(downloadReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(downloadReceiver, filter);
            }
        } catch (Exception ignored) {
        }
    }

    private void unregisterDownloadReceiver() {
        if (downloadReceiver != null) {
            try {
                unregisterReceiver(downloadReceiver);
            } catch (Exception ignored) {
            }
            downloadReceiver = null;
        }
    }

    private boolean openExternal(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme != null && (scheme.equals("http") || scheme.equals("https"))) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        unregisterDownloadReceiver();
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
