package com.bsb.hike.models;

import java.io.File;

import org.json.JSONArray;
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
		PROFILE, IMAGE, VIDEO, AUDIO, LOCATION, CONTACT, AUDIO_RECORDING, UNKNOWN;

		public static HikeFileType fromString(String fileTypeString) {
			return fromString(fileTypeString, false);
		}

		public static HikeFileType fromString(String fileTypeString,
				boolean isRecording) {
			if (fileTypeString.startsWith("video")) {
				return HikeFileType.VIDEO;
			} else if (fileTypeString.startsWith("audio")) {
				return isRecording ? HikeFileType.AUDIO_RECORDING
						: HikeFileType.AUDIO;
			} else if (fileTypeString
					.startsWith(HikeConstants.LOCATION_CONTENT_TYPE)) {
				return HikeFileType.LOCATION;
			} else if (fileTypeString.startsWith("image")) {
				return HikeFileType.IMAGE;
			} else if (fileTypeString
					.startsWith(HikeConstants.CONTACT_CONTENT_TYPE)) {
				return HikeFileType.CONTACT;
			}
			return HikeFileType.UNKNOWN;
		}

		public static String toString(HikeFileType hikeFileType) {
			if (hikeFileType == PROFILE || hikeFileType == IMAGE) {
				return "image/*";
			} else if (hikeFileType == VIDEO) {
				return "video/*";
			} else if (hikeFileType == AUDIO || hikeFileType == AUDIO_RECORDING) {
				return "audio/*";
			} else if (hikeFileType == LOCATION) {
				return HikeConstants.LOCATION_CONTENT_TYPE;
			} else if (hikeFileType == CONTACT) {
				return HikeConstants.CONTACT_CONTENT_TYPE;
			}
			return null;
		}

		public static String getFileTypeMessage(Context context,
				HikeFileType hikeFileType, boolean isSent) {
			if (hikeFileType == PROFILE || hikeFileType == IMAGE) {
				return isSent ? context.getString(R.string.image_msg_sent)
						: context.getString(R.string.image_msg_received);
			} else if (hikeFileType == VIDEO) {
				return isSent ? context.getString(R.string.video_msg_sent)
						: context.getString(R.string.video_msg_received);
			} else if (hikeFileType == AUDIO) {
				return isSent ? context.getString(R.string.audio_msg_sent)
						: context.getString(R.string.audio_msg_received);
			} else if (hikeFileType == LOCATION) {
				return isSent ? context.getString(R.string.location_msg_sent)
						: context.getString(R.string.location_msg_received);
			} else if (hikeFileType == CONTACT) {
				return isSent ? context.getString(R.string.contact_msg_sent)
						: context.getString(R.string.contact_msg_received);
			} else if (hikeFileType == AUDIO_RECORDING) {
				return isSent ? context
						.getString(R.string.audio_recording_msg_sent) : context
						.getString(R.string.audio_recording_msg_received);
			}
			return context.getString(R.string.unknown_msg);
		}
	}

	private String fileName;
	private String fileTypeString;
	private String thumbnailString;
	private Drawable thumbnail;
	private String fileKey;
	private HikeFileType hikeFileType;
	private File file;
	private long recordingDuration = -1;

	private String sourceFilePath;
	
	private double latitude;
	private double longitude;
	private int zoomLevel;
	private String address;

	private String displayName;
	private JSONArray phoneNumbers;
	private JSONArray emails;
	private JSONArray addresses;
	private JSONArray events;

	public HikeFile(JSONObject fileJSON) {
		this.fileName = fileJSON.optString(HikeConstants.FILE_NAME);
		this.fileTypeString = fileJSON.optString(HikeConstants.CONTENT_TYPE);
		this.thumbnailString = fileJSON
				.optString(HikeConstants.THUMBNAIL, null);
		this.thumbnail = thumbnail == null ? Utils
				.stringToDrawable(thumbnailString) : thumbnail;
		this.sourceFilePath = fileJSON.optString(HikeConstants.SOURCE_FILE_PATH);
		this.fileKey = fileJSON.optString(HikeConstants.FILE_KEY);
		this.latitude = fileJSON.optDouble(HikeConstants.LATITUDE);
		this.longitude = fileJSON.optDouble(HikeConstants.LONGITUDE);
		this.zoomLevel = fileJSON.optInt(HikeConstants.ZOOM_LEVEL,
				HikeConstants.DEFAULT_ZOOM_LEVEL);
		this.address = fileJSON.optString(HikeConstants.ADDRESS);
		if (this.file != null) {
			// Update the file name to prevent duplicacy
			this.fileName = this.file.getName();
		}
		this.displayName = fileJSON.optString(HikeConstants.NAME);
		this.phoneNumbers = fileJSON.optJSONArray(HikeConstants.PHONE_NUMBERS);
		this.emails = fileJSON.optJSONArray(HikeConstants.EMAILS);
		this.addresses = fileJSON.optJSONArray(HikeConstants.ADDRESSES);
		this.events = fileJSON.optJSONArray(HikeConstants.EVENTS);
		this.recordingDuration = fileJSON.optLong(HikeConstants.PLAYTIME, -1);
		this.hikeFileType = HikeFileType.fromString(fileTypeString,
				recordingDuration != -1);
		//this.file = TextUtils.isEmpty(this.fileKey) ? null : Utils
			//	.getOutputMediaFile(hikeFileType, fileName);
	}

	public HikeFile(String fileName, String fileTypeString,
			String thumbnailString, Bitmap thumbnail, long recordingDuration) {
		this.fileName = fileName;
		this.fileTypeString = fileTypeString;
		this.hikeFileType = HikeFileType.fromString(fileTypeString,
				recordingDuration != -1);
		this.thumbnailString = thumbnailString;
		this.thumbnail = new BitmapDrawable(thumbnail);
		this.recordingDuration = recordingDuration;
	}
	
	public HikeFile(String fileName, String fileTypeString,
			String thumbnailString, Bitmap thumbnail, long recordingDuration, String source) {
		this.fileName = fileName;
		this.fileTypeString = fileTypeString;
		this.hikeFileType = HikeFileType.fromString(fileTypeString,
				recordingDuration != -1);
		this.thumbnailString = thumbnailString;
		this.thumbnail = new BitmapDrawable(thumbnail);
		this.recordingDuration = recordingDuration;
		this.sourceFilePath = source;
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
			fileJSON.putOpt(HikeConstants.FILE_KEY, fileKey);
			fileJSON.putOpt(HikeConstants.SOURCE_FILE_PATH, sourceFilePath);
			fileJSON.putOpt(HikeConstants.THUMBNAIL, thumbnailString);
			if (recordingDuration != -1) {
				fileJSON.put(HikeConstants.PLAYTIME, recordingDuration);
			}
			if (HikeConstants.LOCATION_CONTENT_TYPE.equals(fileTypeString)) {
				fileJSON.putOpt(HikeConstants.LATITUDE, latitude);
				fileJSON.putOpt(HikeConstants.LONGITUDE, longitude);
				fileJSON.putOpt(HikeConstants.ZOOM_LEVEL, zoomLevel);
				fileJSON.putOpt(HikeConstants.ADDRESS, address);
			} else if (HikeConstants.CONTACT_CONTENT_TYPE
					.equals(fileTypeString)) {
				fileJSON.putOpt(HikeConstants.NAME, displayName);
				fileJSON.putOpt(HikeConstants.PHONE_NUMBERS, phoneNumbers);
				fileJSON.putOpt(HikeConstants.EMAILS, emails);
				fileJSON.putOpt(HikeConstants.ADDRESSES, addresses);
				fileJSON.putOpt(HikeConstants.EVENTS, events);
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

	public void setFile(File f)
	{
		this.file = f;
	}
	
	public void setFileTypeString(String fileTypeString) {
		this.fileTypeString = fileTypeString;
		this.hikeFileType = HikeFileType.fromString(fileTypeString,
				recordingDuration != -1);
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
		if(file == null)
			this.file = Utils.getOutputMediaFile(hikeFileType, fileName);
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

	public long getRecordingDuration() {
		return recordingDuration;
	}

	public boolean wasFileDownloaded() {
		if (hikeFileType == HikeFileType.LOCATION
				|| hikeFileType == HikeFileType.CONTACT) {
			return true;
		}
		if (Utils.getExternalStorageState() == ExternalStorageState.NONE) {
			return false;
		}
		if (file == null) {
			File file = Utils.getOutputMediaFile(hikeFileType, fileName);
			return file == null ? false : file.exists();
		}
		return file.exists();
	}

	public String getFilePath() {
		if (Utils.getExternalStorageState() == ExternalStorageState.NONE) {
			return null;
		}
		if (file == null) {
			file = Utils.getOutputMediaFile(hikeFileType, fileName);
		}
		return file.getPath();
	}

	public File getFile() {
		if (file == null) {
			file = Utils.getOutputMediaFile(hikeFileType, fileName);
		}
		return file;
	}
	
	public String getSourceFilePath() {
		return sourceFilePath;
	}
	
	public void removeSourceFile() {
		sourceFilePath = null;
	}

	public String getDisplayName() {
		return displayName;
	}

	public JSONArray getPhoneNumbers() {
		return phoneNumbers;
	}

	public JSONArray getEmails() {
		return emails;
	}

	public JSONArray getAddresses() {
		return addresses;
	}

	public JSONArray getEvents() {
		return events;
	}

	public void setFileName(String fName)
	{
		fileName = fName;
	}

}
