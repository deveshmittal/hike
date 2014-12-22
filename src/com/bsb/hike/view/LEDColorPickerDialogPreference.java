package com.bsb.hike.view;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

public class LEDColorPickerDialogPreference extends DialogPreference
{
	private Drawable mIcon;
	private Context mContext;
	private ColorPickerView colorPickerView;
	private LedColorView ledColorView;
	
	public LEDColorPickerDialogPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setIcon(context, attrs);
		this.mContext = context;
		setDialogLayoutResource(R.layout.layout_led_color_preference);
	}
	
	private void setIcon(Context context, AttributeSet attrs)
	{
		String iconName = attrs.getAttributeValue(null, "icon");
		iconName = iconName.split("/")[1];
		int id = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());

		this.mIcon = context.getResources().getDrawable(id);
	}
	
	@Override
	protected void onBindDialogView(View view)
	{
		// TODO Auto-generated method stub
		super.onBindDialogView(view);
		colorPickerView = (ColorPickerView)view.findViewById(R.id.color_picker);
	}
	
	@Override
	protected void onBindView(View view)
	{
		// TODO Auto-generated method stub
		super.onBindView(view);
		final ImageView imageView = (ImageView) view.findViewById(R.id.icon);
		if ((imageView != null) && (this.mIcon != null))
		{
			imageView.setImageDrawable(this.mIcon);
			imageView.setVisibility(View.VISIBLE);
		}
		LinearLayout layout = (LinearLayout)view.findViewById(android.R.id.widget_frame);
		ledColorView = (LedColorView)layout.findViewById(R.id.led_color_view);
		ledColorView.setCircleColor(HikeSharedPreferenceUtil.getInstance(mContext).getData(HikeMessengerApp.LED_NOTIFICATION_COLOR_CODE, HikeConstants.LED_DEFAULT_BLUE_COLOR));
	}
	
	@Override
	protected void onPrepareDialogBuilder(Builder builder)
	{
		super.onPrepareDialogBuilder(builder);
		builder.setTitle(mContext.getResources().getString(R.string.led_notification));
		builder.setPositiveButton("OK", this);
		builder.setNegativeButton("Cancel", this);
		colorPickerView.setOldCenterColor(HikeSharedPreferenceUtil.getInstance(mContext).getData(HikeMessengerApp.LED_NOTIFICATION_COLOR_CODE, HikeConstants.LED_DEFAULT_BLUE_COLOR));
	}

	public void onClick(DialogInterface dialog, int which)
	{
		switch (which)
		{
			case DialogInterface.BUTTON_POSITIVE:
				HikeSharedPreferenceUtil.getInstance(mContext).saveData(HikeMessengerApp.LED_NOTIFICATION_COLOR_CODE, colorPickerView.getColor());
				ledColorView.setCircleColor(colorPickerView.getColor());
				notifyChanged();
				dialog.dismiss();
				break;
	
			case DialogInterface.BUTTON_NEGATIVE:
				dialog.dismiss();
				break;
				
			default:
				break;
		}
	}
	
}
