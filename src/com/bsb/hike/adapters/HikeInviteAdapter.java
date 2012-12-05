package com.bsb.hike.adapters;

import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;

public class HikeInviteAdapter extends HikeArrayAdapter {
	SparseBooleanArray checkedItems;

	public HikeInviteAdapter(Activity activity, int viewItemId,
			SparseBooleanArray checkedItems) {
		super(activity, viewItemId, getItems(activity));
		this.activity = activity;
		this.checkedItems = checkedItems;
	}

	private static List<ContactInfo> getItems(Activity activity) {
		HikeUserDatabase db = HikeUserDatabase.getInstance();
		List<ContactInfo> contacts = db.getNonHikeContacts();
		Collections.sort(contacts);
		return contacts;
	}

	@Override
	protected View getItemView(int position, View convertView, ViewGroup parent) {
		ContactInfo contactInfo = (ContactInfo) getItem(position);
		LayoutInflater inflater = (LayoutInflater) activity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = convertView;
		if (v == null) {
			v = inflater.inflate(R.layout.invite_item, parent, false);
		}

		TextView textView = (TextView) v.findViewById(R.id.name);
		textView.setText(contactInfo.getName());

		TextView numView = (TextView) v.findViewById(R.id.number);
		numView.setText(contactInfo.getMsisdn());
		if (!TextUtils.isEmpty(contactInfo.getMsisdnType())) {
			numView.append(" (" + contactInfo.getMsisdnType() + ")");
		}

		((CheckBox) v.findViewById(R.id.checkbox)).setChecked(checkedItems
				.get(position));
		v.setTag(contactInfo);
		return v;
	}

	@Override
	public String getTitle() {
		return activity.getResources().getString(R.string.invite);
	}

}
