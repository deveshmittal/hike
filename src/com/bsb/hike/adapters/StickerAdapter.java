package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants.EmoticonType;
import com.bsb.hike.HikeConstants.STResult;
import com.bsb.hike.R;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.models.StickerPageAdapterItem;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadType;
import com.bsb.hike.modules.stickerdownloadmgr.IStickerResultListener;
import com.bsb.hike.modules.stickerdownloadmgr.StickerDownloadManager;
import com.bsb.hike.smartImageLoader.StickerLoader;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.view.StickerEmoticonIconPageIndicator.StickerEmoticonIconPagerAdapter;

public class StickerAdapter extends PagerAdapter implements StickerEmoticonIconPagerAdapter
{
	private List<StickerCategory> stickerCategoryList;

	private LayoutInflater inflater;

	private Activity activity;

	private Map<StickerCategory, StickerPageObjects> stickerObjMap;
	
	private StickerLoader worker;

	private class StickerPageObjects
	{
		private GridView stickerGridView;

		private StickerPageAdapter spa;

		public StickerPageObjects(GridView sgv)
		{
			stickerGridView = sgv;
		}

		public GridView getStickerGridView()
		{
			return stickerGridView;
		}

		public void setStickerPageAdapter(StickerPageAdapter sp)
		{
			this.spa = sp;
		}

		public StickerPageAdapter getStickerPageAdapter()
		{
			return spa;
		}
	}

	public StickerAdapter(Activity activity)
	{
		this.inflater = LayoutInflater.from(activity);
		this.activity = activity;
		instantiateStickerList();
		stickerObjMap = Collections.synchronizedMap(new HashMap<StickerCategory, StickerAdapter.StickerPageObjects>());
		worker = new StickerLoader(activity.getApplicationContext());

		registerListener();
		Logger.d(getClass().getSimpleName(), "Sticker Adapter instantiated ....");
	}

	/**
	 * Utility method for updating the sticker list
	 */
	public void instantiateStickerList()
	{
		this.stickerCategoryList = StickerManager.getInstance().getStickerCategoryList();
	}

	@Override
	public int getCount()
	{
		return stickerCategoryList.size();
	}

	@Override
	public boolean isViewFromObject(View view, Object object)
	{
		return view == object;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object)
	{
		Logger.d(getClass().getSimpleName(), "Item removed from position : " + position);
		((ViewPager) container).removeView((View) object);
		StickerCategory cat = stickerCategoryList.get(position);
		stickerObjMap.remove(cat);
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position)
	{
		View emoticonPage;
		emoticonPage = inflater.inflate(R.layout.sticker_page, null);
		StickerCategory category = stickerCategoryList.get(position);
		Logger.d(getClass().getSimpleName(), "Instantiate View for categpory : " + category.getCategoryId());
		setupStickerPage(emoticonPage, category);

		((ViewPager) container).addView(emoticonPage);
		emoticonPage.setTag(category.getCategoryId());

		return emoticonPage;
	}

