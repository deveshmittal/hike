package com.bsb.hike.modules.signupmanager;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.registerAccountRequest;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.AccountInfo;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;
import com.bsb.hike.modules.httpmgr.request.requestbody.JsonBody;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class RegisterAccountTask
{
	private String TAG = "RegisterAccountTask";

	public String pin;

	public String unAuthMsisdn;

	public AccountInfo resultAccountInfo;

	public Context context;

	public RegisterAccountTask(String pin, String unAuthMsisdn)
	{
		this.pin = pin;
		this.unAuthMsisdn = unAuthMsisdn;
		this.context = HikeMessengerApp.getInstance();
	}

	public AccountInfo execute()
	{

		JSONObject postObject = getPostObject();
		IRequestBody body = new JsonBody(postObject);
		RequestToken requestToken = registerAccountRequest(body, getRequestListener());
		requestToken.execute();
		return resultAccountInfo;

	}

	private IRequestListener getRequestListener()
	{
		return new IRequestListener()
		{

			@Override
			public void onRequestSuccess(Response result)
			{
				JSONObject response = (JSONObject) result.getBody().getContent();

				if (!Utils.isResponseValid(response))
				{
					Logger.e(TAG, "Sticker download failed null response");

					resultAccountInfo = new AccountInfo.Builder()
							.setToken(null)
							.setMsisdn(null)
							.setUid(null)
							.setBackupToken(null)
							.setSmsCredits(-1)
							.setAllInvitee(0)
							.setAllInviteJoined(0)
							.setCountryCode(null)
							.build();

					return;
				}

				String token = response.optString("token");
				String msisdn = response.optString("msisdn");
				String uid = response.optString("uid");
				String backupToken = response.optString("backup_token");
				int smsCredits = response.optInt(HikeConstants.MqttMessageTypes.SMS_CREDITS);
				int all_invitee = response.optInt(HikeConstants.ALL_INVITEE_2);
				int all_invitee_joined = response.optInt(HikeConstants.ALL_INVITEE_JOINED_2);
				String country_code = response.optString("country_code");

				Logger.d("HTTP", "Successfully created account token:" + token + "msisdn: " + msisdn + " uid: " + uid + "backup_token: " + backupToken);

				resultAccountInfo = new AccountInfo.Builder()
						.setToken(token)
						.setMsisdn(msisdn)
						.setUid(uid)
						.setBackupToken(backupToken)
						.setSmsCredits(smsCredits)
						.setAllInvitee(all_invitee)
						.setAllInviteJoined(all_invitee_joined)
						.setCountryCode(country_code)
						.build();

			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{

			}

			@Override
			public void onRequestFailure(HttpException httpException)
			{
				resultAccountInfo = null;
			}
		};
	}

	private JSONObject getPostObject()
	{
		JSONObject data = new JSONObject();
		try
		{
			TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

			String osVersion = Build.VERSION.RELEASE;
			String deviceId = "";

			try
			{
				deviceId = Utils.getHashedDeviceId(Secure.getString(context.getContentResolver(), Secure.ANDROID_ID));
				Logger.d("AccountUtils", "Android ID is " + Secure.ANDROID_ID);
			}
			catch (NoSuchAlgorithmException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			String os = HikeConstants.ANDROID;
			String carrier = manager.getNetworkOperatorName();
			String device = Build.MANUFACTURER + " " + Build.MODEL;
			String appVersion = "";
			try
			{
				appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
			}
			catch (NameNotFoundException e)
			{
				Logger.e("AccountUtils", "Unable to get app version");
			}

			String deviceKey = manager.getDeviceId();

			data.put("set_cookie", "0");
			data.put("devicetype", os);
			data.put(HikeConstants.LogEvent.OS, os);
			data.put(HikeConstants.LogEvent.OS_VERSION, osVersion);
			data.put("deviceid", deviceId);
			data.put("devicetoken", deviceId);
			data.put("deviceversion", device);
			data.put(HikeConstants.DEVICE_KEY, deviceKey);
			data.put("appversion", appVersion);
			data.put("invite_token", context.getSharedPreferences(HikeMessengerApp.REFERRAL, Context.MODE_PRIVATE).getString("utm_source", ""));

			if (pin != null)
			{
				data.put("msisdn", unAuthMsisdn);
				data.put("pin", pin);
			}
			Utils.addCommonDeviceDetails(data, context);

			Logger.d("AccountUtils", "Creating Account " + data.toString());
		}
		catch (UnsupportedEncodingException e)
		{
			Logger.wtf("AccountUtils", "creating a string entity from an entry string threw!", e);
		}
		catch (JSONException e)
		{
			Logger.wtf("AccountUtils", "creating a string entity from an entry string threw!", e);
		}
		return data;
	}

	void onSuccess(Object result)
	{

	}

	void onFailure(Exception e)
	{

	}

}
