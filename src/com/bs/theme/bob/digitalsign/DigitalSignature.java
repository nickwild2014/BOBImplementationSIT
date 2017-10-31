package com.bs.theme.bob.digitalsign;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.encoders.Base64;

import com.bs.themebridge.util.ThemeBridgeUtil;

public final class DigitalSignature {

	private final static Logger logger = Logger.getLogger(DigitalSignature.class);

	private String SIGNATUREALGO = "SHA1withRSA";
	private String KEYSTORE_PASSWORD = "password";
	private static String PATH_TO_KEYSTORE = "TestCertificate.pfx";
	private String KEY_ALIAS_IN_KEYSTORE = "te-dfe5104e-e960-45ff-b71e-9be93e917148";

	public DigitalSignature() {
	}

	/**
	 * 
	 * @param sfmsMsg
	 *            {@code allows }{@link String}
	 * @return {@code allows }{@link String}
	 * @throws Exception
	 */
	public static String signSFMSMessage(String sfmsMsg) {

		// logger.debug(PATH_TO_KEYSTORE);
		// logger.debug(KEY_ALIAS_IN_KEYSTORE);
		// logger.debug(SIGNATUREALGO);
		try {
			DigitalSignature signer = new DigitalSignature();
			KeyStore keyStore = signer.loadKeyStore();
			
			 Enumeration aliases = keyStore.aliases();
		        String keyAlias = "";
		        while (aliases.hasMoreElements()) {
		            keyAlias = (String) aliases.nextElement();
		            System.out.println("alias "+keyAlias);
		        }
			
			CMSSignedDataGenerator signatureGenerator = signer.setUpProvider(keyStore);
			// content = "some bytes to be signed";
			// content = sfmsMsg;

			byte[] signedBytes = signer.signPkcs7(sfmsMsg.getBytes("UTF-8"), signatureGenerator);
			String signedContent = new String(Base64.encode(signedBytes));
			StringBuffer buffer = null;

			// logger.debug("content length-->" + signedContent.length());
			int j = 65;
			for (int i = 0; i < +signedContent.length(); i++) {
				buffer = new StringBuffer(signedContent);
				if (i == j) {
					buffer.insert(i, "\n");
					j = j + 65;
				}
				signedContent = buffer.toString();
			}

			sfmsMsg = sfmsMsg + "{UMAC:-----BEGIN PKCS7-----\n" + signedContent + "\n-----END PKCS7-----\n}";
			// logger.debug("Signed SFMS Message. Digital signature added ");

		} catch (Exception e) {
			logger.error("DigitalSignature Exception " + e.getMessage());
			String error = customPrintStackTracer("DigitalSignature", e);
			logger.debug("DigitalSignature exceptions!!! " + error);
			e.printStackTrace();
		}

		return sfmsMsg;
	}

	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	KeyStore loadKeyStore() throws Exception {
		// PKCS12, JKS , jceks
		KeyStore keystore = KeyStore.getInstance("PKCS12");
		InputStream inutStream = DigitalSignature.class.getClassLoader().getResourceAsStream(PATH_TO_KEYSTORE);
		keystore.load(inutStream, KEYSTORE_PASSWORD.toCharArray());
		return keystore;
	}

	/**
	 * 
	 * @param keystore
	 *            {@code allows }{@link String}
	 * @return
	 * @throws Exception
	 */
	CMSSignedDataGenerator setUpProvider(final KeyStore keystore) throws Exception {

		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		Certificate[] certchain = (Certificate[]) keystore.getCertificateChain(KEY_ALIAS_IN_KEYSTORE);
		final List<Certificate> certlist = new ArrayList<Certificate>();
		for (int i = 0, length = certchain == null ? 0 : certchain.length; i < length; i++) {
			certlist.add(certchain[i]);
		}
		Store certstore = new JcaCertStore(certlist);
		Certificate cert = keystore.getCertificate(KEY_ALIAS_IN_KEYSTORE);
		ContentSigner signer = new JcaContentSignerBuilder(SIGNATUREALGO).setProvider("BC")
				.build((PrivateKey) (keystore.getKey(KEY_ALIAS_IN_KEYSTORE, KEYSTORE_PASSWORD.toCharArray())));
		CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
		generator.addSignerInfoGenerator(
				new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().setProvider("BC").build())
						.build(signer, (X509Certificate) cert));
		generator.addCertificates(certstore);
		return generator;
	}

	/**
	 * 
	 * @param content
	 *            {@code allows }{@link String}
	 * @param generator
	 *            {@code allows }{@link String}
	 * @return {@code allows }{@link String}
	 * @throws Exception
	 */
	byte[] signPkcs7(final byte[] content, final CMSSignedDataGenerator generator) throws Exception {

		CMSTypedData cmsdata = new CMSProcessableByteArray(content);
		/** attached signature - include message and signature **/
		// logger.debug("Attached signer-OLD");
		// CMSSignedData signeddata = generator.generate(cmsdata, true);
		/** detached signature - signature only, no message included **/
		CMSSignedData signeddata = generator.generate(cmsdata, false);
		logger.debug("Dettached signer-NEW");

		return signeddata.getEncoded();
	}

	public static String customPrintStackTracer(String className, Exception Exception) {
		StringWriter errors = new StringWriter();
		String str = className + " Exceptions!!! ";
		errors.append(str);
		Exception.printStackTrace(new PrintWriter(errors));
		return errors.toString();
	}

	public static void main(String[] args) throws IOException, Exception {
		String sfmsMessage = ThemeBridgeUtil
				.readFile("C:\\Users\\KXT51472.KBANK\\Desktop\\SFMSOutwardNew\\Lengthy.txt");
		String sfmsSignedMessage = DigitalSignature.signSFMSMessage(sfmsMessage);
		System.out.println(sfmsSignedMessage);
	}

}
