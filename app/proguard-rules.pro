# ==============================================================================
# 1. Kotlin & Serialization (Wajib untuk KMP)
# ==============================================================================
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keep class kotlin.Metadata { *; }
# Pertahankan serializer yang dihasilkan compiler plugin jika diakses via refleksi
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# ==============================================================================
# 2. Room 3 (Database Offline)
# ==============================================================================
-keep @androidx.room3.Entity class * { *; }
-keep @androidx.room3.Dao interface * { *; }
-keep @androidx.room3.Database class * { *; }
-keepclassmembers class * {
    @androidx.room3.* <fields>;
    @androidx.room3.* <methods>;
    @androidx.room3.ColumnTypeConverter <methods>;
}

# ==============================================================================
# 3. Jetpack Compose & Lifecycle
# ==============================================================================
# Hapus metadata source information Compose untuk menghemat ukuran DEX
-assumenosideeffects class androidx.compose.runtime.ComposerKt {
    void sourceInformation(...);
    void sourceInformationMarkerStart(...);
    void sourceInformationMarkerEnd(...);
    void traceEventStart(...);
    void traceEventEnd();
}
-keep class * extends androidx.lifecycle.ViewModel { *; }

# ==============================================================================
# 4. Dependency Injection (Koin 4.x)
# ==============================================================================
-keep class * extends org.koin.core.module.Module { *; }
-keepclassmembers class * {
    @org.koin.core.annotation.KoinInternalApi *;
}

# ==============================================================================
# 5. Hardware & Library Pihak Ketiga (Offline Focus)
# ==============================================================================
# ESCPOS Printer
-keep class com.dantsu.escposprinter.** { *; }

# MLKit Barcode (Wajib dijaga agar model native dan binding tidak di-obfuscate)
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }

# FastExcel & Aalto-XML (Memerlukan refleksi untuk XML Stream Factory)
-keep class org.dhatim.fastexcel.** { *; }
-keep class com.fasterxml.aalto.** { *; }
-keep class * implements javax.xml.stream.XMLInputFactory { *; }
-keep class * implements javax.xml.stream.XMLOutputFactory { *; }
-keep class * implements javax.xml.stream.XMLEventFactory { *; }

# ==============================================================================
# 6. Optimisasi Ukuran Agresif (Aman untuk Aplikasi 100% Offline)
# ==============================================================================
# Hapus log print dan Logcat Android di build release
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
}

# HANYA dontwarn untuk API Java SE yang memang tidak ada di Android (Aman)
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn java.nio.file.**
-dontwarn org.tukaani.xz.**
-dontwarn org.brotli.dec.**