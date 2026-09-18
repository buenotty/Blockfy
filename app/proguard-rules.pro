# Keep Blockfy runtime types. Unused Jetpack / Material Icons classes are still stripped.
-keep class com.buenotty.blockfy.** { *; }

# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ViewModels created by Koin
-keep class * extends androidx.lifecycle.ViewModel { *; }

-dontwarn javax.annotation.**
-dontwarn org.bouncycastle.**
