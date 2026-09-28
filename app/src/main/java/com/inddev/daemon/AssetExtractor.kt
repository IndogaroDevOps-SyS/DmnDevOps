package com.inddev.daemon

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object AssetExtractor {
    private const val TAG = "AssetExtractor"

    fun extractAssetsIfNeeded(context: Context): File {
        val filesDir = context.filesDir
        val binDir = File(filesDir, "bin").apply { if (!exists()) mkdirs() }
        
        // Daftar file dan folder root assets yang disalin
        val assetItems = listOf(
            "bin/xray",
            "config.yaml",
            ".env",
            "brain.dat",
            "state.json"
        )

        for (item in assetItems) {
            val outFile = File(filesDir, item)
            if (outFile.exists() && item.contains("bin/")) {
                outFile.setExecutable(true, false)
                continue
            }
            
            try {
                outFile.parentFile?.mkdirs()
                context.assets.open(item).use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (item.contains("bin/")) {
                    outFile.setExecutable(true, false)
                }
                Log.i(TAG, "Berhasil mengekstrak: $item ke ${outFile.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Gagal mengekstrak aset $item: ${e.message}")
            }
        }

        // Ekstraks folder blocklists secara rekursif dari assets
        try {
            val blocklistOutDir = File(filesDir, "blocklists").apply { if (!exists()) mkdirs() }
            val assetManager = context.assets
            val listFiles = assetManager.list("blocklists") ?: emptyArray()
            for (filename in listFiles) {
                val outFile = File(blocklistOutDir, filename)
                assetManager.open("blocklists/$filename").use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            Log.i(TAG, "Berhasil mengekstrak folder blocklists")
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengekstrak folder blocklists: ${e.message}")
        }

        return File(filesDir, "bin/xray")
    }
}
