package com.bsb.hike.ui;

import java.util.ArrayList;

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.DrawerBaseActivity;

public class SettingsActivity extends DrawerBaseActivity implements
		OnItemClickListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);

		afterSetContentView(savedInstanceState);

		TextView titleTV = (TextView) findViewById(R.id.title_centered);
		titleTV.setText(R.string.settings);

		ArrayList<String> items = new ArrayList<String>();
		items.add(getString(R.string.notifications));
		items.add(getString(R.string.blocked_list));
		items.add(getString(R.string.manage_account));
		items.add(getString(R.string.system_health));
		items.add(getString(R.string.faq));
		items.add(getString(R.string.contact));

		final ArrayList<Integer> itemIcons = new ArrayList<Integer>();
		itemIcons.add(R.drawable.ic_notifications);
		itemIcons.add(R.drawable.ic_block);
		itemIcons.add(R.drawable.ic_manage_account);
		itemIcons.add(R.drawable.ic_system_health);
		itemIcons.add(R.drawable.ic_faq);
		itemIcons.add(R.drawable.ic_contact);

		ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this,
				R.layout.setting_item, R.id.item, items) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = super.getView(position, convertView, parent);
				TextView tv = (TextView) v.findViewById(R.id.item);
				tv.setCompoundDrawablesWithIntrinsicBounds(
						itemIcons.get(position), 0, 0, 0);
				return v;
			}

		};

		ListView settingsList = (ListView) findViewById(R.id.settings_content);
		settingsList.setAdapter(listAdapter);
		settingsList.setOnItemClickListener(this);
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view,
			int position, long id) {
		Intent intent = null;
		switch (position) {
		case 0:
			intent = new Intent(this, HikePreferences.class);
			intent.putExtra(HikeConstants.Extras.PREF,
					R.xml.notification_preferences);
			break;
		case 1:
			intent = new Intent(this, HikeListActivity.class);
			intent.putExtra(HikeConstants.Extras.BLOCKED_LIST, true);
			break;
		case 2:
			intent = new Intent(this, HikePreferences.class);
			intent.putExtra(HikeConstants.Extras.PREF,
					R.xml.privacy_preferences);
			break;
		case 3:
			intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(HikeConstants.SYSTEM_HEALTH_URL));
			break;
		case 4:
			intent = new Intent(this, WebViewActivity.class);
			intent.putExtra(HikeConstants.Extras.URL_TO_LOAD,
					HikeConstants.HELP_URL);
			intent.putExtra(HikeConstants.Extras.TITLE, getString(R.string.faq));
			break;
		case 5:
			intent = new Intent(Intent.ACTION_SENDTO);
			intent.setData(Uri.parse("mailto:" + HikeConstants.MAIL));

			StringBuilder message = new StringBuilder("\n\n");

			try {
				message.append(getString(R.string.hike_version)
						+ " "
						+ getPackageManager().getPackageInfo(getPackageName(),
								0).versionName + "\n");
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			message.append(getString(R.string.device_name) + " "
					+ Build.MANUFACTURER + " " + Build.MODEL + "\n");

			message.append(getString(R.string.android_version) + " "
					+ Build.VERSION.RELEASE + "\n");

			String msisdn = getSharedPreferences(
					HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getString(
					HikeMessengerApp.MSISDN_SETTING, "");
			message.append(getString(R.string.msisdn) + " " + msisdn);

			intent.putExtra(Intent.EXTRA_TEXT, message.toString());
			break;
		}
		startActivity(intent);
	}
}
