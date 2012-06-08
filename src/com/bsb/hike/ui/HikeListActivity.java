package com.bsb.hike.ui;

import java.lang.reflect.Constructor;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.HikeArrayAdapter;
import com.bsb.hike.adapters.HikeInviteAdapter;
import com.bsb.hike.utils.Utils;

public class HikeListActivity extends Activity implements OnScrollListener, TextWatcher
{
	private HikeArrayAdapter adapter;
	private ListView listView;
	private EditText filterText;
	private TextView labelView;
	private ViewGroup mInviteToolTip;
	private SharedPreferences sharedPreferences;
	private ImageButton creditsHelpBtn;

	HikeArrayAdapter createListAdapter() throws Exception
	{
		Intent intent = getIntent();
		String adapterClassName = intent.getStringExtra(HikeConstants.ADAPTER_NAME);
		Class<HikeArrayAdapter> cls = (Class<HikeArrayAdapter>) Class.forName(adapterClassName);
		/* assume that there is only one constructor, and it's the one we want */
		Constructor c = cls.getConstructors()[0];
		return (HikeArrayAdapter) c.newInstance(this, -1);
	}

	@Override
	public Object onRetainNonConfigurationInstance()
	{
		return listView.getAdapter();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hikelistactivity);
		labelView = (TextView) findViewById(R.id.title);
		listView = (ListView) findViewById(R.id.contact_list);
		listView.setTextFilterEnabled(true);
		try
		{
			Object o = getLastNonConfigurationInstance();
			adapter = (o instanceof HikeArrayAdapter) ? (HikeArrayAdapter) o : createListAdapter();
		}
		catch(Exception e)
		{
			Log.e("HikeListActivity", "Unable to instantiate adapter", e);
			throw new RuntimeException(e.getCause());
		}

		labelView.setText(adapter.getTitle());

		listView.setAdapter(adapter);
		listView.setOnScrollListener(this);

		filterText = (EditText) findViewById(R.id.filter);
		filterText.addTextChangedListener(this);

		if(adapter instanceof HikeInviteAdapter)
		{
			showCreditsHelp(savedInstanceState);
		}
	}

	private void showCreditsHelp(Bundle savedInstanceState)
	{
		sharedPreferences = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
		creditsHelpBtn = (ImageButton) findViewById(R.id.title_image_btn);
		View buttonBar = (View) findViewById(R.id.button_bar);
		buttonBar.setVisibility(View.VISIBLE);
		creditsHelpBtn.setVisibility(View.VISIBLE);
		creditsHelpBtn.setImageResource(R.drawable.credits_btn);

		if (!sharedPreferences.getBoolean(
					HikeMessengerApp.INVITE_TOOLTIP_DISMISSED, false)) {
				filterText.setEnabled(false);
				mInviteToolTip = (ViewGroup) findViewById(R.id.credits_help_layout);

				if (savedInstanceState == null || !savedInstanceState.getBoolean(HikeConstants.Extras.TOOLTIP_SHOWING))
				{
					Animation alphaIn = AnimationUtils.loadAnimation(
							HikeListActivity.this, android.R.anim.fade_in);
					alphaIn.setStartOffset(500);
					mInviteToolTip.setAnimation(alphaIn);
				}
				mInviteToolTip.setVisibility(View.VISIBLE);
				return;
			}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if(mInviteToolTip!= null && mInviteToolTip.getVisibility() == View.VISIBLE)
		{
			outState.putBoolean(HikeConstants.Extras.TOOLTIP_SHOWING, true);
		}
		super.onSaveInstanceState(outState);
	}

	public void onTitleIconClick(View v) 
	{
		if(v != null)
		{
			Utils.logEvent(HikeListActivity.this, HikeConstants.LogEvent.CREDIT_TOP_BUTTON);
		}
		setToolTipDismissed();
		Intent i = new Intent(HikeListActivity.this, CreditsActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(i);
	}
	
	private void setToolTipDismissed()
	{
		filterText.setEnabled(true);
		if (mInviteToolTip != null && mInviteToolTip.getVisibility() == View.VISIBLE) 
		{
			Animation alphaOut = AnimationUtils.loadAnimation(
					HikeListActivity.this, android.R.anim.fade_out);
			alphaOut.setDuration(200);
			mInviteToolTip.setAnimation(alphaOut);
			mInviteToolTip.setVisibility(View.INVISIBLE);
		}
		Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
		editor.putBoolean(HikeMessengerApp.INVITE_TOOLTIP_DISMISSED, true);
		editor.commit();
	}
	
	public void onToolTipClicked(View v)
	{
		Utils.logEvent(HikeListActivity.this, HikeConstants.LogEvent.INVITE_TOOL_TIP_CLICKED);
		onTitleIconClick(null);
	}

	public void onToolTipClosed(View v)
	{
		Utils.logEvent(HikeListActivity.this, HikeConstants.LogEvent.INVITE_TOOL_TIP_CLOSED);
		setToolTipDismissed();
	}
	
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		if (adapter.getCount() == 0)
		{
			return;
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView arg0, int arg1)
	{		
	}

	@Override
	public boolean onSearchRequested()
	{
		filterText.requestFocus();
		return true;
	}

	@Override
	public void afterTextChanged(Editable text)
	{
		Filterable filterable = (Filterable) listView.getAdapter();
		filterable.getFilter().filter(text);
		
		if(text.length()>0) 
		{
			findViewById(android.R.id.content).requestLayout();
			if(adapter != null)
			{
				adapter.isFiltering = true;
			}
		} 
		else 
		{
			findViewById(android.R.id.content).requestLayout();
			if(adapter != null)
			{
				adapter.isFiltering = false;
			}
		}
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
	{
	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
	{
	}
}
