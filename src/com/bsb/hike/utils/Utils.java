package com.bsb.hike.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.CharBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings.Secure;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.HikeConstants.SMSSyncState;
import com.bsb.hike.HikeConstants.TipType;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikeMessengerApp.CurrentState;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.cropimage.CropImage;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfoData;
import com.bsb.hike.models.ContactInfoData.DataType;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.models.utils.JSONSerializable;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.tasks.CheckForUpdateTask;
import com.bsb.hike.tasks.SignupTask;
import com.bsb.hike.tasks.SyncOldSMSTask;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.SignupActivity;
import com.bsb.hike.ui.WelcomeActivity;
import com.bsb.hike.utils.AccountUtils.AccountInfo;
import com.google.android.maps.GeoPoint;

public class Utils {
	public static Pattern shortCodeRegex;
	public static Pattern msisdnRegex;
	public static Pattern pinRegex;

	public static String shortCodeIntent;

	private static Animation mOutToRight;
	private static Animation mInFromLeft;

	private static TranslateAnimation mOutToLeft;

	private static TranslateAnimation mInFromRight;

	public static float densityMultiplier = 1.0f;

	static {
		shortCodeRegex = Pattern.compile("\\*\\d{3,10}#");
		msisdnRegex = Pattern.compile("\\[(\\+\\d*)\\]");
		pinRegex = Pattern.compile("\\d{4,6}");
	}

	public static String join(Collection<?> s, String delimiter,
			String startWith, String endWith) {
		StringBuilder builder = new StringBuilder();
		Iterator<?> iter = s.iterator();
		while (iter.hasNext()) {
			if (!TextUtils.isEmpty(startWith)) {
				builder.append(startWith);
			}
			builder.append(iter.next());
			if (!TextUtils.isEmpty(endWith)) {
				builder.append(endWith);
			}
			if (!iter.hasNext()) {
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
	public static JSONArray jsonSerialize(
			Collection<? extends JSONSerializable> elements) {
		JSONArray arr = new JSONArray();
		for (JSONSerializable elem : elements) {
			try {
				arr.put(elem.toJSON());
			} catch (JSONException e) {
				Log.e("Utils", "error json serializing", e);
			}
		}
		return arr;
	}

	public static JSONObject jsonSerialize(
			Map<String, ? extends JSONSerializable> elements)
			throws JSONException {
		JSONObject obj = new JSONObject();
		for (Map.Entry<String, ? extends JSONSerializable> element : elements
				.entrySet()) {
			obj.put(element.getKey(), element.getValue().toJSON());
		}
		return obj;
	}

	static final private int ANIMATION_DURATION = 400;

	public static Animation inFromRightAnimation(Context ctx) {
		if (mInFromRight == null) {
			synchronized (Utils.class) {
				mInFromRight = new TranslateAnimation(
						Animation.RELATIVE_TO_PARENT, +1.0f,
						Animation.RELATIVE_TO_PARENT, 0.0f,
						Animation.RELATIVE_TO_PARENT, 0.0f,
						Animation.RELATIVE_TO_PARENT, 0.0f);
				mInFromRight.setDuration(ANIMATION_DURATION);
				mInFromRight.setInterpolator(new AccelerateInterpolator());
			}
		}
		return mInFromRight;
	}

	public static Animation outToLeftAnimation(Context ctx) {
		if (mOutToLeft == null) {
			synchronized (Utils.class) {
				mOutToLeft = new TranslateAnimation(
						Animation.RELATIVE_TO_PARENT, 0.0f,
						Animation.RELATIVE_TO_PARENT, -1.0f,
						Animation.RELATIVE_TO_PARENT, 0.0f,
						Animation.RELATIVE_TO_PARENT, 0.0f);
				mOutToLeft.setDuration(ANIMATION_DURATION);
				mOutToLeft.setInterpolator(new AccelerateInterpolator());
			}
		}

		return mOutToLeft;
	}

	public static Animation outToRightAnimation(Context ctx) {
		if (mOutToRight == null) {
			synchronized (Utils.class) {
				if (mOutToRight == null) {
					mOutToRight = new TranslateAnimation(
							Animation.RELATIVE_TO_PARENT, 0.0f,
							Animation.RELATIVE_TO_PARENT, 1.0f,
							Animation.RELATIVE_TO_PARENT, 0.0f,
							Animation.RELATIVE_TO_PARENT, 0.0f);
					mOutToRight.setDuration(ANIMATION_DURATION);
					mOutToRight.setInterpolator(new AccelerateInterpolator());
				}
			}
		}
		return mOutToRight;
	}

	public static Animation inFromLeftAnimation(Context ctx) {
		if (mInFromLeft == null) {
			synchronized (Utils.class) {
				if (mInFromLeft == null) {
					mInFromLeft = new TranslateAnimation(
							Animation.RELATIVE_TO_PARENT, -1.0f,
							Animation.RELATIVE_TO_PARENT, 0.0f,
							Animation.RELATIVE_TO_PARENT, 0.0f,
							Animation.RELATIVE_TO_PARENT, 0.0f);
					mInFromLeft.setDuration(ANIMATION_DURATION);
					mInFromLeft.setInterpolator(new AccelerateInterpolator());
				}
			}
		}
		return mInFromLeft;
	}

	public static Intent createIntentFromContactInfo(
			final ContactInfo contactInfo, boolean openKeyBoard) {
		Intent intent = new Intent();

		// If the contact info was made using a group conversation, then the
		// Group ID is in the contact ID
		intent.putExtra(
				HikeConstants.Extras.MSISDN,
				Utils.isGroupConversation(contactInfo.getMsisdn()) ? contactInfo
						.getId() : contactInfo.getMsisdn());
		intent.putExtra(HikeConstants.Extras.SHOW_KEYBOARD, true);
		return intent;
	}

	static private int iconHash(String s) {
		/*
		 * ignore everything after :: so that your large icon by default matches
		 * your msisdn
		 */
		s = s.split("::")[0];
		int count = 0;
		for (int i = 0; i < s.length(); ++i) {
			count += s.charAt(i);
		}

		return count;
	}

	public static Drawable getDefaultIconForUser(Context context, String msisdn) {
		return getDefaultIconForUser(context, msisdn, false);
	}

	public static Drawable getDefaultIconForUser(Context context,
			String msisdn, boolean rounded) {
		if (isGroupConversation(msisdn)) {
			int count = 6;
			int id;
			switch (iconHash(msisdn) % count) {
			case 0:
				id = rounded ? R.drawable.ic_group_avatar1_rounded
						: R.drawable.ic_group_avatar1;
				break;
			case 1:
				id = rounded ? R.drawable.ic_group_avatar2_rounded
						: R.drawable.ic_group_avatar2;
				break;
			case 2:
				id = rounded ? R.drawable.ic_group_avatar4_rounded
						: R.drawable.ic_group_avatar4;
				break;
			case 3:
				id = rounded ? R.drawable.ic_group_avatar5_rounded
						: R.drawable.ic_group_avatar5;
				break;
			case 4:
				id = rounded ? R.drawable.ic_group_avatar6_rounded
						: R.drawable.ic_group_avatar6;
				break;
			case 5:
				id = rounded ? R.drawable.ic_group_avatar7_rounded
						: R.drawable.ic_group_avatar7;
				break;
			default:
				id = rounded ? R.drawable.ic_group_avatar1_rounded
						: R.drawable.ic_group_avatar1;
				break;
			}
			return context.getResources().getDrawable(id);
		}
		int count = 7;
		int id;
		switch (iconHash(msisdn) % count) {
		case 0:
			id = rounded ? R.drawable.ic_avatar1_rounded
					: R.drawable.ic_avatar1;
			break;
		case 1:
			id = rounded ? R.drawable.ic_avatar2_rounded
					: R.drawable.ic_avatar2;
			break;
		case 2:
			id = rounded ? R.drawable.ic_avatar3_rounded
					: R.drawable.ic_avatar3;
			break;
		case 3:
			id = rounded ? R.drawable.ic_avatar4_rounded
					: R.drawable.ic_avatar4;
			break;
		case 4:
			id = rounded ? R.drawable.ic_avatar5_rounded
					: R.drawable.ic_avatar5;
			break;
		case 5:
			id = rounded ? R.drawable.ic_avatar6_rounded
					: R.drawable.ic_avatar6;
			break;
		case 6:
			id = rounded ? R.drawable.ic_avatar7_rounded
					: R.drawable.ic_avatar7;
			break;
		default:
			id = rounded ? R.drawable.ic_avatar1_rounded
					: R.drawable.ic_avatar1;
			break;
		}

		return context.getResources().getDrawable(id);
	}

	public static String getDefaultAvatarServerName(String msisdn) {
		String name;
		int count = 7;
		int id = iconHash(msisdn) % count;
		if (isGroupConversation(msisdn)) {
			switch (id) {
			case 0:
				name = "GreenPeople";
				break;
			case 1:
				name = "RedPeople";
				break;
			case 2:
				name = "BluePeople";
				break;
			case 3:
				name = "CoffeePeople";
				break;
			case 4:
				name = "EarthyPeople";
				break;
			case 5:
				name = "PinkPeople";
				break;
			case 6:
				name = "TealPeople";
				break;
			default:
				name = "GreenPeople";
				break;
			}
		} else {
			switch (id) {
			case 0:
				name = "Beach";
				break;
			case 1:
				name = "Candy";
				break;
			case 2:
				name = "Cocktail";
				break;
			case 3:
				name = "Coffee";
				break;
			case 4:
				name = "Digital";
				break;
			case 5:
				name = "Sneakers";
				break;
			case 6:
				name = "Space";
				break;
			default:
				name = "Beach";
				break;
			}
		}
		return name + ".jpg";
	}

	/** Create a File for saving an image or video */
	public static File getOutputMediaFile(HikeFileType type,
			String orgFileName, String fileKey) {
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.

		String path = getFileParent(type);
		if (path == null) {
			return null;
		}

		File mediaStorageDir = new File(path);
		// This location works best if you want the created images to be shared
		// between applications and persist after your app has been uninstalled.

		// Create the storage directory if it does not exist
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				Log.d("Hike", "failed to create directory");
				return null;
			}
		}

		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());

		// File name should only be blank in case of profile images or while
		// capturing new media.
		if (TextUtils.isEmpty(orgFileName)) {
			switch (type) {
			case PROFILE:
			case IMAGE:
				orgFileName = "IMG_" + timeStamp + ".jpg";
				break;
			case VIDEO:
				orgFileName = "MOV_" + timeStamp + ".mp4";
			case AUDIO:
			case AUDIO_RECORDING:
				orgFileName = "AUD_" + timeStamp + ".m4a";
			}
		}

		String fileName = getUniqueFileName(orgFileName, fileKey);

		return new File(mediaStorageDir, fileName);
	}

	public static String getFileParent(HikeFileType type) {
		StringBuilder path = new StringBuilder(
				HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT);
		switch (type) {
		case PROFILE:
			path.append(HikeConstants.PROFILE_ROOT);
			break;
		case IMAGE:
			path.append(HikeConstants.IMAGE_ROOT);
			break;
		case VIDEO:
			path.append(HikeConstants.VIDEO_ROOT);
			break;
		case AUDIO:
			path.append(HikeConstants.AUDIO_ROOT);
			break;
		case AUDIO_RECORDING:
			path.append(HikeConstants.AUDIO_RECORDING_ROOT);
			break;
		default:
			return null;
		}
		return path.toString();
	}

