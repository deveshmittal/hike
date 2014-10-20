package com.bsb.hike.providers;

import java.util.ArrayList;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.platform.Authenticator;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

		// It is observed that providers get initated before conManager (conManager == null). Hence need to init
		conManager.init(getContext());

		hUserDb = conManager.getReadableDatabase();

		// Check for initialization of vital objects
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

		// TODO : This is a stub. TBD on Auth implementation.
		if (!Authenticator.getInstance().isTokenValid(uri.getLastPathSegment()))
		{
			Logger.d(TAG, "Invalid access token");
			return null;
		}

		Cursor c = null;

		// Convert hikeIds to msisdnList since avatar tables have msisdn - image mappings only
		ArrayList<String> msisdnList = conManager.getMsisdnFromId(selectionArgs);

		if (msisdnList == null || msisdnList.size() == 0)
		{
			throw new IllegalArgumentException("Invalid hikeIds");
		}

		Logger.d(TAG, "msisdnList: " + msisdnList.toString());

		// Identify avatar request
		switch (sURIMatcher.match(uri))
		{
		case ROUNDED_INDEX:

			Logger.d(TAG, "Querying rounded avatar table");

			c = hUserDb.query(DBConstants.ROUNDED_THUMBNAIL_TABLE, projection, HikeConstants.MSISDN + " IN " + Utils.getMsisdnStatement(msisdnList), null, null, null, sortOrder);

			break;

		case NORMAL_INDEX:

			Logger.d(TAG, "Querying normal avatar table");

			c = hUserDb.query(DBConstants.THUMBNAILS_TABLE, projection, HikeConstants.MSISDN + " IN " + Utils.getMsisdnStatement(msisdnList), null, null, null, sortOrder);

			break;
		default:
			break;
		}

		if (c == null || !c.moveToFirst())
		{
			throw new IllegalArgumentException("Invalid path/parameters");
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
