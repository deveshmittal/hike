package com.bsb.hike.adapters;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;

public class HikeBlockedUserAdapter extends HikeArrayAdapter implements OnClickListener
{

	private Set<String> blockedUsers;
	private Activity context;

	private static List<ContactInfo> getItems(Activity activity)
	{
		HikeUserDatabase db = new HikeUserDatabase(activity);
		List<ContactInfo> contacts = db.getContacts();
		db.close();
		Collections.sort(contacts);
		return contacts;
	}

	public HikeBlockedUserAdapter(Activity activity, int viewItemId)
	{
		super(activity, viewItemId, getItems(activity));
		this.context = activity;
		HikeUserDatabase db = new HikeUserDatabase(activity);
		this.blockedUsers = db.getBlockedUsers();
		db.close();
	}

	@Override
	protected View getItemView(int position, View convertView, ViewGroup parent)
	{
		ContactInfo contactInfo = (ContactInfo) getItem(position);
		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = convertView;
		if (v == null)
		{
			v = inflater.inflate(R.layout.contact_item, parent, false);
		}

		TextView textView = (TextView) v.findViewById(R.id.name);
		textView.setText(contactInfo.getName());

		Button button = (Button) v.findViewById(R.id.contact_button);
		button.setTag(contactInfo);
		button.setText(blockedUsers.contains(contactInfo.getMsisdn()) ? "Unblock" : "Block");
		button.setOnClickListener(this);

		boolean no_dividers = ((position == getCount() - 1) || (getItem(position + 1) instanceof Section));
		View divider = v.findViewById(R.id.item_divider);
		divider.setVisibility(no_dividers ? View.INVISIBLE : View.VISIBLE);

		return v;
	}

	@Override
	public void onClick(View view)
	{
		ContactInfo contactInfo = (ContactInfo) view.getTag();
		String msisdn = (String) contactInfo.getMsisdn();
		boolean block = !blockedUsers.contains(msisdn);
		boolean b = (block) ? blockedUsers.add(msisdn) : blockedUsers.remove(msisdn);
		this.notifyDataSetChanged();
		HikeMessengerApp.getPubSub().publish(block ? HikePubSub.BLOCK_USER : HikePubSub.UNBLOCK_USER, msisdn);
	}

	@Override
	public String getTitle()
	{
		return context.getResources().getString(R.string.block_users);
	}
}
