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
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.models.StickerPageAdapterItem;
import com.bsb.hike.smartImageLoader.StickerLoader;
import com.bsb.hike.tasks.DownloadStickerTask;
import com.bsb.hike.tasks.DownloadStickerTask.DownloadType;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.utils.RecyclingImageView;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class StickerPageAdapter extends BaseAdapter implements OnClickListener
{

	public static enum ViewType
	{
		STICKER, UPDATE, DOWNLOADING, RETRY, DONE, PLACE_HOLDER
	}

	private int numItemsRow;

	private int sizeEachImage;

	private Activity activity;

	private List<StickerPageAdapterItem> itemList;

	private LayoutInflater inflater;

	private StickerCategory category;

	private StickerLoader stickerLoader;

	private boolean isListFlinging;
	
	public StickerPageAdapter(Activity activity, List<StickerPageAdapterItem> itemList, StickerCategory category, StickerLoader worker)
	{
		this.activity = activity;
		this.itemList = itemList;
		this.category = category;
		this.inflater = LayoutInflater.from(activity);
		this.stickerLoader = worker;
		calculateSizeOfStickerImage();
	}

	public List<StickerPageAdapterItem> getStickerPageAdapterItemList()
	{
		return itemList;
	}

	public void calculateSizeOfStickerImage()
	{
		int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;

		this.numItemsRow = StickerManager.getInstance().getNumColumnsForStickerGrid(activity);

		int emoticonPagerPadding = (int) 2 * activity.getResources().getDimensionPixelSize(R.dimen.emoticon_pager_padding);
		int stickerPadding = (int) 2 * activity.getResources().getDimensionPixelSize(R.dimen.sticker_padding);

		int remainingSpace = (screenWidth - emoticonPagerPadding - stickerPadding) - (this.numItemsRow * StickerManager.SIZE_IMAGE);

		this.sizeEachImage = StickerManager.SIZE_IMAGE + ((int) (remainingSpace / this.numItemsRow));

	}

	@Override
	public int getViewTypeCount()
	{
		return ViewType.values().length;
	}

	@Override
	public int getCount()
	{
		return itemList.size();
	}

	@Override
	public StickerPageAdapterItem getItem(int position)
	{
		return itemList.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}
	
	@Override
	public int getItemViewType(int position)
	{
		ViewType viewType = ViewType.STICKER;  //Default value.
		StickerPageAdapterItem item = getItem(position);
		int itemId = item.getStickerPageAdapterItemId();
		switch(itemId)
		{
		case StickerPageAdapterItem.STICKER :
			viewType = ViewType.STICKER;
			break;
		case StickerPageAdapterItem.UPDATE:
			viewType = ViewType.UPDATE;
			break;
		case StickerPageAdapterItem.DOWNLOADING:
			viewType = ViewType.DOWNLOADING;
			break;
		case StickerPageAdapterItem.RETRY:
			viewType = ViewType.RETRY;
		case StickerPageAdapterItem.DONE:
			viewType = viewType.DONE;
		case StickerPageAdapterItem.PLACE_HOLDER:
			viewType = viewType.PLACE_HOLDER;
		}
		
		return viewType.ordinal();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ViewType viewType = ViewType.values()[getItemViewType(position)];
		StickerPageAdapterItem item = getItem(position);
		
		if (convertView == null)
		{
			switch (viewType)
			{
			case STICKER:
				ImageView stickerImage = new RecyclingImageView(activity);
				int padding = (int) (5 * Utils.densityMultiplier);
				AbsListView.LayoutParams ll = new AbsListView.LayoutParams(sizeEachImage,sizeEachImage);
				stickerImage.setLayoutParams(ll);
				stickerImage.setScaleType(ScaleType.FIT_CENTER);
				stickerImage.setPadding(padding, padding, padding, padding);
				convertView = stickerImage;
				
				break;
			case UPDATE:
				convertView = inflater.inflate(R.layout.update_sticker_set, null);
				break;
			case DOWNLOADING:
				convertView = inflater.inflate(R.layout.downloading_new_stickers, null);
				break;
			case RETRY:
				// TODO Add retry view here
				break;
			case DONE:
				// TODO Add done view here
				break;
			case PLACE_HOLDER:
				// TODO Add placeholder view here
				break;
			}
		}

		switch (viewType)
		{
		case STICKER:
			Sticker sticker = (Sticker) item.getSticker();
			stickerLoader.loadImage(sticker.getSmallStickerPath(activity), (ImageView)convertView, isListFlinging);
			convertView.setTag(sticker);
			convertView.setOnClickListener(this);
				
			break;
		case UPDATE:
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
		case DOWNLOADING:
			break;
		case RETRY:
			break;
		case DONE:
			break;
		case PLACE_HOLDER:
			break;
		}

		return convertView;
	}

	/* This should be used only for recent stickers */
	public void updateRecentsList(Sticker st)
	{
		StickerPageAdapterItem item = new StickerPageAdapterItem(StickerPageAdapterItem.STICKER, st);
		itemList.remove(item);
		
		if (itemList.size() == StickerManager.RECENT_STICKERS_COUNT) // if size is already 30 remove first element and then add
		{
			// remove last sticker
			itemList.remove(itemList.size() - 1);
		}
		itemList.add(0, item);
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
	
	public void addSticker(Sticker st)
	{
		this.itemList.add(new StickerPageAdapterItem(StickerPageAdapterItem.STICKER, st));
	}
}
