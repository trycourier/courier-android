# Keep model classes and their fields for Gson deserialization
-keep class com.courier.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep specific annotations that Gson might use
-keepattributes *Annotation*

# Keep Gson library
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Keep Firebase Messaging library
-keep class com.google.firebase.messaging.** { *; }
-dontwarn com.google.firebase.messaging.**

# Keep OkHttp classes
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Keep all Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.Application

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom view constructors
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    static android.os.Parcelable$Creator CREATOR;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep coroutines library
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep RecyclerView library
-keep class androidx.recyclerview.widget.** { *; }
-dontwarn androidx.recyclerview.widget.**

# Keep AndroidX core library
-keep class androidx.core.** { *; }
-dontwarn androidx.core.**

# Keep AppCompat library
-keep class androidx.appcompat.** { *; }
-dontwarn androidx.appcompat.**

# Keep SwipeRefreshLayout library
-keep class androidx.swiperefreshlayout.** { *; }
-dontwarn androidx.swiperefreshlayout.**

# Keep ConstraintLayout library
-keep class androidx.constraintlayout.** { *; }
-dontwarn androidx.constraintlayout.**

# Keep Material Design library
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# Keep Flexbox layout library
-keep class com.google.android.flexbox.** { *; }
-dontwarn com.google.android.flexbox.**