package com.orange.nowatermark.ui

import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.orange.nowatermark.data.AnalyseData
import com.orange.nowatermark.parser.LocalParser
import com.orange.nowatermark.ui.glass.AuroraBackground
import com.orange.nowatermark.ui.glass.GlassCard
import com.orange.nowatermark.ui.glass.GlassPrimaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class BatchRow(val source: String, val data: AnalyseData, val checked: Boolean = true)

@Composable
fun BatchScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val rows = remember { mutableStateListOf<BatchRow>() }

    AuroraBackground {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                }
                Text("批量解析", style = MaterialTheme.typography.titleLarge, color = Color(0xFF0E1116))
            }
            Spacer(Modifier.height(14.dp))

            GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    label = { Text("作品链接") },
                    placeholder = { Text("一行一个链接") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0A6CFF),
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedContainerColor = Color.White.copy(alpha = 0.35f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.25f)
                    )
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassPrimaryButton(
                        text = if (loading) "解析中…" else "开始解析",
                        enabled = !loading,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val links = input.lines().map { it.trim() }.filter { it.isNotBlank() }
                            if (links.isEmpty()) {
                                Toast.makeText(context, "请先粘贴链接", Toast.LENGTH_SHORT).show()
                                return@GlassPrimaryButton
                            }
                            loading = true
                            rows.clear()
                            scope.launch {
                                links.forEach { link ->
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching { LocalParser.parse(context, link) }
                                    }
                                    result.onSuccess { rows.add(BatchRow(link, it)) }
                                }
                                loading = false
                            }
                        }
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (rows.any { it.checked }) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.28f),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable(enabled = rows.any { it.checked }) {
                                val picked = rows.filter { it.checked }
                                picked.forEach { row ->
                                    val url = row.data.playAddr
                                    if (!url.isNullOrBlank()) {
                                        download(context, url, "video_${System.currentTimeMillis()}.mp4", row.data.referer)
                                    }
                                    row.data.pics.orEmpty().forEachIndexed { i, pic ->
                                        download(context, pic, "image_${System.currentTimeMillis()}_$i.jpg", row.data.referer)
                                    }
                                }
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "下载勾选 (${rows.count { it.checked }})",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF243244)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            if (loading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF0A6CFF), strokeWidth = 3.dp)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(rows) { row ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        tint = Color.White.copy(alpha = 0.5f),
                        elevation = 10.dp,
                        padding = PaddingValues(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = row.checked,
                                onCheckedChange = { checked ->
                                    val idx = rows.indexOf(row)
                                    if (idx >= 0) rows[idx] = row.copy(checked = checked)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0A6CFF))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    LocalParser.displayName(row.data.platform),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF0A6CFF)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    row.data.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF0E1116),
                                    maxLines = 2
                                )
                            }
                            Spacer(Modifier.size(8.dp))
                            coil.compose.AsyncImage(
                                model = row.data.cover,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}
