package com.bsb.hike.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;
import android.util.Pair;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;

public class ContactUtils
{
	/*
	 * Call this when we think the address book has changed. Checks for updates, posts to the server, writes them to the local database and updates existing conversations
	 */
	public static void syncUpdates(Context ctx)
	{

		if (!Utils.isUserOnline(ctx))
		{
			Logger.d("CONTACT UTILS", "Airplane mode is on , skipping sync update tasks.");
			return;
		}
		HikeUserDatabase db = HikeUserDatabase.getInstance();

		List<ContactInfo> newContacts = getContacts(ctx);
		if (newContacts == null)
		{
			return;
		}

		Map<String, List<ContactInfo>> new_contacts_by_id = convertToMap(newContacts);
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

		/*
		 * our address object should an update dictionary, and a list of IDs to remove
		 */

		/* return early if things are in sync */
		if ((new_contacts_by_id.isEmpty()) && (hike_contacts_by_id.isEmpty()))
		{
			Logger.d("ContactUtils", "DB in sync");
			return;
		}

		try
		{
			JSONArray ids_json = new JSONArray();
			for (String string : hike_contacts_by_id.keySet())
			{
				ids_json.put(string);
			}
			Logger.d("ContactUtils", "New contacts:" + new_contacts_by_id.size() + " DELETED contacts: " + ids_json.length());
			List<ContactInfo> updatedContacts = AccountUtils.updateAddressBook(new_contacts_by_id, ids_json);

			/* Delete ids from hike user DB */
			db.deleteMultipleRows(hike_contacts_by_id.keySet()); // this will
																	// delete
																	// all rows
																	// in
																	// HikeUser
																	// DB that
																	// are not
																	// in
																	// Addressbook.
			db.updateContacts(updatedContacts);

		}
		catch (Exception e)
		{
			Logger.e("ContactUtils", "error updating addressbook", e);
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
				/*
				 * Linked list is used because removal using iterator is O(1) in linked list vs O(n) in Arraylist
				 */
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
		Cursor contacts = null;

		List<ContactInfo> contactinfos = new ArrayList<ContactInfo>();
		Map<String, String> contactNames = new HashMap<String, String>();
		try
		{
			contacts = ctx.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, projection, selection, null, null);

			/*
			 * Added this check for an issue where the cursor is null in some random cases (We suspect that happens when hotmail contacts are synced.)
			 */
			if (contacts == null)
			{
				return null;
			}

			int idFieldColumnIndex = contacts.getColumnIndex(ContactsContract.Contacts._ID);
			int nameFieldColumnIndex = contacts.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
			Logger.d("ContactUtils", "Starting to scan address book");
			while (contacts.moveToNext())
			{
				String id = contacts.getString(idFieldColumnIndex);
				String name = contacts.getString(nameFieldColumnIndex);
				contactNames.put(id, name);
			}
		}
		finally
		{
			if (contacts != null)
			{
				contacts.close();
			}
		}

		Cursor phones = null;

		try
		{
			phones = ctx.getContentResolver().query(Phone.CONTENT_URI, new String[] { Phone.CONTACT_ID, Phone.NUMBER }, null, null, null);
			/*
			 * Added this check for an issue where the cursor is null in some random cases (We suspect that happens when hotmail contacts are synced.)
			 */
			if (phones == null)
			{
				return null;
			}

			int numberColIdx = phones.getColumnIndex(Phone.NUMBER);
			int idColIdx = phones.getColumnIndex(Phone.CONTACT_ID);

			while (phones.moveToNext())
			{
				String number = phones.getString(numberColIdx);
				String id = phones.getString(idColIdx);
				String name = contactNames.get(id);
				if ((name != null) && (number != null))
				{
					if (contactsToStore.add("_" + name + "_" + number)) // if
																		// this
																		// element
																		// is
																		// added
																		// successfully
																		// , it
																		// returns
																		// true
					{
						contactinfos.add(new ContactInfo(id, null, name, number));
					}
				}
			}
		}
		finally
		{
			if (phones != null)
			{
				phones.close();

			}
		}

		/*
		 * We will catch exceptions here since we do not know which devices support this URI.
		 */
		Cursor cursorSim = null;
		try
		{
			Uri simUri = Uri.parse("content://icc/adn");
			cursorSim = ctx.getContentResolver().query(simUri, null, null, null, null);

			while (cursorSim.moveToNext())
			{
				try
				{
					String id = cursorSim.getString(cursorSim.getColumnIndex("_id"));
					String name = cursorSim.getString(cursorSim.getColumnIndex("name"));
					String number = cursorSim.getString(cursorSim.getColumnIndex("number"));
					if ((name != null) && (number != null))
					{
						if (contactsToStore.add("_" + name + "_" + number)) // if
																			// this
																			// element
																			// is
																			// added
																			// successfully
																			// ,
																			// it
																			// returns
																			// true
						{
							contactinfos.add(new ContactInfo(id, null, name, number));
						}
					}
				}
				catch (Exception e)
				{
					Logger.w("ContactUtils", "Expection while adding sim contacts", e);
				}
			}
		}
		catch (Exception e)
		{
			Logger.w("ContactUtils", "Expection while querying for sim contacts", e);
		}
		finally
		{
			if (cursorSim != null)
			{
				cursorSim.close();
			}
		}

		return contactinfos;
	}

