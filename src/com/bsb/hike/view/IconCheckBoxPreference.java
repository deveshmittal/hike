package com.bsb.hike.view;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;

public class IconCheckBoxPreference extends CheckBoxPreference {
	private Drawable mIcon;
	private ImageView imageView;

	public IconCheckBoxPreference(final Context context,
			final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		setIcon(context, attrs);
	}

	private void setIcon(Context context, AttributeSet attrs) {
		String iconName = attrs.getAttributeValue(null, "icon");

		int id = context.getResources().getIdentifier(iconName, "drawable",
				context.getPackageName());

		this.mIcon = context.getResources().getDrawable(id);
	}

	public IconCheckBoxPreference(Context context) {
		super(context);
	}

	public IconCheckBoxPreference(final Context context,
			final AttributeSet attrs) {
		super(context, attrs);
		setIcon(context, attrs);
	}

	protected void onBindView(final View view) {
		super.onBindView(view);
		imageView = (ImageView) view.findViewById(R.id.icon);
		if ((imageView != null) && (this.mIcon != null)) {
			imageView.setImageDrawable(this.mIcon);
			imageView.setVisibility(View.VISIBLE);
			imageView.setSelected(isChecked());
		}
	}

	@Override
	protected void notifyChanged() {
		if (imageView != null) {
			imageView.setSelected(isChecked());
		}
		/*
		 * Publish event that the free SMS has been toggled so that the drawer
		 * UI can be updated.
		 */
		if (HikeConstants.FREE_SMS_PREF.equals(getKey())) {
			Log.d(getClass().getSimpleName(), "Free SMS toggled");
			HikeMessengerApp.getPubSub().publish(HikePubSub.FREE_SMS_TOGGLED,
					isChecked());
		} else if (HikeConstants.SSL_PREF.equals(getKey())) {
			HikeMessengerApp.getPubSub().publish(
					HikePubSub.SWITCHED_DATA_CONNECTION, null);
		} else if (HikeConstants.RECEIVE_SMS_PREF.equals(getKey())) {
			if (!isChecked()) {
				Editor editor = PreferenceManager.getDefaultSharedPreferences(
						getContext()).edit();
				editor.putBoolean(HikeConstants.SEND_SMS_PREF, false);
				editor.commit();
			} else {
				if (!getContext().getSharedPreferences(
						HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(
						HikeMessengerApp.SHOWN_SMS_SYNC_POPUP, false)) {
					HikeMessengerApp.getPubSub().publish(
							HikePubSub.SHOW_SMS_SYNC_DIALOG, null);
				}
			}
		}
		super.notifyChanged();
	}

}