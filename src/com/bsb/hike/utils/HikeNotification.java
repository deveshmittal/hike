package com.bsb.hike.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.ui.CentralTimeline;
import com.bsb.hike.ui.ChatThread;

public class HikeNotification {
	public static final int HIKE_NOTIFICATION = 0;

	private Context context;

	private NotificationManager notificationManager;
	private long lastNotificationTime;

	public static final int BATCH_SU_NOTIFICATION_ID = 9876;
	private static final long MIN_TIME_BETWEEN_NOTIFICATIONS = 5 * 1000;

	public HikeNotification(Context context) {
		this.context = context;
		this.notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	public void notifyMessage(ContactInfo contactInfo, ConvMessage convMsg) {
		int notificationId = (int) convMsg.getConversation().getConvId();

		String msisdn = convMsg.getMsisdn();
		String message = (!convMsg.isFileTransferMessage()) ? convMsg
				.getMessage() : HikeFileType.getFileTypeMessage(context,
				convMsg.getMetadata().getHikeFiles().get(0).getHikeFileType(),
				convMsg.isSent());
		// Message will be empty for type 'uj' when the conversation does not
		// exist
		if (TextUtils.isEmpty(message)
				&& convMsg.getParticipantInfoState() == ParticipantInfoState.USER_JOIN) {
			message = String.format(
					context.getString(R.string.joined_hike_new),
					contactInfo.getFirstName());
		}
		long timestamp = convMsg.getTimestamp();

		String key = (contactInfo != null && !TextUtils.isEmpty(contactInfo
				.getName())) ? contactInfo.getName() : msisdn;

		// For showing the name of the contact that sent the message in a group
		// chat
		if (convMsg.isGroupChat()
				&& !TextUtils.isEmpty(convMsg.getGroupParticipantMsisdn())
				&& convMsg.getParticipantInfoState() == ParticipantInfoState.NO_INFO) {
			HikeUserDatabase hUDB = HikeUserDatabase.getInstance();
			ContactInfo participant = hUDB.getContactInfoFromMSISDN(
					convMsg.getGroupParticipantMsisdn(), false);

			if (TextUtils.isEmpty(participant.getName())) {
				HikeConversationsDatabase hCDB = HikeConversationsDatabase
						.getInstance();
				participant.setName(hCDB.getParticipantName(msisdn,
						convMsg.getGroupParticipantMsisdn()));
			}
			message = participant.getFirstName() + HikeConstants.SEPARATOR
					+ message;
		}

		int icon = R.drawable.ic_contact_logo;

		/*
		 * Jellybean has added support for emojis so we don't need to add a '*'
		 * to replace them
		 */
		if (Build.VERSION.SDK_INT < 16) {
			// Replace emojis with a '*'
			message = SmileyParser.getInstance().replaceEmojiWithCharacter(
					message, "*");
		}

		Spanned text = Html.fromHtml(String.format("<bold>%1$s</bold>: %2$s",
				key, message));

		Intent notificationIntent = new Intent(context, ChatThread.class);

		/*
		 * notifications appear to be cached, and their .equals doesn't check
		 * 'Extra's. In order to prevent the wrong intent being fired, set a
		 * data field that's unique to the conversation we want to open.
		 * http://groups
		 * .google.com/group/android-developers/browse_thread/thread
		 * /e61ec1e8d88ea94d/1fe953564bd11609?#1fe953564bd11609
		 */

		notificationIntent.setData((Uri.parse("custom://" + notificationId)));

		notificationIntent.putExtra(HikeConstants.Extras.MSISDN, msisdn);
		if (contactInfo != null) {
			if (contactInfo.getName() != null) {
				notificationIntent.putExtra(HikeConstants.Extras.NAME,
						contactInfo.getName());
			}
		}

		showNotification(notificationIntent, icon, timestamp, notificationId,
				text, key, message);
	}

	public void notifyFavorite(ContactInfo contactInfo) {
		int notificationId = contactInfo.getMsisdn().hashCode();

		String msisdn = contactInfo.getMsisdn();

		long timeStamp = System.currentTimeMillis() / 1000;

		Intent notificationIntent = new Intent(context, CentralTimeline.class);
		notificationIntent.setData((Uri.parse("custom://" + notificationId)));

		int icon = R.drawable.ic_contact_logo;

		String key = (contactInfo != null && !TextUtils.isEmpty(contactInfo
				.getName())) ? contactInfo.getName() : msisdn;

		String message = context.getString(R.string.added_friend);

		Spanned text = Html.fromHtml(String.format("<bold>%1$s</bold>: %2$s",
				key, message));

		showNotification(notificationIntent, icon, timeStamp, notificationId,
				text, key, message);
	}

	public void notifyStatusMessage(StatusMessage statusMessage) {
		/*
		 * We only proceed if the current status preference value is 0 which
		 * denotes that the user wants immediate notifications. Else we simply
		 * return
		 */
		if (PreferenceManager.getDefaultSharedPreferences(this.context).getInt(
				HikeConstants.STATUS_PREF, 0) != 0) {
			return;
		}
		int notificationId = statusMessage.getMsisdn().hashCode();

		long timeStamp = statusMessage.getTimeStamp();

		Intent notificationIntent = new Intent(context, CentralTimeline.class);
		notificationIntent.setData((Uri.parse("custom://" + notificationId)));

		int icon = R.drawable.ic_contact_logo;

		String key = statusMessage.getNotNullName();

		String message = null;
		String text = null;
		if (statusMessage.getStatusMessageType() == StatusMessageType.TEXT) {
			message = "\"" + statusMessage.getText() + "\"";
			/*
			 * Jellybean has added support for emojis so we don't need to add a
			 * '*' to replace them
			 */
			if (Build.VERSION.SDK_INT < 16) {
				// Replace emojis with a '*'
				message = SmileyParser.getInstance().replaceEmojiWithCharacter(
						message, "*");
			}
			text = context.getString(R.string.status_text_notification, key,
					message);
		} else if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC) {
			message = context
					.getString(R.string.status_profile_pic_notification);
			text = key + " " + message;
		} else if (statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST_ACCEPTED) {
			message = context.getString(R.string.confirmed_friend_2);
			text = key + " " + message;
		} else {
			/*
			 * We don't know how to display this type. Just return.
			 */
			return;
		}

		showNotification(notificationIntent, icon, timeStamp, notificationId,
				text, key, message);
	}

