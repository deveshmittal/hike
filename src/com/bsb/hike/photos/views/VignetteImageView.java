package com.bsb.hike.photos.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Bitmap.Config;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * @author akhiltripathi
 *
 *         Custom View Class extends ImageView in android
 * 
 *         Used in applying vignette (radial color fill) over the image as a seperate layer.
 *
 */

class VignetteImageView extends ImageView
{

	public VignetteImageView(Context context)
	{
		super(context);
	}

	public VignetteImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public VignetteImageView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
	}

	private Bitmap makeRadGrad(Bitmap bitmap2, int radius)
	{

		int w = bitmap2.getWidth();
		int h = bitmap2.getHeight();

		RadialGradient gradient = new RadialGradient(w / 2, h / 2, (float) (radius), 0x00000044, 0xFF000000, android.graphics.Shader.TileMode.CLAMP);
		Paint p = new Paint();
		p.setDither(true);
		p.setShader(gradient);

		Bitmap bitmap = Bitmap.createBitmap(w, h, Config.ARGB_8888);
		Canvas c = new Canvas(bitmap);
		c.drawBitmap(bitmap2, 0, 0, null);
		c.drawCircle(w / 2, h / 2, (float) (radius), p);
		return bitmap;
	}

}
