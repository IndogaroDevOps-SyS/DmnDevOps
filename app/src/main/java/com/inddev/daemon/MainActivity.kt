package com.inddev.daemon

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Buat UI dinamis via kode biar gak perlu repot XML kalau ada perubahan
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setBackgroundColor(android.graphics.Color.parseColor("#121212"))
        }

        val titleText = TextView(this).apply {
            text = "IndDev Daemon Console"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        statusText = TextView(this).apply {
            text = "Menyiapkan lingkungan & mengunduh payload..."
            setTextColor(android.graphics.Color.parseColor("#00FF66"))
            textSize = 14f
            setPadding(0, 32, 0, 0)
        }

        layout.addView(titleText)
        layout.addView(statusText)
        setContentView(layout)

        // Jalankan proses download & daemon langsung di Background Thread
        thread {
            try {
                val workingDir = applicationContext.filesDir
                val blocklistDir = File(workingDir, "blocklists")

                if (!blocklistDir.exists() || blocklistDir.list().isNullOrEmpty()) {
                    blocklistDir.mkdirs()
                    updateStatus("Mengunduh blocklists dari GitHub...")

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
                            URL("$baseUrl$fileName").openStream().use { input ->
                                targetFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        } catch (e: Exception) {
                            // Abaikan file yang gagal satuan, lanjut ke file berikutnya
                        }
                    }
                }

                updateStatus("Ekstraksi binary Go...")
                val binaryFile = AssetExtractor.extractAssetsIfNeeded(this)

                updateStatus("Menjalankan binary daemon...")
                val pb = ProcessBuilder(binaryFile.absolutePath, "-config", "config.yaml")
                    .directory(workingDir)
                    .redirectErrorStream(true)

                val env = pb.environment()
                env["HOME"] = workingDir.absolutePath
                env["TMPDIR"] = workingDir.absolutePath

                val process = pb.start()
                updateStatus("Daemon BERHASIL berjalan di latar belakang!")

                // Baca output log dari proses Go secara real-time
                process.inputStream.bufferedReader().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        // Log output bisa dipantau jika diperlukan
                    }
                }

            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: e.toString()
                updateStatus("GAGAL: $errorMsg")
            }
        }
    }

    private fun updateStatus(msg: String) {
        runOnUiThread {
            statusText.text = msg
        }
    }
}
