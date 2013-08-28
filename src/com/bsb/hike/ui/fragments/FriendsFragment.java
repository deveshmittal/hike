package com.bsb.hike.ui.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.adapters.FriendsAdapter;
import com.bsb.hike.adapters.FriendsAdapter.ViewType;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.ui.ChatThread;

public class FriendsFragment extends SherlockListFragment implements Listener,
		OnItemLongClickListener {

	private FriendsAdapter friendsAdapter;

	private String[] pubSubListeners = { HikePubSub.ICON_CHANGED,
			HikePubSub.FAVORITE_TOGGLED, HikePubSub.USER_JOINED,
			HikePubSub.USER_LEFT, HikePubSub.CONTACT_ADDED,
			HikePubSub.REFRESH_FAVORITES, HikePubSub.FRIEND_REQUEST_ACCEPTED,
			HikePubSub.REJECT_FRIEND_REQUEST, HikePubSub.BLOCK_USER,
			HikePubSub.UNBLOCK_USER, HikePubSub.LAST_SEEN_TIME_UPDATED };

	private SharedPreferences preferences;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View parent = inflater.inflate(R.layout.friends, null);

		ListView friendsList = (ListView) parent
				.findViewById(android.R.id.list);
		friendsList.setEmptyView(parent.findViewById(android.R.id.empty));

		friendsAdapter = new FriendsAdapter(getActivity());
		friendsList.setAdapter(friendsAdapter);

		friendsList.setOnItemLongClickListener(this);
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		final SearchView searchView = new SearchView(getSherlockActivity()
				.getSupportActionBar().getThemedContext());
		searchView.setQueryHint(getString(R.string.search_hint));
		searchView.setIconifiedByDefault(false);
		searchView.setIconified(false);
		searchView.setOnQueryTextListener(onQueryTextListener);

		menu.add(Menu.NONE, Menu.NONE, 1, R.string.search_hint)
				.setIcon(R.drawable.ic_top_bar_search)
				.setActionView(searchView)
				.setShowAsAction(
						MenuItem.SHOW_AS_ACTION_ALWAYS
								| MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
	}

	private OnQueryTextListener onQueryTextListener = new OnQueryTextListener() {

		@Override
		public boolean onQueryTextSubmit(String query) {
			return false;
		}

		@Override
		public boolean onQueryTextChange(String newText) {
			friendsAdapter.onQueryChanged(newText);
			return true;
		}
	};

	@Override
	public void onDestroy() {
		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
		super.onDestroy();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		ContactInfo contactInfo = friendsAdapter.getItem(position);

		if (FriendsAdapter.SECTION_ID.equals(contactInfo.getId())) {
			return;
		}

		Intent intent = new Intent(getActivity(), ChatThread.class);
		if (contactInfo.getName() != null) {
			intent.putExtra(HikeConstants.Extras.NAME, contactInfo.getName());
		}
		intent.putExtra(HikeConstants.Extras.MSISDN, contactInfo.getMsisdn());
		startActivity(intent);

		return;
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
		} else if (HikePubSub.USER_JOINED.equals(type)
				|| HikePubSub.USER_LEFT.equals(type)) {
			final ContactInfo contactInfo = HikeUserDatabase.getInstance()
					.getContactInfoFromMSISDN((String) object, true);

			if (contactInfo == null) {
				return;
			}
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (HikePubSub.USER_JOINED.equals(type)) {
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
						friendsAdapter.removeContact(contactInfo, true);
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

			if (contactInfo.getFavoriteType() != FavoriteType.FRIEND) {
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

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view,
			int position, long id) {
		FriendsAdapter.ViewType viewType = FriendsAdapter.ViewType.values()[friendsAdapter
				.getItemViewType(position)];
		if (viewType != ViewType.FRIEND) {
			return false;
		}
		final ContactInfo contactInfo = friendsAdapter.getItem(position);

		ArrayList<String> optionsList = new ArrayList<String>();

		optionsList.add(getString(R.string.remove_from_friends));

		final String[] options = new String[optionsList.size()];
		optionsList.toArray(options);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		ListAdapter dialogAdapter = new ArrayAdapter<CharSequence>(
				getActivity(), R.layout.alert_item, R.id.item, options);

		builder.setAdapter(dialogAdapter,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String option = options[which];
						if (getString(R.string.remove_from_friends).equals(
								option)) {
							FavoriteType favoriteType;
							if (contactInfo.getFavoriteType() == FavoriteType.FRIEND) {
								favoriteType = FavoriteType.REQUEST_RECEIVED_REJECTED;
							} else {
								favoriteType = FavoriteType.NOT_FRIEND;
							}
							Pair<ContactInfo, FavoriteType> favoriteRemoved = new Pair<ContactInfo, FavoriteType>(
									contactInfo, favoriteType);
							HikeMessengerApp.getPubSub().publish(
									HikePubSub.FAVORITE_TOGGLED,
									favoriteRemoved);
						}
					}
				});

		AlertDialog alertDialog = builder.show();
		alertDialog.getListView().setDivider(
				getResources()
						.getDrawable(R.drawable.ic_thread_divider_profile));
		return true;
	}
}
