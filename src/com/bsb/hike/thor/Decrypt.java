package com.bsb.hike.thor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;

import android.annotation.SuppressLint;

import com.bsb.hike.thor.crypt.AESEngine;
import com.bsb.hike.thor.crypt.BufferedBlockCipher2par;

public class Decrypt
{
	private static final String TAG = "Decrypt";

	private static final byte[] KEY = { 0x34, 0x6a, 0x23, 0x65, 0x2a, 0x46, 0x39, 0x2b, 0x4d, 0x73, 0x25, 0x7c, 0x67, 0x31, 0x7e, 0x35, 0x2e, 0x33, 0x72, 0x48, 0x21, 0x77, 0x65,
			0x2c };

	private static final byte EOF_MARKER = (byte) 0xFD;

	private static final byte[] INITIALIZATION_VECTOR = hexStringToByteArray("1e39f369e90db33aa73b442bbbb6b0b9");

	private static final byte[] ENCRYPTION_KEY = hexStringToByteArray("8d4b155cc9ff81e5cbf6fa7819366a3ec621a656416cd793");

	@SuppressLint("NewApi")
	public static boolean decrypt5(File inputFile, File outputFile, String email)
	{
		boolean successDecrypt = false;
		try
		{
			String emailMD5 = md5(email);
			byte[] emailMD5Bytes = hexStringToByteArray(emailMD5 + emailMD5);

			byte[] decryptionKey = new byte[24];
			System.arraycopy(ENCRYPTION_KEY, 0, decryptionKey, 0, 24);

			for (int i = 0; i < 24; i++)
			{
				decryptionKey[i] ^= emailMD5Bytes[i & 0xF];
			}

			Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decryptionKey, "AES"), new IvParameterSpec(INITIALIZATION_VECTOR));
			CipherInputStream cIn = new CipherInputStream(new FileInputStream(inputFile), cipher);
			FileOutputStream fOut = new FileOutputStream(outputFile);

			byte[] buffer = new byte[8192];
			int n;
			while ((n = cIn.read(buffer)) != -1)
			{
				fOut.write(buffer, 0, n);
			}
			fOut.flush();
			fOut.close();
			try
			{
				// if this throws some exception, its ok as decryption is
				// already done
				cIn.close();
			}
			catch (Exception e)
			{
			}
			successDecrypt = true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return successDecrypt;
	}

	private static byte[] hexStringToByteArray(String s)
	{
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2)
		{
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	private static String md5(String md5) throws NoSuchAlgorithmException
	{
		MessageDigest m = MessageDigest.getInstance("MD5");
		m.reset();
		m.update(md5.getBytes());
		byte[] digest = m.digest();
		BigInteger bigInt = new BigInteger(1, digest);
		return bigInt.toString(16);
	}

	public static BufferedBlockCipher2par getCipher(boolean forEncryption)
	{
		BlockCipher engine = new AESEngine();
		BufferedBlockCipher2par cipher = new BufferedBlockCipher2par(engine);
		cipher.init(forEncryption, new KeyParameter(KEY));
		return cipher;
	}

	public static boolean decrypt(File input, File output)
	{
		boolean successDecrypt = false;
		try
		{
			InputStream in = new BufferedInputStream(new FileInputStream(input));
			OutputStream out = new BufferedOutputStream(new FileOutputStream(output));
			byte[] inputbytes = new byte[8192];
			byte[] outputbytes = new byte[8192];
			BufferedBlockCipher2par cipher = getCipher(false);
			int n;
			while ((n = in.read(inputbytes)) != -1)
			{
				cipher.processBytes(inputbytes, 0, n, outputbytes, 0);
				out.write(outputbytes, 0, n);
			}
			in.close();
			out.flush();
			out.close();
			successDecrypt = true;
		}
		catch (FileNotFoundException e)
		{
		}
		catch (IOException e)
		{
		}
		catch (Exception e)
		{
		}
		return successDecrypt;
	}
}
