package com.orange.nowatermark.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.orange.nowatermark.ui.glass.AuroraBackground
import com.orange.nowatermark.ui.glass.GlassCard
import com.orange.nowatermark.ui.glass.glass
import java.io.File

private const val ALIPAY_ASSET = "donate_alipay.png"
private const val WECHAT_ASSET = "donate_wechat.png"

@Composable
fun DonateScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    val asset = if (tab == 0) ALIPAY_ASSET else WECHAT_ASSET

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
                Text("赞赏作者", style = MaterialTheme.typography.titleLarge, color = Color(0xFF0E1116))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "如果这个工具帮到了你，可以请作者喝杯咖啡。",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5B6472)
            )
            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SegmentTab("支付宝", tab == 0, Modifier.weight(1f)) { tab = 0 }
                SegmentTab("微信", tab == 1, Modifier.weight(1f)) { tab = 1 }
            }
            Spacer(Modifier.height(16.dp))

            GlassCard(modifier = Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(18.dp)) {
                Text(
                    if (tab == 0) "支付宝收款码" else "微信收款码",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF0E1116)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "长按识别，或点按保存到相册后扫码",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5B6472)
                )
                Spacer(Modifier.height(16.dp))

                val bitmap = remember(asset) { loadAssetBitmap(context, asset) }
                if (bitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .clickable { saveAsset(context, asset) },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth(0.92f),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "收款码待放入\nassets/$asset",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF5B6472)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    "赞赏完全自愿，不影响任何功能。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5B6472)
                )
            }

            Spacer(Modifier.height(18.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                Text("谢谢你的支持", style = MaterialTheme.typography.titleSmall, color = Color(0xFF0E1116))
                Spacer(Modifier.height(6.dp))
                Text(
                    "每一份心意都会变成下一版优化的动力。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5B6472)
                )
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SegmentTab(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .glass(
                RoundedCornerShape(18.dp),
                if (selected) Color(0xFF0A6CFF) else Color.White.copy(alpha = 0.55f),
                elevation = if (selected) 10.dp else 0.dp
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else Color(0xFF243244)
        )
    }
}

private fun loadAssetBitmap(context: Context, asset: String): android.graphics.Bitmap? =
    runCatching { context.assets.open(asset).use { BitmapFactory.decodeStream(it) } }.getOrNull()

private fun saveAsset(context: Context, asset: String) {
    runCatching { context.assets.open(asset).use { it.readBytes() } }
        .onSuccess { bytes -> saveToGallery(context, bytes, asset) }
        .onFailure { Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show() }
}

private fun saveToGallery(context: Context, bytes: ByteArray, name: String) {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= 29) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/短视频去水印")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    if (uri == null) {
        Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
        return
    }
    resolver.openOutputStream(uri)?.use { it.write(bytes) }
    if (Build.VERSION.SDK_INT >= 29) {
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }
    Toast.makeText(context, "已保存到相册", Toast.LENGTH_SHORT).show()
}
