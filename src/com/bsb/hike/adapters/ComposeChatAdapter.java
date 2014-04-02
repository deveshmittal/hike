package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.tasks.FetchFriendsTask;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.PinnedSectionListView.PinnedSectionListAdapter;

public class ComposeChatAdapter extends FriendsAdapter implements PinnedSectionListAdapter
{
	private static final String TAG = "composeChatAdapter";

	private Map<String, ContactInfo> selectedPeople;

	private Map<String, ContactInfo> existingParticipants;

	private boolean showCheckbox, showExtraAtFirst;

	private int mIconImageSize;

	private IconLoader iconloader;

	private boolean fetchGroups;

	private String existingGroupId;

	private int statusForEmptyContactInfo;

	private List<ContactInfo> newContactsList;

	public ComposeChatAdapter(Context context, ListView listView, boolean fetchGroups, String existingGroupId)
	{
		super(context, listView);
		selectedPeople = new HashMap<String, ContactInfo>();
		existingParticipants = new HashMap<String, ContactInfo>();
		mIconImageSize = context.getResources().getDimensionPixelSize(R.dimen.icon_picture_size);
		iconloader = new IconLoader(context, mIconImageSize);

		this.existingGroupId = existingGroupId;
		this.fetchGroups = fetchGroups;
		groupsList = new ArrayList<ContactInfo>(0);
		filteredGroupsList = new ArrayList<ContactInfo>(0);
	}

	@Override
	public void executeFetchTask()
	{
		setLoadingView();
		FetchFriendsTask fetchFriendsTask = new FetchFriendsTask(this, context, friendsList, hikeContactsList, smsContactsList, filteredFriendsList, filteredHikeContactsList,
				filteredSmsContactsList, groupsList, filteredGroupsList, existingParticipants, fetchGroups, existingGroupId);
		Utils.executeAsyncTask(fetchFriendsTask);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		// TODO Auto-generated method stub
		ViewType viewType = ViewType.values()[getItemViewType(position)];

		ContactInfo contactInfo = null;
		ViewHolder holder = null;

		if (convertView == null)
		{
			convertView = inflateView(viewType);

		}

		contactInfo = getItem(position);
		// either section or other we do have
		if (viewType == ViewType.SECTION)
		{
			TextView tv = (TextView) convertView.findViewById(R.id.name);
			tv.setText(contactInfo.getName());

			TextView count = (TextView) convertView.findViewById(R.id.count);
			count.setText(contactInfo.getMsisdn());
			// set section heading
		}
		else if (viewType == ViewType.EXTRA)
		{
			TextView tv = (TextView) convertView.findViewById(R.id.contact);
			tv.setText(R.string.compose_chat_heading);
		}
		else
		{
			holder = (ViewHolder) convertView.getTag();
			holder.status.setText(contactInfo.getMsisdn());

			String name = contactInfo.getName();
			holder.name.setText("".equals(name) || null == name ? contactInfo.getMsisdn() : name);

			if (viewType == ViewType.NEW_CONTACT)
			{
				holder.status.setText(statusForEmptyContactInfo);
			}
			else
			{
				holder.status.setText(contactInfo.getMsisdn());
			}

			if (contactInfo.isUnknownContact())
			{
				holder.userImage.setScaleType(ScaleType.CENTER_INSIDE);
				holder.userImage.setBackgroundResource(R.drawable.avatar_01_rounded);
				holder.userImage.setImageResource(R.drawable.ic_default_avatar);
			}
			else
			{
				if (contactInfo.hasCustomPhoto())
				{
					holder.userImage.setScaleType(ScaleType.FIT_CENTER);
					holder.userImage.setBackgroundDrawable(null);
					iconloader.loadImage(contactInfo.getMsisdn(), true, holder.userImage, true);
				}
				else
				{
					holder.userImage.setScaleType(ScaleType.CENTER_INSIDE);
					holder.userImage.setBackgroundResource(Utils.getDefaultAvatarResourceId(contactInfo.getMsisdn(), true));
					holder.userImage.setImageResource(R.drawable.ic_default_avatar);
				}
			}
			if (showCheckbox)
			{
				holder.checkbox.setVisibility(View.VISIBLE);
				if (selectedPeople.containsKey(contactInfo.getMsisdn()))
				{

					holder.checkbox.setChecked(true);
				}
				else
				{
					holder.checkbox.setChecked(false);
				}
			}
			else
			{
				holder.checkbox.setVisibility(View.GONE);
			}
		}
		return convertView;
	}

	private View inflateView(ViewType viewType)
	{
		View convertView = null;
		switch (viewType)
		{
		case SECTION:
			convertView = LayoutInflater.from(context).inflate(R.layout.friends_group_view, null);
			break;
		case EXTRA:
			convertView = LayoutInflater.from(context).inflate(R.layout.compose_chat_header, null);
			break;
		default:
			convertView = LayoutInflater.from(context).inflate(R.layout.hike_list_item, null);
			ViewHolder holder = new ViewHolder();
			holder.userImage = (ImageView) convertView.findViewById(R.id.contact_image);
			holder.name = (TextView) convertView.findViewById(R.id.name);
			holder.status = (TextView) convertView.findViewById(R.id.number);
			holder.checkbox = (CheckBox) convertView.findViewById(R.id.checkbox);
			convertView.setTag(holder);
			break;
		}
		return convertView;
	}

