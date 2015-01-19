package com.bsb.hike.platform.content;

import java.io.File;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * Content model
 */
public class PlatformContentModel
{
	private static String TAG = "PlatformContentModel";

	/**
	 * The appID.
	 */
	private String appID;

	/**
	 * The layout_id.
	 */
	private String layout_id;

	private String layout_url;

	/**
	 * The version.
	 */
	private String version;

	/**
	 * The content data.
	 */
	private JsonObject card_data;

	private String fwdcard_data;

	private String fwdlayout_id;

	boolean isForwardCard;

	/**
	 * The hot data.
	 */
	private String formedData;

	private int mHash = -1;

	private int mTemplateHash = -1;

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		if (mHash == -1)
		{
			mHash = new String(appID + layout_id + version + card_data).hashCode();
		}
		return mHash;
	}

	/**
	 * Template hash code. Replace with template UID
	 * 
	 * @return the int
	 */
	public int templateHashCode()
	{
		if (mTemplateHash == -1)
		{
			mTemplateHash = new String(layout_id + version).hashCode();
		}
		return mTemplateHash;
	}
	
	/**
	 * Make.
	 * 
	 * @param card_data
	 *            the content data
	 * @return the platform content model
	 */
	public static PlatformContentModel make(String contentData)
	{
		Log.d(TAG, "making PlatformContentModel");
		JsonParser parser = new JsonParser();
		JsonObject jsonObj = (JsonObject) parser.parse(contentData);
		PlatformContentModel object = null;
		try
		{
			object = new Gson().fromJson(jsonObj, PlatformContentModel.class);
			object.card_data.addProperty(PlatformContentConstants.KEY_TEMPLATE_PATH, PlatformContentConstants.CONTENT_AUTHORITY_BASE + object.appID + File.separator);
		}
		catch (JsonSyntaxException e)
		{
			e.printStackTrace();
			return null;
		}

		return object;
	}

	/**
	 * Make forward card data
	 * 
	 * @param card_data
	 *            the content data
	 * @return the platform content model
	 */
	public static PlatformContentModel makeForwardCardModel(String contentData)
	{
		Log.d(TAG, "making PlatformContentModel");
		JsonParser parser = new JsonParser();
		JsonObject jsonObj = (JsonObject) parser.parse(contentData);
		PlatformContentModel object = null;
		try
		{
			object = new Gson().fromJson(jsonObj, PlatformContentModel.class);
			object.isForwardCard = true;
			object.card_data.addProperty(PlatformContentConstants.KEY_TEMPLATE_PATH, PlatformContentConstants.CONTENT_AUTHORITY_BASE + object.appID + File.separator);
		}
		catch (JsonSyntaxException e)
		{
			e.printStackTrace();
			return null;
		}
		return object;
	}

	/**
	 * Gets the layout_id.
	 * 
	 * @return the layout_id
	 */
	public String getTag()
	{
		if (isForwardCard)
		{
			return fwdlayout_id;
		}
		else
		{
			return layout_id;
		}
	}

	/**
	 * Sets the layout_id.
	 * 
	 * @param layout_id
	 *            the new layout_id
	 */
	public void setTag(String tag)
	{
		this.layout_id = tag;
	}

	/**
	 * Gets the version.
	 * 
	 * @return the version
	 */
	public String getVersion()
	{
		return version;
	}

	/**
	 * Sets the version.
	 * 
	 * @param version
	 *            the new version
	 */
	public void setVersion(String version)
	{
		this.version = version;
	}

	/**
	 * Gets the content data.
	 * 
	 * @return the content data
	 */
	public String getContentJSON()
	{
		if (isForwardCard)
		{
			return fwdcard_data;
		}
		else
		{
			return card_data.toString();
		}
	}

//	/**
//	 * Sets the content data.
//	 * 
//	 * @param card_data
//	 *            the new content data
//	 */
//	public void setContentData(String contentData)
//	{
//		this.card_data = contentData;
//	}

	/**
	 * Gets the hot data.
	 * 
	 * @return the hot data
	 */
	public String getFormedData()
	{
		return formedData;
	}

	/**
	 * Sets the hot data.
	 * 
	 * @param compiledData
	 *            the new hot data
	 */
	public void setFormedData(String compiledData)
	{
		this.formedData = compiledData;
	}

	/**
	 * Gets the appID.
	 * 
	 * @return the appID
	 */
	public String getId()
	{
		return appID;
	}

	/**
	 * Sets the appID.
	 * 
	 * @param appID
	 *            the new appID
	 */
	public void setId(String id)
	{
		this.appID = id;
	}

	private PlatformContentModel()
	{
		// Cannot make objects directly
	}

	@Override
	public String toString()
	{
		return card_data + formedData;
	}

	public String getFwdcard_data()
	{
		return fwdcard_data;
	}

	public void setFwdcard_data(String fwdcard_data)
	{
		this.fwdcard_data = fwdcard_data;
	}

	public String getFwdlayout_id()
	{
		return fwdlayout_id;
	}

	public void setFwdlayout_id(String fwdlayout_id)
	{
		this.fwdlayout_id = fwdlayout_id;
	}

	public String getLayout_url()
	{
		return layout_url;
	}

	public void setLayout_url(String layout_url)
	{
		this.layout_url = layout_url;
	}

}