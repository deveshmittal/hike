package com.bsb.hike.photos.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;

import com.bsb.hike.photos.HikeEffectsFactory;
import com.bsb.hike.photos.HikeEffectsFactory.OnPreviewReadyListener;
import com.bsb.hike.photos.HikePhotosUtils.FilterTools.FilterType;

/**
 * @author akhiltripathi
 *
 *         Custom View for the Filter Preview thumbnails
 * 
 */

public class FilterEffectItemLinearLayout extends EffectItemLinearLayout implements OnPreviewReadyListener
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

	public void setFilter(Context context, FilterType type)
	{
		this.filter = type;
		initiateThumbnailCreation();

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
	public void onPreviewReady(Bitmap preview)
	{
		// TODO Auto-generated method stub
		setImage(preview);
	}

}
