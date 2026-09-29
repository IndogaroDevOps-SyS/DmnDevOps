package com.inddev.daemon

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object AssetExtractor {
    fun extractAssetsIfNeeded(context: Context): File {
        val binDir = File(context.filesDir, "bin")
        if (!binDir.exists()) {
            binDir.mkdirs()
        }

        val binaryFile = File(binDir, "xray")

        // Jika binary belum ada di internal storage, ekstrak dari assets
        if (!binaryFile.exists()) {
            context.assets.open("xray").use { input ->
                FileOutputStream(binaryFile).use { output ->
                    input.copyTo(output)
                }
            }
        }

        // BERIKAN IZIN EKSEKUSI (CHMOD +X) SECARA PAKSA AGAR TIDAK ERROR 13 PERMISSION DENIED
        binaryFile.setExecutable(true, false)

        return binaryFile
    }
}
