package com.bsb.hike.adapters;

import java.util.List;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.tasks.DownloadStickerTask;
import com.bsb.hike.tasks.DownloadStickerTask.DownloadType;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.Utils;

public class StickerPageAdapter extends BaseAdapter implements OnClickListener {

	public static final int MAX_STICKER_PER_ROW_PORTRAIT = 4;

	public static final int MAX_STICKER_PER_ROW_LANDSCAPE = 6;

	public static enum ViewType {
		STICKER, UPDATING_STICKER, DOWNLOADING_MORE
	}

	public static final int SIZE_IMAGE = (int) (80 * Utils.densityMultiplier);

	private int numItemsRow;
	private int sizeEachImage;
	private Activity activity;
	private int numStickers;
	private List<Sticker> stickerList;
	private List<ViewType> viewTypeList;
	private LayoutInflater inflater;
	private String categoryId;
	private int categoryIndex;
	private int numStickerRows;
	private boolean updateAvailable;

	public StickerPageAdapter(Activity activity, List<Sticker> stickerList,
			List<ViewType> viewTypeList, int categoryIndex,
			boolean updateAvailable) {
		this.activity = activity;
		this.stickerList = stickerList;
		this.numStickers = stickerList.size();
		this.viewTypeList = viewTypeList;
		this.categoryIndex = categoryIndex;
		this.categoryId = Utils.getCategoryIdForIndex(categoryIndex);
		this.updateAvailable = updateAvailable;
		this.inflater = LayoutInflater.from(activity);

		calculateNumRowsAndSize();
	}

	private void calculateNumRowsAndSize() {
		int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;

		this.numItemsRow = (int) (screenWidth / SIZE_IMAGE);

		int emoticonPagerPadding = (int) 2
				* activity.getResources().getDimensionPixelSize(
						R.dimen.emoticon_pager_padding);
		int stickerPadding = (int) 2
				* activity.getResources().getDimensionPixelSize(
						R.dimen.sticker_padding);

		int remainingSpace = (screenWidth - emoticonPagerPadding - stickerPadding)
				- (this.numItemsRow * SIZE_IMAGE);

		this.sizeEachImage = SIZE_IMAGE
				+ ((int) (remainingSpace / this.numItemsRow));

		if (numStickers != 0) {
			if (numStickers % numItemsRow == 0) {
				this.numStickerRows = numStickers / numItemsRow;
			} else {
				this.numStickerRows = numStickers / numItemsRow + 1;
			}

			for (int i = 0; i < numStickerRows; i++) {
				viewTypeList.add(updateAvailable ? 1 : 0, ViewType.STICKER);
			}
		}
	}

	@Override
	public int getItemViewType(int position) {
		return viewTypeList.get(position).ordinal();
	}

	@Override
	public int getViewTypeCount() {
		return ViewType.values().length;
	}

	@Override
	public int getCount() {
		return viewTypeList.size();
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
		ViewType viewType = viewTypeList.get(position);
		if (convertView == null) {
			switch (viewType) {
			case STICKER:
				convertView = new LinearLayout(activity);
				break;
			case UPDATING_STICKER:
				convertView = inflater.inflate(R.layout.update_sticker_set,
						null);
				break;
			case DOWNLOADING_MORE:
				convertView = inflater.inflate(
						R.layout.downloading_new_stickers, null);
				break;
			}
		}

		switch (viewType) {
		case STICKER:
			AbsListView.LayoutParams parentParams = new LayoutParams(
					LayoutParams.MATCH_PARENT, sizeEachImage);
			convertView.setLayoutParams(parentParams);

			LinearLayout.LayoutParams childParams = new LinearLayout.LayoutParams(
					sizeEachImage, LayoutParams.MATCH_PARENT);

			((LinearLayout) convertView).removeAllViews();
			/*
			 * If this is the last item, its possible that the number of items
			 * won't fill the complete row
			 */
			int startPosition = updateAvailable ? position - 1 : position;

			int maxCount;
			if ((startPosition == numStickerRows - 1)
					&& (numStickers % numItemsRow != 0)) {
				maxCount = numStickers % numItemsRow;
			} else {
				maxCount = numStickerRows != 0 ? numItemsRow : 0;
			}

			int padding = (int) (5 * Utils.densityMultiplier);
			for (int i = 0; i < maxCount; i++) {
				ImageView imageView = new ImageView(activity);
				imageView.setLayoutParams(childParams);
				imageView.setScaleType(ScaleType.FIT_CENTER);
				imageView.setPadding(padding, padding, padding, padding);

				int index = (startPosition * numItemsRow) + i;

				Sticker sticker = stickerList.get(index);

				if (sticker.getStickerIndex() != -1) {
					if (sticker.getCategoryIndex() == 2) {
						imageView
								.setImageResource(EmoticonConstants.LOCAL_STICKER_SMALL_RES_IDS_2[sticker
										.getStickerIndex()]);
					} else if (sticker.getCategoryIndex() == 1) {
						imageView
								.setImageResource(EmoticonConstants.LOCAL_STICKER_SMALL_RES_IDS_1[sticker
										.getStickerIndex()]);
					}
				} else {
					imageView.setImageDrawable(IconCacheManager.getInstance()
							.getStickerThumbnail(
									sticker.getSmallStickerPath(activity)));
				}
				imageView.setTag(sticker);

				imageView.setOnClickListener(this);

				((LinearLayout) convertView).addView(imageView);
			}
			break;
		case UPDATING_STICKER:
			View button = convertView.findViewById(R.id.update_btn);
			TextView updateText = (TextView) convertView.findViewById(R.id.txt);
			ProgressBar progressBar = (ProgressBar) convertView
					.findViewById(R.id.download_progress);

			if (HikeMessengerApp.stickerTaskMap.containsKey(categoryId)) {
				progressBar.setVisibility(View.VISIBLE);
				updateText.setText(R.string.updating_set);
				updateText.setTextColor(activity.getResources().getColor(
						R.color.downloading_sticker));
				convertView.setClickable(false);
				button.setBackgroundResource(R.drawable.bg_sticker_downloading);
			} else {
				progressBar.setVisibility(View.GONE);
				updateText.setText(R.string.new_stickers_available);
				updateText.setTextColor(activity.getResources().getColor(
						R.color.actionbar_text));
				convertView.setClickable(true);
				button.setBackgroundResource(R.drawable.bg_download_sticker);
				convertView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						DownloadStickerTask downloadStickerTask = new DownloadStickerTask(
								activity, categoryIndex, DownloadType.UPDATE);
						Utils.executeFtResultAsyncTask(downloadStickerTask);

						HikeMessengerApp.stickerTaskMap.put(categoryId,
								downloadStickerTask);
						notifyDataSetChanged();
					}
				});
			}

			break;
		case DOWNLOADING_MORE:
			break;
		}

		return convertView;
	}

	@Override
	public void onClick(View v) {
		Sticker sticker = (Sticker) v.getTag();
		((ChatThread) activity).sendSticker(sticker);
		((HikeMessengerApp)activity.getApplicationContext()).addRecentSticker(sticker);
	}
}
