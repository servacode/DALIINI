# R8 rules for the release build.
#
# Most libraries ship their own consumer rules and are not repeated here: Hilt/Dagger, Room,
# AndroidX (Navigation, Lifecycle, Compose), Retrofit 3, OkHttp 5, Coil 3, Firebase
# Messaging and MapLibre (its JNI peers) all carry META-INF/proguard or
# META-INF/com.android.tools rules in their artifacts. The rules below cover what this
# app itself reaches through reflection, derived from the code:
#
#  - The generated P10 client (packages/api-kotlin) uses kotlinx.serialization through
#    Retrofit's converter. That converter resolves a serializer from a java.lang.reflect.Type
#    at run time, which looks up the static `Companion` field and its `serializer()` method.
#    No Moshi or Gson is used anywhere.
#  - The cache (core/database) and type-safe navigation routes (core/model DirectoryRoute)
#    use the same serializer lookup for @Serializable domain classes.
#  - Retrofit service interfaces are invoked through java.lang.reflect.Proxy and read the
#    generic signatures of `suspend fun ...(): Response<T>`.
#
# Navigation voice plays recorded clips from feature/navigation res/raw through the generated
# NAVIGATION_CLIPS table (R.raw constants, no Resources.getIdentifier), so R8 sees every id it
# needs. The resource shrinker is still told to keep the whole pack in
# feature/navigation/src/main/res/raw/navigation_voice_keep.xml: a clip the table stops naming
# must fail loudly in review, not vanish silently from a release build.

# Generic signatures and annotations Retrofit and kotlinx.serialization read at run time.
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault

# --- kotlinx.serialization, scoped to this app's packages -------------------------------
# Generated models, the domain models cached as JSON, and navigation routes.
-if @kotlinx.serialization.Serializable class com.servacode.directory.**
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class com.servacode.directory.** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class com.servacode.directory.**
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
# `data object` routes and serializable objects expose their serializer on the instance.
-if @kotlinx.serialization.Serializable class com.servacode.directory.**
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
# The generated contextual adapters (UUID, OffsetDateTime, ...) are registered by class
# literal in api.infrastructure.Serializer; keep their names stable for error messages only.
-keepnames class com.servacode.directory.api.infrastructure.*Adapter

# --- Retrofit service interfaces from the generated client --------------------------------
# Retrofit's own rules keep annotated interface methods; the generated APIs are named here
# so that an interface only reached through GeneratedClient.create<T>() is never merged.
-keep,allowobfuscation interface com.servacode.directory.api.apis.*
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# --- Firebase Messaging -------------------------------------------------------------------
# The service is named in the manifest (kept by AAPT); its callbacks are overrides and are
# kept with it. Nothing else of ours is reached by Firebase through reflection.

# --- Crash reports ------------------------------------------------------------------------
# Readable stack traces once the mapping file is applied.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
