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
import java.io.FileOutputStream
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

class DaemonService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var goProcess: Process? = null

    companion object {
        const val ACTION_LOG_BROADCAST = "com.inddev.daemon.LOG_BROADCAST"
        const val EXTRA_LOG_LINE = "extra_log_line"

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

    private fun downloadBlocklistsIfNeeded(homeDir: File) {
        val blocklistDir = File(homeDir, "blocklists")
        if (!blocklistDir.exists()) {
            blocklistDir.mkdirs()
        }

        val filesToDownload = listOf(
            "adaway.txt", "adguarddns.txt", "blocklist-ads.txt",
            "blocklist-malware.txt", "blocklist-tracking.txt",
            "easylist.txt", "easyprivacy.txt", "hagezi-pro.txt",
            "ip-google.txt", "ip-telegram.txt", "reject-list.txt",
            "stevenblack.txt", "v2fly-ads.txt", "v2fly-google.txt", "v2fly-telegram.txt"
        )

        val baseUrl = "https://raw.githubusercontent.com/IndogaroDevOps-SyS/blocklists/main/"
        val total = filesToDownload.size

        addLog(this, "[BLOCKLIST] Memeriksa kelengkapan modul blocklists ($total file)...")

        filesToDownload.forEachIndexed { index, fileName ->
            val targetFile = File(blocklistDir, fileName)
            if (!targetFile.exists() || targetFile.length() == 0L) {
                addLog(this, "[DOWNLOADING] (${index + 1}/$total) Mengunduh $fileName ...")
                try {
                    val url = URL("$baseUrl$fileName")
                    val connection = url.openConnection().apply {
                        connectTimeout = 10000
                        readTimeout = 10000
                    }

                    var bytesCopied = 0L
                    connection.getInputStream().use { input ->
                        FileOutputStream(targetFile).use { output ->
                            val buffer = ByteArray(8192)
                            var bytes = input.read(buffer)
                            while (bytes >= 0) {
                                output.write(buffer, 0, bytes)
                                bytesCopied += bytes
                                bytes = input.read(buffer)
                            }
                        }
                    }
                    val sizeKb = bytesCopied / 1024
                    addLog(this, "[BLOCKLIST OK] (${index + 1}/$total) $fileName ($sizeKb KB)")
                } catch (e: Exception) {
                    addLog(this, "[BLOCKLIST FAIL] (${index + 1}/$total) $fileName: ${e.localizedMessage}")
                }
            } else {
                addLog(this, "[BLOCKLIST READY] (${index + 1}/$total) $fileName sudah ada.")
            }
        }
        addLog(this, "[BLOCKLIST] Sinkronisasi modul blocklists selesai.")
    }

    private fun startGolangDaemon() {
        thread {
            try {
                addLog(this, "[INIT] Memulai daemon system...")
                val homeDir = File("/data/data/$packageName/files/home")
                if (!homeDir.exists()) homeDir.mkdirs()

                // Unduh dan perlihatkan proses download blocklists ke konsol terminal
                downloadBlocklistsIfNeeded(homeDir)

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
