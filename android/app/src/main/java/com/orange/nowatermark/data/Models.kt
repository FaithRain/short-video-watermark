package com.orange.nowatermark.data

/** 解析结果：抖音/快手/小红书/ B 站统一成这一种形状。 */
data class AnalyseData(
    val playAddr: String? = null,
    val cover: String? = null,
    val desc: String? = null,
    val type: Int? = null,
    val pics: List<String>? = null,
    val music: String? = null,
    val videos: List<String>? = null,
    val size: Int? = null,
    val platform: String? = null,
    val referer: String? = null,
    val isSelected: Boolean = false
) {
    val title: String get() = desc?.takeIf { it.isNotBlank() } ?: "未命名"
    val isVideo: Boolean get() = type != 2
}
