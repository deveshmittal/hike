package com.bsb.hike.photos.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;

import com.bsb.hike.R;
import com.bsb.hike.photos.HikeEffectsFactory;
import com.bsb.hike.photos.HikeEffectsFactory.OnFilterAppliedListener;
import com.bsb.hike.photos.HikePhotosListener;
import com.bsb.hike.photos.HikePhotosUtils;
import com.bsb.hike.photos.HikePhotosUtils.FilterTools.FilterType;
import com.bsb.hike.utils.Logger;

/**
 * @author akhiltripathi
 * 
 *         Custom View for the Filter Preview thumbnails
 * 
 */

public class FilterEffectItemLinearLayout extends EffectItemLinearLayout implements OnFilterAppliedListener
{
	private FilterType filter;
	private int tried;

	public FilterEffectItemLinearLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		
	}

	public void init(Bitmap preview, String Title)
	{

		this.setImage(preview);
		this.setText(Title);
		
	}

	public void select()
	{
		HikePhotosUtils.FilterTools.setSelectedFilter(this.filter);
		findViewById(R.id.selectionBar).setBackgroundColor(getResources().getColor(R.color.photos_filters_font_color));
		HikePhotosUtils.FilterTools.setCurrentFilterItem(this);
	}

	public void unSelect()
	{
		findViewById(R.id.selectionBar).setBackgroundColor(getResources().getColor(R.color.photos_pager_background));
	}

	public void setFilter(Context context, FilterType type, boolean setFilterItem)
	{
		this.filter = type;
		initiateThumbnailCreation();
		tried = 0;
		if (type == HikePhotosUtils.FilterTools.getSelectedFilter())
		{
			findViewById(R.id.selectionBar).setBackgroundColor(getResources().getColor(R.color.photos_filters_font_color));
			if(setFilterItem)
			HikePhotosUtils.FilterTools.setCurrentFilterItem(this);
		}
		else
		{
			findViewById(R.id.selectionBar).setBackgroundColor(getResources().getColor(R.color.photos_pager_background));
		}
	}

	private void initiateThumbnailCreation()
	{
		HikeEffectsFactory.loadPreviewThumbnail(this.getIcon(), this.filter, this);
	}

	public FilterType getFilter()
	{
		return filter;
	}

	@Override
	public void onFilterApplied(Bitmap preview)
	{
		if(preview==null && tried == 0)
		{
			tried = 1;
			initiateThumbnailCreation();
			return;
		}
		else if(preview!=null)
		{
			setImage(preview);
			return;
		}
		else
		{
			//Do Nothing
		}
	}

}
