package com.bsb.hike.photos.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ColorMatrixColorFilter;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.bsb.hike.photos.HikeEffectsFactory;
import com.bsb.hike.photos.HikeEffectsFactory.OnFilterAppliedListener;
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

	private Bitmap originalImage,currentImage;
	private FilterType currentFilter;
	private boolean isScaled;

	public EffectsImageView(Context context)
	{
		super(context);
		currentFilter = FilterType.ORIGINAL;
		
	}

	public EffectsImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		currentFilter = FilterType.ORIGINAL;
	}

	public EffectsImageView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		currentFilter = FilterType.ORIGINAL;
	}

	public void getBitmapWithEffectsApplied(Bitmap bitmap,OnFilterAppliedListener listener)
	{
		if(!isScaled)
			listener.onFilterApplied(currentImage.copy(currentImage.getConfig(), true));
		
		else
			HikeEffectsFactory.applyFilterToBitmap(bitmap,listener, currentFilter);
			
	}

	public void handleImage(Bitmap image,boolean hasBeenScaled)
	{
		isScaled = hasBeenScaled;
		originalImage = image;
		currentImage = image;
		this.setImageBitmap(image);

	}
	
	public void changeDisplayImage(Bitmap image)
	{
		currentImage = image;
		this.setImageBitmap(image);
	}

	public void applyEffect(FilterType filter, float value,OnFilterAppliedListener listener)
	{
		 currentFilter = filter ; 
		 HikeEffectsFactory.applyFilterToBitmap(originalImage,listener, filter);
	}

	

}
