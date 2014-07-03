package com.bsb.hike.modules.contactmgr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.util.Pair;

import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.modules.contactmgr.db.HikeUserDatabase;

public class TransientCache extends ContactsCache
{
	// Transient Memory for contacts that are saved in address book with their reference count
	private Map<String, ContactTuple> savedContacts; // TODO name field not needed here

	// Transient Memory for contacts that are not saved in address book with their reference count
	private Map<String, ContactTuple> unsavedContacts;

	// Transient memory for contacts of group participants
	private Map<String, List<String>> groupParticipants;

	private final ReentrantReadWriteLock readWriteLockTrans = new ReentrantReadWriteLock(true);

	private final Lock readLockTrans = readWriteLockTrans.readLock();

	private final Lock writeLockTrans = readWriteLockTrans.writeLock();

	/**
	 * 
	 */
	TransientCache()
	{
		savedContacts = new LinkedHashMap<String, ContactTuple>();
		unsavedContacts = new HashMap<String, ContactTuple>();
		groupParticipants = new HashMap<String, List<String>>();
	}

	/**
	 * get contact info from memory. Returns null if not found in memory
	 * 
	 * @param key
	 * @return
	 */
	ContactInfo getContact(String key)
	{
		readLockTrans.lock();
		try
		{
			ContactTuple tuple = savedContacts.get(key);
			if (null == tuple)
			{
				tuple = unsavedContacts.get(key);
				if (null == tuple)
					return null;
			}
			return tuple.getContact();
		}
		finally
		{
			readLockTrans.unlock();
		}
	}

	/**
	 * inserts contact in saved contacts map. Make sure that it is not already in memory , we are setting reference count to 1 here
	 * 
	 * @param contact
	 *            contactinfo to put in map
	 */
	void insertContact(ContactInfo contact)
	{
		writeLockTrans.lock();
		try
		{
			ContactTuple tuple = new ContactTuple(1, null, contact);
			savedContacts.put(contact.getMsisdn(), tuple);
		}
		finally
		{
			writeLockTrans.unlock();
		}
	}

	/**
	 * inserts contact in unsaved contacts map. Make sure that it is not already in memory , we are setting reference count to 1 here
	 * 
	 * @param contact
	 *            contactinfo to put in map
	 * @param name
	 *            name if contact is not saved in address book
	 */
	void insertContact(ContactInfo contact, String name)
	{
		writeLockTrans.lock();
		try
		{
			ContactTuple tuple = new ContactTuple(1, name, contact);
			unsavedContacts.put(contact.getMsisdn(), tuple);
		}
		finally
		{
			writeLockTrans.unlock();
		}
	}

	/**
	 * Removes this contact from memory if reference count is one otherwise decrements the reference count
	 * 
	 * @param contact
	 */
	void removeContact(String msisdn)
	{
		writeLockTrans.lock();
		try
		{
			ContactTuple tuple = savedContacts.get(msisdn);
			if (null != tuple)
			{
				tuple.setReferenceCount(tuple.getReferenceCount() - 1);
				if (tuple.getReferenceCount() == 0)
				{
					savedContacts.remove(msisdn);
				}
			}
			else
			{
				tuple = unsavedContacts.get(msisdn);
				if (null != tuple)
				{
					tuple.setReferenceCount(tuple.getReferenceCount() - 1);
					if (tuple.getReferenceCount() == 0)
					{
						unsavedContacts.remove(msisdn);
					}
				}
			}
		}
		finally
		{
			writeLockTrans.unlock();
		}
	}

	/**
	 * updates the contact in memory
	 * 
	 * @param contact
	 */
	void updateContact(ContactInfo contact)
	{
		writeLockTrans.lock();
		try
		{
			if (savedContacts.containsKey(contact.getMsisdn()))
			{
				ContactTuple tuple = savedContacts.get(contact.getMsisdn());
				tuple.setContact(contact);
			}
			else if (unsavedContacts.containsKey(contact.getMsisdn()))
			{
				ContactTuple tuple = unsavedContacts.get(contact.getMsisdn());
				tuple.setContact(contact);
			}
		}
		finally
		{
			writeLockTrans.unlock();
		}
	}

