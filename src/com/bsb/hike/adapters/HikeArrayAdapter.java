package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.bsb.hike.R;

public abstract class HikeArrayAdapter extends ArrayAdapter<Object> implements SectionIndexer
{
	private static final int SECTION_TYPE = 0;
	private static final int ITEM_TYPE = 1;

	static public class Section
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

	protected Activity activity;
	private HashMap<String, Integer> alphaIndexer; /* keeps track of Section to location */
	private String[] sections;

	@Override
	public boolean areAllItemsEnabled()
	{
		return false;
	}

	public <T> HikeArrayAdapter(Activity context, int viewItemId, List<T> items)
	{
        super(context, viewItemId);
		this.activity = context;

		alphaIndexer = new HashMap<String, Integer>(items.size());
		String lastChar = items.isEmpty() ? "" : items.get(0).toString().substring(0,1).toUpperCase();

		int i = 0;
		for(Object item : items)
		{
			String c = item.toString().substring(0,1).toUpperCase();
			if (!c.equals(lastChar))
			{
				/* add a new entry */
				alphaIndexer.put(c, i);
				add(new Section(c));
				lastChar = c;
			}

			add(item);
			i++;
		}

        Set<String> sectionLetters = alphaIndexer.keySet();

        ArrayList<String> sectionList = new ArrayList<String>(sectionLetters); 

        Collections.sort(sectionList);

        sections = new String[sectionList.size()];

        sectionList.toArray(sections);
	}

	/**
	 * Inflate the actual item view
	 * @param position which positino in the array
	 * @param convertView existing convertView
	 * @param parent
	 * @return inflated View
	 */
	protected abstract android.view.View getItemView(int position, android.view.View convertView, android.view.ViewGroup parent);

	public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent)
	{
		if (getItemViewType(position) == SECTION_TYPE)
		{
			return getHeaderView(position, convertView, parent);
		}
		else
		{
			return getItemView(position, convertView, parent);
		}
	};

	@Override
	public boolean isEnabled(int position)
	{
		return !(getItem(position) instanceof Section);
	}

	private View getHeaderView(int position, View convertView, ViewGroup parent)
	{
		Section section = (Section) getItem(position);
		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
