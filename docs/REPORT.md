# 橙子去水印 5.7 静态逆向拆解报告

> 目标：`com.wuhen.analyseTools`（应用名「橙子去水印」）v5.7
> 文件：`C:\Users\李福兴\Downloads\com.wuhen.analyseTools_5.7.apk`
> 方法：Craft Anything（craft CLI 0.5.0，--deep）+ JADX 1.5.6（含 `--show-bad-code`）+ 接口实测
> 结论先行：**客户端本身不做任何平台解析**，它是一个「壳 + 播放器 + 本地 FFmpeg 工具集」，所有解析能力都在 `analyse.layzz.cn` 这套服务端上；登录态（uid）是调用解析接口的前置条件。

---

## 0. 一页摘要

| 项目 | 结果 |
| --- | --- |
| 包名 / 版本 | `com.wuhen.analyseTools` / versionName 5.7 |
| 应用名 | 橙子去水印 |
| 体积 | 78,582,752 字节（约 74.9 MB） |
| SHA-256 | `a6a4d1f7640501e00f090e1af0eac9a6593e11b1d963aa56923ed60c44ee9773` |
| minSdk / targetSdk | 26 / 36 |
| 语言 / UI | Kotlin 2.2 + Jetpack Compose Material3（31,022 个类中 19,584 个 Compose 相关） |
| 媒体 | Media3 ExoPlayer（播放）+ FFmpegKit（本地转码/去水印/消音） |
| 网络 | OkHttp 4 + Gson |
| 账号 | 微信 OpenSDK 登录，服务端换 token（60 天本地有效期） |
| 统计 | 友盟 U-App（`5fa102181c520d30739f2fd1`） |
| 广告 | **无任何第三方广告 SDK**；首页 banner 由服务端接口下发 |
| 加固 | 无壳（无 libjiagu/libshell），仅资源名混淆 + 代码混淆 |
| 签名 | 仅 v2，CN=王磊，证书 SHA-256 `e945e55b4cf282d4672a2fd257fb8cf8c8301825f4c11ccb0267df899a72e98a` |
| 服务端 | `https://analyse.layzz.cn`（另有 `www.layzz.cn`、`pics.layzz.cn` 资源域） |
| 核心洞察 | 去水印 = 云端解析 SaaS；本地能力 = 播放/下载/消音/delogo 涂抹；变现 = banner + 强制更新 |
| 复刻交付 | `android/` Kotlin+Compose 工程（12 个源文件 / 1838 行），`dist/orange-nowatermark-5.7-debug.apk`（137 MB，debug 签名） |
| 复刻验证 | `:app:assembleDebug` BUILD SUCCESSFUL；aapt2/apksigner 校验通过；未在真机运行（本机无设备） |

**功能全景（7 个屏幕）**

1. 首页：banner + 四宫格（一键去水印 / 批量下载 / 去除背景音乐 / 操作教程）
2. 单条解析：粘贴分享链接 → 云端返回直链 → 预览 → 勾选保存
3. 批量下载：粘贴主页链接 → 分页拉作品 → 勾选 → 队列下载
4. 去除背景音乐：选本地视频 → FFmpeg `-an` 消音 → 保存
5. 去水印：选本地视频/图片 → 手绘矩形 → FFmpeg `delogo` → 保存
6. 视频裁剪/剪辑：自绘 SmoothTrimSlider + ExoPlayer 预览
7. 我的：微信登录、分享、客服微信、检查更新、两个开关、隐私政策、注销、退出

---

## 1. 安装包结构与身份

### 1.1 基本信息

```
package        com.wuhen.analyseTools
versionName    5.7
minSdkVersion  26 (Android 8.0)
targetSdkVersion 36 (Android 16)
usesCleartextTraffic true
```

三个 DEX，合计 **31,022 个类**：

| DEX | 类 | 方法 | 字符串 |
| --- | ---: | ---: | ---: |
| classes.dex | 17,433 | 65,510 | 91,491 |
| classes2.dex | 7,848 | 63,807 | 69,687 |
| classes3.dex | 5,741 | 47,194 | 43,595 |

### 1.2 签名

- 仅 APK Signature Scheme v2，无 v1/v3；`apksigner verify` 通过。
- 证书主体：`CN=王磊, OU=王磊`；算法 RSA 2048；证书 SHA-256 `e945e55b…a72e98a`。
- 说明：个人开发者签名，未使用 Play App Signing；重打包后**无法**复用微信开放平台登录（微信校验包名+签名）。

### 1.3 权限（11 个）

| 权限 | 用途 |
| --- | --- |
| INTERNET / ACCESS_NETWORK_STATE / ACCESS_WIFI_STATE | 联网、网络状态 |
| READ_MEDIA_VIDEO / READ_MEDIA_IMAGES / READ_MEDIA_AUDIO | Android 13+ 读取媒体 |
| READ_EXTERNAL_STORAGE / WRITE_EXTERNAL_STORAGE | 旧系统兼容 |
| REQUEST_INSTALL_PACKAGES | **应用内自更新，下载 APK 后拉起安装器** |
| DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION | AndroidX 动态广播 |
| com.google.android.gms.permission.AD_ID | 归因 ID（友盟/广告） |

无定位、通讯录、相机、录音等敏感权限。视频选择走系统 Photo Picker / SAF，不申请相机。

### 1.4 组件

12 个组件，仅 3 个 exported：

| 组件 | 类型 | exported | 说明 |
| --- | --- | --- | --- |
| `MainActivity` | Activity | ✅ (LAUNCHER) | 单 Activity 宿主，Compose 根 |
| `wxapi.WXEntryActivity` | Activity | ✅ | 微信回调固定入口 |
| `androidx.profileinstaller.ProfileInstallReceiver` | Receiver | ✅（DUMP 权限） | AndroidX 基线配置 |
| VideoAnalyse / BatchAnalyse / RemoveMusic / VideoEdit / VideoCrop / ImageCrop / Tutorial / Privacy | Activity | ❌ | 全部内部跳转 |
| `FileProvider` | Provider | ❌ | authority `com.wuhen.analyseTools.fileprovider` |
| `androidx.startup.InitializationProvider` | Provider | ❌ | 自动初始化 |

