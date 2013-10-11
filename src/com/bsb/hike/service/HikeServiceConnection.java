package com.bsb.hike.service;

import org.json.JSONObject;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;

public class HikeServiceConnection implements HikePubSub.Listener,
		ServiceConnection {
	private Messenger mService;

	private HikeMessengerApp mApp;

	private Messenger mMessenger;

	public HikeServiceConnection(HikeMessengerApp app, Messenger messenger) {
		this.mApp = app;
		this.mMessenger = messenger;
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MQTT_PUBLISH, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MQTT_PUBLISH_LOW,
				this);
		HikeMessengerApp.getPubSub()
				.addListener(HikePubSub.TOKEN_CREATED, this);
	}

	public void onServiceConnected(ComponentName className, IBinder service) {
		Log.d("HikeServiceConnection", "connection established");
		// This is called when the connection with the service has been
		// established, giving us the service object we can use to
		// interact with the service. We are communicating with our
		// service through an IDL interface, so get a client-side
		// representation of that from the raw service object.
		mService = new Messenger(service);
		mApp.setService(mService);

		try {
			Message msg = Message.obtain();
			msg.what = HikeService.MSG_APP_CONNECTED;
			msg.replyTo = this.mMessenger;
			mService.send(msg);

			HikeMessengerApp.getPubSub().publish(HikePubSub.SERVICE_STARTED,
					null);

		} catch (RemoteException e) {
			Log.e("HikeServiceConncetion", "Couldn't connect to service", e);
			// In this case the service has crashed before we could even
			// do anything with it; we can count on soon being
			// disconnected (and then reconnected if it can be restarted)
			// so there is no need to do anything here.
		}
	}

	public void onServiceDisconnected(ComponentName className) {
		Log.d("HikeServiceConnection", "Connection disconnected");
		// This is called when the connection with the service has been
		// unexpectedly disconnected -- that is, its process crashed.
		mService = null;
		mConnection = null;
		this.mApp.setService(null);
	}

	private static HikeServiceConnection mConnection;

	public static HikeServiceConnection createConnection(
			HikeMessengerApp hikeMessengerApp, Messenger mMessenger) {
		synchronized (HikeServiceConnection.class) {
			if (mConnection == null) {
				Log.i("HikeserviceConnection", "creating connection");
				mConnection = new HikeServiceConnection(hikeMessengerApp,
						mMessenger);
			}
		}

		Log.d("HikeServiceConnection", "binding to service");
		hikeMessengerApp.startService(new Intent(hikeMessengerApp,
				HikeService.class));
		hikeMessengerApp.bindService(new Intent(hikeMessengerApp,
				HikeService.class), mConnection, Context.BIND_AUTO_CREATE);
		return mConnection;
	}

	@Override
	public void onEventReceived(String type, Object object) {
		if (mService == null) {
			Log.e("HikeServiceConnection", "Unable to publish message ");
			return;
		}

		Message msg;
		if (HikePubSub.TOKEN_CREATED.equals(type)) {
			msg = Message.obtain();
			msg.what = HikeService.MSG_APP_TOKEN_CREATED;
			msg.replyTo = this.mMessenger;
		} else {
			JSONObject o = (JSONObject) object;
			String data = o.toString();
			msg = Message.obtain();
			msg.what = HikeService.MSG_APP_PUBLISH;
			Bundle bundle = new Bundle();
			bundle.putString(HikeConstants.MESSAGE, data);

			/* set the QoS */
			msg.arg1 = HikePubSub.MQTT_PUBLISH_LOW.equals(type) ? 0 : 1;

			/*
			 * if this is a message, then grab the messageId out of the json
			 * object so we can get confirmation of success/failure
			 */
			if (HikeConstants.MqttMessageTypes.MESSAGE.equals(o
					.optString(HikeConstants.TYPE))
					|| (HikeConstants.MqttMessageTypes.INVITE.equals(o
							.optString(HikeConstants.TYPE)))) {
				JSONObject json = o.optJSONObject(HikeConstants.DATA);
				long msgId = Long.parseLong(json
						.optString(HikeConstants.MESSAGE_ID));
				bundle.putLong(HikeConstants.MESSAGE_ID, msgId);
			}

			msg.setData(bundle);
			msg.replyTo = this.mMessenger;
		}

		try {
			mService.send(msg);
		} catch (RemoteException e) {
			/* Service is dead. What to do? */
			Log.e("HikeServiceConnection", "Remote Service dead", e);
		}
	}
};
