# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.nikita.workoutstudio.** {
    kotlinx.serialization.KSerializer serializer(...);
}
