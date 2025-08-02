package com.telecommande.core;

import com.telecommande.core.exception.PairingException;
import com.telecommande.core.pairing.PairingListener;
import com.telecommande.core.pairing.PairingSession;
import com.telecommande.core.remote.RemoteSession;
import com.telecommande.core.remote.Remotemessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class AndroidRemoteTv extends BaseAndroidRemoteTv {
    private final Logger logger = LoggerFactory.getLogger(AndroidRemoteTv.class);
    private PairingSession mPairingSession;
    private RemoteSession mRemoteSession;
    private AndroidTvListener mLocalTvListener;

    public void connect(String host, AndroidTvListener androidTvListener) throws GeneralSecurityException, IOException, InterruptedException, PairingException {
        this.mLocalTvListener = androidTvListener;

        if (mLocalTvListener != null) {
            mLocalTvListener.onConnectingToRemote();
        }

        mRemoteSession = new RemoteSession(host, 6466, new RemoteSession.RemoteSessionListener() {
            @Override
            public void onConnected() {
                if (mLocalTvListener != null) mLocalTvListener.onConnected();
            }

            @Override
            public void onSslError() throws GeneralSecurityException, IOException, InterruptedException, PairingException {
                if (mLocalTvListener != null) mLocalTvListener.onError("SSL Error during remote connection");
            }

            @Override
            public void onDisconnected() {
                if (mLocalTvListener != null) mLocalTvListener.onDisconnect();
            }

            @Override
            public void onError(String message) {
                if (mLocalTvListener != null) mLocalTvListener.onError("Remote Session Error: " + message);
            }
        });

        int pairingPort = 6467;
        int remotePort = 6466;

        if (AndroidRemoteContext.getInstance().getKeyStoreFile().exists()) {
            logger.info("Keystore exists, attempting direct remote connection to {}:{}", host, remotePort);
            mRemoteSession.connect();
        } else {
            logger.info("No keystore, initiating pairing with {}:{}", host, pairingPort);
            mPairingSession = new PairingSession();
            mPairingSession.pair(host, pairingPort, new PairingListener() {
                @Override
                public void onSessionCreated() {
                    logger.debug("Pairing session created with TV.");
                    if (mLocalTvListener != null) mLocalTvListener.onSessionCreated();
                }

                @Override
                public void onPerformInputDeviceRole() { /* Non utilisé */ }

                @Override
                public void onPerformOutputDeviceRole(byte[] gamma) { /* Non utilisé */ }

                @Override
                public void onSecretRequested() {
                    if (mLocalTvListener != null) mLocalTvListener.onSecretRequested();
                }

                @Override
                public void onSessionEnded() {
                    logger.debug("Pairing session ended by TV.");
                }

                @Override
                public void onError(String message) {
                    logger.error("Pairing error for host {}:{}: {}", host, pairingPort, message);
                    if (mLocalTvListener != null) mLocalTvListener.onError("Pairing Error: " + message);
                }

                @Override
                public void onPaired() {
                    logger.info("Successfully paired with {}. Now connecting remote session to {}:{}", host, host, remotePort);
                    if (mLocalTvListener != null) mLocalTvListener.onPaired();

                    try {
                        if (mLocalTvListener != null) mLocalTvListener.onConnectingToRemote();
                        mRemoteSession.connect();
                    } catch (GeneralSecurityException | IOException | InterruptedException | PairingException e) {
                        logger.error("Error connecting remote session after pairing to " + host, e);
                        if (mLocalTvListener != null) mLocalTvListener.onError("Post-pairing connection error: " + e.getMessage());
                    }
                }

                @Override
                public void onLog(String message) {
                    logger.debug("Pairing Log for {}: {}", host, message);
                }
            });
        }

    }

    public void sendCommand(Remotemessage.RemoteKeyCode remoteKeyCode, Remotemessage.RemoteDirection remoteDirection) {
        if (mRemoteSession != null) {
            mRemoteSession.sendCommand(remoteKeyCode, remoteDirection);
        } else {
            logger.warn("Cannot send command, mRemoteSession is null.");
        }
    }

    public void sendSecret(String code) {
        if (mPairingSession != null) {
            mPairingSession.provideSecret(code);
        } else {
            logger.warn("Cannot send secret, mPairingSession is null.");
        }
    }

    public void disconnect() {
        logger.info("AndroidRemoteTv.disconnect() called.");

        boolean wasConnectedOrPairing = (mRemoteSession != null || mPairingSession != null);

        if (mRemoteSession != null) {
            logger.debug("Closing RemoteSession socket.");
            mRemoteSession.closeSocket();
            mRemoteSession = null;
        }

        if (mPairingSession != null) {
            logger.debug("Closing PairingSession socket.");
            mPairingSession.closeSocket();
            mPairingSession = null;
        }

        if (mLocalTvListener != null && wasConnectedOrPairing) {
            mLocalTvListener.onDisconnect();
            logger.info("Notified listener of disconnection.");
        }
        mLocalTvListener = null;
        logger.info("Disconnection process in AndroidRemoteTv completed.");
    }
}
