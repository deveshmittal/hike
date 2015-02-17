package com.bsb.hike.photos.view;

import android.content.Context;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.bsb.hike.photos.HikeEffectsFactory;
import com.bsb.hike.photos.FilterTools.FilterType;

public class EffectsView extends ImageView
{

	private BitmapDrawable currentImage;

	public EffectsView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public BitmapDrawable getBitmapWithEffectsApplied()
	{
		return currentImage;
	}

	public void handleImage(BitmapDrawable image)
	{
		currentImage = image;
		this.setImageDrawable(image);

	}

	public ColorMatrixColorFilter applyEffect(FilterType filter, float value)
	{
		return HikeEffectsFactory.applyFiltertoBitmapDrawable(currentImage, filter, value);
	}

}
