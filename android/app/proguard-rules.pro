# FFmpegKit 通过 JNI 调用，保留 native 入口类。
-keep class com.arthenica.ffmpegkit.** { *; }
-keep class com.arthenica.smartexception.** { *; }

# 保守保留数据模型（避免后续接入反射序列化时被裁）。
-keep class com.orange.nowatermark.data.** { *; }

# 去掉日志与行号信息，减小体积。
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