### 1.5 Native 库

`arm64-v8a` + `armeabi-v7a` 双 ABI：

- FFmpegKit：`libavcodec`、`libavformat`、`libavfilter`、`libavutil`、`libswscale`、`libswresample`、`libffmpegkit`、`libffmpegkit_abidetect`
- 友盟：`libumeng-spy`
- 其他：`libc++_shared`、`libandroidx.graphics.path`

没有 `libjiagu`、`libshell`、`libDexHelper` 等加固特征；DEX 未加密，可直接反编译（事实已验证）。

### 1.6 资源

`res` 下 328 个文件，**资源名被混淆**（如 `res/-6.webp`、`res/AI.png`），但字符串默认表只有库自带英文文案；**App 的中文文案全部硬编码在 Compose 代码里**（见 §6）。

`assets` 仅有 2 个条目，无离线教程 HTML——教程页内容是代码里的 `trimIndent()` HTML 字符串，通过 `WebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)` 渲染。

---

## 2. 技术栈与依赖构成

对 31,022 个类做包名统计后的指纹：

| 依赖 | 类数（近似） | 用途 |
| --- | ---: | --- |
| Kotlin 标准库 + 协程（SuspendLambda） | 大量 | 全异步 |
| Jetpack Compose（material3/foundation/runtime） | 19,584 | 全部 UI |
| AndroidX Media3 ExoPlayer | 2,184 | 视频预览 |
| Coil | 334 | 图片/封面加载 |
| OkHttp | 319 | 网络 |
| Gson | 217 | JSON（kotlinx.serialization 也在，仅少量） |
| Room | — | 本地数据库（未见业务使用，可能是传递依赖） |
| 友盟 `com.umeng.commonsdk` / `analytics` | 489 | 统计、渠道 |
| 微信 SDK `com.tencent.mm` | 186 | 登录/分享 |
| FFmpegKit | native | 本地音视频处理 |

**没有**：Pangle、优量汇 GDT、快手、Mintegral、Sigmob、AppLovin、Facebook Audience Network、Google Mobile Ads 等广告 SDK。

> 结论：广告不是 SDK 聚合，而是「服务端下发 banner 图 + 点击跳转链接」的极简实现，规避了广告平台合规与接入成本。

---

## 3. 架构总览

```
┌──────────────────────────── Compose UI ────────────────────────────┐
│ MainActivity(Scaffold + 底部 tab)                                   │
│   ├─ HomeScreen        四宫格 + banner                             │
│   ├─ VideoAnalyse      POST /lyz/v1ApkAnalyse                      │
│   ├─ BatchAnalyse      POST /lyz/v1BatchAnalyse (分页)             │
│   ├─ RemoveMusic       FFmpegKit -an                               │
│   ├─ VideoEdit         FFmpegKit delogo                            │
│   ├─ VideoCrop         ExoPlayer + 自绘 trim slider                │
│   ├─ Tutorial/Privacy  WebView.loadDataWithBaseURL                 │
│   └─ PersonalCenter    微信登录 / 分享 / 客服 / 更新 / 设置         │
├──────────────────────────── 状态层 ────────────────────────────────┤
│ AppState: globalUserData / globalVersionData (Compose MutableState)│
│ DataManager: SharedPreferences (user_prefs / privacy_prefs / home_cache) │
├──────────────────────────── 网络层 ────────────────────────────────┤
│ NetworkConfig.client = OkHttpClient(connect 15s, read 15s)         │
│ 请求体手工拼 JSON；签名在客户端计算（MD5）                          │
└────────────────────────────────────────────────────────────────────┘
                                │
                    https://analyse.layzz.cn
        ┌───────────────┬───────────────┬──────────────┬───────────┐
        │ /lyz/v1ApkAnalyse │ /lyz/v1BatchAnalyse │ /login/wxLogin │ /advert/* │ /app/getVersionInfo │
        └───────────────┴───────────────┴──────────────┴───────────┘
```

设计特点：

1. **极薄客户端**：没有平台适配器、没有 WebView 抓取、没有签名算法逆向；解析逻辑完全黑盒在服务端。
2. **单 Activity + Compose**：除 9 个壳 Activity 外全部 UI 在一个 Activity 内导航。
3. **请求签名是"防小白"级别**：MD5 拼接固定盐，盐硬编码在 DEX 里，可被任意伪造。
4. **无本地缓存业务数据**：只缓存 banner、用户、隐私状态；解析结果不落库。

---

## 4. 网络协议（重点）

所有请求走 `https://analyse.layzz.cn`，`OkHttpClient` 未设置证书固定（no CertificatePinner），未自定义 UA。

### 4.1 单条解析 `POST /lyz/v1ApkAnalyse`

代码位置：`badcode/VideoAnalyseActivityKt.java`（`executeFixedAnalyse`，约 1734 行构造 body）

请求体（字段顺序即 Gson `mapOf` 顺序）：

```json
{
  "link": "https://v.douyin.com/xxxx/",
  "r": "b1c2...-uuid",
  "take": "md5(r + \"analyse_tool#encode\")",
  "uid": "用户uid，游客为空串"
}
```

- `link`：`UtilsKt.extractUrlFromText(输入)`，从整段分享文案里提取第一个 URL（支持"复制打开抖音…"这种带文案的分享）。
- `r`：`UUID.randomUUID().toString()`，每次请求随机。
- `take`：`EncodeUtil.getSingleTake(r)` = `MD5(r + "analyse_tool#encode")` 的 32 位小写 hex；实现用 `BigInteger(1, digest).toString(16)` 再左补 0 到 32 位。
- `uid`：`AppState.globalUserData?.uid ?: ""`。

