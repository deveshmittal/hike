package com.bsb.hike.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.nio.CharBuffer;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.http.CustomByteArrayEntity;
import com.bsb.hike.http.CustomFileEntity;
import com.bsb.hike.http.CustomSSLSocketFactory;
import com.bsb.hike.http.GzipByteArrayEntity;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.http.HttpPatch;
import com.bsb.hike.models.ContactInfo;

public class AccountUtils {

	public static final String HTTP_STRING = "http://";

	public static final String HTTPS_STRING = "https://";

	public static final String PRODUCTION_HOST = "api.im.hike.in";

	public static final String STAGING_HOST = "staging.im.hike.in";

	public static final int PRODUCTION_PORT = 80;

	public static final int PRODUCTION_PORT_SSL = 443;

	public static final int STAGING_PORT = 8080;

	public static final int STAGING_PORT_SSL = 443;

	public static String host = PRODUCTION_HOST;

	public static int port = PRODUCTION_PORT;

	public static String base = HTTP_STRING + host + "/v1";

	public static final String PRODUCTION_FT_HOST = "ft.im.hike.in";

	public static String fileTransferHost = PRODUCTION_FT_HOST;

	public static String fileTransferUploadBase = HTTP_STRING
			+ fileTransferHost + ":" + Integer.toString(port) + "/v1";

	public static final String FILE_TRANSFER_DOWNLOAD_BASE = "/user/ft/";

	public static String fileTransferBaseDownloadUrl = base
			+ FILE_TRANSFER_DOWNLOAD_BASE;

	public static final String FILE_TRANSFER_BASE_VIEW_URL_PRODUCTION = "hike.in/f/";

	public static final String FILE_TRANSFER_BASE_VIEW_URL_STAGING = "staging.im.hike.in/f/";

	public static String fileTransferBaseViewUrl = FILE_TRANSFER_BASE_VIEW_URL_PRODUCTION;

	public static final String REWARDS_PRODUCTION_BASE = "hike.in/rewards/android/";

	public static final String REWARDS_STAGING_BASE = "staging.im.hike.in/rewards/android/";

	public static String rewardsUrl = HTTP_STRING + REWARDS_PRODUCTION_BASE;

	public static final String STICKERS_PRODUCTION_BASE = "hike.in/sticker?catId=%1$s&stId=%2$s";

	public static final String STICKERS_STAGING_BASE = "staging.im.in/sticker?catId=%1$s&stId=%2$s";

	public static String stickersUrl = HTTP_STRING + STICKERS_PRODUCTION_BASE;

	public static boolean ssl = false;

	public static final String NETWORK_PREFS_NAME = "NetworkPrefs";

	public static HttpClient mClient = null;

	public static String mToken = null;

	public static String mUid = null;

	private static String appVersion = null;

	public static void setToken(String token) {
		mToken = token;
	}

	public static void setUID(String uid) {
		mUid = uid;
	}

	public static void setAppVersion(String version) {
		appVersion = version;
	}

	private static synchronized HttpClient getClient() {
		if (mClient != null) {
			return mClient;
		}
		Log.d("SSL", "Initialising the HTTP CLIENT");

		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);

		/*
		 * set the connection timeout to 6 seconds, and the waiting for data
		 * timeout to 30 seconds
		 */
		HttpConnectionParams.setConnectionTimeout(params, 6000);
		HttpConnectionParams.setSoTimeout(params, 30 * 1000);

		SchemeRegistry schemeRegistry = new SchemeRegistry();

