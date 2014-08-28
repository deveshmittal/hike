package com.bsb.hike.ui;

import java.util.ArrayList;

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;

public class SettingsActivity extends HikeAppStateBaseFragmentActivity implements OnItemClickListener
{

	private enum ViewType
	{
		SETTINGS, VERSION
	};

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);

		ArrayList<String> items = new ArrayList<String>();

		items.add(getString(R.string.notifications));
		items.add(getString(R.string.settings_media));
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(HikeConstants.FREE_SMS_PREF, true))
		{
			int credits = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getInt(HikeMessengerApp.SMS_SETTING, 0);
			items.add(getString(R.string.sms_with_credits, credits));
		}
		else
		{
			items.add(getString(R.string.sms));
		}
		items.add(getString(R.string.manage_account));
		items.add(getString(R.string.privacy));
		items.add(getString(R.string.sync_contacts));
		items.add(getString(R.string.help));
		items.add(null);

		final ArrayList<String> itemsSummary = new ArrayList<String>();

		itemsSummary.add(getString(R.string.notifications_hintext));
		itemsSummary.add(getString(R.string.media_settings_hinttext));
		itemsSummary.add(getString(R.string.sms_setting_hinttext));
		itemsSummary.add(getString(R.string.account_hintttext));
		itemsSummary.add(getString(R.string.privacy_setting_hinttext));
		itemsSummary.add(getString(R.string.sync_contacts_hinttext));
		itemsSummary.add(getString(R.string.help_hinttext));

		final ArrayList<Integer> itemIcons = new ArrayList<Integer>();

		itemIcons.add(R.drawable.ic_notifications_settings);
		itemIcons.add(R.drawable.ic_auto_download_media_settings);
		itemIcons.add(R.drawable.ic_sms_settings);
		itemIcons.add(R.drawable.ic_account_settings);
		itemIcons.add(R.drawable.ic_privacy_settings);
		itemIcons.add(R.drawable.ic_sync_contacts);
		itemIcons.add(R.drawable.ic_help_settings);

		ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this, R.layout.setting_item, R.id.item, items)
		{

			@Override
			public int getItemViewType(int position)
			{
				if (getItem(position) == null)
				{
					return ViewType.VERSION.ordinal();
				}
				else
				{
					return ViewType.SETTINGS.ordinal();
				}
			}

			@Override
			public int getViewTypeCount()
			{
				return ViewType.values().length;
			}

			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				ViewType viewType = ViewType.values()[getItemViewType(position)];
				if (convertView == null)
				{
					switch (viewType)
					{
					case SETTINGS:
						convertView = getLayoutInflater().inflate(R.layout.setting_item, null);
						break;

					case VERSION:
						convertView = getLayoutInflater().inflate(R.layout.app_version_item, parent, false);
						break;
					}
				}

				switch (viewType)
				{
				case SETTINGS:
					TextView header = (TextView) convertView.findViewById(R.id.item);
					TextView summary = (TextView) convertView.findViewById(R.id.summary);
					ImageView iconImage = (ImageView) convertView.findViewById(R.id.icon);

					header.setText(getItem(position));
					summary.setText(itemsSummary.get(position));
					iconImage.setImageResource(itemIcons.get(position));
					break;

				case VERSION:
					TextView appVersion = (TextView) convertView.findViewById(R.id.app_version);
					try
					{
						appVersion.setText(getString(R.string.app_version, getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
					}
					catch (NameNotFoundException e)
					{
						appVersion.setText("");
					}
					break;

				}
				return convertView;
			}

		};

		ListView settingsList = (ListView) findViewById(R.id.settings_content);
		settingsList.setAdapter(listAdapter);
		settingsList.setOnItemClickListener(this);
		setupActionBar();
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.settings);
		backContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(SettingsActivity.this, HomeActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		});

		actionBar.setCustomView(actionBarView);
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
	{
		Intent intent = null;
		switch (position)
		{
		case 0:
			intent = new Intent(this, HikePreferences.class);
			intent.putExtra(HikeConstants.Extras.PREF, R.xml.notification_preferences);
			intent.putExtra(HikeConstants.Extras.TITLE, R.string.notifications);
			break;

		case 1:
			intent = new Intent(this, HikePreferences.class);
			intent.putExtra(HikeConstants.Extras.PREF, R.xml.media_download_preferences);
			intent.putExtra(HikeConstants.Extras.TITLE, R.string.auto_download_media);
			break;
		case 2:
			intent = new Intent(this, CreditsActivity.class);
			break;
		case 3:
			intent = new Intent(this, HikePreferences.class);
			intent.putExtra(HikeConstants.Extras.PREF, R.xml.account_preferences);
			intent.putExtra(HikeConstants.Extras.TITLE, R.string.account);
			break;
		case 4:
			intent = Utils.getIntentForPrivacyScreen(this);
			break;
		case 5:
			Intent contactSyncIntent = new Intent(HikeService.MQTT_CONTACT_SYNC_ACTION);
			contactSyncIntent.putExtra(HikeConstants.Extras.MANUAL_SYNC, true);
			sendBroadcast(contactSyncIntent);
			Toast.makeText(getApplicationContext(), R.string.contacts_sync_started, Toast.LENGTH_SHORT).show();
			break;
		case 6:
			intent = new Intent(this, HikePreferences.class);
			intent.putExtra(HikeConstants.Extras.PREF, R.xml.help_preferences);
			intent.putExtra(HikeConstants.Extras.TITLE, R.string.help);
			break;
		}
		if (intent != null)
		{
			startActivity(intent);
		}
	}
}