响应模型：

```kotlin
AnalyseResponse(code: String, message: String, data: AnalyseData?)
AnalyseData(
  playAddr: String,      // 无水印视频直链
  cover: String,         // 封面
  desc: String,          // 文案/标题
  type: Int?,            // 内容类型
  pics: List<String>,    // 图集
  music: String,         // 背景音乐直链
  videos: List<String>,  // 多段视频
  size: Int?,            // 文件大小（字节）
  isSelected: Boolean    // 客户端本地勾选状态
)
```

**实测（2026-11-08）**：

- 服务端在线，成功响应 `code` 为 `"0001"`（版本/广告接口同样规范）。
- 用 `uid=""` 请求单条解析，返回：

```json
{"code":"0002","method":"/error","message":"uid不可为空"}
```

  → **游客不能解析**，必须先微信登录（与 UI 的 `showLoginDialog` 对应）。

### 4.2 批量解析 `POST /lyz/v1BatchAnalyse`

代码位置：`badcode/BatchAnalyseActivityKt.java:2152-2221`

```json
{
  "uid": "xxx",
  "link": "主页链接（同样先 extractUrlFromText）",
  "sign": "md5(uid + link + \"v1-batch#analyse\")",
  "paging": "上一页返回的 paging 游标；首页为空串"
}
```

响应：

```kotlin
BatchAnalyseResponse(code, message, data: BatchData?)
BatchData(videoList: List<AnalyseData>?, paging: String?)
```

分页语义（从 UI 状态推断）：首屏 `paging=""`，把返回的 `data.paging` 存下来作为下一页入参；`isEnd` 由「返回列表为空 / paging 为空」判定。UI 用「获取作品列表」「全选」「下载勾选(n)」「当前进度：x/y」。

### 4.3 微信登录 `GET /login/wxLogin`

代码位置：`badcode/AuthManager.java:92` 附近

```
GET https://analyse.layzz.cn/login/wxLogin?code={微信授权code}&loginType=2
Header: appType: 2
```

响应：`LoginResponse{code, message, data: UserData{uid, headUrl, userName, token}}`

流程：

1. `MainActivity` 创建 `WXAPIFactory.createWXAPI(this, "wx015d5adc36f9e08a", true)`，隐私同意后才 `registerApp`。
2. `SendAuth.Req`（scope `snsapi_userinfo`）拉起微信，回调进 `wxapi.WXEntryActivity`。
3. 拿到 `code` 调上述接口换 `uid/token`，存 `SharedPreferences("user_prefs")`。
4. 本地有效期 **60 天**：`EXPIRATION_TIME = 5184000000L`，超时自动清除登录态。
5. 登录信息同时写入 `AppState.globalUserData`（Compose 全局状态）。

### 4.4 广告 `GET /advert/getAdvertMsg?targetId=6658`

代码位置：`badcode/MainActivityInner.java`（onCreate 内 IO 协程）

响应（实测，节选）：

```json
{
  "code": "0001",
  "data": {
    "bannerList": [
      {"id": 2347, "imgUrl": "https://p26-tt.byteimg.com/...jpg", "targetId": 6658, "sort": 6658, "type": 0}
    ],
    "shareTitle": "短视频永久免费 一键去水印 快来试试吧",
    "sharePic": "http://parse-video-server.oss-cn-hangzhou.aliyuncs.com/share.png",
    "author": "微信: Lany4567",
    "times": 3, "watchTimes": 1,
    "adTip": "还没有看完广告 一天仅需看一次广告 24小时免除广告",
    "skipList": [], "batchAnalyse": 0, "freeType": 0
  }
}
```

客户端只用了 `bannerList[].imgUrl`，缓存到 `SharedPreferences("home_cache")` 的 `banner_json`。

响应里还包含激励视频/插屏字段（`videoAd`、`parseAd`、`prizeType`…），但 5.7 客户端**没有实现**对应 UI——服务端是通用 SaaS 后台，客户端只取了 banner 子集。

### 4.5 版本 `GET /app/getVersionInfo?source=2`

实测响应：

```json
{"code":"0001","data":{
  "appId":2209,"version":"6.0.1","verNum":215,
  "content":"1、增加动图下载\r\n2、优化版本稳定性\r\n3、优化下载逻辑\r\n4、新版本APP永久免费 请放心升级",
  "packageUrl":"https://pics.layzz.cn/jar/app-release.apk",
  "concatMsg":"Lanyu4567","freeType":0,"showAd":1,
  "banner":"http://img.layzz.cn/xc.jpg","bannerId":6658,
  "bannerLink":"http://img.layzz.cn/xc.jpg","notice":null,"batchAnalyse":0}}
```

逻辑：

- `AppState.globalVersionData` 非空且 `version != BuildConfig.VERSION_NAME` → 弹 `UpdateDialog`。
- 文案：`发现新版本 V{version}`、`优化了使用体验`、`立即更新`、`正在下载`、`下载进度: n%`、`立即安装`、`下载失败，尝试浏览器下载`。
- 下载：`MainActivityKt.downloadApkFile` 用 `DownloadManager` 下到外部目录，完成后通过 `FileProvider` + `ACTION_VIEW` 拉起安装器（依赖 `REQUEST_INSTALL_PACKAGES`）。
- `concatMsg` 即「联系客服」显示的微信号（实测 `Lanyu4567`），点击复制，Toast「已复制客服微信」。
- `source=2` 是渠道号；`targetId=6658` 是后台产品位 ID。

### 4.6 其它

- 旧常量 `/lyz/apiAnalyse`、`/lyz/batchAnalyseByPage` 仍在 `NetworkConfig` 里，但**没有任何调用**（历史遗留）。
- 未设置 `Authorization`；解析请求只带 `uid`，不带登录响应里的 `token`——鉴权很弱。
- 明文流量开关 `usesCleartextTraffic=true`，但实际解析请求均为 https；banner 图 `http://img.layzz.cn` 为明文。

