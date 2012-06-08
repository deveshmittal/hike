package com.bsb.hike.utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.NetworkManager;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.utils.JSONSerializable;
import com.bsb.hike.ui.SignupActivity;
import com.bsb.hike.ui.WelcomeActivity;
import com.bsb.hike.utils.AccountUtils.AccountInfo;

public class Utils
{
	public static Pattern shortCodeRegex;
	public static Pattern msisdnRegex; 

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
	}

	public static String join(Collection<?> s, String delimiter, boolean quote)
	{
		StringBuilder builder = new StringBuilder();
		Iterator<?> iter = s.iterator();
		while (iter.hasNext())
		{
			if (quote)
			{
				builder.append("\"");
			}
			builder.append(iter.next());
			if (quote)
			{
				builder.append("\"");
			}
			if (!iter.hasNext())
			{
				break;
			}
			builder.append(delimiter);
		}
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
		editor.commit();
	}

	/* Extract a pin code from a specially formatted message
	 * to the application.
	 * @return null iff the message isn't an SMS pincode,
	 * otherwise return the pincode
	 */
	public static String getSMSPinCode(String body)
	{
		return body;
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

		return false;
	}
	
	public static int convertVersionToInt(String version)
	{
		int v = 0;
		int multiplier = 100;
		StringTokenizer st = new StringTokenizer(version,".");
		while(st.hasMoreTokens())
		{
			v += (Integer.parseInt(st.nextToken())*multiplier);
			multiplier /=10;
		}
		return v;
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

	/**
	 * Used for logging the UI based events from the clients side. 
	 * @param context
	 * @param event: The event which is to be logged.
	 * @param time: This is only used to signify the time the user was on a screen for. For cases where this is not relevant we send 0.s
	 */
	public static void logEvent(Context context, String event, int time)
	{
		SharedPreferences prefs = context.getSharedPreferences(HikeMessengerApp.ANALYTICS, 0);
		
		int currentVal = prefs.getInt(event, 0);
		currentVal = time == 0 ? currentVal+1 : currentVal + time;

		Editor editor = prefs.edit();
		editor.putInt(event, currentVal);
		editor.commit();
	}

	public static ArrayList<String> splitSelectedContacts(String selections)
	{
		Matcher matcher = msisdnRegex.matcher(selections);
		ArrayList<String> contacts = new ArrayList<String>();
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

	public static boolean isGroupConversation(String msisdn)
	{
		return !msisdn.startsWith("+");
	}

	public static String defaultGroupName(List<ContactInfo> participantList, Context context)
	{
		if (participantList.size() > 0) {
			Log.d("Utils",
					"Fetching name for contact: " + participantList.get(0));
			switch (participantList.size()) {
			case 1:
				return participantList.get(0).getFirstName();
			case 2:
				return participantList.get(0).getFirstName() + " and "
						+ participantList.get(1).getFirstName();
			default:
				return participantList.get(0).getFirstName() + " and "
						+ (participantList.size() - 1) + " others";
			}
		}
		return "";
	}

	
	public static JSONObject getDeviceDetails(Context context)
	{
		//{"t": "le", "d"{"tag":"cbs", "device_id": "54330bc905bcf18a","_os": "DDD","_os_version": "EEE","_device": "FFF","_resolution": "GGG","_carrier": "HHH"}}
		int height = ((Activity)context).getWindowManager().getDefaultDisplay().getHeight();
		int width = ((Activity)context).getWindowManager().getDefaultDisplay().getWidth();
		TelephonyManager manager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
		
		String osVersion = Build.VERSION.RELEASE;
		String deviceId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
		String os = "Android";
		String resolution = height + "x" + width;
		String carrier = manager.getNetworkOperatorName();
		String device = Build.MANUFACTURER + " " + Build.MODEL;
				
		JSONObject object = new JSONObject();
		JSONObject data = new JSONObject();
		
		try {
			object.put(HikeConstants.TYPE, NetworkManager.ANALYTICS_EVENT);
			data.put(HikeConstants.LogEvent.TAG, "cbs");
			data.put(HikeConstants.LogEvent.DEVICE_ID, deviceId);
			data.put(HikeConstants.LogEvent.OS, os);
			data.put(HikeConstants.LogEvent.OS_VERSION, osVersion);
			data.put(HikeConstants.LogEvent.DEVICE, device);
			data.put(HikeConstants.LogEvent.RESOLUTION, resolution);
			data.put(HikeConstants.LogEvent.CARRIER, carrier);
			object.put(HikeConstants.DATA, data);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return object;
	}
	
	public static JSONObject getDeviceStats(Context context)
	{
		SharedPreferences prefs = context.getSharedPreferences(HikeMessengerApp.ANALYTICS, 0);
		Map<String, ?> keys = prefs.getAll();
		Iterator<String> i= keys.keySet().iterator();

		JSONObject data = new JSONObject();
		JSONObject obj = new JSONObject();

		try 
		{
			while (i.hasNext())
			{
				String key = i.next();
				Log.d("Utils", "Getting keys: " + key);
				data.put(key, prefs.getInt(key, 0));
			}
			data.put(HikeConstants.LogEvent.TAG, "mob");
			
			obj.put(HikeConstants.TYPE, NetworkManager.ANALYTICS_EVENT);
			obj.put(HikeConstants.DATA, data);
		} 
		catch (JSONException e) 
		{
			e.printStackTrace();
		}

		return obj;
	}

	public static String getContactName(List<ContactInfo> participantList, String msisdn)
	{
		String name = msisdn;
		for(ContactInfo contactInfo : participantList)
		{
			if(contactInfo.getMsisdn().equals(msisdn))
			{
				return contactInfo.getFirstName();
			}
		}
		return name;
	}

	public static CharSequence addContactName(List<ContactInfo> participantList, String msisdn, CharSequence message)
	{
		String name = getContactName(participantList, msisdn);
		SpannableStringBuilder messageWithName = new SpannableStringBuilder(name + " - " + message);
		messageWithName.setSpan(new StyleSpan(Typeface.BOLD), 0, name.length() + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
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
}
