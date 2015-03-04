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
import android.os.Environment;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.FrameLayout;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.photos.HikePhotosListener;
import com.bsb.hike.photos.HikePhotosUtils;
import com.bsb.hike.photos.HikeEffectsFactory.OnFilterAppliedListener;
import com.bsb.hike.photos.HikePhotosUtils.FilterTools.FilterType;
import com.bsb.hike.photos.views.CanvasImageView.OnDoodleStateChangeListener;
import com.bsb.hike.utils.Utils;

/**
 * @author akhiltripathi Custom View extends FrameLayout Packs all the editing
 *         layers <filter layer,vignette layer ,doodle layer> into a single view
 *         ,in same z-order
 * 
 */
public class PhotosEditerFrameLayoutView extends FrameLayout implements
		OnFilterAppliedListener {
	private CanvasImageView doodleLayer;

	private VignetteImageView vignetteLayer;

	private EffectsImageView effectLayer;

	private ColorMatrixColorFilter currentEffect;

	private boolean enableDoodling, enableText, savingFinal;

	private Bitmap imageOriginal, imageEdited, imageScaled,
			scaledImageOriginal;

	private HikeFileType mFileType;
	private String mOriginalName;
	private HikePhotosListener mListener;

	public PhotosEditerFrameLayoutView(Context context) {
		super(context);
		doodleLayer = new CanvasImageView(context);
		vignetteLayer = new VignetteImageView(context);
		effectLayer = new EffectsImageView(context);
		addView(effectLayer);
		addView(vignetteLayer);
		addView(doodleLayer);
		enableDoodling = false;
		enableText = false;
		savingFinal = false;
	}

	public PhotosEditerFrameLayoutView(Context context, AttributeSet attrs) {
		super(context, attrs);
		doodleLayer = new CanvasImageView(context, attrs);
		vignetteLayer = new VignetteImageView(context, attrs);
		effectLayer = new EffectsImageView(context, attrs);
		addView(effectLayer);
		addView(vignetteLayer);
		addView(doodleLayer);
		enableDoodling = false;
		enableText = false;
		savingFinal = false;
		// TODO Auto-generated constructor stub
	}

	public PhotosEditerFrameLayoutView(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		doodleLayer = new CanvasImageView(context, attrs, defStyleAttr);
		vignetteLayer = new VignetteImageView(context, attrs, defStyleAttr);
		effectLayer = new EffectsImageView(context, attrs, defStyleAttr);
		addView(effectLayer);
		addView(vignetteLayer);
		addView(doodleLayer);
		enableDoodling = false;
		enableText = false;
		savingFinal = false;
	}

	public Bitmap getScaledImageOriginal() {
		if (scaledImageOriginal == null) {
			scaledImageOriginal = Bitmap.createScaledBitmap(imageOriginal,
					HikePhotosUtils.dpToPx(getContext(),
							HikeConstants.HikePhotos.PREVIEW_THUMBNAIL_WIDTH),
					HikePhotosUtils.dpToPx(getContext(),
							HikeConstants.HikePhotos.PREVIEW_THUMBNAIL_HEIGHT),
					false);
		}
		return scaledImageOriginal;
	}

	public void setBrushWidth(int width) {
		doodleLayer.setStrokeWidth(width);
	}

	public void applyFilter(FilterType filter) {
		effectLayer.applyEffect(filter,
				HikeConstants.HikePhotos.DEFAULT_FILTER_APPLY_PERCENTAGE, this);
		effectLayer.invalidate();
		vignetteLayer.setFilter(filter);
	}

	/**
	 * 
	 * @param FilePath
	 *            : absolute address of the file to be handled by the editor
	 *            object
	 */
	@SuppressWarnings("deprecation")
	public void loadImageFromFile(String FilePath) {
		imageOriginal = BitmapFactory.decodeFile(FilePath);
		DisplayMetrics metrics = getContext().getResources()
				.getDisplayMetrics();
		int width = metrics.widthPixels;
		if (width < imageOriginal.getWidth()) {
			imageScaled = Bitmap.createScaledBitmap(imageOriginal, width,
					width, false);
			effectLayer.handleImage(imageScaled, true);
		} else {
			effectLayer.handleImage(imageOriginal, false);
			imageScaled = imageOriginal;
		}

	}

	public void loadImageFromBitmap(Bitmap bmp) {
		effectLayer.handleImage(bmp, false);
	}

	public void enableDoodling() {
		doodleLayer.setDrawEnabled(true);
	}

	public void disableDoodling() {
		doodleLayer.setDrawEnabled(false);
	}

	public void setBrushColor(int Color) {
		doodleLayer.setColor(Color);
	}

	public void saveImage(HikeFileType fileType, String originalName,
			HikePhotosListener listener) {
		doodleLayer.getMeasure(imageScaled);
		vignetteLayer.getMeasure(imageOriginal);

		this.mFileType = fileType;
		this.mOriginalName = originalName;
		this.mListener = listener;
		
		savingFinal = true;
		effectLayer.getBitmapWithEffectsApplied(imageOriginal, this);

		

	}

	public void undoLastDoodleDraw() {
		doodleLayer.onClickUndo();

	}

	public void setOnDoodlingStartListener(OnDoodleStateChangeListener listener) {
		doodleLayer.setOnDoodlingStartListener(listener);
	}

	private void saveImagetoFile() {
		File file = null;
		if (mFileType == HikeFileType.IMAGE) {
			try {
				file = File
						.createTempFile(
								Utils.getOriginalFile(mFileType, mOriginalName),
								".jpg");
			} catch (IOException e) {
				e.printStackTrace();
				mListener.onFailure();
			}
		} else {
			File myDir = Environment
					.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
			myDir.mkdir();
			String fname = Utils.getOriginalFile(mFileType, mOriginalName);
			file = new File(myDir, fname);
		}

		if (file.exists()) {
			file.delete();
		}

		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
			imageEdited.compress(Bitmap.CompressFormat.JPEG, 100, out);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				try {
					out.flush();
					out.close();
				} catch (IOException e) {
					// Do nothing
				}
			}
		}

		if (file.exists()) {
			mListener.onComplete(file);
		} else {
			mListener.onFailure();
		}

	}

	private void flattenLayers() {

		if (imageEdited != null) {
			Canvas canvasResult = new Canvas(imageEdited);

			if (vignetteLayer.getVignetteBitmap() != null) {
				canvasResult.drawBitmap(vignetteLayer.getVignetteBitmap(), 0,
						0, null);
			}
			if (doodleLayer.getBitmap() != null) {
				Bitmap temp = Bitmap.createScaledBitmap(doodleLayer.getBitmap(), imageOriginal.getWidth(), imageOriginal.getHeight(), false);
				canvasResult.drawBitmap(temp, 0, 0,doodleLayer.getPaint());
			}
		}

		saveImagetoFile();

	}

	@Override
	public void onFilterApplied(Bitmap preview) {
		// TODO Auto-generated method stub
		if (!savingFinal) {
			vignetteLayer.setVignetteforFilter(preview);
			effectLayer.changeDisplayImage(preview);
		} else {
			imageEdited = preview;
			flattenLayers();
		}

	}
}