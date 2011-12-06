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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;


public class ChatThread extends Activity {
	HikeUserDatabase mDbhelper;
	SQLiteDatabase mDb;
	Cursor mCursor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.chatthread);
	    Intent intent = getIntent();
	    if (intent.getBooleanExtra("edit", false)) {
	    	Log.d("ChatThread", "edit view");
	    	View bottomPanel = findViewById(R.id.bottom_panel);
	    	bottomPanel.setVisibility(View.GONE);
	    	View nameView = findViewById(R.id.name_field);
	    	nameView.setVisibility(View.GONE);
	    	AutoCompleteTextView inputNumberView = (AutoCompleteTextView) findViewById(R.id.input_number);
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

	    	inputNumberView.setAdapter(adapter);
	    	inputNumberView.setVisibility(View.VISIBLE);
	    	inputNumberView.requestFocus();
	    }
	}

	protected void onStop() {
		super.onStop();
		if (mDb != null) {
			mDbhelper.close();
			mDb.close();
		}
	}
}
