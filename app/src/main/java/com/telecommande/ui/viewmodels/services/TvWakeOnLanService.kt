package com.telecommande.ui.viewmodels.services

import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class TvWakeOnLanService(private val application: Application) {
    private val TAG = "WoLService"

    suspend fun sendWakeOnLanPacket(macAddressString: String, broadcastIpString: String? = null) {
        val cleanMacAddress = macAddressString.replace(":", "").replace("-", "")
        if (cleanMacAddress.length != 12) {
            Log.e(TAG, "Format d'adresse MAC invalide pour WoL: $macAddressString")
            return
        }
        try {
            val macBytes = ByteArray(6)
            for (i in 0 until 6) {
                macBytes[i] = cleanMacAddress.substring(i * 2, (i * 2) + 2).toInt(16).toByte()
            }
            val bytes = ByteArray(6 + 16 * macBytes.size)
            for (i in 0 until 6) { bytes[i] = 0xff.toByte() }
            for (i in 6 until bytes.size step macBytes.size) {
                System.arraycopy(macBytes, 0, bytes, i, macBytes.size)
            }

            val targetBroadcastIp = broadcastIpString ?: getBroadcastAddress()
            val address = InetAddress.getByName(targetBroadcastIp)

            withContext(Dispatchers.IO) {
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    socket.send(DatagramPacket(bytes, bytes.size, address, 9))
                    Log.i(TAG, "Paquet WoL envoyé à $macAddressString via $targetBroadcastIp")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'envoi du paquet WoL à $macAddressString", e)
        }
    }

    @Suppress("DEPRECATION")
    fun getBroadcastAddress(): String {
        try {
            val wifiManager = application.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
            val dhcpInfo = wifiManager?.dhcpInfo
            if (dhcpInfo != null && dhcpInfo.ipAddress != 0) {
                val broadcast = (dhcpInfo.ipAddress and dhcpInfo.netmask) or dhcpInfo.netmask.inv()
                val quads = ByteArray(4)
                for (k in 0..3) quads[k] = (broadcast shr k * 8 and 0xFF).toByte()
                InetAddress.getByAddress(quads)?.hostAddress?.let {
                    if (it.isNotEmpty() && it != "0.0.0.0") {
                        Log.i(TAG, "Adresse de broadcast Wi-Fi détectée: $it")
                        return it
                    }
                }
            }
        } catch (e: Exception) { Log.e(TAG, "Erreur getBroadcastAddress", e) }
        Log.w(TAG, "Utilisation de l'adresse de broadcast par défaut: 255.255.255.255")
        return "255.255.255.255"
    }
}
