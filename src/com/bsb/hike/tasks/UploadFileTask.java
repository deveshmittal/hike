package com.bsb.hike.tasks;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.FileTransferTaskBase;
import com.bsb.hike.utils.Utils;

public class UploadFileTask extends FileTransferTaskBase {
	private String msisdn;
	private String filePath;
	private String fileKey;
	private File selectedFile;
	private String fileType;
	private HikeFileType hikeFileType;
	private boolean wasFileSaved;
	private Context context;
	private ConvMessage convMessage;
	private Uri picasaUri;

	public UploadFileTask(String msisdn, String filePath, String fileKey,
			String fileType, HikeFileType hikeFileType, boolean wasFileSaved,
			Context context) {
		this.msisdn = msisdn;
		this.filePath = filePath;
		this.fileKey = fileKey;
		this.fileType = fileType;
		this.hikeFileType = hikeFileType;
		this.wasFileSaved = wasFileSaved;
		this.context = context;
	}

	public UploadFileTask(ConvMessage convMessage, Context context) {
		this.convMessage = convMessage;
		this.context = context;
	}

	public UploadFileTask(Uri picasaUri, HikeFileType hikeFileType,
			String msisdn, Context context) {
		this.picasaUri = picasaUri;
		this.hikeFileType = hikeFileType;
		this.msisdn = msisdn;
		this.context = context;
	}

	@Override
	protected FTResult doInBackground(Void... params) {
		this.cancelTask = new AtomicBoolean(false);
		try {
			String fileName = null;
			if (convMessage == null) {
				if (picasaUri == null) {
					Log.d(getClass().getSimpleName(), "File Path; " + filePath);
					if (filePath == null) {
						return FTResult.UPLOAD_FAILED;
					}

					File file = new File(filePath);
					fileName = file.getName();

					Log.d(getClass().getSimpleName(),
							"File size: " + file.length() + " File name: "
									+ fileName);

					if (HikeConstants.MAX_FILE_SIZE != -1
							&& HikeConstants.MAX_FILE_SIZE < file.length()) {
						return FTResult.FILE_TOO_LARGE;
					}

					// We don't need to save the file if its a recording since
					// its already saved in the hike folder
					if (TextUtils.isEmpty(fileKey) && !wasFileSaved) {
						selectedFile = Utils.getOutputMediaFile(hikeFileType,
								fileName, null);
						Log.d(getClass().getSimpleName(), "Copying file: "
								+ filePath + " to " + selectedFile.getPath());
						// TODO Check performance on low end phones. If slow,
						// should remove from UI thread.
						// Saving the file to hike local folder
						if (!Utils.copyFile(filePath, selectedFile.getPath(),
								hikeFileType)) {
							return FTResult.READ_FAIL;
						}
						filePath = selectedFile.getPath();
					} else {
						selectedFile = new File(filePath);
					}
					fileName = selectedFile.getName();
				} else {
					String[] filePathColumn = { MediaColumns.DATA,
							MediaColumns.DISPLAY_NAME };
					Cursor cursor = context.getContentResolver().query(
							picasaUri, filePathColumn, null, null, null);
					// if it is a picasa image on newer devices with OS 3.0 and
					// up
					if (cursor != null) {
						cursor.moveToFirst();
						int nameIdx = cursor
								.getColumnIndex(MediaColumns.DISPLAY_NAME);
						if (nameIdx != -1) {
							fileName = cursor.getString(nameIdx);
						}
					}
					selectedFile = Utils.getOutputMediaFile(hikeFileType,
							fileName, null);
					if (TextUtils.isEmpty(fileName)) {
						fileName = selectedFile.getName();
					}

					JSONObject metadata = getFileTransferMetadata(fileName,
							fileType, hikeFileType, null, null);

					convMessage = createConvMessage(msisdn, fileName, metadata);

					// Called so that the UI in the Conversation lists screen is
					// updated
					HikeMessengerApp.getPubSub().publish(
							HikePubSub.MESSAGE_SENT, convMessage);

					Utils.downloadPicasaFile(context, selectedFile, picasaUri);
					filePath = selectedFile.getPath();
				}

				Bitmap thumbnail = null;
				String thumbnailString = null;
				if (hikeFileType == HikeFileType.IMAGE) {
					thumbnail = Utils.scaleDownImage(filePath,
							HikeConstants.MAX_DIMENSION_THUMBNAIL_PX, true);
				} else if (hikeFileType == HikeFileType.VIDEO) {
					thumbnail = ThumbnailUtils.createVideoThumbnail(filePath,
							MediaStore.Images.Thumbnails.MICRO_KIND);
				}
				if (thumbnail != null) {
					thumbnailString = Base64
							.encodeToString(Utils.bitmapToBytes(thumbnail,
									Bitmap.CompressFormat.JPEG), Base64.DEFAULT);
				}

				JSONObject metadata = getFileTransferMetadata(fileName,
						fileType, hikeFileType, thumbnailString, thumbnail);

				if (convMessage == null) {
					convMessage = createConvMessage(msisdn, fileName, metadata);

					if (TextUtils.isEmpty(fileKey)) {
						// Called so that the UI in the Conversation lists
						// screen is updated
						HikeMessengerApp.getPubSub().publish(
								HikePubSub.MESSAGE_SENT, convMessage);
					}
				} else {
					/*
					 * Only happens in case of Picasa files where we have
					 * already created a convMessage but did not initially have
					 * a thumbnail.
					 */
					convMessage.setMetadata(metadata);
					HikeConversationsDatabase.getInstance()
							.updateMessageMetadata(convMessage.getMsgID(),
									convMessage.getMetadata());
					HikeMessengerApp.getPubSub().publish(
							HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
				}
			} else {
				HikeFile hikeFile = convMessage.getMetadata().getHikeFiles()
						.get(0);
				filePath = hikeFile.getFilePath();
				fileName = hikeFile.getFileName();
				fileType = hikeFile.getFileTypeString();
			}
			boolean fileWasAlreadyUploaded = true;
			// If we don't have a file key, that means we haven't uploaded the
			// file to the server yet
			if (TextUtils.isEmpty(fileKey)) {
				fileWasAlreadyUploaded = false;

				JSONObject response = AccountUtils.executeFileTransferRequest(
						filePath, fileName, null, this, cancelTask, fileType);

				JSONObject fileJSON = response.getJSONObject("data");
				fileKey = fileJSON.optString(HikeConstants.FILE_KEY);
				fileType = fileJSON.optString(HikeConstants.CONTENT_TYPE);
			}

			JSONObject metadata = new JSONObject();
			JSONArray filesArray = new JSONArray();

			HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			hikeFile.setFileKey(fileKey);
			hikeFile.setFileTypeString(fileType);

			filesArray.put(hikeFile.serialize());
			metadata.put(HikeConstants.FILES, filesArray);

			convMessage.setMetadata(metadata);

			// If the file was just uploaded to the servers, we want to publish
			// this event
			if (!fileWasAlreadyUploaded) {
				HikeMessengerApp.getPubSub().publish(
						HikePubSub.UPLOAD_FINISHED, convMessage);
			}

			Utils.addFileName(hikeFile.getFileName(), hikeFile.getFileKey());
			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT,
					convMessage);
		} catch (Exception e) {
			Log.e(getClass().getSimpleName(), "Exception", e);
			return FTResult.UPLOAD_FAILED;
		}
		return FTResult.SUCCESS;
	}