	/**
	 * updates the contact in memory with contact info object and name if saved contact is deleted from address book or unknown contact is saved
	 * 
	 * @param contact
	 * @param name
	 */
	void updateContact(ContactInfo contact, String name)
	{
		writeLockTrans.lock();
		try
		{
			if (savedContacts.containsKey(contact.getMsisdn()))
			{
				ContactTuple tuple = savedContacts.get(contact.getMsisdn());
				tuple.setContact(contact);
				tuple.setName(name);
			}
			else if (unsavedContacts.containsKey(contact.getMsisdn()))
			{
				ContactTuple tuple = unsavedContacts.get(contact.getMsisdn());
				tuple.setContact(contact);
				tuple.setName(name);
			}
		}
		finally
		{
			writeLockTrans.unlock();
		}
	}

	List<ContactInfo> getAllContacts()
	{
		if (!allContactsLoaded)
		{
			loadMemory();
			allContactsLoaded = true;
		}

		List<ContactInfo> allContacts = new ArrayList<ContactInfo>();
		// Traverse through savedContacts and add in allCOntacts list
		for (Entry<String, ContactTuple> mapEntry : savedContacts.entrySet())
		{
			ContactTuple tuple = mapEntry.getValue();
			allContacts.add(tuple.getContact());
		}

		// then traverse through unsaved and add only whose favorite type is not null
		for (Entry<String, ContactTuple> mapEntry : unsavedContacts.entrySet())
		{
			ContactTuple tuple = mapEntry.getValue();
			FavoriteType favType = tuple.getContact().getFavoriteType();
			if (null != favType)
			{
				allContacts.add(tuple.getContact());
			}
		}

		return allContacts;
	}

	/**
	 * 
	 * @param msisdn
	 * @return
	 */
	String getName(String msisdn)
	{
		ContactInfo c = getContact(msisdn);
		if (null == c)
			return null;
		return c.getName();
	}

	/**
	 * 
	 * @param msisdn
	 * @param name
	 */
	void setUnknownContactName(String msisdn, String name)
	{
		ContactTuple tuple = unsavedContacts.get(msisdn);
		if (null != tuple)
		{
			tuple.setName(name);
		}
	}

	/**
	 * This function will load all the contacts from DB into transient storage. Contacts which are in persistence map will not be loaded into it
	 */
	void loadMemory()
	{
		Pair<Map<String, ContactInfo>, Map<String, ContactInfo>> map = HikeUserDatabase.getInstance().getAllContactInfo();

		Map<String, ContactInfo> savedcontactmap = map.first;

		Map<String, ContactInfo> unsavedcontactmap = map.second;

		LinkedHashMap<String, ContactTuple> temp = new LinkedHashMap<String, ContactTuple>();

		for (Entry<String, ContactInfo> mapEntry : savedcontactmap.entrySet())
		{
			String msisdn = mapEntry.getKey();
			ContactInfo contact = mapEntry.getValue();
			ContactTuple tuple = savedContacts.get(msisdn);
			if (null != tuple)
			{
				tuple.setContact(contact);
				temp.put(msisdn, tuple);
			}
			else
			{
				tuple = new ContactTuple(1, null, contact);
				temp.put(msisdn, tuple);
			}
		}

		savedContacts = temp;

		for (Entry<String, ContactInfo> mapEntry : unsavedcontactmap.entrySet())
		{
			ContactInfo contact = mapEntry.getValue();
			insertContact(contact, null);
		}
	}

	/**
	 * Loads the contact info for a msisdn in transient memory and returns the same
	 * 
	 * @param msisdn
	 * @param ifNotFoundReturnNull
	 * @return
	 */
	ContactInfo loadMemory(String msisdn, boolean ifNotFoundReturnNull)
	{
		ContactInfo c = HikeUserDatabase.getInstance().getContactInfoFromMSISDN(msisdn, ifNotFoundReturnNull);
		if (null != c.getName())
		{
			insertContact(c);
		}
		else
		{
			insertContact(c, null);
		}
		return c;
	}

