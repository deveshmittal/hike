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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
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
import com.bsb.hike.utils.Utils;

public class UpdatesFragment extends SherlockListFragment implements
		OnScrollListener, Listener {

	private StatusMessage noStatusMessage;
	private CentralTimelineAdapter centralTimelineAdapter;
	private String userMsisdn;
	private SharedPreferences prefs;
	private List<StatusMessage> statusMessages;
	private boolean reachedEnd;
	private boolean loadingMoreMessages;

	private String[] pubSubListeners = { HikePubSub.TIMELINE_UPDATE_RECIEVED,
			HikePubSub.LARGER_UPDATE_IMAGE_DOWNLOADED };
	private String[] friendMsisdns;

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

		if (centralTimelineAdapter != null) {
			centralTimelineAdapter.restartImageLoaderThread();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (centralTimelineAdapter != null) {
			centralTimelineAdapter.stopImageLoaderThread();
		}
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

		userMsisdn = prefs.getString(HikeMessengerApp.MSISDN_SETTING, "");

		List<ContactInfo> friendsList = HikeUserDatabase.getInstance()
				.getContactsOfFavoriteType(FavoriteType.FRIEND,
						HikeConstants.BOTH_VALUE, userMsisdn);

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

		centralTimelineAdapter = new CentralTimelineAdapter(getActivity(),
				statusMessages, userMsisdn);
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

			AsyncTask<Void, Void, List<StatusMessage>> asyncTask = new AsyncTask<Void, Void, List<StatusMessage>>() {

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
					if (!isAdded()) {
						return;
					}

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

			};
			if (Utils.isHoneycombOrHigher()) {
				asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				asyncTask.execute();
			}
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}

	@Override
	public void onEventReceived(String type, Object object) {

		if (!isAdded()) {
			return;
		}

		if (HikePubSub.TIMELINE_UPDATE_RECIEVED.equals(type)) {
			final StatusMessage statusMessage = (StatusMessage) object;
			final int startIndex = getStartIndex();
			resetUnseenStatusCount();

			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					statusMessages.add(startIndex, statusMessage);
					if (noStatusMessage != null
							&& (statusMessages.size() >= HikeConstants.MIN_STATUS_COUNT || statusMessage
									.getMsisdn().equals(userMsisdn))) {
						statusMessages.remove(noStatusMessage);
						noStatusMessage = null;
					}
					centralTimelineAdapter.notifyDataSetChanged();
				}
			});
			HikeMessengerApp.getPubSub().publish(
					HikePubSub.RESET_NOTIFICATION_COUNTER, null);
		} else if (HikePubSub.LARGER_UPDATE_IMAGE_DOWNLOADED.equals(type)) {
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					centralTimelineAdapter.notifyDataSetChanged();
				}
			});
		}
	}

	private int getStartIndex() {
		int startIndex = 0;
		if (noStatusMessage != null) {
			startIndex++;
		}
		return startIndex;
	}
}
