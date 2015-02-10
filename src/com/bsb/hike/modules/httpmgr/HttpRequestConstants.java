package com.bsb.hike.modules.httpmgr;

public class HttpRequestConstants
{
	private static boolean isProduction = true;

	private static boolean isSSL = false;

	private static final String HTTP = "http://";

	private static final String HTTPS = "https://";

	private static final String PRODUCTION_API = "api.im.hike.in";

	private static final String STAGING_API = "staging.im.hike.in";

	private static String BASE_URL = HTTP + STAGING_API;

	private static final String BASE_V1 = "/v1";

	private static final String BASE_V2 = "/v2";
	
	private static final String BASE_ACCOUNT = "/account";

	private static final String BASE_USER = "/user";

	private static final String BASE_STICKER = "/stickers";

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
		return BASE_URL + BASE_V1 + BASE_STICKER;
	}

	public static String stickerSignupUpgradeUrl()
	{
		return BASE_URL + BASE_V1 + BASE_STICKER + "/categories";
	}

	public static String stickerShopDownloadUrl()
	{
		return BASE_URL + BASE_V1 + BASE_STICKER + "/shop";
	}

	public static String stickerPalleteImageDownloadUrl()
	{
		return BASE_URL + BASE_V1 + BASE_STICKER + "/enable_disable";
	}

	public static String stickerPreviewImageDownloadUrl()
	{
		return BASE_URL + BASE_V1 + BASE_STICKER + "/preview";
	}

	public static String multiStickerDownloadUrl()
	{
		return BASE_URL + BASE_V1 + BASE_STICKER;
	}

	public static String lastSeenUrl()
	{
		return BASE_URL + BASE_V1 + BASE_USER + "/lastseen";
	}

	public static String bulkLastSeenUrl()
	{
		return BASE_URL + BASE_V2 + BASE_USER + "/bls";
	}

	public static String getStatusBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_USER + "/status";
	}
	
	public static String getHikeJoinTimeBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/profile/";
	}
	
	public static String postDeviceDetailsBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/update";
	}
	
	public static String postGreenBlueDetailsBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/info";
	}
	
	public static String socialCredentialsBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/connect/";
	}
}
