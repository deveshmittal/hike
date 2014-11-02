package com.bsb.hike.ui.fragments;

import java.util.HashMap;
import java.util.Map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.adapters.StickerShopAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.utils.StickerManager;

public class StickerShopFragment extends SherlockFragment implements OnScrollListener, Listener
{
	private String[] pubSubListeners = {};

	private StickerShopAdapter mAdapter;
	
	private ListView listview;

	private int previousFirstVisibleItem;

	private int velocity;

	private long previousEventTime;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		View parent = inflater.inflate(R.layout.sticker_shop, null);
		return parent;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		// TODO Register PubSub Listeners
		super.onActivityCreated(savedInstanceState);
		initAdapterAndList();
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
		if (mAdapter != null)
		{
			mAdapter.getStickerLoader().setExitTasksEarly(true);
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();
		if (mAdapter != null)
		{
			mAdapter.getStickerLoader().setExitTasksEarly(false);
			mAdapter.notifyDataSetChanged();
		}
	}

	private void initAdapterAndList()
	{
		listview = (ListView) getView().findViewById(android.R.id.list);
		Map<String, StickerCategory> stickerCategoriesMap = new HashMap<String, StickerCategory>();
		stickerCategoriesMap.putAll(StickerManager.getInstance().getStickerCategoryMap());
		mAdapter = new StickerShopAdapter(getSherlockActivity(), HikeConversationsDatabase.getInstance().getCursorForStickerShop(),stickerCategoriesMap);
		listview.setAdapter(mAdapter);

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
	public void onScrollStateChanged(AbsListView view, int scrollState)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		// TODO Auto-generated method stub

	}

	public static StickerShopFragment newInstance()
	{
		StickerShopFragment stickerShopFragment = new StickerShopFragment();
		return stickerShopFragment;
	}
}
