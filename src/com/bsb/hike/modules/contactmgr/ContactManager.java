/**
 * 
 */
package com.bsb.hike.modules.contactmgr;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.util.Pair;

import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.modules.iface.ITransientCache;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * @author Gautam & Sidharth
 * 
 */
public class ContactManager implements ITransientCache
{
	// This should always be present so making it loading on class loading itself
	private volatile static ContactManager _instance = new ContactManager();

	private PersistenceCache persistenceCache;

	private TransientCache transientCache;

	private ContactManager()
	{
		persistenceCache = new PersistenceCache();
		transientCache = new TransientCache();
		long t1, t2;
		t1 = System.currentTimeMillis();
		persistenceCache.loadMemory();// loadPersistenceCache();
		t2 = System.currentTimeMillis();
		Logger.d("ConversationsTimeTest", " time taken by loadPersistenceCache : " + (t2 - t1));
	}

	public static ContactManager getInstance()
	{
		return _instance;
	}

	/**
	 * This method should unload the transient memory at app launch event
	 */
	@Override
	public void unload()
	{
		transientCache.clearMemory();
	}

	/**
	 * This method clears the persistence memory
	 */
	public void unloadPersistenceCache()
	{
		persistenceCache.clearMemory();
	}

	/**
	 * 
	 * @param msisdn
	 * @param ifOneToOneConversation
	 */
	public void unloadPersistenceCache(String msisdn, boolean ifOneToOneConversation)
	{
		persistenceCache.removeContact(msisdn, ifOneToOneConversation);
	}

	/**
	 * 
	 * @param msisdns
	 */
	public void unloadPersistenceCache(List<String> msisdns)
	{
		for (String ms : msisdns)
		{
			if (Utils.isGroupConversation(ms))
			{
				persistenceCache.removeGroup(ms);
			}
			else
			{
				persistenceCache.removeContact(ms);
			}
		}
	}

	/**
	 * This method is used when contacts are deleted from the addressbook and we set their name to null
	 * 
	 * @param contacts
	 */
	public void contactsDeleted(List<ContactInfo> contacts)
	{
		for (ContactInfo contact : contacts)
		{
			persistenceCache.contactDeleted(contact);
			transientCache.contactDeleted(contact);
		}
	}

	/**
	 * This method updates the contact info object in memory
	 * 
	 * @param contact
	 */
	public void updateContacts(ContactInfo contact)
	{
		ContactInfo con = persistenceCache.getContact(contact.getMsisdn());
		if (null != con)
		{
			persistenceCache.updateContact(contact);
		}
		else
		{
			transientCache.updateContact(contact);
		}
	}

	/**
	 * This updates a list of contactInfo objects in memory
	 * 
	 * @param updatescontacts
	 */
	public void updateContacts(List<ContactInfo> updatescontacts)
	{
		for (ContactInfo contact : updatescontacts)
		{
			updateContacts(contact);
		}
	}

	public String getName(String msisdn)
	{
		String name = persistenceCache.getName(msisdn);
		if (null == name)
		{
			name = transientCache.getName(msisdn);
		}
		return name;
	}

	public void setUnknownContactName(String msisdn, String name)
	{
		persistenceCache.setUnknownContactName(msisdn, name);
		transientCache.setUnknownContactName(msisdn, name);
	}

	/**
	 * This function will return name or null for a particular msisdn
	 * 
	 * @param msisdn
	 * @return
	 */
	public ContactInfo getContact(String msisdn)
	{
		ContactInfo contact = persistenceCache.getContact(msisdn);
		if (null == contact)
		{
			contact = transientCache.getContact(msisdn);
		}
		return contact;
	}

