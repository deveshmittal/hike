package com.bsb.hike.ui;

import java.util.Collections;
import java.util.List;

import android.app.ListActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.ListAdapter;

import com.bsb.hike.R;
import com.bsb.hike.adapters.HikeArrayAdapter;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;

public class HikeListActivity extends ListActivity
{

	ListAdapter createListAdapter()
	{
		HikeUserDatabase db = new HikeUserDatabase(this);
		List<ContactInfo> contacts = db.getContacts();
		Collections.sort(contacts);
		db.close();
		contacts.toArray(new ContactInfo[]{});
		ListAdapter adapter = new HikeArrayAdapter(this,
												R.layout.invite_item,
												contacts);

		return adapter;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hikelistactivity);
		getListView().setTextFilterEnabled(true);
		setListAdapter(createListAdapter());
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
	}

}
