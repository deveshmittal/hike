package com.bsb.hike.db;

import java.net.URISyntaxException;
import java.util.ArrayList;

import org.json.JSONObject;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;
import android.util.SparseArray;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.DBConstants.HIKE_CONTENT;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.productpopup.ProductContentModel;
import com.bsb.hike.utils.Logger;

public class HikeContentDatabase extends SQLiteOpenHelper implements DBConstants, HIKE_CONTENT
{

	private static final HikeContentDatabase hikeContentDatabase=new HikeContentDatabase();

	SQLiteDatabase mDB;

	private HikeContentDatabase()
	{
		super(HikeMessengerApp.getInstance().getApplicationContext(), DB_NAME, null, DB_VERSION);
		mDB = getWritableDatabase();
	}

	public static HikeContentDatabase getInstance()
	{
		return hikeContentDatabase;
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		mDB = db;
		String[] createQueries = getCreateQueries();
		for (String create : createQueries)
		{
			db.execSQL(create);
		}

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		mDB = db;
		// CREATE all tables, it is possible that few tables are created in this version
		onCreate(mDB);
		String[] updateQueries = getUpdateQueries(oldVersion, newVersion);
		for (String update : updateQueries)
		{
			db.execSQL(update);
		}

		// DO any other update operation here
	}

	private String[] getCreateQueries()
	{
		String[] createAndIndexes = new String[5];
		int i = 0;
		// CREATE TABLE
		// CONTENT TABLE -> _id,content_id,love_id,channel_id,timestamp,metadata
		String contentTable = CREATE_TABLE +CONTENT_TABLE
				+ "("
				+_ID +" INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ CONTENT_ID+" INTEGER UNIQUE, "
				+ NAMESPACE + " TEXT, "
				+ LOVE_ID+ " INTEGER, "
				+CHANNEL_ID+" INTEGER, "
				+HIKE_CONTENT.TIMESTAMP+" INTEGER, "
				+METADATA+" TEXT"
				+")";

		createAndIndexes[i++] = contentTable;
		//CREATE TABLE 
		
		//ALARM TABLE->id,time,willWakeCpu,time,intent
		
		String alarmTable = CREATE_TABLE + ALARM_MGR_TABLE 
				+ "("
				+ _ID + " INTEGER PRIMARY KEY, "
				+ TIME + " TEXT, "
				+ WILL_WAKE_CPU + " INTEGER, "
				+ INTENT + " TEXT," 
				+ HIKE_CONV_DB.TIMESTAMP + " INTEGER" + ")";
		createAndIndexes[i++]=alarmTable;

		String popupDB = CREATE_TABLE + POPUPDATA + "("
				  +_ID +" INTEGER PRIMARY KEY ,"
				  + POPUPDATA + " TEXT ," 
				  + STATUS + " INTEGER ," 
				  + START_TIME + " INTEGER," 
				  + END_TIME + " INTEGER," 
				  + TRIGGER_POINT + " INTEGER " + ")";
		// APP_ALARM TABLE - > id, data
		String contentIndex = CREATE_INDEX + CONTENT_ID_INDEX + " ON " + CONTENT_TABLE + " (" + CONTENT_ID + ")";

		createAndIndexes[i++] = popupDB;

		String nameSpaceIndex = CREATE_INDEX + CONTENT_TABLE_NAMESPACE_INDEX + " ON " + CONTENT_TABLE + " (" + NAMESPACE + ")";

		
		createAndIndexes[i++] = contentIndex;
		createAndIndexes[i++] = nameSpaceIndex;
		// INDEX ENDS HERE

		return createAndIndexes;
	}

	private String[] getUpdateQueries(int oldVersion, int newVersion)
	{
		String[] updateAndIndexes = new String[0];
		// UPDATE TABLE

		// UPDATE INDEXES
		return updateAndIndexes;
	}

