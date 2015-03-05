package com.bsb.hike.photos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.RenderScript;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.photos.HikePhotosUtils.FilterTools.FilterType;

/**
 * @author akhiltripathi
 * 
 *         Factory model class. Effect Filter being applied using ColorMatrix class in android.
 * 
 * @see http://developer.android.com/reference/android/graphics/ColorMatrix.html
 */

public final class HikeEffectsFactory
{

	private static HikeEffectsFactory instance;// singleton instance

	private Bitmap mBitmapIn;

	private Bitmap mBitmapOut;

	private RenderScript mRS;

	private Allocation mInAllocation;

	private Allocation mOutAllocations;

	private ScriptC_HikePhotosEffects mScript;

	private boolean isBasicFilter;

	private boolean isGPUFree = true;

	private void LoadRenderScript(Bitmap image)
	{
		// Initialize RS
		if (mRS == null)
		{
			mRS = RenderScript.create(HikeMessengerApp.getInstance().getApplicationContext());
			mScript = new ScriptC_HikePhotosEffects(mRS);

		}

		// Allocate buffer
		mBitmapIn = image;
		mInAllocation = Allocation.createFromBitmap(mRS, mBitmapIn);
		// Load script
	}

	/**
	 * Method initiates an async task to apply filter to the provided thumbnail (obtained by scaling the image to be handled). Run on a background since loading preview can take
	 * some time in case of complex filters or large filter count. Till then the original image is displayed.
	 */
	public static void loadPreviewThumbnail(Bitmap scaledOriginal, FilterType type, OnFilterAppliedListener listener)
	{
		if (instance == null)
			instance = new HikeEffectsFactory();

		instance.LoadRenderScript(scaledOriginal);
		instance.beginEffectAsyncTask(listener, type, false);

	}

	private Bitmap applyColorMatrixToBitmap(Bitmap bitmap, ColorMatrix matrix)
	{
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
		ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
		Canvas canvasResult = new Canvas(output);
		Paint paint = new Paint();
		paint.setColorFilter(filter);
		canvasResult.drawBitmap(bitmap, 0, 0, paint);
		return output;
	}

	private ColorMatrix getColorMatrixforFilter(FilterType type, boolean pre, float value)
	{
		if (type == null)
		{
			return null;
		}
		ColorMatrix filterColorMatrix = null;
		switch (type)
		{

		case SEPIA:
			filterColorMatrix = getSepiaColorMatrix(value);
			break;
		case GRAYSCALE:
			filterColorMatrix = getBlackAndWhiteColorMatrix();
			break;
		case POLAROID:
			filterColorMatrix = getPolaroidColorMatrix();
			break;
		case FADED:
			filterColorMatrix = getFadedColorMatrix();
			break;
		case BGR:
			filterColorMatrix = getBGRColorMatrix();
			break;
		case E1977:
			if (pre)
			{
				filterColorMatrix = null;
			}
			else
			{
				filterColorMatrix = getContrastColorMatrix(-20f);
			}
			break;
		case X_PRO_2:
			if (pre)
			{
				return null;
			}
			else
			{
				filterColorMatrix = getContrastColorMatrix(30f);
			}
			break;
		case APOLLO:
			if (pre)
			{
				filterColorMatrix = getSaturationColorMatrix(0.5f);
			}
			else
			{
				filterColorMatrix = getBrightnessColorMatrix(1.4f);
				filterColorMatrix.setConcat(getContrastColorMatrix(-20f), filterColorMatrix);
			}
			break;
		case BRANNAN:
			if (pre)
			{
				filterColorMatrix = getBrightnessColorMatrix(1.1f);
				filterColorMatrix.setConcat(getContrastColorMatrix(50f), filterColorMatrix);
			}
			else
			{
				filterColorMatrix = getSaturationColorMatrix(0.7f);
			}
			break;
		case NASHVILLE:
			if (!pre)
			{
				filterColorMatrix = getBrightnessColorMatrix(1.3f);
				filterColorMatrix.setConcat(getContrastColorMatrix(15f), filterColorMatrix);
			}
			break;
		case EARLYBIRD:
			if (pre)
			{
				filterColorMatrix = getSaturationColorMatrix(0.68f);
				filterColorMatrix.setConcat(getBrightnessColorMatrix(1.15f), filterColorMatrix);

			}
			break;
		case INKWELL:
			if (pre)
			{
				filterColorMatrix = getSaturationColorMatrix(0);
			}
			else
			{
				filterColorMatrix = getBrightnessColorMatrix(0.9f);
				filterColorMatrix.setConcat(getContrastColorMatrix(50f), filterColorMatrix);
			}
			break;
		case LO_FI:
			if (pre)
			{
				filterColorMatrix = getBrightnessColorMatrix(1.5f);
				filterColorMatrix.setConcat(getContrastColorMatrix(30f), filterColorMatrix);
			}
			break;
		default:
			filterColorMatrix = null;

		}
		return filterColorMatrix;
	}

