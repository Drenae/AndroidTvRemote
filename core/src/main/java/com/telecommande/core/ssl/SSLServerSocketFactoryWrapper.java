package com.telecommande.core.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;

public class SSLServerSocketFactoryWrapper extends SSLServerSocketFactory {
  private SSLServerSocketFactory mFactory;

  public SSLServerSocketFactoryWrapper(KeyManager[] keyManagers,
      TrustManager[] trustManagers)
      throws NoSuchAlgorithmException, KeyManagementException {
    SSLContext sslcontext = SSLContext.getInstance("TLS");
    sslcontext.init(keyManagers, trustManagers, null);
    mFactory = sslcontext.getServerSocketFactory();
  }
  
  public static SSLServerSocketFactoryWrapper CreateWithDummyTrustManager(
      KeyManager[] keyManagers) throws KeyManagementException,
      NoSuchAlgorithmException {
    TrustManager[] trustManagers = { new DummyTrustManager() };
    return new SSLServerSocketFactoryWrapper(keyManagers, trustManagers);
  }

  @Override
  public ServerSocket createServerSocket(int port) throws IOException {
    return mFactory.createServerSocket(port);
  }

  @Override
  public ServerSocket createServerSocket(int port, int backlog)
      throws IOException {
    return mFactory.createServerSocket(port, backlog);
  }

  @Override
  public ServerSocket createServerSocket(int port, int backlog,
      InetAddress ifAddress) throws IOException {
    return mFactory.createServerSocket(port, backlog, ifAddress);
  }

  @Override
  public String[] getDefaultCipherSuites() {
    return mFactory.getDefaultCipherSuites();
  }

  @Override
  public String[] getSupportedCipherSuites() {
    return mFactory.getSupportedCipherSuites();
  }

}