	public static int updateHikeStatus(Context ctx, String msisdn, boolean onhike)
	{
		HikeUserDatabase db = HikeUserDatabase.getInstance();
		return db.updateHikeContact(msisdn, onhike);
	}

	/**
	 * Used to get the recent contacts where we get the recency from the android contacts table. This method also returns a string which can be used as the argument to a SELECT IN
	 * query
	 * 
	 * @param context
	 * @param limit
	 * @return
	 */
	public static Pair<String, Map<String, Long>> getRecentNumbers(Context context, int limit)
	{
		Cursor c = null;
		try
		{
			String sortBy = limit > -1 ? Phone.LAST_TIME_CONTACTED + " DESC LIMIT " + limit : null;
			c = context.getContentResolver().query(Phone.CONTENT_URI, new String[] { Phone.NUMBER, Phone.LAST_TIME_CONTACTED }, null, null, sortBy);

			Map<String, Long> recentlyContactedNumbers = new HashMap<String, Long>();

			StringBuilder sb = null;

			if (c != null && c.getCount() > 0)
			{
				int numberColIdx = c.getColumnIndex(Phone.NUMBER);
				int lastTimeContactedIdx = c.getColumnIndex(Phone.LAST_TIME_CONTACTED);

				sb = new StringBuilder("(");
				while (c.moveToNext())
				{
					String number = c.getString(numberColIdx);

					if (TextUtils.isEmpty(number))
					{
						continue;
					}

					long lastTimeContacted = c.getLong(lastTimeContactedIdx);

					/*
					 * Checking if we already have this number and whether the last time contacted was sooner than the newer value.
					 */
					if (recentlyContactedNumbers.containsKey(number) && recentlyContactedNumbers.get(number) > lastTimeContacted)
					{
						continue;
					}
					recentlyContactedNumbers.put(number, c.getLong(lastTimeContactedIdx));

					number = DatabaseUtils.sqlEscapeString(number);
					sb.append(number + ",");
				}
				sb.replace(sb.length() - 1, sb.length(), ")");
			}
			else
			{
				sb = new StringBuilder("()");
			}

			return new Pair<String, Map<String, Long>>(sb.toString(), recentlyContactedNumbers);
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
	 * This method will give us the user's most contacted contacts. We also try to get the greenblue contacts if the user has them synced and then sort those based on times
	 * contacts.
	 */
	public static Pair<String, Map<String, Integer>> getMostContactedContacts(Context context, int limit)
	{
		Cursor greenblueContactsCursor = null;
		Cursor phoneContactsCursor = null;
		Cursor otherContactsCursor = null;

		try
		{
			String[] projection = new String[] { ContactsContract.RawContacts.CONTACT_ID };

			String selection = ContactsContract.RawContacts.ACCOUNT_TYPE + "= 'com.whatsapp'";
			greenblueContactsCursor = context.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, projection, selection, null, null);

			int id = greenblueContactsCursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID);

			StringBuilder greenblueContactIds = null;

			if (greenblueContactsCursor.getCount() > 0)
			{
				greenblueContactIds = new StringBuilder("(");

				while (greenblueContactsCursor.moveToNext())
				{
					greenblueContactIds.append(greenblueContactsCursor.getInt(id) + ",");
					String msisdn = greenblueContactsCursor.getInt(id)+"";
					if(isIndianMobileNumber(msisdn))
					{
					greenblueContactIds.append(msisdn + ",");
					}
				}
				greenblueContactIds.replace(greenblueContactIds.lastIndexOf(","), greenblueContactIds.length(), ")");
			}

			String[] newProjection = new String[] { Phone.NUMBER, Phone.TIMES_CONTACTED };
			String newSelection = greenblueContactIds != null ? (Phone.CONTACT_ID + " IN " + greenblueContactIds.toString()) : null;

			phoneContactsCursor = context.getContentResolver().query(Phone.CONTENT_URI, newProjection, newSelection, null, Phone.TIMES_CONTACTED + " DESC LIMIT " + limit);

			Map<String, Integer> mostContactedNumbers = new HashMap<String, Integer>();
			StringBuilder sb = null;

			if (phoneContactsCursor.getCount() > 0)
			{
				sb = new StringBuilder("(");

				extractContactInfo(phoneContactsCursor, sb, mostContactedNumbers, true);

			}
			/*
			 * This number is required when the user does not have enough greenblue contacts.
			 */
			int otherContactsRequired = limit - mostContactedNumbers.size();

			if (otherContactsRequired > 0)
			{
				if (greenblueContactIds != null)
				{
					newSelection = Phone.CONTACT_ID + " NOT IN " + greenblueContactIds.toString();
				}
				else
				{
					newSelection = null;
				}

				otherContactsCursor = context.getContentResolver().query(Phone.CONTENT_URI, newProjection, newSelection, null,
						Phone.TIMES_CONTACTED + " DESC LIMIT " + otherContactsRequired);

				if (otherContactsCursor.getCount() > 0)
				{
					if (sb == null)
					{
						sb = new StringBuilder("(");
					}
					extractContactInfo(otherContactsCursor, sb, mostContactedNumbers, false);
				}
			}

			if (mostContactedNumbers.isEmpty())
			{
				sb = new StringBuilder("()");
			}
			else
			{
				sb.replace(sb.length() - 1, sb.length(), ")");
			}

			return new Pair<String, Map<String, Integer>>(sb.toString(), mostContactedNumbers);

		}
		finally
		{
			if (greenblueContactsCursor != null)
			{
				greenblueContactsCursor.close();
			}
			if (phoneContactsCursor != null)
			{
				phoneContactsCursor.close();
			}
			if (otherContactsCursor != null)
			{
				otherContactsCursor.close();
			}
		}
	}
	
