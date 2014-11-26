package com.bsb.hike.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Pair;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.model.HikeUser;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.sdk.HikeSDKResponseCode;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.ui.HikeAuthActivity;
import com.bsb.hike.utils.HikeSDKConstants;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * Handles requests made by HikeSDK.
 * 
 * @author AtulM
 * 
 */
public class HikeSDKRequestHandler extends Handler implements Listener
{
	private Context mContext;

	private static Message cachedMessage;

	private String cachedToken;

	private Handler authUIHandler;

	public HikeSDKRequestHandler(final Context argContext, Looper looper)
	{
		super(looper);
		authUIHandler = new Handler()
		{
			public void handleMessage(android.os.Message msg)
			{
				Logger.d("THREAD", Thread.currentThread().getName());
				Intent hikeAuthIntent = new Intent(argContext, HikeAuthActivity.class);
				hikeAuthIntent.putExtra(HikeAuthActivity.MESSAGE_INDEX, Message.obtain(msg));
				hikeAuthIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				argContext.startActivity(hikeAuthIntent);
			};
		};

		mContext = argContext;
		HikeMessengerApp.getPubSub().addListener(HikePubSub.AUTH_TOKEN_RECEIVED, HikeSDKRequestHandler.this);
	}

	/**
	 * Return error/failed operation message back to caller messenger
	 * 
	 * @param argMessage
	 */
	private void returnExceptionMessageToCaller(Message argMessage)
	{
		argMessage.arg2 = HikeSDKResponseCode.STATUS_EXCEPTION;
		try
		{
			Logger.i("hikesdk", argMessage.replyTo.hashCode() + "");
			argMessage.replyTo.send(argMessage);
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
		return;
	}

	@Override
	public void handleMessage(Message msg)
	{
		super.handleMessage(msg);

		Logger.d(HikeSDKRequestHandler.class.getCanonicalName(), "Handle message: Verifying!");

		if (msg.arg2 == HikeSDKResponseCode.STATUS_FAILED)
		{
			returnExceptionMessageToCaller(msg);
			return;
		}

		if (!HikeAuthActivity.verifyRequest(mContext, msg))
		{
			cachedMessage = Message.obtain(msg);
			authUIHandler.sendMessage(Message.obtain(msg));
			return;
		}
		Logger.d(HikeSDKRequestHandler.class.getCanonicalName(), "Handle message: Verified!");

		Logger.d(HikeSDKRequestHandler.class.getCanonicalName(), "message.what== " + msg.what);

		if (msg.what == HikeService.SDK_REQ_GET_LOGGED_USER_INFO)
		{

			Bundle reqUserInfoBundle = msg.getData();

			if (reqUserInfoBundle == null || reqUserInfoBundle.isEmpty())
			{
				returnExceptionMessageToCaller(msg);
				return;
			}

			// User info is saved in shared preferences
			SharedPreferences preferences = mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE);

			ContactInfo userInfo = Utils.getUserContactInfo(preferences);

			JSONObject reqUserInfoResponseJSON = new JSONObject();

			JSONArray reqUserInfoJSONArray = new JSONArray();

			if (userInfo != null)
			{
				JSONObject friendJSON = new JSONObject();

				String contactId = userInfo.getId();

				String contactName = userInfo.getNameOrMsisdn();

				if (!TextUtils.isEmpty(contactId) && !TextUtils.isEmpty(contactName))
				{
					try
					{
						friendJSON.put(HikeUser.HIKE_USER_ID_KEY, "-1");

						friendJSON.put(HikeUser.HIKE_USER_NAME_KEY, contactName);
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}

					reqUserInfoJSONArray.put(friendJSON);
				}
			}
			try
			{
				reqUserInfoResponseJSON.put(HikeUser.HIKE_USERS_LIST_ID, reqUserInfoJSONArray);

				if (cachedToken != null)
				{
					reqUserInfoResponseJSON.put(HikeSDKConstants.HIKE_REQ_SDK_CLIENT_ACC_TOKEN, cachedToken);
					cachedToken = null;
				}
			}
			catch (JSONException e)
			{
				returnExceptionMessageToCaller(msg);
				e.printStackTrace();
				return;
			}

			reqUserInfoBundle.putString(HikeSDKConstants.HIKE_REQ_DATA_ID, reqUserInfoResponseJSON.toString());

			msg.setData(reqUserInfoBundle);

			// Set STATUS_OK
			msg.arg2 = HikeSDKResponseCode.STATUS_OK;

			try
			{
				msg.replyTo.send(msg);
			}
			catch (RemoteException e)
			{
				returnExceptionMessageToCaller(msg);
				e.printStackTrace();
				return;
			}

		}
		else if (msg.what == HikeService.SDK_REQ_GET_USERS)
		{

			Bundle reqGetFriendsBundle = msg.getData();

			if (reqGetFriendsBundle == null || reqGetFriendsBundle.isEmpty())
			{
				returnExceptionMessageToCaller(msg);
				return;
			}

			JSONObject reqGetFriendsData = null;

			try
			{
				reqGetFriendsData = new JSONObject(reqGetFriendsBundle.getString(HikeSDKConstants.HIKE_REQ_DATA_ID));
			}
			catch (JSONException e)
			{
				e.printStackTrace();
				returnExceptionMessageToCaller(msg);
				return;
			}
			String requestFilter = "-1";
			try
			{
				requestFilter = reqGetFriendsData.getString(HikeSDKConstants.HIKE_REQ_FILTER_ID);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}

			List<ContactInfo> contacts = null;

			int reqFilterKey = 0;
			try
			{
				reqFilterKey = Integer.parseInt(requestFilter);
			}
			catch (NumberFormatException nfe)
			{
				nfe.printStackTrace();
				returnExceptionMessageToCaller(msg);
				return;
			}

			switch (reqFilterKey)
			{
			case -1:
				// Favourites
				contacts = ContactManager.getInstance().getContactsOfFavoriteType(FavoriteType.FRIEND, 1, "");
				break;
			case -2:
				// On hike
				contacts = new ArrayList<ContactInfo>();
				List<ContactInfo> allContacts = ContactManager.getInstance().getAllContacts();
				for (ContactInfo contact : allContacts)
				{
					if (contact.isOnhike())
					{
						contacts.add(contact);
					}
				}
				break;
			case -3:
				// Not on hike
				contacts = new ArrayList<ContactInfo>();
				List<Pair<AtomicBoolean, ContactInfo>> nonHikePair = ContactManager.getInstance().getNonHikeContacts();
				for (Pair<AtomicBoolean, ContactInfo> pair : nonHikePair)
				{
					contacts.add(pair.second);
				}
				break;
			default:
				// All contacts
				contacts = ContactManager.getInstance().getAllContacts();
			}

			JSONObject responseJSON = new JSONObject();

			JSONArray friendsListJSON = new JSONArray();

			for (ContactInfo contact : contacts)
			{
				JSONObject friendJSON = new JSONObject();

				String contactId = contact.getId();

				String contactName = contact.getNameOrMsisdn();

				if (!TextUtils.isEmpty(contactId) && !TextUtils.isEmpty(contactName))
				{
					try
					{
						friendJSON.put(HikeUser.HIKE_USER_ID_KEY, contactId);
						friendJSON.put(HikeUser.HIKE_USER_NAME_KEY, contactName);
					}
					catch (JSONException jsonException)
					{
						jsonException.printStackTrace();
					}
					friendsListJSON.put(friendJSON);
				}
			}

			try
			{
				responseJSON.put(HikeUser.HIKE_USERS_LIST_ID, friendsListJSON);

				if (cachedToken != null)
				{
					responseJSON.put(HikeSDKConstants.HIKE_REQ_SDK_CLIENT_ACC_TOKEN, cachedToken);
					cachedToken = null;
				}
			}
			catch (JSONException e)
			{
				returnExceptionMessageToCaller(msg);
				e.printStackTrace();
				return;
			}

			reqGetFriendsBundle.putString(HikeSDKConstants.HIKE_REQ_DATA_ID, responseJSON.toString());

			msg.setData(reqGetFriendsBundle);

			// Set STATUS_OK
			msg.arg2 = HikeSDKResponseCode.STATUS_OK;

			try
			{
				msg.replyTo.send(msg);
			}
			catch (RemoteException e)
			{
				returnExceptionMessageToCaller(msg);
				e.printStackTrace();
				return;
			}

		}
		else if (msg.what == HikeService.SDK_REQ_SEND_MESSAGE)
		{
			Bundle reqSendMessageBundle = msg.getData();

			if (reqSendMessageBundle == null)
			{
				returnExceptionMessageToCaller(msg);
				return;
			}

			HikeMessengerApp.getPubSub().publish(HikePubSub.HIKE_SDK_MESSAGE, reqSendMessageBundle.get(HikeSDKConstants.HIKE_REQ_DATA_ID));

			if (cachedToken != null)
			{
				try
				{
					JSONObject sendMessageResponseJSON = new JSONObject();
					sendMessageResponseJSON.put(HikeSDKConstants.HIKE_REQ_SDK_CLIENT_ACC_TOKEN, cachedToken);
					Bundle sendMessageBundle = new Bundle();
					sendMessageBundle.putString(HikeSDKConstants.HIKE_REQ_DATA_ID, sendMessageResponseJSON.toString());
					msg.setData(sendMessageBundle);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}

			// Set STATUS_OK
			msg.arg2 = HikeSDKResponseCode.STATUS_OK;

			try
			{
				msg.replyTo.send(msg);
			}
			catch (RemoteException e)
			{
				returnExceptionMessageToCaller(msg);
				e.printStackTrace();
				return;
			}

		}
		else if (msg.what == HikeService.SDK_REQ_AUTH_CLIENT)
		{
			Bundle reqSendMessageBundle = msg.getData();

			if (reqSendMessageBundle == null)
			{
				returnExceptionMessageToCaller(msg);
				return;
			}
			// Set STATUS_OK
			msg.arg2 = HikeSDKResponseCode.STATUS_OK;

			try
			{
				msg.replyTo.send(msg);
			}
			catch (RemoteException e)
			{
				returnExceptionMessageToCaller(msg);
				e.printStackTrace();
				return;
			}
		}
		else
		{
			returnExceptionMessageToCaller(msg);
			return;
		}

	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (type.equals(HikePubSub.AUTH_TOKEN_RECEIVED))
		{
			try
			{
				cachedToken = (String) object;
				if (cachedMessage != null)
				{
					Bundle cachedMessageBundle = cachedMessage.getData();

					String cachedMessageData = cachedMessageBundle.getString(HikeSDKConstants.HIKE_REQ_DATA_ID);

					try
					{
						JSONObject cachedMessageDataJSON = new JSONObject(cachedMessageData);
						cachedMessageDataJSON.put(HikeSDKConstants.HIKE_REQ_SDK_CLIENT_ACC_TOKEN, cachedToken);
						cachedMessageBundle.putString(HikeSDKConstants.HIKE_REQ_DATA_ID, cachedMessageDataJSON.toString());
						cachedMessage.setData(cachedMessageBundle);
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}
					catch (NullPointerException e)
					{
						e.printStackTrace();
					}

					handleMessage(cachedMessage);

					cachedMessage = null;
				}
			}
			catch (ClassCastException cce)
			{
				cce.printStackTrace();
				if (cachedMessage != null)
				{
					returnExceptionMessageToCaller(cachedMessage);
					cachedMessage = null;
					return;
				}
			}
		}

	}
}