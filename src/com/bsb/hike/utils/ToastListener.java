package com.bsb.hike.utils;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.service.HikeMqttManager;
import com.bsb.hike.service.HikeMqttManager.MQTTConnectionStatus;
import com.bsb.hike.ui.ChatThread;

public class ToastListener implements Listener {

	private WeakReference<Activity> currentActivity;

	private HikeNotification toaster;

	private HikeUserDatabase db;

	private Context context;

	private MQTTConnectionStatus mCurrentUnnotifiedStatus;

	public ToastListener(Context context) {
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_RECEIVED,
				this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.NEW_ACTIVITY, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.CONNECTION_STATUS,
				this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.FAVORITE_TOGGLED,
				this);
		HikeMessengerApp.getPubSub().addListener(
				HikePubSub.TIMELINE_UPDATE_RECIEVED, this);
		HikeMessengerApp.getPubSub().addListener(
				HikePubSub.BATCH_STATUS_UPDATE_PUSH_RECEIVED, this);
		HikeMessengerApp.getPubSub().addListener(
				HikePubSub.CANCEL_ALL_STATUS_NOTIFICATIONS, this);
		this.toaster = new HikeNotification(context);
		this.db = HikeUserDatabase.getInstance();
		this.context = context;
		mCurrentUnnotifiedStatus = MQTTConnectionStatus.INITIAL;
	}

	@Override
	public void onEventReceived(String type, Object object) {
		if (HikePubSub.NEW_ACTIVITY.equals(type)) {
			Activity activity = (Activity) object;
			if ((activity != null)
					&& (mCurrentUnnotifiedStatus != MQTTConnectionStatus.INITIAL)) {
				notifyConnStatus(mCurrentUnnotifiedStatus);
				mCurrentUnnotifiedStatus = MQTTConnectionStatus.INITIAL;
			}

			currentActivity = new WeakReference<Activity>(activity);
		} else if (HikePubSub.MESSAGE_RECEIVED.equals(type)) {
			ConvMessage message = (ConvMessage) object;

			HikeConversationsDatabase hCDB = HikeConversationsDatabase
					.getInstance();
			message.setConversation(hCDB.getConversation(message.getMsisdn(), 0));

			if (message.getConversation() == null) {
				Log.w(getClass().getSimpleName(),
						"The client did not get a GCJ message for us to handle this message.");
				return;
			}
			if ((message.getConversation() instanceof GroupConversation)
					&& ((GroupConversation) message.getConversation())
							.isMuted()) {
				Log.d(getClass().getSimpleName(), "Group has been muted");
				return;
			}
			if (message.getParticipantInfoState() == ParticipantInfoState.NO_INFO
					|| message.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_JOINED
					|| message.getParticipantInfoState() == ParticipantInfoState.USER_JOIN) {
				Activity activity = (currentActivity != null) ? currentActivity
						.get() : null;
				if ((activity instanceof ChatThread)) {
					String contactNumber = ((ChatThread) activity)
							.getContactNumber();
					if (message.getMsisdn().equals(contactNumber)) {
						return;
					}
				}

				/*
				 * the foreground activity isn't going to show this message so
				 * Toast it
				 */
				ContactInfo contactInfo;
				if (message.isGroupChat()) {
					Log.d("ToastListener", "GroupName is "
							+ message.getConversation().getLabel());
					contactInfo = new ContactInfo(message.getMsisdn(),
							message.getMsisdn(), message.getConversation()
									.getLabel(), message.getMsisdn());
				} else {
					contactInfo = this.db.getContactInfoFromMSISDN(
							message.getMsisdn(), false);
				}

				this.toaster.notifyMessage(contactInfo, message);
			}
		} else if (HikePubSub.CONNECTION_STATUS.equals(type)) {
			HikeMqttManager.MQTTConnectionStatus status = (HikeMqttManager.MQTTConnectionStatus) object;
			mCurrentUnnotifiedStatus = status;
			notifyConnStatus(status);
		} else if (HikePubSub.FAVORITE_TOGGLED.equals(type)) {
			final Pair<ContactInfo, FavoriteType> favoriteToggle = (Pair<ContactInfo, FavoriteType>) object;

			ContactInfo contactInfo = favoriteToggle.first;
			FavoriteType favoriteType = favoriteToggle.second;

			/*
			 * Only notify when someone has added the user as a favorite.
			 */
			if (favoriteType != FavoriteType.REQUEST_RECEIVED) {
				return;
			}
			Activity activity = (currentActivity != null) ? currentActivity
					.get() : null;
			toaster.notifyFavorite(contactInfo);
		} else if (HikePubSub.TIMELINE_UPDATE_RECIEVED.equals(type)) {
			if (currentActivity != null && currentActivity.get() != null) {
				return;
			}
			StatusMessage statusMessage = (StatusMessage) object;
			String msisdn = context.getSharedPreferences(
					HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(
					HikeMessengerApp.MSISDN_SETTING, "");
			if (msisdn.equals(statusMessage.getMsisdn())) {
				return;
			}
			toaster.notifyStatusMessage(statusMessage);
		} else if (HikePubSub.BATCH_STATUS_UPDATE_PUSH_RECEIVED.equals(type)) {
			if (currentActivity != null && currentActivity.get() != null) {
				return;
			}
			Pair<String, String> batchSU = (Pair<String, String>) object;
			toaster.notifyBatchUpdate(batchSU.first, batchSU.second);
		} else if (HikePubSub.CANCEL_ALL_STATUS_NOTIFICATIONS.equals(type)) {
			toaster.cancelAllStatusNotifications();
		}
	}

	private void notifyConnStatus(MQTTConnectionStatus status) {
		/* only show the trying to connect message after we've connected once */
		SharedPreferences settings = context.getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		boolean connectedOnce = settings.getBoolean(
				HikeMessengerApp.CONNECTED_ONCE, false);
		if (status == HikeMqttManager.MQTTConnectionStatus.CONNECTED) {
			NotificationManager notificationManager = (NotificationManager) context
					.getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.cancel(HikeConstants.HIKE_SYSTEM_NOTIFICATION);
			if (!connectedOnce) {
				Editor editor = settings.edit();
				editor.putBoolean(HikeMessengerApp.CONNECTED_ONCE, true);
				editor.commit();
			}
			return;
		}

		/* don't show any connection message until we've connected once */
		// Not showing any connection statuses for now..
		if (true) {
			return;
		}

		if ((currentActivity == null) || (currentActivity.get() == null)) {
			// no activity on the screen, so don't toast it
			return;
		}

		int icon = R.drawable.ic_contact_logo;

		int id = -1;
		Log.d("ToastListener", "status is " + status);
		switch (status) {
		case NOTCONNECTED_DATADISABLED:
			id = R.string.notconnected_data_disabled;
			break;
		case NOTCONNECTED_WAITINGFORINTERNET:
			id = R.string.notconnected_no_internet;
			break;
		case NOTCONNECTED_USERDISCONNECT:
			id = R.string.notconnected_no_internet;
			break;
		case NOTCONNECTED_UNKNOWNREASON:
			id = R.string.notconnected_no_internet;
			break;
		default:
			return;
		}

		String text = context.getResources().getString(id);
		Notification notification = new Notification(icon, text,
				System.currentTimeMillis());

		Intent notificationIntent = new Intent(
				android.provider.Settings.ACTION_WIRELESS_SETTINGS);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(context, context.getResources()
				.getString(R.string.hike_network_connection), text,
				contentIntent);

		NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(HikeConstants.HIKE_SYSTEM_NOTIFICATION,
				notification);
	}

}
