package com.bsb.hike;

import java.net.URISyntaxException;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

public class HikeService extends Service {

    private PushManager mPushManager;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("HikeService", "Start Command Called");
        SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
        String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
        String uid = settings.getString(HikeMessengerApp.UID_SETTING, null);

        if ((uid == null) || (token == null)) {
           Log.i("HikeService", "No token or uid, not starting push connection");
           return START_STICKY;
        }

        if (mPushManager == null) {
            Log.d("HikeService", "creating push manager");
            try {
                mPushManager = new PushManager(this, uid, token);
            } catch (URISyntaxException e) {
                Log.e("HikeService", "Unable to start mqtt listener");
                return START_FLAG_RETRY;
            }
            Log.d("HikeService", "Starting push manager");
            mPushManager.start();
        }
        return START_STICKY;
    }

    @Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
