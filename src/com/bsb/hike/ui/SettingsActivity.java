package com.bsb.hike.ui;

import java.util.ArrayList;

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
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
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ImageViewerInfo;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.smartImageLoader.ProfilePicImageLoader;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

public class SettingsActivity extends HikeAppStateBaseFragmentActivity implements OnItemClickListener
{
	private ContactInfo contactInfo;
	
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
		addProfileHeaderView(settingsList);
		settingsList.setAdapter(listAdapter);
		settingsList.setOnItemClickListener(this);
		setupActionBar();
	}

	private void addProfileHeaderView(ListView settingsList)
	{
		View header = getLayoutInflater().inflate(R.layout.profile_header_other, null);
		
		ImageView profileImgView = (ImageView)header.findViewById(R.id.profile_image);
		TextView nameView = (TextView)header.findViewById(R.id.name);
		TextView statusView = (TextView)header.findViewById(R.id.subtext);
		ImageView arrowView = (ImageView)header.findViewById(R.id.view_profile);
		header.findViewById(R.id.divider_view).setVisibility(View.VISIBLE);
		arrowView.setImageDrawable(getResources().getDrawable(R.drawable.ic_arrow));
		arrowView.setOnClickListener(new OnClickListener() 
		{			
			@Override
			public void onClick(View v) 
			{
				Intent intent = new Intent(SettingsActivity.this, ProfileActivity.class);
				startActivity(intent);
			}
		});

		contactInfo = Utils.getUserContactInfo(getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE));
		String mappedId = contactInfo.getMsisdn() + ProfileActivity.PROFILE_ROUND_SUFFIX;
		
		// get name
		String name = contactInfo.getNameOrMsisdn();
		
		// set profile picture
		int mBigImageSize = getResources().getDimensionPixelSize(R.dimen.timeine_big_picture_size);
		(new ProfilePicImageLoader(this, mBigImageSize)).loadImage(mappedId, profileImgView, false, false, true);

		ImageViewerInfo imageViewerInfo = new ImageViewerInfo(contactInfo.getMsisdn() + ProfileActivity.PROFILE_PIC_SUFFIX, null, false, !ContactManager.getInstance().hasIcon(contactInfo.getMsisdn()));
		profileImgView.setTag(imageViewerInfo);

		// get hike status
		StatusMessageType[] statusMessagesTypesToFetch = {StatusMessageType.TEXT};
		StatusMessage status = HikeConversationsDatabase.getInstance().getLastStatusMessage(statusMessagesTypesToFetch, contactInfo);
		
		// set name and status
		nameView.setText(name);
		
		if(status != null)
		{
			statusView.setText(SmileyParser.getInstance().addSmileySpans(status.getText(), true));			
		}
		else
		{
			status = new StatusMessage(HikeConstants.JOINED_HIKE_STATUS_ID, null, contactInfo.getMsisdn(), contactInfo.getName(),
				getString(R.string.joined_hike_update), StatusMessageType.JOINED_HIKE, contactInfo.getHikeJoinTime());
		
			if (status.getTimeStamp() == 0)
			{
				statusView.setText(status.getText());
			}
			else
			{
				statusView.setText(status.getText() + " " + status.getTimestampFormatted(true, SettingsActivity.this));
			}
		}
		
		settingsList.addHeaderView(header, null, false);
	}
	
	public void onViewImageClicked(View v)
	{
		ImageViewerInfo imageViewerInfo = (ImageViewerInfo) v.getTag();

		String mappedId = imageViewerInfo.mappedId;
		String url = imageViewerInfo.url;

		Bundle arguments = new Bundle();
		arguments.putString(HikeConstants.Extras.MAPPED_ID, mappedId);
		arguments.putString(HikeConstants.Extras.URL, url);
		arguments.putBoolean(HikeConstants.Extras.IS_STATUS_IMAGE, imageViewerInfo.isStatusMessage);

		HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_IMAGE, arguments);
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
				onBackPressed();
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
		case 1:
			IntentManager.openSettingNotification(this);
			break;

		case 2:
			IntentManager.openSettingMedia(this);
			break;
		case 3:
			IntentManager.openSettingSMS(this);
			break;
		case 4:
			IntentManager.openSettingAccount(this);
			break;
		case 5:
			if(HikeMessengerApp.syncingContacts)
				return;
			if(!Utils.isUserOnline(this))
			{
				Utils.showNetworkUnavailableDialog(this);
				return;
			}
			Intent contactSyncIntent = new Intent(HikeService.MQTT_CONTACT_SYNC_ACTION);
			contactSyncIntent.putExtra(HikeConstants.Extras.MANUAL_SYNC, true);
			sendBroadcast(contactSyncIntent);
			Toast.makeText(getApplicationContext(), R.string.contacts_sync_started, Toast.LENGTH_SHORT).show();
			Utils.sendUILogEvent(HikeConstants.LogEvent.SETTINGS_REFRESH_CONTACTS);
			break;
		case 6:
			HikeConversationsDatabase.getInstance().updateToNewSharedMediaTable();
			IntentManager.openSettingHelp(this);
			break;
		}
	}
	
	public void onBackPressed()
	{
		if(removeFragment(HikeConstants.IMAGE_FRAGMENT_TAG))
		{
			return;
		}	
		super.onBackPressed();
	}
	
	@Override
	public boolean removeFragment(String tag)
	{
		boolean isRemoved = super.removeFragment(tag);
		
		if(isRemoved)
		{
			setupActionBar();
		}
		return isRemoved;
	}
}
