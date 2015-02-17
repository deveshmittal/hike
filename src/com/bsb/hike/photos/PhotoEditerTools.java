package com.bsb.hike.photos;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.bsb.hike.R;
import com.bsb.hike.photos.FilterTools.FilterType;

//public static int[] BasicMenuIcons={R.drawable.effects_effect,R.drawable.effects_color,R.drawable.effects_frame,R.drawable.effects_text,R.drawable.effects_options};

public class PhotoEditerTools
{
	public static String[] BasicMenuOptions = { "Effects", "Doodles", "Borders", "Text", "Quality" };

	public enum MenuType
	{
		Effects, Doodle, Border, Text, Quality
	}

	public static int[] DoodleColors = { 0xFFFF4040, 0xFFA8FA72, 0xFF72A8FA, 0xFFFA72A8, 0xFFFFFFFF, 0xFFECFA72, 0xFFF9B074, 0xFF5A0949, 0xFF000000 };

	// public static int[] BasicMenuIcons={R.drawable.effects_effect,R.drawable.effects_color,R.drawable.effects_frame,R.drawable.effects_text,R.drawable.effects_options};

	public static int[] DoodleBrushSizes = { 20, 40, 60, 80, 100 };

	public static int dpToPx(Context context, int dps)
	{
		final float scale = context.getResources().getDisplayMetrics().density;
		int pixels = (int) (dps * scale + 0.5f);

		return pixels;
	}
	
	// This snippet hides the system bars.
	public static void hideSystemUI(View mView) {
	    // Set the IMMERSIVE flag.
	    // Set the content to appear under the system bars so that the content
	    // doesn't resize when the system bars hide and show.
	    mView.setSystemUiVisibility(
	            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
	            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
	            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
	            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
	            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
	            | View.SYSTEM_UI_FLAG_IMMERSIVE);
	}


}

class BorderTools
{

	public static Bitmap ApplyBorderToBitmap(Bitmap source, Drawable border)
	{
		Bitmap topImage = null;
		Bitmap b = Bitmap.createBitmap(source.getWidth(), source.getHeight(), source.getConfig());
		Canvas comboImage = new Canvas(b);

		int width = source.getWidth();
		int height = source.getHeight();

		border.setBounds(0, 0, height, width);
		Bitmap tpImg = ((BitmapDrawable) border).getBitmap();
		topImage = Bitmap.createScaledBitmap(tpImg, width, height, true);
		source = Bitmap.createScaledBitmap(source, (int) (width - width * 0.24), (int) (height - height * 0.32), true);

		comboImage.drawBitmap(source, 0.12f * width, 0.176f * height, null);

		comboImage.drawBitmap(topImage, 0f, 0f, null);

		return b;

	}

	public static class BorderList
	{
		public List<String> names = new LinkedList<String>();

		public List<Integer> borders = new LinkedList<Integer>();

		private static BorderList list;

		public void addBorder(final String name, final int id)
		{
			names.add(name);
			borders.add(new Integer(id));
		}

		public static BorderList getBorders()
		{
			if (list != null)
				return list;
			else
			{
				list = new BorderList();
//				list.addBorder("Hearts", R.drawable.test);
				return list;
			}
		}
	}

}

/*
 * 
 * 
 * 
 * 
 * 
 * public void applyBorder(Drawable border) { GPUView.setBitmap(BorderTools.ApplyBorderToBitmap(currentImage, border));
 * 
 * }
 */

