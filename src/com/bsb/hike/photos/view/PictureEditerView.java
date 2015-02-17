package com.bsb.hike.photos.view;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.bsb.hike.R;
import com.bsb.hike.photos.FilterTools.FilterType;

public class PictureEditerView extends FrameLayout
{
	private CanvasImageView doodleLayer;

	private VignetteImageView vignetteLayer;

	private EffectsView effectLayer;

	private ColorMatrixColorFilter currentEffect;

	private boolean enableDoodling = false, enableText = false;

	private Bitmap imageOriginal, imageEdited;

	public PictureEditerView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		doodleLayer = new CanvasImageView(context, attrs);
		vignetteLayer = new VignetteImageView(context, attrs);
		effectLayer = new EffectsView(context, attrs);
		addView(effectLayer);
		addView(vignetteLayer);
		addView(doodleLayer);
		// TODO Auto-generated constructor stub
	}

	public void setBrushWidth(int width)
	{
		doodleLayer.setStrokeWidth(width);
	}

	public void applyFilter(FilterType filter)
	{
		currentEffect = effectLayer.applyEffect(filter, 100);
	}

	@SuppressWarnings("deprecation")
	public void loadImageFromFile(String FilePath)
	{
		imageOriginal = BitmapFactory.decodeFile(FilePath);
		effectLayer.handleImage(new BitmapDrawable(imageOriginal));
	}

	public void enableDoodling()
	{
		doodleLayer.Refresh(imageOriginal);
		doodleLayer.setDrawEnabled(true);
	}

	public Bitmap getImageOriginal()
	{
		return imageOriginal;
	}

	public void disableDoodling()
	{

	}

	public void setBrushColor(int Color)
	{
		doodleLayer.setColor(Color);
	}

	public File saveImage()
	{

		imageEdited = updateSat(imageOriginal, currentEffect);
		String root = Environment.getExternalStorageDirectory().toString();
		File myDir = new File(root + "/colormatrix");
		myDir.mkdirs();
		Random generator = new Random();
		int n = 10000;
		n = generator.nextInt(n);
		String fname = "Image-" + n + ".jpg";
		File file = new File(myDir, fname);
		if (file.exists())
		{
			file.delete();
		}

		FileOutputStream out = null;
		try
		{
			out = new FileOutputStream(file);
			imageEdited.compress(Bitmap.CompressFormat.JPEG, 100, out);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (out != null)
			{
				try
				{
					out.flush();
					out.close();
				}
				catch (IOException e)
				{
					// Do nothing
				}
			}
		}

		if (file.exists())
		{
			return file;
		}
		else
		{
			return null;
		}
	}

	private Bitmap updateSat(Bitmap src, ColorMatrixColorFilter filter)
	{

		int w = src.getWidth();
		int h = src.getHeight();

		Bitmap bitmapResult = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		Canvas canvasResult = new Canvas(bitmapResult);
		Paint paint = new Paint();
		paint.setColorFilter(filter);
		canvasResult.drawBitmap(src, 0, 0, paint);
		Bitmap temp = Bitmap.createScaledBitmap(doodleLayer.getBitmap(), src.getWidth(), src.getHeight(), true);
		canvasResult.drawBitmap(temp, 0, 0, doodleLayer.getPaint());
		return bitmapResult;
	}

	// public void loadImage()
	// {
	// effectLayer.handleImage((BitmapDrawable) getResources().getDrawable(R.drawable.test));
	// imageOriginal = ((BitmapDrawable) getResources().getDrawable(R.drawable.test)).getBitmap();
	// }

}