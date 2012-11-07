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

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.adapters.EmoticonAdapter.EmoticonType;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.Utils;

public class HikeConversationsDatabase extends SQLiteOpenHelper {

	private SQLiteDatabase mDb;

	private static HikeConversationsDatabase hikeConversationsDatabase;

	public static void init(Context context) {
		if (hikeConversationsDatabase == null) {
			hikeConversationsDatabase = new HikeConversationsDatabase(context);
		}
	}

	public static HikeConversationsDatabase getInstance() {
		return hikeConversationsDatabase;
	}

	private HikeConversationsDatabase(Context context) {
		super(context, DBConstants.CONVERSATIONS_DATABASE_NAME, null,
				DBConstants.CONVERSATIONS_DATABASE_VERSION);
		mDb = getWritableDatabase();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		if (db == null) {
			db = mDb;
		}
		String sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.MESSAGES_TABLE
				+ " ( " + DBConstants.MESSAGE + " STRING, "
				+ DBConstants.MSG_STATUS + " INTEGER, " /*
														 * this is to check if
														 * msg sent or recieved
														 * of the msg sent.
														 */
				+ DBConstants.TIMESTAMP + " INTEGER, " + DBConstants.MESSAGE_ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ DBConstants.MAPPED_MSG_ID + " INTEGER, "
				+ DBConstants.CONV_ID + " INTEGER,"
				+ DBConstants.MESSAGE_METADATA + " TEXT, "
				+ DBConstants.GROUP_PARTICIPANT + " TEXT" + " ) ";

		db.execSQL(sql);
		sql = "CREATE INDEX IF NOT EXISTS " + DBConstants.CONVERSATION_INDEX
				+ " ON " + DBConstants.MESSAGES_TABLE + " ( "
				+ DBConstants.CONV_ID + " , " + DBConstants.TIMESTAMP + " DESC"
				+ " )";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.CONVERSATIONS_TABLE
				+ " ( " + DBConstants.CONV_ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + DBConstants.ONHIKE
				+ " INTEGER, " + DBConstants.CONTACT_ID + " STRING, "
				+ DBConstants.MSISDN + " UNIQUE, "
				+ DBConstants.OVERLAY_DISMISSED + " INTEGER" + " )";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.GROUP_MEMBERS_TABLE
				+ " ( " + DBConstants.GROUP_ID + " STRING, "
				+ DBConstants.MSISDN + " TEXT, " + DBConstants.NAME + " TEXT, "
				+ DBConstants.ONHIKE + " INTEGER, " + DBConstants.HAS_LEFT
				+ " INTEGER, " + DBConstants.ON_DND + " INTEGER, "
				+ DBConstants.SHOWN_STATUS + " INTEGER " + " )";
		db.execSQL(sql);
		sql = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.GROUP_INDEX
				+ " ON " + DBConstants.GROUP_MEMBERS_TABLE + " ( "
				+ DBConstants.GROUP_ID + ", " + DBConstants.MSISDN + " ) ";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.GROUP_INFO_TABLE
				+ " ( " + DBConstants.GROUP_ID + " STRING PRIMARY KEY, "
				+ DBConstants.GROUP_NAME + " TEXT, " + DBConstants.GROUP_OWNER
				+ " TEXT, " + DBConstants.GROUP_ALIVE + " INTEGER, "
				+ DBConstants.MUTE_GROUP + " INTEGER DEFAULT 0 " + " )";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.FILE_TABLE + " ( "
				+ DBConstants.FILE_KEY + " STRING PRIMARY KEY, "
				+ DBConstants.FILE_NAME + " STRING UNIQUE" + " )";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.EMOTICON_TABLE
				+ " ( " + DBConstants.EMOTICON_NUM + " INTEGER PRIMARY KEY, "
				+ DBConstants.LAST_USED + " INTEGER" + " )";
		db.execSQL(sql);
		sql = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.EMOTICON_INDEX
				+ " ON " + DBConstants.EMOTICON_TABLE + " ( "
				+ DBConstants.EMOTICON_NUM + " ) ";
		db.execSQL(sql);
	}

	public void deleteAll() {
		mDb.delete(DBConstants.CONVERSATIONS_TABLE, null, null);
		mDb.delete(DBConstants.MESSAGES_TABLE, null, null);
		mDb.delete(DBConstants.GROUP_MEMBERS_TABLE, null, null);
		mDb.delete(DBConstants.GROUP_INFO_TABLE, null, null);
	}

	@Override
	public void close() {
		super.close();
		mDb.close();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (db == null) {
			db = mDb;
		}
		onCreate(db);

		if (oldVersion < 2) {
			String alter = "ALTER TABLE " + DBConstants.GROUP_MEMBERS_TABLE
					+ " ADD COLUMN " + DBConstants.ONHIKE + " INTEGER";
			db.execSQL(alter);
		}
		if (oldVersion < 3) {
			String alter = "ALTER TABLE " + DBConstants.GROUP_MEMBERS_TABLE
					+ " ADD COLUMN " + DBConstants.ON_DND + " INTEGER";
			db.execSQL(alter);
			alter = "ALTER TABLE " + DBConstants.GROUP_MEMBERS_TABLE
					+ " ADD COLUMN " + DBConstants.SHOWN_STATUS + " INTEGER";
			db.execSQL(alter);
		}
		// This is being done to change the column type of column "name" in the
		// group members table
		if (oldVersion < 4) {
			String alter = "ALTER TABLE " + DBConstants.GROUP_MEMBERS_TABLE
					+ " RENAME TO " + "temp_table";

			String dropIndex = "DROP INDEX " + DBConstants.GROUP_INDEX;

			String create = "CREATE TABLE IF NOT EXISTS "
					+ DBConstants.GROUP_MEMBERS_TABLE + " ( "
					+ DBConstants.GROUP_ID + " STRING, " + DBConstants.MSISDN
					+ " TEXT, " + DBConstants.NAME + " TEXT, "
					+ DBConstants.ONHIKE + " INTEGER, " + DBConstants.HAS_LEFT
					+ " INTEGER, " + DBConstants.ON_DND + " INTEGER, "
					+ DBConstants.SHOWN_STATUS + " INTEGER " + " )";

			String createIndex = "CREATE UNIQUE INDEX IF NOT EXISTS "
					+ DBConstants.GROUP_INDEX + " ON "
					+ DBConstants.GROUP_MEMBERS_TABLE + " ( "
					+ DBConstants.GROUP_ID + ", " + DBConstants.MSISDN + " ) ";

			String insert = "INSERT INTO " + DBConstants.GROUP_MEMBERS_TABLE
					+ " SELECT * FROM temp_table";

			String drop = "DROP TABLE temp_table";

			db.execSQL(alter);
			db.execSQL(dropIndex);
			db.execSQL(create);
			db.execSQL(createIndex);
			db.execSQL(insert);
			db.execSQL(drop);
		}
		// Add muteGroup column
		if (oldVersion < 6) {
			String alter = "ALTER TABLE " + DBConstants.GROUP_INFO_TABLE
					+ " ADD COLUMN " + DBConstants.MUTE_GROUP
					+ " INTEGER DEFAULT 0";
			db.execSQL(alter);
		}
	}

