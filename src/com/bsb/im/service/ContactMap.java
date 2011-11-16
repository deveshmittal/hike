package com.bsb.im.service;

import com.bsb.im.R;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class ContactMap
{
	private Activity mActivity;

	public ContactMap(Activity act)
	{
		mActivity = act;
	}

	public String getContact()
	{
		String addressbook = "";
		ContentResolver content = mActivity.getContentResolver();
		// 获得�?��的联系人
		Cursor cur = content.query(ContactsContract.Contacts.CONTENT_URI, null,
				null, null, null);
		// 循环遍历
		if (cur.moveToFirst())
		{
			int idColumn = cur.getColumnIndex(ContactsContract.Contacts._ID);

			int displayNameColumn = cur
					.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
			do
			{
				// 获得联系人的ID�?
				String contactId = cur.getString(idColumn);
				// 获得联系人姓�?
				String disPlayName = cur.getString(displayNameColumn);
				
				// 查看该联系人有多少个电话号码。如果没有这返回值为0
				int phoneCount = cur.getInt(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
				if (phoneCount > 0)
				{
					// 获得联系人的电话号码
					// Cursor phones =
					// getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,ContactsContract.CommonDataKinds.Phone.CONTACT_ID+
					// " = " + contactId, null, null);

					// 获得联系人的手机号码
					Cursor phones = content.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,ContactsContract.CommonDataKinds.Phone.CONTACT_ID+"="+contactId+"and"+ContactsContract.CommonDataKinds.Phone.TYPE+"="+ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,null, null);
					if (phones.moveToFirst())
					{
						do
						{
							// 遍历�?��的电话号�?
							String phoneNumber = phones
									.getString(phones
											.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
							if(addressbook.equals(""))
							{
								addressbook = disPlayName + ":" + phoneNumber;
							}
							else
							{
								addressbook += "," + disPlayName + ":" + phoneNumber;
							}
						}
						while (phones.moveToNext());
					}
				}

			}
			while (cur.moveToNext());
		}
		return addressbook;
	}
	
	public String getPhoneNumber(){  
	    TelephonyManager mTelephonyMgr;  
	    mTelephonyMgr = (TelephonyManager) mActivity.getSystemService(Context.TELEPHONY_SERVICE);   
	    return mTelephonyMgr.getLine1Number();  
	}   
}
