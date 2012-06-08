package com.bsb.hike.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.utils.Utils;

public class HikeUserDatabase extends SQLiteOpenHelper
{
	private SQLiteDatabase mDb;

	private SQLiteDatabase mReadDb;

	private Context mContext;

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		String create = "CREATE TABLE IF NOT EXISTS "+DBConstants.USERS_TABLE 
												+" ( " 
														+ DBConstants.ID + " STRING , "
														+ DBConstants.NAME +" STRING, "
														+ DBConstants.MSISDN+" TEXT COLLATE nocase, "
														+ DBConstants.ONHIKE+" INTEGER, "
														+ DBConstants.PHONE+" TEXT, "
														+ DBConstants.HAS_CUSTOM_PHOTO+" INTEGER, "
														+ DBConstants.OVERLAY_DISMISSED+" INTEGER"
												+ " )";

		db.execSQL(create);

		create = "CREATE TABLE IF NOT EXISTS " + DBConstants.BLOCK_TABLE +
													" ( " + 
														DBConstants.MSISDN + " TEXT " +
													" ) ";
		db.execSQL(create);

		create = "CREATE TABLE IF NOT EXISTS " + DBConstants.THUMBNAILS_TABLE + 
													" ( " +
														DBConstants.MSISDN + " TEXT PRIMARY KEY, " + 
														DBConstants.IMAGE + " BLOB" +
													" ) ";
		db.execSQL(create);
	}

	public HikeUserDatabase(Context context)
	{
		super(context, DBConstants.USERS_DATABASE_NAME, null, DBConstants.USERS_DATABASE_VERSION);
		mDb = getWritableDatabase();
		mReadDb = getReadableDatabase();
		this.mContext = context;
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

	public void addBlockList(List<String> msisdns) throws DbException
	{
		if(msisdns == null)
		{
			return;
		}
		
		SQLiteDatabase db = mDb;
		db.beginTransaction();

		try
		{
			InsertHelper ih = new InsertHelper(db, DBConstants.BLOCK_TABLE);
			final int msisdnColumn = ih.getColumnIndex(DBConstants.MSISDN);
			for (String msisdn : msisdns)
			{
				ih.prepareForReplace();
				ih.bind(msisdnColumn, msisdn);
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
	public void setAddressBookAndBlockList(List<ContactInfo> contacts, List<String> blockedMsisdns) throws DbException
	{
		/* delete all existing entries from database */
		mDb.delete(DBConstants.USERS_TABLE, null, null);

		mDb.delete(DBConstants.BLOCK_TABLE, null, null);
		
		addContacts(contacts);
		addBlockList(blockedMsisdns);
	}
	
	

	public Cursor findUsers(String partialName, String selectedContacts)
	{
		ArrayList<String> contacts = Utils.splitSelectedContacts(selectedContacts);
		StringBuilder selectedNumbers = new StringBuilder("");
		
		if (contacts.size() > 0) 
		{
			for (String contact : contacts) 
			{
				selectedNumbers.append("'"
						+ contact
						+ "',");
			}
			selectedNumbers.delete(selectedNumbers.length() - 1,
					selectedNumbers.length());
		}

		String[] columns = new String[] {
				DBConstants.NAME, 
				DBConstants.ID + " AS _id", 
				DBConstants.MSISDN, 
				DBConstants.ONHIKE, 
				DBConstants.ONHIKE + "=0 AS NotOnHike", 
				DBConstants.PHONE, 
				DBConstants.HAS_CUSTOM_PHOTO}; 

		String selection = "(("  
				+ DBConstants.NAME + " LIKE ? OR " 
				+ DBConstants.MSISDN + " LIKE ?) AND "
				+ DBConstants.MSISDN + " NOT IN (" + selectedNumbers + ")) AND "
				+ DBConstants.MSISDN + " != 'null'";

		String[] selectionArgs = new String[] {partialName, partialName};

		String orderBy = "NotOnHike";

		Cursor cursor = mDb.query(DBConstants.USERS_TABLE, columns, selection, selectionArgs, null, null, orderBy);
		return cursor;
	}

	public ContactInfo getContactInfoFromMSISDN(String msisdn)
	{
		Cursor c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE,DBConstants.PHONE, DBConstants.HAS_CUSTOM_PHOTO }, DBConstants.MSISDN+"=?", new String[] { msisdn }, null, null, null);
		List<ContactInfo> contactInfos = extractContactInfo(c);
		c.close();
		if (contactInfos.isEmpty())
		{
			Log.d(getClass().getSimpleName(), "No contact found");
			return new ContactInfo(msisdn, msisdn, null, msisdn);
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
		int hasCustomPhotoIdx = c.getColumnIndex(DBConstants.HAS_CUSTOM_PHOTO);
		while (c.moveToNext())
		{
			ContactInfo contactInfo = new ContactInfo(c.getString(idx), c.getString(msisdnIdx), c.getString(nameIdx), c.getInt(onhikeIdx) != 0,c.getString(phoneNumIdx), c.getInt(hasCustomPhotoIdx)==1);
			contactInfos.add(contactInfo);
		}
		c.close();
		return contactInfos;
	}
	
	public ContactInfo getContactInfoFromId(String id)
	{
		Cursor c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE,DBConstants.PHONE, DBConstants.HAS_CUSTOM_PHOTO }, DBConstants.ID+"=?", new String[] { id }, null, null, null);
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

	public List<ContactInfo> getNonHikeContacts()
	{
		Cursor c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE,DBConstants.PHONE,DBConstants.HAS_CUSTOM_PHOTO }, DBConstants.ONHIKE + "=0", null, null, null, null);
		List<ContactInfo> contactInfos = extractContactInfo(c);
		c.close();
		if (contactInfos.isEmpty())
		{
			return contactInfos;
		}

		return contactInfos;
	}

	public List<ContactInfo> getContacts()
	{
		return getContacts(true);
	}

	public List<ContactInfo> getContacts(boolean ignoreEmpty)
	{
		String selection = ignoreEmpty ? DBConstants.MSISDN + " != 'null'" : null;
		Cursor c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE,DBConstants.PHONE, DBConstants.HAS_CUSTOM_PHOTO }, selection, null, null, null, null);
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

	public ContactInfo getContactInfoFromPhoneNo(String number)
	{
		Cursor c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE,DBConstants.PHONE, DBConstants.HAS_CUSTOM_PHOTO }, 
												DBConstants.PHONE + "=?", new String[] { number }, null, null, null);
		List<ContactInfo> contactInfos = extractContactInfo(c);
		c.close();
		if (contactInfos.isEmpty())
		{
			return null;
		}

		return contactInfos.get(0);
	}

	public void unblock(String msisdn)
	{
		mDb.delete(DBConstants.BLOCK_TABLE, DBConstants.MSISDN + "=?", new String[] {msisdn});
	}

	public void block(String msisdn)
	{
		ContentValues values = new ContentValues();
		values.put(DBConstants.MSISDN, msisdn);
		mDb.insert(DBConstants.BLOCK_TABLE, null, values);
	}

	public boolean isBlocked(String msisdn)
	{
		/* TODO could make this only select one entry */
		return getBlockedUsers().contains(msisdn);
	}

	public Set<String> getBlockedUsers()
	{
		Set<String> blocked = new HashSet<String>();
		Cursor c = null;
		try
		{
			c = mReadDb.query(DBConstants.BLOCK_TABLE, new String[] { DBConstants.MSISDN}, null, null, null, null, null);
			int idx = c.getColumnIndex(DBConstants.MSISDN);
			while(c.moveToNext())
			{
				blocked.add(c.getString(idx));
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

		return blocked;
	}

	public void setIcon(String msisdn, byte[] data)
	{
		IconCacheManager.getInstance().clearIconForMSISDN(msisdn);
		ContentValues vals = new ContentValues(2);
		vals.put(DBConstants.MSISDN, msisdn);
		vals.put(DBConstants.IMAGE, data);
		mDb.replace(DBConstants.THUMBNAILS_TABLE, null, vals);
		
		String whereClause = DBConstants.MSISDN + "=?"; //msisdn;
		ContentValues customPhotoFlag = new ContentValues(1);
		customPhotoFlag.put(DBConstants.HAS_CUSTOM_PHOTO, 1);
		mDb.update(DBConstants.USERS_TABLE, customPhotoFlag, whereClause, new String[] {msisdn});
	}
	
	public Drawable getIcon(String msisdn)
	{
		Cursor c = mDb.query(DBConstants.THUMBNAILS_TABLE, new String[]{DBConstants.IMAGE}, "msisdn=?", new String[] {msisdn}, null, null, null);
		try
		{
			if (!c.moveToFirst())
			{
				/* lookup based on this msisdn */
				return Utils.getDefaultIconForUser(mContext, msisdn);
			}

			byte[] icondata = c.getBlob(c.getColumnIndex(DBConstants.IMAGE));
			return new BitmapDrawable(BitmapFactory.decodeByteArray(icondata, 0, icondata.length));
		}
		finally
		{
			c.close();
		}
	}
}
