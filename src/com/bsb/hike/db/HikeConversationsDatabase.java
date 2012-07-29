package com.bsb.hike.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.utils.Utils;

public class HikeConversationsDatabase extends SQLiteOpenHelper
{

	private SQLiteDatabase mDb;

	private static HikeConversationsDatabase hikeConversationsDatabase;

	public static void init(Context context)
	{
		if(hikeConversationsDatabase == null)
		{
			hikeConversationsDatabase = new HikeConversationsDatabase(context);
		}
	}

	public static HikeConversationsDatabase getInstance()
	{
		return hikeConversationsDatabase;
	}

	private HikeConversationsDatabase(Context context)
	{
		super(context, DBConstants.CONVERSATIONS_DATABASE_NAME, null, DBConstants.CONVERSATIONS_DATABASE_VERSION);
		mDb = getWritableDatabase();
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		if (db == null)
		{
			db = mDb;
		}
		String sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.MESSAGES_TABLE 
				+ " ( "
				+ DBConstants.MESSAGE +" STRING, " 
				+ DBConstants.MSG_STATUS+" INTEGER, " /* this is to check if msg sent or recieved of the msg sent. */
				+ DBConstants.TIMESTAMP+" INTEGER, "
				+ DBConstants.MESSAGE_ID+" INTEGER PRIMARY KEY AUTOINCREMENT, " 
				+ DBConstants.MAPPED_MSG_ID+" INTEGER, " 
				+ DBConstants.CONV_ID+" INTEGER,"
				+ DBConstants.MESSAGE_METADATA + " TEXT, "
				+ DBConstants.GROUP_PARTICIPANT+" TEXT"
				+ " ) ";

		db.execSQL(sql);
		sql = "CREATE INDEX IF NOT EXISTS "+DBConstants.CONVERSATION_INDEX +" ON " + DBConstants.MESSAGES_TABLE  
				+ " ( "
				+ DBConstants.CONV_ID+" , "
				+ DBConstants.TIMESTAMP +" DESC"
				+ " )";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.CONVERSATIONS_TABLE 
				+ " ( "
				+ DBConstants.CONV_ID +" INTEGER PRIMARY KEY AUTOINCREMENT, " 
				+ DBConstants.ONHIKE +" INTEGER, " 
				+ DBConstants.CONTACT_ID +" STRING, " 
				+ DBConstants.MSISDN +" UNIQUE, "
				+ DBConstants.OVERLAY_DISMISSED+" INTEGER"
				+ " )";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.GROUP_MEMBERS_TABLE
				+ " ( "
				+ DBConstants.GROUP_ID +" STRING, "
				+ DBConstants.MSISDN + " TEXT, "
				+ DBConstants.NAME + " STRING, "
				+ DBConstants.HAS_LEFT + " INTEGER"
				+ " )";
		db.execSQL(sql);
		sql = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.GROUP_INDEX + " ON " + DBConstants.GROUP_MEMBERS_TABLE 
				+ " ( "
				+ DBConstants.GROUP_ID + ", "
				+ DBConstants.MSISDN
				+ " ) ";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.GROUP_INFO_TABLE
				+ " ( "
				+ DBConstants.GROUP_ID +" STRING PRIMARY KEY, "
				+ DBConstants.GROUP_NAME + " TEXT, "
				+ DBConstants.GROUP_OWNER + " TEXT, "
				+ DBConstants.GROUP_ALIVE + " INTEGER"
				+ " )";
		db.execSQL(sql);
	}

	public void deleteAll()
	{
		mDb.delete(DBConstants.CONVERSATIONS_TABLE, null, null);
		mDb.delete(DBConstants.MESSAGES_TABLE, null, null);
		mDb.delete(DBConstants.GROUP_MEMBERS_TABLE, null, null);
		mDb.delete(DBConstants.GROUP_INFO_TABLE, null, null);
	}

