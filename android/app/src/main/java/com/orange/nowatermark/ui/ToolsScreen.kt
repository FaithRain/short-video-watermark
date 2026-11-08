package com.orange.nowatermark.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.orange.nowatermark.media.MediaTools
import com.orange.nowatermark.ui.glass.AuroraBackground
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ToolScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    AuroraBackground {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) { content() }
        }
    }
}

@Composable
fun RemoveMusicScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var picked by remember { mutableStateOf<Uri?>(null) }
    var processing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("选择本地视频，移除音轨（不重新编码）") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            picked = uri
            status = "已选择视频，点击开始处理"
        }
    }
    ToolScaffold("去除背景音乐", onBack) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(status, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { picker.launch(arrayOf("video/*")) }) { Text("选择视频") }
                Button(
                    enabled = picked != null && !processing,
                    onClick = {
                        val uri = picked ?: return@Button
                        processing = true
                        status = "正在移除背景音..."
                        val input = MediaTools.copyToCache(context, uri)
                        if (input == null) {
                            processing = false
                            status = "读取视频失败"
                            return@Button
                        }
                        val output = File(context.externalCacheDir ?: context.cacheDir, "noaudio_${System.currentTimeMillis()}.mp4")
                        val cmd = "-y -i \"${input.absolutePath}\" -an -vcodec copy \"${output.absolutePath}\""
                        MediaTools.runFfmpeg(context, cmd) { ok, log ->
                            processing = false
                            if (ok && output.exists()) {
                                val saved = MediaTools.saveFileToGallery(context, output)
                                status = if (saved) "处理成功，已保存至相册" else "处理成功，但保存失败"
                                Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                            } else {
                                status = "处理失败：${log.takeLast(200)}"
                            }
                        }
                    }
                ) {
                    if (processing) {
                        CircularProgressIndicator(modifier = Modifier.width(16.dp).height(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("开始处理")
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "对应原版 FFmpeg 命令：-an -vcodec copy（只删音轨，不重编码）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WatermarkScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var picked by remember { mutableStateOf<Uri?>(null) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var start by remember { mutableStateOf<Offset?>(null) }
    var end by remember { mutableStateOf<Offset?>(null) }
    var processing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("在画面上拖动，框选水印区域") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            picked = uri
            val dims = MediaTools.videoWidthHeight(context, uri)
            status = if (dims != null) "视频尺寸 ${dims.first}×${dims.second}，框选水印区域" else "已选择视频"
        }
    }

    ToolScaffold("视频去水印（delogo）", onBack) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(status, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { picker.launch(arrayOf("video/*")) }) { Text("选择视频") }
            Spacer(Modifier.height(8.dp))
            if (picked != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .onSizeChanged { boxSize = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    start = offset
                                    end = offset
                                },
                                onDrag = { change, _ ->
                                    end = change.position
                                }
                            )
                        }
                ) {
                    VideoPreview(uri = picked!!, modifier = Modifier.fillMaxSize())
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val s = start
                        val e = end
                        if (s != null && e != null) {
                            drawRect(
                                color = Color(0xFFFF5252),
                                topLeft = Offset(minOf(s.x, e.x), minOf(s.y, e.y)),
                                size = Size(kotlin.math.abs(e.x - s.x), kotlin.math.abs(e.y - s.y)),
                                style = Stroke(width = 4f)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                enabled = picked != null && start != null && end != null && !processing,
                onClick = {
                    val uri = picked ?: return@Button
                    val s = start ?: return@Button
                    val e = end ?: return@Button
                    val dims = MediaTools.videoWidthHeight(context, uri) ?: return@Button
                    val (videoW, videoH) = dims
                    val boxW = boxSize.width.toFloat().takeIf { it > 0f } ?: return@Button
                    val boxH = boxSize.height.toFloat().takeIf { it > 0f } ?: return@Button
                    val left = minOf(s.x, e.x) / boxW * videoW
                    val top = minOf(s.y, e.y) / boxH * videoH
                    val right = maxOf(s.x, e.x) / boxW * videoW
                    val bottom = maxOf(s.y, e.y) / boxH * videoH
                    val x = left.toInt().coerceIn(1, videoW - 4)
                    val y = top.toInt().coerceIn(1, videoH - 4)
                    val w = (right - left).toInt().coerceAtLeast(8).coerceAtMost(videoW - x - 2)
                    val h = (bottom - top).toInt().coerceAtLeast(8).coerceAtMost(videoH - y - 2)

                    processing = true
                    status = "正在处理..."
                    val input = MediaTools.copyToCache(context, uri)
                    if (input == null) {
                        processing = false
                        status = "读取视频失败"
                        return@Button
                    }
                    val output = File(context.externalCacheDir ?: context.cacheDir, "delogo_${System.currentTimeMillis()}.mp4")
                    val cmd = "-y -i \"${input.absolutePath}\" -vf \"delogo=x=$x:y=$y:w=$w:h=$h\" \"${output.absolutePath}\""
                    MediaTools.runFfmpeg(context, cmd) { ok, log ->
                        processing = false
                        if (ok && output.exists()) {
                            MediaTools.saveFileToGallery(context, output)
                            status = "处理完成：x=$x y=$y w=$w h=$h"
                        } else {
                            status = "处理失败：${log.takeLast(200)}"
                        }
                    }
                }
            ) {
                if (processing) {
                    CircularProgressIndicator(modifier = Modifier.width(16.dp).height(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                }
                Text("去除水印并保存")
            }
        }
    }
}

@Composable
fun CropScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var picked by remember { mutableStateOf<Uri?>(null) }
    var duration by remember { mutableFloatStateOf(0f) }
    var startSec by remember { mutableFloatStateOf(0f) }
    var endSec by remember { mutableFloatStateOf(1f) }
    var processing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("选择视频后拖动滑块选择片段") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            picked = uri
            val ms = MediaTools.videoDurationMs(context, uri)
            duration = (ms / 1000f).coerceAtLeast(1f)
            startSec = 0f
            endSec = duration
            status = "时长 ${"%.1f".format(duration)} 秒"
        }
    }

    ToolScaffold("视频裁剪", onBack) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(status, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { picker.launch(arrayOf("video/*")) }) { Text("选择视频") }
            if (picked != null) {
                Spacer(Modifier.height(16.dp))
                Text("起点：${"%.1f".format(startSec)}s")
                Slider(value = startSec, onValueChange = { startSec = it.coerceAtMost(endSec - 0.5f) }, valueRange = 0f..duration)
                Text("终点：${"%.1f".format(endSec)}s")
                Slider(value = endSec, onValueChange = { endSec = it.coerceAtLeast(startSec + 0.5f) }, valueRange = 0f..duration)
                Spacer(Modifier.height(12.dp))
                Button(
                    enabled = !processing,
                    onClick = {
                        val uri = picked ?: return@Button
                        processing = true
                        status = "正在裁剪..."
                        val input = MediaTools.copyToCache(context, uri)
                        if (input == null) {
                            processing = false
                            status = "读取视频失败"
                            return@Button
                        }
                        val output = File(context.externalCacheDir ?: context.cacheDir, "trim_${System.currentTimeMillis()}.mp4")
                        val cmd = "-y -ss ${"%.2f".format(startSec)} -to ${"%.2f".format(endSec)} -i \"${input.absolutePath}\" -c copy \"${output.absolutePath}\""
                        MediaTools.runFfmpeg(context, cmd) { ok, log ->
                            processing = false
                            if (ok && output.exists()) {
                                MediaTools.saveFileToGallery(context, output)
                                status = "裁剪完成，已保存至相册"
                            } else {
                                status = "裁剪失败：${log.takeLast(200)}"
                            }
                        }
                    }
                ) { Text("导出选中片段") }
            }
        }
    }
}

