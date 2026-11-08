# 启动本地 Android 模拟器（需要一次 UAC：AEHD 加速驱动要求管理员）
# 说明：SDK 与 AVD 已复制到 D:\craft-sdk / D:\craft-avd（ASCII 路径），避免中文用户名导致 qemu 崩溃。
$ErrorActionPreference = 'Stop'
if (-not ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Start-Process powershell.exe -Verb RunAs -ArgumentList "-NoProfile","-ExecutionPolicy","Bypass","-File","`"$PSCommandPath`""
    exit
}
$env:ANDROID_SDK_ROOT = 'D:\craft-sdk'
$env:ANDROID_HOME = 'D:\craft-sdk'
$env:ANDROID_AVD_HOME = 'D:\craft-avd'
$env:ANDROID_EMULATOR_HOME = 'D:\craft-emu'
$env:ANDROID_USER_HOME = 'D:\craft-emu'
Start-Process -FilePath 'D:\craft-sdk\emulator\emulator.exe' -ArgumentList '-avd','test35','-no-window','-no-audio','-no-boot-anim','-no-snapshot','-gpu','swiftshader_indirect','-memory','2048','-cores','4' -WindowStyle Hidden
& 'D:\craft-sdk\platform-tools\adb.exe' wait-for-device
Write-Host 'emulator-5554 ready'
