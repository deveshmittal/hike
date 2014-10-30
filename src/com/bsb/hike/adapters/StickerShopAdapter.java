package com.bsb.hike.adapters;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.smartImageLoader.StickerLoader;

public class StickerShopAdapter extends BaseAdapter
{
	public StickerShopAdapter(Context context, List<StickerCategory> stickerCategoryList)
	{

		this.stickerCategoryList = stickerCategoryList;
		this.layoutInflater = LayoutInflater.from(context);
		this.stickerLoader = new StickerLoader(context);
	}

	private List<StickerCategory> stickerCategoryList;

	private LayoutInflater layoutInflater;

	private StickerLoader stickerLoader;

	private boolean isListFlinging;

	@Override
	public int getCount()
	{
		// TODO Auto-generated method stub
		return stickerCategoryList.size();
	}

	@Override
	public Object getItem(int position)
	{
		// TODO Auto-generated method stub
		return stickerCategoryList.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		// TODO Inflate the layout for list_item here
		return null;
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

	public StickerLoader getStickerLoader()
	{
		return stickerLoader;
	}

}