@Composable
private fun VideoPreview(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(Unit) { onDispose { player.release() } }
    LaunchedEffect(uri) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
    }
    AndroidView(
        modifier = modifier,
        factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = false } }
    )
}

@Composable
fun ExtractAudioScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var picked by remember { mutableStateOf<Uri?>(null) }
    var processing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("选择视频，提取为 MP3（对应原版「提取背景音乐」）") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            picked = uri
            status = "已选择视频，点击开始提取"
        }
    }
    ToolScaffold("提取音频", onBack) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(status, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { picker.launch(arrayOf("video/*")) }) { Text("选择视频") }
                Button(
                    enabled = picked != null && !processing,
                    onClick = {
                        val uri = picked ?: return@Button
                        processing = true
                        status = "正在提取音频..."
                        val input = MediaTools.copyToCache(context, uri)
                        if (input == null) {
                            processing = false
                            status = "读取视频失败"
                            return@Button
                        }
                        val output = File(context.externalCacheDir ?: context.cacheDir, "audio_${System.currentTimeMillis()}.mp3")
                        val cmd = "-y -i \"${input.absolutePath}\" -vn -acodec libmp3lame -q:a 2 \"${output.absolutePath}\""
                        MediaTools.runFfmpeg(context, cmd) { ok, log ->
                            processing = false
                            if (ok && output.exists()) {
                                val saved = MediaTools.saveAudioToGallery(context, output)
                                status = if (saved) "提取成功，已保存至 Music/橙子去水印" else "提取成功，但保存失败"
                            } else {
                                status = "提取失败：${log.takeLast(200)}"
                            }
                        }
                    }
                ) {
                    if (processing) {
                        CircularProgressIndicator(modifier = Modifier.width(16.dp).height(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("开始提取")
                }
            }
        }
    }
}

@Composable
fun AudioConvertScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var picked by remember { mutableStateOf<Uri?>(null) }
    var processing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("选择音频文件，转换为 M4A/AAC（对应原版「音频格式转换」）") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            picked = uri
            status = "已选择音频，点击开始转换"
        }
    }
    ToolScaffold("音频格式转换", onBack) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(status, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { picker.launch(arrayOf("audio/*")) }) { Text("选择音频") }
                Button(
                    enabled = picked != null && !processing,
                    onClick = {
                        val uri = picked ?: return@Button
                        processing = true
                        status = "正在转换..."
                        val input = MediaTools.copyToCache(context, uri)
                        if (input == null) {
                            processing = false
                            status = "读取音频失败"
                            return@Button
                        }
                        val output = File(context.externalCacheDir ?: context.cacheDir, "converted_${System.currentTimeMillis()}.m4a")
                        val cmd = "-y -i \"${input.absolutePath}\" -vn -c:a aac -b:a 192k \"${output.absolutePath}\""
                        MediaTools.runFfmpeg(context, cmd) { ok, log ->
                            processing = false
                            status = if (ok && output.exists()) "转换成功：${output.name}" else "转换失败：${log.takeLast(200)}"
                        }
                    }
                ) {
                    if (processing) {
                        CircularProgressIndicator(modifier = Modifier.width(16.dp).height(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("开始转换")
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "对应原版命令：-c:a aac -b:a 192k（无损容器改为 AAC 有损压缩）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