---

## 5. 本地媒体处理（FFmpegKit）

`VideoProcessor.java` 封装了三条命令，统一 `FFmpegKit.executeAsync`，日志 tag `FFmpeg`：

| 能力 | 命令 | 说明 |
| --- | --- | --- |
| 去背景音乐 | `-i in.mp4 -an -vcodec copy out.mp4` | 无重编码，秒级 |
| 提取音频 | `-i in.mp4 -vn -acodec libmp3lame -q:a 2 out.mp3` | 导出 MP3 |
| 多区域去水印 | `-i in.mp4 -vf "delogo=x=..:y=..:w=..:h=..,delogo=..." out.mp4` | 多个矩形用逗号连接；`delogo` 用周边像素插值填充 |

- 输入：`FileUtils` 把 `content://` Uri 复制到 `externalCacheDir/ffmpeg_input_<时间戳>.mp4`，避免 SAF 权限丢失。
- 输出：`externalCacheDir` → 通过 MediaStore 写入相册（`视频已保存至相册`）。
- 失败文案：「保存失败，请检查权限」「未获得存储授权，无法保存视频」。
- `HandleType` 枚举（NONE / TOP_LEFT / BOTTOM_RIGHT / CENTER）用于裁剪页手柄命中测试。

注意：`delogo` 是"插值涂抹"而非 AI 修复，画面会有明显糊痕；这也是这类工具的通病。

---

## 6. 逐屏拆解

### 6.1 首页 HomeScreen

- 顶部：应用名/标题区（M3 TopAppBar 风格）。
- Banner：`AsyncImage`（Coil）加载 `bannerList` 第一张，点击打开 `banner.link`。
- 「核心服务」四宫格（2×2 `Card`）：
  - 一键去水印 / 图片视频去水印
  - 批量下载 / 下载多个视频
  - 去除背景音乐 / 无损去除
  - 操作教程 / 新手必看
- 卡片配色：四种不同 `containerColor`，圆角 `RoundedCornerShape`，图标来自 `material-icons-extended`（`menuBook` 等）。
- 点击「操作教程」→ `TutorialActivity`（无参数）；其余 → 对应 Activity。
- 底部导航：首页 / 我的 两个 tab（`selectedTabState`）。

### 6.2 单条解析 VideoAnalyse

- 输入框：`OutlinedTextField` 多行，placeholder「请输入或粘贴作品链接」，支持粘贴板自动识别（「自动识别剪贴板链接」开关）。
- 按钮「立即开始解析」：空/无 URL → Toast「链接格式不正确」；否则调接口，转圈。
- 成功后：
  - 封面 `cover`（Coil）+ 标题 `desc`
  - 视频：Media3 `ExoPlayer` 播放 `playAddr`
  - 图集：`pics` 横向 `LazyRow`，可多选
  - 多段视频：`videos` 列表
  - 音频：`music` 试听/下载
  - Tab「文案 / 链接」：显示 `desc` 与直链，可复制
  - 底部「保存选中内容 (n)」：逐项 `downloadWithProgress` 到相册
- 错误：`code != "0001"` → Toast(message) 或「网络异常」。
- 未登录：弹 `showLoginDialog`（跳微信授权）。

### 6.3 批量下载 BatchAnalyse

- 输入主页链接 + 「获取作品列表」。
- 列表项 `BatchVideoCell`：封面、标题（空则「未命名」）、Checkbox。
- 每项菜单：复制链接、复制标题、解析链接、文案。
- 顶部工具栏：全选、下载勾选(n)。
- 下载队列：串行 `downloadInQueue`，显示「当前进度：x/y」与单文件进度；失败「连接失败」；完成「成功」。
- 自动下载开关：解析成功后自动下载（我的页设置）。

### 6.4 去除背景音乐 RemoveMusic

- 系统选择器选视频 → 预览（ExoPlayer）。
- 「正在移除背景音...」进度 → FFmpeg `-an -vcodec copy`。
- 完成「处理成功，请播放预览」；「视频已保存至相册」。
- 另一入口是「提取音频」导出 MP3（命令存在）。

### 6.5 去水印 VideoEdit / ImageCrop

- 选视频/图片 → 全屏预览。
- 手指拖拽画矩形（支持多个），四角手柄 `HandleType` 缩放；矩形坐标按预览控件尺寸换算到视频原始分辨率后传给 delogo。
- 保存：FFmpeg 多 `delogo` 链式滤镜，进度条，写入相册。
- 首页有「系统暂不支持裁剪」的兜底文案。

### 6.6 裁剪/剪辑 VideoCrop

- 自绘 `SmoothTrimSlider`：双滑块选择起止时间，带时间气泡、最小片段限制；`VideoPreviewPlayer`（ExoPlayer）实时 seek 预览。
- 导出走 FFmpeg `-ss/-to -c copy`（推断）。

### 6.7 教程 Tutorial

- 无网络请求；HTML 字符串硬编码在 `TutorialActivityKt`，`trimIndent()` 后 `loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)`。
- 内容为图文步骤（因字符串超长，本次未逐字提取）。

### 6.8 我的 PersonalCenter

- 顶部用户卡：未登录「立即登录 / 登录后同步数据」；已登录头像（`headUrl`）、昵称（`userName`）、管理员标识。
- 功能列表：
  - 分享给好友：`Intent.ACTION_SEND`，文案「推荐你一个超好用的去水印神器：橙子去水印！支持视频、图片一键无痕处理。\n下载地址：{packageUrl}」。
  - 联系客服: {concatMsg}（点击复制，Toast「已复制客服微信」）。
  - 检查更新：手动调版本接口，最新则「当前已是最新版本」。
  - 开关：解析成功后自动下载 / 自动识别剪贴板链接。
  - 隐私政策（`PrivacyActivity`，内置 HTML）。
  - 注销账号：弹窗「注销请联系作者：{微信}」，不真正注销（无删除接口）。
  - 退出账号：清 `user_prefs` + `AppState`，Toast「已退出登录」。

