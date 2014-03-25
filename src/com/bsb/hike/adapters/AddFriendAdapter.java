package com.bsb.hike.adapters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;

public class AddFriendAdapter extends SectionedBaseAdapter implements
		OnItemClickListener {
	private HashMap<Integer, List<ContactInfo>> sectionsData;
	private Set<String> selectedFriends;
	private Context context;
	private ListView listView;

	public AddFriendAdapter(Context context, int resource,
			HashMap<Integer, List<ContactInfo>> sectionsData, ListView listView) {
		this.context = context;
		this.sectionsData = sectionsData;
		selectedFriends = new HashSet<String>();
		this.listView = listView;
		listView.setOnItemClickListener(this);
	}

	@Override
	public Object getItem(int section, int position)
	{
		return sectionsData.get(section).get(position );
	}

	@Override
	public long getItemId(int section, int position)
	{
		return 0;
	}

	@Override
	public int getSectionCount()
	{
		return sectionsData.size();
	}

	@Override
	public int getCountForSection(int section)
	{
		return sectionsData.get(section).size();
	}

	@Override
	public View getItemView(int section, int position, View convertView, ViewGroup parent)
	{
		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(
					R.layout.addfriend_listview_tuple, null);
			ViewHolder holder = new ViewHolder();
			holder.userImage = (ImageView) convertView
					.findViewById(R.id.addfriend_user_image);
			holder.name = (TextView) convertView
					.findViewById(R.id.addfriend_user_name);
			holder.status = (TextView) convertView
					.findViewById(R.id.addfriend_user_status);
			holder.checkbox = (ImageView) convertView
					.findViewById(R.id.add_friend_check_box);
			convertView.setTag(holder);
		}
		ViewHolder holder = (ViewHolder) convertView.getTag();
		ContactInfo contact = (ContactInfo) getItem(section,position);
		holder.name.setText(contact.getName());
		if (selectedFriends.contains(contact.getMsisdn())) {
			holder.checkbox.setSelected(true);
		} else {
			holder.checkbox.setSelected(false);
		}

		return convertView;
	}

	@Override
	public int getItemViewType(int section, int position)
	{
		return 0;
	}

	@Override
	public int getItemViewTypeCount()
	{
		return 1;
	}

	@Override
	public int getSectionHeaderViewType(int section)
	{
		return 0;
	}

	@Override
	public int getSectionHeaderViewTypeCount()
	{
		return 1;
	}

	@Override
	public View getSectionHeaderView(int section, View convertView, ViewGroup parent)
	{
		if (convertView == null)
		{
			LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = li.inflate(R.layout.settings_section_layout, parent, false);
			convertView.setBackgroundColor(context.getResources().getColor(R.color.white));
		}
		TextView textView = (TextView) convertView.findViewById(R.id.settings_section_text);
		switch (section)
		{
		case 0:
			textView.setText(getSectionCount()==1? R.string.contacts_on_hike_section : R.string.recommended_contacts_section);
			break;
		case 1:
			textView.setText(R.string.contacts_on_hike_section);
			break;
		default:
			break;
		}
		return convertView;
	}
	
	private static class ViewHolder {
		ImageView userImage;
		TextView name;
		TextView status;
		ImageView checkbox;
	}

	public Set<String> getSelectedFriends() {
		return selectedFriends;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position,
			long arg3) {
		// TODO Auto-generated method stub
		ContactInfo contact = (ContactInfo) getItem(getSectionForPosition(position), getPositionInSectionForPosition(position)-listView.getHeaderViewsCount());
		String msidn = contact.getMsisdn();
		if (selectedFriends.contains(msidn)) {
			selectedFriends.remove(msidn);
		} else {
			selectedFriends.add(msidn);
		}
		// need to confirm what is convention here -- gauravKhanna
		notifyDataSetChanged();
	}
}
