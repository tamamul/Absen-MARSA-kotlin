# ===== Absen Marsa: aturan R8/ProGuard untuk build rilis =====

-keepattributes Signature, InnerClasses, EnclosingMethod, Exceptions
-keepattributes *Annotation*, RuntimeVisibleAnnotations, AnnotationDefault

# --- kotlinx.serialization (model data memakai @Serializable) ---
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.marsa.absen.**$$serializer { *; }
-keepclassmembers class com.marsa.absen.** {
    *** Companion;
}
-keepclasseswithmembers class com.marsa.absen.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Retrofit ---
-keep interface com.marsa.absen.data.remote.MarsaApi { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# --- OkHttp (dependensi opsional yang tidak dipakai) ---
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Pencatat crash (menampilkan nama class pada stack trace) ---
-keep class com.marsa.absen.CrashActivity { *; }
