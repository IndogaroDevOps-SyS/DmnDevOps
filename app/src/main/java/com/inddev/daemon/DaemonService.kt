package com.inddev.daemon

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.io.File
import java.net.URL

class DaemonService : Service() {
    private var process: Process? = null
    private val TAG = "IndDevDaemon"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Inisialisasi Daemon Service...")

        val workingDir = applicationContext.filesDir

        Thread {
            try {
                // Buat folder blocklists sejajar dengan bin
                val blocklistDir = File(workingDir, "blocklists")
                if (!blocklistDir.exists() || blocklistDir.list().isNullOrEmpty()) {
                    blocklistDir.mkdirs()
                    Log.i(TAG, "Folder blocklists kosong. Mengunduh data dari repo GitHub...")
                    
                    // Daftar file blocklist utama yang ada di repo GitHub lu
                    val filesToDownload = listOf(
                        "adaway.txt", "adguarddns.txt", "blocklist-ads.txt",
                        "blocklist-malware.txt", "blocklist-tracking.txt",
                        "easylist.txt", "easyprivacy.txt", "hagezi-pro.txt",
                        "ip-google.txt", "ip-telegram.txt", "reject-list.txt",
                        "stevenblack.txt", "v2fly-ads.txt", "v2fly-google.txt", "v2fly-telegram.txt"
                    )

                    val baseUrl = "https://raw.githubusercontent.com/IndogaroDevOps-SyS/blocklists/main/"

                    for (fileName in filesToDownload) {
                        try {
                            val targetFile = File(blocklistDir, fileName)
                            val fileUrl = URL("$baseUrl$fileName")
                            Log.i(TAG, "Mengunduh $fileName...")
                            fileUrl.openStream().use { input ->
                                targetFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Gagal mendownload $fileName: ${e.localizedMessage}")
                        }
                    }
                    Log.i(TAG, "Semua file blocklist berhasil diunduh ke direktori lokal.")
                }

                // Ekstrak binary Go dari assets jika belum ada
                val binaryFile = AssetExtractor.extractAssetsIfNeeded(this)

                // Eksekusi binary Go daemon
                val pb = ProcessBuilder(binaryFile.absolutePath, "-config", "config.yaml")
                    .directory(workingDir)
                    .redirectErrorStream(true)

                val env = pb.environment()
                env["HOME"] = workingDir.absolutePath
                env["TMPDIR"] = workingDir.absolutePath

                process = pb.start()
                Log.i(TAG, "Binary Go daemon berhasil berjalan.")

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
