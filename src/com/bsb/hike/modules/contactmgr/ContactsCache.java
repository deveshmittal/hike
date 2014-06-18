package com.bsb.hike.modules.contactmgr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.util.Pair;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.modules.contactmgr.db.HikeUserDatabase;

/**
 * @author Gautam This class is used as a cache to store the contacts. This hides the mechanism of storing the contacts object from the clients and the manager. This class should
 *         be enhanced properly when logic of caching is changed. All the threading stuff should be handled by this class only. Most of the operation will be read so synchronizing
 *         with mutex is not good. Taking explicit read write locks will enhance performance.
 */
class ContactsCache
{
	// Memory persistence for all the contacts that should always be loaded
	private Map<String, ContactInfo> persistenceMap;

	// Transient persistence for all the contacts that should always be loaded
	private Map<String, ContactInfo> transientMap;

	private Map<String, List<String>> groupLastMsisdns;

	private boolean allContactsLoaded = false;

	private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

	private final Lock readLock = readWriteLock.readLock();

	private final Lock writeLock = readWriteLock.writeLock();

	private final ReentrantReadWriteLock readWriteLockTrans = new ReentrantReadWriteLock(true);

	private final Lock readLockTrans = readWriteLockTrans.readLock();

	private final Lock writeLockTrans = readWriteLockTrans.writeLock();

	ContactsCache()
	{
		persistenceMap = new HashMap<String, ContactInfo>();
		transientMap = new HashMap<String, ContactInfo>();
		groupLastMsisdns = new HashMap<String, List<String>>();
	}

	/**
	 * @param key
	 * @return contact object if present and null if not present
	 */
	ContactInfo getContact(String key)
	{
		readLock.lock();
		try
		{
			ContactInfo c = null;
			c = persistenceMap.get(key);
			if (c != null)
				return c;
		}
		finally
		{
			readLock.unlock();
		}
		readLockTrans.lock();
		try
		{
			return transientMap.get(key);
		}
		finally
		{
			readLockTrans.unlock();
		}
	}

