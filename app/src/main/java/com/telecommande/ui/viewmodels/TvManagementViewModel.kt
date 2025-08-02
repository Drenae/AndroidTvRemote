package com.telecommande.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.telecommande.core.discovery.DiscoveredTv
import com.telecommande.core.discovery.TvDiscoveryListener
import com.telecommande.core.discovery.TvDiscoveryManager
import com.telecommande.data.AppSettings
import com.telecommande.data.model.PairedTvInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TvManagementViewModel(
    application: Application,
    private val appSettings: AppSettings,
    private val remoteViewModel: RemoteViewModel
) : AndroidViewModel(application), TvDiscoveryListener {

    private val TAG = "TvManagementViewModel"

    private val tvDiscoveryManager: TvDiscoveryManager = TvDiscoveryManager(application, this)
    private val currentDiscoveredTvsMap = mutableMapOf<String, DiscoveredTv>()

    private val _discoveredTvs = MutableStateFlow<List<DiscoveredTv>>(emptyList())
    val discoveredTvs: StateFlow<List<DiscoveredTv>> = _discoveredTvs.asStateFlow()

    private val _discoveryStatusMessage = MutableStateFlow("Prêt à rechercher.")
    val discoveryStatusMessage: StateFlow<String> = _discoveryStatusMessage.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    val pairedTvs: StateFlow<List<PairedTvInfo>> = appSettings.allPairedTvsFlow
        .map { list ->
            Log.d(TAG, "Mise à jour de la liste des TVs appairées: ${list.size} TV(s)")
            list
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeTvIp: StateFlow<String?> = remoteViewModel.currentTargetTvInfo
        .map { it?.ipAddress }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun setActiveTv(tvInfo: PairedTvInfo) {
        Log.d(TAG, "TvManagementViewModel: Demande de définition de ${tvInfo.name} comme TV active.")
        remoteViewModel.setActiveTv(tvInfo)
        stopTvDiscovery()
    }

    fun renameTv(tvInfo: PairedTvInfo, newName: String) {
        if (newName.isBlank()) {
            Log.w(TAG, "Le nouveau nom pour ${tvInfo.ipAddress} ne peut pas être vide.")
            return
        }
        viewModelScope.launch {
            Log.d(TAG, "Renommage de ${tvInfo.ipAddress} en '$newName'")
            appSettings.renamePairedTv(tvInfo.ipAddress, newName)

            val currentRemoteTarget = remoteViewModel.currentTargetTvInfo.value
            if (currentRemoteTarget?.ipAddress == tvInfo.ipAddress) {
                val updatedTvInfoForRemote = currentRemoteTarget.copy(name = newName)
                remoteViewModel.setActiveTv(updatedTvInfoForRemote)
            }
        }
    }

    fun removeTv(tvInfo: PairedTvInfo) {
        viewModelScope.launch {
            Log.d(TAG, "Suppression de la TV : ${tvInfo.name} (${tvInfo.ipAddress})")

            val remoteTargetIpBeforeRemove = remoteViewModel.currentTargetTvInfo.value?.ipAddress
            val wasRemoteTarget = remoteTargetIpBeforeRemove == tvInfo.ipAddress

            appSettings.removePairedTv(tvInfo.ipAddress)

            if (wasRemoteTarget) {
                Log.d(TAG, "La TV cible de RemoteViewModel ${tvInfo.name} a été supprimée. Réinitialisation dans RemoteViewModel.")
                remoteViewModel.resetCurrentConnectionTarget()
            }
            Log.i(TAG, "TV ${tvInfo.name} supprimée de la liste.")
        }
    }

    fun startTvDiscovery() {
        if (_isDiscovering.value) return
        Log.d(TAG, "Lancement de la découverte de TV.")
        currentDiscoveredTvsMap.clear()
        _discoveredTvs.value = emptyList()
        _isDiscovering.value = true
        tvDiscoveryManager.startDiscovery()
    }

    fun stopTvDiscovery() {
        if (!_isDiscovering.value) return
        Log.d(TAG, "Arrêt de la découverte de TV.")
        tvDiscoveryManager.stopDiscovery()
    }

    fun selectDiscoveredTv(discoveredTv: DiscoveredTv) {
        Log.i(TAG, "TV découverte sélectionnée: ${discoveredTv.friendlyName} - ${discoveredTv.ipAddress}")
        stopTvDiscovery()

        val newPairedTv = PairedTvInfo(
            ipAddress = discoveredTv.ipAddress,
            name = discoveredTv.friendlyName,
            macAddress = null // TODO: Voir si le MAC peut être obtenu ici
        )

        viewModelScope.launch {
            appSettings.addOrUpdatePairedTv(newPairedTv)
            remoteViewModel.setActiveTv(newPairedTv)
            Log.d(TAG, "${newPairedTv.name} sélectionnée, ajoutée et définie comme active.")
        }
        _discoveryStatusMessage.value = "Préparation de ${newPairedTv.name}..."
    }

    override fun onDiscoveryStarted() {
        Log.d(TAG, "Listener: Discovery Started")
        viewModelScope.launch {
            _discoveryStatusMessage.value = "Recherche de TV en cours..."
            _isDiscovering.value = true
            currentDiscoveredTvsMap.clear()
            _discoveredTvs.value = emptyList()
        }
    }

    override fun onDiscoveryStopped() {
        Log.d(TAG, "Listener: Discovery Stopped")
        viewModelScope.launch {
            _discoveryStatusMessage.value = if (_discoveredTvs.value.isEmpty()) {
                "Recherche terminée. Aucune TV trouvée."
            } else {
                "Recherche terminée. ${_discoveredTvs.value.size} TV(s) trouvée(s)."
            }
            _isDiscovering.value = false
        }
    }

    override fun onTvFound(tv: DiscoveredTv) {
        Log.i(TAG, "Listener: TV Found - Name: ${tv.friendlyName}, IP: ${tv.ipAddress}")
        viewModelScope.launch {
            if (!currentDiscoveredTvsMap.containsKey(tv.serviceName)) {
                currentDiscoveredTvsMap[tv.serviceName] = tv
                _discoveredTvs.value = ArrayList(currentDiscoveredTvsMap.values)
            }
            _discoveryStatusMessage.value = "${_discoveredTvs.value.size} TV(s) trouvée(s)..."
        }
    }

    override fun onTvLost(tv: DiscoveredTv) {
        Log.i(TAG, "Listener: TV Lost - Name: ${tv.friendlyName ?: tv.serviceName}")
        viewModelScope.launch {
            if (currentDiscoveredTvsMap.remove(tv.serviceName) != null) {
                _discoveredTvs.value = ArrayList(currentDiscoveredTvsMap.values)
            }
            if (currentDiscoveredTvsMap.isEmpty()) {
                _discoveryStatusMessage.value = if (_isDiscovering.value) "Recherche de TV en cours..." else "Aucune TV actuellement visible."
            }
        }
    }

    override fun onDiscoveryError(message: String, errorCode: Int) {
        Log.e(TAG, "Listener: Discovery Error - $message (Code: $errorCode)")
        viewModelScope.launch {
            _discoveryStatusMessage.value = "Erreur de découverte: $message"
            _isDiscovering.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "TvManagementViewModel onCleared. Arrêt de la découverte.")
        tvDiscoveryManager.stopDiscovery()
    }

    companion object {
        fun provideFactory(
            application: Application,
            appSettings: AppSettings,
            remoteViewModel: RemoteViewModel
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TvManagementViewModel::class.java)) {
                    return TvManagementViewModel(application, appSettings, remoteViewModel) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
