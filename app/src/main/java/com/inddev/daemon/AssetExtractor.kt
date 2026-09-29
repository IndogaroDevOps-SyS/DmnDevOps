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

        // Cari file binary yang ada di folder assets aplikasi
        val assetManager = context.assets
        val assetsList = assetManager.list("") ?: emptyArray()
        
        // Pilih file apa saja di assets yang bukan folder bawaan android (seperti images, js, dsb)
        // Atau cari file binary utama lu
        val binaryAssetName = assetsList.firstOrNull { 
            it != "images" && it != "sounds" && it != "webkit" && !it.endsWith(".png") && !it.endsWith(".ogg") 
        } ?: throw IllegalStateException("Tidak ada file binary Go yang ditemukan di folder assets!")

        val binaryFile = File(binDir, binaryAssetName)

        if (!binaryFile.exists()) {
            assetManager.open(binaryAssetName).use { input ->
                FileOutputStream(binaryFile).use { output ->
                    input.copyTo(output)
                }
            }
        }

        // Wajib beri izin eksekusi (chmod +x)
        binaryFile.setExecutable(true, false)

        return binaryFile
    }
}
