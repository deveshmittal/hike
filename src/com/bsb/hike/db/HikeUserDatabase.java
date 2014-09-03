package com.bsb.hike.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.graphics.drawable.Drawable;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.FtueContactsData;
import com.bsb.hike.utils.ContactUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class HikeUserDatabase extends SQLiteOpenHelper
{
	private SQLiteDatabase mDb;

	private SQLiteDatabase mReadDb;

	private static Context mContext;

	private static HikeUserDatabase hikeUserDatabase;

	public static void init(Context context)
	{
		if (hikeUserDatabase == null)
		{
			mContext = context;
			hikeUserDatabase = new HikeUserDatabase(context);
		}
	}

	public static HikeUserDatabase getInstance()
	{
		return hikeUserDatabase;
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		String create = "CREATE TABLE IF NOT EXISTS " + DBConstants.USERS_TABLE + " ( " + DBConstants.ID + " STRING , " + DBConstants.NAME + " TEXT, " + DBConstants.MSISDN
				+ " TEXT COLLATE nocase, " + DBConstants.ONHIKE + " INTEGER, " + DBConstants.PHONE + " TEXT, " + DBConstants.HAS_CUSTOM_PHOTO + " INTEGER, "
				+ DBConstants.OVERLAY_DISMISSED + " INTEGER, " + DBConstants.MSISDN_TYPE + " STRING, " + DBConstants.LAST_MESSAGED + " INTEGER, " + DBConstants.HIKE_JOIN_TIME
				+ " INTEGER DEFAULT 0, " + DBConstants.LAST_SEEN + " INTEGER DEFAULT -1, " + DBConstants.IS_OFFLINE + " INTEGER DEFAULT 1, " + DBConstants.INVITE_TIMESTAMP
				+ " INTEGER DEFAULT 0" + " )";

		db.execSQL(create);

		create = "CREATE TABLE IF NOT EXISTS " + DBConstants.BLOCK_TABLE + " ( " + DBConstants.MSISDN + " TEXT " + " ) ";
		db.execSQL(create);

		create = "CREATE TABLE IF NOT EXISTS " + DBConstants.THUMBNAILS_TABLE + " ( " + DBConstants.MSISDN + " TEXT PRIMARY KEY, " + DBConstants.IMAGE + " BLOB" + " ) ";
		db.execSQL(create);

		create = "CREATE TABLE IF NOT EXISTS " + DBConstants.FAVORITES_TABLE + " ( " + DBConstants.MSISDN + " TEXT PRIMARY KEY, " + DBConstants.FAVORITE_TYPE + " INTEGER" + " ) ";
		db.execSQL(create);

		create = "CREATE INDEX IF NOT EXISTS " + DBConstants.USER_INDEX + " ON " + DBConstants.USERS_TABLE + " (" + DBConstants.MSISDN + ")";
		db.execSQL(create);

		create = "CREATE INDEX IF NOT EXISTS " + DBConstants.THUMBNAIL_INDEX + " ON " + DBConstants.THUMBNAILS_TABLE + " (" + DBConstants.MSISDN + ")";
		db.execSQL(create);

		create = "CREATE INDEX IF NOT EXISTS " + DBConstants.FAVORITE_INDEX + " ON " + DBConstants.FAVORITES_TABLE + " (" + DBConstants.MSISDN + ")";
		db.execSQL(create);

		create = "CREATE TABLE IF NOT EXISTS " + DBConstants.ROUNDED_THUMBNAIL_TABLE + " ( " + DBConstants.MSISDN + " TEXT PRIMARY KEY, " + DBConstants.IMAGE + " BLOB" + " ) ";
		db.execSQL(create);

		create = "CREATE INDEX IF NOT EXISTS " + DBConstants.ROUNDED_THUMBNAIL_INDEX + " ON " + DBConstants.THUMBNAILS_TABLE + " (" + DBConstants.MSISDN + ")";
		db.execSQL(create);
	}

	private HikeUserDatabase(Context context)
	{
		super(context, DBConstants.USERS_DATABASE_NAME, null, DBConstants.USERS_DATABASE_VERSION);
		mDb = getWritableDatabase();
		mReadDb = getReadableDatabase();
	}

	@Override
	public void close()
	{
		mDb.close();
		mReadDb.close();
		super.close();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		Logger.d(getClass().getSimpleName(), "Upgrading users table from " + oldVersion + " to " + newVersion);
		if (oldVersion < 3)
		{
			String alter1 = "ALTER TABLE " + DBConstants.USERS_TABLE + " ADD COLUMN " + DBConstants.MSISDN_TYPE + " STRING";
			String alter2 = "ALTER TABLE " + DBConstants.USERS_TABLE + " ADD COLUMN " + DBConstants.LAST_MESSAGED + " INTEGER";
			db.execSQL(alter1);
			db.execSQL(alter2);
		}
		// Changing the datatype of the name column
		if (oldVersion < 4)
		{
			Logger.d(getClass().getSimpleName(), "Updating table");
			String alter = "ALTER TABLE " + DBConstants.USERS_TABLE + " RENAME TO " + "temp_table";

			String create = "CREATE TABLE IF NOT EXISTS " + DBConstants.USERS_TABLE + " ( " + DBConstants.ID + " STRING , " + DBConstants.NAME + " TEXT, " + DBConstants.MSISDN
					+ " TEXT COLLATE nocase, " + DBConstants.ONHIKE + " INTEGER, " + DBConstants.PHONE + " TEXT, " + DBConstants.HAS_CUSTOM_PHOTO + " INTEGER, "
					+ DBConstants.OVERLAY_DISMISSED + " INTEGER, " + DBConstants.MSISDN_TYPE + " STRING, " + DBConstants.LAST_MESSAGED + " INTEGER" + " )";

			String insert = "INSERT INTO " + DBConstants.USERS_TABLE + " SELECT * FROM temp_table";

			String drop = "DROP TABLE temp_table";

			db.execSQL(alter);
			db.execSQL(create);
			db.execSQL(insert);
			db.execSQL(drop);
		}
		/*
		 * Keeping this as comments to show why we need the next if clause.
		 */
		// Add favorite column
		// if (oldVersion < 5) {
		// String alter = "ALTER TABLE " + DBConstants.USERS_TABLE
		// + " ADD COLUMN " + DBConstants.FAVORITE
		// + " INTEGER DEFAULT " + FavoriteType.NOT_FAVORITE.ordinal();
		// db.execSQL(alter);
		// }
		if (oldVersion < 7)
		{
			// Create the favorites table.
			onCreate(db);

			String tempTable = "tempTable";
			String alter = "ALTER TABLE " + DBConstants.USERS_TABLE + " RENAME TO " + tempTable;

			String create = "CREATE TABLE IF NOT EXISTS " + DBConstants.USERS_TABLE + " ( " + DBConstants.ID + " STRING , " + DBConstants.NAME + " TEXT, " + DBConstants.MSISDN
					+ " TEXT COLLATE nocase, " + DBConstants.ONHIKE + " INTEGER, " + DBConstants.PHONE + " TEXT, " + DBConstants.HAS_CUSTOM_PHOTO + " INTEGER, "
					+ DBConstants.OVERLAY_DISMISSED + " INTEGER, " + DBConstants.MSISDN_TYPE + " STRING, " + DBConstants.LAST_MESSAGED + " INTEGER" + " )";

			String insert = "INSERT INTO " + DBConstants.USERS_TABLE + " SELECT " + DBConstants.ID + ", " + DBConstants.NAME + ", " + DBConstants.MSISDN + ", "
					+ DBConstants.ONHIKE + ", " + DBConstants.PHONE + ", " + DBConstants.HAS_CUSTOM_PHOTO + ", " + DBConstants.OVERLAY_DISMISSED + ", " + DBConstants.MSISDN_TYPE
					+ ", " + DBConstants.LAST_MESSAGED + " FROM " + tempTable;

			String drop = "DROP TABLE " + tempTable;

			db.execSQL(alter);
			db.execSQL(create);
			db.execSQL(insert);
			db.execSQL(drop);
		}
		if (oldVersion < 8)
		{
			String alter = "ALTER TABLE " + DBConstants.USERS_TABLE + " ADD COLUMN " + DBConstants.HIKE_JOIN_TIME + " INTEGER DEFAULT 0";
			db.execSQL(alter);
		}
		if (oldVersion < 10)
		{
			/*
			 * Removing all auto recommended favorites.
			 */
			db.delete(DBConstants.FAVORITES_TABLE, DBConstants.FAVORITE_TYPE + "=" + FavoriteType.AUTO_RECOMMENDED_FAVORITE.ordinal(), null);
		}
		/*
		 * Version 11 for adding indexes.
		 */
		if (oldVersion < 11)
		{
			onCreate(db);
		}
		/*
		 * Version 12 for added last seen column
		 */
		if (oldVersion < 12)
		{
			String alter = "ALTER TABLE " + DBConstants.USERS_TABLE + " ADD COLUMN " + DBConstants.LAST_SEEN + " INTEGER DEFAULT -1";
			db.execSQL(alter);
		}
		/*
		 * Version 13 for is online column
		 */
		if (oldVersion < 13)
		{
			String alter = "ALTER TABLE " + DBConstants.USERS_TABLE + " ADD COLUMN " + DBConstants.IS_OFFLINE + " INTEGER DEFAULT 1";
			db.execSQL(alter);
		}

		/*
		 * Version 14 is for the rounded thumbnails
		 */
		if (oldVersion < 14)
		{
			onCreate(db);
			// set the preferences to 1 , for UserDBAvtar being called for
			// upgrade/
			// this will be used in hike messenger app and home activity while
			// computing the spinner state.
			Editor editor = mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
			editor.putInt(HikeConstants.UPGRADE_AVATAR_PROGRESS_USER, 1);
			editor.commit();
		}
		/*
		 * Version 15 adds the invited timestamp column
		 */
		if (oldVersion < 15)
		{
			String alter = "ALTER TABLE " + DBConstants.USERS_TABLE + " ADD COLUMN " + DBConstants.INVITE_TIMESTAMP + " INTEGER DEFAULT 0";
			db.execSQL(alter);
		}
	}

	public void makeOlderAvatarsRounded()
	{
		makeOlderAvatarsRounded(mDb);
	}

	public void addContacts(List<ContactInfo> contacts, boolean isFirstSync) throws DbException
	{
		SQLiteDatabase db = mDb;
		InsertHelper ih = null;
		

		Map<String, String> msisdnTypeMap = new HashMap<String, String>();
		/*
		 * Since this is the first sync, we just run one query and pickup all the extra info required. For all subsequent syncs we run the query for each contact separately.
		 */
		try
		{
			db.beginTransaction();
			if (isFirstSync)
			{
				// Adding the last contacted and phone type info
				Cursor extraInfo = this.mContext.getContentResolver().query(Phone.CONTENT_URI, new String[] { Phone.NUMBER, Phone.TYPE, Phone.LABEL }, null, null, null);

				int msisdnIdx = extraInfo.getColumnIndex(Phone.NUMBER);
				int typeIdx = extraInfo.getColumnIndex(Phone.TYPE);
				int labelIdx = extraInfo.getColumnIndex(Phone.LABEL);

				while (extraInfo.moveToNext())
				{
					String msisdnType = Phone.getTypeLabel(this.mContext.getResources(), extraInfo.getInt(typeIdx), extraInfo.getString(labelIdx)).toString();

					msisdnTypeMap.put(extraInfo.getString(msisdnIdx), msisdnType);
				}
				extraInfo.close();
			}

			ih = new InsertHelper(db, DBConstants.USERS_TABLE);
			final int msisdnColumn = ih.getColumnIndex(DBConstants.MSISDN);
			final int idColumn = ih.getColumnIndex(DBConstants.ID);
			final int nameColumn = ih.getColumnIndex(DBConstants.NAME);
			final int onHikeColumn = ih.getColumnIndex(DBConstants.ONHIKE);
			final int phoneColumn = ih.getColumnIndex(DBConstants.PHONE);
			final int msisdnTypeColumn = ih.getColumnIndex(DBConstants.MSISDN_TYPE);
			for (ContactInfo contact : contacts)
			{
				ih.prepareForReplace();
				ih.bind(nameColumn, contact.getName());
				ih.bind(msisdnColumn, contact.getMsisdn());
				ih.bind(idColumn, contact.getId());
				ih.bind(onHikeColumn, contact.isOnhike());
				ih.bind(phoneColumn, contact.getPhoneNum());
				if (!isFirstSync)
				{
					String selection = Phone.CONTACT_ID + " =? " + " AND " + Phone.NUMBER + " =? ";
					// Adding the last contacted and phone type info
					Cursor additionalInfo = this.mContext.getContentResolver().query(Phone.CONTENT_URI, new String[] { Phone.TYPE, Phone.LABEL }, selection,
							new String[] { contact.getId(), contact.getPhoneNum() }, null);

					int typeIdx = additionalInfo.getColumnIndex(Phone.TYPE);
					int labelIdx = additionalInfo.getColumnIndex(Phone.LABEL);
					if (additionalInfo.moveToFirst())
					{
						contact.setMsisdnType(Phone.getTypeLabel(this.mContext.getResources(), additionalInfo.getInt(typeIdx), additionalInfo.getString(labelIdx)).toString());
					}
					additionalInfo.close();

					ih.bind(msisdnTypeColumn, contact.getMsisdnType());

					/*
					 * We add to favorites this reference. So should set the favorite type here.
					 */
					Cursor favoriteCursor = mDb.query(DBConstants.FAVORITES_TABLE, new String[] { DBConstants.FAVORITE_TYPE }, DBConstants.MSISDN + "=?",
							new String[] { contact.getMsisdn() }, null, null, null);
					try
					{
						if (favoriteCursor.moveToFirst())
						{
							int favoriteTypeOrdinal = favoriteCursor.getInt(favoriteCursor.getColumnIndex(DBConstants.FAVORITE_TYPE));
							contact.setFavoriteType(FavoriteType.values()[favoriteTypeOrdinal]);
						}
						else
						{
							contact.setFavoriteType(FavoriteType.NOT_FRIEND);
						}
					}
					finally
					{
						favoriteCursor.close();
					}
					HikeMessengerApp.getPubSub().publish(HikePubSub.CONTACT_ADDED, contact);
				}
				else
				{
					ih.bind(msisdnTypeColumn, msisdnTypeMap.get(contact.getPhoneNum()));
					/*
					 * We're saving this parameter to notify that the extra info that we are now fetching (Msisdn type) has been synced. So for apps that update from an older
					 * version, we can just check this value to verify whether the contacts have their extra info synced.
					 */
					Editor editor = mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
					editor.putBoolean(HikeMessengerApp.CONTACT_EXTRA_INFO_SYNCED, true);
					editor.commit();
				}
				ih.execute();
			}
			db.setTransactionSuccessful();
		}
		catch (Exception e)
		{
			Logger.e("HikeUserDatabase", "Unable to insert contacts", e);
			throw new DbException(e);
		}
		finally
		{
			if (ih != null)
			{
				ih.close();
			}
			db.endTransaction();
		}
	}

	public void addBlockList(List<String> msisdns) throws DbException
	{
		if (msisdns == null)
		{
			return;
		}

		SQLiteDatabase db = mDb;
		

		InsertHelper ih = null;
		try
		{
			db.beginTransaction();
			ih = new InsertHelper(db, DBConstants.BLOCK_TABLE);
			final int msisdnColumn = ih.getColumnIndex(DBConstants.MSISDN);
			for (String msisdn : msisdns)
			{
				ih.prepareForReplace();
				ih.bind(msisdnColumn, msisdn);
				ih.execute();
			}
			db.setTransactionSuccessful();
		}
		catch (Exception e)
		{
			Logger.e("HikeUserDatabase", "Unable to insert contacts", e);
			throw new DbException(e);
		}
		finally
		{
			if (ih != null)
			{
				ih.close();
			}
			db.endTransaction();
		}
	}

	/**
	 * Sets the address book from the list of contacts Deletes any existing contacts from the db
	 * 
	 * @param contacts
	 *            list of contacts to set/add
	 */
	public void setAddressBookAndBlockList(List<ContactInfo> contacts, List<String> blockedMsisdns) throws DbException
	{
		/* delete all existing entries from database */
		mDb.delete(DBConstants.USERS_TABLE, null, null);

		mDb.delete(DBConstants.BLOCK_TABLE, null, null);

		addContacts(contacts, true);
		addBlockList(blockedMsisdns);
	}

	public ContactInfo getContactInfoFromMSISDN(String msisdn, boolean ifNotFoundReturnNull)
	{
		Cursor c = null;
		List<ContactInfo> contactInfos = null;
		try
		{
			c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, "max(" + DBConstants.ID + ") as " + DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE, DBConstants.PHONE,
					DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO, DBConstants.FAVORITE_TYPE_SELECTION, DBConstants.HIKE_JOIN_TIME,
					DBConstants.IS_OFFLINE, DBConstants.LAST_SEEN }, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);

			contactInfos = extractContactInfo(c);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

		if (contactInfos != null && contactInfos.isEmpty())
		{
			Logger.d(getClass().getSimpleName(), "No contact found");
			if (ifNotFoundReturnNull)
			{
				return null;
			}
			else
			{
				Cursor favoriteCursor = null;
				try
				{
					favoriteCursor = mReadDb.query(DBConstants.FAVORITES_TABLE, new String[] { DBConstants.FAVORITE_TYPE }, DBConstants.MSISDN + " =? ", new String[] { msisdn },
							null, null, null);

					/*
					 * Setting the favorite type for unknown contacts
					 */
					FavoriteType favoriteType = FavoriteType.NOT_FRIEND;
					if (favoriteCursor.moveToFirst())
					{
						favoriteType = FavoriteType.values()[favoriteCursor.getInt(favoriteCursor.getColumnIndex(DBConstants.FAVORITE_TYPE))];
					}

					String name = null;
					/*
					 * Setting the hike bot name if the msisdn is a hikebot msisdn.
					 */
					if (HikeMessengerApp.hikeBotNamesMap.containsKey(msisdn))
					{
						name = HikeMessengerApp.hikeBotNamesMap.get(msisdn);
					}

					ContactInfo contactInfo = new ContactInfo(msisdn, msisdn, name, msisdn, false);
					contactInfo.setFavoriteType(favoriteType);
					return contactInfo;
				}
				finally
				{
					if (favoriteCursor != null)
					{
						favoriteCursor.close();
					}
				}
			}
		}

		return contactInfos.get(0);
	}

	public List<ContactInfo> getContactInfoFromMSISDN(String[] msisdn)
	{
		Cursor c = null;
		List<ContactInfo> contactInfos = null;
		try
		{
			c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE, DBConstants.PHONE,
					DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO, DBConstants.FAVORITE_TYPE_SELECTION, DBConstants.HIKE_JOIN_TIME,
					DBConstants.IS_OFFLINE, DBConstants.LAST_SEEN }, DBConstants.MSISDN + "=?", msisdn, null, null, null);
			contactInfos = extractContactInfo(c);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

		return contactInfos;

	}

	public List<ContactInfo> getHikeContacts(int limit, String msisdnsIn, String msisdnsNotIn, String myMsisdn)
	{
		Cursor c = null;
		List<ContactInfo> contactInfos = null;
		try
		{
			StringBuilder selectionBuilder = new StringBuilder();
			if (!TextUtils.isEmpty(msisdnsIn))
			{
				selectionBuilder.append(DBConstants.MSISDN + " IN " + msisdnsIn + " AND ");
			}
			if (!TextUtils.isEmpty(msisdnsNotIn))
			{
				selectionBuilder.append(DBConstants.MSISDN + " NOT IN " + msisdnsNotIn + " AND ");
			}
			if (limit > 0)
			{
				c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE, DBConstants.PHONE,
						DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO, DBConstants.FAVORITE_TYPE_SELECTION, DBConstants.HIKE_JOIN_TIME,
						DBConstants.IS_OFFLINE, DBConstants.LAST_SEEN }, selectionBuilder.toString() + DBConstants.MSISDN + "!=" + DatabaseUtils.sqlEscapeString(myMsisdn)
						+ " AND " + DBConstants.ONHIKE + "=1 LIMIT " + limit, null, null, null, null);
			}
			else
			{
				c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE, DBConstants.PHONE,
						DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO, DBConstants.FAVORITE_TYPE_SELECTION, DBConstants.HIKE_JOIN_TIME,
						DBConstants.IS_OFFLINE, DBConstants.LAST_SEEN }, selectionBuilder.toString() + DBConstants.MSISDN + "!=" + DatabaseUtils.sqlEscapeString(myMsisdn)
						+ " AND " + DBConstants.ONHIKE + "=1", null, null, null, null);
			}
			contactInfos = extractContactInfo(c, true);
			return contactInfos;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	private List<ContactInfo> extractContactInfo(Cursor c)
	{
		return extractContactInfo(c, false);
	}

	private List<ContactInfo> extractContactInfo(Cursor c, boolean distinct)
	{
		List<ContactInfo> contactInfos = new ArrayList<ContactInfo>(c.getCount());
		int idx = c.getColumnIndex(DBConstants.ID);
		int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
		int nameIdx = c.getColumnIndex(DBConstants.NAME);
		int onhikeIdx = c.getColumnIndex(DBConstants.ONHIKE);
		int phoneNumIdx = c.getColumnIndex(DBConstants.PHONE);
		int msisdnTypeIdx = c.getColumnIndex(DBConstants.MSISDN_TYPE);
		int lastMessagedIdx = c.getColumnIndex(DBConstants.LAST_MESSAGED);
		int hasCustomPhotoIdx = c.getColumnIndex(DBConstants.HAS_CUSTOM_PHOTO);
		int favoriteIdx = c.getColumnIndex(DBConstants.FAVORITE_TYPE);
		int hikeJoinTimeIdx = c.getColumnIndex(DBConstants.HIKE_JOIN_TIME);
		int isOfflineIdx = c.getColumnIndex(DBConstants.IS_OFFLINE);
		int lastSeenTimeIdx = c.getColumnIndex(DBConstants.LAST_SEEN);

		Set<String> msisdnSet = null;

		if (distinct)
		{
			msisdnSet = new HashSet<String>();
		}
		while (c.moveToNext())
		{
			String msisdn = c.getString(msisdnIdx);

			/*
			 * query with aggregate functions always return at least one row which will have everything null. Accounting for that.
			 */
			if (TextUtils.isEmpty(msisdn))
			{
				continue;
			}

			if (distinct && msisdnSet.contains(msisdn))
			{
				continue;
			}
			else if (distinct)
			{
				msisdnSet.add(msisdn);
			}
			long hikeJoinTime = 0;
			if (hikeJoinTimeIdx != -1)
			{
				hikeJoinTime = c.getLong(hikeJoinTimeIdx);
			}
			ContactInfo contactInfo = new ContactInfo(c.getString(idx), c.getString(msisdnIdx), c.getString(nameIdx), c.getString(phoneNumIdx), c.getInt(onhikeIdx) != 0,
					c.getString(msisdnTypeIdx), c.getLong(lastMessagedIdx), c.getInt(hasCustomPhotoIdx) == 1, hikeJoinTime);
			if (favoriteIdx != -1)
			{
				int favoriteTypeOrd = c.getInt(favoriteIdx);
				contactInfo.setFavoriteType(FavoriteType.values()[favoriteTypeOrd]);
			}
			else
			{
				contactInfo.setFavoriteType(FavoriteType.NOT_FRIEND);
			}

			if (isOfflineIdx != -1)
			{
				contactInfo.setOffline(c.getInt(isOfflineIdx));
			}

			if (lastSeenTimeIdx != -1)
			{
				contactInfo.setLastSeenTime(c.getLong(lastSeenTimeIdx));
			}

			contactInfos.add(contactInfo);
		}
		return contactInfos;
	}

	public ContactInfo getContactInfoFromId(String id)
	{
		Cursor c = null;
		try
		{
			c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE, DBConstants.PHONE,
					DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO }, DBConstants.ID + "=?", new String[] { id }, null, null, null);
			List<ContactInfo> contactInfos = extractContactInfo(c);
			if (contactInfos.isEmpty())
			{
				return null;
			}

			return contactInfos.get(0);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	/**
	 * Adds a single contact to the hike user db
	 * 
	 * @param hikeContactInfo
	 *            contact to add
	 * @return true if the insert was successful
	 */
	public void addContact(ContactInfo hikeContactInfo) throws DbException
	{
		List<ContactInfo> l = new LinkedList<ContactInfo>();
		l.add(hikeContactInfo);
		addContacts(l, false);
	}

	public List<Pair<AtomicBoolean, ContactInfo>> getNonHikeContacts()
	{
		Cursor c = null;

		try
		{
			c = mReadDb.rawQuery("SELECT " + DBConstants.USERS_TABLE + "." + DBConstants.MSISDN + ", " + DBConstants.USERS_TABLE + "." + DBConstants.ID + ", "
					+ DBConstants.USERS_TABLE + "." + DBConstants.NAME + ", " + DBConstants.USERS_TABLE + "." + DBConstants.ONHIKE + ", " + DBConstants.USERS_TABLE + "."
					+ DBConstants.PHONE + ", " + DBConstants.USERS_TABLE + "." + DBConstants.MSISDN_TYPE + ", " + DBConstants.USERS_TABLE + "." + DBConstants.HAS_CUSTOM_PHOTO
					+ ", " + DBConstants.USERS_TABLE + "." + DBConstants.LAST_MESSAGED + " FROM " + DBConstants.USERS_TABLE + " WHERE " + DBConstants.USERS_TABLE + "."
					+ DBConstants.MSISDN + " NOT IN (SELECT " + DBConstants.MSISDN + " FROM " + DBConstants.BLOCK_TABLE + ") AND " + DBConstants.USERS_TABLE + "."
					+ DBConstants.ONHIKE + " =0 AND " + DBConstants.USERS_TABLE + "." + DBConstants.MSISDN + " !='null'" + " ORDER BY " + DBConstants.USERS_TABLE + "."
					+ DBConstants.NAME + " COLLATE NOCASE", null);

			List<Pair<AtomicBoolean, ContactInfo>> contactInfos = new ArrayList<Pair<AtomicBoolean, ContactInfo>>(c.getCount());
			int idx = c.getColumnIndex(DBConstants.ID);
			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			int nameIdx = c.getColumnIndex(DBConstants.NAME);
			int onhikeIdx = c.getColumnIndex(DBConstants.ONHIKE);
			int phoneNumIdx = c.getColumnIndex(DBConstants.PHONE);
			int msisdnTypeIdx = c.getColumnIndex(DBConstants.MSISDN_TYPE);
			int lastMessagedIdx = c.getColumnIndex(DBConstants.LAST_MESSAGED);
			int hasCustomPhotoIdx = c.getColumnIndex(DBConstants.HAS_CUSTOM_PHOTO);
			int favoriteIdx = c.getColumnIndex(DBConstants.FAVORITE_TYPE);

			Set<String> msisdnSet = new HashSet<String>();

			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnIdx);

				if (msisdnSet.contains(msisdn))
				{
					continue;
				}
				if(ContactUtils.isIndianMobileNumber(msisdn))
				{

				msisdnSet.add(msisdn);

				ContactInfo contactInfo = new ContactInfo(c.getString(idx), msisdn, c.getString(nameIdx), c.getString(phoneNumIdx), c.getInt(onhikeIdx) != 0,
						c.getString(msisdnTypeIdx), c.getLong(lastMessagedIdx), c.getInt(hasCustomPhotoIdx) == 1);
				if (favoriteIdx != -1)
				{
					int favoriteTypeOrd = c.getInt(favoriteIdx);
					contactInfo.setFavoriteType(FavoriteType.values()[favoriteTypeOrd]);
				}
				else
				{
					contactInfo.setFavoriteType(FavoriteType.NOT_FRIEND);
				}
				contactInfos.add(new Pair<AtomicBoolean, ContactInfo>(new AtomicBoolean(false), contactInfo));
				}
			}
			return contactInfos;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public List<ContactInfo> getContacts()
	{
		return getContacts(true);
	}

	/**
	 * Returns a list of contact ordered by when they were last messaged
	 * 
	 * @param limit
	 *            The number of contacts required. -1 if limit is not to be considered
	 * @param onHike
	 *            1/0 for hike/non-hike contacts. -1 for both.
	 * @return
	 */
	public List<ContactInfo> getContactsOrderedByLastMessaged(int limit, int onHike)
	{
		String selection = DBConstants.USERS_TABLE + "." + DBConstants.MSISDN + " != 'null'" + (onHike != -1 ? " AND " + DBConstants.ONHIKE + "=" + onHike : "");

		String orderBy = DBConstants.LAST_MESSAGED + " DESC, " + DBConstants.NAME + " COLLATE NOCASE" + (limit > -1 ? " LIMIT " + limit : "");
		String[] columns = { DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE, DBConstants.PHONE, DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED,
				DBConstants.HAS_CUSTOM_PHOTO };
		Cursor c = null;
		try
		{
			c = mReadDb.query(DBConstants.USERS_TABLE, columns, selection, null, null, null, orderBy);
			List<ContactInfo> contactInfos = extractContactInfo(c, true);
			if (contactInfos.isEmpty())
			{
				return contactInfos;
			}
			return contactInfos;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public List<ContactInfo> getContactsOfFavoriteType(FavoriteType favoriteType, int onHike, String myMsisdn)
	{
		return getContactsOfFavoriteType(favoriteType, onHike, myMsisdn, false, false);
	}

	public List<ContactInfo> getContactsOfFavoriteType(FavoriteType favoriteType, int onHike, String myMsisdn, boolean nativeSMSOn)
	{
		return getContactsOfFavoriteType(favoriteType, onHike, myMsisdn, nativeSMSOn, false);
	}

	public List<ContactInfo> getContactsOfFavoriteType(FavoriteType favoriteType, int onHike, String myMsisdn, boolean nativeSMSOn, boolean ignoreUnknownContacts)
	{
		if (favoriteType == FavoriteType.NOT_FRIEND)
		{
			String toAppend = " FROM " + DBConstants.USERS_TABLE + " WHERE " + DBConstants.USERS_TABLE + "." + DBConstants.MSISDN + " NOT IN (SELECT " + DBConstants.MSISDN
					+ " FROM " + DBConstants.FAVORITES_TABLE + ") AND " + DBConstants.USERS_TABLE + "." + DBConstants.MSISDN + " != 'null' AND " + DBConstants.USERS_TABLE + "."
					+ DBConstants.MSISDN + " != " + DatabaseUtils.sqlEscapeString(myMsisdn) + " AND " + DBConstants.USERS_TABLE + "." + DBConstants.MSISDN + " NOT IN (SELECT "
					+ DBConstants.BLOCK_TABLE + "." + DBConstants.MSISDN + " FROM " + DBConstants.BLOCK_TABLE + ")";
			StringBuilder queryB = getQueryTOFetchContactInfo(toAppend, onHike, favoriteType, nativeSMSOn);

			return getContactInfo(queryB.toString(), favoriteType, ignoreUnknownContacts);
		}
		else
		{
			return getContactsOfFavoriteType(new FavoriteType[] { favoriteType }, onHike, myMsisdn, nativeSMSOn, ignoreUnknownContacts);
		}

	}

	private List<ContactInfo> getContactInfo(String query, FavoriteType favoriteType, boolean ignoreUnknownContacts)
	{
		String favoriteMsisdnColumnName = "tempMsisdn";
		Cursor c = null;
		try
		{
			c = mDb.rawQuery(query, null);

			int idx = c.getColumnIndex(DBConstants.ID);
			int userMsisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			int favoriteMsisdnIdx = c.getColumnIndex(favoriteMsisdnColumnName);
			int nameIdx = c.getColumnIndex(DBConstants.NAME);
			int onhikeIdx = c.getColumnIndex(DBConstants.ONHIKE);
			int phoneNumIdx = c.getColumnIndex(DBConstants.PHONE);
			int msisdnTypeIdx = c.getColumnIndex(DBConstants.MSISDN_TYPE);
			int lastMessagedIdx = c.getColumnIndex(DBConstants.LAST_MESSAGED);
			int hasCustomPhotoIdx = c.getColumnIndex(DBConstants.HAS_CUSTOM_PHOTO);
			int lastSeenIdx = c.getColumnIndex(DBConstants.LAST_SEEN);
			int isOfflineIdx = c.getColumnIndex(DBConstants.IS_OFFLINE);
			int inviteTimeIdx = c.getColumnIndex(DBConstants.INVITE_TIMESTAMP);
			int favoriteTypeIdx = c.getColumnIndex(DBConstants.FAVORITE_TYPE);

			Set<String> msisdnSet = null;

			msisdnSet = new HashSet<String>();

			List<ContactInfo> contactInfos = new ArrayList<ContactInfo>();
			while (c.moveToNext())
			{
				String msisdn = c.getString(favoriteType == FavoriteType.NOT_FRIEND ? userMsisdnIdx : favoriteMsisdnIdx);
				if (msisdnSet.contains(msisdn))
				{
					continue;
				}
				msisdnSet.add(msisdn);

				ContactInfo contactInfo;

				String userMsisdn = c.getString(userMsisdnIdx);

				if (TextUtils.isEmpty(userMsisdn))
				{
					if (ignoreUnknownContacts)
					{
						continue;
					}
					contactInfo = new ContactInfo(msisdn, msisdn, null, msisdn);
				}
				else
				{
					contactInfo = new ContactInfo(c.getString(idx), userMsisdn, c.getString(nameIdx), c.getString(phoneNumIdx), c.getInt(onhikeIdx) != 0,
							c.getString(msisdnTypeIdx), c.getLong(lastMessagedIdx), c.getInt(hasCustomPhotoIdx) == 1);

					contactInfo.setOffline(c.getInt(isOfflineIdx));
					contactInfo.setLastSeenTime(c.getLong(lastSeenIdx));
					contactInfo.setInviteTime(c.getLong(inviteTimeIdx));
				}

				if (favoriteType == null && favoriteTypeIdx != -1)
				{
					contactInfo.setFavoriteType(FavoriteType.values()[c.getInt(favoriteTypeIdx)]);
				}
				else
				{
					contactInfo.setFavoriteType(favoriteType);
				}

				contactInfos.add(contactInfo);
			}

			Collections.sort(contactInfos);

			return contactInfos;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public List<ContactInfo> getContactsOfFavoriteType(FavoriteType[] favoriteType, int onHike, String myMsisdn, boolean nativeSMSOn, boolean ignoreUnknownContacts)
	{
		String favoriteMsisdnColumnName = "tempMsisdn";
		StringBuilder favTypes = new StringBuilder("(");
		int total = favoriteType.length;
		if (total == 0)
		{
			return null;
		}
		for (int i = 0; i < total; i++)
		{
			favTypes.append(favoriteType[i].ordinal());
			if (i < total - 1)
			{
				favTypes.append(",");
			}
		}

		String favTypeIn = favTypes.append(")").toString();
		String toAppend = ", " + DBConstants.FAVORITE_TYPE + ", " + DBConstants.FAVORITES_TABLE + "." + DBConstants.MSISDN + " AS " + favoriteMsisdnColumnName + " FROM "
				+ DBConstants.FAVORITES_TABLE + " LEFT OUTER JOIN " + DBConstants.USERS_TABLE + " ON " + DBConstants.FAVORITES_TABLE + "." + DBConstants.MSISDN + " = "
				+ DBConstants.USERS_TABLE + "." + DBConstants.MSISDN + " WHERE " + DBConstants.FAVORITE_TYPE + " in " + favTypeIn + " AND " + favoriteMsisdnColumnName + " != "
				+ DatabaseUtils.sqlEscapeString(myMsisdn) + " AND " + favoriteMsisdnColumnName + " NOT IN (SELECT " + DBConstants.BLOCK_TABLE + "." + DBConstants.MSISDN + " FROM "
				+ DBConstants.BLOCK_TABLE + ")";
		StringBuilder queryB = getQueryTOFetchContactInfo(toAppend, onHike, null, nativeSMSOn);

		return getContactInfo(queryB.toString(), null, ignoreUnknownContacts);
	}

	private StringBuilder getQueryTOFetchContactInfo(String toAppend, int onHike, FavoriteType favoriteType, boolean nativeSMSOn)
	{
		StringBuilder queryBuilder = new StringBuilder("SELECT " + DBConstants.USERS_TABLE + "." + DBConstants.MSISDN + ", " + DBConstants.ID + ", " + DBConstants.NAME + ", "
				+ DBConstants.ONHIKE + ", " + DBConstants.PHONE + ", " + DBConstants.MSISDN_TYPE + ", " + DBConstants.HAS_CUSTOM_PHOTO + ", " + DBConstants.LAST_MESSAGED + ", "
				+ DBConstants.LAST_SEEN + ", " + DBConstants.IS_OFFLINE + ", " + DBConstants.INVITE_TIMESTAMP);
		queryBuilder.append(toAppend);
		String favoriteMsisdnColumnName = "tempMsisdn";
		if (onHike != HikeConstants.BOTH_VALUE)
		{
			queryBuilder.append(" AND " + DBConstants.ONHIKE + " = " + onHike);
			if (onHike == HikeConstants.NOT_ON_HIKE_VALUE)
			{
				queryBuilder.append(" AND ((" + DBConstants.USERS_TABLE + "." + DBConstants.MSISDN + " LIKE '+91%')");
				if (favoriteType != FavoriteType.NOT_FRIEND)
				{
					queryBuilder.append(" OR (" + favoriteMsisdnColumnName + " LIKE '+91%')");
				}
				queryBuilder.append(")");
			}
		}
		else if (!nativeSMSOn)
		{
			queryBuilder.append(" AND ((" + DBConstants.ONHIKE + " =1) OR  (" + DBConstants.USERS_TABLE + "." + DBConstants.MSISDN + " LIKE '+91%')");
			if (favoriteType != FavoriteType.NOT_FRIEND)
			{
				queryBuilder.append(" OR (" + favoriteMsisdnColumnName + " LIKE '+91%')");
			}
			queryBuilder.append(")");
		}
		return queryBuilder;
	}

	public List<Pair<AtomicBoolean, ContactInfo>> getContactsForComposeScreen(boolean freeSMSOn, boolean fwdOrgroupChat, String userMsisdn, boolean nativeSMSOn)
	{
		StringBuilder selectionBuilder = new StringBuilder(DBConstants.MSISDN + " != 'null' AND " + DBConstants.MSISDN + " != " + DatabaseUtils.sqlEscapeString(userMsisdn));

		if (!nativeSMSOn)
		{
			if (freeSMSOn)
			{
				selectionBuilder.append(" AND ((" + DBConstants.ONHIKE + " = 0 AND " + DBConstants.MSISDN + " LIKE '+91%') OR (" + DBConstants.ONHIKE + "=1))");
			}
			else
			{
				selectionBuilder.append(" AND " + DBConstants.ONHIKE + " != 0");
			}
		}

		String selection = selectionBuilder.toString();

		String[] columns = { DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE, DBConstants.PHONE, DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED,
				DBConstants.HAS_CUSTOM_PHOTO };

		Cursor c = null;
		try
		{
			c = mReadDb.query(DBConstants.USERS_TABLE, columns, selection, null, null, null, null);
			List<Pair<AtomicBoolean, ContactInfo>> contactInfos = new ArrayList<Pair<AtomicBoolean, ContactInfo>>(c.getCount());

			int idx = c.getColumnIndex(DBConstants.ID);
			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			int nameIdx = c.getColumnIndex(DBConstants.NAME);
			int onhikeIdx = c.getColumnIndex(DBConstants.ONHIKE);
			int phoneNumIdx = c.getColumnIndex(DBConstants.PHONE);
			int msisdnTypeIdx = c.getColumnIndex(DBConstants.MSISDN_TYPE);
			int lastMessagedIdx = c.getColumnIndex(DBConstants.LAST_MESSAGED);
			int hasCustomPhotoIdx = c.getColumnIndex(DBConstants.HAS_CUSTOM_PHOTO);

			Set<String> addedMsisdns = new HashSet<String>();

			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnIdx);

				if (addedMsisdns.contains(msisdn))
				{
					continue;
				}

				addedMsisdns.add(msisdn);

				ContactInfo contactInfo = new ContactInfo(c.getString(idx), msisdn, c.getString(nameIdx), c.getString(phoneNumIdx), c.getInt(onhikeIdx) != 0,
						c.getString(msisdnTypeIdx), c.getLong(lastMessagedIdx), c.getInt(hasCustomPhotoIdx) == 1);

				contactInfos.add(new Pair<AtomicBoolean, ContactInfo>(new AtomicBoolean(), contactInfo));
			}

			Collections.sort(contactInfos, new Comparator<Pair<AtomicBoolean, ContactInfo>>()
			{

				@Override
				public int compare(Pair<AtomicBoolean, ContactInfo> lhs, Pair<AtomicBoolean, ContactInfo> rhs)
				{
					ContactInfo firstContact = lhs.second;
					ContactInfo secondContact = rhs.second;
					return firstContact.compareTo(secondContact);
				}
			});
			return contactInfos;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public List<ContactInfo> getContacts(boolean ignoreEmpty)
	{
		String selection = ignoreEmpty ? DBConstants.MSISDN + " != 'null'" : null;
		Cursor c = null;
		try
		{
			c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE, DBConstants.PHONE,
					DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO }, selection, null, null, null, null);
			List<ContactInfo> contactInfos = extractContactInfo(c);
			return contactInfos;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public void deleteMultipleRows(Collection<String> ids)
	{
		String ids_joined = "(" + Utils.join(ids, ",", "\"", "\"") + ")";
		mDb.delete(DBConstants.USERS_TABLE, DBConstants.ID + " in " + ids_joined, null);
	}

	public void deleteRow(String id)
	{
		mDb.delete(DBConstants.USERS_TABLE, DBConstants.ID + "=?", new String[] { id });
	}

	public void updateContacts(List<ContactInfo> updatedContacts)
	{
		if (updatedContacts == null)
		{
			return;
		}

		ArrayList<String> ids = new ArrayList<String>(updatedContacts.size());
		for (ContactInfo c : updatedContacts)
		{
			ids.add(c.getId());
		}
		deleteMultipleRows(ids);
		try
		{
			addContacts(updatedContacts, false);
		}
		catch (DbException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public int updateHikeContact(String msisdn, boolean onhike)
	{
		Cursor c = null;
		try
		{
			String selection = DBConstants.MSISDN + "=?";
			String[] args = { msisdn };

			c = mDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.ONHIKE }, selection, args, null, null, null);

			if (!c.moveToFirst())
			{
				return 0;
			}

			boolean onHikeDB = c.getInt(c.getColumnIndex(DBConstants.ONHIKE)) == 1;

			/*
			 * DB is already updated with this value.
			 */
			if (onHikeDB == onhike)
			{
				return 0;
			}

			ContentValues vals = new ContentValues(1);
			vals.put(DBConstants.ONHIKE, onhike);
			return mDb.update(DBConstants.USERS_TABLE, vals, "msisdn=?", new String[] { msisdn });
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public void deleteAll()
	{
		mDb.delete(DBConstants.USERS_TABLE, null, null);
		mDb.delete(DBConstants.BLOCK_TABLE, null, null);
		mDb.delete(DBConstants.THUMBNAILS_TABLE, null, null);
		mDb.delete(DBConstants.FAVORITES_TABLE, null, null);
		mDb.delete(DBConstants.ROUNDED_THUMBNAIL_TABLE, null, null);
	}

	public ContactInfo getContactInfoFromPhoneNo(String number)
	{
		Cursor c = null;
		try
		{
			c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE, DBConstants.PHONE,
					DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO }, DBConstants.PHONE + "=?", new String[] { number }, null, null, null);
			List<ContactInfo> contactInfos = extractContactInfo(c);
			if (contactInfos.isEmpty())
			{
				return null;
			}

			return contactInfos.get(0);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}
	
	public FavoriteType getFriendshipStatus(String number)
	{
		Cursor favoriteCursor = null;
		try
		{
			favoriteCursor = mReadDb.query(DBConstants.FAVORITES_TABLE, new String[] { DBConstants.FAVORITE_TYPE }, DBConstants.MSISDN + " =? ", new String[] { number },
					null, null, null);
			
            FavoriteType favoriteType = FavoriteType.NOT_FRIEND;
			if (favoriteCursor.moveToFirst())
			{
				favoriteType = FavoriteType.values()[favoriteCursor.getInt(favoriteCursor.getColumnIndex(DBConstants.FAVORITE_TYPE))];
			}
			return favoriteType;
		}
		finally
		{
			if (favoriteCursor != null)
			{
				favoriteCursor.close();
			}
		}
	}

	public ContactInfo getContactInfoFromPhoneNoOrMsisdn(String number)
	{
		Cursor c = null;
		try
		{
			c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE, DBConstants.PHONE,
					DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO }, "(" + DBConstants.PHONE + "=? OR " + DBConstants.MSISDN + "=?) AND "
					+ DBConstants.MSISDN + "!='null'", new String[] { number, number }, null, null, null);
			List<ContactInfo> contactInfos = extractContactInfo(c);
			if (contactInfos.isEmpty())
			{
				return null;
			}

			return contactInfos.get(0);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public void unblock(String msisdn)
	{
		mDb.delete(DBConstants.BLOCK_TABLE, DBConstants.MSISDN + "=?", new String[] { msisdn });
	}

	public void block(String msisdn)
	{
		ContentValues values = new ContentValues();
		values.put(DBConstants.MSISDN, msisdn);
		mDb.insert(DBConstants.BLOCK_TABLE, null, values);
	}

	public boolean isBlocked(String msisdn)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.BLOCK_TABLE, null, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);
			return c.moveToFirst();
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public Set<String> getBlockedUsers()
	{
		Set<String> blocked = new HashSet<String>();
		Cursor c = null;
		try
		{
			c = mReadDb.query(DBConstants.BLOCK_TABLE, new String[] { DBConstants.MSISDN }, null, null, null, null, null);
			int idx = c.getColumnIndex(DBConstants.MSISDN);
			while (c.moveToNext())
			{
				blocked.add(c.getString(idx));
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

		return blocked;
	}

	public List<Pair<AtomicBoolean, ContactInfo>> getBlockedUserList()
	{
		String blockedMsisdnColumnName = "blk";

		String query = "SELECT " + DBConstants.USERS_TABLE + "." + DBConstants.MSISDN + ", " + DBConstants.ID + ", " + DBConstants.NAME + ", " + DBConstants.ONHIKE + ", "
				+ DBConstants.PHONE + ", " + DBConstants.MSISDN_TYPE + ", " + DBConstants.HAS_CUSTOM_PHOTO + ", " + DBConstants.LAST_MESSAGED + ", " + DBConstants.BLOCK_TABLE
				+ "." + DBConstants.MSISDN + " AS " + blockedMsisdnColumnName + " FROM " + DBConstants.BLOCK_TABLE + " LEFT OUTER JOIN " + DBConstants.USERS_TABLE + " ON "
				+ DBConstants.BLOCK_TABLE + "." + DBConstants.MSISDN + " = " + DBConstants.USERS_TABLE + "." + DBConstants.MSISDN;

		Set<String> msisdnSet = new HashSet<String>();
		List<Pair<AtomicBoolean, ContactInfo>> blockedContactList = new ArrayList<Pair<AtomicBoolean, ContactInfo>>();

		Cursor c1 = null;
		Cursor c2 = null;
		try
		{
			c1 = mDb.rawQuery(query, null);

			int idx = c1.getColumnIndex(DBConstants.ID);
			int userMsisdnIdx = c1.getColumnIndex(DBConstants.USERS_TABLE + "." + DBConstants.MSISDN);
			int msisdnIdx = c1.getColumnIndex(DBConstants.MSISDN);
			int blockedMsisdnIdx = c1.getColumnIndex(blockedMsisdnColumnName);
			int nameIdx = c1.getColumnIndex(DBConstants.NAME);
			int onhikeIdx = c1.getColumnIndex(DBConstants.ONHIKE);
			int phoneNumIdx = c1.getColumnIndex(DBConstants.PHONE);
			int msisdnTypeIdx = c1.getColumnIndex(DBConstants.MSISDN_TYPE);
			int lastMessagedIdx = c1.getColumnIndex(DBConstants.LAST_MESSAGED);
			int hasCustomPhotoIdx = c1.getColumnIndex(DBConstants.HAS_CUSTOM_PHOTO);

			while (c1.moveToNext())
			{
				String blockedMsisdn = c1.getString(blockedMsisdnIdx);
				String userMsisdn = c1.getString(userMsisdnIdx);

				String msisdn = TextUtils.isEmpty(blockedMsisdn) ? userMsisdn : blockedMsisdn;

				if (msisdnSet.contains(msisdn))
				{
					continue;
				}
				msisdnSet.add(msisdn);

				ContactInfo contactInfo;

				if (TextUtils.isEmpty(userMsisdn))
				{
					contactInfo = new ContactInfo(msisdn, msisdn, msisdn, msisdn);
				}
				else
				{
					contactInfo = new ContactInfo(c1.getString(idx), userMsisdn, c1.getString(nameIdx), c1.getString(phoneNumIdx), c1.getInt(onhikeIdx) != 0,
							c1.getString(msisdnTypeIdx), c1.getLong(lastMessagedIdx), c1.getInt(hasCustomPhotoIdx) == 1);
				}

				blockedContactList.add(new Pair<AtomicBoolean, ContactInfo>(new AtomicBoolean(true), contactInfo));
			}

			String selection = DBConstants.MSISDN + " != 'null'";
			c2 = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE, DBConstants.PHONE,
					DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO }, selection, null, null, null, null);

			while (c2.moveToNext())
			{
				String msisdn = c2.getString(msisdnIdx);
				if (msisdnSet.contains(msisdn))
				{
					continue;
				}
				msisdnSet.add(msisdn);
				ContactInfo contactInfo = new ContactInfo(c2.getString(idx), c2.getString(msisdnIdx), c2.getString(nameIdx), c2.getString(phoneNumIdx), c2.getInt(onhikeIdx) != 0,
						c2.getString(msisdnTypeIdx), c2.getLong(lastMessagedIdx), c2.getInt(hasCustomPhotoIdx) == 1);
				blockedContactList.add(new Pair<AtomicBoolean, ContactInfo>(new AtomicBoolean(false), contactInfo));
			}

			Collections.sort(blockedContactList, new Comparator<Pair<AtomicBoolean, ContactInfo>>()
			{

				@Override
				public int compare(Pair<AtomicBoolean, ContactInfo> lhs, Pair<AtomicBoolean, ContactInfo> rhs)
				{
					boolean lhsBlocked = lhs.first.get();
					boolean rhsBlocked = rhs.first.get();
					if (lhsBlocked && !rhsBlocked)
					{
						return -1;
					}
					else if (rhsBlocked && !lhsBlocked)
					{
						return 1;
					}
					else
					{
						return lhs.second.compareTo(rhs.second);
					}
				}
			});
			return blockedContactList;
		}
		finally
		{
			if (c1 != null)
			{
				c1.close();
			}
			if (c2 != null)
			{
				c2.close();
			}
		}
	}

	public void setIcon(String msisdn, byte[] data, boolean isProfileImage)
	{
		if (!isProfileImage)
		{
			/*
			 * We delete the older file that contained the larger avatar image for this msisdn.
			 */
			Utils.removeLargerProfileImageForMsisdn(msisdn);

			byte[] roundedData = BitmapUtils.getRoundedBitmapBytes(data);

			insertRoundedThumbnailData(mDb, msisdn, roundedData);
		}
		// IconCacheManager.getInstance().clearIconForMSISDN(msisdn);
		HikeMessengerApp.getLruCache().remove(msisdn);
		ContentValues vals = new ContentValues(2);
		vals.put(DBConstants.MSISDN, msisdn);
		vals.put(DBConstants.IMAGE, data);
		mDb.replace(DBConstants.THUMBNAILS_TABLE, null, vals);

		String whereClause = DBConstants.MSISDN + "=?"; // msisdn;
		ContentValues customPhotoFlag = new ContentValues(1);
		customPhotoFlag.put(DBConstants.HAS_CUSTOM_PHOTO, 1);
		mDb.update(DBConstants.USERS_TABLE, customPhotoFlag, whereClause, new String[] { msisdn });
	}

	public Drawable getIcon(String msisdn, boolean rounded)
	{
		Cursor c = null;
		try
		{
			String table = rounded ? DBConstants.ROUNDED_THUMBNAIL_TABLE : DBConstants.THUMBNAILS_TABLE;
			c = mDb.query(table, new String[] { DBConstants.IMAGE }, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);

			if (!c.moveToFirst())
			{
				/* lookup based on this msisdn */
				return null;
			}
			byte[] icondata = c.getBlob(c.getColumnIndex(DBConstants.IMAGE));

			return HikeBitmapFactory.getBitmapDrawable(mContext.getResources(), HikeBitmapFactory.decodeByteArray(icondata, 0, icondata.length));
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public byte[] getIconByteArray(String msisdn, boolean rounded)
	{
		Cursor c = null;
		try
		{
			String table = rounded ? DBConstants.ROUNDED_THUMBNAIL_TABLE : DBConstants.THUMBNAILS_TABLE;
			c = mDb.query(table, new String[] { DBConstants.IMAGE }, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);

			if (!c.moveToFirst())
			{
				/* lookup based on this msisdn */
				return null;
			}
			byte[] icondata = c.getBlob(c.getColumnIndex(DBConstants.IMAGE));
			return icondata;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public String getIconIdentifierString(String msisdn)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.THUMBNAILS_TABLE, new String[] { DBConstants.IMAGE }, "msisdn=?", new String[] { msisdn }, null, null, null);

			if (!c.moveToFirst())
			{
				/* lookup based on this msisdn */
				return null;
			}

			byte[] icondata = c.getBlob(c.getColumnIndex(DBConstants.IMAGE));
			String iconString = Base64.encodeToString(icondata, Base64.DEFAULT);

			if (iconString.length() < 6)
			{
				return iconString;
			}
			else
			{
				return iconString.substring(0, 5) + iconString.substring(iconString.length() - 6);
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public boolean removeIcon(String msisdn)
	{
		/*
		 * We delete the older file that contained the larger avatar image for this msisdn.
		 */
		Utils.removeLargerProfileImageForMsisdn(msisdn);

		int deletedRows = mDb.delete(DBConstants.THUMBNAILS_TABLE, DBConstants.MSISDN + "=?", new String[] { msisdn });

		int deletedRowsFromRoundedTable = mDb.delete(DBConstants.ROUNDED_THUMBNAIL_TABLE, DBConstants.MSISDN + "=?", new String[] { msisdn });

		String whereClause = DBConstants.MSISDN + "=?"; // msisdn;
		ContentValues customPhotoFlag = new ContentValues(1);
		customPhotoFlag.put(DBConstants.HAS_CUSTOM_PHOTO, 0);
		mDb.update(DBConstants.USERS_TABLE, customPhotoFlag, whereClause, new String[] { msisdn });
		if (deletedRows + deletedRowsFromRoundedTable > 0)
		{
			return true;
		}
		else
		{
			return false;
		}
		
	}

	public void updateContactRecency(String msisdn, long timeStamp)
	{
		ContentValues updatedTime = new ContentValues(1);
		updatedTime.put(DBConstants.LAST_MESSAGED, timeStamp);

		String whereClause = DBConstants.MSISDN + "=?";
		int rows = mDb.update(DBConstants.USERS_TABLE, updatedTime, whereClause, new String[] { msisdn });
		Logger.d(getClass().getSimpleName(), "Row has been updated: " + rows);
	}

	public void syncContactExtraInfo()
	{
		Cursor extraInfo = null;

		try
		{
			mDb.beginTransaction();
			extraInfo = this.mContext.getContentResolver().query(Phone.CONTENT_URI, new String[] { Phone.NUMBER, Phone.TYPE, Phone.LABEL }, null, null, null);

			int msisdnIdx = extraInfo.getColumnIndex(Phone.NUMBER);
			int typeIdx = extraInfo.getColumnIndex(Phone.TYPE);
			int labelIdx = extraInfo.getColumnIndex(Phone.LABEL);

			while (extraInfo.moveToNext())
			{
				String msisdnType = Phone.getTypeLabel(this.mContext.getResources(), extraInfo.getInt(typeIdx), extraInfo.getString(labelIdx)).toString();
				ContentValues contentValues = new ContentValues(1);
				contentValues.put(DBConstants.MSISDN_TYPE, msisdnType);
				String whereClause = DBConstants.PHONE + " =? ";
				mDb.update(DBConstants.USERS_TABLE, contentValues, whereClause, new String[] { extraInfo.getString(msisdnIdx) });
			}
			mDb.setTransactionSuccessful();
			Editor editor = mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
			editor.putBoolean(HikeMessengerApp.CONTACT_EXTRA_INFO_SYNCED, true);
			editor.commit();
		}
		finally
		{
			if (extraInfo != null)
			{
				extraInfo.close();
			}
			mDb.endTransaction();
		}
	}

	public void toggleContactFavorite(String msisdn, FavoriteType favoriteType)
	{
		/*
		 * If we are setting the type as not favorite, we'll remove the row itself.
		 */
		if (favoriteType == FavoriteType.NOT_FRIEND)
		{
			mDb.delete(DBConstants.FAVORITES_TABLE, DBConstants.MSISDN + "=?", new String[] { msisdn });
			return;
		}

		SQLiteStatement insertStatement = null;
		InsertHelper ih = null;
		try
		{
			mDb.beginTransaction();
			ih = new InsertHelper(mDb, DBConstants.FAVORITES_TABLE);
			insertStatement = mDb.compileStatement("INSERT OR REPLACE INTO " + DBConstants.FAVORITES_TABLE + " ( " + DBConstants.MSISDN + ", " + DBConstants.FAVORITE_TYPE + " ) "
					+ " VALUES (?, ?)");
			insertStatement.bindString(ih.getColumnIndex(DBConstants.MSISDN), msisdn);
			insertStatement.bindLong(ih.getColumnIndex(DBConstants.FAVORITE_TYPE), favoriteType.ordinal());

			insertStatement.executeInsert();
			mDb.setTransactionSuccessful();
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
			mDb.endTransaction();
		}
	}

	public boolean isContactFavorite(String msisdn)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.FAVORITES_TABLE, new String[] { DBConstants.MSISDN },
					DBConstants.MSISDN + "=? AND " + DBConstants.FAVORITE_TYPE + "=" + FavoriteType.FRIEND.ordinal(), new String[] { msisdn }, null, null, null);
			return c.moveToFirst();
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public List<ContactInfo> getRecentContactsFromListOfNumbers(String selectionNumbers, Map<String, Long> recentValues, boolean indiaOnly, FavoriteType favoriteType,
			int freeSmsSetting, String myMsisdn)
	{

		String[] columns = new String[] { DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE, DBConstants.PHONE, DBConstants.MSISDN_TYPE,
				DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO };

		StringBuilder selectionBuilder = new StringBuilder();

		selectionBuilder.append(DBConstants.PHONE + " IN " + selectionNumbers);
		selectionBuilder.append(indiaOnly ? " AND " + DBConstants.MSISDN + " LIKE '+91%'" : "");
		selectionBuilder.append(favoriteType != null ? " AND " + DBConstants.MSISDN + " NOT IN (SELECT " + DBConstants.MSISDN + " FROM " + DBConstants.FAVORITES_TABLE + ")" : "");
		selectionBuilder.append(" AND " + DBConstants.MSISDN + "!='null'");

		selectionBuilder.append(TextUtils.isEmpty(myMsisdn) ? "" : " AND " + DBConstants.MSISDN + "!=" + DatabaseUtils.sqlEscapeString(myMsisdn));

		if (freeSmsSetting == -1)
		{
			selectionBuilder.append(" AND " + DBConstants.ONHIKE + "=0");
		}
		else if (freeSmsSetting == 0)
		{
			selectionBuilder.append(" AND " + DBConstants.ONHIKE + " =1");
		}
		else if (freeSmsSetting == 1)
		{
			selectionBuilder.append(" AND ((" + DBConstants.ONHIKE + " =1) OR (" + DBConstants.ONHIKE + " =0 AND " + DBConstants.MSISDN + " LIKE '+91%'))");
		}

		String selection = selectionBuilder.toString();

		Cursor c = null;
		try
		{
			c = mReadDb.query(DBConstants.USERS_TABLE, columns, selection, null, null, null, null);
			int idx = c.getColumnIndex(DBConstants.ID);
			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			int nameIdx = c.getColumnIndex(DBConstants.NAME);
			int onhikeIdx = c.getColumnIndex(DBConstants.ONHIKE);
			int phoneNumIdx = c.getColumnIndex(DBConstants.PHONE);
			int msisdnTypeIdx = c.getColumnIndex(DBConstants.MSISDN_TYPE);
			int lastMessagedIdx = c.getColumnIndex(DBConstants.LAST_MESSAGED);
			int hasCustomPhotoIdx = c.getColumnIndex(DBConstants.HAS_CUSTOM_PHOTO);

			List<ContactInfo> contactList = new ArrayList<ContactInfo>();

			Set<String> nameSet = new HashSet<String>();

			while (c.moveToNext())
			{
				String number = c.getString(phoneNumIdx);
				String name = c.getString(nameIdx);

				if (nameSet.contains(name))
				{
					continue;
				}

				nameSet.add(name);

				/*
				 * All our timestamps are in seconds.
				 */
				Long lastMessagedDB = recentValues.get(number) / 1000;
				long lastMessagedCurrent = c.getLong(lastMessagedIdx);

				if ((lastMessagedDB != null) && (lastMessagedDB > lastMessagedCurrent))
				{
					lastMessagedCurrent = lastMessagedDB;
				}

				ContactInfo contactInfo = new ContactInfo(c.getString(idx), c.getString(msisdnIdx), name, c.getString(phoneNumIdx), c.getInt(onhikeIdx) != 0,
						c.getString(msisdnTypeIdx), lastMessagedCurrent, c.getInt(hasCustomPhotoIdx) == 1);
				contactInfo.setFavoriteType(favoriteType);
				contactList.add(contactInfo);
			}

			Collections.sort(contactList, new Comparator<ContactInfo>()
			{
				@Override
				public int compare(ContactInfo lhs, ContactInfo rhs)
				{
					if (lhs.getLastMessaged() != rhs.getLastMessaged())
					{
						return -((Long) lhs.getLastMessaged()).compareTo(rhs.getLastMessaged());
					}
					return lhs.getName().toLowerCase().compareTo(rhs.getName().toLowerCase());
				}
			});

			return contactList;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public List<ContactInfo> getNonHikeRecentContacts(int limit, boolean indiaOnly, FavoriteType favoriteType)
	{
		Pair<String, Map<String, Long>> data = ContactUtils.getRecentNumbers(mContext, limit);
		return getRecentContactsFromListOfNumbers(data.first, data.second, indiaOnly, favoriteType, -1, null);
	}

	public List<ContactInfo> getRecentContacts(int limit, boolean indiaOnly, FavoriteType favoriteType, int freeSmsSetting, String myMsisdn)
	{
		Pair<String, Map<String, Long>> data = ContactUtils.getRecentNumbers(mContext, limit);
		return getRecentContactsFromListOfNumbers(data.first, data.second, indiaOnly, favoriteType, freeSmsSetting, myMsisdn);
	}

	private List<ContactInfo> getNonHikeMostContactedContactsFromListOfNumbers(String selectionNumbers, final Map<String, Integer> mostContactedValues, int limit)
	{

		String[] columns = new String[] { DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE, DBConstants.PHONE, DBConstants.MSISDN_TYPE,
				DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO, DBConstants.FAVORITE_TYPE_SELECTION };

		String selection = DBConstants.PHONE + " IN " + selectionNumbers + " AND " + DBConstants.MSISDN + "!='null' AND " + DBConstants.ONHIKE + "=0 LIMIT " + limit;

		Cursor c = null;
		try
		{
			c = mReadDb.query(DBConstants.USERS_TABLE, columns, selection, null, null, null, null);

			int idx = c.getColumnIndex(DBConstants.ID);
			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			int nameIdx = c.getColumnIndex(DBConstants.NAME);
			int onhikeIdx = c.getColumnIndex(DBConstants.ONHIKE);
			int phoneNumIdx = c.getColumnIndex(DBConstants.PHONE);
			int msisdnTypeIdx = c.getColumnIndex(DBConstants.MSISDN_TYPE);
			int lastMessagedIdx = c.getColumnIndex(DBConstants.LAST_MESSAGED);
			int hasCustomPhotoIdx = c.getColumnIndex(DBConstants.HAS_CUSTOM_PHOTO);
			int favoriteIdx = c.getColumnIndex(DBConstants.FAVORITE_TYPE);

			List<ContactInfo> contactList = new ArrayList<ContactInfo>();

			Set<String> msisdnSet = new HashSet<String>();
			Set<String> nameSet = new HashSet<String>();

			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnIdx);
				String name = c.getString(nameIdx);

				if (msisdnSet.contains(msisdn))
				{
					continue;
				}
				if (nameSet.contains(name))
				{
					continue;
				}

				msisdnSet.add(msisdn);
				nameSet.add(name);

				/*
				 * All our timestamps are in seconds.
				 */
				long lastMessagedCurrent = c.getLong(lastMessagedIdx);

				ContactInfo contactInfo = new ContactInfo(c.getString(idx), msisdn, name, c.getString(phoneNumIdx), c.getInt(onhikeIdx) != 0, c.getString(msisdnTypeIdx),
						lastMessagedCurrent, c.getInt(hasCustomPhotoIdx) == 1);
				contactInfo.setFavoriteType(FavoriteType.values()[c.getInt(favoriteIdx)]);
				contactList.add(contactInfo);
			}

			Collections.sort(contactList, new Comparator<ContactInfo>()
			{
				@Override
				public int compare(ContactInfo lhs, ContactInfo rhs)
				{
					int lhsContactNum = mostContactedValues.get(lhs.getPhoneNum());
					int rhsContactNum = mostContactedValues.get(rhs.getPhoneNum());

					if (lhsContactNum != rhsContactNum)
					{
						return -((Integer) lhsContactNum).compareTo(rhsContactNum);
					}
					return lhs.getName().toLowerCase().compareTo(rhs.getName().toLowerCase());
				}
			});

			return contactList;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public List<ContactInfo> getNonHikeMostContactedContacts(int limit)
	{
		/*
		 * Sending twice the limit to account for the contacts that might be on hike
		 */
		Pair<String, Map<String, Integer>> data = ContactUtils.getMostContactedContacts(mContext, limit * 2);
		return getNonHikeMostContactedContactsFromListOfNumbers(data.first, data.second, limit);
	}

	public List<ContactInfo> getContactNamesFromMsisdnList(String msisdns)
	{
		Cursor c = null;
		try
		{
			c = mReadDb.rawQuery("SELECT max(" + DBConstants.ID + ") AS " + DBConstants.ID + ", " + DBConstants.NAME + ", " + DBConstants.MSISDN + ", " + DBConstants.ONHIKE + ", "
					+ DBConstants.HAS_CUSTOM_PHOTO + " from " + DBConstants.USERS_TABLE + " WHERE " + DBConstants.MSISDN + " IN " + msisdns + " GROUP BY " + DBConstants.MSISDN,
					null);

			List<ContactInfo> contactList = new ArrayList<ContactInfo>();

			final int nameIdx = c.getColumnIndex(DBConstants.NAME);
			final int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			final int onHikeIdx = c.getColumnIndex(DBConstants.ONHIKE);
			final int hasCustomIconIdx = c.getColumnIndex(DBConstants.HAS_CUSTOM_PHOTO);

			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnIdx);

				/*
				 * query with aggregate functions always return at least one row which will have everything null.
				 */
				if (TextUtils.isEmpty(msisdn))
				{
					continue;
				}

				String name = c.getString(nameIdx);
				boolean onHike = c.getInt(onHikeIdx) != 0;

				ContactInfo contactInfo = new ContactInfo(null, msisdn, name, null, onHike);
				contactInfo.setHasCustomPhoto(c.getInt(hasCustomIconIdx) == 1);

				contactList.add(contactInfo);
			}
			return contactList;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public void setMultipleContactsToFavorites(JSONObject favorites)
	{
		SQLiteStatement insertStatement = null;
		InsertHelper ih = null;
		try
		{
			ih = new InsertHelper(mDb, DBConstants.FAVORITES_TABLE);
			insertStatement = mDb.compileStatement("INSERT OR REPLACE INTO " + DBConstants.FAVORITES_TABLE + " ( " + DBConstants.MSISDN + ", " + DBConstants.FAVORITE_TYPE + " ) "
					+ " VALUES (?, ?)");
			mDb.beginTransaction();

			JSONArray msisdns = favorites.names();
			if (msisdns == null)
			{
				return;
			}
			for (int i = 0; i < msisdns.length(); i++)
			{
				String msisdn = msisdns.optString(i);
				JSONObject msisdnInfo = favorites.optJSONObject(msisdn);

				FavoriteType favoriteType;
				if (msisdnInfo.has(HikeConstants.PENDING))
				{
					boolean pending = msisdnInfo.optBoolean(HikeConstants.PENDING);
					favoriteType = pending ? FavoriteType.REQUEST_RECEIVED : FavoriteType.REQUEST_RECEIVED_REJECTED;
				}
				else if (msisdnInfo.has(HikeConstants.REQUEST_PENDING))
				{
					boolean requestPending = msisdnInfo.optBoolean(HikeConstants.REQUEST_PENDING);
					favoriteType = requestPending ? FavoriteType.REQUEST_SENT : FavoriteType.REQUEST_SENT_REJECTED;
				}
				else
				{
					favoriteType = FavoriteType.FRIEND;
				}

				insertStatement.bindString(ih.getColumnIndex(DBConstants.MSISDN), msisdn);
				insertStatement.bindLong(ih.getColumnIndex(DBConstants.FAVORITE_TYPE), favoriteType.ordinal());

				insertStatement.executeInsert();
			}
			mDb.setTransactionSuccessful();
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
			mDb.endTransaction();

			if (favorites.length() > 0)
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.REFRESH_FAVORITES, null);
			}
		}
	}

	public boolean hasIcon(String msisdn)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.THUMBNAILS_TABLE, new String[] { DBConstants.MSISDN }, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);

			return c.moveToFirst();
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public int getPendingFriendRequestCount()
	{
		return (int) DatabaseUtils.longForQuery(mDb, "SELECT COUNT(*) FROM " + DBConstants.FAVORITES_TABLE + " WHERE " + DBConstants.FAVORITE_TYPE + "="
				+ FavoriteType.REQUEST_RECEIVED.ordinal(), null);
	}

	public boolean doesContactExist(String msisdn)
	{
		String[] columns = new String[] { DBConstants.NAME };
		String selection = DBConstants.MSISDN + "=? ";
		Cursor c = null;

		try
		{
			c = mReadDb.query(DBConstants.USERS_TABLE, columns, selection, new String[] { msisdn }, null, null, null);
			return c.moveToFirst();
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public int getHikeContactCount(String myMsisdn)
	{
		String selection = DBConstants.ONHIKE + " = 1 AND "+DBConstants.MSISDN + "!=" + DatabaseUtils.sqlEscapeString(myMsisdn);
		Cursor c = null;
		try
		{
			c = mReadDb.query(true, DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN }, selection, null, null, null, null, null);

			return c.getCount();
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}
	
	public int getNonHikeContactsCount()
	{
		String selection = DBConstants.ONHIKE + " = 0";
		Cursor c = null;
		try
		{
			c = mReadDb.query(true, DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN }, selection, null, null, null, null, null);

			return c.getCount();
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}
	
	

	public void setHikeJoinTime(String msisdn, long hikeJoinTime)
	{
		String whereClause = DBConstants.MSISDN + "=?";
		String[] whereArgs = new String[] { msisdn };

		ContentValues values = new ContentValues(1);
		values.put(DBConstants.HIKE_JOIN_TIME, hikeJoinTime);

		mDb.update(DBConstants.USERS_TABLE, values, whereClause, whereArgs);
	}

	public int getFriendTableRowCount()
	{
		return (int) DatabaseUtils.longForQuery(mDb, "SELECT COUNT(*) FROM " + DBConstants.FAVORITES_TABLE, null);
	}

	public void updateLastSeenTime(String msisdn, long lastSeenTime)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.LAST_SEEN, lastSeenTime);

		mDb.update(DBConstants.USERS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { msisdn });
	}

	public void updateIsOffline(String msisdn, int offline)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.IS_OFFLINE, offline);

		mDb.update(DBConstants.USERS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { msisdn });
	}

	private void makeOlderAvatarsRounded(SQLiteDatabase db)
	{
		Cursor c = null;
		try
		{
			c = db.query(DBConstants.THUMBNAILS_TABLE, null, null, null, null, null, null);

			int thumbnailIdx = c.getColumnIndex(DBConstants.IMAGE);
			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);

			while (c.moveToNext())
			{
				byte[] data = c.getBlob(thumbnailIdx);
				String msisdn = c.getString(msisdnIdx);

				/*
				 * An msisdn starts with a '+' and a group conversation contains a ':'. Rest are profile pic updates.
				 */
				if (!msisdn.startsWith("+") && !msisdn.contains(":"))
				{
					continue;
				}

				byte[] roundedData = BitmapUtils.getRoundedBitmapBytes(data);

				insertRoundedThumbnailData(db, msisdn, roundedData);
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	private void insertRoundedThumbnailData(SQLiteDatabase db, String msisdn, byte[] data)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.MSISDN, msisdn);
		contentValues.put(DBConstants.IMAGE, data);

		db.replace(DBConstants.ROUNDED_THUMBNAIL_TABLE, null, contentValues);
	}

	private String getQueryableNumbersString(List<ContactInfo> contactInfos)
	{
		if (contactInfos.isEmpty())
		{
			return null;
		}
		StringBuilder sb = new StringBuilder("(");
		for (ContactInfo contactInfo : contactInfos)
		{
			sb.append(DatabaseUtils.sqlEscapeString(contactInfo.getMsisdn()) + ",");
		}
		sb.replace(sb.lastIndexOf(","), sb.length(), ")");

		return sb.toString();
	}

	public FtueContactsData getFTUEContacts(SharedPreferences preferences)
	{
		FtueContactsData ftueContactsData = new FtueContactsData();
		
		int limit = HikeConstants.FTUE_LIMIT;

		String myMsisdn = preferences.getString(HikeMessengerApp.MSISDN_SETTING, "");
		
		ftueContactsData.setTotalHikeContactsCount(getHikeContactCount(myMsisdn));
		
		/*
		 * adding server recommended contacts to ftue contacts list;
		 */
		String recommendedContactsSelection = Utils.getServerRecommendedContactsSelection(preferences.getString(HikeMessengerApp.SERVER_RECOMMENDED_CONTACTS, null), myMsisdn);
		Logger.d("getFTUEContacts","recommendedContactsSelection = "+recommendedContactsSelection);
		if (!TextUtils.isEmpty(recommendedContactsSelection))
		{
			List<ContactInfo> recommendedContacts = getHikeContacts(limit*2, recommendedContactsSelection, null, myMsisdn);
			if (recommendedContacts.size() >= limit)
			{
				ftueContactsData.getHikeContacts().addAll(recommendedContacts.subList(0, limit));
				return ftueContactsData;
			}
			else
			{
				ftueContactsData.getHikeContacts().addAll(recommendedContacts);
			}
		}
		
		limit = HikeConstants.FTUE_LIMIT - ftueContactsData.getHikeContacts().size();
		// added server recommended contacts
		
		/*
		 * adding favorites if required;
		 */
		if(limit > 0)
		{
			List<ContactInfo> friendList = getContactsOfFavoriteType(FavoriteType.FRIEND, HikeConstants.ON_HIKE_VALUE, myMsisdn);
			for (ContactInfo contactInfo : friendList)
			{
				if(!Utils.isListContainsMsisdn(ftueContactsData.getHikeContacts(), contactInfo.getMsisdn()))
				{
					ftueContactsData.getHikeContacts().add(contactInfo);
					limit--;
					
					if(limit < 1)
					{
						return ftueContactsData;
					}
				}
			}
			
		}
		else
		{
			return ftueContactsData;
		}
		
		// added favorites contacts
		
		/*
		 * adding random hike contacts if required;
		 */
		if(limit > 0)
		{
			String currentSelection = getQueryableNumbersString(ftueContactsData.getHikeContacts());
			List<ContactInfo> hikeContacts = getHikeContacts(limit * 2, null, currentSelection, myMsisdn);
			if (hikeContacts.size() >= limit)
			{
				ftueContactsData.getHikeContacts().addAll(hikeContacts.subList(0, limit));
				return ftueContactsData;
			}
			else
			{
				ftueContactsData.getHikeContacts().addAll(hikeContacts);
			}

		}
		else
		{
			return ftueContactsData;
		}
		limit = HikeConstants.FTUE_LIMIT - ftueContactsData.getHikeContacts().size();
		
		// added random hike contacts
		
		/*
		 * adding most contacted sms contacts if required;
		 */
		if(limit > 0)
		{
			List<ContactInfo> nonHikeContacts = getNonHikeMostContactedContacts(limit*4);
			ftueContactsData.setTotalSmsContactsCount(getNonHikeContactsCount());

			if (nonHikeContacts.size() >= limit)
			{
				ftueContactsData.getSmsContacts().addAll(nonHikeContacts.subList(0, limit));
			}
			else
			{
				ftueContactsData.getSmsContacts().addAll(nonHikeContacts);
			}
		}

		return ftueContactsData;
	}

	public void updateInvitedTimestamp(String msisdn, long timestamp)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.INVITE_TIMESTAMP, timestamp);

		mDb.update(DBConstants.USERS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { msisdn });
	}

	public ContactInfo getMostRecentContact(int hikeState)
	{
		String selection;
		switch (hikeState)
		{
		case HikeConstants.ON_HIKE_VALUE:
			selection = DBConstants.ONHIKE + "=1";
			break;
		case HikeConstants.NOT_ON_HIKE_VALUE:
			selection = DBConstants.ONHIKE + "=0 AND " + DBConstants.MSISDN + " LIKE '+91%'";
			break;
		default:
			selection = null;
			break;
		}
		selection += (selection == null ? "" : " AND ") + DBConstants.MSISDN + " NOT IN (SELECT " + DBConstants.BLOCK_TABLE + "." + DBConstants.MSISDN + " FROM "
				+ DBConstants.BLOCK_TABLE + ")";

		Cursor c = null;
		try
		{
			c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE, DBConstants.PHONE,
					DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO, DBConstants.FAVORITE_TYPE_SELECTION, DBConstants.HIKE_JOIN_TIME,
					DBConstants.IS_OFFLINE, DBConstants.LAST_SEEN }, selection, null, null, null, DBConstants.LAST_MESSAGED + " DESC LIMIT 1");
			if (c.getCount() != 0)
			{
				ContactInfo ci = extractContactInfo(c).get(0);
				return ci;
			}
			return null;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public ContactInfo getChatThemeFTUEContact(Context context, boolean newUser)
	{
		ContactInfo contactInfo;
		if (newUser)
		{
			/*
			 * For new users, we first try to get a recommended hike contact.
			 */
			String recommendedContactsString = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.SERVER_RECOMMENDED_CONTACTS, null);
			try
			{
				JSONArray recommendedContactsArray = new JSONArray(recommendedContactsString);
				if (recommendedContactsArray.length() != 0)
				{
					for (int i = 0; i < recommendedContactsArray.length(); i++)
					{
						String msisdn = recommendedContactsArray.getString(i);

						if (isBlocked(msisdn))
						{
							continue;
						}

						contactInfo = getContactInfoFromMSISDN(msisdn, false);

						if (contactInfo != null)
						{
							return contactInfo;
						}
					}
				}
			}
			catch (JSONException e)
			{
			}

			/*
			 * If we didn't find any, we pick a hike contact
			 */
			contactInfo = getMostRecentContact(HikeConstants.ON_HIKE_VALUE);
			if (contactInfo != null)
			{
				return contactInfo;
			}

			/*
			 * If we didn't find any there as well, we pick an SMS contact that is most contacted by the user.
			 */
			List<ContactInfo> contactList = getNonHikeMostContactedContacts(1);
			if (contactList.isEmpty())
			{
				contactInfo = null;
			}
			else
			{
				contactInfo = contactList.get(0);
			}
		}
		else
		{
			/*
			 * For an existing user, we first try to pick his last contacted hike contact.
			 */
			contactInfo = getMostRecentContact(HikeConstants.ON_HIKE_VALUE);
			if (contactInfo != null)
			{
				return contactInfo;
			}

			/*
			 * Else we fetch his last contacted SMS contact.
			 */
			contactInfo = getMostRecentContact(HikeConstants.NOT_ON_HIKE_VALUE);
		}
		return contactInfo;
	}

	public List<ContactInfo> fetchAllContacts(String myMsisdn)
	{
		Cursor c = null;
		List<ContactInfo> contactInfos = null;
		try
		{
			c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, "max(" + DBConstants.ID + ") as " + DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE, DBConstants.PHONE,
					DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO },
					DBConstants.MSISDN + " != ?", new String[] { myMsisdn }, DBConstants.MSISDN, null, DBConstants.NAME + " COLLATE NOCASE");

			contactInfos = extractContactInfo(c, true);

			return contactInfos;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public Map<String, FavoriteType> fetchFavoriteTypeMap()
	{
		Cursor c = null;
		Map<String, FavoriteType> favoriteTypeMap = new HashMap<String, ContactInfo.FavoriteType>();

		try
		{
			c = mReadDb.query(DBConstants.FAVORITES_TABLE, new String[] { DBConstants.MSISDN, DBConstants.FAVORITE_TYPE }, null, null, null, null, null);

			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			int favTypeIdx = c.getColumnIndex(DBConstants.FAVORITE_TYPE);

			while (c.moveToNext())
			{
				favoriteTypeMap.put(c.getString(msisdnIdx), FavoriteType.values()[c.getInt(favTypeIdx)]);
			}

			return favoriteTypeMap;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public Set<String> getBlockedMsisdnSet()
	{
		Cursor c = null;
		Set<String> blockedSet = new HashSet<String>();

		try
		{
			c = mReadDb.query(DBConstants.BLOCK_TABLE, new String[] { DBConstants.MSISDN }, null, null, null, null, null);

			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);

			while (c.moveToNext())
			{
				blockedSet.add(c.getString(msisdnIdx));
			}

			return blockedSet;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}
}
