package com.inddev.daemon

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object AssetExtractor {
    fun extractAssetsIfNeeded(context: Context): File {
        // Pindahkan target ekstraksi ke cacheDir agar dijamin bisa dieksekusi (bypass noexec restriction di filesDir)
        val cacheBinDir = File(context.cacheDir, "bin")
        if (!cacheBinDir.exists()) {
            cacheBinDir.mkdirs()
        }

        val binaryFileName = "Golangbin"
        val binaryFile = File(cacheBinDir, binaryFileName)

        // Selalu perbarui binary dari assets jika versi APK baru masuk
        context.assets.open(binaryFileName).use { input ->
            FileOutputStream(binaryFile).use { output ->
                input.copyTo(output)
            }
        }

        // Paksa set permission executable (chmod +x) secara mutlak
        val isSuccess = binaryFile.setExecutable(true, false)
        if (!isSuccess && !binaryFile.canExecute()) {
            // Fallback runtime chmod via sh jika Java API gagal di beberapa custom ROM
            try {
                Runtime.getRuntime().exec(arrayOf("chmod", "755", binaryFile.absolutePath)).waitFor()
            } catch (e: Exception) {
                // Abaikan jika gagal, andalkan setExecutable
            }
        }

        return binaryFile
    }
}
