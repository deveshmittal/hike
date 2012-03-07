package com.bsb.hike.ui;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnActionExpandListener;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.adapters.HikeArrayAdapter;
import com.bsb.hike.adapters.HikeArrayAdapter.Section;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;

public class HikeListActivity extends SherlockActivity implements OnScrollListener, OnActionExpandListener, TextWatcher
{
	private HikeArrayAdapter adapter;
	private TextView sectionText;
	private RelativeLayout sectionContainer;
	private ListView listView;

	HikeArrayAdapter createListAdapter() throws Exception
	{
		HikeUserDatabase db = new HikeUserDatabase(this);
		List<ContactInfo> contacts = db.getContacts();
		Collections.sort(contacts);
		db.close();
		contacts.toArray(new ContactInfo[]{});
		Intent intent = getIntent();
		String adapterClassName = intent.getStringExtra(HikeConstants.ADAPTER_NAME);
		Class<HikeArrayAdapter> cls = (Class<HikeArrayAdapter>) Class.forName(adapterClassName);
		/* assume that there is only one constructor, and it's the one we want */
		Constructor c = cls.getConstructors()[0];
		return (HikeArrayAdapter) c.newInstance(this, -1, contacts);
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
			adapter = createListAdapter();
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
		menu.add("Search").
			setIcon(R.drawable.ic_searchicon).
			setActionView(com.bsb.hike.R.layout.actionbar_search).
			setOnActionExpandListener(this).
			setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
			
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
	public boolean onMenuItemActionExpand(MenuItem item)
	{
		View view = item.getActionView().findViewById(R.id.searchview);
		if (view == null)
		{
			return true;
		}

		EditText editText = (EditText) view;
//		editText.requestFocus();
		editText.addTextChangedListener(this);
		
		return true;
	}

	@Override
	public boolean onMenuItemActionCollapse(MenuItem item)
	{
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
