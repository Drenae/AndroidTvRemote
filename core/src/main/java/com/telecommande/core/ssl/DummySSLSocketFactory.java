package com.telecommande.core.ssl;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

public class DummySSLSocketFactory extends SSLSocketFactoryWrapper {
  
  DummySSLSocketFactory(KeyManager[] keyManagers,
      TrustManager[] trustManagers) throws KeyManagementException,
      NoSuchAlgorithmException {
    super(keyManagers, trustManagers);
  }

  public static DummySSLSocketFactory fromKeyManagers(KeyManager[] keyManagers)
      throws KeyManagementException, NoSuchAlgorithmException {
    TrustManager[] trustManagers = { new DummyTrustManager() };
    return new DummySSLSocketFactory(keyManagers, trustManagers);
  }

}
