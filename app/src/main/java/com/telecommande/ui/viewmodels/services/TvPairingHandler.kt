package com.telecommande.ui.viewmodels.services

import android.util.Log
import com.telecommande.core.AndroidRemoteContext
import com.telecommande.core.AndroidRemoteTv
import com.telecommande.data.AppSettings
import com.telecommande.data.model.PairedTvInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TvPairingHandler(
    private val androidRemoteTv: AndroidRemoteTv,
    private val remoteContext: AndroidRemoteContext,
    private val appSettings: AppSettings,
    private val coroutineScope: CoroutineScope,
    private val onPairingStateChange: (status: String, pinRequired: Boolean) -> Unit,
    private val onPairingError: (errorMessage: String, rawError: String?) -> Unit,
    private val onPairingResetAndInitiate: (ip: String) -> Unit
) {
    private val TAG = "PairingHandler"

    fun isPotentiallyPaired(tvIpAddress: String): Boolean {
        val keystoreFile = remoteContext.keyStoreFile
        Log.d(TAG, "isPotentiallyPaired pour $tvIpAddress. Fichier Keystore global: ${keystoreFile?.absolutePath}, Existe: ${keystoreFile?.exists()}, Longueur: ${keystoreFile?.length()}")
        return keystoreFile?.exists() == true && keystoreFile.length() > 0
    }

    fun submitPin(pin: String, tvName: String, tvIp: String) {
        if (pin.isBlank()) {
            onPairingStateChange("Le PIN ne peut pas être vide.", true)
            return
        }
        onPairingStateChange("Envoi du PIN à $tvName...", true)
        Log.d(TAG, "Envoi du PIN : ${pin.length} chiffres à $tvName ($tvIp)")

        coroutineScope.launch(Dispatchers.IO) {
            try {
                androidRemoteTv.sendSecret(pin)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de l'appel à androidRemoteTv.sendSecret: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onPairingError("Erreur envoi PIN: ${e.localizedMessage}", e.message ?: e.toString())
                }
            }
        }
    }

    fun deleteKeystoreAndReInitiatePairingFullProcess(
        ipOfTvToReset: String,
        updateCurrentTargetTvInfoCallback: (PairedTvInfo) -> Unit,
        defaultTvName: String,
        tvMacAddress: String?
    ) {
        coroutineScope.launch {
            try {
                val keystoreFile = remoteContext.keyStoreFile
                if (keystoreFile?.exists() == true) {
                    withContext(Dispatchers.IO) {
                        if (keystoreFile.delete()) Log.i(TAG, "Keystore (global) supprimé avec succès pour réinitialisation de $ipOfTvToReset.")
                        else Log.e(TAG, "Échec de la suppression du fichier Keystore (global) pour $ipOfTvToReset.")
                    }
                } else {
                    Log.i(TAG, "Aucun Keystore (global) à supprimer pour $ipOfTvToReset.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception lors de la tentative de suppression du Keystore (global) pour $ipOfTvToReset: ${e.message}", e)
            }

            val currentActiveTvIp = appSettings.activeTvIpFlow.firstOrNull()
            if (currentActiveTvIp == ipOfTvToReset) {
                Log.d(TAG, "La TV réinitialisée ($ipOfTvToReset) était la TV active. Effacement des infos actives.")
                appSettings.saveActiveTvInfo(null)
            }
            updateCurrentTargetTvInfoCallback(PairedTvInfo(ipOfTvToReset, defaultTvName, tvMacAddress))

            Log.i(TAG, "Appairage réinitialisé. Prêt pour un nouvel appairage avec $ipOfTvToReset.")
            onPairingResetAndInitiate(ipOfTvToReset)
        }
    }

    fun prepareForPairing(tvName: String) {
        onPairingStateChange("Lancement de l'appairage avec $tvName...", false)
    }
}
