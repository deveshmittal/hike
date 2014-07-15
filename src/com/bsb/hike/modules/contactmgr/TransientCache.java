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

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.ContactInfo.FavoriteType;

public class TransientCache extends ContactsCache
{
	HikeUserDatabase hDb;

	// Transient Memory for contacts that are saved in address book with their reference count
	private Map<String, ContactTuple> savedContacts; // TODO name field not needed here

	// Transient Memory for contacts that are not saved in address book with their reference count
	private Map<String, ContactTuple> unsavedContacts;

	// Transient memory for contacts of group participants
	private Map<String, Map<String, GroupParticipant>> groupParticipants;

	private final ReentrantReadWriteLock readWriteLockTrans = new ReentrantReadWriteLock(true);

	private final Lock readLock = readWriteLockTrans.readLock();

	private final Lock writeLock = readWriteLockTrans.writeLock();

	/**
	 * Initializes all the transient maps
	 */
	TransientCache(HikeUserDatabase db)
	{
		savedContacts = new LinkedHashMap<String, ContactTuple>();
		unsavedContacts = new HashMap<String, ContactTuple>();
		groupParticipants = new HashMap<String, Map<String, GroupParticipant>>();
		hDb = db;
	}

	/**
	 * get contact info from memory. Returns null if not found in memory. This implementation is thread safe.
	 * 
	 * @param key
	 * @return
	 */
	ContactInfo getContact(String key)
	{
		readLock.lock();
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
			readLock.unlock();
		}
	}

	/**
	 * inserts contact in {@link #savedContacts}.
	 * <p>
	 * Make sure that it is not already in memory , we are setting reference count to one here
	 * </p>
	 * 
	 * @param contact
	 *            contactinfo to put in map
	 */
	void insertContact(ContactInfo contact)
	{
		writeLock.lock();
		try
		{
			ContactTuple tuple = new ContactTuple(1, null, contact);
			savedContacts.put(contact.getMsisdn(), tuple);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * inserts contact in {@link #unsavedContacts}.
	 * <p>
	 * Make sure that it is not already in memory , we are setting reference count to one here
	 * </p>
	 * 
	 * @param contact
	 *            contactinfo to put in map
	 * @param name
	 *            name if contact is not saved in address book
	 */
	void insertContact(ContactInfo contact, String name)
	{
		writeLock.lock();
		try
		{
			ContactTuple tuple = new ContactTuple(1, name, contact);
			unsavedContacts.put(contact.getMsisdn(), tuple);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * Inserts the map of msisdn and <code>GroupParticipant</code> into {@link #groupParticipants} with <code>grpId</code> as key.
	 * 
	 * @param grpId
	 * @param groupParticipantsMap
	 */
	void insertGroupParticipants(String grpId, Map<String, GroupParticipant> groupParticipantsMap)
	{
		writeLock.lock();
		try
		{
			groupParticipants.put(grpId, groupParticipantsMap);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * Removes this contact from memory if reference count is one otherwise decrements the reference count
	 * 
	 * @param contact
	 */
	void removeContact(String msisdn)
	{
		writeLock.lock();
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
			writeLock.unlock();
		}
	}

	/**
	 * updates the contact in memory
	 * 
	 * @param contact
	 */
	void updateContact(ContactInfo contact)
	{
		writeLock.lock();
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
			writeLock.unlock();
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
		writeLock.lock();
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
			writeLock.unlock();
		}
	}

	/**
	 * Returns the list of all the contacts sorted by their names. If boolean {@link #allContactsLoaded} is true then all the contacts are retrieved from {@link #savedContacts} and
	 * {@link #unsavedContacts}.
	 * 
	 * @return
	 */
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
	 * This method returns the name of contact using <code>msisdn</code> parameter.
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
	 * Sets the name for a unsaved contact in {@link #unsavedContacts} contact tuple field.
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
	 * This function will load all the contacts from DB into transient storage. Contacts which are in persistence map will not be loaded into it.
	 */
	void loadMemory()
	{
		Pair<Map<String, ContactInfo>, Map<String, ContactInfo>> map = hDb.getAllContactInfo();

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
	ContactInfo putInCache(String msisdn, boolean ifNotFoundReturnNull)
	{
		ContactInfo c = hDb.getContactInfoFromMSISDN(msisdn, ifNotFoundReturnNull);
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
	List<ContactInfo> putInCache(List<String> msisdns)
	{
		if (msisdns.size() > 0)
		{
			Map<String, ContactInfo> map = hDb.getContactInfoFromMsisdns(msisdns, true);

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
			Set<String> blockSet = hDb.getBlockedMsisdnSet();

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
			Map<String, ContactInfo> map = hDb.getNOTFRIENDScontactsFromDB(onHike, myMsisdn, nativeSMSOn, ignoreUnknownContacts);
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

		Map<String, ContactInfo> map = hDb.getContactsOfFavoriteTypeDB(favoriteType, onHike, myMsisdn, nativeSMSOn, ignoreUnknownContacts);

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

		List<ContactInfo> contacts = hDb.getHikeContacts(limit, msisdnsIn, msisdnsNotIn, myMsisdn);
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
			Set<String> blockSet = hDb.getBlockedMsisdnSet();

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
			contacts = hDb.getNonHikeContacts();

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

		List<ContactInfo> contacts = hDb.getNonHikeMostContactedContacts(limit);
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
		ContactInfo contact = null;
		if (allContactsLoaded)
		{
			for (Entry<String, ContactTuple> mapEntry : savedContacts.entrySet())
			{
				ContactTuple tuple = mapEntry.getValue();
				if (null != tuple && tuple.getContact().getPhoneNum().equals(number))
				{
					contact = tuple.getContact();
					break;
				}
			}
		}
		else
		{
			contact = hDb.getContactInfoFromPhoneNo(number);
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
		return contact;
	}

	/**
	 * 
	 * @param number
	 * @return
	 */
	public ContactInfo getContactInfoFromPhoneNoOrMsisdn(String number)
	{
		ContactInfo contact = getContact(number);

		if (null != contact)
		{
			return contact;
		}

		if (allContactsLoaded)
		{
			for (Entry<String, ContactTuple> mapEntry : savedContacts.entrySet())
			{
				ContactTuple tuple = mapEntry.getValue();
				if (null != tuple && tuple.getContact().getPhoneNum().equals(number))
				{
					contact = tuple.getContact();
					break;
				}
			}
		}
		else
		{
			contact = hDb.getContactInfoFromPhoneNoOrMsisdn(number);
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
		return contact;
	}

	/**
	 * Updates the contact by setting the name to null and move it from saved to unsaved contacts map
	 * 
	 * @param contact
	 */
	void contactDeleted(ContactInfo contact)
	{
		writeLock.lock();
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
			writeLock.unlock();
		}
	}

	/**
	 * clears the transient memory
	 */
	void clearMemory()
	{
		writeLock.lock();
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
			writeLock.unlock();
		}
	}

	/**
	 * This method is used to get group Participants of a particular group specified by <code>groupId</code>. First {@link #groupParticipants} is checked if they are loaded in
	 * memory or not , if not then database query is executed and participants are put in the cache {@link #groupParticipants} map.
	 * 
	 * @param groupId
	 * @param activeOnly
	 * @param notShownStatusMsgOnly
	 * @param fetchParticipants
	 * @return
	 */
	Pair<Map<String, GroupParticipant>, List<String>> getGroupParticipants(String groupId, boolean activeOnly, boolean notShownStatusMsgOnly, boolean fetchParticipants)
	{
		Map<String, GroupParticipant> groupParticipantsMap = null;
		readLock.lock();
		try
		{
			groupParticipantsMap = groupParticipants.get(groupId);
			if (null != groupParticipantsMap)
			{
				return new Pair<Map<String, GroupParticipant>, List<String>>(groupParticipantsMap, null);
			}
		}
		finally
		{
			readLock.unlock();
		}

		return HikeConversationsDatabase.getInstance().getGroupParticipants(groupId, activeOnly, notShownStatusMsgOnly);
	}

	/**
	 * This is used for cases when we getGroupParticipants and if found in transient cache we have to increase the reference count by one.
	 * <p>
	 * Since for getting contact info for group participants , we have to check in both persistence and transient cache .These things are done in
	 * {@link ContactManager#getGroupParticipants(String, boolean, boolean, boolean)} where if contact is found in transient cache then it's reference count should be incremented.
	 * </p>
	 * 
	 * @param msisdn
	 */
	void incrementRefCount(String msisdn)
	{
		writeLock.lock();
		try
		{
			ContactTuple tuple = savedContacts.get(msisdn);
			if (null == tuple)
			{
				tuple = unsavedContacts.get(msisdn);
			}

			if (null != tuple)
			{
				tuple.setReferenceCount(tuple.getReferenceCount() + 1);
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}
}
