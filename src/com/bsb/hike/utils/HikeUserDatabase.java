package com.bsb.hike.utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.bsb.hike.models.ContactInfo;

public class HikeUserDatabase extends SQLiteOpenHelper
{
	private SQLiteDatabase mDb;

	private SQLiteDatabase mReadDb;

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		String create = "CREATE TABLE IF NOT EXISTS "+DBConstants.USERS_TABLE 
												+" ( " 
														+ DBConstants.ID + " STRING PRIMARY KEY ON CONFLICT REPLACE NOT NULL, "
														+ DBConstants.NAME +" STRING, "
														+ DBConstants.MSISDN+" TEXT, "
														+ DBConstants.ONHIKE+" INTEGER "
												+ " )";
		db.execSQL(create);
	}

	public HikeUserDatabase(Context context)
	{
		super(context, DBConstants.USERS_DATABASE_NAME, null, DBConstants.USERS_DATABASE_VERSION);
		mDb = getWritableDatabase();
		mReadDb = getReadableDatabase();
	}

	@Override
	public synchronized void close()
	{
		mDb.close();
		mReadDb.close();
		super.close();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		String drop = "DROP TABLE IF EXISTS " + DBConstants.USERS_TABLE;
		db.execSQL(drop);
		onCreate(db);
	}

	public void addContacts(List<ContactInfo> contacts) throws DbException
	{
		SQLiteDatabase db = mDb;
		db.beginTransaction();

		try
		{
			InsertHelper ih = new InsertHelper(db, DBConstants.USERS_TABLE);
			final int msisdnColumn = ih.getColumnIndex(DBConstants.MSISDN);
			final int idColumn = ih.getColumnIndex(DBConstants.ID);
			final int nameColumn = ih.getColumnIndex(DBConstants.NAME);
			final int onHikeColumn = ih.getColumnIndex(DBConstants.ONHIKE);
			for (ContactInfo contact : contacts)
			{
				ih.prepareForReplace();
				ih.bind(nameColumn, contact.name);
				ih.bind(msisdnColumn, contact.number);
				ih.bind(idColumn, contact.id);
				ih.bind(onHikeColumn, contact.onhike);
				ih.execute();
			}
			db.setTransactionSuccessful();
		}
		catch (Exception e)
		{
			Log.e("HikeUserDatabase", "Unable to insert contacts", e);
			throw new DbException(e);
		}
		finally
		{
			db.endTransaction();
		}
	}

	/**
	 * Sets the address book from the list of contacts Deletes any existing contacts from the db
	 * 
	 * @param contacts
	 *            list of contacts to set/add
	 */
	public void setAddressBook(List<ContactInfo> contacts) throws DbException
	{
		/* delete all existing entries from database */
		mDb.delete(DBConstants.USERS_TABLE, null, null);

		addContacts(contacts);
	}

	public Cursor findUsers(String partialName)
	{
		Cursor cursor = mDb.rawQuery("SELECT "
															+ DBConstants.NAME+", "
															+ DBConstants.ID+" AS _id, "
															+ DBConstants.MSISDN+", "
															+ DBConstants.ONHIKE+", "
															+ DBConstants.ONHIKE+"=0 "+ "AS NotOnHike "
														+" FROM "+DBConstants.USERS_TABLE
														+" WHERE "+DBConstants.NAME+" LIKE ? ORDER BY "+DBConstants.NAME+", NotOnHike",
														new String[] { partialName });
		return cursor;
	}

	public ContactInfo getContactInfoFromMSISDN(String msisdn)
	{
		Cursor c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE }, DBConstants.MSISDN+"=?", new String[] { msisdn }, null, null, null);
		List<ContactInfo> contactInfos = extractContactInfo(c);
		if (contactInfos.isEmpty())
		{
			return null;
		}
		c.close();
		return contactInfos.get(0);
	}

	private List<ContactInfo> extractContactInfo(Cursor c)
	{
		List<ContactInfo> contactInfos = new ArrayList<ContactInfo>(c.getCount());
		int idx = c.getColumnIndex(DBConstants.ID);
		int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
		int nameIdx = c.getColumnIndex(DBConstants.NAME);
		int onhikeIdx = c.getColumnIndex(DBConstants.ONHIKE);
		while (c.moveToNext())
		{
			ContactInfo contactInfo = new ContactInfo(c.getString(idx), c.getString(msisdnIdx), c.getString(nameIdx), c.getInt(onhikeIdx) != 0);
			contactInfos.add(contactInfo);
		}
		return contactInfos;
	}

	public ContactInfo getContactInfoFromId(String id)
	{
		Cursor c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE }, DBConstants.ID+"=?", new String[] { id }, null, null, null);
		List<ContactInfo> contactInfos = extractContactInfo(c);
		c.close();
		if (contactInfos.isEmpty())
		{
			return null;
		}

		return contactInfos.get(0);
	}

	/**
	 * Adds a single contact to the hike user db
	 * 
	 * @param hikeContactInfo
	 *            contact to add
	 * @return true if the insert was successful
	 */
	public void addContact(ContactInfo hikeContactInfo) throws DbException
	{
		List<ContactInfo> l = new LinkedList<ContactInfo>();
		l.add(hikeContactInfo);
		addContacts(l);
	}

	public List<ContactInfo> getContacts()
	{
		Cursor c = mReadDb.query(DBConstants.USERS_DATABASE_NAME, new String[] { "msisdn", "id", "name", "onhike" }, null, null, null, null, null);
		List<ContactInfo> contactInfos = extractContactInfo(c);
		c.close();
		if (contactInfos.isEmpty())
		{
			return null;
		}

		return contactInfos;
	}
}
