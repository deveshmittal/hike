package com.bsb.hike.adapters;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
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
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.smartImageLoader.StickerLoader;
import com.bsb.hike.tasks.DownloadStickerTask;
import com.bsb.hike.tasks.DownloadStickerTask.DownloadType;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.utils.RecyclingImageView;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class StickerPageAdapter extends BaseAdapter implements OnClickListener
{

	public static final int MAX_STICKER_PER_ROW_PORTRAIT = 4;

	public static final int MAX_STICKER_PER_ROW_LANDSCAPE = 6;

	public static enum ViewType
	{
		STICKER, UPDATING_STICKER, DOWNLOADING_MORE, RECENT_EMPTY
	}

	public static final int SIZE_IMAGE = (int) (80 * Utils.densityMultiplier);

	private int numItemsRow;

	private int sizeEachImage;

	private Activity activity;

	private List<Sticker> stickerList;

	private List<ViewType> viewTypeList;

	private LayoutInflater inflater;

	private StickerCategory category;

	private int numStickerRows;

	private StickerLoader stickerLoader;

	private boolean isListFlinging;

	public StickerPageAdapter(Activity activity, List<Sticker> stickerList, StickerCategory category, List<ViewType> viewTypeList, StickerLoader worker)
	{
		this.activity = activity;
		this.stickerList = stickerList;
		this.viewTypeList = viewTypeList;
		this.category = category;
		this.inflater = LayoutInflater.from(activity);
		this.stickerLoader = worker;
		calculateNumRowsAndSize(false);
	}

	public List<Sticker> getStickerList()
	{
		return stickerList;
	}

	public List<ViewType> getViewTypeList()
	{
		return viewTypeList;
	}

	public void calculateNumRowsAndSize(boolean recal)
	{
		int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;

		this.numItemsRow = (int) (screenWidth / SIZE_IMAGE);

		int emoticonPagerPadding = (int) 2 * activity.getResources().getDimensionPixelSize(R.dimen.emoticon_pager_padding);
		int stickerPadding = (int) 2 * activity.getResources().getDimensionPixelSize(R.dimen.sticker_padding);

		int remainingSpace = (screenWidth - emoticonPagerPadding - stickerPadding) - (this.numItemsRow * SIZE_IMAGE);

		this.sizeEachImage = SIZE_IMAGE + ((int) (remainingSpace / this.numItemsRow));

		if (stickerList.size() != 0)
		{
			if (stickerList.size() % numItemsRow == 0)
			{
				this.numStickerRows = stickerList.size() / numItemsRow;
			}
			else
			{
				this.numStickerRows = stickerList.size() / numItemsRow + 1;
			}
			if (category.isCustom())
			{
				viewTypeList.clear();
			}

			int count = 0;

			/*
			 * Recal will be used when you download new stickers while scrolling. It will add new sticker rows at the end.
			 */
			if (recal)
			{
				for (int i = 0; i < viewTypeList.size(); i++)
				{
					if (viewTypeList.get(i).equals(ViewType.STICKER))
						count++;
				}
			}
			for (int i = 0; i < numStickerRows - count; i++)
			{
				viewTypeList.add(category.isUpdateAvailable() ? 1 : 0, ViewType.STICKER);
			}
		}
		else if (category.isCustom())
		{
			viewTypeList.add(ViewType.RECENT_EMPTY);
		}
	}

	@Override
	public int getItemViewType(int position)
	{
		return viewTypeList.get(position).ordinal();
	}

	@Override
	public int getViewTypeCount()
	{
		return ViewType.values().length;
	}

	@Override
	public int getCount()
	{
		return viewTypeList.size();
	}

	@Override
	public Object getItem(int position)
	{
		return null;
	}

	@Override
	public long getItemId(int position)
	{
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ViewType viewType = viewTypeList.get(position);
		if (convertView == null)
		{
			switch (viewType)
			{
			case STICKER:
				convertView = new LinearLayout(activity);
				AbsListView.LayoutParams parentParams = new LayoutParams(LayoutParams.MATCH_PARENT, sizeEachImage);
				convertView.setLayoutParams(parentParams);

				LinearLayout.LayoutParams childParams = new LinearLayout.LayoutParams(sizeEachImage, LayoutParams.MATCH_PARENT);

				int padding = (int) (5 * Utils.densityMultiplier);
				for (int i = 0; i < numItemsRow; i++)
				{
					ImageView imageView = new RecyclingImageView(activity);
					imageView.setLayoutParams(childParams);
					imageView.setScaleType(ScaleType.FIT_CENTER);
					imageView.setPadding(padding, padding, padding, padding);

					((LinearLayout) convertView).addView(imageView);
				}
				break;
			case UPDATING_STICKER:
				convertView = inflater.inflate(R.layout.update_sticker_set, null);
				break;
			case DOWNLOADING_MORE:
				convertView = inflater.inflate(R.layout.downloading_new_stickers, null);
				break;
			case RECENT_EMPTY:
				convertView = inflater.inflate(R.layout.recent_empty_view, null);
				break;
			}
		}

		switch (viewType)
		{
		case STICKER:

			/*
			 * If this is the last item, its possible that the number of items won't fill the complete row
			 */
			int startPosition = category.isUpdateAvailable() ? position - 1 : position;

			for (int i = 0; i < numItemsRow; i++)
			{
				ImageView imageView = (ImageView) ((LinearLayout) convertView).getChildAt(i);

				int index = (startPosition * numItemsRow) + i;
				if (index > stickerList.size() - 1)
				{
					imageView.setImageDrawable(null);
					imageView.setTag(null);
					imageView.setOnClickListener(null);
					continue;
				}

				Sticker sticker = stickerList.get(index);
				stickerLoader.loadImage(sticker.getSmallStickerPath(activity), imageView, isListFlinging);
				imageView.setTag(sticker);
				imageView.setOnClickListener(this);
			}
			break;
		case UPDATING_STICKER:
			View button = convertView.findViewById(R.id.update_btn);
			TextView updateText = (TextView) convertView.findViewById(R.id.txt);
			ProgressBar progressBar = (ProgressBar) convertView.findViewById(R.id.download_progress);

			if (StickerManager.getInstance().isStickerDownloading(category.getCategoryId()))
			{
				progressBar.setVisibility(View.VISIBLE);
				updateText.setText(R.string.updating_set);
				updateText.setTextColor(activity.getResources().getColor(R.color.downloading_sticker));
				convertView.setClickable(false);
				button.setBackgroundResource(R.drawable.bg_sticker_downloading);
			}
			else
			{
				progressBar.setVisibility(View.GONE);
				updateText.setText(R.string.new_stickers_available);
				updateText.setTextColor(activity.getResources().getColor(R.color.actionbar_text));
				convertView.setClickable(true);
				button.setBackgroundResource(R.drawable.bg_download_sticker);
				convertView.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						DownloadStickerTask downloadStickerTask = new DownloadStickerTask(activity, category, DownloadType.UPDATE, StickerPageAdapter.this);
						Utils.executeFtResultAsyncTask(downloadStickerTask);

						StickerManager.getInstance().insertTask(category.getCategoryId(), downloadStickerTask);
						notifyDataSetChanged();
					}
				});
			}

			break;
		case DOWNLOADING_MORE:
			break;
		case RECENT_EMPTY:
			break;
		}

		return convertView;
	}

	/* This should be used only for recent stickers */
	public void updateRecentsList(Sticker st)
	{
		stickerList.remove(st);
		if (stickerList.size() == StickerManager.RECENT_STICKERS_COUNT) // if size is already 30 remove first element and then add
		{
			// remove last sticker
			stickerList.remove(stickerList.size() - 1);
		}
		stickerList.add(0, st);
		calculateNumRowsAndSize(true);
	}

	@Override
	public void onClick(View v)
	{
		Sticker sticker = (Sticker) v.getTag();
		((ChatThread) activity).sendSticker(sticker);

		/* In case sticker is clicked on the recents screen, don't update the UI or recents list. Also if this sticker is disabled don't update the recents UI */
		if (!category.isCustom())
		{
			StickerManager.getInstance().addRecentSticker(sticker);
			LocalBroadcastManager.getInstance(activity).sendBroadcast(new Intent(StickerManager.RECENTS_UPDATED).putExtra(StickerManager.RECENT_STICKER_SENT, sticker));
		}
	}

	public void setIsListFlinging(boolean b)
	{
		boolean notify = b != isListFlinging;

		isListFlinging = b;

		if (notify && !isListFlinging)
		{
			notifyDataSetChanged();
		}
	}
	
	public StickerLoader getStickerLoader()
	{
		return stickerLoader;
	}
}
