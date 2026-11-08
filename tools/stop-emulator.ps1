param([string]$Sdk = 'D:\craft-sdk')
& "$Sdk\platform-tools\adb.exe" -s emulator-5554 emu kill
