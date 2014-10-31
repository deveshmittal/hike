package com.bsb.hike.ui.fragments;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.DragSortListView.DragSortListView;
import com.bsb.hike.DragSortListView.DragSortListView.DragScrollProfile;
import com.bsb.hike.adapters.StickerSettingsAdapter;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.IStickerResultListener;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadType;
import com.bsb.hike.modules.stickerdownloadmgr.StickerDownloadManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.StickerManager;

public class StickerSettingsFragment extends SherlockFragment implements Listener, DragScrollProfile, OnItemClickListener
{
	private String[] pubSubListeners = {};

	private List<StickerCategory> stickerCategories = new ArrayList<StickerCategory>();
	
	private Set<StickerCategory> visibleAndUpdateStickerSet = new HashSet<StickerCategory>();  //Stores the categories which have update available and are visible

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
		checkAndInflateUpdateView(parent);
		
		registerListener();
		return parent;
	}

	private void checkAndInflateUpdateView(final View parent)
	{
		if(shouldAddUpdateView())
		{
			final View updateAll = parent.findViewById(R.id.update_all_ll);
			final View confirmAll = parent.findViewById(R.id.confirmation_ll);
			
			Animation alphaIn = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_up_noalpha);
			alphaIn.setDuration(800);
			updateAll.setAnimation(alphaIn);
			updateAll.setVisibility(View.VISIBLE);
			alphaIn.start();
			
			updateAll.setOnClickListener(new View.OnClickListener()
			{
				
				@Override
				public void onClick(View v)
				{
					if(shouldAddUpdateView())
					{
						updateAll.setVisibility(View.INVISIBLE);
						confirmAll.setVisibility(View.VISIBLE);
						setUpdateDetails(parent, confirmAll);
					}
					else
					{
						Toast.makeText(getActivity(), R.string.update_all_fail_string, Toast.LENGTH_SHORT).show();
					}
				}
			});
		}
	}

	private void setUpdateDetails(View parent, final View confirmView)
	{
		TextView categoryCost = (TextView) parent.findViewById(R.id.sticker_cost);
		TextView totalPacks = (TextView) parent.findViewById(R.id.total_packs);
		TextView totalStickers = (TextView) parent.findViewById(R.id.pack_details);
		TextView cancelBtn = (TextView) parent.findViewById(R.id.cancel_btn);
		TextView confirmBtn = (TextView) parent.findViewById(R.id.confirm_btn);
		totalPacks.setText(getString(R.string.n_packs, visibleAndUpdateStickerSet.size()));
		categoryCost.setText(R.string.sticker_pack_free);
		cancelBtn.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				confirmView.setVisibility(View.GONE);
			}
		});
		
		confirmBtn.setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				for(final StickerCategory category : visibleAndUpdateStickerSet)
				{
					category.setState(StickerCategory.DOWNLOADING);
					final DownloadType type = DownloadType.UPDATE;
					StickerDownloadManager.getInstance(getActivity()).DownloadMultipleStickers(category, type, new IStickerResultListener()
					{
						
						@Override
						public void onSuccess(Object result)
						{
							StickerManager.getInstance().sucessFullyDownloadedStickers(result);
						}
						
						@Override
						public void onProgressUpdated(double percentage)
						{
							
						}
						
						@Override
						public void onFailure(Object result, Throwable exception)
						{
							StickerManager.getInstance().stickersDownloadFailed(result);
						}
					});
				}
				
				mAdapter.notifyDataSetChanged();
				confirmView.setVisibility(View.GONE);
			}
		});
	}

	private boolean shouldAddUpdateView()
	{
		visibleAndUpdateStickerSet.clear();
		for(StickerCategory category : stickerCategories)
		{
			if(category.isVisible() && (category.getState() == StickerCategory.UPDATE))
			{
				visibleAndUpdateStickerSet.add(category);
			}
		}
		
		if(visibleAndUpdateStickerSet.size() > 0)
		{
			return true;
		}
		else
		{
			return false;
		}
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
		//mDslv.setOnScrollListener(this);
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
	public void onStop()
	{
		mAdapter.persistChanges();
		super.onStop();
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
		// TODO Some method might be added in future over here to handle clicks on list 
	}
	
	private void registerListener()
	{
		IntentFilter filter = new IntentFilter(StickerManager.STICKERS_UPDATED);
		filter.addAction(StickerManager.STICKERS_FAILED);
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);
	}
	
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent.getAction().equals(StickerManager.STICKERS_UPDATED))
			{
				String categoryId = intent.getStringExtra(StickerManager.CATEGORY_ID);
				final StickerCategory category = StickerManager.getInstance().getCategoryForId(categoryId);
				if(getActivity() != null)
				{
					getActivity().runOnUiThread(new Runnable()
					{

						@Override
						public void run()
						{
							category.setState(StickerCategory.DONE);
							mAdapter.notifyDataSetChanged();

						}
					});
				}
			}
			
			else if(intent.getAction().equals(StickerManager.STICKERS_FAILED))
			{
				Bundle b = intent.getBundleExtra(StickerManager.STICKER_DATA_BUNDLE);
				String categoryId = (String) b.getSerializable(StickerManager.CATEGORY_ID);
				final StickerCategory category = StickerManager.getInstance().getCategoryForId(categoryId);
				if (getActivity() != null)
				{
					getActivity().runOnUiThread(new Runnable()
					{

						@Override
						public void run()
						{
							category.setState(StickerCategory.RETRY);
							mAdapter.notifyDataSetChanged();
						}
					});
				}
			}
		}
	};

}
