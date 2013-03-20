package com.bsb.hike.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;

import com.bsb.hike.R;
import com.bsb.hike.utils.DrawerBaseActivity;

public class StatusDialog extends Dialog {

	Activity activity;

	public StatusDialog(Context context, int theme) {
		super(context, theme);
		activity = (Activity) context;
	}

	@Override
	public void onBackPressed() {
		View v1 = findViewById(R.id.mood_parent);
		View v2 = findViewById(R.id.emoji_container);
		if ((v1 != null && v1.getVisibility() == View.VISIBLE)
				|| (v2 != null && v2.getVisibility() == View.VISIBLE)) {
			v1.setVisibility(View.GONE);
			v2.setVisibility(View.GONE);
			Button post = ((Button) findViewById(R.id.title_icon));
			post.setText(R.string.post);
			((DrawerBaseActivity) activity).toggleEnablePostButton();
			return;
		}
		super.onBackPressed();
	}
}
