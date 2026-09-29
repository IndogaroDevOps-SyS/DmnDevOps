package com.inddev.daemon

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object AssetExtractor {
    fun extractAssetsIfNeeded(context: Context): File {
        // Bypass menggunakan codeCacheDir atau direktori un-restricted app
        val targetDir = File(context.codeCacheDir, "executable")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val binaryFileName = "Golangbin"
        val binaryFile = File(targetDir, binaryFileName)

        context.assets.open(binaryFileName).use { input ->
            FileOutputStream(binaryFile).use { output ->
                input.copyTo(output)
            }
        }

        // Terapkan chmod 777 secara paksa lewat runtime shell supaya bebas blokir SELinux
        try {
            val cmds = arrayOf("chmod", "777", binaryFile.absolutePath)
            Runtime.getRuntime().exec(cmds).waitFor()
        } catch (e: Exception) {
            binaryFile.setExecutable(true, false)
            binaryFile.setReadable(true, false)
            binaryFile.setWritable(true, false)
        }

        return binaryFile
    }
}
