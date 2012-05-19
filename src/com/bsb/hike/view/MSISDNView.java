package com.bsb.hike.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.view.View;

import com.bsb.hike.utils.Utils;

public class MSISDNView extends View {

	private String msisdn;
	private int[] msisdnRes;

	public MSISDNView(Context context) {
		super(context);
	}

	public MSISDNView(Context context, String msisdn) {
		super(context);
		this.msisdn = Utils.formatNo(msisdn);
		this.msisdnRes = Utils.getNumberImage(this.msisdn);
	}

	@Override
	public void draw(Canvas canvas) {
		int prevWidth = 0;
		for(int i = 0; i < msisdnRes.length; i++)
		{
			Bitmap bmp = BitmapFactory.decodeResource(getResources(), msisdnRes[i]);
			canvas.drawBitmap(bmp, prevWidth, 0, null);
			prevWidth += bmp.getWidth();
		}
		super.draw(canvas);
	}
}
