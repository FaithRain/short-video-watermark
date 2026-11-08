package com.orange.nowatermark.parser

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * 用隐藏 WebView 打开网页并在页面里执行 JS，取回结果。
 * 兼容抖音这类需要真实浏览器环境（SecSDK/UIFID）才能产出数据的站点。
 * WebView 必须在主线程创建，因此整个流程切到 Main。
 */
object WebScraper {

    private fun Context.findActivity(): Activity? {
        var c: Context? = this
        while (c is ContextWrapper) {
            if (c is Activity) return c
            c = c.baseContext
        }
        return null
    }

    suspend fun evaluate(
        context: Context,
        url: String,
        expression: String,
        timeoutMs: Long = 20_000,
        ready: (String) -> Boolean = { it != "null" && it != "\"\"" && it.isNotBlank() }
    ): String? = withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { cont ->
            val activity = context.findActivity()
            if (activity == null) {
                cont.resume(null)
                return@suspendCancellableCoroutine
            }
            val root = activity.window?.decorView as? ViewGroup
            val web = try {
                WebView(activity)
            } catch (t: Throwable) {
                cont.resume(null)
                return@suspendCancellableCoroutine
            }
            val handler = Handler(Looper.getMainLooper())
            var done = false

            fun cleanup() {
                runCatching { (web.parent as? ViewGroup)?.removeView(web) }
                runCatching { web.stopLoading() }
                runCatching { web.destroy() }
            }

            fun finish(value: String?) {
                if (done) return
                done = true
                handler.removeCallbacksAndMessages(null)
                cleanup()
                if (cont.isActive) cont.resume(value)
            }

            web.settings.javaScriptEnabled = true
            web.settings.domStorageEnabled = true
            web.settings.databaseEnabled = true
            web.settings.loadsImagesAutomatically = false
            web.setBackgroundColor(0)
            web.alpha = 0f
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)

            var polls = 0
            val maxPolls = (timeoutMs / 500).toInt()

            fun poll() {
                if (done) return
                if (polls++ > maxPolls) {
                    finish(null)
                    return
                }
                web.evaluateJavascript(expression) { raw ->
                    if (raw != null && ready(raw)) {
                        finish(raw)
                    } else {
                        handler.postDelayed({ poll() }, 500)
                    }
                }
            }

            web.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    // 阻止 snssdk:// 等自定义 scheme 跳转，保持页面在原地渲染
                    return !request.url.scheme.orEmpty().startsWith("http")
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    handler.postDelayed({ poll() }, 800)
                }
            }

            runCatching { root?.addView(web, ViewGroup.LayoutParams(1, 1)) }
            web.loadUrl(url)
            handler.postDelayed({ finish(null) }, timeoutMs + 2_000)

            cont.invokeOnCancellation { handler.post { finish(null) } }
        }
    }
}
