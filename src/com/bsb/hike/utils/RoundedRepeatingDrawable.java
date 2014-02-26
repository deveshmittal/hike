package com.bsb.hike.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Used for creating a drawable which consists of a rounded bitmap that is repeating.
 */
public class RoundedRepeatingDrawable extends Drawable
{

	private final float mCornerRadius;

	private final RectF mRect;

	private final BitmapShader mBitmapShader;

	private final Paint mTilePaint;

	public RoundedRepeatingDrawable(Bitmap bitmap, float cornerRadius)
	{
		mCornerRadius = cornerRadius;
		mRect = new RectF();

		mBitmapShader = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);

		mTilePaint = new Paint();
		mTilePaint.setAntiAlias(true);
		mTilePaint.setShader(mBitmapShader);
	}

	@Override
	protected void onBoundsChange(Rect bounds)
	{
		super.onBoundsChange(bounds);
		mRect.set(0, 0, bounds.width(), bounds.height());
	}

	@Override
	public void draw(Canvas canvas)
	{
		canvas.drawRoundRect(mRect, mCornerRadius, mCornerRadius, mTilePaint);
	}

	@Override
	public int getOpacity()
	{
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	public void setAlpha(int alpha)
	{
		mTilePaint.setAlpha(alpha);
	}

	@Override
	public void setColorFilter(ColorFilter cf)
	{
		mTilePaint.setColorFilter(cf);
	}
}