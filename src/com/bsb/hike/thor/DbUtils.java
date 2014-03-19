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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.sqldroid.SQLDroidDriver;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.util.Log;
import android.util.Patterns;

public class DbUtils
{
	private static final String TAG = "DecryptDbUtils";

	public static long getOldTs(int days)
	{
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -days);
		long daysAgo = cal.getTimeInMillis();
		return daysAgo;
	}

	public static Timestamp daysbeforetime(int days)
	{
		java.util.Calendar cal = java.util.Calendar.getInstance();
		java.util.Date utilDate = cal.getTime();
		Date datebefore = new Date(utilDate.getTime() - ((long) (days * 24)) * 3600 * 1000);
		return (new Timestamp(datebefore.getTime()));
	}

	public static void getTables(Connection con) throws SQLException
	{
		DatabaseMetaData md = con.getMetaData();
		ResultSet rs = md.getTables(null, null, "%", null);
		Log.d("TABLES", "Printing tables .....");
		while (rs.next())
		{
			Log.d("TABLES", rs.getString(3));
		}
	}

	public static List<String> getConvs(Connection con)
	{
		List<String> convs = null;
		PreparedStatement pt = null;
		ResultSet msg = null;
		try
		{
			String query = "SELECT key_remote_jid FROM chat_list WHERE key_remote_jid LIKE '91%' ";
			pt = con.prepareStatement(query);
			msg = pt.executeQuery();
			convs = new LinkedList<String>();
			while (msg.next())
			{
				try
				{
					String conv = msg.getString("key_remote_jid");
					if (!conv.contains("-")) // this is 1-1 chat
						convs.add(conv);
				}
				catch (Exception e)
				{

				}
			}
		}
		catch (SQLException e)
		{
			Log.e(TAG, "SQLException in getting convs", e);
		}
		catch (Exception e)
		{
			Log.e(TAG, "Exception in getting convs", e);
		}
		finally
		{
			close(pt, msg);
		}
		return convs;
	}

	public static void calculateScore(HashMap<String, Integer> freq, Connection con, String key_remote_jid, long ts)
	{
		ResultSet msg = null;
		PreparedStatement pt = null;
		try
		{
			String query = "SELECT count(*) as cc FROM messages where key_remote_jid = ? AND timestamp > ?";
			pt = con.prepareStatement(query);
			pt.setString(1, key_remote_jid);
			pt.setLong(2, ts);
			msg = pt.executeQuery();
			while (msg.next())
			{
				int count = msg.getInt("cc");
				if(count > 0)
					freq.put(key_remote_jid, count);
			}
		}
		catch (SQLException e)
		{
			Log.e(TAG, "SQLException in calculating score", e);
		}
		catch (Exception e)
		{
			Log.e(TAG, "Exception in calculating score", e);
		}
		finally
		{
			close(pt, msg);
		}
	}

	private static void close(PreparedStatement pt, ResultSet msg)
	{
		if (msg != null)
			try
			{
				msg.close();
			}
			catch (SQLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		if (pt != null)
			try
			{
				pt.close();
			}
			catch (SQLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
}
