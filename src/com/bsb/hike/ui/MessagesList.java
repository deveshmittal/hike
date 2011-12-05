package com.bsb.hike.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.ContactUtils;

public class MessagesList extends Activity implements OnClickListener {

	public class InviteFriendAsyncTask extends AsyncTask<Uri, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Uri... params) {
			Uri uri = params[0];
			String number = ContactUtils.getMobileNumber(MessagesList.this.getContentResolver(), uri);
			Log.d("MessagesList", "found number: " + number);
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
	private static final int CONTACT_PICKER_RESULT = 1002;

	private boolean mConversationsViewHidden;
	private View mNewMessageView;
	private View mConversationsView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);

		SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
	    if (token == null) {
	    	startActivity(new Intent(this, WelcomeActivity.class));
	    	finish();
	    }

	    AccountUtils.setToken(token);
    	mNewMessageView = findViewById(R.id.new_user_message);
    	mConversationsView = findViewById(R.id.conversations);

    	View searchIconView = findViewById(R.id.search);
    	searchIconView.setOnClickListener(this);
    	View editMessageIconView = findViewById(R.id.edit_message);
    	editMessageIconView.setOnClickListener(this);
	    Intent intent = getIntent();
	    if (intent.getBooleanExtra("first", false)) {
	    	mNewMessageView.setVisibility(View.VISIBLE);
	    	mConversationsView.setVisibility(View.GONE);
	    	mConversationsViewHidden = true;
	    }
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	private void checkForWelcomeMessage() {
		if (mConversationsViewHidden) {
			mConversationsViewHidden = false;
	    	mNewMessageView.setVisibility(View.GONE);
	    	mConversationsView.setVisibility(View.VISIBLE);
		}
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
	public void onClick(View v) {
		Log.d("MessagesList", "OnClick called: "+v);		
	}
}

