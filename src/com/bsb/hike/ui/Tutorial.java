package com.bsb.hike.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;

public class Tutorial extends Activity {

	boolean friendsTutorial;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tutorial);

		friendsTutorial = getIntent().getBooleanExtra(
				HikeConstants.Extras.SHOW_FRIENDS_TUTORIAL, false);
		TextView header = (TextView) findViewById(R.id.tutorial_header);
		TextView info = (TextView) findViewById(R.id.tutorial_info);
		ImageView image = (ImageView) findViewById(R.id.tutorial_image);

		if (friendsTutorial) {
			header.setText(R.string.tutorial_friends_header);
			info.setText(R.string.tutorial_friends_info);
			image.setImageResource(R.drawable.friends_intro);
		} else {
			header.setText(R.string.tutorial_sticker_header);
			info.setText(R.string.tutorial_sticker_info);
			image.setImageResource(R.drawable.stickers_intro);
		}
	}

	public void onClick(View v) {

		Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
				MODE_PRIVATE).edit();
		editor.putBoolean(
				friendsTutorial ? HikeMessengerApp.SHOWN_FRIENDS_TUTORIAL
						: HikeMessengerApp.SHOWN_STICKERS_TUTORIAL, true);
		editor.commit();

		Intent i = new Intent(Tutorial.this, MessagesList.class);
		startActivity(i);
		finish();
	}
}
