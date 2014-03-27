package com.bsb.hike.ui.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View.MeasureSpec;
import android.widget.TextView;

public class SpanUtil {
	public static ImageSpan getImageSpanFromTextView(Context context,
			int layoutId, int textviewId, String text) {
		LayoutInflater lf = (LayoutInflater) context
				.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
		TextView textView = (TextView) lf.inflate(layoutId, null).findViewById(
				textviewId);
		textView.setText(text); // set text
		return getImageSpan(textView);
	}

	public static ImageSpan getImageSpan(TextView textView) {
		// create bitmap drawable for imagespan
		BitmapDrawable bmpDrawable = new BitmapDrawable(
				getBitMapFromTV(textView));
		bmpDrawable.setBounds(0, 0, bmpDrawable.getIntrinsicWidth(),
				bmpDrawable.getIntrinsicHeight());
		return new ImageSpan(bmpDrawable);
	}

	public static Bitmap getBitMapFromTV(TextView textView) {
		// capture bitmapt of genreated textview
		int spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		textView.measure(spec, spec);
		textView.layout(0, 0, textView.getMeasuredWidth(),
				textView.getMeasuredHeight());
		Bitmap b = Bitmap.createBitmap(textView.getWidth(),
				textView.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(b);
		canvas.translate(-textView.getScrollX(), -textView.getScrollY());
		textView.draw(canvas);
		textView.setDrawingCacheEnabled(true);
		Bitmap cacheBmp = textView.getDrawingCache();
		Bitmap viewBmp = cacheBmp.copy(Bitmap.Config.ARGB_8888, true);
		textView.destroyDrawingCache(); // destory drawable
		return viewBmp;
	}

}
