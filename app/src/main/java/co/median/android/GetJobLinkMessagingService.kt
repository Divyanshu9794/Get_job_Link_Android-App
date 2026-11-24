package co.median.android

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class GetJobLinkMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "GetJobLinkMessaging"
        const val TOPIC_NEW_JOBS = "new_jobs"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")
        // Ensure topic subscription is maintained on token refresh
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_NEW_JOBS)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe to topic on token refresh", e)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val title = notification?.title
            ?: data["title"]
            ?: "New Job Posted 🚀"

        val body = notification?.body
            ?: data["body"]
            ?: data["message"]
            ?: "A new opportunity has been posted. Tap to view the job."

        val jobId = data["jobId"] ?: data["job_id"] ?: ""
        val targetUrl = data["targetUrl"] ?: ""

        NotificationHelper.sendNotification(
            context = applicationContext,
            title = title,
            message = body,
            targetUrl = targetUrl,
            jobId = jobId
        )
    }
}
