package com.bsb.hike;

import java.net.URISyntaxException;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class HikeService extends Service {

    private PushManager mPushManager;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("HikeService", "Start Command Called");
        if (mPushManager == null) {
            Log.d("HikeService", "creating push manager");
            try {
                mPushManager = new PushManager();
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
