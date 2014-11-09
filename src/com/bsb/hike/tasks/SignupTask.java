package com.bsb.hike.tasks;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.accounts.NetworkErrorException;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.db.DBBackupRestore;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.models.Birthday;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.ui.SignupActivity;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class SignupTask extends AsyncTask<Void, SignupTask.StateValue, Boolean> implements ActivityCallableTask
{
	private class SMSReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			Bundle extras = intent.getExtras();
			if (extras != null)
			{
				Object[] extra = (Object[]) extras.get("pdus");
				for (int i = 0; i < extra.length; ++i)
				{
					SmsMessage sms = SmsMessage.createFromPdu((byte[]) extra[i]);
					String body = sms.getMessageBody();
					String pin = Utils.getSMSPinCode(body);
					if (pin != null)
					{
						if(getDisplayChild() != SignupActivity.PIN){
							SignupTask.this.addUserInput(pin);
						} else{
							SignupTask.this.autoFillPin(pin);
						}
						this.abortBroadcast();
						break;
					}
				}
			}
		}
	}

	public interface OnSignupTaskProgressUpdate extends FinishableEvent
	{
		public void onProgressUpdate(StateValue value);
	}
	
	public int getDisplayChild(){
		return ((SignupActivity) context).getDisplayItem();
	}

	public void autoFillPin(String pin)
	{
		((SignupActivity) context).autoFillPin(pin);
	}

	public enum State
	{
		MSISDN, ADDRESSBOOK, NAME, PULLING_PIN, PIN, ERROR, PROFILE_IMAGE, SCANNING_CONTACTS, PIN_VERIFIED, BACKUP_AVAILABLE, RESTORING_BACKUP
	};

	public class StateValue
	{
		public State state;

		public String value;
		
		public StateValue(State state, String value)
		{
			this.state = state;
			this.value = value;
		}
	};

	private Context context;

	private String data;

	private SMSReceiver receiver;

	private static SignupTask signupTask;

	private OnSignupTaskProgressUpdate onSignupTaskProgressUpdate;

	private boolean isRunning = false;

	private boolean isPinError = false;

	private HikeHttpRequest profilePicRequest;

	private Bitmap profilePicSmall;

	public static boolean isAlreadyFetchingNumber = false;

	private String userName;
	
	private StateValue mStateValue;

	private static final String INDIA_ISO = "IN";

	public static final String START_UPLOAD_PROFILE = "start";

	public static final String FINISHED_UPLOAD_PROFILE = "finish";
	
	public boolean isRunning()
	{
		return isRunning;
	}

	private SignupTask(Activity activity)
	{
		this.onSignupTaskProgressUpdate = (OnSignupTaskProgressUpdate) activity;
		this.context = activity;
		SignupTask.isAlreadyFetchingNumber = false; 
	}

	public static SignupTask getSignupTask(Activity activity)
	{
		if (signupTask == null || signupTask.isCancelled())
		{
			signupTask = new SignupTask(activity);
		}
		else
		{
			signupTask.setActivity(activity);
		}
		return signupTask;
	}

	public void addUserInput(String string)
	{
		this.data = string;
		synchronized (this)
		{
			this.notify();
		}
	}

	public void addProfilePicPath(String path, Bitmap profilePic)
	{
		if(path != null)
		{
			Editor editor = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
			editor.putString(HikeMessengerApp.SIGNUP_PROFILE_PIC_PATH, path);
			editor.commit();
		}
		this.profilePicSmall = profilePic;
	}

	public void addUserName(String name)
	{
		this.userName = name;
	}

	@Override
	protected Boolean doInBackground(Void... unused)
	{
		deletePreviouslySavedProfileImages();
		Logger.e("SignupTask", "FETCHING NUMBER? " + isAlreadyFetchingNumber);
		isPinError = false;
		isRunning = true;
		SharedPreferences settings = this.context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String msisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING, null);
		boolean ab_scanned = settings.getBoolean(HikeMessengerApp.ADDRESS_BOOK_SCANNED, false);
		boolean canPullInSms = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
		String name = settings.getString(HikeMessengerApp.NAME_SETTING, null);

		if (isCancelled())
		{
			/* just gtfo */
			Logger.d("SignupTask", "Task was cancelled");
			return Boolean.FALSE;
		}

		if (msisdn == null)
		{

			/*
			 * need to get the MSISDN. If we're on Wifi don't bother trying to autodetect
			 */
			ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Activity.CONNECTIVITY_SERVICE);
			NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

			TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			String countryIso = manager.getNetworkCountryIso().toUpperCase();

			AccountUtils.AccountInfo accountInfo = null;
			if (!SignupTask.isAlreadyFetchingNumber && INDIA_ISO.equals(countryIso) && !wifi.isConnected())
			{
				accountInfo = AccountUtils.registerAccount(context, null, null);
				if (accountInfo == null)
				{
					/* network error, signal a failure */
					publishProgress(new StateValue(State.ERROR, null));
					return Boolean.FALSE;
				}
			}
			if (accountInfo == null || TextUtils.isEmpty(accountInfo.msisdn))
			{
				if (!SignupTask.isAlreadyFetchingNumber)
				{
					/* no MSISDN, ask the user for it */
					publishProgress(new StateValue(State.MSISDN, ""));
					SignupTask.isAlreadyFetchingNumber = true;
				}
				/* wait until we're notified that we have the msisdn */
				try
				{
					synchronized (this)
					{
						this.wait();
					}
				}
				catch (InterruptedException e)
				{
					Logger.d("SignupTask", "Interrupted exception while waiting for msisdn", e);
					publishProgress(new StateValue(State.ERROR, null));
					return Boolean.FALSE;
				}

				String number = this.data;
				this.data = null;
				Logger.d("SignupTask", "NUMBER RECEIVED: " + number);

				if (canPullInSms)
				{
					/*
					 * register broadcast receiver to get the actual PIN code, and pass it to us
					 */
					IntentFilter intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
					intentFilter.setPriority(999);
					receiver = new SMSReceiver();

					this.context.getApplicationContext().registerReceiver(receiver, new IntentFilter(intentFilter));
				}

				String unauthedMSISDN = AccountUtils.validateNumber(number);

				if (TextUtils.isEmpty(unauthedMSISDN))
				{
					Logger.d("SignupTask", "Unable to send PIN to user");
					publishProgress(new StateValue(State.ERROR, null));
					return Boolean.FALSE;
				}

				/*
				 * Saving this for the call me functionality
				 */
				Editor editor = settings.edit();
				editor.putString(HikeMessengerApp.MSISDN_ENTERED, unauthedMSISDN);
				editor.commit();

				if (canPullInSms)
				{
					publishProgress(new StateValue(State.PULLING_PIN, null));

					synchronized (this)
					{
						/* wait until we get an SMS from the server */
						try
						{
							this.wait(HikeConstants.PIN_CAPTURE_TIME);
						}
						catch (InterruptedException e)
						{
							Logger.e("SignupTask", "Task was interrupted", e);
						}
					}

				}
				
				accountInfo = null;
				do
				{
					if (this.data == null)
					{
						data = "";
						if (!isPinError)
						{
							publishProgress(new StateValue(State.PIN, data));
						}
						synchronized (this)
						{
							try
							{
								this.wait();
							}
							catch (InterruptedException e)
							{
								Logger.e("SignupTask", "Task was interrupted while taking the pin", e);
							}
						}
					}
					if (isCancelled())
					{
						/* just gtfo */
						Logger.d("SignupTask", "Task was cancelled");
						return Boolean.FALSE;
					}
					String pin = this.data;
					if (TextUtils.isEmpty(pin))
					{
						publishProgress(new StateValue(State.ERROR, HikeConstants.CHANGE_NUMBER));
						return Boolean.FALSE;
					}
					accountInfo = AccountUtils.registerAccount(context, pin, unauthedMSISDN);
					/*
					 * if it fails, we try once again.
					 */
					if (accountInfo == null)
					{
						accountInfo = AccountUtils.registerAccount(context, pin, unauthedMSISDN);
					}

					if (accountInfo == null)
					{
						this.data = null;
						publishProgress(new StateValue(State.ERROR, null));
						return Boolean.FALSE;
					}
					else if (accountInfo.smsCredits == -1)
					{
						this.data = null;
						isPinError = true;
						publishProgress(new StateValue(State.PIN, HikeConstants.PIN_ERROR));
					}
				}
				while (this.data == null);
				
				if(canPullInSms && receiver != null)
				{
					this.context.getApplicationContext().unregisterReceiver(receiver);
					receiver = null;
				}
				
				publishProgress(new StateValue(State.PIN_VERIFIED, null));
				synchronized (this)
				{
					try
					{
						this.wait();
					}
					catch (InterruptedException e)
					{
						Logger.e("SignupTask", "Task was interrupted while taking the pin", e);
					}
				}
				
			}

			Logger.d("SignupTask", "saving MSISDN/Token");
			msisdn = accountInfo.msisdn;
			/* save the new msisdn */
			Utils.savedAccountCredentials(accountInfo, settings.edit());
			/* msisdn set, yay */
			publishProgress(new StateValue(State.MSISDN, msisdn));
		}
		else
		{
			publishProgress(new StateValue(State.MSISDN, HikeConstants.DONE));
		}
		this.data = null;
		// We're doing this to prevent the WelcomeScreen from being shown the
		// next time we start the app.
		if (isCancelled())
		{
			/* just gtfo */
			Logger.d("SignupTask", "Task was cancelled");
			return Boolean.FALSE;
		}

		Editor ed = this.context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
		ed.putBoolean(HikeMessengerApp.ACCEPT_TERMS, true);
		ed.commit();
		
		if(userName != null)
		{
			publishProgress(new StateValue(State.SCANNING_CONTACTS, ""));
		}

		/* scan the addressbook */
		if (!ab_scanned)
		{
			String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
			ContactManager conMgr = ContactManager.getInstance();
			List<ContactInfo> contactinfos = conMgr.getContacts(this.context);
			conMgr.setGreenBlueStatus(this.context, contactinfos);
			
			try
			{
				Map<String, List<ContactInfo>> contacts = conMgr.convertToMap(contactinfos);
				JSONObject jsonForAddressBookAndBlockList = AccountUtils.postAddressBook(token, contacts);

				List<ContactInfo> addressbook = AccountUtils.getContactList(jsonForAddressBookAndBlockList, contacts);
				List<String> blockList = AccountUtils.getBlockList(jsonForAddressBookAndBlockList);

				if (jsonForAddressBookAndBlockList.has(HikeConstants.PREF))
				{
					JSONObject prefJson = jsonForAddressBookAndBlockList.getJSONObject(HikeConstants.PREF);
					JSONArray contactsArray = prefJson.optJSONArray(HikeConstants.CONTACTS);
					if (contactsArray != null)
					{
						Editor editor = settings.edit();
						editor.putString(HikeMessengerApp.SERVER_RECOMMENDED_CONTACTS, contactsArray.toString());
						editor.commit();
					}
				}
				// List<>
				// TODO this exception should be raised from the postAddressBook
				// code
				if (addressbook == null)
				{
					publishProgress(new StateValue(State.ERROR, HikeConstants.ADDRESS_BOOK_ERROR));
					return Boolean.FALSE;
				}
				Logger.d("SignupTask", "about to insert addressbook");
				ContactManager.getInstance().setAddressBookAndBlockList(addressbook, blockList);

			}
			catch (Exception e)
			{
				Logger.e("SignupTask", "Unable to post address book", e);
				publishProgress(new StateValue(State.ERROR, HikeConstants.ADDRESS_BOOK_ERROR));
				return Boolean.FALSE;
			}

			Editor editor = settings.edit();
			editor.putBoolean(HikeMessengerApp.ADDRESS_BOOK_SCANNED, true);
			editor.putBoolean(HikeMessengerApp.GREENBLUE_DETAILS_SENT, true);
			editor.commit();
			/*
			 * addressbook scanned, sick
			 */
			publishProgress(new StateValue(State.ADDRESSBOOK, ""));
		}
		else
		{
			publishProgress(new StateValue(State.ADDRESSBOOK, HikeConstants.DONE));
		}

		if (isCancelled())
		{
			/* just gtfo */
			Logger.d("SignupTask", "Task was cancelled");
			return Boolean.FALSE;
		}

		if (name == null)
		{
			try
			{
				if (userName == null)
				{
					/*
					 * publishing this will cause the the Activity to ask the user for a name and signal us
					 */
					publishProgress(new StateValue(State.NAME, ""));
					synchronized (this)
					{
						this.wait();
					}
				}
				
				if(getDisplayChild() != SignupActivity.SCANNING_CONTACTS)
				{
					publishProgress(new StateValue(State.SCANNING_CONTACTS, ""));
				}
				publishProgress(new StateValue(State.PROFILE_IMAGE, START_UPLOAD_PROFILE));
				AccountUtils.setProfile(userName);
			}
			catch (InterruptedException e)
			{
				Logger.e("SignupTask", "Interrupted exception while waiting for name", e);
				publishProgress(new StateValue(State.ERROR, null));
				return Boolean.FALSE;
			}
			catch (NetworkErrorException e)
			{
				Logger.e("SignupTask", "Unable to set name", e);
				publishProgress(new StateValue(State.ERROR, null));
				return Boolean.FALSE;
			}
			catch (IllegalStateException e)
			{
				Logger.e(getClass().getSimpleName(), "Null Token", e);
				return Boolean.FALSE;
			}

			this.data = null;
			Editor editor = settings.edit();
			editor.putString(HikeMessengerApp.NAME_SETTING, userName);
			/*
			 * Setting these values as true for now. They will be reset on upgrades.
			 */
			editor.putBoolean(HikeMessengerApp.DEVICE_DETAILS_SENT, true);
			editor.putBoolean(HikeMessengerApp.UPGRADE_RAI_SENT, true);
			editor.commit();
		}

		/* set the name */
		publishProgress(new StateValue(State.NAME, userName));

		if (profilePicSmall != null)
		{
				byte[] bytes = BitmapUtils.bitmapToBytes(profilePicSmall, Bitmap.CompressFormat.JPEG, 100);
				ContactManager.getInstance().setIcon(msisdn, bytes, false);
		}
		
		publishProgress(new StateValue(State.PROFILE_IMAGE, FINISHED_UPLOAD_PROFILE));

		this.data = null;
		if (DBBackupRestore.getInstance(context).isBackupAvailable())
		{
			publishProgress(new StateValue(State.BACKUP_AVAILABLE,null));
			synchronized (this)
			{
				try
				{
					this.wait();
				}
				catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
					Logger.d("backup","Interrupted while waiting for user's choice on restore.");
				}
			}
		}

		while (!TextUtils.isEmpty(this.data))
		{
			mStateValue = new StateValue(State.RESTORING_BACKUP,null);
			boolean status = DBBackupRestore.getInstance(context).restoreDB();
			if (status)
			{
				HikeConversationsDatabase.getInstance().resetConversationsStealthStatus();
				ContactManager.getInstance().init(context);
				this.data = null;
				mStateValue = new StateValue(State.RESTORING_BACKUP,Boolean.TRUE.toString());
			}
			else
			{
				mStateValue = new StateValue(State.RESTORING_BACKUP,Boolean.FALSE.toString());
			}
			publishProgress(mStateValue);
			synchronized (this)
			{
				try
				{
					this.wait();
				}
				catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
					Logger.d("backup","Interrupted while waiting for user's post restore animation to complete.");
				}
			}
		}
		Logger.d("SignupTask", "Publishing Token_Created");

		/* tell the service to start listening for new messages */
		HikeMessengerApp.getPubSub().publish(HikePubSub.TOKEN_CREATED, null);
		isAlreadyFetchingNumber = false;

		/*
		 * We show these tips only to upgrading users
		 */
		settings.edit().putBoolean(HikeMessengerApp.SHOWN_WELCOME_HIKE_TIP, true).commit();
		/*
		 * We show this tip only to new signup users
		 */
		settings.edit().putBoolean(HikeMessengerApp.SHOW_STEALTH_INFO_TIP, true).commit();
		/*
		 * We need to show update available for hardcoded categories only to upgrading users.
		 */
		settings.edit().putBoolean(StickerManager.SHOWN_HARDCODED_CATEGORY_UPDATE_AVAILABLE, true).commit();
		return Boolean.TRUE;
	}
	
	public StateValue getStateValue()
	{
		return mStateValue;
	}

	@Override
	protected void onCancelled()
	{
		if (signupTask != null)
		{
			signupTask.isRunning = false;
		}
		signupTask = null;
		Logger.d("SignupTask", "onCancelled called");
		unregisterReceiver();
	}

	@Override
	protected void onPostExecute(Boolean result)
	{

		if (signupTask != null)
		{
			signupTask.isRunning = false;
		}
		signupTask = null;
		onSignupTaskProgressUpdate.onFinish(result.booleanValue());
	}

	@Override
	protected void onProgressUpdate(StateValue... values)
	{
		onSignupTaskProgressUpdate.onProgressUpdate(values[0]);
	}

	@Override
	public void setActivity(Activity activity)
	{
		this.context = activity;
		this.onSignupTaskProgressUpdate = (OnSignupTaskProgressUpdate) activity;
	}

	@Override
	public boolean isFinished()
	{
		return false;
	}

	public void cancelTask()
	{
		this.cancel(true);
		Logger.d("SignupTask", "cancelling it manually");
		unregisterReceiver();
	}

	/*
	 * For removing intent when finishing the activity
	 */
	private void unregisterReceiver()
	{
		if (receiver != null)
		{
			try
			{
				this.context.unregisterReceiver(receiver);
			}
			catch (IllegalArgumentException e)
			{
				Logger.d("SignupTask", "IllegalArgumentException while unregistering receiver", e);
			}
			receiver = null;
		}
	}

	public static SignupTask startTask(Activity activity)
	{
		getSignupTask(activity);
		if (!signupTask.isRunning())
		{
			Utils.executeSignupTask(signupTask);
		}
		return signupTask;
	}

	public static SignupTask startTask(Activity activity, String userName, Bitmap profilePicSmall)
	{
		getSignupTask(activity);
		if (!signupTask.isRunning())
		{
			signupTask.addUserName(userName);
			signupTask.addProfilePicPath(null, profilePicSmall);
			/*
			 * if we are on signupActivity we should not anymore try to
			 * auto auth
			 */
			SignupTask.isAlreadyFetchingNumber = true;
			Utils.executeSignupTask(signupTask);
		}
		return signupTask;
	}

	public static SignupTask restartTask(Activity activity)
	{
		if (signupTask != null && signupTask.isRunning())
		{
			signupTask.cancelTask();
		}
		startTask(activity);
		return signupTask;
	}
	
	public static SignupTask restartTask(Activity activity, String userName, Bitmap profilePicSmall)
	{
		if (signupTask != null && signupTask.isRunning())
		{
			signupTask.cancelTask();
		}
		startTask(activity, userName, profilePicSmall);
		return signupTask;
	}
	
    private void deletePreviouslySavedProfileImages()
    {
    	String dirPath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
    	File dir = new File(dirPath);
    	if (!dir.exists())
    	{
    		return;
    	}
    	for (File file : dir.listFiles())
    	{
    		file.delete();
    	}
    }

}
