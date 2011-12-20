package com.bsb.hike.utils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;

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
}
