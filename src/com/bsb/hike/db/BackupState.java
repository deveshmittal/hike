package com.bsb.hike.db;

import java.io.Serializable;

public class BackupState implements Serializable
{

	private static final long serialVersionUID = 2L;

	private String _dbName;

	private int _dbVersion;

	private long _backupTime;

	public BackupState(String db, int version)
	{
		_dbName = db;
		_dbVersion = version;
		_backupTime = System.currentTimeMillis();
	}

	public int getDBVersion()
	{
		return _dbVersion;
	}

	public long getBackupTime()
	{
		return _backupTime;
	}

	@Override
	public String toString()
	{
		String s = super.toString() + " " + _dbName + " : " + _dbVersion + " at " + _backupTime;
		return s;
	}

}
