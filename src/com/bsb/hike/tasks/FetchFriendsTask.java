package com.bsb.hike.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.adapters.FriendsAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.NUXManager;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.Utils;

public class FetchFriendsTask extends AsyncTask<Void, Void, Void>
{
	private Context context;

	private FriendsAdapter friendsAdapter;

	private Map<String, ContactInfo> selectedPeople;

	private List<ContactInfo> groupTaskList;

	private List<ContactInfo> friendTaskList;
	
	private List<ContactInfo> nuxRecommendedTaskList;
	
	private Set<ContactInfo> nuxHideTaskList;

	private List<ContactInfo> hikeTaskList;

	private List<ContactInfo> smsTaskList;
	
	private List <ContactInfo> recentTaskList;
	
	private List<ContactInfo> recentlyJoinedTaskList;

	private List<ContactInfo> groupsList;

	private List<ContactInfo> friendsList;

	private List<ContactInfo> hikeContactsList;

	private List<ContactInfo> smsContactsList;
	
	private List<ContactInfo> recentContactsList;
	
	private List<ContactInfo> recentlyJoinedContactsList;

	private List<ContactInfo> groupsStealthList;

	private List<ContactInfo> friendsStealthList;

	private List<ContactInfo> hikeStealthContactsList;

	private List<ContactInfo> smsStealthContactsList;

	private List<ContactInfo> recentsStealthList;

	private List<ContactInfo> filteredGroupsList;
	
	private List<ContactInfo> filteredRecentsList;
	
	private List<ContactInfo> filteredRecentlyJoinedList;

	private List<ContactInfo> filteredFriendsList;

	private List<ContactInfo> filteredSmsContactsList;

	private List<ContactInfo> filteredHikeContactsList;

	private String existingGroupId;

	private String sendingMsisdn;

	private boolean fetchGroups = false;

	private boolean creatingOrEditingGroup = false;

	private int stealthMode;

	private Map<String, StatusMessage> lastStatusMessagesMap;

	private boolean fetchSmsContacts;
	
	private boolean fetchHikeContacts;
	
	private boolean fetchRecents;
	
	private boolean fetchRecentlyJoined;

	boolean checkFavTypeInComparision;

	private boolean nativeSMSOn;

	private boolean showDefaultEmptyList;

	public FetchFriendsTask(FriendsAdapter friendsAdapter, Context context, List<ContactInfo> friendsList, List<ContactInfo> hikeContactsList, List<ContactInfo> smsContactsList,
			List<ContactInfo> recentContactsList,List<ContactInfo> recentlyJoinedHikeContactsList, List<ContactInfo> friendsStealthList, List<ContactInfo> hikeStealthContactsList, List<ContactInfo> smsStealthContactsList, List<ContactInfo> recentsStealthList, List<ContactInfo> filteredFriendsList,
			List<ContactInfo> filteredHikeContactsList, List<ContactInfo> filteredSmsContactsList, boolean fetchSmsContacts, boolean checkFavTypeInComparision, boolean fetchRecents, boolean fetchRecentlyJoined, boolean showDefaultEmptyList, boolean fetchHikeContacts)
	{
		this(friendsAdapter, context, friendsList, hikeContactsList, smsContactsList, recentContactsList, recentlyJoinedHikeContactsList,friendsStealthList, hikeStealthContactsList, smsStealthContactsList, recentsStealthList, filteredFriendsList,
				filteredHikeContactsList, filteredSmsContactsList, null,null, null, null, null, null, null, false, null, false, fetchSmsContacts, checkFavTypeInComparision, fetchRecents , fetchRecentlyJoined, showDefaultEmptyList, fetchHikeContacts);
	}