	public static Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
				bitmap.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);

		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		final RectF rectF = new RectF(rect);
		final float roundPx = 4;

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);

		return output;
	}

	public static Bitmap getCircularBitmap(Bitmap bitmap) {
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
				bitmap.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);

		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
				bitmap.getWidth() / 2, paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);
		return output;
	}

	public static void savedAccountCredentials(AccountInfo accountInfo,
			SharedPreferences.Editor editor) {
		AccountUtils.setToken(accountInfo.token);
		AccountUtils.setUID(accountInfo.uid);
		editor.putString(HikeMessengerApp.MSISDN_SETTING, accountInfo.msisdn);
		editor.putString(HikeMessengerApp.TOKEN_SETTING, accountInfo.token);
		editor.putString(HikeMessengerApp.UID_SETTING, accountInfo.uid);
		editor.putInt(HikeMessengerApp.SMS_SETTING, accountInfo.smsCredits);
		editor.putInt(HikeMessengerApp.INVITED, accountInfo.all_invitee);
		editor.putInt(HikeMessengerApp.INVITED_JOINED,
				accountInfo.all_invitee_joined);
		editor.putString(HikeMessengerApp.COUNTRY_CODE,
				accountInfo.country_code);
		editor.commit();
	}

	/*
	 * Extract a pin code from a specially formatted message to the application.
	 * 
	 * @return null iff the message isn't an SMS pincode, otherwise return the
	 * pincode
	 */
	public static String getSMSPinCode(String body) {
		Matcher m = pinRegex.matcher(body);
		return m.find() ? m.group() : null;
	}

	public static boolean requireAuth(Activity activity) {
		SharedPreferences settings = activity.getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		if (!settings.getBoolean(HikeMessengerApp.ACCEPT_TERMS, false)) {
			disconnectAndStopService(activity);
			activity.startActivity(new Intent(activity, WelcomeActivity.class));
			activity.finish();
			return true;
		}

		if (settings.getString(HikeMessengerApp.NAME_SETTING, null) == null) {
			disconnectAndStopService(activity);
			activity.startActivity(new Intent(activity, SignupActivity.class));
			activity.finish();
			return true;
		}

		return false;
	}

	public static void disconnectAndStopService(Activity activity) {
		// Added these lines to prevent the bad username/password bug.
		HikeMessengerApp app = (HikeMessengerApp) activity
				.getApplicationContext();
		app.disconnectFromService();
		activity.stopService(new Intent(activity, HikeService.class));
	}

	public static String formatNo(String msisdn) {
		StringBuilder sb = new StringBuilder(msisdn);
		sb.insert(msisdn.length() - 4, '-');
		sb.insert(msisdn.length() - 7, '-');
		Log.d("Fomat MSISD", "Fomatted number is:" + sb.toString());

		return sb.toString();
	}

	public static boolean isValidEmail(Editable text) {
		return (!TextUtils.isEmpty(text) && android.util.Patterns.EMAIL_ADDRESS
				.matcher(text).matches());
	}

	public static void logEvent(Context context, String event) {
		logEvent(context, event, 1);
	}

	/**
	 * Used for logging the UI based events from the clients side.
	 * 
	 * @param context
	 * @param event
	 *            : The event which is to be logged.
	 * @param time
	 *            : This is only used to signify the time the user was on a
	 *            screen for. For cases where this is not relevant we send 0.s
	 */
	public static void logEvent(Context context, String event, long increment) {
		SharedPreferences prefs = context.getSharedPreferences(
				HikeMessengerApp.ANALYTICS, 0);

		long currentVal = prefs.getLong(event, 0) + increment;

		Editor editor = prefs.edit();
		editor.putLong(event, currentVal);
		editor.commit();
	}

	public static List<String> splitSelectedContacts(String selections) {
		Matcher matcher = msisdnRegex.matcher(selections);
		List<String> contacts = new ArrayList<String>();
		if (matcher.find()) {
			do {
				contacts.add(matcher.group().substring(1,
						matcher.group().length() - 1));
				Log.d("Utils",
						"Adding: "
								+ matcher.group().substring(1,
										matcher.group().length() - 1));
			} while (matcher.find(matcher.end()));
		}
		return contacts;
	}

	public static List<String> splitSelectedContactsName(String selections) {
		String[] selectedContacts = selections.split(", ");
		List<String> contactNames = new ArrayList<String>(
				selectedContacts.length);
		for (int i = 0; i < selectedContacts.length; i++) {
			if (!selectedContacts[i].contains("[")) {
				continue;
			}
			contactNames.add(selectedContacts[i].substring(0,
					selectedContacts[i].indexOf("[")));
		}
		return contactNames;
	}

	public static boolean isGroupConversation(String msisdn) {
		return !msisdn.startsWith("+");
	}

	public static String defaultGroupName(
			Map<String, GroupParticipant> participantList) {
		List<GroupParticipant> groupParticipants = new ArrayList<GroupParticipant>();
		for (Entry<String, GroupParticipant> participant : participantList
				.entrySet()) {
			if (!participant.getValue().hasLeft()) {
				groupParticipants.add(participant.getValue());
			}
		}
		Collections.sort(groupParticipants);

		switch (groupParticipants.size()) {
		case 0:
			return "";
		case 1:
			return groupParticipants.get(0).getContactInfo().getFirstName();
		case 2:
			return groupParticipants.get(0).getContactInfo().getFirstName()
					+ " and "
					+ groupParticipants.get(1).getContactInfo().getFirstName();
		default:
			return groupParticipants.get(0).getContactInfo().getFirstName()
					+ " and " + (groupParticipants.size() - 1) + " others";
		}
	}

	public static String getGroupJoinHighlightText(
			JSONArray participantInfoArray, GroupConversation conversation) {
		JSONObject participant = (JSONObject) participantInfoArray.opt(0);
		String highlight = ((GroupConversation) conversation)
				.getGroupParticipantFirstName(participant
						.optString(HikeConstants.MSISDN));

		if (participantInfoArray.length() == 2) {
			JSONObject participant2 = (JSONObject) participantInfoArray.opt(1);
			String name2 = ((GroupConversation) conversation)
					.getGroupParticipantFirstName(participant2
							.optString(HikeConstants.MSISDN));

			highlight += " and " + name2;
		} else if (participantInfoArray.length() > 2) {
			highlight += " and " + (participantInfoArray.length() - 1)
					+ " others";
		}
		return highlight;
	}

	public static JSONObject getDeviceDetails(Context context) {
		try {
			// {"t": "le", "d"{"tag":"cbs", "device_id":
			// "54330bc905bcf18a","_os": "DDD","_os_version": "EEE","_device":
			// "FFF","_resolution": "GGG","_carrier": "HHH", "_app_version" :
			// "x.x.x"}}
			int height;
			int width;
			JSONObject object = new JSONObject();
			JSONObject data = new JSONObject();

			/*
			 * Doing this to avoid the ClassCastException when the context is
			 * sent from the BroadcastReceiver. As it is, we don't need to send
			 * the resolution from the BroadcastReceiver since it should have
			 * already been sent to the server.
			 */
			if (context instanceof Activity) {
				height = ((Activity) context).getWindowManager()
						.getDefaultDisplay().getHeight();
				width = ((Activity) context).getWindowManager()
						.getDefaultDisplay().getWidth();
				String resolution = height + "x" + width;
				data.put(HikeConstants.LogEvent.RESOLUTION, resolution);
			}
			TelephonyManager manager = (TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE);

			String osVersion = Build.VERSION.RELEASE;
			String deviceId = null;
			try {
				deviceId = getHashedDeviceId(Secure.getString(
						context.getContentResolver(), Secure.ANDROID_ID));
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String os = "Android";
			String carrier = manager.getNetworkOperatorName();
			String device = Build.MANUFACTURER + " " + Build.MODEL;
			String appVersion = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0).versionName;

			object.put(HikeConstants.TYPE,
					HikeConstants.MqttMessageTypes.ANALYTICS_EVENT);
			Map<String, String> referralValues = retrieveReferralParams(context);
			if (!referralValues.isEmpty()) {
				for (Entry<String, String> entry : referralValues.entrySet()) {
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
		} catch (JSONException e) {
			Log.e("Utils", "Invalid JSON", e);
			return null;
		} catch (NameNotFoundException e) {
			Log.e("Utils", "Package not found", e);
			return null;
		}

	}

	public static JSONObject getDeviceStats(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(
				HikeMessengerApp.ANALYTICS, 0);
		Editor editor = prefs.edit();
		Map<String, ?> keys = prefs.getAll();

		JSONObject data = new JSONObject();
		JSONObject obj = new JSONObject();

		try {
			if (keys.isEmpty()) {
				obj = null;
			} else {
				for (String key : keys.keySet()) {
					Log.d("Utils", "Getting keys: " + key);
					data.put(key, prefs.getLong(key, 0));
					editor.remove(key);
				}
				editor.commit();
				data.put(HikeConstants.LogEvent.TAG, HikeConstants.LOGEVENT_TAG);

				obj.put(HikeConstants.TYPE,
						HikeConstants.MqttMessageTypes.ANALYTICS_EVENT);
				obj.put(HikeConstants.DATA, data);
			}
		} catch (JSONException e) {
			Log.e("Utils", "Invalid JSON", e);
		}

		return obj;
	}

	public static CharSequence addContactName(String firstName,
			CharSequence message) {
		SpannableStringBuilder messageWithName = new SpannableStringBuilder(
				firstName + HikeConstants.SEPARATOR + message);
		messageWithName.setSpan(new StyleSpan(Typeface.BOLD), 0,
				firstName.length() + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return messageWithName;
	}

	/**
	 * Used for setting the density multiplier, which is to be multiplied with
	 * any pixel value that is programmatically given
	 * 
	 * @param activity
	 */
	public static void setDensityMultiplier(DisplayMetrics displayMetrics) {
		Utils.densityMultiplier = displayMetrics.scaledDensity;
	}

	public static CharSequence getFormattedParticipantInfo(String info,
			String textToHighight) {
		SpannableStringBuilder ssb = new SpannableStringBuilder(info);
		ssb.setSpan(new StyleSpan(Typeface.BOLD), info.indexOf(textToHighight),
				info.indexOf(textToHighight) + textToHighight.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return ssb;
	}

	/**
	 * Used for preventing the cursor from being shown initially on the text box
	 * in touch screen devices. On touching the text box the cursor becomes
	 * visible
	 * 
	 * @param editText
	 */
	public static void hideCursor(final EditText editText, Resources resources) {
		if (resources.getConfiguration().keyboard == Configuration.KEYBOARD_NOKEYS
				|| resources.getConfiguration().hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
			editText.setCursorVisible(false);
			editText.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						editText.setCursorVisible(true);
					}
					return false;
				}
			});
		}
	}

	public static ContactInfo getUserContactInfo(SharedPreferences prefs) {
		return getUserContactInfo(prefs, false);
	}

	public static ContactInfo getUserContactInfo(SharedPreferences prefs,
			boolean showNameAsYou) {
		String myMsisdn = prefs
				.getString(HikeMessengerApp.MSISDN_SETTING, null);
		long userJoinTime = prefs.getLong(HikeMessengerApp.USER_JOIN_TIME, 0);

		String myName;
		if (showNameAsYou) {
			myName = "You";
		} else {
			myName = prefs.getString(HikeMessengerApp.NAME_SETTING, null);
		}

		ContactInfo contactInfo = new ContactInfo(myName, myMsisdn, myName,
				myMsisdn, true);
		contactInfo.setHikeJoinTime(userJoinTime);

		return contactInfo;
	}

	public static boolean wasScreenOpenedNNumberOfTimes(
			SharedPreferences prefs, String whichScreen) {
		return prefs.getInt(whichScreen, 0) >= HikeConstants.NUM_TIMES_SCREEN_SHOULD_OPEN_BEFORE_TOOL_TIP;
	}

	public static void incrementNumTimesScreenOpen(SharedPreferences prefs,
			String whichScreen) {
		Editor editor = prefs.edit();
		editor.putInt(whichScreen, prefs.getInt(whichScreen, 0) + 1);
		editor.commit();
	}

	public static boolean isUpdateRequired(String version, Context context) {
		try {
			String appVersion = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0).versionName;

			StringTokenizer updateVersion = new StringTokenizer(version, ".");
			StringTokenizer currentVersion = new StringTokenizer(appVersion,
					".");
			while (currentVersion.hasMoreTokens()) {
				if (!updateVersion.hasMoreTokens()) {
					return false;
				}
				int currentVersionToken = Integer.parseInt(currentVersion
						.nextToken());
				int updateVersionToken = Integer.parseInt(updateVersion
						.nextToken());
				if (updateVersionToken > currentVersionToken) {
					return true;
				} else if (updateVersionToken < currentVersionToken) {
					return false;
				}
			}
			while (updateVersion.hasMoreTokens()) {
				if (Integer.parseInt(updateVersion.nextToken()) > 0) {
					return true;
				}
			}
			return false;
		} catch (NameNotFoundException e) {
			Log.e("Utils", "Package not found...", e);
			return false;
		}
	}

	/*
	 * Stores the referral parameters in the app's sharedPreferences.
	 */
	public static void storeReferralParams(Context context,
			List<NameValuePair> params) {
		SharedPreferences storage = context.getSharedPreferences(
				HikeMessengerApp.REFERRAL, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = storage.edit();

		for (NameValuePair nameValuePair : params) {
			String name = nameValuePair.getName();
			String value = nameValuePair.getValue();
			editor.putString(name, value);
		}

		editor.commit();
	}

	/*
	 * Returns a map with the Market Referral parameters pulled from the
	 * sharedPreferences.
	 */
	public static Map<String, String> retrieveReferralParams(Context context) {
		Map<String, String> params = new HashMap<String, String>();
		SharedPreferences storage = context.getSharedPreferences(
				HikeMessengerApp.REFERRAL, Context.MODE_PRIVATE);

		for (String key : storage.getAll().keySet()) {
			String value = storage.getString(key, null);
			if (value != null) {
				params.put(key, value);
			}
		}
		// We don't need these values anymore
		Editor editor = storage.edit();
		editor.clear();
		editor.commit();
		return params;
	}

	public static boolean isUserOnline(Context context) {
		if (context == null) {
			Log.e("HikeService", "Hike service is null!!");
			return false;
		}
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		return (cm != null && cm.getActiveNetworkInfo() != null
				&& cm.getActiveNetworkInfo().isAvailable() && cm
				.getActiveNetworkInfo().isConnected());
	}

	/**
	 * Requests the server to send an account info packet
	 */
	public static void requestAccountInfo(boolean upgrade, boolean sendbot) {
		Log.d("Utils", "Requesting account info");
		JSONObject requestAccountInfo = new JSONObject();
		try {
			requestAccountInfo.put(HikeConstants.TYPE,
					HikeConstants.MqttMessageTypes.REQUEST_ACCOUNT_INFO);

			JSONObject data = new JSONObject();
			data.put(HikeConstants.UPGRADE, upgrade);
			data.put(HikeConstants.SENDBOT, sendbot);

			requestAccountInfo.put(HikeConstants.DATA, data);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH,
					requestAccountInfo);
		} catch (JSONException e) {
			Log.e("Utils", "Invalid JSON", e);
		}
	}

	public static String ellipsizeName(String name) {
		return name.length() <= HikeConstants.MAX_CHAR_IN_NAME ? name : (name
				.substring(0, HikeConstants.MAX_CHAR_IN_NAME - 3) + "...");
	}

	public static String getInviteMessage(Context context, int messageResId) {
		String inviteToken = context.getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(
				HikeConstants.INVITE_TOKEN, "");
		inviteToken = "";
		// Adding the user's invite token to the invite url
		String inviteMessage = context.getString(messageResId, inviteToken);

		return inviteMessage;
	}

	public static void startShareIntent(Context context, String message) {
		Intent s = new Intent(android.content.Intent.ACTION_SEND);
		s.setType("text/plain");
		s.putExtra(Intent.EXTRA_TEXT, message);
		context.startActivity(s);
	}

	public static void bytesToFile(byte[] bytes, File dst) {
		OutputStream out = null;
		try {
			out = new FileOutputStream(dst);
			out.write(bytes, 0, bytes.length);
		} catch (IOException e) {
			Log.e("Utils", "Excecption while copying the file", e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					Log.e("Utils", "Excecption while closing the stream", e);
				}
			}
		}
	}

	public static byte[] fileToBytes(File file) {
		byte[] bytes = new byte[(int) file.length()];
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
			fileInputStream.read(bytes);
			return bytes;
		} catch (IOException e) {
			Log.e("Utils",
					"Excecption while reading the file " + file.getName(), e);
			return null;
		} finally {
			if (fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (IOException e) {
					Log.e("Utils",
							"Excecption while closing the file "
									+ file.getName(), e);
				}
			}
		}
	}

	public static Drawable stringToDrawable(String encodedString) {
		if (TextUtils.isEmpty(encodedString)) {
			return null;
		}
		byte[] thumbnailBytes = Base64.decode(encodedString, Base64.DEFAULT);
		return new BitmapDrawable(BitmapFactory.decodeByteArray(thumbnailBytes,
				0, thumbnailBytes.length));
	}

	public static Bitmap scaleDownImage(String filePath, int dimensionLimit,
			boolean makeSquareThumbnail) {
		Bitmap thumbnail = null;

		int currentWidth = 0;
		int currentHeight = 0;

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;

		BitmapFactory.decodeFile(filePath, options);
		currentHeight = options.outHeight;
		currentWidth = options.outWidth;

		if (dimensionLimit == -1) {
			dimensionLimit = (int) (0.75 * (currentHeight > currentWidth ? currentHeight
					: currentWidth));
		}

		options.inSampleSize = Math
				.round((currentHeight > currentWidth ? currentHeight
						: currentWidth) / (dimensionLimit));
		options.inJustDecodeBounds = false;

		thumbnail = BitmapFactory.decodeFile(filePath, options);
		if (makeSquareThumbnail) {
			return makeSquareThumbnail(thumbnail, dimensionLimit);
		}

		return thumbnail;
	}

	public static Bitmap makeSquareThumbnail(Bitmap thumbnail,
			int dimensionLimit) {
		dimensionLimit = thumbnail.getWidth() < thumbnail.getHeight() ? thumbnail
				.getWidth() : thumbnail.getHeight();

		int startX = thumbnail.getWidth() > dimensionLimit ? (int) ((thumbnail
				.getWidth() - dimensionLimit) / 2) : 0;
		int startY = thumbnail.getHeight() > dimensionLimit ? (int) ((thumbnail
				.getHeight() - dimensionLimit) / 2) : 0;

		Log.d("Utils", "StartX: " + startX + " StartY: " + startY + " WIDTH: "
				+ thumbnail.getWidth() + " Height: " + thumbnail.getHeight());
		Bitmap squareThumbnail = Bitmap.createBitmap(thumbnail, startX, startY,
				dimensionLimit, dimensionLimit);

		if (squareThumbnail != thumbnail) {
			thumbnail.recycle();
		}
		thumbnail = null;
		return squareThumbnail;
	}

	public static Bitmap stringToBitmap(String thumbnailString) {
		byte[] encodeByte = Base64.decode(thumbnailString, Base64.DEFAULT);
		return BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
	}

	public static boolean isThumbnailSquare(Bitmap thumbnail) {
		return (thumbnail.getWidth() == thumbnail.getHeight());
	}

	public static byte[] bitmapToBytes(Bitmap bitmap,
			Bitmap.CompressFormat format) {
		return bitmapToBytes(bitmap, format, 50);
	}

	public static byte[] bitmapToBytes(Bitmap bitmap,
			Bitmap.CompressFormat format, int quality) {
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		bitmap.compress(format, quality, bao);
		return bao.toByteArray();
	}

	public static String getRealPathFromUri(Uri contentUri, Activity activity) {
		String[] proj = { MediaStore.Images.Media.DATA };
		Cursor cursor = activity.managedQuery(contentUri, proj, null, null,
				null);
		if (cursor == null || cursor.getCount() == 0) {
			return null;
		}
		int column_index = cursor
				.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}

	public static enum ExternalStorageState {
		WRITEABLE, READ_ONLY, NONE
	}

	public static ExternalStorageState getExternalStorageState() {
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			return ExternalStorageState.WRITEABLE;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			return ExternalStorageState.READ_ONLY;
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need
			// to know is we can neither read nor write
			return ExternalStorageState.NONE;
		}
	}

	public static String getFirstName(String name) {
		return name.split(" ", 2)[0];
	}

	public static double getFreeSpace() {
		StatFs stat = new StatFs(Environment.getExternalStorageDirectory()
				.getPath());
		double sdAvailSize = (double) stat.getAvailableBlocks()
				* (double) stat.getBlockSize();
		return sdAvailSize;
	}

	public static boolean copyFile(String srcFilePath, String destFilePath,
			HikeFileType hikeFileType) {
		/*
		 * If source and destination have the same path, just return.
		 */
		if (srcFilePath.equals(destFilePath)) {
			return true;
		}
		try {
			InputStream src;
			if (hikeFileType == HikeFileType.IMAGE) {
				String imageOrientation = Utils
						.getImageOrientation(srcFilePath);
				Bitmap tempBmp = Utils.scaleDownImage(srcFilePath,
						HikeConstants.MAX_DIMENSION_FULL_SIZE_PX, false);
				tempBmp = Utils.rotateBitmap(tempBmp,
						Utils.getRotatedAngle(imageOrientation));
				// Temporary fix for when a user uploads a file through Picasa
				// on ICS or higher.
				if (tempBmp == null) {
					return false;
				}
				byte[] fileBytes = Utils.bitmapToBytes(tempBmp,
						Bitmap.CompressFormat.JPEG, 75);
				tempBmp.recycle();
				src = new ByteArrayInputStream(fileBytes);
			} else {
				src = new FileInputStream(new File(srcFilePath));
			}
			OutputStream dest = new FileOutputStream(new File(destFilePath));

			byte[] buffer = new byte[HikeConstants.MAX_BUFFER_SIZE_KB * 1024];
			int len;

			while ((len = src.read(buffer)) > 0) {
				dest.write(buffer, 0, len);
			}

			src.close();
			dest.close();

			return true;
		} catch (FileNotFoundException e) {
			Log.e("Utils", "File not found while copying", e);
			return false;
		} catch (IOException e) {
			Log.e("Utils", "Error while reading/writing/closing file", e);
			return false;
		}
	}

	public static String getImageOrientation(String filePath) {
		ExifInterface exif;
		try {
			exif = new ExifInterface(filePath);
			return exif.getAttribute(ExifInterface.TAG_ORIENTATION);
		} catch (IOException e) {
			Log.e("Utils", "Error while opening file", e);
			return null;
		}
	}

	public static int getRotatedAngle(String imageOrientation) {
		if (!TextUtils.isEmpty(imageOrientation)) {
			switch (Integer.parseInt(imageOrientation)) {
			case ExifInterface.ORIENTATION_ROTATE_180:
				return 180;
			case ExifInterface.ORIENTATION_ROTATE_270:
				return 270;
			case ExifInterface.ORIENTATION_ROTATE_90:
				return 90;
			}
		}
		return 0;
	}

	public static Bitmap rotateBitmap(Bitmap b, int degrees) {
		if (degrees != 0 && b != null) {
			Matrix m = new Matrix();
			m.setRotate(degrees, (float) b.getWidth() / 2,
					(float) b.getHeight() / 2);
			try {
				Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(),
						b.getHeight(), m, true);
				if (b != b2) {
					b.recycle();
					b = b2;
				}
			} catch (OutOfMemoryError e) {
				Log.e("Utils", "Out of memory", e);
			}
		}
		return b;
	}

	public static void setupServerURL(boolean isProductionServer, boolean ssl) {
		Log.d("SSL", "Switching SSL on? " + ssl);

		AccountUtils.ssl = ssl;
		AccountUtils.mClient = null;

		String httpString = ssl ? AccountUtils.HTTPS_STRING
				: AccountUtils.HTTP_STRING;

		AccountUtils.host = isProductionServer ? AccountUtils.PRODUCTION_HOST
				: AccountUtils.STAGING_HOST;
		AccountUtils.port = isProductionServer ? (ssl ? AccountUtils.PRODUCTION_PORT_SSL
				: AccountUtils.PRODUCTION_PORT)
				: (ssl ? AccountUtils.STAGING_PORT_SSL
						: AccountUtils.STAGING_PORT);

		if (isProductionServer) {
			AccountUtils.base = httpString + AccountUtils.host + "/v1";
		} else {
			AccountUtils.base = httpString + AccountUtils.host + ":"
					+ Integer.toString(AccountUtils.port) + "/v1";
		}

		AccountUtils.fileTransferHost = isProductionServer ? AccountUtils.PRODUCTION_FT_HOST
				: AccountUtils.STAGING_HOST;
		AccountUtils.fileTransferUploadBase = httpString
				+ AccountUtils.fileTransferHost + ":"
				+ Integer.toString(AccountUtils.port) + "/v1";

		CheckForUpdateTask.UPDATE_CHECK_URL = httpString
				+ (isProductionServer ? CheckForUpdateTask.PRODUCTION_URL
						: CheckForUpdateTask.STAGING_URL);

		AccountUtils.fileTransferBaseDownloadUrl = AccountUtils.base
				+ AccountUtils.FILE_TRANSFER_DOWNLOAD_BASE;
		AccountUtils.fileTransferBaseViewUrl = AccountUtils.HTTP_STRING
				+ (isProductionServer ? AccountUtils.FILE_TRANSFER_BASE_VIEW_URL_PRODUCTION
						: AccountUtils.FILE_TRANSFER_BASE_VIEW_URL_STAGING);

		AccountUtils.rewardsUrl = httpString
				+ (isProductionServer ? AccountUtils.REWARDS_PRODUCTION_BASE
						: AccountUtils.REWARDS_STAGING_BASE);
		AccountUtils.gamesUrl = httpString
				+ (isProductionServer ? AccountUtils.GAMES_PRODUCTION_BASE
						: AccountUtils.GAMES_STAGING_BASE);
		AccountUtils.stickersUrl = AccountUtils.HTTP_STRING
				+ (isProductionServer ? AccountUtils.STICKERS_PRODUCTION_BASE
						: AccountUtils.STICKERS_STAGING_BASE);
		Log.d("SSL", "Base: " + AccountUtils.base);
		Log.d("SSL", "FTHost: " + AccountUtils.fileTransferHost);
		Log.d("SSL", "FTUploadBase: " + AccountUtils.fileTransferUploadBase);
		Log.d("SSL", "UpdateCheck: " + CheckForUpdateTask.UPDATE_CHECK_URL);
		Log.d("SSL", "FTDloadBase: " + AccountUtils.fileTransferBaseDownloadUrl);
		Log.d("SSL", "FTViewBase: " + AccountUtils.fileTransferBaseViewUrl);
	}

	public static boolean shouldChangeMessageState(ConvMessage convMessage,
			int stateOrdinal) {
		if (convMessage == null || convMessage.getTypingNotification() != null) {
			return false;
		}
		int minStatusOrdinal;
		int maxStatusOrdinal;
		// No need to change the message state for typing notifications
		if (HikeConstants.IS_TYPING.equals(convMessage.getMessage())
				&& convMessage.getMsgID() == -1
				&& convMessage.getMappedMsgID() == -1) {
			return false;
		}
		if (stateOrdinal <= State.SENT_DELIVERED_READ.ordinal()) {
			minStatusOrdinal = State.SENT_UNCONFIRMED.ordinal();
			maxStatusOrdinal = stateOrdinal;
		} else {
			minStatusOrdinal = State.RECEIVED_UNREAD.ordinal();
			maxStatusOrdinal = stateOrdinal;
		}

		int convMessageStateOrdinal = convMessage.getState().ordinal();

		if (convMessageStateOrdinal <= maxStatusOrdinal
				&& convMessageStateOrdinal >= minStatusOrdinal) {
			return true;
		}
		return false;
	}

	public static ConvMessage makeHike2SMSInviteMessage(String msisdn,
			Context context) {
		long time = (long) System.currentTimeMillis() / 1000;

		/*
		 * Randomising the invite text.
		 */
		Random random = new Random();
		int index = random.nextInt(HikeConstants.INVITE_STRINGS.length);

		ConvMessage convMessage = new ConvMessage(getInviteMessage(context,
				HikeConstants.INVITE_STRINGS[index]), msisdn, time,
				ConvMessage.State.SENT_UNCONFIRMED);
		convMessage.setInvite(true);

		return convMessage;
	}

	public static void sendInvite(String msisdn, Context context) {
		sendInvite(msisdn, context, false);
	}

	public static void sendInvite(String msisdn, Context context,
			boolean dbUpdated) {
		SmsManager smsManager = SmsManager.getDefault();

		ConvMessage convMessage = Utils.makeHike2SMSInviteMessage(msisdn,
				context);
		HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH,
				convMessage.serialize());

		ArrayList<String> messages = smsManager.divideMessage(convMessage
				.getMessage());

		/*
		 * The try-catch block is needed for a bug in certain LG devices where
		 * it throws an NPE here.
		 */
		try {
			smsManager.sendMultipartTextMessage(convMessage.getMsisdn(), null,
					messages, null, null);
		} catch (NullPointerException e) {
			Log.d("Send invite", "NPE while trying to send SMS", e);
		}

		if (!dbUpdated) {
			HikeUserDatabase.getInstance().updateInvitedTimestamp(msisdn,
					System.currentTimeMillis() / 1000);
		}
	}

	public static enum WhichScreen {
		FRIENDS_TAB, UPDATES_TAB, SMS_SECTION, OTHER
	}

	/*
	 * msisdn : mobile number to which we need to send the invite context :
	 * context of calling activity v : View of invite button which need to be
	 * set invited if not then send this as null checkPref : preference which
	 * need to set to not show this dialog. header : header text of the dialog
	 * popup body : body text message of dialog popup
	 */
	public static void sendInviteUtil(final ContactInfo contactInfo,
			final Context context, final String checkPref, String header,
			String body) {
		sendInviteUtil(contactInfo, context, checkPref, header, body,
				WhichScreen.OTHER);
	}

	public static void sendInviteUtil(final ContactInfo contactInfo,
			final Context context, final String checkPref, String header,
			String body, final WhichScreen whichScreen) {
		final SharedPreferences settings = context.getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		if (!settings.getBoolean(checkPref, false)) {
			final Dialog dialog = new Dialog(context,
					R.style.Theme_CustomDialog);
			dialog.setContentView(R.layout.operator_alert_popup);
			dialog.setCancelable(true);

			TextView headerView = (TextView) dialog.findViewById(R.id.header);
			TextView bodyView = (TextView) dialog.findViewById(R.id.body_text);
			Button btnOk = (Button) dialog.findViewById(R.id.btn_ok);
			Button btnCancel = (Button) dialog.findViewById(R.id.btn_cancel);

			btnCancel.setText(R.string.cancel);
			btnOk.setText(R.string.ok);

			headerView.setText(header);
			bodyView.setText(String.format(body, contactInfo.getFirstName()));

			CheckBox checkBox = (CheckBox) dialog
					.findViewById(R.id.body_checkbox);
			checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					Editor editor = settings.edit();
					editor.putBoolean(checkPref, isChecked);
					editor.commit();
				}
			});
			checkBox.setText(context.getResources().getString(
					R.string.not_show_call_alert_msg));

			btnOk.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					dialog.dismiss();
					invite(context, contactInfo, whichScreen);
				}
			});

			btnCancel.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					dialog.dismiss();
				}
			});

			dialog.show();
		} else {
			invite(context, contactInfo, whichScreen);
		}
	}

	private static void invite(Context context, ContactInfo contactInfo,
			WhichScreen whichScreen) {
		sendInvite(contactInfo.getMsisdn(), context, true);
		Toast.makeText(context, R.string.invite_sent, Toast.LENGTH_SHORT)
				.show();

		boolean isReminding = contactInfo.getInviteTime() != 0;

		long inviteTime = System.currentTimeMillis() / 1000;
		contactInfo.setInviteTime(inviteTime);

		HikeUserDatabase.getInstance().updateInvitedTimestamp(
				contactInfo.getMsisdn(), inviteTime);

		HikeMessengerApp.getPubSub().publish(HikePubSub.INVITE_SENT, null);

		switch (whichScreen) {
		case FRIENDS_TAB:
			Utils.sendFTUELogEvent(
					!isReminding ? HikeConstants.LogEvent.INVITE_FTUE_FRIENDS_CLICK
							: HikeConstants.LogEvent.REMIND_FTUE_FRIENDS_CLICK,
					contactInfo.getMsisdn());
			break;
		case UPDATES_TAB:
			Utils.sendFTUELogEvent(
					!isReminding ? HikeConstants.LogEvent.INVITE_FTUE_UPDATES_CLICK
							: HikeConstants.LogEvent.REMIND_FTUE_UPDATES_CLICK,
					contactInfo.getMsisdn());
			break;
		case SMS_SECTION:
			Utils.sendFTUELogEvent(
					!isReminding ? HikeConstants.LogEvent.INVITE_SMS_CLICK
							: HikeConstants.LogEvent.REMIND_SMS_CLICK,
					contactInfo.getMsisdn());
			break;
		}
	}

	public static String getAddressFromGeoPoint(GeoPoint geoPoint,
			Context context) {
		try {
			Geocoder geoCoder = new Geocoder(context, Locale.getDefault());
			List<Address> addresses = geoCoder.getFromLocation(
					geoPoint.getLatitudeE6() / 1E6,
					geoPoint.getLongitudeE6() / 1E6, 1);

			final StringBuilder address = new StringBuilder();
			if (!addresses.isEmpty()) {
				for (int i = 0; i < addresses.get(0).getMaxAddressLineIndex(); i++)
					address.append(addresses.get(0).getAddressLine(i) + "\n");
			}

			return address.toString();
		} catch (IOException e) {
			Log.e("Utils", "IOException", e);
			return "";
		}
	}

	public static void addFileName(String fileName, String fileKey) {
		File hikeFileList = new File(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT,
				HikeConstants.HIKE_FILE_LIST_NAME);

		JSONObject currentFiles = getHikeFileListData(hikeFileList);

		if (currentFiles == null) {
			Log.d("Utils", "File did not exist. Will create a new one");
			currentFiles = new JSONObject();
		}
		FileOutputStream fileOutputStream = null;
		ByteArrayInputStream byteArrayInputStream = null;
		try {
			Log.d("Utils", "Adding data : " + "File Name: " + fileName
					+ " File Key: " + fileKey);
			currentFiles.put(fileName, fileKey);
			fileOutputStream = new FileOutputStream(hikeFileList);
			byteArrayInputStream = new ByteArrayInputStream(currentFiles
					.toString().getBytes("UTF-8"));

			int b;
			byte[] data = new byte[8];
			while ((b = byteArrayInputStream.read(data)) != -1) {
				fileOutputStream.write(data, 0, b);
			}
		} catch (FileNotFoundException e) {
			Log.e("Utils", "File not found", e);
		} catch (JSONException e) {
			Log.e("Utils", "Invalid JSON", e);
		} catch (UnsupportedEncodingException e) {
			Log.e("Utils", "Unsupported Encoding Exception", e);
		} catch (IOException e) {
			Log.e("Utils", "IOException", e);
		} finally {
			if (fileOutputStream != null) {
				try {
					fileOutputStream.close();
				} catch (IOException e) {
					Log.e("Utils", "Exception while closing the output stream",
							e);
				}
			}
		}
	}

	public static String getUniqueFileName(String orgFileName, String fileKey) {
		File hikeFileList = new File(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT,
				HikeConstants.HIKE_FILE_LIST_NAME);
		JSONObject currentFiles = getHikeFileListData(hikeFileList);
		if (currentFiles == null || !currentFiles.has(orgFileName)) {
			Log.d("Utils", "File with this name does not exist");
			return orgFileName;
		}

		String fileExtension = orgFileName.contains(".") ? orgFileName
				.substring(orgFileName.lastIndexOf("."), orgFileName.length())
				: "";
		String orgFileNameWithoutExtension = !TextUtils.isEmpty(fileExtension) ? orgFileName
				.substring(0, orgFileName.indexOf(fileExtension)) : orgFileName;
		StringBuilder newFileName = new StringBuilder(
				orgFileNameWithoutExtension);

		String currentNameToCheck = orgFileName;
		int i = 1;
		Log.d("Utils", "File name: " + newFileName.toString() + " Extension: "
				+ fileExtension);
		while (true) {
			String existingFileKey = currentFiles.optString(currentNameToCheck);
			if (TextUtils.isEmpty(existingFileKey)
					|| existingFileKey.equals(fileKey)) {
				break;
			} else {
				newFileName = new StringBuilder(orgFileNameWithoutExtension
						+ "_" + i++);
				currentNameToCheck = newFileName + fileExtension;
			}
		}
		Log.d("Utils", "NewFile name: " + newFileName.toString()
				+ " Extension: " + fileExtension);
		newFileName.append(fileExtension);
		return newFileName.toString();
	}

	public static void makeNewFileWithExistingData(JSONObject data) {
		File hikeFileList = new File(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT,
				HikeConstants.HIKE_FILE_LIST_NAME);

		Log.d("Utils", "Writing data: " + data.toString());

		FileOutputStream fileOutputStream = null;
		ByteArrayInputStream byteArrayInputStream = null;
		try {
			fileOutputStream = new FileOutputStream(hikeFileList);
			byteArrayInputStream = new ByteArrayInputStream(data.toString()
					.getBytes("UTF-8"));

			int b;
			byte[] d = new byte[8];
			while ((b = byteArrayInputStream.read(d)) != -1) {
				fileOutputStream.write(d, 0, b);
			}
		} catch (FileNotFoundException e) {
			Log.e("Utils", "File not found", e);
		} catch (UnsupportedEncodingException e) {
			Log.e("Utils", "Unsupported Encoding Exception", e);
		} catch (IOException e) {
			Log.e("Utils", "IOException", e);
		} finally {
			if (fileOutputStream != null) {
				try {
					fileOutputStream.close();
				} catch (IOException e) {
					Log.e("Utils", "Exception while closing the output stream",
							e);
				}
			}
		}
	}

	private static JSONObject getHikeFileListData(File hikeFileList) {
		if (!hikeFileList.exists()) {
			return null;
		}
		FileInputStream fileInputStream = null;
		JSONObject currentFiles = null;
		try {
			fileInputStream = new FileInputStream(hikeFileList);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					fileInputStream));

			StringBuilder builder = new StringBuilder();
			CharBuffer target = CharBuffer.allocate(10000);
			int read = reader.read(target);

			while (read >= 0) {
				builder.append(target.array(), 0, read);
				target.clear();
				read = reader.read(target);
			}

			currentFiles = new JSONObject(builder.toString());
			Log.d("Utils", "File found: Current data: " + builder.toString());
		} catch (FileNotFoundException e) {
			Log.e("Utils", "File not found", e);
			hikeFileList.delete();
		} catch (IOException e) {
			Log.e("Utils", "IOException", e);
			hikeFileList.delete();
		} catch (JSONException e) {
			Log.e("Utils", "Invalid JSON", e);
			hikeFileList.delete();
		} finally {
			if (fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (IOException e) {
					Log.e("Utils", "Exception while closing the input stream",
							e);
				}
			}
		}
		return currentFiles;
	}

	public static String getSquareThumbnail(JSONObject obj) {
		String thumbnailString = obj.optString(HikeConstants.THUMBNAIL);
		if (TextUtils.isEmpty(thumbnailString)) {
			return thumbnailString;
		}

		Bitmap thumbnailBmp = Utils.stringToBitmap(thumbnailString);
		if (!Utils.isThumbnailSquare(thumbnailBmp)) {
			Bitmap squareThumbnail = Utils.makeSquareThumbnail(thumbnailBmp,
					HikeConstants.MAX_DIMENSION_THUMBNAIL_PX);
			thumbnailString = Base64.encodeToString(Utils.bitmapToBytes(
					squareThumbnail, Bitmap.CompressFormat.JPEG),
					Base64.DEFAULT);
			squareThumbnail.recycle();
			squareThumbnail = null;
		}
		if (!thumbnailBmp.isRecycled()) {
			thumbnailBmp.recycle();
			thumbnailBmp = null;
		}

		return thumbnailString;
	}

	public static String normalizeNumber(String inputNumber, String countryCode) {
		if (inputNumber.startsWith("+")) {
			return inputNumber;
		} else if (inputNumber.startsWith("00")) {
			/*
			 * Doing for US numbers
			 */
			return inputNumber.replaceFirst("00", "+");
		} else if (inputNumber.startsWith("0")) {
			return inputNumber.replaceFirst("0", countryCode);
		} else {
			return countryCode + inputNumber;
		}
	}

	public static void downloadAndSaveFile(Context context, File destFile,
			Uri uri) throws Exception {
		InputStream is = null;
		OutputStream os = null;
		try {

			if (isPicasaUri(uri.toString())
					&& !uri.toString().startsWith("http")) {
				is = context.getContentResolver().openInputStream(uri);
			} else {
				is = new URL(uri.toString()).openStream();
			}
			os = new FileOutputStream(destFile);

			byte[] buffer = new byte[HikeConstants.MAX_BUFFER_SIZE_KB * 1024];
			int len;

			while ((len = is.read(buffer)) > 0) {
				os.write(buffer, 0, len);
			}
		} finally {
			if (os != null) {
				os.close();
			}
			if (is != null) {
				is.close();
			}
		}
	}

	public static boolean isPicasaUri(String picasaUriString) {
		return (picasaUriString.toString().startsWith(
				HikeConstants.OTHER_PICASA_URI_START)
				|| picasaUriString.toString().startsWith(
						HikeConstants.JB_PICASA_URI_START) || picasaUriString
				.toString().startsWith("http"));
	}

	public static Uri makePicasaUri(Uri uri) {
		if (uri.toString().startsWith(
				"content://com.android.gallery3d.provider")) {
			// use the com.google provider, not the com.android
			// provider.
			return Uri.parse(uri.toString().replace("com.android.gallery3d",
					"com.google.android.gallery3d"));
		}
		return uri;
	}

	public static boolean switchSSLOn(Context context) {
		/*
		 * If the preference itself is switched to off, we don't need to check
		 * if the wifi is on or off.
		 */
		if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				HikeConstants.SSL_PREF, true)) {
			return false;
		}
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		return (cm != null && cm.getActiveNetworkInfo() != null && (cm
				.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI));
	}

	public static boolean renameTempProfileImage(String msisdn) {
		String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT
				+ HikeConstants.PROFILE_ROOT;
		String tempFileName = Utils.getTempProfileImageFileName(msisdn);
		String newFileName = Utils.getProfileImageFileName(msisdn);

		File tempFile = new File(directory, tempFileName);
		File newFile = new File(directory, newFileName);
		return tempFile.renameTo(newFile);
	}

	public static boolean removeTempProfileImage(String msisdn) {
		String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT
				+ HikeConstants.PROFILE_ROOT;
		String tempFileName = Utils.getTempProfileImageFileName(msisdn);

		return (new File(directory, tempFileName)).delete();
	}

	public static String getTempProfileImageFileName(String msisdn) {
		return getValidFileNameForMsisdn(msisdn) + "_tmp.jpg";
	}

	public static String getProfileImageFileName(String msisdn) {
		return getValidFileNameForMsisdn(msisdn) + ".jpg";
	}

	public static String getValidFileNameForMsisdn(String msisdn) {
		return msisdn.replaceAll(":", "-");
	}

	public static void removeLargerProfileImageForMsisdn(String msisdn) {
		String path = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT
				+ HikeConstants.PROFILE_ROOT;
		String fileName = Utils.getProfileImageFileName(msisdn);
		(new File(path, fileName)).delete();
	}

	public static void vibrateNudgeReceived(Context context) {
		if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				HikeConstants.VIBRATE_PREF, true)) {
			return;
		}
		AudioManager audioManager = (AudioManager) context
				.getSystemService(Context.AUDIO_SERVICE);
		int ringerMode = audioManager.getRingerMode();

		if (ringerMode != AudioManager.RINGER_MODE_SILENT) {
			Vibrator vibrator = (Vibrator) context
					.getSystemService(Context.VIBRATOR_SERVICE);
			vibrator.vibrate(100);
		}
	}

	private static String convertToHex(byte[] data) {
		StringBuilder buf = new StringBuilder();
		for (byte b : data) {
			int halfbyte = (b >>> 4) & 0x0F;
			int two_halfs = 0;
			do {
				buf.append((0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte)
						: (char) ('a' + (halfbyte - 10)));
				halfbyte = b & 0x0F;
			} while (two_halfs++ < 1);
		}
		return buf.toString();
	}

	public static String SHA1(String text) throws NoSuchAlgorithmException,
			UnsupportedEncodingException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(text.getBytes("iso-8859-1"), 0, text.length());
		byte[] sha1hash = md.digest();
		return convertToHex(sha1hash);
	}

	public static String getHashedDeviceId(String deviceId)
			throws NoSuchAlgorithmException, UnsupportedEncodingException {
		return "and:" + SHA1(deviceId);
	}

	public static void startCropActivity(Activity activity, String path,
			String destPath) {
		/* Crop the image */
		Intent intent = new Intent(activity, CropImage.class);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, destPath);
		intent.putExtra(HikeConstants.Extras.IMAGE_PATH, path);
		intent.putExtra(HikeConstants.Extras.SCALE, true);
		intent.putExtra(HikeConstants.Extras.OUTPUT_X,
				HikeConstants.MAX_DIMENSION_FULL_SIZE_PROFILE_PX);
		intent.putExtra(HikeConstants.Extras.OUTPUT_Y,
				HikeConstants.MAX_DIMENSION_FULL_SIZE_PROFILE_PX);
		intent.putExtra(HikeConstants.Extras.ASPECT_X, 1);
		intent.putExtra(HikeConstants.Extras.ASPECT_Y, 1);
		activity.startActivityForResult(intent, HikeConstants.CROP_RESULT);
	}

	public static long getContactId(Context context, long rawContactId) {
		Cursor cur = null;
		try {
			cur = context.getContentResolver().query(
					ContactsContract.RawContacts.CONTENT_URI,
					new String[] { ContactsContract.RawContacts.CONTACT_ID },
					ContactsContract.RawContacts._ID + "=" + rawContactId,
					null, null);
			if (cur.moveToFirst()) {
				return cur
						.getLong(cur
								.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
		return -1l;
	}

	public static List<ContactInfoData> getContactDataFromHikeFile(
			HikeFile hikeFile) {
		List<ContactInfoData> items = new ArrayList<ContactInfoData>();

		JSONArray phoneNumbers = hikeFile.getPhoneNumbers();
		JSONArray emails = hikeFile.getEmails();
		JSONArray events = hikeFile.getEvents();
		JSONArray addresses = hikeFile.getAddresses();

		if (phoneNumbers != null) {
			for (int i = 0; i < phoneNumbers.length(); i++) {
				JSONObject data = phoneNumbers.optJSONObject(i);
				String key = data.names().optString(0);
				items.add(new ContactInfoData(DataType.PHONE_NUMBER, data
						.optString(key), key));
			}
		}

		if (emails != null) {
			for (int i = 0; i < emails.length(); i++) {
				JSONObject data = emails.optJSONObject(i);
				String key = data.names().optString(0);
				items.add(new ContactInfoData(DataType.EMAIL, data
						.optString(key), key));
			}
		}

		if (events != null) {
			for (int i = 0; i < events.length(); i++) {
				JSONObject data = events.optJSONObject(i);
				String key = data.names().optString(0);
				items.add(new ContactInfoData(DataType.EVENT, data
						.optString(key), key));
			}
		}

		if (addresses != null) {
			for (int i = 0; i < addresses.length(); i++) {
				JSONObject data = addresses.optJSONObject(i);
				String key = data.names().optString(0);
				items.add(new ContactInfoData(DataType.ADDRESS, data
						.optString(key), key));
			}
		}
		return items;
	}

	public static int getNotificationCount(SharedPreferences accountPrefs,
			boolean countUsersStatus) {
		int notificationCount = 0;

		notificationCount += accountPrefs.getInt(
				HikeMessengerApp.UNSEEN_STATUS_COUNT, 0);

		if (countUsersStatus) {
			notificationCount += accountPrefs.getInt(
					HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, 0);
		}

		return notificationCount;
	}

	/*
	 * This method returns whether the device is an mdpi or ldpi device. The
	 * assumption is that these devices are low end and hence a DB call may
	 * block the UI on those devices.
	 */
	public static boolean loadOnUiThread() {
		return ((int) 10 * Utils.densityMultiplier) > 10;
	}

	public static void hideSoftKeyboard(Context context, View v) {
		if (v == null) {
			return;
		}
		InputMethodManager imm = (InputMethodManager) context
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
	}

	public static void sendLocaleToServer(Context context) {
		JSONObject object = new JSONObject();
		JSONObject data = new JSONObject();

		try {
			data.put(HikeConstants.LOCALE, context.getResources()
					.getConfiguration().locale.getLanguage());

			object.put(HikeConstants.TYPE,
					HikeConstants.MqttMessageTypes.ACCOUNT_CONFIG);
			object.put(HikeConstants.DATA, data);

			HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH,
					object);
		} catch (JSONException e) {
			Log.w("Locale", "Invalid JSON", e);
		}
	}

	public static void setReceiveSmsSetting(Context context, boolean value) {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context)
				.edit();
		editor.putBoolean(HikeConstants.RECEIVE_SMS_PREF, value);
		editor.commit();

		sendDefaultSMSClientLogEvent(value);
	}

	public static void setSendUndeliveredSmsSetting(Context context,
			boolean value) {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context)
				.edit();
		editor.putBoolean(HikeConstants.SEND_UNDELIVERED_AS_NATIVE_SMS_PREF,
				value);
		editor.commit();
	}

	public static boolean isContactInternational(String msisdn) {
		return !msisdn.startsWith("+91");
	}

	public static Dialog showSMSSyncDialog(final Context context,
			boolean syncConfirmation) {
		final Dialog dialog = new Dialog(context, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.enable_sms_client_popup);

		final View btnContainer = dialog.findViewById(R.id.button_container);

		final ProgressBar syncProgress = (ProgressBar) dialog
				.findViewById(R.id.loading_progress);
		TextView header = (TextView) dialog.findViewById(R.id.header);
		final TextView info = (TextView) dialog.findViewById(R.id.body);
		Button okBtn = (Button) dialog.findViewById(R.id.btn_ok);
		Button cancelBtn = (Button) dialog.findViewById(R.id.btn_cancel);
		final View btnDivider = dialog.findViewById(R.id.sms_divider);

		header.setText(R.string.import_sms);
		info.setText(R.string.import_sms_info);
		okBtn.setText(R.string.yes);
		cancelBtn.setText(R.string.no);

		setupSyncDialogLayout(syncConfirmation, btnContainer, syncProgress,
				info, btnDivider);

		okBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				HikeMessengerApp.getPubSub().publish(HikePubSub.SMS_SYNC_START,
						null);

				executeSMSSyncStateResultTask(new SyncOldSMSTask(context));

				setupSyncDialogLayout(false, btnContainer, syncProgress, info,
						btnDivider);

				sendSMSSyncLogEvent(true);
			}
		});

		cancelBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();

				sendSMSSyncLogEvent(false);
			}
		});

		dialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				Editor editor = context.getSharedPreferences(
						HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
				editor.putBoolean(HikeMessengerApp.SHOWN_SMS_SYNC_POPUP, true);
				editor.commit();
			}
		});

		dialog.show();
		return dialog;
	}

	private static void setupSyncDialogLayout(boolean syncConfirmation,
			View btnContainer, ProgressBar syncProgress, TextView info,
			View btnDivider) {
		btnContainer.setVisibility(syncConfirmation ? View.VISIBLE : View.GONE);
		syncProgress.setVisibility(syncConfirmation ? View.GONE : View.VISIBLE);
		btnDivider.setVisibility(syncConfirmation ? View.VISIBLE : View.GONE);
		info.setText(syncConfirmation ? R.string.import_sms_info
				: R.string.importing_sms_info);
	}

	private static String getExternalStickerDirectoryForCategoryId(
			Context context, String catId) {
		File dir = context.getExternalFilesDir(null);
		if (dir == null) {
			return null;
		}
		return dir.getPath() + HikeConstants.STICKERS_ROOT + "/" + catId;
	}

	private static String getInternalStickerDirectoryForCategoryId(
			Context context, String catId) {
		return context.getFilesDir().getPath() + HikeConstants.STICKERS_ROOT
				+ "/" + catId;
	}

	/**
	 * Returns the directory for a sticker category.
	 * 
	 * @param context
	 * @param catId
	 * @return
	 */
	public static String getStickerDirectoryForCategoryId(Context context,
			String catId) {
		/*
		 * We give a higher priority to external storage. If we find an
		 * exisiting directory in the external storage, we will return its path.
		 * Otherwise if there is an exisiting directory in internal storage, we
		 * return its path.
		 * 
		 * If the directory is not available in both cases, we return the
		 * external storage's path if external storage is available. Else we
		 * return the internal storage's path.
		 */
		boolean externalAvailable = false;
		if (getExternalStorageState() == ExternalStorageState.WRITEABLE) {
			externalAvailable = true;
			String stickerDirPath = getExternalStickerDirectoryForCategoryId(
					context, catId);

			if (stickerDirPath == null) {
				return null;
			}

			File stickerDir = new File(stickerDirPath);

			if (stickerDir.exists()) {
				return stickerDir.getPath();
			}
		}
		File stickerDir = new File(getInternalStickerDirectoryForCategoryId(
				context, catId));
		if (stickerDir.exists()) {
			return stickerDir.getPath();
		}
		if (externalAvailable) {
			return getExternalStickerDirectoryForCategoryId(context, catId);
		}
		return getInternalStickerDirectoryForCategoryId(context, catId);
	}

	public static int getResolutionId() {
		int densityMultiplierX100 = (int) (densityMultiplier * 100);
		Log.d("Stickers", "Resolutions * 100: " + densityMultiplierX100);

		if (densityMultiplierX100 > 200) {
			return HikeConstants.XXHDPI_ID;
		} else if (densityMultiplierX100 > 150) {
			return HikeConstants.XHDPI_ID;
		} else if (densityMultiplierX100 > 100) {
			return HikeConstants.HDPI_ID;
		} else if (densityMultiplierX100 > 75) {
			return HikeConstants.MDPI_ID;
		} else {
			return HikeConstants.LDPI_ID;
		}
	}

	public static boolean checkIfStickerCategoryExists(Context context,
			String categoryId) {
		String path = getStickerDirectoryForCategoryId(context, categoryId);
		if (path == null) {
			return false;
		}
		File category = new File(path + HikeConstants.LARGE_STICKER_ROOT);
		if (category.exists() && category.list().length > 0) {
			return true;
		}
		return false;
	}

	public static void saveBase64StringToFile(File file, String base64String)
			throws IOException {
		FileOutputStream fos = new FileOutputStream(file);

		byte[] b = Base64.decode(base64String, Base64.DEFAULT);
		if (b == null) {
			throw new IOException();
		}
		fos.write(b);
		fos.flush();
		fos.close();
	}

	public static void saveBitmapToFile(File file, Bitmap bitmap)
			throws IOException {
		saveBitmapToFile(file, bitmap, CompressFormat.PNG, 70);
	}

	public static void saveBitmapToFile(File file, Bitmap bitmap,
			CompressFormat compressFormat, int quality) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);

		byte[] b = bitmapToBytes(bitmap, compressFormat, quality);
		if (b == null) {
			throw new IOException();
		}
		fos.write(b);
		fos.flush();
		fos.close();
	}

	public static String getCategoryIdForIndex(int index) {
		if (index == -1 || index >= HikeMessengerApp.stickerCategories.size()) {
			return "";
		}
		return HikeMessengerApp.stickerCategories.get(index).categoryId;
	}

	public static void setupFormattedTime(TextView tv, long timeElapsed) {
		int totalSeconds = (int) (timeElapsed);
		int minutesToShow = (int) (totalSeconds / 60);
		int secondsToShow = totalSeconds % 60;

		String time = String.format("%d:%02d", minutesToShow, secondsToShow);
		tv.setText(time);
	}

	public static boolean isUserAuthenticated(Context context) {
		return !TextUtils.isEmpty(context.getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(
				HikeMessengerApp.NAME_SETTING, null));
	}

	public static void sendAppState(Context context) {
		if (!isUserAuthenticated(context)) {
			return;
		}

		JSONObject object = new JSONObject();

		try {
			object.put(HikeConstants.TYPE,
					HikeConstants.MqttMessageTypes.APP_STATE);
			if (HikeMessengerApp.currentState == CurrentState.OPENED
					|| HikeMessengerApp.currentState == CurrentState.RESUMED) {
				object.put(HikeConstants.SUB_TYPE, HikeConstants.FOREGROUND);

				JSONObject data = new JSONObject();
				data.put(HikeConstants.JUST_OPENED,
						HikeMessengerApp.currentState == CurrentState.OPENED);
				data.put(HikeConstants.BULK_LAST_SEEN, true); // adding this for
																// bulk
				object.put(HikeConstants.DATA, data);
			} else {
				object.put(HikeConstants.SUB_TYPE, HikeConstants.BACKGROUND);
			}
			HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH_LOW,
					object);
		} catch (JSONException e) {
			Log.w("AppState", "Invalid json", e);
		}

	}

	public static String getLastSeenTimeAsString(Context context,
			long lastSeenTime, int offline) {
		return getLastSeenTimeAsString(context, lastSeenTime, offline, false);
	}

	public static String getLastSeenTimeAsString(Context context,
			long lastSeenTime, int offline, boolean groupParticipant) {
		/*
		 * This refers to the setting being turned off
		 */
		if (offline == -1) {
			return null;
		}
		/*
		 * This refers to the user being online
		 */
		if (offline == 0) {
			return context.getString(R.string.online);
		}

		long lastSeenTimeMillis = lastSeenTime * 1000;
		Calendar lastSeenCalendar = Calendar.getInstance();
		lastSeenCalendar.setTimeInMillis(lastSeenTimeMillis);

		Date lastSeenDate = new Date(lastSeenTimeMillis);

		Calendar nowCalendar = Calendar.getInstance();

		int lastSeenYear = lastSeenCalendar.get(Calendar.YEAR);
		int nowYear = nowCalendar.get(Calendar.YEAR);

		int lastSeenDay = lastSeenCalendar.get(Calendar.DAY_OF_YEAR);
		int nowDay = nowCalendar.get(Calendar.DAY_OF_YEAR);

		int lastSeenDayOfMonth = lastSeenCalendar.get(Calendar.DAY_OF_MONTH);

		/*
		 * More than 7 days old.
		 */
		if ((lastSeenYear < nowYear) || ((nowDay - lastSeenDay) > 7)) {
			return context.getString(R.string.last_seen_while_ago);
		}

		boolean is24Hour = android.text.format.DateFormat
				.is24HourFormat(context);

		String lastSeen;
		/*
		 * More than 1 day old.
		 */
		if ((nowDay - lastSeenDay) > 1) {
			String format;
			if (groupParticipant) {
				format = "dd/MM/yy";
				DateFormat df = new SimpleDateFormat(format);
				lastSeen = df.format(lastSeenDate);
			} else {
				if (is24Hour) {
					format = "d'" + getDayOfMonthSuffix(lastSeenDayOfMonth)
							+ "' MMM, HH:mm";
				} else {
					format = "d'" + getDayOfMonthSuffix(lastSeenDayOfMonth)
							+ "' MMM, h:mmaaa";
				}
				DateFormat df = new SimpleDateFormat(format);
				lastSeen = context.getString(R.string.last_seen_more,
						df.format(lastSeenDate));
			}
		} else {
			String format;
			if (is24Hour) {
				format = "HH:mm";
			} else {
				format = "h:mmaaa";
			}

			DateFormat df = new SimpleDateFormat(format);
			if (groupParticipant) {
				lastSeen = (nowDay > lastSeenDay) ? context
						.getString(R.string.last_seen_yesterday_group_participant)
						: df.format(lastSeenDate);
			} else {
				lastSeen = context.getString(
						(nowDay > lastSeenDay) ? R.string.last_seen_yesterday
								: R.string.last_seen_today, df
								.format(lastSeenDate));
			}
		}

		lastSeen = lastSeen.replace("AM", "am");
		lastSeen = lastSeen.replace("PM", "pm");

		return lastSeen;

	}

	private static String getDayOfMonthSuffix(int dayOfMonth) {
		if (dayOfMonth >= 11 && dayOfMonth <= 13) {
			return "th";
		}
		switch (dayOfMonth % 10) {
		case 1:
			return "st";
		case 2:
			return "nd";
		case 3:
			return "rd";
		default:
			return "th";
		}
	}

	public static long getServerTimeOffset(Context context) {
		return context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
				0).getLong(HikeMessengerApp.SERVER_TIME_OFFSET, 0);
	}

	/**
	 * Applies the server time offset and ensures that the time does not go into
	 * the future
	 * 
	 * @param context
	 * @param time
	 * @return
	 */
	public static long applyServerTimeOffset(Context context, long time) {
		time += getServerTimeOffset(context);
		long now = System.currentTimeMillis() / 1000;
		if (time > now) {
			return now;
		} else {
			return time;
		}
	}

	public static void showTip(final Activity activity, final TipType tipType,
			final View parentView) {
		showTip(activity, tipType, parentView, null);
	}

	public static void showTip(final Activity activity, final TipType tipType,
			final View parentView, String name) {
		parentView.setVisibility(View.VISIBLE);

		View container = parentView.findViewById(R.id.tip_container);
		TextView tipText = (TextView) parentView.findViewById(R.id.tip_text);
		ImageButton closeTip = (ImageButton) parentView
				.findViewById(R.id.close);

		switch (tipType) {
		case EMOTICON:
			container.setBackgroundResource(R.drawable.bg_tip_bottom_left);
			tipText.setText(R.string.emoticons_stickers_tip);
			break;
		case LAST_SEEN:
			container.setBackgroundResource(R.drawable.bg_tip_top_left);
			tipText.setText(R.string.last_seen_tip_friends);
			break;
		case MOOD:
			container.setBackgroundResource(R.drawable.bg_tip_top_left);
			tipText.setText(R.string.moods_tip);
			break;
		case STATUS:
			container.setBackgroundResource(R.drawable.bg_tip_top_left);
			tipText.setText(activity.getString(R.string.status_tip, name));
			break;
		case WALKIE_TALKIE:
			container.setBackgroundResource(R.drawable.bg_tip_bottom_right);
			tipText.setText(R.string.walkie_talkie_tip);
			break;
		}
		if (closeTip != null) {
			closeTip.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					closeTip(tipType, parentView, activity
							.getSharedPreferences(
									HikeMessengerApp.ACCOUNT_SETTINGS, 0));
				}
			});
		}

		parentView.setTag(tipType);
	}

	public static void closeTip(TipType tipType, View parentView,
			SharedPreferences preferences) {
		parentView.setVisibility(View.GONE);

		Editor editor = preferences.edit();

		switch (tipType) {
		case EMOTICON:
			editor.putBoolean(HikeMessengerApp.SHOWN_EMOTICON_TIP, true);
			break;
		case LAST_SEEN:
			editor.putBoolean(HikeMessengerApp.SHOWN_LAST_SEEN_TIP, true);
			break;
		case MOOD:
			editor.putBoolean(HikeMessengerApp.SHOWN_MOODS_TIP, true);
			break;
		case STATUS:
			editor.putBoolean(HikeMessengerApp.SHOWN_STATUS_TIP, true);
			break;
		case WALKIE_TALKIE:
			editor.putBoolean(HikeMessengerApp.SHOWN_WALKIE_TALKIE_TIP, true);
			break;
		}

		editor.commit();
	}

	public static void blockOrientationChange(Activity activity) {
		boolean isPortrait = activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
		activity.setRequestedOrientation(isPortrait ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
				: ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	}

	public static void unblockOrientationChange(Activity activity) {
		if (activity == null) {
			return;
		}
		activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
	}

	public static String getMessageDisplayText(ConvMessage convMessage,
			Context context) {
		if (convMessage.isFileTransferMessage()) {
			HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);

			String message = HikeFileType.getFileTypeMessage(context,
					hikeFile.getHikeFileType(), false)
					+ ". "
					+ AccountUtils.fileTransferBaseViewUrl
					+ hikeFile.getFileKey();
			return message;

		} else if (convMessage.isStickerMessage()) {
			Sticker sticker = convMessage.getMetadata().getSticker();

			String stickerId = sticker.getStickerId();
			String stickerUrlId = stickerId
					.substring(0, stickerId.indexOf("_"));

			String message = context.getString(
					R.string.sent_sticker_sms,
					String.format(AccountUtils.stickersUrl,
							sticker.getCategoryId(), stickerUrlId));
			return message;
		}
		return convMessage.getMessage();
	}

	public static void deleteFile(File file) {
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				deleteFile(f);
			}
		}
		file.delete();
	}

	public static void sendLogEvent(JSONObject data) {
		JSONObject object = new JSONObject();
		try {
			data.put(HikeConstants.LogEvent.TAG, HikeConstants.LOGEVENT_TAG);

			object.put(HikeConstants.TYPE,
					HikeConstants.MqttMessageTypes.ANALYTICS_EVENT);
			object.put(HikeConstants.DATA, data);

			HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH,
					object);
		} catch (JSONException e) {
			Log.w("LogEvent", e);
		}
	}

	private static void sendSMSSyncLogEvent(boolean syncing) {
		JSONObject data = new JSONObject();
		JSONObject metadata = new JSONObject();

		try {
			metadata.put(HikeConstants.PULL_OLD_SMS, syncing);

			data.put(HikeConstants.METADATA, metadata);
			data.put(HikeConstants.SUB_TYPE, HikeConstants.SMS);

			sendLogEvent(data);
		} catch (JSONException e) {
			Log.w("LogEvent", e);
		}

	}

	public static void sendDefaultSMSClientLogEvent(boolean defaultClient) {
		JSONObject data = new JSONObject();
		JSONObject metadata = new JSONObject();

		try {
			metadata.put(HikeConstants.UNIFIED_INBOX, defaultClient);

			data.put(HikeConstants.METADATA, metadata);
			data.put(HikeConstants.SUB_TYPE, HikeConstants.SMS);

			sendLogEvent(data);
		} catch (JSONException e) {
			Log.w("LogEvent", e);
		}

	}

	public static void sendFreeSmsLogEvent(boolean freeSmsOn) {
		JSONObject data = new JSONObject();
		JSONObject metadata = new JSONObject();

		try {
			metadata.put(HikeConstants.FREE_SMS_ON, freeSmsOn);

			data.put(HikeConstants.METADATA, metadata);
			data.put(HikeConstants.SUB_TYPE, HikeConstants.SMS);

			sendLogEvent(data);
		} catch (JSONException e) {
			Log.w("LogEvent", e);
		}

	}

	public static void sendNativeSmsLogEvent(boolean nativeSmsOn) {
		JSONObject data = new JSONObject();
		JSONObject metadata = new JSONObject();

		try {
			metadata.put(HikeConstants.NATIVE_SMS, nativeSmsOn);

			data.put(HikeConstants.METADATA, metadata);
			data.put(HikeConstants.SUB_TYPE, HikeConstants.SMS);

			sendLogEvent(data);
		} catch (JSONException e) {
			Log.w("LogEvent", e);
		}

	}

	public static JSONObject getJSONfromURL(String url) {

		// initialize
		InputStream is = null;
		String result = "";
		JSONObject jObject = null;

		// http post
		try {
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(url);
			HttpResponse response = httpclient.execute(httppost);
			HttpEntity entity = response.getEntity();
			is = entity.getContent();

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					is, "UTF-8"), 8);
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			is.close();
			result = sb.toString();
		} catch (Exception e) {
			Log.e("LogEvent", "Error converting result " + e.toString());
		}

		// try parse the string to a JSON object
		try {
			jObject = new JSONObject(result);
		} catch (JSONException e) {
			Log.e("LogEvent", "Error parsing data " + e.toString());
		}

		return jObject;
	}

	public static Bitmap drawableToBitmap(Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable) drawable).getBitmap();
		}
		/*
		 * http://developer.android.com/reference/android/graphics/Bitmap.Config.
		 * html
		 */
		Bitmap bitmap = Bitmap.createBitmap((int) (48 * densityMultiplier),
				(int) (48 * densityMultiplier), Config.ARGB_8888);

		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return bitmap;
	}

	public static boolean isHoneycombOrHigher() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	}

	public static void executeAsyncTask(AsyncTask<Void, Void, Void> asyncTask) {
		if (isHoneycombOrHigher()) {
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			asyncTask.execute();
		}
	}

	public static void executeFtResultAsyncTask(
			AsyncTask<Void, Void, FTResult> asyncTask) {
		if (isHoneycombOrHigher()) {
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			asyncTask.execute();
		}
	}

	public static void executeIntProgFtResultAsyncTask(
			AsyncTask<Void, Integer, FTResult> asyncTask) {
		if (isHoneycombOrHigher()) {
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			asyncTask.execute();
		}
	}

	public static void executeBoolResultAsyncTask(
			AsyncTask<Void, Void, Boolean> asyncTask) {
		if (isHoneycombOrHigher()) {
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			asyncTask.execute();
		}
	}

	public static void executeHttpTask(
			AsyncTask<HikeHttpRequest, Integer, Boolean> asyncTask,
			HikeHttpRequest... hikeHttpRequests) {
		if (isHoneycombOrHigher()) {
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
					hikeHttpRequests);
		} else {
			asyncTask.execute(hikeHttpRequests);
		}
	}

	public static void executeSignupTask(
			AsyncTask<Void, SignupTask.StateValue, Boolean> asyncTask) {
		if (isHoneycombOrHigher()) {
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			asyncTask.execute();
		}
	}

	public static void executeLongResultTask(
			AsyncTask<Void, Void, Long> asyncTask) {
		if (isHoneycombOrHigher()) {
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			asyncTask.execute();
		}
	}

	public static void executeContactListResultTask(
			AsyncTask<Void, Void, List<Pair<AtomicBoolean, ContactInfo>>> asyncTask) {
		if (isHoneycombOrHigher()) {
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			asyncTask.execute();
		}
	}

	public static void executeContactInfoListResultTask(
			AsyncTask<Void, Void, List<ContactInfo>> asyncTask) {
		if (isHoneycombOrHigher()) {
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			asyncTask.execute();
		}
	}

	public static void executeStringResultTask(
			AsyncTask<Void, Void, String> asyncTask) {
		if (isHoneycombOrHigher()) {
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			asyncTask.execute();
		}
	}

	public static void executeSMSSyncStateResultTask(
			AsyncTask<Void, Void, SMSSyncState> asyncTask) {
		if (isHoneycombOrHigher()) {
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			asyncTask.execute();
		}
	}

	public static void executeConvAsyncTask(
			AsyncTask<Conversation, Void, Conversation[]> asyncTask,
			Conversation... conversations) {
		if (Utils.isHoneycombOrHigher()) {
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
					conversations);
		} else {
			asyncTask.execute(conversations);
		}
	}

	public static Bitmap returnScaledBitmap(Bitmap src, Context context) {
		Resources res = context.getResources();
		if (isHoneycombOrHigher()) {
			int height = (int) res
					.getDimension(android.R.dimen.notification_large_icon_height);
			int width = (int) res
					.getDimension(android.R.dimen.notification_large_icon_width);
			return src = Bitmap.createScaledBitmap(src, width, height, false);
		} else
			return src;

	}

	public static boolean getSendSmsPref(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(HikeConstants.SEND_SMS_PREF, false);
	}

	public static boolean isFilenameValid(String file) {
		File f = new File(file);
		try {
			f.getCanonicalPath();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public static void resetUnseenStatusCount(SharedPreferences prefs) {
		Editor editor = prefs.edit();
		editor.putInt(HikeMessengerApp.UNSEEN_STATUS_COUNT, 0);
		editor.putInt(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, 0);
		editor.commit();
	}

	public static boolean shouldIncrementCounter(ConvMessage convMessage) {
		return !convMessage.isSent()
				&& convMessage.getState() == State.RECEIVED_UNREAD
				&& convMessage.getParticipantInfoState() != ParticipantInfoState.STATUS_MESSAGE;
	}

	public static Intent createIntentForConversation(Context context,
			Conversation conversation) {
		Intent intent = new Intent(context, ChatThread.class);
		if (conversation.getContactName() != null) {
			intent.putExtra(HikeConstants.Extras.NAME,
					conversation.getContactName());
		}
		intent.putExtra(HikeConstants.Extras.MSISDN, conversation.getMsisdn());
		return intent;
	}

	public static void createShortcut(Activity activity, Conversation conv) {
		Intent shortcutIntent = Utils.createIntentForConversation(activity,
				conv);
		Intent intent = new Intent();
		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, conv.getLabel());
		Drawable d = IconCacheManager.getInstance().getIconForMSISDN(
				conv.getMsisdn());
		Bitmap bitmap = ((BitmapDrawable) d).getBitmap();

		int dimension = (int) (Utils.densityMultiplier * 48);

		Bitmap scaled = Bitmap.createScaledBitmap(bitmap, dimension, dimension,
				false);
		bitmap = null;
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, scaled);
		intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
		activity.sendBroadcast(intent);
	}

	public static void onCallClicked(Activity activity,
			final String mContactNumber) {
		final Activity mActivity = activity;
		final SharedPreferences settings = activity.getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		if (!settings.getBoolean(HikeConstants.NO_CALL_ALERT_CHECKED, false)) {
			final Dialog dialog = new Dialog(activity,
					R.style.Theme_CustomDialog);
			dialog.setContentView(R.layout.operator_alert_popup);
			dialog.setCancelable(true);

			TextView header = (TextView) dialog.findViewById(R.id.header);
			TextView body = (TextView) dialog.findViewById(R.id.body_text);
			Button btnOk = (Button) dialog.findViewById(R.id.btn_ok);
			Button btnCancel = (Button) dialog.findViewById(R.id.btn_cancel);

			header.setText(R.string.call_not_free_head);
			body.setText(R.string.call_not_free_body);

			btnCancel.setText(R.string.cancel);
			btnOk.setText(R.string.call);

			CheckBox checkBox = (CheckBox) dialog
					.findViewById(R.id.body_checkbox);
			checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					Editor editor = settings.edit();
					editor.putBoolean(HikeConstants.NO_CALL_ALERT_CHECKED,
							isChecked);
					editor.commit();
				}
			});
			checkBox.setText(activity.getResources().getString(
					R.string.not_show_call_alert_msg));

			btnOk.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Utils.logEvent(mActivity, HikeConstants.LogEvent.MENU_CALL);
					Intent callIntent = new Intent(Intent.ACTION_CALL);
					callIntent.setData(Uri.parse("tel:" + mContactNumber));
					mActivity.startActivity(callIntent);
					dialog.dismiss();
				}
			});

			btnCancel.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					dialog.dismiss();
				}
			});

			dialog.show();
		} else {
			Utils.logEvent(activity, HikeConstants.LogEvent.MENU_CALL);
			Intent callIntent = new Intent(Intent.ACTION_CALL);
			callIntent.setData(Uri.parse("tel:" + mContactNumber));
			activity.startActivity(callIntent);
		}
	}

	public static String getFormattedDateTimeFromTimestamp(long milliSeconds,
			Locale current) {
		String dateFormat = "dd/MM/yyyy hh:mm:ss a";
		DateFormat formatter = new SimpleDateFormat(dateFormat, current);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(milliSeconds * 1000);
		return formatter.format(calendar.getTime());
	}

	public static void sendFTUELogEvent(String key) {
		sendFTUELogEvent(key, null);
	}

	public static void sendFTUELogEvent(String key, String msisdn) {
		try {
			JSONObject data = new JSONObject();
			data.put(HikeConstants.SUB_TYPE, HikeConstants.UI_EVENT);

			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_TYPE, HikeConstants.LogEvent.CLICK);
			metadata.put(HikeConstants.EVENT_KEY, key);

			if (!TextUtils.isEmpty(msisdn)) {
				JSONArray msisdns = new JSONArray();
				msisdns.put(msisdn);

				metadata.put(HikeConstants.TO, msisdns);
			}

			data.put(HikeConstants.METADATA, metadata);

			sendLogEvent(data);
		} catch (JSONException e) {
			Log.w("LE", "Invalid json");
		}
	}

	public static Bitmap returnBigPicture(ConvMessage convMessage,
			Context context) {

		HikeFile hikeFile = null;
		Bitmap bigPictureImage = null;

		// Check if this is a file transfer message of image type
		// construct a bitmap only if the big picture condition matches
		if (convMessage.isFileTransferMessage()) {
			hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			if (hikeFile != null) {
				if (hikeFile.getHikeFileType() == HikeFileType.IMAGE
						&& hikeFile.wasFileDownloaded()
						&& !HikeMessengerApp.fileTransferTaskMap
								.containsKey(convMessage.getMsgID())
						&& hikeFile.getThumbnail() != null) {
					final String filePath = hikeFile.getFilePath(); // check
					bigPictureImage = BitmapFactory.decodeFile(filePath);
				}
			}

		}
		// check if this is a sticker message and find if its non-downloaded or
		// non present.
		if (convMessage.isStickerMessage()) {
			final Sticker sticker = convMessage.getMetadata().getSticker();
			/*
			 * If this is the first category, then the sticker are a part of the
			 * app bundle itself
			 */
			if (sticker.getStickerIndex() != -1) {

				int resourceId = 0;

				if (sticker.getCategoryIndex() == 0) {
					resourceId = EmoticonConstants.LOCAL_STICKER_RES_IDS_1[sticker
							.getStickerIndex()];
				} else if (sticker.getCategoryIndex() == 1) {
					resourceId = EmoticonConstants.LOCAL_STICKER_RES_IDS_2[sticker
							.getStickerIndex()];
				}

				if (resourceId > 0) {
					final Drawable dr = context.getResources().getDrawable(
							resourceId);
					bigPictureImage = Utils.drawableToBitmap(dr);
				}

			} else {
				final String filePath = sticker.getStickerPath(context);
				if (!TextUtils.isEmpty(filePath)) {
					bigPictureImage = BitmapFactory.decodeFile(filePath);
				}
			}
		}
		return bigPictureImage;
	}

	public static void resetUpdateParams(SharedPreferences prefs) {
		Editor prefEditor = prefs.edit();
		prefEditor.remove(HikeMessengerApp.DEVICE_DETAILS_SENT);
		prefEditor.remove(HikeMessengerApp.UPGRADE_RAI_SENT);
		prefEditor.commit();
	}
}
