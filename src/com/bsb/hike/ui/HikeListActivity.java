package com.bsb.hike.ui;

import java.lang.reflect.Constructor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.adapters.HikeArrayAdapter;
import com.bsb.hike.adapters.HikeArrayAdapter.Section;

public class HikeListActivity extends Activity implements OnScrollListener, TextWatcher
{
	private HikeArrayAdapter adapter;
	private TextView sectionText;
	private RelativeLayout sectionContainer;
	private ListView listView;
	private EditText filterText;
	private TextView labelView;

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

		sectionText = (TextView) findViewById(R.id.section_label);
		sectionContainer = (RelativeLayout) findViewById(R.id.section_container);

		listView.setAdapter(adapter);
		listView.setOnScrollListener(this);

		filterText = (EditText) findViewById(R.id.filter);
		filterText.addTextChangedListener(this);
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
