

package com.bsb.hike.photos;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

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
	public static String[] BasicMenuOptions={"Effects","Doodles","Borders","Text","Quality"};

	public enum  MenuType{
		Effects,Doodle,Border,Text,Quality
	}

	public static int[] DoodleColors={0xFFFF4040,0xFFA8FA72,0xFF72A8FA,0xFFFA72A8,0xFFFFFFFF,0xFFECFA72,0xFFF9B074,0xFF5A0949,0xFF000000};
	
	//public static int[] BasicMenuIcons={R.drawable.effects_effect,R.drawable.effects_color,R.drawable.effects_frame,R.drawable.effects_text,R.drawable.effects_options};

	public static int[] DoodleBrushSizes={20,40,60,80,100};
	public static int dpToPx(Context context,int dps) {
		final float scale = context.getResources().getDisplayMetrics().density;
		int pixels = (int) (dps * scale + 0.5f);

		return pixels;
	}

}

class FilterTools {  

	

	public enum FilterType {
		BRIGHTNESS,CONTRAST,SATURATION,HUE,SEPIA,GRAYSCALE,POLAROID,FADED,BGR,INVERSION,X_PRO_2,WILLOW,WALDEN,VALENCIA,TOASTER,SUTRO,SIERRA,RISE,NASHVILLE,MAYFAIR,LO_FI,KELVIN,INKWELL,HUDSON,HEFE,EARLYBIRD,BRANNAN,AMARO,E1977
	}


	public static class FilterList {
		public List<String> names = new ArrayList<String>();
		public List<FilterType> filters = new ArrayList<FilterType>();
		private static FilterList effectfilters,qualityfilters;


		public void addFilter(final String name, final FilterType filter) {
			names.add(name);
			filters.add(filter);
		}

		//X_PRO_2,WILLOW,WALDEN,VALENCIA,TOASTER,SUTRO,SIERRA,RISE,NASHVILLE,MAYFAIR,LO_FI,KELVIN,INKWELL,HUDSON,HEFE,EARLYBIRD,BRANNAN,AMARO,E1977
		//}

		public static FilterList getInstagramEffects()
		{
			if(effectfilters==null)
			{
				effectfilters = new FilterList();
				effectfilters.addFilter("X PRO 2", FilterType.X_PRO_2);
				effectfilters.addFilter("Willow", FilterType.WILLOW);
				effectfilters.addFilter("Walden", FilterType.WALDEN);
				effectfilters.addFilter("Valencia", FilterType.VALENCIA);
				effectfilters.addFilter("Toaster", FilterType.TOASTER);
				effectfilters.addFilter("Sutro", FilterType.SUTRO);
				effectfilters.addFilter("Sierra", FilterType.SIERRA);
				effectfilters.addFilter("Rise", FilterType.RISE);
				effectfilters.addFilter("NashVille", FilterType.NASHVILLE);
				effectfilters.addFilter("MayFair", FilterType.MAYFAIR);
				effectfilters.addFilter("Lo Fi", FilterType.LO_FI);
				effectfilters.addFilter("Kelvin", FilterType.KELVIN);
				effectfilters.addFilter("Inkwell", FilterType.INKWELL);
				effectfilters.addFilter("Hudson", FilterType.HUDSON);
				effectfilters.addFilter("Hefe", FilterType.HEFE);
				effectfilters.addFilter("EarlyBird", FilterType.EARLYBIRD);
				effectfilters.addFilter("Brannan", FilterType.BRANNAN);
				effectfilters.addFilter("Amaro", FilterType.AMARO);
				effectfilters.addFilter("E1977", FilterType.E1977);


			}

			return effectfilters;
		}

