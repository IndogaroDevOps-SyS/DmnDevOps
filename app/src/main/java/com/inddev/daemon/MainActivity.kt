package com.inddev.daemon

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var logTextView: TextView
    private lateinit var scrollView: ScrollView

    private val logReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val logLine = intent?.getStringExtra(DaemonService.EXTRA_LOG_LINE)
            if (logLine != null) {
                appendLog(logLine)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(32, 32, 32, 32)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val titleText = TextView(this).apply {
            text = "IndDev Daemon Console v13"
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 24)
        }

        logTextView = TextView(this).apply {
            setTextColor(Color.parseColor("#00FF66"))
            textSize = 12f
            typeface = Typeface.MONOSPACE
        }

        layout.addView(titleText)
        layout.addView(logTextView)
        scrollView.addView(layout)
        setContentView(scrollView)

        // Muat seluruh riwayat log yang tersimpan di memori Service
        reloadLogHistory()

        // Otomatis aktifkan Daemon Service di background jika belum berjalan
        if (!DaemonService.isServiceRunning) {
            startDaemonService()
        }
    }

    private fun startDaemonService() {
        val serviceIntent = Intent(this, DaemonService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun reloadLogHistory() {
        val sb = StringBuilder()
        for (line in DaemonService.logHistory) {
            sb.append(line).append("\n")
        }
        logTextView.text = sb.toString()
        scrollToBottom()
    }

    private fun appendLog(msg: String) {
        runOnUiThread {
            logTextView.append("$msg\n")
            scrollToBottom()
        }
    }

    private fun scrollToBottom() {
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(logReceiver, IntentFilter(DaemonService.ACTION_LOG_BROADCAST), RECEIVER_EXPORTED)
        } else {
            registerReceiver(logReceiver, IntentFilter(DaemonService.ACTION_LOG_BROADCAST))
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(logReceiver)
        } catch (e: Exception) {
            // ignore
        }
    }
}