	/**
	 * @param filter
	 *            : ColorMatrix of the filter whose partial matrix is required value: %of filter to be applied.0 return Identity and 100 return the input filter
	 * 
	 * @return partial filter of specified percentage
	 */

	private ColorMatrix partialFilterColorMatrix(ColorMatrix filter, float value)
	{
		value = value / 100;
		float[] partialArray = filter.getArray();
		for (int i = 0; i < partialArray.length; i++)
		{
			if (i % 6 == 0)
				partialArray[i] = 1 - (1 - partialArray[i]) * value;
			else
				partialArray[i] *= value;
		}
		ColorMatrix ret = new ColorMatrix(partialArray);

		return ret;
	}

	/**
	 * Method used in saving the final filter onto a bitmap.
	 * 
	 * @param bitmap
	 *            : The bitmap to which the filters have to be applied matrix : The Colormatrix object for the filter type to be applied.
	 * @return Bitmap with the given matrix filter applied to the given bitmap
	 * 
	 */

	public static void applyFilterToBitmap(Bitmap bitmap, OnFilterAppliedListener listener, FilterType type)
	{
		if (instance == null)
			instance = new HikeEffectsFactory();

		instance.LoadRenderScript(bitmap);
		instance.beginEffectAsyncTask(listener, type, false);

	}

	private void beginEffectAsyncTask(OnFilterAppliedListener listener, FilterType type, boolean blur)
	{
		HikeHandlerUtil.getInstance().postRunnableWithDelay(new ApplyFilterTask(type, listener, blur), 0);

	}

	private ColorMatrix getCustomEffectColorMatrix(ColorMatrix[] effects)
	{
		ColorMatrix ret = new ColorMatrix();
		for (ColorMatrix effect : effects)
		{
			ret.setConcat(effect, ret);
		}
		return ret;
	}

	private ColorMatrix getSaturationColorMatrix(float value)
	{
		ColorMatrix ret = new ColorMatrix();
		ret.setSaturation(value);
		return ret;
	}

	private ColorMatrix getOriginalColorMatrix()
	{

		return null;
	}

	private ColorMatrix getInvertColorsColorMatrix(float value)
	{
		float[] array = new float[] { -1, 0, 0, 0, 255 - value, 0, -1, 0, 0, 255 - value, 0, 0, -1, 0, 255 - value, 0, 0, 0, 1, 0 };
		ColorMatrix matrix = new ColorMatrix(array);

		return matrix;
	}

	private ColorMatrix getBlackAndWhiteColorMatrix()
	{

		ColorMatrix matrixA = new ColorMatrix();
		matrixA.setSaturation(0);

		return matrixA;
	}

	private ColorMatrix getContrastColorMatrix(float value)
	{
		float scale = 1 + value / 100;
		float translate = (-.5f * scale + .5f) * 255.f;
		float[] array = new float[] { scale, 0, 0, 0, translate, 0, scale, 0, 0, translate, 0, 0, scale, 0, translate, 0, 0, 0, 1, 0 };
		ColorMatrix matrix = new ColorMatrix(array);

		return matrix;
	}

	private ColorMatrix getBrightnessColorMatrix(float value)
	{
		value = (value - 1) * 100;
		float[] array = new float[] { 1, 0, 0, 0, value, 0, 1, 0, 0, value, 0, 0, 1, 0, value, 0, 0, 0, 1, 0 };
		ColorMatrix matrix = new ColorMatrix(array);

		return matrix;
	}

	private ColorMatrix getBGRColorMatrix()
	{
		float[] array = new float[] { 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0 };
		ColorMatrix matrix = new ColorMatrix(array);

		return matrix;
	}

	private ColorMatrix getOpacityColorMatrix(float value)
	{
		float[] array = new float[] { 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, value, 0 };
		ColorMatrix matrix = new ColorMatrix(array);

		return matrix;
	}

