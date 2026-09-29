package com.inddev.daemon

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object AssetExtractor {
    fun extractAssetsIfNeeded(context: Context): File {
        val packageName = context.packageName
        val homeDir = File("/data/data/$packageName/files/home")
        if (!homeDir.exists()) {
            homeDir.mkdirs()
        }

        val binaryFileName = "Golangbin"
        val binaryFile = File(homeDir, binaryFileName)

        context.assets.open(binaryFileName).use { input ->
            FileOutputStream(binaryFile).use { output ->
                input.copyTo(output)
            }
        }

        // Terapkan permission executable
        binaryFile.setExecutable(true, false)
        try {
            Runtime.getRuntime().exec(arrayOf("chmod", "755", binaryFile.absolutePath)).waitFor()
        } catch (e: Exception) {
            // ignore
        }

        return binaryFile
    }
}
