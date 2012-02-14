package com.bsb.hike.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.utils.Utils;

public class HikeUserDatabase extends SQLiteOpenHelper
{
	private SQLiteDatabase mDb;

	private SQLiteDatabase mReadDb;

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		String create = "CREATE TABLE IF NOT EXISTS "+DBConstants.USERS_TABLE 
												+" ( " 
														+ DBConstants.ID + " STRING , "
														+ DBConstants.NAME +" STRING, "
														+ DBConstants.MSISDN+" TEXT COLLATE nocase, "
														+ DBConstants.ONHIKE+" INTEGER, "
														+ DBConstants.PHONE+" TEXT "
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
		mDb.execSQL(drop);
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
			final int phoneColumn = ih.getColumnIndex(DBConstants.PHONE);
			for (ContactInfo contact : contacts)
			{
				ih.prepareForReplace();
				ih.bind(nameColumn, contact.getName());
				ih.bind(msisdnColumn, contact.getMsisdn());
				ih.bind(idColumn, contact.getId());
				ih.bind(onHikeColumn, contact.isOnhike());
				ih.bind(phoneColumn, contact.getPhoneNum());
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
															+ DBConstants.ONHIKE+"=0 "+ "AS NotOnHike, "
															+ DBConstants.PHONE
														+ " FROM " + DBConstants.USERS_TABLE
														+ " WHERE (" + DBConstants.NAME+" LIKE ? OR "
														+ DBConstants.MSISDN + " LIKE ? )"
														+ " AND " + DBConstants.MSISDN + " != 'null' "
														+ "ORDER BY NotOnHike, " + DBConstants.NAME,
														new String[] { partialName, partialName });
		return cursor;
	}

	public ContactInfo getContactInfoFromMSISDN(String msisdn)
	{
		Cursor c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE,DBConstants.PHONE }, DBConstants.MSISDN+"=?", new String[] { msisdn }, null, null, null);
		List<ContactInfo> contactInfos = extractContactInfo(c);
		c.close();
		if (contactInfos.isEmpty())
		{
			return null;
		}

		return contactInfos.get(0);
	}

	private List<ContactInfo> extractContactInfo(Cursor c)
	{
		List<ContactInfo> contactInfos = new ArrayList<ContactInfo>(c.getCount());
		int idx = c.getColumnIndex(DBConstants.ID);
		int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
		int nameIdx = c.getColumnIndex(DBConstants.NAME);
		int onhikeIdx = c.getColumnIndex(DBConstants.ONHIKE);
		int phoneNumIdx = c.getColumnIndex(DBConstants.PHONE);
		while (c.moveToNext())
		{
			ContactInfo contactInfo = new ContactInfo(c.getString(idx), c.getString(msisdnIdx), c.getString(nameIdx), c.getInt(onhikeIdx) != 0,c.getString(phoneNumIdx));
			contactInfos.add(contactInfo);
		}
		return contactInfos;
	}

	public ContactInfo getContactInfoFromId(String id)
	{
		Cursor c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE,DBConstants.PHONE }, DBConstants.ID+"=?", new String[] { id }, null, null, null);
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
		Cursor c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE,DBConstants.PHONE }, null, null, null, null, null);
		List<ContactInfo> contactInfos = extractContactInfo(c);
		c.close();
		if (contactInfos.isEmpty())
		{
			return contactInfos;
		}

		return contactInfos;
	}

	public void deleteMultipleRows(Collection<String> ids)
	{
		String ids_joined = "(" + Utils.join(ids, ",", true) + ")";
		mDb.delete(DBConstants.USERS_TABLE, DBConstants.ID+" in " + ids_joined, null);
	}

	public void deleteRow(String id)
	{
		mDb.delete(DBConstants.USERS_TABLE, DBConstants.ID+"=?", new String []{id});
	}

	public void updateContacts(List<ContactInfo> updatedContacts)
	{
		if (updatedContacts == null)
		{
			 return;
		}

		ArrayList<String> ids = new ArrayList<String>(updatedContacts.size());
		for(ContactInfo c : updatedContacts)
		{
			ids.add(c.getId());
		}
		deleteMultipleRows(ids);
		try
		{
			addContacts(updatedContacts);
		}
		catch (DbException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void updateHikeContact(String msisdn, boolean onhike)
	{
		ContentValues vals = new ContentValues(1);
		vals.put(DBConstants.ONHIKE, onhike);
		mDb.update(DBConstants.USERS_TABLE, vals, "msisdn=?", new String[]{msisdn});
	}

	public void deleteAll()
	{
		mDb.delete(DBConstants.USERS_TABLE, null, null);
	}
}
