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

	private Bitmap currentImage;

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

	public Bitmap getBitmapWithEffectsApplied()
	{
		return currentImage;
	}

	public void handleImage(Bitmap image)
	{
		currentImage = image;
		this.setImageBitmap(image);

	}
	
	public void changeDisplayImage(Bitmap image)
	{
		this.setImageBitmap(image);
	}

	public void applyEffect(FilterType filter, float value,OnFilterAppliedListener listener)
	{
		 HikeEffectsFactory.applyFilterToBitmap(currentImage,listener, filter, this.getContext());
	}

	

}
