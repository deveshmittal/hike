package com.bsb.hike.ui.fragments;

import java.util.Collections;
import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;

import com.actionbarsherlock.app.SherlockFragment;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.adapters.FriendsAdapter;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.ui.ChatThread;

public class FriendsFragment extends SherlockFragment implements
		OnChildClickListener, Listener {

	private FriendsAdapter friendsAdapter;

	private String[] pubSubListeners = { HikePubSub.ICON_CHANGED,
			HikePubSub.RECENT_CONTACTS_UPDATED, HikePubSub.FAVORITE_TOGGLED,
			HikePubSub.USER_JOINED, HikePubSub.USER_LEFT,
			HikePubSub.CONTACT_ADDED, HikePubSub.REFRESH_FAVORITES,
			HikePubSub.REFRESH_RECENTS, HikePubSub.FRIEND_REQUEST_ACCEPTED,
			HikePubSub.REJECT_FRIEND_REQUEST, HikePubSub.BLOCK_USER,
			HikePubSub.UNBLOCK_USER, HikePubSub.LAST_SEEN_TIME_UPDATED };

	private SharedPreferences preferences;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View parent = inflater.inflate(R.layout.friends, null);

		ExpandableListView friendsList = (ExpandableListView) parent
				.findViewById(R.id.friends_list);
		friendsList.setEmptyView(parent.findViewById(android.R.id.empty));

		friendsAdapter = new FriendsAdapter(getActivity(), friendsList);
		friendsList.setAdapter(friendsAdapter);
		friendsList.setOnChildClickListener(this);

		for (int i = 0; i < friendsAdapter.getGroupCount(); i++) {
			friendsList.expandGroup(i);
		}

		return parent;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		preferences = getActivity().getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
	}

	@Override
	public void onDestroy() {
		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
		super.onDestroy();
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		ContactInfo contactInfo = friendsAdapter.getChild(groupPosition,
				childPosition);

		Intent intent = new Intent(getActivity(), ChatThread.class);
		if (contactInfo.getName() != null) {
			intent.putExtra(HikeConstants.Extras.NAME, contactInfo.getName());
		}
		intent.putExtra(HikeConstants.Extras.MSISDN, contactInfo.getMsisdn());
		startActivity(intent);

		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onEventReceived(final String type, final Object object) {
		if (!isAdded()) {
			return;
		}
		if (HikePubSub.ICON_CHANGED.equals(type)) {
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					friendsAdapter.notifyDataSetChanged();
				}
			});
		} else if (HikePubSub.RECENT_CONTACTS_UPDATED.equals(type)
				|| HikePubSub.USER_JOINED.equals(type)
				|| HikePubSub.USER_LEFT.equals(type)) {
			final ContactInfo contactInfo = HikeUserDatabase.getInstance()
					.getContactInfoFromMSISDN((String) object, true);

			if (contactInfo == null) {
				return;
			}
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (HikePubSub.RECENT_CONTACTS_UPDATED.equals(type)) {
						friendsAdapter.addToGroup(contactInfo,
								FriendsAdapter.RECENT_INDEX);
					} else if (HikePubSub.USER_JOINED.equals(type)) {
						friendsAdapter.addToGroup(contactInfo,
								FriendsAdapter.HIKE_INDEX);
					} else if (HikePubSub.USER_LEFT.equals(type)) {
						friendsAdapter.removeFromGroup(contactInfo,
								FriendsAdapter.HIKE_INDEX);
					}
				}
			});
		} else if (HikePubSub.FAVORITE_TOGGLED.equals(type)
				|| HikePubSub.FRIEND_REQUEST_ACCEPTED.equals(type)
				|| HikePubSub.REJECT_FRIEND_REQUEST.equals(type)) {
			final Pair<ContactInfo, FavoriteType> favoriteToggle = (Pair<ContactInfo, FavoriteType>) object;
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					FavoriteType favoriteType = favoriteToggle.second;
					ContactInfo contactInfo = favoriteToggle.first;
					contactInfo.setFavoriteType(favoriteType);
					if ((favoriteType == FavoriteType.FRIEND)
							|| (favoriteType == FavoriteType.REQUEST_SENT)) {
						friendsAdapter.addToGroup(contactInfo,
								FriendsAdapter.FRIEND_INDEX);
					} else if (favoriteType == FavoriteType.NOT_FRIEND
							|| favoriteType == FavoriteType.REQUEST_RECEIVED_REJECTED) {
						if (contactInfo.isOnhike()) {
							friendsAdapter.addToGroup(contactInfo,
									FriendsAdapter.HIKE_INDEX);
						} else {
							friendsAdapter.removeFromGroup(contactInfo,
									FriendsAdapter.FRIEND_INDEX);
						}
					}
				}
			});
		} else if (HikePubSub.CONTACT_ADDED.equals(type)) {
			final ContactInfo contactInfo = (ContactInfo) object;

			if (contactInfo == null) {
				return;
			}

			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if ((contactInfo.getFavoriteType() != FavoriteType.FRIEND)
							&& (contactInfo.getFavoriteType() != FavoriteType.REQUEST_SENT)
							&& (contactInfo.getFavoriteType() != FavoriteType.REQUEST_SENT_REJECTED)) {
						if (contactInfo.isOnhike()) {
							friendsAdapter.addToGroup(contactInfo,
									FriendsAdapter.HIKE_INDEX);
						}
					} else {
						friendsAdapter.addToGroup(contactInfo,
								FriendsAdapter.FRIEND_INDEX);
					}
				}
			});
		} else if (HikePubSub.REFRESH_FAVORITES.equals(type)) {
			String myMsisdn = preferences.getString(
					HikeMessengerApp.MSISDN_SETTING, "");

			HikeUserDatabase hikeUserDatabase = HikeUserDatabase.getInstance();

			final List<ContactInfo> favoriteList = hikeUserDatabase
					.getContactsOfFavoriteType(FavoriteType.FRIEND,
							HikeConstants.BOTH_VALUE, myMsisdn);
			favoriteList.addAll(hikeUserDatabase.getContactsOfFavoriteType(
					FavoriteType.REQUEST_SENT, HikeConstants.BOTH_VALUE,
					myMsisdn));
			favoriteList.addAll(hikeUserDatabase.getContactsOfFavoriteType(
					FavoriteType.REQUEST_SENT_REJECTED,
					HikeConstants.BOTH_VALUE, myMsisdn));
			Collections.sort(favoriteList, ContactInfo.lastSeenTimeComparator);
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					friendsAdapter.refreshGroupList(favoriteList,
							FriendsAdapter.FRIEND_INDEX);
				}
			});
		} else if (HikePubSub.REFRESH_RECENTS.equals(type)) {
			String myMsisdn = preferences.getString(
					HikeMessengerApp.MSISDN_SETTING, "");

			boolean freeSMSOn = PreferenceManager.getDefaultSharedPreferences(
					getActivity()).getBoolean(HikeConstants.FREE_SMS_PREF,
					false);

			final List<ContactInfo> recentList = HikeUserDatabase.getInstance()
					.getRecentContacts(HikeConstants.RECENT_COUNT_IN_FAVORITE,
							false, FavoriteType.NOT_FRIEND, freeSMSOn ? 1 : 0,
							myMsisdn);
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					friendsAdapter.refreshGroupList(recentList,
							FriendsAdapter.RECENT_INDEX);
				}
			});
		} else if (HikePubSub.BLOCK_USER.equals(type)
				|| HikePubSub.UNBLOCK_USER.equals(type)) {
			String msisdn = (String) object;
			final ContactInfo contactInfo = HikeUserDatabase.getInstance()
					.getContactInfoFromMSISDN(msisdn, true);
			final boolean blocked = HikePubSub.BLOCK_USER.equals(type);
			if (contactInfo == null) {
				return;
			}
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (blocked) {
						friendsAdapter.removeContact(contactInfo);
					} else {
						if (contactInfo.isOnhike()) {
							friendsAdapter.addToGroup(contactInfo,
									FriendsAdapter.HIKE_INDEX);
						}
					}
				}
			});
		} else if (HikePubSub.LAST_SEEN_TIME_UPDATED.equals(type)) {
			final ContactInfo contactInfo = (ContactInfo) object;

			if (contactInfo.getFavoriteType() != FavoriteType.FRIEND
					&& contactInfo.getFavoriteType() != FavoriteType.REQUEST_RECEIVED
					&& contactInfo.getFavoriteType() != FavoriteType.REQUEST_RECEIVED_REJECTED) {
				return;
			}

			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					friendsAdapter.addToGroup(contactInfo,
							FriendsAdapter.FRIEND_INDEX);
				}

			});
		}
	}
}
