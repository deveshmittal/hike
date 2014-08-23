package com.bsb.hike.models;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;

import android.os.Parcel;
import android.os.Parcelable;

public class HikeSharedFile extends HikeFile implements Parcelable
{
	private long msgId;

	private String msisdn;

	private long timeStamp;
	
	private JSONObject fileJSON;

	public HikeSharedFile(JSONObject fileJSON, boolean isSent, long msgId, String msisdn, long timeStamp)
	{
		super(fileJSON, isSent);

		this.fileJSON = fileJSON;
		
		this.msgId = msgId;

		this.msisdn = msisdn;

		this.timeStamp = timeStamp;
	}

	public long getMsgId()
	{
		return msgId;
	}

	public void setMsgId(long msgId)
	{
		this.msgId = msgId;
	}

	public String getMsisdn()
	{
		return msisdn;
	}

	public void setMsisdn(String msisdn)
	{
		this.msisdn = msisdn;
	}

	public long getTimeStamp()
	{
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp)
	{
		this.timeStamp = timeStamp;
	}

	public JSONObject getFileJSON()
	{
		return fileJSON;
	}

	public void setFileJSON(JSONObject fileJSON)
	{
		this.fileJSON = fileJSON;
	}

	public String  getImageLoaderKey(boolean large)
	{
		String key = this.getExactFilePath() + ""+"::" + this.getHikeFileType().ordinal();
		return large ? key + "::"+HikeConstants.LARGE : key;  
	}

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeLong(msgId);
		dest.writeString(msisdn);
		dest.writeLong(timeStamp);
		dest.writeInt(this.isSent() ? 1 : 0);
		dest.writeString(fileJSON.toString());
	}
	

	public static final Creator<HikeSharedFile> CREATOR = new Creator<HikeSharedFile>()
	{
		@Override
		public HikeSharedFile[] newArray(int size)
		{
			return new HikeSharedFile[size];
		}

		@Override
		public HikeSharedFile createFromParcel(Parcel source)
		{

			long msgId = source.readLong();
			String msisdn = source.readString();
			long timeStamp = source.readLong();
			boolean isSent = source.readInt() == 1;
			String fileJsonString = source.readString();
			try
			{
				JSONObject fileJSON = new JSONObject(fileJsonString);
				return new HikeSharedFile(fileJSON, isSent, msgId, msisdn, timeStamp);
			}
			catch (JSONException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
	};

}
