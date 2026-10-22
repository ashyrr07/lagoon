# kotlinx.serialization
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.lagoon.island.**$$serializer { *; }
-keepclassmembers class com.lagoon.island.** { *** Companion; }
-keepclasseswithmembers class com.lagoon.island.** { kotlinx.serialization.KSerializer serializer(...); }
