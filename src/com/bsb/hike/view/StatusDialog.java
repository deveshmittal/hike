package com.bsb.hike.view;

import android.app.Dialog;
import android.content.Context;
import android.view.View;

import com.bsb.hike.R;

public class StatusDialog extends Dialog {

	public StatusDialog(Context context, int theme) {
		super(context, theme);
	}

	@Override
	public void onBackPressed() {
		View v = findViewById(R.id.mood_parent);
		if (v != null && v.getVisibility() == View.VISIBLE) {
			v.setVisibility(View.GONE);
			return;
		}
		super.onBackPressed();
	}

}