### 6.9 隐私与更新弹窗

- 首启（`privacy_prefs.agreed=false`）：展示隐私政策摘要 + WebView 全文，按钮「同意 / 不同意」；不同意则退出。
- **同意前不初始化微信 SDK、不初始化友盟、不发广告/版本请求**（代码判断 `isPrivacyAgreed`），这是相对规范的做法。
- 隐私 HTML 中披露：微信 OpenSDK（深圳市腾讯计算机系统有限公司）；客服邮箱 `yyzzwl@163.com`；客服电话 `15692099555`。
- 更新弹窗：显示版本号与更新说明，下载进度，安装。

---

## 7. 数据与状态管理

| 存储 | key | 内容 |
| --- | --- | --- |
| SharedPreferences `user_prefs` | `user_json` | `UserData` 的 Gson 串 |
| | `login_timestamp` | 登录时间戳，用于 60 天过期 |
| `privacy_prefs` | `agreed` | 隐私是否同意 |
| `home_cache` | `banner_json` | banner 列表缓存 |

`DataManager` 对 Gson 解析失败做了兜底：异常即清空对应 key，避免脏数据崩溃。

`AppState`：

```kotlin
object AppState {
  var globalUserData by mutableStateOf<UserData?>(null)
  var globalVersionData by mutableStateOf<VersionData?>(null)
}
```

Compose 直接读，登录/更新全局刷新。

---

## 8. 服务端画像（推断）

| 路径 | 作用 | 观察 |
| --- | --- | --- |
| `/lyz/v1ApkAnalyse` | 单条解析 | 需 uid；返回统一 `code/message/data` |
| `/lyz/v1BatchAnalyse` | 主页作品分页 | 需 uid；`paging` 游标 |
| `/login/wxLogin` | 微信 code 换 token | `appType:2` 区分客户端 |
| `/advert/getAdvertMsg` | banner/分享文案/客服微信 | `targetId=6658` 产品位；返回大量未使用字段 |
| `/app/getVersionInfo` | 版本/公告/客服 | `source=2` 渠道；`packageUrl` 指向 `pics.layzz.cn` |
| `/error` | 统一错误 method 字段 | 错误响应带 `method:"/error"` |

推断：

- 这是**一套多租户短视频解析 SaaS**（`targetId`、`appId`、`source`、`freeType`、`showAd` 等都是后台配置项），橙子去水印只是其中一个 `targetId=6658` 的产品位。
- 解析能力（抖音/快手/小红书/B站/微博/视频号…）在服务端，客户端文案里不出现平台名，说明**平台支持范围可随时后端扩展**。
- 返回字段 `type`、`pics`、`videos`、`music`、`size` 表明支持：单视频、图集、多段、带音乐，覆盖主流平台内容形态。
- 客服微信与版本公告都由后台下发，**不发版即可换客服/换公告**。

---

## 9. 安全与合规观察

1. **签名算法硬编码**：盐 `analyse_tool#encode`、`v1-batch#analyse` 在 DEX 中明文；任何人都能伪造合法请求。真正的门槛是 uid（微信登录态）。
2. **鉴权薄弱**：解析请求只带 `uid`，不带登录响应里的 `token`；知道他人 uid 即可冒用。
3. **无证书固定**：OkHttp 未配置 `CertificatePinner`，抓包/中间人可读。
4. **明文开关**：`usesCleartextTraffic=true`，banner 图 `http://img.layzz.cn` 为明文，存在被替换风险（仅图片）。
5. **自更新风险**：`REQUEST_INSTALL_PACKAGES` + 服务端 `packageUrl` 可下发任意 APK；未做签名校验。
6. **隐私合规**：同意前不初始化 SDK，做得较好；但「注销账号」只弹联系方式、不真正删号，不符合"便捷注销"要求。
7. **无加固**：代码可完整反编译，接口与签名可复现（本报告即证明）。
8. **资源混淆**：仅资源名混淆，对抗静态分析作用有限。

---

## 10. 复刻实现与交付物（已完成）

### 10.1 复刻边界（诚实说明）

- **已复刻**：全部 UI 结构/文案/交互、请求协议与 MD5 签名、登录态存储（60 天过期）、广告/版本/客服逻辑、本地 FFmpeg 消音与 delogo 涂抹、下载与相册保存、权限与隐私流程。
- **无法凭空复刻**：云端解析能力（各平台私有 API/风控）。原版客户端把链接交给作者 SaaS（`analyse.layzz.cn`），且服务端要求 `uid`（来自微信登录）；微信登录绑定原包名 `com.wuhen.analyseTools` + 原签名证书，重打包无法通过微信校验。
- **因此复刻版**：未登录时用内置演示数据走通完整 UI；登录提供「演示登录」（uid=demo-uid），不冒充真实微信授权；要真实解析，需自建服务端（见 10.5）。

### 10.2 Android 工程（实际文件，12 个源文件 / 1838 行）

