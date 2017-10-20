package com.bs.theme.bob.adapter.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.bs.themebridge.util.ThemeBridgeUtil;

/**
 * @author subhash s
 *
 */
public class SwiftOutEncryption {

	private final static Logger logger = Logger.getLogger(SwiftOutEncryption.class.getName());

	private static final String ALGORITHM = "RSA";
	private static String PATH_TO_PUBLICKEY = "publicKey";
	private static String PATH_TO_PRIVATEKEY = "privateKey";
	private static String PATH_TO_SYMMETRICKEY = "symmetric";

	public byte[] encrypt(byte[] publicKey, byte[] inputData) throws Exception {
		PublicKey key = KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(publicKey));
		// logger.debug("public key " + key);
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.PUBLIC_KEY, key);
		byte[] encryptedBytes = cipher.doFinal(inputData);
		return encryptedBytes;
	}

	public byte[] decrypt(byte[] privateKey, byte[] inputData) throws Exception {
		PrivateKey key = KeyFactory.getInstance(ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(privateKey));
		// logger.debug("PrivateKey key " + key);
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.PRIVATE_KEY, key);
		byte[] decryptedBytes = cipher.doFinal(inputData);
		return decryptedBytes;
	}

	public static void main(String args[]) {
		try {
			String test = ThemeBridgeUtil.readFile("D:\\subhash\\SWIFT.txt");
			logger.debug(test);
			logger.debug("Decrypted Data : " + getSwiftEncryptedMsg(test));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String encryptWithAESKey(String data, byte[] key)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, UnsupportedEncodingException {
		SecretKey secKey = new SecretKeySpec(key, "AES");

		Cipher cipher = Cipher.getInstance("AES");

		cipher.init(Cipher.ENCRYPT_MODE, secKey);
		byte[] newData = cipher.doFinal(data.getBytes());

		return new String(Base64.encodeBase64(newData), "UTF8");
	}

	public static String decryptWithAESKey(String inputData, byte[] key) throws NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance("AES");
		SecretKey secKey = new SecretKeySpec(key, "AES");

		cipher.init(Cipher.DECRYPT_MODE, secKey);
		byte[] newData = cipher.doFinal(Base64.decodeBase64(inputData.getBytes()));
		return new String(newData);

	}

	public static String getSwiftEncryptedMsg(String swiftOutMessage) {
		// InputStream inutStream =
		// SWIFTSwiftOutEncryptionAdaptee.class.getClassLoader().getResourceAsStream(PATH_TO_PUBLICKEY);
		// byte[] publicKey = IOUtils.toByteArray(inutStream);

		InputStream inutStream = SwiftOutEncryption.class.getClassLoader()
				.getResourceAsStream(PATH_TO_SYMMETRICKEY);
		byte[] symmetricKey = null;
		try {
			symmetricKey = IOUtils.toByteArray(inutStream);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// InputStream inutStream1 =
		// SWIFTSwiftOutEncryptionAdaptee.class.getClassLoader().getResourceAsStream(PATH_TO_PRIVATEKEY);
		// byte[] privateKey = IOUtils.toByteArray(inutStream1);
		String encryptedData = null;
		try {
			encryptedData = encryptWithAESKey(swiftOutMessage, symmetricKey);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		logger.debug("encryptedData " + encryptedData);

		// Decrypt encrypted Data by decrypted symmetric key

		String decryptedData = null;
		try {
			decryptedData = decryptWithAESKey(encryptedData, symmetricKey);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}

		// byte[] encryptedData = new
		// SWIFTSwiftOutEncryptionAdaptee().encrypt(publicKey,
		// swiftOutMessage.getBytes());
		// byte[] encryptedMsg = Base64.encodeBase64(encryptedData);
		// logger.debug("encrypted text is " + new String(base));
		// logger.info("encrypted SwiftOut is " + new
		// String(encryptedMsg,"UTF8"));
		// byte[] decryptedData = new
		// SWIFTSwiftOutEncryptionAdaptee().decrypt(privateKey, encryptedData);
		// logger.debug("decrypted text is " + new String(decryptedData));
		return decryptedData;
	}

}
