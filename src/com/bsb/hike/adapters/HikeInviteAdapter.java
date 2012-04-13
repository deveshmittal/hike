package com.bsb.hike.adapters;

import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.utils.Utils;

public class HikeInviteAdapter extends HikeArrayAdapter implements OnClickListener
{

	public HikeInviteAdapter(Activity activity, int viewItemId)
	{
		super(activity, viewItemId, getItems(activity));
		this.activity = activity;
	}

	private static List<ContactInfo> getItems(Activity activity)
	{
		HikeUserDatabase db = new HikeUserDatabase(activity);
		List<ContactInfo> contacts = db.getContacts();
		db.close();
		Collections.sort(contacts);
		return contacts;
	}

	@Override
	protected View getItemView(int position, View convertView, ViewGroup parent)
	{
		ContactInfo contactInfo = (ContactInfo) getItem(position);
		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = null;
		if (v == null)
		{
			v = inflater.inflate(R.layout.invite_item, parent, false);
		}

		TextView textView = (TextView) v.findViewById(R.id.name);
		textView.setText(contactInfo.getName());
		
		TextView numView = (TextView) v.findViewById(R.id.number);
		numView.setText(contactInfo.getPhoneNum());
		
		Button button = (Button) v.findViewById(R.id.contact_button);
		v.setTag(contactInfo);
		if (contactInfo.isOnhike())
		{
			button.setEnabled(false);
			button.setBackgroundResource(R.drawable.ic_contact_logo);
		}

		v.setOnClickListener(this);

		boolean no_dividers = ((position == getCount() - 1) || (getItem(position + 1) instanceof Section));
		View divider = v.findViewById(R.id.item_divider);
		divider.setVisibility(no_dividers ? View.INVISIBLE : View.VISIBLE);

		return v;
	}

	@Override
	public void onClick(View view)
	{
		ContactInfo info = (ContactInfo) view.getTag();
		Intent intent = Utils.createIntentFromContactInfo(info);

		if (!info.isOnhike())
		{
			intent.putExtra(HikeConstants.Extras.INVITE, true);
		}

		activity.setResult(Activity.RESULT_OK, intent);
		activity.finish();
	}

	@Override
	public String getTitle()
	{
		return activity.getResources().getString(R.string.invite);
	}

}
