package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.DragSortListView.DragSortListView;
import com.bsb.hike.DragSortListView.DragSortListView.DragSortListener;
import com.bsb.hike.models.StickerCategory;

public class StickerSettingsAdapter extends BaseAdapter implements DragSortListener
{
	/**
	 * Key is ListView position, value is ArrayList position
	 */
	private SparseIntArray mListMapping = new SparseIntArray();

	private List<StickerCategory> stickerCategories;

	private Context mContext;

	private LayoutInflater mInflater;

	private boolean isListFlinging;

	private boolean isDragged = false;

	public StickerSettingsAdapter(Context context, List<StickerCategory> stickerCategories)
	{
		this.mContext = context;
		this.stickerCategories = stickerCategories;
		this.mInflater = LayoutInflater.from(mContext);
	}

	/**
	 * Resets listview - arraylist position mapping.
	 */
	public void reset()
	{
		resetMappings();
		notifyDataSetChanged();
	}

	private void resetMappings()
	{
		mListMapping.clear();
	}

	@Override
	public int getCount()
	{
		if (stickerCategories != null)
		{
			return stickerCategories.size();
		}
		return 0;
	}

	@Override
	public StickerCategory getItem(int position)
	{
		int pos = mListMapping.get(position, position);
		return stickerCategories.get(pos);
	}

	@Override
	public long getItemId(int position)
	{
		// TODO Auto-generated method stub
		return mListMapping.get(position, position);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		StickerCategory category = getItem(position);
		ViewHolder viewHolder;
		
		if(convertView == null)
		{
			convertView = mInflater.inflate(R.layout.sticker_settings_list_item, null);
			viewHolder = new ViewHolder();
			viewHolder.categoryName = (TextView) convertView.findViewById(R.id.category_name);
			convertView.setTag(viewHolder);
			
		}
		
		else
		{
			viewHolder = (ViewHolder) convertView.getTag();
		}
		
		viewHolder.categoryName.setText(category.getCategoryName());
		
		return convertView;
	}

	public void setIsListFlinging(boolean b)
	{
		boolean notify = b != isListFlinging;

		isListFlinging = b;

		if (notify && !isListFlinging)
		{
			notifyDataSetChanged();
		}
	}

	/**
	 * On drop, this updates the mapping between ArrayList positions and ListView positions. The ArrayList is unchanged.
	 * 
	 * @see DragSortListView.DropListener#drop(int, int)
	 */
	@Override
	public void drop(int from, int to)
	{
		if (true)
		{ // Some Condition, at which the drop should occur.
			if (from != to)
			{
				int cursorFrom = mListMapping.get(from, from);

				if (from > to)
				{
					for (int i = from; i > to; --i)
					{
						mListMapping.put(i, mListMapping.get(i - 1, i - 1));
					}
				}
				else
				{
					for (int i = from; i < to; ++i)
					{
						mListMapping.put(i, mListMapping.get(i + 1, i + 1));
					}
				}
				mListMapping.put(to, cursorFrom);
				isDragged = true;
				cleanMapping();
				notifyDataSetChanged();
			}
		}
		else
		{
			return;
		}
	}

	@Override
	public void drag(int from, int to)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(int which)
	{
		// TODO Auto-generated method stub

	}

	/**
	 * Remove unnecessary mappings from sparse array.
	 */
	private void cleanMapping()
	{
		ArrayList<Integer> toRemove = new ArrayList<Integer>();

		int size = mListMapping.size();
		for (int i = 0; i < size; ++i)
		{
			if (mListMapping.keyAt(i) == mListMapping.valueAt(i))
			{
				toRemove.add(mListMapping.keyAt(i));
			}
		}

		size = toRemove.size();
		for (int i = 0; i < size; ++i)
		{
			mListMapping.delete(toRemove.get(i));
		}
	}

	public void persistChanges()
	{
		// TODO : Add method to persist the dragged changes in listview
	}

	/**
	 * 
	 * @return whether the list was reordered or not
	 */
	public boolean getDragged()
	{
		return isDragged;
	}
	
	private class ViewHolder
	{
		TextView categoryName;
	}

}