	public static boolean isIndianMobileNumber(String number)
	{
		if (HikeMessengerApp.isIndianUser())
		{
			Pattern pattern = Pattern.compile("^(?:(?:\\+|0{0,2})91(\\s*[\\-]\\s*)?|[0]?)?[789]\\d{9}$");
			Matcher matcher = pattern.matcher(number);
			if (matcher.matches())
				return true;
		}else{
			return true;
		}

		return false;
	}

	private static void extractContactInfo(Cursor c, StringBuilder sb, Map<String, Integer> numbers, boolean greenblueContacts)
	{
		int numberColIdx = c.getColumnIndex(Phone.NUMBER);
		int timesContactedIdx = c.getColumnIndex(Phone.TIMES_CONTACTED);

		while (c.moveToNext())
		{
			String number = c.getString(numberColIdx);

			if (TextUtils.isEmpty(number))
			{
				continue;
			}
			if(isIndianMobileNumber(number)){
			
			/*
			 * We apply a multiplier of 2 for greenblue contacts to give them a greater weight.
			 */
			int lastTimeContacted = greenblueContacts ? 2 * c.getInt(timesContactedIdx) : c.getInt(timesContactedIdx);

			/*
			 * Checking if we already have this number and whether the last time contacted was sooner than the newer value.
			 */
			if (numbers.containsKey(number) && numbers.get(number) > lastTimeContacted)
			{
				continue;
			}
			numbers.put(number, lastTimeContacted);

			number = DatabaseUtils.sqlEscapeString(number);
			sb.append(number + ",");
			}
				
		}
	}

	public static void setGreenBlueStatus(Context context, List<ContactInfo> contactinfos)
	{
		Cursor greenblueContactsCursor = null;
		Cursor phoneContactsCursor = null;
		try
		{
			String[] projection = new String[] { ContactsContract.RawContacts.CONTACT_ID };

			String selection = ContactsContract.RawContacts.ACCOUNT_TYPE + "= 'com.whatsapp'";
			greenblueContactsCursor = context.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, projection, selection, null, null);

			/*
			 * We were getting this cursor as null for some reason (saw crashes on the dev console).
			 */
			if (greenblueContactsCursor == null)
			{
				return;
			}

			int id = greenblueContactsCursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID);

			StringBuilder greenblueContactIds = null;
			if (greenblueContactsCursor.getCount() > 0)
			{
				greenblueContactIds = new StringBuilder("(");

				while (greenblueContactsCursor.moveToNext())
				{
					greenblueContactIds.append(greenblueContactsCursor.getInt(id) + ",");
				}
				greenblueContactIds.replace(greenblueContactIds.lastIndexOf(","), greenblueContactIds.length(), ")");
			}

			if (greenblueContactIds != null)
			{
				String[] newProjection = new String[] { Phone.NUMBER, Phone.DISPLAY_NAME };
				String newSelection = (Phone.CONTACT_ID + " IN " + greenblueContactIds.toString());

				phoneContactsCursor = context.getContentResolver().query(Phone.CONTENT_URI, newProjection, newSelection, null, Phone.NUMBER + " DESC");

				if (phoneContactsCursor.getCount() > 0)
				{
					setGreenBlueContacs(phoneContactsCursor, contactinfos);
				}
			}

		}
		finally
		{
			if (greenblueContactsCursor != null)
			{
				greenblueContactsCursor.close();
			}
			if (phoneContactsCursor != null)
			{
				phoneContactsCursor.close();
			}
		}
	}

	private static void setGreenBlueContacs(Cursor c, List<ContactInfo> contactinfos)
	{
		int numberColIdx = c.getColumnIndex(Phone.NUMBER);
		HashSet<String> greenBlueContacts = new HashSet<String>(c.getCount());
		while (c.moveToNext())
		{
			String number = c.getString(numberColIdx);
			greenBlueContacts.add(number);
		}

		for (ContactInfo contact : contactinfos)
		{
			if (greenBlueContacts.contains(contact.getPhoneNum()))
			{
				contact.setOnGreenBlue(true);
			}
		}
	}

}
