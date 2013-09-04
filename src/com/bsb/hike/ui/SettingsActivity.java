package com.bsb.hike.ui;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
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

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;

public class SettingsActivity extends HikeAppStateBaseFragmentActivity
		implements OnItemClickListener {

	private ArrayList<String> itemsSummary = new ArrayList<String>();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);

		ArrayList<String> items = new ArrayList<String>();
		items.add(getString(R.string.manage_account));
		items.add(getString(R.string.notifications));
		items.add(getString(R.string.sms));
		items.add(getString(R.string.privacy));
		items.add(getString(R.string.help));
		
		itemsSummary.add(getString(R.string.account_hintttext));
		itemsSummary.add(getString(R.string.notifications_hintext));
		itemsSummary.add(getString(R.string.sms_setting_hinttext));
		itemsSummary.add(getString(R.string.privacy_setting_hinttext));
		itemsSummary.add(getString(R.string.help_hinttext));

		final ArrayList<Integer> itemIcons = new ArrayList<Integer>();
		itemIcons.add(R.drawable.ic_account_settings);
		itemIcons.add(R.drawable.ic_notifications_settings);
		itemIcons.add(R.drawable.ic_sms_settings);
		itemIcons.add(R.drawable.ic_privacy_settings);
		itemIcons.add(R.drawable.ic_help_settings);
		
		ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this,
				R.layout.setting_item, R.id.item, items) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = super.getView(position, convertView, parent);
				TextView tv = (TextView) v.findViewById(R.id.summary);
				tv.setText(itemsSummary.get(position));
				ImageView iconImage = (ImageView) v.findViewById(R.id.icon);
				iconImage.setImageResource(itemIcons.get(position));

				return v;
			}

		};

		ListView settingsList = (ListView) findViewById(R.id.settings_content);
		settingsList.setAdapter(listAdapter);
		settingsList.setOnItemClickListener(this);
		setupActionBar();
	}

	private void setupActionBar() {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(
				R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.settings);
		backContainer.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(SettingsActivity.this,
						HomeActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		});

		actionBar.setCustomView(actionBarView);
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view,
			int position, long id) {
		Intent intent = null;
		switch (position) {
		case 0:
			intent = new Intent(this, HikePreferences.class);
			intent.putExtra(HikeConstants.Extras.PREF,
					R.xml.account_preferences);
			intent.putExtra(HikeConstants.Extras.TITLE, R.string.account);
			break;
			
		case 1:
			intent = new Intent(this, HikePreferences.class);
			intent.putExtra(HikeConstants.Extras.PREF,
					R.xml.notification_preferences);
			intent.putExtra(HikeConstants.Extras.TITLE, R.string.notifications);
			break;
		case 2:
			intent = new Intent(this, HikePreferences.class);
			intent.putExtra(HikeConstants.Extras.PREF, R.xml.sms_preferences);
			intent.putExtra(HikeConstants.Extras.TITLE, R.string.sms);
			break;
		case 3:
			intent = new Intent(this, HikePreferences.class);
			intent.putExtra(HikeConstants.Extras.PREF,
					R.xml.privacy_preferences);
			intent.putExtra(HikeConstants.Extras.TITLE, R.string.privacy);
			break;
		case 4:
			intent = new Intent(this, HikePreferences.class);
			intent.putExtra(HikeConstants.Extras.PREF, R.xml.help_preferences);
			intent.putExtra(HikeConstants.Extras.TITLE, R.string.help);
			break;
		}
		startActivity(intent);
	}
}
