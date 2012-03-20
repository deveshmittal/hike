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

        this.setLayoutResource(R.layout.icon_checkbox_preference);

        String iconName = attrs.getAttributeValue(null, "icon");
        Log.d("IconCheckBoxPreference", "iconName is " + iconName);

        int id = context.getResources().getIdentifier(iconName, null, context.getPackageName());
        this.mIcon = context.getResources().getDrawable(id);
//        this.mIcon = context.getDrawable(R.styleable.IconPreference_icon);
    }

    public IconCheckBoxPreference(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @Override
    protected void onBindView(final View view) {
        super.onBindView(view);
        final ImageView imageView = (ImageView)view.findViewById(R.id.icon);
        Log.d("IconCheckBoxPreference", "in onBindView " + imageView + " " + this.mIcon);
        if ((imageView != null) && (this.mIcon != null)) {
            imageView.setImageDrawable(this.mIcon);
        }
    }

    /**
     * Sets the icon for this Preference with a Drawable.
     *
     * @param icon The icon for this Preference
     */
    public void setIcon(final Drawable icon) {
    	Log.d("IconCheckBoxPreference", "setIcon " + icon);
        if (((icon == null) && (this.mIcon != null)) || ((icon != null) && (!icon.equals(this.mIcon)))) {
            this.mIcon = icon;
            this.notifyChanged();
        }
    }

    /**
     * Returns the icon of this Preference.
     *
     * @return The icon.
     * @see #setIcon(Drawable)
     */
    public Drawable getIcon() {
        return this.mIcon;
    }
}