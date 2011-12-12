package com.bsb.hike.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bsb.hike.models.ContactInfo;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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

	public void updateAddressBook(List<ContactInfo> contacts) {
		SQLiteDatabase db = mDb;
		db.beginTransaction();
		Cursor c = db.rawQuery("SELECT id FROM users", null);
		int idx = c.getColumnIndex("id");

		Set<String> ids = new HashSet<String>(c.getCount());
		while (c.moveToNext()) {
			String id = c.getString(idx);
			ids.add(id);
		}
		c.close();
		InsertHelper ih = new InsertHelper(db, DATABASE_TABLE);
		final int msisdnColumn = ih.getColumnIndex("msisdn");
		final int idColumn = ih.getColumnIndex("id");
		final int nameColumn = ih.getColumnIndex("name");
		final int onHikeColumn = ih.getColumnIndex("onhike");
		for(ContactInfo contact : contacts) {
			ids.remove(contact.id);
			ih.prepareForReplace();
			ih.bind(nameColumn, contact.name);
			ih.bind(msisdnColumn, contact.number);
			ih.bind(idColumn, contact.id);
			ih.bind(onHikeColumn, contact.onhike);
			ih.execute();
		}

		if (!ids.isEmpty()) {
			String clause = Utils.join(ids, ",");
			db.delete(DATABASE_TABLE, "id in ("+clause+")", null);
		}

		db.setTransactionSuccessful();
		db.endTransaction();
	}

	public Cursor findUsers(String partialName) {
		Cursor cursor = mDb.rawQuery("SELECT name, id AS _id, msisdn, onhike FROM users WHERE name LIKE ?", new String[] { partialName });
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
}