		if (ssl) {
			try {
				KeyStore dummyTrustStore = KeyStore.getInstance(KeyStore
						.getDefaultType());
				dummyTrustStore.load(null, null);
				SSLSocketFactory sf = new CustomSSLSocketFactory(
						dummyTrustStore);
				sf.setHostnameVerifier(SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
				schemeRegistry.register(new Scheme("https", sf, port));
			} catch (Exception e) {
				schemeRegistry.register(new Scheme("http", PlainSocketFactory
						.getSocketFactory(), port));
			}
		} else {
			schemeRegistry.register(new Scheme("http", PlainSocketFactory
					.getSocketFactory(), port));
		}

		ClientConnectionManager cm = new ThreadSafeClientConnManager(params,
				schemeRegistry);
		mClient = new DefaultHttpClient(cm, params);
		mClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT,
				"android-" + appVersion);
		return mClient;
	}

	public static void addUserAgent(URLConnection urlConnection) {
		urlConnection.addRequestProperty("User-Agent", "android-" + appVersion);
	}

	public static JSONObject executeRequest(HttpRequestBase request) {
		HttpClient client = getClient();
		HttpResponse response;
		try {
			Log.d("HTTP", "Performing HTTP Request " + request.getRequestLine());
			Log.d("HTTP", "to host" + request);
			response = client.execute(request);
			Log.d("HTTP", "finished request");
			if (response.getStatusLine().getStatusCode() != 200) {
				Log.w("HTTP", "Request Failed: " + response.getStatusLine());
				return null;
			}

			HttpEntity entity = response.getEntity();
			return getResponse(entity.getContent());
		} catch (ClientProtocolException e) {
			Log.e("HTTP", "Invalid Response", e);
			e.printStackTrace();
		} catch (IOException e) {
			Log.e("HTTP", "Unable to perform request", e);
		}
		return null;
	}

	public static JSONObject getResponse(InputStream is) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder builder = new StringBuilder();
		CharBuffer target = CharBuffer.allocate(10000);
		int read = reader.read(target);
		while (read >= 0) {
			builder.append(target.array(), 0, read);
			target.clear();
			read = reader.read(target);
		}
		Log.d("HTTP", "request finished");
		try {
			return new JSONObject(builder.toString());
		} catch (JSONException e) {
			Log.e("HTTP", "Invalid JSON Response", e);
		}
		return null;
	}

	public static int sendMessage(String phone_no, String message) {
		HttpPost httppost = new HttpPost(base + "/user/msg");
		List<NameValuePair> pairs = new ArrayList<NameValuePair>(2);
		pairs.add(new BasicNameValuePair("to", phone_no));
		pairs.add(new BasicNameValuePair("body", message));
		HttpEntity entity;
		try {
			entity = new UrlEncodedFormEntity(pairs);
			httppost.setEntity(entity);
		} catch (UnsupportedEncodingException e) {
			Log.wtf("URL", "Unable to send message " + message);
			return -1;
		}

		JSONObject obj = executeRequest(httppost);
		if ((obj == null) || ("fail".equals(obj.optString("stat")))) {
			Log.w("HTTP", "Unable to send message");
			return -1;
		}

		int count = obj.optInt("sms_count");
		return count;
	}

	public static void invite(String phone_no) throws UserError {
		HttpPost httppost = new HttpPost(base + "/user/invite");
		addToken(httppost);
		try {
			List<NameValuePair> pairs = new ArrayList<NameValuePair>(1);
			pairs.add(new BasicNameValuePair("to", phone_no));
			HttpEntity entity = new UrlEncodedFormEntity(pairs);
			httppost.setEntity(entity);
		} catch (UnsupportedEncodingException e) {
			Log.wtf("AccountUtils", "encoding exception", e);
			throw new UserError("Invalid PhoneNumber", -2);
		}

		JSONObject obj = executeRequest(httppost);
		if (((obj == null) || ("fail".equals(obj.optString("stat"))))) {
			Log.i("Invite", "Couldn't invite friend: " + obj);
			if (obj == null) {
				throw new UserError("Unable to invite", -1);
			} else {
				throw new UserError(obj.optString("errorMsg"),
						obj.optInt("error"));
			}
		}
	}

	public static class AccountInfo {
		public String token;

		public String msisdn;

		public String uid;

		public int smsCredits;

		public int all_invitee;

		public int all_invitee_joined;

		public String country_code;

		public AccountInfo(String token, String msisdn, String uid,
				int smsCredits, int all_invitee, int all_invitee_joined,
				String country_code) {
			this.token = token;
			this.msisdn = msisdn;
			this.uid = uid;
			this.smsCredits = smsCredits;
			this.all_invitee = all_invitee;
			this.all_invitee_joined = all_invitee_joined;
			this.country_code = country_code;
		}
	}

	public static AccountInfo registerAccount(Context context, String pin,
			String unAuthMSISDN) {
		HttpPost httppost = new HttpPost(base + "/account");
		AbstractHttpEntity entity = null;
		JSONObject data = new JSONObject();
		try {
			TelephonyManager manager = (TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE);

			String osVersion = Build.VERSION.RELEASE;
			String deviceId = "";

			try {
				deviceId = Utils.getHashedDeviceId(Secure.getString(
						context.getContentResolver(), Secure.ANDROID_ID));
				Log.d("AccountUtils", "Android ID is "+ Secure.ANDROID_ID);
			} catch (NoSuchAlgorithmException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			String os = HikeConstants.ANDROID;
			String carrier = manager.getNetworkOperatorName();
			String device = Build.MANUFACTURER + " " + Build.MODEL;
			String appVersion = "";
			try {
				appVersion = context.getPackageManager().getPackageInfo(
						context.getPackageName(), 0).versionName;
			} catch (NameNotFoundException e) {
				Log.e("AccountUtils", "Unable to get app version");
			}

			data.put("set_cookie", "0");
			data.put("devicetype", os);
			data.put(HikeConstants.LogEvent.OS, os);
			data.put(HikeConstants.LogEvent.OS_VERSION, osVersion);
			data.put("deviceid", deviceId);
			data.put("devicetoken", deviceId);
			data.put("deviceversion", device);
			data.put("appversion", appVersion);
			data.put(
					"invite_token",
					context.getSharedPreferences(HikeMessengerApp.REFERRAL,
							Context.MODE_PRIVATE).getString("utm_source", ""));

			if (pin != null) {
				data.put("msisdn", unAuthMSISDN);
				data.put("pin", pin);
			}
			Log.d("AccountUtils", "Creating Account " + data.toString());
			entity = new GzipByteArrayEntity(data.toString().getBytes(),
					HTTP.DEFAULT_CONTENT_CHARSET);
			entity.setContentType("application/json");
			httppost.setEntity(entity);
		} catch (UnsupportedEncodingException e) {
			Log.wtf("AccountUtils",
					"creating a string entity from an entry string threw!", e);
		} catch (JSONException e) {
			Log.wtf("AccountUtils",
					"creating a string entity from an entry string threw!", e);
		}
		httppost.setEntity(entity);

		JSONObject obj = executeRequest(httppost);
		if ((obj == null)) {
			Log.w("HTTP", "Unable to create account");
			// raise an exception?
			return null;
		}

		Log.d("AccountUtils", "AccountCreation " + obj.toString());
		if ("fail".equals(obj.optString("stat"))) {
			if (pin != null)
				return new AccountUtils.AccountInfo(null, null, null, -1, 0, 0,
						null);
			/*
			 * represents normal account creation , when user is on wifi and
			 * account creation failed
			 */
			return new AccountUtils.AccountInfo(null, null, null, -1, 0, 0,
					null);
		}
		String token = obj.optString("token");
		String msisdn = obj.optString("msisdn");
		String uid = obj.optString("uid");
		int smsCredits = obj.optInt(HikeConstants.MqttMessageTypes.SMS_CREDITS);
		int all_invitee = obj.optInt(HikeConstants.ALL_INVITEE_2);
		int all_invitee_joined = obj.optInt(HikeConstants.ALL_INVITEE_JOINED_2);
		String country_code = obj.optString("country_code");

		Log.d("HTTP", "Successfully created account token:" + token
				+ "msisdn: " + msisdn + " uid: " + uid);
		return new AccountUtils.AccountInfo(token, msisdn, uid, smsCredits,
				all_invitee, all_invitee_joined, country_code);
	}

	public static String validateNumber(String number) {
		HttpPost httppost = new HttpPost(base + "/account/validate?digits=4");
		AbstractHttpEntity entity = null;
		JSONObject data = new JSONObject();
		try {
			data.put("phone_no", number);
			entity = new GzipByteArrayEntity(data.toString().getBytes(),
					HTTP.DEFAULT_CONTENT_CHARSET);
			entity.setContentType("application/json");
			httppost.setEntity(entity);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			Log.e("AccountUtils",
					"creating a string entity from an entry string threw!", e);
		}

		JSONObject obj = executeRequest(httppost);
		if (obj == null) {
			Log.w("HTTP", "Unable to Validate Phone Number.");
			// raise an exception?
			return null;
		}

		String msisdn = obj.optString("msisdn");
		Log.d("HTTP", "Successfully validated phone number.");
		return msisdn;
	}

	private static void addToken(HttpRequestBase req)
			throws IllegalStateException {
		assertIfTokenNull();
		if (TextUtils.isEmpty(mToken)) {
			throw new IllegalStateException("Token is null");
		}
		req.addHeader("Cookie", "user=" + mToken + "; UID=" + mUid);
	}

	private static void assertIfTokenNull() {
		Assert.assertTrue("Token is empty", !TextUtils.isEmpty(mToken));
	}

	public static void setName(String name) throws NetworkErrorException,
			IllegalStateException {
		HttpPost httppost = new HttpPost(base + "/account/name");
		addToken(httppost);
		JSONObject data = new JSONObject();

		try {
			data.put("name", name);
			AbstractHttpEntity entity = new GzipByteArrayEntity(data.toString()
					.getBytes(), HTTP.DEFAULT_CONTENT_CHARSET);
			entity.setContentType("application/json");
			httppost.setEntity(entity);
			JSONObject obj = executeRequest(httppost);
			if ((obj == null) || (!"ok".equals(obj.optString("stat")))) {
				throw new NetworkErrorException("Unable to set name");
			}
		} catch (JSONException e) {
			Log.wtf("AccountUtils", "Unable to encode name as JSON");
		} catch (UnsupportedEncodingException e) {
			Log.wtf("AccountUtils", "Unable to encode name");
		}
	}

	public static JSONObject postAddressBook(String token,
			Map<String, List<ContactInfo>> contactsMap)
			throws IllegalStateException, IOException {
		HttpPost httppost = new HttpPost(base + "/account/addressbook");
		addToken(httppost);
		JSONObject data;
		data = getJsonContactList(contactsMap);
		if (data == null) {
			return null;
		}
		String encoded = data.toString();

		Log.d("ACCOUNT UTILS", "Json data is : " + encoded);
		AbstractHttpEntity entity = new GzipByteArrayEntity(encoded.getBytes(),
				HTTP.DEFAULT_CONTENT_CHARSET);
		entity.setContentType("application/json");
		httppost.setEntity(entity);
		JSONObject obj = executeRequest(httppost);
		return obj;
	}

	/**
	 * 
	 * @param new_contacts_by_id
	 *            new entries to update with. These will replace contact IDs on
	 *            the server
	 * @param ids_json
	 *            , these are ids that are no longer present and should be
	 *            removed
	 * @return
	 */
	public static List<ContactInfo> updateAddressBook(
			Map<String, List<ContactInfo>> new_contacts_by_id,
			JSONArray ids_json) throws IllegalStateException {
		HttpPatch request = new HttpPatch(base + "/account/addressbook");
		addToken(request);
		JSONObject data = new JSONObject();

		try {
			data.put("remove", ids_json);
			data.put("update", getJsonContactList(new_contacts_by_id));
		} catch (JSONException e) {
			Log.e("AccountUtils", "Invalid JSON put", e);
			return null;
		}

		String encoded = data.toString();
		// try
		// {
		AbstractHttpEntity entity = new ByteArrayEntity(encoded.getBytes());
		request.setEntity(entity);
		entity.setContentType("application/json");
		JSONObject obj = executeRequest(request);
		return getContactList(obj, new_contacts_by_id);
	}

	private static JSONObject getJsonContactList(
			Map<String, List<ContactInfo>> contactsMap) {
		JSONObject updateContacts = new JSONObject();
		for (String id : contactsMap.keySet()) {
			try {
				List<ContactInfo> list = contactsMap.get(id);
				JSONArray contactInfoList = new JSONArray();
				for (ContactInfo cInfo : list) {
					JSONObject contactInfo = new JSONObject();
					contactInfo.put("name", cInfo.getName());
					contactInfo.put("phone_no", cInfo.getPhoneNum());
					contactInfoList.put(contactInfo);
				}
				updateContacts.put(id, contactInfoList);
			} catch (JSONException e) {
				Log.d("ACCOUNT UTILS",
						"Json exception while getting contact list.");
				e.printStackTrace();
			}
		}
		return updateContacts;
	}

	public static List<ContactInfo> getContactList(JSONObject obj,
			Map<String, List<ContactInfo>> new_contacts_by_id) {
		List<ContactInfo> server_contacts = new ArrayList<ContactInfo>();
		JSONObject addressbook;
		try {
			if ((obj == null) || ("fail".equals(obj.optString("stat")))) {
				Log.w("HTTP", "Unable to upload address book");
				// TODO raise a real exception here
				return null;
			}
			Log.d("AccountUtils", "Reply from addressbook:" + obj.toString());
			addressbook = obj.getJSONObject("addressbook");
		} catch (JSONException e) {
			Log.e("AccountUtils", "Invalid json object", e);
			return null;
		}

		for (Iterator<?> it = addressbook.keys(); it.hasNext();) {
			String id = (String) it.next();
			JSONArray entries = addressbook.optJSONArray(id);
			List<ContactInfo> cList = new_contacts_by_id.get(id);
			for (int i = 0; i < entries.length(); ++i) {
				JSONObject entry = entries.optJSONObject(i);
				String msisdn = entry.optString("msisdn");
				boolean onhike = entry.optBoolean("onhike");
				ContactInfo info = new ContactInfo(id, msisdn, cList.get(i)
						.getName(), cList.get(i).getPhoneNum(), onhike);
				server_contacts.add(info);
			}
		}
		return server_contacts;
	}

	public static List<String> getBlockList(JSONObject obj) {
		JSONArray blocklist;
		List<String> blockListMsisdns = new ArrayList<String>();
		if ((obj == null) || ("fail".equals(obj.optString("stat")))) {
			Log.w("HTTP", "Unable to upload address book");
			// TODO raise a real exception here
			return null;
		}
		Log.d("AccountUtils", "Reply from addressbook:" + obj.toString());
		blocklist = obj.optJSONArray("blocklist");
		if (blocklist == null) {
			Log.e("AccountUtils", "Received blocklist as null");
			return null;
		}

		for (int i = 0; i < blocklist.length(); i++) {
			try {
				blockListMsisdns.add(blocklist.getString(i));
			} catch (JSONException e) {
				Log.e("AccountUtils", "Invalid json object", e);
				return null;
			}
		}
		return blockListMsisdns;
	}

	public static void deleteOrUnlinkAccount(boolean deleteAccount)
			throws NetworkErrorException, IllegalStateException {
		HttpRequestBase request = deleteAccount ? new HttpDelete(base
				+ "/account") : new HttpPost(base + "/account/unlink");
		addToken(request);
		JSONObject obj = executeRequest(request);
		if ((obj == null) || "fail".equals(obj.optString("stat"))) {
			throw new NetworkErrorException("Could not delete account");
		}
	}

	public static void performRequest(HikeHttpRequest hikeHttpRequest,
			boolean addToken) throws NetworkErrorException,
			IllegalStateException {
		HttpRequestBase requestBase = null;
		AbstractHttpEntity entity = null;
		RequestType requestType = hikeHttpRequest.getRequestType();
		try {
			switch (requestType) {
			case PROFILE_PIC:
				requestBase = new HttpPost(base + hikeHttpRequest.getPath());
				entity = new FileEntity(
						new File(hikeHttpRequest.getFilePath()), "");
				break;

			case STATUS_UPDATE:
			case SOCIAL_POST:
			case OTHER:
				requestBase = new HttpPost(base + hikeHttpRequest.getPath());
				entity = new GzipByteArrayEntity(hikeHttpRequest.getPostData(),
						HTTP.DEFAULT_CONTENT_CHARSET);
				break;

			case DELETE_STATUS:
				requestBase = new HttpDelete(base + hikeHttpRequest.getPath());
				break;

			case HIKE_JOIN_TIME:
				requestBase = new HttpGet(base + hikeHttpRequest.getPath());
			}
			if (addToken) {
				addToken(requestBase);
			}

			if (entity != null) {
				entity.setContentType(hikeHttpRequest.getContentType());
				((HttpPost) requestBase).setEntity(entity);
			}
			JSONObject obj = executeRequest(requestBase);
			Log.d("AccountUtils", "Response: " + obj);
			if (((obj == null) || (!"ok".equals(obj.optString("stat")))
					&& requestType != RequestType.HIKE_JOIN_TIME)) {
				throw new NetworkErrorException("Unable to perform request");
			}
			/*
			 * We need the response to save the id of the status.
			 */
			if (requestType == RequestType.STATUS_UPDATE
					|| requestType == RequestType.HIKE_JOIN_TIME
					|| requestType == RequestType.PROFILE_PIC
					|| requestType == RequestType.SOCIAL_POST) {
				hikeHttpRequest.setResponse(obj);
			}
		} catch (UnsupportedEncodingException e) {
			Log.wtf("AccountUtils", "Unable to encode name");
		}
	}

	static float maxSize;

	public static JSONObject executeFileTransferRequest(String filePath,
			String fileName, JSONObject request,
			final FileTransferTaskBase uploadFileTask, String fileType)
			throws Exception {

		HttpClient httpClient = getClient();

		HttpContext httpContext = new BasicHttpContext();

		HttpPut httpPut = new HttpPut(fileTransferUploadBase + "/user/ft");

		addToken(httpPut);
		httpPut.addHeader("Connection", "Keep-Alive");
		httpPut.addHeader("Content-Name", fileName);
		Log.d("Upload", "Content type: " + fileType);
		httpPut.addHeader("Content-Type", TextUtils.isEmpty(fileType) ? ""
				: fileType);
		httpPut.addHeader("X-Thumbnail-Required", "0");

		final AbstractHttpEntity entity;
		if (!HikeConstants.LOCATION_CONTENT_TYPE.equals(fileType)
				&& !HikeConstants.CONTACT_CONTENT_TYPE.equals(fileType)) {
			entity = new CustomFileEntity(new File(filePath), "",
					new ProgressListener() {
						@Override
						public void transferred(long num) {
							uploadFileTask
									.updateProgress((int) ((num / (float) maxSize) * 100));
						}
					});
		} else {
			entity = new CustomByteArrayEntity(request.toString().getBytes(),
					new ProgressListener() {
						@Override
						public void transferred(long num) {
							uploadFileTask
									.updateProgress((int) ((num / (float) maxSize) * 100));
						}
					});
		}

		uploadFileTask.setEntity(entity);

		maxSize = entity.getContentLength();

		httpPut.setEntity(entity);
		HttpResponse response = httpClient.execute(httpPut, httpContext);
		String serverResponse = EntityUtils.toString(response.getEntity());

		JSONObject responseJSON = new JSONObject(serverResponse);
		if ((responseJSON == null)
				|| (!"ok".equals(responseJSON.optString("stat")))) {
			throw new NetworkErrorException("Unable to perform request");
		}
		return responseJSON;
	}

	public static void deleteSocialCredentials(boolean facebook)
			throws NetworkErrorException, IllegalStateException {
		String url = facebook ? "/account/connect/fb"
				: "/account/connect/twitter";
		HttpDelete delete = new HttpDelete(base + url);
		addToken(delete);
		JSONObject obj = executeRequest(delete);
		if ((obj == null) || "fail".equals(obj.optString("stat"))) {
			throw new NetworkErrorException("Could not delete account");
		}
	}

	public static String getServerUrl() {
		return base;
	}

	public static JSONObject downloadSticker(String catId,
			JSONArray existingStickerIds) throws NetworkErrorException,
			IllegalStateException, JSONException {

		JSONObject request = new JSONObject();
		request.put(HikeConstants.CATEGORY_ID, catId);
		request.put(HikeConstants.STICKER_IDS, existingStickerIds);
		request.put(HikeConstants.RESOLUTION_ID, Utils.getResolutionId());
		request.put(HikeConstants.NUMBER_OF_STICKERS,
				HikeConstants.MAX_NUM_STICKER_REQUEST);

		Log.d("Stickers", "Request: " + request);
		GzipByteArrayEntity entity;
		try {
			entity = new GzipByteArrayEntity(request.toString().getBytes(),
					HTTP.DEFAULT_CONTENT_CHARSET);

			HttpPost httpPost = new HttpPost(base + "/stickers");
			addToken(httpPost);
			httpPost.setEntity(entity);

			JSONObject obj = executeRequest(httpPost);
			Log.d("Stickers", "Response: " + obj);

			if (((obj == null) || (!"ok".equals(obj.optString("stat"))))) {
				throw new NetworkErrorException("Unable to perform request");
			}

			return obj;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}

	}
}
