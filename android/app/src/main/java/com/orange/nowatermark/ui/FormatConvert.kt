package com.orange.nowatermark.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.orange.nowatermark.media.MediaTools
import com.orange.nowatermark.ui.glass.GlassCard
import com.orange.nowatermark.ui.glass.GlassPrimaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class ConvertMode(
    val title: String,
    val desc: String,
    val mime: String,
    val outExt: String,
    val isAudio: Boolean
) {
    VIDEO_TO_MP3("视频 → MP3", "提取视频里的声音，保存为 MP3", "video/*", "mp3", true),
    VIDEO_TO_GIF("视频 → GIF", "生成循环动图，适合表情与预览", "video/*", "gif", false),
    AUDIO_TO_M4A("音频 → M4A", "AAC 编码，体积小、兼容好", "audio/*", "m4a", true),
    AUDIO_TO_WAV("音频 → WAV", "无损 PCM，适合后期处理", "audio/*", "wav", true)
}

@Composable
fun FormatConvertScreen(mode: ConvertMode, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var picked by remember { mutableStateOf<Uri?>(null) }
    var processing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            picked = uri
            status = ""
        }
    }

    ToolScaffold(title = mode.title, onBack = onBack) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                tint = Color.White.copy(alpha = 0.46f),
                elevation = 12.dp,
                padding = PaddingValues(18.dp)
            ) {
                Column {
                    Text(mode.title, style = MaterialTheme.typography.titleMedium, color = Color(0xFF0E1116))
                    Spacer(Modifier.height(6.dp))
                    Text(mode.desc, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF5B6472))
                    Spacer(Modifier.height(14.dp))
                    Text(
                        picked?.lastPathSegment ?: "尚未选择文件",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF5B6472)
                    )
                }
            }

            GlassPrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = when {
                    processing -> "转换中…"
                    picked == null -> "选择文件"
                    else -> "开始转换"
                },
                loading = processing,
                onClick = {
                    val uri = picked
                    if (uri == null) {
                        picker.launch(arrayOf(mode.mime))
                        return@GlassPrimaryButton
                    }
                    processing = true
                    status = ""
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            runCatching {
                                val input = MediaTools.copyToCache(context, uri) ?: return@runCatching false
                                val dir: File = context.externalCacheDir ?: context.cacheDir
                                val out = File(dir, "convert_${System.currentTimeMillis()}.${mode.outExt}")
                                val cmd = buildCommand(mode, input.absolutePath, out.absolutePath)
                                var success = false
                                MediaTools.runFfmpeg(context, cmd) { done, _ ->
                                    success = done
                                    if (done) {
                                        if (mode.isAudio) MediaTools.saveAudioToGallery(context, out)
                                        else MediaTools.saveFileToGallery(context, out)
                                    }
                                }
                                success
                            }.getOrDefault(false)
                        }
                        processing = false
                        status = if (ok) "已保存到相册" else "转换失败，请换一个文件重试"
                    }
                }
            )

            if (status.isNotEmpty()) {
                Text(status, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF0A6CFF))
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

private fun buildCommand(mode: ConvertMode, input: String, output: String): String = when (mode) {
    ConvertMode.VIDEO_TO_MP3 -> "-y -i \"$input\" -vn -acodec libmp3lame -q:a 2 \"$output\""
    ConvertMode.VIDEO_TO_GIF -> "-y -i \"$input\" -vf \"fps=12,scale=480:-1:flags=lanczos\" -loop 0 \"$output\""
    ConvertMode.AUDIO_TO_M4A -> "-y -i \"$input\" -vn -c:a aac -b:a 192k \"$output\""
    ConvertMode.AUDIO_TO_WAV -> "-y -i \"$input\" -vn -c:a pcm_s16le \"$output\""
}