		public static FilterList getHikeEffects()
		{
			if(effectfilters==null)
			{
				effectfilters = new FilterList();
				effectfilters.addFilter("Original",FilterType.AMARO);
				effectfilters.addFilter("Faded", FilterType.FADED);
				effectfilters.addFilter("Polaroid", FilterType.POLAROID);
				effectfilters.addFilter("Sepia", FilterType.SEPIA);
				effectfilters.addFilter("Grayscale", FilterType.GRAYSCALE);
				/*effectfilters.addFilter("Sobel Edge Detection", FilterType.SOBEL_EDGE_DETECTION);
				effectfilters.addFilter("Emboss", FilterType.EMBOSS);
				effectfilters.addFilter("Posterize", FilterType.POSTERIZE);
				effectfilters.addFilter("Grouped effectfilters", FilterType.FILTER_GROUP);
				effectfilters.addFilter("Monochrome", FilterType.MONOCHROME);
				effectfilters.addFilter("RGB", FilterType.RGB);
				effectfilters.addFilter("Vignette", FilterType.VIGNETTE);
				effectfilters.addFilter("Lookup (Amatorka)", FilterType.LOOKUP_AMATORKA);
				effectfilters.addFilter("CGA Color Space", FilterType.CGA_COLORSPACE);
				effectfilters.addFilter("Sketch", FilterType.SKETCH);
				effectfilters.addFilter("Toon", FilterType.TOON);
				effectfilters.addFilter("Smooth Toon", FilterType.SMOOTH_TOON);
				effectfilters.addFilter("Bulge Distortion", FilterType.BULGE_DISTORTION);
				effectfilters.addFilter("Glass Sphere", FilterType.GLASS_SPHERE);
				effectfilters.addFilter("Laplacian", FilterType.LAPLACIAN);
				effectfilters.addFilter("Swirl", FilterType.SWIRL);
				effectfilters.addFilter("Color Balance", FilterType.COLOR_BALANCE);*/
			}
			return effectfilters;


		}



		public static FilterList getQualityFilters()
		{
			if(qualityfilters==null)
			{
				qualityfilters = new FilterList();


				/*qualityfilters.addFilter("Contrast", FilterType.CONTRAST);
				qualityfilters.addFilter("Gamma", FilterType.GAMMA);
				qualityfilters.addFilter("Brightness", FilterType.BRIGHTNESS);
				qualityfilters.addFilter("Sharpness", FilterType.SHARPEN);
				qualityfilters.addFilter("3x3 Convolution", FilterType.THREE_X_THREE_CONVOLUTION);
				qualityfilters.addFilter("Saturation", FilterType.SATURATION);
				qualityfilters.addFilter("Exposure", FilterType.EXPOSURE);
				qualityfilters.addFilter("Highlight Shadow", FilterType.HIGHLIGHT_SHADOW);
				qualityfilters.addFilter("Opacity", FilterType.OPACITY);
				qualityfilters.addFilter("RGB", FilterType.RGB);
				qualityfilters.addFilter("White Balance", FilterType.WHITE_BALANCE);
				qualityfilters.addFilter("ToneCurve", FilterType.TONE_CURVE);
				qualityfilters.addFilter("Gaussian Blur", FilterType.GAUSSIAN_BLUR);
				qualityfilters.addFilter("Crosshatch", FilterType.CROSSHATCH);
				qualityfilters.addFilter("Box Blur", FilterType.BOX_BLUR);
				qualityfilters.addFilter("Dilation", FilterType.DILATION);
				qualityfilters.addFilter("Kuwahara", FilterType.KUWAHARA);
				qualityfilters.addFilter("RGB Dilation", FilterType.RGB_DILATION);
				qualityfilters.addFilter("Haze", FilterType.HAZE);
				 */
			}
			return qualityfilters;


		}
	}


}	


class BorderTools
{

	public static Bitmap ApplyBorderToBitmap(Bitmap source,Drawable border)
	{
		Bitmap topImage = null ;
		Bitmap b=Bitmap.createBitmap(source.getWidth(), source.getHeight(), source.getConfig());
		Canvas comboImage = new Canvas(b);


		int width = source.getWidth();
		int height = source.getHeight();

		border.setBounds(0, 0, height, width);
		Bitmap tpImg=((BitmapDrawable)border).getBitmap();
		topImage=Bitmap.createScaledBitmap(tpImg, width, height, true);
		source=Bitmap.createScaledBitmap(source, (int)(width-width*0.24), (int)(height-height*0.32), true);

		comboImage.drawBitmap(source,0.12f*width,0.176f*height,null);

		comboImage.drawBitmap(topImage, 0f, 0f, null);

		return b;

	}


	public static class BorderList {
		public List<String> names = new LinkedList<String>();
		public List<Integer> borders = new LinkedList<Integer>();
		private static BorderList list;


		public void addBorder(final String name, final int id) {
			names.add(name);
			borders.add(new Integer(id));
		}

		public static BorderList getBorders()
		{
			if(list!=null)
				return list;
			else {
				list=new BorderList();
				list.addBorder("Hearts",R.drawable.test);
				return list;
			}
		}
	}

}


class CanvasImageView extends ImageView implements OnTouchListener {

	private Canvas mCanvas;
	private Path mPath;
	private Paint mPaint, mBitmapPaint;
	private ArrayList<PathPoints> paths = new ArrayList<PathPoints>();
	private ArrayList<PathPoints> undonePaths = new ArrayList<PathPoints>();
	private Bitmap mBitmap;
	private int color=0xFFFF0000;
	private int index=0;
	private boolean drawEnabled;