	public FetchFriendsTask(FriendsAdapter friendsAdapter, Context context, List<ContactInfo> friendsList, List<ContactInfo> hikeContactsList, List<ContactInfo> smsContactsList, List<ContactInfo> recentContactsList, List<ContactInfo> recentlyJoinedHikeContactsList,
			List<ContactInfo> friendsStealthList, List<ContactInfo> hikeStealthContactsList, List<ContactInfo> smsStealthContactsList, List<ContactInfo> recentsStealthList, List<ContactInfo> filteredFriendsList,
			List<ContactInfo> filteredHikeContactsList, List<ContactInfo> filteredSmsContactsList, List<ContactInfo> groupsList, List<ContactInfo> groupsStealthList,
			List<ContactInfo> filteredGroupsList, List<ContactInfo> filteredRecentsList,List<ContactInfo> filteredRecentlyJoinedContactsList, Map<String, ContactInfo> selectedPeople, String sendingMsisdn, boolean fetchGroups, String existingGroupId, boolean creatingOrEditingGrou,
			boolean fetchSmsContacts, boolean checkFavTypeInComparision, boolean fetchRecents , boolean fetchRecentlyJoined, boolean showDefaultEmptyList, boolean fetchHikeContacts)
	{
		this.friendsAdapter = friendsAdapter;

		this.context = context;

		this.groupsList = groupsList;
		this.friendsList = friendsList;
		this.hikeContactsList = hikeContactsList;
		this.smsContactsList = smsContactsList;
		this.recentContactsList = recentContactsList;
		this.recentlyJoinedContactsList = recentlyJoinedHikeContactsList;

		this.groupsStealthList = groupsStealthList;
		this.friendsStealthList = friendsStealthList;
		this.hikeStealthContactsList = hikeStealthContactsList;
		this.smsStealthContactsList = smsStealthContactsList;
		this.recentsStealthList = recentsStealthList;

		this.filteredGroupsList = filteredGroupsList;
		this.filteredFriendsList = filteredFriendsList;
		this.filteredHikeContactsList = filteredHikeContactsList;
		this.filteredSmsContactsList = filteredSmsContactsList;
		this.filteredRecentsList = filteredRecentsList;
		this.filteredRecentlyJoinedList = filteredRecentlyJoinedContactsList;
		
		this.selectedPeople = selectedPeople;

		this.fetchGroups = fetchGroups;
		this.existingGroupId = existingGroupId;
		this.sendingMsisdn = sendingMsisdn;

		this.creatingOrEditingGroup = creatingOrEditingGroup;

		this.fetchSmsContacts = fetchSmsContacts;
		this.fetchHikeContacts = fetchHikeContacts;
		this.checkFavTypeInComparision = checkFavTypeInComparision;
		this.fetchRecents = fetchRecents;
		this.fetchRecentlyJoined = fetchRecentlyJoined;

		this.showDefaultEmptyList = showDefaultEmptyList;
		this.stealthMode = HikeSharedPreferenceUtil.getInstance(context).getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);

