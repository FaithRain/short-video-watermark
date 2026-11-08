package com.orange.nowatermark.parser

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.orange.nowatermark.data.AnalyseData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 纯客户端解析：不经过任何自建服务端。
 * 抖音/快手/小红书用隐藏 WebView 让真实页面自己跑完风控，再读取页面里的状态 JSON；
 * B 站直接调公开 API；其它站点读 og:video / og:image。
 */
object LocalParser {

    const val UA =
        "Mozilla/5.0 (Linux; Android 13; V2505A) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    private const val DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private val idRegex = Regex("(?:video|note|slides|explore)/(\\d{8,})|(\\d{15,})")
    private val bvRegex = Regex("BV[0-9A-Za-z]{10}")
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun supports(link: String): Boolean = platform(link) != null

    fun platformName(link: String): String = platform(link) ?: "其它"

    fun displayName(platform: String?): String = when (platform) {
        "douyin", "抖音" -> "抖音"
        "kuaishou", "快手" -> "快手"
        "xiaohongshu", "小红书" -> "小红书"
        "bilibili", "B站", "b站" -> "B站"
        else -> "其它"
    }

    /** B 站 CDN 需要 Referer，播放/下载都要带。 */
    fun refererFor(link: String): String? = when (platform(link)) {
        "bilibili" -> "https://www.bilibili.com/"
        else -> null
    }

    /** 从分享文案里抠出可解析的 URL；支持整段复制文字。 */
    fun extractUrl(text: String): String {
        val urls = Regex("https?://[^\\s\"'<>]+").findAll(text).map { it.value.trimEnd('.', ',', '。', '，', ')', '）') }.toList()
        return urls.firstOrNull { hostOf(it) != null } ?: urls.firstOrNull() ?: text.trim()
    }

    private fun hostOf(url: String): String? =
        runCatching { java.net.URI(url).host?.lowercase() }.getOrNull()?.takeIf { it.isNotBlank() }

    private fun host(link: String): String = hostOf(extractUrl(link)).orEmpty()

    private fun platform(link: String): String? {
        val h = host(link)
        return when {
            h.contains("douyin") -> "douyin"
            h.contains("kuaishou") || h.contains("chenzhongtech") -> "kuaishou"
            h.contains("xiaohongshu") || h.contains("xhslink") -> "xiaohongshu"
            h.contains("bilibili") || h.contains("b23.tv") -> "bilibili"
            else -> null
        }
    }

    suspend fun parse(context: Context, link: String): AnalyseData {
        val url = extractUrl(link)
        return when (platform(url)) {
            "douyin" -> douyin(context, url)
            "kuaishou" -> kuaishou(context, url)
            "xiaohongshu" -> xiaohongshu(context, url)
            "bilibili" -> bilibili(url)
            else -> throw IllegalStateException("暂不支持这个平台，目前支持抖音、快手、小红书、B站")
        }
    }

    // ---------------- 抖音 ----------------

    private suspend fun douyin(context: Context, link: String): AnalyseData {
        val id = withContext(Dispatchers.IO) { resolveId(link) }
            ?: throw IllegalStateException("无法从链接提取作品 ID")
        val raw = WebScraper.evaluate(
            context,
            "https://m.douyin.com/share/video/$id",
            "window._ROUTER_DATA ? JSON.stringify(window._ROUTER_DATA) : null"
        ) ?: throw IllegalStateException("页面加载超时，请检查网络后重试")
        val root = json(decode(raw))
        val item = findDouyinItem(root)
            ?: throw IllegalStateException("页面中没有视频数据，作品可能已删除或需要登录")
        val video = obj(item, "video")
        val play = firstUrl(obj(video, "play_addr"))
            ?.replace("playwm", "play")
            ?.replace("ratio=720p", "ratio=1080p")
        val cover = firstUrl(obj(video, "cover")) ?: firstUrl(obj(video, "origin_cover"))
        val music = firstUrl(obj(obj(item, "music"), "play_url"))
        val pics = arr(item, "images")?.mapNotNull { el ->
            if (el.isJsonObject) firstUrl(el.asJsonObject) else null
        }.orEmpty()
        return AnalyseData(
            playAddr = play ?: pics.firstOrNull(),
            cover = cover ?: pics.firstOrNull(),
            desc = str(item, "desc").orEmpty(),
            type = if (pics.isNotEmpty()) 2 else 1,
            pics = pics,
            music = music,
            videos = play?.let { listOf(it) } ?: emptyList(),
            platform = "抖音"
        )
    }

