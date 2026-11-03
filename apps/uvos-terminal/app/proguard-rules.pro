# UVOS Terminal Proguard Rules
-keep class com.topjohnwu.superuser.** { *; }
-keepclassmembers class java.io.FileDescriptor {
    private int descriptor;
}
-keep class com.uvos.terminal.jni.** { *; }
