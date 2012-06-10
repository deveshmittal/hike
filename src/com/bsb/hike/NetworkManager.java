package com.bsb.hike;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.utils.IconCacheManager;

/**
 * 
 * @author Vijay , Gautam 
 * Class should be singleton as only one instance is required to manage the components.
 *
 */

public class NetworkManager implements HikePubSub.Listener
{

	/* message read by recipient */
	public static final String MESSAGE_READ = "mr";

	public static final String MESSAGE = "m";

	public static final String SMS_CREDITS = "sc";

	public static final String DELIVERY_REPORT = "dr";

	public static final String USER_JOINED = "uj";

	public static final String USER_LEFT = "ul";

	public static final String START_TYPING = "st";

	public static final String END_TYPING = "et";

	public static final String INVITE = "i";

	public static final String ICON = "ic";

	public static final String INVITE_INFO = "ii";

	public static final String GROUP_CHAT_JOIN = "gcj";

	public static final String GROUP_CHAT_LEAVE = "gcl";

	public static final String GROUP_CHAT_END = "gce";

	public static final String GROUP_CHAT_NAME = "gcn";

	public static final String ANALYTICS_EVENT = "le";

	private HikePubSub pubSub;

	private static volatile NetworkManager instance;

	private Context context;

	private NetworkManager(Context context)
	{
		pubSub = HikeMessengerApp.getPubSub();
		pubSub.addListener(HikePubSub.WS_RECEIVED, this);
		this.context = context;
	}

	public static NetworkManager getInstance(Context context)
	{
		if (instance == null)
		{
			synchronized (NetworkManager.class)
			{
				if (instance == null)
				{
					instance = new NetworkManager(context);
				}
			}
		}

		return instance;
	}

	private void onMessage(String msg)
	{
		JSONObject jsonObj;
		String type;
		try
		{
			jsonObj = new JSONObject(msg);
			type = jsonObj.getString(HikeConstants.TYPE);
		}
		catch (JSONException e)
		{
			Log.e("WebSocketPublisher", "Invalid JSON message: " + msg);
			return;
		}

		String msisdn = jsonObj.optString(HikeConstants.FROM);

		if (MESSAGE.equals(type))  // this represents msg from another client through tornado server.
		{
			try
			{
				ConvMessage convMessage = new ConvMessage(jsonObj);
				this.pubSub.publish(HikePubSub.MESSAGE_RECEIVED_FROM_SENDER, convMessage);
			}
			catch (JSONException e)
			{
				Log.d("NETWORK MANAGER", "Invalid JSON", e);
			}
		}
		else if (START_TYPING.equals(type)) /* Start Typing event received*/
		{
			this.pubSub.publish(HikePubSub.TYPING_CONVERSATION, msisdn);
		}
		else if (END_TYPING.equals(type)) /* End Typing event received */
		{
			this.pubSub.publish(HikePubSub.END_TYPING_CONVERSATION, msisdn);
		}
		else if (SMS_CREDITS.equals(type)) /* SMS CREDITS */
		{
			int sms_credits = jsonObj.optInt(HikeConstants.DATA);
			this.pubSub.publish(HikePubSub.SMS_CREDIT_CHANGED, new Integer(sms_credits));
		}
		else if("sr".equals(type)) /* Represents Server has received the msg*/
		{
			String id = jsonObj.optString(HikeConstants.DATA);
			long msgID;
			try
			{
				msgID=Long.parseLong(id);
			}
			catch(NumberFormatException e)
			{
				Log.e("NETWORK MANAGER", "Exception occured while parsing msgId. Exception : "+e);
				msgID = -1;
			}
			this.pubSub.publish(HikePubSub.SERVER_RECEIVED_MSG, msgID);	
		}
		else if(DELIVERY_REPORT.equals(type)) // this handles the case when msg with msgId is recieved by the tornado server and it send back a received msg
		{
			String id = jsonObj.optString(HikeConstants.DATA);
			long msgID;
			try
			{
				msgID=Long.parseLong(id);
			}
			catch(NumberFormatException e)
			{
				Log.e("NETWORK MANAGER", "Exception occured while parsing msgId. Exception : "+e);
				msgID = -1;
			}
			Log.d("NETWORK MANAGER","Delivery report received for msgid : "+msgID +"	;	REPORT : DELIVERED");
			this.pubSub.publish(HikePubSub.MESSAGE_DELIVERED, msgID);	
		}
		else if(MESSAGE_READ.equals(type)) // Message read by recipient
		{
			JSONArray msgIds = jsonObj.optJSONArray("d");
			if(msgIds == null)
			{
				Log.e("NETWORK MANAGER", "Update Error : Message id Array is empty or null . Check problem");
				return;
			}

			long[] ids = new long[msgIds.length()];
			for (int i = 0; i < ids.length; i++)
			{
				ids[i] = msgIds.optLong(i);
			}
			Log.d("NETWORK MANAGER","Delivery report received : " +"	;	REPORT : DELIVERED READ");
			this.pubSub.publish(HikePubSub.MESSAGE_DELIVERED_READ, ids);	
		}
		else if ((USER_JOINED.equals(type)) || (USER_LEFT.equals(type)))
		{
			boolean joined = USER_JOINED.equals(type);
			this.pubSub.publish(joined ? HikePubSub.USER_JOINED : HikePubSub.USER_LEFT, msisdn);
		}
		else if ((ICON.equals(type)))
		{
			IconCacheManager.getInstance().clearIconForMSISDN(msisdn);
		}
		else if(GROUP_CHAT_JOIN.equals(type) || GROUP_CHAT_LEAVE.equals(type) || GROUP_CHAT_END.equals(type))
		{
			try
			{
				HikeConversationsDatabase hCDB = new HikeConversationsDatabase(context);
				Conversation conversation = hCDB.getConversation(jsonObj.getString(HikeConstants.TO), 0);
				hCDB.close();
				ConvMessage convMessage = new ConvMessage(jsonObj, conversation, context, false);
				this.pubSub.publish(HikePubSub.MESSAGE_RECEIVED_FROM_SENDER, convMessage);
			}
			catch (JSONException e)
			{
				Log.d("NETWORK MANAGER", "Invalid JSON", e);
			}
		}
		else if(GROUP_CHAT_END.equals(type))
		{
			String groupId = jsonObj.optString(HikeConstants.FROM);
			this.pubSub.publish(HikePubSub.GROUP_END, groupId);
		}
		else if(GROUP_CHAT_NAME.equals(type))
		{
			String groupId = jsonObj.optString(HikeConstants.FROM);
			pubSub.publish(HikePubSub.GROUP_NAME_CHANGED, groupId);
		}
		else
		{
			Log.d("WebSocketPublisher", "Unknown Type:" + type);
		}
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (type.equals(HikePubSub.WS_RECEIVED)) // signifies msg is received through web sockets.
		{
			String message = (String) object;
			onMessage(message);
		}
	}
}
