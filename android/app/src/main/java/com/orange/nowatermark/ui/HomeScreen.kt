package com.orange.nowatermark.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.orange.nowatermark.Screen
import com.orange.nowatermark.ui.glass.AuroraBackground
import com.orange.nowatermark.ui.glass.GlassCard
import com.orange.nowatermark.ui.glass.glass
import com.orange.nowatermark.ui.glass.pressRipple

private val Ink = Color(0xFF0E1116)
private val Muted = Color(0xFF5B6472)
private val Hair = Color.White.copy(alpha = 0.55f)

@Composable
fun HomeScreen(onNavigate: (Screen) -> Unit, onConvert: (ConvertMode) -> Unit) {
    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(32.dp))
            Text("短视频去水印", style = MaterialTheme.typography.headlineLarge, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text("全部在本机完成 · 链接不上传", style = MaterialTheme.typography.bodyMedium, color = Muted)
            Spacer(Modifier.height(22.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroTile("一键去水印", "粘贴链接即出无水印视频", Icons.Rounded.AutoFixHigh, Color(0xFFEAF2FF), Modifier.weight(1f)) {
                    onNavigate(Screen.ANALYSE)
                }
                HeroTile("批量解析", "多链接一次解析下载", Icons.Rounded.Download, Color(0xFFE9F7F3), Modifier.weight(1f)) {
                    onNavigate(Screen.BATCH)
                }
            }

            SectionLabel("音视频工具")
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                tint = Color.White.copy(alpha = 0.44f),
                elevation = 12.dp,
                padding = PaddingValues(vertical = 4.dp)
            ) {
                ToolRow("去除背景音乐", "无损静音，保留画面", Icons.Rounded.MusicOff, Color(0xFFF4EEFF)) { onNavigate(Screen.REMOVE_MUSIC) }
                Hairline()
                ToolRow("提取音频", "视频转 MP3", Icons.Rounded.AudioFile, Color(0xFFFFF6DF)) { onNavigate(Screen.EXTRACT_AUDIO) }
                Hairline()
                ToolRow("视频去水印", "框选区域，智能擦除", Icons.Rounded.Movie, Color(0xFFEAF0FF)) { onNavigate(Screen.WATERMARK) }
                Hairline()
                ToolRow("视频裁剪", "按时间精确截取", Icons.Rounded.ContentCut, Color(0xFFEDF8EC)) { onNavigate(Screen.CROP) }
            }

            SectionLabel("格式转换")
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                tint = Color.White.copy(alpha = 0.44f),
                elevation = 12.dp,
                padding = PaddingValues(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ConvertChip("视频 → MP3", Icons.Rounded.AudioFile, Modifier.weight(1f)) { onConvert(ConvertMode.VIDEO_TO_MP3) }
                    ConvertChip("视频 → GIF", Icons.Rounded.Movie, Modifier.weight(1f)) { onConvert(ConvertMode.VIDEO_TO_GIF) }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ConvertChip("音频 → M4A", Icons.Rounded.GraphicEq, Modifier.weight(1f)) { onConvert(ConvertMode.AUDIO_TO_M4A) }
                    ConvertChip("音频 → WAV", Icons.Rounded.MusicNote, Modifier.weight(1f)) { onConvert(ConvertMode.AUDIO_TO_WAV) }
                }
            }

            Spacer(Modifier.height(18.dp))
            TutorialTile { onNavigate(Screen.TUTORIAL) }
            Spacer(Modifier.height(44.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(22.dp))
    Text(text, style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun Hairline() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .height(1.dp)
            .background(Hair)
    )
}

@Composable
private fun HeroTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = MutableInteractionSource()
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier
            .scale(if (pressed) 0.96f else 1f)
            .clip(shape)
            .pressRipple(interaction, Color(0xFF0A6CFF), 0.20f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            tint = Color.White.copy(alpha = 0.50f),
            elevation = 14.dp,
            padding = PaddingValues(16.dp)
        ) {
            Column {
                Box(
                    Modifier
                        .size(42.dp)
                        .background(iconBg, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color(0xFF243244), modifier = Modifier.size(21.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, color = Ink)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 2)
            }
        }
    }
}

@Composable
private fun ToolRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    onClick: () -> Unit
) {
    val interaction = MutableInteractionSource()
    Row(
        Modifier
            .fillMaxWidth()
            .pressRipple(interaction, Color(0xFF0A6CFF), 0.12f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .background(iconBg, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF243244), modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = Ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Muted.copy(alpha = 0.55f))
    }
}

@Composable
private fun ConvertChip(text: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = MutableInteractionSource()
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .scale(if (pressed) 0.96f else 1f)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.52f))
            .pressRipple(interaction, Color(0xFF0A6CFF), 0.12f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF243244), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Ink)
    }
}

@Composable
private fun TutorialTile(onClick: () -> Unit) {
    val interaction = MutableInteractionSource()
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .scale(if (pressed) 0.97f else 1f)
            .clip(shape)
            .pressRipple(interaction, Color(0xFF0A6CFF), 0.14f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            tint = Color.White.copy(alpha = 0.42f),
            elevation = 10.dp,
            padding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .background(Color(0xFFEDF8EC), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = Color(0xFF243244), modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("操作教程", style = MaterialTheme.typography.bodyLarge, color = Ink, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Muted.copy(alpha = 0.55f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(onBack: () -> Unit) {
    AuroraBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            TopAppBar(
                title = { Text("操作教程") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
            WebAsset(Modifier.fillMaxSize(), "tutorial.html")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    AuroraBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            TopAppBar(
                title = { Text("隐私政策") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
            WebAsset(Modifier.fillMaxSize(), "privacy.html")
        }
    }
}

@Composable
private fun WebAsset(modifier: Modifier = Modifier, asset: String) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = false
                setBackgroundColor(0x00000000)
                loadUrl("file:///android_asset/$asset")
            }
        }
    )
}

@Composable
fun PrivacyDialog(onAgree: () -> Unit, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        shape = RoundedCornerShape(26.dp),
        containerColor = Color(0xFFF9FBFF),
        title = { Text("隐私政策", fontWeight = FontWeight.SemiBold) },
        text = {
            Text(
                "感谢使用短视频去水印。所有解析都在本机完成，不会上传你的链接；" +
                    "仅在本地保存你的开关偏好。点击「同意」即表示你已阅读并同意隐私政策。",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start
            )
        },
        confirmButton = { TextButton(onClick = onAgree) { Text("同意") } },
        dismissButton = { TextButton(onClick = onExit) { Text("不同意") } }
    )
}
