package com.bsb.hike.photos.view;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
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

	public CanvasImageView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context,attrs,defStyleAttr);
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
