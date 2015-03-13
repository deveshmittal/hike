package com.bsb.hike.productpopup;

import java.util.Comparator;
import java.util.Locale;

import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.platform.content.PlatformContentModel;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static com.bsb.hike.productpopup.ProductPopupsConstants.*;

public class ProductContentModel
{
	public PlatformContentModel mmContentModel;

	private int triggerpoint;

	private long starttime;

	private long endtime;

	private boolean isFullScreen;

	private int hashCode = -1;

	private String formedData;

	private String notifTitle;

	private long pushTime;

	private ProductContentModel(JSONObject contentData)
	{
		this.mmContentModel = PlatformContentModel.make(contentData.toString());

		starttime = contentData.optLong(START_TIME, 0l);
		endtime = contentData.optLong(END_TIME, new Long(Integer.MAX_VALUE));
		triggerpoint = contentData.optInt(TRIGGER_POINT,PopupTriggerPoints.HOME_SCREEN.ordinal());
		isFullScreen = contentData.optBoolean(IS_FULL_SCREEN, false);
		pushTime = contentData.optLong(PUSH_TIME, 0l);
	}

	public static ProductContentModel makeProductContentModel(JSONObject contentData)
	{
		return new ProductContentModel(contentData);

	}

	/**
	 * @return the triggerpoint
	 */
	public int getTriggerpoint()
	{
		return triggerpoint;
	}

	/**
	 * @return the starttime
	 */
	public long getStarttime()
	{
		return starttime;
	}

	/**
	 * @return the endtime
	 */
	public long getEndtime()
	{
		return endtime;
	}

	public String getAppName()
	{
		return mmContentModel.cardObj.getAppName();
	}

	public String getAppVersion()
	{
		return mmContentModel.cardObj.getAppVersion();
	}

	public String getLayoutId()
	{
		return mmContentModel.cardObj.getLayoutId();
	}

	public String getAppPackage()
	{
		return mmContentModel.cardObj.getAppPackage();
	}

	public boolean shouldPlaySound()
	{
		String text = mmContentModel.cardObj.getPush();

		if (PushTypeEnum.getEnum(text)== PushTypeEnum.LOUD)
		{
			return true;
		}

		return false;
	}

	public String getUser()
	{
		return mmContentModel.cardObj.getUser();
	}

	public JsonObject getLd()
	{
		return mmContentModel.cardObj.getLd();
	}

	public int getHeight()
	{
		return Integer.parseInt(mmContentModel.cardObj.getH());
	}

	public boolean isFullScreen()
	{
		return isFullScreen;
	}

	public void setFormedData(String compiledData)
	{
		this.formedData = compiledData;
	}

	public String getNotifTitle()
	{
		return mmContentModel.cardObj.getnotifTitle();
	}

	/**
	 * Gets the hot data.
	 * 
	 * @return the hot data
	 */
	public String getFormedData()
	{
		return formedData;
	}

	public long getPushTime()
	{
		return pushTime;
	}

	@Override
	public int hashCode()
	{
		if (hashCode == -1)
		{
			hashCode = new String(getStarttime() + getTriggerpoint() + "").hashCode();
		}
		return hashCode;
	}

	public String toJSONString()
	{
		Gson gson = new Gson();
		String str = gson.toJson(mmContentModel);
		JsonParser parser = new JsonParser();
		JsonObject jsonObj = (JsonObject) parser.parse(str);

		jsonObj.addProperty(START_TIME, starttime);
		jsonObj.addProperty(END_TIME, endtime);
		jsonObj.addProperty(TRIGGER_POINT, triggerpoint);
		jsonObj.addProperty(IS_FULL_SCREEN, isFullScreen);
		jsonObj.addProperty(PUSH_TIME,pushTime);
		return jsonObj.toString();
	}

	
	/**
	 * Comparater to sort the popups based on the start time ...
	 */
	public static Comparator<ProductContentModel> ProductContentStartTimeComp = new Comparator<ProductContentModel>()
	{
		public int compare(ProductContentModel lhs, ProductContentModel rhs)
		{

			if (rhs.getStarttime() - lhs.getStarttime() > 0)
			{
				return 1;
			}
			else
				return -1;

		}
	};

	/**
	 * Validating the push
	 * @return
	 */
	
	public boolean isPushReceived()
	{	
		if (!TextUtils.isEmpty(getUser()) && !TextUtils.isEmpty(getNotifTitle()))
		{
			return true;
		}
		return false;
	}

	public boolean isPushFuture()
	{
		if (pushTime != 0l)
		{
			return true;
		}
		else
			return false;
	}
}
