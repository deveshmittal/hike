package com.bsb.hike.ui;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.sdk.HikeSDKResponseCode;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.tasks.UtilAtomicAsyncTask;
import com.bsb.hike.tasks.UtilAtomicAsyncTask.UtilAsyncTaskListener;
import com.bsb.hike.utils.AccountUtils;
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

	private static final String BASE_URL = "http://stagingoauth.im.hike.in/o/oauth2/";

	private static final String PATH_AUTHORIZE = "authorize";

	public static final String AUTH_SHARED_PREF_NAME = "364i5j6b3oj4";

	public static final String AUTH_SHARED_PREF_PKG_KEY = "ilugasdgi2";

	private static final String IS_SENT_FOR_SIGNUP_KEY = "IS_SENT_FOR_SIGNUP_KEY";

	private byte CURRENT_STATE = 0;

	private final byte STATE_NORMAL = 0;

	private final byte STATE_IS_CONNECTING = 1;

	private final byte STATE_RETRY_CONNECTION = 2;

	private final byte STATE_CONNECTED = 3;

	private TextView auth_title;

	private TextView auth_app_name;

	private TextView auth_app_dev;

	private ImageView auth_app_icon;

	private TextView textViewDropdown;

	private TextView auth_app_will_setup_acc;

	private Button auth_button_accept;

	private Button auth_button_deny;

	private View layout_app_info;

	private View auth_info_layout;

	private View auth_button_deny_layout;

	private View auth_buttons_layout;

	private View layout_conn_state;

	private TextView text_conn_state;

	private ImageView image_conn_state;

	private View progress_bar_conn_state;

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

		CURRENT_STATE = STATE_NORMAL;
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

			try
			{
				JSONObject analyticsJSON = new JSONObject();
				analyticsJSON.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SDK_AUTH_DIALOG_VIEWED);
				analyticsJSON.put(HikeConstants.Extras.SDK_THIRD_PARTY_PKG, mAppPackage);
				Utils.sendLogEvent(analyticsJSON);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}

			initVar();

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

	/**
	 * Initialize variables and references
	 */
	private void initVar()
	{
		auth_title = ((TextView) findViewById(R.id.auth_title));

		auth_app_name = ((TextView) findViewById(R.id.auth_app_name));

		auth_app_dev = ((TextView) findViewById(R.id.auth_app_dev));

		auth_app_icon = ((ImageView) findViewById(R.id.auth_app_icon));

		textViewDropdown = ((TextView) findViewById(R.id.auth_app_access_to));

		auth_app_will_setup_acc = ((TextView) findViewById(R.id.auth_app_will_setup_acc));

		auth_button_accept = ((Button) findViewById(R.id.auth_button_accept));

		auth_button_deny = ((Button) findViewById(R.id.auth_button_deny));

		layout_app_info = findViewById(R.id.layout_app_info);

		auth_info_layout = findViewById(R.id.auth_info_layout);

		auth_button_deny_layout = findViewById(R.id.auth_button_deny_layout);

		auth_buttons_layout = findViewById(R.id.auth_buttons_layout);

		layout_conn_state = findViewById(R.id.layout_conn_state);

		text_conn_state = ((TextView) findViewById(R.id.text_conn_state));

		progress_bar_conn_state = findViewById(R.id.progress_bar_conn_state);

		image_conn_state = ((ImageView) findViewById(R.id.image_conn_state));
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
		try
		{
			// Set title
			auth_title.setText(String.format(getApplicationContext().getString(R.string.auth_title), mAppName));

			// Set app name
			auth_app_name.setText(mAppName);

			// Set app version
			auth_app_dev.setText(mAppVersion);

			// Set app icon
			auth_app_icon.setImageDrawable(mAppIconLogo);

			// Set "will have access to" text
			textViewDropdown.setText(String.format(getApplicationContext().getString(R.string.auth_app_access_to), mAppName));

			textViewDropdown.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					TextView dropdown = ((TextView) findViewById(R.id.auth_app_access_to));
					if (dropdown.getTag().equals("0"))
					{
						dropdown.setCompoundDrawablesWithIntrinsicBounds(getApplicationContext().getResources().getDrawable(R.drawable.arrowup), null, null, null);
						dropdown.setTag("1");
						findViewById(R.id.auth_info_layout).setVisibility(View.VISIBLE);
					}
					else
					{
						dropdown.setCompoundDrawablesWithIntrinsicBounds(getApplicationContext().getResources().getDrawable(R.drawable.arrowdown), null, null, null);
						dropdown.setTag("0");
						findViewById(R.id.auth_info_layout).setVisibility(View.GONE);
					}
				}
			});

			new Handler().postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					if (textViewDropdown.getTag().equals("0"))
					{
						if (CURRENT_STATE == STATE_NORMAL)
						{
							textViewDropdown.setCompoundDrawablesWithIntrinsicBounds(getApplicationContext().getResources().getDrawable(R.drawable.arrowup), null, null, null);
							textViewDropdown.setTag("1");
							findViewById(R.id.auth_info_layout).setVisibility(View.VISIBLE);
						}
					}
				}
			}, 2000);

			// Set "will setup account on" text
			auth_app_will_setup_acc.setText(String.format(getApplicationContext().getString(R.string.auth_will_setup_acc), mAppName));

			// Set user avatar
			profileImageLoader.loadImage(userContactId, ((ImageView) findViewById(R.id.auth_user_avatar)), false, false, true);

			// Set default contact avatar
			profileImageLoader.loadImage("nullround", ((ImageView) findViewById(R.id.auth_contacts_avatar)), false, false, true);

			// Set "connect" button event
			auth_button_accept.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					try
					{
						JSONObject analyticsJSON = new JSONObject();
						analyticsJSON.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SDK_AUTH_DIALOG_CONNECT);
						analyticsJSON.put(HikeConstants.Extras.SDK_THIRD_PARTY_PKG, mAppPackage);
						Utils.sendLogEvent(analyticsJSON);
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}
					requestAccess();
				}
			});

			// Set "declined" button event
			auth_button_deny.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					try
					{
						JSONObject analyticsJSON = new JSONObject();
						analyticsJSON.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SDK_AUTH_DIALOG_DECLINED);
						analyticsJSON.put(HikeConstants.Extras.SDK_THIRD_PARTY_PKG, mAppPackage);
						Utils.sendLogEvent(analyticsJSON);
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}
					message.arg2 = HikeSDKResponseCode.STATUS_FAILED;
					HikeService.mHikeSDKRequestHandler.handleMessage(message);
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

		displayIsConnectingState();

		String authUrl = BASE_URL + PATH_AUTHORIZE;

		List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();

		params.add(new BasicNameValuePair("response_type", "token"));
		params.add(new BasicNameValuePair("client_id", mAppId));
		params.add(new BasicNameValuePair("scope", "publish_actions"));
		params.add(new BasicNameValuePair("package_name", mAppPackage));
		params.add(new BasicNameValuePair("sha1", "test_sha1"));

		String paramString = URLEncodedUtils.format(params, "UTF-8");
		try
		{
			paramString = URLDecoder.decode(paramString, "UTF-8");
		}
		catch (UnsupportedEncodingException e1)
		{
			e1.printStackTrace();
			displayRetryConnectionState();
			return;
		}

		authUrl += "?" + paramString;

		HttpGet httpGet = new HttpGet(authUrl);

		httpGet.addHeader(new BasicHeader("Content-type", "text/plain"));
		httpGet.addHeader(new BasicHeader("cookie", "uid=UZtZkaEMFSBRwmys;token=EeEKpHJzesU="));

		authTask = new UtilAtomicAsyncTask(HikeAuthActivity.this, null, false, new UtilAsyncTaskListener()
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

					if (TextUtils.isEmpty(prefs.getData(mAppPackage, "")))
					{
						long timestamp = System.currentTimeMillis();
						prefs.saveData(AUTH_SHARED_PREF_PKG_KEY,
								TextUtils.isEmpty(prefs.getData(AUTH_SHARED_PREF_PKG_KEY, "")) ? mAppPackage + ":" + timestamp : prefs.getData(AUTH_SHARED_PREF_PKG_KEY, "") + ","
										+ mAppPackage + ":" + timestamp);
					}

					prefs.saveData(mAppPackage, Integer.toString(accessToken.hashCode()));

					HikeMessengerApp.getPubSub().publish(HikePubSub.AUTH_TOKEN_RECEIVED, accessToken);

					displayConnectedState();
				}
				else
				{
					displayRetryConnectionState();
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
									TextUtils.isEmpty(prefs.getData(AUTH_SHARED_PREF_PKG_KEY, "")) ? mAppPackage + ":" + timestamp : prefs.getData(AUTH_SHARED_PREF_PKG_KEY, "")
											+ "," + mAppPackage + ":" + timestamp);
						}

						prefs.saveData(mAppPackage, Integer.toString(accessToken.hashCode()));

						HikeMessengerApp.getPubSub().publish(HikePubSub.AUTH_TOKEN_RECEIVED, accessToken);

						displayConnectedState();
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

	public void displayIsConnectingState()
	{
		CURRENT_STATE = STATE_IS_CONNECTING;
		auth_button_accept.setText("");
		auth_title.setVisibility(View.GONE);
		layout_app_info.setVisibility(View.GONE);
		textViewDropdown.setVisibility(View.GONE);
		auth_info_layout.setVisibility(View.GONE);
		auth_button_deny_layout.setVisibility(View.GONE);

		// Hack to improve UX
		new Handler().postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				auth_button_accept.setText(getString(R.string.auth_state_connecting));
				auth_button_accept.setOnClickListener(null);
			}
		}, 500);
		layout_conn_state.setVisibility(View.VISIBLE);
		text_conn_state.setText(getString(R.string.auth_please_wait_connect));
		image_conn_state.setVisibility(View.GONE);
		progress_bar_conn_state.setVisibility(View.VISIBLE);
	}

	public void displayConnectedState()
	{
		CURRENT_STATE = STATE_CONNECTED;
		auth_title.setVisibility(View.GONE);
		auth_button_accept.setVisibility(View.GONE);

		layout_app_info.setVisibility(View.GONE);
		layout_app_info.setVisibility(View.GONE);
		auth_info_layout.setVisibility(View.GONE);
		auth_button_deny_layout.setVisibility(View.GONE);
		auth_buttons_layout.setVisibility(View.GONE);
		new Handler().postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				HikeAuthActivity.this.finish();
			}
		}, 2500);
		layout_conn_state.setVisibility(View.VISIBLE);
		text_conn_state.setText(String.format(getApplicationContext().getString(R.string.auth_connected_to_hike), mAppName));
		image_conn_state.setVisibility(View.VISIBLE);
		image_conn_state.setImageResource(R.drawable.ic_tick_auth);
		progress_bar_conn_state.setVisibility(View.GONE);
		try
		{
			JSONObject analyticsJSON = new JSONObject();
			analyticsJSON.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SDK_AUTH_SUCCESS);
			analyticsJSON.put(HikeConstants.Extras.SDK_THIRD_PARTY_PKG, mAppPackage);
			analyticsJSON.put(HikeConstants.Extras.SDK_CONNECTION_TYPE, FileTransferManager.getInstance(getApplicationContext()).getNetworkType());
			Utils.sendLogEvent(analyticsJSON);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public void displayRetryConnectionState()
	{
		CURRENT_STATE = STATE_RETRY_CONNECTION;
		auth_title.setVisibility(View.GONE);
		layout_app_info.setVisibility(View.GONE);
		textViewDropdown.setVisibility(View.GONE);
		auth_info_layout.setVisibility(View.GONE);
		auth_button_deny_layout.setVisibility(View.VISIBLE);
		auth_button_deny.setText(getString(R.string.auth_cancel));
		auth_button_accept.setText(getString(R.string.auth_retry));
		auth_button_deny.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				HikeAuthActivity.this.finish();
			}
		});
		auth_button_accept.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				requestAccess();
			}
		});
		layout_conn_state.setVisibility(View.VISIBLE);
		text_conn_state.setText(getString(R.string.auth_went_wrong));
		image_conn_state.setVisibility(View.VISIBLE);
		image_conn_state.setImageResource(R.drawable.ic_error);
		progress_bar_conn_state.setVisibility(View.GONE);
		try
		{
			JSONObject analyticsJSON = new JSONObject();
			analyticsJSON.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SDK_AUTH_FAILURE);
			analyticsJSON.put(HikeConstants.Extras.SDK_THIRD_PARTY_PKG, mAppPackage);
			analyticsJSON.put(HikeConstants.Extras.SDK_CONNECTION_TYPE, FileTransferManager.getInstance(getApplicationContext()).getNetworkType());
			Utils.sendLogEvent(analyticsJSON);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}
}
