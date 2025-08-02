package com.telecommande.core.remote;

import com.telecommande.core.ssl.KeyStoreManager;
import com.telecommande.core.exception.PairingException;
import com.telecommande.core.ssl.DummyTrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

public class RemoteSession {

    private final Logger logger = LoggerFactory.getLogger(RemoteSession.class);
    private final BlockingQueue<Remotemessage.RemoteMessage> mMessageQueue;
    private static RemoteMessageManager mMessageManager;
    private final String mHost;
    private final int mPort;
    private final RemoteSessionListener mRemoteSessionListener;
    int retry;
    OutputStream outputStream;

    private SSLSocket mSslSocket;

    public RemoteSession(String host, int port, RemoteSessionListener remoteSessionListener) {
        mMessageQueue = new LinkedBlockingDeque<>();
        mMessageManager = new RemoteMessageManager();
        mHost = host;
        mPort = port;
        mRemoteSessionListener = remoteSessionListener;
    }

    public void connect() throws GeneralSecurityException, IOException, InterruptedException, PairingException {

        try {
            SSLContext sSLContext = SSLContext.getInstance("TLS");
            sSLContext.init(new KeyStoreManager().getKeyManagers(), new TrustManager[]{new DummyTrustManager()}, new SecureRandom());
            SSLSocketFactory sslsocketfactory = sSLContext.getSocketFactory();
            SSLSocket sSLSocket = (SSLSocket) sslsocketfactory.createSocket(mHost, mPort);

            this.mSslSocket = sSLSocket;

            mSslSocket.setNeedClientAuth(true);
            mSslSocket.setUseClientMode(true);
            mSslSocket.setKeepAlive(true);
            mSslSocket.setTcpNoDelay(true);
            mSslSocket.startHandshake();

            outputStream = mSslSocket.getOutputStream();
            new RemotePacketParser(mSslSocket.getInputStream(), outputStream, mMessageQueue, new RemoteListener() {
                @Override
                public void onConnected() {
                    mRemoteSessionListener.onConnected();
                }

                @Override
                public void onDisconnected() {

                }

                @Override
                public void onVolume() {

                }

                @Override
                public void onPerformInputDeviceRole() throws PairingException {

                }

                @Override
                public void onPerformOutputDeviceRole(byte[] gamma) throws PairingException {

                }

                @Override
                public void onSessionEnded() {

                }

                @Override
                public void onError(String message) {

                }

                @Override
                public void onLog(String message) {

                }

                @Override
                public void sSLException() {

                }
            }).start();

            Remotemessage.RemoteMessage remoteMessage = waitForMessage();
            logger.info(remoteMessage.toString());

            byte[] remoteConfigure = mMessageManager.createRemoteConfigure(622, "ROG Strix G531GT_G531GT", "ASUSTeK COMPUTER INC.", 1, "1");

            outputStream.write(remoteConfigure);

            waitForMessage();

            byte[] remoteActive = mMessageManager.createRemoteActive(622);
            outputStream.write(remoteActive);
        } catch (SSLException sslException) {
            mRemoteSessionListener.onSslError();
            closeSocket();
        } catch (Exception e) {
            e.printStackTrace();
            mRemoteSessionListener.onError(e.getMessage());
            closeSocket();
        }
    }

    Remotemessage.RemoteMessage waitForMessage() throws InterruptedException, PairingException {
        return mMessageQueue.take();
    }

    public void attemptToReconnect() {
        retry++;
        try {
            connect();
        } catch (GeneralSecurityException | IOException | InterruptedException | PairingException e) {
            mRemoteSessionListener.onError(e.getMessage());
            throw new RuntimeException(e);
        }
    }


    public void sendCommand(Remotemessage.RemoteKeyCode remoteKeyCode, Remotemessage.RemoteDirection remoteDirection) {
        try {
            outputStream.write(mMessageManager.createKeyCommand(remoteKeyCode,remoteDirection));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public interface RemoteSessionListener {
        void onConnected();

        void onSslError() throws GeneralSecurityException, IOException, InterruptedException, PairingException;

        void onDisconnected();

        void onError(String message);
    }

    public void closeSocket() {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                logger.error("IOException while closing RemoteSession OutputStream: " + e.getMessage(), e);
            }
            outputStream = null;
        }
        if (mSslSocket != null && !mSslSocket.isClosed()) {
            logger.debug("Closing RemoteSession SSLSocket.");
            try {
                mSslSocket.close();
            } catch (IOException e) {
                logger.error("IOException while closing RemoteSession SSLSocket: " + e.getMessage(), e);
            }
        }
        mSslSocket = null;
    }
}