	private ColorMatrix getHueColorMatrix(float value)
	{
		value = value / 180f * (float) Math.PI;

		float cosVal = (float) Math.cos(value);
		float sinVal = (float) Math.sin(value);
		float lumR = 0.213f;
		float lumG = 0.715f;
		float lumB = 0.072f;
		float[] mat = new float[] { lumR + cosVal * (1 - lumR) + sinVal * (-lumR), lumG + cosVal * (-lumG) + sinVal * (-lumG), lumB + cosVal * (-lumB) + sinVal * (1 - lumB), 0, 0,
				lumR + cosVal * (-lumR) + sinVal * (0.143f), lumG + cosVal * (1 - lumG) + sinVal * (0.140f), lumB + cosVal * (-lumB) + sinVal * (-0.283f), 0, 0,
				lumR + cosVal * (-lumR) + sinVal * (-(1 - lumR)), lumG + cosVal * (-lumG) + sinVal * (lumG), lumB + cosVal * (1 - lumB) + sinVal * (lumB), 0, 0, 0f, 0f, 0f, 1f,
				0f, 0f, 0f, 0f, 0f, 1f };
		ColorMatrix colorMatrix = new ColorMatrix(mat);
		return colorMatrix;

	}

	private ColorMatrix getBinaryColorMatrix()
	{
		ColorMatrix colorMatrix = new ColorMatrix();
		colorMatrix.setSaturation(0);

		float m = 255f;
		float t = -255 * 128f;
		ColorMatrix threshold = new ColorMatrix(new float[] { m, 0, 0, 1, t, 0, m, 0, 1, t, 0, 0, m, 1, t, 0, 0, 0, 1, 0 });

		colorMatrix.postConcat(threshold);
		return colorMatrix;
	}

	private ColorMatrix getFadedColorMatrix()
	{

		ColorMatrix matrixA = new ColorMatrix(new float[] { .66f, .33f, .33f, 0, 0, // red
				.33f, .66f, .33f, 0, 0, // green
				.33f, .33f, .66f, 0, 0, // blue
				0, 0, 0, 1, 0 // alpha
				});

		return matrixA;
	}

	private ColorMatrix getPolaroidColorMatrix()
	{

		final ColorMatrix matrixA = new ColorMatrix(new float[] { 0.953125f, 0.0f, 0.0f, 0.0f, 0.121f, 0.0f, 0.957031f, 0.0f, 0.0f, 0.0625f, 0.0f, 0.0f, 0.761718f, 0.0f, 0.2461f,
				0.0f, 0.0f, 0.0f, 1.0f, 0.0f });

		return matrixA;
	}

	private ColorMatrix getSepiaColorMatrix(float value)
	{
		value = value / 100;
		ColorMatrix sepiaMatrix = new ColorMatrix();
		float[] sepMat = { 1 - (1 - 0.3930000066757202f) * value, 0.7689999938011169f * value, 0.1889999955892563f * value, 0, 0, 0.3490000069141388f * value,
				1 - (1 - 0.6859999895095825f) * value, 0.1679999977350235f * value, 0, 0, 0.2720000147819519f * value, 0.5339999794960022f * value,
				1 - (1 - 0.1309999972581863f) * value, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1 };
		sepiaMatrix.set(sepMat);
		return sepiaMatrix;
	}

	private static Handler uiHandler = new Handler(Looper.getMainLooper());

	public class ApplyFilterTask implements Runnable
	{

		private FilterType effect;

		private OnFilterAppliedListener readyListener;

		private Bitmap mBitmapOut;

		private boolean blurImage;

		public ApplyFilterTask(FilterType effectType, OnFilterAppliedListener listener, boolean isThumbnail)
		{
			// TODO Auto-generated constructor stub
			effect = effectType;
			readyListener = listener;
			blurImage = isThumbnail;
			mBitmapOut = Bitmap.createBitmap(mBitmapIn.getWidth(), mBitmapIn.getHeight(), mBitmapIn.getConfig());
			mOutAllocations = Allocation.createFromBitmap(mRS, mBitmapOut);

		}

		protected void onPostExecute(Bitmap result)
		{
			isGPUFree = true;

		}

