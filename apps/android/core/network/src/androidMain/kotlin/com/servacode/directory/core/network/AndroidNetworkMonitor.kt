package com.servacode.directory.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidNetworkMonitor @Inject constructor(
    @ApplicationContext context: Context,
) : NetworkMonitor {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    override val online: Flow<Boolean> = capabilities { it.hasInternet() }

    override val unmetered: Flow<Boolean> = capabilities {
        it.hasInternet() && it.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    /**
     * One reading of the active connection, and another whenever the system says it changed.
     *
     * Both questions this monitor answers are read from the same capabilities, so they are asked
     * the same way rather than through two subscriptions that could disagree.
     */
    private fun capabilities(answer: (NetworkCapabilities) -> Boolean): Flow<Boolean> = callbackFlow {
        fun publish() {
            val network = connectivity.activeNetwork
            val capabilities = network?.let(connectivity::getNetworkCapabilities)
            trySend(capabilities?.let(answer) == true)
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = publish()
            override fun onLost(network: Network) = publish()
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = publish()
        }
        publish()
        connectivity.registerNetworkCallback(
            NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
            callback,
        )
        awaitClose { connectivity.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun NetworkCapabilities.hasInternet(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