	/**
	 * This method loads the contactInfo of msisdns in transient memory and returns the list of same
	 * 
	 * @param msisdns
	 * @return
	 */
	List<ContactInfo> loadMemory(List<String> msisdns)
	{
		if (msisdns.size() > 0)
		{
			Map<String, ContactInfo> map = HikeUserDatabase.getInstance().getContactInfoFromMsisdns(msisdns, true);

			for (Entry<String, ContactInfo> mapEntry : map.entrySet())
			{
				ContactInfo contact = mapEntry.getValue();
				if (null == contact.getName())
				{
					insertContact(contact, null);
				}
				else
				{
					insertContact(contact);
				}
			}

			return new ArrayList<ContactInfo>(map.values());
		}
		return null;
	}

	/**
	 * 
	 * @param onHike
	 * @param myMsisdn
	 * @param nativeSMSOn
	 * @param ignoreUnknownContacts
	 * @return
	 */
	List<ContactInfo> getNOTFRIENDScontacts(int onHike, String myMsisdn, boolean nativeSMSOn, boolean ignoreUnknownContacts)
	{
		List<ContactInfo> contacts = new ArrayList<ContactInfo>();

		if (allContactsLoaded)
		{
			Set<String> blockSet = HikeUserDatabase.getInstance().getBlockedMsisdnSet();

			for (Entry<String, ContactTuple> savedMapEntry : savedContacts.entrySet())
			{
				String msisdn = savedMapEntry.getKey();
				if (!blockSet.contains(msisdn) && !msisdn.equals(myMsisdn))
				{
					ContactTuple tuple = savedMapEntry.getValue();
					contacts.add(tuple.getContact());
				}
			}

			if (!ignoreUnknownContacts)
			{
				for (Entry<String, ContactTuple> unsavedMapEntry : unsavedContacts.entrySet())
				{
					String msisdn = unsavedMapEntry.getKey();
					if (!blockSet.contains(msisdn) && !msisdn.equals(myMsisdn))
					{
						ContactTuple tuple = unsavedMapEntry.getValue();
						contacts.add(tuple.getContact());
					}
				}
			}
		}
		else
		{
			Map<String, ContactInfo> map = HikeUserDatabase.getInstance().getNOTFRIENDScontactsFromDB(onHike, myMsisdn, nativeSMSOn, ignoreUnknownContacts);
			if (map != null)
			{
				for (Entry<String, ContactInfo> mapEntry : map.entrySet())
				{
					String msisdn = mapEntry.getKey();
					ContactInfo contact = mapEntry.getValue();
					if (getContact(msisdn) == null)
					{
						if (null == contact.getName())
						{
							insertContact(contact, null);
						}
						else
						{
							insertContact(contact);
						}
					}
					contacts.add(contact);
				}
			}
		}
		return contacts;
	}

	/**
	 * 
	 * @param favoriteType
	 * @param onHike
	 * @param myMsisdn
	 * @param nativeSMSOn
	 * @param ignoreUnknownContacts
	 * @return
	 */
	List<ContactInfo> getContactsOfFavoriteType(FavoriteType[] favoriteType, int onHike, String myMsisdn, boolean nativeSMSOn, boolean ignoreUnknownContacts)
	{
		// TODO first check if all contacts are loaded

		Map<String, ContactInfo> map = HikeUserDatabase.getInstance().getContactsOfFavoriteTypeDB(favoriteType, onHike, myMsisdn, nativeSMSOn, ignoreUnknownContacts);

		List<ContactInfo> contacts = new ArrayList<ContactInfo>();

		if (map != null)
		{
			for (Entry<String, ContactInfo> mapEntry : map.entrySet())
			{
				String msisdn = mapEntry.getKey();
				ContactInfo contact = mapEntry.getValue();
				if (getContact(msisdn) == null)
				{
					if (null == contact.getName())
					{
						insertContact(contact, null);
					}
					else
					{
						insertContact(contact);
					}
				}
				contacts.add(contact);
			}
		}

		return contacts;
	}

	/**
	 * 
	 * @param limit
	 * @param msisdnsIn
	 * @param msisdnsNotIn
	 * @param myMsisdn
	 * @return
	 */
	List<ContactInfo> getHikeContacts(int limit, String msisdnsIn, String msisdnsNotIn, String myMsisdn)
	{
		// TODO first check if all contacts are loaded

		List<ContactInfo> contacts = HikeUserDatabase.getInstance().getHikeContacts(limit, msisdnsIn, msisdnsNotIn, myMsisdn);
		for (ContactInfo contact : contacts)
		{
			if (null == getContact(contact.getMsisdn()))
			{
				if (null == contact.getName())
				{
					insertContact(contact, null);
				}
				else
				{
					insertContact(contact);
				}
			}
		}
		return contacts;
	}

