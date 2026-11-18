package com.jesusjk0915.ahorro

import android.app.DownloadManager
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

const val CURRENT_VERSION = "2.3.0"
const val VERSION_URL = "https://JesusJK0915.github.io/ahorro/version.json"
const val INSTALL_ACTION = "ahorro.INSTALL_RESULT"

data class UpdateInfo(val version: String, val url: String, val notes: String)

object UpdateManager {
    fun check(onResult: (UpdateInfo?) -> Unit) {
        Thread {
            var info: UpdateInfo? = null
            try {
                val conn = URL(VERSION_URL).openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.requestMethod = "GET"
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val o = JSONObject(text)
                info = UpdateInfo(
                    version = o.optString("version", ""),
                    url = o.optString("url", ""),
                    notes = o.optString("notes", ""),
                )
            } catch (e: Exception) {
                info = null
            }
            Handler(Looper.getMainLooper()).post { onResult(info) }
        }.start()
    }

    fun isNewer(remote: String): Boolean {
        val a = remote.split(".").mapNotNull { it.toIntOrNull() }
        val b = CURRENT_VERSION.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x > y) return true
            if (x < y) return false
        }
        return false
    }

    fun startDownload(context: Context, url: String) {
        try {
            val source = Uri.parse(url)
            val scheme = source.scheme
            if (scheme != "https" && scheme != "http") return
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val req = DownloadManager.Request(source)
            req.setTitle("Ahorros · Actualización")
            req.setDescription("Descargando la nueva versión…")
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "ahorro-update.apk")
            val id = dm.enqueue(req)
            context.getSharedPreferences("ahorro_download", Context.MODE_PRIVATE)
                .edit().putLong("download_id", id).apply()
            Toast.makeText(context, "Descargando actualización…", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo iniciar la descarga", Toast.LENGTH_SHORT).show()
        }
    }
}
