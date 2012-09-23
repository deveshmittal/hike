package com.bsb.hike.models;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.ui.HikePreferences;
import com.bsb.hike.ui.WebViewActivity;

public abstract class ProfileItem
{
	/**
	 * Profile item that (when selected) goes to a link
	 * @author vr
	 *
	 */
	static public class ProfileLinkItem extends ProfileItem
	{

		private String url;

		public ProfileLinkItem(String title, int icon, String url)
		{
			super(title, icon);
			this.url = url;
		}

		@Override
		public Intent getIntent(Context context)
		{
			Intent intent = new Intent(context, WebViewActivity.class);
			intent.putExtra(HikeConstants.Extras.URL_TO_LOAD, this.url);
			intent.putExtra(HikeConstants.Extras.TITLE, "Help");
			return intent;
		}

		@Override
		public void bindView(Context context, View v)
		{
			super.bindView(context, v);
			ProfileViewHolder holder = (ProfileViewHolder) v.getTag(R.id.profile_item_extra);
			holder.extraView.setBackgroundResource(R.drawable.ic_arrow);
			holder.extraView.setVisibility(View.VISIBLE);
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
			intent.putExtra(HikeConstants.Extras.PREF, preference);
			return intent;
		}

		@Override
		public void bindView(Context context, View v)
		{
			super.bindView(context, v);
			ProfileViewHolder holder = (ProfileViewHolder) v.getTag(R.id.profile_item_extra);
			holder.extraView.setBackgroundResource(R.drawable.ic_arrow);
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
		ProfileViewHolder holder = (ProfileViewHolder) v.getTag(R.id.profile_item_extra);
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

	public ProfileViewHolder createViewHolder(View v, ProfileItem p)
	{
		ProfileViewHolder holder = new ProfileViewHolder();
		v.setTag(R.id.profile_item_extra, holder);
		v.setTag(R.id.profile, p);
		holder.iconView = (ImageView) v.findViewById(R.id.profile_item_icon);
		holder.titleView = (TextView) v.findViewById(R.id.profile_item_name);
		
		holder.extraView = (ImageView) v.findViewById(R.id.arrow);
	
		return holder;
	}
	
	static public class ProfileViewHolder
	{
		public ImageView iconView; /* Icon */
		public TextView titleView; /* Title */
		public View extraView; /* Any view on the right side */
		public TextView descriptionView; /* Secondary heading */
	}
}