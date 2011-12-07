package com.bsb.hike.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class HikeUserDatabase extends SQLiteOpenHelper {
	private static final int DATABASE_VERSION = 2;
	private static final String DATABASE_NAME = "hikeusers";
	private static final String DATABASE_TABLE = "users";

	@Override
	public void onCreate(SQLiteDatabase db) {
		String create = "CREATE TABLE IF NOT EXISTS users ( id STRING PRIMARY KEY ON CONFLICT REPLACE NOT NULL, name STRING, msisdn STRING, onhike INTEGER )";
		db.execSQL(create);
	}

	public HikeUserDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		String drop = "DROP TABLE IF EXISTS " + DATABASE_TABLE;
		db.execSQL(drop);
		onCreate(db);
	}

	public void updateAddressBook(List<ContactInfo> contacts) {
		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		Cursor c = db.rawQuery("SELECT id FROM users", null);
		int idx = c.getColumnIndex("id");

		Set<String> ids = new HashSet<String>(c.getCount());
		while (c.moveToNext()) {
			String id = c.getString(idx);
			ids.add(id);
		}
		c.close();
		Log.d("DB", "DB Size is: "+ids.size());
		InsertHelper ih = new InsertHelper(db, DATABASE_TABLE);
		final int msisdnColumn = ih.getColumnIndex("msisdn");
		final int idColumn = ih.getColumnIndex("id");
		final int nameColumn = ih.getColumnIndex("name");
		final int onHikeColumn = ih.getColumnIndex("onhike");
		Log.d("DBUtils", "starting insert " + contacts.size());
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
			Log.d("db", "deleting entry: " + clause);
			db.delete(DATABASE_TABLE, "id in ("+clause+")", null);
		}

		db.setTransactionSuccessful();
		db.endTransaction();
		db.close();
	}
}