    private fun findDouyinItem(root: JsonObject): JsonObject? {
        val loader = obj(root, "loaderData") ?: return null
        for (key in loader.keySet()) {
            val node = obj(loader, key) ?: continue
            for (field in listOf("videoInfoRes", "noteInfoRes")) {
                val res = obj(node, field) ?: continue
                val list = arr(res, "item_list") ?: continue
                if (list.size() > 0 && list.get(0).isJsonObject) return list.get(0).asJsonObject
            }
            obj(node, "aweme_detail")?.let { return it }
        }
        return null
    }

    // ---------------- 快手 ----------------

    private suspend fun kuaishou(context: Context, link: String): AnalyseData {
        val pageUrl = withContext(Dispatchers.IO) { follow(link) } ?: link
        val expr = "(()=>{try{if(window.__APOLLO_STATE__)return JSON.stringify(window.__APOLLO_STATE__);if(window.INIT_STATE)return JSON.stringify(window.INIT_STATE);return null}catch(e){return null}})()"
        val raw = WebScraper.evaluate(context, pageUrl, expr, timeoutMs = 30_000, ready = { it != null && it != "null" && it.contains("mainMvUrls") })
        val state = raw?.let { runCatching { json(decode(it)) }.getOrNull() }
        var play: String? = null
        var cover: String? = null
        var caption: String? = null
        state?.let { root ->
            walk(root) { o ->
                if (play == null) play = str(o, "photoUrl")
                if (play == null) {
                    val mv = arr(o, "mainMvUrls")
                    if (mv != null && mv.size() > 0 && mv.get(0).isJsonObject) {
                        play = str(mv.get(0).asJsonObject, "url")
                    }
                }
                if (cover == null) {
                    cover = str(o, "coverUrl") ?: str(o, "cover")
                    if (cover == null) {
                        val cvs = arr(o, "coverUrls")
                        if (cvs != null && cvs.size() > 0 && cvs.get(0).isJsonObject) cover = str(cvs.get(0).asJsonObject, "url")
                    }
                }
                if (caption == null) caption = str(o, "caption")
            }
        }
        if (play.isNullOrBlank() || play == "null") {
            play = meta(context, pageUrl, "og:video") ?: meta(context, pageUrl, "og:video:url")
        }
        if (cover.isNullOrBlank() || cover == "null") cover = meta(context, pageUrl, "og:image")
        val finalPlay = play?.takeIf { it.isNotBlank() && it != "null" }
            ?: throw IllegalStateException("没有从快手页面拿到视频地址，作品可能已删除或需要登录")
        return AnalyseData(
            playAddr = finalPlay,
            cover = cover,
            desc = caption.orEmpty(),
            type = 1,
            videos = listOf(finalPlay),
            platform = "快手"
        )
    }

    // ---------------- 小红书 ----------------

