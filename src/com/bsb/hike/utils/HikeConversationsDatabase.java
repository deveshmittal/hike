package com.bsb.hike.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;

public class HikeConversationsDatabase extends SQLiteOpenHelper
{

	private SQLiteDatabase mDb;

	private Context mCtx;

	public HikeConversationsDatabase(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		mDb = getWritableDatabase();
		mCtx = context;
	}

	private static final int DATABASE_VERSION = 1;

	private static final String MESSAGESTABLE = "messages";

	private static final String CONVERSATIONSTABLE = "conversations";

	private static final String DATABASE_NAME = "chats";

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		if (db == null)
		{
			db = mDb;
		}
		String sql = "CREATE TABLE IF NOT EXISTS " + MESSAGESTABLE + "(message STRING, " + "msgStatus INTEGER, " + // this is to check if msg sent or recieved of the msg sent.
				"timestamp INTEGER, " + "msgid INTEGER PRIMARY KEY AUTOINCREMENT," + "convid INTEGER)";

		db.execSQL(sql);
		sql = "CREATE INDEX IF NOT EXISTS conversation_idx ON " + MESSAGESTABLE + "( convid, timestamp DESC)";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + CONVERSATIONSTABLE + "(convid INTEGER PRIMARY KEY AUTOINCREMENT, " + "onhike INTEGER, " + "contactid STRING, " + "msisdn UNIQUE)";
		db.execSQL(sql);
	}

	public void clearDatabase(SQLiteDatabase db)
	{
		if (db == null)
		{
			db = mDb;
		}

		db.execSQL("DROP TABLE IF EXISTS " + CONVERSATIONSTABLE);
		db.execSQL("DROP TABLE IF EXISTS " + MESSAGESTABLE);
	}

	@Override
	public synchronized void close()
	{
		super.close();
		mDb.close();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		clearDatabase(db);
		onCreate(db);
	}

	public long addConversationMessages(ConvMessage message)
	{
		List<ConvMessage> l = new ArrayList<ConvMessage>(1);
		l.add(message);
		return addConversations(l);
	}

	public int updateMsgStatus(long convID, long msgID, int val)
	{
		ContentValues values = new ContentValues();
		values.put("msgStatus", val);
		String whereClause;
		if (convID > 0 && msgID <= 0) // this is to handle the case where we need to update status for a particular convID
		{
			String[] whereArgs = { String.valueOf(convID) };
			whereClause = "convid=?";
			return mDb.update(MESSAGESTABLE, values, "convid=?", whereArgs);
		}
		else if(convID > 0 && msgID > 0) // update status for msgID=X and convID=y
		{
			String[] whereArgs = { String.valueOf(msgID), String.valueOf(convID)};
			whereClause="msgid=? and convid=?";
			return mDb.update(MESSAGESTABLE, values, "msgid=? and convid=?", whereArgs);
		}
		else
		{
			String[] whereArgs = { String.valueOf(msgID) };
			whereClause="msgid=?";
			return mDb.update(MESSAGESTABLE, values, "msgid=?", whereArgs);
		}
	}

	public void updateConversation()
	{

	}

	private void bindConversationInsert(SQLiteStatement insertStatement, ConvMessage conv)
	{
		final int messageColumn = 1;
		final int msgStatusColumn = 2;
		final int timestampColumn = 3;
		final int msisdnColumn = 4;

		insertStatement.clearBindings();
		insertStatement.bindString(messageColumn, conv.getMessage());
		// 0 -> SENT_UNCONFIRMED ; 1 -> SENT_CONFIRMED ; 2 -> RECEIVED_UNREAD ;
		// 3 -> RECEIVED_READ
		insertStatement.bindLong(msgStatusColumn, conv.getState().ordinal());
		insertStatement.bindLong(timestampColumn, conv.getTimestamp());
		insertStatement.bindString(msisdnColumn, conv.getMsisdn());
	}

	public long addConversations(List<ConvMessage> convMessages)
	{
		SQLiteStatement insertStatement = mDb.compileStatement("INSERT INTO " + MESSAGESTABLE + " (message, msgStatus, timestamp, convid) " + "SELECT ?, ?, ?, convid FROM "
				+ CONVERSATIONSTABLE + " WHERE " + CONVERSATIONSTABLE + ".msisdn=?");
		mDb.beginTransaction();

		long msgId = -1;

		for (ConvMessage conv : convMessages)
		{
			bindConversationInsert(insertStatement, conv);
			msgId = insertStatement.executeInsert();
			if (msgId <= 0)
			{
				Conversation conversation = addConversation(conv.getMsisdn());
				if (conversation != null)
				{
					conversation.addMessage(conv);
				}
				bindConversationInsert(insertStatement, conv);
				msgId = insertStatement.executeInsert();
				assert (msgId >= 0);
			}
		}

		mDb.setTransactionSuccessful();
		mDb.endTransaction();
		return msgId;
	}

	public void deleteConversation(Long[] ids)
	{
		mDb.beginTransaction();
		for (int i = 0; i < ids.length; i++)
		{
			Long[] bindArgs = new Long[] { ids[i] };
			mDb.execSQL("DELETE FROM " + CONVERSATIONSTABLE + " WHERE convid = ?", bindArgs);
			mDb.execSQL("DELETE FROM " + MESSAGESTABLE + " WHERE convid = ?", bindArgs);
		}
		mDb.setTransactionSuccessful();
		mDb.endTransaction();
	}

	public Conversation addConversation(String msisdn)
	{
		HikeUserDatabase huDb = new HikeUserDatabase(mCtx);
		ContactInfo contactInfo = huDb.getContactInfoFromMSISDN(msisdn);
		huDb.close();
		InsertHelper ih = new InsertHelper(mDb, CONVERSATIONSTABLE);
		ih.prepareForInsert();
		ih.bind(ih.getColumnIndex("msisdn"), msisdn);
		if (contactInfo != null)
		{
			ih.bind(ih.getColumnIndex("contactid"), contactInfo.id);
			ih.bind(ih.getColumnIndex("onhike"), contactInfo.onhike);
		}
		long id = ih.execute();
		if (id >= 0)
		{
			Conversation conv = new Conversation(msisdn, id, (contactInfo != null) ? contactInfo.id : null, (contactInfo != null) ? contactInfo.name : null,
					(contactInfo != null) ? contactInfo.onhike : false);
			HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_CONVERSATION, conv);
			return conv;
		}
		/* TODO does this happen? If so, what should we do? */
		Log.wtf("COnversationadding", "Couldn't add conversation --- race condition?");
		return null;
	}

	private List<ConvMessage> getConversationThread(String msisdn, String contactid, long convid, int limit, Conversation conversation)
	{
		String limitStr = new Integer(limit).toString();
		Cursor c = mDb.query(MESSAGESTABLE, new String[] { "message, msgStatus, timestamp" }, "convid=?", new String[] { Long.toString(convid) }, null, null, "msgid DESC",
				limitStr);
		final int msgColumn = c.getColumnIndex("message");
		final int msgStatusColumn = c.getColumnIndex("msgStatus");
		final int tsColumn = c.getColumnIndex("timestamp");
		List<ConvMessage> elements = new ArrayList<ConvMessage>(c.getCount());
		while (c.moveToNext())
		{
			ConvMessage message = new ConvMessage(c.getString(msgColumn), msisdn, c.getInt(tsColumn), ConvMessage.stateValue(c.getInt(msgStatusColumn)));
			elements.add(elements.size(), message);
			message.setConversation(conversation);
		}
		Collections.reverse(elements);
		c.close();

		return elements;
	}

	public Conversation getConversation(String msisdn, int limit)
	{
		Cursor c = mDb.query(CONVERSATIONSTABLE, new String[] { "convid", "contactid" }, "msisdn=?", new String[] { msisdn }, null, null, null);
		if (!c.moveToFirst())
		{
			return null;
		}

		long convid = c.getInt(c.getColumnIndex("convid"));
		String contactid = c.getString(c.getColumnIndex("contactid"));
		c.close();

		HikeUserDatabase huDb = new HikeUserDatabase(mCtx);
		ContactInfo contactInfo = huDb.getContactInfoFromMSISDN(msisdn);
		huDb.close();
		Conversation conv = new Conversation(msisdn, convid, contactid, (contactInfo != null) ? contactInfo.name : null, (contactInfo != null) ? contactInfo.onhike : false);
		List<ConvMessage> messages = getConversationThread(msisdn, contactid, convid, limit, conv);
		conv.setMessages(messages);
		return conv;
	}

	public List<Conversation> getConversations()
	{
		Cursor c = mDb.query(CONVERSATIONSTABLE, new String[] { "convid, contactid", "msisdn" }, null, null, null, null, null);
		List<Conversation> conversations = new ArrayList<Conversation>();
		final int msisdnIdx = c.getColumnIndex("msisdn");
		final int convIdx = c.getColumnIndex("convid");
		final int contactIdx = c.getColumnIndex("contactid");
		HikeUserDatabase huDb = null;
		try
		{
			huDb = new HikeUserDatabase(mCtx);
			while (c.moveToNext())
			{
				// TODO this can be expressed in a single sql query
				String msisdn = c.getString(msisdnIdx);
				ContactInfo contactInfo = huDb.getContactInfoFromMSISDN(msisdn);
				Conversation conv = new Conversation(msisdn, c.getLong(convIdx), c.getString(contactIdx), (contactInfo != null) ? contactInfo.name : null,
						(contactInfo != null) ? contactInfo.onhike : false);
				conv.setMessages(getConversationThread(conv.getMsisdn(), conv.getContactId(), conv.getConvId(), 1, conv));
				conversations.add(conv);
			}
		}
		catch (Exception e)
		{
			Log.e("HikeConversationsDatabase", "Unable to retrieve conversations", e);
		}
		finally
		{
			huDb.close();
		}
		Collections.sort(conversations, Collections.reverseOrder());
		return conversations;
	}

}
