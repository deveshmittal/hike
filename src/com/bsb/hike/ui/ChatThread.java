package com.bsb.hike.ui;
import java.util.ArrayList;

import com.bsb.hike.R;
import com.bsb.hike.utils.HikeUserDatabase;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.Contacts.People;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;


public class ChatThread extends Activity {
	HikeUserDatabase mDbhelper;
	SQLiteDatabase mDb;
	Cursor mCursor;
	private long mContactId;
	private String mContactName;
	private String mContactNumber;
	private ConversationsAdapter mAdapter;
	private EditText mComposeView;
	private ListView mConversationsView;

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
				mContactNumber = cursor.getString(cursor.getColumnIndex("msisdn"));
				mContactName = cursor.getString(cursor.getColumnIndex("name"));
				return mContactName;
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
				mContactId = id;
				createConversation();
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
	    mContactId = intent.getLongExtra("id", -1);
	    if (mContactId < 0) {
	    	createAutoCompleteView();
	    } else {
	    	mContactName = intent.getStringExtra("contactName");
	    	createConversation();
	    }
	}

	public void onSendClick(View v) {
		Log.d("ChatThread", "Send Button Called");
		String message = mComposeView.getText().toString();
		mComposeView.setText("");
		int time = (int) System.currentTimeMillis()/10000;
		Conversation conversation = new Conversation(message, time, true, false /*TODO this should come from the activity */);
		mAdapter.add(conversation);
	    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
	    imm.hideSoftInputFromWindow(mComposeView.getWindowToken(), 0);
//		mConversationsView.requestFocus();
	}

	private void createConversation() {
    	View bottomPanel = findViewById(R.id.bottom_panel);
    	bottomPanel.setVisibility(View.VISIBLE);
    	TextView nameView = (TextView) findViewById(R.id.name_field);
    	nameView.setVisibility(View.VISIBLE);
    	final AutoCompleteTextView inputNumberView = (AutoCompleteTextView) findViewById(R.id.input_number);
    	inputNumberView.setVisibility(View.GONE);
 
	    nameView.setText(mContactName);
	    ArrayList<Conversation> conversations = new ArrayList<Conversation>();
	    mConversationsView = (ListView) findViewById(R.id.conversations_list);
	    mAdapter = new ConversationsAdapter(this, R.layout.conversation_item, conversations);
	    mConversationsView.setAdapter(mAdapter);
	    mComposeView = (EditText) findViewById(R.id.msg_compose);
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
