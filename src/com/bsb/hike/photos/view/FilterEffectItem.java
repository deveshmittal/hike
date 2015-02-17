package com.bsb.hike.photos.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;

import com.bsb.hike.photos.FilterTools.FilterType;
import com.bsb.hike.photos.HikeEffectsFactory;
import com.bsb.hike.photos.PhotoEditerTools;
import com.bsb.hike.photos.HikeEffectsFactory.OnPreviewReadyListener;

//preview type to be changed to bitmap later

public class FilterEffectItem extends EffectItem implements OnPreviewReadyListener
{
	private FilterType filter;

	public FilterEffectItem(Context context, AttributeSet attrs)
	{
		super(context, attrs);

	}

	public void init(Bitmap preview, String Title)
	{

		preview = Bitmap.createScaledBitmap(preview, PhotoEditerTools.dpToPx(this.getContext(), 80), PhotoEditerTools.dpToPx(this.getContext(), 80), false);
		this.setImage(preview);
		this.setText(Title);

	}

	public void setFilter(Context context, FilterType type)
	{
		this.filter = type;
		HikeEffectsFactory.LoadPreviewThumbnail(this.getIcon(), type, this);
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
