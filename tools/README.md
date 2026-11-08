# 本地模拟器（开发验证用）

手机不再是必需品。本机已装：

- emulator 37.2.13 + android-35 x86_64 镜像
- AVD：`test35`（1080x2400 / 420dpi / RAM 2048）
- 位置：`D:\craft-sdk`（SDK）、`D:\craft-avd`（AVD）、`D:\craft-emu`（emulator home）

## 为什么在 D:

Windows 用户名含中文时，qemu 会在 `ucrtbase.dll` 里 `0xc0000409` 崩溃；把 SDK/AVD 放到纯 ASCII 路径后正常。
复制与启动都需要管理员令牌（沙箱里普通进程不能写 D:，AEHD 也要管理员）。

## 启动

右键 `tools/start-emulator.ps1` → 使用 PowerShell 运行（会弹一次 UAC），等 `emulator-5554 ready`。

或手动：

```powershell
$env:ANDROID_SDK_ROOT='D:\craft-sdk'; $env:ANDROID_HOME='D:\craft-sdk'
$env:ANDROID_AVD_HOME='D:\craft-avd'
$env:ANDROID_EMULATOR_HOME='D:\craft-emu'; $env:ANDROID_USER_HOME='D:\craft-emu'
Start-Process 'D:\craft-sdk\emulator\emulator.exe' -ArgumentList '-avd','test35','-no-window','-no-audio','-no-boot-anim','-no-snapshot','-gpu','swiftshader_indirect','-memory','2048','-cores','4'
```

## 安装 / 截图

```bash
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.orange.nowatermark/.MainActivity
adb exec-out screencap -p > shot.png
```

## 已知限制

- 模拟器里抖音 WebView 页面拿不到 `_ROUTER_DATA`（提示「页面中没有视频数据」），抖音请用真机验证；B 站/本地媒体功能在模拟器正常。
- 模拟器为 3 键导航，底部悬浮导航会与系统栏重叠，属显示差异。
