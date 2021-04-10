package com.betroix.proxyland

import android.content.Context
import android.util.Log
import androidx.work.*
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.TimeUnit


class ProxylandSdkWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    companion object {
        private val TAG = "Proxyland Worker"

        private var api: IApi? = null

        fun start(
            context: Context,
            partnerId: String,
            apiKey: String,
            remoteId: String
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<ProxylandSdkWorker>(
                15,
                TimeUnit.MINUTES,
                5,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        "partnerId" to partnerId,
                        "apiKey" to apiKey,
                        "remoteId" to remoteId
                    )
                )
                .build()

            try {
                // Already start early
                api = Api(partnerId, apiKey, remoteId)
                api?.createSocket()
                api?.startSocket()
                    ?.subscribe({}, {})
            } catch (t: Throwable) {
                // ignored
            }

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "proxyland_sdk_worker_periodic",
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
        }

        private fun repeatWork(
            context: Context,
            partnerId: String,
            apiKey: String,
            remoteId: String
        ) {
            Thread.sleep(1000)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<ProxylandSdkWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        "partnerId" to partnerId,
                        "apiKey" to apiKey,
                        "remoteId" to remoteId
                    )
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "proxyland_sdk_worker",
                    ExistingWorkPolicy.KEEP,
                    workRequest
                )
        }
    }

    override fun doWork(): Result {
        try {
            api?.stopSocket()
        } catch (t: Throwable) {
        }

        val partnerId = inputData.getString("partnerId") ?: return Result.failure()
        val apiKey = inputData.getString("apiKey") ?: return Result.failure()
        val remoteId = inputData.getString("remoteId") ?: return Result.failure()
        api = Api(partnerId, apiKey, remoteId)

        try {
            Log.d(TAG, "Proxyland socket start")
            api?.createSocket()
            api?.startSocket()
                ?.blockingSubscribe({}, {})

            Single.timer(9, TimeUnit.MINUTES)
                .blockingGet()
        } catch (t: Throwable) {
            repeatWork(applicationContext, partnerId, apiKey, remoteId)
            return Result.failure()
        }

        repeatWork(applicationContext, partnerId, apiKey, remoteId)
        return Result.success()
    }

    override fun onStopped() {
//        try {
//            api?.stopSocket()
//        } catch (t: Throwable) {}
    }
}