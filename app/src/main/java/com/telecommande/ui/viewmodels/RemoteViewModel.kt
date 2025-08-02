package com.telecommande.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.telecommande.core.AndroidRemoteContext
import com.telecommande.core.AndroidRemoteTv
import com.telecommande.core.AndroidTvListener
import com.telecommande.data.AppSettings
import com.telecommande.data.model.PairedTvInfo
import com.telecommande.ui.viewmodels.delegates.RemoteViewModelStateDelegate
import com.telecommande.ui.viewmodels.services.TvCommandSender
import com.telecommande.ui.viewmodels.services.TvConnectionManager
import com.telecommande.ui.viewmodels.services.TvPairingHandler
import com.telecommande.ui.viewmodels.services.TvWakeOnLanService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RemoteViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "RemoteViewModel"

    private val appSettings = AppSettings(application)
    private val coreAndroidRemoteTv = AndroidRemoteTv()

    private val stateDelegate = RemoteViewModelStateDelegate(viewModelScope, appSettings)
    private val wolService = TvWakeOnLanService(application)
    private val commandSender = TvCommandSender(coreAndroidRemoteTv, viewModelScope, stateDelegate.isConnected::value)

    private val _currentTargetTvInfo = MutableStateFlow<PairedTvInfo?>(null)
    val currentTargetTvInfo: StateFlow<PairedTvInfo?> = _currentTargetTvInfo.asStateFlow()

    private var wolAndConnectJob: Job? = null

    private val pairingHandler = TvPairingHandler(
        coreAndroidRemoteTv,
        AndroidRemoteContext.getInstance(),
        appSettings,
        viewModelScope,
        onPairingStateChange = { status, pinRequired -> stateDelegate.setPairingState(status, pinRequired) },
        onPairingError = { errorMessage, rawError ->
            val tvName = _currentTargetTvInfo.value?.name ?: "TV inconnue"
            val tvIp = _currentTargetTvInfo.value?.ipAddress ?: "IP inconnue"
            stateDelegate.handleDisconnectOrError(
                errorMessage, true,
                tvName,
                tvIp,
                rawError,
                deleteKeystoreAndReInitiatePairingCallback = { this.deleteKeystoreAndReInitiatePairing() },
                initiatePairingProcessCallback = { this.initiatePairingProcessForCurrentTv() }
            )
        },
        onPairingResetAndInitiate = { ipOfResetTv ->
            Log.d(TAG, "onPairingResetAndInitiate appelée pour IP: $ipOfResetTv")
            if (_currentTargetTvInfo.value?.ipAddress == ipOfResetTv) {
                initiatePairingProcessForCurrentTv()
            } else {
                Log.w(TAG, "onPairingResetAndInitiate pour $ipOfResetTv, mais currentTarget est ${_currentTargetTvInfo.value?.ipAddress}. Ne fait rien.")
            }
        }
    )

    private val connectionManager = TvConnectionManager(
        coreAndroidRemoteTv,
        viewModelScope,
        createTvListener(),
        onConnectionAttemptStart = { isPairing, tvIp ->
            val tvName = _currentTargetTvInfo.value?.name ?: "TV"
            if (!stateDelegate.pinRequired.value && !stateDelegate.isConnected.value &&
                stateDelegate.uiState.value != AppUiState.PAIRING &&
                stateDelegate.uiState.value != AppUiState.LOADING &&
                stateDelegate.uiState.value != AppUiState.CONNECTION_ERROR
            ) {
                if (!isPairing) {
                    stateDelegate.setLoadingState("Connexion à $tvName...")
                }
            }
        },
        onConnectionErrorCallback = { errorMessage, rawError ->
            val tvName = _currentTargetTvInfo.value?.name ?: "TV inconnue"
            val tvIp = _currentTargetTvInfo.value?.ipAddress ?: "IP inconnue"
            stateDelegate.handleDisconnectOrError(
                errorMessage, true,
                tvName,
                tvIp,
                rawError,
                deleteKeystoreAndReInitiatePairingCallback = { this.deleteKeystoreAndReInitiatePairing() },
                initiatePairingProcessCallback = { this.initiatePairingProcessForCurrentTv() }
            )
        }
    )

    val uiState: StateFlow<AppUiState> = stateDelegate.uiState
    val connectionStatus: StateFlow<String> = stateDelegate.connectionStatus
    val pinRequired: StateFlow<Boolean> = stateDelegate.pinRequired
    val currentPin: StateFlow<String> = stateDelegate.currentPin
    val isConnected: StateFlow<Boolean> = stateDelegate.isConnected

    init {
        Log.i(TAG, "RemoteViewModel Initializing...")
        viewModelScope.launch {
            val activeTv = appSettings.getActiveTvInfo()
            if (activeTv != null) {
                _currentTargetTvInfo.value = activeTv
                Log.d(TAG, "TV active chargée depuis AppSettings: ${activeTv.name} (${activeTv.ipAddress})")
                if (pairingHandler.isPotentiallyPaired(activeTv.ipAddress)) {
                    Log.d(TAG, "Keystore trouvé. Tentative de connexion auto.")
                    attemptTurnOnTvAndConnect(isAutoAttempt = true)
                } else {
                    Log.d(TAG, "Aucun Keystore pour ${activeTv.name}. Lancement appairage auto.")
                    initiatePairingProcessForCurrentTv()
                }
            } else {
                Log.d(TAG, "Aucune TV active trouvée. En attente de sélection.")
                stateDelegate.setDisconnectedState(
                    "Aucune TV configurée. Allez dans 'Gérer les TVs'.",
                    AppUiState.NO_TV_CONFIGURED
                )
            }
        }
    }

    private fun initiatePairingProcessForCurrentTv() {
        val tvInfo = _currentTargetTvInfo.value
        if (tvInfo == null) {
            Log.w(TAG, "initiatePairing: Aucune TV cible.")
            stateDelegate.setDisconnectedState("Sélectionnez une TV pour l'appairage.", AppUiState.NO_TV_CONFIGURED)
            return
        }
        Log.d(TAG, "(Ré)initiation appairage pour ${tvInfo.name} (${tvInfo.ipAddress}).")
        wolAndConnectJob?.cancel()
        connectionManager.cancelConnectionJob()
        pairingHandler.prepareForPairing(tvInfo.name ?: "TV Android")
        connectionManager.connect(tvInfo.ipAddress, isInitialPairingAttempt = true)
    }

    private fun initiateConnectionOrPairingForCurrentTv() {
        val tvInfo = _currentTargetTvInfo.value
        if (tvInfo == null) {
            Log.e(TAG, "Aucune TV cible pour connexion/appairage.")
            stateDelegate.setDisconnectedState("Aucune TV sélectionnée.", AppUiState.NO_TV_CONFIGURED)
            return
        }
        Log.d(TAG, "Initiation connexion/appairage pour : ${tvInfo.name} (${tvInfo.ipAddress})")
        wolAndConnectJob?.cancel()
        connectionManager.cancelConnectionJob()
        if (pairingHandler.isPotentiallyPaired(tvInfo.ipAddress)) {
            Log.d(TAG, "Keystore trouvé. Tentative connexion.")
            attemptTurnOnTvAndConnect(isAutoAttempt = false)
        } else {
            Log.d(TAG, "Aucun Keystore. (Ré)initiation appairage.")
            initiatePairingProcessForCurrentTv()
        }
    }

    private fun createTvListener(): AndroidTvListener {
        return object : AndroidTvListener {
            override fun onSecretRequested() {
                viewModelScope.launch {
                    val tvName = _currentTargetTvInfo.value?.name ?: "la TV"
                    Log.d(TAG, "PIN requis par $tvName.")
                    stateDelegate.setPairingState("PIN requis par $tvName", pinIsRequiredEvt = true)
                }
            }

            override fun onConnected() {
                viewModelScope.launch {
                    val tv = _currentTargetTvInfo.value
                    if (tv == null) { Log.e(TAG, "onConnected mais tv est null"); return@launch }
                    if (stateDelegate.isAwaitingConnectionAfterPairing.value) {
                        Log.d(TAG, "Appairage finalisé et connexion établie avec ${tv.name}.")
                    }
                    stateDelegate.setConnectedState(tv.name ?: "TV Connectée", tv.ipAddress)
                }
            }

            override fun onPaired() {
                viewModelScope.launch {
                    val tv = _currentTargetTvInfo.value
                    if (tv == null) { Log.e(TAG, "onPaired mais tv est null"); return@launch }
                    Log.d(TAG, "Listener: onPaired pour ${tv.name}.")
                    stateDelegate.handlePaired(tv.name ?: "TV") {
                        if (!stateDelegate.isConnected.value && !connectionManager.isConnectJobActive()) {
                            Log.d(TAG, "onPaired (callback): Pin accepté, lancement connexion finale.")
                            connectionManager.connect(tv.ipAddress, isInitialPairingAttempt = false)
                        } else if (stateDelegate.isConnected.value) {
                            Log.d(TAG, "onPaired (callback): Déjà connecté.")
                        } else {
                            Log.d(TAG, "onPaired (callback): Connexion déjà en cours.")
                        }
                    }
                }
            }

            override fun onConnectingToRemote() {
                Log.d(TAG,"Listener: onConnectingToRemote pour ${_currentTargetTvInfo.value?.name ?: "TV"}...")
            }

            override fun onDisconnect() {
                viewModelScope.launch {
                    Log.w(TAG, "Listener: onDisconnect.")
                    val tvName = _currentTargetTvInfo.value?.name ?: "la TV"
                    val tvIp = _currentTargetTvInfo.value?.ipAddress ?: "IP inconnue"
                    stateDelegate.handleDisconnectOrError(
                        "Déconnexion de $tvName.", false,
                        tvName, tvIp, null,
                        deleteKeystoreAndReInitiatePairingCallback = { deleteKeystoreAndReInitiatePairing() },
                        initiatePairingProcessCallback = { initiatePairingProcessForCurrentTv() }
                    )
                }
            }

            override fun onError(error: String) {
                viewModelScope.launch {
                    Log.e(TAG, "Listener: onError - $error")
                    val tvName = _currentTargetTvInfo.value?.name ?: "la TV"
                    val tvIp = _currentTargetTvInfo.value?.ipAddress ?: "IP inconnue"
                    stateDelegate.handleDisconnectOrError(
                        "Erreur: ${error.take(100)}", true,
                        tvName, tvIp, error,
                        deleteKeystoreAndReInitiatePairingCallback = { deleteKeystoreAndReInitiatePairing() },
                        initiatePairingProcessCallback = { initiatePairingProcessForCurrentTv() }
                    )
                }
            }

            override fun onSessionCreated() {
                Log.d(TAG,"Listener: onSessionCreated (non utilisé pour l'état UI principal)")
            }
        }
    }

    fun retryConnectionOrInitiatePairing() {
        Log.d(TAG, "Demande de reconnexion ou d'initiation d'appairage.")
        val tvInfo = _currentTargetTvInfo.value
        if (tvInfo == null) {
            Log.w(TAG, "retry: Aucune TV cible.")
            stateDelegate.setDisconnectedState("Aucune TV sélectionnée.", AppUiState.NO_TV_CONFIGURED)
            return
        }
        initiateConnectionOrPairingForCurrentTv()
    }

    private fun attemptTurnOnTvAndConnect(isAutoAttempt: Boolean = false) {
        val tvInfo = _currentTargetTvInfo.value
        if (tvInfo == null) {
            if (!isAutoAttempt) stateDelegate.setDisconnectedState("Sélectionnez une TV.", AppUiState.NO_TV_CONFIGURED)
            return
        }
        if (stateDelegate.isConnected.value && wolAndConnectJob?.isActive != true && !isAutoAttempt) {
            return
        }
        if (wolAndConnectJob?.isActive == true) return

        wolAndConnectJob = viewModelScope.launch {
            val tvName = tvInfo.name ?: "TV"
            val tvIp = tvInfo.ipAddress
            val tvMac = tvInfo.macAddress
            if (tvMac.isNullOrBlank()) {
                if (!isAutoAttempt) stateDelegate.setLoadingState("MAC non définie. Connexion directe...")
                connectionManager.connect(tvIp, isInitialPairingAttempt = false, isAutoAttemptAfterWoL = isAutoAttempt)
                return@launch
            }
            if (!isAutoAttempt) stateDelegate.setLoadingState("Allumage de $tvName (WoL)...")
            wolService.sendWakeOnLanPacket(tvMac)
            val delayMs = if (isAutoAttempt) 5000L else 10000L
            if (!isAutoAttempt) stateDelegate.setLoadingState("Attente démarrage $tvName (${delayMs/1000}s)...")
            delay(delayMs)
            if (stateDelegate.pinRequired.value || stateDelegate.isConnected.value) return@launch
            if (!isAutoAttempt) stateDelegate.setLoadingState("Connexion à $tvName après WoL...")
            connectionManager.connect(tvIp, isInitialPairingAttempt = false, isAutoAttemptAfterWoL = isAutoAttempt)
        }
    }

    fun sendPowerKeyPress() {
        viewModelScope.launch {
            if (stateDelegate.isConnected.value) {
                Log.i(TAG, "Envoi de KEYCODE_POWER (TV connectée: ${_currentTargetTvInfo.value?.name})")
                commandSender.sendPower()
            } else {
                Log.w(TAG, "sendPowerKeyPress: Non connecté. Tentative d'allumage/reconnexion pour ${_currentTargetTvInfo.value?.name}.")
                attemptTurnOnTvAndConnect(isAutoAttempt = false)
            }
        }
    }

    fun sendVolumeUp() = if (isConnected.value) commandSender.sendVolumeUp() else Log.w(TAG, "sendVolumeUp: Non connecté")
    fun sendVolumeDown() = if (isConnected.value) commandSender.sendVolumeDown() else Log.w(TAG, "sendVolumeDown: Non connecté")
    fun sendHome() = if (isConnected.value) commandSender.sendHome() else Log.w(TAG, "sendHome: Non connecté")
    fun sendBack() = if (isConnected.value) commandSender.sendBack() else Log.w(TAG, "sendBack: Non connecté")
    fun sendDpadUp() = if (isConnected.value) commandSender.sendDpadUp() else Log.w(TAG, "sendDpadUp: Non connecté")
    fun sendDpadDown() = if (isConnected.value) commandSender.sendDpadDown() else Log.w(TAG, "sendDpadDown: Non connecté")
    fun sendDpadLeft() = if (isConnected.value) commandSender.sendDpadLeft() else Log.w(TAG, "sendDpadLeft: Non connecté")
    fun sendDpadRight() = if (isConnected.value) commandSender.sendDpadRight() else Log.w(TAG, "sendDpadRight: Non connecté")
    fun sendDpadCenter() = if (isConnected.value) commandSender.sendDpadCenter() else Log.w(TAG, "sendDpadCenter: Non connecté")
    fun sendMute() = if (isConnected.value) commandSender.sendMute() else Log.w(TAG, "sendMute: Non connecté")
    fun sendChannelUp() = if (isConnected.value) commandSender.sendChannelUp() else Log.w(TAG, "sendChannelUp: Non connecté")
    fun sendChannelDown() = if (isConnected.value) commandSender.sendChannelDown() else Log.w(TAG, "sendChannelDown: Non connecté")
    fun sendRewindBack() = if (isConnected.value) commandSender.sendRewindBack() else Log.w(TAG, "sendRewindBack: Non connecté")
    fun sendRewindForward() = if (isConnected.value) commandSender.sendRewindForward() else Log.w(TAG, "sendRewindForward: Non connecté")
    fun sendPlay() = if (isConnected.value) commandSender.sendPlay() else Log.w(TAG, "sendPlay: Non connecté")
    fun sendStop() = if (isConnected.value) commandSender.sendStop() else Log.w(TAG, "sendStop: Non connecté")

    fun updateCurrentPin(newPin: String) { stateDelegate.updateCurrentPin(newPin) }

    fun submitPin() {
        val tvInfo = _currentTargetTvInfo.value
        if (tvInfo == null) {
            stateDelegate.setPairingState("Erreur: Aucune TV pour l'appairage.", false); return
        }
        pairingHandler.submitPin(stateDelegate.currentPin.value, tvInfo.name ?: "TV", tvInfo.ipAddress)
    }

    fun deleteKeystoreAndReInitiatePairing() {
        val tvInfo = _currentTargetTvInfo.value
        val ipToUse = tvInfo?.ipAddress
        if (ipToUse == null) {
            stateDelegate.setDisconnectedState("Aucune TV pour réinit. appairage.", AppUiState.NO_TV_CONFIGURED); return
        }
        Log.i(TAG, "Suppression Keystore et réinit. appairage pour $ipToUse.")
        wolAndConnectJob?.cancel()
        connectionManager.cancelConnectionJob()
        pairingHandler.deleteKeystoreAndReInitiatePairingFullProcess(
            ipOfTvToReset = ipToUse,
            updateCurrentTargetTvInfoCallback = { updatedTvInfo ->
                if (_currentTargetTvInfo.value?.ipAddress == updatedTvInfo.ipAddress || _currentTargetTvInfo.value == null) {
                    _currentTargetTvInfo.value = updatedTvInfo
                }
            },
            defaultTvName = tvInfo.name ?: "TV Android",
            tvMacAddress = tvInfo.macAddress
        )
    }

    override fun onCleared() {
        super.onCleared()
        wolAndConnectJob?.cancel()
        connectionManager.disconnect()
    }

    fun userCancelledPinEntry() {
        viewModelScope.launch {
            connectionManager.cancelConnectionJob()
            val currentTvName = _currentTargetTvInfo.value?.name
            if (currentTvName == null) {
                stateDelegate.setDisconnectedState("Appairage annulé.", AppUiState.NO_TV_CONFIGURED)
            } else {
                stateDelegate.setDisconnectedState("Appairage annulé pour $currentTvName.", AppUiState.PAIRING_NEEDED)
            }
        }
    }

    fun setActiveTv(tvInfo: PairedTvInfo) {
        viewModelScope.launch {
            Log.i(TAG, "setActiveTv: Définition de ${tvInfo.name} (${tvInfo.ipAddress}) comme TV active.")
            if (stateDelegate.isConnected.value && _currentTargetTvInfo.value?.ipAddress != tvInfo.ipAddress) {
                Log.d(TAG, "setActiveTv: Déconnexion de la TV précédente.")
                connectionManager.disconnect()
                stateDelegate.setLoadingState("Changement de TV...")
                delay(100)
            }

            appSettings.saveActiveTvInfo(tvInfo)
            _currentTargetTvInfo.value = tvInfo

            wolAndConnectJob?.cancel()
            connectionManager.cancelConnectionJob()

            if (pairingHandler.isPotentiallyPaired(tvInfo.ipAddress)) {
                Log.d(TAG, "Keystore trouvé. Tentative de connexion.")
                attemptTurnOnTvAndConnect(isAutoAttempt = false)
            } else {
                Log.d(TAG, "Aucun Keystore. Lancement appairage.")
                initiatePairingProcessForCurrentTv()
            }
        }
    }

    fun resetCurrentConnectionTarget() {
        viewModelScope.launch {
            Log.d(TAG, "Réinitialisation de la cible de connexion actuelle.")
            if (_currentTargetTvInfo.value != null) {
                _currentTargetTvInfo.value = null
                connectionManager.disconnect()
                stateDelegate.setDisconnectedState(
                    "Aucune TV ciblée pour la connexion. Sélectionnez-en une.",
                    AppUiState.NO_TV_CONFIGURED
                )
            }
        }
    }
}