		@Override
		public void run()
		{
			// TODO Auto-generated method stub

			isGPUFree = false;
			isBasicFilter = false;

			float[] preMatrix = getPreScriptEffects();
			if (preMatrix != null)
			{
				mScript.set_preMatrix(preMatrix);
			}

			float[] postMatrix = getPostScriptEffects();
			if (postMatrix != null)
			{
				mScript.set_postMatrix(postMatrix);
			}

			applyEffect(effect);

			uiHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					readyListener.onFilterApplied(mBitmapOut);
				}
			});
		}

		private void applyEffect(FilterType effect)
		{
			int[] ro, ri, go, gi, bo, bi, ci, co;
			Splines red, green, blue, composite;

			switch (effect)
			{
			case FILTER1:
				mScript.set_r(new int[] { 0x33, 0xCD, 0 });
				mScript.set_g(new int[] { 0x27, 0x98, 0 });
				mScript.set_b(new int[] { 0xCD, 0x83, 0 });
				mScript.forEach_filter1(mInAllocation, mOutAllocations);
				break;
			case E1977:

				ri = new int[] { 0, 22, 43, 63, 75, 96, 119, 147, 164, 191, 209, 235, 255 };
				ro = new int[] { 75, 81, 82, 113, 128, 151, 168, 201, 220, 222, 219, 225, 231 };
				gi = new int[] { 0, 16, 30, 43, 76, 89, 111, 130, 155, 176, 188, 216, 237, 255 };
				go = new int[] { 54, 59, 57, 58, 87, 104, 124, 144, 169, 190, 202, 222, 240, 244 };
				bi = new int[] { 0, 23, 40, 67, 90, 108, 134, 153, 175, 195, 211, 234, 255 };
				bo = new int[] { 65, 61, 65, 84, 118, 134, 167, 188, 209, 214, 210, 213, 209 };
				red = new Splines(ri, ro);
				green = new Splines(gi, go);
				blue = new Splines(bi, bo);

				mScript.set_rSpline(red.getInterpolationMatrix());
				mScript.set_gSpline(green.getInterpolationMatrix());
				mScript.set_bSpline(blue.getInterpolationMatrix());
				mScript.forEach_filter_1977_or_xpro(mInAllocation, mOutAllocations);

				break;
			case CLASSIC:

				ri = new int[] { 0, 147, 255 };
				ro = new int[] { 0, 111, 255 };
				gi = new int[] { 0, 192, 255 };
				go = new int[] { 17, 193, 235 };
				bi = new int[] { 0, 255 };
				bo = new int[] { 35, 226 };
				red = new Splines(ri, ro);
				green = new Splines(gi, go);
				blue = new Splines(bi, bo);
				mScript.set_rSpline(red.getInterpolationMatrix());
				mScript.set_gSpline(green.getInterpolationMatrix());
				mScript.set_bSpline(blue.getInterpolationMatrix());
				mScript.set_r(new int[] { 0xF7, 0x04, 0 });
				mScript.set_g(new int[] { 0xDA, 0x08, 0 });
				mScript.set_b(new int[] { 0xAE, 0x2E, 0 });
				mScript.forEach_filter_classic(mInAllocation, mOutAllocations);
				break;
			case KELVIN:

				ri = new int[] { 0, 149, 255 };
				ro = new int[] { 64, 229, 255 };
				gi = new int[] { 0, 159, 255 };
				go = new int[] { 38, 181, 255 };
				bi = new int[] { 0, 255 };
				bo = new int[] { 62, 189 };
				red = new Splines(ri, ro);
				green = new Splines(gi, go);
				blue = new Splines(bi, bo);
				mScript.set_rSpline(red.getInterpolationMatrix());
				mScript.set_gSpline(green.getInterpolationMatrix());
				mScript.set_bSpline(blue.getInterpolationMatrix());
				mScript.set_r(new int[] { 0xFF, 0, 0 });
				mScript.set_g(new int[] { 0xB0, 0, 0 });
				mScript.set_b(new int[] { 0x7C, 0, 0 });
				mScript.forEach_filter_kelvin(mInAllocation, mOutAllocations);
				break;
			case RETRO:
				ci = new int[] { 0, 91, 182, 255 };
				co = new int[] { 0, 85, 199, 255 };
				bi = new int[] { 0, 91, 255 };
				bo = new int[] { 0, 136, 255 };
				composite = new Splines(ci, co);
				blue = new Splines(bi, bo);
				mScript.set_compositeSpline(composite.getInterpolationMatrix());
				mScript.set_bSpline(blue.getInterpolationMatrix());

				mScript.set_r(new int[] { 0xF3, 0, 0 });
				mScript.set_g(new int[] { 0xE4, 0, 0 });
				mScript.set_b(new int[] { 0x8E, 0, 0 });
				mScript.forEach_filter_retro(mInAllocation, mOutAllocations);
				break;
			case X_PRO_2:

				ri = new int[] { 0, 23, 44, 63, 83, 106, 128, 149, 170, 187, 214, 235, 255 };
				ro = new int[] { 0, 13, 28, 48, 71, 102, 131, 160, 185, 209, 231, 246, 255 };
				gi = new int[] { 0, 20, 42, 62, 87, 103, 128, 146, 167, 190, 211, 234, 255 };
				go = new int[] { 0, 10, 25, 45, 75, 96, 130, 154, 183, 209, 231, 244, 255 };
				bi = new int[] { 0, 22, 41, 64, 82, 105, 128, 150, 170, 191, 212, 234, 255 };
				bo = new int[] { 31, 47, 62, 79, 95, 111, 129, 145, 161, 180, 196, 212, 255 };
				red = new Splines(ri, ro);
				green = new Splines(gi, go);
				blue = new Splines(bi, bo);
				mScript.set_rSpline(red.getInterpolationMatrix());
				mScript.set_gSpline(green.getInterpolationMatrix());
				mScript.set_bSpline(blue.getInterpolationMatrix());
				mScript.forEach_filter_1977_or_xpro(mInAllocation, mOutAllocations);
				break;
			case APOLLO:
				ri = new int[] { 30, 120, 222, 255 };
				ro = new int[] { 20, 137, 221, 221 };
				gi = new int[] { 0, 117, 255 };
				go = new int[] { 0, 141, 255 };
				bi = new int[] { 0, 255 };
				bo = new int[] { 0, 255 };
				red = new Splines(ri, ro);
				green = new Splines(gi, go);
				blue = new Splines(bi, bo);
				mScript.set_rSpline(red.getInterpolationMatrix());
				mScript.set_gSpline(green.getInterpolationMatrix());
				mScript.set_bSpline(blue.getInterpolationMatrix());
				mScript.forEach_filter_1977_or_xpro(mInAllocation, mOutAllocations);
				break;
			case BRANNAN:
				bi = new int[] { 0, 183, 255 };
				bo = new int[] { 0, 148, 255 };
				blue = new Splines(bi, bo);
				mScript.set_r(new int[] { 0x8C, 0, 0 });
				mScript.set_g(new int[] { 0x8C, 0, 0 });
				mScript.set_b(new int[] { 0x63, 0, 0 });
				mScript.set_bSpline(blue.getInterpolationMatrix());
				mScript.forEach_filter_brannan(mInAllocation, mOutAllocations);
				break;
			case EARLYBIRD:
				mScript.set_r(new int[] { 0xFC, 0, 0 });
				mScript.set_g(new int[] { 0xF3, 0, 0 });
				mScript.set_b(new int[] { 0xD6, 0, 0 });
				mScript.forEach_filter_earlyBird(mInAllocation, mOutAllocations);
				break;
			case INKWELL:
				ci = new int[] { 0, 16, 82, 151, 255 };
				co = new int[] { 0, 0, 88, 184, 224 };
				composite = new Splines(ci, co);
				mScript.set_compositeSpline(composite.getInterpolationMatrix());
				mScript.forEach_filter_inkwell(mInAllocation, mOutAllocations);
				break;
			case LO_FI:
				ci = new int[] { 0, 90, 170, 255 };
				co = new int[] { 0, 47, 171, 255 };
				composite = new Splines(ci, co);
				mScript.set_compositeSpline(composite.getInterpolationMatrix());
				mScript.forEach_filter_lomofi(mInAllocation, mOutAllocations);
				break;
			case NASHVILLE:
				gi = new int[] { 0, 255 };
				go = new int[] { 38, 255 };
				bi = new int[] { 0, 255 };
				bo = new int[] { 127, 255 };
				green = new Splines(gi, go);
				blue = new Splines(bi, bo);
				mScript.set_gSpline(green.getInterpolationMatrix());
				mScript.set_bSpline(blue.getInterpolationMatrix());
				mScript.set_r(new int[] { 0xA6, 0xF6, 0 });
				mScript.set_g(new int[] { 0x65, 0xD8, 0 });
				mScript.set_b(new int[] { 0x30, 0xAC, 0 });
				mScript.forEach_filter_nashville(mInAllocation, mOutAllocations);
				break;
			default:
				isBasicFilter = true;
				mScript.forEach_filter_colorMatrix(mInAllocation, mOutAllocations);
				break;

			}

			mOutAllocations.copyTo(mBitmapOut);

		}

		float[] getPreScriptEffects()
		{
			ColorMatrix matrix = getColorMatrixforFilter(this.effect, true, HikeConstants.HikePhotos.DEFAULT_FILTER_APPLY_PERCENTAGE);
			if (matrix != null)
				return matrix.getArray();
			else
				return null;
		}

		float[] getPostScriptEffects()
		{
			ColorMatrix matrix = getColorMatrixforFilter(this.effect, false, HikeConstants.HikePhotos.DEFAULT_FILTER_APPLY_PERCENTAGE);
			if (matrix != null)
				return matrix.getArray();
			else
				return null;

		}

	}

	public interface OnFilterAppliedListener
	{
		void onFilterApplied(Bitmap preview);
	}

}

