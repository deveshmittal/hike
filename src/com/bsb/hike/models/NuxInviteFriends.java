package com.bsb.hike.models;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.utils.Utils;

public class NuxInviteFriends
{
	private String mainReward;

	private String subTextReward;

	private String buttext;

	private boolean toggleskipbutton;

	private String image;

	private Bitmap bitmap;

	StringToBitmap mmStringToBitmap;
	public NuxInviteFriends(String mainReward, String subTextReward, String buttext, String image, boolean skip_toggle_button)
	{

		this.mainReward = mainReward;
		this.subTextReward = subTextReward;
		this.buttext = buttext;
		this.image = image;
		this.toggleskipbutton = skip_toggle_button;
		if (!TextUtils.isEmpty(image))
		{
			mmStringToBitmap = new StringToBitmap(image);
			HikeHandlerUtil.getInstance().postRunnableWithDelay(mmStringToBitmap, 0);
		}
		else
		{
			bitmap = BitmapFactory.decodeResource(HikeMessengerApp.getInstance().getResources(), R.drawable.ic_nuxinvite);
		}
	}

	class StringToBitmap implements Runnable
	{
		private String base64image = null;

		public StringToBitmap(String base64)
		{
			base64image = base64;
		}

		@Override
		public void run()
		{
			bitmap = HikeBitmapFactory.stringToBitmap(base64image);
		}

	}

	/**
	 * @return the mainReward
	 */
	public String getRewardTitle()
	{
		return mainReward;
	}

	/**
	 * @return the subTextReward
	 */
	public String getRewardSubText()
	{
		return subTextReward;
	}

	/**
	 * @return the buttext
	 */
	public String getButText()
	{
		return buttext;
	}

	/**
	 * @return the skip_toggle_button
	 */
	public boolean isToggleSkipButton()
	{
		return toggleskipbutton;
	}

	/**
	 * @return the bitmap
	 */
	public Bitmap getImageBitmap()
	{
		return bitmap;
	}

}
