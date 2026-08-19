# ARH-Terminal R8 / ProGuard Optimization & Shrinking Rules

# 1. SSHJ & BouncyCastle Cryptography
-keep class net.schmizz.sshj.** { *; }
-dontwarn net.schmizz.sshj.**
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn org.slf4j.**
-dontwarn net.i2p.crypto.eddsa.**
-dontwarn sun.security.**

# 2. Coroutines & Flow
-keepnames class kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.**
-keepclassmembers class * extends kotlinx.coroutines.CoroutineScope {
    public <methods>;
}

# 3. Dagger / Hilt Dependency Injection
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.accessibilityservice.AccessibilityService { *; }
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**

# 4. Data Models & JSON Serialization
-keepclassmembers class com.arh.terminal.data.** { *; }
-keepclassmembers class com.arh.terminal.core.** { *; }
-keepclassmembers class com.pocketshell.core.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# 5. Jetpack Compose
-keepclassmembers class androidx.compose.ui.platform.AndroidComposeView { *; }
