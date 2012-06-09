package com.bsb.hike;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.bsb.hike.db.DbConversationListener;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.service.HikeMqttManager.MQTTConnectionStatus;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.service.HikeServiceConnection;
import com.bsb.hike.ui.WelcomeActivity;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.ActivityTimeLogger;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.ToastListener;

public class HikeMessengerApp extends Application
{
	public static final String ACCOUNT_SETTINGS = "accountsettings";

	public static final String MSISDN_SETTING = "msisdn";

	public static final String CARRIER_SETTING = "carrier";

	public static final String NAME_SETTING = "name";

	public static final String TOKEN_SETTING = "token";

	public static final String MESSAGES_SETTING = "messageid";

	public static final String UID_SETTING = "uid";
	
	public static final String UPDATE_SETTING = "update";

	public static final String ANALYTICS = "analytics";

	public static final String ADDRESS_BOOK_SCANNED = "abscanned";

	public static final String CONTACT_LIST_EMPTY = "contactlistempty";

	public static final String SMS_SETTING = "smscredits";

	public static final String NAME = "name";

	public static final String ACCEPT_TERMS = "acceptterms";

	public static final String CONNECTED_ONCE = "connectedonce";

	public static final String MESSAGES_LIST_TOOLTIP_DISMISSED = "messageslist_tooltip";

	public static final String SPLASH_SEEN = "splashseen";

	public static final String INVITE_TOOLTIP_DISMISSED = "inviteToolTip";

	public static final String EMAIL = "email";

	public static final String CHAT_INVITE_TOOL_TIP_DISMISSED = "chatInviteToolTipDismissed";

	public static final String INVITED = "invited";

	public static final String INVITED_JOINED = "invitedJoined";

	public static final String CHAT_GROUP_INFO_TOOL_TIP_DISMISSED = "chatGroupInfoToolTipDismissed";

	private static HikePubSub mPubSubInstance;

	private static Messenger mMessenger;

	private NetworkManager mNetworkManager;

	private Messenger mService;

	private HikeServiceConnection mServiceConnection;

	private boolean mInitialized;

	class IncomingHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			Log.d("HikeMessengerApp", "In handleMessage " + msg.what);
			switch (msg.what)
			{
				case HikeService.MSG_APP_PUBLISH:
					Log.d("HikeMessengerApp", "received message " );
					Bundle bundle = msg.getData();
					String message = bundle.getString("msg");
					Log.d("HikeMessengerApp", "received message " + message);
					mPubSubInstance.publish(HikePubSub.WS_RECEIVED, message);
					break;
				case HikeService.MSG_APP_MESSAGE_STATUS:
					boolean success = msg.arg1 != 0;
					Long msgId = (Long) msg.obj;
					Log.d("HikeMessengerApp", "received msg status msgId:" + msgId + " state: " + success);
					String event = success ? HikePubSub.SERVER_RECEIVED_MSG : HikePubSub.MESSAGE_FAILED;
					mPubSubInstance.publish(event, msgId);
					break;
				case HikeService.MSG_APP_CONN_STATUS:
					Log.d("HikeMessengerApp", "received connection status " + msg.arg1);
					int s = msg.arg1;
					MQTTConnectionStatus status = MQTTConnectionStatus.values()[s];
					mPubSubInstance.publish(HikePubSub.CONNECTION_STATUS, status);
					break;
				case HikeService.MSG_APP_INVALID_TOKEN:
					Log.d("HikeMessengerApp", "received invalid token message from service");
					HikeMessengerApp.this.disconnectFromService();
					HikeMessengerApp.this.stopService(new Intent(HikeMessengerApp.this, HikeService.class));
					HikeMessengerApp.this.startActivity(new Intent(HikeMessengerApp.this, WelcomeActivity.class));
			}
		}
	}

	static
	{
		mPubSubInstance = new HikePubSub();
	}

	public void sendToService(Message message)
	{
		try
		{
			mService.send(message);
		}
		catch (RemoteException e)
		{
			Log.e("HikeMessengerApp", "Unable to connect to service", e);
		}
	}

	public void disconnectFromService()
	{
		if (mInitialized)
		{
			synchronized(HikeMessengerApp.class)
			{
				if (mInitialized)
				{
					mInitialized = false;
					unbindService(mServiceConnection);
					mServiceConnection = null;
				}
			}
		}
	}

	public void connectToService()
	{
		Log.d("HikeMessengerApp", "calling connectToService:" + mInitialized);
		if (!mInitialized)
		{
			synchronized(HikeMessengerApp.class)
			{
				if (!mInitialized)
				{
					mInitialized = true;
					Log.d("HikeMessengerApp", "Initializing service");
					mServiceConnection = HikeServiceConnection.createConnection(this, mMessenger);
				}
			}
		}
	}

	public void onCreate()
	{
		super.onCreate();

		SmileyParser.init(this);

		IconCacheManager.init(this);
		/* add the db write listener */
		new DbConversationListener(getApplicationContext());

		/* add the generic websocket listener. This will turn strings into objects and re-broadcast them */
		mNetworkManager = NetworkManager.getInstance(getApplicationContext());

		/* add a handler to handle toasts. The object initializes itself it it's constructor */
		new ToastListener(getApplicationContext());

		mMessenger = new Messenger(new IncomingHandler());

		SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
		if (token != null)
		{
			AccountUtils.setToken(token);
		}
		/*For logging the time each activity is seen by the user*/
		new ActivityTimeLogger();
	}

	public static HikePubSub getPubSub()
	{
		return mPubSubInstance;
	}

	public void setService(Messenger service)
	{
		this.mService = service;
	}

}
