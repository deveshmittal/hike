package com.bsb.hike.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.Protip;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.HomeActivity;

public class HikeNotification {
	public static final int HIKE_NOTIFICATION = 0;
	public static final int BATCH_SU_NOTIFICATION_ID = 9876;
	public static final int PROTIP_NOTIFICATION_ID = -123;
	public static final int GAMING_PACKET_NOTIFICATION_ID = -124;
	public static final int FREE_SMS_POPUP_NOTIFICATION_ID = -125;
	public static final int APP_UPDATE_AVAILABLE_ID = -126;
	private static final long MIN_TIME_BETWEEN_NOTIFICATIONS = 5 * 1000;
	private static final String SEPERATOR = " ";

	private final Context context;

	private final NotificationManager notificationManager;
	private long lastNotificationTime;
	private final SharedPreferences sharedPreferences;

	public HikeNotification(final Context context) {
		this.context = context;
		this.notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		this.sharedPreferences = context.getSharedPreferences(
				HikeMessengerApp.STATUS_NOTIFICATION_SETTING, 0);
	}

	public void notifySMSPopup(final String bodyString) {
		/*
		 * return straight away if the block notification setting is ON
		 */
		if (sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS,
				false)) {
			return;
		}

		/*
		 * invoke the chat thread here. The free SMS invite switch popup should
		 * already be showing here ideally by now.
		 */
		final Intent notificationIntent = Utils.getHomeActivityIntent(context,
				HomeActivity.CHATS_TAB_INDEX);
		notificationIntent.putExtra(HikeConstants.Extras.NAME,
				context.getString(R.string.team_hike));

		notificationIntent.setData((Uri.parse("custom://"
				+ FREE_SMS_POPUP_NOTIFICATION_ID)));
		final Drawable avatarDrawable = context.getResources().getDrawable(
				R.drawable.hike_avtar_protip);
		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder = getNotificationBuilder(
				context.getString(R.string.team_hike), bodyString, bodyString,
				avatarDrawable, smallIconId, false);
		setNotificationIntentForBuilder(mBuilder, notificationIntent);

