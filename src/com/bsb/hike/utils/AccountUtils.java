package com.bsb.hike.utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

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
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class AccountUtils {
	private static final int PORT = 3001;
	private static final String HOST = "ec2-175-41-153-127.ap-southeast-1.compute.amazonaws.com";
	private static final String BASE = "http://" + HOST + ":" + Integer.toString(PORT) + "/v1";

	public static final String NETWORK_PREFS_NAME = "NetworkPrefs";

	private static HttpClient mClient = null;

	private static synchronized HttpClient getClient() {
		if (mClient != null) {
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

	public static JSONObject executeRequest(HttpRequestBase request) {
		HttpClient client = getClient();
		HttpResponse response;
		try {
			Log.d("HTTP", "Performing HTTP Request " + request.getRequestLine());
			Log.d("HTTP", "to host" + request);
			response = client.execute(request);
			HttpEntity entity = response.getEntity();
			BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
			StringBuilder builder = new StringBuilder();
			CharBuffer target = CharBuffer.allocate(1024);
			int read = reader.read(target);
			while (read >= 0) {
				builder.append(target.array());
				read = reader.read(target);
			}
			try {
				return new JSONObject(builder.toString());
			} catch (JSONException e) {
				Log.e("HTTP", "Invalid JSON Response", e);
			}
		} catch (ClientProtocolException e) {
			Log.e("HTTP", "Invalid Response", e);
			e.printStackTrace();
		} catch (IOException e) {
			Log.e("HTTP", "Unable to perform request", e);
		}
		return null;		
	}

	public static String getMSISDN() {

		HttpRequestBase httpget = new HttpGet(BASE + "/account");
		addMSISDNHeader(httpget);
		JSONObject obj = executeRequest(httpget);
		try {
			if (obj.has("stat") && "fail".equals(obj.getString("stat"))) {
				Log.e("HTTP", "Unable to get MSISDN");
				return null;
			}

			String msisdn = obj.getString("phone_no");
			return msisdn;
		} catch (JSONException e) {
			Log.e("HTTP", "Invalid JSON Object", e);
			return null;
		}
	}

	public static String registerAccount(String name) throws UnsupportedEncodingException {
		HttpPost httppost = new HttpPost(BASE + "/account");
		List<NameValuePair> pairs = new ArrayList<NameValuePair>(1);
		pairs.add(new BasicNameValuePair("name", name));
		HttpEntity entity = new UrlEncodedFormEntity(pairs);
		httppost.setEntity(entity);

		addMSISDNHeader(httppost);
		JSONObject obj = executeRequest(httppost);
		try {
			if ((obj == null) ||
				(obj.has("stat") && ("fail".equals(obj.get("stat"))))) {
				Log.w("HTTP", "Unable to create account");
				return null;
			}

			String token = obj.getString("token");
			Log.d("HTTP", "Successfully created account");
			return token;
		} catch (JSONException e) {
			Log.e("HTTP", "Invalid JSON Object", e);
			return null;
		}
	}

	private static void addMSISDNHeader(HttpRequestBase req) {
		//TODO remove this line.  just for testing
		req.addHeader("X-MSISDN-AIRTEL", "123456789");		
	}
}
