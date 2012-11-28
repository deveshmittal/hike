package com.bsb.hike.models;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;

public class HikeFile {
	public static enum HikeFileType {
		PROFILE, IMAGE, VIDEO, AUDIO, LOCATION;

		public static HikeFileType fromString(String fileTypeString) {
			if (fileTypeString.startsWith("video")) {
				return HikeFileType.VIDEO;
			} else if (fileTypeString.startsWith("audio")) {
				return HikeFileType.AUDIO;
			} else if (fileTypeString
					.startsWith(HikeConstants.LOCATION_CONTENT_TYPE)) {
				return HikeFileType.LOCATION;
			} else {
				return HikeFileType.IMAGE;
			}
		}

		public static String toString(HikeFileType hikeFileType) {
			if (hikeFileType == PROFILE || hikeFileType == IMAGE) {
				return "image/*";
			} else if (hikeFileType == VIDEO) {
				return "video/*";
			} else if (hikeFileType == AUDIO) {
				return "audio/*";
			} else if (hikeFileType == LOCATION) {
				return HikeConstants.LOCATION_CONTENT_TYPE;
			}
			return null;
		}

		public static String getFileTypeMessage(Context context,
				HikeFileType hikeFileType) {
			if (hikeFileType == PROFILE || hikeFileType == IMAGE) {
				return context.getString(R.string.image_msg);
			} else if (hikeFileType == VIDEO) {
				return context.getString(R.string.video_msg);
			} else if (hikeFileType == AUDIO) {
				return context.getString(R.string.audio_msg);
			} else if (hikeFileType == LOCATION) {
				return context.getString(R.string.location_msg);
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
	private double latitude;
	private double longitude;
	private int zoomLevel;
	private String address;

	public HikeFile(JSONObject fileJSON) {
		this.fileName = fileJSON.optString(HikeConstants.FILE_NAME);
		this.fileTypeString = fileJSON.optString(HikeConstants.CONTENT_TYPE);
		this.thumbnailString = Utils.getSquareThumbnail(fileJSON);
		this.thumbnail = thumbnail == null ? Utils
				.stringToDrawable(thumbnailString) : thumbnail;
		this.fileKey = fileJSON.optString(HikeConstants.FILE_KEY);
		this.hikeFileType = HikeFileType.fromString(fileTypeString);
		this.latitude = fileJSON.optDouble(HikeConstants.LATITUDE);
		this.longitude = fileJSON.optDouble(HikeConstants.LONGITUDE);
		this.zoomLevel = fileJSON.optInt(HikeConstants.ZOOM_LEVEL,
				HikeConstants.DEFAULT_ZOOM_LEVEL);
		this.address = fileJSON.optString(HikeConstants.ADDRESS);
		this.file = TextUtils.isEmpty(this.fileKey) ? null : Utils
				.getOutputMediaFile(hikeFileType, fileName, fileKey);
		if (this.file != null) {
			// Update the file name to prevent duplicacy
			this.fileName = this.file.getName();
		}
	}

	public HikeFile(String fileName, String fileTypeString,
			String thumbnailString, Bitmap thumbnail) {
		this.fileName = fileName;
		this.fileTypeString = fileTypeString;
		this.hikeFileType = HikeFileType.fromString(fileTypeString);
		this.thumbnailString = thumbnailString;
		this.thumbnail = new BitmapDrawable(thumbnail);
	}

	public HikeFile(double latitude, double longitude, int zoomLevel,
			String address, String thumbnailString, Bitmap thumbnail) {
		this.fileName = HikeConstants.LOCATION_FILE_NAME;
		this.latitude = latitude;
		this.longitude = longitude;
		this.zoomLevel = zoomLevel;
		this.fileTypeString = HikeConstants.LOCATION_CONTENT_TYPE;
		this.hikeFileType = HikeFileType.fromString(fileTypeString);
		this.address = address;
		this.thumbnailString = thumbnailString;
		this.thumbnail = new BitmapDrawable(thumbnail);
	}

	public JSONObject serialize() {
		try {
			JSONObject fileJSON = new JSONObject();
			fileJSON.putOpt(HikeConstants.CONTENT_TYPE, fileTypeString);
			fileJSON.putOpt(HikeConstants.FILE_NAME, fileName);
			if (!HikeConstants.LOCATION_CONTENT_TYPE.equals(fileTypeString)) {
				fileJSON.putOpt(HikeConstants.FILE_KEY, fileKey);
				fileJSON.putOpt(HikeConstants.THUMBNAIL, thumbnailString);
			} else {
				fileJSON.putOpt(HikeConstants.LATITUDE, latitude);
				fileJSON.putOpt(HikeConstants.LONGITUDE, longitude);
				fileJSON.putOpt(HikeConstants.ZOOM_LEVEL, zoomLevel);
				fileJSON.putOpt(HikeConstants.ADDRESS, address);
				fileJSON.putOpt(HikeConstants.THUMBNAIL, thumbnailString);
				fileJSON.putOpt(HikeConstants.FILE_KEY, fileKey);
			}

			return fileJSON;
		} catch (JSONException e) {
			Log.e(getClass().getSimpleName(), "Invalid JSON", e);
		}
		return null;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileTypeString(String fileTypeString) {
		this.fileTypeString = fileTypeString;
		this.hikeFileType = HikeFileType.fromString(fileTypeString);
	}

	public String getFileTypeString() {
		return fileTypeString;
	}

	public String getThumbnailString() {
		return thumbnailString;
	}

	public void setThumbnail(Drawable thumbnail) {
		this.thumbnail = thumbnail;
	}

	public Drawable getThumbnail() {
		return thumbnail;
	}

	public void setFileKey(String fileKey) {
		this.fileKey = fileKey;
		this.file = Utils.getOutputMediaFile(hikeFileType, fileName, fileKey);
	}

	public String getFileKey() {
		return fileKey;
	}

	public HikeFileType getHikeFileType() {
		return hikeFileType;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public int getZoomLevel() {
		return zoomLevel;
	}

	public String getAddress() {
		return address;
	}

	public boolean wasFileDownloaded() {
		if (hikeFileType == HikeFileType.LOCATION) {
			return true;
		}
		if (Utils.getExternalStorageState() == ExternalStorageState.NONE) {
			return false;
		}
		if (file == null) {
			return Utils.getOutputMediaFile(hikeFileType, fileName, fileKey)
					.exists();
		}
		return file.exists();
	}

	public String getFilePath() {
		if (Utils.getExternalStorageState() == ExternalStorageState.NONE) {
			return null;
		}
		if (file == null) {
			return Utils.getOutputMediaFile(hikeFileType, fileName, fileKey)
					.getPath();
		}
		return file.getPath();
	}

	public File getFile() {
		if (file == null) {
			return Utils.getOutputMediaFile(hikeFileType, fileName, fileKey);
		}
		return file;
	}
}
