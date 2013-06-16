package com.bsb.hike.adapters;

import java.util.List;

import android.app.Activity;
import android.graphics.BitmapFactory;
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

import com.bsb.hike.R;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.tasks.DownloadStickerTask;
import com.bsb.hike.tasks.DownloadStickerTask.DownloadType;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.Utils;

public class StickerAdapter extends BaseAdapter implements OnClickListener {

	public static final int MAX_STICKER_PER_ROW_PORTRAIT = 4;

	public static final int MAX_STICKER_PER_ROW_LANDSCAPE = 6;

	public static enum ViewType {
		STICKER, UPDATING_STICKER, DOWNLOADING_MORE
	}

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

	public StickerAdapter(Activity activity, List<Sticker> stickerList,
			List<ViewType> viewTypeList, int categoryIndex, int numItemsRow,
			int numStickerRows, boolean updateAvailable) {
		this.activity = activity;

		this.numItemsRow = numItemsRow;

		this.sizeEachImage = (int) ((activity.getResources()
				.getDisplayMetrics().widthPixels
				- (2 * activity.getResources().getDimension(
						R.dimen.sticker_padding)) - (2 * activity
				.getResources().getDimension(R.dimen.emoticon_pager_padding))) / numItemsRow);

		this.stickerList = stickerList;
		this.numStickers = stickerList.size();
		this.viewTypeList = viewTypeList;
		this.categoryIndex = categoryIndex;
		this.categoryId = Utils.getCategoryIdForIndex(categoryIndex);
		this.numStickerRows = numStickerRows;
		this.updateAvailable = updateAvailable;
		this.inflater = LayoutInflater.from(activity);
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
					imageView
							.setImageResource(EmoticonConstants.LOCAL_STICKER_SMALL_RES_IDS[sticker
									.getStickerIndex()]);
				} else {
					imageView.setImageBitmap(BitmapFactory.decodeFile(sticker
							.getSmallStickerPath(activity)));
				}
				imageView.setTag(sticker);

				imageView.setOnClickListener(this);

				((LinearLayout) convertView).addView(imageView);
			}
			break;
		case UPDATING_STICKER:
			TextView updateText = (TextView) convertView.findViewById(R.id.txt);
			ProgressBar progressBar = (ProgressBar) convertView
					.findViewById(R.id.download_progress);

			if (ChatThread.stickerTaskMap.containsKey(categoryId)) {
				progressBar.setVisibility(View.VISIBLE);
				updateText.setText(R.string.updating_set);
				convertView.setClickable(false);
				convertView
						.setBackgroundResource(R.drawable.invite_to_hike_pressed);
			} else {
				progressBar.setVisibility(View.VISIBLE);
				updateText.setText(R.string.new_stickers_available);
				convertView.setClickable(true);
				convertView
						.setBackgroundResource(R.drawable.invite_chatthread_button);
				convertView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						DownloadStickerTask downloadStickerTask = new DownloadStickerTask(
								activity, categoryIndex, DownloadType.UPDATE);
						downloadStickerTask.execute();

						ChatThread.stickerTaskMap.put(categoryId,
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
	}
}
