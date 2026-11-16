# ProGuard / R8 Rules for Lectures (MUST Timetable & Student Companion)

# --- General Android & Line Numbers ---
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# --- Kotlin & Coroutines ---
-keepattributes EnclosingMethod,InnerClasses
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }

# --- AndroidX & Jetpack Compose ---
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class androidx.navigation.** { *; }

# --- Room Persistence Library ---
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.TypeConverter class * { *; }
-keepclassmembers class * {
    @androidx.room.Dao *;
    @androidx.room.Database *;
    @androidx.room.Entity *;
    @androidx.room.TypeConverter *;
}

# --- Kotlinx Serialization ---
-keepattributes *Annotation*,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepnames class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * {
    companion object;
}
-keepclassmembers class * extends kotlinx.serialization.KSerializer {
    public static <fields>;
}
-dontwarn kotlinx.serialization.**

# --- Application Models & Data Layer ---
-keep class com.lectures.features.timetable.domain.** { *; }
-keep class com.lectures.features.timetable.data.** { *; }
-keep class com.lectures.features.rooms.** { *; }
-keep class com.lectures.features.tasks.TaskCategory { *; }
-keep class com.lectures.features.tasks.DayProgress { *; }
-keep class com.lectures.features.tasks.TaskDashboardMetrics { *; }
-keep class com.lectures.features.notes.NoteTemplate { *; }
-keep class com.lectures.features.notes.CourseOption { *; }
-keep class com.lectures.features.calendar.AcademicMilestone { *; }
-keep class com.lectures.features.calendar.DayAcademicContext { *; }
-keep class com.lectures.features.calendar.DayWorkload { *; }
-keep class com.lectures.core.notification.InAppNotificationItem { *; }

# --- Alarms, Receivers, Workers, and System Services ---
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends android.content.BroadcastReceiver {
    public <init>();
}
-keep class com.lectures.core.alarm.** { *; }
-keep class com.lectures.core.notification.** { *; }
-keep class com.lectures.core.auth.** { *; }
-keep class com.lectures.core.sync.** { *; }
-keep class com.lectures.core.update.** { *; }
-keep class com.lectures.core.network.** { *; }
-keep class com.lectures.core.analytics.** { *; }
-keep class com.lectures.core.database.** { *; }
-keep class com.lectures.core.storage.** { *; }
-keep class com.lectures.features.feedback.** { *; }
-keep class com.lectures.features.onboarding.** { *; }

# --- Firebase, Credentials, and Networking ---
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn androidx.credentials.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }



