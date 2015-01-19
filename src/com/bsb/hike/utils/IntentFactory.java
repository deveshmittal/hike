package com.bsb.hike.utils;

import java.io.File;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.ui.ComposeChatActivity;
import com.bsb.hike.ui.CreditsActivity;
import com.bsb.hike.ui.FileSelectActivity;
import com.bsb.hike.ui.GalleryActivity;
import com.bsb.hike.ui.HikeListActivity;
import com.bsb.hike.ui.HikePreferences;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.SettingsActivity;
import com.bsb.hike.ui.ShareLocation;
import com.bsb.hike.ui.TimelineActivity;
import com.bsb.hike.ui.WebViewActivity;

public class IntentFactory {
	public static void openSetting(Context context) {
		context.startActivity(new Intent(context, SettingsActivity.class));
	}

	public static void openSettingNotification(Context context) {
		Intent intent = new Intent(context, HikePreferences.class);
		intent.putExtra(HikeConstants.Extras.PREF,
				R.xml.notification_preferences);
		intent.putExtra(HikeConstants.Extras.TITLE, R.string.notifications);
		context.startActivity(intent);
	}

	public static void openSettingPrivacy(Context context) {
		context.startActivity(Utils.getIntentForPrivacyScreen(context));
	}

	public static void openSettingMedia(Context context) {
		Intent intent = new Intent(context, HikePreferences.class);
		intent.putExtra(HikeConstants.Extras.PREF,
				R.xml.media_download_preferences);
		intent.putExtra(HikeConstants.Extras.TITLE, R.string.settings_media);
		context.startActivity(intent);
	}

	public static void openSettingSMS(Context context) {
		context.startActivity(new Intent(context, CreditsActivity.class));
	}

	public static void openSettingAccount(Context context) {
		Intent intent = new Intent(context, HikePreferences.class);
		intent.putExtra(HikeConstants.Extras.PREF, R.xml.account_preferences);
		intent.putExtra(HikeConstants.Extras.TITLE, R.string.account);
		context.startActivity(intent);
	}

	public static void openSettingHelp(Context context) {
		Intent intent = new Intent(context, HikePreferences.class);
		intent.putExtra(HikeConstants.Extras.PREF, R.xml.help_preferences);
		intent.putExtra(HikeConstants.Extras.TITLE, R.string.help);
		context.startActivity(intent);
	}

	public static void openInviteSMS(Context context) {
		context.startActivity(new Intent(context, HikeListActivity.class));
	}

	public static void openInviteWatsApp(Context context) {
		Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
		whatsappIntent.setType("text/plain");
		whatsappIntent.setPackage(HikeConstants.PACKAGE_WATSAPP);
		String inviteText = HikeSharedPreferenceUtil.getInstance(context)
				.getData(HikeConstants.WATSAPP_INVITE_MESSAGE_KEY,
						context.getString(R.string.watsapp_invitation));
		String inviteToken = context.getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(
				HikeConstants.INVITE_TOKEN, "");
		inviteText = inviteText + inviteToken;
		whatsappIntent.putExtra(Intent.EXTRA_TEXT, inviteText);
		try {
			context.startActivity(whatsappIntent);
		} catch (android.content.ActivityNotFoundException ex) {
			Toast.makeText(context.getApplicationContext(),
					"Could not find WatsApp in System", Toast.LENGTH_SHORT)
					.show();
		}
	}

	public static void openTimeLine(Context context) {
		context.startActivity(new Intent(context, TimelineActivity.class));
	}

	public static void openHikeExtras(Context context) {
		context.startActivity(getGamingIntent(context));
	}

	public static Intent getGamingIntent(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		Intent intent = new Intent(context.getApplicationContext(),
				WebViewActivity.class);
		String hikeExtrasUrl = prefs.getString(HikeConstants.HIKE_EXTRAS_URL,
				AccountUtils.gamesUrl);

		if (!TextUtils.isEmpty(hikeExtrasUrl)) {
			if (Utils.switchSSLOn(context)) {
				intent.putExtra(
						HikeConstants.Extras.URL_TO_LOAD,
						AccountUtils.HTTPS_STRING
								+ hikeExtrasUrl
								+ HikeConstants.ANDROID
								+ "/"
								+ prefs.getString(
										HikeMessengerApp.REWARDS_TOKEN, ""));
			} else {
				intent.putExtra(
						HikeConstants.Extras.URL_TO_LOAD,
						AccountUtils.HTTP_STRING
								+ hikeExtrasUrl
								+ HikeConstants.ANDROID
								+ "/"
								+ prefs.getString(
										HikeMessengerApp.REWARDS_TOKEN, ""));
			}
		}

		String hikeExtrasName = prefs.getString(HikeConstants.HIKE_EXTRAS_NAME,
				context.getString(R.string.hike_extras));

		if (!TextUtils.isEmpty(hikeExtrasName)) {
			intent.putExtra(HikeConstants.Extras.TITLE, hikeExtrasName);
		}

		return intent;
	}

	public static void openHikeRewards(Context context) {
		context.startActivity(getRewardsIntent(context));
	}

