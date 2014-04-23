package com.bsb.hike.ui.utils;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
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
			if (resultCode == activity.RESULT_OK)
			{
				String encryptedPattern = String.valueOf(data.getCharArrayExtra(LockPatternActivity.EXTRA_PATTERN));
				HikeSharedPreferenceUtil.getInstance(activity).saveData(HikeMessengerApp.STEALTH_ENCRYPTED_PATTERN, encryptedPattern);
				HikeSharedPreferenceUtil.getInstance(activity).saveData(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, true);
			}
			break;// _ReqCreateLockPattern

		case HikeConstants.ResultCodes.CONFIRM_LOCK_PATTERN:
			String msg = "";

			switch (resultCode)
			{
			case Activity.RESULT_OK:
				msg = "Ok ok";
				break;
			case Activity.RESULT_CANCELED:
				msg = "canceled";
				break;
			case LockPatternActivity.RESULT_FAILED:
				msg = "failed";
				break;
			default:
				return;
			}

			Toast toast = Toast.makeText(activity, msg, Toast.LENGTH_SHORT);
			toast.setDuration(Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();

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

	public static void createNewPattern(Activity activity)
	{
		Intent i = new Intent(LockPatternActivity.ACTION_CREATE_PATTERN, null, activity, LockPatternActivity.class);
		i.putExtra(LockPatternActivity.EXTRA_THEME, getThemeForLockPatternActivity());
		i.putExtra(Settings.Security.METADATA_ENCRYPTER_CLASS, LPEncrypter.class);
		i.putExtra(Settings.Security.METADATA_AUTO_SAVE_PATTERN, true);
		i.putExtra(Settings.Display.METADATA_MIN_WIRED_DOTS, mBarMinWiredDots);
		activity.startActivityForResult(i, HikeConstants.ResultCodes.CREATE_LOCK_PATTERN);
	}// onClick()

	public static void confirmPattern(Activity activity)
	{
		Intent i = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null, activity, LockPatternActivity.class);
		String encryptedPattern = HikeSharedPreferenceUtil.getInstance(activity).getData(HikeMessengerApp.STEALTH_ENCRYPTED_PATTERN, "");
		i.putExtra(LockPatternActivity.EXTRA_PATTERN, encryptedPattern.toCharArray());
		i.putExtra(LockPatternActivity.EXTRA_THEME, getThemeForLockPatternActivity());
		i.putExtra(Settings.Security.METADATA_ENCRYPTER_CLASS, LPEncrypter.class);
		i.putExtra(Settings.Security.METADATA_AUTO_SAVE_PATTERN, true);
		i.putExtra(Settings.Display.METADATA_MIN_WIRED_DOTS, mBarMaxTries);
		activity.startActivityForResult(i, HikeConstants.ResultCodes.CONFIRM_LOCK_PATTERN);
	}// onClick()

	public class LPEncrypter implements com.haibison.android.lockpattern.util.IEncrypter
	{

		@Override
		public char[] encrypt(Context context, List<Cell> pattern)
		{
			// CRC32 c = new CRC32();
			// c.update(s.getBytes());
			// pattern.get(0).
			// return String.format("%08x", c.getValue());
			return LockPatternUtils.patternToString(pattern).toCharArray();
		}

		@Override
		public List<Cell> decrypt(Context context, char[] encryptedPattern)
		{
			return LockPatternUtils.stringToPattern(String.valueOf(encryptedPattern));
		}
	}

}
