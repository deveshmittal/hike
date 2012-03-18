package com.bsb.hike.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.ProfileItem;

public class ProfileArrayAdapter extends ArrayAdapter<ProfileItem>
{
	static public class ProfileViewHolder
	{
		public ImageView iconView; /* Icon */
		public TextView titleView; /* Title */
		public View extraView; /* Any view on the right side */
		public TextView descriptionView; /* Secondary heading */
	}

	private final Context context;
	private LayoutInflater inflater;

	public ProfileArrayAdapter(Context context, int textViewResourceId, ProfileItem[] objects)
	{
		super(context, textViewResourceId, objects);
		this.context = context;
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ProfileItem profileItem = getItem(position);
		View v = null;
		if (v == null)
		{
			v = inflater.inflate(R.layout.profile_item, parent, false);
			profileItem.createViewHolder(v);
		}

		profileItem.bindView(this.context, v);
		return v;
	}
}