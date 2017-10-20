package com.bs.theme.bob.digitalsign;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataParser;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.RecipientId;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.encoders.Base64;

/**
 * 
 * @author Supreme
 *
 */
public final class SignarSignature {

	private static final String PATH_TO_KEYSTORE = "TestCertificate.pfx";
	private static final String KEY_ALIAS_IN_KEYSTORE = "te-dfe5104e-e960-45ff-b71e-9be93e917148";
	private static final String KEYSTORE_PASSWORD = "password";
	private static final String SIGNATUREALGO = "SHA1withRSA";

	public SignarSignature() {
	}

	private final static Logger logger = Logger.getLogger(DigitalSignature.class);

	public static void main(String[] args) throws Exception {

		// String content = readFile("/Users/Supreme/tryout/sfms.msg.java");
		// String content = readFile("D:\\_Prasath\\00_TASK\\sfms printer
		// friendly\\03SFMS-Inward.txt");
		String content = readFile("D:\\_Prasath\\00_TASK\\SFMSdigitalSignature\\sampleNeerajOutward.txt");

		signSFMSMessage(content);
		// logger.debug(signSFMSMessage(content));

		// writeFile("C:/Users/KXT51472.KBANK/Desktop/sfmsoutputNEW.txt",
		// sfmsMsg);
	}

	/**
	 * 
	 * @param sfmsMsg
	 *            {@code allows }{@link String}
	 * @return {@code allows }{@link String}
	 * @throws Exception
	 */
	public static String signSFMSMessage(String sfmsMsg) {

		try {
			/** Supreme added **/
			SignarSignature signer = new SignarSignature();

			KeyStore keyStore = signer.loadKeyStore();
			CMSSignedDataGenerator signatureGenerator = signer.setUpProvider(keyStore);

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
			logger.debug("Signed Encoded Bytes: " + sfmsMsg);
			// logger.debug("Signed SFMS Message. Digital signature added ");

		} catch (Exception e) {
			logger.error("DigitalSignature Exception " + e.getMessage());
			String error = customPrintStackTracer("DigitalSignature", e);
			logger.debug("DigitalSignature exceptions!!! " + error);
			e.printStackTrace();
		}

		return sfmsMsg;
	}

	KeyStore loadKeyStore() throws Exception {
		// KeyStore keystore = KeyStore.getInstance("JKS");
		KeyStore keystore = KeyStore.getInstance("PKCS12");
		// InputStream is = new FileInputStream(PATH_TO_KEYSTORE);
		InputStream inutStream = DigitalSignature.class.getClassLoader().getResourceAsStream(PATH_TO_KEYSTORE);
		keystore.load(inutStream, KEYSTORE_PASSWORD.toCharArray());
		return keystore;
	}

	CMSSignedDataGenerator setUpProvider(final KeyStore keystore) throws Exception {

		Security.addProvider(new BouncyCastleProvider());
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

	byte[] signPkcs7(final byte[] content, final CMSSignedDataGenerator generator) throws Exception {
		CMSTypedData cmsdata = new CMSProcessableByteArray(content);
		/** attached signature - include message and signature **/
		// CMSSignedData signeddata = generator.generate(cmsdata, true);
		/** detached signature - signature only, no message included **/
		CMSSignedData signeddata = generator.generate(cmsdata, false); // Detached-Signer-NEW
		return signeddata.getEncoded();
	}

	void getActualData(byte[] input) throws CMSException {
		CMSSignedData signedData = new CMSSignedData(Base64.decode(input));
		// CMSProcessable cmsProcesableContent = new
		// CMSProcessableByteArray(Base64.decode(Sig_Bytes.getBytes()));
	}

	public static String customPrintStackTracer(String className, Exception Exception) {
		StringWriter errors = new StringWriter();
		String str = className + " Exceptions!!! ";
		errors.append(str);
		Exception.printStackTrace(new PrintWriter(errors));
		return errors.toString();
	}

	public static String readFile(String filePath) throws Exception {
		FileReader fileReader = new FileReader(filePath);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String line = null;
		StringBuilder stringBuilder = new StringBuilder();
		String ls = System.getProperty("line.separator");
		try {
			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line);
				stringBuilder.append(ls);
			}
		} catch (Exception ex) {
			throw new Exception(ex);
		} finally {
			try {
				fileReader.close();
			} catch (Exception ex) {

			}
			try {
				bufferedReader.close();
			} catch (Exception ex) {

			}
		}

		return stringBuilder.toString();
	}

	public static boolean writeFile(String filePath, String messageToBeWrite) {
		boolean isSucceed = false;
		Writer output = null;
		File file = null;
		try {
			file = new File(filePath);
			output = new BufferedWriter(new FileWriter(file));

			output.write(messageToBeWrite);

		} catch (Exception ex) {
			ex.printStackTrace();

		} finally {
			try {
				output.close();
			} catch (IOException e) {

			}
		}
		return isSucceed;
	}

}