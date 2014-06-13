/**
 * 
 */
package com.bsb.hike.modules.contactmgr;

import java.util.ArrayList;
import java.util.List;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.iface.ITransientCache;

/**
 * @author Gautam
 * 
 */
public class ContactManager implements ITransientCache
{
	// This should always be present so making it loading on class loading itself
	private volatile static ContactManager _instance = new ContactManager();

	private ContactsCache cache = new ContactsCache();

	private ContactManager()
	{
		cache = new ContactsCache();
		loadPersistenceCache();
	}

	public static ContactManager getInstance()
	{
		return _instance;
	}

	/**
	 * This method loads the persistence memory
	 */
	private void loadPersistenceCache()
	{
		cache.loadPersistenceMemory();
	}

	/**
	 * This method puts the contactInfo object list in persistence memory
	 * 
	 * @param contacts
	 */
	public List<ContactInfo> loadPersistenceCache(List<String> msisdns)
	{
		return cache.loadPersistenceMemory(msisdns);
	}

	/**
	 * This method should load the transient memory at app launch event
	 */
	@Override
	public void load()
	{
		cache.loadTransientMem();
	}

	/**
	 * This method puts the contact info for msisdns in transient memory and Returns the list for same.
	 * 
	 * @param msisdns
	 * @return
	 */
	public List<ContactInfo> load(List<String> msisdns)
	{
		return cache.loadTransientMem(msisdns);
	}

	/**
	 * This method should unload the transient memory at app launch event
	 */
	@Override
	public void unload()
	{
		cache.clearTransientMemory();
	}

	/**
	 * This function will return name or null for a particular msisdn
	 * 
	 * @param msisdn
	 * @return
	 */
	public ContactInfo getContact(String msisdn)
	{
		ContactInfo c = null;
		c = cache.getContact(msisdn);
		return c;
	}

	/**
	 * Returns the contactInfo for a particular msisdn. Inserts the object in transient memory if loadInTransient is set to true otherwise in persistence memory
	 * 
	 * @param msisdn
	 * @param loadInTransient
	 * @return
	 */
	public ContactInfo getContact(String msisdn, boolean loadInTransient)
	{
		return getContact(msisdn, loadInTransient, false);
	}

	/**
	 * Returns the contactInfo for a particular msisdn.Inserts the object in transient memory if loadInTransient is set to true otherwise in persistence memory
	 * 
	 * @param msisdn
	 * @param loadInTransient
	 * @param ifNotFoundReturnNull
	 *            if set to true returns null if not a saved contact
	 * @return
	 */
	public ContactInfo getContact(String msisdn, boolean loadInTransient, boolean ifNotFoundReturnNull)
	{
		ContactInfo contact = cache.getContact(msisdn);
		if (null == contact)
		{
			if (loadInTransient)
			{
				contact = cache.loadTransientMem(msisdn, ifNotFoundReturnNull);
			}
			else
			{
				contact = cache.loadPersistenceMemory(msisdn, ifNotFoundReturnNull);
			}
		}
		else
		{
			if (ifNotFoundReturnNull && contact.getName() == null)
			{
				return null;
			}
		}
		return contact;
	}

	/**
	 * Returns List of contactInfo objects for a some msisdns. Inserts the contactInfo in transient if loadInTransient is set to true otherwise in persistence memory
	 * 
	 * @param msisdns
	 * @param loadInTransient
	 * @return
	 */
	public List<ContactInfo> getContact(List<String> msisdns, boolean loadInTransient)
	{
		List<ContactInfo> contacts = new ArrayList<ContactInfo>();

		List<String> msisdnsDB = new ArrayList<String>();

		for (String msisdn : msisdns)
		{
			ContactInfo c = cache.getContact(msisdn);
			if (null != c)
			{
				contacts.add(c);
			}
			else
			{
				msisdnsDB.add(msisdn);
			}
		}

		if (msisdnsDB.size() > 0)
		{
			List<ContactInfo> contactsDB;
			if (loadInTransient)
			{
				contactsDB = cache.loadTransientMem(msisdnsDB);
			}
			else
			{
				contactsDB = cache.loadPersistenceMemory(msisdnsDB);
			}

			if (null != contactsDB)
			{
				contacts.addAll(contactsDB);
			}
		}
		return contacts;
	}

	/**
	 * Returns the list of all the contacts
	 * 
	 * @return
	 */
	public List<ContactInfo> getAllContacts()
	{
		return cache.getAllContacts();
	}

}
