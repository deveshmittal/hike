package com.bsb.hike.db;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.FileChannel;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.CBCEncryption;
import com.bsb.hike.utils.Logger;

public class DBBackupRestore
{
	private static DBBackupRestore _instance = null;

	private static final String HIKE_PACKAGE_NAME = "com.bsb.hike";

	public static final String DATABASE_EXT = ".db";

	public static final String BACKUP = "backup";

	private static final String[] dbNames = { DBConstants.CONVERSATIONS_DATABASE_NAME };

	private static final String[] resetTableNames = { DBConstants.STICKER_SHOP_TABLE, DBConstants.STICKER_CATEGORIES_TABLE };

	private String backupToken;

	private Context mContext;

	private DBBackupRestore(Context context)
	{
		this.mContext = context;
		SharedPreferences settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		backupToken = settings.getString(HikeMessengerApp.BACKUP_TOKEN_SETTING, null);
	}

	public static DBBackupRestore getInstance(Context context)
	{
		if (_instance == null)
		{
			synchronized (DBBackupRestore.class)
			{
				if (_instance == null)
					_instance = new DBBackupRestore(context.getApplicationContext());
			}
		}
		return _instance;
	}

	public boolean backupDB()
	{
		Long time = System.currentTimeMillis();
		try
		{
			for (String fileName : dbNames)
			{
				File dbCopy = exportDatabse(fileName);
				if (dbCopy == null || !dbCopy.exists())
					return false;

				File backup = getDBBackupFile(dbCopy.getName());
				Logger.d(getClass().getSimpleName(), "encrypting with key: " + backupToken);
				CBCEncryption.encryptFile(dbCopy, backup, backupToken);
				dbCopy.delete();
			}
		}
		catch (Exception e)
		{
			deleteTempFiles();
			e.printStackTrace();
			return false;
		}
		if (!updateBackupState(null))
		{
			return false;
		}
		time = System.currentTimeMillis() - time;
		Logger.d(getClass().getSimpleName(), "Backup complete!! in " + time / 1000 + "." + time % 1000 + "s");
		return true;
	}

