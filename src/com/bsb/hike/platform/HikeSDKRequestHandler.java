package com.bsb.hike.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.bsb.hike.utils.HikeSDKConstants;
import com.bsb.hike.utils.Utils;

/**
 * Handles requests made by HikeSDK. Implements pub sub listener which is where all the requests are retrieved.
 * 
 * @author AtulM
 * 
 */
public class HikeSDKRequestHandler implements Listener
{
	private Context mContext;

	private static HikeSDKRequestHandler hikeSDKReqHandler;

	private String[] mHikePubSubListeners = { HikePubSub.SDK_REQ_GET_INFO, HikePubSub.SDK_REQ_GET_USERS, HikePubSub.SDK_REQ_SEND_MSG };

	private HikeSDKRequestHandler(Context argContext)
	{
		this.mContext = argContext;
		HikeMessengerApp.getPubSub().addListeners(this, mHikePubSubListeners);
	}

	public static HikeSDKRequestHandler getInstance(Context argContext)
	{
		if (hikeSDKReqHandler == null)
		{
			hikeSDKReqHandler = new HikeSDKRequestHandler(argContext.getApplicationContext());
		}
		return hikeSDKReqHandler;
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
			argMessage.replyTo.send(argMessage);
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
		return;
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.SDK_REQ_GET_INFO.equals(type))
		{

			if (!(object instanceof Message))
			{
				return;
			}

			Message msg = (Message) object;
			Bundle reqUserInfoBundle = msg.getData();

			if (reqUserInfoBundle == null || reqUserInfoBundle.isEmpty())
			{
				returnExceptionMessageToCaller(msg);
				return;
			}

			// TODO: Authenticate request

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
			}
			catch (JSONException e)
			{
				returnExceptionMessageToCaller(msg);
				e.printStackTrace();
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
			}

		}
		else if (HikePubSub.SDK_REQ_GET_USERS.equals(type))
		{
			if (!(object instanceof Message))
			{
				return;
			}

			Message msg = (Message) object;

			Bundle reqGetFriendsBundle = msg.getData();

			if (reqGetFriendsBundle == null || reqGetFriendsBundle.isEmpty())
			{
				returnExceptionMessageToCaller(msg);
				return;
			}

			// TODO: Authenticate request
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
			}
			catch (JSONException e)
			{
				returnExceptionMessageToCaller(msg);
				e.printStackTrace();
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
			}

		}
		else if (HikePubSub.SDK_REQ_SEND_MSG.equals(type))
		{
			if (!(object instanceof Message))
			{
				return;
			}

			Message msg = (Message) object;

			Bundle reqSendMessageBundle = msg.getData();

			if (reqSendMessageBundle == null)
			{
				returnExceptionMessageToCaller(msg);
				return;
			}

			HikeMessengerApp.getPubSub().publish(HikePubSub.HIKE_SDK_MESSAGE, reqSendMessageBundle.get(HikeSDKConstants.HIKE_REQ_DATA_ID));

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
			}

		}
	}
}