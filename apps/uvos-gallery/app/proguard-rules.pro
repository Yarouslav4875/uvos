# UVOS Gallery Proguard Rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**
