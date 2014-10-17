package com.bsb.hike.adapters;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.EmoticonType;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.StickerPageAdapter.ViewType;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.smartImageLoader.StickerLoader;
import com.bsb.hike.tasks.DownloadStickerTask;
import com.bsb.hike.tasks.DownloadStickerTask.DownloadType;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
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
		private ListView stickerListView;

		private View downloadingParent;

		private TextView downloadingText;

		private Button downloadingFailed;

		private StickerPageAdapter spa;

		public StickerPageObjects(ListView slv, View dp, TextView dt, Button df)
		{
			stickerListView = slv;
			downloadingParent = dp;
			downloadingText = dt;
			downloadingFailed = df;
		}

		public ListView getStickerListView()
		{
			return stickerListView;
		}

		public View getDownloadingParent()
		{
			return downloadingParent;
		}

		public TextView getDownloadingText()
		{
			return downloadingText;
		}

		public Button getDownloadingFailedButton()
		{
			return downloadingFailed;
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

	public StickerAdapter(Activity activity, boolean isPortrait)
	{
		this.inflater = LayoutInflater.from(activity);
		this.activity = activity;
		stickerCategoryList = StickerManager.getInstance().getStickerCategoryList();
		stickerObjMap = Collections.synchronizedMap(new HashMap<StickerCategory, StickerAdapter.StickerPageObjects>());
		worker = new StickerLoader(activity.getApplicationContext());

		registerListener();
		Logger.d(getClass().getSimpleName(), "Sticker Adapter instantiated ....");
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
		StickerCategory cat = StickerManager.getInstance().getCategoryForIndex(position);
		stickerObjMap.remove(cat);
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position)
	{
		View emoticonPage;
		emoticonPage = inflater.inflate(R.layout.sticker_page, null);
		StickerCategory category = StickerManager.getInstance().getCategoryForIndex(position);
		Logger.d(getClass().getSimpleName(), "Instantiate View for categpory : " + category.getCategoryId());
		setupStickerPage(emoticonPage, category, false, null);

		((ViewPager) container).addView(emoticonPage);
		emoticonPage.setTag(category.getCategoryId());

		return emoticonPage;
	}

	private void registerListener()
	{
		IntentFilter filter = new IntentFilter(StickerManager.STICKERS_DOWNLOADED);
		filter.addAction(StickerManager.STICKERS_FAILED);
		filter.addAction(StickerManager.RECENTS_UPDATED);
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
					if (intent.getAction().equals(StickerManager.STICKERS_FAILED) && DownloadType.NEW_CATEGORY.equals(type))
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

								spo.getDownloadingParent().setVisibility(View.GONE);
								spo.getStickerListView().setVisibility(View.GONE);
								spo.getDownloadingFailedButton().setVisibility(View.VISIBLE);
								if(failedDueToLargeFile)
								{
									spo.getDownloadingFailedButton().setText(R.string.sticker_download_failed_large_file);
								}
								spo.getDownloadingFailedButton().setOnClickListener(new OnClickListener()
								{
									@Override
									public void onClick(View v)
									{
										DownloadStickerTask downloadStickerTask = new DownloadStickerTask(activity, cat, type, null);
										Utils.executeFtResultAsyncTask(downloadStickerTask);

										StickerManager.getInstance().insertTask(cat.getCategoryId(), downloadStickerTask);
										spo.getDownloadingText().setText(activity.getString(R.string.downloading_category, cat.getCategoryId()));
									}
								});

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

	public void setupStickerPage(final View parent, final StickerCategory category, boolean failed, final DownloadType downloadTypeBeforeFail)
	{
		final ListView stickerListView = (ListView) parent.findViewById(R.id.emoticon_grid);

		View downloadingParent = parent.findViewById(R.id.downloading_container);
		final TextView downloadingText = (TextView) parent.findViewById(R.id.downloading_sticker);

		Button downloadingFailed = (Button) parent.findViewById(R.id.sticker_fail_btn);

		stickerListView.setVisibility(View.GONE);
		downloadingParent.setVisibility(View.GONE);
		downloadingFailed.setVisibility(View.GONE);

		StickerPageObjects spo = new StickerPageObjects(stickerListView, downloadingParent, downloadingText, downloadingFailed);
		stickerObjMap.put(category, spo);
		DownloadStickerTask currentStickerTask = (DownloadStickerTask) StickerManager.getInstance().getTask(category.getCategoryId());

		if (currentStickerTask != null && currentStickerTask.getDownloadType().equals(DownloadType.NEW_CATEGORY))
		{
			Logger.d(getClass().getSimpleName(), "Downloading new category " + category.getCategoryId());

			downloadingParent.setVisibility(View.VISIBLE);

			downloadingText.setText(activity.getString(R.string.downloading_category, category.getCategoryId()));
		}
		else
		{
			initStickers(spo, category);
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

	private void initStickers(StickerPageObjects spo, final StickerCategory category)
	{
		if (!isCurrentEmoticonTypeStickers())
		{
			return;
		}

		spo.getDownloadingParent().setVisibility(View.GONE);
		spo.getDownloadingFailedButton().setVisibility(View.GONE);
		spo.getStickerListView().setVisibility(View.VISIBLE);
		final List<Sticker> stickersList;
		if (category.isCustom())
		{
			//right now only recent category is custom
			Set<Sticker> lhs = StickerManager.getInstance().getRecentStickerList();

			/*
			 * here using LinkedList as in recents we have to remove the sticker frequently to move it to front and in linked list 
			 * remove operation is faster compared to arraylist
			 */
			stickersList = new LinkedList<Sticker>();
			Iterator<Sticker> it = lhs.iterator();
			while (it.hasNext())
			{
				try
				{
					Sticker st = (Sticker) it.next();
					stickersList.add(0, st);
				}
				catch (Exception e)
				{
					Logger.e(getClass().getSimpleName(), "Exception in recent stickers", e);
				}
			}
		}
		else
		{

			long t1 = System.currentTimeMillis();
			stickersList = new ArrayList<Sticker>();

			String categoryDirPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(activity, category.getCategoryId());

			if (categoryDirPath != null)
			{
				File categoryDir = new File(categoryDirPath + HikeConstants.SMALL_STICKER_ROOT);

				if (categoryDir.exists())
				{
					String[] stickerIds = categoryDir.list(StickerManager.getInstance().stickerFileFilter);
					for (String stickerId : stickerIds)
					{
						Sticker s = new Sticker(category, stickerId);
						stickersList.add(s);
					}
				}
			}
			Collections.sort(stickersList);
			long t2 = System.currentTimeMillis();
			Logger.d(getClass().getSimpleName(), "Time to sort category : " + category.getCategoryId() + " in ms : " + (t2 - t1));
		}

		boolean updateAvailable = category.isUpdateAvailable();

		final List<ViewType> viewTypeList = new ArrayList<StickerPageAdapter.ViewType>();
		final DownloadStickerTask currentStickerTask = (DownloadStickerTask) StickerManager.getInstance().getTask(category.getCategoryId());
		if (updateAvailable || (currentStickerTask != null && currentStickerTask.getDownloadType() == DownloadType.UPDATE))
		{
			viewTypeList.add(ViewType.UPDATING_STICKER);
			updateAvailable = true;
		}
		if (currentStickerTask != null && currentStickerTask.getDownloadType() == DownloadType.MORE_STICKERS)
		{
			viewTypeList.add(ViewType.DOWNLOADING_MORE);
		}
		final StickerPageAdapter stickerPageAdapter = new StickerPageAdapter(activity, stickersList, category, viewTypeList, worker);
		spo.setStickerPageAdapter(stickerPageAdapter);
		spo.getStickerListView().setAdapter(stickerPageAdapter);
		spo.getStickerListView().setOnScrollListener(new OnScrollListener()
		{
			private int previousFirstVisibleItem;

			private long previousEventTime;

			private int velocity;

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState)
			{
				/*
				 * Only set flinging true if the list is actually flinging and the velocity is greater than 10.
				 */
				stickerPageAdapter.setIsListFlinging(scrollState == SCROLL_STATE_FLING && velocity > HikeConstants.MAX_VELOCITY_FOR_LOADING_IMAGES);
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
			{
				if (previousFirstVisibleItem != firstVisibleItem)
				{
					long currTime = System.currentTimeMillis();
					long timeToScrollOneElement = currTime - previousEventTime;
					velocity = (int) (((double) 1 / timeToScrollOneElement) * 1000);

					previousFirstVisibleItem = firstVisibleItem;
					previousEventTime = currTime;
				}

				int currentIdx = ((ChatThread) activity).getCurrentPage();
				if (currentIdx == -1)
				{
					return;
				}

				StickerCategory sc = StickerManager.getInstance().getCategoryForIndex(currentIdx);
				if (stickersList.isEmpty() || !category.getCategoryId().equals(sc.getCategoryId()))
				{
					return;
				}
				if (!StickerManager.getInstance().isStickerDownloading(category.getCategoryId()) && !category.isUpdateAvailable())
				{
					if (firstVisibleItem + visibleItemCount >= totalItemCount - 1)
					{
						Logger.d(getClass().getSimpleName(), "Downloading more stickers " + category.getCategoryId());
						// if downloading more is not already inserted, then only insert that view
						if (!viewTypeList.get(viewTypeList.size() - 1).equals(ViewType.DOWNLOADING_MORE))
							viewTypeList.add(ViewType.DOWNLOADING_MORE);
						DownloadStickerTask downloadStickerTask = new DownloadStickerTask(activity, category, DownloadType.MORE_STICKERS, stickerPageAdapter);

						StickerManager.getInstance().insertTask(category.getCategoryId(), downloadStickerTask);
						stickerPageAdapter.notifyDataSetChanged();
						Utils.executeFtResultAsyncTask(downloadStickerTask);
					}
				}
			}
		});

	}

	private void addDefaultStickers(List<Sticker> stickerList, StickerCategory cat, String[] stickerIds)
	{
		int count = stickerIds.length;
		for (int i = 0; i < count; i++)
		{
			stickerList.add(new Sticker(cat, stickerIds[i]));
		}
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
}
