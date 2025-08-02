package com.telecommande.core.pairing;

interface SecretProvider {

    void requestSecret(PairingSession pairingSession);

}
