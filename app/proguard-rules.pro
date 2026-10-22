# Lagoon R8 rules.
#
# Shrinking and obfuscation stay ON for release builds (smaller APK, dead code removed).
# R8 writes the mapping file to app/build/outputs/mapping/<flavor>Release/mapping.txt; the
# release workflow keeps it as a build artefact and attaches the FOSS one to the GitHub
# Release so obfuscated stack traces can be retraced. Do not add -dontobfuscate here.

# --- Crash readability -------------------------------------------------------------------
# Keep file names and line numbers so stack traces in bug reports point at real lines even
# before retracing. SourceFile is deliberately not renamed: this is an open-source app and
# readable traces matter more than a few KB.
-keepattributes SourceFile,LineNumberTable
# Needed by Dagger/Hilt generated code, kotlinx.serialization and Kotlin metadata.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault,*Annotation*

# --- Components referenced from AndroidManifest.xml ---------------------------------------
# AGP already emits keep rules for manifest components, but the island is hosted by a
# system-bound service, so make the guarantee explicit: a renamed listener or accessibility
# service would silently stop being bound by the system.
-keep class com.lagoon.island.LagoonApp
-keep class * extends android.app.Activity
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.service.notification.NotificationListenerService
-keep class * extends android.accessibilityservice.AccessibilityService
# Anything the app marks @Keep (classes instantiated reflectively by the framework).
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * { @androidx.annotation.Keep *; }

# --- kotlinx.serialization (settings DataStore, export/import, navigation keys) -----------
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
# Generated serializers and the Companion.serializer() accessors of our @Serializable classes.
-keep,includedescriptorclasses class com.lagoon.island.**$$serializer { *; }
-keepclassmembers class com.lagoon.island.** { *** Companion; }
-keepclasseswithmembers class com.lagoon.island.** { kotlinx.serialization.KSerializer serializer(...); }
# @Serializable enums are encoded by constant name: keep the names stable or exported
# settings files stop importing after an update.
-keepclassmembers @kotlinx.serialization.Serializable enum com.lagoon.island.** { <fields>; }

# --- Hilt / Dagger ------------------------------------------------------------------------
# The Hilt AARs ship their own consumer rules (generated components, entry points, the
# @HiltAndroidApp base class). Only the bridge between generated code and our classes is
# pinned here so that a change in the library's consumer rules fails loudly in CI rather
# than at runtime.
-keep class com.lagoon.island.Hilt_* { *; }
-keep class com.lagoon.island.**.Hilt_* { *; }
-keep class dagger.hilt.internal.aggregatedroot.codegen.** { *; }
-keep class hilt_aggregated_deps.** { *; }

# --- Jetpack Compose ----------------------------------------------------------------------
# Compose ships consumer rules; nothing extra is needed. Do not keep androidx.compose.**
# wholesale: it would add megabytes to the APK.

# --- androidx.palette / Coil --------------------------------------------------------------
# palette-ktx is plain Java with no reflection. Coil 3 ships its own consumer rules.
# Nothing needed.

# --- Kotlin coroutines --------------------------------------------------------------------
# kotlinx-coroutines-android ships consumer rules (ServiceLoader lookups for the main
# dispatcher are rewritten by R8). Nothing needed.

# --- Noise ---------------------------------------------------------------------------------
# Optional JVM-only classes referenced from Kotlin stdlib / coroutines metadata.
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn org.jetbrains.annotations.**
