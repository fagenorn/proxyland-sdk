package com.betroix.proxyland

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.*

class ProxylandSdk(context: Context, partnerId: String) {
    companion object {
        private val TAG = "Proxyland Sdk"
    }

    private val api: IApi
    private var initialized = false
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("proxyland", Context.MODE_PRIVATE)
    private val remoteIdKey = "proxyland-remote-id"

    init {
        api = Api(partnerId, getRemoteId())
    }

    fun initialize() {
        if (initialized) return
        initialized = true

        Observable.create<Unit> { subscription ->

            try {
                api.createSocket()
                api.startSocket()
                    .doOnSuccess { subscription.onComplete() }
                    .subscribe({}, {})
            } catch (t: Throwable) {
                subscription.onError(t)
            }

        }.doOnError { Log.e(TAG, "START SOCKET", it) }
            .blockingSubscribe()
    }

    fun initializeAsync(
        completedCallback: (() -> Unit)? = null,
        errorCallback: ((t: Throwable) -> Unit)? = null
    ) {
        if (initialized) return
        initialized = true

        Observable.create<Unit> { subscription ->

            try {
                api.createSocket()
                api.startSocket()
                    .doOnSuccess { subscription.onComplete() }
                    .doOnError { subscription.onError(it) }
                    .subscribe({}, {})
            } catch (t: Throwable) {
                subscription.tryOnError(t)
            }

        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnComplete { completedCallback?.invoke() }
            .doOnError {
                Log.e(TAG, "START SOCKET", it)
                errorCallback?.invoke(it)
            }
            .subscribe({}, {})
    }

    private fun getRemoteId(): String {
        if (!sharedPreferences.contains(remoteIdKey)) {
            val editor = sharedPreferences.edit()
            editor.putString(remoteIdKey, UUID.randomUUID().toString())
            editor.apply()
        }

        return sharedPreferences.getString(remoteIdKey, null).toString()
    }
}