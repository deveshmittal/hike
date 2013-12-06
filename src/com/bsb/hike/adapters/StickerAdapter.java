package com.bsb.hike.adapters;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
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
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.StickerPageAdapter.ViewType;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.tasks.DownloadStickerTask;
import com.bsb.hike.tasks.DownloadStickerTask.DownloadType;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.StickerManager.StickerCategoryId;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.StickerEmoticonIconPageIndicator.StickerEmoticonIconPagerAdapter;

public class StickerAdapter extends PagerAdapter implements
		StickerEmoticonIconPagerAdapter {

	private List<StickerCategory> stickerCategoryList;
	private LayoutInflater inflater;
	private Activity activity;

	public StickerAdapter(Activity activity, boolean isPortrait) {
		this.inflater = LayoutInflater.from(activity);
		this.activity = activity;
		stickerCategoryList = StickerManager.getInstance().getStickerCategoryList();
	}

	@Override
	public int getCount() {
		return stickerCategoryList.size();
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == object;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		((ViewPager) container).removeView((View) object);
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		View emoticonPage;
		emoticonPage = inflater.inflate(R.layout.sticker_page, null);
		StickerCategory category = StickerManager.getInstance().getCategoryForIndex(position);
		setupStickerPage(emoticonPage, category, false, null);

		((ViewPager) container).addView(emoticonPage);
		emoticonPage.setTag(category.categoryId);

		return emoticonPage;
	}

	public void setupStickerPage(final View parent, final StickerCategory category, boolean failed, final DownloadType downloadTypeBeforeFail)
	{

		final ListView stickerList = (ListView) parent.findViewById(R.id.emoticon_grid);

		View downloadingParent = parent.findViewById(R.id.downloading_container);
		final TextView downloadingText = (TextView) parent.findViewById(R.id.downloading_sticker);

		Button downloadingFailed = (Button) parent.findViewById(R.id.sticker_fail_btn);

		stickerList.setVisibility(View.GONE);
		downloadingParent.setVisibility(View.GONE);
		downloadingFailed.setVisibility(View.GONE);

		final DownloadStickerTask currentStickerTask = (DownloadStickerTask) StickerManager.getInstance().getTask(category.categoryId.name());

		if (currentStickerTask != null && currentStickerTask.getDownloadType().equals(DownloadType.NEW_CATEGORY))
		{

			Log.d(getClass().getSimpleName(), "Downloading new category " + category.categoryId.name());

			downloadingParent.setVisibility(View.VISIBLE);

			downloadingText.setText(activity.getString(R.string.downloading_category, category.categoryId.name()));
		}
		else if (failed && downloadTypeBeforeFail == DownloadType.NEW_CATEGORY)
		{

			Log.d(getClass().getSimpleName(), "Download failed for new category " + category.categoryId.name());

			downloadingFailed.setVisibility(View.VISIBLE);

			downloadingFailed.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					DownloadStickerTask downloadStickerTask = new DownloadStickerTask(activity, category, downloadTypeBeforeFail,null);
					Utils.executeFtResultAsyncTask(downloadStickerTask);

					StickerManager.getInstance().insertTask(category.categoryId.name(), downloadStickerTask);
					downloadingText.setText(activity.getString(R.string.downloading_category, category.categoryId.name()));
					//setupStickerPage(parent, category, false, null);
				}
			});
		}
		else
		{
			stickerList.setVisibility(View.VISIBLE);
			final ArrayList<Sticker> stickersList;
			if (category.categoryId.equals(StickerCategoryId.recent))
			{
				Object[] lhs = StickerManager.getInstance().getRecentStickerList().toArray();
				int size = lhs.length;
				stickersList = new ArrayList<Sticker>(size);
				for (int i = size - 1; i >= 0; i--)
				{
					try
					{
						Sticker st = (Sticker) lhs[i];
						stickersList.add(st);
					}
					catch (Exception e)
					{
						Log.e(getClass().getSimpleName(), "Exception in recent stickers", e);
					}
				}
			}
			else
			{

				long t1 = System.currentTimeMillis();
				stickersList = new ArrayList<Sticker>();
				if (category.categoryId.equals(StickerCategoryId.doggy))
				{
					addDefaultStickers(stickersList, category, StickerManager.getInstance().LOCAL_STICKER_IDS_DOGGY);
				}
				else if (category.categoryId.equals(StickerCategoryId.humanoid))
				{
					addDefaultStickers(stickersList, category, StickerManager.getInstance().LOCAL_STICKER_IDS_HUMANOID);
				}

				String categoryDirPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(activity, category.categoryId.name());

				if (categoryDirPath != null)
				{
					File categoryDir = new File(categoryDirPath + HikeConstants.SMALL_STICKER_ROOT);

					if (categoryDir.exists())
					{
						String[] stickerIds = categoryDir.list();
						for (String stickerId : stickerIds)
						{
							stickersList.add(new Sticker(category, stickerId));
						}
					}
				}
				Collections.sort(stickersList);
				long t2 = System.currentTimeMillis();
				Log.d(getClass().getSimpleName(), "Time to sort category : " + category.categoryId + " in ms : " + (t2 - t1));
			}
	
			boolean updateAvailable = category.updateAvailable;
			final List<ViewType> viewTypeList = new ArrayList<StickerPageAdapter.ViewType>();
			if (updateAvailable || (currentStickerTask != null && currentStickerTask.getDownloadType() == DownloadType.UPDATE))
			{
				viewTypeList.add(ViewType.UPDATING_STICKER);
				updateAvailable = true;
			}
			if (currentStickerTask != null && currentStickerTask.getDownloadType() == DownloadType.MORE_STICKERS)
			{
				viewTypeList.add(ViewType.DOWNLOADING_MORE);
			}
			final StickerPageAdapter stickerPageAdapter = new StickerPageAdapter(activity, stickersList, category, viewTypeList);
			
			stickerList.setAdapter(stickerPageAdapter);
			stickerList.setOnScrollListener(new OnScrollListener()
			{

				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState)
				{
				}

				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
				{
					int currentIdx = ((ChatThread) activity).getCurrentPage();
					StickerCategory sc = StickerManager.getInstance().getCategoryForIndex(currentIdx);
					if (stickersList.isEmpty() || !category.categoryId.equals(sc.categoryId)
							|| !activity.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(category.categoryId.downloadPref(), false)
							|| category.hasReachedEnd())
					{
						return;
					}
					if (!StickerManager.getInstance().isStickerDownloading(category.categoryId.name()) && !category.hasReachedEnd())
					{
						if (firstVisibleItem + visibleItemCount >= totalItemCount - 1)
						{
							Log.d(getClass().getSimpleName(), "Downloading more stickers " + category.categoryId.name());
							viewTypeList.add(ViewType.DOWNLOADING_MORE);
							DownloadStickerTask downloadStickerTask = new DownloadStickerTask(activity, category, DownloadType.MORE_STICKERS,stickerPageAdapter);

							StickerManager.getInstance().insertTask(category.categoryId.name(), downloadStickerTask);
							stickerPageAdapter.notifyDataSetChanged();
							Utils.executeFtResultAsyncTask(downloadStickerTask);
						}
					}
				}
			});

		}
	}

	private void addDefaultStickers(List<Sticker> stickerList, StickerCategory cat, String[] stickerIds)
	{
		for (int i = 0; i < stickerIds.length; i++)
		{
			stickerList.add(new Sticker(cat, stickerIds[i], i));
		}
	}

	@Override
	public int getIconResId(int index) {
		return stickerCategoryList.get(index).categoryId.resId();
	}

	@Override
	public boolean isUpdateAvailable(int index) {
		return stickerCategoryList.get(index).updateAvailable;
	}
}
