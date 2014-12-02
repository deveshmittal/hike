package com.bsb.hike.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class CBCEncryption
{
	private static final String defaultPassword = "gauravmittal1111";

	private static final String salt = "03xy9z52twq8r4s1uv67";

	private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

	private static final String KEY_ALGORITHM = "AES";

	public static final String encryptExt = ".enc";

	public static final String decryptExt = ".dec";

	private static int pswdIterations = 2048;

	private static int keySize = 256;

	private static int cipherStreamBytesize = 256;

	private static byte[] ivBytes = { 'I', 'n', 'i', 't', 'i', 'a', 'l', 'i', 'z', 'e', 'V', 'e', 'c', 't', 'o', 'r' };

	public static File encryptFile(File file, File encryptedFile) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException, InvalidKeySpecException
	{
		return encryptFile(file, encryptedFile, defaultPassword);
	}

	public static File encryptFile(File file, File encryptedFile, String password) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException, InvalidKeySpecException
	{
		Long time = System.currentTimeMillis();
		FileInputStream fis = new FileInputStream(file.getPath());
		// This stream write the encrypted text. This stream will be wrapped by another stream.
		FileOutputStream fos = new FileOutputStream(encryptedFile.getPath());

		// Create cipher
		Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, getKeySpec(password), getIvSpec());
		// Wrap the output stream
		CipherOutputStream cos = new CipherOutputStream(fos, cipher);
		// Write bytes
		int b;
		byte[] d = new byte[cipherStreamBytesize];
		while ((b = fis.read(d)) != -1)
		{
			cos.write(d, 0, b);
		}
		// Flush and close streams.
		cos.flush();
		cos.close();
		fis.close();

		time = System.currentTimeMillis() - time;
		Logger.d(CBCEncryption.class.getSimpleName(), "Encryption complete!! in " + time / 1000 + "." + time % 1000 + "s");
		return encryptedFile;
	}

	public static File decryptFile(File file, File decryptedFile) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException, InvalidKeySpecException
	{
		return decryptFile(file, decryptedFile, defaultPassword);
	}

	public static File decryptFile(File file, File decryptedFile, String password) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException, InvalidKeySpecException
	{
		Long time = System.currentTimeMillis();
		FileInputStream fis = new FileInputStream(file.getPath());
		FileOutputStream fos = new FileOutputStream(decryptedFile.getPath());
		Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, getKeySpec(password), getIvSpec());
		CipherInputStream cis = new CipherInputStream(fis, cipher);
		int b;
		byte[] d = new byte[cipherStreamBytesize];
		while ((b = cis.read(d)) != -1)
		{
			fos.write(d, 0, b);
		}
		fos.flush();
		fos.close();
		cis.close();

		time = System.currentTimeMillis() - time;
		Logger.d(CBCEncryption.class.getSimpleName(), "Decryption complete!! in " + time / 1000 + "." + time % 1000 + "s");
		return decryptedFile;
	}

	private static SecretKeySpec getKeySpec(String password) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), pswdIterations, keySize);
		SecretKey tmp = factory.generateSecret(spec);
		SecretKeySpec secret = new SecretKeySpec(tmp.getEncoded(), KEY_ALGORITHM);
		return secret;
	}

	public static IvParameterSpec getIvSpec()
	{
		IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);
		return ivParameterSpec;
	}
}
