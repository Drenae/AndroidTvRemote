package com.telecommande.core.remote;

import com.telecommande.core.exception.PairingException;

public interface RemoteListener {
    void onConnected();

    void onDisconnected();

    void onVolume();

    void onPerformInputDeviceRole() throws PairingException;

    void onPerformOutputDeviceRole(byte[] gamma)
            throws PairingException;

    void onSessionEnded();

    void onError(String message);

    void onLog(String message);

    void sSLException();
}