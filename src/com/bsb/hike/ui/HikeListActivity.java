package com.bsb.hike.ui;

import java.lang.reflect.Constructor;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnActionExpandListener;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.adapters.HikeArrayAdapter;
import com.bsb.hike.adapters.HikeArrayAdapter.Section;

public class HikeListActivity extends SherlockActivity implements OnScrollListener, OnActionExpandListener, TextWatcher
{
	private HikeArrayAdapter adapter;
	private TextView sectionText;
	private RelativeLayout sectionContainer;
	private ListView listView;
	private MenuItem searchMenu;

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

		sectionText = (TextView) findViewById(R.id.section_label);
		sectionContainer = (RelativeLayout) findViewById(R.id.section_container);

		listView.setAdapter(adapter);
		listView.setOnScrollListener(this);
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		if (adapter.getCount() == 0)
		{
			return;
		}

		Object o = adapter.getItem(firstVisibleItem);
		if (!(o instanceof Section))
		{
			String title = (String) adapter.idForPosition(firstVisibleItem);
			sectionText.setText(title);
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView arg0, int arg1)
	{		
	}

	public boolean onCreateOptionsMenu(Menu menu)
	{
		getSupportMenuInflater().inflate(R.menu.list_menu, menu);
		searchMenu = menu.getItem(0);
		searchMenu.setOnActionExpandListener(this);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);			
		}
	}

	@Override
	public boolean onSearchRequested()
	{
		return searchMenu.expandActionView();
	}

	@Override
	public boolean onMenuItemActionExpand(MenuItem item)
	{
		if (item.getItemId() != R.id.menu_search)
		{
			return true;
		}

		View view = item.getActionView().findViewById(R.id.searchview);
		if (view == null)
		{
			return true;
		}

		final EditText editText = (EditText) view;
		editText.addTextChangedListener(this);

		/* add this in a runnable, because if we try to
		 * expand this now the editText isn't actually visible.
		 */
		editText.post(new Runnable()
		{
			public void run()
			{
				editText.requestFocusFromTouch();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(editText, 0);
			}
		});
		return true;
	}

	@Override
	public boolean onMenuItemActionCollapse(MenuItem item)
	{
		if (item.getItemId() != R.id.menu_search)
		{
			return true;
		}

		View view = item.getActionView().findViewById(R.id.searchview);
		final EditText editText = (EditText) view;
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
		editText.post(new Runnable()
		{
			public void run()
			{
				editText.clearFocus();
			}
		});
		return true;
	}

	@Override
	public void afterTextChanged(Editable text)
	{
		Filterable filterable = (Filterable) listView.getAdapter();
		filterable.getFilter().filter(text);
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
