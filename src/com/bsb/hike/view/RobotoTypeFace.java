package com.bsb.hike.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

public class RobotoTypeFace {
	
	public Typeface bold;
	public Typeface thin;
	public Typeface normal;

	public static RobotoTypeFace robotoTypeFace;
	
	public RobotoTypeFace(Context context)
	{
		Log.e("INITIALISING", "CONTEXT: " + context +" "+context.getAssets());
		bold = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Bold.ttf");
		thin = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Thin.ttf");
		normal = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Regular.ttf");
	}
}