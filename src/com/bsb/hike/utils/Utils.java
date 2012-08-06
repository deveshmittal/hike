package com.bsb.hike.utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.utils.JSONSerializable;
import com.bsb.hike.ui.SignupActivity;
import com.bsb.hike.ui.WelcomeActivity;
import com.bsb.hike.utils.AccountUtils.AccountInfo;

public class Utils
{
	public static Pattern shortCodeRegex;
	public static Pattern msisdnRegex;
	public static Pattern pinRegex;

	public static String shortCodeIntent;

	private static Animation mOutToRight;
	private static Animation mInFromLeft;

	private static TranslateAnimation mOutToLeft;

	private static TranslateAnimation mInFromRight;

	public static float densityMultiplier;

	static
	{
		shortCodeRegex = Pattern.compile("\\*\\d{3,10}#");
		msisdnRegex = Pattern.compile("\\[(\\+\\d*)\\]");
		pinRegex = Pattern.compile("\\d{6}");
	}

	public static String join(Collection<?> s, String delimiter, String startWith, String endWith)
	{
		StringBuilder builder = new StringBuilder();
		Iterator<?> iter = s.iterator();
		while (iter.hasNext())
		{
			if (!TextUtils.isEmpty(startWith))
			{
				builder.append(startWith);
			}
			builder.append(iter.next());
			if (!TextUtils.isEmpty(endWith))
			{
				builder.append(endWith);
			}
			if (!iter.hasNext())
			{
				break;
			}
			builder.append(delimiter);
		}
		Log.d("Utils", "Joined string is: " + builder.toString());
		return builder.toString();
	}

	/*
	 * serializes the given collection into an object. Ignores exceptions
	 */
	public static JSONArray jsonSerialize(Collection<? extends JSONSerializable> elements)
	{
		JSONArray arr = new JSONArray();
		for (JSONSerializable elem : elements)
		{
			try
			{
				arr.put(elem.toJSON());
			}
			catch (JSONException e)
			{
				Log.e("Utils", "error json serializing", e);
			}
		}
		return arr;
	}

	public static JSONObject jsonSerialize(Map<String, ? extends JSONSerializable> elements) throws JSONException
	{
		JSONObject obj = new JSONObject();
		for (Map.Entry<String, ? extends JSONSerializable> element : elements.entrySet())
		{
			obj.put(element.getKey(), element.getValue().toJSON());
		}
		return obj;
	}

