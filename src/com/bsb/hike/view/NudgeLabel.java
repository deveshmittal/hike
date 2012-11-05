package com.bsb.hike.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

public class NudgeLabel extends CustomFontTextView {

	private GestureDetector gestureDetector;
	private DoubleTapListener doubleTapListener;

	public NudgeLabel(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		gestureDetector = new GestureDetector(simpleOnGestureListener);
	}

	SimpleOnGestureListener simpleOnGestureListener = new SimpleOnGestureListener(){

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			Log.d(getClass().getSimpleName(), "Double Tap event");
			if(doubleTapListener != null)
			{
				doubleTapListener.sendPoke();
			}
			return true;
		}
		
	};
	
	public NudgeLabel(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public NudgeLabel(Context context) {
		this(context, null, 0);
	}

	public void setDoublTap(DoubleTapListener doubleTapListener)
	{
		this.doubleTapListener = doubleTapListener;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		gestureDetector.onTouchEvent(event);
		return true;
	}

	public interface DoubleTapListener
	{
		public void sendPoke();
	}
}
