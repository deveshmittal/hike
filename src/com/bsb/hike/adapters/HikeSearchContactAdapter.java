package com.bsb.hike.adapters;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.utils.IconCacheManager;

public class HikeSearchContactAdapter extends ArrayAdapter<ContactInfo> implements TextWatcher
{
	private static List<ContactInfo> getItems()
	{
		HikeUserDatabase db = HikeUserDatabase.getInstance();
		List<ContactInfo> contacts = db.getContactsOrderedByOnHike();
		return contacts;
	}

	private Context context;

	public HikeSearchContactAdapter(Activity context)
	{
		super(context, -1, getItems());
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ContactInfo contactInfo = (ContactInfo) getItem(position);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = convertView;
		if (v == null)
		{
			v = inflater.inflate(R.layout.name_item, parent, false);
		}

		v.setTag(contactInfo);

		TextView textView = (TextView) v.findViewById(R.id.name);
		textView.setText(contactInfo.getName());

		textView = (TextView) v.findViewById(R.id.number);
		textView.setText(contactInfo.getMsisdn());

		ImageView onhike = (ImageView) v.findViewById(R.id.onhike);
		onhike.setImageResource(contactInfo.isOnhike() ? R.drawable.ic_hike_user : R.drawable.ic_sms_user);

		ImageView avatar = (ImageView) v.findViewById(R.id.user_img);
		avatar.setImageDrawable(IconCacheManager.getInstance().getIconForMSISDN(contactInfo.getMsisdn()));

		return v;
	}

	@Override
	public void afterTextChanged(Editable editable)
	{
		((Filterable) this).getFilter().filter(editable.toString());
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
	{}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
	{}

	
}
