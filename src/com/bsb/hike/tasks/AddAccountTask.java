package com.bsb.hike.tasks;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
//import com.bsb.hike.tasks.AddAccountTask.State;
//import com.bsb.hike.tasks.AddAccountTask.StateValue;

import com.bsb.hike.tasks.SignupTask.State;
import com.bsb.hike.tasks.SignupTask.StateValue;

import com.bsb.hike.tasks.SignupTask.OnSignupTaskProgressUpdate;
import com.bsb.hike.ui.AddAccountActivity;
import com.bsb.hike.ui.SignupActivity;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

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

public class AddAccountTask extends AsyncTask<Void, SignupTask.StateValue, Boolean> implements ActivityCallableTask{

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
							AddAccountTask.this.addUserInput(pin);
						} else{
							AddAccountTask.this.autoFillPin(pin);
						}
						this.abortBroadcast();
						break;
					}
				}
			}
		}
	}
	
//	public enum State
//	{
//		MSISDN, ADDRESSBOOK, NAME, PULLING_PIN, PIN, ERROR, PROFILE_IMAGE, GENDER, SCANNING_CONTACTS, PIN_VERIFIED, BACKUP_AVAILABLE, RESTORING_BACKUP
//	};

//	public class StateValue
//	{
//		public State state;
//
//		public String value;
//		
//		public StateValue(State state, String value)
//		{
//			this.state = state;
//			this.value = value;
//		}
//	}

	private static AddAccountTask addAccountTask;

	private static boolean isRunning;;
	
	private static boolean isAlreadyFetchingNumber;
	private boolean isPinError;
	private Context context;
	private String INDIA_ISO = "IN";
	private String data;
	private SMSReceiver receiver;
	private String userName;
	private Object isFemale;

	private SignupTask.OnSignupTaskProgressUpdate onAddAccountTaskProgressUpdate;

