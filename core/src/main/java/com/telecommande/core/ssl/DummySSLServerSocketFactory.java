package com.telecommande.core.ssl;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

public class DummySSLServerSocketFactory extends SSLServerSocketFactoryWrapper {

  DummySSLServerSocketFactory(KeyManager[] keyManagers,
      TrustManager[] trustManagers) throws KeyManagementException,
      NoSuchAlgorithmException {
    super(keyManagers, trustManagers);
  }

  public static DummySSLServerSocketFactory fromKeyManagers(
      KeyManager[] keyManagers) throws KeyManagementException,
      NoSuchAlgorithmException {
    TrustManager[] trustManagers = { new DummyTrustManager() };
    return new DummySSLServerSocketFactory(keyManagers, trustManagers);
  }

}
