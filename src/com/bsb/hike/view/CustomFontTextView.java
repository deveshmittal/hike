package com.bsb.hike.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;

public class CustomFontTextView extends TextView implements ViewTreeObserver.OnGlobalLayoutListener
{
	private int style;

    private int maxLines;
    private void setFont(AttributeSet attrs)
    {
        setTypeface(getTypeface(), style);
    }

    public CustomFontTextView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        setFont(attrs);
        setMaxLinesForEllipsizedText(attrs);
    }

    public CustomFontTextView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        setFont(attrs);
        setMaxLinesForEllipsizedText(attrs);
    }

	public CustomFontTextView(Context context)
	{
		super(context);
	}

	@Override
	public void setTypeface(Typeface tf, int style)
	{
		if (!isInEditMode())
		{
			if (style == Typeface.ITALIC || style == Typeface.BOLD_ITALIC)
			{
				style = Typeface.NORMAL;
			}
			super.setTypeface(tf, style);
		}
	}

    /*
     *     sets maximum lines for ellipsized text and ellipsizes the text from the end.
     *     use app:maxLines: "int" to declare from the xml
     */
    private void setMaxLinesForEllipsizedText(AttributeSet attr) {
        ViewTreeObserver vto = getViewTreeObserver();
        this.maxLines = attr.getAttributeIntValue(HikeConstants.NAMESPACE, HikeConstants.MAX_LINES, 0);
        vto.addOnGlobalLayoutListener(this);
    }

    /*
     *     sets maximum lines for ellipsized text and ellipsizes the text from the end.
     *     use the below method from code
     */
    @Override
    public void setMaxLines(int maxlines) {
        this.maxLines = maxlines;
        super.setMaxLines(maxlines);
    }

    @Override
    public void onGlobalLayout() {
        if(maxLines!=0 && getLineCount() > maxLines){
            int lineEndIndex = getLayout().getLineEnd(maxLines -1);
            String text = getText().subSequence(0, lineEndIndex - 3)+"...";
            setText(text);
        }
    }
}
