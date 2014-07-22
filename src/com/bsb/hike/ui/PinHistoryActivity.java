package com.bsb.hike.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.ui.fragments.PinHistoryFragment;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;

public class PinHistoryActivity extends HikeAppStateBaseFragmentActivity 
{	
	private PinHistoryFragment mainFragment;
		
	private String msisdn;

	private long convId;
	
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		initialisePinHistory(savedInstanceState);
	}	
	
	private void initialisePinHistory(Bundle savedInstanceState)
	{
		setContentView(R.layout.timeline);
		setupMainFragment(savedInstanceState);
		setupActionBar();
	}
	
	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setIcon(R.drawable.hike_logo_top_bar);
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);
		actionBarView.findViewById(R.id.seprator).setVisibility(View.GONE);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.pin_history);

		backContainer.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});

		actionBar.setCustomView(actionBarView);
	}


	private void setupMainFragment(Bundle savedInstanceState)
	{
		if (savedInstanceState != null) {
            return;
        }
		
		msisdn = getIntent().getExtras().getString(HikeConstants.TEXT_PINS);
		convId = getIntent().getExtras().getLong(HikeConstants.EXTRA_CONV_ID);
        mainFragment = new PinHistoryFragment(msisdn,convId);
        
        getSupportFragmentManager().beginTransaction()
                .add(R.id.parent_layout, mainFragment).commit();		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onBackPressed()
	{
		super.onBackPressed();
	}
	
	
	@Override
	protected void onResume()
	{
		super.onResume();
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
	}
}
