package com.bsb.hike.ui;

import java.util.Collections;
import java.util.List;

import android.app.ListActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.adapters.HikeArrayAdapter;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;

public class HikeListActivity extends ListActivity implements OnScrollListener
{
	private HikeArrayAdapter adapter;
	private TextView sectionText;

	HikeArrayAdapter createListAdapter()
	{
		HikeUserDatabase db = new HikeUserDatabase(this);
		List<ContactInfo> contacts = db.getContacts();
		Collections.sort(contacts);
		db.close();
		contacts.toArray(new ContactInfo[]{});
		HikeArrayAdapter adapter = new HikeArrayAdapter(this,
												R.layout.invite_item,
												contacts);

		return adapter;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		ListView listView = getListView();
		setContentView(R.layout.hikelistactivity);
		listView.setTextFilterEnabled(true);
		adapter = createListAdapter();

		sectionText = (TextView) findViewById(R.id.section_label);

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
		Log.d("HikeListActivity", "Top Item is " + firstVisibleItem);
		Log.d("HikeListActivity", "Section is " + adapter.getSectionForPosition(firstVisibleItem));
		String title = (String) adapter.idForPosition(firstVisibleItem);
		sectionText.setText(title);
	}

	@Override
	public void onScrollStateChanged(AbsListView arg0, int arg1)
	{		
	}

}
