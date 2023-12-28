# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# disable obfuscation
-dontobfuscate

# Keep JNI interface
-keep class org.fcitx.fcitx5.android.core.* { *; }
-keep class org.fcitx.fcitx5.android.data.pinyin.customphrase.PinyinCustomPhrase {
    public <init>(...);
}

# Keep dependency magic
-keep class ** extends org.mechdancer.dependency.Component {
    int hashCode();
    boolean equals(java.lang.Object);
}

# remove kotlin null checks
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkNotNull(...);
    static void checkExpressionValueIsNotNull(...);
    static void checkNotNullExpressionValue(...);
    static void checkReturnedValueIsNotNull(...);
    static void checkFieldIsNotNull(...);
    static void checkParameterIsNotNull(...);
    static void checkNotNullParameter(...);
}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
