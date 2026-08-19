package com.arh.terminal.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class NetworkType {
    Wifi,
    Cellular,
    Ethernet,
    Disconnected
}

data class NetworkStatus(
    val isConnected: Boolean,
    val type: NetworkType
)

@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val status: Flow<NetworkStatus> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            trySend(NetworkStatus(isConnected = true, type = NetworkType.Wifi))
            close()
            return@callbackFlow
        }

        fun currentStatus(): NetworkStatus {
            val active = connectivityManager.activeNetwork ?: return NetworkStatus(false, NetworkType.Disconnected)
            val caps = connectivityManager.getNetworkCapabilities(active) ?: return NetworkStatus(false, NetworkType.Disconnected)
            val type = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.Wifi
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.Cellular
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.Ethernet
                else -> NetworkType.Disconnected
            }
            val connected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            return NetworkStatus(connected, type)
        }

        trySend(currentStatus())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(currentStatus())
            }

            override fun onLost(network: Network) {
                trySend(NetworkStatus(false, NetworkType.Disconnected))
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(currentStatus())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        var registered = false
        try {
            connectivityManager.registerNetworkCallback(request, callback)
            registered = true
        } catch (_: Throwable) {
            trySend(NetworkStatus(isConnected = true, type = NetworkType.Wifi))
        }

        awaitClose {
            if (registered) {
                try {
                    connectivityManager.unregisterNetworkCallback(callback)
                } catch (_: Throwable) { }
            }
        }
    }
}
