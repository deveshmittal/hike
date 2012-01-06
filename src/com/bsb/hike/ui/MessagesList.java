package com.bsb.hike.ui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
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
import com.bsb.hike.HikeService;
import com.bsb.hike.R;
import com.bsb.hike.adapters.ConversationsAdapter;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.ContactUtils;
import com.bsb.hike.utils.HikeConversationsDatabase;

public class MessagesList extends Activity implements OnClickListener, HikePubSub.Listener, android.content.DialogInterface.OnClickListener, Runnable {

	private Map<String, Conversation> mConversationsByMSISDN;
	private Set<String> mConversationsAdded;

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

	private class DeleteConversationsAsyncTask extends AsyncTask<Long, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Long... params) {
		    HikeConversationsDatabase db = null;
		    try {
		        db = new HikeConversationsDatabase(MessagesList.this);
		        db.deleteConversation(params);
		    } finally {
	            if (db != null) {
	                db.close();
	            }
		    }
		    return Boolean.TRUE;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result == Boolean.TRUE) {
				mAdapter.clear();
				mAdapter.notifyDataSetChanged();
				mAdapter.setNotifyOnChange(false);
				mConversationsByMSISDN.clear();
			}
		}		
	}

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

    private RelativeLayout mEmptyView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
	    if (token == null) {
	    	startActivity(new Intent(this, WelcomeActivity.class));
	    	finish();
	        return;
	    } else if (!settings.getBoolean(HikeMessengerApp.ADDRESS_BOOK_SCANNED, false)) {
	        startActivity(new Intent(this, ScanningAddressBook.class));
	        finish();
	        return;
	    }

	    AccountUtils.setToken(token);
	    HikeMessengerApp.getPubSub().publish(HikePubSub.TOKEN_CREATED, token);
	    //TODO this is being called everytime this activity is created.  Way too often
	    startService(new Intent(this, HikeService.class));

	    setContentView(R.layout.main);
    	mConversationsView = (ListView) findViewById(R.id.conversations);

    	mSearchIconView = findViewById(R.id.search);
    	mSearchIconView.setOnClickListener(this);
    	mEditMessageIconView = findViewById(R.id.edit_message);
    	mEditMessageIconView.setOnClickListener(this);
 
    	/* set the empty view layout for the list */
    	LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	mEmptyView = (RelativeLayout) vi.inflate(R.layout.empty_conversations, null);
    	mEmptyView.setVisibility(View.GONE);
    	mEmptyView.setOnClickListener(this);
    	RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
    			RelativeLayout.LayoutParams.FILL_PARENT,
    			RelativeLayout.LayoutParams.FILL_PARENT);
    	params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
    	mEmptyView.setLayoutParams(params);
    	((ViewGroup) mConversationsView.getParent()).addView(mEmptyView);
    	mConversationsView.setEmptyView(mEmptyView);
  
    	HikeConversationsDatabase db = new HikeConversationsDatabase(this);
    	List<Conversation> conversations = db.getConversations();
    	db.close();

    	mConversationsByMSISDN = new HashMap<String, Conversation>(conversations.size());
    	mConversationsAdded = new HashSet<String>();

    	/* Use an iterator so we can remove conversations w/ no messages
    	 * from our list
    	 */
    	for (Iterator<Conversation> iter = conversations.iterator(); iter.hasNext();)
    	{
    	    Conversation conv = (Conversation) iter.next();
    	    mConversationsByMSISDN.put(conv.getMsisdn(), conv);
    	    if (conv.getMessages().isEmpty()) {
    	        iter.remove();
    	    } else {
    	        mConversationsAdded.add(conv.getMsisdn());
    	    }
    	}

        mAdapter = new ConversationsAdapter(this, R.layout.conversation_item, conversations);
        /* because notifyOnChange gets re-enabled whenever we call notifyDataSetChanged
         * it's simpler to assume it's set to false and always notifyOnChange by hand
         */
        mAdapter.setNotifyOnChange(false);
        mConversationsView.setAdapter(mAdapter);

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
			return true;
		case R.id.deleteconversations:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.delete_all_question).setPositiveButton("Delete", this).setNegativeButton(R.string.cancel, this).show();
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
    protected void onDestroy() {
        super.onDestroy();
        HikeMessengerApp.getPubSub().removeListener(HikePubSub.MESSAGE_RECEIVED, this);
        HikeMessengerApp.getPubSub().removeListener(HikePubSub.NEW_CONVERSATION, this);
    }

    @Override
	public void onClick(View v) {
		if ( (v == mEditMessageIconView) || (v == mEmptyView)) {
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
			    //When a message gets sent from a user we don't have a conversation for, the message gets
			    //broadcasted first then the conversation gets created.  It's okay that we don't add it now, because
			    //when the conversation is broadcasted it will contain the messages
				return;
			}

			/* notification must be done on the thread that created the view (the UI thread in our case)
			 * We don't want to sort the list on the UI thread so instead, disable notification and manually notify on the UI thread
			 * We have to ensure it's disabled because calling notifyDataSetChanged will re-enable notifyOnChange
			 */
			mAdapter.setNotifyOnChange(false);
			if (!mConversationsAdded.contains(conv.getMsisdn())) {
			    mConversationsAdded.add(conv.getMsisdn());
			    mAdapter.add(conv);
			}

			conv.addMessage(message);

			runOnUiThread(this);
		} else if (HikePubSub.NEW_CONVERSATION.equals(type)) {
			final Conversation conversation = (Conversation) object;
			mConversationsByMSISDN.put(conversation.getMsisdn(), conversation);
			if (conversation.getMessages().isEmpty()) {
			    return;
			}

			mConversationsAdded.add(conversation.getMsisdn());
            mAdapter.add(conversation);

			runOnUiThread(this);
		}
	}

	public void run() {
	    mAdapter.notifyDataSetChanged();
	    //notifyDataSetChanged sets notifyonChange to true but we want it to always be false
	    mAdapter.setNotifyOnChange(false);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		case DialogInterface.BUTTON_POSITIVE:
			Long[] ids = new Long[mAdapter.getCount()];
			for(int i = 0;i < ids.length; i++) {
				ids[i] = mAdapter.getItem(i).getConvId();
			}
			DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask();
			task.execute(ids);
			break;
		default:
		}
	}
}

