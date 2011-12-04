package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.ContactInfo;
import com.bsb.hike.utils.HikeUserDatabase;

import android.app.Activity;
import android.content.Intent;
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
					ContactsContract.Contacts.DISPLAY_NAME
			};

			String selection = ContactsContract.Contacts.HAS_PHONE_NUMBER + "='1'";
			Cursor contacts = managedQuery(ContactsContract.Contacts.CONTENT_URI, projection, selection, null, null);

			int idFieldColumnIndex = contacts.getColumnIndex(ContactsContract.Contacts._ID);
			int nameFieldColumnIndex = contacts.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
			List<ContactInfo> contactinfos = new ArrayList<ContactInfo>();
			while (contacts.moveToNext()) {
				String id = contacts.getString(idFieldColumnIndex);
				String name = contacts.getString(nameFieldColumnIndex);
				//TODO batch this up
				Cursor phoneCursor = managedQuery(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "= ?", new String[]{id}, null);
				int numberIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
				int idIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID);
				while(phoneCursor.moveToNext()) {
					String number = phoneCursor.getString(numberIdx);
					String rowId = phoneCursor.getString(idIdx);
					contactinfos.add(new ContactInfo(rowId, name, number));
				}
			}

			SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
			String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
			try {
				List<ContactInfo> addressbook = AccountUtils.postAddressBook(token, contactinfos);
				Log.d("SAB", "about to insert");
				HikeUserDatabase db = new HikeUserDatabase(ScanningAddressBook.this);
				db.updateAddressBook(addressbook);
			} catch(Exception e) {
				//TODO raise a dialog here, ask the user to retry later?  Or continue?
				Log.e("ScanningAddressBook", "Unable to post address book", e);
			}
			Log.d("ScanningAddressBook", "Finished scanning addressbook");
			Intent intent = new Intent(ScanningAddressBook.this, MessagesList.class);
			intent.putExtra("first", true);
			startActivity(intent);
			finish();
			return null;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//TODO this is called when you rotate the screen.  We shouldn't
		Log.d(ScanningAddressBook.class.getSimpleName(), "onCreate");
	 	setContentView(R.layout.scanningcontactlist);
	 	ScanAddressBookTask sab = new ScanAddressBookTask();
	 	sab.execute();
	}

}
