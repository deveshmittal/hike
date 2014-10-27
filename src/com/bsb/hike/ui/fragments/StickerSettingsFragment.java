package com.bsb.hike.ui.fragments;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.actionbarsherlock.app.SherlockFragment;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.DragSortListView.DragSortListView;
import com.bsb.hike.DragSortListView.DragSortListView.DragScrollProfile;
import com.bsb.hike.adapters.StickerSettingsAdapter;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

public class StickerSettingsFragment extends SherlockFragment implements Listener, OnScrollListener, DragScrollProfile, OnItemClickListener
{
	private String[] pubSubListeners = {};

	private List<StickerCategory> stickerCategories = new ArrayList<StickerCategory>();

	private StickerSettingsAdapter mAdapter;
	
	private DragSortListView mDslv;

	private int previousFirstVisibleItem;

	private int velocity;

	private long previousEventTime;
	
	private HikeSharedPreferenceUtil prefs;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View parent = inflater.inflate(R.layout.sticker_settings, null);
		prefs = HikeSharedPreferenceUtil.getInstance(getActivity());
		showTipIfRequired(parent);
		initAdapterAndList(parent);
		return parent;
	}

	/**
	 * Utility method to show category reordering tip
	 * @param parent
	 */
	private void showTipIfRequired(View parent)
	{
		if(!prefs.getData(HikeMessengerApp.IS_STICKER_CATEGORY_REORDERING_TIP_SHOWN, false))  //Showing the tip here
		{
			((View) parent.findViewById(R.id.reorder_tip)).setVisibility(View.VISIBLE); 
		}
	}

	private void initAdapterAndList(View parent)
	{
		// TODO Initialise the listview and adapter here
		// TODO Add the empty state for listView here as well. Will there be an empty state for settings?
		stickerCategories.addAll(StickerManager.getInstance().getMyStickerCategoryList());
		mAdapter = new StickerSettingsAdapter(getActivity(), stickerCategories);
		mDslv = (DragSortListView) parent.findViewById(R.id.item_list);
		mDslv.setOnScrollListener(this);
		mDslv.setAdapter(mAdapter);
		mDslv.setDragScrollProfile(this);
		mDslv.setClickable(true);
		mDslv.setOnItemClickListener(this);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
	}

	@Override
	public void onDestroy()
	{
		// TODO Clear the adapter and stickercategory list as well
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		super.onDestroy();
	}

	@Override
	public void onPause()
	{
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	public void onResume()
	{
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		// TODO Auto-generated method stub

	}

	public static StickerSettingsFragment newInstance()
	{
		StickerSettingsFragment stickerSettingsFragment = new StickerSettingsFragment();
		return stickerSettingsFragment;
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		// TODO Auto-generated method stub
		if (previousFirstVisibleItem != firstVisibleItem)
		{
			long currTime = System.currentTimeMillis();
			long timeToScrollOneElement = currTime - previousEventTime;
			velocity = (int) (((double) 1 / timeToScrollOneElement) * 1000);

			previousFirstVisibleItem = firstVisibleItem;
			previousEventTime = currTime;
		}

		if (mAdapter == null)
		{
			return;
		}

		mAdapter.setIsListFlinging(velocity > HikeConstants.MAX_VELOCITY_FOR_LOADING_IMAGES_SMALL);

	}

	@Override
	public float getSpeed(float w, long t)
	{
		// TODO Fine tune these parameters further
		if (w > 0.8f)
		{
			return ((float) mAdapter.getCount()) / 1.0f;
		}
		else
		{
			return 1.0f * w;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		StickerCategory category = (StickerCategory) parent.getItemAtPosition(position);
		CheckBox categoryVisibBox = (CheckBox) view.findViewById(R.id.category_checkbox);
		categoryVisibBox.toggle();
		category.setVisible(categoryVisibBox.isChecked());
	}
}
