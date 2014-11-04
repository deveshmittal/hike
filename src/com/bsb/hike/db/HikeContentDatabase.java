package com.bsb.hike.db;

import com.bsb.hike.db.DBConstants.HIKE_CONTENT;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class HikeContentDatabase extends SQLiteOpenHelper implements DBConstants,HIKE_CONTENT{

	private static HikeContentDatabase hikeContentDatabase;
	SQLiteDatabase mDB;
	private HikeContentDatabase(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		mDB = getWritableDatabase();
	}
	
	public static  HikeContentDatabase getInstance(Context context){
		if(hikeContentDatabase==null){
			hikeContentDatabase = new HikeContentDatabase(context);
		}
		return hikeContentDatabase;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		mDB = db;
		String[] createQueries = getCreateQueries();
		for(String create:createQueries){
			db.execSQL(create);
		}

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		mDB = db;
		// CREATE all tables, it is possible that few tables are created in this version
		onCreate(mDB);
		String [] updateQueries = getUpdateQueries(oldVersion, newVersion);
		for(String update : updateQueries){
			db.execSQL(update);
		}
		// DO any other update operation here
	}
	
	private String[] getCreateQueries(){
		String[] createAndIndexes = new String[2];
		// CREATE TABLE
		//CONTENT TABLE -> _id,content_id,love_id,channel_id,timestamp,metadata
		String contentTable = CREATE_TABLE +CONTENT_TABLE
				+ "("
				+_ID +" INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ CONTENT_ID+" INTEGER UNIQUE, "
				+ LOVE_ID+ " INTEGER, "
				+CHANNEL_ID+" INTEGER, "
				+HIKE_CONTENT.TIMESTAMP+" INTEGER, "
				+METADATA+" TEXT"
				+")";
		createAndIndexes[0] = contentTable;
		// CREATE TABLE ENDS HERE
		//CREATE INDEXES
		String contentIndex = CREATE_INDEX + CONTENT_ID_INDEX + " ON "+CONTENT_TABLE +" ("+CONTENT_ID+")";
		createAndIndexes[1] = contentIndex;
		// INDEX ENDS HERE
		return createAndIndexes;
	}
	
	private String[] getUpdateQueries(int oldVersion,int newVersion){
		String[] updateAndIndexes = new String[0];
		// UPDATE  TABLE
		
		//UPDATE INDEXES
		return updateAndIndexes;
	}

}