		this.nativeSMSOn = Utils.getSendSmsPref(context);
	}

	@Override
	protected Void doInBackground(Void... params)
	{
		long startTime = System.currentTimeMillis();
		String myMsisdn = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.MSISDN_SETTING, "");

		boolean removeExistingParticipants = !TextUtils.isEmpty(existingGroupId);
		if (fetchGroups)
		{
			groupTaskList = HikeMessengerApp.getContactManager().getConversationGroupsAsContacts(true);
			removeSendingMsisdnAndStealthContacts(groupTaskList, groupsStealthList, true);
		}

		long queryTime = System.currentTimeMillis();
		List<ContactInfo> allContacts = HikeMessengerApp.getContactManager().getAllContacts();
		Set<String> blockSet = ContactManager.getInstance().getBlockedMsisdnSet();
		
		NUXManager nuxManager = NUXManager.getInstance(context);
		
		
		if(nuxManager.is_NUX_Active()){
//			nuxHideTaskList = nuxManager.getNuxSelectFriendsPojo().getRecoList();
//			blockSet.addAll(nuxHideTaskList);
		}
		if(fetchRecents)
		{
			List<ContactInfo> convContacts = HikeMessengerApp.getContactManager().getAllConversationContactsSorted(true, false);
			recentTaskList = new ArrayList<ContactInfo>();

			for(ContactInfo recentContact : convContacts)
			{
				if(recentTaskList.size() >= HikeConstants.MAX_RECENTS_TO_SHOW)
					break;
			    String msisdn = recentContact.getMsisdn();
			    boolean hideStealthMsisdn = HikeMessengerApp.isStealthMsisdn(msisdn) && stealthMode != HikeConstants.STEALTH_ON;
			    boolean removeSendingMsisdn = (sendingMsisdn!=null && sendingMsisdn.equals(msisdn));
			    if (blockSet.contains(msisdn) || HikeMessengerApp.hikeBotNamesMap.containsKey(msisdn) || myMsisdn.equals(msisdn) || hideStealthMsisdn || removeSendingMsisdn)
			    {
			    	continue;
			    }
			    recentTaskList.add(recentContact);
			}
		}
		
		Logger.d("TestQuery", "query time: " + (System.currentTimeMillis() - queryTime));

		friendTaskList = new ArrayList<ContactInfo>();
		hikeTaskList = new ArrayList<ContactInfo>();
		smsTaskList = new ArrayList<ContactInfo>();
		recentlyJoinedTaskList = new ArrayList<ContactInfo>();
		nuxRecommendedTaskList = new ArrayList<ContactInfo>();
		
		if(nuxManager.is_NUX_Active()){
			nuxRecommendedTaskList = nuxManager.getRecommendedContacts();
		}
		
		long iterationTime = System.currentTimeMillis();
		for (ContactInfo contactInfo : allContacts)
		{
			String msisdn = contactInfo.getMsisdn();
			if (msisdn.equals(myMsisdn) || HikeMessengerApp.hikeBotNamesMap.containsKey(msisdn))
			{
				continue;
			}
			if (blockSet.contains(msisdn) || (sendingMsisdn!=null && sendingMsisdn.equals(msisdn)))
			{
				continue;
			}
			if(fetchRecents && recentTaskList.contains(contactInfo))
			{
				continue;
			}
			FavoriteType favoriteType = contactInfo.getFavoriteType();

			addToRecentlyJoinedIfNeeded(contactInfo);
			if (shouldAddToFavorites(favoriteType))
			{
				friendTaskList.add(contactInfo);
			}
			else
			{	
				if (null != contactInfo.getName())
				{
					if (contactInfo.isOnhike())
					{
						hikeTaskList.add(contactInfo);
					}
					else if (fetchSmsContacts && shouldShowSmsContact(msisdn))
					{
						smsTaskList.add(contactInfo);
					}
				}
			}
		}

		Logger.d("TestQuery", "Iteration time: " + (System.currentTimeMillis() - iterationTime));

		long sortTime = System.currentTimeMillis();
		Collections.sort(friendTaskList, checkFavTypeInComparision ? ContactInfo.lastSeenTimeComparator : ContactInfo.lastSeenTimeComparatorWithoutFav);
		Logger.d("TestQuery", "Sorting time: " + (System.currentTimeMillis() - sortTime));

		Collections.sort(recentlyJoinedTaskList , new Comparator<ContactInfo>()
		{
			@Override
			public int compare(ContactInfo lhs, ContactInfo rhs)
			{
				return (lhs.getHikeJoinTime() < rhs.getHikeJoinTime()) ? 1 : -1;
			}
		});
		
		/*
		 * 
		 */
		if (recentlyJoinedTaskList.size() > HikeConstants.MAX_RECENTLY_JOINED_HIKE_TO_SHOW)
		{
			recentlyJoinedTaskList = recentlyJoinedTaskList.subList(0, HikeConstants.MAX_RECENTLY_JOINED_HIKE_TO_SHOW);
		}
		
		hikeTaskList.removeAll(recentlyJoinedTaskList);
		friendTaskList.removeAll(recentlyJoinedTaskList);
		
		if (removeExistingParticipants)
		{
			List<PairModified<GroupParticipant,String>> groupParticipantsList = ContactManager.getInstance().getGroupParticipants(existingGroupId, true, false);
			Map<String, PairModified<GroupParticipant, String>> groupParticipants = new HashMap<String, PairModified<GroupParticipant,String>>();
			for(PairModified<GroupParticipant,String> grpParticipant : groupParticipantsList)
			{
				String msisdn = grpParticipant.getFirst().getContactInfo().getMsisdn();
				groupParticipants.put(msisdn, grpParticipant);
			}

			removeContactsFromList(friendTaskList, groupParticipants);
			if(fetchHikeContacts){
				removeContactsFromList(hikeTaskList, groupParticipants);
			}
			if (fetchSmsContacts)
			{
				removeContactsFromList(smsTaskList, groupParticipants);
			}

			for (PairModified<GroupParticipant,String> groupParticipant : groupParticipants.values())
			{
				ContactInfo contactInfo = groupParticipant.getFirst().getContactInfo();

				selectedPeople.put(contactInfo.getMsisdn(), contactInfo);
			}
		}
		addToStealthList(friendTaskList, friendsStealthList, false);
		if(fetchHikeContacts){
			addToStealthList(hikeTaskList, hikeStealthContactsList, false);
		}
		if (fetchSmsContacts)
		{
			addToStealthList(smsTaskList, smsStealthContactsList, false);
		}

		lastStatusMessagesMap = HikeConversationsDatabase.getInstance().getLastStatusMessages(false, HikeConstants.STATUS_TYPE_LIST_TO_FETCH, friendTaskList);

		Logger.d("TestQuery", "total time: " + (System.currentTimeMillis() - startTime));

		return null;
	}

	private void addToRecentlyJoinedIfNeeded(ContactInfo contactInfo)
	{
		if(fetchRecentlyJoined && contactInfo.isOnhike() && !contactInfo.isUnknownContact())
		{
			if(stealthMode != HikeConstants.STEALTH_ON && HikeMessengerApp.isStealthMsisdn(contactInfo.getMsisdn()))
			{
				return;
			}
			long hikeJoinTime = contactInfo.getHikeJoinTime();
			if(hikeJoinTime > 0)
			{
				recentlyJoinedTaskList.add(contactInfo);
			}
		}
	}

	private boolean shouldAddToFavorites(FavoriteType favoriteType)
	{
		return favoriteType == FavoriteType.REQUEST_RECEIVED || favoriteType == FavoriteType.FRIEND || favoriteType == FavoriteType.REQUEST_SENT
				|| favoriteType == FavoriteType.REQUEST_SENT_REJECTED;
	}

	private boolean shouldShowSmsContact(String msisdn)
	{
		if (TextUtils.isEmpty(msisdn))
		{
			return false;
		}

		if (!nativeSMSOn)
		{
			return msisdn.startsWith(HikeConstants.INDIA_COUNTRY_CODE);
		}

		return true;
	}

	private void addToStealthList(List<ContactInfo> contactList, List<ContactInfo> stealthList, boolean isGroupTask)
	{
		if (creatingOrEditingGroup)
		{
			return;
		}

		for (Iterator<ContactInfo> iter = contactList.iterator(); iter.hasNext();)
		{
			ContactInfo contactInfo = iter.next();
			/*
			 * if case of group contactInfo.getId() will retrun groupId, which is treated as msisdn for groups.
			 */
			String msisdn = isGroupTask ? contactInfo.getId() : contactInfo.getMsisdn();
			if (HikeMessengerApp.isStealthMsisdn(msisdn))
			{
				stealthList.add(contactInfo);

				/*
				 * If stealth mode is currently off, we should remove these contacts from the list.
				 */
				if (stealthMode != HikeConstants.STEALTH_ON)
				{
					iter.remove();
				}
			}
		}
	}

	private void removeContactsFromList(List<ContactInfo> contactList, Map<String, PairModified<GroupParticipant, String>> groupParticipants)
	{
		for (Iterator<ContactInfo> iter = contactList.iterator(); iter.hasNext();)
		{
			ContactInfo contactInfo = iter.next();
			if (groupParticipants.containsKey(contactInfo.getMsisdn()))
			{
				iter.remove();
			}
		}
	}

	private void removeSendingMsisdnAndStealthContacts(List<ContactInfo> contactList, List<ContactInfo> stealthList, boolean isGroupTask)
	{
		for (Iterator<ContactInfo> iter = contactList.iterator(); iter.hasNext();)
		{
			ContactInfo contactInfo = iter.next();
			/*
			 * if case of group contactInfo.getId() will retrun groupId, which is treated as msisdn for groups.
			 */
			String msisdn = isGroupTask ? contactInfo.getId() : contactInfo.getMsisdn();
			if (HikeMessengerApp.isStealthMsisdn(msisdn) && !creatingOrEditingGroup)
			{
				stealthList.add(contactInfo);

				/*
				 * If stealth mode is currently off, we should remove these contacts from the list.
				 */
				if (stealthMode != HikeConstants.STEALTH_ON)
				{
					iter.remove();
				}
			}
			if(sendingMsisdn!=null && sendingMsisdn.equals(msisdn))
			{
				iter.remove();
			}
		}
	}

	@Override
	protected void onPostExecute(Void result)
	{
		/*
		 * Clearing all the lists initially to ensure we remove any existing contacts in the list that might be there because of the 'ai' packet.
		 */
		clearAllLists();
		
		if (fetchGroups)
		{
			groupsList.addAll(groupTaskList);
		}
		if(fetchRecents)
		{
			recentContactsList.addAll(recentTaskList);
		}
		friendsAdapter.initiateLastStatusMessagesMap(lastStatusMessagesMap);
		friendsList.addAll(friendTaskList);
		
		if(fetchHikeContacts){
			hikeContactsList.addAll(hikeTaskList);
		}
		if(fetchRecents)
		{
			filteredRecentsList.addAll(recentTaskList);
		}

		if (fetchSmsContacts)
		{
			smsContactsList.addAll(smsTaskList);
		}

		if (fetchGroups)
		{
			filteredGroupsList.addAll(groupTaskList);
		}
		filteredFriendsList.addAll(friendTaskList);
		if(fetchHikeContacts){
			filteredHikeContactsList.addAll(hikeTaskList);
		}
		if (fetchSmsContacts)
		{
			filteredSmsContactsList.addAll(smsTaskList);
		}

		if(fetchRecentlyJoined)
		{
			recentlyJoinedContactsList.addAll(recentlyJoinedTaskList);
			filteredRecentlyJoinedList.addAll(recentlyJoinedTaskList);
		}
		friendsAdapter.setListFetchedOnce(true);
		// We dont need to show contacts in NUX Invite screen
		if(showDefaultEmptyList)
		{
			friendsAdapter.setEmptyView();
		}
		else
		{
			friendsAdapter.makeCompleteList(true, true);
		}
	}

	private void clearAllLists()
	{
		if (fetchGroups)
		{
			groupsList.clear();
			filteredGroupsList.clear();
		}

		if(fetchRecents)
		{
			recentContactsList.clear();
			filteredRecentsList.clear();
		}

		if(fetchRecentlyJoined)
		{
			recentlyJoinedContactsList.clear();
			filteredRecentlyJoinedList.clear();
		}
		
		friendsList.clear();
		hikeContactsList.clear();
		smsContactsList.clear();
		filteredFriendsList.clear();
		filteredHikeContactsList.clear();
		filteredSmsContactsList.clear();
	}
}