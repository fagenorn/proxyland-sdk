package com.betroix.proxyland

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers


class ProxylandSdkService : Service() {
    companion object {
        private val TAG = "Proxyland Sdk"
    }

    private var api: IApi? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (api != null) {
            return START_REDELIVER_INTENT;
        }

        if (intent == null) {
            return START_NOT_STICKY;
        }

        val extras = intent.extras ?: return START_NOT_STICKY
        val partnerId = extras.get("partnerId") as String
        val apiKey = extras.get("apiKey") as String
        val remoteId = extras.get("remoteId") as String

        val tempApi = Api(partnerId, apiKey, remoteId)

        Observable.create<Unit> { subscription ->
            try {
                tempApi.createSocket()
                tempApi.startSocket()
                    .doOnSuccess { subscription.onComplete() }
                    .doOnError { subscription.onError(it) }
                    .subscribe({}, {})

                api = tempApi
            } catch (t: Throwable) {
                subscription.tryOnError(t)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { Log.e(TAG, "START SOCKET", it) }
            .subscribe({}, {})

        return START_REDELIVER_INTENT;
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }
}