	public void notifyBatchUpdate(String header, String message) {
		int notificationId = 9876;

		long timeStamp = System.currentTimeMillis() / 1000;

		Intent notificationIntent = new Intent(context, CentralTimeline.class);
		notificationIntent.setData((Uri.parse("custom://" + notificationId)));

		int icon = R.drawable.ic_contact_logo;

		String key = header;

		String text = message;

		showNotification(notificationIntent, icon, timeStamp, notificationId,
				text, key, message);
	}

	private void showNotification(Intent notificationIntent, int icon,
			long timestamp, int notificationId, CharSequence text, String key,
			String message) {

		boolean shouldNotPlayNotification = (System.currentTimeMillis() - lastNotificationTime) < MIN_TIME_BETWEEN_NOTIFICATIONS;

		SharedPreferences preferenceManager = PreferenceManager
				.getDefaultSharedPreferences(this.context);

		int playSound = preferenceManager.getBoolean(HikeConstants.SOUND_PREF,
				true) && !shouldNotPlayNotification ? Notification.DEFAULT_SOUND
				: 0;

		int vibrate = preferenceManager.getBoolean(HikeConstants.VIBRATE_PREF,
				true) && !shouldNotPlayNotification ? Notification.DEFAULT_VIBRATE
				: 0;

		boolean playNativeJingle = preferenceManager.getBoolean(
				HikeConstants.NATIVE_JINGLE_PREF, true);

		boolean led = preferenceManager
				.getBoolean(HikeConstants.LED_PREF, true);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				context).setSmallIcon(R.drawable.ic_contact_logo)
				.setContentTitle(key).setContentText(message).setTicker(text)
				.setDefaults(vibrate);

		if (playNativeJingle && playSound != 0) {
			mBuilder.setSound(Uri.parse("android.resource://"
					+ context.getPackageName() + "/" + R.raw.v1));
		} else if (playSound != 0) {
			mBuilder.setDefaults(playSound);
		}

		if (led) {
			mBuilder.setLights(Color.BLUE, 300, 1000);
		}

		TaskStackBuilder stackBuilder = TaskStackBuilder.from(context);
		stackBuilder.addNextIntent(notificationIntent);

		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(resultPendingIntent);

		notificationManager.notify(notificationId, mBuilder.getNotification());

		lastNotificationTime = shouldNotPlayNotification ? lastNotificationTime
				: System.currentTimeMillis();
	}
}
