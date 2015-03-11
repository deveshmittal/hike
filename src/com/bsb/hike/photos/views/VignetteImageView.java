package com.bsb.hike.photos.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import com.bsb.hike.photos.HikePhotosUtils.FilterTools.FilterType;

/**
 *         Custom View Class extends ImageView in android
 * 
 *         Used in applying vignette (radial color fill) over the image as a seperate layer.
 *         
 * @author akhiltripathi
 */

class VignetteImageView extends ImageView
{
	private int width;

	private Bitmap vignetteBitmap;

	private FilterType filter;

	public void setFilter(FilterType Type)
	{
		this.filter = Type;
	}

	public VignetteImageView(Context context)
	{
		super(context);
		this.filter = FilterType.ORIGINAL;
	}

	public VignetteImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.filter = FilterType.ORIGINAL;
	}

	public VignetteImageView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		this.filter = FilterType.ORIGINAL;
	}

	public void getMeasure(Bitmap bitmap)
	{
		if (bitmap != null)
		{
			width = bitmap.getWidth();
			setVignetteforFilter(bitmap);
		}
	}

	public Bitmap getVignetteBitmap()
	{
		return vignetteBitmap;
	}

	/**
	 * 
	 * Draws a vignette on the layer of provided image size respective to the current set filter.
	 * 
	 * @author akhiltripathi
	 * 
	 * @param original
	 */
	public void setVignetteforFilter(Bitmap original)
	{

		if (filter == null)
		{
			return;
		}

		width = original.getWidth();
		int colors[];
		float stops[];
		vignetteBitmap = Bitmap.createBitmap(width, width, Config.ARGB_8888);
		
		switch (filter)
		{
		case X_PRO_2:
			// Vignette: Stop 1 = #000000 84%, Opacity = 0%; Stop 2 = #232443 120%, Opacity = 100%
			colors = new int[] { 0x00000000, 0x00000000, 0xFF232443 };
			stops = new float[] { 0.0f, 0.92f / 1.5f, 1.0f };
			makeRadialGradient(1.5f, colors, stops);
			break;
		case POLAROID:
			colors = new int[] { 0x00000000, 0xBB232443 };
			stops = new float[] { 0.0f, 1.0f };
			makeRadialGradient(1.5f, colors, stops);
			break;
		case RETRO:
		case KELVIN:
		case EARLYBIRD:
		case BGR:
			// Vignette: Stop 1 = #000000 74%, Opacity = 0%; Stop 2 = #000000 120%, Opacity = 100%
			colors = new int[] { 0x00000000,0x00000000, 0xFF000000 };
			stops = new float[] { 0.0f,0.98f/1.5f, 1.0f };
			makeRadialGradient(1.5f, colors, stops);
			break;
		case APOLLO:
			// Vignette Stop 1: #18363f, Position 72%, Opacity 0% Stop 2: #18363f, Position 120%, Opacity 100%
			colors = new int[] { 0x00000000, 0x0018363F, 0xFF18363F };
			stops = new float[] { 0.0f, 0.92f / 1.6f, 1.0f };
			makeRadialGradient(1.6f, colors, stops);
			break;
		default:
			break;
		}
		this.setImageBitmap(vignetteBitmap);
	}

	private void makeRadialGradient(float radiusRatio, int[] colors, float[] stops)
	{

		float radius = radiusRatio * width/2;

		RadialGradient gradient = new RadialGradient(width/2,width/2, radius, colors, stops, android.graphics.Shader.TileMode.CLAMP);

		Paint p = new Paint();
		p.setDither(true);
		p.setShader(gradient);

		Canvas c = new Canvas(vignetteBitmap);

		c.drawCircle(width/2, width/2, (radius), p);
	}

}
