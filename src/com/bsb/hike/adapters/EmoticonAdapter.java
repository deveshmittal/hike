package com.bsb.hike.adapters;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.LayoutParams;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.StickerAdapter.ViewType;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.tasks.DownloadStickerTask;
import com.bsb.hike.tasks.DownloadStickerTask.DownloadType;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

public class EmoticonAdapter extends PagerAdapter implements
		OnItemClickListener {

	public enum EmoticonType {
		HIKE_EMOTICON, EMOJI, STICKERS
	}

	public final int MAX_EMOTICONS_PER_ROW;

	public static final int MAX_EMOTICONS_PER_ROW_PORTRAIT = 7;

	public static final int MAX_EMOTICONS_PER_ROW_LANDSCAPE = 10;

	public static final int RECENTS_SUBCATEGORY_INDEX = -1;

	private int EMOTICON_NUM_PAGES;

	private LayoutInflater inflater;
	private Activity activity;
	private EditText composeBox;
	private int[] recentEmoticons;
	private int[] emoticonResIds;
	private int[] emoticonSubCategories;
	private EmoticonType emoticonType;
	private int idOffset;

	private final int EMOTICON_SIZE = (int) (27 * Utils.densityMultiplier);

	public EmoticonAdapter(Activity activity, EditText composeBox,
			EmoticonType emoticonType, boolean isPortrait) {
		MAX_EMOTICONS_PER_ROW = isPortrait ? MAX_EMOTICONS_PER_ROW_PORTRAIT
				: MAX_EMOTICONS_PER_ROW_LANDSCAPE;

		this.inflater = LayoutInflater.from(activity);
		this.activity = activity;
		this.composeBox = composeBox;
		this.emoticonType = emoticonType;

		switch (emoticonType) {
		// Incrementing these numbers to show a recents tab as well.
		case HIKE_EMOTICON:
			emoticonResIds = EmoticonConstants.DEFAULT_SMILEY_RES_IDS;
			emoticonSubCategories = SmileyParser.HIKE_EMOTICONS_SUBCATEGORIES;
			idOffset = 0;
			break;
		case EMOJI:
			emoticonResIds = EmoticonConstants.EMOJI_RES_IDS;
			emoticonSubCategories = SmileyParser.EMOJI_SUBCATEGORIES;
			idOffset = EmoticonConstants.DEFAULT_SMILEY_RES_IDS.length;
			break;
		case STICKERS:
			emoticonResIds = EmoticonConstants.LOCAL_STICKER_RES_IDS;
			emoticonSubCategories = null;
			idOffset = 0;
		}

		EMOTICON_NUM_PAGES = calculateNumPages(emoticonType);
	}

	private int calculateNumPages(EmoticonType emoticonType) {
		switch (emoticonType) {
		case EMOJI:
			return SmileyParser.EMOJI_SUBCATEGORIES.length + 1;
		case HIKE_EMOTICON:
			return SmileyParser.HIKE_EMOTICONS_SUBCATEGORIES.length + 1;
		case STICKERS:
			return HikeMessengerApp.stickerCategories.size() - 1;
		}
		return 0;
	}

	@Override
	public int getCount() {
		return EMOTICON_NUM_PAGES;
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
		if (emoticonType != EmoticonType.STICKERS) {
			emoticonPage = inflater
					.inflate(
							emoticonType == EmoticonType.STICKERS ? R.layout.sticker_page
									: R.layout.emoticon_page, null);

			GridView emoticonGrid = (GridView) emoticonPage
					.findViewById(R.id.emoticon_grid);
			emoticonGrid.setNumColumns(MAX_EMOTICONS_PER_ROW);
			emoticonGrid.setVerticalScrollBarEnabled(false);
			emoticonGrid.setHorizontalScrollBarEnabled(false);
			emoticonGrid.setAdapter(new EmoticonPageAdapter(position));
			emoticonGrid.setOnItemClickListener(this);

			((ViewPager) container).addView(emoticonPage);
		} else {
			emoticonPage = inflater.inflate(R.layout.sticker_page, null);

			setupStickerPage(emoticonPage, position, false, null);

			((ViewPager) container).addView(emoticonPage);
			emoticonPage.setTag(Utils.getCategoryIdForIndex(position));
		}
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

		final DownloadStickerTask currentStickerTask = (DownloadStickerTask) ChatThread.stickerTaskMap
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
					downloadStickerTask.execute();

					ChatThread.stickerTaskMap.put(categoryId,
							downloadStickerTask);
					setupStickerPage(parent, position, false, null);
				}
			});
		} else {
			stickerList.setVisibility(View.VISIBLE);

			new AsyncTask<Void, Void, List<Sticker>>() {

				boolean updateAvailable;

				@Override
				protected List<Sticker> doInBackground(Void... params) {
					List<Sticker> stickerList = new ArrayList<Sticker>();
					if (position == 0) {
						for (int i = 0; i < EmoticonConstants.LOCAL_STICKER_IDS.length; i++) {
							stickerList.add(new Sticker(position,
									EmoticonConstants.LOCAL_STICKER_IDS[i], i));
						}
					}
					File categoryDir = new File(
							Utils.getExternalStickerDirectoryForCatgoryId(
									activity, categoryId));

					if (categoryDir.exists()) {
						String[] stickerIds = categoryDir.list();
						for (String stickerId : stickerIds) {
							stickerList.add(new Sticker(position, stickerId));
						}

						HikeConversationsDatabase hCDB = HikeConversationsDatabase
								.getInstance();
						updateAvailable = hCDB
								.isStickerUpdateAvailable(categoryId);
					}

					Collections.sort(stickerList);

					return stickerList;
				}

				@Override
				protected void onPostExecute(List<Sticker> result) {
					final List<ViewType> viewTypeList = new ArrayList<StickerAdapter.ViewType>();
					if (updateAvailable
							|| (currentStickerTask != null && currentStickerTask
									.getDownloadType() == DownloadType.UPDATE)) {
						viewTypeList.add(ViewType.UPDATING_STICKER);
					}
					int numItemsRow = activity.getResources()
							.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? StickerAdapter.MAX_STICKER_PER_ROW_PORTRAIT
							: StickerAdapter.MAX_STICKER_PER_ROW_LANDSCAPE;

					int numStickerRows = 0;
					if (!result.isEmpty()) {
						int stickerNum = result.size();
						if (stickerNum % numItemsRow == 0) {
							numStickerRows = stickerNum / numItemsRow;
						} else {
							numStickerRows = stickerNum / numItemsRow + 1;
						}

						for (int i = 0; i < numStickerRows; i++) {
							viewTypeList.add(ViewType.STICKER);
						}
					}
					if (currentStickerTask != null
							&& currentStickerTask.getDownloadType() == DownloadType.MORE_STICKERS) {
						viewTypeList.add(ViewType.DOWNLOADING_MORE);
					}
					final StickerAdapter stickerAdapter = new StickerAdapter(
							activity, result, viewTypeList, position,
							numItemsRow, numStickerRows, updateAvailable);

					stickerList.setAdapter(stickerAdapter);
					stickerList.setOnScrollListener(new OnScrollListener() {

						@Override
						public void onScrollStateChanged(AbsListView view,
								int scrollState) {
						}

						@Override
						public void onScroll(AbsListView view,
								int firstVisibleItem, int visibleItemCount,
								int totalItemCount) {
							Log.d(getClass().getSimpleName(), "reached end? "
									+ HikeConversationsDatabase.getInstance()
											.hasReachedStickerEnd(categoryId));
							if (position == 0
									|| ((ChatThread) activity).getCurrentPage() != position
									|| HikeConversationsDatabase.getInstance()
											.hasReachedStickerEnd(categoryId)) {
								return;
							}
							if (!ChatThread.stickerTaskMap
									.containsKey(categoryId)) {
								if (firstVisibleItem + visibleItemCount >= totalItemCount - 1) {
									Log.d(getClass().getSimpleName(),
											"Downloading more stickers "
													+ categoryId);
									viewTypeList.add(ViewType.DOWNLOADING_MORE);

									DownloadStickerTask downloadStickerTask = new DownloadStickerTask(
											activity, position,
											DownloadType.MORE_STICKERS);
									downloadStickerTask.execute();

									ChatThread.stickerTaskMap.put(categoryId,
											downloadStickerTask);
									stickerAdapter.notifyDataSetChanged();
								}
							}
						}
					});
				}

			}.execute();
		}
	}

	private class EmoticonPageAdapter extends BaseAdapter {

		int currentPage;
		int offset;
		LayoutInflater inflater;

		public EmoticonPageAdapter(int currentPage) {
			this.currentPage = currentPage;
			this.inflater = LayoutInflater.from(activity);

			/*
			 * There will be a positive offset for subcategories having a
			 * greater than 1 index.
			 */
			if (currentPage > 1) {
				for (int i = 0; i <= currentPage - 2; i++) {
					this.offset += emoticonSubCategories[i];
				}
			} else if (currentPage == 0) {
				int startOffset = idOffset;
				int endOffset = startOffset + emoticonResIds.length;

				recentEmoticons = HikeConversationsDatabase.getInstance()
						.fetchEmoticonsOfType(emoticonType, startOffset,
								endOffset, -1);
			}
		}

		@Override
		public int getCount() {
			if (currentPage == 0) {
				return recentEmoticons.length;
			} else {
				return emoticonSubCategories[currentPage - 1];
			}
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.emoticon_item, null);
			}

			LayoutParams lp;

			lp = new LayoutParams(EMOTICON_SIZE, EMOTICON_SIZE);

			convertView.setLayoutParams(lp);
			if (currentPage == 0) {
				convertView.setTag(Integer.valueOf(idOffset
						+ recentEmoticons[position]));
				((ImageView) convertView)
						.setImageResource(emoticonResIds[recentEmoticons[position]]);
			} else {
				convertView.setTag(Integer
						.valueOf(idOffset + offset + position));
				((ImageView) convertView)
						.setImageResource(emoticonResIds[offset + position]);
			}
			return convertView;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		int emoticonIndex = (Integer) arg1.getTag();
		HikeConversationsDatabase.getInstance().updateRecencyOfEmoticon(
				emoticonIndex, System.currentTimeMillis());
		// We don't add an emoticon if the compose box is near its maximum
		// length of characters
		if (composeBox.length() >= activity.getResources().getInteger(
				R.integer.max_length_message) - 20) {
			return;
		}
		SmileyParser.getInstance().addSmiley(composeBox, emoticonIndex);
	}
}
