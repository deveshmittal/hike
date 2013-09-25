package com.bsb.hike.adapters;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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
		stickerCategoryList = HikeMessengerApp.stickerCategories;
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

		setupStickerPage(emoticonPage, position, false, null);

		((ViewPager) container).addView(emoticonPage);
		emoticonPage.setTag(Utils.getCategoryIdForIndex(position));

		return emoticonPage;
	}

	public void setupStickerPage(final View parent, final int position,
			boolean failed, final DownloadType downloadTypeBeforeFail) {

		final ListView stickerList = (ListView) parent
				.findViewById(R.id.emoticon_grid);

		View downloadingParent = parent
				.findViewById(R.id.downloading_container);
		TextView downloadingText = (TextView) parent
				.findViewById(R.id.downloading_sticker);

		Button downloadingFailed = (Button) parent
				.findViewById(R.id.sticker_fail_btn);

		stickerList.setVisibility(View.GONE);
		downloadingParent.setVisibility(View.GONE);
		downloadingFailed.setVisibility(View.GONE);

		final String categoryId = Utils.getCategoryIdForIndex(position);

		final DownloadStickerTask currentStickerTask = (DownloadStickerTask) HikeMessengerApp.stickerTaskMap
				.get(categoryId);

		if (currentStickerTask != null
				&& currentStickerTask.getDownloadType() == DownloadType.NEW_CATEGORY) {

			Log.d(getClass().getSimpleName(), "Downloading new category "
					+ categoryId);

			downloadingParent.setVisibility(View.VISIBLE);

			downloadingText.setText(activity.getString(
					R.string.downloading_category, categoryId));
		} else if (failed
				&& downloadTypeBeforeFail == DownloadType.NEW_CATEGORY) {

			Log.d(getClass().getSimpleName(),
					"Download failed for new category " + categoryId);

			downloadingFailed.setVisibility(View.VISIBLE);

			downloadingFailed.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					DownloadStickerTask downloadStickerTask = new DownloadStickerTask(
							activity, position, downloadTypeBeforeFail);
					Utils.executeFtResultAsyncTask(downloadStickerTask);

					HikeMessengerApp.stickerTaskMap.put(categoryId,
							downloadStickerTask);
					setupStickerPage(parent, position, false, null);
				}
			});
		} else {
			stickerList.setVisibility(View.VISIBLE);

			AsyncTask<Void, Void, List<Sticker>> stickerTask = new AsyncTask<Void, Void, List<Sticker>>() {

				boolean updateAvailable;

				@Override
				protected List<Sticker> doInBackground(Void... params) {
					List<Sticker> stickerList = new ArrayList<Sticker>();

					if (position == 1) {
						addDefaultStickers(stickerList,
								EmoticonConstants.LOCAL_STICKER_IDS_2);
					} else if (position == 0) {
						addDefaultStickers(stickerList,
								EmoticonConstants.LOCAL_STICKER_IDS_1);
					}

					String categoryDirPath = Utils
							.getStickerDirectoryForCategoryId(activity,
									categoryId);

					if (categoryDirPath != null) {
						File categoryDir = new File(categoryDirPath
								+ HikeConstants.SMALL_STICKER_ROOT);

						if (categoryDir.exists()) {
							String[] stickerIds = categoryDir.list();
							for (String stickerId : stickerIds) {
								stickerList
										.add(new Sticker(position, stickerId));
							}

						}
					}
					updateAvailable = stickerCategoryList.get(position).updateAvailable;

					Collections.sort(stickerList);

					return stickerList;
				}

				private void addDefaultStickers(List<Sticker> stickerList,
						String[] stickerIds) {
					for (int i = 0; i < stickerIds.length; i++) {
						stickerList
								.add(new Sticker(position, stickerIds[i], i));
					}
				}

				@Override
				protected void onPostExecute(final List<Sticker> result) {
					final List<ViewType> viewTypeList = new ArrayList<StickerPageAdapter.ViewType>();
					if (updateAvailable
							|| (currentStickerTask != null && currentStickerTask
									.getDownloadType() == DownloadType.UPDATE)) {
						viewTypeList.add(ViewType.UPDATING_STICKER);
						updateAvailable = true;
					}
					if (currentStickerTask != null
							&& currentStickerTask.getDownloadType() == DownloadType.MORE_STICKERS) {
						viewTypeList.add(ViewType.DOWNLOADING_MORE);
					}
					final StickerPageAdapter stickerPageAdapter = new StickerPageAdapter(
							activity, result, viewTypeList, position,
							updateAvailable);

					stickerList.setAdapter(stickerPageAdapter);
					stickerList.setOnScrollListener(new OnScrollListener() {

						@Override
						public void onScrollStateChanged(AbsListView view,
								int scrollState) {
						}

						@Override
						public void onScroll(AbsListView view,
								int firstVisibleItem, int visibleItemCount,
								int totalItemCount) {
							if (result.isEmpty()
									|| ((ChatThread) activity).getCurrentPage() != position
									|| !activity
											.getSharedPreferences(
													HikeMessengerApp.ACCOUNT_SETTINGS,
													0)
											.getBoolean(
													EmoticonConstants.STICKER_DOWNLOAD_PREF[position],
													false)
									|| HikeConversationsDatabase.getInstance()
											.hasReachedStickerEnd(categoryId)) {
								return;
							}
							if (!HikeMessengerApp.stickerTaskMap
									.containsKey(categoryId)) {
								if (firstVisibleItem + visibleItemCount >= totalItemCount - 1) {
									Log.d(getClass().getSimpleName(),
											"Downloading more stickers "
													+ categoryId);
									viewTypeList.add(ViewType.DOWNLOADING_MORE);

									DownloadStickerTask downloadStickerTask = new DownloadStickerTask(
											activity, position,
											DownloadType.MORE_STICKERS);
									Utils.executeFtResultAsyncTask(downloadStickerTask);

									HikeMessengerApp.stickerTaskMap.put(
											categoryId, downloadStickerTask);
									stickerPageAdapter.notifyDataSetChanged();
								}
							}
						}
					});
				}

			};
			if (Utils.isHoneycombOrHigher()) {
				stickerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				stickerTask.execute();
			}
		}
	}

	@Override
	public int getIconResId(int index) {
		return stickerCategoryList.get(index).categoryResId;
	}

	@Override
	public boolean isUpdateAvailable(int index) {
		return stickerCategoryList.get(index).updateAvailable;
	}
}
