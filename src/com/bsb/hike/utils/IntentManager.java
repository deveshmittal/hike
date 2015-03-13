package com.bsb.hike.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.models.NuxCustomMessage;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.ComposeChatActivity;
import com.bsb.hike.ui.ConnectedAppsActivity;
import com.bsb.hike.ui.CreateNewGroupOrBroadcastActivity;
import com.bsb.hike.ui.CreditsActivity;
import com.bsb.hike.ui.GalleryActivity;
import com.bsb.hike.ui.FtueBroadcast;
import com.bsb.hike.ui.HikeAuthActivity;
import com.bsb.hike.ui.HikeCameraActivity;
import com.bsb.hike.ui.HikeListActivity;
import com.bsb.hike.ui.HikePreferences;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.NUXInviteActivity;
import com.bsb.hike.ui.NuxSendCustomMessageActivity;
import com.bsb.hike.ui.PeopleActivity;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.SettingsActivity;
import com.bsb.hike.ui.SignupActivity;
import com.bsb.hike.ui.StickerSettingsActivity;
import com.bsb.hike.ui.StickerShopActivity;
import com.bsb.hike.ui.TimelineActivity;
import com.bsb.hike.ui.WebViewActivity;
import com.bsb.hike.ui.WelcomeActivity;
import com.bsb.hike.voip.VoIPConstants;
import com.bsb.hike.voip.VoIPService;
import com.bsb.hike.voip.VoIPUtils;
import com.bsb.hike.voip.view.CallRateActivity;
import com.bsb.hike.voip.view.VoIPActivity;
import com.google.android.gms.internal.co;

public class IntentManager
{
	public static void openSetting(Context context)
	{
		context.startActivity(new Intent(context, SettingsActivity.class));
	}

	public static void openSettingNotification(Context context)
	{
		Intent intent = new Intent(context, HikePreferences.class);
		intent.putExtra(HikeConstants.Extras.PREF, R.xml.notification_preferences);
		intent.putExtra(HikeConstants.Extras.TITLE, R.string.notifications);
		context.startActivity(intent);
	}

	public static void openSettingPrivacy(Context context)
	{
		context.startActivity(Utils.getIntentForPrivacyScreen(context));
	}

	public static void openSettingMedia(Context context)
	{
		Intent intent = new Intent(context, HikePreferences.class);
		intent.putExtra(HikeConstants.Extras.PREF, R.xml.media_download_preferences);
		intent.putExtra(HikeConstants.Extras.TITLE, R.string.settings_media);
		context.startActivity(intent);
	}

	public static void openSettingSMS(Context context)
	{
		context.startActivity(new Intent(context, CreditsActivity.class));
	}

	public static void openSettingAccount(Context context)
	{
		Intent intent = new Intent(context, HikePreferences.class);
		intent.putExtra(HikeConstants.Extras.PREF, R.xml.account_preferences);
		intent.putExtra(HikeConstants.Extras.TITLE, R.string.account);
		context.startActivity(intent);
	}

	public static void openSettingHelp(Context context)
	{
		Intent intent = new Intent(context, HikePreferences.class);
		intent.putExtra(HikeConstants.Extras.PREF, R.xml.help_preferences);
		intent.putExtra(HikeConstants.Extras.TITLE, R.string.help);
		context.startActivity(intent);
	}

	public static void openInviteSMS(Context context)
	{
		context.startActivity(new Intent(context, HikeListActivity.class));
	}

