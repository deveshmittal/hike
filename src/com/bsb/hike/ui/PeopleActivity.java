package com.bsb.hike.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.ui.fragments.FriendsFragment;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;

public class PeopleActivity extends HikeAppStateBaseFragmentActivity implements Listener
{
	FriendsFragment mainFragment;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		initialisePeopleScreen(savedInstanceState);
	}

	private void initialisePeopleScreen(Bundle savedInstanceState)
	{

		setContentView(R.layout.home);
		setupMainFragment(savedInstanceState);
		setupActionBar();
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setIcon(R.drawable.ic_top_bar_search);
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);
		actionBarView.findViewById(R.id.seprator).setVisibility(View.GONE);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.favorites);

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
		
        mainFragment = new FriendsFragment();
        
        getSupportFragmentManager().beginTransaction()
                .add(R.id.home_screen, mainFragment).commit();
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getSupportMenuInflater().inflate(R.menu.friends_menu, menu);
		
		final SearchView searchView = new SearchView(getSupportActionBar().getThemedContext());
		searchView.setQueryHint(getString(R.string.search_hint));
		searchView.setIconifiedByDefault(false);
		searchView.setIconified(false);
		searchView.setOnQueryTextListener(onQueryTextListener);
		searchView.clearFocus();

		MenuItem searchItem = menu.add(Menu.NONE, Menu.NONE, 1, R.string.search_hint);

		searchItem.setIcon(R.drawable.ic_top_bar_search).setActionView(searchView)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

		searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener()
		{

			@Override
			public boolean onMenuItemActionExpand(MenuItem item)
			{
				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem item)
			{
				searchView.setQuery("", true);
				return true;
			}
		});


		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		Intent intent = null;

		switch (item.getItemId())
		{
		case R.id.show_timeline:
			intent = new Intent(this, TimelineActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			break;	
		}

		if (intent != null)
		{
			startActivity(intent);
			return true;
		}
		else
		{
			return super.onOptionsItemSelected(item);
		}
	}

	private OnQueryTextListener onQueryTextListener = new OnQueryTextListener()
	{

		@Override
		public boolean onQueryTextSubmit(String query)
		{
			return false;
		}

		@Override
		public boolean onQueryTextChange(String newText)
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.FRIENDS_TAB_QUERY, newText);
			return true;
		}
	};
	
	@Override
	public void onBackPressed()
	{
		if (getIntent().getBooleanExtra(HikeConstants.Extras.FROM_NOTIFICATION, false))
		{
			Intent intent = new Intent(PeopleActivity.this, HomeActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
		}

		super.onBackPressed();
	}

	@Override
	protected void onStop()
	{
		/*
		 * Ensuring we reset when leaving the activity as well, since we might receive a request when we were in this activity.
		 */
		Utils.resetFriendsCount(this);
		super.onStop();
	}

	@Override
	protected void onResume()
	{
		Utils.resetFriendsCount(this);
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, this);
		super.onResume();
	}
	
	@Override
	protected void onPause()
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, null);
		super.onPause();
	}

}
