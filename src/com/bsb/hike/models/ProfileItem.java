package com.bsb.hike.models;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.ProfileArrayAdapter.ProfileViewHolder;
import com.bsb.hike.ui.HikePreferences;

public abstract class ProfileItem
{
	/**
	 * Profile item that (when selected) goes to a link
	 * @author vr
	 *
	 */
	static public class ProfileLinkItem extends ProfileItem
	{

		private Uri url;

		public ProfileLinkItem(String title, int icon, String url)
		{
			super(title, icon);
			this.url = Uri.parse(url);
		}

		@Override
		public Intent getIntent(Context context)
		{
			return null;
//			return new Intent(Intent.ACTION_VIEW, this.url);
		}
	}

	/**
	 * Profile item that (when selected) launches the preference activity
	 * @author vr
	 *
	 */
	static public class ProfilePreferenceItem extends ProfileItem
	{
		private int preference;
		public ProfilePreferenceItem(String title, int icon, int preference)
		{
			super(title, icon);
			this.preference = preference;
		}

		@Override
		public Intent getIntent(Context ctx)
		{
			Intent intent = new Intent(ctx, HikePreferences.class);
			intent.putExtra("pref", preference);
			return intent;
		}

	}
	/**
	 * A profile item that displays the contents of a setting
	 * @author vr
	 *
	 */
	static public class ProfileSettingsItem extends ProfileItem
	{
		private String settingsName;

		public ProfileSettingsItem(String title, int icon, String settingsName)
		{
			super(title, icon);
			this.settingsName = settingsName;
		}

		@Override
		public Intent getIntent(Context context)
		{
			return null;
/*			Uri uri = Uri.parse("http://www.bsb.com/hike-help/" + settingsName);
			return new Intent(Intent.ACTION_VIEW, uri);
*/
		}

		@Override
		public void bindView(Context context, View v)
		{
			super.bindView(context, v);
			ProfileViewHolder holder = (ProfileViewHolder) v.getTag();
			SharedPreferences settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
			String credits = Integer.toString(settings.getInt(this.settingsName, 0));
			((TextView) holder.extraView).setText(credits);
			holder.extraView.setVisibility(View.VISIBLE);
		}		
	}

	String title;
	int icon;

	public ProfileItem(String title, int icon)
	{
		this.title = title;
		this.icon = icon;
	}

	/**
	 * The intent to launch when this item is selected.
	 * Return null if you don't want this item to be selectable
	 * @return Intent to launch when this item is selected (or null)
	 */
	public abstract Intent getIntent(Context context);

	public void bindView(Context context, View v)
	{
		ProfileViewHolder holder = (ProfileViewHolder) v.getTag();
		holder.iconView.setImageDrawable(context.getResources().getDrawable(getIcon()));
		holder.titleView.setText(getTitle());		
	}
	
	public String getTitle()
	{
		return title;
	}

	public int getIcon()
	{
		return icon;
	}

	public ProfileViewHolder createViewHolder(View v)
	{
		ProfileViewHolder holder = new ProfileViewHolder();
		v.setTag(holder);
		holder.iconView = (ImageView) v.findViewById(R.id.profile_item_icon);
		holder.titleView = (TextView) v.findViewById(R.id.profile_item_name);
		holder.extraView = (TextView) v.findViewById(R.id.profile_item_extra);
		return holder;
	}
}