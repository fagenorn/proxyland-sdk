package com.betroix.proxyland.utils.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject

class ConnectivityMonitor : ConnectivityManager.NetworkCallback() {
    companion object {
        private val TAG = "Proxyland Network Monitor"
    }

    val isCellular : Subject<Boolean> = PublishSubject.create()

    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        super.onCapabilitiesChanged(network, networkCapabilities)
        if(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            Log.d(TAG,"Using CELLULAR")
            isCellular.onNext(true)
        } else {
            Log.d(TAG,"Using WiFi")
            isCellular.onNext(false)
        }
    }
}
