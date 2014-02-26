package com.bsb.hike.filetransfer;

import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
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
import com.bsb.hike.http.CustomByteArrayEntity;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.ProgressListener;
import com.bsb.hike.utils.Utils;
import com.google.android.maps.GeoPoint;

public class UploadContactOrLocationTask extends FileTransferBase
{
	private static final String STATIC_MAP_UNFORMATTED_URL = "http://maps.googleapis.com/maps/api/staticmap?center=%1$f,%2$f&zoom=%3$d&size=%4$dx%4$d&markers=size:mid|color:red|%1$f,%2$f&sensor=false";

	private String msisdn;

	private double latitude;

	private double longitude;

	private int zoomLevel;

	private String address;

	private JSONObject contactJson;

	private boolean uploadingContact;

	protected long maxSize = 100; // just to avoid divide by zero operation exception

	private boolean isRecipientOnhike;

	private FutureTask<FTResult> futureTask;

	protected UploadContactOrLocationTask(Handler handler, ConcurrentHashMap<Long, FutureTask<FTResult>> fileTaskMap, Context ctx, String msisdn, double latitude,
			double longitude, int zoomLevel, boolean isRecipientOnhike)
	{
		super(handler, fileTaskMap, ctx, null, -1, null);
		this.latitude = latitude;
		this.longitude = longitude;
		this.zoomLevel = zoomLevel;
		this.msisdn = msisdn;
		this.uploadingContact = false;
		this.isRecipientOnhike = isRecipientOnhike;
		_state = FTState.INITIALIZED;
		createConvMessage();
	}

	protected UploadContactOrLocationTask(Handler handler, ConcurrentHashMap<Long, FutureTask<FTResult>> fileTaskMap, Context ctx, String msisdn, JSONObject contactJson,
			boolean isRecipientOnhike)
	{
		super(handler, fileTaskMap, ctx, null, -1, null);
		this.msisdn = msisdn;
		this.contactJson = contactJson;
		this.uploadingContact = true;
		this.isRecipientOnhike = isRecipientOnhike;
		_state = FTState.INITIALIZED;
		createConvMessage();
	}

	protected UploadContactOrLocationTask(Handler handler, ConcurrentHashMap<Long, FutureTask<FTResult>> fileTaskMap, Context ctx, Object convMessage, boolean uploadingContact,
			boolean isRecipientOnhike)
	{
		super(handler, fileTaskMap, ctx, null, -1, null);
		this.userContext = convMessage;
		this.uploadingContact = uploadingContact;
		this.isRecipientOnhike = isRecipientOnhike;
		_state = FTState.INITIALIZED;
	}

	protected void setFutureTask(FutureTask<FTResult> fuTask)
	{
		futureTask = fuTask;
		fileTaskMap.put(((ConvMessage) userContext).getMsgID(), futureTask);
	}

