package com.betroix.proxyland

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import com.betroix.proxyland.utils.network.ConnectivityMonitor
import java.util.*

class ProxylandSdk private constructor() {
    companion object {
        fun initializeAsync(context: Context, partnerId: String, apiKey: String) {
            val intent = Intent(context, ProxylandSdkService::class.java);
            intent.putExtra("partnerId", partnerId)
            intent.putExtra("apiKey", apiKey)
            intent.putExtra("remoteId", getRemoteId(context))

            setupListeners(context)
            context.startService(intent)
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

        private fun getRemoteId(context: Context): String {
            val sharedPreferences = context.getSharedPreferences("proxyland", Context.MODE_PRIVATE)
            if (!sharedPreferences.contains(remoteIdKey)) {
                val editor = sharedPreferences.edit()
                editor.putString(remoteIdKey, UUID.randomUUID().toString())
                editor.apply()
            }

            return sharedPreferences.getString(remoteIdKey, null).toString()
        }
    }
}