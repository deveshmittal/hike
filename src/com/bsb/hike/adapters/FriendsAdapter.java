package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.PinnedSectionListView.PinnedSectionListAdapter;

public class FriendsAdapter extends BaseAdapter implements OnClickListener,
		PinnedSectionListAdapter {

	public static final int FRIEND_INDEX = 0;
	public static final int HIKE_INDEX = 1;
	public static final int SMS_INDEX = 2;

	public static final String EXTRA_ID = "-910";
	public static final String SECTION_ID = "-911";

	public static final String INVITE_MSISDN = "-123";
	public static final String GROUP_MSISDN = "-124";

	public enum ViewType {
		SECTION, FRIEND, NOT_FRIEND, FRIEND_REQUEST, EXTRA
	}

	private LayoutInflater layoutInflater;

	private List<ContactInfo> completeList;

	private List<ContactInfo> friendsList;
	private List<ContactInfo> hikeContactsList;
	private List<ContactInfo> smsContactsList;
	private List<ContactInfo> filteredFriendsList;
	private List<ContactInfo> filteredHikeContactsList;
	private List<ContactInfo> filteredSmsContactsList;

	private Context context;
	private ContactInfo friendsSection;
	private ContactInfo hikeContactsSection;
	private ContactInfo smsContactsSection;
	private ContactInfo inviteExtraItem;
	private ContactInfo groupExtraItem;
	private ContactFilter contactFilter;
	private String queryText;
	private boolean lastSeenPref;

	public FriendsAdapter(final Context context) {
		this.layoutInflater = LayoutInflater.from(context);
		this.context = context;
		this.contactFilter = new ContactFilter();
		this.lastSeenPref = PreferenceManager.getDefaultSharedPreferences(
				context).getBoolean(HikeConstants.LAST_SEEN_PREF, true);

		completeList = new ArrayList<ContactInfo>();

		friendsList = new ArrayList<ContactInfo>(0);
		hikeContactsList = new ArrayList<ContactInfo>(0);
		smsContactsList = new ArrayList<ContactInfo>(0);

		filteredFriendsList = new ArrayList<ContactInfo>(0);
		filteredHikeContactsList = new ArrayList<ContactInfo>(0);
		filteredSmsContactsList = new ArrayList<ContactInfo>(0);

		FetchFriendsTask fetchFriendsTask = new FetchFriendsTask();
		Utils.executeAsyncTask(fetchFriendsTask);
	}

	private class FetchFriendsTask extends AsyncTask<Void, Void, Void> {

		List<ContactInfo> favoriteTaskList;
		List<ContactInfo> hikeTaskList;
		List<ContactInfo> smsTaskList;

		@Override
		protected Void doInBackground(Void... params) {
			String myMsisdn = context.getSharedPreferences(
					HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(
					HikeMessengerApp.MSISDN_SETTING, "");

			boolean nativeSMSOn = PreferenceManager
					.getDefaultSharedPreferences(context).getBoolean(
							HikeConstants.SEND_SMS_PREF, false);

			HikeUserDatabase hikeUserDatabase = HikeUserDatabase.getInstance();

			favoriteTaskList = hikeUserDatabase.getContactsOfFavoriteType(
					FavoriteType.FRIEND, HikeConstants.BOTH_VALUE, myMsisdn,
					nativeSMSOn);
			favoriteTaskList.addAll(hikeUserDatabase.getContactsOfFavoriteType(
					FavoriteType.REQUEST_RECEIVED, HikeConstants.BOTH_VALUE,
					myMsisdn, nativeSMSOn, false));
			favoriteTaskList.addAll(hikeUserDatabase.getContactsOfFavoriteType(
					FavoriteType.REQUEST_SENT, HikeConstants.BOTH_VALUE,
					myMsisdn, nativeSMSOn));
			favoriteTaskList.addAll(hikeUserDatabase.getContactsOfFavoriteType(
					FavoriteType.REQUEST_SENT_REJECTED,
					HikeConstants.BOTH_VALUE, myMsisdn, nativeSMSOn));
			Collections.sort(favoriteTaskList,
					ContactInfo.lastSeenTimeComparator);

			hikeTaskList = hikeUserDatabase.getContactsOfFavoriteType(
					FavoriteType.NOT_FRIEND, HikeConstants.ON_HIKE_VALUE,
					myMsisdn, nativeSMSOn);
			hikeTaskList.addAll(hikeUserDatabase.getContactsOfFavoriteType(
					FavoriteType.REQUEST_RECEIVED, HikeConstants.ON_HIKE_VALUE,
					myMsisdn, nativeSMSOn, true));
			hikeTaskList.addAll(hikeUserDatabase.getContactsOfFavoriteType(
					FavoriteType.REQUEST_RECEIVED_REJECTED,
					HikeConstants.ON_HIKE_VALUE, myMsisdn, nativeSMSOn, true));
			Collections.sort(hikeTaskList);

			smsTaskList = hikeUserDatabase.getContactsOfFavoriteType(
					FavoriteType.NOT_FRIEND, HikeConstants.NOT_ON_HIKE_VALUE,
					myMsisdn, nativeSMSOn);
			Collections.sort(smsTaskList);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			friendsList = favoriteTaskList;
			hikeContactsList = hikeTaskList;
			smsContactsList = smsTaskList;

			filteredFriendsList.addAll(favoriteTaskList);
			filteredHikeContactsList.addAll(hikeTaskList);
			filteredSmsContactsList.addAll(smsTaskList);
			makeCompleteList(true);
		}
	}

	public void onQueryChanged(String s) {
		queryText = s;
		contactFilter.filter(queryText);
	}

	private class ContactFilter extends Filter {
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();

			if (!TextUtils.isEmpty(constraint)) {

				String textToBeFiltered = constraint.toString().toLowerCase()
						.trim();

				List<ContactInfo> filteredFriendsList = new ArrayList<ContactInfo>();
				List<ContactInfo> filteredHikeContactsList = new ArrayList<ContactInfo>();
				List<ContactInfo> filteredSmsContactsList = new ArrayList<ContactInfo>();

				for (ContactInfo info : friendsList) {
					String name = info.getName();
					if (name != null) {
						name = name.toLowerCase();
						if (name.contains(textToBeFiltered)) {
							filteredFriendsList.add(info);
							continue;
						}
					}

					if (info.getMsisdn() != null) {
						if (info.getMsisdn().contains(textToBeFiltered)) {
							filteredFriendsList.add(info);
						}
					}
				}

				for (ContactInfo info : hikeContactsList) {
					String name = info.getName();
					if (name != null) {
						name = name.toLowerCase();
						if (name.contains(textToBeFiltered)) {
							filteredHikeContactsList.add(info);
							continue;
						}
					}

					if (info.getMsisdn() != null) {
						if (info.getMsisdn().contains(textToBeFiltered)) {
							filteredHikeContactsList.add(info);
						}
					}
				}

				for (ContactInfo info : smsContactsList) {
					String name = info.getName();
					if (name != null) {
						name = name.toLowerCase();
						if (name.contains(textToBeFiltered)) {
							filteredSmsContactsList.add(info);
							continue;
						}
					}

					if (info.getMsisdn() != null) {
						if (info.getMsisdn().contains(textToBeFiltered)) {
							filteredSmsContactsList.add(info);
						}
					}
				}

				List<List<ContactInfo>> resultList = new ArrayList<List<ContactInfo>>(
						3);
				resultList.add(filteredFriendsList);
				resultList.add(filteredHikeContactsList);
				resultList.add(filteredSmsContactsList);

				results.values = resultList;
			} else {
				List<List<ContactInfo>> resultList = new ArrayList<List<ContactInfo>>(
						3);
				resultList.add(friendsList);
				resultList.add(hikeContactsList);
				resultList.add(smsContactsList);

				results.values = resultList;
			}
			results.count = 1;
			return results;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results) {
			List<List<ContactInfo>> resultList = (List<List<ContactInfo>>) results.values;

			filteredFriendsList.clear();
			filteredFriendsList.addAll(resultList.get(0));

			filteredHikeContactsList.clear();
			filteredHikeContactsList.addAll(resultList.get(1));

			filteredSmsContactsList.clear();
			filteredSmsContactsList.addAll(resultList.get(2));

			makeCompleteList(true);
		}
	}

	public void makeCompleteList(boolean filtered) {
		if (!filtered) {
			contactFilter.filter(queryText);
			return;
		}

		completeList.clear();

		inviteExtraItem = new ContactInfo(EXTRA_ID, INVITE_MSISDN,
				context.getString(R.string.invite_friends_hike), null);
		completeList.add(inviteExtraItem);

		groupExtraItem = new ContactInfo(EXTRA_ID, GROUP_MSISDN,
				context.getString(R.string.create_group), null);
		completeList.add(groupExtraItem);

		friendsSection = new ContactInfo(SECTION_ID,
				Integer.toString(filteredFriendsList.size()),
				context.getString(R.string.friends_lower_case), null);
		completeList.add(friendsSection);

		completeList.addAll(filteredFriendsList);

		hikeContactsSection = new ContactInfo(SECTION_ID,
				Integer.toString(filteredHikeContactsList.size()),
				context.getString(R.string.hike_contacts), null);
		completeList.add(hikeContactsSection);

		completeList.addAll(filteredHikeContactsList);

		smsContactsSection = new ContactInfo(SECTION_ID,
				Integer.toString(filteredSmsContactsList.size()),
				context.getString(R.string.sms_contacts), null);
		completeList.add(smsContactsSection);

		completeList.addAll(filteredSmsContactsList);

		notifyDataSetChanged();
	}

	private void removeContactByMatchingMsisdn(List<ContactInfo> contactList,
			ContactInfo contactInfo) {
		for (int i = 0; i < contactList.size(); i++) {
			ContactInfo listContactInfo = contactList.get(i);
			if (listContactInfo.getMsisdn().equals(contactInfo.getMsisdn())) {
				contactList.remove(i);
				break;
			}
		}
	}

	public void removeContact(ContactInfo contactInfo,
			boolean remakeCompleteList) {
		removeContactByMatchingMsisdn(friendsList, contactInfo);

		removeContactByMatchingMsisdn(hikeContactsList, contactInfo);

		removeContactByMatchingMsisdn(smsContactsList, contactInfo);
		if (remakeCompleteList) {
			makeCompleteList(false);
		}
	}

	public void addToGroup(ContactInfo contactInfo, int groupIndex) {
		removeContact(contactInfo, false);

		if (getCount() == 0) {
			return;
		}

		switch (groupIndex) {
		case FRIEND_INDEX:
			friendsList.add(contactInfo);
			Collections.sort(friendsList, ContactInfo.lastSeenTimeComparator);
			break;
		case HIKE_INDEX:
			hikeContactsList.add(contactInfo);
			Collections.sort(hikeContactsList);
		case SMS_INDEX:
			smsContactsList.add(contactInfo);
			Collections.sort(smsContactsList);
		}

		makeCompleteList(false);
	}

	public void refreshGroupList(List<ContactInfo> newGroupList, int groupIndex) {
		List<ContactInfo> groupList = null;
		switch (groupIndex) {
		case FRIEND_INDEX:
			groupList = friendsList;
			break;
		case HIKE_INDEX:
			groupList = hikeContactsList;
			break;
		case SMS_INDEX:
			groupList = smsContactsList;
			break;
		}
		groupList.clear();

		groupList.addAll(newGroupList);

		makeCompleteList(false);
	}

	public void removeFromGroup(ContactInfo contactInfo, int groupIndex) {
		switch (groupIndex) {
		case FRIEND_INDEX:
			removeContactByMatchingMsisdn(friendsList, contactInfo);
			break;
		case HIKE_INDEX:
			removeContactByMatchingMsisdn(hikeContactsList, contactInfo);
			break;
		case SMS_INDEX:
			removeContactByMatchingMsisdn(smsContactsList, contactInfo);
			break;
		}
		makeCompleteList(false);
	}

	@Override
	public int getCount() {
		return completeList.size();
	}

	@Override
	public ContactInfo getItem(int position) {
		return completeList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getViewTypeCount() {
		return ViewType.values().length;
	}

	@Override
	public int getItemViewType(int position) {
		ContactInfo contactInfo = getItem(position);
		if (SECTION_ID.equals(contactInfo.getId())) {
			return ViewType.SECTION.ordinal();
		} else if (EXTRA_ID.equals(contactInfo.getId())) {
			return ViewType.EXTRA.ordinal();
		} else {
			FavoriteType favoriteType = contactInfo.getFavoriteType();
			if (favoriteType == FavoriteType.FRIEND
					|| favoriteType == FavoriteType.REQUEST_SENT
					|| favoriteType == FavoriteType.REQUEST_SENT_REJECTED) {
				return ViewType.FRIEND.ordinal();
			} else if (favoriteType == FavoriteType.REQUEST_RECEIVED) {
				/*
				 * Accounting for the friends header
				 */
				if (position <= ((filteredFriendsList.size() - 1) + 1)) {
					return ViewType.FRIEND_REQUEST.ordinal();
				}
			}
			return ViewType.NOT_FRIEND.ordinal();
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		ViewType viewType = ViewType.values()[getItemViewType(position)];

		ContactInfo contactInfo = getItem(position);

		if (convertView == null) {
			switch (viewType) {
			case FRIEND:
				convertView = layoutInflater.inflate(
						R.layout.friends_child_view, null);
				break;

			case NOT_FRIEND:
				convertView = layoutInflater.inflate(
						R.layout.contact_child_view, null);
				break;

			case SECTION:
				convertView = layoutInflater.inflate(
						R.layout.friends_group_view, null);
				break;
			case FRIEND_REQUEST:
				convertView = layoutInflater.inflate(
						R.layout.friend_request_view, null);
				break;
			case EXTRA:
				convertView = layoutInflater.inflate(
						R.layout.friends_tab_extra_item, null);
			}
		}

		if (viewType == ViewType.FRIEND || viewType == ViewType.NOT_FRIEND
				|| viewType == ViewType.FRIEND_REQUEST) {
			ImageView avatar = (ImageView) convertView
					.findViewById(R.id.avatar);
			TextView name = (TextView) convertView.findViewById(R.id.contact);

			avatar.setImageDrawable(IconCacheManager.getInstance()
					.getIconForMSISDN(contactInfo.getMsisdn(), true));
			name.setText(TextUtils.isEmpty(contactInfo.getName()) ? contactInfo
					.getMsisdn() : contactInfo.getName());

			if (viewType == ViewType.FRIEND
					|| viewType == ViewType.FRIEND_REQUEST) {
				TextView lastSeen = (TextView) convertView
						.findViewById(R.id.last_seen);
				ImageView avatarFrame = (ImageView) convertView
						.findViewById(R.id.avatar_frame);

				lastSeen.setTextColor(context.getResources().getColor(
						R.color.conversation_timestamp));
				lastSeen.setVisibility(View.GONE);

				avatarFrame
						.setImageResource(R.drawable.frame_avatar_medium_selector);

				if (contactInfo.getFavoriteType() == FavoriteType.FRIEND
						&& lastSeenPref) {
					String lastSeenString = Utils.getLastSeenTimeAsString(
							context, contactInfo.getLastSeenTime(),
							contactInfo.getOffline());
					if (!TextUtils.isEmpty(lastSeenString)) {
						if (contactInfo.getOffline() == 0) {
							lastSeen.setTextColor(context.getResources()
									.getColor(R.color.unread_message));
							avatarFrame
									.setImageResource(R.drawable.frame_avatar_medium_highlight_selector);
						}
						lastSeen.setVisibility(View.VISIBLE);
						lastSeen.setText(lastSeenString);
					}
				} else {
					if (contactInfo.getFavoriteType() == FavoriteType.REQUEST_SENT) {
						lastSeen.setVisibility(View.VISIBLE);
						lastSeen.setText(R.string.request_pending);
					} else if (viewType == ViewType.FRIEND_REQUEST) {
						lastSeen.setVisibility(View.VISIBLE);
						lastSeen.setText(R.string.sent_friend_request);

						ImageButton acceptBtn = (ImageButton) convertView
								.findViewById(R.id.accept);
						ImageButton rejectBtn = (ImageButton) convertView
								.findViewById(R.id.reject);

						acceptBtn.setTag(contactInfo);
						rejectBtn.setTag(contactInfo);

						acceptBtn.setOnClickListener(acceptOnClickListener);
						rejectBtn.setOnClickListener(rejectOnClickListener);
					}
				}
			} else {
				ImageView addFriend = (ImageView) convertView
						.findViewById(R.id.add_friend);
				addFriend.setTag(contactInfo);
				addFriend.setOnClickListener(this);
			}

		} else if (viewType == ViewType.SECTION) {
			TextView headerName = (TextView) convertView
					.findViewById(R.id.name);
			TextView headerCount = (TextView) convertView
					.findViewById(R.id.count);

			headerName.setText(contactInfo.getName());
			headerCount.setText(contactInfo.getMsisdn());
		} else if (viewType == ViewType.EXTRA) {
			TextView headerName = (TextView) convertView
					.findViewById(R.id.contact);
			ImageView headerIcon = (ImageView) convertView
					.findViewById(R.id.icon);

			if (contactInfo.getMsisdn().equals(INVITE_MSISDN)) {
				headerIcon.setImageResource(R.drawable.ic_invite_to_hike);
				headerName.setText(R.string.invite_friends_hike);
			} else {
				headerIcon.setImageResource(R.drawable.ic_create_group);
				headerName.setText(R.string.create_group);
			}
		}

		return convertView;
	}

	@Override
	public void onClick(View v) {

		ContactInfo contactInfo = null;
		Object tag = v.getTag();

		if (tag instanceof ContactInfo) {
			contactInfo = (ContactInfo) tag;
		}

		FavoriteType favoriteType;
		if (contactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED) {
			favoriteType = FavoriteType.FRIEND;
		} else {
			favoriteType = FavoriteType.REQUEST_SENT;
		}

		Pair<ContactInfo, FavoriteType> favoriteAdded = new Pair<ContactInfo, FavoriteType>(
				contactInfo, favoriteType);
		HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_TOGGLED,
				favoriteAdded);
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		ContactInfo contactInfo = getItem(position);
		if (SECTION_ID.equals(contactInfo.getId())) {
			return false;
		} else {
			return true;
		}
	}

	private OnClickListener acceptOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			ContactInfo contactInfo = (ContactInfo) v.getTag();
			respondToFriendRequest(contactInfo, true);
		}
	};

	private OnClickListener rejectOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			ContactInfo contactInfo = (ContactInfo) v.getTag();
			respondToFriendRequest(contactInfo, false);
		}
	};

	private void respondToFriendRequest(ContactInfo contactInfo, boolean accept) {
		FavoriteType favoriteType = accept ? FavoriteType.FRIEND
				: FavoriteType.REQUEST_RECEIVED_REJECTED;
		Pair<ContactInfo, FavoriteType> favoriteAdded = new Pair<ContactInfo, FavoriteType>(
				contactInfo, favoriteType);
		HikeMessengerApp
				.getPubSub()
				.publish(
						favoriteType == FavoriteType.FRIEND ? HikePubSub.FAVORITE_TOGGLED
								: HikePubSub.REJECT_FRIEND_REQUEST,
						favoriteAdded);

		removeFromGroup(contactInfo, FRIEND_INDEX);
	}

	@Override
	public boolean isItemViewTypePinned(int viewType) {
		return viewType == ViewType.SECTION.ordinal();
	}

	public List<ContactInfo> getCompleteList() {
		return completeList;
	}

	public void setCompleteList(List<ContactInfo> completeList) {
		this.completeList = completeList;
	}

	public List<ContactInfo> getFriendsList() {
		return friendsList;
	}

	public void setFriendsList(List<ContactInfo> friendsList) {
		this.friendsList = friendsList;
	}

	public List<ContactInfo> getFilteredFriendsList() {
		return filteredFriendsList;
	}

	public void setFilteredFriendsList(List<ContactInfo> filteredFriendsList) {
		this.filteredFriendsList = filteredFriendsList;
	}

}
