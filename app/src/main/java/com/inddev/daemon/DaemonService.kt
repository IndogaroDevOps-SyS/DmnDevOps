package com.inddev.daemon

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.io.File

class DaemonService : Service() {
    private var process: Process? = null
    private val TAG = "IndDevDaemon"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Memulai proses daemon Go...")
        
        val binaryFile = AssetExtractor.extractAssetsIfNeeded(this)
        val workingDir = applicationContext.filesDir

        Thread {
            try {
                // Menyesuaikan argumen ke config.yaml sesuai permintaan lu
                val pb = ProcessBuilder(binaryFile.absolutePath, "-config", "config.yaml")
                    .directory(workingDir)
                    .redirectErrorStream(true)

                val env = pb.environment()
                env["HOME"] = workingDir.absolutePath
                env["TMPDIR"] = workingDir.absolutePath

                process = pb.start()
                Log.i(TAG, "Binary Go berhasil dieksekusi di PID: ${process?.pid()}")

                process?.inputStream?.bufferedReader()?.use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        Log.d(TAG, "[Go-Core] $line")
                    }
                }

                val exitCode = process?.waitFor()
                Log.w(TAG, "Proses Go berhenti dengan exit code: $exitCode")

            } catch (e: Exception) {
                Log.e(TAG, "Gagal menjalankan daemon: ${e.localizedMessage}", e)
            }
        }.start()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        process?.destroy()
        Log.i(TAG, "Daemon Service dihentikan.")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
