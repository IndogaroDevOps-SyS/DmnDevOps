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

        // Daftar file statis yang wajib sejajar dengan binary
        val requiredFiles = listOf("Golangbin", ".env", "state.json", "brain.dat", "config.yml")

        for (fileName in requiredFiles) {
            val targetFile = File(homeDir, fileName)
            try {
                // Ekstrak dan timpa dari assets ke homeDir
                context.assets.open(fileName).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                // Lanjutkan loop jika salah satu file absen, tapi catat stack (opsional)
                e.printStackTrace()
            }
        }

        val binaryFile = File(homeDir, "Golangbin")
        if (binaryFile.exists()) {
            binaryFile.setExecutable(true, false)
            try {
                Runtime.getRuntime().exec(arrayOf("chmod", "755", binaryFile.absolutePath)).waitFor()
            } catch (e: Exception) {
                // Abaikan error eksekusi shell permission
            }
        }

        return binaryFile
    }
}
