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

        // Hapus file salah tangkap sebelumnya jika ada (seperti OWNERS)
        val wrongFiles = listOf("OWNERS", "xray")
        for (name in wrongFiles) {
            val badFile = File(binDir, name)
            if (badFile.exists()) badFile.delete()
        }

        // Wajib strict mengambil file bernama "Golangbin"
        val binaryFileName = "Golangbin"
        val binaryFile = File(binDir, binaryFileName)

        // Selalu timpa / perbarui binary jika ada versi baru di APK
        context.assets.open(binaryFileName).use { input ->
            FileOutputStream(binaryFile).use { output ->
                input.copyTo(output)
            }
        }

        // Berikan hak eksekusi chmod +x secara mutlak
        binaryFile.setExecutable(true, false)

        return binaryFile
    }
}
