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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Button;
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

public class HikeListActivity extends Activity implements OnScrollListener, TextWatcher, OnClickListener
{
	private HikeArrayAdapter adapter;
	private ListView listView;
	private EditText filterText;
	private TextView labelView;
	private ViewGroup creditsHelpLayout;
	private ImageButton closeBtn;
	private Button learnMore;
	private SharedPreferences sharedPreferences;
	private Editor editor;
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
			showCreditsHelp();
		}
	}
	
	private void showCreditsHelp()
	{
		sharedPreferences = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
		creditsHelpBtn = (ImageButton) findViewById(R.id.title_image_btn);
		View buttonBar = (View) findViewById(R.id.button_bar);
		buttonBar.setVisibility(View.VISIBLE);
		creditsHelpBtn.setVisibility(View.VISIBLE);
		creditsHelpBtn.setImageResource(R.drawable.credits_btn_selector);
		creditsHelpBtn.setOnClickListener(this);

		if(sharedPreferences.getBoolean(HikeConstants.Extras.SHOW_CREDITS_HELP, true))
		{
			editor = sharedPreferences.edit();
			creditsHelpLayout = (ViewGroup) findViewById(R.id.credits_help_layout);
			closeBtn = (ImageButton) creditsHelpLayout.findViewById(R.id.close);
			learnMore = (Button) creditsHelpLayout.findViewById(R.id.learn_more_btn);
			
			creditsHelpLayout.setVisibility(View.VISIBLE);
			closeBtn.setOnClickListener(this);
			learnMore.setOnClickListener(this);

			int i = 0;

			if((i = sharedPreferences.getInt(HikeConstants.Extras.CREDITS_HELP_COUNTER, 1)) < 3)
			{
				editor.putInt(HikeConstants.Extras.CREDITS_HELP_COUNTER, ++i);
				editor.commit();
			}
			else
			{
				removeCreditsHelp();
			}
		}
	}
	
	@Override
	public void onClick(View v) 
	{
		switch (v.getId()) 
		{
		case R.id.learn_more_btn:
			creditsHelpLayout.setVisibility(View.GONE);
			removeCreditsHelp();
		case R.id.title_image_btn:
			Intent i = new Intent(HikeListActivity.this, CreditsActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
			break;
		case R.id.close:
			creditsHelpLayout.setVisibility(View.GONE);
			removeCreditsHelp();
			break;
		}
	}
	
	private void removeCreditsHelp()
	{
		editor.putBoolean(HikeConstants.Extras.SHOW_CREDITS_HELP, false);
		editor.remove(HikeConstants.Extras.CREDITS_HELP_COUNTER);
		editor.commit();
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
