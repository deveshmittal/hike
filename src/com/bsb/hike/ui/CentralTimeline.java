package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

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
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.utils.DrawerBaseActivity;
import com.bsb.hike.utils.Utils;

public class CentralTimeline extends DrawerBaseActivity implements
		OnItemClickListener, Listener {

	private CentralTimelineAdapter centralTimelineAdapter;
	private ListView timelineContent;
	private List<StatusMessage> statusMessages;
	private int friendRequests;
	private StatusMessage noStatusMessage;
	private StatusMessage noFriendMessage;

	private String[] pubSubListeners = new String[] {
			HikePubSub.FAVORITE_TOGGLED, HikePubSub.STATUS_MESSAGE_RECEIVED };
	private String userMsisdn;

	@Override
	protected void onPause() {
		super.onPause();
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, null);
	}

	@Override
	protected void onResume() {
		super.onResume();
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, this);
		if (isLastStatusMessageUnseen()) {
			HikeConversationsDatabase.getInstance().setStatusMessagesSeen(null);
			HikeMessengerApp.getPubSub().publish(
					HikePubSub.RESET_NOTIFICATION_COUNTER, null);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.central_timeline);
		afterSetContentView(savedInstanceState, false);

		TextView titleTV = (TextView) findViewById(R.id.title);
		titleTV.setText(R.string.recent_updates);

		timelineContent = (ListView) findViewById(R.id.timeline_content);

		userMsisdn = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
				MODE_PRIVATE).getString(HikeMessengerApp.MSISDN_SETTING, "");

		List<ContactInfo> friendRequestList = HikeUserDatabase.getInstance()
				.getContactsOfFavoriteType(FavoriteType.RECOMMENDED_FAVORITE,
						HikeConstants.BOTH_VALUE);

		List<ContactInfo> friendsList = HikeUserDatabase.getInstance()
				.getContactsOfFavoriteType(FavoriteType.FAVORITE,
						HikeConstants.BOTH_VALUE);

		friendRequests = friendRequestList.size();

		int friendMsisdnLength = friendRequestList.size() + friendsList.size();

		ArrayList<String> msisdnList = new ArrayList<String>();

		for (ContactInfo contactInfo : friendRequestList) {
			/*
			 * We don't show status updates from unknown contacts
			 */
			if (contactInfo.getMsisdn().equals(contactInfo.getId())
					|| TextUtils.isEmpty(contactInfo.getMsisdn())) {
				continue;
			}
			msisdnList.add(contactInfo.getMsisdn());
		}
		for (ContactInfo contactInfo : friendsList) {
			/*
			 * We don't show status updates from unknown contacts
			 */
			if (contactInfo.getMsisdn().equals(contactInfo.getId())
					|| TextUtils.isEmpty(contactInfo.getMsisdn())) {
				continue;
			}
			msisdnList.add(contactInfo.getMsisdn());
		}
		msisdnList.add(userMsisdn);

		String[] friendMsisdns = new String[msisdnList.size()];
		msisdnList.toArray(friendMsisdns);
		statusMessages = HikeConversationsDatabase.getInstance()
				.getStatusMessages(friendMsisdns);

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

		SharedPreferences preferences = getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
		String name = preferences
				.getString(HikeMessengerApp.NAME_SETTING, null);
		String lastStatus = preferences.getString(HikeMessengerApp.LAST_STATUS,
				"");

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
			}
		}
		if (friendMsisdnLength == 0) {
			noFriendMessage = new StatusMessage(
					CentralTimelineAdapter.EMPTY_STATUS_NO_FRIEND_ID, null,
					"12345", getString(R.string.team_hike), getString(
							R.string.hey_name, name),
					StatusMessageType.NO_STATUS,
					System.currentTimeMillis() / 1000);
			statusMessages.add(0, noFriendMessage);
		}

		centralTimelineAdapter = new CentralTimelineAdapter(this,
				statusMessages, userMsisdn);
		timelineContent.setAdapter(centralTimelineAdapter);
		timelineContent.setOnItemClickListener(this);

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		for (int i = 0; i < friendRequests; i++) {
			notificationManager.cancel(statusMessages.get(i).getMsisdn()
					.hashCode());
		}

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view,
			int position, long id) {
		StatusMessage statusMessage = centralTimelineAdapter.getItem(position);
		if ((statusMessage.getStatusMessageType() == StatusMessageType.NO_STATUS)
				|| (statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST)) {
			return;
		} else if (userMsisdn.equals(statusMessage.getMsisdn())) {
			Intent intent = new Intent(this, ProfileActivity.class);
			intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
			startActivity(intent);
			return;
		}

		Intent intent = Utils.createIntentFromContactInfo(new ContactInfo(null,
				statusMessage.getMsisdn(), statusMessage.getName(),
				statusMessage.getMsisdn()));
		intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
		intent.setClass(this, ChatThread.class);
		startActivity(intent);
	}

	public void onYesBtnClick(View v) {
		StatusMessage statusMessage = (StatusMessage) v.getTag();

		if (CentralTimelineAdapter.EMPTY_STATUS_NO_FRIEND_ID == statusMessage
				.getId()) {
			parentLayout.openRightSidebar();
		} else if (CentralTimelineAdapter.EMPTY_STATUS_NO_STATUS_ID == statusMessage
				.getId()) {
			showStatusDialog(false);
		} else {
			toggleFavoriteAndRemoveTimelineItem(statusMessage,
					FavoriteType.FAVORITE);
		}
	}

	public void onNoBtnClick(View v) {
		StatusMessage statusMessage = (StatusMessage) v.getTag();
		toggleFavoriteAndRemoveTimelineItem(statusMessage,
				FavoriteType.NOT_FAVORITE);
	}

	private void toggleFavoriteAndRemoveTimelineItem(
			StatusMessage statusMessage, FavoriteType favoriteType) {
		ContactInfo contactInfo = HikeUserDatabase.getInstance()
				.getContactInfoFromMSISDN(statusMessage.getMsisdn(), false);

		Pair<ContactInfo, FavoriteType> favoriteAdded = new Pair<ContactInfo, FavoriteType>(
				contactInfo, favoriteType);
		HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_TOGGLED,
				favoriteAdded);

		for (StatusMessage listItem : statusMessages) {
			if (listItem.getMsisdn().equals(statusMessage.getMsisdn())
					&& listItem.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST) {
				statusMessages.remove(listItem);
				friendRequests--;
				break;
			}
		}
		centralTimelineAdapter.notifyDataSetChanged();
	}

	public void onDetailsBtnClick(View v) {
		StatusMessage statusMessage = (StatusMessage) v.getTag();

		Intent intent = new Intent(this, ProfileActivity.class);
		intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);

		if ((statusMessage.getStatusMessageType() == StatusMessageType.NO_STATUS)
				|| (statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST)) {
			return;
		} else if (userMsisdn.equals(statusMessage.getMsisdn())) {
			startActivity(intent);
			return;
		}

		intent.setClass(this, ProfileActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(HikeConstants.Extras.CONTACT_INFO,
				statusMessage.getMsisdn());
		startActivity(intent);
	}

	@Override
	public void onEventReceived(String type, Object object) {
		super.onEventReceived(type, object);
		if (HikePubSub.FAVORITE_TOGGLED.equals(type)) {
			final Pair<ContactInfo, FavoriteType> favoriteToggle = (Pair<ContactInfo, FavoriteType>) object;
			ContactInfo contactInfo = favoriteToggle.first;
			if (favoriteToggle.second != FavoriteType.RECOMMENDED_FAVORITE
					|| favoriteToggle.second != FavoriteType.FAVORITE) {
				return;
			}
			int startIndex = getStartIndex();
			statusMessages
					.add(startIndex,
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
			friendRequests++;
			if (noStatusMessage != null) {
				statusMessages.remove(noFriendMessage);
				noFriendMessage = null;
			}
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					centralTimelineAdapter.notifyDataSetChanged();
				}
			});
			HikeMessengerApp.getPubSub().publish(
					HikePubSub.RESET_NOTIFICATION_COUNTER, null);

		} else if (HikePubSub.STATUS_MESSAGE_RECEIVED.equals(type)) {
			StatusMessage statusMessage = (StatusMessage) object;
			ContactInfo contactInfo = HikeUserDatabase.getInstance()
					.getContactInfoFromMSISDN(statusMessage.getMsisdn(), false);
			statusMessage.setName(contactInfo.getName());

			int startIndex = getStartIndex();

			statusMessages.add(friendRequests + startIndex, statusMessage);
			HikeConversationsDatabase.getInstance().setStatusMessagesSeen(null);
			if (noStatusMessage != null
					&& statusMessages.size() >= HikeConstants.MIN_STATUS_COUNT) {
				statusMessages.remove(noStatusMessage);
				noStatusMessage = null;
			}
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
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

	private boolean isLastStatusMessageUnseen() {
		/*
		 * We check here whether the latest status message is unseen or not. For
		 * that we ignore the friend requests (Which are always marked as
		 * unseen).
		 */
		int startIndex = friendRequests;
		if (statusMessages.size() <= friendRequests) {
			return false;
		}
		return !statusMessages.get(startIndex).isStatusSeen();
	}

	public void onViewImageClicked(View v) {

	}
}
