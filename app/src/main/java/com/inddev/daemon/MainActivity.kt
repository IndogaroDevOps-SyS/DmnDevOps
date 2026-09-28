package com.inddev.daemon

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Langsung jalankan DaemonService di background
        val serviceIntent = Intent(this, DaemonService::class.java)
        startService(serviceIntent)
        
        // Tutup activity utama supaya langsung "ngumpet" jadi daemon murni di background
        finish()
    }
}
