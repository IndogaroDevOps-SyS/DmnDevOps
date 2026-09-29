package com.inddev.daemon

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object AssetExtractor {
    fun extractAssetsIfNeeded(context: Context): File {
        val packageName = context.packageName
        // Tiru persis struktur direktori home Termux: /data/data/<package>/files/home
        val homeDir = File("/data/data/$packageName/files/home")
        if (!homeDir.exists()) {
            homeDir.mkdirs()
        }

        val binaryFileName = "Golangbin"
        val binaryFile = File(homeDir, binaryFileName)

        // Salin binary dari assets
        context.assets.open(binaryFileName).use { input ->
            FileOutputStream(binaryFile).use { output ->
                input.copyTo(output)
            }
        }

        // Terapkan izin eksekusi penuh ala chmod 755 / 777
        binaryFile.setExecutable(true, false)
        binaryFile.setReadable(true, false)
        binaryFile.setWritable(true, true)

        try {
            Runtime.getRuntime().exec(arrayOf("chmod", "755", binaryFile.absolutePath)).waitFor()
        } catch (e: Exception) {
            // ignore
        }

        return binaryFile
    }
}
