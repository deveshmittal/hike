package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.utils.IconCacheManager;

@SuppressWarnings("unchecked")
public class HikeSearchContactAdapter extends ArrayAdapter<ContactInfo> implements TextWatcher
{
	private Context context;
	private List<ContactInfo> filteredList;
	private List<ContactInfo> completeList;
	private ContactFilter contactFilter;

	public HikeSearchContactAdapter(Activity context, List<ContactInfo> contactList)
	{
		super(context, -1, contactList);
		this.filteredList = contactList;
		this.completeList = new ArrayList<ContactInfo>();
		this.completeList.addAll(contactList);
		this.context = context;
		this.contactFilter = new ContactFilter();
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
		this.contactFilter.filter(editable.toString());
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
	{}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
	{}

	private class ContactFilter extends Filter
	{

		@Override
		protected FilterResults performFiltering(CharSequence constraint) 
		{
			FilterResults results = new FilterResults();
			if(!TextUtils.isEmpty(constraint))
			{
				constraint = constraint.toString().toLowerCase();
				List<ContactInfo> filteredContacts = new ArrayList<ContactInfo>();

				for (ContactInfo info : HikeSearchContactAdapter.this.completeList)
				{
					if(info.getName().toLowerCase().contains(constraint) || info.getMsisdn().contains(constraint))
					{
						filteredContacts.add(info);
					}
				}
				results.count = filteredContacts.size();
				results.values = filteredContacts;
			}
			else
			{
				results.count = HikeSearchContactAdapter.this.completeList.size();
				results.values = HikeSearchContactAdapter.this.completeList;
			}
			return results;
		}

		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) 
		{
			filteredList = (ArrayList<ContactInfo>) results.values;
			notifyDataSetChanged();
			clear();
			for(ContactInfo contactInfo : filteredList)
			{
				add(contactInfo);
			}
			notifyDataSetInvalidated();
		}
		
	}
}