	private static class ViewHolder
	{
		ImageView userImage;

		TextView name;

		TextView status;

		CheckBox checkbox;
	}

	@Override
	public void makeCompleteList(boolean filtered)
	{
		boolean shouldContinue = makeSetupForCompleteList(filtered);

		if (!shouldContinue)
		{
			return;
		}

		// hack for header, as we are using pinnedSectionListView

		if (fetchGroups && !groupsList.isEmpty())
		{
			ContactInfo groupSection = new ContactInfo(SECTION_ID, Integer.toString(filteredGroupsList.size()), context.getString(R.string.group_chats), FRIEND_PHONE_NUM);
			if (filteredGroupsList.size() > 0)
			{
				completeList.add(groupSection);
				completeList.addAll(filteredGroupsList);
			}
		}

		ContactInfo friendsSection = new ContactInfo(SECTION_ID, Integer.toString(filteredFriendsList.size()), context.getString(R.string.friends), FRIEND_PHONE_NUM);
		updateFriendsList(friendsSection);

		if (isHikeContactsPresent())
		{
			ContactInfo hikeContactsSection = new ContactInfo(SECTION_ID, Integer.toString(filteredHikeContactsList.size()), context.getString(R.string.hike_contacts),
					CONTACT_PHONE_NUM);
			updateHikeContactList(hikeContactsSection);
		}
		if (showSMSContacts)
		{
			ContactInfo smsContactsSection = new ContactInfo(SECTION_ID, Integer.toString(filteredSmsContactsList.size()), context.getString(R.string.sms_contacts),
					CONTACT_PHONE_NUM);
			updateSMSContacts(smsContactsSection);
		}
		if (newContactsList != null)
		{
			completeList.addAll(newContactsList);
		}
		if (completeList.size() != 0 && showExtraAtFirst)
		{
			// items are > 0
			ContactInfo header = new ContactInfo(EXTRA_ID, null, null, null);
			completeList.add(0, header);
		}

		notifyDataSetChanged();
		if (emptyView != null)
		{
			listView.setEmptyView(emptyView);
		}
	}

	public void addContact(ContactInfo contactInfo)
	{
		selectedPeople.put(contactInfo.getMsisdn(), contactInfo);
		notifyDataSetChanged();
	}

	public void removeContact(ContactInfo contactInfo)
	{
		selectedPeople.remove(contactInfo.getMsisdn());
		notifyDataSetChanged();
	}

	public void clearAllSelection(boolean showCheckbox)
	{
		selectedPeople.clear();
		this.showCheckbox = showCheckbox;
		notifyDataSetChanged();
	}

	public void showCheckBoxAgainstItems(boolean showCheckbox)
	{
		this.showCheckbox = showCheckbox;

	}

	public ArrayList<ContactInfo> getAllSelectedContacts()
	{
		return new ArrayList<ContactInfo>(selectedPeople.values());
	}

	public int getSelectedContactCount()
	{
		return selectedPeople.size();
	}

	public void setShowExtraAtFirst(boolean showExtraAtFirst)
	{
		this.showExtraAtFirst = showExtraAtFirst;
	}

	@Override
	protected void makeFilteredList(CharSequence constraint, List<ContactInfo> friendList, List<ContactInfo> hikeContactList, List<ContactInfo> smsList)
	{
		// TODO Auto-generated method stub

		super.makeFilteredList(constraint, friendList, hikeContactList, smsList);
		// to add new section and number for user typed number
		String text = constraint.toString();
		if (isIntegers(text))
		{
			newContactsList = new ArrayList<ContactInfo>();
			ContactInfo section = new ContactInfo(SECTION_ID, null, context.getString(R.string.compose_chat_other_contacts), null);
			String normalisedMsisdn = getNormalisedMsisdn(text);
			ContactInfo info = new ContactInfo(normalisedMsisdn, normalisedMsisdn, normalisedMsisdn, text);
			newContactsList.add(section);
			newContactsList.add(info);
		}
		else
		{
			newContactsList = null;
		}
	}

	private boolean isIntegers(String input)
	{
		return input.matches("\\+?\\d+");
	}

	private String getNormalisedMsisdn(String textEntered)
	{
		return Utils.normalizeNumber(textEntered,
				context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.COUNTRY_CODE, HikeConstants.INDIA_COUNTRY_CODE));
	}

	public void setStatusForEmptyContactInfo(int statusStringId)
	{
		this.statusForEmptyContactInfo = statusStringId;
	}

	public boolean isContactAdded(ContactInfo info)
	{
		return selectedPeople.containsKey(info.getMsisdn());
	}

	public boolean isContactPresentInExistingParticipants(ContactInfo info)
	{
		return existingParticipants.containsKey(info.getMsisdn());
	}

	@Override
	public int getItemViewType(int position)
	{
		ContactInfo info = getItem(position);
		if (info.isUnknownContact())
		{
			return ViewType.NEW_CONTACT.ordinal();
		}
		return super.getItemViewType(position);
	}

}
