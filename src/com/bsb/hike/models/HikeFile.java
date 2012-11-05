package com.bsb.hike.models;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
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
		AUDIO,
		LOCATION;
		
		public static HikeFileType fromString(String fileTypeString)
		{
			if("video".startsWith(fileTypeString))
			{
				return HikeFileType.VIDEO;
			}
			else if("audio".startsWith(fileTypeString))
			{
				return HikeFileType.AUDIO;
			}
			else if(HikeConstants.LOCATION_CONTENT_TYPE.startsWith(fileTypeString))
			{
				return HikeFileType.LOCATION;
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
			else if (hikeFileType == LOCATION)
			{
				return HikeConstants.LOCATION_CONTENT_TYPE;
			}
			return null;
		}

		public static String toProperString(HikeFileType hikeFileType)
		{
			if (hikeFileType == PROFILE ||
					hikeFileType == IMAGE)
			{
				return HikeConstants.IMAGE;
			}
			else if (hikeFileType == VIDEO)
			{
				return HikeConstants.VIDEO;
			}
			else if (hikeFileType == AUDIO)
			{
				return HikeConstants.AUDIO;
			}
			else if (hikeFileType == LOCATION)
			{
				return HikeConstants.LOCATION_FILE_NAME;
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
	private File file;
	private int latitude;
	private int longitude;
	private int zoomLevel;
	private String address;

	public HikeFile(JSONObject fileJSON)
	{
		this.fileName = fileJSON.optString(HikeConstants.FILE_NAME);
		this.fileTypeString = fileJSON.optString(HikeConstants.CONTENT_TYPE);
		this.thumbnailString = fileJSON.optString(HikeConstants.THUMBNAIL);
		this.thumbnail = thumbnail == null ? Utils.stringToDrawable(thumbnailString) : thumbnail;
		this.fileKey = fileJSON.optString(HikeConstants.FILE_KEY);
		this.hikeFileType = HikeFileType.fromString(fileTypeString);
		this.latitude = fileJSON.optInt(HikeConstants.LATITUDE);
		this.longitude = fileJSON.optInt(HikeConstants.LONGITUDE);
		this.zoomLevel = fileJSON.optInt(HikeConstants.ZOOM_LEVEL, HikeConstants.DEFAULT_ZOOM_LEVEL);
		this.address = fileJSON.optString(HikeConstants.ADDRESS);
		this.file = TextUtils.isEmpty(this.fileKey) ? null : Utils.getOutputMediaFile(hikeFileType, fileName, fileKey);
		if(this.file != null)
		{
			//Update the file name to prevent duplicacy
			this.fileName = this.file.getName();
		}
	}

	public HikeFile(String fileName, String fileTypeString, String thumbnailString, Bitmap thumbnail)
	{
		this.fileName = fileName;
		this.fileTypeString = fileTypeString;
		this.hikeFileType = HikeFileType.fromString(fileTypeString);
		this.thumbnailString = thumbnailString;
		this.thumbnail = new BitmapDrawable(thumbnail);
	}

	public HikeFile(int latitude, int longitude, int zoomLevel, String address, String thumbnailString, Bitmap thumbnail)
	{
		this.latitude = latitude;
		this.longitude = longitude;
		this.zoomLevel = zoomLevel;
		this.address = address;
		this.thumbnailString = thumbnailString;
		this.thumbnail = new BitmapDrawable(thumbnail);
	}

	public JSONObject serialize()
	{
		try 
		{
			JSONObject fileJSON = new JSONObject();
			fileJSON.putOpt(HikeConstants.CONTENT_TYPE, fileTypeString);
			if(!HikeConstants.LOCATION_CONTENT_TYPE.equals(fileTypeString))
			{
				fileJSON.putOpt(HikeConstants.FILE_NAME, fileName);
				fileJSON.putOpt(HikeConstants.FILE_KEY, fileKey);
				fileJSON.putOpt(HikeConstants.THUMBNAIL, thumbnailString);
			}
			else
			{
				fileJSON.putOpt(HikeConstants.LATITUDE, latitude);
				fileJSON.putOpt(HikeConstants.LONGITUDE, longitude);
				fileJSON.putOpt(HikeConstants.ZOOM_LEVEL, zoomLevel);
				fileJSON.putOpt(HikeConstants.ADDRESS, address);
				fileJSON.putOpt(HikeConstants.THUMBNAIL, thumbnailString);
				fileJSON.putOpt(HikeConstants.FILE_KEY, fileKey);
			}

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

	public void setFileTypeString(String fileTypeString)
	{
		this.fileTypeString = fileTypeString;
		this.hikeFileType = HikeFileType.fromString(fileTypeString);
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
		this.file = Utils.getOutputMediaFile(hikeFileType, fileName, fileKey);
	}
	
	public String getFileKey() 
	{
		return fileKey;
	}

	public HikeFileType getHikeFileType()
	{
		return hikeFileType;
	}

	public int getLatitude() 
	{
		return latitude;
	}

	public int getLongitude() 
	{
		return longitude;
	}

	public int getZoomLevel() 
	{
		return zoomLevel;
	}

	public String getAddress() 
	{
		return address;
	}

	public boolean wasFileDownloaded()
	{
		if(hikeFileType == HikeFileType.LOCATION)
		{
			return true;
		}
		if(Utils.getExternalStorageState() == ExternalStorageState.NONE)
		{
			return false;
		}
		if(file == null)
		{
			return Utils.getOutputMediaFile(hikeFileType, fileName, fileKey).exists();
		}
		return file.exists();
	}

	public String getFilePath()
	{
		if(Utils.getExternalStorageState() == ExternalStorageState.NONE)
		{
			return null;
		}
		if(file == null)
		{
			return Utils.getOutputMediaFile(hikeFileType, fileName, fileKey).getPath();
		}
		return file.getPath();
	}

	public File getFile()
	{
		if(file == null)
		{
			return Utils.getOutputMediaFile(hikeFileType, fileName, fileKey);
		}
		return file;
	}
}
