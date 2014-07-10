package com.bsb.hike.modules.contactmgr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.database.DatabaseUtils;
import android.util.Pair;

import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.db.HikeUserDatabase;
import com.bsb.hike.utils.Utils;

class PersistenceCache extends ContactsCache
{
	// Memory persistence for all one to one conversation contacts that should always be loaded
	private Map<String, ContactInfo> convsContactsPersistence;

	// Memory persistence for all group conversation last messaged contacts with reference count that should always be loaded
	private Map<String, ContactTuple> groupContactsPersistence;

	// Memory persistence for all group names and list of msisdns(last message in group) that should always be loaded
	private Map<String, Pair<String, ConcurrentLinkedQueue<String>>> groupPersistence;

	private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

	private final Lock readLock = readWriteLock.readLock();

	private final Lock writeLock = readWriteLock.writeLock();

	/**
	 * 
	 */
	PersistenceCache()
	{
		convsContactsPersistence = new HashMap<String, ContactInfo>();
		groupContactsPersistence = new HashMap<String, ContactTuple>();
		groupPersistence = new HashMap<String, Pair<String, ConcurrentLinkedQueue<String>>>();
		loadMemory();
	}

	/**
	 * get contact info from memory. Returns null if not found in memory. The implementation is thread safe
	 * 
	 * @param key
	 * @return
	 */
	ContactInfo getContact(String key)
	{
		readLock.lock();
		try
		{
			ContactInfo c = null;
			c = convsContactsPersistence.get(key);
			if (null == c) // contact not found in persistence cache
			{
				ContactTuple tuple = groupContactsPersistence.get(key);
				if (null != tuple)
				{
					c = tuple.getContact();
				}
			}
			return c;
		}
		finally
		{
			readLock.unlock();
		}
	}

