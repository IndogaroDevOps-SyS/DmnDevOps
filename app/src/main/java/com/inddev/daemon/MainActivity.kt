package com.inddev.daemon

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Jalankan DaemonService di background secara aman
        val serviceIntent = Intent(this, DaemonService::class.java)
        startService(serviceIntent)
    }
}
