package dev.wolly.dsbmaterial.api

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File

object UpdateInstaller {
    private const val TAG = "UpdateInstaller"

    suspend fun downloadApk(context: Context, apkUrl: String): File? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.cacheDir, "apk").apply { mkdirs() }
            val apkFile = File(dir, "astra-update.apk")
            val response = DSBNetwork.client.newCall(
                Request.Builder()
                    .url(apkUrl)
                    .header("User-Agent", "Astra")
                    .build()
            ).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "APK download responded ${response.code}")
                response.close()
                return@withContext null
            }
            response.body?.byteStream()?.use { input ->
                apkFile.outputStream().use { output -> input.copyTo(output) }
            }
            response.close()
            apkFile
        } catch (e: Exception) {
            Log.e(TAG, "APK download failed", e)
            null
        }
    }

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
