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
            text = "IndDev Daemon Console v10 (SDK 28)"
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 24)
        }

        logTextView = TextView(this).apply {
            text = "[INIT] Memulai daemon (targetSdk 28 mode)...\n"
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
                val homeDir = File("/data/data/$packageName/files/home")
                if (!homeDir.exists()) {
                    homeDir.mkdirs()
                }

                appendLog("Working Directory: ${homeDir.absolutePath}")

                val blocklistDir = File(homeDir, "blocklists")
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
                            // ignore individual failure
                        }
                    }
                    appendLog("Modul blocklists selesai.")
                } else {
                    appendLog("Modul blocklists siap.")
                }

                appendLog("Mengekstrak Golangbin dari assets...")
                val binaryFile = AssetExtractor.extractAssetsIfNeeded(this)
                appendLog("Binary path: ${binaryFile.absolutePath}")

                appendLog("Menjalankan Golangbin...")
                val pb = ProcessBuilder(binaryFile.absolutePath, "-config", "config.yaml")
                    .directory(homeDir)
                    .redirectErrorStream(true)

                val env = pb.environment()
                env["HOME"] = homeDir.absolutePath
                env["TMPDIR"] = homeDir.absolutePath

                val process = pb.start()
                appendLog("SUCCESS: Golangbin daemon berhasil berjalan!")

                process.inputStream.bufferedReader().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        appendLog("[GO-DAEMON] $line")
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
