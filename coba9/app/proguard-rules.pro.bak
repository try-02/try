# ============================================================
#  PROGUARD / R8 RULES — POS OFFLINE (OPTIMIZED FOR R8 FULL MODE)
#  Compose + Room + FastExcel 0.20.2 + CameraX
#  + ML Kit + ESC/POS Printer
# ============================================================

# =========================================================
# 1. ATURAN UMUM
# =========================================================
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# =========================================================
# 2. HAPUS LOG & DEBUG (AKTIF UNTUK PROD RELEASE)
# =========================================================
# Memastikan tidak ada jejak log transaksi atau debugging POS di aplikasi rilis
# -assumenosideeffects class android.util.Log {
#    public static int v(...);
#    public static int d(...);
#    public static int i(...);
#    public static int w(...);
# }

-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# KOREKSI R8 FULL MODE: Jangan hapus Intrinsics secara paksa lewat -assumenosideeffects 
# karena di AGP rilis terbaru/R8 Full Mode hal ini sering memicu runtime crash (NullPointer).
# R8 secara cerdas sudah mengoptimalkan bytecode ini dengan aman tanpa aturan manual ini.

-assumenosideeffects class androidx.compose.runtime.ComposerKt {
    void sourceInformation(...);
    void sourceInformationMarkerStart(...);
    void sourceInformationMarkerEnd(...);
    void traceEventStart(...);
    void traceEventEnd();
}

# =========================================================
# 3. ROOM DATABASE
# =========================================================
-keep @androidx.room3.Entity class * { *; }
-keep @androidx.room3.Dao interface * { *; }
-keep @androidx.room3.Database class * { *; }

-keepclassmembers class * {
    @androidx.room3.* <fields>;
    @androidx.room3.* <methods>;
}

-keep class * {
    @androidx.room3.ColumnTypeConverter <methods>;
}

# =========================================================
# 4. FASTEXCEL 0.20.2 + AALTO XML 1.4.0
# =========================================================
-keep class com.fasterxml.aalto.** { *; }
-keep interface com.fasterxml.aalto.** { *; }
-keep class com.fasterxml.core.** { *; }
-keep class org.dhatim.fastexcel.** { *; }

-keep class javax.xml.stream.** { *; }
-keep interface javax.xml.stream.** { *; }
-keep class org.codehaus.stax2.** { *; }
-keep interface org.codehaus.stax2.** { *; }
-keep class * implements javax.xml.stream.XMLInputFactory { *; }
-keep class * extends javax.xml.stream.XMLInputFactory { *; }

-dontwarn org.dhatim.fastexcel.**
-dontwarn org.dhatim.fastexcel.reader.**
-dontwarn com.fasterxml.aalto.**
-dontwarn com.fasterxml.core.**
-dontwarn org.codehaus.stax2.**
-dontwarn javax.xml.stream.**

# =========================================================
# 5. ESCPOS THERMAL PRINTER
# =========================================================
-keep class com.dantsu.escposprinter.** { *; }

# =========================================================
# 6. CAMERAX & ML KIT
# =========================================================
-dontwarn androidx.camera.**
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }
-dontwarn com.google.mlkit.**

# =========================================================
# 7. COMPOSE
# =========================================================
-dontwarn androidx.compose.**

# =========================================================
# 8. ENUM
# =========================================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# =========================================================
# 9. PARCELABLE & SERIALIZABLE
# =========================================================
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
}

# =========================================================
# 10. DONTWARN / OPTIONAL DEPENDENCIES
# =========================================================
-dontwarn java.awt.**
-dontwarn javax.**
-dontwarn java.nio.file.**
-dontwarn java.lang.invoke.**
-dontwarn java.lang.reflect.AnnotatedType

-dontwarn org.tukaani.xz.**
-dontwarn org.brotli.dec.**
-dontwarn org.objectweb.asm.**
-dontwarn com.github.luben.zstd.**
