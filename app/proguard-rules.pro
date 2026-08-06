-keepattributes Signature
-keepattributes *Annotation*
-keep class com.haprial.app.data.model.** { *; }
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn io.noties.**
-dontwarn javax.annotation.**
