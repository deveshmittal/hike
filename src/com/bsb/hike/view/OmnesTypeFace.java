package com.bsb.hike.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

public class OmnesTypeFace {
	
	public Typeface bold;
	public Typeface thin;
	public Typeface normal;

	public static OmnesTypeFace omnesTypeFace;
	
	public OmnesTypeFace(Context context)
	{
		Log.e("INITIALISING", "CONTEXT: " + context +" "+context.getAssets());
		bold = Typeface.createFromAsset(context.getAssets(), "fonts/Omnes-Semibold.otf");
		normal = Typeface.createFromAsset(context.getAssets(), "fonts/Omnes-Regular.otf");
	}
}