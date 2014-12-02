package com.bsb.hike.ui;

import java.util.LinkedList;
import java.util.List;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.sdk.HikeSDKResponseCode;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.tasks.UtilAtomicAsyncTask;
import com.bsb.hike.tasks.UtilAtomicAsyncTask.UtilAsyncTaskListener;
import com.bsb.hike.utils.HikeSDKConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * This activity is responsible for displaying "connect-with-hike" dialog and providing data as per user action.
 * 
 * @author AtulM
 * 
 */
public class HikeAuthActivity extends Activity
{

	/** The host name. */
	private String mAppName;

	/** The host dev name. */
	private String mAppVersion;

	/** The host image */
	private Drawable mAppIconLogo;

	/** The app id. */
	private String mAppId;

	/** The app package. */
	private String mAppPackage;

	private Message message;

	private IconLoader profileImageLoader;

	private String userContactId;

	private UtilAtomicAsyncTask authTask;

	private HikeSharedPreferenceUtil settingPref;

	private boolean isInitialized;

	private boolean isRoutedToSignUp;

	/** The Constant MESSAGE_INDEX. Used for passing messages between activities */
	public static final String MESSAGE_INDEX = "MESSAGE_INDEX";

	private static final String BASE_URL = "http://staging2.im.hike.in:5000/o/oauth2/";

	private static final String PATH_AUTHORIZE = "authorize";

	public static final String AUTH_SHARED_PREF_NAME = "364i5j6b3oj4";

	public static final String AUTH_SHARED_PREF_PKG_KEY = "ilugasdgi2";

	private static final String IS_SENT_FOR_SIGNUP_KEY = "IS_SENT_FOR_SIGNUP_KEY";

