package com.bsb.hike.modules.httpmgr;

public class HttpRequestConstants
{
	private static boolean isProduction = true;
	
	private static boolean isSSL = false;
	
	private static final String HTTP = "http://";
	
	private static final String HTTPS = "https://";
	
	private static final String PRODUCTION_API = "api.im.hike.in";
	
	private static final String STAGING_API = "staging.im.hike.in";
	
	private static String BASE_URL = HTTP + PRODUCTION_API;
	
	private static final String BASE_V1 = "/v1";
	
	private static final String STICKERS_BASE = "/stickers";
	
	public static synchronized void toggleStaging(boolean production)
	{
		isProduction = production;
		changeBaseUrl();
	}
	
	public static synchronized void toggleSSL(boolean ssl)
	{
		isSSL = ssl;
		changeBaseUrl();
	}
	
	private static void changeBaseUrl()
	{
		BASE_URL = "";
		BASE_URL += (isSSL) ? HTTPS : HTTP;
		BASE_URL += (isProduction) ? PRODUCTION_API : STAGING_API;
	}
	
	
	
	/*********************************************************************************************************************************************/
	
	public static String singleStickerDownloadBase()
	{
		return BASE_URL + BASE_V1 + STICKERS_BASE;
	}
	
	public static String stickerSignupUpgradeUrl()
	{
		return BASE_URL + BASE_V1 + STICKERS_BASE + "/categories";
	}
	
	public static String stickerShopDownloadUrl()
	{
		return BASE_URL + BASE_V1 + STICKERS_BASE + "/shop";
	}
	
	public static String stickerPalleteImageDownloadUrl()
	{
		return BASE_URL + BASE_V1 + STICKERS_BASE + "/enable_disable";
	}
	
	public static String stickerPreviewImageDownloadUrl()
	{
		return BASE_URL + BASE_V1 + STICKERS_BASE + "/preview";
	}
	
	public static String multiStickerDownloadUrl()
	{
		return BASE_URL + BASE_V1 + STICKERS_BASE;
	}
}
