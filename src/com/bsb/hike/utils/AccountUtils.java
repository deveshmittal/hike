package com.bsb.hike.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.bsb.hike.http.HttpPatch;
import com.bsb.hike.models.ContactInfo;

public class AccountUtils
{
	public static final String HOST = "ec2-175-41-153-127.ap-southeast-1.compute.amazonaws.com";

	private static final int PORT = 3001;

	private static final String BASE = "http://" + HOST + ":" + Integer.toString(PORT) + "/v1";

	public static final String NETWORK_PREFS_NAME = "NetworkPrefs";

	private static HttpClient mClient = null;

	private static String mToken = null;

	public static String MSISDN = "+555555555555";

	public static void setToken(String token)
	{
		mToken = token;
	}

	private static synchronized HttpClient getClient()
	{
		if (mClient != null)
		{
			return mClient;

		}

		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);

		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), PORT));
		schemeRegistry.register(new Scheme("ws", PlainSocketFactory.getSocketFactory(), PORT));

		ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
		mClient = new DefaultHttpClient(cm, params);

		return mClient;
	}

	public static JSONObject executeRequest(HttpRequestBase request)
	{
		HttpClient client = getClient();
		HttpResponse response;
		try
		{
			Log.d("HTTP", "Performing HTTP Request " + request.getRequestLine());
			Log.d("HTTP", "to host" + request);
			response = client.execute(request);
			Log.d("HTTP", "finished request");
			if (response.getStatusLine().getStatusCode() != 200)
			{
				Log.w("HTTP", "Request Failed: " + response.getStatusLine());
				return null;
			}

			HttpEntity entity = response.getEntity();
			BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
			StringBuilder builder = new StringBuilder();
			CharBuffer target = CharBuffer.allocate(10000);
			int read = reader.read(target);
			while (read >= 0)
			{
				builder.append(target.array(), 0, read);
				target.clear();
				read = reader.read(target);
			}
			Log.d("HTTP", "request finished");
			try
			{
				return new JSONObject(builder.toString());
			}
			catch (JSONException e)
			{
				Log.e("HTTP", "Invalid JSON Response", e);
			}
		}
		catch (ClientProtocolException e)
		{
			Log.e("HTTP", "Invalid Response", e);
			e.printStackTrace();
		}
		catch (IOException e)
		{
			Log.e("HTTP", "Unable to perform request", e);
		}
		return null;
	}

	public static int sendMessage(String phone_no, String message)
	{
		HttpPost httppost = new HttpPost(BASE + "/user/msg");
		List<NameValuePair> pairs = new ArrayList<NameValuePair>(2);
		pairs.add(new BasicNameValuePair("to", phone_no));
		pairs.add(new BasicNameValuePair("body", message));
		HttpEntity entity;
		try
		{
			entity = new UrlEncodedFormEntity(pairs);
			httppost.setEntity(entity);
		}
		catch (UnsupportedEncodingException e)
		{
			Log.wtf("URL", "Unable to send message " + message);
			return -1;
		}

		JSONObject obj = executeRequest(httppost);
		if ((obj == null) || ("fail".equals(obj.optString("stat"))))
		{
			Log.w("HTTP", "Unable to send message");
			return -1;
		}

		int count = obj.optInt("sms_count");
		return count;
	}

	public static String getMSISDN()
	{
		HttpRequestBase httpget = new HttpGet(BASE + "/account");
		addMSISDNHeader(httpget);
		JSONObject obj = executeRequest(httpget);
		try
		{
			if (obj.has("stat") && "fail".equals(obj.getString("stat")))
			{
				Log.e("HTTP", "Unable to get MSISDN");
				return null;
			}

			String msisdn = obj.getString("phone_no");
			return msisdn;
		}
		catch (JSONException e)
		{
			Log.e("HTTP", "Invalid JSON Object", e);
			return null;
		}
	}

	public static void invite(String phone_no) throws UserError
	{
		HttpPost httppost = new HttpPost(BASE + "/user/invite");
		addToken(httppost);
		try
		{
			List<NameValuePair> pairs = new ArrayList<NameValuePair>(1);
			pairs.add(new BasicNameValuePair("to", phone_no));
			HttpEntity entity = new UrlEncodedFormEntity(pairs);
			httppost.setEntity(entity);
		}
		catch (UnsupportedEncodingException e)
		{
			Log.wtf("AccountUtils", "encoding exception", e);
			throw new UserError("Invalid PhoneNumber", -2);
		}

		JSONObject obj = executeRequest(httppost);
		if (((obj == null) || ("fail".equals(obj.optString("stat")))))
		{
			Log.i("Invite", "Couldn't invite friend: " + obj);
			if (obj == null)
			{
				throw new UserError("Unable to invite", -1);
			}
			else
			{
				throw new UserError(obj.optString("errorMsg"), obj.optInt("error"));
			}
		}
	}

	public static class AccountInfo
	{
		public String token;

		public String msisdn;

		public String uid;

		public AccountInfo(String token, String msisdn, String uid)
		{
			this.token = token;
			this.msisdn = msisdn;
			this.uid = uid;
		}
	}

	public static AccountInfo registerAccount()
	{
		HttpPost httppost = new HttpPost(BASE + "/account");
		HttpEntity entity = null;
		try
		{
			List<NameValuePair> pairs = new ArrayList<NameValuePair>(1);
			pairs.add(new BasicNameValuePair("set_cookie", "0"));
			entity = new UrlEncodedFormEntity(pairs);
			httppost.setEntity(entity);
		}
		catch (UnsupportedEncodingException e)
		{
			Log.wtf("AccountUtils", "creating a string entity from an entry string threw!");
		}

		httppost.setEntity(entity);

		addMSISDNHeader(httppost);
		JSONObject obj = executeRequest(httppost);
		if ((obj == null) || ("fail".equals(obj.optString("stat"))))
		{
			Log.w("HTTP", "Unable to create account");
			// raise an exception?
			return null;
		}

		String token = obj.optString("token");
		String msisdn = obj.optString("msisdn");
		String uid = obj.optString("uid");
		Log.d("HTTP", "Successfully created account token:" + token + "msisdn: " + msisdn + " uid: " + uid);
		return new AccountUtils.AccountInfo(token, msisdn, uid);
	}

	public static HikeWebSocketClient startWebSocketConnection()
	{
		Header header = new BasicHeader("Cookie", "user=" + mToken);
		Header[] headers = new Header[] { header };
		URI uri;
		try
		{
			uri = new URI(BASE + "/user/msgs");
		}
		catch (URISyntaxException e)
		{
			e.printStackTrace();
			Log.e("AccountUtils", "Error creating websocket", e);
			return null;
		}
		Log.d("AccountUtils", "about to create websocket");
		HikeWebSocketClient socket = new HikeWebSocketClient(uri, new Draft_10WithExtra(headers));
		socket.connect();
		return socket;
	}

	private static void addToken(HttpRequestBase req)
	{
		req.addHeader("Cookie", "user=" + mToken);
	}

	private static void addMSISDNHeader(HttpRequestBase req)
	{
		// TODO remove this line. just for testing
		req.addHeader("X-MSISDN-AIRTEL", MSISDN);
	}

	public static List<ContactInfo> postAddressBook(String token, Map<String, List<ContactInfo>> contactsMap) throws IllegalStateException, IOException
	{
		HttpPost httppost = new HttpPost(BASE + "/account/addressbook");
		addToken(httppost);
		JSONObject data;
		data = getJsonContactList(contactsMap);
		if(data == null)
		{
			return null;
		}
		String encoded = data.toString();
		Log.d("ACCOUNT UTILS","Json data is : "+encoded);
		AbstractHttpEntity entity = new ByteArrayEntity(encoded.getBytes());
		entity.setContentType("application/json");
		httppost.setEntity(entity);
		JSONObject obj = executeRequest(httppost);
		return getContactList(obj, contactsMap);
	}

	/**
	 * 
	 * @param new_contacts_by_id
	 *            new entries to update with. These will replace contact IDs on the server
	 * @param ids_json
	 *            , these are ids that are no longer present and should be removed
	 * @return
	 */
	public static List<ContactInfo> updateAddressBook(Map<String, List<ContactInfo>> new_contacts_by_id, JSONArray ids_json)
	{
		HttpPatch request = new HttpPatch(BASE + "/account/addressbook");
		addToken(request);
		JSONObject data = new JSONObject();

		try
		{
			data.put("remove", ids_json);
			data.put("update", getJsonContactList(new_contacts_by_id));
		}
		catch (JSONException e)
		{
			Log.e("AccountUtils", "Invalid JSON put", e);
			return null;
		}

		String encoded = data.toString();
		//try
		//{
			AbstractHttpEntity entity = new ByteArrayEntity(encoded.getBytes());
			request.setEntity(entity);
			entity.setContentType("application/json");
			JSONObject obj = executeRequest(request);
			return getContactList(obj, new_contacts_by_id);
		//}
		/*catch (UnsupportedEncodingException e)
		{
			Log.e("AccountUtils", "Unable to encode request body", e);
			return null;
		}*/
	}

	private static JSONObject getJsonContactList(Map<String, List<ContactInfo>> contactsMap)
	{
		JSONObject updateContacts = new JSONObject();
		for (String id : contactsMap.keySet())
		{
			try
			{
				List<ContactInfo> list = contactsMap.get(id);
				JSONArray contactInfoList = new JSONArray();
				for (ContactInfo cInfo : list)
				{
					JSONObject contactInfo = new JSONObject();
					contactInfo.put("name", cInfo.getName());
					contactInfo.put("phone_no", cInfo.getPhoneNum());
					contactInfoList.put(contactInfo);
				}
				updateContacts.put(id, contactInfoList);
			}
			catch (JSONException e)
			{
				Log.d("ACCOUNT UTILS","Json exception while getting contact list.");
				e.printStackTrace();
			}
		}
		return updateContacts;
	}

	private static List<ContactInfo> getContactList(JSONObject obj, Map<String, List<ContactInfo>> new_contacts_by_id)
	{
		List<ContactInfo> server_contacts = new ArrayList<ContactInfo>();
		JSONObject addressbook;
		try
		{
			if ((obj == null) || ("fail".equals(obj.optString("stat"))))
			{
				Log.w("HTTP", "Unable to upload address book");
				// TODO raise a real exception here
				return null;
			}
			Log.d("AccountUtils", "Reply from addressbook:" + obj.toString());
			addressbook = obj.getJSONObject("addressbook");
		}
		catch (JSONException e)
		{
			Log.e("AccountUtils", "Invalid json object", e);
			return null;
		}

		for (Iterator<?> it = addressbook.keys(); it.hasNext();)
		{
			String id = (String) it.next();
			JSONArray entries = addressbook.optJSONArray(id);
			List<ContactInfo> cList = new_contacts_by_id.get(id);
			for (int i = 0; i < entries.length(); ++i)
			{
				JSONObject entry = entries.optJSONObject(i);
				String msisdn = entry.optString("msisdn");
				boolean onhike = entry.optBoolean("onhike");
				ContactInfo info = new ContactInfo(id, msisdn, cList.get(i).getName(), onhike, cList.get(i).getPhoneNum());
				server_contacts.add(info);
			}
		}
		return server_contacts;
	}
}