//	public interface OnAddAccountTaskProgressUpdate extends FinishableEvent implements SignupTask.OnSignupTaskProgressUpdate
//	{
//		public void onProgressUpdate(StateValue value);
//	}
	
	public AddAccountTask(Activity activity) {
		this.onAddAccountTaskProgressUpdate = (SignupTask.OnSignupTaskProgressUpdate) activity;
		this.context = activity;
		AddAccountTask.isAlreadyFetchingNumber = false; 
	}

	@Override
	public void setActivity(Activity activity) {
		 this.context = activity;
		this.onAddAccountTaskProgressUpdate = (SignupTask.OnSignupTaskProgressUpdate) activity;
	}
	
	public void cancelTask()
	{
		this.cancel(true);
		Logger.d("AddAccountTask", "cancelling it manually");
		unregisterReceiver();
	}
	
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
				Logger.d("AddAccountTask", "IllegalArgumentException while unregistering receiver", e);
			}
			receiver = null;
		}
	}

	public void autoFillPin(String pin) {
		((AddAccountActivity) context).autoFillPin(pin);		
	}

	public void addUserInput(String string) {
		this.data = string;
		synchronized (this)
		{
			this.notify();
		}		
	}

	public int getDisplayChild() {
		return ((AddAccountActivity) context).getDisplayItem();

	}

	@Override
	public boolean isFinished() {
		return false;
	}
	
	protected void onPreExecute()
	{
		isRunning = true;
		super.onPreExecute();
	}

	@Override
	protected Boolean doInBackground(Void... arg0) {
		Logger.e("AddAccountTask", "FETCHING NUMBER? " + isAlreadyFetchingNumber);
		isPinError = false;
		SharedPreferences settings = this.context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String msisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING, null);
		boolean ab_scanned = settings.getBoolean(HikeMessengerApp.ADDRESS_BOOK_SCANNED, false);
		boolean canPullInSms = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
		String name = settings.getString(HikeMessengerApp.NAME_SETTING, null);

		if (isCancelled())
		{
			/* just gtfo */
			Logger.d("AddAccountTask", "Task was cancelled");
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
			if (!AddAccountTask.isAlreadyFetchingNumber && INDIA_ISO .equals(countryIso) && !wifi.isConnected())
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
				if (!AddAccountTask.isAlreadyFetchingNumber)
				{
					/* no MSISDN, ask the user for it */
					publishProgress(new StateValue(State.MSISDN, ""));
					AddAccountTask.isAlreadyFetchingNumber = true;
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
					Logger.d("AddAccountTask", "Interrupted exception while waiting for msisdn", e);
					publishProgress(new StateValue(State.ERROR, null));
					return Boolean.FALSE;
				}

				String number = this.data;
				this.data = null;
				Logger.d("AddAccountTask", "NUMBER RECEIVED: " + number);

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
					Logger.d("AddAccountTask", "Unable to send PIN to user");
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
							Logger.e("AddAccountTask", "Task was interrupted", e);
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
								Logger.e("AddAccountTask", "Task was interrupted while taking the pin", e);
							}
						}
					}
					if (isCancelled())
					{
						/* just gtfo */
						Logger.d("AddAccountTask", "Task was cancelled");
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
						Logger.e("AddAccountTask", "Task was interrupted while taking the pin", e);
					}
				}
				
			}

			Logger.d("AddAccountTask", "saving MSISDN/Token");
			msisdn = accountInfo.msisdn;
			/* save the new msisdn */
			Utils.addAccountCredentials(context,accountInfo, settings, settings.edit());
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
			Logger.d("AddAccountTask", "Task was cancelled");
			return Boolean.FALSE;
		}

		Editor ed = this.context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
		ed.putBoolean(HikeMessengerApp.ACCEPT_TERMS, true);
		ed.commit();
		
		if(userName != null)
		{
			publishProgress(new StateValue(State.GENDER, ""));
		}
		

		if (isCancelled())
		{
			/* just gtfo */
			Logger.d("AddAccountTask", "Task was cancelled");
			return Boolean.FALSE;
		}
	

		Logger.d("AddAccountTask", "Publishing Token_Created");

		/* tell the service to start listening for new messages */
		HikeMessengerApp.getPubSub().publish(HikePubSub.TOKEN_CREATED, null);
		isAlreadyFetchingNumber = false;

		Editor edit = settings.edit();
		/*
		 * We show these tips only to upgrading users
		 */
		edit.putBoolean(HikeMessengerApp.SHOWN_WELCOME_HIKE_TIP, true);
		/*
		 * We show this tip only to new signup users
		 */
		edit.putBoolean(HikeMessengerApp.SHOW_STEALTH_INFO_TIP, true);
		
		/*
		 * We don't want to show red dot on overflow menu for new users
		 */
		edit.putBoolean(HikeConstants.IS_HOME_OVERFLOW_CLICKED, true);
		
		edit.commit();
		return Boolean.TRUE;
	}

	public static AddAccountTask startTask(Activity activity) {
		getAddAccountTask(activity);
		if (!AddAccountTask.isRunning())
		{
			Utils.executeAddAccountTask(addAccountTask);
		}
		return addAccountTask;
	}

	private static boolean isRunning() {
		return isRunning;
	}

	public static AddAccountTask getAddAccountTask(Activity activity)
	{
		if (addAccountTask == null || addAccountTask.isCancelled())
		{
			addAccountTask = new AddAccountTask(activity);
		}
		else
		{
			addAccountTask.setActivity(activity);
		}
		return addAccountTask;
	}
	
	protected void onPostExecute(Boolean result)
	{

		if (addAccountTask != null)
		{
			addAccountTask.isRunning = false;
		}
		addAccountTask = null;
		onAddAccountTaskProgressUpdate.onFinish(result.booleanValue());
	}
	
	@Override
	protected void onProgressUpdate(StateValue... values)
	{
		onAddAccountTaskProgressUpdate.onProgressUpdate(values[0]);
	}

}
