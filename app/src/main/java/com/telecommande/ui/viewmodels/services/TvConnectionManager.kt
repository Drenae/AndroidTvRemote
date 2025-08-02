package com.telecommande.ui.viewmodels.services

import android.util.Log
import com.telecommande.core.AndroidRemoteTv
import com.telecommande.core.AndroidTvListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TvConnectionManager(
    private val androidRemoteTv: AndroidRemoteTv,
    private val coroutineScope: CoroutineScope,
    private val tvListener: AndroidTvListener,
    private val onConnectionAttemptStart: (isPairing: Boolean, tvIp: String) -> Unit,
    private val onConnectionErrorCallback: (errorMessage: String, rawError: String?) -> Unit
) {
    private val TAG = "ConnectionManager"
    private var connectJob: Job? = null

    fun isConnectJobActive(): Boolean = connectJob?.isActive == true

    fun connect(
        tvIpAddress: String,
        isInitialPairingAttempt: Boolean = false,
        isAutoAttemptAfterWoL: Boolean = false
    ) {
        if (connectJob?.isActive == true) {
            Log.d(TAG, "Job de connexion précédent actif, annulation pour nouvelle tentative vers $tvIpAddress.")
            connectJob?.cancel()
        }

        onConnectionAttemptStart(isInitialPairingAttempt, tvIpAddress)
        Log.d(TAG, "Lancement de androidRemoteTv.connect($tvIpAddress) | isInitialPairing: $isInitialPairingAttempt | isAuto: $isAutoAttemptAfterWoL")

        connectJob = coroutineScope.launch(Dispatchers.IO) {
            try {
                androidRemoteTv.connect(tvIpAddress, tvListener)
            } catch (e: Exception) {
                Log.e(TAG, "Exception dans androidRemoteTv.connect($tvIpAddress): ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onConnectionErrorCallback(
                        "Échec connexion: ${e.localizedMessage ?: "Erreur inconnue"}",
                        e.message ?: e.toString()
                    )
                }
            }
        }
    }

    fun disconnect() {
        Log.d(TAG, "Demande de déconnexion...")
        connectJob?.cancel()
        coroutineScope.launch(Dispatchers.IO) {
            try {
                androidRemoteTv.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la déconnexion explicite: ${e.message}", e)
            }
        }
    }

    fun cancelConnectionJob() {
        if (connectJob?.isActive == true) {
            Log.d(TAG, "Annulation du job de connexion en cours.")
            connectJob?.cancel()
        }
        connectJob = null
    }
}