class Splines
{
	private int maxLength;

	private double slopes[];

	private int interpolationOutput[];

	private double inputArray[];

	private double outputArray[];

	public Splines(int inputs[], int outputs[])
	{
		// TODO Auto-generated constructor stub
		this.maxLength = 256;
		this.inputArray = new double[inputs.length];
		this.outputArray = new double[inputs.length];
		this.interpolationOutput = new int[maxLength];
		this.slopes = new double[inputs.length];

		for (int i = 0; i < inputs.length; i++)
			this.inputArray[i] = inputs[i] + 0.0;
		for (int i = 0; i < outputs.length; i++)
			this.outputArray[i] = outputs[i] + 0.0;
		init();
	}

	public static double splineEval(double x, double x0, double x1, double y0, double y1, double s0, double s1)
	{
		double h = x1 - x0;
		double t = (x - x0) / h;
		double u = 1 - t;

		return u * u * (y0 * (2 * t + 1) + s0 * h * t) + t * t * (y1 * (3 - 2 * t) - s1 * h * u);
	}

	public static void computeSplineSlopes(int n, double x[], double y[], double s[])
	{
		int i, j;
		double h[] = new double[n];
		double hinv[] = new double[n];
		double g[] = new double[n];
		double a[] = new double[n + 1];
		double b[] = new double[n + 1];
		double fac;

		for (i = 0; i < n; i++)
		{
			h[i] = x[i + 1] - x[i];
			hinv[i] = 1.0 / h[i];
			g[i] = 3 * (y[i + 1] - y[i]) * hinv[i] * hinv[i];
		}
		a[0] = 2 * hinv[0];
		b[0] = g[0];
		for (i = 1; i <= n; i++)
		{
			fac = hinv[i - 1] / a[i - 1];
			a[i] = (2 - fac) * hinv[i - 1];
			b[i] = g[i - 1] - fac * b[i - 1];
			if (i < n)
			{
				a[i] += 2 * hinv[i];
				b[i] += g[i];
			}
		}
		s[n] = b[n] / a[n];
		for (i = n - 1; i >= 0; i--)
			s[i] = (b[i] - hinv[i] * s[i + 1]) / a[i];
	}

	public int[] getInterpolationMatrix()
	{
		return this.interpolationOutput;
	}

	private void init()
	{
		int ix = 0, iy = 0;
		computeSplineSlopes(inputArray.length - 1, inputArray, outputArray, slopes);
		int seg;
		for (seg = 0; seg < inputArray.length - 1; seg++)
		{
			int nDivs = (int) (inputArray[seg + 1] - inputArray[seg]);
			for (int i = 0; i < nDivs; i++)
			{
				double rx = inputArray[seg] + i;
				double ry = splineEval(rx, inputArray[seg], inputArray[seg + 1], outputArray[seg], outputArray[seg + 1], slopes[seg], slopes[seg + 1]);
				iy = (int) (ry);
				ix = (int) (rx);
				if (iy > 255)
					iy = 255;
				if (iy < 0)
					iy = 0;
				interpolationOutput[ix] = iy;
			}
		}
		interpolationOutput[255] = (int) this.outputArray[this.outputArray.length - 1];

	}

}
