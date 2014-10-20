package com.bsb.hike.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import android.os.Environment;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.utils.CBCEncryption;
import com.bsb.hike.utils.Logger;

public class DBBackupRestore
{
	private static DBBackupRestore _instance = null;

	private static final String HIKE_PACKAGE_NAME = "com.bsb.hike";

	public static final String DATABASE_EXT = ".db";

	private static final String[] dbNames = { DBConstants.CONVERSATIONS_DATABASE_NAME };

	private DBBackupRestore()
	{

	}

	public static DBBackupRestore getInstance()
	{
		if (_instance == null)
		{
			synchronized (FileTransferManager.class)
			{
				if (_instance == null)
					_instance = new DBBackupRestore();
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
				CBCEncryption.encryptFile(dbCopy, backup);
				dbCopy.delete();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
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
		try
		{
			File currentDB = getCurrentDBFile(databaseName);
			dbCopy = getDBCopyFile(currentDB.getName());
			if (currentDB.exists())
			{
				FileChannel src = new FileInputStream(currentDB).getChannel();
				FileChannel dst = new FileOutputStream(dbCopy).getChannel();
				dst.transferFrom(src, 0, src.size());
				src.close();
				dst.close();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
		time = System.currentTimeMillis() - time;
		Logger.d(getClass().getSimpleName(), "DB Export complete!! in " + time / 1000 + "." + time % 1000 + "s");
		return dbCopy;
	}

	public boolean restoreDB()
	{
		Long time = System.currentTimeMillis();
		try
		{
			for (String fileName : dbNames)
			{
				File currentDB = getCurrentDBFile(fileName);
				File DBCopy = getDBCopyFile(currentDB.getName());
				File backup = getDBBackupFile(DBCopy.getName());
				CBCEncryption.decryptFile(backup, DBCopy);
				importDatabase(DBCopy);
				DBCopy.delete();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		time = System.currentTimeMillis() - time;
		Logger.d(getClass().getSimpleName(), "Restore complete!! in " + time / 1000 + "." + time % 1000 + "s");
		return true;
	}

	private void importDatabase(File dbCopy)
	{
		Long time = System.currentTimeMillis();
		File currentDB = getCurrentDBFile(dbCopy.getName());
		if (dbCopy.exists())
		{
			try
			{
				FileChannel src = new FileInputStream(dbCopy).getChannel();
				FileChannel dst = new FileOutputStream(currentDB).getChannel();
				dst.transferFrom(src, 0, src.size());
				src.close();
				dst.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				Logger.d(getClass().getSimpleName(), "copy fail");
			}
			time = System.currentTimeMillis() - time;
			Logger.d(getClass().getSimpleName(), "DB import complete!! in " + time / 1000 + "." + time % 1000 + "s");
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
		return true;
	}

	private static File getCurrentDBFile(String dbName)
	{
		File data = Environment.getDataDirectory();
		String currentDBPath = "//data//" + HIKE_PACKAGE_NAME + "//databases//" + dbName + "";
		File currentDB = new File(data, currentDBPath);
		return currentDB;
	}

	private static File getDBBackupFile(String name)
	{
		new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT).mkdirs();
		return new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT, name + ".backup");
	}

	private static File getDBCopyFile(String name)
	{
		new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT).mkdirs();
		return new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT, name);
	}

}
