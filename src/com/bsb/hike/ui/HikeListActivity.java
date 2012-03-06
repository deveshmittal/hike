package com.bsb.hike.ui;

import java.util.Collections;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.utils.ContactUtils;

public class HikeListActivity extends ListActivity
{

	ListAdapter createListAdapter()
	{
		HikeUserDatabase db = new HikeUserDatabase(this);
		List<ContactInfo> contacts = db.getContacts();
		Collections.sort(contacts);
		db.close();
		contacts.toArray(new ContactInfo[]{});
		ListAdapter adapter = new ArrayAdapter<ContactInfo>(this,
												R.layout.invite_item,
												contacts)
		{
			public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent)
			{
				ContactInfo contactInfo = getItem(position);
				LayoutInflater inflater = (LayoutInflater) HikeListActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View v = convertView;
				if (v == null)
				{
					v = inflater.inflate(R.layout.invite_item, parent, false);
				}

				TextView textView = (TextView) v.findViewById(R.id.name);
				textView.setText(contactInfo.getName());

				Button button = (Button) v.findViewById(R.id.invite_button);
				button.setEnabled(!contactInfo.isOnhike());

				return v;
			};
		};

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