    private suspend fun xiaohongshu(context: Context, link: String): AnalyseData {
        val pageUrl = withContext(Dispatchers.IO) { follow(link) } ?: link
        val expr = "(()=>{try{return window.__INITIAL_STATE__?JSON.stringify(window.__INITIAL_STATE__):null}catch(e){return null}})()"
        val raw = WebScraper.evaluate(context, pageUrl, expr, timeoutMs = 30_000, ready = { it != null && it != "null" && (it.contains("noteDetailMap") || it.contains("noteData")) })
            ?: throw IllegalStateException("页面加载超时，请检查网络后重试")
        val root = runCatching { json(decode(raw)) }.getOrNull()
            ?: throw IllegalStateException("小红书页面数据解析失败")
        val note = findXhsNote(root)
            ?: throw IllegalStateException("页面中没有笔记数据，可能需要登录或作品已删除")
        val video = obj(note, "video")
        var play: String? = null
        // 新版：video.mediaV2 是 JSON 字符串，里面的 stream.h264[].masterUrl
        val mediaV2 = str(video, "mediaV2")
        if (!mediaV2.isNullOrBlank()) {
            play = deepFind(mediaV2, "masterUrl")
                ?: Regex("\"masterUrl\"\\s*:\\s*\"(.*?)\"")
                    .find(mediaV2)?.groupValues?.get(1)
        }
        if (play.isNullOrBlank()) {
            val media = obj(video, "media")
            val stream = obj(media, "stream")
            for (codec in listOf("h264", "h265", "av1")) {
                val list = arr(stream, codec) ?: continue
                if (list.size() > 0 && list.get(0).isJsonObject) {
                    play = str(list.get(0).asJsonObject, "masterUrl")
                        ?: str(list.get(0).asJsonObject, "backupUrls")
                    if (!play.isNullOrBlank()) break
                }
            }
        }
        val images = arr(note, "imageList")?.mapNotNull { el ->
            if (el.isJsonObject) str(el.asJsonObject, "urlDefault") ?: str(el.asJsonObject, "url") else null
        }.orEmpty()
        var title = str(note, "title") ?: str(note, "desc")
        if (title.isNullOrBlank()) {
            val nick = str(obj(note, "user"), "nickName")
            title = if (nick.isNullOrBlank()) "小红书笔记" else "${nick}的小红书笔记"
        }
        val cover = images.firstOrNull()
        return AnalyseData(
            playAddr = play ?: cover,
            cover = cover,
            desc = title.orEmpty(),
            type = if (play.isNullOrBlank() && images.isNotEmpty()) 2 else 1,
            pics = images,
            videos = play?.let { listOf(it) } ?: emptyList(),
            platform = "小红书"
        )
    }

    private fun findXhsNote(root: JsonObject): JsonObject? {
        val note = obj(root, "note")
        val map = obj(note, "noteDetailMap")
        if (map != null) {
            for (key in map.keySet()) {
                val entry = obj(map, key) ?: continue
                obj(entry, "note")?.let { return it }
            }
        }
        // 新版：__INITIAL_STATE__.noteData.data.noteData
        obj(root, "noteData")?.let { nd ->
            obj(nd, "data")?.let { data ->
                obj(data, "noteData")?.let { return it }
            }
        }
        return null
    }

    private fun deepFind(jsonText: String, key: String): String? =
        runCatching { deepFind(json(jsonText), key) }.getOrNull()

    private fun deepFind(el: JsonElement?, key: String): String? = when {
        el == null || el.isJsonNull -> null
        el.isJsonObject -> {
            val o = el.asJsonObject
            val direct = o.get(key)
            if (direct != null && direct.isJsonPrimitive) direct.asString
            else o.entrySet().asSequence().mapNotNull { deepFind(it.value, key) }.firstOrNull()
        }
        el.isJsonArray -> el.asJsonArray.asSequence().mapNotNull { deepFind(it, key) }.firstOrNull()
        else -> null
    }

    // ---------------- B 站 ----------------

    private suspend fun bilibili(link: String): AnalyseData = withContext(Dispatchers.IO) {
        val finalUrl = follow(link) ?: link
        val bvid = bvRegex.find(finalUrl)?.value ?: bvRegex.find(link)?.value
            ?: throw IllegalStateException("无法从链接提取 BV 号")
        val view = getJson(
            "https://api.bilibili.com/x/web-interface/view?bvid=$bvid",
            mapOf("Referer" to "https://www.bilibili.com/", "User-Agent" to DESKTOP_UA)
        )
        val data = obj(view, "data") ?: throw IllegalStateException("B 站接口没有返回数据")
        val cid = arr(data, "pages")?.takeIf { it.size() > 0 }?.get(0)?.asJsonObject?.let { str(it, "cid") }
            ?: str(data, "cid")
        val title = str(data, "title").orEmpty()
        val cover = str(data, "pic")
        var play: String? = null
        if (!cid.isNullOrBlank()) {
            val playUrl = getJson(
                "https://api.bilibili.com/x/player/playurl?bvid=$bvid&cid=$cid&qn=64&fnval=16&fourk=1",
                mapOf("Referer" to "https://www.bilibili.com/", "User-Agent" to DESKTOP_UA)
            )
            val pd = obj(playUrl, "data")
            val dash = obj(pd, "dash")
            val videos = arr(dash, "video")
            if (videos != null && videos.size() > 0 && videos.get(0).isJsonObject) {
                val v = videos.get(0).asJsonObject
                play = str(v, "baseUrl") ?: str(v, "base_url")
            }
            if (play.isNullOrBlank()) {
                val durl = arr(pd, "durl")
                if (durl != null && durl.size() > 0 && durl.get(0).isJsonObject) {
                    play = str(durl.get(0).asJsonObject, "url")
                }
            }
        }
        AnalyseData(
            playAddr = play,
            cover = cover,
            desc = title,
            type = 1,
            videos = play?.let { listOf(it) } ?: emptyList(),
            platform = "B站",
            referer = "https://www.bilibili.com/"
        )
    }

