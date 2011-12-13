package com.bsb.hike.ui;

import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.ConversationsAdapter;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.ContactUtils;
import com.bsb.hike.utils.HikeConversationsDatabase;

public class MessagesList extends Activity implements OnClickListener, HikePubSub.Listener {

	private HashMap<String, Conversation> mConversationsByMSISDN;

	public class InviteFriendAsyncTask extends AsyncTask<Uri, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Uri... params) {
			Uri uri = params[0];
			String number = ContactUtils.getMobileNumber(MessagesList.this.getContentResolver(), uri);
			return AccountUtils.invite(number);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			Context ctx = MessagesList.this.getApplicationContext();
			int duration;
			CharSequence message;
			if (result.booleanValue()) {
				duration = Toast.LENGTH_SHORT;
				message = getString(R.string.invite_sent);
			} else {
				duration = Toast.LENGTH_LONG;
				message = getString(R.string.invite_failed);
			}
			Toast toast = Toast.makeText(ctx, message, duration);
			toast.show();
		}
	}

	private static final int INVITE_PICKER_RESULT = 1001;

	private ListView mConversationsView;
	private View mSearchIconView;
	private View mEditMessageIconView;

	private ConversationsAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
	    if (token == null) {
	    	startActivity(new Intent(this, WelcomeActivity.class));
	    	finish();
	    }

	    AccountUtils.setToken(token);
	    HikeMessengerApp app = (HikeMessengerApp) getApplication();
	    app.startWebSocket();
	    setContentView(R.layout.main);
    	mConversationsView = (ListView) findViewById(R.id.conversations);

    	mSearchIconView = findViewById(R.id.search);
    	mSearchIconView.setOnClickListener(this);
    	mEditMessageIconView = findViewById(R.id.edit_message);
    	mEditMessageIconView.setOnClickListener(this);
    	LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	RelativeLayout emptyView = (RelativeLayout) vi.inflate(R.layout.empty_conversations, null);
    	emptyView.setVisibility(View.GONE);
    	emptyView.setOnClickListener(this);
    	RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
    			RelativeLayout.LayoutParams.FILL_PARENT,
    			RelativeLayout.LayoutParams.FILL_PARENT);
    	params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
    	emptyView.setLayoutParams(params);
    	((ViewGroup) mConversationsView.getParent()).addView(emptyView);
    	mConversationsView.setEmptyView(emptyView);
  
    	HikeConversationsDatabase db = new HikeConversationsDatabase(this);
    	List<Conversation> conversations = db.getConversations();
    	mAdapter = new ConversationsAdapter(this, R.layout.conversation_item, conversations);
    	mConversationsView.setAdapter(mAdapter);
    	db.close();

    	mConversationsByMSISDN = new HashMap<String, Conversation>(conversations.size());
    	for (Conversation conv : conversations) {
    		mConversationsByMSISDN.put(conv.getMsisdn(), conv);
    	}
   
    	HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_RECEIVED, this);
    	HikeMessengerApp.getPubSub().addListener(HikePubSub.NEW_CONVERSATION, this);

    	mConversationsView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int pos,
					long id) {
				Conversation conversation = (Conversation) adapter.getItemAtPosition(pos);
				Intent intent = new Intent(MessagesList.this, ChatThread.class);
				if (conversation.getContactName() != null) {
					intent.putExtra("name", conversation.getContactName());
				}
				if (conversation.getContactId() != null) {
					intent.putExtra("id", Long.parseLong(conversation.getContactId()));
				}
				intent.putExtra("msisdn", conversation.getMsisdn());
				startActivity(intent);
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch(requestCode) {
			case INVITE_PICKER_RESULT:
				Uri uri = data.getData();
				InviteFriendAsyncTask task = new InviteFriendAsyncTask();
				task.execute(uri);
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
		case R.id.invite:
			intent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
			startActivityForResult(intent, INVITE_PICKER_RESULT);
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onStop() {
		super.onDestroy();
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.MESSAGE_RECEIVED, this);
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.NEW_CONVERSATION, this);
	}

	@Override
	public void onClick(View v) {
		Log.d("MessagesList", "OnClick called: "+v);
		if (v == mEditMessageIconView) {
			Intent intent = new Intent(this, ChatThread.class);
			intent.putExtra("edit", true);
			startActivity(intent);
		}
	}

	@Override
	public void onEventReceived(String type, Object object) {
		if (HikePubSub.MESSAGE_RECEIVED.equals(type)) {
			ConvMessage message = (ConvMessage) object;
			/* find the conversation corresponding to this message */
			String msisdn = message.getMsisdn();
			Conversation conv = mConversationsByMSISDN.get(msisdn);
			if (conv == null) {
				Log.w("MessagesList", "New Conversation is null");
				return;
			}
			conv.addMessage(message);
			/* this could be a singleton object within this class */
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mAdapter.notifyDataSetChanged();
				}		
			});
		} else if (HikePubSub.NEW_CONVERSATION.equals(type)) {
			final Conversation conversation = (Conversation) object;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mAdapter.add(conversation);
				}
			});
		}
	}
}

