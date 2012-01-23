package com.bsb.hike.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;

import com.bsb.hike.models.ContactInfo;

public class ContactUtils
{
	/**
	 * Gets the mobile number for a contact. If there's no mobile number, gets the default one
	 */
	public static String getMobileNumber(ContentResolver cr, Uri contact)
	{
		Cursor cursor = cr.query(contact, new String[] { "_id" }, null, null, null);
		if ((cursor == null) || (!cursor.moveToFirst()))
		{
			Log.w("ContactUtils", "No contact found: " + contact);
			return null;
		}

		String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
		cursor.close();

		Cursor phones = cr.query(Phone.CONTENT_URI, null, Phone.CONTACT_ID + " = " + contactId, null, null);
		String number = null;
		if (phones == null)
		{
			return null;
		}

		while (phones.moveToNext())
		{
			number = phones.getString(phones.getColumnIndex(Phone.NUMBER));
			int type = phones.getInt(phones.getColumnIndex(Phone.TYPE));
			switch (type)
			{
			case Phone.TYPE_MOBILE:
				break;
			}
		}

		phones.close();
		return number;
	}

	public static ContactInfo getContactInfo(String phoneNumber, Context context)
	{
		ContentResolver contentResolver = context.getContentResolver();
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));

		Cursor cursor = null;
		HikeUserDatabase db = null;
		try
		{

			cursor = contentResolver.query(uri, new String[] { PhoneLookup.DISPLAY_NAME, PhoneLookup._ID }, null, null, null);
			// if cursor is empty, just return null
			if (!cursor.moveToFirst())
			{
				return null;
			}

			// lookup the user via the retrieved contactID
			String id = cursor.getString(cursor.getColumnIndexOrThrow(PhoneLookup._ID));
			db = new HikeUserDatabase(context);
			ContactInfo contactInfo = db.getContactInfoFromId(id);
			return contactInfo;
		}
		catch (Exception e)
		{
			return null;
		}
		finally
		{
			if (cursor != null)
			{
				cursor.close();
			}
			if (db != null)
			{
				db.close();
			}
		}
	}

	/*
	 * Call this when we think the address book has changed.
	 * Checks for updates, posts to the server, writes them to the local database
	 * and updates existing conversations
	 */
	public static void syncUpdates(Context ctx)
	{

		HikeUserDatabase db = new HikeUserDatabase(ctx);

		Map<String, Set<ContactInfo>> new_contacts_by_id = convertToMap(getContacts(ctx));
		Map<String, Set<ContactInfo>> hike_contacts_by_id = convertToMap(db.getContacts());

		/* iterate over every item in the phone db,
		 * items that are equal remove from both maps
		 * items that are different, leave in 'new' map and remove from 'hike' map
		 * send the 'new' map as items to add, and send the 'hike' map as IDs to remove 
		 */
		Map.Entry<String, Set<ContactInfo>> entry = null;
		for (Iterator<Map.Entry<String, Set<ContactInfo>>> iterator = new_contacts_by_id.entrySet().iterator(); iterator.hasNext(); entry = iterator.next())
		{
			String id = entry.getKey();
			Set<ContactInfo> contacts_for_id = entry.getValue();
			Set<ContactInfo> hike_contacts_for_id = hike_contacts_by_id.get(id);
			if (contacts_for_id.equals(hike_contacts_for_id))
			{
				/* hike db is up to date, so don't send update */
				iterator.remove();
				hike_contacts_by_id.remove(id);
			} else {
				/* item is different than our db, so send an update */
				hike_contacts_by_id.remove(id);
			}
		}

		/* our address object should an update dictionary, and a list of IDs to remove */

		/* return early if things are in sync */
		if ((new_contacts_by_id.isEmpty()) && (hike_contacts_by_id.isEmpty()))
		{
			Log.d("ContactUtils", "DB in sync");
			db.close();
			return;
		}

		try
		{
			List<ContactInfo> contacts = AccountUtils.updateAddressBook(new_contacts_by_id, hike_contacts_by_id.keySet());
			for (ContactInfo contactInfo : contacts)
			{
				db.addContact(contactInfo);
			}
		} catch(Exception e)
		{
			Log.e("ContactUtils", "error updating addressbook", e);
		} finally
		{
			db.close();
			db = null;
		}
	}

	private static Map<String, Set<ContactInfo>> convertToMap(List<ContactInfo> contacts)
	{
		Map<String, Set<ContactInfo>> ret = new HashMap<String, Set<ContactInfo>>(contacts.size());
		for (ContactInfo contactInfo : contacts)
		{
			if ("__TD_HIKE__".equals(contactInfo.id))
			{
				continue;
			}

			Set<ContactInfo> l = ret.get(contactInfo.id);
			if (l == null)
			{
				l = new HashSet<ContactInfo>();
				ret.put(contactInfo.id, l);
			}

			l.add(contactInfo);
		}

		return ret;
	}

	public static List<ContactInfo> getContacts(Context ctx)
	{
		String[] projection = new String[] { ContactsContract.Contacts._ID, ContactsContract.Contacts.HAS_PHONE_NUMBER, ContactsContract.Contacts.DISPLAY_NAME };

		String selection = ContactsContract.Contacts.HAS_PHONE_NUMBER + "='1'";
		Cursor contacts = ctx.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, projection, selection, null, null);

		int idFieldColumnIndex = contacts.getColumnIndex(ContactsContract.Contacts._ID);
		int nameFieldColumnIndex = contacts.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
		List<ContactInfo> contactinfos = new ArrayList<ContactInfo>();
		Log.d("ContactUtils", "Starting to scan address book");
		long tm = System.currentTimeMillis();
		Map<String, String> contactNames = new HashMap<String, String>();
		while (contacts.moveToNext())
		{
			String id = contacts.getString(idFieldColumnIndex);
			String name = contacts.getString(nameFieldColumnIndex);
			contactNames.put(id, name);
		}

		contacts.close();

		Cursor phones = ctx.getContentResolver().query(Phone.CONTENT_URI, new String[] { Phone.CONTACT_ID, Phone.NUMBER }, null, null, null);

		int numberColIdx = phones.getColumnIndex(Phone.NUMBER);
		int idColIdx = phones.getColumnIndex(Phone.CONTACT_ID);
		while (phones.moveToNext())
		{
			String number = phones.getString(numberColIdx);
			String id = phones.getString(idColIdx);
			String name = contactNames.get(id);
			if ((name != null) && (number != null))
			{
				contactinfos.add(new ContactInfo(id, number, name));
			}
		}

		phones.close();
		Log.d("ContactUtils", "Scanning address book took " + (System.currentTimeMillis() - tm)/1000 + " seconds for " + contactinfos.size() + " entries");
		return contactinfos;
	}
}