```
android/
  settings.gradle.kts / build.gradle.kts / gradle.properties
  app/build.gradle.kts
  app/src/main/AndroidManifest.xml
  app/src/main/java/com/orange/nowatermark/
    MainActivity.kt          # Screen 枚举 + Scaffold/NavigationBar + 隐私/更新弹窗 + 首页数据加载
    data/Models.kt           # AnalyseData / UserData / VersionData / BannerItem / 批量响应
    data/Sign.kt             # md5 / singleTake(r) / batchSign(uid, link)
    data/Store.kt            # AppState（Compose 全局状态）+ DataManager（4 组 SharedPreferences）
    net/Api.kt               # OkHttp 15s；v1ApkAnalyse / v1BatchAnalyse / getVersionInfo / getAdvertMsg
    media/MediaTools.kt      # copyToCache、时长/宽高、runFfmpeg、保存到相册（MediaStore）
    ui/HomeScreen.kt         # 首页四宫格 + banner + Tutorial/Privacy(WebView) + 两个弹窗
    ui/AnalyseScreen.kt      # 单条解析：剪贴板、结果卡、图集、ExoPlayer、复制/保存
    ui/BatchScreen.kt        # 批量：作品列表、勾选、全选、队列下载
    ui/ToolsScreen.kt        # 消音 / 去水印(delogo 框选) / 裁剪(双滑块)
    ui/ProfileScreen.kt      # 我的：登录、分享、客服、检查更新、开关、隐私/注销/退出
    ui/theme/Theme.kt        # M3 紫色主题 + Android 12 动态取色
  app/src/main/res/…         # strings/themes/colors/图标（自适应）
  app/src/main/assets/       # privacy.html、tutorial.html
```

### 10.3 构建环境与结果

- 环境：JDK 17（`D:/Java17`）、Gradle 8.9（解压版）、SDK 手工装配于 `default-workspace/android-sdk`（platform-tools、build-tools 35.0.0、platforms/android-35；`sdkmanager` 因网络代理取不到 manifest，改为直接下载 zip 解压）。
- 关键适配：项目路径含中文 → `gradle.properties` 增加 `android.overridePathCheck=true`；用户目录不可写 → `ANDROID_USER_HOME`、`GRADLE_USER_HOME` 指向工作区；`buildToolsVersion = "35.0.0"` 并写入 `android-sdk/licenses/android-sdk-license`。
- 构建命令（工作区根目录为 BASE）：
  ```bash
  export ANDROID_HOME="$BASE/android-sdk" ANDROID_USER_HOME="$BASE/.android-home" \
         GRADLE_USER_HOME="$BASE/.gradle-home" JAVA_HOME="D:/Java17"
  cd projects/wuhen-analysetools-5.7/android
  ../../../gradle-8.9/bin/gradle --no-daemon :app:assembleDebug
  ```
- 结果：`BUILD SUCCESSFUL`；产物 `app/build/outputs/apk/debug/app-debug.apk`，复制为 **`dist/orange-nowatermark-5.7-debug.apk`**。
  - 大小 137 MB（ffmpeg-kit-full-gpl 含 arm64-v8a + armeabi-v7a 全量编解码库）
  - SHA-256 `3b1f56c0be20173ea7d5fdb417e29606b6e0b4cd5f860b64cd5851064483b85b`
  - `aapt2 dump badging`：package `com.orange.nowatermark`，versionCode 570 / versionName 5.7，minSdk 26 / targetSdk 35，label「橙子去水印」
  - `apksigner verify`：Android Debug 证书（v2 签名，可直接安装；非商店签名）
- 已知构建噪音：aapt2 对含中文的绝对路径打印 "Failed to stat android.jar" 警告但不影响产物；ffmpeg .so 无法 strip（正常）。

### 10.4 复刻对照

| 维度 | 原版 | 复刻版 |
| --- | --- | --- |
| 包名 | com.wuhen.analyseTools | com.orange.nowatermark（避免冒充） |
| UI | Compose M3，首页四宫格 + 底部两 tab | 同（M3 紫色主题 + 动态取色） |
| 单条解析 | POST /lyz/v1ApkAnalyse + take 签名 | 完全一致字段与签名；uid 为空/演示时走演示数据 |
| 批量 | POST /lyz/v1BatchAnalyse + sign | 一致；列表/勾选/队列下载 |
| 广告/版本 | getAdvertMsg?targetId=6658、getVersionInfo?source=2 | 一致，含 banner 缓存与更新弹窗 |
| 本地媒体 | FFmpegKit（-an copy / delogo / trim） | 一致命令；保存到 Movies/橙子去水印 |
| 登录 | 微信 OpenSDK | 演示登录（微信需原包名+签名，见 10.1） |
| 其他 | 友盟统计、更新下载安装 | 未接（隐私最小化；更新仅提示） |

### 10.5 若要真正跑通解析（自建服务端）

- 单条：分享短链 → 重定向 → 提取作品 ID → 调平台接口 → 返回与 `AnalyseResponse` 同构 JSON，前端零改动。
- 批量：主页作品分页（游标/页码）→ `BatchAnalyseResponse{data:{videoList,paging}}`。
- 登录：自建账号体系替换 `/login/wxLogin`，下发 uid/token。
- 必须：签名校验（`take`/`sign`）、token 鉴权、限流、内容合规。

### 10.6 未完成/未验证

- 未在真机/模拟器安装运行（本机无 emulator 镜像与 adb 设备），仅完成编译与静态校验。
- 「我的」页检查更新只提示版本、不实现 APK 下载安装；裁剪页用双滑块替代原版自绘 SmoothTrimSlider；水印框选按预览尺寸比例换算 delogo 参数（与原文思路一致，但未逐像素验证）。
- 微信登录、注销账号后台流程未实现（原版注销也只是提示联系作者）。

---

## 11. 未知项与后续验证建议

| 未知 | 原因 | 验证方法 |
| --- | --- | --- |
| 解析成功响应真实字段值 | 需微信登录 uid | 真机登录后抓包；或向作者申请测试 uid |
| `type` 枚举含义 | 无样本 | 分别解析视频/图集 |
| 批量 `paging` 具体格式 | 无样本 | 登录后抓两次分页请求 |
| 教程 HTML 全文 | 超长字符串未逐字提取 | 反编译 `TutorialActivityKt` 的 `trimIndent` 常量 |
| 解析支持的平台清单 | 服务端能力 | 逐一提交各平台分享链接 |
| 更新是否强制 | 5.7 已落后（服务端 6.0.1） | 真机启动观察弹窗能否关闭 |

---

## 12. 附录：关键文件与代码位置

