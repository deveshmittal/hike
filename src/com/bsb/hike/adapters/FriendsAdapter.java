package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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

	public static final String SECTION_ID = "-911";

	private enum ViewType {
		SECTION, FRIEND, NOT_FRIEND, FRIEND_REQUEST
	}

	private LayoutInflater layoutInflater;
	private List<ContactInfo> completeList;
	private List<ContactInfo> friendsList;
	private List<ContactInfo> otherContactsList;
	private Context context;
	private ContactInfo friendsSection;
	private ContactInfo contactsSection;

	public FriendsAdapter(final Context context) {
		this.layoutInflater = LayoutInflater.from(context);
		this.context = context;

		completeList = new ArrayList<ContactInfo>();

		friendsList = new ArrayList<ContactInfo>(0);
		otherContactsList = new ArrayList<ContactInfo>(0);

		new AsyncTask<Void, Void, Void>() {

			List<ContactInfo> favoriteTaskList;
			List<ContactInfo> otherTaskList;

			@Override
			protected Void doInBackground(Void... params) {
				String myMsisdn = context.getSharedPreferences(
						HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(
						HikeMessengerApp.MSISDN_SETTING, "");

				HikeUserDatabase hikeUserDatabase = HikeUserDatabase
						.getInstance();

				favoriteTaskList = hikeUserDatabase
						.getContactsOfFavoriteType(FavoriteType.FRIEND,
								HikeConstants.BOTH_VALUE, myMsisdn);
				favoriteTaskList.addAll(hikeUserDatabase
						.getContactsOfFavoriteType(
								FavoriteType.REQUEST_RECEIVED,
								HikeConstants.BOTH_VALUE, myMsisdn, true));
				favoriteTaskList.addAll(hikeUserDatabase
						.getContactsOfFavoriteType(FavoriteType.REQUEST_SENT,
								HikeConstants.BOTH_VALUE, myMsisdn));
				favoriteTaskList.addAll(hikeUserDatabase
						.getContactsOfFavoriteType(
								FavoriteType.REQUEST_SENT_REJECTED,
								HikeConstants.BOTH_VALUE, myMsisdn));
				Collections.sort(favoriteTaskList,
						ContactInfo.lastSeenTimeComparator);

				otherTaskList = hikeUserDatabase.getContactsOfFavoriteType(
						FavoriteType.NOT_FRIEND, HikeConstants.BOTH_VALUE,
						myMsisdn);
				otherTaskList.addAll(hikeUserDatabase
						.getContactsOfFavoriteType(
								FavoriteType.REQUEST_RECEIVED,
								HikeConstants.BOTH_VALUE, myMsisdn, true));
				otherTaskList.addAll(hikeUserDatabase
						.getContactsOfFavoriteType(
								FavoriteType.REQUEST_RECEIVED_REJECTED,
								HikeConstants.BOTH_VALUE, myMsisdn, true));
				Collections.sort(otherTaskList);

				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				friendsList = favoriteTaskList;
				otherContactsList = otherTaskList;

				makeCompleteList();
			}

		}.execute();
	}

	public void makeCompleteList() {
		completeList.clear();

		friendsSection = new ContactInfo(SECTION_ID,
				Integer.toString(friendsList.size()),
				context.getString(R.string.friends), null);
		completeList.add(friendsSection);

		completeList.addAll(friendsList);

		contactsSection = new ContactInfo(SECTION_ID,
				Integer.toString(otherContactsList.size()),
				context.getString(R.string.contacts), null);
		completeList.add(contactsSection);

		completeList.addAll(otherContactsList);

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

		removeContactByMatchingMsisdn(otherContactsList, contactInfo);

		if (remakeCompleteList) {
			makeCompleteList();
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
			otherContactsList.add(contactInfo);
			Collections.sort(otherContactsList);
		}

		makeCompleteList();
	}

	public void refreshGroupList(List<ContactInfo> newGroupList, int groupIndex) {
		List<ContactInfo> groupList = null;
		switch (groupIndex) {
		case FRIEND_INDEX:
			groupList = friendsList;
			break;
		case HIKE_INDEX:
			groupList = otherContactsList;
			break;
		}
		groupList.clear();

		groupList.addAll(newGroupList);

		makeCompleteList();
	}

	public void removeFromGroup(ContactInfo contactInfo, int groupIndex) {
		switch (groupIndex) {
		case FRIEND_INDEX:
			removeContactByMatchingMsisdn(friendsList, contactInfo);
			break;
		case HIKE_INDEX:
			removeContactByMatchingMsisdn(otherContactsList, contactInfo);
			break;
		}
		makeCompleteList();
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
				if (position <= ((friendsList.size() - 1) + 1)) {
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

				if (contactInfo.getFavoriteType() == FavoriteType.FRIEND) {
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
						lastSeen.setText(R.string.friend_request_sent);
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

				TextView info = (TextView) convertView.findViewById(R.id.info);
				info.setText(contactInfo.isOnhike() ? R.string.tap_chat
						: R.string.tap_sms);
			}

		} else {
			TextView headerName = (TextView) convertView
					.findViewById(R.id.name);
			TextView headerCount = (TextView) convertView
					.findViewById(R.id.count);

			headerName.setText(contactInfo.getName());
			headerCount.setText(contactInfo.getMsisdn());
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
}
