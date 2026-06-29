package com.cfg.android.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.cfg.android.data.local.dao.SyncQueueDao
import com.cfg.android.data.remote.ApiService
import com.cfg.android.data.remote.dto.SyncBatchRequest
import com.cfg.android.data.remote.dto.SyncItem
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncQueueDao: SyncQueueDao,
    private val apiService: ApiService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val pending = syncQueueDao.getPending()
        if (pending.isEmpty()) return Result.success()

        val items = pending.map {
            SyncItem(
                clientUuid = it.clientUuid,
                eventType = it.eventType,
                payload = it.payload,
                localTimestamp = Instant.ofEpochMilli(it.localTimestamp).toString()
            )
        }

        return try {
            val response = apiService.syncBatch(SyncBatchRequest(items))
            if (response.isSuccessful) {
                response.body()?.data?.forEach { result ->
                    val uuid = result["clientUuid"] as? String ?: return@forEach
                    val status = result["status"] as? String ?: return@forEach
                    when (status) {
                        "PROCESSED", "ALREADY_PROCESSED" -> syncQueueDao.updateStatus(uuid, "SYNCED")
                        "ERROR" -> syncQueueDao.incrementRetry(uuid)
                    }
                }
                syncQueueDao.clearSynced()
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "cfg_sync_worker"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun triggerImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
