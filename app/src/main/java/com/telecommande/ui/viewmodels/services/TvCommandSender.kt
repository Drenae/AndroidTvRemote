package com.telecommande.ui.viewmodels.services

import android.util.Log
import com.telecommande.core.AndroidRemoteTv
import com.telecommande.core.remote.Remotemessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TvCommandSender(
    private val androidRemoteTv: AndroidRemoteTv,
    private val coroutineScope: CoroutineScope,
    private val isConnectedChecker: () -> Boolean
) {
    private val TAG = "CommandSender"

    fun sendCommand(keyCode: Remotemessage.RemoteKeyCode, action: Remotemessage.RemoteDirection = Remotemessage.RemoteDirection.SHORT) {
        if (!isConnectedChecker()) {
            Log.w(TAG, "Impossible d'envoyer ${keyCode.name}: non connecté.")
            return
        }
        coroutineScope.launch(Dispatchers.IO) {
            try {
                androidRemoteTv.sendCommand(keyCode, action)
                Log.d(TAG, "Commande ${keyCode.name} envoyée.")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de l'envoi de la commande ${keyCode.name}", e)
            }
        }
    }

    fun sendPower() = sendCommand(Remotemessage.RemoteKeyCode.KEYCODE_POWER)
    fun sendVolumeUp() = sendCommand(Remotemessage.RemoteKeyCode.KEYCODE_VOLUME_UP)
    fun sendVolumeDown() = sendCommand(Remotemessage.RemoteKeyCode.KEYCODE_VOLUME_DOWN)
    fun sendHome() = sendCommand(Remotemessage.RemoteKeyCode.KEYCODE_HOME)
    fun sendBack() = sendCommand(Remotemessage.RemoteKeyCode.KEYCODE_BACK)
    fun sendDpadUp() = sendCommand(Remotemessage.RemoteKeyCode.KEYCODE_DPAD_UP)
    fun sendDpadDown() = sendCommand(Remotemessage.RemoteKeyCode.KEYCODE_DPAD_DOWN)
    fun sendDpadLeft() = sendCommand(Remotemessage.RemoteKeyCode.KEYCODE_DPAD_LEFT)
    fun sendDpadRight() = sendCommand(Remotemessage.RemoteKeyCode.KEYCODE_DPAD_RIGHT)
    fun sendDpadCenter() = sendCommand(Remotemessage.RemoteKeyCode.KEYCODE_DPAD_CENTER)
    fun sendMute() = sendCommand(Remotemessage.RemoteKeyCode.KEYCODE_VOLUME_MUTE)
    fun sendChannelUp() = sendCommand(Remotemessage.RemoteKeyCode.KEYCODE_CHANNEL_UP)
    fun sendChannelDown() = sendCommand(Remotemessage.RemoteKeyCode.KEYCODE_CHANNEL_DOWN)
    fun sendRewindBack() = sendCommand(Remotemessage.RemoteKeyCode.KEYCODE_MEDIA_REWIND)
    fun sendRewindForward() = sendCommand(Remotemessage.RemoteKeyCode.KEYCODE_MEDIA_FAST_FORWARD)
    fun sendPlay() = sendCommand(Remotemessage.RemoteKeyCode.KEYCODE_MEDIA_PLAY)
    fun sendStop() = sendCommand(Remotemessage.RemoteKeyCode.KEYCODE_MEDIA_STOP)
}