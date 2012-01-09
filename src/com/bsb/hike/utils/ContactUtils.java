package com.bsb.hike.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;

import com.bsb.hike.models.ContactInfo;

public class ContactUtils {
	/**
	 * Gets the mobile number for a contact.  If there's no mobile number, gets the default one
	 */
	public static String getMobileNumber(ContentResolver cr, Uri contact) {
		Cursor cursor = cr.query(contact, new String[]{"_id"}, null, null, null);
		if ((cursor == null) || 
			(!cursor.moveToFirst())) {
			Log.w("ContactUtils", "No contact found: " + contact);
			return null;
		}

		String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
	    cursor.close();

		Cursor phones = cr.query(Phone.CONTENT_URI, null, Phone.CONTACT_ID + " = " + contactId, null, null);
		String number = null;
		if (phones == null) {
			return null;
		}

		while (phones.moveToNext()) {
			number = phones.getString(phones.getColumnIndex(Phone.NUMBER));
			int type = phones.getInt(phones.getColumnIndex(Phone.TYPE));
			switch (type) {
			case Phone.TYPE_MOBILE:
				break;
			}
		}

		phones.close();
		return number;
	}

	public static ContactInfo getContactInfo(String phoneNumber, Context context) {
	    ContentResolver contentResolver = context.getContentResolver();
	    Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));

	    Cursor cursor = null;
	    HikeUserDatabase db = null;
	    try {

	        cursor = contentResolver.query(uri, new String[]{PhoneLookup.DISPLAY_NAME, PhoneLookup._ID}, null, null, null);
	        // if cursor is empty, just return null
	        if (!cursor.moveToFirst()) {
	            return null;
	        }

	        // lookup the user via the retrieved contactID
	        String id = cursor.getString(cursor.getColumnIndexOrThrow(PhoneLookup._ID));
	        db = new HikeUserDatabase(context);
	        ContactInfo contactInfo = db.getContactInfoFromId(id);
	        return contactInfo;
	    } catch (Exception e) {
	        return null;
	    } finally {
	        if (cursor != null)
	        {
	            cursor.close();
	        }
	        if (db != null) {
	            db.close();
	        }
	    }
	}
}
