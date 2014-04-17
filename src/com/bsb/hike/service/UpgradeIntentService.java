package com.bsb.hike.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;

public class UpgradeIntentService extends IntentService
{

	private static final String TAG = "UpgradeIntentService";

	private SharedPreferences prefs;

	Context context;

	@Override
	protected void onHandleIntent(Intent dbIntent)
	{
		makeRoundedThumbsForUserDb();

		initialiseSharedMediaAndFileThumbnailTable();
		context = this;
		prefs = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		// setting the preferences to 2 to indicate we're done with the
		// migration !
		Editor editor = prefs.edit();
		editor.putInt(HikeConstants.UPGRADE_AVATAR_CONV_DB, 2);
		editor.putInt(HikeConstants.UPGRADE_AVATAR_PROGRESS_USER, 2);
		editor.putBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false);
		editor.commit();

		// fire the pubsub event to let the HomeActivity class know that the
		// avatar
		// upgrade is done and it can stop the spinner
		HikeMessengerApp.getPubSub().publish(HikePubSub.FINISHED_AVTAR_UPGRADE, null);
	}

	public UpgradeIntentService()
	{

		super(TAG);

	}

	private void makeRoundedThumbsForUserDb()
	{
		HikeUserDatabase.getInstance().makeOlderAvatarsRounded();
	}

	private void initialiseSharedMediaAndFileThumbnailTable()
	{
		HikeConversationsDatabase.getInstance().initialiseSharedMediaAndFileThumbnailTable();
	}

}