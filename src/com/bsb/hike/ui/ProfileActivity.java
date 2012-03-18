package com.bsb.hike.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.ProfileArrayAdapter;
import com.bsb.hike.models.ProfileItem;
import com.bsb.hike.models.utils.IconCacheManager;

public class ProfileActivity extends Activity implements OnItemClickListener
{

	private ImageView mIconView;
	private TextView mNameView;
	private ListView mListView;
	private TextView mTitleView;
	private TextView mMadeWithLoveView;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.profile);

		mIconView = (ImageView) findViewById(R.id.profile);
		mNameView = (TextView) findViewById(R.id.name);
		mListView = (ListView) findViewById(R.id.profile_preferences);
		mTitleView = (TextView) findViewById(R.id.title);
		mMadeWithLoveView = (TextView) findViewById(R.id.made_with_love);

		Drawable drawable = IconCacheManager.getInstance().getIconForMSISDN(HikeConstants.ME);
		mIconView.setImageDrawable(drawable);

		SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String name = settings.getString(HikeMessengerApp.NAME, "Set a name!");
		mNameView.setText(name);

		mTitleView.setText(getResources().getString(R.string.profile_title));

		/* add the heart in code because unicode isn't supported via xml*/
		mMadeWithLoveView.setText(String.format(getString(R.string.made_with_love), "\u2665"));

		ProfileItem[] items = new ProfileItem[] 
			{
				new ProfileItem.ProfileSettingsItem("Credits", R.drawable.ic_credits, HikeMessengerApp.SMS_SETTING),
				new ProfileItem.ProfilePreferenceItem("Notifications", R.drawable.ic_notifications, "notifications"),
				new ProfileItem. ProfilePreferenceItem("Privacy", R.drawable.ic_privacy, "privacy"),
				new ProfileItem.ProfileLinkItem("Help", R.drawable.ic_help, "http://www.bsb.im/about")
			};
		ProfileArrayAdapter adapter = new ProfileArrayAdapter(this, R.layout.profile_item, items);
		mListView.setAdapter(adapter);

		mListView.setOnItemClickListener(this);
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
	{
		ProfileItem item = (ProfileItem) adapterView.getItemAtPosition(position);
		Intent intent = item.getIntent(this);
		startActivity(intent);
	}

}
