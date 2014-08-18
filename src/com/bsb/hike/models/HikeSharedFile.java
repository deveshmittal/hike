package com.bsb.hike.models;

import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

public class HikeSharedFile extends HikeFile implements Parcelable
{
	private long msgId;

	private String msisdn;

	private long timeStamp;

	public HikeSharedFile(JSONObject fileJSON, boolean isSent, long msgId, String msisdn, long timeStamp)
	{
		super(fileJSON, isSent);

		this.msgId = msgId;

		this.msisdn = msisdn;

		this.timeStamp = timeStamp;
	}

	public HikeSharedFile(Parcel source)
	{
		setMsgId(source.readLong());
		setMsisdn(source.readString());
		setTimeStamp(source.readLong());
		setFileKey(source.readString());
		setFileName(source.readString());
		setSourceFilePath(source.readString());
		setFileTypeString(source.readString());
		setHikeFileType(HikeFileType.values()[source.readInt()]);
		setRecordingDuration(source.readLong());
		setSent(source.readInt() == 1);
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
		dest.writeString(this.getFileKey());
		dest.writeString(this.getFileName());
		dest.writeString(this.getFilePath());
		dest.writeString(this.getFileTypeString());
		dest.writeInt(this.getHikeFileType().ordinal());
		dest.writeLong(this.getRecordingDuration());
		dest.writeInt(this.isSent() ? 1 : 0);
	}

}
