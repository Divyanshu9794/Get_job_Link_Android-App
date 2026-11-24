package co.median.android

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FetchJobsSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            JobFetchEngine.fetchFetchJobs()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
