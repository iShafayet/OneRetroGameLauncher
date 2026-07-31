# Keep Room / Hilt generated bits
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn okhttp3.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}
-keepclasseswithmembers class * {
    @javax.inject.* <fields>;
    @javax.inject.* <init>(...);
}

# Kotlinx Serialization (JSON models if used reflectively)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class **$$serializer {
    *** INSTANCE;
}

# Enums / Parcelable that R8 sometimes over-prunes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
