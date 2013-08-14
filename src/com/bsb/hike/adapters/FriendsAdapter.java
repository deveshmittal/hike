package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
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

public class FriendsAdapter extends BaseExpandableListAdapter implements
		OnClickListener {

	public static final int FRIEND_INDEX = 0;
	public static final int RECENT_INDEX = 1;
	public static final int HIKE_INDEX = 2;

	private final int[] headerRes = { R.string.friends, R.string.recents,
			R.string.contacts };

	private enum ChildType {
		FRIEND, NOT_FRIEND
	}

	private LayoutInflater layoutInflater;
	private List<List<ContactInfo>> contactList;
	private boolean freeSMSOn;
	private Context context;
	private ExpandableListView parentView;

	public FriendsAdapter(final Context context, ExpandableListView parent) {
		this.layoutInflater = LayoutInflater.from(context);
		this.context = context;
		this.parentView = parent;

		freeSMSOn = PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(HikeConstants.FREE_SMS_PREF, false);

		contactList = new ArrayList<List<ContactInfo>>(0);

		new AsyncTask<Void, Void, List<List<ContactInfo>>>() {

			List<ContactInfo> favoriteTaskList;
			List<ContactInfo> onHikeTaskList;
			List<ContactInfo> recentTaskList;

			@Override
			protected List<List<ContactInfo>> doInBackground(Void... params) {
				Log.d(getClass().getSimpleName(),
						"Favorite List started fetching");
				List<List<ContactInfo>> friendsList = new ArrayList<List<ContactInfo>>();

				String myMsisdn = context.getSharedPreferences(
						HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(
						HikeMessengerApp.MSISDN_SETTING, "");

				HikeUserDatabase hikeUserDatabase = HikeUserDatabase
						.getInstance();

				favoriteTaskList = hikeUserDatabase
						.getContactsOfFavoriteType(FavoriteType.FRIEND,
								HikeConstants.BOTH_VALUE, myMsisdn);
				favoriteTaskList.addAll(hikeUserDatabase
						.getContactsOfFavoriteType(FavoriteType.REQUEST_SENT,
								HikeConstants.BOTH_VALUE, myMsisdn));
				favoriteTaskList.addAll(hikeUserDatabase
						.getContactsOfFavoriteType(
								FavoriteType.REQUEST_SENT_REJECTED,
								HikeConstants.BOTH_VALUE, myMsisdn));
				Collections.sort(favoriteTaskList,
						ContactInfo.lastSeenTimeComparator);

				onHikeTaskList = hikeUserDatabase.getContactsOfFavoriteType(
						FavoriteType.NOT_FRIEND, HikeConstants.ON_HIKE_VALUE,
						myMsisdn);
				onHikeTaskList.addAll(hikeUserDatabase
						.getContactsOfFavoriteType(
								FavoriteType.REQUEST_RECEIVED,
								HikeConstants.ON_HIKE_VALUE, myMsisdn));
				onHikeTaskList.addAll(hikeUserDatabase
						.getContactsOfFavoriteType(
								FavoriteType.REQUEST_RECEIVED_REJECTED,
								HikeConstants.ON_HIKE_VALUE, myMsisdn));
				Collections.sort(onHikeTaskList);

				recentTaskList = hikeUserDatabase.getRecentContacts(
						HikeConstants.RECENT_COUNT_IN_FAVORITE, false,
						FavoriteType.NOT_FRIEND, freeSMSOn ? 1 : 0, myMsisdn);

				friendsList.add(favoriteTaskList);
				friendsList.add(recentTaskList);
				friendsList.add(onHikeTaskList);

				return friendsList;
			}

			@Override
			protected void onPostExecute(List<List<ContactInfo>> result) {
				Log.d(getClass().getSimpleName(), "Favorite List fetched");
				contactList = result;
				notifyDataSetChanged();

				for (int i = 0; i < getGroupCount(); i++) {
					parentView.expandGroup(i);
				}
			}

		}.execute();
	}

	public void removeContact(ContactInfo contactInfo) {
		for (int i = 0; i < contactList.size(); i++) {

			List<ContactInfo> groupList = contactList.get(i);

			boolean elementRemoved = false;

			for (int j = 0; j < groupList.size(); j++) {
				ContactInfo groupListContact = groupList.get(j);
				if (groupListContact.getMsisdn()
						.equals(contactInfo.getMsisdn())) {
					groupList.remove(j);
					elementRemoved = true;
					break;
				}
			}

			/*
			 * If a contact is present in the friends list, we can be sure its
			 * not present in the other lists.
			 */
			if (elementRemoved && i == FRIEND_INDEX) {
				break;
			}
		}
	}

	public void addToGroup(ContactInfo contactInfo, int groupIndex) {
		removeContact(contactInfo);

		if (getGroupCount() == 0) {
			return;
		}

		List<ContactInfo> groupList = contactList.get(groupIndex);
		if (groupIndex != RECENT_INDEX) {
			groupList.add(contactInfo);
			if (groupIndex != 0) {
				Collections.sort(groupList);
			} else {
				Collections.sort(groupList, ContactInfo.lastSeenTimeComparator);
			}
		} else {
			groupList.add(0, contactInfo);
			if (groupList.size() > HikeConstants.RECENT_COUNT_IN_FAVORITE) {
				groupList.remove(groupList.size() - 1);
			}
		}

		notifyDataSetChanged();
	}

	public void refreshGroupList(List<ContactInfo> newGroupList, int groupIndex) {
		List<ContactInfo> groupList = contactList.get(groupIndex);
		groupList.clear();

		groupList.addAll(newGroupList);
		notifyDataSetChanged();
	}

	public void removeFromGroup(ContactInfo contactInfo, int groupIndex) {
		List<ContactInfo> groupList = contactList.get(groupIndex);
		groupList.remove(contactInfo);

		notifyDataSetChanged();
	}

	@Override
	public ContactInfo getChild(int groupPosition, int childPosition) {
		List<ContactInfo> groupList = contactList.get(groupPosition);
		return groupList.get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition + (groupPosition * 10000);
	}

	@Override
	public int getChildType(int groupPosition, int childPosition) {
		if (groupPosition == FRIEND_INDEX) {
			return ChildType.FRIEND.ordinal();
		}
		return ChildType.NOT_FRIEND.ordinal();
	}

	@Override
	public int getChildTypeCount() {
		return ChildType.values().length;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {

		ChildType childType = ChildType.values()[getChildType(groupPosition,
				childPosition)];

		ContactInfo contactInfo = getChild(groupPosition, childPosition);

		if (convertView == null) {
			switch (childType) {
			case FRIEND:
				convertView = layoutInflater.inflate(
						R.layout.friends_child_view, null);
				break;

			case NOT_FRIEND:
				convertView = layoutInflater.inflate(
						R.layout.contact_child_view, null);
				break;
			}
		}

		ImageView avatar = (ImageView) convertView.findViewById(R.id.avatar);
		TextView name = (TextView) convertView.findViewById(R.id.contact);

		avatar.setImageDrawable(IconCacheManager.getInstance()
				.getIconForMSISDN(contactInfo.getMsisdn()));
		name.setText(TextUtils.isEmpty(contactInfo.getName()) ? contactInfo
				.getMsisdn() : contactInfo.getName());

		if (childType == ChildType.FRIEND) {
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
					|| contactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED
					|| contactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED_REJECTED) {
				String lastSeenString = Utils
						.getLastSeenTimeAsString(context,
								contactInfo.getLastSeenTime(),
								contactInfo.getOffline());
				if (!TextUtils.isEmpty(lastSeenString)) {
					if (contactInfo.getOffline() == 0) {
						lastSeen.setTextColor(context.getResources().getColor(
								R.color.unread_message));
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
				}
			}
		} else {
			ImageView addFriend = (ImageView) convertView
					.findViewById(R.id.add_friend);
			addFriend.setTag(contactInfo);
			addFriend.setOnClickListener(this);
		}

		convertView.findViewById(R.id.divider).setVisibility(
				isLastChild ? View.GONE : View.VISIBLE);

		return convertView;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return contactList.get(groupPosition).size();
	}

	@Override
	public List<ContactInfo> getGroup(int groupPosition) {
		return contactList.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return contactList.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = layoutInflater.inflate(R.layout.friends_group_view,
					null);
		}

		TextView name = (TextView) convertView.findViewById(R.id.name);
		ImageView groupIndicator = (ImageView) convertView
				.findViewById(R.id.group_indicator);
		if (contactList.get(groupPosition).isEmpty()) {
			groupIndicator.setVisibility(View.GONE);
		} else {
			groupIndicator.setVisibility(View.VISIBLE);
			groupIndicator
					.setImageResource(isExpanded ? R.drawable.ic_list_open
							: R.drawable.ic_list_close);
		}

		name.setText(headerRes[groupPosition]);

		return convertView;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
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

}