	@Override
	public void close()
	{
		super.close();
		mDb.close();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		if (db == null)
		{
			db = mDb;
		}

		db.execSQL("DROP TABLE IF EXISTS " + DBConstants.CONVERSATIONS_TABLE);
		db.execSQL("DROP TABLE IF EXISTS " + DBConstants.MESSAGES_TABLE);
		db.execSQL("DROP TABLE IF EXISTS " + DBConstants.GROUP_MEMBERS_TABLE);
		db.execSQL("DROP TABLE IF EXISTS " + DBConstants.GROUP_INFO_TABLE);
		onCreate(db);
	}

	public void updateOnHikeStatus(String msisdn, boolean onHike)
	{
		ContentValues values = new ContentValues();
		values.put(DBConstants.ONHIKE, onHike);
		String[] whereArgs = { msisdn };
		int rowsAffected = mDb.update(DBConstants.CONVERSATIONS_TABLE, values, DBConstants.MSISDN + "=?", whereArgs);
	}

	public void addConversationMessages(ConvMessage message)
	{
		List<ConvMessage> l = new ArrayList<ConvMessage>(1);
		l.add(message);
		addConversations(l);
	}

	public void updateMsgStatus(long msgID, int val)
	{
		ContentValues values = new ContentValues();
		values.put(DBConstants.MSG_STATUS, val);
		String[] whereArgs = { String.valueOf(msgID), String.valueOf(val) };
		int rowsAffected = mDb.update(DBConstants.MESSAGES_TABLE, values,
				DBConstants.MESSAGE_ID + "=? AND " + DBConstants.MSG_STATUS
				+ " < ?", whereArgs);
		Log.d("HIKE CONVERSATION DB", "Update Msg status to : "
				+ ConvMessage.stateValue(val) + "	;	for msgID : " + msgID
				+ "	;	Rows Affected : " + rowsAffected);
	}

	private void bindConversationInsert(SQLiteStatement insertStatement, ConvMessage conv)
	{
		final int messageColumn = 1;
		final int msgStatusColumn = 2;
		final int timestampColumn = 3;
		final int mappedMsgIdColumn = 4;
		final int messageMetadataColumn = 5;
		final int groupParticipant = 6;
		final int msisdnColumn = 7;

		insertStatement.clearBindings();
		insertStatement.bindString(messageColumn, conv.getMessage());
		// 0 -> SENT_UNCONFIRMED ; 1 -> SENT_CONFIRMED ; 2 -> RECEIVED_UNREAD ;
		// 3 -> RECEIVED_READ
		insertStatement.bindLong(msgStatusColumn, conv.getState().ordinal());
		insertStatement.bindLong(timestampColumn, conv.getTimestamp());
		insertStatement.bindLong(mappedMsgIdColumn, conv.getMappedMsgID());
		insertStatement.bindString(msisdnColumn, conv.getMsisdn());
		insertStatement.bindString(messageMetadataColumn, conv.getMetadata() != null ? conv.getMetadata().serialize() : "");
		insertStatement.bindString(groupParticipant, conv.getGroupParticipantMsisdn() != null ? conv.getGroupParticipantMsisdn() : "");
	}

	public boolean wasMessageReceived(ConvMessage conv)
	{
		Log.d("HikeConversationsDatabase", "CHECKING MESSAGE ID: " + conv.getMappedMsgID()
				+ " MESSAGE TIMESTAMP: " + conv.getTimestamp());
		Cursor c = mDb.query(DBConstants.MESSAGES_TABLE + "," + DBConstants.CONVERSATIONS_TABLE,
				new String[] { DBConstants.MESSAGE },
				DBConstants.MAPPED_MSG_ID + "=? AND "
						+ DBConstants.TIMESTAMP + "=? AND "
						+ DBConstants.CONVERSATIONS_TABLE + "."
						+ DBConstants.MSISDN + "=?",
						new String[] { Long.toString(conv.getMappedMsgID()),
				Long.toString(conv.getTimestamp()),
				conv.getMsisdn() }, null, null, null);
		int count = c.getCount();
		c.close();
		return (count != 0);
	}

	public void addConversations(List<ConvMessage> convMessages)
	{
		SQLiteStatement insertStatement = mDb.compileStatement("INSERT INTO " + DBConstants.MESSAGES_TABLE 
				+ " ( "
				+ DBConstants.MESSAGE+","
				+ DBConstants.MSG_STATUS+","
				+ DBConstants.TIMESTAMP+","
				+ DBConstants.MAPPED_MSG_ID+" ,"
				+ DBConstants.MESSAGE_METADATA +","
				+ DBConstants.GROUP_PARTICIPANT + ","
				+ DBConstants.CONV_ID
				+ " ) "
				+ " SELECT ?, ?, ?, ?, ?, ?,"+ DBConstants.CONV_ID 
				+ " FROM " + DBConstants.CONVERSATIONS_TABLE 
				+ " WHERE " + DBConstants.CONVERSATIONS_TABLE + "."+DBConstants.MSISDN+"=?");
		mDb.beginTransaction();

		long msgId = -1;

		for (ConvMessage conv : convMessages)
		{
			bindConversationInsert(insertStatement, conv);
			msgId = insertStatement.executeInsert();
			/* Represents we dont have any conversation made for this msisdn. Here we are also checking whether the message is a group message, If it is and the conversation does not exist we do not add a conversation.*/
			if (msgId <= 0 && !Utils.isGroupConversation(conv.getMsisdn()))
			{
				Conversation conversation = addConversation(conv.getMsisdn(), !conv.isSMS(), null, null);
				if (conversation != null)
				{
					conversation.addMessage(conv);
				}
				bindConversationInsert(insertStatement, conv);
				msgId = insertStatement.executeInsert();
				conv.setConversation(conversation);
				assert (msgId >= 0);
			}
			else if (conv.getConversation() == null)
			{
				//conversation not set, retrieve it from db
				Conversation conversation = this.getConversation(conv.getMsisdn(), 0);
				conv.setConversation(conversation);
			}
			conv.setMsgID(msgId);
		}

		insertStatement.close();
		mDb.setTransactionSuccessful();
		mDb.endTransaction();
	}

	public void deleteConversation(Long[] ids, List<String> msisdns)
	{
		mDb.beginTransaction();
		for (int i = 0; i < ids.length; i++)
		{
			Long[] bindArgs = new Long[] { ids[i] };
			String msisdn = msisdns.get(i);
			mDb.execSQL("DELETE FROM " + DBConstants.CONVERSATIONS_TABLE + " WHERE "+ DBConstants.CONV_ID +"= ?", bindArgs);
			mDb.execSQL("DELETE FROM " + DBConstants.MESSAGES_TABLE + " WHERE "+ DBConstants.CONV_ID +"= ?", bindArgs);
			if (Utils.isGroupConversation(msisdn))
			{
				mDb.delete(DBConstants.GROUP_MEMBERS_TABLE, DBConstants.GROUP_ID + " =?", new String[]{msisdn});
				mDb.delete(DBConstants.GROUP_INFO_TABLE, DBConstants.GROUP_ID + " =?", new String[]{msisdn});
			}
		}
		mDb.setTransactionSuccessful();
		mDb.endTransaction();
	}

