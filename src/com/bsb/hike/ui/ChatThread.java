package com.bsb.hike.ui;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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
import com.bsb.hike.adapters.MessagesAdapter;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.utils.HikeConversationsDatabase;
import com.bsb.hike.utils.HikeUserDatabase;


public class ChatThread extends Activity implements HikePubSub.Listener, TextWatcher {

	private HikePubSub mPubSub;
	private HikeUserDatabase mDbhelper;
	private Cursor mCursor;
	private long mContactId;
	private String mContactName;
	private String mContactNumber;
	private MessagesAdapter mAdapter;
	private EditText mComposeView;
	private ListView mConversationsView;
	private Conversation mConversation;
	private long mTextLastChanged;
	private TextView mNameView;
	private SetTypingText mClearTypingCallback;
	private ResetTypingNotification mResetTypingNotification;
	private Handler mUiThreadHandler;

	@Override
	protected void onPause() {
		super.onPause();
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, null);
	}

	@Override
	protected void onResume() {
		super.onResume();
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, this);
	}

	private void createAutoCompleteView() {
    	Log.d("ChatThread", "edit view");
    	View bottomPanel = findViewById(R.id.bottom_panel);
    	bottomPanel.setVisibility(View.GONE);
    	View nameView = findViewById(R.id.name_field);
    	nameView.setVisibility(View.GONE);
    	final AutoCompleteTextView inputNumberView = (AutoCompleteTextView) findViewById(R.id.input_number);
		mDbhelper = new HikeUserDatabase(this);
        String[] columns = new String[] { "name", "msisdn", "onhike"};
        int[] to = new int[] { R.id.name, R.id.number, R.id.onhike};
    	SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.name_item, null, columns, to);
        adapter.setViewBinder(new DropDownViewBinder());
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
				mCursor = mDbhelper.findUsers(str);
				return mCursor;
			}
		});

    	inputNumberView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> list, View _empty, int position,
					long id) {
				mContactId = id;
				mDbhelper.close();
				mDbhelper = null;
				createConversation();
				mComposeView.requestFocus();
			}
		});
  
    	inputNumberView.setAdapter(adapter);
    	inputNumberView.setVisibility(View.VISIBLE);
    	inputNumberView.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

	@Override
	protected void onDestroy() {
        super.onDestroy();
        HikeMessengerApp.getPubSub().removeListener(HikePubSub.MESSAGE_RECEIVED, this);
        HikeMessengerApp.getPubSub().removeListener(HikePubSub.TYPING_CONVERSATION, this);
        HikeMessengerApp.getPubSub().removeListener(HikePubSub.END_TYPING_CONVERSATION, this);
        if (mDbhelper != null) {
            mDbhelper.close();
            mDbhelper = null;
        }
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
		mUiThreadHandler = new Handler();
	}

	public void onSendClick(View v) {
		Log.d("ChatThread", "Send Button Called");
		String message = mComposeView.getText().toString();
		mComposeView.setText("");
		int time = (int) System.currentTimeMillis()/1000;
		ConvMessage convMessage = new ConvMessage(message, mContactNumber, Long.toString(mContactId), time, true);
		convMessage.setConversation(mConversation);
		mAdapter.add(convMessage);
		mPubSub.publish(HikePubSub.MESSAGE_SENT, convMessage);
		mPubSub.publish(HikePubSub.WS_SEND, convMessage.serialize("send"));
	    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
	    imm.hideSoftInputFromWindow(mComposeView.getWindowToken(), 0);
	}

	private void createConversation() {
    	final AutoCompleteTextView inputNumberView = (AutoCompleteTextView) findViewById(R.id.input_number);
    	inputNumberView.setVisibility(View.GONE);

        View bottomPanel = findViewById(R.id.bottom_panel);
        bottomPanel.setVisibility(View.VISIBLE);
        mNameView = (TextView) findViewById(R.id.name_field);
        mNameView.setVisibility(View.VISIBLE);
        mNameView.setText(mContactName);

        HikeConversationsDatabase db = new HikeConversationsDatabase(this);
        mConversation = db.getConversation(mContactNumber, 10);
        if (mConversation == null) {
            mConversation = db.addConversation(mContactNumber);
        }
        db.close();
        List<ConvMessage> messages = mConversation.getMessages();

        mConversationsView = (ListView) findViewById(R.id.conversations_list);
        mConversationsView.setStackFromBottom(true);
        mAdapter = new MessagesAdapter(this, messages, mConversation);
        mConversationsView.setAdapter(mAdapter);
        mComposeView = (EditText) findViewById(R.id.msg_compose);
        HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_RECEIVED,
                this);
        HikeMessengerApp.getPubSub().addListener(
                HikePubSub.TYPING_CONVERSATION, this);
        HikeMessengerApp.getPubSub().addListener(
                HikePubSub.END_TYPING_CONVERSATION, this);
        mComposeView.addTextChangedListener(this);
	}

	private class SetTypingText implements Runnable {
		public SetTypingText(boolean direction) {
			this.direction = direction;
		}
		boolean direction;

		@Override
		public void run() {
			if (direction) {
				mNameView.setText(mContactName + " is typing");
			} else {
				mNameView.setText(mContactName);
			}
		}
	}

	@Override
	public void onEventReceived(String type, Object object) {
		if (HikePubSub.MESSAGE_RECEIVED.equals(type)) {
			final ConvMessage conv = (ConvMessage) object;
			if (conv.getMsisdn().indexOf(mContactNumber) != -1) {
				/* we publish the message before the conversation is created, 
				 * so it's safer to just tack it on here */
				conv.setConversation(mConversation);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mAdapter.add(conv);
					}});
			}
		} else if (HikePubSub.END_TYPING_CONVERSATION.equals(type)) {
			if (mContactNumber.equals(object)) {
				if (mClearTypingCallback != null) {
					//we can assume that if we don't have the callback, then the UI should be in the right state already
					runOnUiThread(mClearTypingCallback);
					mUiThreadHandler.removeCallbacks(mClearTypingCallback);
				}
			}
		} else if (HikePubSub.TYPING_CONVERSATION.equals(type)) {
			if (mContactNumber.equals(object)) {
				runOnUiThread(new SetTypingText(true));
				//Lazily create the callback to reset the label
				if (mClearTypingCallback == null) {
					mClearTypingCallback = new SetTypingText(false);
				} else {
					//we've got another typing notification, so we want to clear it a while from now
					mUiThreadHandler.removeCallbacks(mClearTypingCallback);
				}
				mUiThreadHandler.postDelayed(mClearTypingCallback, 20*1000);
			}
		}
	}

	public String getContactNumber() {
		return mContactNumber;
	}

	class ResetTypingNotification implements Runnable {
		@Override
		public void run() {
			long current = System.currentTimeMillis();
			if (current - mTextLastChanged >= 5*1000) { //text hasn't changed in 10 seconds, send an event
			    mPubSub.publish(HikePubSub.WS_SEND, mConversation.serialize("stop_typing"));
				mTextLastChanged = 0;
			} else { //text has changed, fire a new event
				long delta = 10*1000 - (current - mTextLastChanged);
				mUiThreadHandler.postDelayed(mResetTypingNotification, delta);
			}
		}
	};

	@Override
	public void afterTextChanged(Editable editable) {
		if (editable.toString().isEmpty()) {
			return;
		}

		if (mResetTypingNotification == null) {
			mResetTypingNotification = new ResetTypingNotification();
		}

		if (mTextLastChanged == 0) {
			//we're currently not in 'typing' mode
			mTextLastChanged = System.currentTimeMillis();
			//fire an event
            mPubSub.publish(HikePubSub.WS_SEND, mConversation.serialize("stop_typing"));

			//create a timer to clear the event
			mUiThreadHandler.removeCallbacks(mResetTypingNotification); //clear any existing ones
			mUiThreadHandler.postDelayed(mResetTypingNotification, 10*1000);
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int before,
			int count) {
		//blank
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		//blank
	}
}
