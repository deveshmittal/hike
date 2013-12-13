package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.utils.IconCacheManager;

@SuppressWarnings("unchecked")
public class HikeSearchContactAdapter extends
		ArrayAdapter<Pair<AtomicBoolean, ContactInfo>> implements TextWatcher {
	private Context context;
	private List<Pair<AtomicBoolean, ContactInfo>> filteredList;
	private List<Pair<AtomicBoolean, ContactInfo>> completeList;
	private ContactFilter contactFilter;
	private EditText inputNumber;
	private String groupId;
	private Map<String, GroupParticipant> groupParticipants;
	private boolean forwarding;

	public HikeSearchContactAdapter(Activity context,
			List<Pair<AtomicBoolean, ContactInfo>> contactList,
			EditText inputNumber, String groupId, boolean forwarding,
			Map<String, GroupParticipant> groupParticipants) {
		super(context, -1, contactList);
		this.filteredList = contactList;
		this.completeList = new ArrayList<Pair<AtomicBoolean, ContactInfo>>();
		this.completeList.addAll(contactList);
		this.context = context;
		this.contactFilter = new ContactFilter();
		this.inputNumber = inputNumber;
		this.groupId = groupId;
		this.forwarding = forwarding;
		this.groupParticipants = groupParticipants;
	}

	Comparator<Pair<AtomicBoolean, ContactInfo>> comparator = new Comparator<Pair<AtomicBoolean, ContactInfo>>() {

		@Override
		public int compare(Pair<AtomicBoolean, ContactInfo> lhs,
				Pair<AtomicBoolean, ContactInfo> rhs) {
			AtomicBoolean lhsIsCheck = lhs.first;
			AtomicBoolean rhsIsCheck = rhs.first;

			if (lhsIsCheck.get() != rhsIsCheck.get()) {
				if (lhsIsCheck.get()) {
					return -1;
				} else {
					return 1;
				}
			}

			if (lhs.second == null) {
				return 1;
			}
			return lhs.second.compareTo(rhs.second);
		}
	};

	public void sort() {
		Collections.sort(completeList, comparator);
		Collections.sort(filteredList, comparator);
	}

	public void addItemToCompleteList(Pair<AtomicBoolean, ContactInfo> pair) {
		completeList.add(0, pair);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Pair<AtomicBoolean, ContactInfo> item = (Pair<AtomicBoolean, ContactInfo>) getItem(position);
		ContactInfo contactInfo = item.second;
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = convertView;
		if (v == null) {
			v = inflater.inflate(R.layout.compose_list_item, parent, false);
		}

		v.setTag(item);

		TextView textView = (TextView) v.findViewById(R.id.name);
		textView.setText(contactInfo != null ? contactInfo.getName()
				: getNumber(inputNumber.getText().toString()));

		TextView numberTextView = (TextView) v.findViewById(R.id.number);
		numberTextView.setText(contactInfo != null ? contactInfo.getMsisdn()
				: context.getString(R.string.tap_to_add));

		CheckBox checkBox = (CheckBox) v.findViewById(R.id.checkbox);
		checkBox.setVisibility(forwarding ? View.GONE : View.VISIBLE);
		checkBox.setChecked(item.first.get());

		if (contactInfo != null
				&& !TextUtils.isEmpty(contactInfo.getMsisdnType())) {
			numberTextView.append(" (" + contactInfo.getMsisdnType() + ")");
		}

		ImageView onhike = (ImageView) v.findViewById(R.id.hike_status);
		onhike.setVisibility(View.VISIBLE);
		onhike.setImageResource(contactInfo != null ? (contactInfo.isOnhike() ? R.drawable.ic_hike_user
				: R.drawable.ic_sms_user)
				: 0);

		ImageView avatar = (ImageView) v.findViewById(R.id.contact_image);
		if (contactInfo != null
				&& contactInfo.getId().equals(contactInfo.getPhoneNum())) {
			avatar.setImageDrawable(IconCacheManager.getInstance()
					.getIconForMSISDN(contactInfo.getPhoneNum(), true));
		} else {
			avatar.setImageDrawable(contactInfo != null ? IconCacheManager
					.getInstance().getIconForMSISDN(contactInfo.getMsisdn(),
							true) : context.getResources().getDrawable(
					R.drawable.ic_avatar1_rounded));
		}

		numberTextView.setVisibility(isEnabled(position) ? View.VISIBLE
				: View.INVISIBLE);
		return v;
	}

	@Override
	public void afterTextChanged(Editable editable) {
		this.contactFilter.filter(editable.toString());
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
			int arg3) {
	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
	}

	private class ContactFilter extends Filter {
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();

			String textToBeFiltered = TextUtils.isEmpty(constraint) ? ""
					: constraint.toString().toLowerCase();

			if (!TextUtils.isEmpty(textToBeFiltered)
					|| !TextUtils.isEmpty(groupId)) {
				final Set<String> currentSelectionSet = new HashSet<String>();
				Set<String> selectedSMSContacts = new HashSet<String>();
				if (!TextUtils.isEmpty(groupId)) {
					currentSelectionSet.addAll(groupParticipants.keySet());
					// Done to add unknown numbers to the already selected list
					for (Entry<String, GroupParticipant> participantEntry : groupParticipants
							.entrySet()) {
						if (!participantEntry.getValue().getContactInfo()
								.isOnhike()) {
							selectedSMSContacts.add(participantEntry.getKey());
						}
					}
				}

				List<Pair<AtomicBoolean, ContactInfo>> filteredContacts = new ArrayList<Pair<AtomicBoolean, ContactInfo>>();

				for (Pair<AtomicBoolean, ContactInfo> item : HikeSearchContactAdapter.this.completeList) {
					ContactInfo info = item.second;
					if (info != null) {
						if (!currentSelectionSet.isEmpty()) {
							if (currentSelectionSet.contains(info.getMsisdn())) {
								if (!info.isOnhike()) {
									selectedSMSContacts.add(info.getMsisdn());
								}
								continue;
							}
						}
						if (info.getName().toLowerCase()
								.contains(textToBeFiltered)
								|| info.getMsisdn().contains(textToBeFiltered)) {
							filteredContacts.add(item);
						}
					}
				}
				if (shouldShowExtraElement(textToBeFiltered)) {
					if (currentSelectionSet.contains(textToBeFiltered)) {
						filteredContacts
								.add(new Pair<AtomicBoolean, ContactInfo>(
										new AtomicBoolean(true), null));
					} else {
						filteredContacts
								.add(new Pair<AtomicBoolean, ContactInfo>(
										new AtomicBoolean(), null));
					}
				}
				results.count = filteredContacts.size();
				results.values = filteredContacts;
			} else {
				results.count = HikeSearchContactAdapter.this.completeList
						.size();
				results.values = HikeSearchContactAdapter.this.completeList;
			}
			return results;
		}

		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results) {
			filteredList = (ArrayList<Pair<AtomicBoolean, ContactInfo>>) results.values;
			notifyDataSetChanged();
			clear();
			for (Pair<AtomicBoolean, ContactInfo> item : filteredList) {
				add(item);
			}
			notifyDataSetInvalidated();
		}
	}

	private boolean shouldShowExtraElement(String s) {
		String pattern = "(\\+?\\d*)";
		if (s.matches(pattern)) {
			return true;
		}
		return false;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		if (position < getCount() && getItem(position) == null) {
			return getNumber(inputNumber.getText().toString()).matches(
					HikeConstants.VALID_MSISDN_REGEX);
		}
		return super.isEnabled(position);
	}

	private String getNumber(String textInEditText) {
		int indexTextToBeFiltered = textInEditText
				.lastIndexOf(HikeConstants.GROUP_PARTICIPANT_SEPARATOR) + 2;

		return !textInEditText
				.contains(HikeConstants.GROUP_PARTICIPANT_SEPARATOR) ? textInEditText
				: textInEditText.substring(indexTextToBeFiltered);
	}

}