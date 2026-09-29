package com.inddev.daemon

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var logTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(32, 32, 32, 32)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val titleText = TextView(this).apply {
            text = "IndDev Daemon Console v6 (Bypass Mode)"
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 24)
        }

        logTextView = TextView(this).apply {
            text = "[INIT] Memulai bypass daemon...\n"
            setTextColor(Color.parseColor("#00FF66"))
            textSize = 12f
            typeface = Typeface.MONOSPACE
        }

        layout.addView(titleText)
        layout.addView(logTextView)
        scrollView.addView(layout)
        setContentView(scrollView)

        thread {
            try {
                val packageName = applicationContext.packageName
                val workingDir = File("/data/data/$packageName/files")
                if (!workingDir.exists()) {
                    workingDir.mkdirs()
                }
                
                appendLog("Direktori kerja: ${workingDir.absolutePath}")

                val blocklistDir = File(workingDir, "blocklists")
                if (!blocklistDir.exists() || blocklistDir.list().isNullOrEmpty()) {
                    blocklistDir.mkdirs()
                    appendLog("Mengunduh modul blocklists...")

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
                            // ignore individual download error
                        }
                    }
                    appendLog("Modul blocklists selesai.")
                } else {
                    appendLog("Modul blocklists siap.")
                }

                appendLog("Mengekstrak Golangbin ke cache bypass...")
                val binaryFile = AssetExtractor.extractAssetsIfNeeded(this)
                appendLog("Binary bypass di: ${binaryFile.absolutePath}")

                appendLog("Menjalankan Golangbin via bypass shell...")
                // Eksekusi menggunakan perintah shell langsung untuk melewati batasan Java ProcessBuilder direct execution
                val command = "cd ${workingDir.absolutePath} && ${binaryFile.absolutePath} -config config.yaml"
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                appendLog("SUCCESS: Proses bypass Golangbin diluncurkan!")

                process.inputStream.bufferedReader().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        appendLog("[BYPASS-GO] $line")
                    }
                }

                process.errorStream.bufferedReader().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        appendLog("[BYPASS-ERR] $line")
                    }
                }

            } catch (e: Exception) {
                appendLog("\n[FATAL ERROR]: ${e.localizedMessage}\n${e.stackTraceToString()}")
            }
        }
    }

    private fun appendLog(msg: String) {
        runOnUiThread {
            logTextView.append("$msg\n")
        }
    }
}