	/**
	 * Returns the contactInfo for a particular msisdn.If not found in memory makes a db call
	 * 
	 * Inserts the object in transient memory if loadInTransient is set to true otherwise in persistence memory
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
	 * Returns the contactInfo for a particular msisdn.If not found in memory makes a db call
	 * 
	 * Inserts the object in transient memory if loadInTransient is set to true otherwise in persistence memory
	 * 
	 * @param msisdn
	 * @param loadInTransient
	 * @param ifNotFoundReturnNull
	 *            if set to true returns null if not a saved contact
	 * @return
	 */
	public ContactInfo getContact(String msisdn, boolean loadInTransient, boolean ifNotFoundReturnNull)
	{
		ContactInfo contact = getContact(msisdn);
		if (null == contact)
		{
			if (loadInTransient)
			{
				contact = transientCache.loadMemory(msisdn, ifNotFoundReturnNull);
			}
			else
			{
				contact = persistenceCache.loadMemory(msisdn, ifNotFoundReturnNull);
			}
		}
		else
		{
			if (ifNotFoundReturnNull && contact.getName() == null)
			{
				return null;
			}

			if (!loadInTransient)
			{
				// TODO cache.moveToPersistence(msisdn, contact);
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
			ContactInfo c = getContact(msisdn);
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
				contactsDB = transientCache.loadMemory(msisdnsDB);
			}
			else
			{
				contactsDB = persistenceCache.loadMemory(msisdnsDB);
			}

			if (null != contactsDB)
			{
				contacts.addAll(contactsDB);
			}
		}

		if (!loadInTransient)
		{
			// TODO cache.moveToPersistence(msisdns);
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
		return transientCache.getAllContacts();
	}

	public void removeOlderLastGroupMsisdns(String groupId, List<String> currentGroupMsisdns)
	{
		persistenceCache.removeOlderLastGroupMsisdn(groupId, currentGroupMsisdns);
	}

	public List<ContactInfo> getContactsOfFavoriteType(FavoriteType favoriteType, int onHike, String myMsisdn)
	{
		return getContactsOfFavoriteType(favoriteType, onHike, myMsisdn, false, false);
	}

	public List<ContactInfo> getContactsOfFavoriteType(FavoriteType favoriteType, int onHike, String myMsisdn, boolean nativeSMSOn)
	{
		return getContactsOfFavoriteType(favoriteType, onHike, myMsisdn, nativeSMSOn, false);
	}

	public List<ContactInfo> getContactsOfFavoriteType(FavoriteType favoriteType, int onHike, String myMsisdn, boolean nativeSMSOn, boolean ignoreUnknownContacts)
	{
		if (favoriteType == FavoriteType.NOT_FRIEND)
		{
			return transientCache.getNOTFRIENDScontacts(onHike, myMsisdn, nativeSMSOn, ignoreUnknownContacts);
		}
		else
		{
			return getContactsOfFavoriteType(new FavoriteType[] { favoriteType }, onHike, myMsisdn, nativeSMSOn, ignoreUnknownContacts);
		}
	}

	public List<ContactInfo> getContactsOfFavoriteType(FavoriteType[] favoriteType, int onHike, String myMsisdn, boolean nativeSMSOn, boolean ignoreUnknownContacts)
	{
		return transientCache.getContactsOfFavoriteType(favoriteType, onHike, myMsisdn, nativeSMSOn, ignoreUnknownContacts);
	}

	public List<ContactInfo> getHikeContacts(int limit, String msisdnsIn, String msisdnsNotIn, String myMsisdn)
	{
		return transientCache.getHikeContacts(limit, msisdnsIn, msisdnsNotIn, myMsisdn);
	}

	public List<Pair<AtomicBoolean, ContactInfo>> getNonHikeContacts()
	{
		return transientCache.getNonHikeContacts();
	}

	public List<ContactInfo> getNonHikeMostContactedContacts(int limit)
	{
		return transientCache.getNonHikeMostContactedContacts(limit);
	}

	public ContactInfo getContactInfoFromPhoneNo(String number)
	{
		return transientCache.getContactInfoFromPhoneNo(number);
	}

	public ContactInfo getContactInfoFromPhoneNoOrMsisdn(String number)
	{
		return transientCache.getContactInfoFromPhoneNoOrMsisdn(number);
	}

	@Override
	public void load()
	{
		// TODO Auto-generated method stub

	}
}
