package com.bsb.hike.adapters;

import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.StickerDownloadManager;
import com.bsb.hike.smartImageLoader.StickerLoader;
import com.bsb.hike.smartImageLoader.StickerPreviewLoader;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class StickerShopAdapter extends CursorAdapter
{
	private LayoutInflater layoutInflater;

	private Context context;

	private StickerPreviewLoader stickerPreviewLoader;
	
	private boolean isListFlinging;

	private int idColoumn;

	private int categoryNameColoumn;

	private int totalStickersCountColoumn;

	private int categorySizeColoumn;

	private Map<String, StickerCategory> stickerCategoriesMap;
	
	private final int FULLY_DOWNLOADED = 0;
	
	private final int NOT_DOWNLOADED = 1;
	
	private final int UPDATE_AVAILABLE = 2;
	
	private final int RETRY = 3;

	class ViewHolder
	{
		TextView categoryName;

		TextView totalStickers;

		TextView stickersPackDetails;

		ImageView downloadState;
		
		ImageView categoryPreviewIcon;

		View downloadProgress;
	}

	public StickerShopAdapter(Context context, Cursor cursor, Map<String, StickerCategory> stickerCategoriesMap)
	{
		super(context, cursor, false);
		this.context = context;
		this.layoutInflater = LayoutInflater.from(context);
		this.stickerPreviewLoader = new StickerPreviewLoader(context, true);
		this.idColoumn = cursor.getColumnIndex(DBConstants._ID);
		this.categoryNameColoumn = cursor.getColumnIndex(DBConstants.CATEGORY_NAME);
		this.totalStickersCountColoumn = cursor.getColumnIndex(DBConstants.TOTAL_NUMBER);
		this.categorySizeColoumn = cursor.getColumnIndex(DBConstants.CATEGORY_SIZE);
		this.stickerCategoriesMap = stickerCategoriesMap;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent)
	{
		View v = layoutInflater.inflate(R.layout.sticker_shop_list_item, parent, false);
		ViewHolder viewholder = new ViewHolder();
		viewholder.categoryName = (TextView) v.findViewById(R.id.category_name);
		viewholder.stickersPackDetails = (TextView) v.findViewById(R.id.pack_details);
		viewholder.downloadState = (ImageView) v.findViewById(R.id.category_download_btn);
		viewholder.categoryPreviewIcon = (ImageView) v.findViewById(R.id.category_icon);
		viewholder.downloadProgress = v.findViewById(R.id.download_progress_bar);
		viewholder.downloadState.setOnClickListener(mDownloadButtonClickListener);
		v.setTag(viewholder);
		return v;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor)
	{
		ViewHolder viewholder = (ViewHolder) view.getTag();
		viewholder.downloadProgress.setVisibility(View.GONE); //This is being done to clear the spinner animation. 
		viewholder.downloadProgress.clearAnimation();
		String categoryId = cursor.getString(idColoumn);
		String categoryName = cursor.getString(categoryNameColoumn);
		int totalStickerCount = cursor.getInt(totalStickersCountColoumn);
		int categorySizeInBytes = cursor.getInt(categorySizeColoumn);
		viewholder.categoryName.setText(cursor.getString(categoryNameColoumn));
		stickerPreviewLoader.loadImage(categoryId, viewholder.categoryPreviewIcon, isListFlinging);

		if (totalStickerCount > 0)
		{
			String detailsStirng = context.getResources().getString(R.string.n_stickers, totalStickerCount);
			if (categorySizeInBytes > 0)
			{
				detailsStirng += ", " + Utils.getSizeForDisplay(categorySizeInBytes);
			}
			viewholder.stickersPackDetails.setVisibility(View.VISIBLE);
			viewholder.stickersPackDetails.setText(detailsStirng);
		}
		else
		{
			viewholder.stickersPackDetails.setVisibility(View.GONE);
		}

		StickerCategory category;
		if (stickerCategoriesMap.containsKey(categoryId))
		{
			category = stickerCategoriesMap.get(categoryId);
		}
		else
		{
			category = new StickerCategory(categoryId, categoryName, totalStickerCount, categorySizeInBytes);
			stickerCategoriesMap.put(categoryId, category);
		}
		viewholder.downloadState.setVisibility(View.VISIBLE);
		
		if(category.isVisible())
		{
			switch (category.getState())
			{
			case StickerCategory.NONE:
			case StickerCategory.DONE:
				if (category.getDownloadedStickersCount() == 0)
				{
					viewholder.downloadState.setImageLevel(NOT_DOWNLOADED);
				}
				else
				{
					viewholder.downloadState.setImageLevel(FULLY_DOWNLOADED);
				}
				break;
			case StickerCategory.UPDATE:
				viewholder.downloadState.setImageLevel(UPDATE_AVAILABLE);
				break;
			case StickerCategory.RETRY:
				viewholder.downloadState.setImageLevel(RETRY);
				break;
			case StickerCategory.DOWNLOADING:
				viewholder.downloadState.setVisibility(View.GONE);
				viewholder.downloadProgress.setVisibility(View.VISIBLE);
				viewholder.downloadProgress.setAnimation(AnimationUtils.loadAnimation(context, R.anim.rotate));
				break;
			default:
				break;
			}
		}
		else
		{
			viewholder.downloadState.setImageLevel(NOT_DOWNLOADED);
		}
		viewholder.downloadState.setTag(category);
	}
	

	public StickerPreviewLoader getStickerPreviewLoader()
	{
		return stickerPreviewLoader;
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

	private OnClickListener mDownloadButtonClickListener = new OnClickListener()
	{
		
		@Override
		public void onClick(View view)
		{
			ImageView downloadButton = (ImageView) view;
			StickerCategory category = (StickerCategory) view.getTag();
			switch (downloadButton.getDrawable().getLevel())
			{
			case NOT_DOWNLOADED:
				StickerDownloadManager.getInstance(mContext).DownloadEnableDisableImage(mContext, category.getCategoryId(), null);
			case UPDATE_AVAILABLE:
			case RETRY:
				StickerManager.getInstance().initialiseDownloadStickerTask(category, mContext);
				break;

			default:
				break;
			}
		}
	};

}