	public static Intent getRewardsIntent(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		Intent intent = new Intent(context.getApplicationContext(),
				WebViewActivity.class);
		String rewards_url = prefs.getString(HikeConstants.REWARDS_URL,
				AccountUtils.rewardsUrl);

		if (!TextUtils.isEmpty(rewards_url)) {
			if (Utils.switchSSLOn(context)) {
				intent.putExtra(
						HikeConstants.Extras.URL_TO_LOAD,
						AccountUtils.HTTPS_STRING
								+ rewards_url
								+ HikeConstants.ANDROID
								+ "/"
								+ prefs.getString(
										HikeMessengerApp.REWARDS_TOKEN, ""));
			} else {
				intent.putExtra(
						HikeConstants.Extras.URL_TO_LOAD,
						AccountUtils.HTTP_STRING
								+ rewards_url
								+ HikeConstants.ANDROID
								+ "/"
								+ prefs.getString(
										HikeMessengerApp.REWARDS_TOKEN, ""));
			}
		}

		String rewards_name = prefs.getString(HikeConstants.REWARDS_NAME,
				context.getString(R.string.rewards));

		if (!TextUtils.isEmpty(rewards_name)) {
			intent.putExtra(HikeConstants.Extras.TITLE, rewards_name);
		}
		intent.putExtra(HikeConstants.Extras.WEBVIEW_ALLOW_LOCATION, true);

		return intent;
	}

	public static Intent getForwardStickerIntent(Context context,
			String stickerId, String categoryId) {
		Utils.sendUILogEvent(HikeConstants.LogEvent.FORWARD_MSG);
		Intent intent = new Intent(context, ComposeChatActivity.class);
		intent.putExtra(HikeConstants.Extras.FORWARD_MESSAGE, true);
		JSONArray multipleMsgArray = new JSONArray();
		try {
			JSONObject multiMsgFwdObject = new JSONObject();
			multiMsgFwdObject
					.putOpt(StickerManager.FWD_CATEGORY_ID, categoryId);
			multiMsgFwdObject.putOpt(StickerManager.FWD_STICKER_ID, stickerId);
			multipleMsgArray.put(multiMsgFwdObject);
		} catch (JSONException e) {
			Logger.e(context.getClass().getSimpleName(), "Invalid JSON", e);
		}

		intent.putExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT,
				multipleMsgArray.toString());
		return intent;
	}

	public static Intent getImageCaptureIntent(Context context) {
		/*
		 * Cannot send a file because of an android issue
		 * http://stackoverflow.com/questions/10494839 /verifiyandsetparameter
		 * -error-when-trying-to-record-video
		 */
		File selectedFile = Utils.getOutputMediaFile(HikeFileType.IMAGE, null,
				true);
		if (selectedFile == null) {
			Toast.makeText(
					context,
					context.getResources().getString(
							R.string.no_external_storage), Toast.LENGTH_LONG)
					.show();
			return null;
		}
		Intent pickIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		pickIntent
				.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(selectedFile));
		/*
		 * For images, save the file path as a preferences since in some devices
		 * the reference to the file becomes null.
		 */
		HikeSharedPreferenceUtil.getInstance(context).saveData(
				HikeMessengerApp.FILE_PATH, selectedFile.getAbsolutePath());
		return pickIntent;
	}

	public static Intent getVideoRecordingIntent() {
		Intent newMediaFileIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		newMediaFileIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT,
				(long) (0.9 * HikeConstants.MAX_FILE_SIZE));
		Intent pickVideo = new Intent(Intent.ACTION_PICK).setType("video/*");
		return Intent.createChooser(pickVideo, "").putExtra(
				Intent.EXTRA_INITIAL_INTENTS,
				new Intent[] { newMediaFileIntent });
	}

	public static Intent getLocationPickerIntent(Context context) {
		return new Intent(context, ShareLocation.class);
	}

	public static Intent getContactPickerIntent() {
		return new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
	}

	public static Intent getHileGallaryShare(Context context, String msisdn,
			boolean onHike) {
		Intent imageIntent = new Intent(context, GalleryActivity.class);
		imageIntent.putExtra(HikeConstants.Extras.MSISDN, msisdn);
		imageIntent.putExtra(HikeConstants.Extras.ON_HIKE, onHike);
		return imageIntent;
	}

	public static Intent getAudioShareIntent(Context context) {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("audio/*");
		return intent;
	}

	public static Intent getFileSelectActivityIntent(Context context) {
		return new Intent(context, FileSelectActivity.class);
	}
	
	/**
	 * Returns intent for viewing a user's profile screen
	 * 
	 * @param context
	 * @param isConvOnHike
	 * @param mMsisdn
	 * @return
	 */
	public static Intent getSingleProfileIntent(Context context, boolean isConvOnHike, String mMsisdn)
	{
		Intent intent = new Intent();
		
		intent.setClass(context, ProfileActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		/**
		 * Negation of is self chat true
		 */
		if(!(HikeSharedPreferenceUtil.getInstance(context).getData(HikeMessengerApp.MSISDN_SETTING, "").equals(mMsisdn)))
		{
			intent.putExtra(HikeConstants.Extras.CONTACT_INFO, mMsisdn);
			intent.putExtra(HikeConstants.Extras.ON_HIKE, isConvOnHike);
		}
		
		return intent;
	}
	
	/**
	 * Returns intent for viewing group profile screen
	 * 
	 * @param context
	 * @param mMsisdn
	 * @return
	 */
	
	public static Intent getGroupProfileIntent(Context context, String mMsisdn)
	{
		Intent intent = new Intent();
		
		intent.setClass(context, ProfileActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		intent.putExtra(HikeConstants.Extras.GROUP_CHAT, true);
		intent.putExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT, mMsisdn);
		
		return intent;
	}
	
	/**
	 * Used for retrieving the intent to place a call
	 * 
	 * @param mMsisdn
	 * @return
	 */
	public static Intent getCallIntent(String mMsisdn)
	{
		Intent callIntent = new Intent(Intent.ACTION_CALL);
		callIntent.setData(Uri.parse("tel:" + mMsisdn));
		return callIntent;
	}

}
