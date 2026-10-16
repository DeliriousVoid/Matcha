# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/deliriousvoid/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Disable obfuscation while keeping shrinking and optimization
-dontobfuscate

# Preserve line numbers for stacktraces
-keepattributes SourceFile,LineNumberTable

# AndroidX Media3 Session, Common & ExoPlayer IPC keep rules
-keep class androidx.media3.session.** { *; }
-keep interface androidx.media3.session.** { *; }
-keepclassmembers class androidx.media3.session.** { *; }

-keep class androidx.media3.common.** { *; }
-keep interface androidx.media3.common.** { *; }

-keep class androidx.media3.exoplayer.** { *; }
-keep interface androidx.media3.exoplayer.** { *; }

-keep class androidx.media3.ui.** { *; }

# CameraX
-keep class androidx.camera.** { *; }
-keep interface androidx.camera.** { *; }

# MLKit Barcode Scanner
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }

# Keep Parcelables for Binder IPC
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# App models, playback service & MainActivity
-keep class com.deliriousvoid.openvkmatcha.data.model.** { *; }
-keep class com.deliriousvoid.openvkmatcha.playback.** { *; }
-keep class com.deliriousvoid.openvkmatcha.MainActivity { *; }

# Guava ListenableFuture
-keep class com.google.common.util.concurrent.ListenableFuture { *; }

# NewPipeExtractor
-keep class org.schabi.newpipe.extractor.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**
-dontwarn java.beans.**
-dontwarn javax.script.**
-dontwarn jdk.dynalink.**
