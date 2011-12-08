package com.bsb.hike.utils;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.bsb.hike.models.Conversation;

public class HikeConversationsDatabase extends SQLiteOpenHelper {

	private SQLiteDatabase mDb;
	public HikeConversationsDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		mDb = getWritableDatabase();
	}

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "hikeconversations";
	private static final String DATABASE_TABLE = "conversations";

	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql = "CREATE TABLE IF NOT EXISTS " + DATABASE_TABLE + 
				"(message STRING, id INTEGER, sent INTEGER, timestamp INTEGER, msgid INTEGER)";
		db.execSQL(sql);
		sql = "CREATE INDEX IF NOT EXISTS conversation_idx ON " + DATABASE_TABLE + "( id, timestamp DESC)";
		db.execSQL(sql);
		sql = "CREATE INDEX IF NOT EXISTS msgid_idx ON " + DATABASE_TABLE + "( msgid)";
		db.execSQL(sql);
	}

	public void clearDatabase(SQLiteDatabase db) {
		if (db == null) {
			db = mDb;
		}

		db.execSQL("DROP INDEX IF EXISTS conversation_idx");
		db.execSQL("DROP INDEX IF EXISTS msgid_idx");
		db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
	}

	@Override
	public synchronized void close() {
		super.close();
		mDb.close();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		clearDatabase(db);
		onCreate(db);
	}

	public void addConversations(List<Conversation> conversations) {
		mDb.beginTransaction();
		InsertHelper ih = new InsertHelper(mDb, DATABASE_TABLE);
		final int idColumn = ih.getColumnIndex("id");
		final int msgColumn = ih.getColumnIndex("message");
		final int sentColumn = ih.getColumnIndex("sent");
		final int tsColumn = ih.getColumnIndex("timestamp");
		for(Conversation conv : conversations) {
			ih.prepareForReplace();
			ih.bind(msgColumn, conv.getMessage());
			ih.bind(idColumn, conv.getId());
			ih.bind(sentColumn, conv.isSent());
			ih.bind(tsColumn, conv.getTimestamp());
			ih.execute();
		}
	}

	public List<Conversation> getConversationThread(long id, int limit) {
		String limitStr = new Integer(limit).toString();
		Cursor c = mDb.query(DATABASE_TABLE, new String[] {"message, id, sent, timestamp"}, "id=?", new String[] {Long.toString(id)}, null, null, "timestamp DESC", limitStr);
		final int idColumn = c.getColumnIndex("id");
		final int msgColumn = c.getColumnIndex("message");
		final int sentColumn = c.getColumnIndex("sent");
		final int tsColumn = c.getColumnIndex("timestamp");
		List<Conversation> elements = new ArrayList<Conversation>(c.getCount());		
		while (c.moveToNext()) {
			Conversation conv = new Conversation(c.getString(msgColumn), c.getLong(idColumn), c.getInt(tsColumn),  c.getInt(sentColumn) != 0);
			elements.add(conv);
		}

		if (elements.isEmpty()) {
			return null;
		}
		return elements;
	}	
}
