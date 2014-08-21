package com.bsb.hike.ui.utils;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;
import com.haibison.android.lockpattern.LockPatternActivity;
import com.haibison.android.lockpattern.util.Settings;
import com.haibison.android.lockpattern.widget.LockPatternUtils;
import com.haibison.android.lockpattern.widget.LockPatternView.Cell;

public class LockPattern
{
	public static int mBarMinWiredDots = 4;

	private static int mBarMaxTries = 5;

	char[] encryptedPattern;

	public static void onLockActivityResult(Activity activity, int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
		case HikeConstants.ResultCodes.CREATE_LOCK_PATTERN:
			boolean isReset = data.getBooleanExtra(HikeConstants.Extras.STEALTH_PASS_RESET, false);
			if (resultCode == activity.RESULT_OK)
			{
				String encryptedPattern = String.valueOf(data.getCharArrayExtra(LockPatternActivity.EXTRA_PATTERN));
				HikeSharedPreferenceUtil.getInstance(activity).saveData(HikeMessengerApp.STEALTH_ENCRYPTED_PATTERN, encryptedPattern);
				HikeSharedPreferenceUtil.getInstance(activity).saveData(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, true);
				//only firing this event if this is not the password reset flow
				if (!isReset)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_STEALTH_FTUE_ENTER_PASS_TIP, null);
					Utils.sendUILogEvent(HikeConstants.LogEvent.STEALTH_FTUE_DONE);
				}
			}
			else
			{
				//making this check so that we can find out if this is password reset flow or otherwise
				if(!isReset)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.CLEAR_FTUE_STEALTH_CONV, null);
				}
			}
			break;// _ReqCreateLockPattern

		case HikeConstants.ResultCodes.CONFIRM_LOCK_PATTERN:
			switch (resultCode)
			{
			case Activity.RESULT_OK:
				HikeSharedPreferenceUtil.getInstance(activity).saveData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_ON);
				HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_MODE_TOGGLED, true);
				HikeAnalyticsEvent.sendStealthEnabled(true);
				break;
			case Activity.RESULT_CANCELED:
				HikeSharedPreferenceUtil.getInstance(activity).saveData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);
				HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_MODE_TOGGLED, false);
				Utils.sendUILogEvent(HikeConstants.LogEvent.ENTER_WRONG_STEALTH_MODE);
				break;
			case LockPatternActivity.RESULT_FAILED:
				break;
			default:
				return;
			}

			break;
		case HikeConstants.ResultCodes.CONFIRM_AND_ENTER_NEW_PASSWORD:
			//adding this to handle the case where the user is selecting a new password
			switch (resultCode)
			{
			case Activity.RESULT_OK:
				LockPattern.createNewPattern(activity, true);
				break;
			case Activity.RESULT_CANCELED:
				break;
			case LockPatternActivity.RESULT_FAILED:
				break;
			default:
				return;
			}
			break;
		}
	}

	/**
	 * Gets the theme that the user chose to apply to {@link LockPatternActivity}.
	 * 
	 * @return the theme for {@link LockPatternActivity}.
	 */
	public static int getThemeForLockPatternActivity()
	{

		return R.style.Alp_42447968_Theme_Dialog_Dark;
	}// getThemeForLockPatternActivity()

	/**
	 * This method creates a new pattern.
	 * @param activity
	 * @param isResetPassword 
	 */
	public static void createNewPattern(Activity activity, boolean isResetPassword)
	{
		Intent i = new Intent(LockPatternActivity.ACTION_CREATE_PATTERN, null, activity, LockPatternActivity.class);
		i.putExtra(LockPatternActivity.EXTRA_THEME, getThemeForLockPatternActivity());
		i.putExtra(Settings.Security.METADATA_AUTO_SAVE_PATTERN, true);
		i.putExtra(Settings.Display.METADATA_MIN_WIRED_DOTS, mBarMinWiredDots);
		i.putExtra(HikeConstants.Extras.STEALTH_PASS_RESET, isResetPassword);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		activity.startActivityForResult(i, HikeConstants.ResultCodes.CREATE_LOCK_PATTERN);
	}// onClick()

	/**
	 * This method validates an existing password
	 * @param activity
	 * @param isResetPassword  
	 */
	public static void confirmPattern(Activity activity, boolean isResetPassword)
	{
		Intent i = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null, activity, LockPatternActivity.class);
		String encryptedPattern = HikeSharedPreferenceUtil.getInstance(activity).getData(HikeMessengerApp.STEALTH_ENCRYPTED_PATTERN, "");
		i.putExtra(LockPatternActivity.EXTRA_PATTERN, encryptedPattern.toCharArray());
		i.putExtra(LockPatternActivity.EXTRA_THEME, getThemeForLockPatternActivity());
		i.putExtra(Settings.Security.METADATA_AUTO_SAVE_PATTERN, true);
		i.putExtra(HikeConstants.Extras.STEALTH_PASS_RESET, isResetPassword);
		i.putExtra(Settings.Display.METADATA_MIN_WIRED_DOTS, mBarMaxTries);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		activity.startActivityForResult(i, isResetPassword?HikeConstants.ResultCodes.CONFIRM_AND_ENTER_NEW_PASSWORD:HikeConstants.ResultCodes.CONFIRM_LOCK_PATTERN);
	}// onClick()

}
