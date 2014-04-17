package com.bsb.hike.ui;

import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.HikeAppStateBaseActivity;
import com.bsb.hike.utils.StickerManager;

public class Tutorial extends HikeAppStateBaseActivity
{

	boolean friendsTutorial;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tutorial);

		TextView header = (TextView) findViewById(R.id.tutorial_header);
		TextView info = (TextView) findViewById(R.id.tutorial_info);
		ImageView image = (ImageView) findViewById(R.id.tutorial_image);

		header.setText(R.string.tutorial_sticker_header);
		info.setText(R.string.tutorial_sticker_info);
		image.setImageResource(R.drawable.stickers_intro);
	}

	public void onClick(View v)
	{

		Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).edit();
		editor.putBoolean(StickerManager.SHOWN_STICKERS_TUTORIAL, true);
		editor.commit();

		Intent i = new Intent(Tutorial.this, HomeActivity.class);
		startActivity(i);
		finish();
	}
}
