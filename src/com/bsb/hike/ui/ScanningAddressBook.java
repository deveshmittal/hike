package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.ContactInfo;
import com.bsb.hike.utils.ContactUtils;
import com.bsb.hike.utils.HikeUserDatabase;

public class ScanningAddressBook extends Activity {

	private class ScanAddressBookTask extends AsyncTask<Void, Void, Void> {

		private ScanningAddressBook mActivity;
		private Cursor mContacts;

		public ScanAddressBookTask(ScanningAddressBook activity) {
			mActivity = activity;
		}

		public void setActivity(ScanningAddressBook activity) {
			if (activity == null) {
				stopManagingCursor(mContacts);
			} else {
				activity.startManagingCursor(mContacts);
			}
			mActivity = activity;
		}

		@Override
		protected Void doInBackground(Void... params) {
			String[] projection = new String[] {
					ContactsContract.Contacts._ID,
					ContactsContract.Contacts.HAS_PHONE_NUMBER,
					ContactsContract.Contacts.DISPLAY_NAME
			};

			String selection = ContactsContract.Contacts.HAS_PHONE_NUMBER + "='1'";
			mContacts = mActivity.managedQuery(ContactsContract.Contacts.CONTENT_URI, projection, selection, null, null);

			int idFieldColumnIndex = mContacts.getColumnIndex(ContactsContract.Contacts._ID);
			int nameFieldColumnIndex = mContacts.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
			List<ContactInfo> contactinfos = new ArrayList<ContactInfo>();
			while (mContacts.moveToNext()) {
				String id = mContacts.getString(idFieldColumnIndex);
				String name = mContacts.getString(nameFieldColumnIndex);
				String number = ContactUtils.getMobileNumber(getContentResolver(), Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id));
				contactinfos.add(new ContactInfo(id, name, number));
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

	ScanAddressBookTask mTask;

	@Override
	public Object onRetainNonConfigurationInstance() {
		mTask.setActivity(null);
		return mTask;
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//TODO this is called when you rotate the screen.  We shouldn't
		Log.d(ScanningAddressBook.class.getSimpleName(), "onCreate");
	 	setContentView(R.layout.scanningcontactlist);
	 	Object retained = getLastNonConfigurationInstance();
	 	if (retained instanceof ScanAddressBookTask) {
	 		mTask = (ScanAddressBookTask) retained;
	 		mTask.setActivity(this);
	 	} else {
	 		mTask = new ScanAddressBookTask(this);
	 		mTask.execute();
	 	}
	}

}
