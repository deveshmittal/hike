package com.bsb.hike.utils;

import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.DatabaseUtils.InsertHelper;
import android.util.Log;

public class HikeUserDatabase extends SQLiteOpenHelper {
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "hikeusers";
	private static final String DATABASE_TABLE = "users";

	@Override
	public void onCreate(SQLiteDatabase db) {
		String create = "CREATE TABLE IF NOT EXISTS users ( id STRING, name STRING, msisdn STRING, onhike INTEGER )";
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
		InsertHelper ih = new InsertHelper(db, DATABASE_TABLE);
		final int msisdnColumn = ih.getColumnIndex("msisdn");
		final int idColumn = ih.getColumnIndex("id");
		final int onHikeColumn = ih.getColumnIndex("onhike");
		db.delete(DATABASE_TABLE, "", new String[]{});
		onCreate(db);

		for(ContactInfo contact : contacts) {
			Log.d("db", "inserted");
			ih.prepareForInsert();
			ih.bind(msisdnColumn, contact.number);
			ih.bind(idColumn, contact.id);
			ih.bind(onHikeColumn, contact.onhike);
			ih.execute();
		}
	}
}
