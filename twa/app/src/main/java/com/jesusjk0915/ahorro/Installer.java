package com.jesusjk0915.ahorro;

import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInstaller;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

/** Instala el APK descargado usando la API moderna PackageInstaller. */
public final class Installer {
    public static final String PREFS = "ahorro_download";
    public static final String KEY_ID = "download_id";

    private Installer() {}

    /** Llamado cuando termina una descarga (desde el receiver dinámico o del Manifest). */
    public static void handle(Context c, Intent intent) {
        if (intent == null || !DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) return;
        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long saved = p.getLong(KEY_ID, -1L);
        if (id == -1L || id != saved) return;

        // Nos quedamos con el ID para que el otro receiver no vuelva a instalar.
        p.edit().remove(KEY_ID).apply();

        // Solo instalar si la descarga terminó correctamente.
        DownloadManager dm = (DownloadManager) c.getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm != null && !downloadOk(dm, id)) {
            Toast.makeText(c, "La descarga falló, intenta de nuevo", Toast.LENGTH_LONG).show();
            return;
        }

        File apk = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "ahorro-update.apk");
        install(c, apk);
    }

    private static boolean downloadOk(DownloadManager dm, long id) {
        DownloadManager.Query q = new DownloadManager.Query();
        q.setFilterById(id);
        Cursor cur = dm.query(q);
        if (cur == null) return false;
        try {
            if (!cur.moveToFirst()) return false;
            int status = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            return status == DownloadManager.STATUS_SUCCESSFUL;
        } finally {
            cur.close();
        }
    }

    private static void install(Context c, File apk) {
        if (!apk.exists() || apk.length() == 0) {
            Toast.makeText(c, "No se encontró el APK descargado", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            PackageInstaller pm = c.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            int sessionId = pm.createSession(params);
            PackageInstaller.Session session = pm.openSession(sessionId);
            try {
                OutputStream out = session.openWrite("ahorro-update", 0, apk.length());
                FileInputStream in = new FileInputStream(apk);
                try {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                } finally {
                    in.close();
                }
                out.flush();
                session.fsync(out);
                out.close();

                Intent cb = new Intent(c, MainActivity.class);
                cb.setAction("ahorro.INSTALL_RESULT");
                PendingIntent pending = PendingIntent.getActivity(c, sessionId, cb,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                session.commit(pending.getIntentSender());
            } finally {
                session.close();
            }
        } catch (Exception e) {
            // Fallback: abrir el instalador del sistema con un URI content:// válido.
            try {
                Uri uri = FileProvider.getUriForFile(c,
                        c.getPackageName() + ".fileprovider", apk);
                Intent fallback = new Intent(Intent.ACTION_VIEW);
                fallback.setDataAndType(uri, "application/vnd.android.package-archive");
                fallback.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(fallback);
            } catch (Exception ignored) {
                Toast.makeText(c, "No se pudo instalar la actualización", Toast.LENGTH_LONG).show();
            }
        }
    }
}
