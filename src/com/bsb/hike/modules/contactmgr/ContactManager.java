/**
 * 
 */
package com.bsb.hike.modules.contactmgr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import com.bsb.hike.db.DbException;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.FtueContactsData;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.modules.iface.ITransientCache;
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
		hDb.updateContactRecency(msisdn, timestamp);
	}

	public void block(String msisdn)
	{
		hDb.block(msisdn);
	}

	public void toggleContactFavorite(String msisdn, FavoriteType ftype)
	{
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
		hDb.setHikeJoinTime(msisdn, hikeJoinTime);
	}

	public void setIcon(String msisdn, byte[] data, boolean b)
	{
		hDb.setIcon(msisdn, data, b);
	}

	public FavoriteType getFriendshipStatus(String msisdn)
	{
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
		hDb.updateLastSeenTime(msisdn, lastSeenTime);
	}

	public void updateIsOffline(String msisdn, int isOffline)
	{
		hDb.updateIsOffline(msisdn, isOffline);
	}

	public boolean doesContactExist(String msisdn)
	{
		return hDb.doesContactExist(msisdn);
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
		
	
}
