package com.bsb.hike.modules.contactmgr;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;

/**
 * @author Gautam This class is used as a cache to store the contacts. This hides the mechanism of storing the contacts object from the clients and the manager. 
 * This class should be enhanced properly when logic of caching is changed. All the threading stuff should be handled by this class only. Most of the operation will be read so synchronizing
 * with mutex is not good. Taking explicit read write locks will enhance performance.
 */
class ContactsCache
{
	// Memory persistence for all the contacts that should always be laoded
	private Map<String, ContactInfo> persistenceMap;

	// Transient persistence for all the contacts that should always be laoded
	private Map<String, ContactInfo> transientMap;

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

	/**
	 * This function will load all the contacts from DB into transient storage. Contacts which are in persistence map will not be loaded into it
	 */
	void loadTransientMem()
	{
		HikeUserDatabase.getInstance().getContactsMap(persistenceMap);
	}

	/**
	 * This will load all the persistent contacts in the memory
	 */
	void loadPersistenceMemory()
	{
		
	}
	
	public void clearTransientMemory()
	{
		writeLockTrans.lock();
		try
		{
			if (transientMap != null)
			{
				transientMap.clear();
			}
		}
		finally
		{
			writeLockTrans.unlock();
		}
	}
}
