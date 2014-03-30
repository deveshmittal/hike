package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.tasks.FetchFriendsTask;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.PinnedSectionListView.PinnedSectionListAdapter;

public class ComposeChatAdapter extends FriendsAdapter implements PinnedSectionListAdapter
{
	private static final String TAG = "composeChatAdapter";

	Map<String, ContactInfo> selectedPeople;

	private boolean showCheckbox, showExtraAtFirst;

	private int mIconImageSize;

	private IconLoader iconloader;

	private boolean fetchGroups;

	private String existingGroupId;

	public ComposeChatAdapter(Context context, boolean fetchGroups, String existingGroupId)
	{
		super(context);
		selectedPeople = new HashMap<String, ContactInfo>();

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
		FetchFriendsTask fetchFriendsTask = new FetchFriendsTask(this, context, friendsList, hikeContactsList, smsContactsList, filteredFriendsList, filteredHikeContactsList,
				filteredSmsContactsList, groupsList, filteredGroupsList, selectedPeople, fetchGroups, existingGroupId);
		Utils.executeAsyncTask(fetchFriendsTask);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		// TODO Auto-generated method stub
		Log.i(TAG, "in getview position " + position);
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
			TextView tv = (TextView) convertView.findViewById(R.id.settings_section_text);
			tv.setText(contactInfo.getName());
			// set section heading
		}
		else if (viewType == ViewType.EXTRA)
		{
			TextView tv = (TextView) convertView.findViewById(R.id.contact);
			tv.setText(R.string.compose_chat_heading);
		}
		else
		{
			Log.d(TAG, "in getview position is " + position + " and contact info is " + contactInfo);
			holder = (ViewHolder) convertView.getTag();
			holder.name.setText(contactInfo.getName());
			holder.status.setText(contactInfo.getMsisdn());
			iconloader.loadImage(contactInfo.getMsisdn(), true, holder.userImage, true);
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
		Log.d(TAG, "in getview viewtype " + viewType + " and convert view is null");
		switch (viewType)
		{
		case SECTION:
			convertView = LayoutInflater.from(context).inflate(R.layout.settings_section_layout, null);
			break;
		case EXTRA:
			convertView = LayoutInflater.from(context).inflate(R.layout.compose_chat_header, null);
			break;
		default:
			Log.d(TAG, "in getview not section ");
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
		if (showExtraAtFirst)
		{
			ContactInfo header = new ContactInfo(EXTRA_ID, null, null, null);
			completeList.add(header);
		}

		if (fetchGroups && !groupsList.isEmpty())
		{
			ContactInfo groupSection = new ContactInfo(SECTION_ID, null, context.getString(R.string.group_chats), FRIEND_PHONE_NUM);

			completeList.add(groupSection);
			completeList.addAll(filteredGroupsList);
		}

		ContactInfo friendsSection = new ContactInfo(SECTION_ID, null, context.getString(R.string.compose_chat_friends_on_hike), FRIEND_PHONE_NUM);
		updateFriendsList(friendsSection);

		if (isHikeContactsPresent())
		{
			ContactInfo hikeContactsSection = new ContactInfo(SECTION_ID, null, context.getString(R.string.compose_chat_contacts_on_hike), CONTACT_PHONE_NUM);
			updateHikeContactList(hikeContactsSection);
		}
		if (showSMSContacts)
		{
			ContactInfo smsContactsSection = new ContactInfo(SECTION_ID, null, context.getString(R.string.compose_chat_all_contacts), CONTACT_PHONE_NUM);
			updateSMSContacts(smsContactsSection);
		}

		notifyDataSetChanged();
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
}
