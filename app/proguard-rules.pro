# ---------------------------------------------------------------------------
# VPN engine (vpnLib) - JNI + reflection heavy, keep it intact.
# ---------------------------------------------------------------------------
-keep class de.blinkt.openvpn.** { *; }
-keep class org.spongycastle.** { *; }
-dontwarn de.blinkt.openvpn.**
-dontwarn org.spongycastle.**

# Native entry points
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
-keep class * implements de.blinkt.openvpn.core.NativeUtils { *; }

# ---------------------------------------------------------------------------
# Data binding generates subclasses of these at build time.
# ---------------------------------------------------------------------------
-keep class * extends androidx.databinding.ViewDataBinding {
    public static *** inflate(android.view.LayoutInflater);
    public static *** bind(android.view.View);
}
-keep class androidx.databinding.** { *; }
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# ---------------------------------------------------------------------------
# Glide
# ---------------------------------------------------------------------------
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** { **[] $VALUES; public *; }

# ---------------------------------------------------------------------------
# Koin
# ---------------------------------------------------------------------------
-keep class org.koin.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>();
}
