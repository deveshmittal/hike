package com.bsb.hike.view;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.Display;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class CustomMovableFrameLayout extends FrameLayout {
	
	

	public CustomMovableFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);		
	}
	
	public CustomMovableFrameLayout(Context context){
		super(context);
	}
	
	public CustomMovableFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);		
	}
	 
	public void setXaxis(float x){
		WindowManager wm  = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
		Display dis = wm.getDefaultDisplay();
		Point pt = new Point();
		dis.getSize(pt);
		float width = (float) pt.x;
		width = width/(float)100;
		setX((float)x*width + (float)(0.5 * width * 0.15));
	}
	
	public float getXaxis(){
		WindowManager wm  = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
		Display dis = wm.getDefaultDisplay();
		Point pt = new Point();
		dis.getSize(pt);
		float width = (float) pt.x;
		width = width/(float)100;
		return width * getX() + (float)(0.5 * width * 0.15);
	}

}
