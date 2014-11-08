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
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.R.anim;
import com.bsb.hike.DragSortListView.DragSortListView;
import com.bsb.hike.DragSortListView.DragSortListView.DragScrollProfile;
import com.bsb.hike.DragSortListView.DragSortListView.DropListener;
import com.bsb.hike.adapters.StickerSettingsAdapter;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.IStickerResultListener;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadType;
import com.bsb.hike.modules.stickerdownloadmgr.StickerDownloadManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

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
		
		return parent;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		prefs = HikeSharedPreferenceUtil.getInstance(getActivity());
		initAdapterAndList();
		showTipIfRequired();
		checkAndInflateUpdateView();
		
		registerListener();
		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
	}

	private void checkAndInflateUpdateView()
	{
		if(shouldAddUpdateView())
		{	
			final View parent = getView();
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
		
		displayTotalStickersCount(totalStickers);
		
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
				for(StickerCategory category : visibleAndUpdateStickerSet)
				{
					category.setState(StickerCategory.DOWNLOADING);
					final DownloadType type = DownloadType.UPDATE;
					StickerDownloadManager.getInstance(getActivity()).DownloadMultipleStickers(category, type, null);
				}
				
				mAdapter.notifyDataSetChanged();
				confirmView.setVisibility(View.GONE);
			}
		});
	}

	private void displayTotalStickersCount(TextView totalStickers)
	{
		int totalCount = 0;
		int totalSize = 0;
		for(StickerCategory category : visibleAndUpdateStickerSet)
		{
			if(category.getMoreStickerCount() > 0)
			{
				totalCount += category.getMoreStickerCount(); 
			}
			
			if(category.getCategorySize() > 0)
			{
				totalSize += category.getCategorySize();
			}
		}
		if(totalCount > 0)
		{
			String text = getActivity().getResources().getString(R.string.n_stickers, totalCount);
			if(totalSize > 0)
			{
				text += ", " + Utils.getSizeForDisplay(totalSize);
			}
			
			totalStickers.setText(text);
		}
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
	private void showTipIfRequired()
	{
		showDragTip();
	}

	private void showDragTip()
	{
		if(!prefs.getData(HikeMessengerApp.IS_STICKER_CATEGORY_REORDERING_TIP_SHOWN, false))  //Showing the tip here
		{
			final View parent = getView().findViewById(R.id.list_ll);
			final View v =(View) parent.findViewById(R.id.reorder_tip);
			v.setVisibility(View.VISIBLE);
			
			mDslv.addDropListener(new DropListener()
			{
				@Override
				public void drop(int from, int to)
				{	
					if(!prefs.getData(HikeMessengerApp.IS_STICKER_CATEGORY_REORDERING_TIP_SHOWN, false))
					{
						prefs.saveData(HikeMessengerApp.IS_STICKER_CATEGORY_REORDERING_TIP_SHOWN, true); // Setting the tip flag
						StickerCategory category = mAdapter.getItem(from);
						if ((from == to) || (!category.isVisible())) // Dropping at the same position. No need to perform Drop.
						{
							return;
						}

						if (from > mAdapter.getLastVisibleIndex() && to > mAdapter.getLastVisibleIndex() + 1)
						{
							return;
						}

						ImageView tickImage = (ImageView) parent.findViewById(R.id.reorder_indicator);
						tickImage.setImageResource(R.drawable.art_tick);
						TextView tipText = (TextView) parent.findViewById(R.id.drag_tip);
						tipText.setText(getResources().getString(R.string.great_job));
						tipText.setTextColor(getResources().getColor(R.color.white));
						((View) parent).findViewById(R.id.drag_tip_subtext).setVisibility(View.GONE);
						v.setBackgroundColor(getResources().getColor(R.color.sticker_drag_tip_bg_color));
						
						TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.ABSOLUTE, 0,
								Animation.ABSOLUTE, -v.getHeight());
						animation.setDuration(400);
						animation.setStartOffset(800);
						parent.setAnimation(animation);
						
						animation.setAnimationListener(new AnimationListener()
						{
							@Override
							public void onAnimationStart(Animation animation)
							{
							}

							@Override
							public void onAnimationRepeat(Animation animation)
							{

							}

							@Override
							public void onAnimationEnd(Animation animation)
							{
								v.setVisibility(View.GONE);
								TranslateAnimation temp = new TranslateAnimation(0, 0, 0, 0);
								temp.setDuration(1l);
								parent.startAnimation(temp);
							}
						});
					}
				}
			});
		}

	}

	private void initAdapterAndList()
	{
		View parent = getView();
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
	public void onDestroy()
	{
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		unregisterListeners();
		super.onDestroy();
	}

	@Override
	public void onPause()
	{
		// TODO Auto-generated method stub
		super.onPause();
		if (mAdapter != null)
		{
			mAdapter.getStickerPreviewLoader().setExitTasksEarly(true);
		}
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
		if (mAdapter != null)
		{
			mAdapter.getStickerPreviewLoader().setExitTasksEarly(false);
			mAdapter.notifyDataSetChanged();
		}
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
		StickerCategory category = mAdapter.getItem(position);
		if(category.getState() == StickerCategory.RETRY && category.isVisible())
		{
			category.setState(StickerCategory.DOWNLOADING);
			StickerManager.getInstance().initialiseDownloadStickerTask(category, getActivity());
			mAdapter.notifyDataSetChanged();
		}
		
		else
		{
			return;
		}
	}
	
	private void registerListener()
	{
		IntentFilter filter = new IntentFilter(StickerManager.STICKERS_UPDATED);
		filter.addAction(StickerManager.STICKERS_FAILED);
		filter.addAction(StickerManager.MORE_STICKERS_DOWNLOADED);
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);
	}
	
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent.getAction().equals(StickerManager.STICKERS_UPDATED) || intent.getAction().equals(StickerManager.MORE_STICKERS_DOWNLOADED))
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
				final boolean failedDueToLargeFile =b.getBoolean(StickerManager.STICKER_DOWNLOAD_FAILED_FILE_TOO_LARGE);
				if (getActivity() != null)
				{
					getActivity().runOnUiThread(new Runnable()
					{

						@Override
						public void run()
						{
							if(failedDueToLargeFile)
							{
								Toast.makeText(getActivity(), R.string.out_of_space, Toast.LENGTH_SHORT).show();
							}
							category.setState(StickerCategory.RETRY);
							mAdapter.notifyDataSetChanged();
						}
					});
				}
			}
		}
	};
	
	public void unregisterListeners()
	{
		LocalBroadcastManager.getInstance(getSherlockActivity()).unregisterReceiver(mMessageReceiver);
	}

}
