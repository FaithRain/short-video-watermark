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
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.MenuBook
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.orange.nowatermark.Screen
import com.orange.nowatermark.ui.glass.AuroraBackground
import com.orange.nowatermark.ui.glass.GlassCard
import com.orange.nowatermark.ui.glass.centerPressRipple
import com.orange.nowatermark.ui.glass.glass

@Composable
fun HomeScreen(onNavigate: (Screen) -> Unit) {
    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(34.dp))
            Text("短视频去水印", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF0E1116))
            Spacer(Modifier.height(26.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureTile("一键去水印", Icons.Rounded.AutoFixHigh, Color(0xFFEAF2FF), Modifier.weight(1f)) {
                    onNavigate(Screen.ANALYSE)
                }
                FeatureTile("批量解析", Icons.Rounded.Download, Color(0xFFE9F7F3), Modifier.weight(1f)) {
                    onNavigate(Screen.BATCH)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureTile("去除背景音乐", Icons.Rounded.MusicOff, Color(0xFFF4EEFF), Modifier.weight(1f)) {
                    onNavigate(Screen.REMOVE_MUSIC)
                }
                FeatureTile("操作教程", Icons.Rounded.MenuBook, Color(0xFFEDF8EC), Modifier.weight(1f)) {
                    onNavigate(Screen.TUTORIAL)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureTile("提取音频", Icons.Rounded.AudioFile, Color(0xFFFFF6DF), Modifier.weight(1f)) {
                    onNavigate(Screen.EXTRACT_AUDIO)
                }
                FeatureTile("音频转换", Icons.Rounded.Equalizer, Color(0xFFEAF0FF), Modifier.weight(1f)) {
                    onNavigate(Screen.AUDIO_CONVERT)
                }
            }
            Spacer(Modifier.height(44.dp))
        }
    }
}

@Composable
private fun FeatureTile(
    title: String,
    icon: ImageVector,
    iconBg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = MutableInteractionSource()
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .scale(if (pressed) 0.95f else 1f)
            .clip(shape)
            .centerPressRipple(interaction, Color(0xFF0A6CFF))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            tint = Color.White.copy(alpha = 0.50f),
            elevation = 12.dp,
            padding = PaddingValues(vertical = 20.dp, horizontal = 10.dp)
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(iconBg, RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color(0xFF243244), modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, color = Color(0xFF0E1116))
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
