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

        val assetManager = context.assets
        
        try {
            // Ambil SEMUA entitas di root folder assets secara dinamis
            val assetsList = assetManager.list("") ?: emptyArray()

            for (fileName in assetsList) {
                // Lewati folder bawaan Android/sistem jika ada
                if (fileName == "images" || fileName == "sounds" || fileName == "webkit") continue

                val targetFile = File(homeDir, fileName)
                try {
                    // Ekstrak file. Jika 'fileName' adalah sub-folder, open() akan throw exception dan diabaikan otomatis.
                    assetManager.open(fileName).use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    // Abaikan exception untuk sub-direktori (KISS approach)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val binaryFile = File(homeDir, "Golangbin")
        if (binaryFile.exists()) {
            binaryFile.setExecutable(true, false)
            try {
                Runtime.getRuntime().exec(arrayOf("chmod", "755", binaryFile.absolutePath)).waitFor()
            } catch (e: Exception) {
                // Abaikan jika env shell tidak support
            }
        }

        return binaryFile
    }
}