| 内容 | 位置 |
| --- | --- |
| 接口常量 | `jadx-src/sources/com/wuhen/analyseTools/NetworkConfig.java` |
| 单条解析请求体 | `badcode/VideoAnalyseActivityKt.java`（约 1734 行） |
| 批量请求体与签名 | `badcode/BatchAnalyseActivityKt.java:2152,2160,2221` |
| 微信登录 | `badcode/AuthManager.java`（约 92 行） |
| FFmpeg 命令 | `jadx-src/.../VideoProcessor.java` |
| 用户/隐私/缓存 | `jadx-src/.../DataManager.java` |
| 全局状态 | `jadx-src/.../AppState.java` |
| 隐私 HTML（含联系方式） | `jadx-src/.../PrivacyActivity.java` |
| UI 文案表 | `ui_strings.txt` |
| 主题色 | `ui_theme.txt` |
| 反编译源码 | `jadx-src/`（19,910 个 .java，376 MB） |
| 类清单 | `all_classes.txt`、`classes_wuhen*.txt` |
| 完整清单/Manifest | `pkg.json` |

### 复刻所需的三个公式

```text
take  = md5( r + "analyse_tool#encode" )        // 单条解析
sign  = md5( uid + link + "v1-batch#analyse" )  // 批量解析
token 有效期 = 5184000000 ms = 60 天             // 本地登录态
```

---

*报告基于 5.7 安装包静态分析与 2026-11-08 的接口实测；服务端随时可能变更，签名与字段以抓包为准。*

---

# 附录 B：6.1.0 增量对比（2026-11 实测）

手机端（vivo V2505A）上最新版为 **6.1.0 / versionCode 220**（服务端版本接口仍返回 6.0.1/215），
APK 66,975,240 字节，sha256 `1dc433898f03cc168c4ec00893f9455b34c4983cb79ecb594dded78318d04bd1`，
仅 arm64-v8a，compileSdk/targetSdk 36。与 5.7 相比：

## B1. 客户端结构
- 代码经 R8 混淆为 `y3/*`、`z4/*` 等短包，`com.wuhen.analyseTools` 只剩 27 个类（5.7 为 93）。
- 新增 Activity：`VideoWatermarkActivity`、`SmartImageWatermarkActivity`、`ExtractAudioActivity`、`AudioConverterActivity`；`ImageCropActivity` 下线。
- 新增权限：VIBRATE、WAKE_LOCK、CHANGE_NETWORK_STATE、CHANGE_WIFI_STATE、BIND_JOB_SERVICE、TT_PANGOLIN。
- 接入穿山甲（com.byazt.* / com.byted.live.lite.*，`pangle.fileprovider`、`TTMultiProvider`），
  `TTAdSdk.init` 使用 appId `5162825`、appName「一键无痕去水印」，仅隐私同意后初始化。
- 新增 native：libPglbizssdk_ml、libpanglearmor、libpangleflipped、libttmplayer_lite、libtobEmbedEncrypt、
  libtt_ugen_layout、libavmdl_lite、libopencv_java4（23MB）。

## B2. 协议变化
- 5 个端点、body、签名算法全部不变：`take = MD5(r + "analyse_tool#encode")`、
  `sign = %032x MD5(uid + link + "v1-batch#analyse")`。
- **新增鉴权头**：单条与批量在 token 非空时带 `Authorization: <token>`；服务端可选择校验。
- 模型：`AdData` 增加 `ads: AdsConfig`（indexAd/videoAd/videoAdReward/videoAdCard/toolAd/myAd/
  videoAdAfter/parseAd/parseAdCard/batchAd）；`VersionData` 删除 showAd/banner/bannerId/bannerLink。
  线上 `/advert/getAdvertMsg` 实测 ads.id=541、targetId=6658，多个广告位共用代码位 `887469077`。

## B3. 去水印算法升级（视频/图片）
5.7 只是 FFmpeg `delogo` 矩形模糊；6.1.0 改为 **OpenCV inpaint 逐帧修补**：
1. `OpenCVLoader.initDebug()`，选区从预览坐标缩放到视频真实分辨率，外扩 30px 得 ROI；
2. ROI 内做 mask：黑底白框 → 灰度 → dilate 9×9 → GaussianBlur 5×5(σ=2) → threshold 128；
3. FFmpeg 以 `fps=30` 抽帧（超过 3000 帧拒绝处理）；
4. 每帧 `Photo.inpaint(Telea, radius)`，radius 按选区最大边取 3/6/9/12；
5. 时间维平滑：每 2 帧与上一结果 0.7/0.3 混合，再 0.85 结果 + 0.15 原帧；
6. 音频 `-vn -c:a copy` 单独抽出，最后 `-c:v mpeg4 -q:v 2 -pix_fmt yuv420p` 重新封装，按旋转角加 transpose。

## B4. 复刻工程同步项
- `versionName 6.1.0 / versionCode 610`，保存目录改为 `Movies|Music/AnalyseTools` 语义（本复刻用「橙子去水印」）。
- `AdData` 增加 `AdsConfig`；接口层在登录后附带 `Authorization` 头。
- 新增「提取音频」（`-vn -acodec libmp3lame -q:a 2`）与「音频格式转换」（`-c:a aac -b:a 192k`）两个工具页。
- 未复刻项：穿山甲 SDK、OpenCV inpaint 视频去水印（体积与授权原因），仍保留 5.7 的 delogo 方案，差异已在 §B3 说明。


## 附录 C：端到端联调记录（2026-11-08）

### C1 服务端
- 启动：`vendor` 离线依赖 + `python run.py`，监听 `0.0.0.0:8000`；`/health` 返回 `{"ok":true,"platforms":["douyin","kuaishou","xiaohongshu","bilibili","generic"]}`。
- 冒烟：`tests/smoke.py` 8/8 通过（health / platforms / version / advert / demo login / bad signature / bilibili parse / batch bad sign）。
- SQLite：`orange.db`，含 users、parse_logs、cache、settings。

