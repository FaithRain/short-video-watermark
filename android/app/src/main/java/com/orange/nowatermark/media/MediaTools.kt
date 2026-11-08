package com.orange.nowatermark.media

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File

object MediaTools {

    fun copyToCache(context: Context, uri: Uri): File? = runCatching {
        val suffix = context.contentResolver.getType(uri)?.substringAfterLast('/') ?: "mp4"
        val out = File(context.externalCacheDir ?: context.cacheDir, "input_${System.currentTimeMillis()}.$suffix")
        context.contentResolver.openInputStream(uri)!!.use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        }
        out
    }.getOrNull()

    fun videoWidthHeight(context: Context, uri: Uri): Pair<Int, Int>? = runCatching {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        retriever.release()
        if (w > 0 && h > 0) w to h else null
    }.getOrNull()

    fun videoDurationMs(context: Context, uri: Uri): Long = runCatching {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val d = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        retriever.release()
        d
    }.getOrDefault(0L)

    /** 本地 FFmpeg 执行；回调在主线程。 */
    fun runFfmpeg(context: Context, command: String, onResult: (Boolean, String) -> Unit) {
        FFmpegKit.executeAsync(command) { session ->
            val success = ReturnCode.isSuccess(session.returnCode)
            val log = session.failStackTrace ?: session.output ?: ""
            (context as? android.app.Activity)?.runOnUiThread {
                onResult(success, log)
            } ?: android.os.Handler(android.os.Looper.getMainLooper()).post {
                onResult(success, log)
            }
        }
    }

    fun saveFileToGallery(context: Context, file: File): Boolean = runCatching {
        val name = file.name
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/橙子去水印")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return false
            resolver.openOutputStream(uri)!!.use { out -> file.inputStream().use { it.copyTo(out) } }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "橙子去水印")
            if (!dir.exists()) dir.mkdirs()
            val target = File(dir, name)
            file.inputStream().use { input -> target.outputStream().use { input.copyTo(it) } }
            MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf("video/mp4"), null)
            true
        }
    }.getOrElse {
        Toast.makeText(context, "保存失败，请检查权限", Toast.LENGTH_SHORT).show()
        false
    }

    /** 音频保存到 Music/橙子去水印（与 6.1.0 的保存目录一致）。 */
    fun saveAudioToGallery(context: Context, file: File): Boolean = runCatching {
        val name = file.name
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, name)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/橙子去水印")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) ?: return false
            resolver.openOutputStream(uri)!!.use { out -> file.inputStream().use { it.copyTo(out) } }
            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "橙子去水印")
            if (!dir.exists()) dir.mkdirs()
            val target = File(dir, name)
            file.inputStream().use { input -> target.outputStream().use { input.copyTo(it) } }
            MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf("audio/mpeg"), null)
            true
        }
    }.getOrElse {
        Toast.makeText(context, "保存失败，请检查权限", Toast.LENGTH_SHORT).show()
        false
    }
}