	public static void openInviteWatsApp(Context context)
	{
		Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
		whatsappIntent.setType("text/plain");
		whatsappIntent.setPackage(HikeConstants.PACKAGE_WATSAPP);
		String inviteText = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.WATSAPP_INVITE_MESSAGE_KEY, context.getString(R.string.watsapp_invitation));
		String inviteToken = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeConstants.INVITE_TOKEN, "");
		inviteText = inviteText + inviteToken;
		whatsappIntent.putExtra(Intent.EXTRA_TEXT, inviteText);
		try
		{
			context.startActivity(whatsappIntent);
		}
		catch (android.content.ActivityNotFoundException ex)
		{
			Toast.makeText(context.getApplicationContext(), "Could not find WhatsApp in System", Toast.LENGTH_SHORT).show();
		}
	}

	public static void openTimeLine(Context context)
	{
		context.startActivity(new Intent(context, TimelineActivity.class));
	}

	public static void openHikeExtras(Context context)
	{
		context.startActivity(getGamingIntent(context));
	}

	public static Intent getGamingIntent(Context context)
	{
		SharedPreferences prefs = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		Intent intent = new Intent(context.getApplicationContext(), WebViewActivity.class);
		String hikeExtrasUrl = prefs.getString(HikeConstants.HIKE_EXTRAS_URL, AccountUtils.gamesUrl);

		if (!TextUtils.isEmpty(hikeExtrasUrl))
		{
			if (Utils.switchSSLOn(context))
			{
				intent.putExtra(HikeConstants.Extras.URL_TO_LOAD,
						AccountUtils.HTTPS_STRING + hikeExtrasUrl + HikeConstants.ANDROID + "/" + prefs.getString(HikeMessengerApp.REWARDS_TOKEN, ""));
			}
			else
			{
				intent.putExtra(HikeConstants.Extras.URL_TO_LOAD,
						AccountUtils.HTTP_STRING + hikeExtrasUrl + HikeConstants.ANDROID + "/" + prefs.getString(HikeMessengerApp.REWARDS_TOKEN, ""));
			}
		}

		String hikeExtrasName = prefs.getString(HikeConstants.HIKE_EXTRAS_NAME, context.getString(R.string.hike_extras));

		if (!TextUtils.isEmpty(hikeExtrasName))
		{
			intent.putExtra(HikeConstants.Extras.TITLE, hikeExtrasName);
		}

		return intent;
	}

	public static void openHikeRewards(Context context)
	{
		context.startActivity(getRewardsIntent(context));
	}

	public static Intent getRewardsIntent(Context context)
	{
		SharedPreferences prefs = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		Intent intent = new Intent(context.getApplicationContext(), WebViewActivity.class);
		String rewards_url = prefs.getString(HikeConstants.REWARDS_URL, AccountUtils.rewardsUrl);

		if (!TextUtils.isEmpty(rewards_url))
		{
			if (Utils.switchSSLOn(context))
			{
				intent.putExtra(HikeConstants.Extras.URL_TO_LOAD,
						AccountUtils.HTTPS_STRING + rewards_url + HikeConstants.ANDROID + "/" + prefs.getString(HikeMessengerApp.REWARDS_TOKEN, ""));
			}
			else
			{
				intent.putExtra(HikeConstants.Extras.URL_TO_LOAD,
						AccountUtils.HTTP_STRING + rewards_url + HikeConstants.ANDROID + "/" + prefs.getString(HikeMessengerApp.REWARDS_TOKEN, ""));
			}
		}

		String rewards_name = prefs.getString(HikeConstants.REWARDS_NAME, context.getString(R.string.rewards));

		if (!TextUtils.isEmpty(rewards_name))
		{
			intent.putExtra(HikeConstants.Extras.TITLE, rewards_name);
		}
		intent.putExtra(HikeConstants.Extras.WEBVIEW_ALLOW_LOCATION, true);

		return intent;
	}

	public static Intent getWebViewActivityIntent(Context context, String url, String title)
	{

		Intent intent = new Intent(context.getApplicationContext(), WebViewActivity.class);
		intent.putExtra(HikeConstants.Extras.URL_TO_LOAD, url);

		if (!TextUtils.isEmpty(title))
		{
			intent.putExtra(HikeConstants.Extras.TITLE, title);
		}
		intent.putExtra(HikeConstants.Extras.WEBVIEW_ALLOW_LOCATION, true);

		return intent;

	}
	
	public static void createNewBroadcastActivityIntent(Context appContext, List<String> selectedContactList)
	{
		Intent intent = new Intent(appContext.getApplicationContext(), CreateNewGroupOrBroadcastActivity.class);
		intent.putStringArrayListExtra(HikeConstants.Extras.BROADCAST_RECIPIENTS, (ArrayList<String>)selectedContactList);
		intent.putExtra(HikeConstants.IS_BROADCAST, true);
		appContext.startActivity(intent);
	}

	public static void createBroadcastFtue(Context appContext)
	{
		Intent intent = new Intent(appContext.getApplicationContext(), FtueBroadcast.class);
		intent.putExtra(HikeConstants.Extras.COMPOSE_MODE, HikeConstants.Extras.CREATE_BROADCAST_MODE);
		intent.putExtra(HikeConstants.Extras.CREATE_BROADCAST, true);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		appContext.startActivity(intent);
	}
	
	public static void createBroadcastDefault(Context appContext)
	{
		Intent intent = new Intent(appContext.getApplicationContext(), ComposeChatActivity.class);
		intent.putExtra(HikeConstants.Extras.COMPOSE_MODE, HikeConstants.Extras.CREATE_BROADCAST_MODE);
		intent.putExtra(HikeConstants.Extras.CREATE_BROADCAST, true);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		appContext.startActivity(intent);
	}
	
	public static void onBackPressedCreateNewBroadcast(Context appContext, ArrayList<String> broadcastRecipients)
	{
		Intent intent = new Intent(appContext.getApplicationContext(), ComposeChatActivity.class);
		intent.putStringArrayListExtra(HikeConstants.Extras.BROADCAST_RECIPIENTS, broadcastRecipients);
		intent.putExtra(HikeConstants.Extras.COMPOSE_MODE, HikeConstants.Extras.CREATE_BROADCAST_MODE);
		intent.putExtra(HikeConstants.Extras.CREATE_BROADCAST, true);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		appContext.startActivity(intent);
	}
	
	public static Intent getForwardIntentForConvMessage(Context context, ConvMessage convMessage, String metadata)
	{
		Intent intent = new Intent(context, ComposeChatActivity.class);
		intent.putExtra(HikeConstants.Extras.FORWARD_MESSAGE, true);
		JSONArray multipleMsgArray = new JSONArray();
		JSONObject multiMsgFwdObject = new JSONObject();
		try
		{
			multiMsgFwdObject.put(HikeConstants.MESSAGE_TYPE.MESSAGE_TYPE, convMessage.getMessageType());
			if (metadata != null)
			{
				multiMsgFwdObject.put(HikeConstants.METADATA, metadata);
			}
			multiMsgFwdObject.put(HikeConstants.HIKE_MESSAGE, convMessage.getMessage());
			multipleMsgArray.put(multiMsgFwdObject);
		}
		catch (JSONException e)
		{
			Logger.e(context.getClass().getSimpleName(), "Invalid JSON", e);
		}
		intent.putExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT, multipleMsgArray.toString());
		intent.putExtra(HikeConstants.Extras.PREV_MSISDN, convMessage.getMsisdn());

		return intent;
	}

	public static Intent getForwardStickerIntent(Context context, String stickerId, String categoryId)
	{
		Intent intent = new Intent(context, ComposeChatActivity.class);
		intent.putExtra(HikeConstants.Extras.FORWARD_MESSAGE, true);
		JSONArray multipleMsgArray = new JSONArray();
		try
		{
			JSONObject multiMsgFwdObject = new JSONObject();
			multiMsgFwdObject.putOpt(StickerManager.FWD_CATEGORY_ID, categoryId);
			multiMsgFwdObject.putOpt(StickerManager.FWD_STICKER_ID, stickerId);
			multipleMsgArray.put(multiMsgFwdObject);
		}
		catch (JSONException e)
		{
			Logger.e(context.getClass().getSimpleName(), "Invalid JSON", e);
		}

		intent.putExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT, multipleMsgArray.toString());
		return intent;
	}

	public static Intent getForwardImageIntent(Context context, File argFile)
	{
		Intent intent = new Intent(context, ComposeChatActivity.class);
		intent.putExtra(HikeConstants.Extras.FORWARD_MESSAGE, true);
		intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(argFile));
		intent.setType("image");
		return intent;
	}

	public static Intent getHikeGalleryPickerIntent(Context context, boolean allowMultiSelect, PendingIntent argIntent)
	{
		Intent intent = new Intent(context, GalleryActivity.class);
		Bundle b = new Bundle();
		b.putParcelable(GalleryActivity.PENDING_INTENT_KEY, argIntent);
		b.putBoolean(GalleryActivity.DISABLE_MULTI_SELECT_KEY, !allowMultiSelect);
		intent.putExtras(b);
		return intent;
	}
	
	public static Intent getHikeGalleryPickerIntentForResult(Context context, boolean allowMultiSelect,boolean categorizeByFolders,int actionBarType,PendingIntent argIntent)
	{
		Intent intent = new Intent(context, GalleryActivity.class);
		Bundle b = new Bundle();
		b.putParcelable(GalleryActivity.PENDING_INTENT_KEY, argIntent);
		b.putBoolean(GalleryActivity.RETURN_RESULT_KEY, true);
		b.putBoolean(GalleryActivity.DISABLE_MULTI_SELECT_KEY, !allowMultiSelect);
		b.putBoolean(GalleryActivity.FOLDERS_REQUIRED_KEY, categorizeByFolders);
		b.putInt(GalleryActivity.ACTION_BAR_TYPE_KEY, actionBarType);
		intent.putExtras(b);
		return intent;
	}

	public static void openConnectedApps(Context appContext)
	{
		appContext.startActivity(new Intent(appContext, ConnectedAppsActivity.class));
	}

	public static void openHikeSDKAuth(Context appContext, Message msg)
	{
		Intent hikeAuthIntent = new Intent("com.bsb.hike.ui.HikeAuthActivity");
		hikeAuthIntent.putExtra(HikeAuthActivity.MESSAGE_INDEX, Message.obtain(msg));
		hikeAuthIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		hikeAuthIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		appContext.startActivity(hikeAuthIntent);
	}

	public static void openWelcomeActivity(Context appContext)
	{
		Intent i = new Intent(appContext, WelcomeActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		appContext.startActivity(i);
	}

	public static void openSignupActivity(Context appContext)
	{
		Intent i = new Intent(appContext, SignupActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		appContext.startActivity(i);
	}

	public static void openHomeActivity(Context context)
	{
		Intent in = new Intent(context, HomeActivity.class);
		context.startActivity(in);
	}

	public static Intent openInviteFriends(Activity context)
	{
		// TODO Auto-generated method stub
		Intent in = new Intent(context, NUXInviteActivity.class);
		in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return in;
	}

	public static Intent openNuxFriendSelector(Activity context)
	{
		Intent in = new Intent(context, ComposeChatActivity.class);
		in.putExtra(HikeConstants.Extras.FORWARD_MESSAGE, true);
		in.putExtra(HikeConstants.Extras.NUX_INCENTIVE_MODE, true);
		return in;
	}

	public static Intent openNuxCustomMessage(Activity context)
	{
		Intent in = new Intent(context, NuxSendCustomMessageActivity.class);
		return in;
	}
	
	public static Intent getComposeChatIntent(Activity context)
	{
		Intent intent = new Intent(context, ComposeChatActivity.class);
		intent.putExtra(HikeConstants.Extras.EDIT, true);
		return intent;
	}
	
	public static Intent getFavouritesIntent(Activity context)
	{
		Intent intent = new Intent(context, PeopleActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
	}

	public static Intent getStickerShopIntent(Activity context)
	{
		Intent intent = new Intent(context, StickerShopActivity.class);
		return intent;
	}

	public static Intent getStickerSettingIntent(Activity context)
	{
		Intent intent = new Intent(context, StickerSettingsActivity.class);
		return intent;
	}
	
	public static Intent getProfileIntent(Activity context)
	{

		Intent intent = new Intent();
		intent.setClass(context, ProfileActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
	}

	public static void openHikeCameraActivity(Activity argActivity)
	{
		Intent in = new Intent(argActivity, HikeCameraActivity.class);
		argActivity.startActivity(in);
	}

	public static Intent getChatThreadIntent(Context context, String msisdn)
	{
		Intent intent = new Intent(context, ChatThread.class);
		intent.putExtra(HikeConstants.Extras.MSISDN, msisdn);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
	}

	public static Intent getVoipCallIntent(Context context, String msisdn, VoIPUtils.CallSource source)
	{
		Intent intent = new Intent(context, VoIPService.class);
		intent.putExtra(VoIPConstants.Extras.ACTION, VoIPConstants.Extras.OUTGOING_CALL);
		intent.putExtra(VoIPConstants.Extras.MSISDN, msisdn);
		intent.putExtra(VoIPConstants.Extras.CALL_SOURCE, source.ordinal());
		return intent;
	}

	public static Intent getVoipCallRateActivityIntent(Context context)
	{
		Intent intent = new Intent(context, CallRateActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		return intent;
	}

	public static Intent getVoipIncomingCallIntent(Context context)
	{
		Intent intent = new Intent(context, VoIPActivity.class);
		intent.putExtra(VoIPConstants.Extras.INCOMING_CALL, true);
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		if(HikeMessengerApp.currentState != HikeMessengerApp.CurrentState.RESUMED && HikeMessengerApp.currentState != HikeMessengerApp.CurrentState.OPENED)
		{
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
		}
		else
		{
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		}
		return intent;
	}
}
