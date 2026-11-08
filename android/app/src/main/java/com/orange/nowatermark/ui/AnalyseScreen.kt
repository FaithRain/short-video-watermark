package com.orange.nowatermark.ui

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.orange.nowatermark.data.AnalyseData
import com.orange.nowatermark.parser.LocalParser
import com.orange.nowatermark.ui.glass.AuroraBackground
import com.orange.nowatermark.ui.glass.GlassCard
import com.orange.nowatermark.ui.glass.GlassPrimaryButton
import com.orange.nowatermark.ui.glass.glass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun AnalyseScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    var link by remember {
        mutableStateOf(
            clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim().orEmpty()
        )
    }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var data by remember { mutableStateOf<AnalyseData?>(null) }

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                }
                Text("一键去水印", style = MaterialTheme.typography.titleLarge, color = Color(0xFF0E1116))
            }
            Spacer(Modifier.height(18.dp))

            GlassCard(modifier = Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(18.dp)) {
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("作品链接") },
                    placeholder = { Text("粘贴抖音 / 快手 / 小红书 / B站链接") },
                    trailingIcon = {
                        if (link.isNotEmpty()) {
                            IconButton(onClick = { link = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = "清空")
                            }
                        }
                    },
                    minLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0A6CFF),
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedContainerColor = Color.White.copy(alpha = 0.35f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.25f)
                    )
                )
                Spacer(Modifier.height(14.dp))
                GlassPrimaryButton(
                    text = if (loading) "解析中…" else "立即开始解析",
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (link.isBlank()) {
                            error = "请先粘贴作品链接"
                            return@GlassPrimaryButton
                        }
                        loading = true
                        error = null
                        data = null
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching { LocalParser.parse(context, link) }
                            }
                            loading = false
                            result.onSuccess { data = it }
                                .onFailure { error = it.message ?: "解析失败" }
                        }
                    }
                )
            }

            if (loading) {
                Spacer(Modifier.height(26.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF0A6CFF), strokeWidth = 3.dp)
                }
            }

            error?.let {
                Spacer(Modifier.height(18.dp))
                Text(it, color = Color(0xFFD93F3F), style = MaterialTheme.typography.bodyMedium)
            }

            data?.let { item ->
                Spacer(Modifier.height(20.dp))
                ResultCard(item) { url, name ->
                    download(context, url, name, item.referer)
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun ResultCard(item: AnalyseData, onDownload: (String, String) -> Unit) {
    var tab by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val referer = item.referer
    val play = item.playAddr.orEmpty()
    val pics = item.pics.orEmpty()
    val desc = item.desc.orEmpty()
    GlassCard(modifier = Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        Text(
            LocalParser.displayName(item.platform),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF0A6CFF)
        )
        Spacer(Modifier.height(6.dp))
        Text(item.title, style = MaterialTheme.typography.titleSmall, color = Color(0xFF0E1116))
        Spacer(Modifier.height(14.dp))

        if (item.isVideo && play.isNotBlank()) {
            val player = remember(referer) {
                val headers = HashMap<String, String>()
                headers["User-Agent"] = LocalParser.UA
                referer?.let { headers["Referer"] = it }
                val factory = DefaultHttpDataSource.Factory()
                    .setDefaultRequestProperties(headers)
                    .setAllowCrossProtocolRedirects(true)
                    .setUserAgent(LocalParser.UA)
                ExoPlayer.Builder(context)
                    .setMediaSourceFactory(DefaultMediaSourceFactory(factory))
                    .build()
                    .apply { setMediaItem(MediaItem.fromUri(play)); prepare() }
            }
            androidx.compose.runtime.DisposableEffect(player) {
                onDispose { player.release() }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(Color(0xFF0C0F14), RoundedCornerShape(18.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = true
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        if (pics.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(pics) { url ->
                    coil.compose.AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier
                            .size(96.dp)
                            .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        TabRow(
            selectedTabIndex = tab,
            containerColor = Color.Transparent,
            contentColor = Color(0xFF0A6CFF),
            divider = {},
            indicator = { positions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(positions[tab]),
                    color = Color(0xFF0A6CFF)
                )
            }
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("文案") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("链接") })
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (tab == 0) desc.ifBlank { "（无文案）" } else play.ifBlank { "（无直链）" },
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF5B6472)
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassPill("复制文案") {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("文案", desc))
                Toast.makeText(context, "文案已复制", Toast.LENGTH_SHORT).show()
            }
            GlassPill("复制链接") {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("链接", play))
                Toast.makeText(context, "链接已复制", Toast.LENGTH_SHORT).show()
            }
            GlassPill(if (item.isVideo) "保存视频" else "保存图片", primary = true) {
                if (item.isVideo) {
                    onDownload(play, "video_${System.currentTimeMillis()}.mp4")
                } else {
                    pics.forEachIndexed { i, url ->
                        onDownload(url, "image_${System.currentTimeMillis()}_$i.jpg")
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassPill(text: String, primary: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (primary) Color(0xFF0A6CFF) else Color.White.copy(alpha = 0.55f),
                RoundedCornerShape(18.dp)
            )
            .glass(RoundedCornerShape(18.dp), if (primary) Color.Transparent else Color.White.copy(alpha = 0.5f), elevation = 0.dp)
            .clickableNoRipple(onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (primary) Color.White else Color(0xFF243244)
        )
    }
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    )

fun download(context: Context, url: String, fileName: String, referer: String?) {
    if (url.isBlank()) return
    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle(fileName)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(
            if (fileName.endsWith(".mp4")) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES,
            fileName
        )
        .addRequestHeader("User-Agent", LocalParser.UA)
    referer?.let { request.addRequestHeader("Referer", it) }
    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    dm.enqueue(request)
    Toast.makeText(context, "已开始下载", Toast.LENGTH_SHORT).show()
}
