package com.bsb.hike.modules.contactmgr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.utils.Logger;

public class TransientCache extends ContactsCache
{
	HikeUserDatabase hDb;

	// Transient Memory for contacts with their reference count
	private Map<String, ContactTuple> transientContacts;

	// Transient memory for contacts of group participants
	private Map<String, Map<String, Pair<GroupParticipant, String>>> groupParticipants;

	private final ReentrantReadWriteLock readWriteLockTrans = new ReentrantReadWriteLock(true);

	private final Lock readLock = readWriteLockTrans.readLock();

	private final Lock writeLock = readWriteLockTrans.writeLock();

	/**
	 * Initializes all the transient maps
	 */
	TransientCache(HikeUserDatabase db)
	{
		transientContacts = new LinkedHashMap<String, ContactTuple>();
		groupParticipants = new HashMap<String, Map<String, Pair<GroupParticipant, String>>>();
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
			ContactTuple tuple = transientContacts.get(key);
			if (null == tuple)
			{
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
	 * inserts contact in {@link #transientContacts} if name field in contactInfo is not null otherwise in {@link #unsavedContacts}
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
			ContactTuple tuple = new ContactTuple(1, contact);
			transientContacts.put(contact.getMsisdn(), tuple);
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
	void insertGroupParticipants(String grpId, Map<String, Pair<GroupParticipant, String>> groupParticipantsMap)
	{
		writeLock.lock();
		try
		{
			Map<String, Pair<GroupParticipant, String>> groupParticipantsList = groupParticipants.get(grpId);
			if (null == groupParticipantsList)
			{
				groupParticipantsList = new HashMap<String, Pair<GroupParticipant, String>>();
				groupParticipants.put(grpId, groupParticipantsList);
			}
			groupParticipantsList.putAll(groupParticipantsMap);
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
			ContactTuple tuple = transientContacts.get(msisdn);
			if (null != tuple)
			{
				tuple.setReferenceCount(tuple.getReferenceCount() - 1);
				if (tuple.getReferenceCount() == 0)
				{
					transientContacts.remove(msisdn);
				}
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * Removes the group participant from {@link #groupParticipants} , should be called when user leaves a group chat.
	 * @param groupId
	 * @param msisdn
	 */
	void removeGroupParticipants(String groupId, String msisdn)
	{
		writeLock.lock();
		try
		{
			Map<String, Pair<GroupParticipant, String>> groupParticipantsList = groupParticipants.get(groupId);
			if(null != groupParticipantsList)
			{
				groupParticipantsList.remove(msisdn);
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
			if (transientContacts.containsKey(contact.getMsisdn()))
			{
				ContactTuple tuple = transientContacts.get(contact.getMsisdn());
				tuple.setContact(contact);
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * Returns the list of all the contacts sorted by their names. If boolean {@link #allContactsLoaded} is true then all the contacts are retrieved from {@link #transientContacts} and
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
		readLock.lock();
		try
		{
			for (Entry<String, ContactTuple> mapEntry : transientContacts.entrySet())
			{
				ContactTuple tuple = mapEntry.getValue();
				allContacts.add(tuple.getContact());
			}
		}
		finally
		{
			readLock.unlock();
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
	 * Returns the name of an unsaved contact using groupId as name is different in different group
	 * 
	 * @param groupId
	 * @param msisdn
	 * @return
	 */
	String getName(String groupId, String msisdn)
	{
		readLock.lock();
		try
		{
			Map<String, Pair<GroupParticipant, String>> map = groupParticipants.get(groupId);
			if (null != map)
			{
				Pair<GroupParticipant, String> grpParticipantPair = map.get(msisdn);
				if (null != grpParticipantPair)
					return grpParticipantPair.second;
			}
			return null;
		}
		finally
		{
			readLock.unlock();
		}
	}

	/**
	 * Sets the name for a unsaved contact in {@link #groupParticipants}.
	 * 
	 * @param msisdn
	 * @param name
	 */
	void setUnknownContactName(String grpId, String msisdn, String name)
	{
		writeLock.lock();
		try
		{
			Map<String, Pair<GroupParticipant, String>> groupParticipantMap = groupParticipants.get(grpId);
			if (null != groupParticipantMap)
			{
				GroupParticipant grpParticipant = groupParticipantMap.get(msisdn).first;
				groupParticipantMap.put(msisdn, new Pair<GroupParticipant, String>(grpParticipant, name));
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * This function will load all the contacts from DB into transient storage. Contacts which are in persistence map will not be loaded into it.
	 */
	void loadMemory()
	{
		Map<String, ContactInfo> contactMap = hDb.getAllContactInfo();

		LinkedHashMap<String, ContactTuple> temp = new LinkedHashMap<String, ContactTuple>();

		writeLock.lock();
		try
		{
			for (Entry<String, ContactInfo> mapEntry : contactMap.entrySet())
			{
				String msisdn = mapEntry.getKey();
				ContactInfo contact = mapEntry.getValue();
				ContactTuple tuple = transientContacts.get(msisdn); // TODO should also check in persistence cache
				if (null != tuple)
				{
					tuple.setContact(contact);
					temp.put(msisdn, tuple);
				}
				else
				{
					tuple = new ContactTuple(1, contact);
					temp.put(msisdn, tuple);
				}
				transientContacts.remove(msisdn);
			}

			temp.putAll(transientContacts);

			transientContacts = temp;
		}
		finally
		{
			writeLock.unlock();
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
		insertContact(c);
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
		List<ContactInfo> contactsList = new ArrayList<ContactInfo>();
		if (msisdns.size() > 0)
		{
			Map<String, ContactInfo> map = hDb.getContactInfoFromMsisdns(msisdns, true);

			for (Entry<String, ContactInfo> mapEntry : map.entrySet())
			{
				ContactInfo contact = mapEntry.getValue();
				insertContact(contact);
			}
			contactsList.addAll(map.values());
		}
		return contactsList;
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

			readLock.lock();
			try
			{
				for (Entry<String, ContactTuple> savedMapEntry : transientContacts.entrySet())
				{
					String msisdn = savedMapEntry.getKey();
					if (!blockSet.contains(msisdn) && !msisdn.equals(myMsisdn))
					{
						ContactTuple tuple = savedMapEntry.getValue();
						ContactInfo contact = tuple.getContact();
						if (!(ignoreUnknownContacts && (null == contact.getName())))
						{
							contacts.add(contact);
						}
					}
				}
			}
			finally
			{
				readLock.unlock();
			}
		}
		else
		{
			Map<String, ContactInfo> map = hDb.getNOTFRIENDScontactsFromDB(onHike, myMsisdn, nativeSMSOn, ignoreUnknownContacts);
			if (map != null)
			{
				contacts.addAll(map.values());
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
		List<ContactInfo> contacts = new ArrayList<ContactInfo>();
		if (allContactsLoaded)
		{
			Set<String> blockSet = hDb.getBlockedMsisdnSet();
			boolean flag;

			readLock.lock();
			try
			{
				for (Entry<String, ContactTuple> savedMapEntry : transientContacts.entrySet())
				{
					String msisdn = savedMapEntry.getKey();
					ContactTuple tuple = savedMapEntry.getValue();
					ContactInfo contactInfo = tuple.getContact();

					if (ignoreUnknownContacts && (null == contactInfo.getName()))
					{
						continue;
					}

					if (onHike != HikeConstants.BOTH_VALUE)
					{
						if (!(contactInfo.isOnhike() == (onHike == 1 ? true : false)))
						{
							continue;
						}

						if (onHike == HikeConstants.NOT_ON_HIKE_VALUE)
						{
							if (!msisdn.contains("+91"))
							{
								continue;
							}
						}
					}

					if (!nativeSMSOn)
					{
						if (!(contactInfo.isOnhike() || msisdn.contains("+91")))
						{
							continue;
						}
					}

					flag = false;
					for (FavoriteType favType : favoriteType)
					{
						if (contactInfo.getFavoriteType() == favType)
						{
							flag = true;
							break;
						}
					}

					if (flag && !blockSet.contains(msisdn) && !msisdn.equals(myMsisdn))
					{
						contacts.add(contactInfo);
					}
				}
			}
			finally
			{
				readLock.unlock();
			}
		}
		else
		{
			Map<String, ContactInfo> map = hDb.getContactsOfFavoriteTypeDB(favoriteType, onHike, myMsisdn, nativeSMSOn, ignoreUnknownContacts);
			if (map != null)
			{
				contacts.addAll(map.values());
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
		List<ContactInfo> contacts = new ArrayList<ContactInfo>();
		if (allContactsLoaded)
		{
			readLock.lock();
			try
			{
				for (Entry<String, ContactTuple> mapEntry : transientContacts.entrySet())
				{
					String msisdn = mapEntry.getKey();
					ContactInfo contactInfo = mapEntry.getValue().getContact();
					if (msisdnsIn.contains(msisdn) && (!msisdnsNotIn.contains(msisdn)) && !msisdn.equals(myMsisdn) && contactInfo.isOnhike())
					{
						contacts.add(contactInfo);
						limit--;
						if (limit == 0)
							break;
					}
				}
			}
			finally
			{
				readLock.unlock();
			}
		}
		else
		{
			contacts = hDb.getHikeContacts(limit, msisdnsIn, msisdnsNotIn, myMsisdn);
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

			readLock.lock();
			try
			{
				for (Entry<String, ContactTuple> savedMapEntry : transientContacts.entrySet())
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
			finally
			{
				readLock.unlock();
			}
		}
		else
		{
			contacts = hDb.getNonHikeContacts();
		}
		return contacts;
	}

	/**
	 * 
	 * @param limit
	 * @return
	 */
	List<ContactInfo> getNonHikeMostContactedContacts(String phoneNumbers, final Map<String, Integer> mostContactedValues, int limit)
	{
		List<ContactInfo> contacts = new ArrayList<ContactInfo>();
		if (allContactsLoaded)
		{
			readLock.lock();
			try
			{
				for (Entry<String, ContactTuple> savedMapEntry : transientContacts.entrySet())
				{
					String msisdn = savedMapEntry.getKey();
					ContactInfo contactInfo = savedMapEntry.getValue().getContact();
					if (phoneNumbers.contains(msisdn) && !contactInfo.isOnhike())
					{
						contacts.add(contactInfo);
						limit = limit - 1;
						if (limit == 0)
							break;
					}
				}
			}
			finally
			{
				readLock.unlock();
			}

			Collections.sort(contacts, new Comparator<ContactInfo>()
			{
				@Override
				public int compare(ContactInfo lhs, ContactInfo rhs)
				{
					int lhsContactNum = mostContactedValues.get(lhs.getPhoneNum());
					int rhsContactNum = mostContactedValues.get(rhs.getPhoneNum());

					if (lhsContactNum != rhsContactNum)
					{
						return -((Integer) lhsContactNum).compareTo(rhsContactNum);
					}
					return lhs.getName().toLowerCase().compareTo(rhs.getName().toLowerCase());
				}
			});
		}
		else
		{
			contacts = hDb.getNonHikeMostContactedContactsFromListOfNumbers(phoneNumbers, mostContactedValues, limit);
		}
		return contacts;
	}

	/**
	 * Traverse through all the contacts in transient cache to find if any contactInfo object contains given number as phoneNumber. This method returns null if not found in memory
	 * and not makes a DB call.
	 * 
	 * @param number
	 * @return
	 */
	public ContactInfo getContactInfoFromPhoneNo(String number)
	{
		ContactInfo contact = null;
		if (allContactsLoaded)
		{
			readLock.lock();
			try
			{
				for (Entry<String, ContactTuple> mapEntry : transientContacts.entrySet())
				{
					ContactTuple tuple = mapEntry.getValue();
					if (null != tuple && tuple.getContact().getPhoneNum().equals(number))
					{
						contact = tuple.getContact();
						break;
					}
				}
			}
			finally
			{
				readLock.unlock();
			}
		}
		else
		{
			contact = hDb.getContactInfoFromPhoneNo(number);
			if (null == getContact(contact.getMsisdn()))
			{
				insertContact(contact);
			}
		}
		return contact;
	}

	/**
	 * This method is used when <code>number</code> can be msisdn or phone number . First we check in cache assuming number is msisdn , if not found then traverse through all the
	 * contacts in transient cache to find if any contactInfo object contains given number as phoneNumber And if not found in transient cache we make a DB call.
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
			readLock.lock();
			try
			{
			for (Entry<String, ContactTuple> mapEntry : transientContacts.entrySet())
			{
				ContactTuple tuple = mapEntry.getValue();
				if (null != tuple && tuple.getContact().getPhoneNum().equals(number))
				{
					contact = tuple.getContact();
					break;
				}
			}
			}
			finally
			{
				readLock.unlock();
			}
		}
		else
		{
			contact = hDb.getContactInfoFromPhoneNoOrMsisdn(number);
			if (null == getContact(contact.getMsisdn()))
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
		writeLock.lock();
		try
		{
			ContactTuple tuple = transientContacts.get(contact.getMsisdn());
			if (null != tuple)
			{
				tuple.getContact().setName(null);
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
			if (null != transientContacts)
			{
				transientContacts.clear();
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
	Pair<Map<String, Pair<GroupParticipant, String>>, List<String>> getGroupParticipants(String groupId, boolean activeOnly, boolean notShownStatusMsgOnly,
			boolean fetchParticipants)
	{
		Map<String, Pair<GroupParticipant, String>> groupParticipantsMap = null;
		readLock.lock();
		try
		{
			groupParticipantsMap = groupParticipants.get(groupId);
			if (null != groupParticipantsMap)
			{
				return new Pair<Map<String, Pair<GroupParticipant, String>>, List<String>>(groupParticipantsMap, null);
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
			ContactTuple tuple = transientContacts.get(msisdn);
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

	/**
	 * Returns the count of number of participants in a particular group.
	 * 
	 * @param groupId
	 * @return
	 */
	int getGroupParticipantsCount(String groupId)
	{
		Map<String, Pair<GroupParticipant, String>> g = groupParticipants.get(groupId);
		if (null != g)
		{
			return g.size();
		}
		return HikeConversationsDatabase.getInstance().getActiveParticipantCount(groupId);
	}

	boolean doesContactExist(String msisdn)
	{
		ContactInfo contact = getContact(msisdn);
		if (allContactsLoaded)
		{
			return (null != contact);
		}
		else
		{
			if (null != contact)
				return true;
			else
			{
				return hDb.doesContactExist(msisdn);
			}
		}
	}
}