	@Override
	public FTResult call()
	{
		try
		{
			if (userContext == null)
			{
				createConvMessage();
			}
			if (!uploadingContact)
			{
				HikeFile hikeFile = ((ConvMessage) userContext).getMetadata().getHikeFiles().get(0);
				latitude = hikeFile.getLatitude();
				longitude = hikeFile.getLongitude();
				address = hikeFile.getAddress();
				
				if(address == null)
					address = Utils.getAddressFromGeoPoint(new GeoPoint((int) (latitude * 1E6), (int) (longitude * 1E6)), context);

				if(TextUtils.isEmpty(hikeFile.getThumbnailString()))
				{
					fetchThumbnailAndUpdateConvMessage(latitude, longitude, zoomLevel, address, (ConvMessage) userContext);
				}
			}

			if (!fileTaskMap.containsKey(((ConvMessage) userContext).getMsgID()))
				fileTaskMap.put(((ConvMessage) userContext).getMsgID(), futureTask);

			boolean fileWasAlreadyUploaded = true;
			// If we don't have a file key, that means we haven't uploaded the
			// file to the server yet
			if (TextUtils.isEmpty(fileKey))
			{
				fileWasAlreadyUploaded = false;

				JSONObject response = executeFileTransferRequest(null, uploadingContact ? HikeConstants.CONTACT_FILE_NAME : HikeConstants.LOCATION_FILE_NAME,
						((ConvMessage) userContext).getMetadata().getJSON(), uploadingContact ? HikeConstants.CONTACT_CONTENT_TYPE : HikeConstants.LOCATION_CONTENT_TYPE);

				JSONObject fileJSON = response.getJSONObject("data");
				fileKey = fileJSON.optString(HikeConstants.FILE_KEY);
			}

			JSONObject metadata = new JSONObject();
			JSONArray filesArray = new JSONArray();

			HikeFile hikeFile = ((ConvMessage) userContext).getMetadata().getHikeFiles().get(0);
			hikeFile.setFileKey(fileKey);
			hikeFile.setFileTypeString(uploadingContact ? HikeConstants.CONTACT_CONTENT_TYPE : HikeConstants.LOCATION_CONTENT_TYPE);

			filesArray.put(hikeFile.serialize());
			Log.d(getClass().getSimpleName(), "JSON FINAL: " + hikeFile.serialize());
			metadata.put(HikeConstants.FILES, filesArray);

			((ConvMessage) userContext).setMetadata(metadata);

			// If the file was just uploaded to the servers, we want to publish
			// this event
			if (!fileWasAlreadyUploaded)
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.UPLOAD_FINISHED, (ConvMessage) userContext);
			}

			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, (ConvMessage) userContext);
		}
		catch (Exception e)
		{
			Log.e(getClass().getSimpleName(), "Exception", e);
			return FTResult.UPLOAD_FAILED;
		}
		return FTResult.SUCCESS;
	}

	private JSONObject executeFileTransferRequest(String filePath, String fileName, JSONObject request, String fileType) throws Exception
	{

		HttpClient httpClient = AccountUtils.getClient();

		HttpContext httpContext = new BasicHttpContext();

		HttpPut httpPut = new HttpPut(AccountUtils.fileTransferUploadBase + "/user/ft");

		AccountUtils.addToken(httpPut);
		httpPut.addHeader("Connection", "Keep-Alive");
		httpPut.addHeader("Content-Name", fileName);
		Log.d("Upload", "Content type: " + fileType);
		httpPut.addHeader("Content-Type", TextUtils.isEmpty(fileType) ? "" : fileType);
		httpPut.addHeader("X-Thumbnail-Required", "0");
		final AbstractHttpEntity entity;
		entity = new CustomByteArrayEntity(request.toString().getBytes(), new ProgressListener()
		{
			@Override
			public void transferred(long num)
			{
				incrementBytesTransferred((int) num);
				progressPercentage = (int) ((num / (float) maxSize) * 100);
			}
		});
		maxSize = entity.getContentLength();

		httpPut.setEntity(entity);
		HttpResponse response = httpClient.execute(httpPut, httpContext);
		String serverResponse = EntityUtils.toString(response.getEntity());

		JSONObject responseJSON = new JSONObject(serverResponse);
		if ((responseJSON == null) || (!"ok".equals(responseJSON.optString("stat"))))
		{
			throw new NetworkErrorException("Unable to perform request");
		}
		return responseJSON;
	}

	private JSONObject getFileTransferMetadataForLocation(double latitude, double longitude, int zoomLevel, String address, String thumbnailString) throws JSONException
	{
		JSONArray files = new JSONArray();
		files.put(new HikeFile(latitude, longitude, zoomLevel, address, thumbnailString, null).serialize());
		JSONObject metadata = new JSONObject();
		metadata.put(HikeConstants.FILES, files);

		return metadata;
	}

	private JSONObject getFileTransferMetadataForContact(JSONObject contactJson) throws JSONException
	{
		contactJson.put(HikeConstants.FILE_NAME, contactJson.optString(HikeConstants.NAME, HikeConstants.CONTACT_FILE_NAME));
		contactJson.put(HikeConstants.CONTENT_TYPE, HikeConstants.CONTACT_CONTENT_TYPE);
		JSONArray files = new JSONArray();
		files.put(contactJson);
		JSONObject metadata = new JSONObject();
		metadata.put(HikeConstants.FILES, files);

		return metadata;
	}

	private void createConvMessage()
	{
		try
		{
			JSONObject metadata;
			if (!uploadingContact)
			{
				metadata = getFileTransferMetadataForLocation(latitude, longitude, zoomLevel, null, null);
			}
			else
			{
				metadata = getFileTransferMetadataForContact(contactJson);
			}

			userContext = createConvMessage(msisdn, metadata);
			long id = ((ConvMessage) userContext).getMsgID();
			if (TextUtils.isEmpty(fileKey))
			{
				// Called so that the UI in the Conversation lists screen is
				// updated
				HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, (ConvMessage) userContext);
			}
		}
		catch (Exception e)
		{
			Log.e(getClass().getSimpleName(), "Exception", e);
			return;
		}
	}

	private ConvMessage createConvMessage(String msisdn, JSONObject metadata) throws JSONException
	{
		long time = System.currentTimeMillis() / 1000;
		ConvMessage convMessage = new ConvMessage(HikeConstants.LOCATION_FILE_NAME, msisdn, time, ConvMessage.State.SENT_UNCONFIRMED);
		convMessage.setMetadata(metadata);
		convMessage.setSMS(!isRecipientOnhike);
		HikeConversationsDatabase.getInstance().addConversationMessages(convMessage);

		HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_MESSAGE_CREATED, convMessage);
		return convMessage;
	}

	private void fetchThumbnailAndUpdateConvMessage(double latitude, double longitude, int zoomLevel, String address, ConvMessage convMessage) throws Exception
	{
		String staticMapUrl = String.format(Locale.US, STATIC_MAP_UNFORMATTED_URL, latitude, longitude, zoomLevel, HikeConstants.MAX_DIMENSION_LOCATION_THUMBNAIL_PX);
		Log.d(getClass().getSimpleName(), "Static map url: " + staticMapUrl);

		Bitmap thumbnail = BitmapFactory.decodeStream((InputStream) new URL(staticMapUrl).getContent());
		String thumbnailString = Base64.encodeToString(Utils.bitmapToBytes(thumbnail, Bitmap.CompressFormat.JPEG), Base64.DEFAULT);

		JSONObject metadata = getFileTransferMetadataForLocation(latitude, longitude, zoomLevel, address, thumbnailString);

		convMessage.setMetadata(metadata);
		HikeConversationsDatabase.getInstance().updateMessageMetadata(convMessage.getMsgID(), convMessage.getMetadata());
		// HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
		Intent intent = new Intent(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}

	protected void postExecute(FTResult result)
	{
		fileTaskMap.remove(((ConvMessage) userContext).getMsgID());
		Log.d(getClass().getSimpleName(), "error display: removing" + ((ConvMessage) userContext).getMsgID());
		if (userContext != null)
		{
			// HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
			Intent intent = new Intent(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED);
			LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
		}

		if (result != FTResult.SUCCESS)
		{
			if (result == FTResult.UPLOAD_FAILED)
			{
				handler.post(new Runnable()
				{
					@Override
					public void run()
					{
						Toast.makeText(context, R.string.upload_failed, Toast.LENGTH_SHORT).show();
					}
				});
			}
		}
	}

}
