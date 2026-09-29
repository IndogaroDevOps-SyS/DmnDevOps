package com.inddev.daemon

import android.content.Intent
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
            text = "IndDev Daemon Manager"
            setTextColor(Color.WHITE)
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 24)
        }

        val statusText = TextView(this).apply {
            text = "Tekan tombol di bawah untuk mengaktifkan Daemon System.\nIkon aplikasi akan otomatis disembunyikan saat daemon aktif."
            setTextColor(Color.parseColor("#888888"))
            textSize = 13f
            setPadding(0, 0, 0, 32)
        }

        val startButton = Button(this).apply {
            text = "AKTIFKAN DAEMON SYSTEM"
            setBackgroundColor(Color.parseColor("#00FF66"))
            setTextColor(Color.BLACK)
            setTypeface(null, Typeface.BOLD)
            setOnClickListener {
                val serviceIntent = Intent(this@MainActivity, DaemonService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                finishAndRemoveTask() // Tutup activity & hapus dari recent apps
            }
        }

        layout.addView(titleText)
        layout.addView(statusText)
        layout.addView(startButton)
        scrollView.addView(layout)
        setContentView(scrollView)
    }
}
