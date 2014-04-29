package com.bsb.hike.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.adapters.FriendsAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.utils.Utils;

public class FetchFriendsTask extends AsyncTask<Void, Void, Void>
{
	private Context context;

	private FriendsAdapter friendsAdapter;

	private Map<String, ContactInfo> selectedPeople;

	private List<ContactInfo> groupTaskList;

	private List<ContactInfo> friendTaskList;

	private List<ContactInfo> hikeTaskList;

	private List<ContactInfo> smsTaskList;

	private List<ContactInfo> groupsList;

	private List<ContactInfo> friendsList;

	private List<ContactInfo> hikeContactsList;

	private List<ContactInfo> smsContactsList;

	private List<ContactInfo> filteredGroupsList;

	private List<ContactInfo> filteredFriendsList;

	private List<ContactInfo> filteredSmsContactsList;

	private List<ContactInfo> filteredHikeContactsList;

	private String existingGroupId;

	private boolean fetchGroups = false;

	public FetchFriendsTask(FriendsAdapter friendsAdapter, Context context, List<ContactInfo> friendsList, List<ContactInfo> hikeContactsList, List<ContactInfo> smsContactsList,
			List<ContactInfo> filteredFriendsList, List<ContactInfo> filteredHikeContactsList, List<ContactInfo> filteredSmsContactsList)
	{
		this(friendsAdapter, context, friendsList, hikeContactsList, smsContactsList, filteredFriendsList, filteredHikeContactsList, filteredSmsContactsList, null, null, null,
				false, null);
	}

	public FetchFriendsTask(FriendsAdapter friendsAdapter, Context context, List<ContactInfo> friendsList, List<ContactInfo> hikeContactsList, List<ContactInfo> smsContactsList,
			List<ContactInfo> filteredFriendsList, List<ContactInfo> filteredHikeContactsList, List<ContactInfo> filteredSmsContactsList, List<ContactInfo> groupsList,
			List<ContactInfo> filteredGroupsList, Map<String, ContactInfo> selectedPeople, boolean fetchGroups, String existingGroupId)
	{
		this.friendsAdapter = friendsAdapter;

		this.context = context;

		this.groupsList = groupsList;
		this.friendsList = friendsList;
		this.hikeContactsList = hikeContactsList;
		this.smsContactsList = smsContactsList;

		this.filteredGroupsList = filteredGroupsList;
		this.filteredFriendsList = filteredFriendsList;
		this.filteredHikeContactsList = filteredHikeContactsList;
		this.filteredSmsContactsList = filteredSmsContactsList;

		this.selectedPeople = selectedPeople;

		this.fetchGroups = fetchGroups;
		this.existingGroupId = existingGroupId;
	}

	@Override
	protected Void doInBackground(Void... params)
	{
		String myMsisdn = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.MSISDN_SETTING, "");

		boolean nativeSMSOn = Utils.getSendSmsPref(context);

		boolean removeExistingParticipants = !TextUtils.isEmpty(existingGroupId);

		if (fetchGroups)
		{
			groupTaskList = HikeConversationsDatabase.getInstance().getGroupNameAndParticipantsAsContacts(context);
		}

		HikeUserDatabase hikeUserDatabase = HikeUserDatabase.getInstance();

		friendTaskList = hikeUserDatabase.getContactsOfFavoriteType(FavoriteType.FRIEND, HikeConstants.BOTH_VALUE, myMsisdn, nativeSMSOn);
		friendTaskList.addAll(hikeUserDatabase.getContactsOfFavoriteType(FavoriteType.REQUEST_RECEIVED, HikeConstants.BOTH_VALUE, myMsisdn, nativeSMSOn, false));
		friendTaskList.addAll(hikeUserDatabase.getContactsOfFavoriteType(FavoriteType.REQUEST_SENT, HikeConstants.BOTH_VALUE, myMsisdn, nativeSMSOn));
		friendTaskList.addAll(hikeUserDatabase.getContactsOfFavoriteType(FavoriteType.REQUEST_SENT_REJECTED, HikeConstants.BOTH_VALUE, myMsisdn, nativeSMSOn));
		Collections.sort(friendTaskList, ContactInfo.lastSeenTimeComparator);

		hikeTaskList = hikeUserDatabase.getContactsOfFavoriteType(FavoriteType.NOT_FRIEND, HikeConstants.ON_HIKE_VALUE, myMsisdn, nativeSMSOn);
		hikeTaskList.addAll(hikeUserDatabase.getContactsOfFavoriteType(FavoriteType.REQUEST_RECEIVED_REJECTED, HikeConstants.ON_HIKE_VALUE, myMsisdn, nativeSMSOn, true));
		Collections.sort(hikeTaskList);

		smsTaskList = hikeUserDatabase.getContactsOfFavoriteType(FavoriteType.NOT_FRIEND, HikeConstants.NOT_ON_HIKE_VALUE, myMsisdn, nativeSMSOn);
		Collections.sort(smsTaskList);

		if (removeExistingParticipants)
		{
			Map<String, GroupParticipant> groupParticipants = HikeConversationsDatabase.getInstance().getGroupParticipants(existingGroupId, true, false);

			removeContactsFromList(friendTaskList, groupParticipants);
			removeContactsFromList(hikeTaskList, groupParticipants);
			removeContactsFromList(smsTaskList, groupParticipants);

			for (GroupParticipant groupParticipant : groupParticipants.values())
			{
				ContactInfo contactInfo = groupParticipant.getContactInfo();

				selectedPeople.put(contactInfo.getMsisdn(), contactInfo);
			}
		}

		return null;
	}

	private void removeContactsFromList(List<ContactInfo> contactList, Map<String, GroupParticipant> groupParticipants)
	{
		List<Integer> indicesToRemove = new ArrayList<Integer>();

		for (int i = contactList.size() - 1; i >= 0; i--)
		{
			ContactInfo contactInfo = contactList.get(i);
			if (groupParticipants.containsKey(contactInfo.getMsisdn()))
			{
				indicesToRemove.add(i);
			}
		}

		for (Integer i : indicesToRemove)
		{
			contactList.remove(i.intValue());
		}
	}

	@Override
	protected void onPostExecute(Void result)
	{
		if (fetchGroups)
		{
			groupsList.addAll(groupTaskList);
		}
		friendsList.addAll(friendTaskList);
		hikeContactsList.addAll(hikeTaskList);
		smsContactsList.addAll(smsTaskList);

		if (fetchGroups)
		{
			filteredGroupsList.addAll(groupTaskList);
		}
		filteredFriendsList.addAll(friendTaskList);
		filteredHikeContactsList.addAll(hikeTaskList);
		filteredSmsContactsList.addAll(smsTaskList);

		friendsAdapter.setListFetchedOnce(true);

		friendsAdapter.makeCompleteList(true);
	}
}