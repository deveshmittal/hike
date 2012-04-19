package com.bsb.hike.adapters;

import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.utils.Utils;

public class HikeSearchContactAdapter extends ArrayAdapter<ContactInfo> implements OnClickListener, TextWatcher
{
	private static List<ContactInfo> getItems(Activity activity)
	{
		HikeUserDatabase db = new HikeUserDatabase(activity);
		List<ContactInfo> contacts = db.getContacts();
		db.close();
		Collections.sort(contacts);
		return contacts;
	}

	private Context context;

	public HikeSearchContactAdapter(Activity context)
	{
		super(context, -1, getItems(context));
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ContactInfo contactInfo = (ContactInfo) getItem(position);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = null;
		if (v == null)
		{
			v = inflater.inflate(R.layout.name_item, parent, false);
		}

		v.setTag(contactInfo);

		TextView textView = (TextView) v.findViewById(R.id.name);
		textView.setText(contactInfo.getName());

		textView = (TextView) v.findViewById(R.id.number);
		textView.setText(contactInfo.getMsisdn());

		View onhike = v.findViewById(R.id.onhike);
		onhike.setVisibility(contactInfo.isOnhike() ? View.VISIBLE : View.INVISIBLE);

		return v;
	}

	@Override
	public void onClick(View view)
	{
		ContactInfo contactInfo = (ContactInfo) view.getTag();
		Intent intent = Utils.createIntentFromContactInfo(contactInfo);
		intent.setClass(this.context, ChatThread.class);
		this.context.startActivity(intent);
	}

	@Override
	public void afterTextChanged(Editable editable)
	{
		((Filterable) this).getFilter().filter(editable.toString());
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
	{
		// TODO Auto-generated method stub
		
	}
}
