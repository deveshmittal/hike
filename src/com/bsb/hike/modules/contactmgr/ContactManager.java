/**
 * 
 */
package com.bsb.hike.modules.contactmgr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;
import android.util.Pair;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.DbException;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.FtueContactsData;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.modules.iface.ITransientCache;
import com.bsb.hike.utils.AccountUtils;
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

	private HikeUserDatabase hDb;

	private Context context;

	private ContactManager()
	{
	}

	public static ContactManager getInstance()
	{
		if (_instance == null)
		{
			synchronized (ContactManager.class)
			{
				if (_instance == null)
				{
					_instance = new ContactManager();
				}
			}
		}
		return _instance;
	}

	public void init(Context ctx)
	{
		context = ctx.getApplicationContext();
		hDb = new HikeUserDatabase(ctx);
		persistenceCache = new PersistenceCache(hDb);
		transientCache = new TransientCache(hDb);
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
	public void clearCache()
	{
		persistenceCache.clearMemory();
		transientCache.clearMemory();
	}

	/**
	 * This method should be used when a conversation gets deleted or last sender in group is changed
	 * 
	 * @param msisdn
	 * @param ifOneToOneConversation
	 */
	public void removeContact(String msisdn, boolean ifOneToOneConversation)
	{
		persistenceCache.removeContact(msisdn, ifOneToOneConversation);
	}

	/**
	 * This is used to remove the list of msisdns from either group or 1-1 conversation
	 * 
	 * @param msisdns
	 */
	public void removeContacts(List<String> msisdns)
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
	 * This method is used when contacts are deleted from the addressbook and we set their name to null in the cache
	 * 
	 * @param contacts
	 */
	public void deleteContacts(List<ContactInfo> contacts)
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

	public String getName(String groupId, String msisdn)
	{
		String name = getName(msisdn); // maybe a saved contact in a group
		if (null != name)
			return name;

		name = persistenceCache.getName(groupId, msisdn);
		if (null == name)
		{
			name = transientCache.getName(groupId, msisdn);
		}
		return name;
	}

	public void setUnknownContactName(String grpId, String msisdn, String name)
	{
		persistenceCache.setUnknownContactName(grpId, msisdn, name);
		transientCache.setUnknownContactName(grpId, msisdn, name);
	}

	/**
	 * This function will return name or null for a particular msisdn
	 * <p>
	 * Search in Persistence first, if not found, search in Transient
	 * </p>
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
	 * <p>
	 * Inserts the object in transient memory if loadInTransient is set to true otherwise in persistence memory
	 * </p>
	 * 
	 * @param msisdn
	 * @param loadInTransient
	 * @return
	 */
	public ContactInfo getContact(String msisdn, boolean loadInTransient, boolean ifOneToOneConversation)
	{
		return getContact(msisdn, loadInTransient, ifOneToOneConversation, false);
	}

	/**
	 * Returns the contactInfo for a particular msisdn.If not found in memory makes a db call
	 * <p>
	 * Inserts the object in transient memory if loadInTransient is set to true otherwise in persistence memory
	 * </p>
	 * 
	 * @param msisdn
	 * @param loadInTransient
	 * @param ifNotFoundReturnNull
	 *            if set to true returns null if not a saved contact
	 * @return
	 */
	public ContactInfo getContact(String msisdn, boolean loadInTransient, boolean ifOneToOneConversation, boolean ifNotFoundReturnNull)
	{
		ContactInfo contact = getContact(msisdn);
		if (null == contact)
		{
			if (loadInTransient)
			{
				contact = transientCache.putInCache(msisdn, ifNotFoundReturnNull);
			}
			else
			{
				contact = persistenceCache.putInCache(msisdn, ifNotFoundReturnNull, ifOneToOneConversation);
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
				// move to persistence if found in transient using getcontact used in first line of method (if loadintransient is false)
				ContactInfo con = persistenceCache.getContact(msisdn);
				if (null == con)
				{
					con = transientCache.getContact(msisdn);
					persistenceCache.insertContact(con, ifOneToOneConversation);

				}
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
	public List<ContactInfo> getContact(List<String> msisdns, boolean loadInTransient, boolean ifOneToOneConversation)
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
				contactsDB = transientCache.putInCache(msisdnsDB);
			}
			else
			{
				contactsDB = persistenceCache.putInCache(msisdnsDB, ifOneToOneConversation);
			}

			if (null != contactsDB)
			{
				contacts.addAll(contactsDB);
			}
		}

		if (!loadInTransient)
		{
			// move to persistence if found in transient using getcontact used in first line of method (if loadintransient is false)
			for (String msisdn : msisdns)
			{
				ContactInfo con = persistenceCache.getContact(msisdn);
				if (null == con)
				{
					con = transientCache.getContact(msisdn);
					persistenceCache.insertContact(con, ifOneToOneConversation);
				}
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
		return transientCache.getAllContacts();
	}

	public void removeOlderLastGroupMsisdns(String groupId, List<String> currentGroupMsisdns)
	{
		List<String> msisdns = persistenceCache.removeOlderLastGroupMsisdn(groupId, currentGroupMsisdns);
		List<String> msisdnsDB = new ArrayList<String>();
		for(String ms : msisdns)
		{
			ContactInfo contact = transientCache.getContact(ms);
			if(null == contact)
			{
				msisdnsDB.add(ms);
			}
			else
			{
				persistenceCache.insertContact(contact, false);
			}
		}
		persistenceCache.putInCache(msisdnsDB, false);
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
			List<ContactInfo> contacts = transientCache.getNOTFRIENDScontacts(onHike, myMsisdn, nativeSMSOn, ignoreUnknownContacts);

			if (!transientCache.isAllContactsLoaded())
			{
				// if all contacts are not loaded then we get contacts from DB (using method getNOTFRIENDScontacts) , some contacts can be already in memory either in persistence
				// cache or transient cache . To avoid duplicates we first check in both cache if not found then only insert in transient cache.
				for (ContactInfo con : contacts)
				{
					if (null == getContact(con.getMsisdn()))
					{
						transientCache.insertContact(con);
					}
				}
			}
			return contacts;
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
		ContactInfo contact = persistenceCache.getContactInfoFromPhoneNo(number);
		if (null != contact)
		{
			return contact;
		}
		return transientCache.getContactInfoFromPhoneNo(number);
	}

	public ContactInfo getContactInfoFromPhoneNoOrMsisdn(String number)
	{
		ContactInfo contact = persistenceCache.getContactInfoFromPhoneNoOrMsisdn(number);
		if (null != contact)
		{
			return contact;
		}
		return transientCache.getContactInfoFromPhoneNoOrMsisdn(number);
	}

	@Override
	public void load()
	{
		// TODO Auto-generated method stub

	}

	/**
	 * return true if conversation with this id already exists group and 1-1 conversations both will be checked The implementation is thread safe
	 * 
	 * @param msisdn
	 * @return
	 */
	public boolean isConvExists(String id)
	{
		return persistenceCache.convExist(id);
	}

	/**
	 * Thread safe implementation
	 * 
	 * @param groupId
	 * @return
	 */
	public boolean isGroupExist(String groupId)
	{
		return persistenceCache.isGroupExists(groupId);
	}

	public void insertGroup(String grpId, String groupName)
	{
		persistenceCache.insertGroup(grpId, groupName);
	}

	public List<ContactInfo> getContactsFromDB(boolean b)
	{
		return hDb.getContacts(b);
	}

	public void deleteMultipleContactInDB(Set<String> keySet)
	{
		hDb.deleteMultipleRows(keySet);
	}

	public void updateContactsinDB(List<ContactInfo> updatedContacts)
	{
		hDb.updateContacts(updatedContacts);
	}

	public int updateHikeStatus(String msisdn, boolean onhike)
	{
		ContactInfo contact = getContact(msisdn);
		if (null != contact)
		{
			ContactInfo updatedContact = new ContactInfo(contact);
			updatedContact.setOnhike(onhike);
			updateContacts(updatedContact);
		}
		return hDb.updateHikeContact(msisdn, onhike);
	}

	public Set<String> getBlockedUsers()
	{
		return hDb.getBlockedUsers();
	}

	public boolean hasIcon(String msisdn)
	{
		return hDb.hasIcon(msisdn);
	}

	public void updateContactRecency(String msisdn, long timestamp)
	{
		ContactInfo contact = getContact(msisdn);
		if (null != contact)
		{
			ContactInfo updatedContact = new ContactInfo(contact);
			updatedContact.setLastMessaged(timestamp);
			updateContacts(updatedContact);
		}
		hDb.updateContactRecency(msisdn, timestamp);
	}

	public void block(String msisdn)
	{
		hDb.block(msisdn);
	}

	public void toggleContactFavorite(String msisdn, FavoriteType ftype)
	{
		ContactInfo contact = getContact(msisdn);
		if (null != contact)
		{
			ContactInfo updatedContact = new ContactInfo(contact);
			updatedContact.setFavoriteType(ftype);
			updateContacts(updatedContact);
		}
		hDb.toggleContactFavorite(msisdn, ftype);
	}

	public void unblock(String msisdn)
	{
		hDb.unblock(msisdn);
	}

	public void removeIcon(String id)
	{
		hDb.removeIcon(id);
	}

	public void setHikeJoinTime(String msisdn, long hikeJoinTime)
	{
		ContactInfo contact = getContact(msisdn);
		if (null != contact)
		{
			ContactInfo updatedContact = new ContactInfo(contact);
			updatedContact.setHikeJoinTime(hikeJoinTime);
			updateContacts(updatedContact);
		}
		hDb.setHikeJoinTime(msisdn, hikeJoinTime);
	}

	public void setIcon(String msisdn, byte[] data, boolean b)
	{
		hDb.setIcon(msisdn, data, b);
	}

	public FavoriteType getFriendshipStatus(String msisdn)
	{
		ContactInfo contact = getContact(msisdn);
		if (null != contact)
		{
			return contact.getFavoriteType();
		}
		return hDb.getFriendshipStatus(msisdn);
	}

	public String getIconIdentifierString(String id)
	{
		return hDb.getIconIdentifierString(id);
	}

	public void setMultipleContactsToFavorites(JSONObject favorites)
	{
		hDb.setMultipleContactsToFavorites(favorites);
	}

	public boolean isBlocked(String msisdn)
	{
		return hDb.isBlocked(msisdn);
	}

	public void updateLastSeenTime(String msisdn, long lastSeenTime)
	{
		ContactInfo contact = getContact(msisdn);
		if (null != contact)
		{
			ContactInfo updatedContact = new ContactInfo(contact);
			updatedContact.setLastSeenTime(lastSeenTime);
			updateContacts(updatedContact);
		}
		hDb.updateLastSeenTime(msisdn, lastSeenTime);
	}

	public void updateIsOffline(String msisdn, int isOffline)
	{
		ContactInfo contact = getContact(msisdn);
		if (null != contact)
		{
			ContactInfo updatedContact = new ContactInfo(contact);
			updatedContact.setOffline(isOffline);
			updateContacts(updatedContact);
		}
		hDb.updateIsOffline(msisdn, isOffline);
	}

	public boolean doesContactExist(String msisdn)
	{
		return transientCache.doesContactExist(msisdn);
	}

	public void makeOlderAvatarsRounded()
	{
		hDb.makeOlderAvatarsRounded();
	}

	public Drawable getIcon(String msisdn, boolean rounded)
	{
		return hDb.getIcon(msisdn, rounded);
	}

	public byte[] getIconByteArray(String id, boolean rounded)
	{
		return hDb.getIconByteArray(id, rounded);
	}

	public void deleteAll()
	{
		hDb.deleteAll();
	}

	public Set<String> getBlockedMsisdnSet()
	{
		return hDb.getBlockedMsisdnSet();
	}

	public void setAddressBookAndBlockList(List<ContactInfo> contacts, List<String> blockedMsisdns) throws DbException
	{
		hDb.setAddressBookAndBlockList(contacts, blockedMsisdns);
	}

	public void syncContactExtraInfo()
	{
		hDb.syncContactExtraInfo();
	}

	public List<Pair<AtomicBoolean, ContactInfo>> getBlockedUserList()
	{
		return hDb.getBlockedUserList();
	}

	public FtueContactsData getFTUEContacts(SharedPreferences prefs)
	{
		return hDb.getFTUEContacts(prefs);
	}

	public void updateInvitedTimestamp(String msisdn, long time)
	{
		ContactInfo contact = getContact(msisdn);
		if (null != contact)
		{
			ContactInfo updatedContact = new ContactInfo(contact);
			updatedContact.setInviteTime(time);
			updateContacts(updatedContact);
		}
		hDb.updateInvitedTimestamp(msisdn, time);
	}

	/**
	 * Returns a list of participants to a group
	 * 
	 * @param groupId
	 * @return
	 */
	public Map<String, Pair<GroupParticipant, String>> getGroupParticipants(String groupId, boolean activeOnly, boolean notShownStatusMsgOnly)
	{
		return getGroupParticipants(groupId, activeOnly, notShownStatusMsgOnly, true);
	}

	/**
	 * Returns a list of participants to a group
	 * 
	 * @param groupId
	 * @return
	 */
	public Map<String, Pair<GroupParticipant, String>> getGroupParticipants(String groupId, boolean activeOnly, boolean notShownStatusMsgOnly, boolean fetchParticipants)
	{
		Pair<Map<String, Pair<GroupParticipant, String>>, List<String>> groupPair = transientCache.getGroupParticipants(groupId, activeOnly, notShownStatusMsgOnly,
				fetchParticipants);
		Map<String, Pair<GroupParticipant, String>> groupParticipantsMap = groupPair.first;
		List<String> allMsisdns = groupPair.second;

		if (null != allMsisdns)
		{
			// at least one msisdn is required to run this in query
			if (fetchParticipants && allMsisdns.size() > 0)
			{
				List<ContactInfo> list = new ArrayList<ContactInfo>();
				List<String> msisdnsDB = new ArrayList<String>();

				// traverse all msisdns if found in transient memory increment ref count by one
				for (String ms : allMsisdns)
				{
					ContactInfo contact = transientCache.getContact(ms);
					if (null != contact)
					{
						// increment ref count
						transientCache.incrementRefCount(ms);
						list.add(contact);
					}
					else
					{
						contact = persistenceCache.getContact(ms);
						if (null != contact)
						{
							transientCache.insertContact(contact);
							list.add(contact);
						}
						else
						{
							msisdnsDB.add(ms);
						}
					}
				}

				list.addAll(transientCache.putInCache(msisdnsDB));

				for (ContactInfo contactInfo : list)
				{
					Pair<GroupParticipant, String> groupParticipantPair = groupParticipantsMap.get(contactInfo.getMsisdn());
					if (contactInfo.getName() == null)
					{
						String name = groupParticipantPair.first.getContactInfo().getName();
						setUnknownContactName(groupId, contactInfo.getMsisdn(), name);
						groupParticipantPair.first.setContactInfo(contactInfo);
					}
					else
					{
						groupParticipantPair.first.setContactInfo(contactInfo);
					}
				}
			}
		}

		transientCache.insertGroupParticipants(groupId, groupParticipantsMap);
		return groupParticipantsMap;
	}

	/**
	 * This method adds group participants for a particular <code>groupId</code> into {@link TransientCache}.
	 * @param groupId
	 * @param participantList
	 */
	public void addGroupParticipants(String groupId, Map<String, Pair<GroupParticipant, String>> participantList)
	{
		transientCache.insertGroupParticipants(groupId, participantList);
	}

	/**
	 * This method removes group participant of a particular group from transient cache
	 * @param groupId
	 * @param msisdn
	 */
	public void removeGroupParticipant(String groupId, String msisdn)
	{
		transientCache.removeGroupParticipants(groupId, msisdn);
	}

	/**
	 * Sets the group name in persistence cache , should be called when group name is changed
	 * @param groupId
	 * @param name
	 */
	public void setGroupName(String groupId,String name)
	{
		persistenceCache.setGroupName(groupId, name);
	}
	
	/**
	 * Returns the number of participants in a particular group.
	 * @param groupId
	 * @return
	 */
	public int getActiveParticipantCount(String groupId)
	{
		return transientCache.getGroupParticipantsCount(groupId);
	}
	
	/*
	 * Call this when we think the address book has changed. Checks for updates, posts to the server, writes them to the local database and updates existing conversations
	 */
	public void syncUpdates(Context ctx)
	{

		if (!Utils.isUserOnline(ctx))
		{
			Logger.d("CONTACT UTILS", "Airplane mode is on , skipping sync update tasks.");
			return;
		}

		List<ContactInfo> newContacts = getContacts(ctx);
		if (newContacts == null)
		{
			return;
		}

		Map<String, List<ContactInfo>> new_contacts_by_id = convertToMap(newContacts);
		Map<String, List<ContactInfo>> hike_contacts_by_id = convertToMap(ContactManager.getInstance().getContactsFromDB(false));

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

			updateContacts(updatedContacts);
			
			List<ContactInfo> contactsToDelete = new ArrayList<ContactInfo>();
			
			for(Entry<String,List<ContactInfo>> mapEntry : hike_contacts_by_id.entrySet())
			{
				List<ContactInfo> contacts = mapEntry.getValue();
				contactsToDelete.addAll(contacts);
			}
						
			deleteContacts(contactsToDelete);
			
			/* Delete ids from hike user DB */
			deleteMultipleContactInDB(hike_contacts_by_id.keySet()); 
			updateContactsinDB(updatedContacts);

		}
		catch (Exception e)
		{
			Logger.e("ContactUtils", "error updating addressbook", e);
		}
	}

	private boolean areListsEqual(List<ContactInfo> list1, List<ContactInfo> list2)
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

	public Map<String, List<ContactInfo>> convertToMap(List<ContactInfo> contacts)
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

	public List<ContactInfo> getContacts(Context ctx)
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

	public int updateHikeStatus(Context ctx, String msisdn, boolean onhike)
	{
		ContactInfo contact = getContact(msisdn);
		if (null != contact)
		{
			ContactInfo updatedContact = new ContactInfo(contact);
			updatedContact.setOnhike(onhike);
			updateContacts(updatedContact);
		}
		return updateHikeStatus(msisdn, onhike);
	}

	/**
	 * Used to get the recent contacts where we get the recency from the android contacts table. This method also returns a string which can be used as the argument to a SELECT IN
	 * query
	 * 
	 * @param context
	 * @param limit
	 * @return
	 */
	public Pair<String, Map<String, Long>> getRecentNumbers(Context context, int limit)
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
	public Pair<String, Map<String, Integer>> getMostContactedContacts(Context context, int limit)
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

	private void extractContactInfo(Cursor c, StringBuilder sb, Map<String, Integer> numbers, boolean greenblueContacts)
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

	public void setGreenBlueStatus(Context context, List<ContactInfo> contactinfos)
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

	private void setGreenBlueContacs(Cursor c, List<ContactInfo> contactinfos)
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
