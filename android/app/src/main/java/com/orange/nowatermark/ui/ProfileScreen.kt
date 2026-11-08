package com.orange.nowatermark.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orange.nowatermark.Screen
import com.orange.nowatermark.data.AppState
import com.orange.nowatermark.data.DataManager
import com.orange.nowatermark.ui.glass.AuroraBackground
import com.orange.nowatermark.ui.glass.GlassCard

@Composable
fun ProfileScreen(onNavigate: (Screen) -> Unit) {
    val context = LocalContext.current
    var autoDownload by remember { mutableStateOf(AppState.autoDownload) }
    var autoClipboard by remember { mutableStateOf(AppState.autoClipboard) }

    AuroraBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFF6FB6FF), Color(0xFF0A6CFF))),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.ContentCut, contentDescription = null, tint = Color.White)
                        }
                        Spacer(Modifier.size(14.dp))
                        Column {
                            Text(
                                "短视频去水印",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0E1116)
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "v1.0.0 · 完全本地解析",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF5B6472)
                            )
                        }
                    }
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(vertical = 4.dp)) {
                    MenuRow("分享给好友", Icons.Outlined.Share) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "推荐一个完全本地的去水印工具：短视频去水印，支持抖音/快手/小红书/B站，粘贴即用。"
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, "分享给好友"))
                    }
                    MenuRow("赞赏作者", Icons.Outlined.FavoriteBorder) { onNavigate(Screen.DONATE) }
                    MenuRow("隐私政策", Icons.Outlined.Lock) { onNavigate(Screen.PRIVACY) }
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                    Text("偏好设置", style = MaterialTheme.typography.titleSmall, color = Color(0xFF0E1116))
                    Spacer(Modifier.height(12.dp))
                    SettingRow("解析成功后自动下载", "节省一步操作", autoDownload) {
                        autoDownload = it
                        AppState.autoDownload = it
                        DataManager.saveSettings(autoDownload, autoClipboard)
                    }
                    Spacer(Modifier.height(10.dp))
                    SettingRow("自动识别剪贴板链接", "打开页面自动粘贴", autoClipboard) {
                        autoClipboard = it
                        AppState.autoClipboard = it
                        DataManager.saveSettings(autoDownload, autoClipboard)
                    }
                }
            }

            item {
                Text(
                    "所有解析都在本机完成，不上传链接、不收集个人信息。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5B6472),
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun MenuRow(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF243244), modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF0E1116), modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF9AA5B5))
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF0E1116))
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF5B6472))
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF0A6CFF),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFB9C3D3)
            )
        )
    }
}
