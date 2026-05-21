# Keep data models used with Firestore serialization
-keep class com.kinetic.trainer.data.model.** { *; }
# Keep Hilt-generated components
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
# Signal protocol
-dontwarn org.whispersystems.**
-keep class org.whispersystems.** { *; }