	public void updateOnHikeStatus(String msisdn, boolean onHike) {
		ContentValues values = new ContentValues();
		values.put(DBConstants.ONHIKE, onHike);
		String[] whereArgs = { msisdn };
		mDb.update(DBConstants.CONVERSATIONS_TABLE, values, DBConstants.MSISDN
				+ "=?", whereArgs);
		mDb.update(DBConstants.GROUP_MEMBERS_TABLE, values, DBConstants.MSISDN
				+ "=?", whereArgs);
	}

	public void addConversationMessages(ConvMessage message) {
		List<ConvMessage> l = new ArrayList<ConvMessage>(1);
		l.add(message);
		addConversations(l);
	}

	public void updateMsgStatus(long msgID, int val, String msisdn) {
		String initialWhereClause = DBConstants.MESSAGE_ID + " ="
				+ String.valueOf(msgID);

		String query = "UPDATE " + DBConstants.MESSAGES_TABLE + " SET "
				+ DBConstants.MSG_STATUS + " =" + val + " WHERE "
				+ initialWhereClause;

		executeUpdateMessageStatusStatement(query, val, msisdn);
	}

	public void updateBatch(long[] ids, int status, String msisdn) {
		StringBuilder sb = new StringBuilder("(");
		/* TODO make utils.join work for arrays */
		for (int i = 0; i < ids.length; i++) {
			sb.append(ids[i]);
			if (i != ids.length - 1) {
				sb.append(",");
			}
		}
		sb.append(")");

		String initialWhereClause = DBConstants.MESSAGE_ID + " in "
				+ sb.toString();

		String query = "UPDATE " + DBConstants.MESSAGES_TABLE + " SET "
				+ DBConstants.MSG_STATUS + " =" + status + " WHERE "
				+ initialWhereClause;

		executeUpdateMessageStatusStatement(query, status, msisdn);
	}

	public void executeUpdateMessageStatusStatement(String updateStatement,
			int status, String msisdn) {
		int minStatusOrdinal;
		int maxStatusOrdinal;
		if (status <= State.SENT_DELIVERED_READ.ordinal()) {
			minStatusOrdinal = State.SENT_UNCONFIRMED.ordinal();
			maxStatusOrdinal = status;
		} else {
			minStatusOrdinal = State.RECEIVED_UNREAD.ordinal();
			maxStatusOrdinal = status;
		}

		updateStatement = updateStatement
				+ " AND "
				+ DBConstants.MSG_STATUS
				+ " >= "
				+ minStatusOrdinal
				+ " AND "
				+ DBConstants.MSG_STATUS
				+ " <= "
				+ maxStatusOrdinal
				+ (!TextUtils.isEmpty(msisdn) ? (" AND " + DBConstants.CONV_ID
						+ "=(SELECT " + DBConstants.CONV_ID + " FROM "
						+ DBConstants.CONVERSATIONS_TABLE + " WHERE "
						+ DBConstants.MSISDN + " ='" + msisdn + "')") : "");
		Log.d(getClass().getSimpleName(), "UPDATE STATEMENT: "
				+ updateStatement);
		mDb.execSQL(updateStatement);
	}

