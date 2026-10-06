package com.manishraj.saavnmusic.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Online/offline as an observable mode (REDESIGN_SPEC §4: offline is a
 * mode, not an error screen). Screens branch on [online] to show the
 * offline banner + local-content variant instead of failing.
 */
@Singleton
class ConnectivityObserver
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val manager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        private val _online = MutableStateFlow(currentlyOnline())
        val online: StateFlow<Boolean> = _online

        init {
            manager?.registerDefaultNetworkCallback(
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _online.value = true
                    }

                    override fun onLost(network: Network) {
                        _online.value = currentlyOnline()
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        capabilities: NetworkCapabilities,
                    ) {
                        _online.value =
                            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    }
                },
            )
        }

        private fun currentlyOnline(): Boolean {
            val capabilities = manager?.getNetworkCapabilities(manager.activeNetwork) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }
