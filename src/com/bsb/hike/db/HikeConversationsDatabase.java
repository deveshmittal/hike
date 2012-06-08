package com.bsb.hike.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.NetworkManager;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.utils.Utils;

public class HikeConversationsDatabase extends SQLiteOpenHelper
{

	private SQLiteDatabase mDb;

	private Context mCtx;

	public HikeConversationsDatabase(Context context)
	{
		super(context, DBConstants.CONVERSATIONS_DATABASE_NAME, null, DBConstants.CONVERSATIONS_DATABASE_VERSION);
		mDb = getWritableDatabase();
		mCtx = context;
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
																														+ DBConstants.OVERLAY_DISMISSED+" INTEGER, "
																														+ DBConstants.GROUP_NAME + " TEXT"
																												+ " )";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.GROUP_TABLE
																												+ " ( "
																														+ DBConstants.GROUP_ID +" STRING, "
																														+ DBConstants.MSISDN + " TEXT"
																												+ " )";
		db.execSQL(sql);
		sql = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.GROUP_INDEX + " ON " + DBConstants.GROUP_TABLE 
																										+ " ( "
																												+ DBConstants.GROUP_ID + ", "
																												+ DBConstants.MSISDN
																												+ " ) ";
		db.execSQL(sql);
	}

	public void deleteAll()
	{
		mDb.delete(DBConstants.CONVERSATIONS_TABLE, null, null);
		mDb.delete(DBConstants.MESSAGES_TABLE, null, null);
		mDb.delete(DBConstants.GROUP_TABLE, null, null);
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
		if (db == null)
		{
			db = mDb;
		}

		db.execSQL("DROP TABLE IF EXISTS " + DBConstants.CONVERSATIONS_TABLE);
		db.execSQL("DROP TABLE IF EXISTS " + DBConstants.MESSAGES_TABLE);
		db.execSQL("DROP TABLE IF EXISTS " + DBConstants.GROUP_TABLE);
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
		int rowsAffected = mDb.update(DBConstants.MESSAGES_TABLE, values, DBConstants.MESSAGE_ID+"=? AND " + DBConstants.MSG_STATUS + " < ?", whereArgs);
		Log.d("HIKE CONVERSATION DB", "Update Msg status to : " + ConvMessage.stateValue(val) + "	;	for msgID : " + msgID + "	;	Rows Affected : " + rowsAffected);
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
		Log.d("HikeConversationsDatabase", "CHECKING MESSAGE ID: "+conv.getMappedMsgID()+" MESSAGE TIMESTAMP: "+conv.getTimestamp());
		Cursor c = mDb.query(
				DBConstants.MESSAGES_TABLE+","+DBConstants.CONVERSATIONS_TABLE,
				new String[] { DBConstants.MESSAGE },
				DBConstants.MAPPED_MSG_ID + "=? AND "
						+ DBConstants.TIMESTAMP + "=? AND "
						+ DBConstants.CONVERSATIONS_TABLE 
						+ "."
						+ DBConstants.MSISDN +"=?",
						new String[] { Long.toString(conv.getMappedMsgID()),
						Long.toString(conv.getTimestamp()), conv.getMsisdn() }, null,
						null, null);
		int count = c.getCount();
		c.close();
		return (count!=0);
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
			/* Represents we dont have any conversation made for this msisdn.*/
			if (msgId <= 0)
			{
				Conversation conversation = addConversation(conv.getMsisdn(), !conv.isSMS(), null);
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

	public void deleteConversation(Long[] ids)
	{
		mDb.beginTransaction();
		for (int i = 0; i < ids.length; i++)
		{
			Long[] bindArgs = new Long[] { ids[i] };
			mDb.execSQL("DELETE FROM " + DBConstants.CONVERSATIONS_TABLE + " WHERE "+ DBConstants.CONV_ID +"= ?", bindArgs);
			mDb.execSQL("DELETE FROM " + DBConstants.MESSAGES_TABLE + " WHERE "+ DBConstants.CONV_ID +"= ?", bindArgs);
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
	public Conversation addConversation(String msisdn, boolean onhike, String groupName)
	{
		HikeUserDatabase huDb = new HikeUserDatabase(mCtx);
		ContactInfo contactInfo = Utils.isGroupConversation(msisdn) ?  new ContactInfo(msisdn, msisdn, groupName, msisdn) : huDb.getContactInfoFromMSISDN(msisdn);
		huDb.close();
		InsertHelper ih = new InsertHelper(mDb, DBConstants.CONVERSATIONS_TABLE);
		ih.prepareForInsert();
		ih.bind(ih.getColumnIndex(DBConstants.MSISDN), msisdn);
		if (contactInfo != null)
		{
			ih.bind(ih.getColumnIndex(DBConstants.CONTACT_ID), contactInfo.getId());
			onhike |= contactInfo.isOnhike();
		}

		ih.bind(ih.getColumnIndex(DBConstants.ONHIKE), onhike);
		
		if(groupName != null)
		{
			ih.bind(ih.getColumnIndex(DBConstants.GROUP_NAME), groupName);
		}
		long id = ih.execute();

		if (id >= 0)
		{
			Conversation conv = new Conversation(msisdn, id, (contactInfo != null) ? contactInfo.getId() : null, (contactInfo != null) ? contactInfo.getName() : null, onhike);
			if (conv.isGroupConversation()) 
			{
				Log.d(getClass().getSimpleName(), "Fetching participants...");
				conv.setGroupParticipants(getGroupParticipants(msisdn));
				Log.d(getClass().getSimpleName(), "Participants size: " + conv.getGroupParticipants().size());
			}
			HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_CONVERSATION, conv);
			return conv;
		}
		/* TODO does this happen? If so, what should we do? */
		Log.wtf("Conversationadding", "Couldn't add conversation --- race condition?");
		return null;
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
		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, 
										new String[] 
												{ 
													DBConstants.CONV_ID, 
													DBConstants.CONTACT_ID,
													DBConstants.ONHIKE,
													DBConstants.GROUP_NAME
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

			huDb = new HikeUserDatabase(mCtx);
			ContactInfo contactInfo = Utils.isGroupConversation(msisdn) ? new ContactInfo(msisdn, msisdn, c.getString(c.getColumnIndex(DBConstants.GROUP_NAME)), msisdn) : huDb.getContactInfoFromMSISDN(msisdn);

			onhike |= contactInfo.isOnhike();
			Conversation conv = new Conversation(msisdn, convid, contactid, contactInfo.getName(), onhike);
			if (limit > 0) 
			{
				List<ConvMessage> messages = getConversationThread(msisdn, contactid, convid, limit, conv);
				conv.setMessages(messages);
			}
			if (Utils.isGroupConversation(msisdn)) 
			{
				conv.setGroupParticipants(getGroupParticipants(msisdn));
			}
			return conv;
		}
		finally
		{
			if (c != null)
			{
				c.close();				
			}

			if (huDb != null)
			{
				huDb.close();				
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
													DBConstants.MSISDN,
													DBConstants.GROUP_NAME
												}, 
										null, null, null, null, null);
		
		List<Conversation> conversations = new ArrayList<Conversation>();
		final int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
		final int convIdx = c.getColumnIndex(DBConstants.CONV_ID);
		final int contactIdx = c.getColumnIndex(DBConstants.CONTACT_ID);
		final int groupNameIdx = c.getColumnIndex(DBConstants.GROUP_NAME);
		
		HikeUserDatabase huDb = null;
		try
		{
			huDb = new HikeUserDatabase(mCtx);
			while (c.moveToNext())
			{
				// TODO this can be expressed in a single sql query
				String msisdn = c.getString(msisdnIdx);
				ContactInfo contactInfo = null;
				contactInfo = Utils.isGroupConversation(msisdn) ?
								huDb.getContactInfoFromMSISDN(msisdn) :
								new ContactInfo(msisdn, msisdn, c.getString(groupNameIdx), msisdn);

				Conversation conv = new Conversation(msisdn, c.getLong(convIdx), c.getString(contactIdx), contactInfo.getName(),
						(contactInfo != null) ? contactInfo.isOnhike() : false);
				if(conv.isGroupConversation())
				{
					conv.setGroupParticipants(getGroupParticipants(msisdn));
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
			huDb.close();
			c.close();
		}
		Collections.sort(conversations, Collections.reverseOrder());
		return conversations;
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
	public void addGroupParticipants(String groupId, List<ContactInfo> participantList)
	{
		InsertHelper ih = new InsertHelper(mDb, DBConstants.GROUP_TABLE);
		for(ContactInfo participant : participantList)
		{
			// TODO change this logic for greater numbers.
			try 
			{
				Log.d(getClass().getSimpleName(), "Adding participant: " + participant.getName());
				ih.prepareForInsert();
				ih.bind(ih.getColumnIndex(DBConstants.GROUP_ID), groupId);
				ih.bind(ih.getColumnIndex(DBConstants.MSISDN), participant.getMsisdn());
				ih.execute();
			} 
			catch (SQLiteConstraintException e) 
			{
				Log.w(getClass().getSimpleName(), "Trying to insert same row again", e);
			}
		}
		ih.close();
	}

	/**
	 * Should be called when a participant leaves the group
	 * @param groupId: The group ID of the group containing the participant
	 * @param msisdn: The msisdn of the participant
	 */
	public void removeParticipant(String groupId, String msisdn)
	{
		mDb.delete(DBConstants.GROUP_TABLE, DBConstants.GROUP_ID + " = ? AND " + DBConstants.MSISDN + " = ?", new String[] {groupId, msisdn});
	}

	/**
	 * Returns a list of participants to a group
	 * @param groupId
	 * @return
	 */
	public List<ContactInfo> getGroupParticipants(String groupId)
	{
		Cursor c = mDb.query(DBConstants.GROUP_TABLE, new String[] {DBConstants.MSISDN}, DBConstants.GROUP_ID + " = ? ", new String[] {groupId}, null, null, null);

		List<ContactInfo> participantList = new ArrayList<ContactInfo>();

		HikeUserDatabase huDB = new HikeUserDatabase(mCtx);
		while(c.moveToNext())
		{
			String msisdn = c.getString(c.getColumnIndex(DBConstants.MSISDN));
			ContactInfo contactInfo = huDB.getContactInfoFromMSISDN(msisdn);
			Log.d(getClass().getSimpleName(), "Contact info is null: " + msisdn + " " + contactInfo); 
			participantList.add(contactInfo);
			Log.d(getClass().getSimpleName(), "Fetching participant: " + c.getString(c.getColumnIndex(DBConstants.MSISDN)));
		}
		huDB.close();
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
		Cursor c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] {DBConstants.GROUP_NAME}, DBConstants.MSISDN + " = ? ", new String[] {groupId}, null, null, null);
		String groupName = "";
		if(c.moveToFirst())
		{
			groupName = c.getString(c.getColumnIndex(DBConstants.GROUP_NAME));
		}
		c.close();
		return groupName;
	}

	public boolean doesConversationExist(Conversation conversation)
	{
		Cursor c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] {DBConstants.MSISDN}, DBConstants.MSISDN + " = ? ", new String[] {conversation.getMsisdn()}, null, null, null);
		try
		{
			return c.moveToFirst();
		}
		finally
		{
			c.close();
		}
	}
}
