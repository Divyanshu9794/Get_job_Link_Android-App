package co.median.android

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class NotificationItem(
    val title: String,
    val message: String,
    val targetUrl: String,
    val jobId: String,
    val timestamp: Long,
    var read: Boolean = false,
    val type: String = "new_job"
)

object NotificationHelper {

    private const val TAG = "NotificationHelper"
    const val CHANNEL_JOB_ALERTS = "new_job_alerts"
    const val CHANNEL_GENERAL = "general_channel"
    const val REQUEST_CODE_PERMISSION = 1001

    private const val PREFS_NAME = "notification_history_prefs"
    private const val KEY_NOTIFICATIONS = "notifications_json"
    private const val KEY_LAST_CHECK = "last_notification_check_at"

    @JvmStatic
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val jobAlertsChannel = NotificationChannel(
                CHANNEL_JOB_ALERTS,
                "New Job Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new tech job listings and opportunities"
                enableVibration(true)
                setShowBadge(true)
            }

            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General application updates and announcements"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(jobAlertsChannel)
            notificationManager.createNotificationChannel(generalChannel)
        }
    }

    @JvmStatic
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_PERMISSION
                )
            }
        }
    }

    @JvmStatic
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @JvmStatic
    fun areNotificationsEnabled(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = notificationManager.getNotificationChannel(CHANNEL_JOB_ALERTS)
            if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) {
                return false
            }
        }
        return hasNotificationPermission(context)
    }

    @JvmStatic
    fun openNotificationSettings(context: Context) {
        try {
            val intent = Intent().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                } else {
                    action = "android.settings.APP_NOTIFICATION_SETTINGS"
                    putExtra("app_package", context.packageName)
                    putExtra("app_uid", context.applicationInfo.uid)
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening notification settings", e)
        }
    }

    @JvmStatic
    fun setupFcmToken(context: Context) {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("new_jobs")
                .addOnSuccessListener { Log.d(TAG, "Subscribed to new_jobs topic") }

            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d(TAG, "FCM token obtained: $token")
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (!uid.isNullOrBlank() && !token.isNullOrBlank()) {
                        val updates = mapOf(
                            "fcmToken" to token,
                            "fcmTokens" to FieldValue.arrayUnion(token),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        FirebaseFirestore.getInstance().collection("users").document(uid)
                            .update(updates)
                            .addOnFailureListener {
                                FirebaseFirestore.getInstance().collection("users").document(uid)
                                    .set(updates, SetOptions.merge())
                            }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up FCM token", e)
        }
    }

    @JvmStatic
    fun clearFcmTokenOnLogout(context: Context) {
        try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("new_jobs")
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (!uid.isNullOrBlank()) {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        if (!token.isNullOrBlank()) {
                            val updates = mapOf(
                                "fcmTokens" to FieldValue.arrayRemove(token)
                            )
                            FirebaseFirestore.getInstance().collection("users").document(uid)
                                .update(updates)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing FCM token on logout", e)
        }
    }

    @JvmStatic
    @JvmOverloads
    @SuppressLint("MissingPermission")
    fun sendNotification(
        context: Context,
        title: String,
        message: String,
        targetUrl: String? = null,
        jobId: String? = null,
        channelId: String = CHANNEL_JOB_ALERTS,
        notificationId: Int = if (!jobId.isNullOrEmpty()) jobId.hashCode() else (System.currentTimeMillis() % 10000).toInt()
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, LaunchActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!jobId.isNullOrEmpty()) {
                putExtra("job_id", jobId)
                putExtra("job_title", title)
                putExtra("job_description", message)
            }
            if (!targetUrl.isNullOrEmpty()) {
                putExtra(MainActivity.INTENT_TARGET_URL, targetUrl)
                putExtra("job_url", targetUrl)
            }
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            pendingIntentFlags
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        if (hasNotificationPermission(context)) {
            try {
                with(NotificationManagerCompat.from(context)) {
                    notify(notificationId, builder.build())
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }

        saveNotificationToHistory(
            context = context,
            title = title,
            message = message,
            targetUrl = targetUrl,
            jobId = jobId,
            timestamp = System.currentTimeMillis(),
            read = false,
            type = "new_job"
        )
    }

    @JvmStatic
    fun saveNotificationToHistory(
        context: Context,
        title: String,
        message: String,
        targetUrl: String?,
        jobId: String?,
        timestamp: Long,
        read: Boolean = false,
        type: String = "new_job"
    ) {
        try {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(KEY_NOTIFICATIONS, "[]") ?: "[]"
            val array = JSONArray(jsonString)

            // Prevent duplicate entries for the same jobId if non-blank
            if (!jobId.isNullOrBlank()) {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (obj.optString("jobId") == jobId) {
                        return // Already exists
                    }
                }
            }

            val item = JSONObject().apply {
                put("title", title)
                put("message", message)
                put("targetUrl", targetUrl ?: "")
                put("jobId", jobId ?: "")
                put("timestamp", timestamp)
                put("read", read)
                put("type", type)
            }

            val newArray = JSONArray()
            newArray.put(item)
            for (i in 0 until minOf(array.length(), 49)) { // keep max 50
                newArray.put(array.get(i))
            }

            prefs.edit().putString(KEY_NOTIFICATIONS, newArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun checkNewJobsOnAppOpen(context: Context, onComplete: ((newCount: Int, newJobsMsg: String) -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0L)
                val now = System.currentTimeMillis()

                // Default threshold if first time: past 7 days
                val threshold = if (lastCheck == 0L) now - (7 * 24 * 60 * 60 * 1000L) else lastCheck

                val allJobs: List<co.median.android.Job> = JobRepository.getAllJobs()
                val newJobs: List<co.median.android.Job> = allJobs.filter { jobItem ->
                    jobItem.createdAt > threshold
                }

                var newlyAddedCount = 0
                if (newJobs.isNotEmpty()) {
                    for (jobItem in newJobs) {
                        val title = "🚀 New Job Posted"
                        val companyName = if (jobItem.company.isNotBlank()) jobItem.company else "Top Company"
                        val body = "${jobItem.title} at $companyName"
                        saveNotificationToHistory(
                            context = context,
                            title = title,
                            message = body,
                            targetUrl = jobItem.applicationUrl,
                            jobId = jobItem.id,
                            timestamp = if (jobItem.createdAt > 0) jobItem.createdAt else now,
                            read = false,
                            type = "new_job"
                        )
                        newlyAddedCount++
                    }
                }

                // Update last check timestamp locally and in Firestore
                prefs.edit().putLong(KEY_LAST_CHECK, now).apply()
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (!uid.isNullOrBlank()) {
                    val updates = mapOf("lastNotificationCheckAt" to now)
                    FirebaseFirestore.getInstance().collection("users").document(uid)
                        .set(updates, SetOptions.merge())
                }

                val msg = if (newlyAddedCount == 1) {
                    "1 new job was posted since your last visit."
                } else if (newlyAddedCount > 1) {
                    "$newlyAddedCount new jobs were added since your last visit."
                } else ""

                withContext(Dispatchers.Main) {
                    onComplete?.invoke(newlyAddedCount, msg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking new jobs on app open", e)
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(0, "")
                }
            }
        }
    }

    @JvmStatic
    fun markNotificationAsRead(context: Context, jobId: String) {
        if (jobId.isBlank()) return
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(KEY_NOTIFICATIONS, "[]") ?: "[]"
            val array = JSONArray(jsonString)
            var modified = false

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optString("jobId") == jobId) {
                    obj.put("read", true)
                    modified = true
                }
            }

            if (modified) {
                prefs.edit().putString(KEY_NOTIFICATIONS, array.toString()).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun getUnreadCount(context: Context): Int {
        var count = 0
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(KEY_NOTIFICATIONS, "[]") ?: "[]"
            val array = JSONArray(jsonString)

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (!obj.optBoolean("read", false)) {
                    count++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return count
    }

    @JvmStatic
    fun markAllAsRead(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(KEY_NOTIFICATIONS, "[]") ?: "[]"
            val array = JSONArray(jsonString)

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                obj.put("read", true)
            }

            prefs.edit().putString(KEY_NOTIFICATIONS, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun getNotificationHistory(context: Context): List<NotificationItem> {
        val list = mutableListOf<NotificationItem>()
        try {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(KEY_NOTIFICATIONS, "[]") ?: "[]"
            val array = JSONArray(jsonString)

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    NotificationItem(
                        title = obj.optString("title", ""),
                        message = obj.optString("message", ""),
                        targetUrl = obj.optString("targetUrl", ""),
                        jobId = obj.optString("jobId", ""),
                        timestamp = obj.optLong("timestamp", 0L),
                        read = obj.optBoolean("read", false),
                        type = obj.optString("type", "new_job")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    @JvmStatic
    fun clearNotificationHistory(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_NOTIFICATIONS).apply()
    }
}
