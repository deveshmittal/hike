package com.bsb.hike.chatthread;

import java.io.File;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class ChatThreadUtils
{
	private static final String TAG = "ChatThreadUtils";

	protected static void playUpDownAnimation(Context context, final View view)
	{
		if (view == null)
		{
			return;
		}

		Animation an = AnimationUtils.loadAnimation(context, R.anim.down_up_up_part);
		an.setAnimationListener(new AnimationListener()
		{

			@Override
			public void onAnimationStart(Animation animation)
			{
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
			}

			@Override
			public void onAnimationEnd(Animation animation)
			{
				view.setVisibility(View.GONE);
			}
		});
		view.startAnimation(an);
	}

	/**
	 * This method is used to add pin related parameters in the convMessage
	 * 
	 * @param convMessage
	 */
	protected static void modifyMessageToPin(Context context, ConvMessage convMessage)
	{
		convMessage.setMessageType(HikeConstants.MESSAGE_TYPE.TEXT_PIN);
		JSONObject jsonObject = new JSONObject();
		try
		{
			jsonObject.put(HikeConstants.PIN_MESSAGE, 1);
			convMessage.setMetadata(jsonObject);
			convMessage.setHashMessage(HikeConstants.HASH_MESSAGE_TYPE.HASH_PIN_MESSAGE);
		}
		catch (JSONException je)
		{
			Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show();
			je.printStackTrace();
		}
	}

	protected static boolean checkMessageTypeFromHash(Context context, ConvMessage convMessage, String hashType)
	{
		Pattern p = Pattern.compile("(?i)" + hashType + ".*", Pattern.DOTALL);
		String message = convMessage.getMessage();
		if (p.matcher(message).matches())
		{

			convMessage.setMessage(message.substring(hashType.length()).trim());

			if (TextUtils.isEmpty(convMessage.getMessage()))
			{
				Toast.makeText(context, R.string.text_empty_error, Toast.LENGTH_SHORT).show();
				return false;
			}

			return true;
		}
		return false;
	}

	protected static void doBulkMqttPublish(JSONArray ids, String msisdn)
	{
		JSONObject jsonObject = new JSONObject();

		try
		{
			jsonObject.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.MESSAGE_READ);
			jsonObject.put(HikeConstants.TO, msisdn);
			jsonObject.put(HikeConstants.DATA, ids);
		}

		catch (JSONException e)
		{
			Logger.wtf(TAG, "Exception in Adding bulk messages : " + e.toString());
		}

		HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, jsonObject);
		HikeMessengerApp.getPubSub().publish(HikePubSub.MSG_READ, msisdn);
	}

	protected static void clearTempData(Context context)
	{
		Editor editor = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE).edit();
		editor.remove(HikeMessengerApp.TEMP_NAME);
		editor.remove(HikeMessengerApp.TEMP_NUM);
		editor.commit();
	}

	protected static void initiateFileTransferFromIntentData(Context context, String msisdn, String fileType, String filePath, boolean convOnHike)
	{
		initiateFileTransferFromIntentData(context, msisdn, fileType, filePath, null, false, -1, convOnHike);
	}

	protected static void initiateFileTransferFromIntentData(Context context, String msisdn, String fileType, String filePath, String fileKey, boolean isRecording,
			long recordingDuration, boolean convOnHike)
	{
		HikeFileType hikeFileType = HikeFileType.fromString(fileType, isRecording);

		Logger.d(TAG, "Forwarding file- Type:" + fileType + " Path: " + filePath);

		if (Utils.isPicasaUri(filePath))
		{
			FileTransferManager.getInstance(context).uploadFile(Uri.parse(filePath), hikeFileType, msisdn, convOnHike);
		}
		else
		{
			initialiseFileTransfer(context, msisdn, filePath, fileKey, hikeFileType, fileType, isRecording, recordingDuration, true, convOnHike);
		}
	}

	protected static void initialiseFileTransfer(Context context, String msisdn, String filePath, String fileKey, HikeFileType hikeFileType, String fileType, boolean isRecording,
			long recordingDuration, boolean isForwardingFile, boolean convOnHike)
	{
		clearTempData(context);

		if (filePath == null)
		{
			Toast.makeText(context, R.string.unknown_msg, Toast.LENGTH_SHORT).show();
			return;
		}
		File file = new File(filePath);
		Logger.d(TAG, "File size: " + file.length() + " File name: " + file.getName());

		if (HikeConstants.MAX_FILE_SIZE != -1 && HikeConstants.MAX_FILE_SIZE < file.length())
		{
			Toast.makeText(context, R.string.max_file_size, Toast.LENGTH_SHORT).show();
			return;
		}
		FileTransferManager.getInstance(context).uploadFile(msisdn, file, fileKey, fileType, hikeFileType, isRecording, isForwardingFile, convOnHike, recordingDuration);
	}
	
	protected static void onShareFile(Context context, String msisdn, Intent intent, boolean isConvOnHike)
	{
		String fileKey = null;

		if (intent.hasExtra(HikeConstants.Extras.FILE_KEY))
		{
			fileKey = intent.getStringExtra(HikeConstants.Extras.FILE_KEY);
		}
		String filePath = intent.getStringExtra(HikeConstants.Extras.FILE_PATH);
		String fileType = intent.getStringExtra(HikeConstants.Extras.FILE_TYPE);

		boolean isRecording = false;
		long recordingDuration = -1;

		if (intent.hasExtra(HikeConstants.Extras.RECORDING_TIME))
		{
			recordingDuration = intent.getLongExtra(HikeConstants.Extras.RECORDING_TIME, -1);
			isRecording = true;
			fileType = HikeConstants.VOICE_MESSAGE_CONTENT_TYPE;
		}

		if (filePath == null)
		{
			Toast.makeText(context, R.string.unknown_msg, Toast.LENGTH_SHORT).show();
		}
		else
		{
			ChatThreadUtils.initiateFileTransferFromIntentData(context, msisdn, fileType, filePath, fileKey, isRecording, recordingDuration, isConvOnHike);
		}
	}

	protected static boolean shouldShowLastSeen(Context context, FavoriteType mFavoriteType, boolean convOnHike)
	{
		if ((mFavoriteType == FavoriteType.FRIEND || mFavoriteType == FavoriteType.REQUEST_RECEIVED || mFavoriteType == FavoriteType.REQUEST_RECEIVED_REJECTED) && convOnHike)
		{
			return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.LAST_SEEN_PREF, true);
		}
		return false;
	}
	
	protected static boolean checkNetworkError()
	{
		return HikeMessengerApp.networkError;
	}
	
	protected static void initialiseLocationTransfer(Context context, String msisdn, double latitude, double longitude, int zoomLevel, boolean convOnHike)
	{
		FileTransferManager.getInstance(context).uploadLocation(msisdn, latitude, longitude, zoomLevel, convOnHike);
	}

	protected static void initialiseContactTransfer(Context context, String msisdn, JSONObject contactJson, boolean convOnHike)
	{
		Logger.i(TAG, "initiate contact transfer " + contactJson.toString());
		FileTransferManager.getInstance(context).uploadContact(msisdn, contactJson, convOnHike);
	}
	
}
