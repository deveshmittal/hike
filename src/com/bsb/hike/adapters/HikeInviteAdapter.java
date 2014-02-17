package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.smartImageLoader.IconLoader;

public class HikeInviteAdapter extends HikeArrayAdapter<Pair<AtomicBoolean, ContactInfo>> implements TextWatcher
{

	private List<Pair<AtomicBoolean, ContactInfo>> completeList;

	private List<Pair<AtomicBoolean, ContactInfo>> filteredList;

	private ContactFilter filter;

	private String filterString;

	private boolean showingBlockedList;

	private IconLoader iconLoader;

	private int mIconImageSize;

	public HikeInviteAdapter(Activity activity, int viewItemId, List<Pair<AtomicBoolean, ContactInfo>> completeList, boolean showingBLockedList)
	{

		super(activity, viewItemId, completeList);
		mIconImageSize = activity.getResources().getDimensionPixelSize(R.dimen.icon_picture_size);
		this.activity = activity;
		this.filteredList = completeList;
		this.completeList = new ArrayList<Pair<AtomicBoolean, ContactInfo>>(completeList.size());
		this.completeList.addAll(completeList);
		this.filter = new ContactFilter();
		this.showingBlockedList = showingBLockedList;
		iconLoader = new IconLoader(activity, mIconImageSize);
	}

	public void selectAllToggled()
	{
		filter.filter(filterString);
	}

	public List<Pair<AtomicBoolean, ContactInfo>> getCompleteList()
	{
		return completeList;
	}

	@Override
	protected View getItemView(int position, View convertView, ViewGroup parent)
	{
		Pair<AtomicBoolean, ContactInfo> pair = getItem(position);

		AtomicBoolean isChecked = null;
		ContactInfo contactInfo = null;
		if (pair != null)
		{
			isChecked = pair.first;
			contactInfo = pair.second;
		}
		else
		{
			contactInfo = new ContactInfo(filterString, filterString, filterString, filterString);
		}

		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = convertView;
		if (v == null)
		{
			v = inflater.inflate(R.layout.compose_list_item, parent, false);
		}
		ImageView imageView = (ImageView) v.findViewById(R.id.contact_image);
		if (pair != null)
		{
			iconLoader.loadImage(contactInfo.getMsisdn(), true, imageView, true);
		}
		else
			imageView.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_avatar1_rounded));

		TextView textView = (TextView) v.findViewById(R.id.name);
		textView.setText(contactInfo.getName());

		TextView numView = (TextView) v.findViewById(R.id.number);
		if (pair != null)
		{
			numView.setText(contactInfo.getMsisdn());
			if (!TextUtils.isEmpty(contactInfo.getMsisdnType()))
			{
				numView.append(" (" + contactInfo.getMsisdnType() + ")");
			}
		}
		else
		{
			numView.setText(showingBlockedList ? R.string.tap_here_block : R.string.tap_here_invite);
		}
		numView.setVisibility(isEnabled(position) ? View.VISIBLE : View.INVISIBLE);

		CheckBox checkBox = (CheckBox) v.findViewById(R.id.checkbox);
		checkBox.setVisibility(pair != null ? View.VISIBLE : View.GONE);
		checkBox.setButtonDrawable(showingBlockedList ? R.drawable.block_button : R.drawable.compose_checkbox);

		if (pair != null)
		{
			checkBox.setChecked(isChecked.get());
			v.setTag(pair);
		}
		else
		{
			v.setTag(contactInfo);
		}
		return v;
	}

	@Override
	public String getTitle()
	{
		return activity.getResources().getString(R.string.invite);
	}

	@Override
	public void afterTextChanged(Editable s)
	{
		filter.filter(s);
		filterString = s.toString();
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after)
	{
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
	}

	private class ContactFilter extends Filter
	{
		@Override
		protected FilterResults performFiltering(CharSequence constraint)
		{
			FilterResults results = new FilterResults();

			String textToBeFiltered = TextUtils.isEmpty(constraint) ? "" : constraint.toString().toLowerCase();

			if (!TextUtils.isEmpty(textToBeFiltered))
			{

				List<Pair<AtomicBoolean, ContactInfo>> filteredContacts = new ArrayList<Pair<AtomicBoolean, ContactInfo>>();

				for (Pair<AtomicBoolean, ContactInfo> info : HikeInviteAdapter.this.completeList)
				{
					if (info != null)
					{
						ContactInfo contactInfo = info.second;
						if (contactInfo.getName().toLowerCase().contains(textToBeFiltered) || contactInfo.getMsisdn().contains(textToBeFiltered))
						{
							filteredContacts.add(info);
						}
					}
				}
				if (shouldShowExtraElement(textToBeFiltered))
				{
					filteredContacts.add(null);
				}
				results.count = filteredContacts.size();
				results.values = filteredContacts;

			}
			else
			{
				results.count = HikeInviteAdapter.this.completeList.size();
				results.values = HikeInviteAdapter.this.completeList;
			}
			return results;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results)
		{
			filteredList = (ArrayList<Pair<AtomicBoolean, ContactInfo>>) results.values;
			notifyDataSetChanged();
			clear();
			for (Pair<AtomicBoolean, ContactInfo> pair : filteredList)
			{
				add(pair);
			}
			notifyDataSetInvalidated();
		}
	}

	private boolean shouldShowExtraElement(String s)
	{
		String pattern = "(\\+?\\d*)";
		if (s.matches(pattern))
		{
			return true;
		}
		return false;
	}

	@Override
	public boolean areAllItemsEnabled()
	{
		return false;
	}

	@Override
	public boolean isEnabled(int position)
	{
		if (filteredList.get(position) == null)
		{
			return filterString.matches(HikeConstants.VALID_MSISDN_REGEX);
		}
		return super.isEnabled(position);
	}
}
