package com.bsb.hike.utils;

import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;

public class AppRater
{

	private final static int[] LAUNCHES_UNTIL_PROMPT = { 5, 10, 25, 50, 100 };

	private static boolean showingDialog = false;

	public static void appLaunched(Context mContext)
	{
		showingDialog = false;

		SharedPreferences prefs = mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		if (prefs.getBoolean(HikeMessengerApp.DONT_SHOW_APP_RATER, false))
		{
			return;
		}

		SharedPreferences.Editor editor = prefs.edit();

		// Increment launch counter
		int launchCount = prefs.getInt(HikeMessengerApp.APP_LAUNCHES, 0) + 1;
		editor.putInt(HikeMessengerApp.APP_LAUNCHES, launchCount);
		editor.commit();

		for (int launch : LAUNCHES_UNTIL_PROMPT)
		{
			if (launch == launchCount)
			{
				showRateDialog(mContext, prefs.edit());
			}
		}
	}

	private static void showRateDialog(final Context mContext, final SharedPreferences.Editor editor)
	{
		Builder builder = new Builder(mContext);

		builder.setTitle(R.string.app_rate_title);
		builder.setMessage(R.string.app_rate_content);

		builder.setPositiveButton(R.string.rate_now, new OnClickListener()
		{

			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + mContext.getPackageName()));
				marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				try
				{
					mContext.startActivity(marketIntent);
				}
				catch (ActivityNotFoundException e)
				{
					Log.e("AppRater", "Unable to open market");
				}
				dialog.dismiss();
				editor.putBoolean(HikeMessengerApp.DONT_SHOW_APP_RATER, true);
				editor.commit();
			}
		});

		builder.setNeutralButton(R.string.ask_me_later, new OnClickListener()
		{

			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
			}
		});

		builder.setNegativeButton(R.string.no_thanks_rate, new OnClickListener()
		{

			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
				editor.putBoolean(HikeMessengerApp.DONT_SHOW_APP_RATER, true);
				editor.commit();
			}
		});

		showingDialog = true;
		builder.show();
	}

	public static boolean showingDialog()
	{
		return showingDialog;
	}
}