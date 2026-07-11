# Keep DTOs — kotlinx.serialization + Retrofit reflect on these at runtime.
-keep class ch.rhosys.gitzi.data.remote.dto.** { *; }
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class ch.rhosys.gitzi.**$$serializer {
    *** serializer(...);
}
-keep,includedescriptorclasses class ch.rhosys.gitzi.**$$serializer { *; }
-keepclassmembers class ch.rhosys.gitzi.** {
    *** Companion;
}
-keepclasseswithmembers class ch.rhosys.gitzi.** {
    kotlinx.serialization.KSerializer serializer(...);
}
