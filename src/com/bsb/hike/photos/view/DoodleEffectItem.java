package com.bsb.hike.photos.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;
import android.util.AttributeSet;

import com.bsb.hike.photos.PhotoEditerTools;

public class DoodleEffectItem extends EffectItem
{
	private int BrushWidth;

	private int BrushColor;

	private int RingColor;

	public DoodleEffectItem(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		// TODO Auto-generated constructor stub
		BrushWidth = PhotoEditerTools.dpToPx(context, 30);
		BrushColor = 0xFF000000;
		RingColor = 0xFFFFFFFF;
		setImage(getCircleIcon());
	}

	public void Refresh()
	{

		setImage(getCircleIcon());
		invalidate();
	}

	public int getBrushColor()
	{
		return BrushColor;
	}

	public void setBrushColor(int Color)
	{
		BrushColor = Color;
	}

	public int getRingColor()
	{
		return RingColor;
	}

	public void setRingColor(int Color)
	{
		RingColor = Color;
	}

	private Bitmap getCircleIcon()
	{
		int diameter = BrushWidth + PhotoEditerTools.dpToPx(this.getContext(), 4);
		Bitmap bitmap = Bitmap.createBitmap(diameter, diameter, Config.ARGB_8888);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(RingColor);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawCircle(diameter / 2, diameter / 2, (diameter / 2), paint);
		paint.setColor(BrushColor);
		canvas.drawCircle(diameter / 2, diameter / 2, (BrushWidth / 2), paint);
		return bitmap;
	}

	public void setBrushWidth(int brushWidth)
	{
		BrushWidth = brushWidth;
	}

	public int getBrushWidth()
	{
		return BrushWidth;
	}

}