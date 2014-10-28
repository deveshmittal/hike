package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.DragSortListView.DragSortListView;
import com.bsb.hike.DragSortListView.DragSortListView.DragSortListener;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

public class StickerSettingsAdapter extends BaseAdapter implements DragSortListener, OnClickListener
{
	/**
	 * Key is ListView position, value is ArrayList position ( which is to be interpreted as stickerCategoryIndex - 1 )
	 */
	private SparseIntArray mListMapping = new SparseIntArray();

	private List<StickerCategory> stickerCategories;

	private Context mContext;

	private LayoutInflater mInflater;

	private boolean isListFlinging;

	private Set<StickerCategory> stickerSet = new HashSet<StickerCategory>();  //Stores the categories which have been reordered

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
		final StickerCategory category = getItem(position);
		ViewHolder viewHolder;
		
		if(convertView == null)
		{
			convertView = mInflater.inflate(R.layout.sticker_settings_list_item, null);
			viewHolder = new ViewHolder();
			viewHolder.categoryName = (TextView) convertView.findViewById(R.id.category_name);
			viewHolder.checkBox = (CheckBox) convertView.findViewById(R.id.category_checkbox);
			viewHolder.categoryPreviewImage = (ImageView) convertView.findViewById(R.id.category_icon);
			viewHolder.checkBox.setTag(category);
			viewHolder.checkBox.setOnClickListener(this);
			convertView.setTag(viewHolder);
			
		}
		
		else
		{
			viewHolder = (ViewHolder) convertView.getTag();
		}
		
		viewHolder.categoryName.setText(category.getCategoryName());
		viewHolder.checkBox.setChecked(category.isVisible());
		viewHolder.categoryPreviewImage.setImageDrawable(StickerManager.getInstance().getCategoryPreviewAsset(mContext, category.getCategoryId()));
		
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
				cleanMapping();
				if(!HikeSharedPreferenceUtil.getInstance(mContext).getData(HikeMessengerApp.IS_STICKER_CATEGORY_REORDERING_TIP_SHOWN, false))  //Resetting the tip flag
				{
					HikeSharedPreferenceUtil.getInstance(mContext).saveData(HikeMessengerApp.IS_STICKER_CATEGORY_REORDERING_TIP_SHOWN, true); // Setting the tip flag}

				}
				notifyDataSetChanged();
				
				if( from > to)
				{
					for(int i = from; i>= to; --i)
					{
						addToStickerSet(i);
					}
				}
				else
				{
					for (int i = from; i<= to; ++i)
					{
						addToStickerSet(i);
					}
				}
			}
		}
		else
		{
			return;
		}
			
	}
	/**
	 * Adds to Categories to stickerSet and also changes it's categoryIndex
	 * @param categoryPos
	 */
	public void addToStickerSet(int categoryPos)
	{
		StickerCategory category = getItem(categoryPos);
		category.setCategoryIndex(categoryPos + 1);  // stickerCategoryIndex is categoryPos + 1
		stickerSet.add(category);
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
		if(stickerSet.size() > 0)
		{
			StickerManager.getInstance().saveVisibilityAndIndex(stickerSet);
		}
	}

	private class ViewHolder
	{
		TextView categoryName;
		
		CheckBox checkBox;
		
		ImageView categoryPreviewImage;
	}
	
	@Override
	public void onClick(View v)
	{
		StickerCategory category = (StickerCategory) v.getTag();
		boolean visibility = !category.isVisible(); 
		CheckBox checkBox = (CheckBox) v;
		category.setVisible(visibility);
		checkBox.setChecked(visibility);
		stickerSet.add(category);
	}

	public Set<StickerCategory> getStickerSet()
	{
		return stickerSet;
	}

}
