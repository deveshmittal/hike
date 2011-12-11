package com.bsb.hike.ui;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.ConversationsAdapter;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.utils.HikeConversationsDatabase;
import com.bsb.hike.utils.HikeUserDatabase;


public class ChatThread extends Activity {

	private HikePubSub mPubSub;
	private HikeUserDatabase mDbhelper;
	private Cursor mCursor;
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
    	String[] columns = new String[] { "name", "msisdn"};
    	int[] to = new int[] { R.id.name, R.id.number };
    	SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.name_item, null, columns, to);
    	adapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
			@Override
			public CharSequence convertToString(Cursor cursor) {
				Log.d("Cursor", "Cursor is " + cursor);
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
				mCursor = mDbhelper.findUsers(str);
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
	public Object onRetainNonConfigurationInstance() {
		Intent intent = new Intent();
		if (mContactName != null) {
			intent.putExtra("name", mContactName);
		}
		if (mContactId >= 0) {
			intent.putExtra("id", mContactId);
		}

		if (mContactNumber != null) {
			intent.putExtra("msisdn", mContactNumber);
		}

		return intent;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.chatthread);
	    mPubSub = HikeMessengerApp.getPubSub();
	    Object o = getLastNonConfigurationInstance();
	    Intent intent = (o instanceof Intent) ? (Intent) o : getIntent();
	    mContactNumber = intent.getStringExtra("msisdn");
	    if (mContactNumber == null) {
	    	createAutoCompleteView();
	    } else {
		    mContactId = intent.getLongExtra("id", -1);
		    mContactName = intent.getStringExtra("name");
	    	createConversation();
	    }
	}

	public void onSendClick(View v) {
		Log.d("ChatThread", "Send Button Called");
		String message = mComposeView.getText().toString();
		mComposeView.setText("");
		int time = (int) System.currentTimeMillis()/10000;
		ConvMessage convMessage = new ConvMessage(message, mContactNumber, Long.toString(mContactId), time, true);
		mAdapter.add(convMessage);
		mPubSub.publish(HikePubSub.MESSAGE_SENT, convMessage);
	    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
	    imm.hideSoftInputFromWindow(mComposeView.getWindowToken(), 0);
	}

	private void createConversation() {
    	View bottomPanel = findViewById(R.id.bottom_panel);
    	bottomPanel.setVisibility(View.VISIBLE);
    	TextView nameView = (TextView) findViewById(R.id.name_field);
    	nameView.setVisibility(View.VISIBLE);
    	final AutoCompleteTextView inputNumberView = (AutoCompleteTextView) findViewById(R.id.input_number);
    	inputNumberView.setVisibility(View.GONE);
  	    nameView.setText(mContactName);

 	    HikeConversationsDatabase db = new HikeConversationsDatabase(this);
    	List<ConvMessage> messages = db.getConversationThread(mContactNumber, 10);

	    mConversationsView = (ListView) findViewById(R.id.conversations_list);
	    mAdapter = new ConversationsAdapter(this, R.layout.conversation_item, messages);
	    mConversationsView.setAdapter(mAdapter);
	    mComposeView = (EditText) findViewById(R.id.msg_compose);
	}

	protected void onStop() {
		super.onStop();
		Log.d(getLocalClassName(), "OnStop called");
		if (mDbhelper != null) {
			mDbhelper.close();
			mDbhelper = null;
		}
	}
}
