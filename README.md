# 短视频去水印

纯本地解析的 Android 去水印客户端：粘贴分享链接，直接在手机内解析出无水印视频/图集，**链接不上传、无需登录、无自建服务端**。

当前版本 **1.0.0**（versionCode 1000）。

## 功能

- **一键去水印**：抖音 / 快手 / 小红书 / B 站，支持视频与图文
- **批量解析**：多行链接逐条解析、勾选下载
- **去除背景音乐**：FFmpegKit `-an -vcodec copy` 无损静音
- **提取音频**：视频转 MP3（`libmp3lame -q:a 2`）
- **音频转换**：转 M4A/AAC
- **本地水印擦除 / 视频裁剪**：框选 delogo / 首尾裁剪
- **赞赏作者**：支付宝 / 微信收款码（把图片放到 `app/src/main/assets/donate_alipay.png` 与 `donate_wechat.png` 即可）

界面采用浅色「液态玻璃」风格（Aurora 背景 + 半透明玻璃卡 + 悬浮胶囊导航）。

## 解析原理

| 平台 | 方式 |
| --- | --- |
| 抖音 | App 内隐藏 WebView 打开分享页，读取 `window._ROUTER_DATA` → `videoInfoRes`，`playwm`→`play` 去水印 |
| 快手 | 短链展开 → `window.__APOLLO_STATE__` 递归取 `photoUrl` / `mainMvUrls`，og:video 兜底 |
| 小红书 | `window.__INITIAL_STATE__.note.noteDetailMap` → 视频流 / 图集 |
| B 站 | 开放 API：`x/web-interface/view` + `x/player/playurl`，播放/下载时带 `Referer: https://www.bilibili.com/` |

> 抖音页面由真实 WebView 加载，ByteDance SecSDK 正常执行，因此无需服务端签名。

## 构建

需要 JDK 17、Android SDK（platform 35 / build-tools 35.0.0）、Gradle 8.9。

```bash
export ANDROID_HOME=/path/to/android-sdk
cd android
gradle assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

依赖包含 `com.arthenica:ffmpeg-kit-full-gpl:6.0-2`（Maven Central 已下线，建议在 `settings.gradle.kts` 保留阿里云镜像）。

## 目录

```
android/app/src/main/java/com/orange/nowatermark/
├── MainActivity.kt          # 导航与隐私弹窗
├── data/                    # 模型与本地存储
├── media/MediaTools.kt      # FFmpeg / 相册保存
├── parser/LocalParser.kt    # 四平台解析
├── parser/WebScraper.kt     # 隐藏 WebView 取值
└── ui/                      # Compose 屏幕与玻璃组件
docs/REPORT.md               # 原版 APK 逆向分析报告（5.7 / 6.1.0）
docs/DESIGN.md               # 设计系统
```

## 已知限制

- 抖音解析依赖 WebView 加载分享页，首次约 3–8 秒；页面改版可能导致失效。
- 快手 / 小红书逻辑已实现，尚未用真实分享链接大规模验证。
- 仅支持 Android 8.0+（minSdk 26）。
