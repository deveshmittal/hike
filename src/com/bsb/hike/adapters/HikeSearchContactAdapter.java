package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.utils.MyDrawable;
import com.bsb.hike.utils.Utils;

@SuppressWarnings("unchecked")
public class HikeSearchContactAdapter extends ArrayAdapter<ContactInfo> implements TextWatcher, OnItemClickListener
{
	private Context context;
	private List<ContactInfo> filteredList;
	private List<ContactInfo> completeList;
	private ContactFilter contactFilter;
	private boolean isGroupChat;
	private EditText inputNumber;
	private boolean hasPrefillText;
	private Button topBarBtn;

	public HikeSearchContactAdapter(Activity context, List<ContactInfo> contactList, EditText inputNumber, boolean isGroupChat, boolean hasPrefillText, Button topBarBtn)
	{
		super(context, -1, contactList);
		this.filteredList = contactList;
		this.completeList = new ArrayList<ContactInfo>();
		this.completeList.addAll(contactList);
		this.context = context;
		this.contactFilter = new ContactFilter();
		this.inputNumber = inputNumber;
		this.isGroupChat = isGroupChat;
		this.hasPrefillText = hasPrefillText;
		this.topBarBtn = topBarBtn;
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

			String textInEditText = TextUtils.isEmpty(constraint) ? "" : constraint.toString().toLowerCase();
			int indexTextToBeFiltered = textInEditText.lastIndexOf(HikeConstants.GROUP_PARTICIPANT_SEPARATOR) + 2;
			String textToBeFiltered = (!textInEditText.contains(HikeConstants.GROUP_PARTICIPANT_SEPARATOR) ?
							textInEditText : textInEditText.substring(indexTextToBeFiltered));

			if(!TextUtils.isEmpty(textToBeFiltered) || !TextUtils.isEmpty(textInEditText))
			{
				List<String> numbersSelected = Utils.splitSelectedContacts(textInEditText);
				List<ContactInfo> filteredContacts = new ArrayList<ContactInfo>();

				for (ContactInfo info : HikeSearchContactAdapter.this.completeList)
				{
					if(!numbersSelected.isEmpty())
					{
						boolean alreadySelected = false;
						for(String number: numbersSelected)
						{
							if(number.equals(info.getMsisdn()))
							{
								alreadySelected = true;
								break;
							}
						}
						if(alreadySelected)
						{
							continue;
						}
					}
					if(info.getName().toLowerCase().contains(textToBeFiltered) || info.getMsisdn().contains(textToBeFiltered))
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

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) 
	{
		if (!isGroupChat) 
		{
			ContactInfo contactInfo = (ContactInfo) view.getTag();
			Intent intent = Utils.createIntentFromContactInfo(contactInfo);
			intent.setClass(context, ChatThread.class);
			intent.putExtra(HikeConstants.Extras.KEEP_MESSAGE, hasPrefillText);
			context.startActivity(intent);
		}
		else
		{
			ContactInfo contactInfo = (ContactInfo) view.getTag();
			String currentText = inputNumber.getText().toString();
			int insertIndex = currentText.contains(HikeConstants.GROUP_PARTICIPANT_SEPARATOR) ? currentText.lastIndexOf(HikeConstants.GROUP_PARTICIPANT_SEPARATOR) + 2 : 0;

			String textToBeShown = contactInfo.getName() + "[" + contactInfo.getMsisdn() + "]" + HikeConstants.GROUP_PARTICIPANT_SEPARATOR;

			MyDrawable myDrawable = new MyDrawable(contactInfo.getName(), context, contactInfo.isOnhike());
			myDrawable.setBounds(
					(int) (0 * Utils.densityMultiplier), 
					(int) (0 * Utils.densityMultiplier), 
					(int) (myDrawable.getPaint().measureText(contactInfo.getName()) + ((int)17 * Utils.densityMultiplier)),
					(int) (27 * Utils.densityMultiplier));

			ImageSpan imageSpan = new ImageSpan(myDrawable);

			Editable editable = inputNumber.getText();
			editable.replace(insertIndex, currentText.length(), textToBeShown);
			editable.setSpan(
					imageSpan,
					insertIndex, 
					insertIndex + textToBeShown.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			topBarBtn.setEnabled(editable.toString().contains(", "));
		}
	}
}