	private void registerListener()
	{
		IntentFilter filter = new IntentFilter(StickerManager.STICKERS_DOWNLOADED);
		filter.addAction(StickerManager.STICKERS_FAILED);
		filter.addAction(StickerManager.RECENTS_UPDATED);
		filter.addAction(StickerManager.MORE_STICKERS_DOWNLOADED);
		LocalBroadcastManager.getInstance(activity).registerReceiver(mMessageReceiver, filter);
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent.getAction().equals(StickerManager.RECENTS_UPDATED))
			{
				StickerPageObjects spo = stickerObjMap.get(StickerManager.getInstance().getCategoryForId(StickerManager.RECENT));
				if (spo != null)
				{
					final StickerPageAdapter stickerPageAdapter = spo.getStickerPageAdapter();
					if (stickerPageAdapter != null)
					{
						Sticker st = (Sticker) intent.getSerializableExtra(StickerManager.RECENT_STICKER_SENT);
						if (st != null)
						{
							stickerPageAdapter.updateRecentsList(st);
							activity.runOnUiThread(new Runnable()
							{
								@Override
								public void run()
								{
									if (!isCurrentEmoticonTypeStickers())
									{
										return;
									}

									stickerPageAdapter.notifyDataSetChanged();
								}
							});
						}
					}
				}
			}
			/**
			 * More stickers downloaded case
			 */
			else if(intent.getAction().equals(StickerManager.MORE_STICKERS_DOWNLOADED))
			{
				String categoryId = intent.getStringExtra(StickerManager.CATEGORY_ID);
				final StickerCategory category = StickerManager.getInstance().getCategoryForId(categoryId);
				activity.runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						category.setState(StickerCategory.DONE);
						initStickers(category);
					}
				});
			}
			
			else
			{
				Bundle b = intent.getBundleExtra(StickerManager.STICKER_DATA_BUNDLE);
				final StickerCategory cat = (StickerCategory) b.getSerializable(StickerManager.STICKER_CATEGORY);
				final DownloadType type = (DownloadType) b.getSerializable(StickerManager.STICKER_DOWNLOAD_TYPE);
				final StickerPageObjects spo = stickerObjMap.get(cat);
				final boolean failedDueToLargeFile =b.getBoolean(StickerManager.STICKER_DOWNLOAD_FAILED_FILE_TOO_LARGE);
				// if this category is already loaded then only proceed else ignore
				if (spo != null)
				{
					if (intent.getAction().equals(StickerManager.STICKERS_FAILED) && (DownloadType.NEW_CATEGORY.equals(type) || DownloadType.MORE_STICKERS.equals(type)))
					{
						activity.runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								if (!isCurrentEmoticonTypeStickers())
								{
									return;
								}

								Logger.d(getClass().getSimpleName(), "Download failed for new category " + cat.getCategoryId());
								cat.setState(StickerCategory.RETRY);
								addViewBasedOnState(stickerObjMap.get(cat), cat);
							}
						});
					}
					else if (intent.getAction().equals(StickerManager.STICKERS_DOWNLOADED) && DownloadType.NEW_CATEGORY.equals(type))
					{
						activity.runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								initStickers(spo, cat);
							}
						});
					}
				}
			}
		}
	};

	public void setupStickerPage(final View parent, final StickerCategory category)
	{
		final GridView stickerGridView = (GridView) parent.findViewById(R.id.emoticon_grid);

		ViewGroup emptyView = (ViewGroup) parent.findViewById(R.id.emptyViewHolder);

		checkAndSetEmptyView(parent, emptyView, category, stickerGridView);

		StickerPageObjects spo = new StickerPageObjects(stickerGridView);
		stickerGridView.setNumColumns(StickerManager.getInstance().getNumColumnsForStickerGrid(activity));
		stickerObjMap.put(category, spo);
		initStickers(spo, category);
		
	}
	
	private void checkAndSetEmptyView(final View parent, ViewGroup emptyView, final StickerCategory category, final GridView stickerGridView)
	{
		View empty;
		if (category.isCustom())
		{
			// Set Recents EmptyView
			empty = LayoutInflater.from(activity).inflate(R.layout.recent_empty_view, emptyView);
		}

		else
		{
			// Set Download EmptyView
			empty = LayoutInflater.from(activity).inflate(R.layout.sticker_pack_empty_view, emptyView);
			TextView downloadBtn = (TextView) empty.findViewById(R.id.download_btn);
			TextView categoryName = (TextView) empty.findViewById(R.id.category_name);
			categoryName.setText(category.getCategoryName());
			downloadBtn.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					category.setState(StickerCategory.DOWNLOADING);
					StickerDownloadManager.getInstance(activity).DownloadMultipleStickers(category, DownloadType.NEW_CATEGORY, null, new IStickerResultListener()
					{
						
						@Override
						public void onSuccess(Object result)
						{
							StickerManager.getInstance().sucessFullyDownloadedStickers(result);
						}
						
						@Override
						public void onProgressUpdated(double percentage)
						{
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void onFailure(Object result, Throwable exception)
						{
							StickerManager.getInstance().stickersDownloadFailed(result);
						}
					});
					setupStickerPage(parent, category);

				}
			});

		}

		stickerGridView.setEmptyView(empty);
	}

	private void addViewBasedOnState(StickerPageObjects stickerPageObjects, StickerCategory category)
	{
		StickerPageAdapter spa = stickerPageObjects.getStickerPageAdapter();
		List<StickerPageAdapterItem> stickerPageList = spa.getStickerPageAdapterItemList();
		int state = category.getState();
		stickerPageList.remove(0);
		/* We add UI elements based on the current state of the sticker category*/
		addStickerPageAdapterItem(state, stickerPageList);
		spa.notifyDataSetChanged();
	}
	
	/**
	 * Adds StickerPageAdapter Items to the list passed based on the state of the category
	 * @param state
	 * @param stickerPageList
	 */
	private void addStickerPageAdapterItem(int state, List<StickerPageAdapterItem> stickerPageList)
	{
		switch (state) 
		{
		case StickerCategory.UPDATE :
			stickerPageList.add(0, new StickerPageAdapterItem(StickerPageAdapterItem.UPDATE));
			break;
			
		case StickerCategory.DOWNLOADING :
			stickerPageList.add(0, new StickerPageAdapterItem(StickerPageAdapterItem.DOWNLOADING));
			break;
			
		case StickerCategory.RETRY :
			stickerPageList.add(0, new StickerPageAdapterItem(StickerPageAdapterItem.RETRY));
			break;
			
		case StickerCategory.DONE :
			stickerPageList.add(0, new StickerPageAdapterItem(StickerPageAdapterItem.DONE));
			break;
		}
	}

	/**
	 * This method was added to ensure that the current emoticon type in the chat thread is of sticker type. This was added since there was a case in low end devices where the
	 * palette was dismissed but the sticker's scroll listener still tried to get categories
	 * 
	 * @return
	 */
	private boolean isCurrentEmoticonTypeStickers()
	{
		EmoticonType emoticonType = ((ChatThread) activity).getCurrentEmoticonType();
		if (emoticonType != EmoticonType.STICKERS)
		{
			return false;
		}
		return true;
	}

	public void initStickers(StickerCategory category)
	{
		StickerPageObjects spo = stickerObjMap.get(category);
		initStickers(spo, category);
	}
	
	private void initStickers(StickerPageObjects spo, final StickerCategory category)
	{
		if (!isCurrentEmoticonTypeStickers())
		{
			return;
		}

		spo.getStickerGridView().setVisibility(View.VISIBLE);
		final List<Sticker> stickersList = category.getStickerList(activity);
		final List<StickerPageAdapterItem> stickerPageList = generateStickerPageAdapterItemList(stickersList);
		
		int state = category.getState(); 
		/* We add UI elements based on the current state of the sticker category*/
		addStickerPageAdapterItem(state, stickerPageList);
		/**
		 * Adding the placeholders in 0 sticker case in pallete. The placeholders will be added when state is either downloading or retry.
		 */
		if(stickersList.size() == 0 && (state == StickerCategory.DOWNLOADING || state == StickerCategory.RETRY))
		{
			int totalPlaceHolders = 2 * StickerManager.getInstance().getNumColumnsForStickerGrid(activity) - 1;
			while(totalPlaceHolders > 0)
			{
				stickerPageList.add(new StickerPageAdapterItem(StickerPageAdapterItem.PLACE_HOLDER));
				totalPlaceHolders --;
			}
		}
		/**
		 * If StickerPageAdapter is already initialised, we clear the prev list and add new items
		 */
		if(spo.getStickerPageAdapter() != null)
		{
			StickerPageAdapter stickerPageAdapter = spo.getStickerPageAdapter();
			stickerPageAdapter.getStickerPageAdapterItemList().clear();
			stickerPageAdapter.getStickerPageAdapterItemList().addAll(stickerPageList);
			stickerPageAdapter.notifyDataSetChanged();
		}
		else
		{
			final StickerPageAdapter stickerPageAdapter = new StickerPageAdapter(activity, stickerPageList, category, worker);
			spo.setStickerPageAdapter(stickerPageAdapter);
			spo.getStickerGridView().setAdapter(stickerPageAdapter);
		}
	}
	
	private List<StickerPageAdapterItem> generateStickerPageAdapterItemList(List<Sticker> stickersList)
	{
		List<StickerPageAdapterItem> stickerPageList = new ArrayList<StickerPageAdapterItem>();
		if(stickersList != null)
		{
			for (Sticker st : stickersList)
			{
				stickerPageList.add(new StickerPageAdapterItem(StickerPageAdapterItem.STICKER, st));
			}
		}
		return stickerPageList;
	}

	@Override
	public int getIconResId(int index)
	{
		// TODO need to remove this hardcoded drawable usage. we should actually use pallate icons
		// saved in folders of each category.
		return R.drawable.recents;//stickerCategoryList.get(index).categoryId.resId();
	}

	@Override
	public boolean isUpdateAvailable(int index)
	{
		return stickerCategoryList.get(index).isUpdateAvailable();
	}

	public void unregisterListeners()
	{
		LocalBroadcastManager.getInstance(activity).unregisterReceiver(mMessageReceiver);
	}
	
	public StickerLoader getStickerLoader()
	{
		return worker;
	}

	@Override
	public StateListDrawable getPalleteIconDrawable(int index)
	{
		StickerCategory category = stickerCategoryList.get(index);
		return StickerManager.getStateListDrawableForStickerPalette(activity, category.getCategoryId());
	}
	
	/**
	 * Returns Sticker Category object based on position
	 * @param position
	 * @return
	 */
	public StickerCategory getStickerCategory(int position)
	{
		return stickerCategoryList.get(position);
	}
}
