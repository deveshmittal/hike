package com.bsb.hike.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.utils.IconCacheManager;

public class ProfileActivity extends Activity
{

	private class ProfileItem
	{
		public ProfileItem(String title, int icon)
		{
			this.title = title;
			this.icon = icon;
		}
		String title;
		int icon;
	}

	private class ProfileArrayAdapter extends ArrayAdapter<ProfileItem>
	{

		private LayoutInflater inflater;

		public ProfileArrayAdapter(Context context, int textViewResourceId, ProfileItem[] objects)
		{
			super(context, textViewResourceId, objects);
			this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			ProfileItem profileItem = getItem(position);
			View v = convertView;
			if (v == null)
			{
				v = inflater.inflate(R.layout.profile_item, parent, false);
			}

			ImageView imageView = (ImageView) v.findViewById(R.id.profile_item_icon);
			TextView textView = (TextView) v.findViewById(R.id.profile_item_name);
			View extraView = v.findViewById(R.id.profile_item_extra);

			imageView.setImageDrawable(getResources().getDrawable(profileItem.icon));
			textView.setText(profileItem.title);
			if (profileItem.icon == R.drawable.ic_credits)
			{
				SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
				String credits = Integer.toString(settings.getInt(HikeMessengerApp.SMS_SETTING, 0));
				((TextView) extraView).setText(credits);
				extraView.setVisibility(View.VISIBLE);
			}
			return v;
		}
	}

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
				new ProfileItem("Credits", R.drawable.ic_credits),
				new ProfileItem("Notifications", R.drawable.ic_notifications),
				new ProfileItem("Privacy", R.drawable.ic_privacy),
				new ProfileItem("Help", R.drawable.ic_help)
			};
		ProfileArrayAdapter adapter = new ProfileArrayAdapter(this, R.layout.profile_item, items);
		mListView.setAdapter(adapter);
	}

}
