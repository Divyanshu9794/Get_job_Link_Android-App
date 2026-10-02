package co.median.android

import android.content.Context

object NotificationMatchingHelper {

    fun getUnreadMatchingJobs(context: Context, allJobs: List<Job>): List<Job> {
        val profile = FirebaseProfileRepository.loadLocalProfile(context)
        var lastOpened = profile.lastOpenedAt

        if (lastOpened == 0L) {
            // Initial threshold: jobs added in the last 7 days
            lastOpened = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            val updated = profile.copy(lastOpenedAt = lastOpened)
            FirebaseProfileRepository.saveProfileLocally(context, updated)
            FirebaseProfileRepository.saveProfileToFirebase(context, updated) { _, _ -> }
        }

        val recentJobs = allJobs.filter { job ->
            if (job.createdAt > 0L) {
                job.createdAt > lastOpened
            } else {
                false
            }
        }

        val matchingJobs = recentJobs.filter { job ->
            val matchResult = ProfileMatchingHelper.calculateMatch(profile, job)
            matchResult.matchPercentage >= 50
        }

        return matchingJobs
    }

    fun markNotificationsAsRead(context: Context) {
        val profile = FirebaseProfileRepository.loadLocalProfile(context)
        val updated = profile.copy(lastOpenedAt = System.currentTimeMillis())
        FirebaseProfileRepository.saveProfileLocally(context, updated)
        FirebaseProfileRepository.saveProfileToFirebase(context, updated) { _, _ -> }
    }
}