		notificationManager.notify(FREE_SMS_POPUP_NOTIFICATION_ID,
				mBuilder.getNotification());

	}

	public void notifyMessage(final Protip proTip) {
		final SharedPreferences preferenceManager = PreferenceManager
				.getDefaultSharedPreferences(this.context);

		// we've got to invoke the timeline here
		final Intent notificationIntent = Utils.getHomeActivityIntent(context,
				HomeActivity.UPDATES_TAB_INDEX);
		notificationIntent.putExtra(HikeConstants.Extras.NAME,
				context.getString(R.string.team_hike));

		notificationIntent.setData((Uri.parse("custom://"
				+ PROTIP_NOTIFICATION_ID)));

		final Drawable avatarDrawable = context.getResources().getDrawable(
				R.drawable.hike_avtar_protip);
		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder = getNotificationBuilder(
				context.getString(R.string.team_hike), proTip.getHeader(),
				proTip.getHeader(), avatarDrawable, smallIconId, false);

		setNotificationIntentForBuilder(mBuilder, notificationIntent);

		if (!sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS,
				false)) {
			notificationManager.notify(PROTIP_NOTIFICATION_ID,
					mBuilder.getNotification());
		}
	}

	/*
	 * method to send a notification of an hike update available or
	 * applicationspush update. if isApplicationsPushUpdate is false than it is
	 * hike app update.
	 */
	public void notifyUpdatePush(int updateType, String packageName,
			String message, boolean isApplicationsPushUpdate) {

		message = (TextUtils.isEmpty(message)) ? context
				.getString(R.string.update_app) : message;
		final Drawable avatarDrawable = context.getResources().getDrawable(
				R.drawable.hike_avtar_protip);
		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder = getNotificationBuilder(
				context.getString(R.string.team_hike), message, message,
				avatarDrawable, smallIconId, false);

		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("market://details?id=" + packageName));
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY
				| Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		mBuilder.setContentIntent(PendingIntent.getActivity(context, 0, intent,
				0));

		if (!sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS,
				false)) {
			int notificationId = isApplicationsPushUpdate ? GAMING_PACKET_NOTIFICATION_ID
					: APP_UPDATE_AVAILABLE_ID;
			notificationManager.notify(notificationId,
					mBuilder.getNotification());
		}
		// TODO:: we should reset the gaming download message from preferences
	}

	public void notifyMessage(final ContactInfo contactInfo,
			final ConvMessage convMsg, boolean isRich, Bitmap bigPictureImage) {
		final String msisdn = convMsg.getMsisdn();
		// we are using the MSISDN now to group the notifications
		final int notificationId = msisdn.hashCode();

		String message = (!convMsg.isFileTransferMessage()) ? convMsg
				.getMessage() : HikeFileType.getFileTypeMessage(context,
				convMsg.getMetadata().getHikeFiles().get(0).getHikeFileType(),
				convMsg.isSent());
		// Message will be empty for type 'uj' when the conversation does not
		// exist
		if (TextUtils.isEmpty(message)
				&& (convMsg.getParticipantInfoState() == ParticipantInfoState.USER_JOIN || convMsg
						.getParticipantInfoState() == ParticipantInfoState.CHAT_BACKGROUND)) {
			if (convMsg.getParticipantInfoState() == ParticipantInfoState.USER_JOIN) {
				message = String
						.format(context.getString(convMsg.getMetadata()
								.isOldUser() ? R.string.user_back_on_hike
								: R.string.joined_hike_new), contactInfo
								.getFirstName());
			} else {
				message = context.getString(R.string.chat_bg_changed,
						contactInfo.getFirstName());
			}
		}
		final long timestamp = convMsg.getTimestamp();

		String key = (contactInfo != null && !TextUtils.isEmpty(contactInfo
				.getName())) ? contactInfo.getName() : msisdn;

		// we've got to invoke the chat thread from here with the respective
		// users
		final Intent notificationIntent = new Intent(context, ChatThread.class);
		if (contactInfo.getName() != null) {
			notificationIntent.putExtra(HikeConstants.Extras.NAME,
					contactInfo.getName());
		}
		notificationIntent.putExtra(HikeConstants.Extras.MSISDN,
				contactInfo.getMsisdn());

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
		final int icon = returnSmallIcon();

		/*
		 * Jellybean has added support for emojis so we don't need to add a '*'
		 * to replace them
		 */
		if (Build.VERSION.SDK_INT < 16) {
			// Replace emojis with a '*'
			message = SmileyParser.getInstance().replaceEmojiWithCharacter(
					message, "*");
		}

		String partName = "";
		// For showing the name of the contact that sent the message in a group
		// chat
		if (convMsg.isGroupChat()
				&& !TextUtils.isEmpty(convMsg.getGroupParticipantMsisdn())
				&& convMsg.getParticipantInfoState() == ParticipantInfoState.NO_INFO) {

			HikeUserDatabase hUDB = HikeUserDatabase.getInstance();
			ContactInfo participant = hUDB.getContactInfoFromMSISDN(
					convMsg.getGroupParticipantMsisdn(), false);

			Conversation gConv = convMsg.getConversation();

			key = participant.getName();
			if (TextUtils.isEmpty(key)) {
				key = participant.getMsisdn();
			}
			partName = key;
			message = key + HikeConstants.SEPARATOR + message;
			key = gConv.getLabel();
		}

		boolean doesBigPictureExist = (bigPictureImage == null) ? false : true;
		final String text = String.format("%1$s: %2$s", key, message);
		// For showing the name of the contact that sent the message in a group
		// chat

		if (doesBigPictureExist && isRich) {
			final String messageString = (!convMsg.isFileTransferMessage()) ? convMsg
					.getMessage() : HikeFileType.getFileTypeMessage(context,
					convMsg.getMetadata().getHikeFiles().get(0)
							.getHikeFileType(), convMsg.isSent());

			if (convMsg.isGroupChat()) {
				message = partName + HikeConstants.SEPARATOR + messageString;
			} else
				message = messageString;
			// big picture messages ! intercept !
			showNotification(notificationIntent, icon, timestamp,
					notificationId, text, key, message, msisdn, bigPictureImage);
		} else {
			// regular message
			showNotification(notificationIntent, icon, timestamp,
					notificationId, text, key, message, msisdn, null);
		}
	}

	public void notifyFavorite(final ContactInfo contactInfo) {
		final int notificationId = contactInfo.getMsisdn().hashCode();

		final String msisdn = contactInfo.getMsisdn();

		final long timeStamp = System.currentTimeMillis() / 1000;

		final Intent notificationIntent = Utils.getHomeActivityIntent(context,
				HomeActivity.FRIENDS_TAB_INDEX);
		notificationIntent.setData((Uri.parse("custom://" + notificationId)));

		final int icon = returnSmallIcon();

		final String key = (contactInfo != null && !TextUtils
				.isEmpty(contactInfo.getName())) ? contactInfo.getName()
				: msisdn;

		final String message = context
				.getString(R.string.add_as_friend_notification_line);

		final String text = context.getString(
				R.string.add_as_friend_notification, key);

		showNotification(notificationIntent, icon, timeStamp, notificationId,
				text, key, message, msisdn, null);
		addNotificationId(notificationId);
	}

	public void notifyStatusMessage(final StatusMessage statusMessage) {
		/*
		 * We only proceed if the current status preference value is 0 which
		 * denotes that the user wants immediate notifications. Else we simply
		 * return
		 */
		if (PreferenceManager.getDefaultSharedPreferences(this.context).getInt(
				HikeConstants.STATUS_PREF, 0) != 0) {
			return;
		}
		final int notificationId = statusMessage.getMsisdn().hashCode();

		final long timeStamp = statusMessage.getTimeStamp();

		final Intent notificationIntent = Utils.getHomeActivityIntent(context,
				HomeActivity.UPDATES_TAB_INDEX);
		notificationIntent.setData((Uri.parse("custom://" + notificationId)));

		final int icon = returnSmallIcon();

		final String key = statusMessage.getNotNullName();

		String message = null;
		String text = null;
		if (statusMessage.getStatusMessageType() == StatusMessageType.TEXT) {
			message = context.getString(R.string.status_text_notification, "\""
					+ statusMessage.getText() + "\"");
			/*
			 * Jellybean has added support for emojis so we don't need to add a
			 * '*' to replace them
			 */
			if (Build.VERSION.SDK_INT < 16) {
				// Replace emojis with a '*'
				message = SmileyParser.getInstance().replaceEmojiWithCharacter(
						message, "*");
			}
			text = key + " " + message;
		} else if (statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST_ACCEPTED) {
			message = context.getString(R.string.confirmed_friend_2, key);
			text = message;
		} else if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC) {
			message = context.getString(
					R.string.status_profile_pic_notification, key);
			text = key + " " + message;
		} else {
			/*
			 * We don't know how to display this type. Just return.
			 */
			return;
		}

		showNotification(notificationIntent, icon, timeStamp, notificationId,
				text, key, message, statusMessage.getMsisdn(), null);
		addNotificationId(notificationId);
	}

	public void notifyBigPictureStatusNotification(final String imagePath,
			final String msisdn, final String name) {

		if (PreferenceManager.getDefaultSharedPreferences(this.context).getInt(
				HikeConstants.STATUS_PREF, 0) != 0) {
			return;
		}

		final int notificationId = msisdn.hashCode();
		final String key = TextUtils.isEmpty(name) ? msisdn : name;
		final String message = context
				.getString(R.string.status_profile_pic_notification);
		final String text = key + " " + message;

		final int icon = returnSmallIcon();
		final Bitmap bigPictureImage = BitmapFactory.decodeFile(imagePath);
		final Intent notificationIntent = Utils.getHomeActivityIntent(context,
				HomeActivity.UPDATES_TAB_INDEX);
		notificationIntent.setData((Uri.parse("custom://" + notificationId)));
		notificationIntent.putExtra(HikeConstants.Extras.MSISDN,
				msisdn.toString());

		showNotification(notificationIntent, icon, System.currentTimeMillis(),
				notificationId, text, key, message, msisdn, bigPictureImage);
	}

	public void notifyBatchUpdate(final String header, final String message) {
		final long timeStamp = System.currentTimeMillis() / 1000;

		final int notificationId = (int) timeStamp;

		final Intent notificationIntent = Utils.getHomeActivityIntent(context,
				HomeActivity.UPDATES_TAB_INDEX);
		notificationIntent.setData((Uri.parse("custom://" + notificationId)));

		final int icon = returnSmallIcon();

		final String key = header;

		final String text = message;

		showNotification(notificationIntent, icon, timeStamp, notificationId,
				text, key, message, null, null); // TODO: change this.
		addNotificationId(notificationId);
	}

	private void addNotificationId(final int id) {
		String ids = sharedPreferences.getString(HikeMessengerApp.STATUS_IDS,
				"");

		ids += Integer.toString(id) + SEPERATOR;

		final Editor editor = sharedPreferences.edit();
		editor.putString(HikeMessengerApp.STATUS_IDS, ids);
		editor.commit();
	}

	public void cancelAllStatusNotifications() {
		final String ids = sharedPreferences.getString(
				HikeMessengerApp.STATUS_IDS, "");
		final String[] idArray = ids.split(SEPERATOR);

		for (final String id : idArray) {
			if (TextUtils.isEmpty(id.trim())) {
				continue;
			}
			notificationManager.cancel(Integer.parseInt(id));
		}

		final Editor editor = sharedPreferences.edit();
		editor.remove(HikeMessengerApp.STATUS_IDS);
		editor.commit();
	}

	public void cancelAllNotifications() {
		notificationManager.cancelAll();
	}

	private void showNotification(final Intent notificationIntent,
			final int icon, final long timestamp, final int notificationId,
			final CharSequence text, final String key, final String message,
			final String msisdn, final Bitmap bigPictureImage) {

		final boolean shouldNotPlayNotification = (System.currentTimeMillis() - lastNotificationTime) < MIN_TIME_BETWEEN_NOTIFICATIONS;

		final Drawable avatarDrawable = IconCacheManager.getInstance()
				.getIconForMSISDN(msisdn);
		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder;
		if (bigPictureImage != null) {
			mBuilder = getNotificationBuilder(key, message, text.toString(),
					avatarDrawable, smallIconId, true);
			final NotificationCompat.BigPictureStyle bigPicStyle = new NotificationCompat.BigPictureStyle();
			bigPicStyle.setBigContentTitle(key);
			bigPicStyle.setSummaryText(message);
			mBuilder.setStyle(bigPicStyle);
			// set the big picture image
			bigPicStyle.bigPicture(bigPictureImage);

			mBuilder.setSound(null);
		} else {
			mBuilder = getNotificationBuilder(key, message, text.toString(),
					avatarDrawable, smallIconId, false);
		}

		setNotificationIntentForBuilder(mBuilder, notificationIntent);
		if (!sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS,
				false)) {
			notificationManager.notify(notificationId,
					mBuilder.getNotification());
			lastNotificationTime = shouldNotPlayNotification ? lastNotificationTime
					: System.currentTimeMillis();
		}
	}

	private int returnSmallIcon() {
		if (Build.VERSION.SDK_INT < 16) {
			return R.drawable.ic_contact_logo;

		} else {
			return R.drawable.ic_stat_notify;
		}

	}

	/*
	 * creates a notification builder with sound, led and vibrate option set
	 * according to app preferences. forceNotPlaySound : true if we want to
	 * force not to play notification sounds or lights.
	 */
	public NotificationCompat.Builder getNotificationBuilder(
			String contentTitle, String contentText, String tickerText,
			Drawable avatarDrawable, int smallIconId, boolean forceNotPlaySound) {

		final SharedPreferences preferenceManager = PreferenceManager
				.getDefaultSharedPreferences(this.context);

		final boolean shouldNotPlayNotification = (System.currentTimeMillis() - lastNotificationTime) < MIN_TIME_BETWEEN_NOTIFICATIONS;
		final int vibrate = preferenceManager.getBoolean(
				HikeConstants.VIBRATE_PREF, false) ? Notification.DEFAULT_VIBRATE
				: 0;
		final boolean led = preferenceManager.getBoolean(
				HikeConstants.LED_PREF, true);

		final int playSound = preferenceManager.getBoolean(
				HikeConstants.SOUND_PREF, true) && !shouldNotPlayNotification ? Notification.DEFAULT_SOUND
				: 0;

		final boolean playNativeJingle = preferenceManager.getBoolean(
				HikeConstants.NATIVE_JINGLE_PREF, true);

		final Bitmap avatarBitmap = Utils.returnScaledBitmap(
				(Utils.drawableToBitmap(avatarDrawable)), context);

		final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				context).setContentTitle(contentTitle)
				.setSmallIcon(smallIconId).setLargeIcon(avatarBitmap)
				.setContentText(contentText).setAutoCancel(true)
				.setTicker(tickerText).setDefaults(vibrate)
				.setPriority(Notification.PRIORITY_HIGH);

		if (!forceNotPlaySound) {
			if (playNativeJingle && playSound != 0) {
				mBuilder.setSound(Uri.parse("android.resource://"
						+ context.getPackageName() + "/" + R.raw.v1));
			} else if (playSound != 0) {
				mBuilder.setDefaults(mBuilder.getNotification().defaults
						| playSound);
			}
			if (led) {
				mBuilder.setLights(Color.BLUE, HikeConstants.LED_LIGHTS_ON_MS,
						HikeConstants.LED_LIGHTS_OFF_MS);
			}
		}
		return mBuilder;
	}

	public void setNotificationIntentForBuilder(
			NotificationCompat.Builder mBuilder, Intent notificationIntent) {
		final TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
		stackBuilder.addNextIntent(notificationIntent);

		final PendingIntent resultPendingIntent = stackBuilder
				.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(resultPendingIntent);
	}
}
