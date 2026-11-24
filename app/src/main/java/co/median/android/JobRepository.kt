package co.median.android

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

object JobRepository {
    private const val TAG = "JobRepository"
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _jobsState = MutableStateFlow<List<Job>>(emptyList())
    val jobsState: StateFlow<List<Job>> = _jobsState.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null
    private var isListening = false

    fun startRealtimeJobsListener(context: Context? = null) {
        if (isListening && listenerRegistration != null) return

        try {
            listenerRegistration = firestore.collection("jobs")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to Firestore jobs: ${error.localizedMessage}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val jobsList = mutableListOf<Job>()
                        for (doc in snapshot.documents) {
                            val job = Job.fromDocument(doc)
                            if (job.title.isNotBlank() || job.company.isNotBlank()) {
                                jobsList.add(job)
                            }
                        }

                        val uniqueJobs = jobsList.distinctBy { it.id }
                        _jobsState.value = uniqueJobs

                        if (BuildConfig.DEBUG && uniqueJobs.isNotEmpty()) {
                            val latestDateStr = uniqueJobs.maxByOrNull { it.createdAt }?.postedDate ?: "N/A"
                            Log.d(TAG, "Firebase Project: get-job-link")
                            Log.d(TAG, "Collection: jobs")
                            Log.d(TAG, "Fetched Jobs: ${uniqueJobs.size}")
                            Log.d(TAG, "Latest Job Date: $latestDateStr")
                            Log.d(TAG, "Query/Filter: Realtime Firestore snapshot")
                        }
                    }
                }
            isListening = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firestore jobs listener", e)
        }
    }

    suspend fun getAllJobs(): List<Job> {
        val current = _jobsState.value
        if (current.isNotEmpty()) return current

        return try {
            val snapshot = firestore.collection("jobs").get().await()
            val jobsList = mutableListOf<Job>()
            for (doc in snapshot.documents) {
                val job = Job.fromDocument(doc)
                if (job.title.isNotBlank() || job.company.isNotBlank()) {
                    jobsList.add(job)
                }
            }
            val unique = jobsList.distinctBy { it.id }
            _jobsState.value = unique
            unique
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching jobs from Firestore", e)
            _jobsState.value
        }
    }

    suspend fun getSavedJobs(userId: String): List<Job> {
        if (userId.isBlank()) return emptyList()
        return try {
            val snapshot = firestore.collection("userActivity")
                .document(userId)
                .collection("savedJobs")
                .get()
                .await()

            val savedIds = snapshot.documents.mapNotNull { it.id }
            if (savedIds.isEmpty()) return emptyList()

            val allJobs = getAllJobs()
            allJobs.filter { savedIds.contains(it.id) }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching saved jobs for user $userId", e)
            emptyList()
        }
    }

    suspend fun saveJob(userId: String, jobId: String): Boolean {
        if (userId.isBlank() || jobId.isBlank()) return false
        return try {
            firestore.collection("userActivity")
                .document(userId)
                .collection("savedJobs")
                .document(jobId)
                .set(mapOf("savedAt" to System.currentTimeMillis()))
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving job $jobId", e)
            false
        }
    }

    suspend fun unsaveJob(userId: String, jobId: String): Boolean {
        if (userId.isBlank() || jobId.isBlank()) return false
        return try {
            firestore.collection("userActivity")
                .document(userId)
                .collection("savedJobs")
                .document(jobId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error unsaving job $jobId", e)
            false
        }
    }

    fun stopListener() {
        listenerRegistration?.remove()
        listenerRegistration = null
        isListening = false
    }
}
