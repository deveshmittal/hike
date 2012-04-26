package com.bsb.hike.adapters;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
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
import com.bsb.hike.models.ConvMessage;

public class HikeInviteAdapter extends HikeArrayAdapter implements OnClickListener
{
	private Set<String> mInvitedUsers;

	public HikeInviteAdapter(Activity activity, int viewItemId)
	{
		super(activity, viewItemId, getItems(activity));
		this.activity = activity;
		mInvitedUsers = new HashSet<String>();
	}

	private static List<ContactInfo> getItems(Activity activity)
	{
		HikeUserDatabase db = new HikeUserDatabase(activity);
		List<ContactInfo> contacts = db.getNonHikeContacts();
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

		TextView invitedText = (TextView) v.findViewById(R.id.invited_text);

		/* hide the button if the person has been invited */
		button.setVisibility(mInvitedUsers.contains(contactInfo.getMsisdn()) ? View.INVISIBLE : View.VISIBLE);
		invitedText.setVisibility(mInvitedUsers.contains(contactInfo.getMsisdn()) ? View.VISIBLE : View.INVISIBLE);

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
		String msisdn = info.getMsisdn();
		if (info.isOnhike() || mInvitedUsers.contains(msisdn))
		{
			return;
		}

		mInvitedUsers.add(info.getMsisdn());
		long time = (long) System.currentTimeMillis() / 1000;
		ConvMessage convMessage = new ConvMessage(
				this.activity.getResources().getString(R.string.invite_message),
				msisdn, time, ConvMessage.State.SENT_UNCONFIRMED);
		convMessage.setInvite(true);
		HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, convMessage.serialize());
		this.notifyDataSetChanged();
	}

	@Override
	public String getTitle()
	{
		return activity.getResources().getString(R.string.invite);
	}

}
