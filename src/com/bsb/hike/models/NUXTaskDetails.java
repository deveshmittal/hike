package com.bsb.hike.models;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.NUXConstants;

public class NUXTaskDetails implements NUXConstants
{

	private String incentiveId;

	private String activityId;

	private int min;

	private int incentiveAmount;

	private boolean isNuxSkippable;

	/**
	 * @param incentiveId
	 * @param activityId
	 * @param incrMax
	 * @param incrMin
	 * @param min
	 * @param max
	 * @param incentiveAmount
	 * @param isNuxSkippable
	 */
	public NUXTaskDetails(String incentiveId, String activityId, int min, int max, int incentiveAmount, boolean isNuxSkippable)
	{
		super();
		this.incentiveId = incentiveId;
		this.activityId = activityId;
		this.min = min;
		this.max = max;
		this.isNuxSkippable = isNuxSkippable;
		this.incentiveAmount = incentiveAmount;
	}

	private int max;

	/**
	 * @return the incentiveId
	 */
	public String getIncentiveId()
	{
		return incentiveId;
	}

	/**
	 * @return the activityId
	 */
	public String getActivityId()
	{
		return activityId;
	}

	/**
	 * @return the min
	 */
	public int getMin()
	{
		return min;
	}

	/**
	 * @return the max
	 */
	public int getMax()
	{
		return max;
	}

	public int getIncentiveAmount()
	{
		return incentiveAmount;
	}

	public boolean isNuxSkippable()
	{
		return isNuxSkippable;
	}
}
