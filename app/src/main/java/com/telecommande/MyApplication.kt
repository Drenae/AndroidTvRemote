package com.telecommande

import android.app.Application
import android.util.Log
import com.telecommande.core.AndroidRemoteContext
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.security.Security

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        try {
            Security.removeProvider("BC")
            Security.insertProviderAt(BouncyCastleProvider(), 1)
            Log.i("MyApplication", "Fournisseur BouncyCastle inséré.")
            Log.i("MyApplication", "BC Provider version: ${Security.getProvider("BC")?.version}")
        } catch (e: Exception) {
            Log.e("MyApplication", "Erreur lors de l'insertion du fournisseur BouncyCastle", e)
        }

        val remoteContextInstance = AndroidRemoteContext.getInstance()
        Log.i("MyApplication", "AndroidRemoteContext.getInstance(applicationContext) appelé dans MyApplication.onCreate().")
        Log.i("MyApplication", "Nom du client initialisé : ${remoteContextInstance.clientName}")

        val internalKeystoreFile = File(this.applicationContext.filesDir, "androidtv_secure.keystore")
        remoteContextInstance.keyStoreFile = internalKeystoreFile
        Log.i("MyApplication", "Chemin du Keystore explicitement configuré à : ${remoteContextInstance.keyStoreFile.absolutePath}")

    }
}
