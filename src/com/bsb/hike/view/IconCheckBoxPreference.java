package com.bsb.hike.view;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bsb.hike.R;

public class IconCheckBoxPreference extends CheckBoxPreference {
	private Drawable mIcon;

	public IconCheckBoxPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        setIcon(context, attrs);
	}

    private void setIcon(Context context, AttributeSet attrs)
	{
        String iconName = attrs.getAttributeValue(null, "icon");

        int id = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
    
        this.mIcon = context.getResources().getDrawable(id);
	}

	public IconCheckBoxPreference(Context context)
	{
		super(context);
	}

	public IconCheckBoxPreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		setIcon(context, attrs);
    }

    protected void onBindView(final View view) {
        super.onBindView(view);
        final ImageView imageView = (ImageView) view.findViewById(R.id.icon);
        if ((imageView != null) && (this.mIcon != null))
        {
        	imageView.setImageDrawable(this.mIcon);
        	imageView.setVisibility(View.VISIBLE);
        }
    }
}