package com.bsb.hike.utils;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
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
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.Protip;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.HomeActivity;

public class HikeNotification
{
	private String VIB_OFF, VIB_DEF, VIB_SHORT, VIB_LONG;

	private String NOTIF_SOUND_OFF, NOTIF_SOUND_DEFAULT, NOTIF_SOUND_HIKE;

	public static final int HIKE_NOTIFICATION = 0;

	public static final int BATCH_SU_NOTIFICATION_ID = 9876;

	public static final int PROTIP_NOTIFICATION_ID = -123;

	public static final int GAMING_PACKET_NOTIFICATION_ID = -124;

	public static final int FREE_SMS_POPUP_NOTIFICATION_ID = -125;

	public static final int APP_UPDATE_AVAILABLE_ID = -126;

	public static final int STEALTH_NOTIFICATION_ID = -127;
	
	public static final int STEALTH_POPUP_NOTIFICATION_ID = -128;
	
	public static final int HIKE_TO_OFFLINE_PUSH_NOTIFICATION_ID = -129;

	private static final long MIN_TIME_BETWEEN_NOTIFICATIONS = 5 * 1000;

	private static final String SEPERATOR = " ";

	private final Context context;

	private final NotificationManager notificationManager;

	private long lastNotificationTime;

	private final SharedPreferences sharedPreferences;

	public HikeNotification(final Context context)
	{
		this.context = context;
		this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		this.sharedPreferences = context.getSharedPreferences(HikeMessengerApp.STATUS_NOTIFICATION_SETTING, 0);
		if (VIB_DEF == null)
		{
			Resources res = context.getResources();
			VIB_OFF = res.getString(R.string.vib_off);
			VIB_DEF = res.getString(R.string.vib_default);
			VIB_SHORT = res.getString(R.string.vib_short);
			VIB_LONG = res.getString(R.string.vib_long);
			NOTIF_SOUND_OFF = res.getString(R.string.notif_sound_off);
			NOTIF_SOUND_DEFAULT = res.getString(R.string.notif_sound_default);
			NOTIF_SOUND_HIKE = res.getString(R.string.notif_sound_Hike);
		}
	}

	public void notifySMSPopup(final String bodyString)
	{
		/*
		 * return straight away if the block notification setting is ON
		 */
		if (sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false))
		{
			return;
		}

		/*
		 * invoke the chat thread here. The free SMS invite switch popup should already be showing here ideally by now.
		 */
		final Intent notificationIntent = Utils.getHomeActivityIntent(context);
		notificationIntent.putExtra(HikeConstants.Extras.NAME, context.getString(R.string.team_hike));

		notificationIntent.setData((Uri.parse("custom://" + FREE_SMS_POPUP_NOTIFICATION_ID)));
		final Drawable avatarDrawable = context.getResources().getDrawable(R.drawable.hike_avtar_protip);
		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder = getNotificationBuilder(context.getString(R.string.team_hike), bodyString, bodyString, avatarDrawable, smallIconId, false);
		setNotificationIntentForBuilder(mBuilder, notificationIntent);

