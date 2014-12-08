package com.bsb.hike.userlogs;

import java.io.UnsupportedEncodingException;

//AES 128 bit encryption logic using an digest of a password
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.bsb.hike.utils.Logger;

import android.util.Base64;

public class AESEncryption {
	private final static String TAG = "AESEncryption";
	private final static String encryptionAlgo  = "AES";
	private static SecretKeySpec secretKey;
	private static byte[] key;
	private static String initV = "0011223344556677";
	private static String decryptedString;
	private static String encryptedString;

	public static String getDecryptedString() {
		return decryptedString;
	}

	private static void setDecryptedString(String decrypted) {
		decryptedString = decrypted;
	}

	public static String getEncryptedString() {
		return encryptedString;
	}

	private static void setEncryptedString(String encrypted) {
		encryptedString = encrypted;
	}
	
	public static SecretKeySpec makeKey(String password, String algorithm) {

		MessageDigest sha = null;
		try {
			key = password.getBytes("UTF-8");
			sha = MessageDigest.getInstance(algorithm);
			key = sha.digest(key);
			Logger.d(TAG, "Initial Length of " + algorithm + " key : " + key.length);
			key = Arrays.copyOf(key, 16); // use only first 128 bit
			/*StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < key.length; i++)
				hexString.append(Integer.toHexString(0xFF & key[i]));
			Logger.d(TAG,"Hex String of Key : " + hexString.toString());*/
			Logger.d(TAG,"Base64 String of Key : " + Base64.encode(key, 0).toString());
			
			secretKey = new SecretKeySpec(key, encryptionAlgo);

		} catch (NoSuchAlgorithmException e) {
			Logger.d(TAG, e.toString());
		} catch (UnsupportedEncodingException e) {
			Logger.d(TAG, e.toString());
		}
		return secretKey;

	}

	
	public static String encrypt(String strToEncrypt) {
		String encrypted = null;
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			IvParameterSpec iv = new IvParameterSpec(initV.getBytes("UTF-8"));
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
			encrypted = new String(Base64.encode(
					cipher.doFinal(strToEncrypt.getBytes("UTF-8")),
					Base64.NO_WRAP));
			setEncryptedString(encrypted);
		} catch (Exception e) {
			Logger.d(TAG,"Error while encrypting: " + e.toString());
		}
		return encrypted;
	}

	public static String decrypt(String strToDecrypt) {
		String decrypted = null;
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			IvParameterSpec iv = new IvParameterSpec(initV.getBytes("UTF-8"));
			cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
			decrypted = new String(cipher.doFinal(Base64.decode(strToDecrypt, 0)));
			setDecryptedString(decrypted);
		} catch (Exception e) {
			Logger.d(TAG,"Error while decrypting: " + e.toString());
		}
		return decrypted;
	}
}