### C2 真机
- 设备：vivo V2505A（10AG5S0AX100843），Android 15。
- 安装：`adb push` 到 `/data/local/tmp` 后 `pm install`（直接 `adb install` 会被系统安装确认弹窗拒绝）。
- 端口：`adb reverse tcp:8000 tcp:8000`，手机内 `http://127.0.0.1:8000` 即本机服务端。

### C3 客户端到服务端链路
1. 「我的」→「解析服务：线上 analyse.layzz.cn」点击切换为「本地 http://127.0.0.1:8000」。
2. 退出账号 → 立即登录：客户端 `GET /login/wxLogin?code=demo_<ts>&loginType=2`（Header `appType: 2`），服务端创建 `demo_*` 用户并返回 uid/token；`users` 表新增记录。
3. 首页 → 一键去水印 → 粘贴 `https://www.bilibili.com/video/BV1GJ411x7h7` → 立即开始解析：客户端 POST `/lyz/v1ApkAnalyse`（link/r/take/uid）→ 服务端 bilibili 适配器解析 734ms → 返回 playAddr（经 `/media` HMAC 代理）、标题、封面、时长与分P。
4. 客户端结果卡渲染标题「【官方 MV】Never Gonna Give You Up - Rick Astley」、封面与播放器。`parse_logs` 记录 `demo_* / bilibili / ok / 0001 / 734ms`。

### C4 已知限制
- 16 KB 对齐警告：ffmpeg-kit 6.0-2 预编译 `libav*.so` 为 4 KB 段对齐，Android 15 首次启动弹系统提示，可「不再显示」，不影响运行；要彻底消除需自行以 16 KB 对齐重编 FFmpeg 或换用系统 MediaCodec 方案。
- 签名：debug keystore（`tmp/debug.keystore`），非生产签名。
- 微信登录：无 AppSecret 时走演示登录；配置 `WX_SECRET` 后走微信 `jscode2session`。
- 批量解析：仅抖音主页（secUid 游标翻页）；其他平台返回「需要登录态」。
- 广告：未接入穿山甲 SDK，仅还原服务端 banner 与 `ads` 配置下发的数据通路。

## 附录 D：抖音解析——Argus 风控与浏览器 worker（本轮新增）

- 现象：`/aweme/v1/web/aweme/detail` 返回 `Blocked by ArgusSecurityPlugin Uifid Not Found`；
  随机 uifid → `Signature Not Found`；真实 a_bogus/X-Bogus/ttwid/msToken 均无法通过。
- 原因：签名的输入包含真实浏览器生成的 UIFID，纯 HTTP 客户端无法伪造。
- 方案：浏览器 worker 打开 `m.douyin.com/share/video/{id}`，读取 `window._ROUTER_DATA` 中的
  `videoInfoRes.item_list[0]`（含 `video.play_addr`，`playwm` 替换为 `play` 即无水印），
  回灌 `POST /worker/douyin`，服务端缓存后按正常协议返回。
- 落地：`server/tools/douyin_worker.py`（adb + CDP 编排）、`tools/douyin_worker.mjs`（CDP 抓取与回灌）、
  App 端新增 `WebFetchActivity`（WebView 开启远程调试）。实测：worker 回灌后
  `/lyz/v1ApkAnalyse` 返回 `code 0001` 与可播放的无水印直链。
- 生产化：worker 换成服务器上的 Playwright/Chromium，流程不变。
- 自动化：`douyin_auto_worker=True` 时服务端在缓存未命中后自动拉起 worker（首次解析约 9 秒返回「正在抓取」提示，重试即命中）。

## 附录 E：客户端本地解析（抖音，不依赖服务端）

- 入口：「一键去水印」页「本地解析」开关（默认开启），仅抖音走本地。
- 链路：短链 OkHttp 跟随 → 隐藏 WebView 打开 `m.douyin.com/share/video/{id}`（真实 SecSDK 环境）
  → `Runtime` 不可用，改用 `evaluateJavascript` 轮询 `window._ROUTER_DATA`
  → 取 `videoInfoRes.item_list[0]` → `playwm`→`play` → 播放/下载。
- 关键坑：`evaluateJavascript` 返回值是 JSON 字符串（会被再编码一层），需二次 `JsonParser`；
  Gson 的 `getAsJsonArray` 遇到 `JsonNull` 会抛 `ClassCastException`，统一用类型判断的安全访问器。
- 实测：服务端关闭、`adb reverse` 移除时仍解析成功（视频 01:11、文案完整）。
- 限制：仅抖音；其它平台继续走服务端，或按相同模式扩展。

## 附录 F：v1.00 纯本地重构（2026-11-08）

- 版本：versionCode 100 / versionName "1.00"；应用名改为「短视频去水印」。
- 删除：`server/`（FastAPI、适配器、worker、vendor）、`net/Api.kt`、`data/Sign.kt`、`WebFetchActivity`，以及登录/广告/版本检查/解析服务切换等所有联网业务。
- 保留并新增本地能力（`parser/LocalParser.kt`）：
  - 抖音：隐藏 WebView 读 `window._ROUTER_DATA`（已验证）；
  - B 站：直连 `x/web-interface/view` + `x/player/playurl`，播放/下载带 Referer（已验证）；
  - 快手：`window.__APOLLO_STATE__` + og:video 兜底（已实现，待真实链接验证）；
  - 小红书：`window.__INITIAL_STATE__.note.noteDetailMap`（已实现，待真实链接验证）。
- 播放器：ExoPlayer 通过 `DefaultHttpDataSource` 注入 Referer/UA，解决 B 站 CDN 403。
- 批量解析：改为「一行一个链接」逐条本地解析、勾选后 DownloadManager 下载。
- 数据/隐私：只保留本地开关与隐私同意，不再有账号与缓存。
- 真机验证：抖音短链（01:11）、B 站 BV1GJ411x7h7（03:32）均在本机解析并可播放。
