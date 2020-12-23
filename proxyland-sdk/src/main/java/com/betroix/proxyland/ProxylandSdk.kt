package com.betroix.proxyland

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import com.betroix.proxyland.utils.network.ConnectivityMonitor
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.*

class ProxylandSdk private constructor(
    private val context: Context,
    partnerId: String,
    apiKey: String
) {
    companion object {
        private val TAG = "Proxyland Sdk"
        private var instance: ProxylandSdk? = null;

        private fun initializeInternal(
            context: Context,
            partnerId: String,
            apiKey: String
        ): Observable<Unit> {
            if (instance != null) Observable.empty<Unit>()
            val sdk = ProxylandSdk(context, partnerId, apiKey)
            instance = sdk;

            return Observable.create { subscription ->
                try {
                    sdk.api.createSocket()
                    sdk.api.startSocket()
                        .doOnSuccess { subscription.onComplete() }
                        .doOnError { subscription.onError(it) }
                        .subscribe({}, {})
                } catch (t: Throwable) {
                    subscription.tryOnError(t)
                }
            }
        }

        fun initialize(context: Context, partnerId: String, apiKey: String) {
            initializeInternal(context, partnerId, apiKey).doOnError {
                Log.e(
                    TAG,
                    "START SOCKET",
                    it
                )
            }
                .blockingSubscribe()
        }

        fun initializeAsync(
            context: Context, partnerId: String, apiKey: String,
            completedCallback: (() -> Unit)? = null,
            errorCallback: ((t: Throwable) -> Unit)? = null
        ) {
            initializeInternal(context, partnerId, apiKey).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete { completedCallback?.invoke() }
                .doOnError {
                    Log.e(TAG, "START SOCKET", it)
                    errorCallback?.invoke(it)
                }
                .subscribe({}, {})
        }
    }

    private val api: IApi
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("proxyland", Context.MODE_PRIVATE)
    private val remoteIdKey = "proxyland-remote-id"

    init {
        setupListeners()
        api = Api(partnerId, apiKey, getRemoteId())
    }

    private fun getRemoteId(): String {
        if (!sharedPreferences.contains(remoteIdKey)) {
            val editor = sharedPreferences.edit()
            editor.putString(remoteIdKey, UUID.randomUUID().toString())
            editor.apply()
        }

        return sharedPreferences.getString(remoteIdKey, null).toString()
    }

    private fun setupListeners() {
        // connection listener
        val connectivityMonitor = ConnectivityMonitor()
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(connectivityMonitor)
        } else {
            val builder = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)

            connectivityManager.registerNetworkCallback(builder.build(), connectivityMonitor)
        }
    }
}