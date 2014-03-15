package com.bsb.hike.thor;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import javax.crypto.NoSuchPaddingException;

import org.sqldroid.SQLDroidDriver;

import com.bsb.hike.utils.Utils;

import android.content.Context;
import android.util.Log;

public class ThorThread implements Runnable
{
	private static final String OLD_BACKUP = "/mnt/sdcard/WhatsApp/Databases/msgstore.db.crypt";

	private static final String NEW_BACKUP = "/mnt/sdcard/WhatsApp/Databases/msgstore.db.crypt5";

	private static final String DB_FILE = "/mnt/sdcard/WhatsApp/Databases/msgstore.db";

	private static final String TEMP_DB_FILE = DB_FILE + "-journal";

	private static final String CONN_STR = "jdbc:sqldroid:" + DB_FILE;

	private static final String TAG = "ThorThread";

	private Context ctx;

	protected ThorThread(Context context)
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
					{
						final long t1 = System.currentTimeMillis();
						successDecrypt = Decrypt.decrypt5(inputFile, outputFile, email);
						final long t2 = System.currentTimeMillis();
						Log.d(TAG, "Decrypt processing Done in time : " + (t2 - t1));

					}
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
				}
			}
		}
		catch (ClassNotFoundException e)
		{
			Log.e(TAG,"Exception",e);
		}
		catch (SQLException e)
		{
			Log.e(TAG,"SQLException",e);
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
				Log.e(TAG,"SQLException in closing connection",e);
			}
		}
	}
}
