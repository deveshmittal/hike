package com.bsb.hike.ui.fragments;

import java.util.ArrayList;
import java.util.List;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.PinHistoryAdapter;

public class PinHistoryFragment extends SherlockListFragment implements OnScrollListener
{
	private PinHistoryAdapter PHadapter;
	
	private List<Pair<String, String>>pins = null;
	
	private String userMSISDN;
	
	private SharedPreferences prefs;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View parent = inflater.inflate(R.layout.sticky_pins, null);
		ListView pinsList = (ListView) parent.findViewById(android.R.id.list);
		pinsList.setEmptyView(parent.findViewById(android.R.id.empty));
		return parent;
	}

	@Override
	public void onResume()
	{
		super.onResume();
		
		if (PHadapter != null)
		{
		}
	}

	@Override
	public void onPause()
	{
		super.onPause();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		prefs = getActivity().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		userMSISDN = prefs.getString(HikeMessengerApp.MSISDN_SETTING, "");

		pins = new ArrayList<Pair<String, String>>();

		PHadapter = new PinHistoryAdapter(getActivity(), pins, userMSISDN);
		
		setListAdapter(PHadapter);
		getListView().setOnScrollListener(this);
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
	}

	@Override
	public void onScroll(AbsListView view, final int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) 
	{
		
	}	
}
