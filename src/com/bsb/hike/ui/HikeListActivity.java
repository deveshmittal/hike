package com.bsb.hike.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
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
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.adapters.HikeArrayAdapter;
import com.bsb.hike.adapters.HikeInviteAdapter;
import com.bsb.hike.utils.Utils;

public class HikeListActivity extends Activity implements OnScrollListener, TextWatcher, Listener
{
	private HikeArrayAdapter adapter;
	private ListView listView;
	private EditText filterText;
	private TextView labelView;
	private ViewGroup mInviteToolTip;
	private SharedPreferences sharedPreferences;
	private ImageButton creditsHelpBtn;
	private TextView inviteUrl;
	private String inviteUrlWithToken;

	HikeArrayAdapter createListAdapter() throws Exception
	{
		Intent intent = getIntent();
		String adapterClassName = intent.getStringExtra(HikeConstants.ADAPTER_NAME);
		return new HikeInviteAdapter(this, -1);
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
		inviteUrl = (TextView) findViewById(R.id.invite_url);

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

		setInviteToken();

		if(adapter instanceof HikeInviteAdapter)
		{
			showCreditsHelp(savedInstanceState);
		}
		HikeMessengerApp.getPubSub().addListener(HikePubSub.INVITE_TOKEN_ADDED, this);
	}

	@Override
	protected void onDestroy() 
	{
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.INVITE_TOKEN_ADDED, this);
		super.onDestroy();
	}

	public void onShareUrlClicked(View v)
	{
		// Adding the user's invite token to the invite url
		String inviteMessage = getString(R.string.invite_message);
		String defaultInviteURL = getString(R.string.default_invite_url);
		inviteMessage = inviteMessage.replace(defaultInviteURL, inviteUrlWithToken);

		Intent s = new Intent(android.content.Intent.ACTION_SEND);

		s.setType("text/plain");
		s.putExtra(Intent.EXTRA_TEXT, inviteMessage);
		startActivity(s);
	}

	private void setInviteToken()
	{
		inviteUrlWithToken = getString(R.string.default_invite_url) + getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getString(HikeConstants.INVITE_TOKEN, "");
		inviteUrl.setText(inviteUrlWithToken);
	}

	@Override
	public void onBackPressed() 
	{
		if(adapter instanceof HikeInviteAdapter)
		{
			Utils.incrementNumTimesScreenOpen(sharedPreferences, HikeMessengerApp.NUM_TIMES_INVITE);
		}
		super.onBackPressed();
	}

	private void showCreditsHelp(Bundle savedInstanceState)
	{
		sharedPreferences = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
		creditsHelpBtn = (ImageButton) findViewById(R.id.title_image_btn);
		View buttonBar = (View) findViewById(R.id.button_bar);
		buttonBar.setVisibility(View.VISIBLE);
		creditsHelpBtn.setVisibility(View.VISIBLE);
		creditsHelpBtn.setImageResource(R.drawable.credits_btn);

		if (!sharedPreferences.getBoolean(HikeMessengerApp.INVITE_TOOLTIP_DISMISSED, false) 
				&& Utils.wasScreenOpenedNNumberOfTimes(sharedPreferences, HikeMessengerApp.NUM_TIMES_INVITE)) 
		{
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

	@Override
	public void onEventReceived(String type, Object object) 
	{
		if(HikePubSub.INVITE_TOKEN_ADDED.equals(type))
		{
			runOnUiThread(new Runnable() 
			{
				@Override
				public void run() 
				{
					setInviteToken();
				}
			});
		}
	}
}
