package com.bsb.hike.ui;

import java.util.HashSet;
import java.util.Set;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.AccountUtils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;

public class ScanningAddressBook extends Activity {

	private class ScanAddressBookTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			String[] projection = new String[] {
					ContactsContract.Contacts._ID,
					ContactsContract.Contacts.HAS_PHONE_NUMBER,
			};

			String selection = ContactsContract.Contacts.HAS_PHONE_NUMBER + "='1'";
			Cursor contacts = managedQuery(ContactsContract.Contacts.CONTENT_URI, projection, selection, null, null);

			Set<String> phonenumbers = new HashSet<String>();
			int idFieldColumnIndex = contacts.getColumnIndex(ContactsContract.Contacts._ID);

			while (contacts.moveToNext()) {
				String id = contacts.getString(idFieldColumnIndex);
				//TODO batch this up
				Cursor phoneCursor = managedQuery(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "= ?", new String[]{id}, null);
				int idx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
				while(phoneCursor.moveToNext()) {
					String number = phoneCursor.getString(idx);
					Log.d("ScanningAddressBook", "adding number: "+number);
					phonenumbers.add(number);
				}
			}

			SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
			String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
			try {
				AccountUtils.postAddressBook(token, phonenumbers);
			} catch(Exception e) {
				//TODO raise a dialog here, ask the user to retry later?  Or continue?
				Log.e("ScanningAddressBook", "Unable to post address book");
			}
//			finish();
			return null;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	 	setContentView(R.layout.scanningcontactlist);
	 	ScanAddressBookTask sab = new ScanAddressBookTask();
	 	sab.execute();
	}

}
