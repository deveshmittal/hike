package com.bsb.hike.adapters;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class BaseFragmentAdapter extends BaseAdapter
{
	public void onFragmentCreate()
	{
	}

	public void onFragmentDestroy()
	{
	}

	@Override
	public int getCount()
	{
		return 0;
	}

	@Override
	public Object getItem(int i)
	{
		return null;
	}

	@Override
	public long getItemId(int i)
	{
		return 0;
	}

	@Override
	public View getView(int i, View view, ViewGroup viewGroup)
	{
		return null;
	}
}
