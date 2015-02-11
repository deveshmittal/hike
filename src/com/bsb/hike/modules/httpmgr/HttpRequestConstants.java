package com.bsb.hike.modules.httpmgr;


public class HttpRequestConstants
{
	private static boolean isProduction = true;

	private static boolean isSSL = false;

	private static final String HTTP = "http://";

	private static final String HTTPS = "https://";

	private static final String PRODUCTION_API = "api.im.hike.in";

	private static final String STAGING_API = "staging.im.hike.in";
	
	public static final int PRODUCTION_PORT = 80;

	public static final int PRODUCTION_PORT_SSL = 443;

	public static final int STAGING_PORT = 80;

	public static final int STAGING_PORT_SSL = 443;
	
	public static final int PORT = STAGING_PORT;

	private static String BASE_URL = HTTP + STAGING_API;

	private static final String BASE_V1 = "/v1";

	private static final String BASE_V2 = "/v2";

	private static final String BASE_ACCOUNT = "/account";

	private static final String BASE_USER = "/user";

	private static final String BASE_STICKER = "/stickers";

	private static final String BASE_INVITE = "/invite";

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

	public static String sendUserLogsInfoBaseUrl()
	{
		return BASE_URL + BASE_V1 + "/";
	}

	public static String getGroupBaseUrl()
	{
		return BASE_URL + BASE_V1 + "/group/";
	}

	public static String getAvatarBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/avatar";
	}

	public static String getStaticAvatarBaseUrl()
	{
		return BASE_URL + ":" + PORT + "/static/avatars/";
	}

	public static String postToSocialNetworkBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/spread";
	}
	
	public static String sendTwitterInviteBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_INVITE + "/twitter";
	}

	public static String registerAccountBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT;
	}
	
	public static String validateNumberBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/validate";
	}
	
	public static String setProfileUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/profile";
	}
	
	public static String postAddressbookBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/addressbook";
	}
	
	public static String updateAddressbookBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/addressbook";
	}
	
	public static String sendDeviceDetailBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/device";
	}
	
	public static String preActivationBaseUrl()
	{
		return BASE_URL + BASE_V1 + "/pa";
	}
}
