# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# --- Embedded ADB client (libadb-android + BouncyCastle + spake2) ---
# libadb resolves these through provider lookup and reflection during ADB pairing, so the
# names must survive shrinking.
-keep class io.github.muntashirakon.adb.** { *; }
-keep class io.github.muntashirakon.crypto.** { *; }
-dontwarn io.github.muntashirakon.adb.**
-dontwarn io.github.muntashirakon.crypto.**
-dontwarn org.bouncycastle.**
-dontwarn javax.security.auth.**
-dontwarn org.conscrypt.**
