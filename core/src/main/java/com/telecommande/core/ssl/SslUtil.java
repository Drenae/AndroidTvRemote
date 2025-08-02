package com.telecommande.core.ssl;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.x509.X509V1CertificateGenerator;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.security.auth.x500.X500Principal;

public class SslUtil {

	public static KeyPair generateRsaKeyPair() throws NoSuchAlgorithmException {
		KeyPairGenerator kg = KeyPairGenerator.getInstance("RSA");
		KeyPair kp = kg.generateKeyPair();
		return kp;
	}

	public static KeyStore getEmptyKeyStore() throws GeneralSecurityException,
			IOException {
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(null, null);
		return ks;
	}

	@SuppressWarnings("deprecation")
	public static X509Certificate generateX509V1Certificate(KeyPair pair,
			String name) throws GeneralSecurityException {
		java.security.Security
				.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

		Calendar calendar = Calendar.getInstance();
		calendar.set(2009, 0, 1);
		Date startDate = new Date(calendar.getTimeInMillis());
		calendar.set(2029, 0, 1);
		Date expiryDate = new Date(calendar.getTimeInMillis());

		BigInteger serialNumber = BigInteger.valueOf(Math.abs(System
				.currentTimeMillis()));

		X509V1CertificateGenerator certGen = new X509V1CertificateGenerator();
		X500Principal dnName = new X500Principal(name);
		certGen.setSerialNumber(serialNumber);
		certGen.setIssuerDN(dnName);
		certGen.setNotBefore(startDate);
		certGen.setNotAfter(expiryDate);
		certGen.setSubjectDN(dnName);
		certGen.setPublicKey(pair.getPublic());
		certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");

		X509Certificate cert = certGen.generate(pair.getPrivate(), "BC");
		return cert;
	}

	@SuppressWarnings("deprecation")
	public static X509Certificate generateX509V3Certificate(KeyPair pair,
			String name, Date notBefore, Date notAfter, BigInteger serialNumber)
			throws GeneralSecurityException {
		java.security.Security
				.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

		org.bouncycastle.x509.X509V3CertificateGenerator certGen = new org.bouncycastle.x509.X509V3CertificateGenerator();
		X500Name dnName = new org.bouncycastle.asn1.x500.X500Name(name);
		X500Principal principal = new X500Principal(name);

		certGen.setSerialNumber(serialNumber);
		certGen.setIssuerDN(principal);
		certGen.setSubjectDN(principal);
		certGen.setNotBefore(notBefore);
		certGen.setNotAfter(notAfter);
		certGen.setPublicKey(pair.getPublic());
		certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");

		certGen.addExtension(X509Extension.basicConstraints, true,
				new BasicConstraints(false));

		certGen.addExtension(X509Extension.keyUsage, true, new KeyUsage(
				KeyUsage.digitalSignature | KeyUsage.keyEncipherment
						| KeyUsage.keyCertSign));
		certGen.addExtension(X509Extension.extendedKeyUsage, true,
				new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));

		AuthorityKeyIdentifier authIdentifier = createAuthorityKeyIdentifier(
				pair.getPublic(), dnName, serialNumber);

		certGen.addExtension(X509Extension.authorityKeyIdentifier, true,
				authIdentifier);

		certGen.addExtension(X509Extension.subjectAlternativeName, false,
				new GeneralNames(new GeneralName(GeneralName.rfc822Name,
						"googletv@test.test")));

		X509Certificate cert = certGen.generate(pair.getPrivate(), "BC");
		return cert;
	}

	private static AuthorityKeyIdentifier createAuthorityKeyIdentifier(
			PublicKey publicKey, org.bouncycastle.asn1.x500.X500Name dnName,
			BigInteger serialNumber) {
		GeneralName genName = new GeneralName(dnName);
		SubjectPublicKeyInfo info;
		try {
			info = new SubjectPublicKeyInfo((ASN1Sequence) new ASN1InputStream(
					publicKey.getEncoded()).readObject());
		} catch (IOException e) {
			throw new RuntimeException("Error encoding public key");
		}
		return new AuthorityKeyIdentifier(info, new GeneralNames(genName),
				serialNumber);
	}

	public static X509Certificate generateX509V3Certificate(KeyPair pair,
			String name) throws GeneralSecurityException {
		Calendar calendar = Calendar.getInstance();
		calendar.set(2009, 0, 1);
		Date notBefore = new Date(calendar.getTimeInMillis());
		calendar.set(2099, 0, 1);
		Date notAfter = new Date(calendar.getTimeInMillis());

		BigInteger serialNumber = BigInteger.valueOf(Math.abs(System
				.currentTimeMillis()));

		return generateX509V3Certificate(pair, name, notBefore, notAfter,
				serialNumber);
	}

	public static X509Certificate generateX509V3Certificate(KeyPair pair,
			String name, BigInteger serialNumber)
			throws GeneralSecurityException {
		Calendar calendar = Calendar.getInstance();
		calendar.set(2009, 0, 1);
		Date notBefore = new Date(calendar.getTimeInMillis());
		calendar.set(2099, 0, 1);
		Date notAfter = new Date(calendar.getTimeInMillis());

		return generateX509V3Certificate(pair, name, notBefore, notAfter,
				serialNumber);
	}

	public SSLContext generateTestSslContext() throws GeneralSecurityException,
			IOException {
		SSLContext sslcontext = SSLContext.getInstance("SSLv3");
		KeyManager[] keyManagers = SslUtil.generateTestServerKeyManager(
				"SunX509", "test");
		sslcontext.init(keyManagers,
				new TrustManager[] { new DummyTrustManager() }, null);
		return sslcontext;
	}

	public static KeyManager[] getFileBackedKeyManagers(
			String keyManagerInstanceName, String fileName, String password)
			throws GeneralSecurityException, IOException {
		KeyManagerFactory km = KeyManagerFactory
				.getInstance(keyManagerInstanceName);
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(new FileInputStream(fileName), password.toCharArray());
		km.init(ks, password.toCharArray());
		return km.getKeyManagers();
	}

	public static KeyManager[] generateTestServerKeyManager(
			String keyManagerInstanceName, String password)
			throws GeneralSecurityException, IOException {
		KeyManagerFactory km = KeyManagerFactory
				.getInstance(keyManagerInstanceName);
		KeyPair pair = SslUtil.generateRsaKeyPair();
		X509Certificate cert = SslUtil.generateX509V1Certificate(pair,
				"CN=Test Server Cert");
		Certificate[] chain = { cert };

		KeyStore ks = SslUtil.getEmptyKeyStore();
		ks.setKeyEntry("test-server", pair.getPrivate(),
				password.toCharArray(), chain);
		km.init(ks, password.toCharArray());
		return km.getKeyManagers();
	}
}