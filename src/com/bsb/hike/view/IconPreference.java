package com.bsb.hike.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bsb.hike.R;

public class IconPreference extends Preference {

	private Drawable mIcon;
	private String iconName;
	private Context mContext;
	
	public IconPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setIcon(context, attrs);
	}

	private void setIcon(Context context, AttributeSet attrs) {
		this.iconName = attrs.getAttributeValue(null, "icon");
		mContext = context;
		
		int id = context.getResources().getIdentifier(this.iconName, "drawable",
				context.getPackageName());

		this.mIcon = context.getResources().getDrawable(id);
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		final ImageView imageView = (ImageView) view.findViewById(R.id.icon);
		if ((imageView != null) && (this.mIcon != null)) {
			imageView.setImageDrawable(this.mIcon);
			imageView.setVisibility(View.VISIBLE);
			if(this.iconName.equals("ic_fb_priv")){
				int leftMargin = (int) mContext.getResources().getDimension(R.dimen.ic_fb_priv_left_margin);
				int rightMargin = (int) mContext.getResources().getDimension(R.dimen.ic_fb_priv_right_margin);
				imageView.setPadding(leftMargin, 0, rightMargin, 0);
			}
			if(this.iconName.equals("ic_twitter_priv")){
				Log.d("test = ",iconName);
				int leftMargin = (int) mContext.getResources().getDimension(R.dimen.ic_twitter_priv_left_margin);
				int rightMargin = (int) mContext.getResources().getDimension(R.dimen.ic_twitter_priv_right_margin);
				imageView.setPadding(leftMargin, 0, rightMargin, 0);
			}
		}
	}

}