	public File exportDatabse(String databaseName)
	{
		Long time = System.currentTimeMillis();
		File dbCopy;

		FileChannel src = null;
		FileChannel dst = null;
		FileInputStream in = null;
		FileOutputStream out = null;

		try
		{
			File currentDB = getCurrentDBFile(databaseName);
			dbCopy = getDBCopyFile(currentDB.getName());
			in = new FileInputStream(currentDB);
			src = in.getChannel();
			out = new FileOutputStream(dbCopy);
			dst = out.getChannel();

			dst.transferFrom(src, 0, src.size());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
		finally
		{
			closeChannelsAndStreams(src, dst, in, out);
		}
		time = System.currentTimeMillis() - time;
		Logger.d(getClass().getSimpleName(), "DB Export complete!! in " + time / 1000 + "." + time % 1000 + "s");
		return dbCopy;
	}

	public boolean restoreDB()
	{
		Long time = System.currentTimeMillis();
		BackupState state = getBackupState();
		if (state == null)
		{
			return false;
		}
		if (state.getDBVersion() > DBConstants.CONVERSATIONS_DATABASE_VERSION)
		{
			return false;
		}
		try
		{
			for (String fileName : dbNames)
			{
				File currentDB = getCurrentDBFile(fileName);
				File dbCopy = getDBCopyFile(currentDB.getName());
				File backup = getDBBackupFile(dbCopy.getName());
				Logger.d(getClass().getSimpleName(), "decrypting with key: " + backupToken);
				CBCEncryption.decryptFile(backup, dbCopy, backupToken);
				importDatabase(dbCopy);
				dbCopy.delete();
			}
		}
		catch (Exception e)
		{
			deleteTempFiles();
			e.printStackTrace();
			return false;
		}
		state.restorePrefs(mContext);
		postRestoreSetup(state);
		time = System.currentTimeMillis() - time;
		Logger.d(getClass().getSimpleName(), "Restore complete!! in " + time / 1000 + "." + time % 1000 + "s");
		return true;
	}

	private void importDatabase(File dbCopy)
	{
		Long time = System.currentTimeMillis();

		FileChannel src = null;
		FileChannel dst = null;
		FileInputStream in = null;
		FileOutputStream out = null;

		try
		{
			File currentDB = getCurrentDBFile(dbCopy.getName());
			in = new FileInputStream(dbCopy);
			src = in.getChannel();
			out = new FileOutputStream(currentDB);
			dst = out.getChannel();

			dst.transferFrom(src, 0, src.size());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.d(getClass().getSimpleName(), "copy fail");
		}
		finally
		{
			closeChannelsAndStreams(src, dst, in, out);
		}
		time = System.currentTimeMillis() - time;
		Logger.d(getClass().getSimpleName(), "DB import complete!! in " + time / 1000 + "." + time % 1000 + "s");
	}

	private void postRestoreSetup(BackupState state)
	{
		if (state.getDBVersion() < DBConstants.CONVERSATIONS_DATABASE_VERSION)
		{
			HikeConversationsDatabase.getInstance().upgrade(state.getDBVersion() , DBConstants.CONVERSATIONS_DATABASE_VERSION);
		}
		for (String table : resetTableNames)
		{
			HikeConversationsDatabase.getInstance().clearTable(table);
		}
		HikeConversationsDatabase.getInstance().upgradeForStickerShopVersion1();
	}

	private void closeChannelsAndStreams(Closeable... closeables)
	{
		for (Closeable closeable : closeables)
		{
			try
			{
				if (closeable != null)
					closeable.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public boolean isBackupAvailable()
	{
		for (String fileName : dbNames)
		{
			File currentDB = getCurrentDBFile(fileName);
			File DBCopy = getDBCopyFile(currentDB.getName());
			File backup = getDBBackupFile(DBCopy.getName());
			if (!backup.exists())
				return false;
		}
		BackupState state = getBackupState();
		if (state != null)
		{
			if (state.getBackupTime() > 0)
			{
				return true;
			}
		}
		return false;
	}

	public long getLastBackupTime()
	{
		BackupState state = getBackupState();
		if (state != null)
		{
			return state.getBackupTime();
		}
		return -1;
	}

	private void deleteTempFiles()
	{
		for (String fileName : dbNames)
		{
			File currentDB = getCurrentDBFile(fileName);
			File dbCopy = getDBCopyFile(currentDB.getName());
			dbCopy.delete();
		}
	}

	public void deleteAllFiles()
	{
		for (String fileName : dbNames)
		{
			File currentDB = getCurrentDBFile(fileName);
			File dbCopy = getDBCopyFile(currentDB.getName());
			File backup = getDBBackupFile(dbCopy.getName());
			dbCopy.delete();
			backup.delete();
		}
	}

	private File getCurrentDBFile(String dbName)
	{
		File data = Environment.getDataDirectory();
		String currentDBPath = "//data//" + HIKE_PACKAGE_NAME + "//databases//" + dbName + "";
		File currentDB = new File(data, currentDBPath);
		return currentDB;
	}

	private File getDBBackupFile(String name)
	{
		new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT).mkdirs();
		return new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT, name + "." + BACKUP);
	}

	private File getDBCopyFile(String name)
	{
		new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT).mkdirs();
		return new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT, name);
	}
	
	private File getBackupStateFile()
	{
		new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT).mkdirs();
		return new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT, BACKUP);
	}
	
	private boolean updateBackupState(BackupState state)
	{
		if (state == null)
		{
			state = new BackupState(dbNames[0], DBConstants.CONVERSATIONS_DATABASE_VERSION);
			state.backupPrefs(mContext);
		}
		File backupStateFile = getBackupStateFile();
		FileOutputStream fileOut = null;
		ObjectOutputStream out = null;
		try
		{
			fileOut = new FileOutputStream(backupStateFile);
			out = new ObjectOutputStream(fileOut);
			out.writeObject(state);
			out.close();
			fileOut.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
		finally
		{
			closeChannelsAndStreams(fileOut,out);
		}
		return true;
	}
	
	private BackupState getBackupState()
	{
		BackupState state = null;
		File backupStateFile = getBackupStateFile();
		FileInputStream fileIn = null;
		ObjectInputStream in = null;
		try
		{
			if (!backupStateFile.exists())
			{
				return null;
			}
			fileIn = new FileInputStream(backupStateFile);
			in = new ObjectInputStream(fileIn);
			state = (BackupState) in.readObject();
			in.close();
			fileIn.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		finally
		{
			closeChannelsAndStreams(fileIn,in);
		}
		return state;
	}
	
	public boolean updatePrefs()
	{
		BackupState state = getBackupState();
		state.backupPrefs(mContext);
		return updateBackupState(state);
	}

}