    // ---------------- 通用 WebView 兜底 ----------------

    private suspend fun generic(context: Context, link: String): AnalyseData {
        val expr = """(()=>{const v=document.querySelector('meta[property="og:video"]')||
            document.querySelector('meta[property="og:video:url"]');
            const i=document.querySelector('meta[property="og:image"]');
            const t=document.querySelector('meta[property="og:title"]');
            return JSON.stringify({video:v?v.content:null,image:i?i.content:null,title:t?t.content:null})})()"""
        val raw = WebScraper.evaluate(context, link, expr) ?: throw IllegalStateException("页面加载超时")
        val root = runCatching { json(decode(raw)) }.getOrNull()
        val play = root?.let { str(it, "video") }
        val image = root?.let { str(it, "image") }
        val title = root?.let { str(it, "title") }
        if (play.isNullOrBlank()) throw IllegalStateException("没有从页面找到视频地址，暂不支持该站点")
        return AnalyseData(
            playAddr = play,
            cover = image,
            desc = title.orEmpty(),
            type = 1,
            videos = listOf(play),
            platform = "其它"
        )
    }

    private suspend fun meta(context: Context, pageUrl: String, prop: String): String? {
        val expr = """document.querySelector('meta[property="$prop"]')?.content || null"""
        val raw = WebScraper.evaluate(context, pageUrl, expr) ?: return null
        val text = decode(raw)
        return text.takeIf { it.isNotBlank() && it != "null" }
    }

    // ---------------- 工具 ----------------

    private fun json(text: String): JsonObject = JsonParser.parseString(text).asJsonObject

    private fun decode(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("\"")) {
            text = JsonParser.parseString(text).asString
        }
        return text
    }

    private fun obj(o: JsonObject?, name: String): JsonObject? {
        val e = o?.get(name) ?: return null
        return if (e.isJsonObject) e.asJsonObject else null
    }

    private fun arr(o: JsonObject?, name: String): JsonArray? {
        val e = o?.get(name) ?: return null
        return if (e.isJsonArray) e.asJsonArray else null
    }

    private fun str(o: JsonObject?, name: String): String? {
        val e = o?.get(name) ?: return null
        return if (e.isJsonPrimitive) runCatching { e.asString }.getOrNull() else null
    }

    private fun firstUrl(node: JsonObject?): String? {
        val list = arr(node, "url_list") ?: return null
        for (i in 0 until list.size()) {
            val el = list.get(i)
            if (el.isJsonPrimitive) {
                val u = runCatching { el.asString }.getOrNull()
                if (!u.isNullOrBlank()) return u
            }
        }
        return null
    }

    private fun walk(element: JsonElement, visit: (JsonObject) -> Unit) {
        when {
            element.isJsonObject -> {
                val o = element.asJsonObject
                visit(o)
                for (key in o.keySet()) o.get(key)?.let { walk(it, visit) }
            }
            element.isJsonArray -> {
                val a = element.asJsonArray
                for (i in 0 until a.size()) walk(a.get(i), visit)
            }
        }
    }

    private fun extractId(text: String): String? {
        val m = idRegex.find(text) ?: return null
        return m.groupValues[1].ifBlank { m.groupValues[2] }.ifBlank { null }
    }

    private fun resolveId(link: String): String? {
        extractId(link)?.let { return it }
        return runCatching {
            extractId(follow(link).orEmpty())
        }.getOrNull()
    }

    private fun follow(link: String): String? = runCatching {
        val req = Request.Builder().url(link).header("User-Agent", UA).build()
        client.newCall(req).execute().use { it.request.url.toString() }
    }.getOrNull()

    private fun getJson(url: String, headers: Map<String, String>): JsonObject {
        val builder = Request.Builder().url(url).get()
        headers.forEach { (k, v) -> builder.addHeader(k, v) }
        client.newCall(builder.build()).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            return json(body)
        }
    }
}
