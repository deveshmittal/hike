package com.bsb.hike.adapters;

import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.bsb.hike.R;

public abstract class HikeArrayAdapter<T> extends ArrayAdapter<T> implements SectionIndexer
{
	private static final int SECTION_TYPE = 0;

	private static final int ITEM_TYPE = 1;

	public boolean isFiltering = true;

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

	private HashMap<String, Integer> alphaIndexer; /*
													 * keeps track of Section to location
													 */

	private String[] sections;

	@Override
	public boolean areAllItemsEnabled()
	{
		return false;
	}

	public HikeArrayAdapter(Activity context, int viewItemId, List<T> items)
	{
		super(context, viewItemId, items);
		this.activity = context;
	}

	/**
	 * Inflate the actual item view
	 * 
	 * @param position
	 *            which positino in the array
	 * @param convertView
	 *            existing convertView
	 * @param parent
	 * @return inflated View
	 */
	protected abstract android.view.View getItemView(int position, android.view.View convertView, android.view.ViewGroup parent);

	/**
	 * Get the title for the activity
	 * 
	 * @return String title of this activity
	 */
	public abstract String getTitle();

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
			v = inflater.inflate(R.layout.section, parent, false);
		}

		TextView tv = (TextView) v.findViewById(R.id.section_txt);
		tv.setText(section.title);

		if (isFiltering)
		{
			v.setVisibility(View.GONE);
		}
		else
		{
			v.setVisibility(View.VISIBLE);
		}

		return v;
	}

	public String idForPosition(int position)
	{
		Object o = getItem(position);
		return o.toString().substring(0, 1).toUpperCase();
	}

	public int getPositionForSection(int section)
	{
		if (alphaIndexer == null)
		{
			return 0;
		}
		return alphaIndexer.get(sections[section]);
	}

	public int getSectionForPosition(int position)
	{
		return 1;
	}

	public String[] getSections()
	{
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
