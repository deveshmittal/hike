package com.bsb.hike.ui.fragments;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.adapters.CentralTimelineAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.Protip;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.StatusUpdate;
import com.bsb.hike.utils.Utils;

public class UpdatesFragment extends SherlockListFragment implements
		OnScrollListener, Listener {

	private StatusMessage noStatusMessage;
	private StatusMessage noFriendMessage;
	private CentralTimelineAdapter centralTimelineAdapter;
	private String userMsisdn;
	private SharedPreferences prefs;
	private List<StatusMessage> statusMessages;
	private int friendRequests;
	private boolean reachedEnd;
	private boolean loadingMoreMessages;
	private String[] friendMsisdns;

	private String[] pubSubListeners = { HikePubSub.FAVORITE_TOGGLED,
			HikePubSub.TIMELINE_UPDATE_RECIEVED };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.updates, null);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (prefs.getInt(HikeMessengerApp.UNSEEN_STATUS_COUNT, 0) > 0
				|| prefs.getInt(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, 0) > 0) {
			resetUnseenStatusCount();
			HikeMessengerApp.getPubSub().publish(
					HikePubSub.RESET_NOTIFICATION_COUNTER, null);
		}
		HikeMessengerApp.getPubSub().publish(
				HikePubSub.CANCEL_ALL_STATUS_NOTIFICATIONS, null);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.updates_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.new_update:
			Intent intent = new Intent(getActivity(), StatusUpdate.class);
			intent.putExtra(HikeConstants.Extras.FROM_CONVERSATIONS_SCREEN,
					true);
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void resetUnseenStatusCount() {
		Editor editor = prefs.edit();
		editor.putInt(HikeMessengerApp.UNSEEN_STATUS_COUNT, 0);
		editor.putInt(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, 0);
		editor.commit();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		prefs = getActivity().getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		int unseenCount = Utils.getNotificationCount(prefs, true);

		userMsisdn = prefs.getString(HikeMessengerApp.MSISDN_SETTING, "");

		List<ContactInfo> friendRequestList = HikeUserDatabase.getInstance()
				.getContactsOfFavoriteType(FavoriteType.REQUEST_RECEIVED,
						HikeConstants.BOTH_VALUE, userMsisdn);

		List<ContactInfo> friendsList = HikeUserDatabase.getInstance()
				.getContactsOfFavoriteType(FavoriteType.FRIEND,
						HikeConstants.BOTH_VALUE, userMsisdn);

		int friendMsisdnLength = friendsList.size();
		friendRequests = friendRequestList.size();

		ArrayList<String> msisdnList = new ArrayList<String>();

		for (ContactInfo contactInfo : friendsList) {
			if (TextUtils.isEmpty(contactInfo.getMsisdn())) {
				continue;
			}
			msisdnList.add(contactInfo.getMsisdn());
		}
		msisdnList.add(userMsisdn);

		friendMsisdns = new String[msisdnList.size()];
		msisdnList.toArray(friendMsisdns);
		statusMessages = HikeConversationsDatabase.getInstance()
				.getStatusMessages(true,
						HikeConstants.MAX_STATUSES_TO_LOAD_INITIALLY, -1,
						friendMsisdns);

		for (ContactInfo contactInfo : friendRequestList) {
			statusMessages
					.add(0,
							new StatusMessage(
									CentralTimelineAdapter.FRIEND_REQUEST_ID,
									null,
									contactInfo.getMsisdn(),
									TextUtils.isEmpty(contactInfo.getName()) ? contactInfo
											.getMsisdn() : contactInfo
											.getName(),
									getString(R.string.added_as_hike_friend),
									StatusMessageType.FRIEND_REQUEST, System
											.currentTimeMillis() / 1000));
		}

		long currentProtipId = prefs.getLong(HikeMessengerApp.CURRENT_PROTIP,
				-1);

		Protip protip = null;
		boolean showProtip = false;
		if (currentProtipId == -1) {
			protip = HikeConversationsDatabase.getInstance().getLastProtip();
			if (protip != null) {
				if (Utils.showProtip(protip, prefs)) {
					showProtip = true;
					Editor editor = prefs.edit();
					editor.putLong(HikeMessengerApp.CURRENT_PROTIP,
							protip.getId());
					editor.putLong(HikeMessengerApp.PROTIP_WAIT_TIME,
							protip.getWaitTime());
					editor.commit();
				}
			}
		} else {
			showProtip = true;
			protip = HikeConversationsDatabase.getInstance().getProtipForId(
					currentProtipId);
		}

		if (showProtip && protip != null) {
			statusMessages.add(0, new StatusMessage(protip));
		}

		String name = Utils.getFirstName(prefs.getString(
				HikeMessengerApp.NAME_SETTING, null));
		String lastStatus = prefs.getString(HikeMessengerApp.LAST_STATUS, "");

		/*
		 * If we already have a few status messages in the timeline, no need to
		 * prompt the user to post his/her own message.
		 */
		if (statusMessages.size() < HikeConstants.MIN_STATUS_COUNT) {
			if (TextUtils.isEmpty(lastStatus)) {
				noStatusMessage = new StatusMessage(
						CentralTimelineAdapter.EMPTY_STATUS_NO_STATUS_ID, null,
						"12345", getString(R.string.team_hike), getString(
								R.string.hey_name, name),
						StatusMessageType.NO_STATUS,
						System.currentTimeMillis() / 1000);
				statusMessages.add(0, noStatusMessage);
			} else if (statusMessages.isEmpty()) {
				noStatusMessage = new StatusMessage(
						CentralTimelineAdapter.EMPTY_STATUS_NO_STATUS_RECENTLY_ID,
						null, "12345", getString(R.string.team_hike),
						getString(R.string.hey_name, name),
						StatusMessageType.NO_STATUS,
						System.currentTimeMillis() / 1000);
				statusMessages.add(0, noStatusMessage);
			}
		}
		if (friendMsisdnLength == 0
				&& HikeUserDatabase.getInstance().getFriendTableRowCount() == 0) {
			noFriendMessage = new StatusMessage(
					CentralTimelineAdapter.EMPTY_STATUS_NO_FRIEND_ID, null,
					"12345", getString(R.string.team_hike), getString(
							R.string.hey_name, name),
					StatusMessageType.NO_STATUS,
					System.currentTimeMillis() / 1000);
			statusMessages.add(0, noFriendMessage);
		}

		centralTimelineAdapter = new CentralTimelineAdapter(getActivity(),
				this, statusMessages, userMsisdn, unseenCount);
		setListAdapter(centralTimelineAdapter);
		getListView().setOnScrollListener(this);

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
	}

	@Override
	public void onDestroy() {
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		super.onDestroy();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		StatusMessage statusMessage = centralTimelineAdapter.getItem(position);
		if ((statusMessage.getStatusMessageType() == StatusMessageType.NO_STATUS)
				|| (statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST)
				|| (statusMessage.getStatusMessageType() == StatusMessageType.PROTIP)) {
			return;
		} else if (userMsisdn.equals(statusMessage.getMsisdn())) {
			Intent intent = new Intent(getActivity(), ProfileActivity.class);
			intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
			startActivity(intent);
			return;
		}

		Intent intent = Utils.createIntentFromContactInfo(new ContactInfo(null,
				statusMessage.getMsisdn(), statusMessage.getNotNullName(),
				statusMessage.getMsisdn()));
		intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
		intent.setClass(getActivity(), ChatThread.class);
		startActivity(intent);
	}

	@Override
	public void onScroll(AbsListView view, final int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		if (!reachedEnd
				&& !loadingMoreMessages
				&& !statusMessages.isEmpty()
				&& (firstVisibleItem + visibleItemCount) >= (statusMessages
						.size() - HikeConstants.MIN_INDEX_TO_LOAD_MORE_MESSAGES)) {

			Log.d(getClass().getSimpleName(), "Loading more items");
			loadingMoreMessages = true;

			new AsyncTask<Void, Void, List<StatusMessage>>() {

				@Override
				protected List<StatusMessage> doInBackground(Void... params) {
					List<StatusMessage> olderMessages = HikeConversationsDatabase
							.getInstance()
							.getStatusMessages(
									true,
									HikeConstants.MAX_OLDER_STATUSES_TO_LOAD_EACH_TIME,
									(int) statusMessages.get(
											statusMessages.size() - 1).getId(),
									friendMsisdns);
					return olderMessages;
				}

				@Override
				protected void onPostExecute(List<StatusMessage> olderMessages) {
					if (!olderMessages.isEmpty()) {
						statusMessages.addAll(statusMessages.size(),
								olderMessages);
						centralTimelineAdapter.notifyDataSetChanged();
						getListView().setSelection(firstVisibleItem);
					} else {
						/*
						 * This signifies that we've reached the end. No need to
						 * query the db anymore unless we add a new message.
						 */
						reachedEnd = true;
					}

					loadingMoreMessages = false;
				}

			}.execute();

		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}

	public void toggleFavoriteAndRemoveTimelineItem(
			StatusMessage statusMessage, FavoriteType favoriteType) {
		ContactInfo contactInfo = HikeUserDatabase.getInstance()
				.getContactInfoFromMSISDN(statusMessage.getMsisdn(), false);

		Pair<ContactInfo, FavoriteType> favoriteAdded = new Pair<ContactInfo, FavoriteType>(
				contactInfo, favoriteType);
		HikeMessengerApp
				.getPubSub()
				.publish(
						favoriteType == FavoriteType.FRIEND ? HikePubSub.FAVORITE_TOGGLED
								: HikePubSub.REJECT_FRIEND_REQUEST,
						favoriteAdded);

		for (StatusMessage listItem : statusMessages) {
			if (listItem.getMsisdn() == null) {
				continue;
			}
			if (listItem.getMsisdn().equals(statusMessage.getMsisdn())
					&& listItem.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST) {
				friendRequests--;
				statusMessages.remove(listItem);
				break;
			}
		}
		centralTimelineAdapter.decrementUnseenCount();
		centralTimelineAdapter.notifyDataSetChanged();
		HikeMessengerApp.getPubSub().publish(
				HikePubSub.DECREMENT_NOTIFICATION_COUNTER, null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onEventReceived(String type, Object object) {

		if (!isAdded()) {
			return;
		}

		if (HikePubSub.FAVORITE_TOGGLED.equals(type)) {
			final Pair<ContactInfo, FavoriteType> favoriteToggle = (Pair<ContactInfo, FavoriteType>) object;
			final ContactInfo contactInfo = favoriteToggle.first;
			if (favoriteToggle.second != FavoriteType.REQUEST_RECEIVED
					&& favoriteToggle.second != FavoriteType.REQUEST_SENT) {
				return;
			}
			final int startIndex = getStartIndex();
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (favoriteToggle.second == FavoriteType.REQUEST_RECEIVED) {
						statusMessages.add(
								startIndex,
								new StatusMessage(
										CentralTimelineAdapter.FRIEND_REQUEST_ID,
										null,
										contactInfo.getMsisdn(),
										TextUtils.isEmpty(contactInfo.getName()) ? contactInfo
												.getMsisdn() : contactInfo
												.getName(),
										getString(R.string.added_as_hike_friend),
										StatusMessageType.FRIEND_REQUEST,
										System.currentTimeMillis() / 1000));
						friendRequests++;
					}
					if (noFriendMessage != null) {
						statusMessages.remove(noFriendMessage);
						noFriendMessage = null;
					} else if (favoriteToggle.second == FavoriteType.REQUEST_RECEIVED) {
						/*
						 * Since a new item was added, we increment the unseen
						 * count.
						 */
						centralTimelineAdapter.incrementUnseenCount();
					}

					centralTimelineAdapter.notifyDataSetChanged();
				}
			});

		} else if (HikePubSub.TIMELINE_UPDATE_RECIEVED.equals(type)) {
			final StatusMessage statusMessage = (StatusMessage) object;
			final int startIndex = getStartIndex();
			resetUnseenStatusCount();

			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					statusMessages.add(friendRequests + startIndex,
							statusMessage);
					if (noStatusMessage != null
							&& (statusMessages.size() >= HikeConstants.MIN_STATUS_COUNT || statusMessage
									.getMsisdn().equals(userMsisdn))) {
						statusMessages.remove(noStatusMessage);
						noStatusMessage = null;
					}
					centralTimelineAdapter.incrementUnseenCount();
					centralTimelineAdapter.notifyDataSetChanged();
				}
			});
			HikeMessengerApp.getPubSub().publish(
					HikePubSub.RESET_NOTIFICATION_COUNTER, null);
		}
	}

	private int getStartIndex() {
		int startIndex = 0;
		if (noFriendMessage != null) {
			startIndex++;
		}
		if (noStatusMessage != null) {
			startIndex++;
		}
		return startIndex;
	}
}
