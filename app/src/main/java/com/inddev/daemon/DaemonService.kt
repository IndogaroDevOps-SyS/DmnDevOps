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
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

class DaemonService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var goProcess: Process? = null

    companion object {
        const val ACTION_LOG_BROADCAST = "com.inddev.daemon.LOG_BROADCAST"
        const val EXTRA_LOG_LINE = "extra_log_line"
        
        // Ring buffer simpan 500 log terakhir agar saat UI dibuka log tidak hilang
        val logHistory = ConcurrentLinkedQueue<String>()
        @Volatile var isServiceRunning = false

        fun addLog(context: Context?, line: String) {
            if (logHistory.size > 500) {
                logHistory.poll()
            }
            logHistory.add(line)
            context?.let {
                val intent = Intent(ACTION_LOG_BROADCAST).apply {
                    putExtra(EXTRA_LOG_LINE, line)
                    setPackage(it.packageName)
                }
                it.sendBroadcast(intent)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isServiceRunning) {
            isServiceRunning = true
            startForegroundServiceWithNotification()
            acquireWakeLock()
            startGolangDaemon()
        } else {
            addLog(this, "[SYSTEM] Daemon Service sudah aktif di background.")
        }
        return START_STICKY
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
            .setContentText("Golangbin system is running...")
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
            acquire(10 * 60 * 1000L)
        }
    }

    private fun startGolangDaemon() {
        thread {
            try {
                addLog(this, "[INIT] Memulai daemon system...")
                val homeDir = File("/data/data/$packageName/files/home")
                if (!homeDir.exists()) homeDir.mkdirs()

                addLog(this, "[INIT] Mengekstrak aset & binary secara dinamis...")
                val binaryFile = AssetExtractor.extractAssetsIfNeeded(this)
                addLog(this, "[INIT] Binary path: ${binaryFile.absolutePath}")

                addLog(this, "[INIT] Menjalankan Golangbin secara native...")
                val pb = ProcessBuilder(binaryFile.absolutePath)
                    .directory(homeDir)
                    .redirectErrorStream(true)

                val env = pb.environment()
                env["HOME"] = homeDir.absolutePath
                env["TMPDIR"] = homeDir.absolutePath

                goProcess = pb.start()
                addLog(this, "[SUCCESS] Golangbin daemon aktif & terhubung!")

                goProcess?.inputStream?.bufferedReader()?.use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        line?.let { addLog(this, "[GO-DAEMON] $it") }
                    }
                }

                val exitCode = goProcess?.waitFor()
                addLog(this, "[SYSTEM] Golangbin terhenti dengan exit code: $exitCode")
            } catch (e: Exception) {
                addLog(this, "[FATAL ERROR] ${e.localizedMessage}\n${e.stackTraceToString()}")
            } finally {
                isServiceRunning = false
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        goProcess?.destroy()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        isServiceRunning = false
        addLog(this, "[SYSTEM] Daemon Service dimatikan.")
        
        // Pulihkan ikon jika service dimatikan
        val componentName = ComponentName(this, MainActivity::class.java)
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
