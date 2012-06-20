package com.bsb.hike.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.ui.ChatThread;

public class HikeNotification
{
	public static final int HIKE_NOTIFICATION = 0;

	private Context context;

	private NotificationManager notificationManager;

	public HikeNotification(Context context)
	{
		this.context = context;
		this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	public void notify(ContactInfo contactInfo,ConvMessage convMsg)
	{
		SharedPreferences preferenceManager = PreferenceManager.getDefaultSharedPreferences(this.context);
		int playSound = preferenceManager.getBoolean(HikeConstants.SOUND_PREF, true) ? Notification.DEFAULT_SOUND : 0;
		int vibrate = preferenceManager.getBoolean(HikeConstants.VIBRATE_PREF, true) ? Notification.DEFAULT_VIBRATE : 0;

		String msisdn = convMsg.getMsisdn();
		String message = convMsg.getMessage();
		long timestamp = convMsg.getTimestamp();
		String key = (contactInfo != null && !TextUtils.isEmpty(contactInfo.getName())) ? contactInfo.getName() : msisdn;

		// For showing the name of the contact that sent the message in a group chat
		if(convMsg.isGroupChat() && !TextUtils.isEmpty(convMsg.getGroupParticipantMsisdn()) && convMsg.getParticipantInfoState() == ParticipantInfoState.NO_INFO)
		{
			HikeUserDatabase hUDB = new HikeUserDatabase(context);
			ContactInfo participant = hUDB.getContactInfoFromMSISDN(convMsg.getGroupParticipantMsisdn());
			hUDB.close();
			if(TextUtils.isEmpty(participant.getName()))
			{
				HikeConversationsDatabase hCDB = new HikeConversationsDatabase(context);
				participant.setName(hCDB.getParticipantName(msisdn, convMsg.getGroupParticipantMsisdn()));
				hCDB.close();
			}
			message = participant.getFirstName() + HikeConstants.SEPARATOR + message;
		}

		int icon = R.drawable.ic_contact_logo;

		// TODO this doesn't turn the text bold :(
		Spanned text = Html.fromHtml(String.format("<bold>%1$s</bold>:%2$s", key, message));
		Notification notification = new Notification(icon, text, timestamp * 1000);

		notification.flags = notification.flags | Notification.FLAG_AUTO_CANCEL;

		notification.defaults |= playSound | vibrate;

		int notificationId = (int)convMsg.getConversation().getConvId();
		Intent notificationIntent = new Intent(context, ChatThread.class);

		/* notifications appear to be cached, and their .equals doesn't check 'Extra's.
		 * In order to prevent the wrong intent being fired, set a data field that's unique to the
		 * conversation we want to open.
		 * http://groups.google.com/group/android-developers/browse_thread/thread/e61ec1e8d88ea94d/1fe953564bd11609?#1fe953564bd11609
		 */

		notificationIntent.setData((Uri.parse("custom://"+notificationId)));

		notificationIntent.putExtra(HikeConstants.Extras.MSISDN, msisdn);
		if (contactInfo != null)
		{
			if (contactInfo.getId() != null)
			{
				notificationIntent.putExtra(HikeConstants.Extras.ID, contactInfo.getId());
			}
			if (contactInfo.getName() != null)
			{
				notificationIntent.putExtra(HikeConstants.Extras.NAME, contactInfo.getName());
			}
		}

		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, key, message, contentIntent);

		Log.d("HikeNotification","CONVERSATION ID : " + notificationId);
		notificationManager.notify(notificationId, notification);
	}
}
