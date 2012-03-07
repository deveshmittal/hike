package com.bsb.hike.ui;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
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
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;

public class HikeListActivity extends ListActivity implements OnScrollListener
{
	private HikeArrayAdapter adapter;
	private TextView sectionText;
	private RelativeLayout sectionContainer;

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
		ListView listView = getListView();
		setContentView(R.layout.hikelistactivity);
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

		EditText editText = (EditText) findViewById(R.id.filter);
		editText.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable text)
			{
				Filterable filterable = (Filterable) getListAdapter();
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
			
		});

		setListAdapter(adapter);
		getListView().setOnScrollListener(this);
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
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

}
