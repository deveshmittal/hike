package com.bsb.hike.filetransfer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.utils.Logger;

public class FTAnalyticEvents
{
	public int mRetryCount;

	public int mPauseCount;

	public String mNetwork;

	public int mAttachementType;

	public static final String FT_RETRY_COUNT = "rc";

	public static final String FT_PAUSE_COUNT = "pc";

	public static final String FT_NETWORK_TYPE = "nw";

	public static final String FT_ATTACHEMENT_TYPE = "at";

	public static final String FT_STATUS = "s";
	
	public static final int FT_SUCCESS = 0;

	public static final int FT_FAILED = 1;

	public static final int GALLERY_ATTACHEMENT = 0;

	public static final int FILE_ATTACHEMENT = 1;

	public static final int VIDEO_ATTACHEMENT = 2;

	public static final int CAMERA_ATTACHEMENT = 3;

	public static final int AUDIO_ATTACHEMENT = 4;

	public static final int DOWNLOAD_ATTACHEMENT = 5;

	public static final int OTHER_ATTACHEMENT = 6;
	
	public FTAnalyticEvents(JSONObject logMetaData)
	{
		if(logMetaData == null)
			return;
		try
		{
			this.mAttachementType = logMetaData.getInt(FT_ATTACHEMENT_TYPE);
			this.mNetwork = logMetaData.getString(FT_NETWORK_TYPE);
			this.mRetryCount = logMetaData.getInt(FT_RETRY_COUNT);
			this.mPauseCount = logMetaData.getInt(FT_PAUSE_COUNT);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}
	
	public FTAnalyticEvents()
	{
	}

	public void saveAnalyticEvent(File logEventFile)
	{
		FileWriter file = null;
		try
		{
			file = new FileWriter(logEventFile);
			JSONObject metadata = new JSONObject();
			metadata.put(FT_ATTACHEMENT_TYPE, this.mAttachementType);
			metadata.put(FT_NETWORK_TYPE, this.mNetwork);
			metadata.put(FT_RETRY_COUNT, this.mRetryCount);
			metadata.put(FT_PAUSE_COUNT, this.mPauseCount);
			Logger.d("FTAnalyticEvents", "write data = " + metadata.toString());
			file.write(metadata.toString());
		}
		catch (IOException i)
		{
			i.printStackTrace();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if(file != null)
					file.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public static FTAnalyticEvents getAnalyticEvents(File mFile)
	{
		if (mFile == null || !mFile.exists())
			return new FTAnalyticEvents();

		FileReader file = null;
		StringBuffer sb = new StringBuffer();
		BufferedReader bufReader = null;
		FTAnalyticEvents ftAnalyticEvent = null;
		try
		{
			file = new FileReader(mFile);
			bufReader = new BufferedReader(file);
			int s = 0;
			while ((s = bufReader.read())!=-1) {
	            sb.append((char)s);
			}
			JSONObject data = new JSONObject(sb.toString());
			ftAnalyticEvent = new FTAnalyticEvents(data);
		}
		catch (IOException i)
		{
			i.printStackTrace();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if(bufReader != null)
					bufReader.close();
				if(file != null)
					file.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return ftAnalyticEvent != null ? ftAnalyticEvent : new FTAnalyticEvents();
	}

	/*
	 * We send an event every time user transfer file whether it is succeeded or canceled.
	 */
	public void sendFTSuccessFailureEvent(String network,  int fileSize, int status)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(FT_ATTACHEMENT_TYPE, this.mAttachementType);
			metadata.put(FT_NETWORK_TYPE, network);
			metadata.put(FT_RETRY_COUNT, this.mRetryCount);
			metadata.put(FT_PAUSE_COUNT, this.mPauseCount);
			metadata.put(HikeConstants.FILE_SIZE, fileSize);
			metadata.put(FT_STATUS, status);

			JSONObject data = new JSONObject();
			data.put(HikeConstants.C_TIME_STAMP, System.currentTimeMillis());
			data.put(HikeConstants.SUB_TYPE, HikeConstants.UI_EVENT);
			data.put(HikeConstants.METADATA, metadata);
			data.put(HikeConstants.LogEvent.TAG, HikeConstants.LogEvent.FILE_TRANSFER_STATUS);

			JSONObject object = new JSONObject();
			object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.ANALYTICS_EVENT);
			object.put(HikeConstants.DATA, data);

			HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, object);
		}
		catch (JSONException e)
		{
			Logger.e("FTAnalyticsEvent", "Exception is sending analytics event for file transfer", e);
		}
	}

	public String toString()
	{
		return "AttachementType : " + mAttachementType + ", NetworkType : " + mNetwork + ", RetryCount : " + mRetryCount + ", PauseCount : " + mPauseCount;
	}
}
