package com.bsb.hike.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;

import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;

public class ContactUtils
{
	/**
	 * Gets the mobile number for a contact. If there's no mobile number, gets the default one
	 */
	public static ContactInfo getContactInfoFromURI(Context context, Uri contact)
	{
		Cursor cursor = context.getContentResolver().query(contact, new String[] { "_id" }, null, null, null);
		if ((cursor == null) || (!cursor.moveToFirst()))
		{
			Log.w("ContactUtils", "No contact found: " + contact);
			if (cursor != null)
			{
				cursor.close();
			}
			return null;
		}

		String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
		cursor.close();

		HikeUserDatabase db = HikeUserDatabase.getInstance();
		ContactInfo contactInfo = db.getContactInfoFromId(contactId);
		if (contactInfo == null)
		{
			Cursor phones = context.getContentResolver().query(Phone.CONTENT_URI, null, Phone.CONTACT_ID + " = " + contactId, null, null);
			String number = null;
			while ((phones != null) && (phones.moveToNext()))
			{
				number = phones.getString(phones.getColumnIndex(Phone.NUMBER));
				int type = phones.getInt(phones.getColumnIndex(Phone.TYPE));
				Log.d("ContactUtils", "Number is " + number);
				switch (type)
				{
				case Phone.TYPE_MOBILE:
					break;
				}
			}

			if (phones != null)
			{
				phones.close();
			}

			if (number != null)
			{
				contactInfo = db.getContactInfoFromPhoneNo(number);
			}
		}
		return contactInfo;
	}

	/*
	 * Call this when we think the address book has changed. Checks for updates, posts to the server, writes them to the local database and updates existing conversations
	 */
	public static void syncUpdates(Context ctx)
	{

		if(!Utils.isUserOnline(ctx))
		{
			Log.d("CONTACT UTILS", "Airplane mode is on , skipping sync update tasks.");
			return;
		}
		HikeUserDatabase db = HikeUserDatabase.getInstance();

		Map<String, List<ContactInfo>> new_contacts_by_id = convertToMap(getContacts(ctx));
		Map<String, List<ContactInfo>> hike_contacts_by_id = convertToMap(db.getContacts(false));

		/*
		 * iterate over every item in the phone db, items that are equal remove from both maps items that are different, leave in 'new' map and remove from 'hike' map send the
		 * 'new' map as items to add, and send the 'hike' map as IDs to remove
		 */
		Map.Entry<String, List<ContactInfo>> entry = null;
		for (Iterator<Map.Entry<String, List<ContactInfo>>> iterator = new_contacts_by_id.entrySet().iterator(); iterator.hasNext();)
		{
			entry = iterator.next();
			String id = entry.getKey();
			List<ContactInfo> contacts_for_id = entry.getValue();
			List<ContactInfo> hike_contacts_for_id = hike_contacts_by_id.get(id);

			/*
			 * If id is not present in hike user DB i.e new contact is added to Phone AddressBook. When the items are the same, we remove the item @ the current iterator. This will
			 * result in the item *not* being sent to the server
			 */
			if (hike_contacts_for_id == null)
			{
				continue;
			}
			else if (areListsEqual(contacts_for_id, hike_contacts_for_id))
			{
				/* hike db is up to date, so don't send update */
				iterator.remove();
				hike_contacts_by_id.remove(id);
				continue;
			}
			/* item is different than our db, so send an update */
			hike_contacts_by_id.remove(id);
		}

		/* our address object should an update dictionary, and a list of IDs to remove */

		/* return early if things are in sync */
		if ((new_contacts_by_id.isEmpty()) && (hike_contacts_by_id.isEmpty()))
		{
			Log.d("ContactUtils", "DB in sync");
			return;
		}

		try
		{
			JSONArray ids_json = new JSONArray();
			for (String string : hike_contacts_by_id.keySet())
			{
				ids_json.put(string);
			}
			List<ContactInfo> updatedContacts = AccountUtils.updateAddressBook(new_contacts_by_id, ids_json);

			/* Delete ids from hike user DB */
			db.deleteMultipleRows(hike_contacts_by_id.keySet()); // this will delete all rows in HikeUser DB that are not in Addressbook.
			db.updateContacts(updatedContacts);

		}
		catch (Exception e)
		{
			Log.e("ContactUtils", "error updating addressbook", e);
		}
	}

	private static boolean areListsEqual(List<ContactInfo> list1, List<ContactInfo> list2)
	{
		if (list1 != null && list2 != null)
		{
			if (list1.size() != list2.size())
				return false;
			else if (list1.size() == 0 && list2.size() == 0)
			{
				return false;
			}
			else
			// represents same number of elements
			{
				/* compare each element */
				HashSet<ContactInfo> set1 = new HashSet<ContactInfo>(list1.size());
				for (ContactInfo c : list1)
				{
					set1.add(c);
				}
				boolean flag = true;
				for (ContactInfo c : list2)
				{
					if (!set1.contains(c))
					{
						flag = false;
						break;
					}
				}
				return flag;
			}
		}
		else
		{
			return false;
		}
	}

	public static Map<String, List<ContactInfo>> convertToMap(List<ContactInfo> contacts)
	{
		Map<String, List<ContactInfo>> ret = new HashMap<String, List<ContactInfo>>(contacts.size());
		for (ContactInfo contactInfo : contacts)
		{
			if ("__HIKE__".equals(contactInfo.getId()))
			{
				continue;
			}

			List<ContactInfo> l = ret.get(contactInfo.getId());
			if (l == null)
			{
				/* Linked list is used because removal using iterator is O(1) in linked list vs O(n) in Arraylist */
				l = new LinkedList<ContactInfo>();
				ret.put(contactInfo.getId(), l);
			}
			l.add(contactInfo);
		}

		return ret;
	}

	public static List<ContactInfo> getContacts(Context ctx)
	{
		HashSet<String> contactsToStore = new HashSet<String>();
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
				if (contactsToStore.add("_" + name + "_" + number)) // if this element is added successfully , it returns true
				{
					contactinfos.add(new ContactInfo(id, null, name, number));
				}
			}
		}

		phones.close();

		return contactinfos;
	}

	public static void updateHikeStatus(Context ctx, String msisdn, boolean onhike)
	{
		HikeUserDatabase db = HikeUserDatabase.getInstance();
		db.updateHikeContact(msisdn, onhike);
	}
}
