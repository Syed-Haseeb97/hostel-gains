package com.hostelgains.data.steps

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

/**
 * 15-minute periodic background worker ensuring step sensor samples are synced into DataStore.
 */
class StepSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val WORK_NAME = "step_sync_periodic_work"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StepSyncWorker>(15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val sensorSource = StepSensorSource(applicationContext)
            if (!sensorSource.isSensorAvailable) {
                return Result.success()
            }

            val stepRepository = StepRepository(applicationContext)
            val raw = withTimeoutOrNull(2000L) {
                sensorSource.rawStepFlow.firstOrNull()
            }

            if (raw != null) {
                stepRepository.recordRawSteps(raw)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
