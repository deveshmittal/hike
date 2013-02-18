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
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.utils.ContactUtils;
import com.bsb.hike.utils.Utils;

public class HikeUserDatabase extends SQLiteOpenHelper {
	private SQLiteDatabase mDb;

	private SQLiteDatabase mReadDb;

	private Context mContext;

	private static HikeUserDatabase hikeUserDatabase;

	public static void init(Context context) {
		if (hikeUserDatabase == null) {
			hikeUserDatabase = new HikeUserDatabase(context);
		}
	}

	public static HikeUserDatabase getInstance() {
		return hikeUserDatabase;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String create = "CREATE TABLE IF NOT EXISTS " + DBConstants.USERS_TABLE
				+ " ( " + DBConstants.ID + " STRING , " + DBConstants.NAME
				+ " TEXT, " + DBConstants.MSISDN + " TEXT COLLATE nocase, "
				+ DBConstants.ONHIKE + " INTEGER, " + DBConstants.PHONE
				+ " TEXT, " + DBConstants.HAS_CUSTOM_PHOTO + " INTEGER, "
				+ DBConstants.OVERLAY_DISMISSED + " INTEGER, "
				+ DBConstants.MSISDN_TYPE + " STRING, "
				+ DBConstants.LAST_MESSAGED + " INTEGER" + " )";

		db.execSQL(create);

		create = "CREATE TABLE IF NOT EXISTS " + DBConstants.BLOCK_TABLE
				+ " ( " + DBConstants.MSISDN + " TEXT " + " ) ";
		db.execSQL(create);

		create = "CREATE TABLE IF NOT EXISTS " + DBConstants.THUMBNAILS_TABLE
				+ " ( " + DBConstants.MSISDN + " TEXT PRIMARY KEY, "
				+ DBConstants.IMAGE + " BLOB" + " ) ";
		db.execSQL(create);

		create = "CREATE TABLE IF NOT EXISTS " + DBConstants.FAVORITES_TABLE
				+ " ( " + DBConstants.MSISDN + " TEXT PRIMARY KEY, "
				+ DBConstants.FAVORITE_TYPE + " INTEGER" + " ) ";
		db.execSQL(create);
	}

	private HikeUserDatabase(Context context) {
		super(context, DBConstants.USERS_DATABASE_NAME, null,
				DBConstants.USERS_DATABASE_VERSION);
		mDb = getWritableDatabase();
		mReadDb = getReadableDatabase();
		this.mContext = context;
	}