	public static boolean bypassAuthHttp = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.bsb.hike.utils.HikeAppStateBaseFragmentActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null)
		{
			if (savedInstanceState.getBoolean(IS_SENT_FOR_SIGNUP_KEY, false))
			{
				HikeAuthActivity.this.finish();
				return;
			}

			message = (Message) savedInstanceState.getParcelable(MESSAGE_INDEX);
		}

		settingPref = HikeSharedPreferenceUtil.getInstance(getApplicationContext(), HikeMessengerApp.ACCOUNT_SETTINGS);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		if (isRoutedToSignUp && (!settingPref.getData(HikeMessengerApp.ACCEPT_TERMS, false)) && (settingPref.getData(HikeMessengerApp.NAME_SETTING, null) == null))
		{
			this.finish();
			return;
		}

		if (isUserSignedUp() && !isInitialized)
		{
			setContentView(R.layout.auth_main);

			ContactInfo contactInfo = Utils.getUserContactInfo(getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE));

			userContactId = contactInfo.getMsisdn() + ProfileActivity.PROFILE_ROUND_SUFFIX;

			profileImageLoader = new IconLoader(this, getResources().getDimensionPixelSize(R.dimen.auth_permission_icon));

			retrieveContent();

			bindContentsAndActions();

			settingPref.saveData(HikeMessengerApp.PENDING_SDK_AUTH, false);

			isInitialized = true;
		}
		else
		{
			isRoutedToSignUp = true;
			settingPref.saveData(HikeMessengerApp.PENDING_SDK_AUTH, true);
			return;
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putBoolean(IS_SENT_FOR_SIGNUP_KEY, true);

		outState.putParcelable(MESSAGE_INDEX, getIntent().getParcelableExtra(MESSAGE_INDEX));

		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
		if (savedInstanceState != null)
		{
			if (savedInstanceState.getBoolean(IS_SENT_FOR_SIGNUP_KEY, false))
			{
				HikeAuthActivity.this.finish();
				return;
			}

			message = (Message) savedInstanceState.getParcelable(MESSAGE_INDEX);
		}
	}

	/**
	 * Retrieve content from intent.
	 */
	private void retrieveContent()
	{
		if (message == null)
		{
			message = (Message) getIntent().getParcelableExtra(MESSAGE_INDEX);
		}

		if (message != null)
		{
			Bundle authBundle = message.getData();

			if (authBundle != null)
			{
				String data = authBundle.getString(HikeSDKConstants.HIKE_REQ_DATA_ID);

				try
				{
					JSONObject authJSON = new JSONObject(data);
					mAppId = authJSON.getString(HikeSDKConstants.HIKE_REQ_SDK_CLIENT_APP_ID);
					mAppPackage = authJSON.getString(HikeSDKConstants.HIKE_REQ_SDK_CLIENT_PKG_NAME);

					if (TextUtils.isEmpty(mAppId) || TextUtils.isEmpty(mAppPackage))
					{
						onFailed("App id and App secret are mandatory for auth");
					}
				}
				catch (JSONException e)
				{
					e.printStackTrace();
					onFailed("Improper JSON from SDK");
				}
			}
		}
		else
		{
			this.finish();
			return;
		}
		// We have host application package name. Get its name, dev name and image from package info
		PackageManager pm = getApplicationContext().getPackageManager();
		try
		{
			PackageInfo packageInfo = pm.getPackageInfo(mAppPackage, PackageManager.GET_ACTIVITIES);
			mAppName = packageInfo.applicationInfo.loadLabel(getApplicationContext().getPackageManager()).toString();
			mAppIconLogo = packageInfo.applicationInfo.loadIcon(pm);
			mAppVersion = "ver " + packageInfo.versionName;
		}
		catch (PackageManager.NameNotFoundException e)
		{
			e.printStackTrace();
			onFailed("Invalid/unavailable package name");
		}
	}

	/**
	 * Bind data and actions.
	 */
	private void bindContentsAndActions()
	{
		Animation fadeIn = new AlphaAnimation(0, 1);
		fadeIn.setInterpolator(new DecelerateInterpolator());
		fadeIn.setDuration(1000);

		findViewById(R.id.auth_splash_content).startAnimation(fadeIn);

		try
		{

			new Handler().postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					findViewById(R.id.auth_info).setVisibility(View.VISIBLE);
				}
			}, 2000);

			new Handler().postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					findViewById(R.id.auth_splash).setVisibility(View.GONE);
				}
			}, 2200);

			// Set title
			((TextView) findViewById(R.id.auth_title)).setText(String.format(getApplicationContext().getString(R.string.auth_title), mAppName));

			// Set app name
			((TextView) findViewById(R.id.auth_app_name)).setText(mAppName);

			// Set app version
			((TextView) findViewById(R.id.auth_app_dev)).setText(mAppVersion);

			// Set app icon
			((ImageView) findViewById(R.id.auth_app_icon)).setImageDrawable(mAppIconLogo);

			// Set "will have access to" text
			((TextView) findViewById(R.id.auth_app_access_to)).setText(String.format(getApplicationContext().getString(R.string.auth_app_access_to), mAppName));

			// Set "will setup account on" text
			((TextView) findViewById(R.id.auth_app_will_setup_acc)).setText(String.format(getApplicationContext().getString(R.string.auth_will_setup_acc), mAppName));

			// Set user avatar
			profileImageLoader.loadImage(userContactId, ((ImageView) findViewById(R.id.auth_user_avatar)), false, false, true);

			// Set default contact avatar
			profileImageLoader.loadImage("nullround", ((ImageView) findViewById(R.id.auth_contacts_avatar)), false, false, true);

			// Set "connect" button event
			((Button) findViewById(R.id.auth_button_accept)).setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					requestAccess();
				}
			});

			// Set "declined" button event
			((Button) findViewById(R.id.auth_button_deny)).setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					message.arg2 = HikeSDKResponseCode.STATUS_FAILED;
					HikeService.mHikeSDKRequestHandler.handleMessage(message);
					// Logger.d(HikeAuthActivity.class.getCanonicalName(), "message" + message.toString() + " replyto: " + message.replyTo.toString());
					// message.replyTo.send(message);
					Logger.d(HikeAuthActivity.class.getCanonicalName(), "shutting auth activity successfully!");
					HikeAuthActivity.this.finish();
				}
			});

		}
		catch (NullPointerException npe)
		{
			// Invalid layout file. Not usually possible.
			npe.printStackTrace();
		}
	}

	/**
	 * Request access token from hike api.
	 */
	public void requestAccess()
	{

		String authUrl = BASE_URL + PATH_AUTHORIZE;

		List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();

		params.add(new BasicNameValuePair("response_type", "token"));
		params.add(new BasicNameValuePair("client_id", mAppId));
		params.add(new BasicNameValuePair("scope", "publish_actions"));
		params.add(new BasicNameValuePair("package_name", "test_package_ame"));
		params.add(new BasicNameValuePair("sha1", "test_sh1"));

		String paramString = URLEncodedUtils.format(params, "utf-8");

		authUrl += "?" + paramString;

		HttpGet httpGet = new HttpGet(authUrl);

		httpGet.addHeader(new BasicHeader("Content-type", "text/plain"));
		httpGet.addHeader(new BasicHeader("cookie", "uid=UZtZkaEMFSBRwmys;token=EeEKpHJzesU="));

		authTask = new UtilAtomicAsyncTask(HikeAuthActivity.this, null, new UtilAsyncTaskListener()
		{

			private HikeSharedPreferenceUtil prefs;

			@Override
			public void onFailed()
			{

				Logger.d(HikeAuthActivity.class.getCanonicalName(), "on task failed");

				if (bypassAuthHttp)
				{
					String expiresIn = "12341";
					String accessToken = "ashjfbqiywgr13irb";

					prefs = HikeSharedPreferenceUtil.getInstance(getApplicationContext(), AUTH_SHARED_PREF_NAME);
					prefs.saveData(mAppPackage, Integer.toString(accessToken.hashCode()));

					HikeMessengerApp.getPubSub().publish(HikePubSub.AUTH_TOKEN_RECEIVED, accessToken);

					HikeAuthActivity.this.finish();
				}
				else
				{
					// display a toast and request user to relog
					Toast.makeText(getApplicationContext(), "Connection failed. Please try again.", Toast.LENGTH_LONG).show();
				}

			}

			@Override
			public void onComplete(String argResponse)
			{
				// response example - {"state":null,"expires_in":5184000,"access_token":"F78SWrzfx-SKNbo2QZZBHA==","token_type":"Bearer"}}
				try
				{
					Logger.d(HikeAuthActivity.class.getCanonicalName(), "on task success");
					JSONObject responseJSON = new JSONObject(argResponse);
					JSONObject responseData = responseJSON.getJSONObject("response");
					if (responseData != null)
					{
						if (responseData.has("error"))
						{
							onFailed();
							return;
						}
						String expiresIn = responseData.getString("expires_in");
						String accessToken = responseData.getString("access_token");

						prefs = HikeSharedPreferenceUtil.getInstance(getApplicationContext(), AUTH_SHARED_PREF_NAME);

						if (TextUtils.isEmpty(prefs.getData(mAppPackage, "")))
						{
							long timestamp = System.currentTimeMillis();
							prefs.saveData(AUTH_SHARED_PREF_PKG_KEY,
									TextUtils.isEmpty(prefs.getData(AUTH_SHARED_PREF_PKG_KEY, "")) ? mAppPackage + ":" + timestamp : prefs.getData(AUTH_SHARED_PREF_PKG_KEY, "") + ","
											+ mAppPackage + ":" + timestamp);
						}

						prefs.saveData(mAppPackage, Integer.toString(accessToken.hashCode()));

						HikeMessengerApp.getPubSub().publish(HikePubSub.AUTH_TOKEN_RECEIVED, accessToken);

						HikeAuthActivity.this.finish();
					}
					else
					{
						onFailed();
						return;
					}
				}
				catch (JSONException e)
				{
					e.printStackTrace();
					onFailed();
					return;
				}
			}
		});

		Utils.executeUtilAtomicTask(authTask, httpGet);
	}

	@Override
	protected void onStop()
	{
		if (authTask != null && !authTask.isCancelled())
		{
			authTask.cancel(false);
		}

		super.onStop();
	}

	/**
	 * Gets the app name.
	 * 
	 * @return the app name
	 */
	public String getmAppName()
	{
		return mAppName;
	}

	/**
	 * Sets the app name.
	 * 
	 * @param mAppName
	 *            the new app name
	 */
	public void setmAppName(String mAppName)
	{
		this.mAppName = mAppName;
	}

	/**
	 * Gets the app dev name.
	 * 
	 * @return the app dev name
	 */
	public String getmAppDevName()
	{
		return mAppVersion;
	}

	/**
	 * Sets the app dev name.
	 * 
	 * @param mAppVersion
	 *            the new app dev name
	 */
	public void setmAppDevName(String mAppDevName)
	{
		this.mAppVersion = mAppDevName;
	}

	/**
	 * Gets the app id.
	 * 
	 * @return the app id
	 */
	public String getmAppId()
	{
		return mAppId;
	}

	/**
	 * Sets the app id.
	 * 
	 * @param mAppId
	 *            the new app id
	 */
	public void setmAppId(String mAppId)
	{
		this.mAppId = mAppId;
	}

	/**
	 * Gets the app package.
	 * 
	 * @return the app package
	 */
	public String getmAppPackage()
	{
		return mAppPackage;
	}

	/**
	 * Sets the app package.
	 * 
	 * @param mAppPackage
	 *            the new app package
	 */
	public void setmAppPackage(String mAppPackage)
	{
		this.mAppPackage = mAppPackage;
	}

	/**
	 * Called whenever there is an exception/failure in auth process
	 * 
	 * @param argMessage
	 */
	public void onFailed(String argMessage)
	{
		Logger.d(HikeAuthActivity.class.getCanonicalName(), "onfailed is called");

		if (message != null)
		{
			message.arg2 = HikeSDKResponseCode.STATUS_FAILED;

			Bundle messageBundle = new Bundle();

			messageBundle.putString(HikeSDKConstants.HIKE_REQ_DATA_ID, argMessage);

			message.setData(messageBundle);

			try
			{
				message.replyTo.send(message);
				Logger.d(HikeAuthActivity.class.getCanonicalName(), "shutting auth activity successfully!");
			}
			catch (RemoteException e)
			{
				e.printStackTrace();
			}
		}
		HikeAuthActivity.this.finish();
	}

	public Drawable getmAppImage()
	{
		return mAppIconLogo;
	}

	public void setmAppImage(Drawable mAppImage)
	{
		this.mAppIconLogo = mAppImage;
	}

	public static boolean verifyRequest(Context argContext, Message argMessage)
	{
		Bundle messageData = argMessage.getData();
		String jsonString = messageData.getString(HikeSDKConstants.HIKE_REQ_DATA_ID);
		try
		{
			JSONObject jsonObject = new JSONObject(jsonString);
			String access = jsonObject.getString(HikeSDKConstants.HIKE_REQ_SDK_CLIENT_ACC_TOKEN);
			String pkg = jsonObject.getString(HikeSDKConstants.HIKE_REQ_SDK_CLIENT_PKG_NAME);
			return verifyRequest(argContext, pkg, access);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return false;
		}
		catch (NullPointerException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public static boolean verifyRequest(Context argContext, String pkgName, String accessT)
	{
		try
		{
			String access = Integer.toString(accessT.hashCode());
			String pkg = pkgName;
			HikeSharedPreferenceUtil prefs = HikeSharedPreferenceUtil.getInstance(argContext, AUTH_SHARED_PREF_NAME);
			String savedAccess = prefs.getData(pkg, "");
			if (access.equals(savedAccess))
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		catch (NullPointerException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public boolean isUserSignedUp()
	{
		if (!settingPref.getData(HikeMessengerApp.ACCEPT_TERMS, false))
		{
			if (!isRoutedToSignUp)
			{
				HikeAuthActivity.this.startActivity(new Intent(HikeAuthActivity.this, WelcomeActivity.class));
				isRoutedToSignUp = true;
			}
			return false;
		}

		if (settingPref.getData(HikeMessengerApp.NAME_SETTING, null) == null)
		{
			if (!isRoutedToSignUp)
			{
				HikeAuthActivity.this.startActivity(new Intent(HikeAuthActivity.this, SignupActivity.class));
				isRoutedToSignUp = true;
			}
			return false;
		}

		return true;
	}

	@Override
	protected void onPause()
	{
		overridePendingTransition(0, 0);
		super.onPause();
	}

	@Override
	public void onBackPressed()
	{
		onFailed("Declined");
		super.onBackPressed();
	}

}
