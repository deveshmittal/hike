package com.bsb.hike.view;

import com.bsb.hike.utils.Utils;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.EditText;

public class CustomFontEditText extends EditText {

	private String fontName;

	private void setFont(AttributeSet attrs)
	{
		fontName = attrs.getAttributeValue(null, "font");
	}

	public CustomFontEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setFont(attrs);
	}

	public CustomFontEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		setFont(attrs);
	}

	public CustomFontEditText(Context context) {
		super(context);
	}

	@Override
	public void setTypeface(Typeface tf, int style) {
		if (!isInEditMode()) {
			/*
			 *  If we are dealing with LDPI phones, we use the default font,
			 *  They have a rendering issue with the font that we're using
			 */
			if (Utils.densityMultiplier <= 0.75f) 
			{
				if (style == Typeface.ITALIC) 
				{
					style = Typeface.NORMAL;
				}
				super.setTypeface(tf, style);
				return;
			}
			
			if (CustomTypeFace.customTypeFace == null) {
				CustomTypeFace.customTypeFace = new CustomTypeFace(getContext(), fontName);
			}

			if (style == Typeface.BOLD) {
				super.setTypeface(CustomTypeFace.customTypeFace.bold);
			} else if (style == Typeface.ITALIC) {
				super.setTypeface(CustomTypeFace.customTypeFace.thin);
			} else {
				super.setTypeface(CustomTypeFace.customTypeFace.normal);
			}
		}
	}
}