	void removeContact(ContactInfo c)
	{
		writeLockTrans.lock();
		try
		{
			if (transientMap.containsKey(c.getMsisdn()))
			{
				transientMap.remove(c.getMsisdn());
				allContactsLoaded = false;
				return;
			}
		}
		finally
		{
			writeLockTrans.unlock();
		}

		writeLock.lock();
		try
		{
			if (persistenceMap.containsKey(c.getMsisdn()))
			{
				persistenceMap.remove(c.getMsisdn());
				allContactsLoaded = false;
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * @param key
	 * @param contact
	 *            This will insert the contact in the transient memory
	 */
	void insertContact(String key, ContactInfo c, boolean insertInTransient)
	{
		if (insertInTransient)
		{
			writeLockTrans.lock();
			try
			{
				transientMap.put(key, c);
			}
			finally
			{
				writeLockTrans.unlock();
			}
		}
		else
		{
			writeLock.lock();
			try
			{
				persistenceMap.put(key, c);
			}
			finally
			{
				writeLock.unlock();
			}
		}
	}

	/**
	 * @param key
	 * @param contact
	 *            This will insert the contact in the persistent memory
	 */
	void insertContact(String key, ContactInfo c)
	{
		insertContact(key, c, false);
	}

	/**
	 * Inserts all the entries in a map to persistence map
	 * 
	 * @param map
	 */
	void insertContactsFromMap(Map<String, ContactInfo> map)
	{
		insertContactsFromMap(map, false);
	}

	/**
	 * Inserts all the entries in a map to transient map
	 * 
	 * @param map
	 * @param insertInTransient
	 */
	void insertContactsFromMap(Map<String, ContactInfo> map, boolean insertInTransient)
	{
		if (insertInTransient)
		{
			writeLockTrans.lock();
			try
			{
				transientMap.putAll(map);
			}
			finally
			{
				writeLockTrans.unlock();
			}
		}
		else
		{
			writeLock.lock();
			try
			{
				persistenceMap.putAll(map);
			}
			finally
			{
				writeLock.unlock();
			}
		}
	}

	/**
	 * @param key
	 * @param c
	 *            This function will update the contact object in the corresponding memory
	 */
	void updateContact(String key, ContactInfo c)
	{
		writeLock.lock();
		boolean val = false;
		try
		{
			if (val = persistenceMap.containsKey(key))
				persistenceMap.put(key, c);
		}
		finally
		{
			writeLock.unlock();
		}

		// if contact is not found in persistence memory
		if (val == false)
		{
			writeLockTrans.lock();
			try
			{
				if (transientMap.containsKey(key))
					transientMap.put(key, c);
			}
			finally
			{
				writeLockTrans.unlock();
			}
		}
	}

	List<ContactInfo> getAllContacts()
	{
		if (!allContactsLoaded)
		{
			loadTransientMem();
			allContactsLoaded = true;
		}

		List<ContactInfo> allContacts = new ArrayList<ContactInfo>(persistenceMap.values());
		allContacts.addAll(transientMap.values());

		return allContacts;
	}

	/**
	 * This function will load all the contacts from DB into transient storage. Contacts which are in persistence map will not be loaded into it
	 */
	void loadTransientMem()
	{
		Map<String, ContactInfo> map = HikeUserDatabase.getInstance().getAllContactInfo();

		for (Entry<String, ContactInfo> mapEntry : map.entrySet())
		{
			String msisdn = mapEntry.getKey();
			ContactInfo contact = mapEntry.getValue();
			if (getContact(msisdn) == null)
			{
				insertContact(msisdn, contact, true);
			}
		}
	}

	/**
	 * This method loads the contactInfo of msisdns in transient memory and returns the list of same
	 * 
	 * @param msisdns
	 * @return
	 */
	List<ContactInfo> loadTransientMem(List<String> msisdns)
	{
		if (msisdns.size() > 0)
		{
			Map<String, ContactInfo> map = HikeUserDatabase.getInstance().getContactInfoFromMsisdns(msisdns, true);

			insertContactsFromMap(map, true);

			return new ArrayList<ContactInfo>(map.values());
		}

		return null;
	}

	/**
	 * Loads the contact info for a msisdn in transient memory and returns the same
	 * 
	 * @param msisdn
	 * @return
	 */
	ContactInfo loadTransientMem(String msisdn, Boolean ifNotFoundReturnNull)
	{
		ContactInfo c = HikeUserDatabase.getInstance().getContactInfoFromMSISDN(msisdn, ifNotFoundReturnNull);
		insertContact(msisdn, c, true);
		return c;
	}

	/**
	 * This will load all the persistent contacts in the memory
	 */
	void loadPersistenceMemory()
	{
		List<String> msisdns = HikeConversationsDatabase.getInstance().getConversationMsisdns();
		if (msisdns.size() > 0)
		{
			persistenceMap = HikeUserDatabase.getInstance().getContactInfoFromMsisdns(msisdns, true);
		}
	}

	/**
	 * This method loads the contactInfo of msisdns in msisdnsDB in persistence memory and returns the list of same
	 * 
	 * @param msisdnsDB
	 * @return
	 */
	List<ContactInfo> loadPersistenceMemory(List<String> msisdns)
	{
		if (msisdns.size() > 0)
		{
			Map<String, ContactInfo> map = HikeUserDatabase.getInstance().getContactInfoFromMsisdns(msisdns, true);

			insertContactsFromMap(map);

			return new ArrayList<ContactInfo>(map.values());
		}

		return null;
	}

	/**
	 * This method loads the contact info for a msisdn in persistence memory and returns the same
	 * 
	 * @param msisdn
	 * @return
	 */
	ContactInfo loadPersistenceMemory(String msisdn, Boolean ifNotFoundReturnNull)
	{
		ContactInfo c = HikeUserDatabase.getInstance().getContactInfoFromMSISDN(msisdn, ifNotFoundReturnNull);
		insertContact(msisdn, c);
		return c;
	}

	void clearTransientMemory()
	{
		writeLockTrans.lock();
		try
		{
			if (transientMap != null)
			{
				transientMap.clear();
				allContactsLoaded = false;
			}
		}
		finally
		{
			writeLockTrans.unlock();
		}
	}

	void clearPersistenceMemory()
	{
		writeLock.lock();
		try
		{
			if (persistenceMap != null)
			{
				persistenceMap.clear();
				allContactsLoaded = false;
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	void clearPersistenceMemory(String msisdn)
	{
		writeLock.lock();
		try
		{
			if (persistenceMap != null)
			{
				persistenceMap.remove(msisdn);
			}
			allContactsLoaded = false;
		}
		finally
		{
			writeLock.unlock();
		}
	}

	void clearPersistenceMemory(List<String> msisdns)
	{
		for (String m : msisdns)
		{
			clearPersistenceMemory(m);
		}
	}

	void removeFromCache(List<ContactInfo> contacts)
	{
		for (ContactInfo contact : contacts)
		{
			removeContact(contact);
		}
	}

	void removeOlderLastGroupMsisdn(String groupId, List<String> currentGroupMsisdns)
	{
		if (groupLastMsisdns != null)
		{
			List<String> grpMsisdns = groupLastMsisdns.get(groupId);
			if (null != grpMsisdns)
			{
				Map<String, Boolean> map = new HashMap<String, Boolean>();
				for (String s : currentGroupMsisdns)
				{
					map.put(s, true);
				}
				for (String msisdn : grpMsisdns)
				{
					if (!map.containsKey(msisdn))
					{
						clearPersistenceMemory(msisdn);
					}
				}
			}
			groupLastMsisdns.put(groupId, currentGroupMsisdns);
		}
	}

	List<ContactInfo> getNOTFRIENDScontacts(int onHike, String myMsisdn, boolean nativeSMSOn, boolean ignoreUnknownContacts)
	{
		Map<String, ContactInfo> map = HikeUserDatabase.getInstance().getNOTFRIENDScontactsFromDB(onHike, myMsisdn, nativeSMSOn, ignoreUnknownContacts);

		List<ContactInfo> contacts = new ArrayList<ContactInfo>();

		if (map != null)
		{
			for (Entry<String, ContactInfo> mapEntry : map.entrySet())
			{
				String msisdn = mapEntry.getKey();
				ContactInfo contact = mapEntry.getValue();
				if (getContact(msisdn) == null)
				{
					insertContact(msisdn, contact, true);
				}
				contacts.add(contact);
			}
		}

		return contacts;
	}

	List<ContactInfo> getContactsOfFavoriteType(FavoriteType[] favoriteType, int onHike, String myMsisdn, boolean nativeSMSOn, boolean ignoreUnknownContacts)
	{
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
					insertContact(msisdn, contact, true);
				}
				contacts.add(contact);
			}
		}

		return contacts;
	}

	List<ContactInfo> getHikeContacts(int limit, String msisdnsIn, String msisdnsNotIn, String myMsisdn)
	{
		List<ContactInfo> contacts = HikeUserDatabase.getInstance().getHikeContacts(limit, msisdnsIn, msisdnsNotIn, myMsisdn);
		for (ContactInfo contact : contacts)
		{
			if (null == getContact(contact.getMsisdn()))
			{
				insertContact(contact.getMsisdn(), contact, true);
			}
		}
		return contacts;
	}

	List<Pair<AtomicBoolean, ContactInfo>> getNonHikeContacts()
	{
		List<Pair<AtomicBoolean, ContactInfo>> contacts = HikeUserDatabase.getInstance().getNonHikeContacts();
		
		for (Pair<AtomicBoolean, ContactInfo> p : contacts)
		{
			ContactInfo contact = p.second;
			if (null == getContact(contact.getMsisdn()))
			{
				insertContact(contact.getMsisdn(), contact, true);
			}
		}

		return contacts;
	}

	List<ContactInfo> getNonHikeMostContactedContacts(int limit)
	{
		List<ContactInfo> contacts = HikeUserDatabase.getInstance().getNonHikeMostContactedContacts(limit);
		for(ContactInfo contact : contacts)
		{
			if(null == getContact(contact.getMsisdn()))
			{
				insertContact(contact.getMsisdn(), contact, true);
			}
		}
		return contacts;
	}
}
