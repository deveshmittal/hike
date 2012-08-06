package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Gravity;
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
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
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
	private String groupId;
	private int numContactsSelected = 0;
	private int numSMSContactsSelected = 0;

	public HikeSearchContactAdapter(
			Activity context, List<ContactInfo> contactList, 
			EditText inputNumber, boolean isGroupChat, boolean hasPrefillText, 
			Button topBarBtn, String groupId)
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
		this.groupId = groupId;
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
		textView.setText(contactInfo != null ? contactInfo.getName() : inputNumber.getText());

		TextView numberTextView = (TextView) v.findViewById(R.id.number);
		numberTextView.setText(contactInfo != null ? contactInfo.getMsisdn() : context.getString(R.string.tap_to_message));
		if(contactInfo != null && !TextUtils.isEmpty(contactInfo.getMsisdnType()))
		{
			textView.append(" (" + contactInfo.getMsisdnType() + ")");
		}

		ImageView onhike = (ImageView) v.findViewById(R.id.onhike);
		onhike.setImageResource(contactInfo != null ? (contactInfo.isOnhike() ? R.drawable.ic_hike_user : R.drawable.ic_sms_user) : 0);

		ImageView avatar = (ImageView) v.findViewById(R.id.user_img);
		avatar.setImageDrawable(contactInfo != null ? IconCacheManager.getInstance().getIconForMSISDN(contactInfo.getMsisdn()) : context.getResources().getDrawable(R.drawable.ic_avatar0));

		if(contactInfo == null)
		{
			numberTextView.setVisibility(inputNumber.getText().toString().matches(HikeConstants.VALID_MSISDN_REGEX) ? View.VISIBLE: View.INVISIBLE);
			onhike.setVisibility(View.GONE);
		}
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

			if(!TextUtils.isEmpty(textToBeFiltered) || !TextUtils.isEmpty(textInEditText) || !TextUtils.isEmpty(groupId))
			{
				final List<String> currentSelectionsInTextBox = Utils.splitSelectedContacts(textInEditText);
				final Set<String> currentSelectionSet = new HashSet<String>();
				/*
				 * Making a set of all the currently selected contacts. Only used in case of group chat.
				 */
				currentSelectionSet.addAll(currentSelectionsInTextBox);
				if(!TextUtils.isEmpty(groupId))
				{
					currentSelectionSet.addAll(HikeConversationsDatabase.getInstance().getGroupParticipants(groupId, true).keySet());
				}

				List<ContactInfo> filteredContacts = new ArrayList<ContactInfo>();

				if (topBarBtn != null) 
				{
					((Activity) context).runOnUiThread(new Runnable() 
					{
						@Override
						public void run() 
						{
							/*
							 *  We only enable this if the user has selected more than one participant OR if we are adding participants in an
							 *  existing group the user should have selected at least one participant 
							 */
							topBarBtn.setEnabled((currentSelectionSet.size() > 1 && currentSelectionsInTextBox.size() > 0) || (currentSelectionsInTextBox.size() > 1));
						}
					});
				}
				Set<String> selectedSMSContacts = new HashSet<String>();
				for (ContactInfo info : HikeSearchContactAdapter.this.completeList)
				{
					if(info != null)
					{
						if(!currentSelectionSet.isEmpty())
						{
							if(currentSelectionSet.contains(info.getMsisdn()))
							{
								if(!info.isOnhike())
								{
									selectedSMSContacts.add(info.getMsisdn());
								}
								continue;
							}
						}
						if(info.getName().toLowerCase().contains(textToBeFiltered) || info.getMsisdn().contains(textToBeFiltered))
						{
							filteredContacts.add(info);
						}
					}
				}
				if(!isGroupChat && shouldShowExtraElement(textToBeFiltered))
				{
					filteredContacts.add(null);
				}
				results.count = filteredContacts.size();
				results.values = filteredContacts;

				numContactsSelected = currentSelectionSet.size();
				numSMSContactsSelected = selectedSMSContacts.size();
			}
			else
			{
				results.count = HikeSearchContactAdapter.this.completeList.size();
				results.values = HikeSearchContactAdapter.this.completeList;

				numContactsSelected = 0;
				numSMSContactsSelected = 0;
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
		ContactInfo contactInfo = (ContactInfo) view.getTag();
		if(contactInfo == null)
		{
			String number = normalizeNumber(inputNumber.getText().toString());
			if(!number.matches(HikeConstants.VALID_MSISDN_REGEX))
			{
				return;
			}
			Log.d(getClass().getSimpleName(), "Formatted number: " + number);
			contactInfo = new ContactInfo(number, number, number, number);
		}
		if (!isGroupChat) 
		{
			Intent intent = Utils.createIntentFromContactInfo(contactInfo);
			intent.setClass(context, ChatThread.class);
			intent.putExtra(HikeConstants.Extras.KEEP_MESSAGE, hasPrefillText);
			context.startActivity(intent);
		}
		else
		{
			inputNumber.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
			/*
			 * Checking if the number of participants has crossed the set limit.
			 * We have two limits - SMS contacts and total contacts.
			 */
			if (numContactsSelected >= HikeConstants.MAX_CONTACTS_IN_GROUP || 
					(numSMSContactsSelected >= HikeConstants.MAX_SMS_CONTACTS_IN_GROUP && !contactInfo.isOnhike()))
			{
				Toast toast = Toast.makeText(
						getContext(), 
						((numContactsSelected < HikeConstants.MAX_CONTACTS_IN_GROUP) ? R.string.max_sms : R.string.max_contact), 
						Toast.LENGTH_SHORT);
				toast.setGravity(Gravity.TOP, 0, (int) (50 * Utils.densityMultiplier));
				toast.show();
				return;
			}
			String currentText = inputNumber.getText().toString();
			int insertIndex = currentText.contains(HikeConstants.GROUP_PARTICIPANT_SEPARATOR) ? currentText.lastIndexOf(HikeConstants.GROUP_PARTICIPANT_SEPARATOR) + 2 : 0;

			String textToBeShown = contactInfo.getName() + "[" + contactInfo.getMsisdn() + "]" + HikeConstants.GROUP_PARTICIPANT_SEPARATOR;
			String nameToBeShown = Utils.ellipsizeName(contactInfo.getName());

			MyDrawable myDrawable = new MyDrawable(nameToBeShown, context, contactInfo.isOnhike());
			myDrawable.setBounds(
					(int) (0 * Utils.densityMultiplier), 
					(int) (0 * Utils.densityMultiplier), 
					(int) (myDrawable.getPaint().measureText(nameToBeShown) + ((int)17 * Utils.densityMultiplier)),
					(int) (27 * Utils.densityMultiplier));

			ImageSpan imageSpan = new ImageSpan(myDrawable);

			Editable editable = inputNumber.getText();
			editable.replace(insertIndex, currentText.length(), textToBeShown);
			editable.setSpan(
					imageSpan,
					insertIndex, 
					insertIndex + textToBeShown.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			inputNumber.setSelection(inputNumber.length());
		}
	}
	
	private boolean shouldShowExtraElement(String s)
	{
	    String pattern= "(\\+?\\d*)";
	        if(s.matches(pattern)){
	            return true;
	        }
	        return false;   
	}

	private String normalizeNumber(String inputNumber)
	{
		return inputNumber.startsWith("+") ? inputNumber : inputNumber.startsWith("0") ? inputNumber.replaceFirst("0", "+91") : ("+91" + inputNumber);
	}
}