	public void updateMessageMetadata(long msgID, MessageMetadata metadata) {
		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.MESSAGE_METADATA, metadata.serialize());
		mDb.update(DBConstants.MESSAGES_TABLE, contentValues,
				DBConstants.MESSAGE_ID + "=?",
				new String[] { String.valueOf(msgID) });
	}

	private void bindConversationInsert(SQLiteStatement insertStatement,
			ConvMessage conv) {
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
		insertStatement.bindString(messageMetadataColumn,
				conv.getMetadata() != null ? conv.getMetadata().serialize()
						: "");
		insertStatement.bindString(
				groupParticipant,
				conv.getGroupParticipantMsisdn() != null ? conv
						.getGroupParticipantMsisdn() : "");
	}

	public boolean wasMessageReceived(ConvMessage conv) {
		Log.d("HikeConversationsDatabase",
				"CHECKING MESSAGE ID: " + conv.getMappedMsgID()
						+ " MESSAGE TIMESTAMP: " + conv.getTimestamp());
		Cursor c = mDb
				.query(DBConstants.MESSAGES_TABLE + ","
						+ DBConstants.CONVERSATIONS_TABLE,
						new String[] { DBConstants.MESSAGE },
						DBConstants.MAPPED_MSG_ID + "=? AND "
								+ DBConstants.MESSAGE + "=? AND "
								+ DBConstants.CONVERSATIONS_TABLE + "."
								+ DBConstants.MSISDN + "=?",
						new String[] { Long.toString(conv.getMappedMsgID()),
								conv.getMessage(), conv.getMsisdn() }, null,
						null, null);
		int count = c.getCount();
		c.close();
		return (count != 0);
	}

	public void addConversations(List<ConvMessage> convMessages) {
		SQLiteStatement insertStatement = mDb.compileStatement("INSERT INTO "
				+ DBConstants.MESSAGES_TABLE + " ( " + DBConstants.MESSAGE
				+ "," + DBConstants.MSG_STATUS + "," + DBConstants.TIMESTAMP
				+ "," + DBConstants.MAPPED_MSG_ID + " ,"
				+ DBConstants.MESSAGE_METADATA + ","
				+ DBConstants.GROUP_PARTICIPANT + "," + DBConstants.CONV_ID
				+ " ) " + " SELECT ?, ?, ?, ?, ?, ?," + DBConstants.CONV_ID
				+ " FROM " + DBConstants.CONVERSATIONS_TABLE + " WHERE "
				+ DBConstants.CONVERSATIONS_TABLE + "." + DBConstants.MSISDN
				+ "=?");
		mDb.beginTransaction();

		long msgId = -1;

		for (ConvMessage conv : convMessages) {
			bindConversationInsert(insertStatement, conv);
			msgId = insertStatement.executeInsert();
			/*
			 * Represents we dont have any conversation made for this msisdn.
			 * Here we are also checking whether the message is a group message,
			 * If it is and the conversation does not exist we do not add a
			 * conversation.
			 */
			if (msgId <= 0 && !Utils.isGroupConversation(conv.getMsisdn())) {
				Conversation conversation = addConversation(conv.getMsisdn(),
						!conv.isSMS(), null, null);
				if (conversation != null) {
					conversation.addMessage(conv);
				}
				bindConversationInsert(insertStatement, conv);
				msgId = insertStatement.executeInsert();
				conv.setConversation(conversation);
				assert (msgId >= 0);
			} else if (conv.getConversation() == null) {
				// conversation not set, retrieve it from db
				Conversation conversation = this.getConversation(
						conv.getMsisdn(), 0);
				conv.setConversation(conversation);
			}
			conv.setMsgID(msgId);
		}

		insertStatement.close();
		mDb.setTransactionSuccessful();
		mDb.endTransaction();
	}

	public void deleteConversation(Long[] ids, List<String> msisdns) {
		mDb.beginTransaction();
		for (int i = 0; i < ids.length; i++) {
			Long[] bindArgs = new Long[] { ids[i] };
			String msisdn = msisdns.get(i);
			mDb.execSQL("DELETE FROM " + DBConstants.CONVERSATIONS_TABLE
					+ " WHERE " + DBConstants.CONV_ID + "= ?", bindArgs);
			mDb.execSQL("DELETE FROM " + DBConstants.MESSAGES_TABLE + " WHERE "
					+ DBConstants.CONV_ID + "= ?", bindArgs);
			if (Utils.isGroupConversation(msisdn)) {
				mDb.delete(DBConstants.GROUP_MEMBERS_TABLE,
						DBConstants.GROUP_ID + " =?", new String[] { msisdn });
				mDb.delete(DBConstants.GROUP_INFO_TABLE, DBConstants.GROUP_ID
						+ " =?", new String[] { msisdn });
			}
		}
		mDb.setTransactionSuccessful();
		mDb.endTransaction();
	}

	/**
	 * Add a conversation to the db
	 * 
	 * @param msisdn
	 *            the msisdn of the contact
	 * @param onhike
	 *            true iff the contact is onhike. If this is false, we consult
	 *            the local db as well
	 * @param groupName
	 *            the name of the group. Sent as <code>null</code> if the
	 *            conversation is not a group conversation
	 * @return Conversation object representing the conversation
	 */
	public Conversation addConversation(String msisdn, boolean onhike,
			String groupName, String groupOwner) {
		HikeUserDatabase huDb = HikeUserDatabase.getInstance();
		ContactInfo contactInfo = Utils.isGroupConversation(msisdn) ? new ContactInfo(
				msisdn, msisdn, groupName, msisdn) : huDb
				.getContactInfoFromMSISDN(msisdn, false);
		InsertHelper ih = null;
		try {
			ih = new InsertHelper(mDb, DBConstants.CONVERSATIONS_TABLE);
			ih.prepareForInsert();
			ih.bind(ih.getColumnIndex(DBConstants.MSISDN), msisdn);
			if (contactInfo != null) {
				ih.bind(ih.getColumnIndex(DBConstants.CONTACT_ID),
						contactInfo.getId());
				onhike |= contactInfo.isOnhike();
			}

			ih.bind(ih.getColumnIndex(DBConstants.ONHIKE), onhike);

			long id = ih.execute();

			if (id >= 0) {
				Conversation conv;
				if (Utils.isGroupConversation(msisdn)) {
					conv = new GroupConversation(msisdn, id,
							(contactInfo != null) ? contactInfo.getName()
									: null, groupOwner, true);
					Log.d(getClass().getSimpleName(),
							"Adding a new group conversation: " + msisdn);
					InsertHelper groupInfoIH = null;
					try {
						groupInfoIH = new InsertHelper(mDb,
								DBConstants.GROUP_INFO_TABLE);
						groupInfoIH.prepareForInsert();
						groupInfoIH.bind(groupInfoIH
								.getColumnIndex(DBConstants.GROUP_ID), msisdn);
						groupInfoIH.bind(groupInfoIH
								.getColumnIndex(DBConstants.GROUP_NAME),
								groupName);
						groupInfoIH.bind(groupInfoIH
								.getColumnIndex(DBConstants.GROUP_OWNER),
								groupOwner);
						groupInfoIH.bind(groupInfoIH
								.getColumnIndex(DBConstants.GROUP_ALIVE), 1);
						groupInfoIH.execute();
					} finally {
						if (groupInfoIH != null) {
							groupInfoIH.close();
						}
					}

					Log.d(getClass().getSimpleName(),
							"Fetching participants...");
					((GroupConversation) conv)
							.setGroupParticipantList(getGroupParticipants(
									msisdn, false, false));
					Log.d(getClass().getSimpleName(), "Participants size: "
							+ ((GroupConversation) conv)
									.getGroupParticipantList().size());
				} else {
					conv = new Conversation(msisdn, id,
							(contactInfo != null) ? contactInfo.getName()
									: null, onhike);
				}
				HikeMessengerApp.getPubSub().publish(
						HikePubSub.NEW_CONVERSATION, conv);
				return conv;
			}
			/* TODO does this happen? If so, what should we do? */
			Log.wtf("Conversationadding",
					"Couldn't add conversation --- race condition?");
			return null;
		} finally {
			if (ih != null) {
				ih.close();
			}
		}
	}

	public List<ConvMessage> getConversationThread(String msisdn, long convid,
			int limit, Conversation conversation, long maxMsgId) {
		String limitStr = new Integer(limit).toString();
		String selection = DBConstants.CONV_ID
				+ " = ?"
				+ (maxMsgId == -1 ? "" : " AND " + DBConstants.MESSAGE_ID + "<"
						+ maxMsgId);
		/* TODO this should be ORDER BY timestamp */
		Cursor c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] {
				DBConstants.MESSAGE, DBConstants.MSG_STATUS,
				DBConstants.TIMESTAMP, DBConstants.MESSAGE_ID,
				DBConstants.MAPPED_MSG_ID, DBConstants.MESSAGE_METADATA,
				DBConstants.GROUP_PARTICIPANT }, selection,
				new String[] { Long.toString(convid) }, null, null,
				DBConstants.MESSAGE_ID + " DESC", limitStr);

		final int msgColumn = c.getColumnIndex(DBConstants.MESSAGE);
		final int msgStatusColumn = c.getColumnIndex(DBConstants.MSG_STATUS);
		final int tsColumn = c.getColumnIndex(DBConstants.TIMESTAMP);
		final int mappedMsgIdColumn = c
				.getColumnIndex(DBConstants.MAPPED_MSG_ID);
		final int msgIdColumn = c.getColumnIndex(DBConstants.MESSAGE_ID);
		final int metadataColumn = c
				.getColumnIndex(DBConstants.MESSAGE_METADATA);
		final int groupParticipantColumn = c
				.getColumnIndex(DBConstants.GROUP_PARTICIPANT);
		List<ConvMessage> elements = new ArrayList<ConvMessage>(c.getCount());
		while (c.moveToNext()) {
			ConvMessage message = new ConvMessage(c.getString(msgColumn),
					msisdn, c.getInt(tsColumn), ConvMessage.stateValue(c
							.getInt(msgStatusColumn)), c.getLong(msgIdColumn),
					c.getLong(mappedMsgIdColumn),
					c.getString(groupParticipantColumn));
			String metadata = c.getString(metadataColumn);
			try {
				message.setMetadata(metadata);
			} catch (JSONException e) {
				Log.e(HikeConversationsDatabase.class.getName(),
						"Invalid JSON metadata", e);
			}
			elements.add(elements.size(), message);
			message.setConversation(conversation);
		}
		Collections.reverse(elements);
		c.close();

		return elements;
	}

	public Conversation getConversation(String msisdn, int limit) {
		Log.d(getClass().getSimpleName(), "Fetching conversation with msisdn: "
				+ msisdn);
		Cursor c = null;
		HikeUserDatabase huDb = null;
		Conversation conv = null;
		try {
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] {
					DBConstants.CONV_ID, DBConstants.CONTACT_ID,
					DBConstants.ONHIKE }, DBConstants.MSISDN + "=?",
					new String[] { msisdn }, null, null, null);
			if (!c.moveToFirst()) {
				Log.d(getClass().getSimpleName(), "Could not find db entry");
				return null;
			}

			long convid = c.getInt(c.getColumnIndex(DBConstants.CONV_ID));
			boolean onhike = c.getInt(c.getColumnIndex(DBConstants.ONHIKE)) != 0;

			if (Utils.isGroupConversation(msisdn)) {
				conv = getGroupConversation(msisdn, convid);
			} else {
				huDb = HikeUserDatabase.getInstance();
				ContactInfo contactInfo = huDb.getContactInfoFromMSISDN(msisdn,
						false);

				onhike |= contactInfo.isOnhike();
				conv = new Conversation(msisdn, convid, contactInfo.getName(),
						onhike);

			}
			if (limit > 0) {
				List<ConvMessage> messages = getConversationThread(msisdn,
						convid, limit, conv, -1);
				conv.setMessages(messages);
			}
			return conv;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public List<Conversation> getConversations() {
		long startTime = System.currentTimeMillis();
		// select max(msgid), messages.convid, message, (select msisdn from
		// conversations where conversations.convid = messages.convid) from
		// messages, conversations group by messages.convid
		String sqlStatement = "SELECT msgid, messages.convid, message, msgStatus, max(timestamp) as timestamp, mappedMsgId, metadata, groupParticipant, msisdn from (SELECT * FROM messages ORDER BY msgid ASC) as messages, conversations where conversations.convid = messages.convid group by conversations.convid";
		Cursor mainCursor = mDb.rawQuery(sqlStatement, null);
		Cursor groupInfoCursor = null;

		List<Conversation> conversations = new ArrayList<Conversation>();

		final int msisdnIdx = mainCursor.getColumnIndex(DBConstants.MSISDN);
		final int convIdx = mainCursor.getColumnIndex(DBConstants.CONV_ID);
		final int messageIdx = mainCursor.getColumnIndex(DBConstants.MESSAGE);
		final int msgStatusIdx = mainCursor
				.getColumnIndex(DBConstants.MSG_STATUS);
		final int tsIdx = mainCursor.getColumnIndex(DBConstants.TIMESTAMP);
		final int mappedMsgIdIdx = mainCursor
				.getColumnIndex(DBConstants.MAPPED_MSG_ID);
		final int msgIdIdx = mainCursor.getColumnIndex(DBConstants.MESSAGE_ID);
		final int metadataIdx = mainCursor
				.getColumnIndex(DBConstants.MESSAGE_METADATA);
		final int groupParticipantIdx = mainCursor
				.getColumnIndex(DBConstants.GROUP_PARTICIPANT);

		Map<String, Conversation> msisdnConversationMap = new HashMap<String, Conversation>();

		StringBuilder oneToOneSelections = new StringBuilder("(");
		try {
			for (String string : mainCursor.getColumnNames()) {
				Log.d(getClass().getSimpleName(), string);
			}
			while (mainCursor.moveToNext()) {
				String msisdn = mainCursor.getString(msisdnIdx);
				long convId = mainCursor.getInt(convIdx);
				Log.d(getClass().getSimpleName(), "Fetching Conversations: "
						+ msisdn);
				Log.d(getClass().getSimpleName(),
						"Message: " + mainCursor.getString(messageIdx));
				if (!Utils.isGroupConversation(msisdn)) {
					oneToOneSelections.append("'" + msisdn + "',");
				}
				/*
				 * Making conversation with just the msisdn and convid. We will
				 * add additional details later.
				 */
				Conversation conversation = new Conversation(msisdn, convId);

				ConvMessage convMessage = new ConvMessage(
						mainCursor.getString(messageIdx),
						msisdn,
						mainCursor.getLong(tsIdx),
						ConvMessage.stateValue(mainCursor.getInt(msgStatusIdx)),
						mainCursor.getLong(msgIdIdx), mainCursor
								.getLong(mappedMsgIdIdx), mainCursor
								.getString(groupParticipantIdx));
				convMessage.setMetadata(mainCursor.getString(metadataIdx));

				conversation.addMessage(convMessage);
				msisdnConversationMap.put(msisdn, conversation);
			}
			oneToOneSelections.replace(oneToOneSelections.length() - 1,
					oneToOneSelections.length(), ")");

			/*
			 * Getting the name for one to one conversations
			 */
			List<ContactInfo> contactList = HikeUserDatabase.getInstance()
					.getContactNamesFromMsisdnList(oneToOneSelections);
			for (ContactInfo contact : contactList) {
				Conversation conversation = msisdnConversationMap.get(contact
						.getMsisdn());
				conversation.setContactName(contact.getName());
				conversation.setOnhike(contact.isOnhike());
			}

			/*
			 * Getting the info for group conversations
			 */
			groupInfoCursor = mDb.query(DBConstants.GROUP_INFO_TABLE,
					new String[] { DBConstants.GROUP_ID,
							DBConstants.GROUP_NAME, DBConstants.GROUP_OWNER,
							DBConstants.GROUP_ALIVE }, null, null, null, null,
					null);

			final int groupIdIdx = groupInfoCursor
					.getColumnIndex(DBConstants.GROUP_ID);
			final int groupNameIdx = groupInfoCursor
					.getColumnIndex(DBConstants.GROUP_NAME);
			final int groupOwnerIdx = groupInfoCursor
					.getColumnIndex(DBConstants.GROUP_OWNER);
			final int groupAliveIdx = groupInfoCursor
					.getColumnIndex(DBConstants.GROUP_ALIVE);

			Map<String, Map<String, GroupParticipant>> groupIdParticipantsMap = getAllGroupParticipants();

			Log.d(getClass().getSimpleName(), "Group Conversation: "
					+ groupInfoCursor.getCount());
			while (groupInfoCursor.moveToNext()) {
				String groupId = groupInfoCursor.getString(groupIdIdx);
				String groupName = groupInfoCursor.getString(groupNameIdx);
				String groupOwner = groupInfoCursor.getString(groupOwnerIdx);
				boolean isGroupAlive = groupInfoCursor.getInt(groupAliveIdx) != 0;

				Conversation conversation = msisdnConversationMap.get(groupId);
				Map<String, GroupParticipant> groupParticipants = groupIdParticipantsMap
						.get(groupId);

				GroupConversation groupConversation = new GroupConversation(
						groupId, conversation.getConvId(), groupName,
						groupOwner, isGroupAlive);
				groupConversation.setGroupParticipantList(groupParticipants);
				groupConversation.setMessages(conversation.getMessages());

				msisdnConversationMap.remove(groupId);
				msisdnConversationMap.put(groupId, groupConversation);
			}
		} catch (Exception e) {
			Log.e("HikeConversationsDatabase",
					"Unable to retrieve conversations", e);
		} finally {
			mainCursor.close();
			if (groupInfoCursor != null) {
				groupInfoCursor.close();
			}
		}
		conversations.addAll(msisdnConversationMap.values());
		Collections.sort(conversations, Collections.reverseOrder());
		Log.d(getClass().getSimpleName(),
				"Conversation Start Time: "
						+ (System.currentTimeMillis() - startTime));
		return conversations;
	}

	private Map<String, Map<String, GroupParticipant>> getAllGroupParticipants() {
		Cursor c = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] {
				DBConstants.GROUP_ID, DBConstants.MSISDN, DBConstants.HAS_LEFT,
				DBConstants.ONHIKE, DBConstants.NAME, DBConstants.ON_DND },
				null, null, null, null, null);

		try {
			int groupIdIdx = c.getColumnIndex(DBConstants.GROUP_ID);
			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			int hasLeftIdx = c.getColumnIndex(DBConstants.HAS_LEFT);
			int onHikeIdx = c.getColumnIndex(DBConstants.ONHIKE);
			int nameIdx = c.getColumnIndex(DBConstants.NAME);
			int onDndIdx = c.getColumnIndex(DBConstants.ON_DND);

			Map<String, Map<String, GroupParticipant>> groupIdParticipantsMap = new HashMap<String, Map<String, GroupParticipant>>();
			HikeUserDatabase huDB = HikeUserDatabase.getInstance();

			while (c.moveToNext()) {
				String groupId = c.getString(groupIdIdx);
				String msisdn = c.getString(msisdnIdx);

				// TODO make this single query.
				ContactInfo contactInfo = huDB.getContactInfoFromMSISDN(msisdn,
						false);
				if (TextUtils.isEmpty(contactInfo.getName())) {
					contactInfo.setName(c.getString(nameIdx));
				}
				contactInfo.setOnhike(c.getInt(onHikeIdx) == 1 ? true : false);

				GroupParticipant groupParticipant = new GroupParticipant(
						contactInfo, c.getInt(hasLeftIdx) != 0,
						c.getInt(onDndIdx) != 0);

				Map<String, GroupParticipant> participantList = groupIdParticipantsMap
						.get(groupId);
				if (participantList == null) {
					participantList = new HashMap<String, GroupParticipant>();
					groupIdParticipantsMap.put(groupId, participantList);
				}
				participantList.put(msisdn, groupParticipant);
			}
			return groupIdParticipantsMap;
		} finally {
			c.close();
		}
	}

	private Conversation getGroupConversation(String msisdn, long convid) {
		Cursor groupCursor = null;
		try {
			groupCursor = mDb.query(DBConstants.GROUP_INFO_TABLE, new String[] {
					DBConstants.GROUP_NAME, DBConstants.GROUP_OWNER,
					DBConstants.GROUP_ALIVE, DBConstants.MUTE_GROUP },
					DBConstants.GROUP_ID + " = ? ", new String[] { msisdn },
					null, null, null);
			if (!groupCursor.moveToFirst()) {
				Log.w(getClass().getSimpleName(), "Could not find db entry: "
						+ msisdn);
				return null;
			}

			String groupName = groupCursor.getString(groupCursor
					.getColumnIndex(DBConstants.GROUP_NAME));
			String groupOwner = groupCursor.getString(groupCursor
					.getColumnIndex(DBConstants.GROUP_OWNER));
			boolean isGroupAlive = groupCursor.getInt(groupCursor
					.getColumnIndex(DBConstants.GROUP_ALIVE)) != 0;
			boolean isMuted = groupCursor.getInt(groupCursor
					.getColumnIndex(DBConstants.MUTE_GROUP)) != 0;

			GroupConversation conv = new GroupConversation(msisdn, convid,
					groupName, groupOwner, isGroupAlive);
			conv.setGroupParticipantList(getGroupParticipants(msisdn, false,
					false));
			conv.setIsMuted(isMuted);

			return conv;
		} finally {
			if (groupCursor != null) {
				groupCursor.close();
			}
		}
	}

	public JSONArray updateStatusAndSendDeliveryReport(long convID) {

		Cursor c = mDb
				.query(DBConstants.MESSAGES_TABLE,
						new String[] { DBConstants.MESSAGE_ID,
								DBConstants.MAPPED_MSG_ID },
						DBConstants.CONV_ID + "=? and "
								+ DBConstants.MSG_STATUS + "=?",
						new String[] {
								Long.toString(convID),
								Integer.toString(ConvMessage.State.RECEIVED_UNREAD
										.ordinal()) }, null, null, null);
		/* If there are no rows in the cursor then simply return null */
		if (c.getCount() <= 0) {
			c.close();
			return null;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("(");

		final int msgIdIdx = c.getColumnIndex(DBConstants.MESSAGE_ID);
		final int mappedMsgIdIdx = c.getColumnIndex(DBConstants.MAPPED_MSG_ID);

		JSONArray ids = new JSONArray();
		while (c.moveToNext()) {
			long msgId = c.getLong(msgIdIdx);
			long mappedMsgId = c.getLong(mappedMsgIdIdx);
			ids.put(String.valueOf(mappedMsgId));
			sb.append(msgId);
			if (!c.isLast()) {
				sb.append(",");
			}
		}
		sb.append(")");
		ContentValues values = new ContentValues();
		values.put(DBConstants.MSG_STATUS,
				ConvMessage.State.RECEIVED_READ.ordinal());
		int rowsAffected = mDb.update(DBConstants.MESSAGES_TABLE, values,
				DBConstants.MESSAGE_ID + " in " + sb.toString(), null);
		Log.d("HIKE CONVERSATION DB ", "Rows Updated : " + rowsAffected);
		c.close();
		return ids;
	}

	/* deletes a single message */
	public void deleteMessage(ConvMessage convMessage) {
		Long[] bindArgs = new Long[] { convMessage.getMsgID() };
		mDb.execSQL("DELETE FROM " + DBConstants.MESSAGES_TABLE + " WHERE "
				+ DBConstants.MESSAGE_ID + "= ?", bindArgs);
		HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_DELETED,
				convMessage);
	}

	public boolean wasOverlayDismissed(String msisdn) {
		Cursor c = mDb.query(DBConstants.CONVERSATIONS_TABLE,
				new String[] { DBConstants.OVERLAY_DISMISSED },
				DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null,
				null);
		int s = 0;
		if (c.moveToFirst()) {
			s = c.getInt(0);
		}
		c.close();
		return (s == 0) ? false : true;
	}

	public void setOverlay(boolean dismiss, String msisdn) {
		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.OVERLAY_DISMISSED, dismiss);
		if (msisdn != null) {
			mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues,
					DBConstants.MSISDN + "=?", new String[] { msisdn });
		} else {
			mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, null,
					null);
		}
	}

	/**
	 * Add a new participants to a group
	 * 
	 * @param groupId
	 *            The id of the group to which the participants are to be added
	 * @param participantList
	 *            A list of the participants to be added
	 */
	public int addGroupParticipants(String groupId,
			Map<String, GroupParticipant> participantList) {
		boolean participantsAlreadyAdded = true;
		boolean infoChangeOnly = false;

		Map<String, GroupParticipant> currentParticipants = getGroupParticipants(
				groupId, true, false);
		if (currentParticipants.isEmpty()) {
			participantsAlreadyAdded = false;
		}
		for (Entry<String, GroupParticipant> newParticipantEntry : participantList
				.entrySet()) {
			if (!currentParticipants.containsKey(newParticipantEntry.getKey())) {
				participantsAlreadyAdded = false;
				infoChangeOnly = false;
			} else {
				GroupParticipant currentParticipant = currentParticipants
						.get(newParticipantEntry.getKey());
				Log.d(getClass().getSimpleName(), "COMPARING current: "
						+ currentParticipant.onDnd() + " new: "
						+ newParticipantEntry.getValue().onDnd());
				if (currentParticipant.onDnd() != newParticipantEntry
						.getValue().onDnd()) {
					participantsAlreadyAdded = false;
					infoChangeOnly = true;
				}
				if (currentParticipant.getContactInfo().isOnhike() != newParticipantEntry
						.getValue().getContactInfo().isOnhike()) {
					participantsAlreadyAdded = false;
					infoChangeOnly = true;
				}
			}
		}
		if (participantsAlreadyAdded) {
			return HikeConstants.NO_CHANGE;
		}

		SQLiteStatement insertStatement = null;
		InsertHelper ih = null;
		try {
			ih = new InsertHelper(mDb, DBConstants.GROUP_MEMBERS_TABLE);
			insertStatement = mDb.compileStatement("INSERT OR REPLACE INTO "
					+ DBConstants.GROUP_MEMBERS_TABLE + " ( "
					+ DBConstants.GROUP_ID + ", " + DBConstants.MSISDN + ", "
					+ DBConstants.NAME + ", " + DBConstants.ONHIKE + ", "
					+ DBConstants.HAS_LEFT + ", " + DBConstants.ON_DND + ", "
					+ DBConstants.SHOWN_STATUS + " ) "
					+ " VALUES (?, ?, ?, ?, ?, ?, ?)");
			mDb.beginTransaction();
			for (Entry<String, GroupParticipant> participant : participantList
					.entrySet()) {
				GroupParticipant groupParticipant = participant.getValue();
				insertStatement.bindString(
						ih.getColumnIndex(DBConstants.GROUP_ID), groupId);
				insertStatement.bindString(
						ih.getColumnIndex(DBConstants.MSISDN),
						participant.getKey());
				insertStatement.bindString(ih.getColumnIndex(DBConstants.NAME),
						groupParticipant.getContactInfo().getName());
				insertStatement.bindLong(ih.getColumnIndex(DBConstants.ONHIKE),
						groupParticipant.getContactInfo().isOnhike() ? 1 : 0);
				insertStatement.bindLong(
						ih.getColumnIndex(DBConstants.HAS_LEFT), 0);
				insertStatement.bindLong(ih.getColumnIndex(DBConstants.ON_DND),
						groupParticipant.onDnd() ? 1 : 0);
				insertStatement.bindLong(
						ih.getColumnIndex(DBConstants.SHOWN_STATUS),
						groupParticipant.getContactInfo().isOnhike() ? 1 : 0);

				insertStatement.executeInsert();
			}
			return infoChangeOnly ? HikeConstants.PARTICIPANT_STATUS_CHANGE
					: HikeConstants.NEW_PARTICIPANT;
		} finally {
			if (insertStatement != null) {
				insertStatement.close();
			}
			if (ih != null) {
				ih.close();
			}
			mDb.setTransactionSuccessful();
			mDb.endTransaction();
		}
	}

	public int updateDndStatus(String msisdn) {
		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.ON_DND, 0);
		return mDb.update(DBConstants.GROUP_MEMBERS_TABLE, contentValues,
				DBConstants.MSISDN + "=?", new String[] { msisdn });
	}

	public int updateShownStatus(String groupId) {
		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.SHOWN_STATUS, 1);
		return mDb.update(DBConstants.GROUP_MEMBERS_TABLE, contentValues,
				DBConstants.GROUP_ID + "=?", new String[] { groupId });
	}

	/**
	 * Should be called when a participant leaves the group
	 * 
	 * @param groupId
	 *            : The group ID of the group containing the participant
	 * @param msisdn
	 *            : The msisdn of the participant
	 */
	public int setParticipantLeft(String groupId, String msisdn) {
		if (!doesConversationExist(groupId)) {
			return 0;
		}
		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.HAS_LEFT, 1);
		return mDb.update(DBConstants.GROUP_MEMBERS_TABLE, contentValues,
				DBConstants.GROUP_ID + " = ? AND " + DBConstants.MSISDN
						+ " = ? ", new String[] { groupId, msisdn });
	}

	/**
	 * Returns a list of participants to a group
	 * 
	 * @param groupId
	 * @return
	 */
	public Map<String, GroupParticipant> getGroupParticipants(String groupId,
			boolean activeOnly, boolean notShownStatusMsgOnly) {
		String selection = DBConstants.GROUP_ID
				+ " =? "
				+ (activeOnly ? " AND " + DBConstants.HAS_LEFT + "=0" : "")
				+ (notShownStatusMsgOnly ? " AND " + DBConstants.SHOWN_STATUS
						+ "=0" : "");
		Cursor c = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] {
				DBConstants.MSISDN, DBConstants.HAS_LEFT, DBConstants.ONHIKE,
				DBConstants.NAME, DBConstants.ON_DND }, selection,
				new String[] { groupId }, null, null, null);

		Map<String, GroupParticipant> participantList = new HashMap<String, GroupParticipant>();

		HikeUserDatabase huDB = HikeUserDatabase.getInstance();
		while (c.moveToNext()) {
			String msisdn = c.getString(c.getColumnIndex(DBConstants.MSISDN));
			ContactInfo contactInfo = huDB.getContactInfoFromMSISDN(msisdn,
					false);
			if (TextUtils.isEmpty(contactInfo.getName())) {
				contactInfo.setName(c.getString(c
						.getColumnIndex(DBConstants.NAME)));
			}
			contactInfo
					.setOnhike(c.getInt(c.getColumnIndex(DBConstants.ONHIKE)) == 1 ? true
							: false);

			GroupParticipant groupParticipant = new GroupParticipant(
					contactInfo, c.getInt(c
							.getColumnIndex(DBConstants.HAS_LEFT)) != 0,
					c.getInt(c.getColumnIndex(DBConstants.ON_DND)) != 0);
			participantList.put(msisdn, groupParticipant);
			Log.d(getClass().getSimpleName(), "Fetching participant: " + msisdn);
		}
		c.close();
		return participantList;
	}

	/**
	 * Reutrn the group name corresponding to a group ID.
	 * 
	 * @param groupId
	 * @return
	 */
	public String getGroupName(String groupId) {
		Cursor c = mDb.query(DBConstants.GROUP_INFO_TABLE,
				new String[] { DBConstants.GROUP_NAME }, DBConstants.GROUP_ID
						+ " = ? ", new String[] { groupId }, null, null, null);
		String groupName = "";
		if (c.moveToFirst()) {
			groupName = c.getString(c.getColumnIndex(DBConstants.GROUP_NAME));
		}
		c.close();
		return groupName;
	}

	public boolean doesConversationExist(String msisdn) {
		Cursor c = mDb.query(DBConstants.CONVERSATIONS_TABLE,
				new String[] { DBConstants.MSISDN }, DBConstants.MSISDN
						+ " = ? ", new String[] { msisdn }, null, null, null);
		try {
			return c.moveToFirst();
		} finally {
			c.close();
		}
	}

	public boolean isGroupMuted(String groupId) {
		Cursor c = mDb.query(DBConstants.GROUP_INFO_TABLE,
				new String[] { DBConstants.GROUP_ID }, DBConstants.GROUP_ID
						+ " = ? AND " + DBConstants.MUTE_GROUP + " = 1",
				new String[] { groupId }, null, null, null);
		try {
			return c.moveToFirst();
		} finally {
			c.close();
		}
	}

	public void toggleGroupMute(String groupId, boolean isMuted) {
		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.MUTE_GROUP, isMuted);

		mDb.update(DBConstants.GROUP_INFO_TABLE, contentValues,
				DBConstants.GROUP_ID + "=?", new String[] { groupId });
	}

	public int setGroupName(String groupId, String groupname) {
		if (!doesConversationExist(groupId)) {
			return 0;
		}
		ContentValues values = new ContentValues(1);
		values.put(DBConstants.GROUP_NAME, groupname);
		return mDb.update(DBConstants.GROUP_INFO_TABLE, values,
				DBConstants.GROUP_ID + " = ?", new String[] { groupId });
	}

	public String getParticipantName(String groupId, String msisdn) {
		Cursor c = mDb.query(DBConstants.GROUP_MEMBERS_TABLE,
				new String[] { DBConstants.NAME }, DBConstants.GROUP_ID
						+ " = ? AND " + DBConstants.MSISDN + " = ? ",
				new String[] { groupId, msisdn }, null, null, null);
		String name = "";
		if (c.moveToFirst()) {
			name = c.getString(c.getColumnIndex(DBConstants.NAME));
		}
		c.close();
		return name;
	}

	public int setGroupDead(String groupId) {
		if (!doesConversationExist(groupId)) {
			return 0;
		}
		ContentValues values = new ContentValues(1);
		values.put(DBConstants.GROUP_ALIVE, 0);
		return mDb.update(DBConstants.GROUP_INFO_TABLE, values,
				DBConstants.GROUP_ID + " = ?", new String[] { groupId });
	}

	public long[] getUnreadMessageIds(long convId) {
		Cursor cursor = mDb.query(DBConstants.MESSAGES_TABLE,
				new String[] { DBConstants.MESSAGE_ID },
				DBConstants.MSG_STATUS + " IN " + "("
						+ ConvMessage.State.SENT_CONFIRMED.ordinal() + ", "
						+ ConvMessage.State.SENT_DELIVERED.ordinal() + ")"
						+ " AND " + DBConstants.CONV_ID + " =?",
				new String[] { convId + "" }, null, null, null);
		try {
			Log.d(getClass().getSimpleName(),
					"Number of unread messages for conversation " + convId
							+ " = " + cursor.getCount());
			if (!cursor.moveToFirst()) {
				return null;
			}
			long[] ids = new long[cursor.getCount()];
			int i = 0;
			int idIdx = cursor.getColumnIndex(DBConstants.MESSAGE_ID);
			do {
				ids[i++] = cursor.getLong(idIdx);
				Log.d(getClass().getSimpleName(), "Inserting id: " + ids[i - 1]);
			} while (cursor.moveToNext());
			return ids;
		} finally {
			cursor.close();
		}
	}

	public void addFile(String fileKey, String fileName) {
		Log.d(getClass().getSimpleName(), "Adding file with File Key: "
				+ fileKey + " File Name: " + fileName);
		InsertHelper ih = null;
		try {
			ih = new InsertHelper(mDb, DBConstants.FILE_TABLE);
			ih.prepareForInsert();
			ih.bind(ih.getColumnIndex(DBConstants.FILE_KEY), fileKey);
			ih.bind(ih.getColumnIndex(DBConstants.FILE_NAME), fileName);
			ih.execute();
		} finally {
			if (ih != null) {
				ih.close();
			}
		}
	}

	public String getFileKey(String fileName) {
		Cursor cursor = null;
		try {
			cursor = mDb.query(DBConstants.FILE_TABLE,
					new String[] { DBConstants.FILE_KEY },
					DBConstants.FILE_NAME + " =?", new String[] { fileName },
					null, null, null);
			if (cursor.moveToFirst()) {
				return cursor.getString(cursor
						.getColumnIndex(DBConstants.FILE_KEY));
			}
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public List<String> listOfGroupConversationsWithMsisdn(String msisdn) {
		Cursor cursor = null;
		try {
			List<String> groupConversations = new ArrayList<String>();
			cursor = mDb.query(DBConstants.GROUP_MEMBERS_TABLE,
					new String[] { DBConstants.GROUP_ID }, DBConstants.MSISDN
							+ "=? AND " + DBConstants.HAS_LEFT + "=0",
					new String[] { msisdn }, null, null, null);
			int groupIdIdx = cursor.getColumnIndex(DBConstants.GROUP_ID);
			while (cursor.moveToNext()) {
				groupConversations.add(cursor.getString(groupIdIdx));
			}
			return groupConversations;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * Called when forwarding a message so that the groups can also be displayed
	 * in the contact list.
	 * 
	 * @return
	 */
	public List<ContactInfo> getGroupNameAndParticipantsAsContacts() {
		Cursor groupCursor = null;
		try {
			List<ContactInfo> groups = new ArrayList<ContactInfo>();
			groupCursor = mDb.query(DBConstants.GROUP_INFO_TABLE, new String[] {
					DBConstants.GROUP_ID, DBConstants.GROUP_NAME },
					DBConstants.GROUP_ALIVE + "=1", null, null, null, null);
			int groupNameIdx = groupCursor
					.getColumnIndex(DBConstants.GROUP_NAME);
			int groupIdIdx = groupCursor.getColumnIndex(DBConstants.GROUP_ID);
			while (groupCursor.moveToNext()) {
				String groupId = groupCursor.getString(groupIdIdx);
				String groupName = groupCursor.getString(groupNameIdx);

				Map<String, GroupParticipant> groupParticipantMap = getGroupParticipants(
						groupId, true, false);
				groupName = TextUtils.isEmpty(groupName) ? Utils
						.defaultGroupName(groupParticipantMap) : groupName;
				int numMembers = groupParticipantMap.size();

				// Here we make this string the msisdn so that it can be
				// displayed in the list view when forwarding the message
				String numberMembers = numMembers
						+ (numMembers > 0 ? " Members" : " Member");

				groups.add(new ContactInfo(groupId, numberMembers, groupName,
						groupId, true));
			}

			return groups;
		} finally {
			if (groupCursor != null) {
				groupCursor.close();
			}
		}
	}

	public void updateRecencyOfEmoticon(int emoticonIndex, long lastUsed) {
		SQLiteStatement insertStatement = null;
		try {
			insertStatement = mDb.compileStatement("INSERT OR REPLACE INTO "
					+ DBConstants.EMOTICON_TABLE + " ( "
					+ DBConstants.EMOTICON_NUM + ", " + DBConstants.LAST_USED
					+ " ) " + " VALUES (?, ?)");

			insertStatement.bindLong(1, emoticonIndex);
			insertStatement.bindLong(2, lastUsed);

			long id = insertStatement.executeInsert();
			Log.d(getClass().getSimpleName(), "iNserted row: " + id);
		} finally {
			if (insertStatement != null) {
				insertStatement.close();
			}
		}
	}

	public int[] fetchEmoticonsOfType(EmoticonType emoticonType,
			int startOffset, int endOffset, int limit) {
		Cursor c = null;
		try {
			String[] columns = new String[] { DBConstants.EMOTICON_NUM };
			String selection = DBConstants.EMOTICON_NUM
					+ ">="
					+ startOffset
					+ (endOffset != 0 ? " AND " + DBConstants.EMOTICON_NUM
							+ "<" + (endOffset) : "");
			Log.d(getClass().getSimpleName(), selection);
			String orderBy = DBConstants.LAST_USED + " DESC LIMIT " + limit;

			c = mDb.query(DBConstants.EMOTICON_TABLE, columns, selection, null,
					null, null, orderBy);
			int[] emoticonIndices = new int[c.getCount()];
			int emoticonIndexIdx = c.getColumnIndex(DBConstants.EMOTICON_NUM);
			int i = 0;
			while (c.moveToNext()) {
				emoticonIndices[i++] = emoticonType == EmoticonType.HIKE_EMOTICON ? c
						.getInt(emoticonIndexIdx) : c.getInt(emoticonIndexIdx)
						- EmoticonConstants.DEFAULT_SMILEY_RES_IDS.length;
			}
			Log.d(getClass().getSimpleName(), "Emoticon RES ID size: "
					+ emoticonIndices.length);
			return emoticonIndices;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}
}
