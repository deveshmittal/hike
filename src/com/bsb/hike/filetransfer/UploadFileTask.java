package com.bsb.hike.filetransfer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.HikeConstants.ImageQuality;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.filetransfer.FileTransferBase.FTState;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.MultipleConvMessage;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.FileTransferCancelledException;
import com.bsb.hike.utils.HikeTestUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class UploadFileTask extends FileTransferBase
{
	private String X_SESSION_ID;

	private Uri picasaUri = null;

	private String fileType;

	private String msisdn;

	private boolean isRecipientOnhike;

	private boolean isRecording;

	private File selectedFile = null;

	private long recordingDuration = -1;

	private boolean isForwardMsg;

	private FutureTask<FTResult> futureTask;

	private int num = 0;

	private static String BOUNDARY = "----------V2ymHFg03ehbqgZCaKO6jy";

	private int bufferSize = 1;

	private boolean freshStart;
	
	private HikeTestUtil mTestUtil = null;
	
	private ArrayList<ContactInfo> contactList;
	
	private ArrayList<ConvMessage> messageList;
	
	private boolean isMultiMsg;

	protected UploadFileTask(Handler handler, ConcurrentHashMap<Long, FutureTask<FTResult>> fileTaskMap, Context ctx, String token, String uId, String msisdn, File sourceFile,
			String fileKey, String fileType, HikeFileType hikeFileType, boolean isRecording, boolean isForwardMsg, boolean isRecipientOnHike, long recordingDuration)
	{
		super(handler, fileTaskMap, ctx, sourceFile, -1, hikeFileType, token, uId);
		this.msisdn = msisdn;
		this.fileType = fileType;
		this.isRecipientOnhike = isRecipientOnHike;
		this.recordingDuration = recordingDuration;
		this.isRecording = isRecording;
		this.isForwardMsg = isForwardMsg;
		this.isRecipientOnhike = isRecipientOnHike;
		this.fileKey = fileKey;
		_state = FTState.INITIALIZED;
		mTestUtil = HikeTestUtil.getInstance(context);
		createConvMessage();		
	}
	
	protected UploadFileTask(Handler handler, ConcurrentHashMap<Long, FutureTask<FTResult>> fileTaskMap, Context ctx, String token, String uId, ArrayList<ContactInfo> contactList, File sourceFile,
			String fileKey, String fileType, HikeFileType hikeFileType, boolean isRecording, boolean isForwardMsg, boolean isRecipientOnHike, long recordingDuration)
	{
		super(handler, fileTaskMap, ctx, sourceFile, -1, hikeFileType, token, uId);
		this.contactList = contactList;
		this.isMultiMsg = true;
		this.fileType = fileType;
		this.isRecipientOnhike = isRecipientOnHike;
		this.recordingDuration = recordingDuration;
		this.isRecording = isRecording;
		this.isForwardMsg = isForwardMsg;
		this.isRecipientOnhike = isRecipientOnHike;
		this.fileKey = fileKey;
		_state = FTState.INITIALIZED;
		createConvMessage();
	}
	
	protected UploadFileTask(Handler handler, ConcurrentHashMap<Long, FutureTask<FTResult>> fileTaskMap, Context ctx, String token, String uId, Object convMessage,
			boolean isRecipientOnHike)
	{
		super(handler, fileTaskMap, ctx, null, -1, null, token, uId);
		this.isRecipientOnhike = isRecipientOnHike;
		userContext = convMessage;
		HikeFile hikeFile = ((ConvMessage) userContext).getMetadata().getHikeFiles().get(0);
		if (!TextUtils.isEmpty(hikeFile.getSourceFilePath()))
			if (hikeFile.getSourceFilePath().startsWith(HikeConstants.PICASA_PREFIX))
			{
				this.picasaUri = Uri.parse(hikeFile.getSourceFilePath().substring(HikeConstants.PICASA_PREFIX.length()));
			}
		_state = FTState.INITIALIZED;
	}

	protected UploadFileTask(Handler handler, ConcurrentHashMap<Long, FutureTask<FTResult>> fileTaskMap, Context ctx, String token, String uId, Uri picasaUri, Object convMessage,
			boolean isRecipientOnHike)
	{
		super(handler, fileTaskMap, ctx, null, -1, null, token, uId);
		this.picasaUri = picasaUri;
		this.isRecipientOnhike = isRecipientOnHike;
		userContext = convMessage;
		_state = FTState.INITIALIZED;
		createConvMessage();
	}

	protected UploadFileTask(Handler handler, ConcurrentHashMap<Long, FutureTask<FTResult>> fileTaskMap, Context ctx, String token, String uId, Uri picasaUri,
			HikeFileType hikeFileType, String msisdn, boolean isRecipientOnHike)
	{
		super(handler, fileTaskMap, ctx, null, -1, null, token, uId);
		this.picasaUri = picasaUri;
		this.hikeFileType = hikeFileType;
		this.msisdn = msisdn;
		this.isRecipientOnhike = isRecipientOnHike;
		_state = FTState.INITIALIZED;
		createConvMessage();
	}

	protected void setFutureTask(FutureTask<FTResult> fuTask)
	{
		futureTask = fuTask;
		if (isMultiMsg)
		{
			for (ConvMessage msg : messageList)
			{
				fileTaskMap.put(msg.getMsgID(),futureTask);
			}
		}
		else
		{
			fileTaskMap.put(((ConvMessage) userContext).getMsgID(), futureTask);
		}
	}

	// private ConvMessage createConvMessage(Uri picasaUri, File mFile, HikeFileType hikeFileType, String msisdn, boolean isRecipientOnhike, String fileType, long
	// recordingDuration) throws FileTransferCancelledException, Exception
	private void createConvMessage()
	{
		try
		{
			// TODO Auto-generated method stub
			System.gc();
			File destinationFile;
			String fileName = Utils.getFinalFileName(hikeFileType);
			JSONObject metadata;
			if (picasaUri == null)
			{
				destinationFile = mFile;
				fileName = destinationFile.getName();
				Bitmap thumbnail = null;
				String thumbnailString = null;
				String quality = null;
				if (hikeFileType == HikeFileType.IMAGE)
				{
					Bitmap.Config config = Bitmap.Config.RGB_565;
					if(Utils.hasJellyBeanMR1()){
						config = Bitmap.Config.ARGB_8888;
					}
					thumbnail = HikeBitmapFactory.scaleDownBitmap(destinationFile.getPath(), HikeConstants.MAX_DIMENSION_THUMBNAIL_PX, HikeConstants.MAX_DIMENSION_THUMBNAIL_PX,
							config, false, false);
					thumbnail = Utils.getRotatedBitmap(destinationFile.getPath(), thumbnail);
					if (thumbnail == null && !TextUtils.isEmpty(fileKey))
					{
						BitmapDrawable bd = HikeMessengerApp.getLruCache().getFileIconFromCache(fileKey);
						if (bd != null)
							thumbnail = HikeMessengerApp.getLruCache().getFileIconFromCache(fileKey).getBitmap();
					}
					quality = getImageQuality();
				}
				else if (hikeFileType == HikeFileType.VIDEO)
				{
					thumbnail = ThumbnailUtils.createVideoThumbnail(destinationFile.getPath(), MediaStore.Images.Thumbnails.MICRO_KIND);
					if (thumbnail == null && !TextUtils.isEmpty(fileKey))
					{
						BitmapDrawable bd = HikeMessengerApp.getLruCache().getFileIconFromCache(fileKey);
						if (bd != null)
							thumbnail = HikeMessengerApp.getLruCache().getFileIconFromCache(fileKey).getBitmap();
					}
				}
				if (thumbnail != null)
				{
					int compressQuality;
					if (hikeFileType == HikeFileType.IMAGE)
					{
//						Bitmap bluredThumb = Utils.createBlurredImage(thumbnail, context);
//						if(bluredThumb != null){
//							compressQuality = 60;
//							thumbnail = bluredThumb;
//						}else
							compressQuality = 25;
					}else{
						compressQuality = 75;
					}
					byte [] tBytes = BitmapUtils.bitmapToBytes(thumbnail, Bitmap.CompressFormat.JPEG, compressQuality);
					thumbnail = HikeBitmapFactory.decodeByteArray(tBytes, 0, tBytes.length);
					thumbnailString = Base64.encodeToString(tBytes, Base64.DEFAULT);
					// thumbnail.recycle();
					Logger.d(getClass().getSimpleName(), "Sent Thumbnail Size : " + tBytes.length);
				}
				metadata = getFileTransferMetadata(fileName, fileType, hikeFileType, thumbnailString, thumbnail, recordingDuration, mFile.getPath(), (int) mFile.length(), quality);
			}
			else
			// this is the case for picasa picture
			{
				String[] filePathColumn = { MediaColumns.DATA, MediaColumns.DISPLAY_NAME };
				Cursor cursor = context.getContentResolver().query(picasaUri, filePathColumn, null, null, null);
				// if it is a picasa image on newer devices with OS 3.0 and
				// up
				if (cursor != null)
				{
					cursor.moveToFirst();
					int nameIdx = cursor.getColumnIndex(MediaColumns.DISPLAY_NAME);
					if (nameIdx != -1)
					{
						// fileName = cursor.getString(nameIdx);
					}
				}
				destinationFile = Utils.getOutputMediaFile(hikeFileType, fileName, true);
				if (TextUtils.isEmpty(fileName))
				{
					fileName = destinationFile.getName();
				}
				metadata = getFileTransferMetadata(fileName, fileType, hikeFileType, null, null, recordingDuration, HikeConstants.PICASA_PREFIX + picasaUri.toString(), 0, ImageQuality.IMAGE_QUALITY_ORIGINAL);
			}
			if (isMultiMsg)
			{
				messageList = new ArrayList<ConvMessage>();

				MessageMetadata messageMetadata = new MessageMetadata(metadata, true);
				for (ContactInfo contact : contactList)
				{
					ConvMessage msg = createConvMessage(fileName, messageMetadata, contact.getMsisdn(), isRecipientOnhike);
					messageList.add(msg);
				}
				userContext = messageList.get(0);
				ArrayList<ConvMessage> pubsubMsgList = new ArrayList<ConvMessage>();
				pubsubMsgList.add((ConvMessage) userContext);
				MultipleConvMessage multiConMsg = new MultipleConvMessage(pubsubMsgList, contactList);
				HikeConversationsDatabase.getInstance().addConversations(multiConMsg.getMessageList(), multiConMsg.getContactList(),false);
				multiConMsg.sendPubSubForConvScreenMultiMessage();
			}
			else
			{
				userContext = createConvMessage(fileName, metadata, msisdn, isRecipientOnhike);
				HikeConversationsDatabase.getInstance().addConversationMessages((ConvMessage)userContext);
				HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, (ConvMessage) userContext);
			}
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
	}

	private ConvMessage createConvMessage(String fileName, MessageMetadata metadata, String msisdn, boolean isRecipientOnhike) throws JSONException
	{
		long time = System.currentTimeMillis() / 1000;
		ConvMessage convMessage = new ConvMessage(fileName, msisdn, time, ConvMessage.State.SENT_UNCONFIRMED);
		convMessage.setMetadata(metadata);
		convMessage.setSMS(!isRecipientOnhike);
		HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_MESSAGE_CREATED, convMessage);
		return convMessage;
	}

	private ConvMessage createConvMessage(String fileName, JSONObject metadata, String msisdn, boolean isRecipientOnhike) throws JSONException
	{
		long time = System.currentTimeMillis() / 1000;
		ConvMessage convMessage = new ConvMessage(fileName, msisdn, time, ConvMessage.State.SENT_UNCONFIRMED);
		convMessage.setMetadata(metadata);
		convMessage.setSMS(!isRecipientOnhike);
		HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_MESSAGE_CREATED, convMessage);
		return convMessage;
	}

	private JSONObject getFileTransferMetadata(String fileName, String fileType, HikeFileType hikeFileType, String thumbnailString, Bitmap thumbnail, long recordingDuration,
			String sourceFilePath, int fileSize, String img_quality) throws JSONException
	{
		JSONArray files = new JSONArray();
		files.put(new HikeFile(fileName, TextUtils.isEmpty(fileType) ? HikeFileType.toString(hikeFileType) : fileType, thumbnailString, thumbnail, recordingDuration,
				sourceFilePath, fileSize, true, img_quality).serialize());
		JSONObject metadata = new JSONObject();
		metadata.put(HikeConstants.FILES, files);
		return metadata;
	}

	/**
	 * This function do the initial steps for uploading i.e Create copy of file to upload (if required)
	 * 
	 * Note : All these steps are done if and only if required else this function will simply return
	 * 
	 * @throws Exception
	 */
	private void initFileUpload() throws FileTransferCancelledException, Exception
	{
		msgId = ((ConvMessage) userContext).getMsgID();
		if (isMultiMsg)
		{
			for (int i=1 ; i < messageList.size() ; i++)
			{
				messageList.get(i).setMsgID(msgId + i);
			}
		}
		HikeFile hikeFile = ((ConvMessage) userContext).getMetadata().getHikeFiles().get(0);
		hikeFileType = hikeFile.getHikeFileType();

		// UPLOAD INIT LOG
		mTestUtil.setMsgidTimestampPair(msgId, System.currentTimeMillis());

		selectedFile = new File(hikeFile.getFilePath());
		String fileName = selectedFile.getName();
		if (hikeFile.getFilePath() == null)
		{
			throw new FileNotFoundException("File is not accessible. SDCard unmount");
		}
		if (picasaUri == null)
		{
			if (hikeFile.getSourceFilePath() == null)
			{
				Logger.d("This filepath: ", selectedFile.getPath());
				Logger.d("Hike filepath: ", Utils.getFileParent(hikeFileType, true));
			}
			else
			{
				mFile = new File(hikeFile.getSourceFilePath());
				if (mFile.exists() && hikeFileType == HikeFileType.IMAGE && !mFile.getPath().startsWith(Utils.getFileParent(hikeFileType, true)))
				{
					selectedFile = Utils.getOutputMediaFile(hikeFileType, fileName, true);
					if (selectedFile == null)
						throw new Exception(FileTransferManager.READ_FAIL);
					
					if(selectedFile.exists())
					{
						selectedFile = Utils.getOutputMediaFile(hikeFileType, null, true);
					}
					if (!Utils.copyImage(mFile.getPath(), selectedFile.getPath(), context))
					{
						Logger.d(getClass().getSimpleName(), "throwing copy file exception");
						throw new Exception(FileTransferManager.READ_FAIL);
					}
					hikeFile.setFile(selectedFile);
				}
				// do not copy the file if it is video or audio or any other file
				else
				{
					selectedFile = mFile;
					hikeFile.setFile(selectedFile);
				}
				hikeFile.removeSourceFile();
			}
		}
		else
		// picasa case
		{
			try
			{
				Utils.downloadAndSaveFile(context, selectedFile, picasaUri);
			}
			catch (Exception e)
			{
				throw new Exception(FileTransferManager.UNABLE_TO_DOWNLOAD);
			}

			Bitmap thumbnail = null;
			String thumbnailString = null;
			if (hikeFileType == HikeFileType.IMAGE)
			{
				Bitmap.Config config = Bitmap.Config.RGB_565;
				if(Utils.hasJellyBeanMR1()){
					config = Bitmap.Config.ARGB_8888;
				}
				thumbnail = HikeBitmapFactory.scaleDownBitmap(selectedFile.getPath(), HikeConstants.MAX_DIMENSION_THUMBNAIL_PX, HikeConstants.MAX_DIMENSION_THUMBNAIL_PX,
						config, true, false);
				thumbnail = Utils.getRotatedBitmap(selectedFile.getPath(), thumbnail);
			}
			else if (hikeFileType == HikeFileType.VIDEO)
			{
				thumbnail = ThumbnailUtils.createVideoThumbnail(selectedFile.getPath(), MediaStore.Images.Thumbnails.MINI_KIND);
			}
			if (thumbnail != null)
			{
				int compressQuality;
				if (hikeFileType == HikeFileType.IMAGE)
				{
//					Bitmap bluredThumb = Utils.createBlurredImage(thumbnail, context);
//					if(bluredThumb != null){
//						compressQuality = 60;
//						thumbnail = bluredThumb;
//					}else
					compressQuality = 25;
				}else{
					compressQuality = 75;
				}
				byte [] tBytes = BitmapUtils.bitmapToBytes(thumbnail, Bitmap.CompressFormat.JPEG, compressQuality);
				thumbnail = HikeBitmapFactory.decodeByteArray(tBytes, 0, tBytes.length);
				thumbnailString = Base64.encodeToString(tBytes, Base64.DEFAULT);
				
				// thumbnail.recycle();
			}
			else
			{
				throw new Exception("Network error");
			}
			
			JSONObject metadata = getFileTransferMetadata(fileName, fileType, hikeFileType, thumbnailString, thumbnail, ImageQuality.IMAGE_QUALITY_ORIGINAL);
			hikeFile.removeSourceFile();
			((ConvMessage) userContext).setMetadata(metadata);
		}

		if (isMultiMsg)
		{
			for (ConvMessage msg : messageList)
			{
				HikeConversationsDatabase.getInstance().updateMessageMetadata(msg.getMsgID(), msg.getMetadata());
			}
		}
		else
		{
			HikeConversationsDatabase.getInstance().updateMessageMetadata(((ConvMessage) userContext).getMsgID(), ((ConvMessage) userContext).getMetadata());
		}
		fileName = hikeFile.getFileName();
		fileType = hikeFile.getFileTypeString();
		hikeFileType = hikeFile.getHikeFileType();

		stateFile = getStateFile((ConvMessage) userContext);
		Logger.d(getClass().getSimpleName(), "Upload state bin file :: " + fileName + ".bin." + ((ConvMessage) userContext).getMsgID());
	}

	private JSONObject getFileTransferMetadata(String fileName, String fileType, HikeFileType hikeFileType, String thumbnailString, Bitmap thumbnail, String img_quality) throws JSONException
	{
		JSONArray files = new JSONArray();
		files.put(new HikeFile(fileName, TextUtils.isEmpty(fileType) ? HikeFileType.toString(hikeFileType) : fileType, thumbnailString, thumbnail, recordingDuration, true, img_quality)
				.serialize());
		JSONObject metadata = new JSONObject();
		metadata.put(HikeConstants.FILES, files);
		return metadata;
	}

	@Override
	public FTResult call()
	{
		mThread = Thread.currentThread();
		boolean isValidKey = false;
		try{
			isValidKey = isFileKeyValid();
		}catch(Exception e){
			Logger.e(getClass().getSimpleName(), "Exception", e);
			_state = FTState.ERROR;
			HikeFile hikeFile = ((ConvMessage) userContext).getMetadata().getHikeFiles().get(0);
			stateFile = getStateFile((ConvMessage) userContext);
			saveFileKeyState(fileKey);
			fileKey = null;
			return FTResult.UPLOAD_FAILED;
		}
		try
		{
			if (isValidKey)
			{
				try
				{
					initFileUpload();
				}
				catch (Exception e)
				{

				}
			}
			else
			{
				initFileUpload();
			}
		}
		catch (FileTransferCancelledException e)
		{
			Logger.e(getClass().getSimpleName(), "Exception", e);
			return FTResult.UPLOAD_FAILED;
		}
		catch (FileNotFoundException e)
		{
			Logger.e(getClass().getSimpleName(), "Exception", e);
			return FTResult.CARD_UNMOUNT;
		}
		catch (Exception e)
		{
			if (e != null)
			{
				Logger.e(getClass().getSimpleName(), "Exception", e);
				if (FileTransferManager.READ_FAIL.equals(e.getMessage()))
					return FTResult.READ_FAIL;
				else if (FileTransferManager.UNABLE_TO_DOWNLOAD.equals(e.getMessage()))
					return FTResult.DOWNLOAD_FAILED;
			}
		}

		// time spent in initializing file upload
		long initTime = mTestUtil.getTimestampForMsgid(msgId);
		long currTime = System.currentTimeMillis();
		mTestUtil.setMsgidTimestampPair(msgId, currTime);
		long diffTime = System.currentTimeMillis() - initTime;
		
		try 
		{
			mTestUtil.writeDataToFile("FT," +  msgId + "," + "UPLOAD-PREP-TIME" + "," + Long.toString(diffTime) + "," + HikeTestUtil.getCurrentTimeInMilliseconds() +
					"," + mFile.getName() + "," + mTestUtil.getMessageDelay() + "," + Utils.getCellLocation(context));
		}
		catch (RemoteException e1) 
		{
			e1.printStackTrace();
		}		

		try
		{
			if (_state == FTState.CANCELLED)
				return FTResult.CANCELLED;

			_state = FTState.IN_PROGRESS;
			boolean fileWasAlreadyUploaded = true;

			// If we don't have a file key, that means we haven't uploaded the
			// file to the server yet
			if (TextUtils.isEmpty(fileKey))
			{
				fileWasAlreadyUploaded = false;

				JSONObject response = null;
				freshStart = true;
				while(freshStart)
				{
					freshStart = false;
					response = uploadFile(selectedFile); // <<----- this is the main upload function where upload to server is done
				}

				if (_state == FTState.CANCELLED)
					return FTResult.CANCELLED;
				else if (_state == FTState.PAUSED)
					return FTResult.PAUSED;
				else if (response == null)
					return FTResult.UPLOAD_FAILED;
				JSONObject fileJSON = response.getJSONObject(HikeConstants.DATA_2);
				fileKey = fileJSON.optString(HikeConstants.FILE_KEY);
				fileType = fileJSON.optString(HikeConstants.CONTENT_TYPE);
				fileSize = fileJSON.optInt(HikeConstants.FILE_SIZE);
				String md5Hash = fileJSON.optString("md5_original");
				Logger.d(getClass().getSimpleName(), "Server md5 : " + md5Hash);
				if (md5Hash != null)
				{
					String file_md5Hash = Utils.fileToMD5(selectedFile.getPath());
					Logger.d(getClass().getSimpleName(), "Phone's md5 : " + file_md5Hash);
					// if (!md5Hash.equals(file_md5Hash))
					// {
					// Logger.d(getClass().getSimpleName(), "The md5's are not equal...Deleting the files...");
					// deleteStateFile();
					// return FTResult.FAILED_UNRECOVERABLE;
					// }
				}
				// else
				// {
				// deleteStateFile();
				// return FTResult.FAILED_UNRECOVERABLE;
				// }
			}

			long lastTime = mTestUtil.getTimestampForMsgid(msgId);
			long _currTime = System.currentTimeMillis();
			long difTime = _currTime - lastTime;
			mTestUtil.writeDataToFile("FT," +  msgId + "," + "UPLOAD-TIME" + "," + Long.toString(difTime) +
			"," + Long.toString(_currTime) + "," + mFile.getName() + "," + fileKey + "," + mTestUtil.getMessageDelay() + "," + Utils.getCellLocation(context));		

			JSONObject metadata = new JSONObject();
			JSONArray filesArray = new JSONArray();

			HikeFile hikeFile = ((ConvMessage) userContext).getMetadata().getHikeFiles().get(0);
			hikeFile.setFileKey(fileKey);
			hikeFile.setFileSize(fileSize);
			hikeFile.setFileTypeString(fileType);

			filesArray.put(hikeFile.serialize());
			metadata.put(HikeConstants.FILES, filesArray);

			if (isMultiMsg)
			{
				long ts = System.currentTimeMillis() / 1000;

				MessageMetadata messageMetadata = new MessageMetadata(metadata, true);
				for (ConvMessage msg : messageList)
				{
					msg.setMetadata(messageMetadata);
					msg.setTimestamp(ts);
					HikeConversationsDatabase.getInstance().updateMessageMetadata(msg.getMsgID(), msg.getMetadata());
					//HikeMessengerApp.getPubSub().publish(HikePubSub.UPLOAD_FINISHED, msg);
				}
				ArrayList<ConvMessage> pubsubMsgList = new ArrayList<ConvMessage>();
				pubsubMsgList.add((ConvMessage) userContext);
				HikeMessengerApp.getPubSub().publish(HikePubSub.MULTI_FILE_UPLOADED, new MultipleConvMessage(pubsubMsgList, contactList));
			}
			else
			{
				((ConvMessage) userContext).setMetadata(metadata);
	
				// The file was just uploaded to the servers, we want to publish
				// this event
				((ConvMessage) userContext).setTimestamp(System.currentTimeMillis() / 1000);
				HikeMessengerApp.getPubSub().publish(HikePubSub.UPLOAD_FINISHED, ((ConvMessage) userContext));
	
				HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, ((ConvMessage) userContext));
			}
			deleteStateFile();
			Utils.addFileName(hikeFile.getFileName(), hikeFile.getFileKey());
			_state = FTState.COMPLETED;
		}
		catch (MalformedURLException e)
		{
			error();
			Logger.e(getClass().getSimpleName(), "Exception", e);
			return FTResult.UPLOAD_FAILED;
		}
		catch (FileNotFoundException e)
		{
			error();
			Logger.e(getClass().getSimpleName(), "Exception", e);
			return FTResult.READ_FAIL;
		}
		catch (ClientProtocolException e)
		{
			error();
			Logger.e(getClass().getSimpleName(), "Exception", e);
			return FTResult.UPLOAD_FAILED;
		}
		catch (IOException e)
		{
			error();
			Logger.e(getClass().getSimpleName(), "Exception", e);
			return FTResult.UPLOAD_FAILED;
		}
		catch (JSONException e)
		{
			error();
			Logger.e(getClass().getSimpleName(), "Exception", e);
			return FTResult.UPLOAD_FAILED;
		}
		catch (Exception e)
		{
			error();
			Logger.e(getClass().getSimpleName(), "Exception", e);
			return FTResult.UPLOAD_FAILED;
		}
		return FTResult.SUCCESS;
	}

	private JSONObject uploadFile(File sourceFile) throws MalformedURLException, FileNotFoundException, IOException, JSONException, ClientProtocolException, Exception
	{
		int mStart = 0;
		JSONObject responseJson = null;
		FileSavedState fst = FileTransferManager.getInstance(context).getUploadFileState(sourceFile, msgId);
		setFileTotalSize((int) sourceFile.length());
		// Bug Fix: 13029
		setBytesTransferred(fst.getTransferredSize());
		long temp = _bytesTransferred;
		temp *= 100;
		if (_totalSize > 0)
		{
			temp /= _totalSize;
			progressPercentage = (int) temp;
		}
		// represents this file is either not started or unrecovered error has happened
		Logger.d(getClass().getSimpleName(), "Starting Upload from state : " + fst.getFTState().toString());
		if (fst.getFTState().equals(FTState.NOT_STARTED))
		{
			// here as we are starting new upload, we have to create the new session id
			X_SESSION_ID = UUID.randomUUID().toString();
			Logger.d(getClass().getSimpleName(), "SESSION_ID: " + X_SESSION_ID);
		}
		else if (fst.getFTState().equals(FTState.PAUSED) || fst.getFTState().equals(FTState.ERROR))
		{
			/*
			 * In case user paused the transfer during the last chunk. The Upload was completed and the response from server was stored with the state file. So when resumed, the
			 * response is read from state file. If this is not null the response is returned.
			 */
			if (fst.getResponseJson() != null)
			{
				_state = FTState.COMPLETED;
				deleteStateFile();

				responseJson = fst.getResponseJson();
				return responseJson;
			}
			X_SESSION_ID = fst.getSessionId();
			if(X_SESSION_ID != null)
				mStart = AccountUtils.getBytesUploaded(String.valueOf(X_SESSION_ID));
			else
				mStart = 0;
			if (mStart <= 0)
			{
				X_SESSION_ID = UUID.randomUUID().toString();
				mStart = 0;
			}
			Logger.d(getClass().getSimpleName(), "SESSION_ID: " + X_SESSION_ID);
		}
		long length = sourceFile.length();
		if (length < 1)
		{
			throw new FileNotFoundException("File size less than 1 byte");
		}
		if (mStart >= length)
		{
			mStart = 0;
			X_SESSION_ID = UUID.randomUUID().toString();
		}
		// @GM setting transferred bytes if there are any
		setBytesTransferred(mStart);
		mUrl = new URL(AccountUtils.fileTransferUploadBase + "/user/pft/");
		RandomAccessFile raf = new RandomAccessFile(sourceFile, "r");
		raf.seek(mStart);

		setChunkSize();
		if (chunkSize > length)
			chunkSize = (int) length;
		setBufferSize();

		String boundaryMesssage = getBoundaryMessage();
		String boundary = "\r\n--" + BOUNDARY + "--\r\n";

		int start = mStart;
		int end = (int) length;
		if (end > (start + chunkSize))
			end = start + chunkSize;
		end--;

		byte[] fileBytes = setupFileBytes(boundaryMesssage, boundary, chunkSize);

		while (end < length && responseJson == null)
		{
			if (_state != FTState.IN_PROGRESS) // this is to check if user has PAUSED or cancelled the upload
				break;

			Logger.d(getClass().getSimpleName(), "bytes " + start + "-" + end + "/" + length + ";  chunk:" + chunkSize + ";  buffer:" + bufferSize);
			boolean resetAndUpdate = false;
			int bytesRead = raf.read(fileBytes, boundaryMesssage.length(), chunkSize);
			if (bytesRead == -1)
			{
				raf.close();
				throw new IOException("Exception in partial read. files ended");
			}
			String contentRange = "bytes " + start + "-" + end + "/" + length;
			String responseString = send(contentRange, fileBytes);

			if (end == (length - 1) && responseString != null)
			{
				Logger.d(getClass().getSimpleName(), "response: " + responseString);
				responseJson = new JSONObject(responseString);
				incrementBytesTransferred(chunkSize);
				resetAndUpdate = true; // To update UI
			}
			else
			{

				// In case there is error uploading this chunk
				if (responseString == null)
				{
					if (shouldRetry())
					{
						if (freshStart)
						{
							raf.close();
							return null;
						}
						raf.seek(start);
						setChunkSize();
						if (chunkSize > length)
							chunkSize = (int) length;
						if (end != (start + chunkSize - 1))
						{
							end = (start + chunkSize - 1);
							fileBytes = setupFileBytes(boundaryMesssage, boundary, chunkSize);
						}
						setBufferSize();
					}
					else
					{
						raf.close();
						throw new IOException("Exception in partial upload. response null");
					}

				}
				// When the chunk uploaded successfully
				else
				{
					start += chunkSize;
					incrementBytesTransferred(chunkSize);
					saveIntermediateProgress(X_SESSION_ID);
					resetAndUpdate = true; // To reset retry logic and update UI

					end = (int) length;
					if (end >= (start + chunkSize))
					{
						end = start + chunkSize;
						end--;
					}
					else
					{
						end--;
						chunkSize = end - start + 1;
						fileBytes = setupFileBytes(boundaryMesssage, boundary, chunkSize);
					}
				}
			}

			/*
			 * Resetting reconnect logic Updating UI
			 */
			if (resetAndUpdate)
			{
				retry = true;
				reconnectTime = 0;
				retryAttempts = 0;
				temp = _bytesTransferred;
				temp *= 100;
				temp /= _totalSize;
				progressPercentage = (int) temp;
				if(_state != FTState.PAUSED)
					LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED));
			}
		}

		switch (_state)
		{
		case CANCELLED:
			Logger.d(getClass().getSimpleName(), "FT Cancelled");
			deleteStateFile();
			break;
		case IN_PROGRESS:
			Logger.d(getClass().getSimpleName(), "FT Completed");
			_state = FTState.COMPLETED;
			deleteStateFile();
			break;
		case PAUSED:
			_state = FTState.PAUSED;
			Logger.d(getClass().getSimpleName(), "FT PAUSED");
			// In case upload was complete response JSON is to be saved not the Session_ID
			if (responseJson != null)
				saveFileState(X_SESSION_ID,responseJson);
			else
				saveFileState(X_SESSION_ID);
			break;
		default:
			break;
		}
		try
		{
			// we don't want to screw up result even if inputstream is not closed
			raf.close();
		}
		catch (IOException e)
		{

		}
		return responseJson;
	}

	private void setBufferSize()
	{
		bufferSize = 1;
		while ((bufferSize * 2) < chunkSize)
			bufferSize *= 2;
	}

	private byte[] setupFileBytes(String boundaryMesssage, String boundary, int chunkSize)
	{
		byte[] fileBytes = new byte[boundaryMesssage.length() + chunkSize + boundary.length()];
		System.arraycopy(boundaryMesssage.getBytes(), 0, fileBytes, 0, boundaryMesssage.length());
		System.arraycopy(boundary.getBytes(), 0, fileBytes, boundaryMesssage.length() + chunkSize, boundary.length());
		return fileBytes;
	}

	String getBoundaryMessage()
	{
		String sendingFileType = "";
		if (HikeConstants.LOCATION_CONTENT_TYPE.equals(fileType) || HikeConstants.CONTACT_CONTENT_TYPE.equals(fileType)
				|| HikeConstants.VOICE_MESSAGE_CONTENT_TYPE.equals(fileType))
		{
			sendingFileType = fileType;
		}
		StringBuffer res = new StringBuffer("--").append(BOUNDARY).append("\r\n");
		String name = selectedFile.getName();
		try
		{
			name = URLEncoder.encode(selectedFile.getName(), "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Logger.d(getClass().getSimpleName(), "encode file name: " + name);
		res.append("Content-Disposition: form-data; name=\"").append("file").append("\"; filename=\"").append(name).append("\"\r\n").append("Content-Type: ")
				.append(sendingFileType).append("\r\n\r\n");
		return res.toString();
	}
	
	private File getStateFile(ConvMessage msg)
	{
		HikeFile file = msg.getMetadata().getHikeFiles().get(0);
		return new File(FileTransferManager.getInstance(context).getHikeTempDir(), file.getFileName() + ".bin." + msg.getMsgID());
	}
	
	@Override
	protected void deleteStateFile()
	{
		if (isMultiMsg)
		{
			for (ConvMessage msg:messageList)
			{
				super.deleteStateFile(getStateFile(msg));
			}
		}
		else
		{
			super.deleteStateFile();
		}
	}
	
	@Override
	protected void saveFileState(String sessionId)
	{
		if (isMultiMsg)
		{
			for (ConvMessage msg:messageList)
			{
				super.saveFileState(getStateFile(msg), _state, sessionId, null);
			}
		}
		else
		{
			super.saveFileState(sessionId);
		}
	}
	
	@Override
	protected void saveFileKeyState(String fileKey)
	{
		if (isMultiMsg)
		{
			for (ConvMessage msg:messageList)
			{
				super.saveFileKeyState(getStateFile(msg), fileKey);
			}
		}
		else
		{
			super.saveFileKeyState(fileKey);
		}
	}

	private boolean isFileKeyValid() throws Exception
	{
		if (TextUtils.isEmpty(fileKey)){
			msgId = ((ConvMessage) userContext).getMsgID();
			HikeFile hikeFile = ((ConvMessage) userContext).getMetadata().getHikeFiles().get(0);
			selectedFile = new File(hikeFile.getFilePath());
			FileSavedState fst = FileTransferManager.getInstance(context).getUploadFileState(selectedFile, msgId);
			deleteStateFile();
			if(fst != null && !TextUtils.isEmpty(fst.getFileKey())){
				fileKey = fst.getFileKey();
			}else
				return false;
		}
		// If we are not able to verify the filekey validity from the server, fall back to uploading the file		
		final int MAX_RETRY = 3;
		int retry =0;
		while(retry < MAX_RETRY)
		{
			try
			{
				mUrl = new URL(AccountUtils.fileTransferBaseDownloadUrl + fileKey);
				HttpClient client = new DefaultHttpClient();
				client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 2 * 60 * 1000);
				HttpHead head = new HttpHead(mUrl.toString());
				head.addHeader("Cookie", "user=" + token + ";uid=" + uId);
	
				HttpResponse resp = client.execute(head);
				int resCode = resp.getStatusLine().getStatusCode();
				// Make sure the response code is 200.
				if (resCode == RESPONSE_OK)
				{
					// This is to get the file size from server
					// continue anyway if not able to obtain the size
					try
					{
						String range = resp.getFirstHeader("Content-Range").getValue();
						fileSize = Integer.valueOf(range.substring(range.lastIndexOf("/") + 1, range.length()));
					}
					catch (Exception e)
					{
						e.printStackTrace();
						fileSize = 0;
					}
					return true;
				}
				else
				{
					fileKey = null;
					return false;
				}
			}
			catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				retry++;
				if(retry == (MAX_RETRY-1))
					throw e;
			}
		}
		throw new Exception("Network error.");
	}

	/*
	 * this function was created to notify the UI but is not required for now. Not deleted if required again
	 */
	private boolean shouldSendProgress()
	{
		int x = progressPercentage / 10;
		if (x < num)
			return false;
		// @GM 'num++' will create a problem in future if with decide to increase "BUFFER_SIZE"(which we will)
		// num++;
		num = x + 1;
		return true;
	}

	private String send(String contentRange, byte[] fileBytes)
	{
		HttpClient client = new DefaultHttpClient();
		client.getParams().setParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, bufferSize);
		client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 2 * 60 * 1000);
		// client.getParams().setParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false);
		long time = System.currentTimeMillis();
		HttpPost post = new HttpPost(mUrl.toString());
		String res = null;
		int resCode = 0;
		try
		{
			post.addHeader("Connection", "Keep-Alive");
			post.addHeader("Content-Name", selectedFile.getName());
			post.addHeader("X-Thumbnail-Required", "0");
			post.addHeader("X-SESSION-ID", X_SESSION_ID);
			post.addHeader("X-CONTENT-RANGE", contentRange);
			post.addHeader("Cookie", "user=" + token + ";UID=" + uId);
			Logger.d(getClass().getSimpleName(), "user=" + token + ";UID=" + uId);
			post.setHeader("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);

			post.setEntity(new ByteArrayEntity(fileBytes));
			HttpResponse response = client.execute(post);
			resCode = response.getStatusLine().getStatusCode();
			res = EntityUtils.toString(response.getEntity());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.e(getClass().getSimpleName(), "FT Upload error : " + e.getMessage());
			if (e.getMessage() == null)
			{
				error();
				res = null;
				retry = false;
			}
			return null;
		}
		if (resCode != 0 && resCode != RESPONSE_OK && resCode != RESPONSE_ACCEPTED)
		{
			error();
			res = null;
			if (retryAttempts >= MAX_RETRY_ATTEMPTS || resCode == RESPONSE_BAD_REQUEST || resCode == RESPONSE_NOT_FOUND)
			{
				retry = false;
			}
			else if (resCode == INTERNAL_SERVER_ERROR)
			{
				deleteStateFile();
				_state = FTState.IN_PROGRESS;
				freshStart = true;
			}
			else if (resCode >= 400)
			{
				_state = FTState.IN_PROGRESS;
				freshStart = true;
			}
		}
		time = System.currentTimeMillis() - time;
		Logger.d(getClass().getSimpleName(), "Upload time: " + time / 1000 + "." + time % 1000 + "s.  Response: " + resCode);
		return res;
	}

	private FTResult closeStreams(ByteArrayOutputStream bos, InputStream is, HttpURLConnection hc)
	{
		try
		{
			if (bos != null)
				bos.close();
		}
		catch (Exception e2)
		{
			e2.printStackTrace();
		}
		try
		{
			if (is != null)
				is.close();
		}
		catch (Exception e2)
		{
			e2.printStackTrace();
		}
		try
		{
			if (hc != null)
				hc.disconnect();
		}
		catch (Exception e2)
		{
			e2.printStackTrace();
		}
		return FTResult.SUCCESS;
	}

	private byte[] getPostBytes(byte[] fileBytes)
	{
		// ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] postBytes = null;
		String boundaryMesssage = getBoundaryMessage();
		String boundary = "\r\n--" + BOUNDARY + "--\r\n";
		postBytes = new byte[boundaryMesssage.length() + fileBytes.length + boundary.length()];
		System.arraycopy(boundaryMesssage.getBytes(), 0, postBytes, 0, boundaryMesssage.length());
		System.arraycopy(fileBytes, 0, postBytes, boundaryMesssage.length(), fileBytes.length);
		System.arraycopy(boundary.getBytes(), 0, postBytes, boundaryMesssage.length() + fileBytes.length, boundary.length());
		// try
		// {
		// bos.write(getBoundaryMessage(contentRange).getBytes());
		// bos.write(fileBytes);
		// bos.write(("\r\n--" + BOUNDARY + "--\r\n").getBytes());
		// postBytes = bos.toByteArray();
		// bos.flush();
		// bos.close();
		// }
		// catch (IOException e)
		// {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// finally
		// {
		// if (bos != null)
		// try
		// {
		// bos.close();
		// }
		// catch (IOException e)
		// {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
		return postBytes;
	}

	private void error()
	{
		_state = FTState.ERROR;
		saveFileState(X_SESSION_ID);
	}

	public void postExecute(FTResult result)
	{
		Logger.d(getClass().getSimpleName(), "PostExecute--> Thread Details : " + Thread.currentThread().toString() + "Time : " + System.currentTimeMillis() / 1000);
		Logger.d(getClass().getSimpleName(), result.toString());
		if (userContext != null)
		{
			removeTask();
			this.pausedProgress = -1;
			if(result != FTResult.PAUSED)
				LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED));
		}

		if (result != FTResult.PAUSED && result != FTResult.SUCCESS)
		{
			final int errorStringId = result == FTResult.READ_FAIL ? R.string.unable_to_read : result == FTResult.CANCELLED ? R.string.upload_cancelled
					: result == FTResult.FAILED_UNRECOVERABLE ? R.string.upload_failed : result == FTResult.CARD_UNMOUNT ? R.string.card_unmount
							: result == FTResult.DOWNLOAD_FAILED ? R.string.download_failed : R.string.upload_failed;

			handler.post(new Runnable()
			{
				@Override
				public void run()
				{
					Toast.makeText(context, errorStringId, Toast.LENGTH_SHORT).show();
				}
			});
		}
		if (selectedFile != null && hikeFileType != HikeFileType.AUDIO_RECORDING)
		{
			context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(selectedFile)));
		}
	}
	
	private void removeTask()
	{
		if (isMultiMsg)
		{
			for (ConvMessage msg: messageList)
			{
				FileTransferManager.getInstance(context).removeTask(msg.getMsgID());
			}
		}
		else
		{
			FileTransferManager.getInstance(context).removeTask(((ConvMessage) userContext).getMsgID());
		}
	}
	
	private String getImageQuality(){
		SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		int quality = appPrefs.getInt(HikeConstants.IMAGE_QUALITY, ImageQuality.QUALITY_DEFAULT);
		String imageQuality = ImageQuality.IMAGE_QUALITY_DEFAULT;
		switch (quality)
		{
		case ImageQuality.QUALITY_ORIGINAL:
			imageQuality = ImageQuality.IMAGE_QUALITY_ORIGINAL;
			break;
		case ImageQuality.QUALITY_MEDIUM:
			imageQuality = ImageQuality.IMAGE_QUALITY_MEDIUM;
			break;
		case ImageQuality.QUALITY_SMALL:
			imageQuality = ImageQuality.IMAGE_QUALITY_SMALL;
			break;
		}
		return imageQuality;
	}
}