	public void insertIntoAlarmManagerDB(long time, int requestCode, boolean WillWakeCPU, Intent intent)
	{
		ContentValues cv = new ContentValues();
		cv.put(_ID, requestCode);
		cv.put(TIME, time + "");
		cv.put(WILL_WAKE_CPU, WillWakeCPU);
		cv.put(INTENT, intent.toUri(0));

		mDB.insertWithOnConflict(ALARM_MGR_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
	}

	public void deleteFromAlarmManagerDB(int requestCode)
	{
		mDB.delete(ALARM_MGR_TABLE, _ID + "=" + requestCode, null);
	}

	public void rePopulateAlarmWhenClosed()
	{
		Logger.d(HikeAlarmManager.TAG, "Populating alarm started");
		String selectQuery = "SELECT  * FROM " + ALARM_MGR_TABLE;

		Cursor cursor = mDB.rawQuery(selectQuery, null);
		try
		{
			if (cursor.moveToFirst())
			{
				do
				{
					Logger.d(HikeAlarmManager.TAG, "rePopulating  Alarms");
					int requestCode = cursor.getInt(cursor.getColumnIndex(_ID));
					long time = Long.parseLong(cursor.getString(cursor.getColumnIndex(TIME)));
					int willWakeCpu = cursor.getInt(cursor.getColumnIndex(WILL_WAKE_CPU));
					String intent = cursor.getString(cursor.getColumnIndex(INTENT));
					Uri asd = Uri.parse(intent);

					Intent intentAlarm = Intent.getIntent(asd.toString());

					HikeAlarmManager.setAlarmWithIntent(HikeMessengerApp.getInstance(), time, requestCode, (willWakeCpu != 0), intentAlarm);

				}
				while (cursor.moveToNext());
			}
		}
		catch (URISyntaxException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param pkt
	 * @param notifTime
	 * @param TriggerPoint
	 * 
	 *            Saving the popUp in the database
	 */
	public void savePopup(ProductContentModel productContentModel, int status)
	{
		ContentValues cv = new ContentValues();
		cv.put(POPUPDATA, productContentModel.toJSONString());
		cv.put(STATUS, status);
		cv.put(START_TIME, productContentModel.getStarttime());
		cv.put(END_TIME, productContentModel.getEndtime());
		cv.put(TRIGGER_POINT, productContentModel.getTriggerpoint());
		cv.put(_ID, productContentModel.hashCode());
		long val = mDB.insertWithOnConflict(POPUPDATA, null, cv,SQLiteDatabase.CONFLICT_REPLACE);
		Logger.d("ProductPopup", "DB Inserted Successfully..." + val + "");
	}

	/**
	 * 
	 * @return
	 * 
	 * This method is responsible for getting the popup data from the DB to memory.
	 * 
	 * This is called only once from the the HikeMessageApp (onCreate)
	 */
	public SparseArray<ArrayList<ProductContentModel>> getAllPopup()
	{
		Logger.d("ProductPopup", "getAllPopup\n");
		SparseArray<ArrayList<ProductContentModel>> mmSparseArray = new SparseArray<ArrayList<ProductContentModel>>();
		JSONObject productPopupModel=null;
		Cursor c=null;
		ArrayList<ProductContentModel> mmArray=new ArrayList<ProductContentModel>();
		try
		{
			String query = "select * from "+POPUPDATA +" order by "+ TRIGGER_POINT;
			
			c = mDB.rawQuery(query, null);

			if (c.moveToFirst())
			{
				do
				{
					int triggerPoint = c.getInt(c.getColumnIndex(TRIGGER_POINT));
					int startTime = c.getInt(c.getColumnIndex(START_TIME));
					String json = c.getString(c.getColumnIndex(POPUPDATA));
					int endTime = c.getInt(c.getColumnIndex(END_TIME));
					productPopupModel = new JSONObject(json);

					if (mmSparseArray.get(triggerPoint) == null)
					{
						mmArray = new ArrayList<ProductContentModel>();
						mmArray.add(ProductContentModel.makeProductContentModel(productPopupModel));
						mmSparseArray.put(triggerPoint, mmArray);
					}
					else
					{
						mmArray = mmSparseArray.get(triggerPoint);
						mmArray.add(ProductContentModel.makeProductContentModel(productPopupModel));
						mmSparseArray.put(triggerPoint, mmArray);
					}
					Logger.d("ProductPopup>", triggerPoint + " >" + startTime + ">>>" + endTime);

				}
				while ((c.moveToNext()));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(c!=null)
			{
				c.close();
			}
		}

		return mmSparseArray;
	}

	
	/**
	 * 
	 * @param args
	 * 
	 * Deleting the Popup from the Database
	 */
	public void deletePopup(String[] args)
	{	
			String id=TextUtils.join(", ", args);
			Logger.d("ProductPopup","ids deletd are "+id+"<<<<<command to be excetuing"+String.format("DELETE FROM " + POPUPDATA + " WHERE "+ _ID+ " IN ( "+ id + ")" ));
			mDB.execSQL(String.format("DELETE FROM " + POPUPDATA + " WHERE "+ _ID+ " IN ( "+ id + ")"));
	}
	
	/**
	 * 
	 * @param hashcode
	 * @param status
	 * 
	 * Updating the status of the Popup.
	 */
	public void updatePopupStatus(int hashcode, int status)
	{
		ContentValues cv = new ContentValues();
		cv.put(STATUS, status);
		mDB.update(POPUPDATA, cv, _ID + "= " + hashcode, null);
	}
	
	
	
}
