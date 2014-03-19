package com.bsb.hike.adapters;

import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.AccountData;

/**
 * Custom adapter used to display account icons and descriptions in the account spinner.
 */
public class AccountAdapter extends ArrayAdapter<AccountData>
{

	LayoutInflater layoutInflater;

	public AccountAdapter(Context context, List<AccountData> accountData)
	{
		super(context, android.R.layout.simple_spinner_item, accountData);
		setDropDownViewResource(R.layout.account_entry);
		layoutInflater = LayoutInflater.from(context);
	}

	public View getDropDownView(int position, View convertView, ViewGroup parent)
	{
		// Inflate a view template
		if (convertView == null)
		{
			convertView = layoutInflater.inflate(R.layout.account_entry, parent, false);
		}
		TextView firstAccountLine = (TextView) convertView.findViewById(R.id.firstAccountLine);
		TextView secondAccountLine = (TextView) convertView.findViewById(R.id.secondAccountLine);
		ImageView accountIcon = (ImageView) convertView.findViewById(R.id.accountIcon);

		// Populate template
		AccountData data = getItem(position);
		firstAccountLine.setText(data.getName());
		secondAccountLine.setText(data.getTypeLabel());
		Drawable icon = data.getIcon();
		if (icon == null)
		{
			icon = getContext().getResources().getDrawable(android.R.drawable.ic_menu_search);
		}
		accountIcon.setImageDrawable(icon);
		return convertView;
	}
}