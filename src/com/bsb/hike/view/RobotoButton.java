package com.bsb.hike.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.Button;

public class RobotoButton extends Button {

	public RobotoButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public RobotoButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public RobotoButton(Context context) {
		super(context);
	}

	@Override
	public void setTypeface(Typeface tf, int style) {
		if (!isInEditMode()) {
			if (RobotoTypeFace.robotoTypeFace == null) {
				RobotoTypeFace.robotoTypeFace = new RobotoTypeFace(getContext());
			}

			if (style == Typeface.BOLD) {
				super.setTypeface(RobotoTypeFace.robotoTypeFace.bold);
			} else if (style == Typeface.ITALIC) {
				super.setTypeface(RobotoTypeFace.robotoTypeFace.thin);
			} else {
				super.setTypeface(RobotoTypeFace.robotoTypeFace.normal);
			}
		}
	}
}