	public CanvasImageView(Context context) {
		super(context);
		init();
	}

	public CanvasImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}



	public void setStrokeWidth(int width)
	{
		mPaint.setStrokeWidth(width);
	}

	private void init() {

		this.setOnTouchListener(this);
		setDrawingCacheEnabled(true);
		buildDrawingCache(true);
		mBitmapPaint = new Paint(Paint.DITHER_FLAG);
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);
		mPaint.setColor(0xFFFF0000);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setTextSize(30);
		setStrokeWidth(40);
		mPath = new Path();
		paths.add(new PathPoints(mPath, 0xFFFF0000, false));

	}

	public void Refresh(Bitmap source)
	{
		if(mBitmap==null)
		{
			int measuredWidth = View.MeasureSpec.makeMeasureSpec(this.getWidth(), View.MeasureSpec.UNSPECIFIED);
	        int measuredHeight = View.MeasureSpec.makeMeasureSpec(this.getHeight(), View.MeasureSpec.UNSPECIFIED);

	        mBitmap=Bitmap.createBitmap(measuredWidth  , measuredHeight, source.getConfig());
			mCanvas = new Canvas(mBitmap);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {

		if(mBitmap!=null)
		{
			int i=0;
			canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
			for (i=index;i<paths.size();i++) {
				PathPoints p=paths.get(i); 
				if (p.isTextToDraw()) {
					//canvas.drawText(p.textToDraw, p.x, p.y, mPaint);
				} else {
					canvas.drawPath(p.getPath(), mPaint);
				}
			}
			index=i-1;

		}
		super.onDraw(canvas);

	}

	public Bitmap getBitmap()
	{
		return mBitmap;
	}

	public Paint getPaint()
	{
		return mPaint;
	}

	public void setColor(int color) {
		this.color = color;
		mPaint.setColor(color);
	}

	public void setDrawEnabled(boolean drawEnabled) {
		this.drawEnabled = drawEnabled;
	}


	private float mX, mY;
	private static final float TOUCH_TOLERANCE = 0;

	private void touch_start(float x, float y) {
		mPath.reset();
		mPath.moveTo(x, y);
		mX = x;
		mY = y;
	}

	private void touch_move(float x, float y) {
		float dx = Math.abs(x - mX);
		float dy = Math.abs(y - mY);
		if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
			mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
			mX = x;
			mY = y;
		}
	}

	private void touch_up() {
		mPath.lineTo(mX, mY);
		// commit the path to our offscreen
		mCanvas.drawPath(mPath, mPaint);

		// kill this so we don't double draw
		mPath=new Path();
		paths.add(new PathPoints(mPath, color, false));

	}

	@Override
	public boolean onTouch(View arg0, MotionEvent event) {
		if(drawEnabled){
			float x = event.getX();

			float y = event.getY();

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				touch_start(x, y);
				invalidate();

				break;
			case MotionEvent.ACTION_MOVE:
				touch_move(x, y);
				invalidate();

				break;
			case MotionEvent.ACTION_UP:

				touch_up();
				invalidate();

				break;
			}
		}
		return true;
	}

	public void onClickUndo() {
		if (paths.size() > 0) {
			undonePaths.add(paths.remove(paths.size() - 1));
			invalidate();
		} else {

		}
		// toast the user
	}

	public void onClickRedo() {
		if (undonePaths.size() > 0) {
			paths.add(undonePaths.remove(undonePaths.size() - 1));
			invalidate();
		} else {

		}
		// toast the user
	}


	class PathPoints {
		private Path path;
		private int color;
		private String textToDraw;
		private boolean isTextToDraw;
		private int x, y;

		public PathPoints(Path path, int color, boolean isTextToDraw) {
			this.path = path;
			this.color = color;
			this.isTextToDraw = isTextToDraw;
		}

		public PathPoints(int color, String textToDraw, boolean isTextToDraw,
				int x, int y) {
			this.color = color;
			this.textToDraw = textToDraw;
			this.isTextToDraw = isTextToDraw;
			this.x = x;
			this.y = y;
		}

		public Path getPath() {
			return path;
		}

		public void setPath(Path path) {
			this.path = path;
		}

		
		public int getColor() {
			return color;
		}

		public void setColor(int color) {
			this.color = color;
		}

		public String getTextToDraw() {
			return textToDraw;
		}

		public void setTextToDraw(String textToDraw) {
			this.textToDraw = textToDraw;
		}

		public boolean isTextToDraw() {
			return isTextToDraw;
		}

		public void setTextToDraw(boolean isTextToDraw) {
			this.isTextToDraw = isTextToDraw;
		}

		public int getX() {
			return x;
		}

		public void setX(int x) {
			this.x = x;
		}

		public int getY() {
			return y;
		}

		public void setY(int y) {
			this.y = y;
		}

	}
}

