package com.bsb.hike.utils;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.Protip;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.service.HikeMqttManagerNew.MQTTConnectionStatus;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.utils.StickerManager.StickerCategoryId;

public class ToastListener implements Listener
{

	private WeakReference<Activity> currentActivity;

	private HikeNotification toaster;

	private HikeUserDatabase db;

	private Context context;

	private MQTTConnectionStatus mCurrentUnnotifiedStatus;

	String[] hikePubSubListeners = { HikePubSub.PUSH_AVATAR_DOWNLOADED, HikePubSub.PUSH_FILE_DOWNLOADED, HikePubSub.PUSH_STICKER_DOWNLOADED, HikePubSub.MESSAGE_RECEIVED,
			HikePubSub.NEW_ACTIVITY, HikePubSub.CONNECTION_STATUS, HikePubSub.FAVORITE_TOGGLED, HikePubSub.TIMELINE_UPDATE_RECIEVED, HikePubSub.BATCH_STATUS_UPDATE_PUSH_RECEIVED,
			HikePubSub.CANCEL_ALL_STATUS_NOTIFICATIONS, HikePubSub.CANCEL_ALL_NOTIFICATIONS, HikePubSub.PROTIP_ADDED, HikePubSub.UPDATE_PUSH, HikePubSub.APPLICATIONS_PUSH,
			HikePubSub.SHOW_FREE_INVITE_SMS };

