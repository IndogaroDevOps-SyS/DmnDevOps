package com.inddev.daemon

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.widget.Button
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

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(24, 24, 24, 24)
        }

        val titleText = TextView(this).apply {
            text = "IndDev Daemon Console v12"
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }

        // Action Buttons Container
        val buttonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 20)
        }

        val toggleButton = Button(this).apply {
            text = if (DaemonService.isServiceRunning) "STOP DAEMON" else "START DAEMON"
            setBackgroundColor(if (DaemonService.isServiceRunning) Color.parseColor("#FF3366") else Color.parseColor("#00FF66"))
            setTextColor(Color.BLACK)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, 8, 0)
            }
            setOnClickListener {
                if (DaemonService.isServiceRunning) {
                    stopService(Intent(this@MainActivity, DaemonService::class.java))
                    text = "START DAEMON"
                    setBackgroundColor(Color.parseColor("#00FF66"))
                } else {
                    startDaemonService()
                    text = "STOP DAEMON"
                    setBackgroundColor(Color.parseColor("#FF3366"))
                }
            }
        }

        val stealthButton = Button(this).apply {
            text = "STEALTH MODE"
            setBackgroundColor(Color.parseColor("#9933FF"))
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(8, 0, 0, 0)
            }
            setOnClickListener {
                // Pastikan service aktif sebelum menyembunyikan ikon
                if (!DaemonService.isServiceRunning) {
                    startDaemonService()
                }
                hideLauncherIcon()
            }
        }

        buttonContainer.addView(toggleButton)
        buttonContainer.addView(stealthButton)

        scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0A0A0A"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            setPadding(16, 16, 16, 16)
        }

        logTextView = TextView(this).apply {
            setTextColor(Color.parseColor("#00FF66"))
            textSize = 11f
            typeface = Typeface.MONOSPACE
        }

        scrollView.addView(logTextView)
        mainLayout.addView(titleText)
        mainLayout.addView(buttonContainer)
        mainLayout.addView(scrollView)

        setContentView(mainLayout)

        // Muat ulang seluruh riwayat log dari Ring Buffer Daemon
        reloadLogHistory()

        // Otomatis jalankan daemon jika belum aktif saat app dibuka
        if (!DaemonService.isServiceRunning) {
            startDaemonService()
            toggleButton.text = "STOP DAEMON"
            toggleButton.setBackgroundColor(Color.parseColor("#FF3366"))
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

    private fun hideLauncherIcon() {
        val componentName = ComponentName(this, MainActivity::class.java)
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        finishAndRemoveTask()
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
