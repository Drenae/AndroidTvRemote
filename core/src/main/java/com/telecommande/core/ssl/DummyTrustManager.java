package com.telecommande.core.ssl;

import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

public class DummyTrustManager implements X509TrustManager {

  public void checkClientTrusted(X509Certificate[] chain, String authType) {
    return;
  }

  public void checkServerTrusted(X509Certificate[] chain, String authType) {
    return;
  }

  public X509Certificate[] getAcceptedIssuers() {
    return new X509Certificate[0];
  }

}