		notificationManager.notify(FREE_SMS_POPUP_NOTIFICATION_ID, mBuilder.getNotification());

	}

	public void notifyStealthPopup(final String headerString)
	{
		/*
		 * return straight away if the block notification setting is ON
		 */
		if (sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false))
		{
			return;
		}

		/*
		 * invoke the chat thread here. The Stealth tip popup should already be showing here ideally by now.
		 */
		final Intent notificationIntent = Utils.getHomeActivityIntent(context);
		notificationIntent.putExtra(HikeConstants.Extras.HAS_TIP, true);
		notificationIntent.putExtra(HikeConstants.Extras.NAME, context.getString(R.string.team_hike));

		notificationIntent.setData((Uri.parse("custom://" + STEALTH_POPUP_NOTIFICATION_ID)));
		final Drawable avatarDrawable = context.getResources().getDrawable(R.drawable.hike_avtar_protip);
		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder = getNotificationBuilder(context.getString(R.string.team_hike), headerString, headerString, avatarDrawable, smallIconId, false);
		setNotificationIntentForBuilder(mBuilder, notificationIntent);

		notificationManager.notify(FREE_SMS_POPUP_NOTIFICATION_ID, mBuilder.getNotification());

	}

	public void notifyAtomicPopup(final String message, Intent notificationIntent)
	{
		/*
		 * return straight away if the block notification setting is ON
		 */
		if (sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false))
		{
			return;
		}

		notificationIntent.putExtra(HikeConstants.Extras.NAME, context.getString(R.string.team_hike));

		final Drawable avatarDrawable = context.getResources().getDrawable(R.drawable.hike_avtar_protip);
		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder = getNotificationBuilder(context.getString(R.string.team_hike), message, message, avatarDrawable, smallIconId, false);
		setNotificationIntentForBuilder(mBuilder, notificationIntent);

		notificationManager.notify(FREE_SMS_POPUP_NOTIFICATION_ID, mBuilder.getNotification());

	}

	public void notifyMessage(final Protip proTip)
	{
		final SharedPreferences preferenceManager = PreferenceManager.getDefaultSharedPreferences(this.context);

		// we've got to invoke the timeline here
		final Intent notificationIntent = Utils.getTimelineActivityIntent(context);
		notificationIntent.putExtra(HikeConstants.Extras.NAME, context.getString(R.string.team_hike));

		notificationIntent.setData((Uri.parse("custom://" + PROTIP_NOTIFICATION_ID)));

		final Drawable avatarDrawable = context.getResources().getDrawable(R.drawable.hike_avtar_protip);
		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder = getNotificationBuilder(context.getString(R.string.team_hike), proTip.getHeader(), proTip.getHeader(), avatarDrawable, smallIconId,
				false);

		setNotificationIntentForBuilder(mBuilder, notificationIntent);

		if (!sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false))
		{
			notificationManager.notify(PROTIP_NOTIFICATION_ID, mBuilder.getNotification());
		}
	}

	/*
	 * method to send a notification of an hike update available or applicationspush update. if isApplicationsPushUpdate is false than it is hike app update.
	 */
	public void notifyUpdatePush(int updateType, String packageName, String message, boolean isApplicationsPushUpdate)
	{

		message = (TextUtils.isEmpty(message)) ? context.getString(R.string.update_app) : message;
		final Drawable avatarDrawable = context.getResources().getDrawable(R.drawable.hike_avtar_protip);
		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder = getNotificationBuilder(context.getString(R.string.team_hike), message, message, avatarDrawable, smallIconId, false);

		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("market://details?id=" + packageName));
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		mBuilder.setContentIntent(PendingIntent.getActivity(context, 0, intent, 0));

		if (!sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false))
		{
			int notificationId = isApplicationsPushUpdate ? GAMING_PACKET_NOTIFICATION_ID : APP_UPDATE_AVAILABLE_ID;
			notificationManager.notify(notificationId, mBuilder.getNotification());
		}
		// TODO:: we should reset the gaming download message from preferences
	}

	public void notifyMessage(final ContactInfo contactInfo, final ConvMessage convMsg, boolean isRich, Bitmap bigPictureImage)
	{
		boolean isPin = false;
		
		if(convMsg.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
			isPin = true;
		
		final String msisdn = convMsg.getMsisdn();
		// we are using the MSISDN now to group the notifications
		final int notificationId = msisdn.hashCode();

		String message = (!convMsg.isFileTransferMessage()) ? convMsg.getMessage() : HikeFileType.getFileTypeMessage(context, convMsg.getMetadata().getHikeFiles().get(0)
				.getHikeFileType(), convMsg.isSent());
		// Message will be empty for type 'uj' when the conversation does not
		// exist
		if (TextUtils.isEmpty(message)
				&& (convMsg.getParticipantInfoState() == ParticipantInfoState.USER_JOIN || convMsg.getParticipantInfoState() == ParticipantInfoState.CHAT_BACKGROUND))
		{
			if (convMsg.getParticipantInfoState() == ParticipantInfoState.USER_JOIN)
			{
				message = String.format(context.getString(convMsg.getMetadata().isOldUser() ? R.string.user_back_on_hike : R.string.joined_hike_new), contactInfo.getFirstName());
			}
			else
			{
				message = context.getString(R.string.chat_bg_changed, contactInfo.getFirstName());
			}
		}
		final long timestamp = convMsg.getTimestamp();

		String key = (contactInfo != null && !TextUtils.isEmpty(contactInfo.getName())) ? contactInfo.getName() : msisdn;

		// we've got to invoke the chat thread from here with the respective
		// users
		final Intent notificationIntent = new Intent(context, ChatThread.class);
		if (contactInfo.getName() != null)
		{
			notificationIntent.putExtra(HikeConstants.Extras.NAME, contactInfo.getName());
		}
		notificationIntent.putExtra(HikeConstants.Extras.MSISDN, contactInfo.getMsisdn());
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		/*
		 * notifications appear to be cached, and their .equals doesn't check 'Extra's. In order to prevent the wrong intent being fired, set a data field that's unique to the
		 * conversation we want to open. http://groups .google.com/group/android-developers/browse_thread/thread /e61ec1e8d88ea94d/1fe953564bd11609?#1fe953564bd11609
		 */

		notificationIntent.setData((Uri.parse("custom://" + notificationId)));

		notificationIntent.putExtra(HikeConstants.Extras.MSISDN, msisdn);
		if (contactInfo != null)
		{
			if (contactInfo.getName() != null)
			{
				notificationIntent.putExtra(HikeConstants.Extras.NAME, contactInfo.getName());
			}
		}
		final int icon = returnSmallIcon();

		/*
		 * Jellybean has added support for emojis so we don't need to add a '*' to replace them
		 */
		if (Build.VERSION.SDK_INT < 16)
		{
			// Replace emojis with a '*'
			message = SmileyParser.getInstance().replaceEmojiWithCharacter(message, "*");
		}

		String partName = "";
		// For showing the name of the contact that sent the message in a group
		// chat
		if (convMsg.isGroupChat() && !TextUtils.isEmpty(convMsg.getGroupParticipantMsisdn()) && convMsg.getParticipantInfoState() == ParticipantInfoState.NO_INFO)
		{

			GroupConversation gConv = (GroupConversation) convMsg.getConversation();

			ContactInfo participant = gConv.getGroupParticipant(convMsg.getGroupParticipantMsisdn()).getContactInfo();

			key = participant.getName();
			if (TextUtils.isEmpty(key))
			{
				key = participant.getMsisdn();
			}
			partName = key;
			if (isPin)
			{
				message = key +" "+ context.getString(R.string.pin_notif_text) + HikeConstants.SEPARATOR + message;
			}
			else
			{
				message = key + HikeConstants.SEPARATOR + message;
			}
			key = gConv.getLabel();
		}

		boolean doesBigPictureExist = (bigPictureImage == null) ? false : true;
		final String text = String.format("%1$s: %2$s", key, message);
		// For showing the name of the contact that sent the message in a group
		// chat

		if (doesBigPictureExist && isRich)
		{
			final String messageString = (!convMsg.isFileTransferMessage()) ? convMsg.getMessage() : HikeFileType.getFileTypeMessage(context, convMsg.getMetadata().getHikeFiles()
					.get(0).getHikeFileType(), convMsg.isSent());

			if (convMsg.isGroupChat())
			{
				message = partName + HikeConstants.SEPARATOR + messageString;
			}
			else
				message = messageString;
			// big picture messages ! intercept !
			showNotification(notificationIntent, icon, timestamp, notificationId, text, key, message, msisdn, bigPictureImage, !convMsg.isStickerMessage(), isPin);
		}
		else
		{
			// regular message
			showNotification(notificationIntent, icon, timestamp, notificationId, text, key, message, msisdn, null, isPin);
		}
	}
	
	public void notifyHikeToOfflinePush(ArrayList<String> msisdnList, HashMap<String, String> nameMap)
	{
		/*
		 * return straight away if the block notification setting is ON
		 */
		if (sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false))
		{
			return;
		}

		final int notificationId = HIKE_TO_OFFLINE_PUSH_NOTIFICATION_ID;
		final Intent notificationIntent = new Intent(context, ChatThread.class);
		
		String firstMsisdn = msisdnList.get(0);
		notificationIntent.putExtra(HikeConstants.Extras.MSISDN, (firstMsisdn));
		notificationIntent.putExtra(HikeConstants.Extras.NAME, (nameMap.get(firstMsisdn)));

		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		notificationIntent.setData((Uri.parse("custom://" + notificationId)));
		final Drawable avatarDrawable = context.getResources().getDrawable(R.drawable.offline_notification);
		final int smallIconId = returnSmallIcon();
		
		String title = (msisdnList.size() > 1)  ? context.getString(R.string.hike_to_offline_push_title_multiple, msisdnList.size()) : (HikeMessengerApp.isStealthMsisdn(firstMsisdn) ? context.getString(R.string.stealth_notification_message) : context.getString(R.string.hike_to_offline_push_title_single, nameMap.get(firstMsisdn)));
		String message = context.getString(R.string.hike_to_offline_text);
		NotificationCompat.Builder mBuilder = getNotificationBuilder(title, message, message, avatarDrawable, smallIconId, false);
		setNotificationIntentForBuilder(mBuilder, notificationIntent);

		notificationManager.notify(notificationId, mBuilder.getNotification());

	}

	public void notifyStealthMessage()
	{
		final int notificationId = STEALTH_NOTIFICATION_ID;

		String message = context.getString(R.string.stealth_notification_message);
		String key = "hike";

		String text = message;

		// we've got to invoke the timeline here
		final Intent notificationIntent = Utils.getHomeActivityIntent(context);
		notificationIntent.setData((Uri.parse("custom://" + notificationId)));

		final Drawable avatarDrawable = context.getResources().getDrawable(R.drawable.hike_avtar_protip);
		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder = getNotificationBuilder(key, message, text, avatarDrawable, smallIconId, false);

		setNotificationIntentForBuilder(mBuilder, notificationIntent);

		final boolean shouldNotPlayNotification = (System.currentTimeMillis() - lastNotificationTime) < MIN_TIME_BETWEEN_NOTIFICATIONS;
		if (!sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false))
		{
			notificationManager.notify(notificationId, mBuilder.getNotification());
			lastNotificationTime = shouldNotPlayNotification ? lastNotificationTime : System.currentTimeMillis();
		}
	}

	public void notifyFavorite(final ContactInfo contactInfo)
	{
		final int notificationId = contactInfo.getMsisdn().hashCode();

		final String msisdn = contactInfo.getMsisdn();

		final long timeStamp = System.currentTimeMillis() / 1000;

		final Intent notificationIntent = Utils.getPeopleActivityIntent(context);
		notificationIntent.setData((Uri.parse("custom://" + notificationId)));

		final int icon = returnSmallIcon();

		final String key = (contactInfo != null && !TextUtils.isEmpty(contactInfo.getName())) ? contactInfo.getName() : msisdn;

		final String message = context.getString(R.string.add_as_favorite_notification_line);

		final String text = context.getString(R.string.add_as_favorite_notification, key);

		showNotification(notificationIntent, icon, timeStamp, notificationId, text, key, message, msisdn, null, false);
		addNotificationId(notificationId);
	}

	public void notifyStatusMessage(final StatusMessage statusMessage)
	{
		/*
		 * We only proceed if the current status preference value is 0 which denotes that the user wants immediate notifications. Else we simply return
		 */
		if (PreferenceManager.getDefaultSharedPreferences(this.context).getInt(HikeConstants.STATUS_PREF, 0) != 0)
		{
			return;
		}
		final int notificationId = statusMessage.getMsisdn().hashCode();

		final long timeStamp = statusMessage.getTimeStamp();

		final Intent notificationIntent = Utils.getTimelineActivityIntent(context);
		notificationIntent.setData((Uri.parse("custom://" + notificationId)));

		final int icon = returnSmallIcon();

		final String key = statusMessage.getNotNullName();

		String message = null;
		String text = null;
		if (statusMessage.getStatusMessageType() == StatusMessageType.TEXT)
		{
			message = context.getString(R.string.status_text_notification, "\"" + statusMessage.getText() + "\"");
			/*
			 * Jellybean has added support for emojis so we don't need to add a '*' to replace them
			 */
			if (Build.VERSION.SDK_INT < 16)
			{
				// Replace emojis with a '*'
				message = SmileyParser.getInstance().replaceEmojiWithCharacter(message, "*");
			}
			text = key + " " + message;
		}
		else if (statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST_ACCEPTED)
		{
			message = context.getString(R.string.favorite_confirmed_notification, key);
			text = message;
		}
		else if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC)
		{
			message = context.getString(R.string.status_profile_pic_notification, key);
			text = key + " " + message;
		}
		else
		{
			/*
			 * We don't know how to display this type. Just return.
			 */
			return;
		}

		showNotification(notificationIntent, icon, timeStamp, notificationId, text, key, message, statusMessage.getMsisdn(), null, false);
		addNotificationId(notificationId);
	}

	public void notifyBigPictureStatusNotification(final String imagePath, final String msisdn, final String name)
	{

		if (PreferenceManager.getDefaultSharedPreferences(this.context).getInt(HikeConstants.STATUS_PREF, 0) != 0)
		{
			return;
		}

		final int notificationId = msisdn.hashCode();
		final String key = TextUtils.isEmpty(name) ? msisdn : name;
		final String message = context.getString(R.string.status_profile_pic_notification);
		final String text = key + " " + message;

		final int icon = returnSmallIcon();
		final Intent notificationIntent = Utils.getTimelineActivityIntent(context);
		final Bitmap bigPictureImage = HikeBitmapFactory.decodeBitmapFromFile(imagePath, Bitmap.Config.RGB_565);
		notificationIntent.setData((Uri.parse("custom://" + notificationId)));
		notificationIntent.putExtra(HikeConstants.Extras.MSISDN, msisdn.toString());

		showNotification(notificationIntent, icon, System.currentTimeMillis(), notificationId, text, key, message, msisdn, bigPictureImage, false);
	}

	public void notifyBatchUpdate(final String header, final String message)
	{
		final long timeStamp = System.currentTimeMillis() / 1000;

		final int notificationId = (int) timeStamp;

		final Intent notificationIntent = Utils.getTimelineActivityIntent(context);
		notificationIntent.setData((Uri.parse("custom://" + notificationId)));

		final int icon = returnSmallIcon();

		final String key = header;

		final String text = message;

		showNotification(notificationIntent, icon, timeStamp, notificationId, text, key, message, null, null, false); // TODO: change this.
		addNotificationId(notificationId);
	}

	private void addNotificationId(final int id)
	{
		String ids = sharedPreferences.getString(HikeMessengerApp.STATUS_IDS, "");

		ids += Integer.toString(id) + SEPERATOR;

		final Editor editor = sharedPreferences.edit();
		editor.putString(HikeMessengerApp.STATUS_IDS, ids);
		editor.commit();
	}

	public void cancelAllStatusNotifications()
	{
		final String ids = sharedPreferences.getString(HikeMessengerApp.STATUS_IDS, "");
		final String[] idArray = ids.split(SEPERATOR);

		for (final String id : idArray)
		{
			if (TextUtils.isEmpty(id.trim()))
			{
				continue;
			}
			notificationManager.cancel(Integer.parseInt(id));
		}

		final Editor editor = sharedPreferences.edit();
		editor.remove(HikeMessengerApp.STATUS_IDS);
		editor.commit();
	}

	public void cancelAllNotifications()
	{
		notificationManager.cancelAll();
	}

	private void showNotification(final Intent notificationIntent, final int icon, final long timestamp, final int notificationId, final CharSequence text, final String key,
			final String message, final String msisdn, final Bitmap bigPictureImage, boolean isFTMessage, boolean isPin)
	{

		final boolean shouldNotPlayNotification = (System.currentTimeMillis() - lastNotificationTime) < MIN_TIME_BETWEEN_NOTIFICATIONS;

		final Drawable avatarDrawable = Utils.getAvatarDrawableForNotificationOrShortcut(context, msisdn, isPin);

		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder;
		if (bigPictureImage != null)
		{
			mBuilder = getNotificationBuilder(key, message, text.toString(), avatarDrawable, smallIconId, isFTMessage);
			final NotificationCompat.BigPictureStyle bigPicStyle = new NotificationCompat.BigPictureStyle();
			bigPicStyle.setBigContentTitle(key);
			bigPicStyle.setSummaryText(message);
			mBuilder.setStyle(bigPicStyle);
			// set the big picture image
			bigPicStyle.bigPicture(bigPictureImage);
		}
		else
		{
			mBuilder = getNotificationBuilder(key, message, text.toString(), avatarDrawable, smallIconId, false);
		}

		setNotificationIntentForBuilder(mBuilder, notificationIntent);
		if (!sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false))
		{
			notificationManager.notify(notificationId, mBuilder.getNotification());
			lastNotificationTime = shouldNotPlayNotification ? lastNotificationTime : System.currentTimeMillis();
		}
	}

	private void showNotification(final Intent notificationIntent, final int icon, final long timestamp, final int notificationId, final CharSequence text, final String key,
			final String message, final String msisdn, final Bitmap bigPictureImage, boolean isPin)
	{
		showNotification(notificationIntent, icon, timestamp, notificationId, text, key, message, msisdn, bigPictureImage, false, isPin);
	}

	private int returnSmallIcon()
	{
		if (Build.VERSION.SDK_INT < 16)
		{
			return R.drawable.ic_contact_logo;

		}
		else
		{
			return R.drawable.ic_stat_notify;
		}

	}

	/*
	 * creates a notification builder with sound, led and vibrate option set according to app preferences. forceNotPlaySound : true if we want to force not to play notification
	 * sounds or lights.
	 */
	public NotificationCompat.Builder getNotificationBuilder(String contentTitle, String contentText, String tickerText, Drawable avatarDrawable, int smallIconId,
			boolean forceNotPlaySound)
	{

		final SharedPreferences preferenceManager = PreferenceManager.getDefaultSharedPreferences(this.context);

		String vibrate = preferenceManager.getString(HikeConstants.VIBRATE_PREF_LIST, VIB_DEF);
		final boolean led = preferenceManager.getBoolean(HikeConstants.LED_PREF, true);

		final Bitmap avatarBitmap = HikeBitmapFactory.returnScaledBitmap((HikeBitmapFactory.drawableToBitmap(avatarDrawable, Bitmap.Config.RGB_565)), context);

		final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context).setContentTitle(contentTitle).setSmallIcon(smallIconId).setLargeIcon(avatarBitmap)
				.setContentText(contentText).setAutoCancel(true).setTicker(tickerText).setPriority(Notification.PRIORITY_DEFAULT);

		if (!forceNotPlaySound)
		{
			final boolean shouldNotPlayNotification = (System.currentTimeMillis() - lastNotificationTime) < MIN_TIME_BETWEEN_NOTIFICATIONS;
			String notifSond = preferenceManager.getString(HikeConstants.NOTIF_SOUND_PREF, NOTIF_SOUND_HIKE);
			if (!shouldNotPlayNotification)
			{
				Logger.i("notif", "sound " + notifSond);
				if (!NOTIF_SOUND_OFF.equals(notifSond))
				{
					if (NOTIF_SOUND_HIKE.equals(notifSond))
					{
						mBuilder.setSound(Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.hike_jingle_15));
					}
					else
					{
						mBuilder.setDefaults(mBuilder.getNotification().defaults | Notification.DEFAULT_SOUND);
					}
				}

				if (!VIB_OFF.equals(vibrate))
				{
					if (VIB_DEF.equals(vibrate))
					{
						mBuilder.setDefaults(mBuilder.getNotification().defaults | Notification.DEFAULT_VIBRATE);
					}
					else if (VIB_SHORT.equals(vibrate))
					{
						// short vibrate
						mBuilder.setVibrate(HikeConstants.SHORT_VIB_PATTERN);
					}
					else if (VIB_LONG.equals(vibrate))
					{
						// long vibrate
						mBuilder.setVibrate(HikeConstants.LONG_VIB_PATTERN);
					}
				}
			}
			if (led)
			{
				mBuilder.setLights(Color.BLUE, HikeConstants.LED_LIGHTS_ON_MS, HikeConstants.LED_LIGHTS_OFF_MS);
			}
		}
		return mBuilder;
	}

	public void setNotificationIntentForBuilder(NotificationCompat.Builder mBuilder, Intent notificationIntent)
	{
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(contentIntent);
	}
}
