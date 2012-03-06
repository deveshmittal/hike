package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;

public class HikeArrayAdapter extends ArrayAdapter<Object> implements SectionIndexer
{
	private static final int SECTION_TYPE = 0;
	private static final int ITEM_TYPE = 1;

	public class Section
	{
		public String title;
		public Section(String c)
		{
			this.title = c;
		}

		public String toString()
		{
			return title;
		}

	}

	private Context context;
	private HashMap<String, Integer> alphaIndexer;
	private String[] sections;
	
	@Override
	public boolean areAllItemsEnabled()
	{
		return false;
	}

	public HikeArrayAdapter(Context context, int inviteItem, List<ContactInfo> contacts)
	{
        super(context, inviteItem);
		this.context = context;

		alphaIndexer = new HashMap<String, Integer>(contacts.size());
		String lastChar = contacts.isEmpty() ? "" : contacts.get(0).toString().substring(0,1).toUpperCase();

		int i = 0;
		for(ContactInfo contact : contacts)
		{
			String c = contact.toString().substring(0,1).toUpperCase();
			if (!c.equals(lastChar))
			{
				/* add a new entry */
				alphaIndexer.put(c, i);
				add(new Section(c));
				lastChar = c;
			}

			add(contact);
			i++;
		}

        Set<String> sectionLetters = alphaIndexer.keySet();

        ArrayList<String> sectionList = new ArrayList<String>(sectionLetters); 

        Collections.sort(sectionList);

        sections = new String[sectionList.size()];

        sectionList.toArray(sections);
	}

	public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent)
	{
		if (getItemViewType(position) == SECTION_TYPE)
		{
			return getHeaderView(position, convertView, parent);
		}

		ContactInfo contactInfo = (ContactInfo) getItem(position);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = convertView;
		if (v == null)
		{
			v = inflater.inflate(R.layout.invite_item, parent, false);
		}

		TextView textView = (TextView) v.findViewById(R.id.name);
		textView.setText(contactInfo.getName());

		Button button = (Button) v.findViewById(R.id.invite_button);
		button.setEnabled(!contactInfo.isOnhike());

		boolean no_dividers = ((position == getCount() - 1) ||
								(getItem(position+1) instanceof Section));
		View divider = v.findViewById(R.id.item_divider);
		divider.setVisibility(no_dividers ? View.INVISIBLE : View.VISIBLE);
		return v;
	};

	@Override
	public boolean isEnabled(int position)
	{
		return !(getItem(position) instanceof Section);
	}

	private View getHeaderView(int position, View convertView, ViewGroup parent)
	{
		Section section = (Section) getItem(position);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = convertView;
		if (v == null)
		{
			v = inflater.inflate(R.layout.section_item, parent, false);
		}

		TextView tv = (TextView) v.findViewById(R.id.section_title);
		tv.setText(section.title);
		return v;
	}

	public String idForPosition(int position)
	{
		Object o = getItem(position);
		return o.toString().substring(0,1).toUpperCase();
	}

    public int getPositionForSection(int section) {
        return alphaIndexer.get(sections[section]);
    }

    public int getSectionForPosition(int position) {
        return 1;
    }

    public String[] getSections() {
         return sections;
    }

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public int getViewTypeCount()
	{
		return 2;/* section headers plus items */
	}

	@Override
	public int getItemViewType(int position)
	{
		Object o = getItem(position);
		if (o instanceof Section)
		{
			return SECTION_TYPE;
		}

		return ITEM_TYPE;
	}
}
