package com.bsb.hike.photos.views;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.photos.HikePhotosUtils;
import com.bsb.hike.photos.HikePhotosUtils.FilterTools.FilterType;
import com.bsb.hike.photos.views.CanvasImageView.OnDoodleStateChangeListener;
import com.bsb.hike.utils.Utils;

/**
 * @author akhiltripathi Custom View extends FrameLayout Packs all the editing layers <filter layer,vignette layer ,doodle layer> into a single view ,in same z-order
 * 
 */
public class PhotosEditerFrameLayoutView extends FrameLayout
{
	private CanvasImageView doodleLayer;

	private VignetteImageView vignetteLayer;

	private EffectsImageView effectLayer;

	private ColorMatrixColorFilter currentEffect;

	private boolean enableDoodling = false, enableText = false;

	private Bitmap imageOriginal, imageEdited, scaledImageOriginal;

	public PhotosEditerFrameLayoutView(Context context)
	{
		super(context);
		doodleLayer = new CanvasImageView(context);
		vignetteLayer = new VignetteImageView(context);
		effectLayer = new EffectsImageView(context);
		addView(effectLayer);
		addView(vignetteLayer);
		addView(doodleLayer);
	}

	public PhotosEditerFrameLayoutView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		doodleLayer = new CanvasImageView(context, attrs);
		vignetteLayer = new VignetteImageView(context, attrs);
		effectLayer = new EffectsImageView(context, attrs);
		addView(effectLayer);
		addView(vignetteLayer);
		addView(doodleLayer);
		// TODO Auto-generated constructor stub
	}

	public PhotosEditerFrameLayoutView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		doodleLayer = new CanvasImageView(context, attrs, defStyleAttr);
		vignetteLayer = new VignetteImageView(context, attrs, defStyleAttr);
		effectLayer = new EffectsImageView(context, attrs, defStyleAttr);
		addView(effectLayer);
		addView(vignetteLayer);
		addView(doodleLayer);
	}

	public Bitmap getScaledImageOriginal()
	{
		if (scaledImageOriginal == null)
		{
			scaledImageOriginal = Bitmap.createScaledBitmap(imageOriginal, HikePhotosUtils.dpToPx(getContext(), HikeConstants.HikePhotos.PREVIEW_THUMBNAIL_WIDTH),
					HikePhotosUtils.dpToPx(getContext(), HikeConstants.HikePhotos.PREVIEW_THUMBNAIL_HEIGHT), false);
		}
		return scaledImageOriginal;
	}

	public void setBrushWidth(int width)
	{
		doodleLayer.setStrokeWidth(width);
	}

	public void applyFilter(FilterType filter)
	{
		currentEffect = effectLayer.applyEffect(filter, HikeConstants.HikePhotos.DEFAULT_FILTER_APPLY_PERCENTAGE);
		effectLayer.invalidate();
	}

	/**
	 * 
	 * @param FilePath
	 *            : absolute address of the file to be handled by the editor object
	 */
	@SuppressWarnings("deprecation")
	public void loadImageFromFile(String FilePath)
	{
		imageOriginal = BitmapFactory.decodeFile(FilePath);
		effectLayer.handleImage(new BitmapDrawable(imageOriginal));
	}

	public void loadImageFromBitmap(Bitmap bmp)
	{
		effectLayer.handleImage(new BitmapDrawable(bmp));
	}

	public void enableDoodling()
	{
		doodleLayer.setDrawEnabled(true);
	}

	public void disableDoodling()
	{
		doodleLayer.setDrawEnabled(false);
	}

	public void setBrushColor(int Color)
	{
		doodleLayer.setColor(Color);
	}

	public File saveImage()
	{
		doodleLayer.getMeasure(imageOriginal);
		imageEdited = flattenLayersToBitmap(imageOriginal, currentEffect);
		File myDir = new File(Utils.getFileParent(HikeFileType.IMAGE, false));
		myDir.mkdir();
		String fname = Utils.getOriginalFile(HikeFileType.IMAGE, null);
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

	public void undoLastDoodleDraw()
	{
		doodleLayer.onClickUndo();

	}

	public void setOnDoodlingStartListener(OnDoodleStateChangeListener listener)
	{
		doodleLayer.setOnDoodlingStartListener(listener);
	}

	private Bitmap flattenLayersToBitmap(Bitmap src, ColorMatrixColorFilter filter)
	{

		int w = src.getWidth();
		int h = src.getHeight();

		Bitmap bitmapResult = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		Canvas canvasResult = new Canvas(bitmapResult);
		Paint paint = new Paint();
		paint.setColorFilter(filter);
		canvasResult.drawBitmap(src, 0, 0, paint);
		if (doodleLayer.getBitmap() != null)
		{
			Bitmap temp = Bitmap.createScaledBitmap(doodleLayer.getBitmap(), src.getWidth(), src.getHeight(), true);
			canvasResult.drawBitmap(temp, 0, 0, doodleLayer.getPaint());
		}
		return bitmapResult;
	}

}