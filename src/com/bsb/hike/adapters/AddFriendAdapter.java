package com.bsb.hike.adapters;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;

public class AddFriendAdapter extends ArrayAdapter<ContactInfo> implements
		OnItemClickListener {
	private List<ContactInfo> contacts;
	private Set<String> selectedFriends;
	private Context context;
	private ListView listView;

	public AddFriendAdapter(Context context, int resource,
			List<ContactInfo> contacts, ListView listView) {
		super(context, resource);
		this.context = context;
		this.contacts = contacts;
		selectedFriends = new HashSet<String>();
		this.listView = listView;
		listView.setOnItemClickListener(this);
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return contacts.size();
	}

	@Override
	public ContactInfo getItem(int position) {
		return contacts.get(position);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
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
		ContactInfo contact = getItem(position);
		holder.name.setText(contact.getName());
		if (selectedFriends.contains(contact.getMsisdn())) {
			holder.checkbox.setImageResource(R.drawable.select_all_checkbox);
		} else {
			holder.checkbox.setImageResource(-1);
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
		ContactInfo contact = getItem(position - listView.getHeaderViewsCount());
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
