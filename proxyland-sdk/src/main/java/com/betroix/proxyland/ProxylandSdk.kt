package com.betroix.proxyland

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.work.WorkManager
import com.betroix.proxyland.utils.network.ConnectivityMonitor
import java.util.*

class ProxylandSdk private constructor() {
    companion object {
        fun initializeAsync(context: Context, partnerId: String, apiKey: String) {
            setPartnerId(context, partnerId)
            setApiKey(context, apiKey)

            setupListeners(context)
            WorkManager.getInstance(context).cancelAllWorkByTag("proxyland_sdk_worker")
            WorkManager.getInstance(context).cancelAllWorkByTag("proxyland_sdk_worker_periodic")
            ProxylandSdkWorker.start(context, partnerId, apiKey, getRemoteId(context))
        }

        private fun setupListeners(context: Context) {
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

        private const val remoteIdKey = "proxyland-remote-id"

        private const val partnerIdKey = "proxyland-partner-id"

        private const val apiKeyKey = "proxyland-api-key"

        fun getRemoteId(context: Context): String {
            val sharedPreferences = context.getSharedPreferences("proxyland", Context.MODE_PRIVATE)
            if (!sharedPreferences.contains(remoteIdKey)) {
                val editor = sharedPreferences.edit()
                editor.putString(remoteIdKey, UUID.randomUUID().toString())
                editor.apply()
            }

            return sharedPreferences.getString(remoteIdKey, null).toString()
        }

        private fun setPartnerId(context: Context, string: String): Unit {
            val sharedPreferences = context.getSharedPreferences("proxyland", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString(partnerIdKey, string)
            editor.apply()
        }

        private fun setApiKey(context: Context, string: String): Unit {
            val sharedPreferences = context.getSharedPreferences("proxyland", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString(apiKeyKey, string)
            editor.apply()
        }

        fun getPartnerId(context: Context): String? {
            val sharedPreferences = context.getSharedPreferences("proxyland", Context.MODE_PRIVATE)
            if (!sharedPreferences.contains(partnerIdKey)) {
                return null
            }

            return sharedPreferences.getString(partnerIdKey, null).toString()
        }

        fun getApiKey(context: Context): String? {
            val sharedPreferences = context.getSharedPreferences("proxyland", Context.MODE_PRIVATE)
            if (!sharedPreferences.contains(apiKeyKey)) {
                return null
            }

            return sharedPreferences.getString(apiKeyKey, null).toString()
        }
    }
}