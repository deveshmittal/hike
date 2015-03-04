package com.bsb.hike.photos.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;

import com.bsb.hike.R;
import com.bsb.hike.photos.HikeEffectsFactory;
import com.bsb.hike.photos.HikeEffectsFactory.OnFilterAppliedListener;
import com.bsb.hike.photos.HikePhotosListener;
import com.bsb.hike.photos.HikePhotosUtils;
import com.bsb.hike.photos.HikePhotosUtils.FilterTools.FilterType;

/**
 * @author akhiltripathi
 *
 *         Custom View for the Filter Preview thumbnails
 * 
 */

public class FilterEffectItemLinearLayout extends EffectItemLinearLayout implements OnFilterAppliedListener
{
	private FilterType filter;

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
		this.setForegroundColor(R.color.white);
		HikePhotosUtils.FilterTools.setCurrentFilterItem(this);
	}
	
	public void unSelect()
	{
		this.setForegroundColor(R.color.photos_filters_font_color);
	}

	public void setFilter(Context context, FilterType type)
	{
		this.filter = type;
		initiateThumbnailCreation();

		if (type == HikePhotosUtils.FilterTools.getSelectedFilter())
		{
			this.setForegroundColor(R.color.photos_filters_selected_font_color);
			HikePhotosUtils.FilterTools.setCurrentFilterItem(this);
		}
		else
		{
			this.setForegroundColor(R.color.photos_filters_font_color);
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
		// TODO Auto-generated method stub
		setImage(preview);
	}

}
