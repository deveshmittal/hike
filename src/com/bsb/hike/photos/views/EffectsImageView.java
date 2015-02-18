package com.bsb.hike.photos.views;

import android.content.Context;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.bsb.hike.photos.HikeEffectsFactory;
import com.bsb.hike.photos.HikePhotosUtils.FilterTools.FilterType;
/**
 * @author akhiltripathi
 *
 *         Custom View Class extends ImageView in android
 * 
 *         An object of EffectsImageView represents a layer on which the effects filters will be applied
 * 
 *        
 *
 */
public class EffectsImageView extends ImageView
{

	private BitmapDrawable currentImage;

	public EffectsImageView(Context context)
	{
		super(context);
	}

	public EffectsImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public EffectsImageView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
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
