package com.telecommande.ui.viewmodels.delegates

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.telecommande.data.AppSettings
import com.telecommande.ui.viewmodels.AppUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RemoteViewModelStateDelegate(
    private val viewModelScope: CoroutineScope,
    private val appSettings: AppSettings
) {
    private val TAG = "StateDelegate"

    private val _uiState = MutableStateFlow(AppUiState.LOADING)
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Initialisation...")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _pinRequired = MutableStateFlow(false)
    val pinRequired: StateFlow<Boolean> = _pinRequired.asStateFlow()

    private val _currentPin = MutableStateFlow("")
    val currentPin: StateFlow<String> = _currentPin.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    var isAwaitingConnectionAfterPairing = mutableStateOf(false)

    fun setLoadingState(message: String = "Chargement...") {
        _uiState.value = AppUiState.LOADING
        _connectionStatus.value = message
        _isConnected.value = false
    }

    fun setPairingState(statusMessage: String, pinIsRequiredEvt: Boolean = true) {
        _uiState.value = AppUiState.PAIRING
        _connectionStatus.value = statusMessage
        _pinRequired.value = pinIsRequiredEvt
        _isConnected.value = false
        if (!pinIsRequiredEvt) {
            _currentPin.value = ""
        }
        isAwaitingConnectionAfterPairing.value = false
    }

    fun setConnectedState(tvName: String, tvIp: String) {
        Log.d(TAG, "Connecté à $tvName ($tvIp).")
        _connectionStatus.value = "Connecté à $tvName"
        _isConnected.value = true
        _pinRequired.value = false
        _currentPin.value = ""
        _uiState.value = AppUiState.REMOTE_CONTROL
        isAwaitingConnectionAfterPairing.value = false
        viewModelScope.launch { appSettings.savePairedTvName(tvName) }
    }

    fun setDisconnectedState(statusMessage: String, targetUiState: AppUiState = AppUiState.CONNECTION_ERROR) {
        _isConnected.value = false
        _connectionStatus.value = statusMessage
        isAwaitingConnectionAfterPairing.value = false
        if (_uiState.value != AppUiState.PAIRING || targetUiState == AppUiState.PAIRING) {
            _uiState.value = targetUiState
        }
    }

    fun updateCurrentPin(newPin: String) {
        _currentPin.value = newPin
    }

    fun handlePaired(tvName: String, onPairedContinuation: () -> Unit) {
        Log.d(TAG, "PIN accepté pour $tvName. En attente de connexion finale.")
        _pinRequired.value = false
        _currentPin.value = ""
        _connectionStatus.value = "PIN accepté. Connexion en cours..."
        isAwaitingConnectionAfterPairing.value = true
        onPairedContinuation()
    }

    fun handleDisconnectOrError(
        logOrUserMessage: String,
        isActualError: Boolean,
        tvName: String,
        tvIp: String,
        rawError: String? = null,
        deleteKeystoreAndReInitiatePairingCallback: () -> Unit,
        initiatePairingProcessCallback: () -> Unit
    ) {
        Log.e(TAG, "Problème avec $tvName ($tvIp): $logOrUserMessage. RawError: $rawError")
        isAwaitingConnectionAfterPairing.value = false
        _isConnected.value = false

        if (rawError != null) {
            when {
                rawError.containsAnyOf(listOf("SSL", "certificate", "EACCES", "trust anchor"), ignoreCase = true) -> {
                    Log.w(TAG, "Erreur SSL/Certificat détectée: $rawError. Réinitialisation de l'appairage.")
                    _connectionStatus.value = "Problème d'appairage SSL. Réinitialisation..."
                    deleteKeystoreAndReInitiatePairingCallback()
                    return
                }
                rawError.containsAnyOf(listOf("Pairing Error", "Secret incorrect", "Pin verification failed", "Pairing failed", "BadPaddingException"), ignoreCase = true) -> {
                    _connectionStatus.value = "PIN incorrect ou erreur d'appairage."
                    _currentPin.value = ""
                    setPairingState("PIN incorrect. Veuillez réessayer.", pinIsRequiredEvt = true)
                    return
                }
                rawError.contains("Le PIN ne peut pas être vide", ignoreCase = true) -> {
                    setPairingState(rawError, pinIsRequiredEvt = true)
                    return
                }
                rawError.containsAnyOf(listOf("failed to connect", "timeout", "ETIMEDOUT", "ECONNREFUSED", "No route to host", "EHOSTUNREACH"), ignoreCase = true) -> {
                    val message = "TV non joignable ($tvIp)."
                    _connectionStatus.value = message + if (_uiState.value == AppUiState.PAIRING) " Vérifiez la TV et le réseau." else ""
                    if (_uiState.value != AppUiState.PAIRING) {
                        _uiState.value = AppUiState.CONNECTION_ERROR
                    }
                    return
                }
            }
        }

        val displayMessage = if (isActualError) {
            val conciseError = rawError?.substringBefore('\n')?.take(80) ?: logOrUserMessage.take(100)
            "Erreur: $conciseError"
        } else {
            logOrUserMessage
        }
        setDisconnectedState(displayMessage)
    }

    private fun String.containsAnyOf(keywords: List<String>, ignoreCase: Boolean = false): Boolean {
        return keywords.any { this.contains(it, ignoreCase) }
    }
}
