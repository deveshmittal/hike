package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.Comparator;
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
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
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
import com.bsb.hike.utils.UserError;

public class MessagesList extends Activity implements OnClickListener, HikePubSub.Listener, android.content.DialogInterface.OnClickListener, Runnable
{

	private Map<String, Conversation> mConversationsByMSISDN;

	private Set<String> mConversationsAdded;

	@Override
	protected void onPause()
	{
		super.onPause();
		Log.d("MESSAGE LIST","Currently in pause state. .......");
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, null);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		Log.d("MESSAGE LIST","Resumed .....");
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, this);
	}

	private class DeleteConversationsAsyncTask extends AsyncTask<Conversation, Void, Conversation[]>
	{

		@Override
		protected Conversation[] doInBackground(Conversation... convs)
		{
			HikeConversationsDatabase db = null;
			ArrayList<Long> ids = new ArrayList<Long>(convs.length);
			for (Conversation conv : convs)
			{
				ids.add(conv.getConvId());
			}

			try
			{
				db = new HikeConversationsDatabase(MessagesList.this);
				db.deleteConversation(ids.toArray(new Long[]{}));
			}
			finally
			{
				if (db != null)
				{
					db.close();
				}
			}
			return convs;
		}

		@Override
		protected void onPostExecute(Conversation[] deleted)
		{
			for (Conversation conversation : deleted)
			{
				mAdapter.remove(conversation);
				mConversationsByMSISDN.remove(conversation.getMsisdn());
				mConversationsAdded.remove(conversation.getMsisdn());
			}

			mAdapter.notifyDataSetChanged();
			mAdapter.setNotifyOnChange(false);
		}
	}

	public class InviteFriendAsyncTask extends AsyncTask<Uri, Void, String>
	{
		@Override
		protected String doInBackground(Uri... params)
		{
			Uri uri = params[0];
			String number = ContactUtils.getMobileNumber(MessagesList.this.getContentResolver(), uri);
			try
			{
				AccountUtils.invite(number);
				return getString(R.string.invite_sent);
			}
			catch(UserError err)
			{
				return err.message;
			}
		}

		@Override
		protected void onPostExecute(String message)
		{
			Context ctx = MessagesList.this.getApplicationContext();
			int duration = Toast.LENGTH_LONG;
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

	private Comparator<? super Conversation> mConversationsComparator;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Log.d("MESSAGE LIST","On create is Called .....");
		SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
		if (token == null)
		{
			startActivity(new Intent(this, WelcomeActivity.class));
			finish();
			return;
		}
		else if (!settings.getBoolean(HikeMessengerApp.ADDRESS_BOOK_SCANNED, false))
		{
			startActivity(new Intent(this, AccountCreateSuccess.class));
			finish();
			return;
		}

		HikeMessengerApp.getPubSub().publish(HikePubSub.TOKEN_CREATED, token);
		// TODO this is being called everytime this activity is created. Way too often
		HikeMessengerApp app = (HikeMessengerApp) getApplicationContext();
		app.connectToService();

		setContentView(R.layout.main);
		mConversationsView = (ListView) findViewById(R.id.conversations);

/*		mSearchIconView = findViewById(R.id.search);
		mSearchIconView.setOnClickListener(this);*/

		mEditMessageIconView = findViewById(R.id.edit_message);
		mEditMessageIconView.setOnClickListener(this);

		/* set the empty view layout for the list */
		LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mEmptyView = (RelativeLayout) vi.inflate(R.layout.empty_conversations, null);

		mEmptyView.setVisibility(View.GONE);

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);
		params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
		mEmptyView.setLayoutParams(params);
		((ViewGroup) mConversationsView.getParent()).addView(mEmptyView);
		mConversationsView.setEmptyView(mEmptyView);

		HikeConversationsDatabase db = new HikeConversationsDatabase(this);
		List<Conversation> conversations = db.getConversations();
		db.close();

		mConversationsByMSISDN = new HashMap<String, Conversation>(conversations.size());
		mConversationsAdded = new HashSet<String>();

		/*
		 * Use an iterator so we can remove conversations w/ no messages from our list
		 */
		for (Iterator<Conversation> iter = conversations.iterator(); iter.hasNext();)
		{
			Conversation conv = (Conversation) iter.next();
			mConversationsByMSISDN.put(conv.getMsisdn(), conv);
			if (conv.getMessages().isEmpty())
			{
				iter.remove();
			}
			else
			{
				mConversationsAdded.add(conv.getMsisdn());
			}
		}

		mAdapter = new ConversationsAdapter(this, R.layout.conversation_item, conversations);
		/* we need this object every time a message comes in, seems simplest to just create it once */
		mConversationsComparator = new Conversation.ConversationComparator();

		/*
		 * because notifyOnChange gets re-enabled whenever we call notifyDataSetChanged it's simpler to assume it's set to false and always notifyOnChange by hand
		 */
		mAdapter.setNotifyOnChange(false);
		mConversationsView.setAdapter(mAdapter);

		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_RECEIVED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.NEW_CONVERSATION, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_SENT, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MSG_READ, this);

		mConversationsView.setOnItemClickListener(new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int pos, long id)
			{
				Conversation conversation = (Conversation) adapter.getItemAtPosition(pos);
				Intent intent = createIntentForConversation(conversation);
				startActivity(intent);
			}
		});

		/* register for long-press's */
		registerForContextMenu(mConversationsView);
	}

	private Intent createIntentForConversation(Conversation conversation)
	{
		Intent intent = new Intent(MessagesList.this, ChatThread.class);
		if (conversation.getContactName() != null)
		{
			intent.putExtra("name", conversation.getContactName());
		}
		if (conversation.getContactId() != null)
		{
			intent.putExtra("id", conversation.getContactId());
		}
		intent.putExtra("msisdn", conversation.getMsisdn());
		return intent;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Conversation conv = mAdapter.getItem((int) info.id);
		switch (item.getItemId())
		{
		case R.id.pin:
	        Intent shortcutIntent = createIntentForConversation(conv);
	        Intent intent = new Intent();
	        Log.i("CreateShortcut", "Creating intent for broadcasting");
	        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
	        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, conv.getContactName());
	        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_hikelogo));
	        intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
	        sendBroadcast(intent);
			return true;
		case R.id.delete:
			DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask();
			task.execute(conv);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.conversation_menu, menu);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (resultCode == RESULT_OK)
		{
			switch (requestCode)
			{
			case INVITE_PICKER_RESULT:
				Uri uri = data.getData();
				InviteFriendAsyncTask task = new InviteFriendAsyncTask();
				task.execute(uri);
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		Intent intent;
		switch (item.getItemId())
		{
		case R.id.invite:
			intent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
			startActivityForResult(intent, INVITE_PICKER_RESULT);
			return true;
		case R.id.deleteconversations:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.delete_all_question).setPositiveButton("Delete", this).setNegativeButton(R.string.cancel, this).show();
		case R.id.settings:
			intent = new Intent(this, HikePreferences.class);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.MSG_READ, this);
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.MESSAGE_SENT, this);
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.MESSAGE_RECEIVED, this);
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.NEW_CONVERSATION, this);
	}

	@Override
	public void onClick(View v)
	{
		if ((v == mEditMessageIconView) || (v == mEmptyView))
		{
			Intent intent = new Intent(this, ChatThread.class);
			intent.putExtra("edit", true);
			startActivity(intent);
		}
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if ( (HikePubSub.MESSAGE_RECEIVED.equals(type)) || (HikePubSub.MESSAGE_SENT.equals(type)) )
		{
			Log.d("MESSAGE LIST","New msg event sent or received.");
			ConvMessage message = (ConvMessage) object;
			/* find the conversation corresponding to this message */
			String msisdn = message.getMsisdn();
			Conversation conv = mConversationsByMSISDN.get(msisdn);
			if (conv == null)
			{
				// When a message gets sent from a user we don't have a conversation for, the message gets
				// broadcasted first then the conversation gets created. It's okay that we don't add it now, because
				// when the conversation is broadcasted it will contain the messages
				return;
			}

			/*
			 * notification must be done on the thread that created the view (the UI thread in our case) We don't want to sort the list on the UI thread so instead, disable
			 * notification and manually notify on the UI thread We have to ensure it's disabled because calling notifyDataSetChanged will re-enable notifyOnChange
			 */
			mAdapter.setNotifyOnChange(false);

			if (!mConversationsAdded.contains(conv.getMsisdn()))
			{
				mConversationsAdded.add(conv.getMsisdn());
				mAdapter.add(conv);
			}

			conv.addMessage(message);
			mAdapter.sort(mConversationsComparator);

			runOnUiThread(this);
		}
		else if (HikePubSub.NEW_CONVERSATION.equals(type))
		{
			final Conversation conversation = (Conversation) object;
			mConversationsByMSISDN.put(conversation.getMsisdn(), conversation);
			if (conversation.getMessages().isEmpty())
			{
				return;
			}

			mConversationsAdded.add(conversation.getMsisdn());
			mAdapter.add(conversation);

			runOnUiThread(this);
		}
		else if(HikePubSub.MSG_READ.equals(type))
		{
			String msisdn = (String)object;
			Conversation conv = mConversationsByMSISDN.get(msisdn);
			ConvMessage msg = conv.getMessages().get(conv.getMessages().size()-1);
			msg.setState(ConvMessage.State.RECEIVED_READ);
			Log.d("MESSAGE LIST","Msg is : "+msg.getMessage() + "	State : "+msg.getState().name());
			conv.getMessages().set(conv.getMessages().size()-1, msg);
			Log.d("MESSAGE LIST","Msg event received");
			mAdapter.setNotifyOnChange(false);
			runOnUiThread(this);
		}
	}

	public void run()
	{
		mAdapter.notifyDataSetChanged();
		// notifyDataSetChanged sets notifyonChange to true but we want it to always be false
		mAdapter.setNotifyOnChange(false);
	}

	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		switch (which)
		{
		case DialogInterface.BUTTON_POSITIVE:
			Conversation[] convs = new Conversation[mAdapter.getCount()];
			for (int i = 0; i < convs.length; i++)
			{
				convs[i] = mAdapter.getItem(i);
			}
			DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask();
			task.execute(convs);
			break;
		default:
		}
	}
}
