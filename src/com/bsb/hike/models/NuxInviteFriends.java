package com.bsb.hike.models;

import android.graphics.Bitmap;
import android.text.TextUtils;

import com.bsb.hike.utils.Utils;

public class NuxInviteFriends
{
	private String mainReward;

	private String subTextReward;

	private String buttext;

	private boolean toggleskipbutton;

	private String image;

	private Bitmap bitmap;

	public NuxInviteFriends(String mainReward, String subTextReward, String buttext, String image, boolean skip_toggle_button)
	{

		this.mainReward = mainReward;
		this.subTextReward = subTextReward;
		this.buttext = buttext;
		this.image = image;
		this.toggleskipbutton = skip_toggle_button;
		if (!TextUtils.isEmpty(image))
		{
			bitmap = Utils.stringToBitmap(image);
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
