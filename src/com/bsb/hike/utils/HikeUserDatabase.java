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

public class HikeUserDatabase extends SQLiteOpenHelper {
	private SQLiteDatabase mDb;
	private SQLiteDatabase mReadDb;

	private static final int DATABASE_VERSION = 2;
	private static final String DATABASE_NAME = "hikeusers";
	private static final String DATABASE_TABLE = "users";

	@Override
	public void onCreate(SQLiteDatabase db) {
		String create = "CREATE TABLE IF NOT EXISTS users ( id STRING PRIMARY KEY ON CONFLICT REPLACE NOT NULL, name STRING, msisdn TEXT, onhike INTEGER )";
		db.execSQL(create);
	}

	public HikeUserDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		mDb = getWritableDatabase();
		mReadDb = getReadableDatabase();
	}

	@Override
	public synchronized void close() {
		mDb.close();
		mReadDb.close();
		super.close();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		String drop = "DROP TABLE IF EXISTS " + DATABASE_TABLE;
		db.execSQL(drop);
		onCreate(db);
	}

    public boolean addContacts(List<ContactInfo> contacts) {
        SQLiteDatabase db = mDb;
        db.beginTransaction();

        try {
            InsertHelper ih = new InsertHelper(db, DATABASE_TABLE);
            final int msisdnColumn = ih.getColumnIndex("msisdn");
            final int idColumn = ih.getColumnIndex("id");
            final int nameColumn = ih.getColumnIndex("name");
            final int onHikeColumn = ih.getColumnIndex("onhike");
            for (ContactInfo contact : contacts) {
                ih.prepareForReplace();
                ih.bind(nameColumn, contact.name);
                ih.bind(msisdnColumn, contact.number);
                ih.bind(idColumn, contact.id);
                ih.bind(onHikeColumn, contact.onhike);
                ih.execute();
            }
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e("HikeUserDatabase", "Unable to insert contacts", e);
            return false;
        } finally {
            db.endTransaction();
        }
    }

	/**
	 * Sets the address book from the list of contacts
	 * Deletes any existing contacts from the db
	 * @param contacts list of contacts to set/add
	 */
	public void setAddressBook(List<ContactInfo> contacts) {
		/* delete all existing entries from database */
		mDb.delete(DATABASE_TABLE, null, null);

		addContacts(contacts);
	}

    public Cursor findUsers(String partialName) {
		Cursor cursor = mDb.rawQuery("SELECT name, id AS _id, msisdn, onhike, onhike=0 AS NotOnHike FROM users WHERE name LIKE ? ORDER BY name, NotOnHike", new String[] { partialName });
		return cursor;
	}

	public ContactInfo getContactInfoFromMSISDN(String msisdn) {
		Cursor c = mReadDb.query(DATABASE_TABLE, new String[]{"msisdn", "id", "name", "onhike"}, "msisdn=?", new String[]{msisdn}, null, null, null);
		List<ContactInfo> contactInfos = extractContactInfo(c);
		if (contactInfos.isEmpty()) {
			return null;
		}
		return contactInfos.get(0);
	}

	private List<ContactInfo> extractContactInfo(Cursor c) {
		List<ContactInfo> contactInfos = new ArrayList<ContactInfo>(c.getCount());
		int idx = c.getColumnIndex("id");
		int msisdnIdx = c.getColumnIndex("msisdn");
		int nameIdx = c.getColumnIndex("name");
		int onhikeIdx = c.getColumnIndex("onhike");
		while (c.moveToNext()) {
			ContactInfo contactInfo = new ContactInfo(c.getString(idx), c.getString(msisdnIdx), c.getString(nameIdx), c.getInt(onhikeIdx) != 0);
			contactInfos.add(contactInfo);
		}
		return contactInfos;
	}

    public ContactInfo getContactInfoFromId(String id) {
        Cursor c = mReadDb.query(DATABASE_TABLE, new String[]{"msisdn", "id", "name", "onhike"}, "id=?", new String[] {id}, null, null, null);
        List<ContactInfo> contactInfos = extractContactInfo(c);
        if (contactInfos.isEmpty()) {
            return null;
        }

        return contactInfos.get(0);
    }

    /** Adds a single contact to the hike user db
     * @param hikeContactInfo contact to add
     * @return true iff the insert was successful
     */
    public boolean addContact(ContactInfo hikeContactInfo) {
        List<ContactInfo> l = new LinkedList<ContactInfo>();
        l.add(hikeContactInfo);
        return addContacts(l);
    }
}
