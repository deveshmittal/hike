package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.utils.AccountUtils;
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
				mActivity.stopManagingCursor(mContacts);
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
			Log.d("SAB", "Starting to scan address book");
            ContentResolver cr = getContentResolver();
			long tm = System.currentTimeMillis();
			while (mContacts.moveToNext()) {
				String id = mContacts.getString(idFieldColumnIndex);
				String name = mContacts.getString(nameFieldColumnIndex);
		        Cursor phones = cr.query(Phone.CONTENT_URI, null, Phone.CONTACT_ID + " = " + id, null, null);
		        String number = null;
		        if (phones == null) {
		            return null;
		        }

		        while (phones.moveToNext()) {
		            number = phones.getString(phones.getColumnIndex(Phone.NUMBER));
		            int type = phones.getInt(phones.getColumnIndex(Phone.TYPE));
		            switch (type) {
		            case Phone.TYPE_MOBILE:
		                break;
		            }
		        }

		        phones.close();
//				String number = ContactUtils.getMobileNumber(getContentResolver(), Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id));
				if (number != null) {
					contactinfos.add(new ContactInfo(id, number, name));
				}
			}
            Log.d("SAB", "Finished to scan address book " + (System.currentTimeMillis() - tm));
			SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
			String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
			HikeUserDatabase db = null;
			try {
				List<ContactInfo> addressbook = AccountUtils.postAddressBook(token, contactinfos);
				Log.d("SAB", "about to insert");
				db = new HikeUserDatabase(ScanningAddressBook.this);
				db.updateAddressBook(addressbook);
			} catch(Exception e) {
				//TODO raise a dialog here, ask the user to retry later?  Or continue?
				Log.e("ScanningAddressBook", "Unable to post address book", e);
			} finally {
				if (db != null) {
					db.close();
				}
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