	private JSONObject getFileTransferMetadata(String fileName,
			String fileType, HikeFileType hikeFileType, String thumbnailString,
			Bitmap thumbnail) throws JSONException {
		JSONArray files = new JSONArray();
		files.put(new HikeFile(fileName,
				TextUtils.isEmpty(fileType) ? HikeFileType
						.toString(hikeFileType) : fileType, thumbnailString,
				thumbnail).serialize());
		JSONObject metadata = new JSONObject();
		metadata.put(HikeConstants.FILES, files);

		return metadata;
	}

	private ConvMessage createConvMessage(String msisdn, String fileName,
			JSONObject metadata) throws JSONException {
		long time = System.currentTimeMillis() / 1000;
		ConvMessage convMessage = new ConvMessage(fileName, msisdn, time,
				ConvMessage.State.SENT_UNCONFIRMED);
		convMessage.setMetadata(metadata);
		HikeConversationsDatabase.getInstance().addConversationMessages(
				convMessage);

		HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_MESSAGE_CREATED,
				convMessage);

		ChatThread.fileTransferTaskMap.put(convMessage.getMsgID(), this);
		return convMessage;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		updateProgress(values[0]);
	}

	@Override
	protected void onPostExecute(FTResult result) {
		if (convMessage != null) {
			ChatThread.fileTransferTaskMap.remove(convMessage.getMsgID());
			HikeMessengerApp.getPubSub().publish(
					HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
		}

		if (result != FTResult.SUCCESS) {
			int errorStringId = 0;
			if (result == FTResult.FILE_TOO_LARGE) {
				errorStringId = R.string.max_file_size;
			} else if (result == FTResult.READ_FAIL) {
				errorStringId = R.string.unable_to_read;
			} else if (result == FTResult.UPLOAD_FAILED) {
				errorStringId = R.string.upload_failed;
			}
			Toast.makeText(context, errorStringId, Toast.LENGTH_SHORT).show();
		}
	}
}