	static final private int ANIMATION_DURATION = 400;
	public static Animation inFromRightAnimation(Context ctx)
	{
		if (mInFromRight == null)
		{
			synchronized(Utils.class)
			{
				mInFromRight = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, +1.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
						Animation.RELATIVE_TO_PARENT, 0.0f);
				mInFromRight.setDuration(ANIMATION_DURATION);
				mInFromRight.setInterpolator(new AccelerateInterpolator());				
			}
		}
		return mInFromRight;
	}

	public static Animation outToLeftAnimation(Context ctx)
	{
		if (mOutToLeft == null)
		{
			synchronized(Utils.class)
			{
				mOutToLeft = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, -1.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
						Animation.RELATIVE_TO_PARENT, 0.0f);
				mOutToLeft.setDuration(ANIMATION_DURATION);
				mOutToLeft.setInterpolator(new AccelerateInterpolator());				
			}
		}

		return mOutToLeft;
	}

	public static Animation outToRightAnimation(Context ctx)
	{
		if (mOutToRight == null)
		{
			synchronized(Utils.class)
			{
				if (mOutToRight == null)
				{
					mOutToRight = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 1.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
							Animation.RELATIVE_TO_PARENT, 0.0f);
					mOutToRight.setDuration(ANIMATION_DURATION);
					mOutToRight.setInterpolator(new AccelerateInterpolator());
				}
			}
		}
		return mOutToRight;
	}

	public static Animation inFromLeftAnimation(Context ctx)
	{
		if (mInFromLeft == null)
		{
			synchronized(Utils.class)
			{
				if (mInFromLeft == null)
				{
					mInFromLeft = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, -1.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
							Animation.RELATIVE_TO_PARENT, 0.0f);
					mInFromLeft.setDuration(ANIMATION_DURATION);
					mInFromLeft.setInterpolator(new AccelerateInterpolator());
				}
			}
		}
		return mInFromLeft;
	}

	public static Intent createIntentFromContactInfo(final ContactInfo contactInfo)
	{
		Intent intent = new Intent();
		intent.putExtra(HikeConstants.Extras.ID, contactInfo.getId());
		intent.putExtra(HikeConstants.Extras.MSISDN, contactInfo.getMsisdn());
		return intent;
	}

	static private int iconHash(String s)
	{
		/* ignore everything after ::
		 * so that your large icon by default
		 * matches your msisdn
		 */
		s = s.split("::")[0];
		int count = 0;
		for(int i = 0; i < s.length(); ++i)
		{
			count += s.charAt(i);
		}

		return count;
	}

	public static Drawable getDefaultIconForUser(Context context, String msisdn)
	{
		if(isGroupConversation(msisdn))
		{
			return context.getResources().getDrawable(R.drawable.ic_group_avatar);
		}
		int count = 1;
		int id;
		switch(iconHash(msisdn) % count)
		{
		case 0:
			id = R.drawable.ic_avatar0;
			break;
		default:
			id = R.drawable.ic_avatar0;
			break;
		}

		return context.getResources().getDrawable(id);
	}

	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;

	/** Create a file Uri for saving an image or video */
	public static Uri getOutputMediaFileUri(int type){
	      return Uri.fromFile(getOutputMediaFile(type));
	}

	/** Create a File for saving an image or video */
	public static File getOutputMediaFile(int type){
	    // To be safe, you should check that the SDCard is mounted
	    // using Environment.getExternalStorageState() before doing this.

	    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
	              Environment.DIRECTORY_PICTURES), "Hike");
	    // This location works best if you want the created images to be shared
	    // between applications and persist after your app has been uninstalled.

	    // Create the storage directory if it does not exist
	    if (! mediaStorageDir.exists()){
	        if (! mediaStorageDir.mkdirs()){
	            Log.d("Hike", "failed to create directory");
	            return null;
	        }
	    }

	    // Create a media file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    File mediaFile;
	    if (type == MEDIA_TYPE_IMAGE){
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "IMG_"+ timeStamp + ".jpg");
	    } else if(type == MEDIA_TYPE_VIDEO) {
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "VID_"+ timeStamp + ".mp4");
	    } else {
	        return null;
	    }

	    return mediaFile;
	}

	public static Bitmap getRoundedCornerBitmap(Bitmap bitmap)
	{
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
		        bitmap.getHeight(), Config.ARGB_8888);
		    Canvas canvas = new Canvas(output);

		    final int color = 0xff424242;
		    final Paint paint = new Paint();
		    final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		    final RectF rectF = new RectF(rect);
		    final float roundPx = 5;

		    paint.setAntiAlias(true);
		    canvas.drawARGB(0, 0, 0, 0);
		    paint.setColor(color);
		    canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

		    paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		    canvas.drawBitmap(bitmap, rect, rect, paint);

		    return output;
	}

	public static void savedAccountCredentials(AccountInfo accountInfo, SharedPreferences.Editor editor)
	{
		editor.putString(HikeMessengerApp.MSISDN_SETTING, accountInfo.msisdn);
		editor.putString(HikeMessengerApp.TOKEN_SETTING, accountInfo.token);
		editor.putString(HikeMessengerApp.UID_SETTING, accountInfo.uid);
		editor.putInt(HikeMessengerApp.SMS_SETTING, accountInfo.smsCredits);
		editor.putInt(HikeMessengerApp.INVITED, accountInfo.all_invitee);
		editor.putInt(HikeMessengerApp.INVITED_JOINED, accountInfo.all_invitee_joined);
		editor.commit();
	}

	/* Extract a pin code from a specially formatted message
	 * to the application.
	 * @return null iff the message isn't an SMS pincode,
	 * otherwise return the pincode
	 */
	public static String getSMSPinCode(String body)
	{
		Matcher m = pinRegex.matcher(body);
		return m.find() ? m.group() : null;
	}

	public static boolean requireAuth(Activity activity)
	{
		SharedPreferences settings = activity.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		if (!settings.getBoolean(HikeMessengerApp.ACCEPT_TERMS, false))
		{
			activity.startActivity(new Intent(activity, WelcomeActivity.class));
			activity.finish();
			return true;
		}

		if (settings.getString(HikeMessengerApp.NAME_SETTING, null) == null)
		{
			activity.startActivity(new Intent(activity, SignupActivity.class));
			activity.finish();
			return true;
		}

		Log.d("Utils", "auth token is " + settings.getString(HikeMessengerApp.TOKEN_SETTING, null));
		return false;
	}
	
	public static int[] getNumberImage(String msisdn)
	{
		int[] msisdnRes = new int[msisdn.length()];
		
		for (int i = 0; i < msisdnRes.length; i++) {
			char c = msisdn.charAt(i);
			switch (c) {
			case '+':
				msisdnRes[i] = R.drawable.no_plus;
				break;
			case '0':
				msisdnRes[i] = R.drawable.no0;
				break;
			case '1':
				msisdnRes[i] = R.drawable.no1;
				break;
			case '2':
				msisdnRes[i] = R.drawable.no2;
				break;
			case '3':
				msisdnRes[i] = R.drawable.no3;
				break;
			case '4':
				msisdnRes[i] = R.drawable.no4;
				break;
			case '5':
				msisdnRes[i] = R.drawable.no5;
				break;
			case '6':
				msisdnRes[i] = R.drawable.no6;
				break;
			case '7':
				msisdnRes[i] = R.drawable.no7;
				break;
			case '8':
				msisdnRes[i] = R.drawable.no8;
				break;
			case '9':
				msisdnRes[i] = R.drawable.no9;
				break;
			case '-':
				msisdnRes[i] = R.drawable.no_dash;
				break;
			default:
				msisdnRes[i] = R.drawable.no0;
				break;
			}
		}
		return msisdnRes;
	}
	
	public static String formatNo(String msisdn)
	{
		StringBuilder sb = new StringBuilder(msisdn);
		sb.insert(msisdn.length() - 4, '-');
		sb.insert(msisdn.length() - 7, '-');
		Log.d("Fomat MSISD", "Fomatted number is:" + sb.toString());
		
		return sb.toString();
	}

	public static boolean isValidEmail(Editable text)
	{
		return (!TextUtils.isEmpty(text) &&
				android.util.Patterns.EMAIL_ADDRESS.matcher(text).matches());
	}

	public static void logEvent(Context context, String event)
	{
		logEvent(context, event, 1);
	}

	/**
	 * Used for logging the UI based events from the clients side. 
	 * @param context
	 * @param event: The event which is to be logged.
	 * @param time: This is only used to signify the time the user was on a screen for. For cases where this is not relevant we send 0.s
	 */
	public static void logEvent(Context context, String event, long increment)
	{
		SharedPreferences prefs = context.getSharedPreferences(HikeMessengerApp.ANALYTICS, 0);
		
		long currentVal = prefs.getLong(event, 0) + increment;

		Editor editor = prefs.edit();
		editor.putLong(event, currentVal);
		editor.commit();
	}

	public static List<String> splitSelectedContacts(String selections)
	{
		Matcher matcher = msisdnRegex.matcher(selections);
		List<String> contacts = new ArrayList<String>();
		if (matcher.find()) 
		{
			do 
			{
				contacts.add(matcher.group().substring(1, matcher.group().length() - 1));
				Log.d("Utils", "Adding: " + matcher.group().substring(1, matcher.group().length() - 1));
			} 
			while (matcher.find(matcher.end()));
		}
		return contacts;
	}

	public static List<String> splitSelectedContactsName(String selections)
	{
		String[] selectedContacts = selections.split(", ");
		List<String> contactNames = new ArrayList<String>(selectedContacts.length);
		for(int i = 0; i<selectedContacts.length; i++)
		{
			contactNames.add(selectedContacts[i].substring(0, selectedContacts[i].indexOf("[")));
		}
		return contactNames;
	}

	public static boolean isGroupConversation(String msisdn)
	{
		return !msisdn.startsWith("+");
	}

	public static String defaultGroupName(Map<String, GroupParticipant> participantList)
	{
		List<GroupParticipant> groupParticipants = new ArrayList<GroupParticipant>();
		for(Entry<String, GroupParticipant> participant : participantList.entrySet())
		{
			if(!participant.getValue().hasLeft())
			{
				groupParticipants.add(participant.getValue());
			}
		}
		Collections.sort(groupParticipants);

		switch (groupParticipants.size()) 
		{
		case 0:
			return "";
		case 1:
			return groupParticipants.get(0).getContactInfo().getFirstName();
		case 2:
			return groupParticipants.get(0).getContactInfo().getFirstName() + " and "
			+ groupParticipants.get(1).getContactInfo().getFirstName();
		default:
			return groupParticipants.get(0).getContactInfo().getFirstName() + " and "
			+ (groupParticipants.size() - 1) + " others";
		}
	}
	
	public static JSONObject getDeviceDetails(Context context)
	{
		try 
		{
			//{"t": "le", "d"{"tag":"cbs", "device_id": "54330bc905bcf18a","_os": "DDD","_os_version": "EEE","_device": "FFF","_resolution": "GGG","_carrier": "HHH", "appversion" : "x.x.x"}}
			int height;
			int width;
			JSONObject object = new JSONObject();
			JSONObject data = new JSONObject();

			/*
			 *  Doing this to avoid the ClassCastException when the context is sent from the BroadcastReceiver.
			 *  As it is, we don't need to send the resolution from the BroadcastReceiver since it should have
			 *  already been sent to the server.
			 */
			if (context instanceof Activity) 
			{
				height = ((Activity) context).getWindowManager().getDefaultDisplay().getHeight();
				width = ((Activity) context).getWindowManager().getDefaultDisplay().getWidth();
				String resolution = height + "x" + width;
				data.put(HikeConstants.LogEvent.RESOLUTION, resolution);
			}
			TelephonyManager manager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);

			String osVersion = Build.VERSION.RELEASE;
			String deviceId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
			String os = "Android";
			String carrier = manager.getNetworkOperatorName();
			String device = Build.MANUFACTURER + " " + Build.MODEL;
			String appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0 ).versionName;

			object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.ANALYTICS_EVENT);
			Map<String, String> referralValues = retrieveReferralParams(context);
			if(!referralValues.isEmpty())
			{
				for(Entry<String, String> entry: referralValues.entrySet())
				{
					data.put(entry.getKey(), entry.getValue());
				}
			}
			data.put(HikeConstants.LogEvent.TAG, "cbs");
			data.put(HikeConstants.LogEvent.DEVICE_ID, deviceId);
			data.put(HikeConstants.LogEvent.OS, os);
			data.put(HikeConstants.LogEvent.OS_VERSION, osVersion);
			data.put(HikeConstants.LogEvent.DEVICE, device);
			data.put(HikeConstants.LogEvent.CARRIER, carrier);
			data.put(HikeConstants.LogEvent.APP_VERSION, appVersion);
			object.put(HikeConstants.DATA, data);

			return object;
		} 
		catch (JSONException e) 
		{
			Log.e("Utils", "Invalid JSON", e);
			return null;
		} 
		catch (NameNotFoundException e) 
		{
			Log.e("Utils", "Package not found", e);
			return null;
		}


	}
	
	public static JSONObject getDeviceStats(Context context)
	{
		SharedPreferences prefs = context.getSharedPreferences(HikeMessengerApp.ANALYTICS, 0);
		Editor editor = prefs.edit();
		Map<String, ?> keys = prefs.getAll();

		JSONObject data = new JSONObject();
		JSONObject obj = new JSONObject();

		try 
		{
			if(keys.isEmpty())
			{
				obj = null;
			}
			else
			{
				for (String key : keys.keySet())
				{
					Log.d("Utils", "Getting keys: " + key);
					data.put(key, prefs.getLong(key, 0));
					editor.remove(key);
				}
				editor.commit();
				data.put(HikeConstants.LogEvent.TAG, "mob");

				obj.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.ANALYTICS_EVENT);
				obj.put(HikeConstants.DATA, data);
			}
		} 
		catch (JSONException e) 
		{
			Log.e("Utils", "Invalid JSON", e);
		}

		return obj;
	}

	public static CharSequence addContactName(String firstName, CharSequence message)
	{
		SpannableStringBuilder messageWithName = new SpannableStringBuilder(firstName + HikeConstants.SEPARATOR + message);
		messageWithName.setSpan(new StyleSpan(Typeface.BOLD), 0, firstName.length() + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return messageWithName;
	}

	/**
	 * Used for setting the density multiplier, which is to be multiplied with any pixel value that is programmatically given
	 * @param activity
	 */
	public static void setDensityMultiplier(Activity activity)
	{
		if(Utils.densityMultiplier == 0.0f)
		{
			DisplayMetrics metrics = new DisplayMetrics();
			activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
			Utils.densityMultiplier = metrics.scaledDensity;
		}
	}

	public static CharSequence getFormattedParticipantInfo(String info)
	{
		SpannableStringBuilder ssb = new SpannableStringBuilder(info);
		ssb.setSpan(new ForegroundColorSpan(0xff666666), 0, info.indexOf(" "), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return ssb;
	}

	/**
	 * Used for preventing the cursor from being shown initially on the text box in touch screen devices. On touching the text box the cursor becomes visible
	 * @param editText
	 */
	public static void hideCursor(final EditText editText, Resources resources)
	{
		if (resources.getConfiguration().keyboard == Configuration.KEYBOARD_NOKEYS 
				|| resources.getConfiguration().hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
			editText.setCursorVisible(false);
			editText.setOnTouchListener(new OnTouchListener() 
			{
				@Override
				public boolean onTouch(View v, MotionEvent event) 
				{
					if(event.getAction() == MotionEvent.ACTION_DOWN)
					{
						editText.setCursorVisible(true);
					}
					return false;
				}
			});
		}
	}
	
	public static ContactInfo getUserContactInfo(SharedPreferences prefs)
	{
		String myMsisdn = prefs.getString(HikeMessengerApp.MSISDN_SETTING, null);
		String myName = prefs.getString(HikeMessengerApp.NAME_SETTING, null);
		return new ContactInfo(myMsisdn, myMsisdn, myName, myMsisdn, true);
	}

	public static boolean wasScreenOpenedNNumberOfTimes(SharedPreferences prefs, String whichScreen)
	{
		return prefs.getInt(whichScreen, 0) >= HikeConstants.NUM_TIMES_SCREEN_SHOULD_OPEN_BEFORE_TOOL_TIP;
	}

	public static void incrementNumTimesScreenOpen(SharedPreferences prefs, String whichScreen)
	{
		Editor editor= prefs.edit();
		editor.putInt(whichScreen, prefs.getInt(whichScreen, 0) + 1);
		editor.commit();
	}

	public static boolean isUpdateRequired(String version, Context context)
	{
		try {
			String appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0 ).versionName;

			StringTokenizer updateVersion = new StringTokenizer(version,".");
			StringTokenizer currentVersion = new StringTokenizer(appVersion, ".");
			while(currentVersion.hasMoreTokens())
			{
				if(!updateVersion.hasMoreTokens())
				{
					return false;
				}
				if(Integer.parseInt(updateVersion.nextToken()) > Integer.parseInt(currentVersion.nextToken()))
				{
					return true;
				}
			}
			while(updateVersion.hasMoreTokens())
			{
				if(Integer.parseInt(updateVersion.nextToken()) > 0)
				{
					return true;
				}
			}
			return false;
		} 
		catch (NameNotFoundException e) 
		{
			Log.e("Utils", "Package not found...", e);
			return false;
		}
	}
	
	 /*
     * Stores the referral parameters in the app's sharedPreferences.
     */
    public static void storeReferralParams(Context context, List<NameValuePair> params)
    {
        SharedPreferences storage = context.getSharedPreferences(HikeMessengerApp.REFERRAL, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = storage.edit();
 
        for(NameValuePair nameValuePair: params)
        {
        	String name = nameValuePair.getName();
            String value = nameValuePair.getValue();
            editor.putString(name, value);
        }
 
        editor.commit();
    }
 
    /*
     * Returns a map with the Market Referral parameters pulled from the sharedPreferences.
     */
    public static Map<String, String> retrieveReferralParams(Context context)
    {
        Map<String, String> params = new HashMap<String, String>();
        SharedPreferences storage = context.getSharedPreferences(HikeMessengerApp.REFERRAL, Context.MODE_PRIVATE);
 
        for(String key : storage.getAll().keySet())
        {
            String value = storage.getString(key, null);
            if(value != null)
            {
                params.put(key, value);
            }
        }
        // We don't need these values anymore
        Editor editor = storage.edit();
        editor.clear();
        editor.commit();
        return params;
    }

    
    public static boolean isUserOnline(Context context)
	{
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		return (cm != null && cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isAvailable() && cm.getActiveNetworkInfo().isConnected());
	}

    /**
     * Requests the server to send an account info packet
     */
    public static void requestAccountInfo()
    {
    	Log.d("Utils", "Requesting account info");
    	JSONObject requestAccountInfo = new JSONObject();
		try 
		{
			requestAccountInfo.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.REQUEST_ACCOUNT_INFO);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, requestAccountInfo);
		} 
		catch (JSONException e) 
		{
			Log.e("Utils", "Invalid JSON", e);
		}
    }

    public static String ellipsizeName(String name)
    {
    	return name.length() <= HikeConstants.MAX_CHAR_IN_NAME ? name : (name.substring(0, HikeConstants.MAX_CHAR_IN_NAME - 3) + "...");
    }

    public static void startInviteShareIntent(Context context)
    {
    	String inviteUrlWithToken = context.getString(R.string.default_invite_url) + context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeConstants.INVITE_TOKEN, "");
    	// Adding the user's invite token to the invite url
    	String inviteMessage = context.getString(R.string.invite_share_message);
    	String defaultInviteURL = context.getString(R.string.default_invite_url);
    	inviteMessage = inviteMessage.replace(defaultInviteURL, inviteUrlWithToken);

    	Intent s = new Intent(android.content.Intent.ACTION_SEND);

    	s.setType("text/plain");
    	s.putExtra(Intent.EXTRA_TEXT, inviteMessage);
    	context.startActivity(s);
    }
}
