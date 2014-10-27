package com.bsb.hike.ui;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.R;
import com.bsb.hike.ui.fragments.StickerSettingsFragment;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;

public class StickerSettingsActivity extends HikeAppStateBaseFragmentActivity
{
	private StickerSettingsFragment stickerSettingsFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sticker_settings_page);
		setupSettingsFragment(savedInstanceState);
		setupActionBar();
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = getLayoutInflater().inflate(R.layout.sticker_shop_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);
		View stickerSettingsBtn = actionBarView.findViewById(R.id.sticker_settings_btn);
		stickerSettingsBtn.setVisibility(View.GONE);
		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.my_stickers);

		backContainer.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				finish();
			}
		});

		actionBar.setCustomView(actionBarView);
	}

	private void setupSettingsFragment(Bundle savedInstanceState)
	{
		if (savedInstanceState != null)
			return;
		else
			stickerSettingsFragment = StickerSettingsFragment.newInstance();
		getSupportFragmentManager().beginTransaction().add(R.id.sticker_settings_parent, stickerSettingsFragment).commit();

	}

}
