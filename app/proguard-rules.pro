# Keep Kotlinx Serialization models & generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.example.data.model.**$$serializer { *; }
-keepclassmembers class com.example.data.model.** { *** Companion; }
-keep,includedescriptorclasses class com.example.ui.**$$serializer { *; }
-keepclassmembers class com.example.ui.** { *** Companion; }

# Keep Media3 / ExoPlayer classes referenced dynamically
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Coil decoders
-keep class coil.** { *; }
-dontwarn coil.**

# Keep DataStore protobuf / preferences
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**
