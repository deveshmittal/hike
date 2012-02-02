package com.bsb.hike.ui;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.ContactUtils;
import com.bsb.hike.utils.HikeUserDatabase;

public class ScanningAddressBook extends Activity
{

	private class ScanAddressBookTask extends AsyncTask<Void, Void, Void>
	{

		@Override
		protected Void doInBackground(Void... params)
		{
			List<ContactInfo> contactinfos = ContactUtils.getContacts(ScanningAddressBook.this);
			SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
			String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
			HikeUserDatabase db = null;
			try
			{
				Map<String, List<ContactInfo>> contacts = ContactUtils.convertToMap(contactinfos);
				List<ContactInfo> addressbook = AccountUtils.postAddressBook(token, contacts);
				//TODO this exception should be raised from the postAddressBook code
				if (addressbook == null)
				{
					throw new IOException("Unable to retrieve address book");
				}
				Log.d("SAB", "about to insert");
				db = new HikeUserDatabase(ScanningAddressBook.this);
				db.setAddressBook(addressbook);

				/* Add a default message from hike */
				// TODO get the number for hikebot from the server?
				ContactInfo hikeContactInfo = new ContactInfo("__HIKE__", HikeConstants.HIKEBOT, "HikeBot", HikeConstants.HIKEBOT);
				hikeContactInfo.setOnhike(true);
				db.addContact(hikeContactInfo);
				ConvMessage message = new ConvMessage(getResources().getString(R.string.hikebot_message), hikeContactInfo.getMsisdn(),
						System.currentTimeMillis() / 1000, ConvMessage.State.RECEIVED_UNREAD);

				HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED_FROM_SENDER, message);
			}
			catch (Exception e)
			{
				Log.e("ScanningAddressBook", "Unable to post address book", e);
				Intent intent = new Intent(ScanningAddressBook.this, AccountCreateSuccess.class);
				intent.putExtra("failed", true);
				startActivity(intent);
				finish();
				return null;
			}
			finally
			{
				if (db != null)
				{
					db.close();
				}
			}

			Log.d("ScanningAddressBook", "Finished scanning addressbook");
			SharedPreferences.Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
			editor.putBoolean(HikeMessengerApp.ADDRESS_BOOK_SCANNED, true);
			editor.commit();

			Intent intent = new Intent(ScanningAddressBook.this, MessagesList.class);
			startActivity(intent);
			finish();
			
			return null;
		}
	}

	ScanAddressBookTask mTask;

	@Override
	public Object onRetainNonConfigurationInstance()
	{
		return mTask;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Log.d(ScanningAddressBook.class.getSimpleName(), "onCreate");
		setContentView(R.layout.scanningcontactlist);
		Object retained = getLastNonConfigurationInstance();
		if (retained instanceof ScanAddressBookTask)
		{
			mTask = (ScanAddressBookTask) retained;
		}
		else
		{
			mTask = new ScanAddressBookTask();
			mTask.execute();
		}
	}

}