	@Override
	public void close() {
		mDb.close();
		mReadDb.close();
		super.close();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(getClass().getSimpleName(), "Upgrading users table from "
				+ oldVersion + " to " + newVersion);
		if (oldVersion < 3) {
			String alter1 = "ALTER TABLE " + DBConstants.USERS_TABLE
					+ " ADD COLUMN " + DBConstants.MSISDN_TYPE + " STRING";
			String alter2 = "ALTER TABLE " + DBConstants.USERS_TABLE
					+ " ADD COLUMN " + DBConstants.LAST_MESSAGED + " INTEGER";
			db.execSQL(alter1);
			db.execSQL(alter2);
		}
		// Changing the datatype of the name column
		if (oldVersion < 4) {
			Log.d(getClass().getSimpleName(), "Updating table");
			String alter = "ALTER TABLE " + DBConstants.USERS_TABLE
					+ " RENAME TO " + "temp_table";

			String create = "CREATE TABLE IF NOT EXISTS "
					+ DBConstants.USERS_TABLE + " ( " + DBConstants.ID
					+ " STRING , " + DBConstants.NAME + " TEXT, "
					+ DBConstants.MSISDN + " TEXT COLLATE nocase, "
					+ DBConstants.ONHIKE + " INTEGER, " + DBConstants.PHONE
					+ " TEXT, " + DBConstants.HAS_CUSTOM_PHOTO + " INTEGER, "
					+ DBConstants.OVERLAY_DISMISSED + " INTEGER, "
					+ DBConstants.MSISDN_TYPE + " STRING, "
					+ DBConstants.LAST_MESSAGED + " INTEGER" + " )";

			String insert = "INSERT INTO " + DBConstants.USERS_TABLE
					+ " SELECT * FROM temp_table";

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
		if (oldVersion < 7) {
			// Create the favorites table.
			onCreate(db);

			String tempTable = "tempTable";
			String alter = "ALTER TABLE " + DBConstants.USERS_TABLE
					+ " RENAME TO " + tempTable;

			String create = "CREATE TABLE IF NOT EXISTS "
					+ DBConstants.USERS_TABLE + " ( " + DBConstants.ID
					+ " STRING , " + DBConstants.NAME + " TEXT, "
					+ DBConstants.MSISDN + " TEXT COLLATE nocase, "
					+ DBConstants.ONHIKE + " INTEGER, " + DBConstants.PHONE
					+ " TEXT, " + DBConstants.HAS_CUSTOM_PHOTO + " INTEGER, "
					+ DBConstants.OVERLAY_DISMISSED + " INTEGER, "
					+ DBConstants.MSISDN_TYPE + " STRING, "
					+ DBConstants.LAST_MESSAGED + " INTEGER" + " )";

			String insert = "INSERT INTO " + DBConstants.USERS_TABLE
					+ " SELECT " + DBConstants.ID + ", " + DBConstants.NAME
					+ ", " + DBConstants.MSISDN + ", " + DBConstants.ONHIKE
					+ ", " + DBConstants.PHONE + ", "
					+ DBConstants.HAS_CUSTOM_PHOTO + ", "
					+ DBConstants.OVERLAY_DISMISSED + ", "
					+ DBConstants.MSISDN_TYPE + ", "
					+ DBConstants.LAST_MESSAGED + " FROM " + tempTable;

			String drop = "DROP TABLE " + tempTable;

			db.execSQL(alter);
			db.execSQL(create);
			db.execSQL(insert);
			db.execSQL(drop);
		}
	}

	public void addContacts(List<ContactInfo> contacts, boolean isFirstSync)
			throws DbException {
		SQLiteDatabase db = mDb;
		db.beginTransaction();

		Map<String, String> msisdnTypeMap = new HashMap<String, String>();
		/*
		 * Since this is the first sync, we just run one query and pickup all
		 * the extra info required. For all subsequent syncs we run the query
		 * for each contact separately.
		 */
		if (isFirstSync) {
			// Adding the last contacted and phone type info
			Cursor extraInfo = this.mContext.getContentResolver().query(
					Phone.CONTENT_URI,
					new String[] { Phone.NUMBER, Phone.TYPE, Phone.LABEL },
					null, null, null);

			int msisdnIdx = extraInfo.getColumnIndex(Phone.NUMBER);
			int typeIdx = extraInfo.getColumnIndex(Phone.TYPE);
			int labelIdx = extraInfo.getColumnIndex(Phone.LABEL);

			while (extraInfo.moveToNext()) {
				String msisdnType = Phone.getTypeLabel(
						this.mContext.getResources(),
						extraInfo.getInt(typeIdx),
						extraInfo.getString(labelIdx)).toString();

				msisdnTypeMap.put(extraInfo.getString(msisdnIdx), msisdnType);
			}
			extraInfo.close();
		}

		InsertHelper ih = null;
		try {
			ih = new InsertHelper(db, DBConstants.USERS_TABLE);
			final int msisdnColumn = ih.getColumnIndex(DBConstants.MSISDN);
			final int idColumn = ih.getColumnIndex(DBConstants.ID);
			final int nameColumn = ih.getColumnIndex(DBConstants.NAME);
			final int onHikeColumn = ih.getColumnIndex(DBConstants.ONHIKE);
			final int phoneColumn = ih.getColumnIndex(DBConstants.PHONE);
			final int msisdnTypeColumn = ih
					.getColumnIndex(DBConstants.MSISDN_TYPE);
			for (ContactInfo contact : contacts) {
				ih.prepareForReplace();
				ih.bind(nameColumn, contact.getName());
				ih.bind(msisdnColumn, contact.getMsisdn());
				ih.bind(idColumn, contact.getId());
				ih.bind(onHikeColumn, contact.isOnhike());
				ih.bind(phoneColumn, contact.getPhoneNum());
				if (!isFirstSync) {
					String selection = Phone.CONTACT_ID + " =? " + " AND "
							+ Phone.NUMBER + " =? ";
					// Adding the last contacted and phone type info
					Cursor additionalInfo = this.mContext.getContentResolver()
							.query(Phone.CONTENT_URI,
									new String[] { Phone.TYPE, Phone.LABEL },
									selection,
									new String[] { contact.getId(),
											contact.getPhoneNum() }, null);

					int typeIdx = additionalInfo.getColumnIndex(Phone.TYPE);
					int labelIdx = additionalInfo.getColumnIndex(Phone.LABEL);
					if (additionalInfo.moveToFirst()) {
						contact.setMsisdnType(Phone.getTypeLabel(
								this.mContext.getResources(),
								additionalInfo.getInt(typeIdx),
								additionalInfo.getString(labelIdx)).toString());
					}
					Log.d(getClass().getSimpleName(),
							"Msisdn Type: " + contact.getMsisdnType());
					additionalInfo.close();

					ih.bind(msisdnTypeColumn, contact.getMsisdnType());

					/*
					 * We add to favorites this reference. So should set the
					 * favorite type here.
					 */
					Cursor favoriteCursor = mDb.query(
							DBConstants.FAVORITES_TABLE,
							new String[] { DBConstants.FAVORITE_TYPE },
							DBConstants.MSISDN + "=?",
							new String[] { contact.getMsisdn() }, null, null,
							null);
					try {
						if (favoriteCursor.moveToFirst()) {
							int favoriteTypeOrdinal = favoriteCursor
									.getInt(favoriteCursor
											.getColumnIndex(DBConstants.FAVORITE_TYPE));
							contact.setFavoriteType(FavoriteType.values()[favoriteTypeOrdinal]);
						} else {
							contact.setFavoriteType(FavoriteType.NOT_FAVORITE);
						}
					} finally {
						favoriteCursor.close();
					}
					HikeMessengerApp.getPubSub().publish(
							HikePubSub.CONTACT_ADDED, contact);
				} else {
					ih.bind(msisdnTypeColumn,
							msisdnTypeMap.get(contact.getPhoneNum()));
					/*
					 * We're saving this parameter to notify that the extra info
					 * that we are now fetching (Msisdn type) has been synced.
					 * So for apps that update from an older version, we can
					 * just check this value to verify whether the contacts have
					 * their extra info synced.
					 */
					Editor editor = mContext.getSharedPreferences(
							HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
					editor.putBoolean(
							HikeMessengerApp.CONTACT_EXTRA_INFO_SYNCED, true);
					editor.commit();
				}
				ih.execute();
			}
			db.setTransactionSuccessful();
		} catch (Exception e) {
			Log.e("HikeUserDatabase", "Unable to insert contacts", e);
			throw new DbException(e);
		} finally {
			if (ih != null) {
				ih.close();
			}
			db.endTransaction();
		}
	}

	public void addBlockList(List<String> msisdns) throws DbException {
		if (msisdns == null) {
			return;
		}

		SQLiteDatabase db = mDb;
		db.beginTransaction();

		InsertHelper ih = null;
		try {
			ih = new InsertHelper(db, DBConstants.BLOCK_TABLE);
			final int msisdnColumn = ih.getColumnIndex(DBConstants.MSISDN);
			for (String msisdn : msisdns) {
				ih.prepareForReplace();
				ih.bind(msisdnColumn, msisdn);
				ih.execute();
			}
			db.setTransactionSuccessful();
		} catch (Exception e) {
			Log.e("HikeUserDatabase", "Unable to insert contacts", e);
			throw new DbException(e);
		} finally {
			if (ih != null) {
				ih.close();
			}
			db.endTransaction();
		}
	}

	/**
	 * Sets the address book from the list of contacts Deletes any existing
	 * contacts from the db
	 * 
	 * @param contacts
	 *            list of contacts to set/add
	 */
	public void setAddressBookAndBlockList(List<ContactInfo> contacts,
			List<String> blockedMsisdns) throws DbException {
		/* delete all existing entries from database */
		mDb.delete(DBConstants.USERS_TABLE, null, null);

		mDb.delete(DBConstants.BLOCK_TABLE, null, null);

		addContacts(contacts, true);
		addBlockList(blockedMsisdns);
	}

	public ContactInfo getContactInfoFromMSISDN(String msisdn,
			boolean ifNotFoundReturnNull) {
		Cursor c = mReadDb.query(DBConstants.USERS_TABLE, new String[] {
				DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME,
				DBConstants.ONHIKE, DBConstants.PHONE, DBConstants.MSISDN_TYPE,
				DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO,
				DBConstants.FAVORITE_TYPE_SELECTION }, DBConstants.MSISDN
				+ "=?", new String[] { msisdn }, null, null, null);

		List<ContactInfo> contactInfos = extractContactInfo(c);
		c.close();

		if (contactInfos.isEmpty()) {
			Log.d(getClass().getSimpleName(), "No contact found");
			if (ifNotFoundReturnNull) {
				return null;
			} else {
				Cursor favoriteCursor = mReadDb.query(
						DBConstants.FAVORITES_TABLE,
						new String[] { DBConstants.FAVORITE_TYPE },
						DBConstants.MSISDN + " =? ", new String[] { msisdn },
						null, null, null);
				try {
					/*
					 * Setting the favorite type for unknown contacts
					 */
					FavoriteType favoriteType = FavoriteType.NOT_FAVORITE;
					if (favoriteCursor.moveToFirst()) {
						favoriteType = FavoriteType.values()[favoriteCursor
								.getInt(favoriteCursor
										.getColumnIndex(DBConstants.FAVORITE_TYPE))];
					}
					ContactInfo contactInfo = new ContactInfo(msisdn, msisdn,
							null, msisdn, false);
					contactInfo.setFavoriteType(favoriteType);
					return contactInfo;
				} finally {
					favoriteCursor.close();
				}
			}
		}

		return contactInfos.get(0);
	}

	private List<ContactInfo> extractContactInfo(Cursor c) {
		return extractContactInfo(c, false);
	}

	private List<ContactInfo> extractContactInfo(Cursor c, boolean distinct) {
		List<ContactInfo> contactInfos = new ArrayList<ContactInfo>(
				c.getCount());
		int idx = c.getColumnIndex(DBConstants.ID);
		int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
		int nameIdx = c.getColumnIndex(DBConstants.NAME);
		int onhikeIdx = c.getColumnIndex(DBConstants.ONHIKE);
		int phoneNumIdx = c.getColumnIndex(DBConstants.PHONE);
		int msisdnTypeIdx = c.getColumnIndex(DBConstants.MSISDN_TYPE);
		int lastMessagedIdx = c.getColumnIndex(DBConstants.LAST_MESSAGED);
		int hasCustomPhotoIdx = c.getColumnIndex(DBConstants.HAS_CUSTOM_PHOTO);
		int favoriteIdx = c.getColumnIndex(DBConstants.FAVORITE_TYPE);

		Set<String> msisdnSet = null;

		if (distinct) {
			msisdnSet = new HashSet<String>();
		}
		while (c.moveToNext()) {
			String msisdn = c.getString(msisdnIdx);
			if (distinct && msisdnSet.contains(msisdn)) {
				continue;
			} else if (distinct) {
				msisdnSet.add(msisdn);
			}
			ContactInfo contactInfo = new ContactInfo(c.getString(idx),
					c.getString(msisdnIdx), c.getString(nameIdx),
					c.getString(phoneNumIdx), c.getInt(onhikeIdx) != 0,
					c.getString(msisdnTypeIdx), c.getLong(lastMessagedIdx),
					c.getInt(hasCustomPhotoIdx) == 1);
			if (favoriteIdx != -1) {
				int favoriteTypeOrd = c.getInt(favoriteIdx);
				contactInfo
						.setFavoriteType(FavoriteType.values()[favoriteTypeOrd]);
			} else {
				contactInfo.setFavoriteType(FavoriteType.NOT_FAVORITE);
			}
			contactInfos.add(contactInfo);
		}
		c.close();
		return contactInfos;
	}

	public ContactInfo getContactInfoFromId(String id) {
		Cursor c = mReadDb.query(DBConstants.USERS_TABLE, new String[] {
				DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME,
				DBConstants.ONHIKE, DBConstants.PHONE, DBConstants.MSISDN_TYPE,
				DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO },
				DBConstants.ID + "=?", new String[] { id }, null, null, null);
		List<ContactInfo> contactInfos = extractContactInfo(c);
		c.close();
		if (contactInfos.isEmpty()) {
			return null;
		}

		return contactInfos.get(0);
	}

	/**
	 * Adds a single contact to the hike user db
	 * 
	 * @param hikeContactInfo
	 *            contact to add
	 * @return true if the insert was successful
	 */
	public void addContact(ContactInfo hikeContactInfo) throws DbException {
		List<ContactInfo> l = new LinkedList<ContactInfo>();
		l.add(hikeContactInfo);
		addContacts(l, false);
	}

	public List<Pair<AtomicBoolean, ContactInfo>> getNonHikeContacts() {
		Cursor c = mReadDb.rawQuery("SELECT " + DBConstants.USERS_TABLE + "."
				+ DBConstants.MSISDN + ", " + DBConstants.USERS_TABLE + "."
				+ DBConstants.ID + ", " + DBConstants.USERS_TABLE + "."
				+ DBConstants.NAME + ", " + DBConstants.USERS_TABLE + "."
				+ DBConstants.ONHIKE + ", " + DBConstants.USERS_TABLE + "."
				+ DBConstants.PHONE + ", " + DBConstants.USERS_TABLE + "."
				+ DBConstants.MSISDN_TYPE + ", " + DBConstants.USERS_TABLE
				+ "." + DBConstants.HAS_CUSTOM_PHOTO + ", "
				+ DBConstants.USERS_TABLE + "." + DBConstants.LAST_MESSAGED
				+ " FROM " + DBConstants.USERS_TABLE + " WHERE "
				+ DBConstants.USERS_TABLE + "." + DBConstants.MSISDN
				+ " NOT IN (SELECT " + DBConstants.MSISDN + " FROM "
				+ DBConstants.BLOCK_TABLE + ") AND " + DBConstants.USERS_TABLE
				+ "." + DBConstants.ONHIKE + " =0 AND "
				+ DBConstants.USERS_TABLE + "." + DBConstants.MSISDN
				+ " !='null'" + " ORDER BY " + DBConstants.USERS_TABLE + "."
				+ DBConstants.NAME + " COLLATE NOCASE", null);

		List<Pair<AtomicBoolean, ContactInfo>> contactInfos = new ArrayList<Pair<AtomicBoolean, ContactInfo>>(
				c.getCount());
		int idx = c.getColumnIndex(DBConstants.ID);
		int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
		int nameIdx = c.getColumnIndex(DBConstants.NAME);
		int onhikeIdx = c.getColumnIndex(DBConstants.ONHIKE);
		int phoneNumIdx = c.getColumnIndex(DBConstants.PHONE);
		int msisdnTypeIdx = c.getColumnIndex(DBConstants.MSISDN_TYPE);
		int lastMessagedIdx = c.getColumnIndex(DBConstants.LAST_MESSAGED);
		int hasCustomPhotoIdx = c.getColumnIndex(DBConstants.HAS_CUSTOM_PHOTO);
		int favoriteIdx = c.getColumnIndex(DBConstants.FAVORITE_TYPE);

		while (c.moveToNext()) {
			ContactInfo contactInfo = new ContactInfo(c.getString(idx),
					c.getString(msisdnIdx), c.getString(nameIdx),
					c.getString(phoneNumIdx), c.getInt(onhikeIdx) != 0,
					c.getString(msisdnTypeIdx), c.getLong(lastMessagedIdx),
					c.getInt(hasCustomPhotoIdx) == 1);
			if (favoriteIdx != -1) {
				int favoriteTypeOrd = c.getInt(favoriteIdx);
				contactInfo
						.setFavoriteType(FavoriteType.values()[favoriteTypeOrd]);
			} else {
				contactInfo.setFavoriteType(FavoriteType.NOT_FAVORITE);
			}
			contactInfos.add(new Pair<AtomicBoolean, ContactInfo>(
					new AtomicBoolean(false), contactInfo));
		}
		c.close();

		return contactInfos;
	}

	public List<ContactInfo> getContacts() {
		return getContacts(true);
	}

	/**
	 * Returns a list of contact ordered by when they were last messaged
	 * 
	 * @param limit
	 *            The number of contacts required. -1 if limit is not to be
	 *            considered
	 * @param onHike
	 *            1/0 for hike/non-hike contacts. -1 for both.
	 * @return
	 */
	public List<ContactInfo> getContactsOrderedByLastMessaged(int limit,
			int onHike) {
		String selection = DBConstants.USERS_TABLE
				+ "."
				+ DBConstants.MSISDN
				+ " != 'null'"
				+ (onHike != -1 ? " AND " + DBConstants.ONHIKE + "=" + onHike
						: "");

		Log.d(getClass().getSimpleName(), "Selection: " + selection);

		String orderBy = DBConstants.LAST_MESSAGED + " DESC, "
				+ DBConstants.NAME + " COLLATE NOCASE"
				+ (limit > -1 ? " LIMIT " + limit : "");
		String[] columns = { DBConstants.MSISDN, DBConstants.ID,
				DBConstants.NAME, DBConstants.ONHIKE, DBConstants.PHONE,
				DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED,
				DBConstants.HAS_CUSTOM_PHOTO };
		Cursor c = mReadDb.query(DBConstants.USERS_TABLE, columns, selection,
				null, null, null, orderBy);
		List<ContactInfo> contactInfos = extractContactInfo(c, true);
		c.close();
		if (contactInfos.isEmpty()) {
			return contactInfos;
		}
		return contactInfos;
	}

	public List<ContactInfo> getContactsOfFavoriteType(
			FavoriteType favoriteType, int onHike) {
		String favoriteMsisdnColumnName = "tempMsisdn";
		StringBuilder queryBuilder = new StringBuilder("SELECT "
				+ DBConstants.USERS_TABLE + "." + DBConstants.MSISDN + ", "
				+ DBConstants.ID + ", " + DBConstants.NAME + ", "
				+ DBConstants.ONHIKE + ", " + DBConstants.PHONE + ", "
				+ DBConstants.MSISDN_TYPE + ", " + DBConstants.HAS_CUSTOM_PHOTO
				+ ", " + DBConstants.LAST_MESSAGED);
		if (favoriteType != null) {
			if (favoriteType == FavoriteType.NOT_FAVORITE) {
				queryBuilder.append(" FROM " + DBConstants.USERS_TABLE
						+ " WHERE " + DBConstants.USERS_TABLE + "."
						+ DBConstants.MSISDN + " NOT IN (SELECT "
						+ DBConstants.MSISDN + " FROM "
						+ DBConstants.FAVORITES_TABLE + ") AND "
						+ DBConstants.USERS_TABLE + "." + DBConstants.MSISDN
						+ " != 'null'");
			} else {
				queryBuilder.append(", " + DBConstants.FAVORITES_TABLE + "."
						+ DBConstants.MSISDN + " AS "
						+ favoriteMsisdnColumnName + " FROM "
						+ DBConstants.FAVORITES_TABLE + " LEFT OUTER JOIN "
						+ DBConstants.USERS_TABLE + " ON "
						+ DBConstants.FAVORITES_TABLE + "."
						+ DBConstants.MSISDN + " = " + DBConstants.USERS_TABLE
						+ "." + DBConstants.MSISDN + " WHERE "
						+ DBConstants.FAVORITE_TYPE + " = "
						+ favoriteType.ordinal());
			}
		}
		if (onHike != -1) {
			queryBuilder.append(" AND " + DBConstants.ONHIKE + " = " + onHike);
		}
		String query = queryBuilder.toString();
		Log.d(getClass().getSimpleName(), "Favorites query: " + query);

		Cursor c = mDb.rawQuery(query, null);

		int idx = c.getColumnIndex(DBConstants.ID);
		int userMsisdnIdx = c.getColumnIndex(DBConstants.USERS_TABLE + "."
				+ DBConstants.MSISDN);
		int favoriteMsisdnIdx = c.getColumnIndex(favoriteMsisdnColumnName);
		int nameIdx = c.getColumnIndex(DBConstants.NAME);
		int onhikeIdx = c.getColumnIndex(DBConstants.ONHIKE);
		int phoneNumIdx = c.getColumnIndex(DBConstants.PHONE);
		int msisdnTypeIdx = c.getColumnIndex(DBConstants.MSISDN_TYPE);
		int lastMessagedIdx = c.getColumnIndex(DBConstants.LAST_MESSAGED);
		int hasCustomPhotoIdx = c.getColumnIndex(DBConstants.HAS_CUSTOM_PHOTO);

		Set<String> msisdnSet = null;

		msisdnSet = new HashSet<String>();

		List<ContactInfo> contactInfos = new ArrayList<ContactInfo>();
		while (c.moveToNext()) {
			String msisdn = c
					.getString(favoriteType == FavoriteType.NOT_FAVORITE ? userMsisdnIdx
							: favoriteMsisdnIdx);
			if (msisdnSet.contains(msisdn)) {
				continue;
			}
			msisdnSet.add(msisdn);

			ContactInfo contactInfo;

			String userMsisdn = c.getString(userMsisdnIdx);

			if (TextUtils.isEmpty(userMsisdn)) {
				contactInfo = new ContactInfo(msisdn, msisdn, null, msisdn);
			} else {
				contactInfo = new ContactInfo(c.getString(idx), userMsisdn,
						c.getString(nameIdx), c.getString(phoneNumIdx),
						c.getInt(onhikeIdx) != 0, c.getString(msisdnTypeIdx),
						c.getLong(lastMessagedIdx),
						c.getInt(hasCustomPhotoIdx) == 1);
			}

			contactInfo.setFavoriteType(favoriteType);
			contactInfos.add(contactInfo);
		}
		c.close();

		Collections.sort(contactInfos);

		return contactInfos;
	}

	public List<ContactInfo> getContactsForComposeScreen(boolean freeSMSOn,
			boolean fwdOrgroupChat) {
		String selection = DBConstants.MSISDN
				+ " != 'null'"
				+ ((freeSMSOn && fwdOrgroupChat) ? " AND (("
						+ DBConstants.ONHIKE + " = 0 AND " + DBConstants.MSISDN
						+ " LIKE '+91%') OR (" + DBConstants.ONHIKE + "=1))"
						: (fwdOrgroupChat ? " AND " + DBConstants.ONHIKE
								+ " != 0" : ""));

		Log.d(getClass().getSimpleName(), "Selection: " + selection);

		boolean shouldSortInDB = !freeSMSOn || fwdOrgroupChat;

		String orderBy = (shouldSortInDB) ? DBConstants.ONHIKE + " DESC, "
				+ DBConstants.NAME + " COLLATE NOCASE" : "";

		String[] columns = { DBConstants.MSISDN, DBConstants.ID,
				DBConstants.NAME, DBConstants.ONHIKE, DBConstants.PHONE,
				DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED,
				DBConstants.HAS_CUSTOM_PHOTO };
		Cursor c = mReadDb.query(DBConstants.USERS_TABLE, columns, selection,
				null, null, null, orderBy);
		List<ContactInfo> contactInfos = extractContactInfo(c);
		c.close();

		if (!shouldSortInDB) {
			Collections.sort(contactInfos, new Comparator<ContactInfo>() {

				@Override
				public int compare(ContactInfo lhs, ContactInfo rhs) {
					if (lhs.isOnhike() != rhs.isOnhike()) {
						return (lhs.isOnhike()) ? -1 : 1;
					} else {
						if (lhs.isOnhike()) {
							return lhs.compareTo(rhs);
						} else {
							if ((lhs.getMsisdn().startsWith(
									HikeConstants.INDIA_COUNTRY_CODE) && rhs
									.getMsisdn().startsWith(
											HikeConstants.INDIA_COUNTRY_CODE))
									|| (!lhs.getMsisdn().startsWith(
											HikeConstants.INDIA_COUNTRY_CODE) && !rhs
											.getMsisdn()
											.startsWith(
													HikeConstants.INDIA_COUNTRY_CODE))) {
								return lhs.compareTo(rhs);
							}
							return lhs.getMsisdn().startsWith(
									HikeConstants.INDIA_COUNTRY_CODE) ? -1 : 1;
						}
					}
				}
			});
		}
		return contactInfos;
	}

	public List<ContactInfo> getContacts(boolean ignoreEmpty) {
		String selection = ignoreEmpty ? DBConstants.MSISDN + " != 'null'"
				: null;
		Cursor c = mReadDb.query(DBConstants.USERS_TABLE, new String[] {
				DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME,
				DBConstants.ONHIKE, DBConstants.PHONE, DBConstants.MSISDN_TYPE,
				DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO },
				selection, null, null, null, null);
		List<ContactInfo> contactInfos = extractContactInfo(c);
		c.close();
		if (contactInfos.isEmpty()) {
			return contactInfos;
		}
		return contactInfos;
	}

	public void deleteMultipleRows(Collection<String> ids) {
		String ids_joined = "(" + Utils.join(ids, ",", "\"", "\"") + ")";
		mDb.delete(DBConstants.USERS_TABLE, DBConstants.ID + " in "
				+ ids_joined, null);
	}

	public void deleteRow(String id) {
		mDb.delete(DBConstants.USERS_TABLE, DBConstants.ID + "=?",
				new String[] { id });
	}

	public void updateContacts(List<ContactInfo> updatedContacts) {
		if (updatedContacts == null) {
			return;
		}

		ArrayList<String> ids = new ArrayList<String>(updatedContacts.size());
		for (ContactInfo c : updatedContacts) {
			ids.add(c.getId());
		}
		deleteMultipleRows(ids);
		try {
			addContacts(updatedContacts, false);
		} catch (DbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void updateHikeContact(String msisdn, boolean onhike) {
		ContentValues vals = new ContentValues(1);
		vals.put(DBConstants.ONHIKE, onhike);
		mDb.update(DBConstants.USERS_TABLE, vals, "msisdn=?",
				new String[] { msisdn });
	}

	public void deleteAll() {
		mDb.delete(DBConstants.USERS_TABLE, null, null);
		mDb.delete(DBConstants.BLOCK_TABLE, null, null);
		mDb.delete(DBConstants.THUMBNAILS_TABLE, null, null);
		mDb.delete(DBConstants.FAVORITES_TABLE, null, null);
	}

	public ContactInfo getContactInfoFromPhoneNo(String number) {
		Cursor c = mReadDb.query(DBConstants.USERS_TABLE, new String[] {
				DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME,
				DBConstants.ONHIKE, DBConstants.PHONE, DBConstants.MSISDN_TYPE,
				DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO },
				DBConstants.PHONE + "=?", new String[] { number }, null, null,
				null);
		List<ContactInfo> contactInfos = extractContactInfo(c);
		c.close();
		if (contactInfos.isEmpty()) {
			return null;
		}

		return contactInfos.get(0);
	}

	public void unblock(String msisdn) {
		mDb.delete(DBConstants.BLOCK_TABLE, DBConstants.MSISDN + "=?",
				new String[] { msisdn });
	}

	public void block(String msisdn) {
		ContentValues values = new ContentValues();
		values.put(DBConstants.MSISDN, msisdn);
		mDb.insert(DBConstants.BLOCK_TABLE, null, values);
	}

	public boolean isBlocked(String msisdn) {
		Cursor c = null;
		try {
			c = mDb.query(DBConstants.BLOCK_TABLE, null, DBConstants.MSISDN
					+ "=?", new String[] { msisdn }, null, null, null);
			return c.moveToFirst();
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public Set<String> getBlockedUsers() {
		Set<String> blocked = new HashSet<String>();
		Cursor c = null;
		try {
			c = mReadDb.query(DBConstants.BLOCK_TABLE,
					new String[] { DBConstants.MSISDN }, null, null, null,
					null, null);
			int idx = c.getColumnIndex(DBConstants.MSISDN);
			while (c.moveToNext()) {
				blocked.add(c.getString(idx));
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}

		return blocked;
	}

	public List<Pair<AtomicBoolean, ContactInfo>> getBlockedUserList() {
		List<ContactInfo> contactList = getContacts();
		List<Pair<AtomicBoolean, ContactInfo>> blockedContactList = new ArrayList<Pair<AtomicBoolean, ContactInfo>>();
		Set<String> blockedUsers = getBlockedUsers();
		Collections.sort(contactList);
		for (ContactInfo contactInfo : contactList) {
			blockedContactList.add(new Pair<AtomicBoolean, ContactInfo>(
					new AtomicBoolean(blockedUsers.contains(contactInfo
							.getMsisdn())), contactInfo));
		}
		return blockedContactList;
	}

	public void setIcon(String msisdn, byte[] data, boolean isProfileImage) {
		if (!isProfileImage) {
			/*
			 * We delete the older file that contained the larger avatar image
			 * for this msisdn.
			 */
			Utils.removeLargerProfileImageForMsisdn(msisdn);

			Bitmap tempBitmap = BitmapFactory.decodeByteArray(data, 0,
					data.length);
			Bitmap roundedBitmap = Utils.getRoundedCornerBitmap(tempBitmap);
			data = Utils
					.bitmapToBytes(roundedBitmap, Bitmap.CompressFormat.PNG);
			tempBitmap.recycle();
			roundedBitmap.recycle();
		}
		IconCacheManager.getInstance().clearIconForMSISDN(msisdn);
		ContentValues vals = new ContentValues(2);
		vals.put(DBConstants.MSISDN, msisdn);
		vals.put(DBConstants.IMAGE, data);
		mDb.replace(DBConstants.THUMBNAILS_TABLE, null, vals);

		String whereClause = DBConstants.MSISDN + "=?"; // msisdn;
		ContentValues customPhotoFlag = new ContentValues(1);
		customPhotoFlag.put(DBConstants.HAS_CUSTOM_PHOTO, 1);
		mDb.update(DBConstants.USERS_TABLE, customPhotoFlag, whereClause,
				new String[] { msisdn });
	}

	public Drawable getIcon(String msisdn) {
		Cursor c = mDb.query(DBConstants.THUMBNAILS_TABLE,
				new String[] { DBConstants.IMAGE }, "msisdn=?",
				new String[] { msisdn }, null, null, null);
		try {
			if (!c.moveToFirst()) {
				/* lookup based on this msisdn */
				return Utils.getDefaultIconForUser(mContext, msisdn);
			}

			byte[] icondata = c.getBlob(c.getColumnIndex(DBConstants.IMAGE));
			return new BitmapDrawable(BitmapFactory.decodeByteArray(icondata,
					0, icondata.length));
		} finally {
			c.close();
		}
	}

	public void removeIcon(String msisdn) {
		/*
		 * We delete the older file that contained the larger avatar image for
		 * this msisdn.
		 */
		Utils.removeLargerProfileImageForMsisdn(msisdn);

		mDb.delete(DBConstants.THUMBNAILS_TABLE, DBConstants.MSISDN + "=?",
				new String[] { msisdn });

		String whereClause = DBConstants.MSISDN + "=?"; // msisdn;
		ContentValues customPhotoFlag = new ContentValues(1);
		customPhotoFlag.put(DBConstants.HAS_CUSTOM_PHOTO, 0);
		mDb.update(DBConstants.USERS_TABLE, customPhotoFlag, whereClause,
				new String[] { msisdn });
	}

	public void updateContactRecency(String msisdn, long timeStamp) {
		ContentValues updatedTime = new ContentValues(1);
		updatedTime.put(DBConstants.LAST_MESSAGED, timeStamp);

		String whereClause = DBConstants.MSISDN + "=?";
		int rows = mDb.update(DBConstants.USERS_TABLE, updatedTime,
				whereClause, new String[] { msisdn });
		Log.d(getClass().getSimpleName(), "Row has been updated: " + rows);
	}

	public void syncContactExtraInfo() {
		Cursor extraInfo = this.mContext.getContentResolver().query(
				Phone.CONTENT_URI,
				new String[] { Phone.NUMBER, Phone.TYPE, Phone.LABEL }, null,
				null, null);

		int msisdnIdx = extraInfo.getColumnIndex(Phone.NUMBER);
		int typeIdx = extraInfo.getColumnIndex(Phone.TYPE);
		int labelIdx = extraInfo.getColumnIndex(Phone.LABEL);

		mDb.beginTransaction();

		try {
			while (extraInfo.moveToNext()) {
				String msisdnType = Phone.getTypeLabel(
						this.mContext.getResources(),
						extraInfo.getInt(typeIdx),
						extraInfo.getString(labelIdx)).toString();
				Log.d(getClass().getSimpleName(), "Msisdntype: " + msisdnType);
				ContentValues contentValues = new ContentValues(1);
				contentValues.put(DBConstants.MSISDN_TYPE, msisdnType);
				String whereClause = DBConstants.PHONE + " =? ";
				mDb.update(DBConstants.USERS_TABLE, contentValues, whereClause,
						new String[] { extraInfo.getString(msisdnIdx) });
			}
			mDb.setTransactionSuccessful();
			Editor editor = mContext.getSharedPreferences(
					HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
			editor.putBoolean(HikeMessengerApp.CONTACT_EXTRA_INFO_SYNCED, true);
			editor.commit();
		} finally {
			extraInfo.close();
			mDb.endTransaction();
		}
	}

	public void toggleContactFavorite(String msisdn, FavoriteType favoriteType) {
		/*
		 * If we are setting the type as not favorite, we'll remove the row
		 * itself.
		 */
		if (favoriteType == FavoriteType.NOT_FAVORITE) {
			mDb.delete(DBConstants.FAVORITES_TABLE, DBConstants.MSISDN + "=?",
					new String[] { msisdn });
			return;
		}

		SQLiteStatement insertStatement = null;
		InsertHelper ih = null;
		try {
			ih = new InsertHelper(mDb, DBConstants.FAVORITES_TABLE);
			insertStatement = mDb.compileStatement("INSERT OR REPLACE INTO "
					+ DBConstants.FAVORITES_TABLE + " ( " + DBConstants.MSISDN
					+ ", " + DBConstants.FAVORITE_TYPE + " ) "
					+ " VALUES (?, ?)");
			mDb.beginTransaction();
			insertStatement.bindString(ih.getColumnIndex(DBConstants.MSISDN),
					msisdn);
			insertStatement.bindLong(
					ih.getColumnIndex(DBConstants.FAVORITE_TYPE),
					favoriteType.ordinal());

			insertStatement.executeInsert();
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

	public void addAutoRecommendedFavorites() {

		String selection = DBConstants.LAST_MESSAGED + ">0" + " AND "
				+ DBConstants.MSISDN + " NOT IN (SELECT " + DBConstants.MSISDN
				+ " FROM " + DBConstants.FAVORITES_TABLE + ")";
		String orderBy = DBConstants.LAST_MESSAGED + " DESC LIMIT "
				+ HikeConstants.MAX_AUTO_RECOMMENDED_FAVORITE;

		Cursor c = mDb.query(true, DBConstants.USERS_TABLE,
				new String[] { DBConstants.MSISDN }, selection, null, null,
				null, orderBy, null);

		int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);

		SQLiteStatement insertStatement = null;
		InsertHelper ih = null;
		try {
			ih = new InsertHelper(mDb, DBConstants.FAVORITES_TABLE);
			insertStatement = mDb.compileStatement("INSERT OR REPLACE INTO "
					+ DBConstants.FAVORITES_TABLE + " ( " + DBConstants.MSISDN
					+ ", " + DBConstants.FAVORITE_TYPE + " ) " + " VALUES (?, "
					+ FavoriteType.AUTO_RECOMMENDED_FAVORITE.ordinal() + ")");
			mDb.beginTransaction();
			while (c.moveToNext()) {
				String msisdn = c.getString(msisdnIdx);
				insertStatement.bindString(
						ih.getColumnIndex(DBConstants.MSISDN), msisdn);
				insertStatement.executeInsert();
			}
		} finally {
			if (insertStatement != null) {
				insertStatement.close();
			}
			if (ih != null) {
				ih.close();
			}
			c.close();
			mDb.setTransactionSuccessful();
			mDb.endTransaction();

			Log.d(getClass().getSimpleName(),
					"Auto rec fav added: " + c.getCount());
			// HikeMessengerApp.getPubSub().publish(
			// HikePubSub.AUTO_RECOMMENDED_FAVORITES_ADDED, null);
		}
	}

	public boolean isContactFavorite(String msisdn) {
		Cursor c = null;
		try {
			c = mDb.query(DBConstants.FAVORITES_TABLE,
					new String[] { DBConstants.MSISDN }, DBConstants.MSISDN
							+ "=? AND " + DBConstants.FAVORITE_TYPE + "="
							+ FavoriteType.FAVORITE.ordinal(),
					new String[] { msisdn }, null, null, null);
			return c.moveToFirst();
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public List<ContactInfo> getNonHikeRecentContactsFromListOfNumbers(
			String selectionNumbers, Map<String, Long> recentValues,
			boolean indiaOnly, FavoriteType favoriteType) {

		String[] columns = new String[] { DBConstants.MSISDN, DBConstants.ID,
				DBConstants.NAME, DBConstants.ONHIKE, DBConstants.PHONE,
				DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED,
				DBConstants.HAS_CUSTOM_PHOTO };

		String selection = DBConstants.PHONE
				+ " IN "
				+ selectionNumbers
				+ " AND "
				+ DBConstants.ONHIKE
				+ "=0"
				+ (indiaOnly ? " AND " + DBConstants.MSISDN + " LIKE '+91%'"
						: "")
				+ (favoriteType != null ? " AND " + DBConstants.MSISDN
						+ " NOT IN (SELECT " + DBConstants.MSISDN + " FROM "
						+ DBConstants.FAVORITES_TABLE + ")" : "");

		Log.d(getClass().getSimpleName(), "Selection query: " + selection);

		Cursor c = null;
		try {
			c = mReadDb.query(DBConstants.USERS_TABLE, columns, selection,
					null, null, null, null);
			int idx = c.getColumnIndex(DBConstants.ID);
			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			int nameIdx = c.getColumnIndex(DBConstants.NAME);
			int onhikeIdx = c.getColumnIndex(DBConstants.ONHIKE);
			int phoneNumIdx = c.getColumnIndex(DBConstants.PHONE);
			int msisdnTypeIdx = c.getColumnIndex(DBConstants.MSISDN_TYPE);
			int lastMessagedIdx = c.getColumnIndex(DBConstants.LAST_MESSAGED);
			int hasCustomPhotoIdx = c
					.getColumnIndex(DBConstants.HAS_CUSTOM_PHOTO);

			List<ContactInfo> contactList = new ArrayList<ContactInfo>();

			Set<String> nameSet = new HashSet<String>();

			while (c.moveToNext()) {
				String number = c.getString(phoneNumIdx);
				String name = c.getString(nameIdx);

				if (nameSet.contains(name)) {
					continue;
				}

				nameSet.add(name);

				/*
				 * All our timestamps are in seconds.
				 */
				Long lastMessagedDB = recentValues.get(number) / 1000;
				long lastMessagedCurrent = c.getLong(lastMessagedIdx);

				if ((lastMessagedDB != null)
						&& (lastMessagedDB > lastMessagedCurrent)) {
					lastMessagedCurrent = lastMessagedDB;
				}

				ContactInfo contactInfo = new ContactInfo(c.getString(idx),
						c.getString(msisdnIdx), name, c.getString(phoneNumIdx),
						c.getInt(onhikeIdx) != 0, c.getString(msisdnTypeIdx),
						lastMessagedCurrent, c.getInt(hasCustomPhotoIdx) == 1);
				contactInfo.setFavoriteType(favoriteType);
				contactList.add(contactInfo);
			}

			Collections.sort(contactList, new Comparator<ContactInfo>() {
				@Override
				public int compare(ContactInfo lhs, ContactInfo rhs) {
					if (lhs.getLastMessaged() != rhs.getLastMessaged()) {
						return -((Long) lhs.getLastMessaged()).compareTo(rhs
								.getLastMessaged());
					}
					return lhs.getName().toLowerCase()
							.compareTo(rhs.getName().toLowerCase());
				}
			});

			return contactList;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public List<ContactInfo> getNonHikeRecentContacts(int limit,
			boolean indiaOnly, FavoriteType favoriteType) {
		Pair<String, Map<String, Long>> data = ContactUtils.getRecentNumbers(
				mContext, limit);
		return getNonHikeRecentContactsFromListOfNumbers(data.first,
				data.second, indiaOnly, favoriteType);
	}

	private List<Pair<AtomicBoolean, ContactInfo>> getNonHikeMostContactedContactsFromListOfNumbers(
			String selectionNumbers,
			final Map<String, Integer> mostContactedValues, int limit) {

		String[] columns = new String[] { DBConstants.MSISDN, DBConstants.ID,
				DBConstants.NAME, DBConstants.ONHIKE, DBConstants.PHONE,
				DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED,
				DBConstants.HAS_CUSTOM_PHOTO };

		String selection = DBConstants.PHONE + " IN " + selectionNumbers
				+ " AND " + DBConstants.ONHIKE + "=0 LIMIT " + limit;

		Log.d(getClass().getSimpleName(), "Selection query: " + selection);

		Cursor c = null;
		try {
			c = mReadDb.query(DBConstants.USERS_TABLE, columns, selection,
					null, null, null, null);

			int idx = c.getColumnIndex(DBConstants.ID);
			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			int nameIdx = c.getColumnIndex(DBConstants.NAME);
			int onhikeIdx = c.getColumnIndex(DBConstants.ONHIKE);
			int phoneNumIdx = c.getColumnIndex(DBConstants.PHONE);
			int msisdnTypeIdx = c.getColumnIndex(DBConstants.MSISDN_TYPE);
			int lastMessagedIdx = c.getColumnIndex(DBConstants.LAST_MESSAGED);
			int hasCustomPhotoIdx = c
					.getColumnIndex(DBConstants.HAS_CUSTOM_PHOTO);

			List<Pair<AtomicBoolean, ContactInfo>> contactList = new ArrayList<Pair<AtomicBoolean, ContactInfo>>();

			Set<String> nameSet = new HashSet<String>();

			while (c.moveToNext()) {
				String name = c.getString(nameIdx);

				if (nameSet.contains(name)) {
					continue;
				}

				nameSet.add(name);

				/*
				 * All our timestamps are in seconds.
				 */
				long lastMessagedCurrent = c.getLong(lastMessagedIdx);

				ContactInfo contactInfo = new ContactInfo(c.getString(idx),
						c.getString(msisdnIdx), name, c.getString(phoneNumIdx),
						c.getInt(onhikeIdx) != 0, c.getString(msisdnTypeIdx),
						lastMessagedCurrent, c.getInt(hasCustomPhotoIdx) == 1);
				contactList.add(new Pair<AtomicBoolean, ContactInfo>(
						new AtomicBoolean(false), contactInfo));
			}

			Collections.sort(contactList,
					new Comparator<Pair<AtomicBoolean, ContactInfo>>() {
						@Override
						public int compare(
								Pair<AtomicBoolean, ContactInfo> lhs,
								Pair<AtomicBoolean, ContactInfo> rhs) {
							int lhsContactNum = mostContactedValues
									.get(lhs.second.getPhoneNum());
							int rhsContactNum = mostContactedValues
									.get(rhs.second.getPhoneNum());

							if (lhsContactNum != rhsContactNum) {
								return -((Integer) lhsContactNum)
										.compareTo(rhsContactNum);
							}
							return lhs.second
									.getName()
									.toLowerCase()
									.compareTo(
											rhs.second.getName().toLowerCase());
						}
					});

			for (int i = 0; i < Math.min(HikeConstants.MAX_PRECHECKED_CONTACTS,
					contactList.size()); i++) {
				Pair<AtomicBoolean, ContactInfo> val = contactList.get(i);
				val.first.set(true);
			}

			return contactList;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public List<Pair<AtomicBoolean, ContactInfo>> getNonHikeMostContactedContacts(
			int limit) {
		/*
		 * Sending twice the limit to account for the contacts that might be on
		 * hike
		 */
		Pair<String, Map<String, Integer>> data = ContactUtils
				.getMostContactedContacts(mContext, limit * 2);
		return getNonHikeMostContactedContactsFromListOfNumbers(data.first,
				data.second, limit);
	}

	public List<ContactInfo> getContactNamesFromMsisdnList(String msisdns) {
		// select max(name), msisdn from users where msisdn in (...) group by
		// msisdn;
		Cursor c = mReadDb.rawQuery("SELECT max(" + DBConstants.NAME + ") AS "
				+ DBConstants.NAME + ", " + DBConstants.MSISDN + ", "
				+ DBConstants.ONHIKE + " from " + DBConstants.USERS_TABLE
				+ " WHERE " + DBConstants.MSISDN + " IN " + msisdns
				+ " GROUP BY " + DBConstants.MSISDN, null);
		try {
			List<ContactInfo> contactList = new ArrayList<ContactInfo>();

			final int nameIdx = c.getColumnIndex(DBConstants.NAME);
			final int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			final int onHikeIdx = c.getColumnIndex(DBConstants.ONHIKE);

			while (c.moveToNext()) {
				String msisdn = c.getString(msisdnIdx);
				String name = c.getString(nameIdx);
				boolean onHike = c.getInt(onHikeIdx) != 0;
				Log.d(getClass().getSimpleName(), "Name: " + name);
				contactList.add(new ContactInfo(null, msisdn, name, null,
						onHike));
			}
			return contactList;
		} finally {
			c.close();
		}
	}

	public void setMultipleContactsToFavorites(JSONObject favorites) {
		SQLiteStatement insertStatement = null;
		InsertHelper ih = null;
		try {
			ih = new InsertHelper(mDb, DBConstants.FAVORITES_TABLE);
			insertStatement = mDb.compileStatement("INSERT OR REPLACE INTO "
					+ DBConstants.FAVORITES_TABLE + " ( " + DBConstants.MSISDN
					+ ", " + DBConstants.FAVORITE_TYPE + " ) "
					+ " VALUES (?, ?)");
			mDb.beginTransaction();

			JSONArray msisdns = favorites.names();
			if (msisdns == null) {
				return;
			}
			for (int i = 0; i < msisdns.length(); i++) {
				String msisdn = msisdns.optString(i);
				JSONObject msisdnInfo = favorites.optJSONObject(msisdn);
				FavoriteType favoriteType = msisdnInfo
						.optBoolean(HikeConstants.PENDING) ? FavoriteType.RECOMMENDED_FAVORITE
						: FavoriteType.FAVORITE;

				insertStatement.bindString(
						ih.getColumnIndex(DBConstants.MSISDN), msisdn);
				insertStatement.bindLong(
						ih.getColumnIndex(DBConstants.FAVORITE_TYPE),
						favoriteType.ordinal());

				insertStatement.executeInsert();
			}
		} finally {
			if (insertStatement != null) {
				insertStatement.close();
			}
			if (ih != null) {
				ih.close();
			}
			mDb.setTransactionSuccessful();
			mDb.endTransaction();

			Log.d(getClass().getSimpleName(), favorites.length() + "updated");
			if (favorites.length() > 0) {
				HikeMessengerApp.getPubSub().publish(
						HikePubSub.REFRESH_FAVORITES, null);
			}
		}
	}

	public boolean hasIcon(String msisdn) {
		Cursor c = mDb.query(DBConstants.THUMBNAILS_TABLE,
				new String[] { DBConstants.MSISDN }, DBConstants.MSISDN + "=?",
				new String[] { msisdn }, null, null, null);
		try {
			return c.moveToFirst();
		} finally {
			c.close();
		}
	}

	public int getPendingFriendRequestCount() {
		return (int) DatabaseUtils.longForQuery(mDb, "SELECT COUNT(*) FROM "
				+ DBConstants.FAVORITES_TABLE + " WHERE "
				+ DBConstants.FAVORITE_TYPE + "="
				+ FavoriteType.RECOMMENDED_FAVORITE.ordinal(), null);
	}

	public List<Pair<AtomicBoolean, ContactInfo>> getFamilyList(
			Context context, int limit) {
		String[] columns = new String[] { DBConstants.MSISDN, DBConstants.ID,
				DBConstants.NAME, DBConstants.ONHIKE, DBConstants.PHONE,
				DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED,
				DBConstants.HAS_CUSTOM_PHOTO };

		String userName = context.getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(
				HikeMessengerApp.NAME_SETTING, "");
		String lastName = null;
		String[] userNameSplit = userName.split(" ");
		if (userNameSplit.length > 1) {
			lastName = userNameSplit[userNameSplit.length - 1];
		}

		StringBuilder selectionStringBuilder = new StringBuilder("(");

		String[] familyKeywords = context.getResources().getStringArray(
				R.array.family_array);

		for (String nuxKeyword : familyKeywords) {
			selectionStringBuilder.append(DBConstants.NAME + " LIKE "
					+ DatabaseUtils.sqlEscapeString("%" + nuxKeyword + "%")
					+ " OR ");
		}
		if (lastName != null) {
			selectionStringBuilder.append(DBConstants.NAME + " LIKE "
					+ DatabaseUtils.sqlEscapeString("% " + lastName)
					+ " OR ");
		}
		selectionStringBuilder.replace(
				selectionStringBuilder.lastIndexOf("OR "),
				selectionStringBuilder.length(), ") AND " + DBConstants.ONHIKE
						+ "=0 AND " + DBConstants.MSISDN + " != 'null' " +"LIMIT " + limit);
		String selection = selectionStringBuilder.toString();

		Log.d(getClass().getSimpleName(), "Selection query: " + selection);

		Cursor c = null;
		try {
			c = mReadDb.query(DBConstants.USERS_TABLE, columns, selection,
					null, null, null, null);

			int idx = c.getColumnIndex(DBConstants.ID);
			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			int nameIdx = c.getColumnIndex(DBConstants.NAME);
			int onhikeIdx = c.getColumnIndex(DBConstants.ONHIKE);
			int phoneNumIdx = c.getColumnIndex(DBConstants.PHONE);
			int msisdnTypeIdx = c.getColumnIndex(DBConstants.MSISDN_TYPE);
			int lastMessagedIdx = c.getColumnIndex(DBConstants.LAST_MESSAGED);
			int hasCustomPhotoIdx = c
					.getColumnIndex(DBConstants.HAS_CUSTOM_PHOTO);

			List<Pair<AtomicBoolean, ContactInfo>> contactList = new ArrayList<Pair<AtomicBoolean, ContactInfo>>();

			Set<String> nameSet = new HashSet<String>();

			while (c.moveToNext()) {
				String name = c.getString(nameIdx);

				if (nameSet.contains(name)) {
					continue;
				}

				nameSet.add(name);

				long lastMessagedCurrent = c.getLong(lastMessagedIdx);

				ContactInfo contactInfo = new ContactInfo(c.getString(idx),
						c.getString(msisdnIdx), name, c.getString(phoneNumIdx),
						c.getInt(onhikeIdx) != 0, c.getString(msisdnTypeIdx),
						lastMessagedCurrent, c.getInt(hasCustomPhotoIdx) == 1);
				contactList.add(new Pair<AtomicBoolean, ContactInfo>(
						new AtomicBoolean(false), contactInfo));
			}

			for (int i = 0; i < Math.min(HikeConstants.MAX_PRECHECKED_CONTACTS,
					contactList.size()); i++) {
				Pair<AtomicBoolean, ContactInfo> val = contactList.get(i);
				val.first.set(true);
			}

			return contactList;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public boolean doesContactExist(String msisdn) {
		String[] columns = new String[] { DBConstants.NAME };
		String selection = DBConstants.MSISDN + "=? ";
		Cursor c = null;

		try {
			c = mReadDb.query(DBConstants.USERS_TABLE, columns, selection,
					new String[] { msisdn }, null, null, null);
			return c.moveToFirst();
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}
}
