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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject

object NotificationHelper {

    const val CHANNEL_JOB_ALERTS = "new_job_alerts"
    const val CHANNEL_GENERAL = "general_channel"
    const val REQUEST_CODE_PERMISSION = 1001

    private const val PREFS_NAME = "notification_history_prefs"
    private const val KEY_NOTIFICATIONS = "notifications_json"
    private const val KEY_UNREAD_COUNT = "unread_count"

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
    @JvmOverloads
    @SuppressLint("MissingPermission")
    fun sendNotification(
        context: Context,
        title: String,
        message: String,
        targetUrl: String? = null,
        jobId: String? = null,
        channelId: String = CHANNEL_JOB_ALERTS,
        notificationId: Int = (System.currentTimeMillis() % 10000).toInt()
    ) {
        createNotificationChannels(context)

        val intent = if (!jobId.isNullOrEmpty()) {
            Intent(context, JobDetailsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("job_id", jobId)
                putExtra("job_title", title)
                putExtra("job_description", message)
                if (!targetUrl.isNullOrEmpty()) {
                    putExtra("job_url", targetUrl)
                }
            }
        } else {
            Intent(context, LaunchActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (!targetUrl.isNullOrEmpty()) {
                    putExtra(MainActivity.INTENT_TARGET_URL, targetUrl)
                }
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

        // Save to notification history
        saveNotificationToHistory(context, title, message, targetUrl, jobId, System.currentTimeMillis())
    }

    private fun saveNotificationToHistory(
        context: Context,
        title: String,
        message: String,
        targetUrl: String?,
        jobId: String?,
        timestamp: Long
    ) {
        try {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(KEY_NOTIFICATIONS, "[]") ?: "[]"
            val array = JSONArray(jsonString)

            val item = JSONObject().apply {
                put("title", title)
                put("message", message)
                put("targetUrl", targetUrl ?: "")
                put("jobId", jobId ?: "")
                put("timestamp", timestamp)
            }

            // Insert at beginning
            val newArray = JSONArray()
            newArray.put(item)
            for (i in 0 until minOf(array.length(), 49)) { // keep max 50
                newArray.put(array.get(i))
            }

            val currentUnread = prefs.getInt(KEY_UNREAD_COUNT, 0)
            prefs.edit()
                .putString(KEY_NOTIFICATIONS, newArray.toString())
                .putInt(KEY_UNREAD_COUNT, currentUnread + 1)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun getUnreadCount(context: Context): Int {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_UNREAD_COUNT, 0)
    }

    @JvmStatic
    fun markAllAsRead(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_UNREAD_COUNT, 0).apply()
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
                        timestamp = obj.optLong("timestamp", 0L)
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

data class NotificationItem(
    val title: String,
    val message: String,
    val targetUrl: String,
    val jobId: String,
    val timestamp: Long
)
