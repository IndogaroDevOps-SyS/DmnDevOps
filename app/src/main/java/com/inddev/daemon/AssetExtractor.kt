package com.inddev.daemon

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object AssetExtractor {
    fun extractAssetsIfNeeded(context: Context): File {
        // Gunakan path absolut sandbox aplikasi ala Termux
        val targetDir = File("/data/data/com.inddev.daemon/files/bin")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val binaryFileName = "Golangbin"
        val binaryFile = File(targetDir, binaryFileName)

        // Salin dari assets
        context.assets.open(binaryFileName).use { input ->
            FileOutputStream(binaryFile).use { output ->
                input.copyTo(output)
            }
        }

        // Terapkan chmod 755 secara tegas menggunakan perintah shell sistem
        try {
            val process = Runtime.getRuntime().exec(arrayOf("chmod", "755", binaryFile.absolutePath))
            process.waitFor()
        } catch (e: Exception) {
            // Fallback ke Java API jika shell chmod dibatasi
            binaryFile.setExecutable(true, false)
            binaryFile.setReadable(true, false)
            binaryFile.setWritable(true, true)
        }

        return binaryFile
    }
}