	public ToastListener(Context context)
	{
		HikeMessengerApp.getPubSub().addListeners(this, hikePubSubListeners);
		this.toaster = new HikeNotification(context);
		this.db = HikeUserDatabase.getInstance();
		this.context = context;
		mCurrentUnnotifiedStatus = MQTTConnectionStatus.NOT_CONNECTED;
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.NEW_ACTIVITY.equals(type))
		{
			Activity activity = (Activity) object;
			if ((activity != null) && (mCurrentUnnotifiedStatus != MQTTConnectionStatus.NOT_CONNECTED))
			{
				notifyConnStatus(mCurrentUnnotifiedStatus);
				mCurrentUnnotifiedStatus = MQTTConnectionStatus.NOT_CONNECTED;
			}

			currentActivity = new WeakReference<Activity>(activity);
		}
		else if (HikePubSub.MESSAGE_RECEIVED.equals(type))
		{
			ConvMessage message = (ConvMessage) object;
			if (message.isShouldShowPush())
			{
				HikeConversationsDatabase hCDB = HikeConversationsDatabase.getInstance();
				message.setConversation(hCDB.getConversation(message.getMsisdn(), 0));

				if (message.getConversation() == null)
				{
					Logger.w(getClass().getSimpleName(), "The client did not get a GCJ message for us to handle this message.");
					return;
				}
				if ((message.getConversation() instanceof GroupConversation) && ((GroupConversation) message.getConversation()).isMuted())
				{
					Logger.d(getClass().getSimpleName(), "Group has been muted");
					return;
				}
				if (message.getParticipantInfoState() == ParticipantInfoState.NO_INFO || message.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_JOINED
						|| message.getParticipantInfoState() == ParticipantInfoState.USER_JOIN || message.getParticipantInfoState() == ParticipantInfoState.CHAT_BACKGROUND)
				{
					if (message.getParticipantInfoState() == ParticipantInfoState.CHAT_BACKGROUND)
					{
						boolean showNotification = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.CHAT_BG_NOTIFICATION_PREF, true);
						if (!showNotification)
						{
							return;
						}
					}

					Activity activity = (currentActivity != null) ? currentActivity.get() : null;
					if ((activity instanceof ChatThread))
					{
						String contactNumber = ((ChatThread) activity).getContactNumber();
						if (message.getMsisdn().equals(contactNumber))
						{
							return;
						}
					}

					/*
					 * the foreground activity isn't going to show this message so Toast it
					 */
					ContactInfo contactInfo;
					if (message.isGroupChat())
					{
						Logger.d("ToastListener", "GroupName is " + message.getConversation().getLabel());
						contactInfo = new ContactInfo(message.getMsisdn(), message.getMsisdn(), message.getConversation().getLabel(), message.getMsisdn());
					}
					else
					{
						contactInfo = this.db.getContactInfoFromMSISDN(message.getMsisdn(), false);
					}

					if(message.getConversation().isStealth())
					{
						this.toaster.notifyStealthMessage();
					}
					else
					{
						/*
						 * Check if this is a big picture message, else toast a normal push message
						 */
						Bitmap bigPicture = returnBigPicture(message, context);
						this.toaster.notifyMessage(contactInfo, message, bigPicture != null ? true : false, bigPicture);
					}
				}

			}
		}
		else if (HikePubSub.CONNECTION_STATUS.equals(type))
		{
			HikeMqttManagerNew.MQTTConnectionStatus status = (HikeMqttManagerNew.MQTTConnectionStatus) object;
			mCurrentUnnotifiedStatus = status;
			notifyConnStatus(status);
		}
		else if (HikePubSub.FAVORITE_TOGGLED.equals(type))
		{
			final Pair<ContactInfo, FavoriteType> favoriteToggle = (Pair<ContactInfo, FavoriteType>) object;

			ContactInfo contactInfo = favoriteToggle.first;
			FavoriteType favoriteType = favoriteToggle.second;

			/*
			 * Only notify when someone has added the user as a favorite.
			 */
			if (favoriteType != FavoriteType.REQUEST_RECEIVED)
			{
				return;
			}
			Activity activity = (currentActivity != null) ? currentActivity.get() : null;
			if(HikeMessengerApp.isStealthMsisdn(contactInfo.getMsisdn()))
			{
				this.toaster.notifyStealthMessage();
			}
			else
			{
				toaster.notifyFavorite(contactInfo);
			}
		}
		else if (HikePubSub.TIMELINE_UPDATE_RECIEVED.equals(type))
		{
			if (currentActivity != null && currentActivity.get() != null)
			{
				return;
			}
			StatusMessage statusMessage = (StatusMessage) object;
			String msisdn = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.MSISDN_SETTING, "");
			if (msisdn.equals(statusMessage.getMsisdn()))
			{
				return;
			}
			toaster.notifyStatusMessage(statusMessage);
		}
		else if (HikePubSub.BATCH_STATUS_UPDATE_PUSH_RECEIVED.equals(type))
		{
			if (currentActivity != null && currentActivity.get() != null)
			{
				return;
			}
			Pair<String, String> batchSU = (Pair<String, String>) object;
			toaster.notifyBatchUpdate(batchSU.first, batchSU.second);
		}
		else if (HikePubSub.CANCEL_ALL_STATUS_NOTIFICATIONS.equals(type))
		{
			toaster.cancelAllStatusNotifications();
		}
		else if (HikePubSub.PUSH_AVATAR_DOWNLOADED.equals(type))
		{
			if (currentActivity != null && currentActivity.get() != null)
			{
				return;
			}
			/*
			 * this object contains a Bundle containing 3 strings among which one is imagepath of downloaded avtar. and other two are msisdn and name from which notification has
			 * come.
			 */
			Bundle notifyBundle = (Bundle) object;
			toaster.notifyBigPictureStatusNotification(notifyBundle.getString(HikeConstants.Extras.IMAGE_PATH), notifyBundle.getString(HikeConstants.Extras.MSISDN),
					notifyBundle.getString(HikeConstants.Extras.NAME));
		}
		else if (HikePubSub.PUSH_FILE_DOWNLOADED.equals(type) || HikePubSub.PUSH_STICKER_DOWNLOADED.equals(type))
		{
			if (object == null)
				return;
			ConvMessage message = (ConvMessage) object;
			if (currentActivity != null && currentActivity.get() != null)
			{
				return;
			}

			if (!message.isShouldShowPush())
			{
				return;
			}

			if ((message.getConversation() instanceof GroupConversation) && ((GroupConversation) message.getConversation()).isMuted())
			{
				Logger.d(getClass().getSimpleName(), "Group has been muted");
				return;
			}
			if(message.getConversation().isStealth())
			{
				Logger.d(getClass().getSimpleName(), "this conversation is stealth");
				return;
			}
			final Bitmap bigPicture = returnBigPicture(message, context);
			if (bigPicture != null)
			{
				ContactInfo contactInfo;
				if (message.isGroupChat())
				{
					Logger.d("ToastListener", "GroupName is " + message.getConversation().getLabel());
					contactInfo = new ContactInfo(message.getMsisdn(), message.getMsisdn(), message.getConversation().getLabel(), message.getMsisdn());
				}
				else
				{
					contactInfo = this.db.getContactInfoFromMSISDN(message.getMsisdn(), false);
				}
				toaster.notifyMessage(contactInfo, message, true, bigPicture);
			}
		}
		else if (HikePubSub.CANCEL_ALL_NOTIFICATIONS.equals(type))
		{
			toaster.cancelAllNotifications();
		}
		else if (HikePubSub.PROTIP_ADDED.equals(type))
		{
			Protip proTip = (Protip) object;
			if (currentActivity != null && currentActivity.get() != null)
			{
				return;
			}
			// the only check we now need is to check whether the pro tip has to
			// push flag true or not
			if (proTip.isShowPush())
				toaster.notifyMessage(proTip);
		}
		else if (HikePubSub.UPDATE_PUSH.equals(type))
		{
			int update = ((Integer) object).intValue();
			// future todo: possibly handle the case where the alert has been
			// shown in
			// the app once for the update and
			// now the user has got a push update from our server.
			// if its critical, let it go through, if its normal, check the
			// preference.
			toaster.notifyUpdatePush(update, context.getPackageName(),
					context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeConstants.Extras.UPDATE_MESSAGE, ""), false);
		}
		else if (HikePubSub.APPLICATIONS_PUSH.equals(type))
		{
			if (object instanceof String)
			{
				String packageName = ((String) object);
				toaster.notifyUpdatePush(-1, packageName,
						context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeConstants.Extras.APPLICATIONSPUSH_MESSAGE, ""), true);
			}

		}
		else if (HikePubSub.SHOW_FREE_INVITE_SMS.equals(type))
		{
			if (object != null && object instanceof Bundle)
			{
				Bundle bundle = (Bundle) object;
				String bodyString = bundle.getString(HikeConstants.Extras.FREE_SMS_POPUP_BODY);
				// TODO: we may need the title tomorrow, so we can extract that
				// too from the bundle
				if (!TextUtils.isEmpty(bodyString))
				{
					toaster.notifySMSPopup(bodyString);
				}
			}
		}
	}

	private Bitmap returnBigPicture(ConvMessage convMessage, Context context)
	{

		HikeFile hikeFile = null;
		Bitmap bigPictureImage = null;

		// Check if this is a file transfer message of image type
		// construct a bitmap only if the big picture condition matches
		if (convMessage.isFileTransferMessage())
		{
			hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			if (hikeFile != null)
			{
				if (hikeFile.getHikeFileType() == HikeFileType.IMAGE && hikeFile.wasFileDownloaded() && hikeFile.getThumbnail() != null)
				{
					final String filePath = hikeFile.getFilePath(); // check
					bigPictureImage = HikeBitmapFactory.decodeFile(filePath);
				}
			}

		}
		// check if this is a sticker message and find if its non-downloaded or
		// non present.
		if (convMessage.isStickerMessage())
		{
			final Sticker sticker = convMessage.getMetadata().getSticker();
			/*
			 * If this is the first category, then the sticker are a part of the app bundle itself
			 */
			if (sticker.isDefaultSticker())
			{
				int resourceId = 0;

				if (StickerCategoryId.humanoid.equals(sticker.getCategory().categoryId))
				{
					resourceId = StickerManager.getInstance().LOCAL_STICKER_RES_IDS_HUMANOID[sticker.getStickerIndex()];
				}
				else if (StickerCategoryId.expressions.equals(sticker.getCategory().categoryId))
				{
					resourceId = StickerManager.getInstance().LOCAL_STICKER_RES_IDS_EXPRESSIONS[sticker.getStickerIndex()];
				}

				if (resourceId > 0)
				{
					final Drawable dr = context.getResources().getDrawable(resourceId);
					bigPictureImage = HikeBitmapFactory.drawableToBitmap(dr);
				}

			}
			else
			{
				final String filePath = sticker.getStickerPath(context);
				if (!TextUtils.isEmpty(filePath))
				{
					bigPictureImage = HikeBitmapFactory.decodeFile(filePath);
				}
			}
		}
		return bigPictureImage;
	}
	
	private void notifyConnStatus(MQTTConnectionStatus status)
	{
		/* only show the trying to connect message after we've connected once */
		SharedPreferences settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		boolean connectedOnce = settings.getBoolean(HikeMessengerApp.CONNECTED_ONCE, false);
		if (status == HikeMqttManagerNew.MQTTConnectionStatus.CONNECTED)
		{
			NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.cancel(HikeConstants.HIKE_SYSTEM_NOTIFICATION);
			if (!connectedOnce)
			{
				Editor editor = settings.edit();
				editor.putBoolean(HikeMessengerApp.CONNECTED_ONCE, true);
				editor.commit();
			}
			return;
		}

	}

}
