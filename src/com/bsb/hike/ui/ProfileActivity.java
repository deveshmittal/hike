package com.bsb.hike.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.ProfileArrayAdapter;
import com.bsb.hike.models.ProfileItem;
import com.bsb.hike.models.utils.IconCacheManager;

public class ProfileActivity extends Activity implements OnItemClickListener, OnClickListener
{

	private ImageView mIconView;
	private TextView mNameView;
	private ListView mListView;
	private TextView mTitleView;
	private TextView mMadeWithLoveView;
	private ImageView mTitleIcon;
	private View mProfilePictureChangeOverlay;
	private boolean mEditable = false; /* is this page currently editable */
	private EditText mNameViewEdittable;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.profile);

		mIconView = (ImageView) findViewById(R.id.profile);
		mNameView = (TextView) findViewById(R.id.name);
		mNameViewEdittable = (EditText) findViewById(R.id.name_editable);
		mListView = (ListView) findViewById(R.id.profile_preferences);
		mTitleView = (TextView) findViewById(R.id.title);
		mTitleIcon = (ImageView) findViewById(R.id.title_icon);
		mMadeWithLoveView = (TextView) findViewById(R.id.made_with_love);
		mProfilePictureChangeOverlay = findViewById(R.id.profile_change_overlay);

		Drawable drawable = IconCacheManager.getInstance().getIconForMSISDN(HikeConstants.ME);
		mIconView.setImageDrawable(drawable);

		SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String name = settings.getString(HikeMessengerApp.NAME, "Set a name!");
		mNameView.setText(name);

		mNameView.setOnClickListener(this);

		mTitleView.setText(getResources().getString(R.string.profile_title));
		mTitleIcon.setImageResource(R.drawable.ic_editmessage);
		mTitleIcon.setVisibility(View.VISIBLE);

		/* add the heart in code because unicode isn't supported via xml*/
		//mMadeWithLoveView.setText(String.format(getString(R.string.made_with_love), "\u2665"));

		ProfileItem[] items = new ProfileItem[] 
			{
				new ProfileItem.ProfileSettingsItem("Credits", R.drawable.ic_credits, HikeMessengerApp.SMS_SETTING),
				new ProfileItem.ProfilePreferenceItem("Notifications", R.drawable.ic_notifications, R.xml.notification_preferences),
				new ProfileItem. ProfilePreferenceItem("Privacy", R.drawable.ic_privacy, R.xml.privacy_preferences),
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
		if (intent != null)
		{
			startActivity(intent);			
		}
	}

	public void onTitleIconClick(View view)
	{
		mEditable = !mEditable;
		if (mEditable)
		{
			mNameViewEdittable.setText(mNameView.getText());
		}
		else
		{
			/* save the new fields */
		}

		mProfilePictureChangeOverlay.setVisibility(mEditable ? View.VISIBLE : View.GONE);
		mNameViewEdittable.setVisibility(mEditable ? View.VISIBLE : View.GONE);
		mNameView.setVisibility(!mEditable ? View.VISIBLE : View.GONE);
	}

	@Override
	public void onClick(View view)
	{
		if (view == mNameView)
		{
			mNameView.setVisibility(View.GONE);
			EditText editableTextView = (EditText) findViewById(R.id.name_editable);
			editableTextView.setVisibility(View.VISIBLE);
			editableTextView.requestFocus();
		}
	}

}
