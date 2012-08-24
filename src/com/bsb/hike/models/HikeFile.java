package com.bsb.hike.models;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;

public class HikeFile 
{
	public static enum HikeFileType
	{
		PROFILE,
		IMAGE,
		VIDEO,
		AUDIO;
		
		public static HikeFileType fromString(String fileTypeString)
		{
			if(fileTypeString.startsWith("video"))
			{
				return HikeFileType.VIDEO;
			}
			else if(fileTypeString.startsWith("audio"))
			{
				return HikeFileType.AUDIO;
			}
			else
			{
				return HikeFileType.IMAGE;
			}
		}
		
		public static String toString(HikeFileType hikeFileType)
		{
			if (hikeFileType == PROFILE ||
					hikeFileType == IMAGE)
			{
				return "image/*";
			}
			else if (hikeFileType == VIDEO)
			{
				return "video/*";
			}
			else if (hikeFileType == AUDIO)
			{
				return "audio/*";
			}
			return null;
		}
	}

	private String fileName;
	private String fileTypeString;
	private String thumbnailString;
	private Drawable thumbnail;
	private String fileKey;
	private HikeFileType hikeFileType;

	public HikeFile(JSONObject fileJSON)
	{
		this.fileName = fileJSON.optString(HikeConstants.FILE_NAME);
		this.fileTypeString = fileJSON.optString(HikeConstants.CONTENT_TYPE);
		this.thumbnailString = fileJSON.optString(HikeConstants.THUMBNAIL);
		this.thumbnail = thumbnail == null ? Utils.stringToDrawable(thumbnailString) : thumbnail;
		this.fileKey = fileJSON.optString(HikeConstants.FILE_KEY);
		this.hikeFileType = HikeFileType.fromString(fileTypeString);
	}

	public HikeFile(String fileName, String fileTypeString, String thumbnailString, Bitmap thumbnail)
	{
		this.fileName = fileName;
		this.fileTypeString = fileTypeString;
		this.hikeFileType = HikeFileType.fromString(fileTypeString);
		this.thumbnailString = thumbnailString;
		this.thumbnail = new BitmapDrawable(thumbnail);
	}

	public JSONObject serialize()
	{
		try 
		{
			JSONObject fileJSON = new JSONObject();
			fileJSON.putOpt(HikeConstants.FILE_NAME, fileName);
			fileJSON.putOpt(HikeConstants.CONTENT_TYPE, fileTypeString);
			fileJSON.putOpt(HikeConstants.FILE_KEY, fileKey);
			fileJSON.putOpt(HikeConstants.THUMBNAIL, thumbnailString);

			return fileJSON;
		} 
		catch (JSONException e) 
		{
			Log.e(getClass().getSimpleName(), "Invalid JSON", e);
		}
		return null;
	}

	public String getFileName() 
	{
		return fileName;
	}

	public String getFileTypeString() 
	{
		return fileTypeString;
	}

	public String getThumbnailString() 
	{
		return thumbnailString;
	}

	public void setThumbnail(Drawable thumbnail)
	{
		this.thumbnail = thumbnail;
	}

	public Drawable getThumbnail() 
	{
		return thumbnail;
	}

	public void setFileKey(String fileKey)
	{
		this.fileKey = fileKey;
	}
	
	public String getFileKey() 
	{
		return fileKey;
	}

	public HikeFileType getHikeFileType()
	{
		return hikeFileType;
	}

	public boolean wasFileDownloaded()
	{
		if(Utils.getExternalStorageState() == ExternalStorageState.NONE)
		{
			return false;
		}
		return Utils.getOutputMediaFile(hikeFileType, fileName, fileKey).exists();
	}

	public String getFilePath()
	{
		if(Utils.getExternalStorageState() == ExternalStorageState.NONE)
		{
			return null;
		}
		return Utils.getOutputMediaFile(hikeFileType, fileName, fileKey).getPath();
	}
}
