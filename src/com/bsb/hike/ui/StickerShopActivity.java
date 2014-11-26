package com.bsb.hike.ui;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.ui.fragments.StickerShopFragment;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;

public class StickerShopActivity extends HikeAppStateBaseFragmentActivity
{
	private StickerShopFragment stickerShopFragment;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sticker_shop_parent);
		setupShopFragment(savedInstanceState);
		setupActionBar();
	}

	@Override
	public void onPause()
	{
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	public void onResume()
	{
		super.onResume();
	}

	@Override
	public void onDestroy()
	{
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	private void setupShopFragment(Bundle savedInstanceState)
	{
		if (savedInstanceState != null)
		{
			return;
		}
		stickerShopFragment = StickerShopFragment.newInstance();

		getSupportFragmentManager().beginTransaction().add(R.id.sticker_shop_parent, stickerShopFragment).commit();

	}

	public void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = getLayoutInflater().inflate(R.layout.sticker_shop_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);
		View stickerSettingsBtn = actionBarView.findViewById(R.id.sticker_settings_btn);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.sticker_shop);

		backContainer.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				finish();
			}
		});

		stickerSettingsBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Utils.sendUILogEvent(HikeConstants.LogEvent.STICKER_SETTING_BTN_CLICKED);
				Intent i = new Intent(StickerShopActivity.this, StickerSettingsActivity.class);
				startActivity(i);
			}
		});

		actionBar.setCustomView(actionBarView);
	}

}
