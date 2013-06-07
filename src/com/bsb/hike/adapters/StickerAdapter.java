package com.bsb.hike.adapters;

import java.util.List;

import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.utils.EmoticonConstants;

public class StickerAdapter extends BaseAdapter implements OnClickListener {

	private List<String> stIds;
	private int numItemsRow;
	private int sizeEachImage;
	private Activity activity;

	public StickerAdapter(Activity activity, String catId, int numItemsRow) {
		this.activity = activity;
		this.numItemsRow = numItemsRow;
		this.sizeEachImage = (int) (activity.getResources().getDisplayMetrics().widthPixels / numItemsRow);
	}

	@Override
	public int getCount() {
		int count = EmoticonConstants.LOCAL_STICKER_RES_IDS.length
				/ numItemsRow;
		if (EmoticonConstants.LOCAL_STICKER_RES_IDS.length % numItemsRow != 0) {
			count++;
		}
		return count;
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
			convertView = new LinearLayout(activity);
		}

		AbsListView.LayoutParams parentParams = new LayoutParams(
				LayoutParams.MATCH_PARENT, sizeEachImage);
		convertView.setLayoutParams(parentParams);

		LinearLayout.LayoutParams childParams = new LinearLayout.LayoutParams(
				sizeEachImage, LayoutParams.MATCH_PARENT);

		/*
		 * If this is the last item, its possible that the number of items won't
		 * fill the complete row
		 */
		int maxCount;
		if ((position == getCount() - 1)
				&& (EmoticonConstants.LOCAL_STICKER_RES_IDS.length
						% numItemsRow != 0)) {
			maxCount = EmoticonConstants.LOCAL_STICKER_RES_IDS.length
					% numItemsRow;
		} else {
			maxCount = numItemsRow;
		}

		for (int i = 0; i < maxCount; i++) {
			ImageView imageView = new ImageView(activity);
			imageView.setLayoutParams(childParams);
			imageView.setScaleType(ScaleType.CENTER_INSIDE);
			imageView.setPadding(10, 10, 10, 10);
			imageView
					.setImageResource(EmoticonConstants.LOCAL_STICKER_RES_IDS[(position * numItemsRow)
							+ i]);
			imageView.setTag((position * numItemsRow) + i);
			imageView.setOnClickListener(this);

			((LinearLayout) convertView).addView(imageView);
		}
		return convertView;
	}

	@Override
	public void onClick(View v) {
		int emoticonIndex = (Integer) v.getTag();
		((ChatThread) activity).sendSticker(0, emoticonIndex);
	}
}