	/**
	 * Add a conversation to the db
	 * @param msisdn the msisdn of the contact
	 * @param onhike true iff the contact is onhike.  If this is false, we consult the local db as well
	 * @param groupName the name of the group. Sent as <code>null</code> if the conversation is not a group conversation
	 * @return Conversation object representing the conversation
	 */
	public Conversation addConversation(String msisdn, boolean onhike, String groupName, String groupOwner)
	{
		HikeUserDatabase huDb = HikeUserDatabase.getInstance();
		ContactInfo contactInfo = Utils.isGroupConversation(msisdn) ?  new ContactInfo(msisdn, msisdn, groupName, msisdn) : huDb.getContactInfoFromMSISDN(msisdn);
		InsertHelper ih = null;
		try 
		{
			ih = new InsertHelper(mDb, DBConstants.CONVERSATIONS_TABLE);
			ih.prepareForInsert();
			ih.bind(ih.getColumnIndex(DBConstants.MSISDN), msisdn);
			if (contactInfo != null)
			{
				ih.bind(ih.getColumnIndex(DBConstants.CONTACT_ID), contactInfo.getId());
				onhike |= contactInfo.isOnhike();
			}

			ih.bind(ih.getColumnIndex(DBConstants.ONHIKE), onhike);

			long id = ih.execute();

			if (id >= 0)
			{
				Conversation conv;
				if (Utils.isGroupConversation(msisdn)) 
				{
					conv = new GroupConversation(msisdn, id, (contactInfo != null) ? contactInfo.getId() : null, (contactInfo != null) ? contactInfo.getName() : null, groupOwner, true);
					Log.d(getClass().getSimpleName(), "Adding a new group conversation: " + msisdn);
					InsertHelper groupInfoIH = null;
					try 
					{
						groupInfoIH = new InsertHelper(mDb, DBConstants.GROUP_INFO_TABLE);
						groupInfoIH.prepareForInsert();
						groupInfoIH.bind(groupInfoIH.getColumnIndex(DBConstants.GROUP_ID), msisdn);
						groupInfoIH.bind(groupInfoIH.getColumnIndex(DBConstants.GROUP_NAME), groupName);
						groupInfoIH.bind(groupInfoIH.getColumnIndex(DBConstants.GROUP_OWNER), groupOwner);
						groupInfoIH.bind(groupInfoIH.getColumnIndex(DBConstants.GROUP_ALIVE), 1);
						groupInfoIH.execute();
					} 
					finally 
					{
						if (groupInfoIH != null) 
						{
							groupInfoIH.close();
						}
					}

					Log.d(getClass().getSimpleName(), "Fetching participants...");
					((GroupConversation)conv).setGroupParticipantList(getGroupParticipants(msisdn, false));
					Log.d(getClass().getSimpleName(), "Participants size: " + ((GroupConversation)conv).getGroupParticipantList().size());
				}
				else
				{
					conv = new Conversation(msisdn, id, (contactInfo != null) ? contactInfo.getId() : null, (contactInfo != null) ? contactInfo.getName() : null, onhike);
				}
				HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_CONVERSATION, conv);
				return conv;
			}
			/* TODO does this happen? If so, what should we do? */
			Log.wtf("Conversationadding", "Couldn't add conversation --- race condition?");
			return null;
		} 
		finally 
		{
			if (ih != null) 
			{
				ih.close();
			}
		}
	}

	private List<ConvMessage> getConversationThread(String msisdn, String contactid, long convid, int limit, Conversation conversation)
	{
		String limitStr = new Integer(limit).toString();
		/*TODO this should be ORDER BY timestamp */
		Cursor c = mDb.query(DBConstants.MESSAGES_TABLE, 
				new String[] {    
				DBConstants.MESSAGE,
				DBConstants.MSG_STATUS,
				DBConstants.TIMESTAMP,
				DBConstants.MESSAGE_ID,
				DBConstants.MAPPED_MSG_ID,
				DBConstants.MESSAGE_METADATA,
				DBConstants.GROUP_PARTICIPANT
		}, 
		DBConstants.CONV_ID + " = ?", 
		new String[] { Long.toString(convid) }, null, null,
		DBConstants.MESSAGE_ID+" DESC", limitStr);

		final int msgColumn = c.getColumnIndex(DBConstants.MESSAGE);
		final int msgStatusColumn = c.getColumnIndex(DBConstants.MSG_STATUS);
		final int tsColumn = c.getColumnIndex(DBConstants.TIMESTAMP);
		final int mappedMsgIdColumn = c.getColumnIndex(DBConstants.MAPPED_MSG_ID);
		final int msgIdColumn = c.getColumnIndex(DBConstants.MESSAGE_ID);
		final int metadataColumn = c.getColumnIndex(DBConstants.MESSAGE_METADATA);
		final int groupParticipantColumn = c.getColumnIndex(DBConstants.GROUP_PARTICIPANT);
		List<ConvMessage> elements = new ArrayList<ConvMessage>(c.getCount());
		while (c.moveToNext())
		{
			ConvMessage message = new ConvMessage(c.getString(msgColumn), msisdn, c.getInt(tsColumn), ConvMessage.stateValue(c.getInt(msgStatusColumn)), c.getLong(msgIdColumn),
					c.getLong(mappedMsgIdColumn), c.getString(groupParticipantColumn));
			String metadata = c.getString(metadataColumn);
			try
			{
				message.setMetadata(metadata);
			}
			catch (JSONException e)
			{
				Log.e(HikeConversationsDatabase.class.getName(), "Invalid JSON metadata", e);
			}
			elements.add(elements.size(), message);
			message.setConversation(conversation);
		}
		Collections.reverse(elements);
		c.close();

		return elements;
	}

	public Conversation getConversation(String msisdn, int limit)
	{
		Log.d(getClass().getSimpleName(), "Fetching conversation with msisdn: " + msisdn);
		Cursor c = null;
		HikeUserDatabase huDb = null;
		Conversation conv = null;
		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, 
					new String[] 
							{ 
					DBConstants.CONV_ID, 
					DBConstants.CONTACT_ID,
					DBConstants.ONHIKE
							}, 
							DBConstants.MSISDN+"=?", 
							new String[] 
									{ 
					msisdn 
									}, 
									null, null, null);
			if (!c.moveToFirst())
			{
				Log.d(getClass().getSimpleName(), "Could not find db entry");
				return null;
			}

			long convid = c.getInt(c.getColumnIndex(DBConstants.CONV_ID));
			String contactid = c.getString(c.getColumnIndex(DBConstants.CONTACT_ID));
			boolean onhike = c.getInt(c.getColumnIndex(DBConstants.ONHIKE)) != 0;

			if(Utils.isGroupConversation(msisdn))
			{
				conv = getGroupConversation(msisdn, convid, contactid);
			}
			else
			{
				huDb = HikeUserDatabase.getInstance();
				ContactInfo contactInfo = huDb.getContactInfoFromMSISDN(msisdn);

				onhike |= contactInfo.isOnhike();
				conv = new Conversation(msisdn, convid, contactid, contactInfo.getName(), onhike);

			}
			if (limit > 0) 
			{
				List<ConvMessage> messages = getConversationThread(msisdn, contactid, convid, limit, conv);
				conv.setMessages(messages);
			}
			return conv;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public List<Conversation> getConversations()
	{
		Cursor c = mDb.query(DBConstants.CONVERSATIONS_TABLE, 
				new String[] 
						{ 
				DBConstants.CONV_ID,
				DBConstants.CONTACT_ID,
				DBConstants.MSISDN
						}, 
						null, null, null, null, null);

		List<Conversation> conversations = new ArrayList<Conversation>();
		final int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
		final int convIdx = c.getColumnIndex(DBConstants.CONV_ID);
		final int contactIdx = c.getColumnIndex(DBConstants.CONTACT_ID);

		HikeUserDatabase huDb = null;
		try
		{
			huDb = HikeUserDatabase.getInstance();
			while (c.moveToNext())
			{
				Conversation conv;
				// TODO this can be expressed in a single sql query
				String msisdn = c.getString(msisdnIdx);
				long convid = c.getInt(convIdx);
				String contactid = c.getString(contactIdx);
				Log.d(getClass().getSimpleName(), "Fetching Converstaions: " + msisdn);
				if(Utils.isGroupConversation(msisdn))
				{
					conv = getGroupConversation(msisdn, convid, contactid);
				}
				else
				{
					ContactInfo contactInfo = huDb.getContactInfoFromMSISDN(msisdn);
					conv = new Conversation(msisdn, convid, contactid, contactInfo.getName(),
							(contactInfo != null) ? contactInfo.isOnhike() : false);
				}

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
			c.close();
		}
		Collections.sort(conversations, Collections.reverseOrder());
		return conversations;
	}

	private Conversation getGroupConversation(String msisdn, long convid, String contactid)
	{
		Cursor groupCursor = null;
		try 
		{
			groupCursor = mDb.query(DBConstants.GROUP_INFO_TABLE, 
					new String[] 
							{ 
					DBConstants.GROUP_NAME, 
					DBConstants.GROUP_OWNER, 
					DBConstants.GROUP_ALIVE
							}, 
							DBConstants.GROUP_ID + " = ? ", 
							new String[] 
									{
					msisdn
									}, 
									null, null, null);
			if(!groupCursor.moveToFirst())
			{
				Log.w(getClass().getSimpleName(), "Could not find db entry: " + msisdn);
				return null;
			}

			String groupName = groupCursor.getString(groupCursor.getColumnIndex(DBConstants.GROUP_NAME));
			String groupOwner = groupCursor.getString(groupCursor.getColumnIndex(DBConstants.GROUP_OWNER));
			boolean isGroupAlive = groupCursor.getInt(groupCursor.getColumnIndex(DBConstants.GROUP_ALIVE)) != 0;

			ContactInfo contactInfo = new ContactInfo(msisdn, msisdn, groupName, msisdn);

			GroupConversation conv = new GroupConversation(msisdn, convid, contactid, contactInfo.getName(), groupOwner, isGroupAlive);
			conv.setGroupParticipantList(getGroupParticipants(msisdn, false));

			return conv;
		} 
		finally
		{
			if (groupCursor != null) {
				groupCursor.close();
			}
		}
	}

	public JSONArray updateStatusAndSendDeliveryReport(long convID)
	{

		Cursor c = mDb.query(DBConstants.MESSAGES_TABLE, 
				new String[] 
						{ 
				DBConstants.MESSAGE_ID,
				DBConstants.MAPPED_MSG_ID 
						}, 
						DBConstants.CONV_ID+"=? and "+DBConstants.MSG_STATUS+"=?",
						new String[] 
								{ 
				Long.toString(convID), 
				Integer.toString(ConvMessage.State.RECEIVED_UNREAD.ordinal()) 
								}, 
								null, null, null);
		/* If there are no rows in the cursor then simply return null */
		if (c.getCount() <= 0)
		{
			c.close();
			return null;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("(");

		final int msgIdIdx = c.getColumnIndex(DBConstants.MESSAGE_ID);
		final int mappedMsgIdIdx = c.getColumnIndex(DBConstants.MAPPED_MSG_ID);

		JSONArray ids = new JSONArray();
		while (c.moveToNext())
		{
			long msgId = c.getLong(msgIdIdx);
			long mappedMsgId = c.getLong(mappedMsgIdIdx);
			ids.put(String.valueOf(mappedMsgId));
			sb.append(msgId);
			if (!c.isLast())
			{
				sb.append(",");
			}
		}
		sb.append(")");
		ContentValues values = new ContentValues();
		values.put(DBConstants.MSG_STATUS, ConvMessage.State.RECEIVED_READ.ordinal());
		int rowsAffected = mDb.update(DBConstants.MESSAGES_TABLE, values, DBConstants.MESSAGE_ID+" in " + sb.toString(), null);
		Log.d("HIKE CONVERSATION DB ","Rows Updated : "+rowsAffected);
		c.close();
		return ids;
	}

	public void updateBatch(long[] ids, int status)
	{
		StringBuilder sb = new StringBuilder("(");
		/* TODO make utils.join work for arrays */
		for (int i = 0; i < ids.length; i++)
		{
			sb.append(ids[i]);
			if (i != ids.length - 1)
			{
				sb.append(",");
			}
		}
		sb.append(")");

		ContentValues values = new ContentValues();
		values.put(DBConstants.MSG_STATUS, status);
		mDb.update(DBConstants.MESSAGES_TABLE, values, DBConstants.MESSAGE_ID+" in " + sb.toString(), null);
	}

	/* deletes a single message */
	public void deleteMessage(long msgId)
	{
		Long[] bindArgs = new Long[] { msgId };
		mDb.execSQL("DELETE FROM " + DBConstants.MESSAGES_TABLE + " WHERE "+DBConstants.MESSAGE_ID+"= ?", bindArgs);
	}

	public boolean wasOverlayDismissed(String msisdn)
	{
		Cursor c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] {DBConstants.OVERLAY_DISMISSED}, DBConstants.MSISDN + "=?", new String[]{msisdn}, null, null, null);
		int s = 0;
		if(c.moveToFirst())
		{
			s = c.getInt(0);
		}
		c.close();
		return (s==0) ? false : true;
	}

	public void setOverlay(boolean dismiss, String msisdn)
	{
		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.OVERLAY_DISMISSED, dismiss);
		if (msisdn != null) {
			mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues,
					DBConstants.MSISDN + "=?", new String[] { msisdn });
		} else {
			mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues,
					null, null);
		}
	}

	/**
	 * Add a new participants to a group
	 * @param groupId The id of the group to which the participants are to be added
	 * @param participantList A list of the participants to be added
	 */
	public void addGroupParticipants(String groupId, Map<String, GroupParticipant> participantList)
	{
		SQLiteStatement insertStatement = null;
		InsertHelper ih = null;
		try
		{
			ih = new InsertHelper(mDb, DBConstants.GROUP_MEMBERS_TABLE);
			insertStatement = mDb.compileStatement("INSERT OR REPLACE INTO " + DBConstants.GROUP_MEMBERS_TABLE + " VALUES (?, ?, ?, ?)");
			mDb.beginTransaction();
			for(Entry<String, GroupParticipant> participant : participantList.entrySet())
			{
				insertStatement.bindString(ih.getColumnIndex(DBConstants.GROUP_ID), groupId);
				insertStatement.bindString(ih.getColumnIndex(DBConstants.MSISDN), participant.getKey());
				insertStatement.bindString(ih.getColumnIndex(DBConstants.NAME), participant.getValue().getContactInfo().getName());
				insertStatement.bindLong(ih.getColumnIndex(DBConstants.HAS_LEFT), 0);

				insertStatement.executeInsert();
			}
		}
		finally
		{
			if (insertStatement != null) 
			{
				insertStatement.close();
			}
			if (ih != null) 
			{
				ih.close();
			}
			mDb.setTransactionSuccessful();
			mDb.endTransaction();
		}
	}

	/**
	 * Should be called when a participant leaves the group
	 * @param groupId: The group ID of the group containing the participant
	 * @param msisdn: The msisdn of the participant
	 */
	public int setParticipantLeft(String groupId, String msisdn)
	{
		if(!doesConversationExist(groupId))
		{
			return 0;
		}
		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.HAS_LEFT, 1);
		return mDb.update(DBConstants.GROUP_MEMBERS_TABLE, contentValues, DBConstants.GROUP_ID + " = ? AND " + DBConstants.MSISDN + " = ? ", new String[] {groupId, msisdn});
	}

	/**
	 * Returns a list of participants to a group
	 * @param groupId
	 * @return
	 */
	public Map<String, GroupParticipant> getGroupParticipants(String groupId, boolean activeOnly)
	{
		String selection = DBConstants.GROUP_ID + " =? " + (activeOnly ? "AND " + DBConstants.HAS_LEFT + "=0" : "");
		Cursor c = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] {DBConstants.MSISDN, DBConstants.HAS_LEFT}, selection, new String[] {groupId}, null, null, null);

		Map<String, GroupParticipant> participantList = new HashMap<String, GroupParticipant>();

		HikeUserDatabase huDB = HikeUserDatabase.getInstance();
		while(c.moveToNext())
		{
			String msisdn = c.getString(c.getColumnIndex(DBConstants.MSISDN));
			ContactInfo contactInfo = huDB.getContactInfoFromMSISDN(msisdn);
			if(TextUtils.isEmpty(contactInfo.getName()))
			{
				contactInfo.setName(getParticipantName(groupId, contactInfo.getMsisdn()));
			}

			GroupParticipant groupParticipant = new GroupParticipant(contactInfo, c.getInt(c.getColumnIndex(DBConstants.HAS_LEFT)) != 0);
			participantList.put(msisdn, groupParticipant);
			Log.d(getClass().getSimpleName(), "Fetching participant: " + msisdn);
		}
		c.close();
		return participantList;
	}

	/**
	 * Reutrn the group name corresponding to a group ID. 
	 * @param groupId
	 * @return
	 */
	public String getGroupName(String groupId)
	{
		Cursor c = mDb.query(DBConstants.GROUP_INFO_TABLE, new String[] {DBConstants.GROUP_NAME}, DBConstants.GROUP_ID + " = ? ", new String[] {groupId}, null, null, null);
		String groupName = "";
		if(c.moveToFirst())
		{
			groupName = c.getString(c.getColumnIndex(DBConstants.GROUP_NAME));
		}
		c.close();
		return groupName;
	}

	public boolean doesConversationExist(String msisdn)
	{
		Cursor c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] {DBConstants.MSISDN}, DBConstants.MSISDN + " = ? ", new String[] {msisdn}, null, null, null);
		try
		{
			return c.moveToFirst();
		}
		finally
		{
			c.close();
		}
	}

	public int setGroupName(String groupId, String groupname)
	{
		if(!doesConversationExist(groupId))
		{
			return 0;
		}
		ContentValues values = new ContentValues(1);
		values.put(DBConstants.GROUP_NAME, groupname);
		return mDb.update(DBConstants.GROUP_INFO_TABLE, values, DBConstants.GROUP_ID + " = ?", new String[] { groupId });
	}

	public String getParticipantName(String groupId, String msisdn)
	{
		Cursor c = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] {DBConstants.NAME}, DBConstants.GROUP_ID + " = ? AND " + DBConstants.MSISDN + " = ? ", new String[] {groupId, msisdn}, null, null, null);
		String name = "";
		if(c.moveToFirst())
		{
			name = c.getString(c.getColumnIndex(DBConstants.NAME));
		}
		c.close();
		return name;
	}

	public int setGroupDead(String groupId)
	{
		if(!doesConversationExist(groupId))
		{
			return 0;
		}
		ContentValues values = new ContentValues(1);
		values.put(DBConstants.GROUP_ALIVE, 0);
		return mDb.update(DBConstants.GROUP_INFO_TABLE, values, DBConstants.GROUP_ID + " = ?", new String[]{groupId});
	}

	public long[] getUnreadMessageIds(long convId)
	{
		Cursor cursor = mDb.query(
				DBConstants.MESSAGES_TABLE, 
				new String[] {DBConstants.MESSAGE_ID}, 
				DBConstants.MSG_STATUS + " NOT IN " + 
				"(" + ConvMessage.State.SENT_DELIVERED_READ.ordinal() + ", " 
						+ ConvMessage.State.RECEIVED_READ.ordinal() + ", " 
				+ ConvMessage.State.RECEIVED_UNREAD.ordinal() +	", " 
						+ ConvMessage.State.SENT_UNCONFIRMED.ordinal() + ")" 
				+ " AND " 
						+ DBConstants.CONV_ID + " =?", 
				new String[] {convId + ""}, null, null, null);
		try
		{
			Log.d(getClass().getSimpleName(), "Number of unread messages for conversation " + convId + " = " + cursor.getCount());
			if(!cursor.moveToFirst())
			{
				return null;
			}
			long[] ids = new long[cursor.getCount()];
			int i = 0;
			int idIdx = cursor.getColumnIndex(DBConstants.MESSAGE_ID);
			do
			{
				ids[i++] = cursor.getLong(idIdx);
				Log.d(getClass().getSimpleName(), "Inserting id: " + ids[i - 1]);
			} while(cursor.moveToNext());
			return ids;
		}
		finally
		{
			cursor.close();
		}
	}
}
