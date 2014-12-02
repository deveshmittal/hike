package com.bsb.hike.providers;

import java.util.Arrays;
import java.util.List;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.ui.HikeAuthActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;

/**
 * Provides hike contact's avatar blob data
 * 
 * @author Atul M
 * 
 */
public class HikeProvider extends ContentProvider
{
	private static final String TAG = HikeProvider.class.getName();

	private static final String AUTHORITY = "com.bsb.hike.providers.HikeProvider";

	private static final String BASE_PATH_ROUNDED = "avatarRounded";

	private static final String BASE_PATH_NORMAL = "avatarNormal";

	private static final int ROUNDED_INDEX = 1;

	private static final int NORMAL_INDEX = 2;

	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_ROUNDED);

	private SQLiteDatabase hUserDb;

	private ContactManager conManager;

	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static
	{
		sURIMatcher.addURI(AUTHORITY, BASE_PATH_ROUNDED, ROUNDED_INDEX);
		sURIMatcher.addURI(AUTHORITY, BASE_PATH_NORMAL, NORMAL_INDEX);
	}

	@Override
	public boolean onCreate()
	{
		conManager = ContactManager.getInstance();

		// It is observed that providers get initiated before conManager (conManager == null). Hence need to init
		conManager.init(getContext());

		hUserDb = conManager.getReadableDatabase();

		// Check for initialization of required objects
		if (hUserDb != null)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
	{

		// Authenticate
		List<String> uriPathSegments = uri.getPathSegments();

		if (uriPathSegments != null)
		{
			try
			{
				String accessT = uriPathSegments.get(uriPathSegments.size() - 2);
				String pkgName = uriPathSegments.get(uriPathSegments.size() - 1);
				if (HikeAuthActivity.verifyRequest(getContext(), pkgName, accessT))
				{
					String newUri = "content://" + uri.getAuthority();
					int segmentsListSize = uriPathSegments.size();

					for (int i = 0; i < segmentsListSize; i++)
					{
						if (i == (segmentsListSize - 2))
						{
							break;
						}
						newUri += "/" + uriPathSegments.get(i);
					}
					uri = Uri.parse(newUri);
				}
				else
				{
					throw new RuntimeException("Required missing authentication!");
				}
			}
			catch (IndexOutOfBoundsException iobe)
			{
				iobe.printStackTrace();
				return null;
			}
		}

		Cursor c = null;

		// Identify avatar request
		switch (sURIMatcher.match(uri))
		{
		case ROUNDED_INDEX:

			try
			{
				// For better security, use hard-coded selection columns
				if (selection == null)
				{
					c = hUserDb.rawQuery("SELECT roundedThumbnailTable.image, users.id " + "FROM roundedThumbnailTable " + "INNER JOIN users "
							+ "ON roundedThumbnailTable.msisdn=users.msisdn", null);
				}
				else
				{
					if (selectionArgs != null && selectionArgs.length > 0)
					{
						// TODO:Improve this. Make it more generic
						if (selectionArgs[0].equals("-1"))
						{
							// self avatar request
							ContactInfo contactInfo = Utils.getUserContactInfo(HikeSharedPreferenceUtil.getInstance(getContext(), HikeMessengerApp.ACCOUNT_SETTINGS).getPref());
							c = ContactManager
									.getInstance()
									.getReadableDatabase()
									.query(DBConstants.ROUNDED_THUMBNAIL_TABLE, new String[] { DBConstants.IMAGE }, DBConstants.MSISDN + "=?",
											new String[] { contactInfo.getMsisdn() }, null, null, null);
						}
						else
						{
							c = hUserDb.rawQuery("SELECT roundedThumbnailTable.image, users.id" + " FROM roundedThumbnailTable " + "INNER JOIN users "
									+ "ON roundedThumbnailTable.msisdn=users.msisdn " + "WHERE users.id IN " + Utils.getMsisdnStatement(Arrays.asList(selectionArgs)), null);
						}
					}
				}
			}
			catch (SQLiteException e)
			{
				c = null;
				e.printStackTrace();
			}
			break;

		case NORMAL_INDEX:
			try
			{
				// For better security, use hard-coded selection columns
				if (selection == null)
				{
					c = hUserDb.rawQuery("SELECT thumbnails.image, users.id " + "FROM thumbnails " + "INNER JOIN users " + "ON thumbnails.msisdn=users.msisdn", null);
				}
				else
				{
					if (selectionArgs != null && selectionArgs.length > 0)
					{
						// TODO:Improve this. Make it more generic
						if (selectionArgs[0].equals("-1"))
						{
							// self avatar request
							ContactInfo contactInfo = Utils.getUserContactInfo(HikeSharedPreferenceUtil.getInstance(getContext(), HikeMessengerApp.ACCOUNT_SETTINGS).getPref());
							c = ContactManager
									.getInstance()
									.getReadableDatabase()
									.query(DBConstants.THUMBNAILS_TABLE, new String[] { DBConstants.IMAGE }, DBConstants.MSISDN + "=?", new String[] { contactInfo.getMsisdn() },
											null, null, null);
						}
						else
						{
							c = hUserDb.rawQuery("SELECT thumbnails.image, users.id " + "FROM thumbnails " + "INNER JOIN users " + "ON thumbnails.msisdn=users.msisdn "
									+ "WHERE users.id IN " + Utils.getMsisdnStatement(Arrays.asList(selectionArgs)), null);
						}
					}
				}
			}
			catch (SQLiteException e)
			{
				e.printStackTrace();
			}
			break;
		default:
			break;
		}

		return c;
	}

	@Override
	public String getType(Uri uri)
	{
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values)
	{
		Logger.d(TAG, "Invalid access");
		throw new UnsupportedOperationException("Invalid access");
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{
		Logger.d(TAG, "Invalid access");
		throw new UnsupportedOperationException("Invalid access");
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
	{
		Logger.d(TAG, "Invalid access");
		throw new UnsupportedOperationException("Invalid access");
	}

}
