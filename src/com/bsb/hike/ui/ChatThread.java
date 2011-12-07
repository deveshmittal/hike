package com.bsb.hike.ui;
import com.bsb.hike.R;
import com.bsb.hike.utils.HikeUserDatabase;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.Contacts.People;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;


public class ChatThread extends Activity {
	HikeUserDatabase mDbhelper;
	SQLiteDatabase mDb;
	Cursor mCursor;
	private long mContactId;
	private String mContactName;

	private void createAutoCompleteView() {
    	Log.d("ChatThread", "edit view");
    	View bottomPanel = findViewById(R.id.bottom_panel);
    	bottomPanel.setVisibility(View.GONE);
    	View nameView = findViewById(R.id.name_field);
    	nameView.setVisibility(View.GONE);
    	final AutoCompleteTextView inputNumberView = (AutoCompleteTextView) findViewById(R.id.input_number);
		mDbhelper = new HikeUserDatabase(this);
		mDb = mDbhelper.getReadableDatabase();
    	String[] columns = new String[] { "name", "msisdn"};
    	int[] to = new int[] { R.id.name, R.id.number };
    	SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.name_item, null, columns, to);
    	adapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
			@Override
			public CharSequence convertToString(Cursor cursor) {
				String name = cursor.getString(cursor.getColumnIndex("name"));
				return name;
			}
		});

    	adapter.setFilterQueryProvider(new FilterQueryProvider() {
			@Override
			public Cursor runQuery(CharSequence constraint) {
				String str = (constraint != null) ? constraint + "%" : "%";
				if (mCursor != null) {
					stopManagingCursor(mCursor);
					mCursor.close();
				}
				mCursor = mDb.rawQuery("SELECT name, id AS _id, msisdn, onhike FROM users WHERE name LIKE ?", new String[] { str });
				startManagingCursor(mCursor);
				return mCursor;
			}
		});

    	inputNumberView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> list, View _empty, int position,
					long id) {
				String contactName = inputNumberView.getText().toString();
				Log.d("TAG", "Name is " + contactName);
				createConversation(id, contactName);
			}
		});
  
    	inputNumberView.setAdapter(adapter);
    	inputNumberView.setVisibility(View.VISIBLE);
    	inputNumberView.requestFocus();
    }		

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.chatthread);

	    Intent intent = getIntent();
	    long contactId = intent.getLongExtra("id", -1);
	    if (contactId < 0) {
	    	createAutoCompleteView();
	    } else {
	    	String contactName = intent.getStringExtra("contactName");
	    	createConversation(contactId, contactName);
	    }
	}

	private void createConversation(long contactId, String contactName) {
    	View bottomPanel = findViewById(R.id.bottom_panel);
    	bottomPanel.setVisibility(View.VISIBLE);
    	TextView nameView = (TextView) findViewById(R.id.name_field);
    	nameView.setVisibility(View.VISIBLE);
    	final AutoCompleteTextView inputNumberView = (AutoCompleteTextView) findViewById(R.id.input_number);
    	inputNumberView.setVisibility(View.GONE);
 
	    nameView.setText(contactName);
		mContactId = contactId;
		mContactName = contactName;
	}

	protected void onStop() {
		super.onStop();
		Log.d(getLocalClassName(), "OnStop called");
		if (mDb != null) {
			mDbhelper.close();
			mDb.close();
			mDb = null;
			mDbhelper = null;
		}
	}
}
