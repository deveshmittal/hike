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
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.models.StickerPageAdapterItem;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadSource;
import com.bsb.hike.smartImageLoader.StickerLoader;
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
	
	AbsListView absListView;
	
	public StickerPageAdapter(Activity activity, List<StickerPageAdapterItem> itemList, StickerCategory category, StickerLoader worker, AbsListView absListView )
	{
		this.activity = activity;
		this.itemList = itemList;
		this.category = category;
		this.inflater = LayoutInflater.from(activity);
		this.stickerLoader = worker;
		this.absListView = absListView;
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
		int itemId = item.getType();
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
				viewHolder.tickImage = (ImageView) convertView.findViewById(R.id.sticker_placeholder);
				
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
			viewHolder = (ViewHolder) convertView.getTag();
		}
		
		switch (viewType)
		{
		case STICKER:
			Sticker sticker = (Sticker) item.getSticker();
			stickerLoader.loadImage(sticker.getSmallStickerPath(), ((ImageView) convertView), isListFlinging);
			convertView.setOnClickListener(this);
				
			break;
		case UPDATE:
			viewHolder.image.setVisibility(View.VISIBLE);
			viewHolder.progress.setVisibility(View.GONE);
			viewHolder.tickImage.setVisibility(View.GONE);
			if(item.getCategoryMoreStickerCount() > 0)
			{
				viewHolder.text.setVisibility(View.VISIBLE);
				viewHolder.text.setText(activity.getResources().getString(R.string.n_more, item.getCategoryMoreStickerCount()));
			}
			else
			{
				viewHolder.text.setVisibility(View.GONE);
			}
			
			convertView.setOnClickListener(this);

			break;
		case DOWNLOADING:
			viewHolder.progress.setVisibility(View.VISIBLE);
			viewHolder.text.setVisibility(View.GONE);
			viewHolder.tickImage.setVisibility(View.GONE);
			
			break;
		case RETRY:
			viewHolder.image.setImageBitmap(HikeBitmapFactory.decodeResource(activity.getResources(), R.drawable.ic_retry_sticker));
			viewHolder.image.setVisibility(View.VISIBLE);
			viewHolder.text.setVisibility(View.VISIBLE);
			viewHolder.progress.setVisibility(View.GONE);
			viewHolder.text.setText(activity.getResources().getString(R.string.retry_sticker));
			viewHolder.tickImage.setVisibility(View.GONE);
			convertView.setOnClickListener(this);
			
			break;
		case DONE:
			viewHolder.image.setVisibility(View.GONE);
			viewHolder.text.setVisibility(View.GONE);
			viewHolder.progress.setVisibility(View.GONE);
			viewHolder.tickImage.setVisibility(View.VISIBLE);
			viewHolder.tickImage.setImageBitmap(HikeBitmapFactory.decodeResource(activity.getResources(), R.drawable.ic_done_palette));
			convertView.setOnClickListener(this);
			
			break;
		case PLACE_HOLDER:
			viewHolder.image.setVisibility(View.VISIBLE);
			break;
		}
		viewHolder.position = position;
		return convertView;
	}

	private void initialiseDownloadStickerTask(DownloadSource source)
	{
		StickerManager.getInstance().initialiseDownloadStickerTask(category, source, activity);
		replaceDownloadingatTop();
	}

	/**
	 * Replaces the view at index 0 with Downloading view
	 */
	protected void replaceDownloadingatTop()
	{
		if(itemList.size() > 0 && (itemList.get(0).getType() != StickerPageAdapterItem.STICKER))
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
		ViewHolder viewHolder = (ViewHolder) v.getTag();
		int position = viewHolder.position;
		StickerPageAdapterItem item = (StickerPageAdapterItem) getItem(position); 
		switch (item.getType())
		{
		case StickerPageAdapterItem.STICKER:
			Sticker sticker = item.getSticker();
			String source = category.isCustom() ? StickerManager.FROM_RECENT : StickerManager.FROM_OTHER;
			((ChatThread) activity).sendSticker(sticker, source);

			/* In case sticker is clicked on the recents screen, don't update the UI or recents list. Also if this sticker is disabled don't update the recents UI */
			if (!category.isCustom())
			{
				StickerManager.getInstance().addRecentSticker(sticker);
				LocalBroadcastManager.getInstance(activity).sendBroadcast(new Intent(StickerManager.RECENTS_UPDATED).putExtra(StickerManager.RECENT_STICKER_SENT, sticker));
			}
			break;
		case StickerPageAdapterItem.UPDATE:
			initialiseDownloadStickerTask(DownloadSource.X_MORE);
			break;
		case StickerPageAdapterItem.RETRY:
			initialiseDownloadStickerTask(DownloadSource.RETRY);
			break;
		case StickerPageAdapterItem.DONE:
			final int count = getCount()-1;
			absListView.smoothScrollBy(count*sizeEachImage, 300);
			break;
		default:
			break;
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
		
		int position;
		
		ImageView tickImage;
	}
	
}