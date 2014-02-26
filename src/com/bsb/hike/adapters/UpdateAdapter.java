package com.bsb.hike.adapters;

import android.widget.BaseAdapter;

public class UpdateAdapter implements Runnable
{

	private BaseAdapter mAdapter;

	public UpdateAdapter(BaseAdapter adapter)
	{
		this.mAdapter = adapter;
	}

	@Override
	public void run()
	{
		mAdapter.notifyDataSetChanged();
	}

}
