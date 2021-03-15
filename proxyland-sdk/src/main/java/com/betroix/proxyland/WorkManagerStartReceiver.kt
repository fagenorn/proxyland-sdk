package com.betroix.proxyland

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager

class WorkManagerStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val partnerId = ProxylandSdk.getPartnerId(context) ?: return
        val apiKey = ProxylandSdk.getApiKey(context) ?: return

        WorkManager.getInstance(context).cancelAllWorkByTag("proxyland_sdk_worker")
        WorkManager.getInstance(context).cancelAllWorkByTag("proxyland_sdk_worker_periodic")
        ProxylandSdkWorker.start(context, partnerId, apiKey, ProxylandSdk.getRemoteId(context))
    }
}