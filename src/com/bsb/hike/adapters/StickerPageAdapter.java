package com.bsb.hike.adapters;

import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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
import com.bsb.hike.HikeConstants.STResult;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.models.StickerPageAdapterItem;
import com.bsb.hike.modules.stickerdownloadmgr.IStickerResultListener;
import com.bsb.hike.modules.stickerdownloadmgr.StickerDownloadManager;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadType;
import com.bsb.hike.smartImageLoader.StickerLoader;
import com.bsb.hike.tasks.DownloadStickerTask;
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

		int stickerPadding = (int) 2 * activity.getResources().getDimensionPixelSize(R.dimen.sticker_padding);
		int horizontalSpacing = (int) (this.numItemsRow - 1) * activity.getResources().getDimensionPixelSize(R.dimen.sticker_grid_horizontal_padding);
		
		int remainingSpace = (screenWidth - horizontalSpacing - stickerPadding) - (this.numItemsRow * StickerManager.SIZE_IMAGE);

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
			break;
		case StickerPageAdapterItem.DONE:
			viewType = ViewType.DONE;
			break;
		case StickerPageAdapterItem.PLACE_HOLDER:
			viewType = ViewType.PLACE_HOLDER;
			break;
		}
		
		return viewType.ordinal();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ViewType viewType = ViewType.values()[getItemViewType(position)];
		StickerPageAdapterItem item = getItem(position);
		ViewHolder viewHolder = null;
		AbsListView.LayoutParams ll = new AbsListView.LayoutParams(sizeEachImage,sizeEachImage);
		
		if (convertView == null)
		{
			viewHolder = new ViewHolder();
			
			switch (viewType)
			{
			case STICKER:
				convertView = new RecyclingImageView(activity);
				int padding = (int) (5 * Utils.densityMultiplier);
				convertView.setLayoutParams(ll);
				((ImageView) convertView).setScaleType(ScaleType.FIT_CENTER);
				((ImageView) convertView).setPadding(padding, padding, padding, padding);
				
				break;
			case UPDATE:                //Since all of these have the same layout to be inflated
			case DOWNLOADING:
			case RETRY:
			case DONE:
				convertView = inflater.inflate(R.layout.update_sticker_set, null);
				convertView.setLayoutParams(ll);
				viewHolder.text = (TextView) convertView.findViewById(R.id.new_number_stickers);
				viewHolder.image = (ImageView) convertView.findViewById(R.id.update_btn);
				viewHolder.progress = (ProgressBar) convertView.findViewById(R.id.download_progress);
				break;
			case PLACE_HOLDER:
				convertView = inflater.inflate(R.layout.update_sticker_set, null);
				viewHolder.image = (ImageView) convertView.findViewById(R.id.sticker_placeholder);
				convertView.setLayoutParams(ll);
				break;
			}
			convertView.setTag(viewHolder);
		}
		
		else
		{
			try{
				
			viewHolder = (ViewHolder) convertView.getTag();
			}
			catch(ClassCastException e)
			{
			}
		}

		switch (viewType)
		{
		case STICKER:
			Sticker sticker = (Sticker) item.getSticker();
			stickerLoader.loadImage(sticker.getSmallStickerPath(), ((ImageView) convertView), isListFlinging);
			convertView.setTag(sticker);
			convertView.setOnClickListener(this);
				
			break;
		case UPDATE:
			viewHolder.image.setVisibility(View.VISIBLE);

			if(item.getCategoryMoreStickerCount() > 0)
			{
				viewHolder.text.setVisibility(View.VISIBLE);
				viewHolder.text.setText(activity.getResources().getString(R.string.n_more, item.getCategoryMoreStickerCount()));
			}
			else
			{
				viewHolder.text.setVisibility(View.GONE);
			}
			
			viewHolder.image.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					initialiseDownloadStickerTask();
				}
			});

			break;
		case DOWNLOADING:
			viewHolder.progress.setVisibility(View.VISIBLE);
			
			break;
		case RETRY:
			viewHolder.image.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_retry_sticker));
			viewHolder.image.setVisibility(View.VISIBLE);
			viewHolder.text.setVisibility(View.VISIBLE);
			viewHolder.text.setText(activity.getResources().getString(R.string.retry_sticker));
			viewHolder.image.setOnClickListener(new View.OnClickListener()
			{
				
				@Override
				public void onClick(View v)
				{
					initialiseDownloadStickerTask();
				}
			});
			
			break;
		case DONE:
			viewHolder.image.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_done_palette));
			viewHolder.image.setVisibility(View.VISIBLE);
			viewHolder.text.setVisibility(View.VISIBLE);
			viewHolder.text.setText(activity.getResources().getString(R.string.see_them));
			viewHolder.image.setOnClickListener(new View.OnClickListener()
			{
				
				@Override
				public void onClick(View v)
				{
					// TODO Add method to scroll to the new stickers
				}
			});
			break;
		case PLACE_HOLDER:
			viewHolder.image.setVisibility(View.VISIBLE);
			break;
		}

		return convertView;
	}

	private void initialiseDownloadStickerTask()
	{
		StickerManager.getInstance().initialiseDownloadStickerTask(category, activity);
		replaceDownloadingatTop();
	}

	/**
	 * Replaces the view at index 0 with Downloading view
	 */
	protected void replaceDownloadingatTop()
	{
		if(itemList.size() > 0 && (itemList.get(0).getStickerPageAdapterItemId() != StickerPageAdapterItem.STICKER))
		{
			itemList.remove(0);
			itemList.add(0, new StickerPageAdapterItem(StickerPageAdapterItem.DOWNLOADING));
			notifyDataSetChanged();
		}
		
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
	
	private class ViewHolder
	{
		ImageView image;
		
		TextView text;
		
		ProgressBar progress;
	}

}
