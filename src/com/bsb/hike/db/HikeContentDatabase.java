package com.bsb.hike.db;

import com.bsb.hike.db.DBConstants.HIKE_CONTENT;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
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
		String[] createAndIndexes = new String[3];
		int i=0;
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
		createAndIndexes[i++] = contentTable;
		// APP_ALARM TABLE - > id, data
		String createAlarmtable = CREATE_TABLE + APP_ALARM_TABLE + "(" + HIKE_CONTENT.ID + " INTEGER UNIQUE " + ALARM_DATA + " TEXT " + ")";
		createAndIndexes[i++] = createAlarmtable;
		// CREATE TABLE ENDS HERE
		// CREATE INDEXES
		String contentIndex = CREATE_INDEX + CONTENT_ID_INDEX + " ON "+CONTENT_TABLE +" ("+CONTENT_ID+")";
		createAndIndexes[i++] = contentIndex;
		// INDEX ENDS HERE
		
		return createAndIndexes;
	}
	
	private String[] getUpdateQueries(int oldVersion,int newVersion){
		String[] updateAndIndexes = new String[0];
		// UPDATE  TABLE
		
		//UPDATE INDEXES
		return updateAndIndexes;
	}
	
	public void insertUpdateAppAlarm(int id, String data)
	{
		Cursor c = mDB.query(APP_ALARM_TABLE, null, HIKE_CONTENT.ID + "=?", new String[] { String.valueOf(id) }, null, null, null);
		ContentValues cv = new ContentValues();
		cv.put(ALARM_DATA, data);
		if (c.moveToFirst())
		{
			// update query
			mDB.update(APP_ALARM_TABLE, cv, HIKE_CONTENT.ID + "=?", new String[] { String.valueOf(id) });
		}
		else
		{
			// insert query
			cv.put(HIKE_CONTENT.ID, id);
			mDB.insert(data, null, cv);
		}
	}
	
	public void deleteAppAlarm(int id){
		mDB.delete(APP_ALARM_TABLE, HIKE_CONTENT.ID + "=?", new String[] { String.valueOf(id) });
	}

	public String getAppAlarm(int id){
		Cursor c = mDB.query(APP_ALARM_TABLE, null, HIKE_CONTENT.ID + "=?", new String[] { String.valueOf(id) }, null, null, null);
		if(c.moveToFirst()){
			return c.getString(c.getColumnIndex(ALARM_DATA));
		}
		return null;
	}
}
