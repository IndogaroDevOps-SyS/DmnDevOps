package com.inddev.daemon

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object AssetExtractor {
    fun extractAssetsIfNeeded(context: Context): File {
        // Gunakan path absolut fisik yang konsisten (ala Termux /data/data/)
        val packageName = context.packageName
        val absoluteFilesDir = File("/data/data/$packageName/files")
        val binDir = File(absoluteFilesDir, "bin")
        
        if (!binDir.exists()) {
            binDir.mkdirs()
        }

        val binaryFileName = "Golangbin"
        val binaryFile = File(binDir, binaryFileName)

        // Selalu timpa / perbarui binary dari assets
        context.assets.open(binaryFileName).use { input ->
            FileOutputStream(binaryFile).use { output ->
                input.copyTo(output)
            }
        }

        // Paksa set permission executable
        binaryFile.setExecutable(true, false)
        try {
            Runtime.getRuntime().exec(arrayOf("chmod", "755", binaryFile.absolutePath)).waitFor()
        } catch (e: Exception) {
            // ignore
        }

        return binaryFile
    }
}
