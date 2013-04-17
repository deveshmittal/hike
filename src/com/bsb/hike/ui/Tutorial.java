package com.bsb.hike.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;

public class Tutorial extends Activity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.signup_tutorial_base);

		Button titleBtn = (Button) findViewById(R.id.title_icon);
		titleBtn.setText(R.string.next_signup);
		titleBtn.setVisibility(View.VISIBLE);
		findViewById(R.id.button_bar_2).setVisibility(View.VISIBLE);

		TextView mTitleView = (TextView) findViewById(R.id.title);
		mTitleView.setText(R.string.meet_hike);

		boolean isLandscape = getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT;
		findViewById(R.id.intro_img).setVisibility(
				isLandscape ? View.GONE : View.VISIBLE);
	}

	public void onTitleIconClick(View v) {
		Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
				MODE_PRIVATE).edit();
		editor.putBoolean(HikeMessengerApp.INTRO_DONE, true);
		editor.commit();

		Intent i = new Intent(Tutorial.this, HikeListActivity.class);
		i.putExtra(HikeConstants.Extras.SHOW_MOST_CONTACTED, true);
		startActivity(i);
		finish();
	}
}