	/**
	 * 
	 * @return
	 */
	List<Pair<AtomicBoolean, ContactInfo>> getNonHikeContacts()
	{
		List<Pair<AtomicBoolean, ContactInfo>> contacts = new ArrayList<Pair<AtomicBoolean, ContactInfo>>();
		if (allContactsLoaded)
		{
			Set<String> blockSet = HikeUserDatabase.getInstance().getBlockedMsisdnSet();

			for (Entry<String, ContactTuple> savedMapEntry : savedContacts.entrySet())
			{
				String msisdn = savedMapEntry.getKey();
				if (!blockSet.contains(msisdn))
				{
					ContactTuple tuple = savedMapEntry.getValue();
					ContactInfo contactInfo = tuple.getContact();
					if (!contactInfo.isOnhike())
					{
						contacts.add(new Pair<AtomicBoolean, ContactInfo>(new AtomicBoolean(false), contactInfo));
					}
				}
			}
		}
		else
		{
			contacts = HikeUserDatabase.getInstance().getNonHikeContacts();

			for (Pair<AtomicBoolean, ContactInfo> p : contacts)
			{
				ContactInfo contact = p.second;
				if (null == getContact(contact.getMsisdn()))
				{
					if (null == contact.getName())
					{
						insertContact(contact, null);
					}
					else
					{
						insertContact(contact);
					}
				}
			}
		}
		return contacts;
	}

	/**
	 * 
	 * @param limit
	 * @return
	 */
	List<ContactInfo> getNonHikeMostContactedContacts(int limit)
	{
		// TODO first check if all contacts are loaded

		List<ContactInfo> contacts = HikeUserDatabase.getInstance().getNonHikeMostContactedContacts(limit);
		for (ContactInfo contact : contacts)
		{
			if (null == getContact(contact.getMsisdn()))
			{
				if (null == contact.getName())
				{
					insertContact(contact, null);
				}
				else
				{
					insertContact(contact);
				}
			}
		}
		return contacts;
	}

	/**
	 * 
	 * @param number
	 * @return
	 */
	public ContactInfo getContactInfoFromPhoneNo(String number)
	{
		// TODO first check if all contacts are loaded

		ContactInfo contact = HikeUserDatabase.getInstance().getContactInfoFromPhoneNo(number);
		if (null == getContact(contact.getMsisdn()))
		{
			if (null == contact.getName())
			{
				insertContact(contact, null);
			}
			else
			{
				insertContact(contact);
			}
		}
		return contact;
	}

	/**
	 * 
	 * @param number
	 * @return
	 */
	public ContactInfo getContactInfoFromPhoneNoOrMsisdn(String number)
	{
		// TODO first check if all contacts are loaded

		ContactInfo contact = HikeUserDatabase.getInstance().getContactInfoFromPhoneNoOrMsisdn(number);
		if (null == getContact(contact.getMsisdn()))
		{
			if (null == contact.getName())
			{
				insertContact(contact, null);
			}
			else
			{
				insertContact(contact);
			}
		}
		return contact;
	}

	/**
	 * Updates the contact by setting the name to null and move it from saved to unsaved contacts map
	 * 
	 * @param contact
	 */
	void contactDeleted(ContactInfo contact)
	{
		writeLockTrans.lock();
		try
		{
			ContactTuple tuple = savedContacts.get(contact.getMsisdn());
			if (null != tuple)
			{
				savedContacts.remove(contact.getMsisdn());
				tuple.setName(null);
				unsavedContacts.put(contact.getMsisdn(), tuple);
			}
		}
		finally
		{
			writeLockTrans.unlock();
		}
	}

	/**
	 * clears the transient memory
	 */
	void clearMemory()
	{
		writeLockTrans.lock();
		try
		{
			if (null != savedContacts)
			{
				savedContacts.clear();
			}

			if (null != unsavedContacts)
			{
				unsavedContacts.clear();
			}

			if (null != groupParticipants)
			{
				groupParticipants.clear();
			}

			allContactsLoaded = false;
		}
		finally
		{
			writeLockTrans.unlock();
		}
	}
}