class VignetteImageView extends ImageView
{

	public VignetteImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	private Bitmap makeRadGrad(Bitmap bitmap2,int radius) {



		int w = bitmap2.getWidth();
		int h = bitmap2.getHeight();

		RadialGradient gradient = new RadialGradient(w/2, h/2, (float) (radius), 0x00000044,0xFF000000, android.graphics.Shader.TileMode.CLAMP);
		Paint p = new Paint();
		p.setDither(true);
		p.setShader(gradient);

		Bitmap bitmap = Bitmap.createBitmap(w, h, Config.ARGB_8888);
		Canvas c = new Canvas(bitmap);
		c.drawBitmap(bitmap2,0,0, null);
		c.drawCircle(w/2, h/2, (float) (radius), p);
		//viewImage.setImageBitmap(bitmap);
		return bitmap;
	}

}



class EffectsView extends ImageView
{

	private BitmapDrawable currentImage;
	

	public EffectsView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public BitmapDrawable getBitmapWithEffectsApplied()
	{
		return currentImage;
	}

		
	public void handleImage(BitmapDrawable image) {
		currentImage=image;
		this.setImageDrawable(image);
		
	}

	public ColorMatrixColorFilter applyEffect(FilterType filter,float value)
	{
		return HikeEffectsFactory.applyFiltertoBitmapDrawable(currentImage, filter, value);
	}

}


class PictureEditerView extends FrameLayout
{
	private CanvasImageView doodleLayer;
	private VignetteImageView vignetteLayer;
	private EffectsView effectLayer;
	private ColorMatrixColorFilter currentEffect;
	private boolean enableDoodling=false,enableText=false;
	private Bitmap imageOriginal,imageEdited;
	

	public PictureEditerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		doodleLayer=new CanvasImageView(context,attrs);
		vignetteLayer=new VignetteImageView(context, attrs);
		effectLayer=new EffectsView(context, attrs);
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
		currentEffect=effectLayer.applyEffect(filter, 100);
	}
	
	@SuppressWarnings("deprecation")
	public void loadImageFromFile(String FilePath)
	{
		imageOriginal=BitmapFactory.decodeFile(FilePath);
		effectLayer.handleImage(new BitmapDrawable(imageOriginal) );
	}
	
	public void enableDoodling()
	{
		doodleLayer.Refresh(imageOriginal);
		doodleLayer.setDrawEnabled(true);
	}
	
	public void disableDoodling()
	{
		
	}
	
	public void setBrushColor(int Color)
	{
		doodleLayer.setColor(Color);
	}
	
	public void saveImage() {

		imageEdited=updateSat(imageOriginal, currentEffect ) ;
		String root = Environment.getExternalStorageDirectory().toString();
		File myDir = new File(root + "/colormatrix");    
		myDir.mkdirs();
		Random generator = new Random();
		int n = 10000;
		n = generator.nextInt(n);
		String fname = "Image-"+ n +".jpg";
		File file = new File (myDir, fname);
		if (file.exists ()) file.delete (); 
		try {
			FileOutputStream out = new FileOutputStream(file);
			imageEdited.compress(Bitmap.CompressFormat.JPEG, 100, out);
			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		
	}
	
	private Bitmap updateSat(Bitmap src,ColorMatrixColorFilter filter) {

		int w = src.getWidth();
		int h = src.getHeight();

		Bitmap bitmapResult = 
				Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		Canvas canvasResult = new Canvas(bitmapResult);
		Paint paint = new Paint();
		paint.setColorFilter(filter);
		canvasResult.drawBitmap(src, 0, 0, paint);
		Bitmap temp=Bitmap.createScaledBitmap(doodleLayer.getBitmap(), src.getWidth(), src.getHeight(), true);
		canvasResult.drawBitmap(temp,0,0,doodleLayer.getPaint());
		return bitmapResult;
	}

	
	
	public void loadImage()
	{
		effectLayer.handleImage((BitmapDrawable)getResources().getDrawable(R.drawable.test));
		imageOriginal=((BitmapDrawable)getResources().getDrawable(R.drawable.test)).getBitmap();
	}
	
}

/*
 * 



	
	public void applyBorder(Drawable border)
	{
		GPUView.setBitmap(BorderTools.ApplyBorderToBitmap(currentImage, border));

	}
	
	



	

 */