	/**
	 * inserts contact in one to one conversations map.
	 * 
	 * @param contact
	 */
	void insertContact(ContactInfo contact)
	{
		writeLock.lock();
		try
		{
			convsContactsPersistence.put(contact.getMsisdn(), contact);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * inserts contact in group contacts map. Make sure that it is not already in memory , we are setting reference count to 1 here
	 * 
	 * @param contact
	 * @param name
	 *            name if contact is not saved in address book otherwise null
	 */
	void insertContact(ContactInfo contact, String name)
	{
		writeLock.lock();
		try
		{
			ContactTuple tuple = new ContactTuple(1, name, contact);
			groupContactsPersistence.put(contact.getMsisdn(), tuple);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * Removes the contact from one to one conversation map
	 * 
	 * @param contact
	 */
	void removeContact(String msisdn)
	{
		removeContact(msisdn, true);
	}

	/**
	 * Removes the contact the convsContactPersistence OR decrements the reference count in groupContactsPersistence , if reference count is 0 removes from this map Depending on
	 * whether it is one to one conversation or group conversation
	 * 
	 * @param contact
	 * @param ifOneToOneConversation
	 *            true if it is one to one conversation contact otherwise false
	 */
	void removeContact(String msisdn, boolean ifOneToOneConversation)
	{
		writeLock.lock();
		try
		{
			removeFromCache(msisdn, ifOneToOneConversation);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * This method is not Thread safe , removes the contact for a particular msisdn from the cache - if it is one to one conversation removes from convs map otherwise from group
	 * map
	 * 
	 * @param msisdn
	 * @param ifOneToOneConversation
	 */
	private void removeFromCache(String msisdn, boolean ifOneToOneConversation)
	{
		if (ifOneToOneConversation)
		{
			convsContactsPersistence.remove(msisdn);
		}
		else
		{
			ContactTuple tuple = groupContactsPersistence.get(msisdn);
			if (null != tuple)
			{
				tuple.setReferenceCount(tuple.getReferenceCount() - 1);
				if (tuple.getReferenceCount() == 0)
				{
					groupContactsPersistence.remove(msisdn);
				}
			}
		}
	}

	/**
	 * This method is thread safe and removes the mapping for particular grpId (if group is deleted) and their corresponding last msisnds are removed from the group contacts map
	 * 
	 * @param grpId
	 */
	void removeGroup(String grpId)
	{
		writeLock.lock();
		try
		{
			Pair<String, ConcurrentLinkedQueue<String>> pp = groupPersistence.get(grpId);
			ConcurrentLinkedQueue<String> lastMsisdns = pp.second;
			for (String ms : lastMsisdns)
			{
				removeFromCache(ms, false);
			}
			groupPersistence.remove(grpId);
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
			if (convsContactsPersistence.containsKey(contact.getMsisdn()))
			{
				convsContactsPersistence.put(contact.getMsisdn(), contact);
			}
			else
			{
				ContactTuple tuple = groupContactsPersistence.get(contact.getMsisdn());
				if (null != tuple)
				{
					tuple.setContact(contact);
				}
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * Updates the contact tuple in group contacts persistence map if unknown contact is saved or saved contact is deleted
	 * 
	 * @param contact
	 * @param name
	 */
	void updateContact(ContactInfo contact, String name)
	{
		readLock.lock();
		ContactTuple tuple = null;
		try
		{
			tuple = groupContactsPersistence.get(contact.getMsisdn());
		}
		finally
		{
			writeLock.unlock();
		}
		if (null != tuple)
		{
			tuple.setContact(contact);
			tuple.setName(name);
		}

	}

	/**
	 * Returns name of group or contact. This implementation is threadsafe
	 * 
	 * @param msisdn
	 * @return
	 */

	String getName(String msisdn)
	{
		/**
		 * Always try to take locks when and where required. Here we are separating out locking into different zones so that lock acquired should be for minimum time possible.
		 */
		if (Utils.isGroupConversation(msisdn))
		{
			readLock.lock();
			try
			{
				Pair<String, ConcurrentLinkedQueue<String>> grp = groupPersistence.get(msisdn);
				return grp.first;
			}
			finally
			{
				readLock.unlock();
			}
		}
		readLock.lock();
		try
		{
			ContactInfo c = null;
			c = convsContactsPersistence.get(msisdn);
			if (null == c)
			{
				ContactTuple tuple = groupContactsPersistence.get(msisdn);
				if (null != tuple)
				{
					c = tuple.getContact();
					if (null == c.getName())
					{
						return tuple.getName();
					}
				}
			}

			if (null == c)
				return null;

			return c.getName();
		}
		finally
		{
			readLock.unlock();
		}
	}

	/**
	 * This function sets name in group participant cache if the contact is not saved
	 * @param msisdn
	 * @param name
	 */
	void setUnknownContactName(String msisdn, String name)
	{
		writeLock.lock();
		try
		{
			ContactTuple tuple = groupContactsPersistence.get(msisdn);
			if (null != tuple)
			{
				tuple.setName(name);
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * This will load all persistence contacts in memory
	 */
	void loadMemory()
	{
		Pair<List<String>, Map<String, List<String>>> allmsisdns = HikeConversationsDatabase.getInstance().getConversationMsisdns();
		List<String> oneToOneMsisdns = allmsisdns.first;
		Map<String, List<String>> groupLastMsisdnsMap = allmsisdns.second;

		Map<String, String> groupNamesMap = HikeConversationsDatabase.getInstance().getGroupNames();

		List<String> grouplastMsisdns = new ArrayList<String>();

		for (Entry<String, String> mapEntry : groupNamesMap.entrySet())
		{
			String grpId = mapEntry.getKey();
			String name = mapEntry.getValue();
			List<String> lastMsisdns = groupLastMsisdnsMap.get(grpId);
			grouplastMsisdns.addAll(lastMsisdns);
			ConcurrentLinkedQueue<String> lastMsisdnsConcurrentLinkedQueue = new ConcurrentLinkedQueue<String>(lastMsisdns);
			Pair<String, ConcurrentLinkedQueue<String>> groupPair = new Pair<String, ConcurrentLinkedQueue<String>>(name, lastMsisdnsConcurrentLinkedQueue);
			groupPersistence.put(grpId, groupPair);
		}

		List<String> msisdnsToGetContactInfo = new ArrayList<String>();
		msisdnsToGetContactInfo.addAll(oneToOneMsisdns);
		msisdnsToGetContactInfo.addAll(grouplastMsisdns);

		Map<String, ContactInfo> contactsMap = new HashMap<String, ContactInfo>();
		if (msisdnsToGetContactInfo.size() > 0)
		{
			contactsMap = HikeUserDatabase.getInstance().getContactInfoFromMsisdns(msisdnsToGetContactInfo, true);
		}

		// grouplastMsisdns list convert it to map
		Map<String, Boolean> temp = new HashMap<String, Boolean>();
		for (String ms : grouplastMsisdns)
		{
			temp.put(ms, true);
		}

		// traverse through oneToOneMsisdns and get from contactsMap and put in convsContactsPersistence , remove from contactsMap if not present in grouplastMsisdns map
		for (String ms : oneToOneMsisdns)
		{
			ContactInfo contact = contactsMap.get(ms);
			convsContactsPersistence.put(ms, contact);
			if (!temp.containsKey(ms))
			{
				contactsMap.remove(ms);
			}
		}

		// traverse through contactsMap which is left put in groupContactsPersistence if contactInfo name is null we have to get names for that
		StringBuilder unknownGroupMsisdns = new StringBuilder("(");
		for (Entry<String, ContactInfo> mapEntry : contactsMap.entrySet())
		{
			String msisdn = mapEntry.getKey();
			ContactInfo contact = mapEntry.getValue();
			ContactTuple tuple = new ContactTuple(1, null, contact);
			groupContactsPersistence.put(msisdn, tuple);

			if (null == contact.getName())
			{
				unknownGroupMsisdns.append(DatabaseUtils.sqlEscapeString(msisdn) + ",");
			}
		}

		// get names of unknown group contacts from group members table
		Map<String, String> unknownGroupMsisdnsName = new HashMap<String, String>();

		int idx = unknownGroupMsisdns.lastIndexOf(",");
		if (idx >= 0)
		{
			unknownGroupMsisdns.replace(idx, unknownGroupMsisdns.length(), ")");
			unknownGroupMsisdnsName = HikeConversationsDatabase.getInstance().getGroupMembersName(unknownGroupMsisdns.toString());
		}

		// set names for unknown group contacts
		for (Entry<String, String> mapEntry : unknownGroupMsisdnsName.entrySet())
		{
			String msisdn = mapEntry.getKey();
			String name = mapEntry.getValue();

			ContactTuple tuple = groupContactsPersistence.get(msisdn);
			tuple.setName(name);
		}
	}

	/**
	 * This method loads the contact info for a msisdn in persistence convs map and returns the same
	 * 
	 * @param msisdn
	 * @param ifNotFoundReturnNull
	 *            if true returns null if contact is not saved
	 */
	ContactInfo putInCache(String msisdn, boolean ifNotFoundReturnNull)
	{
		return putInCache(msisdn, ifNotFoundReturnNull, true);
	}

	/**
	 * This method loads the contact info for a msisdn in persistence memory and returns the same
	 * 
	 * @param msisdn
	 * @param ifNotFoundReturnNull
	 *            if true returns null if contact is not saved
	 * @param ifOneToOneConversation
	 * @return Returns contact info object
	 */
	ContactInfo putInCache(String msisdn, boolean ifNotFoundReturnNull, boolean ifOneToOneConversation)
	{
		ContactInfo contact = HikeUserDatabase.getInstance().getContactInfoFromMSISDN(msisdn, ifNotFoundReturnNull);
		if (ifOneToOneConversation)
		{
			insertContact(contact);
		}
		else
		{
			insertContact(contact, null);
		}
		return contact;
	}

	/**
	 * This method loads the contactInfo of msisdns in persistence convs map and returns the list of same
	 * 
	 * @param msisdns
	 */
	List<ContactInfo> putInCache(List<String> msisdns)
	{
		return putInCache(msisdns, true);
	}

	/**
	 * This method loads the contactInfo of msisdns in persistence memory and returns the list of same
	 * 
	 * @param msisdns
	 * @param ifOneToOneConversation
	 * @return
	 */
	List<ContactInfo> putInCache(List<String> msisdns, boolean ifOneToOneConversation)
	{
		if (msisdns.size() > 0)
		{
			Map<String, ContactInfo> map = HikeUserDatabase.getInstance().getContactInfoFromMsisdns(msisdns, true);

			for (Entry<String, ContactInfo> mapEntry : map.entrySet())
			{
				ContactInfo contact = mapEntry.getValue();
				if (ifOneToOneConversation)
				{
					insertContact(contact);
				}
				else
				{
					insertContact(contact, null);
				}
			}

			return new ArrayList<ContactInfo>(map.values());
		}

		return null;
	}

	/**
	 * Updates the contact by setting the name to null
	 * 
	 * @param contact
	 */
	void contactDeleted(ContactInfo contact)
	{
		ContactInfo updatedContact = new ContactInfo(contact);
		updatedContact.setName(null);
		updateContact(updatedContact);
	}

	/**
	 * This method is used for removing msisdns from the group persistence cache and their reference count is decremented in group contacts map by which is done by removecontact
	 * method
	 * 
	 * @param groupId
	 * @param currentGroupMsisdns
	 */
	void removeOlderLastGroupMsisdn(String groupId, List<String> currentGroupMsisdns)
	{
		if (groupPersistence != null)
		{
			Pair<String, ConcurrentLinkedQueue<String>> nameAndLastMsisdns;
			readLock.lock();
			try
			{
				nameAndLastMsisdns = groupPersistence.get(groupId);
			}
			finally
			{
				readLock.unlock();
			}

			if (null != nameAndLastMsisdns)
			{
				ConcurrentLinkedQueue<String> grpMsisdns = nameAndLastMsisdns.second;
				if (null != grpMsisdns)
				{
					boolean flag;
					writeLock.lock();
					try
					{
						for (String msisdn : grpMsisdns)
						{
							flag = false;
							for (String ms : currentGroupMsisdns)
							{
								if (ms.equals(msisdn))
								{
									flag = true;
									break;
								}
							}
							if (!flag)
							{
								removeFromCache(msisdn, false);
							}
						}
					}
					finally
					{
						writeLock.unlock();
					}
				}
				grpMsisdns.clear();
				grpMsisdns.addAll(currentGroupMsisdns);
			}
		}
	}

	/**
	 * clears the persistence memory
	 */
	void clearMemory()
	{
		writeLock.lock();
		try
		{
			if (null != convsContactsPersistence)
			{
				convsContactsPersistence.clear();
			}

			if (null != groupContactsPersistence)
			{
				groupContactsPersistence.clear();
			}

			if (null != groupPersistence)
			{
				groupPersistence.clear();
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}
}
