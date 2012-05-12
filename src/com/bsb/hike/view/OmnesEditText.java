package com.bsb.hike.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.EditText;

public class OmnesEditText extends EditText {

	public OmnesEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public OmnesEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public OmnesEditText(Context context) {
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
