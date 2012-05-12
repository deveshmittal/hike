package com.bsb.hike.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class OmnesTextView extends TextView {

	public OmnesTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public OmnesTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public OmnesTextView(Context context) {
		super(context);
	}

	@Override
	public void setTypeface(Typeface tf, int style) {
		if (!isInEditMode()) {
			if (OmnesTypeFace.omnesTypeFace == null) {
				OmnesTypeFace.omnesTypeFace = new OmnesTypeFace(getContext());
			}

			if (style == Typeface.BOLD) {
				super.setTypeface(OmnesTypeFace.omnesTypeFace.bold);
			} else if (style == Typeface.ITALIC) {
				super.setTypeface(OmnesTypeFace.omnesTypeFace.thin);
			} else {
				super.setTypeface(OmnesTypeFace.omnesTypeFace.normal);
			}
		}
	}
}
