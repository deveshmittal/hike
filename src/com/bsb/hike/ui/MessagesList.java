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
import android.os.Handler;
import android.provider.ContactsContract.Contacts;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.ConversationsAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.ContactUtils;
import com.bsb.hike.utils.UserError;
import com.bsb.hike.utils.Utils;

public class MessagesList extends Activity implements OnClickListener, HikePubSub.Listener, android.content.DialogInterface.OnClickListener, Runnable, TextWatcher, OnEditorActionListener
{
	private static final int INVITE_PICKER_RESULT = 1001;

	public static final Object COMPOSE = "compose";

	private ListView mConversationsView;

	private View mSearchIconView;

	private View mEditMessageIconView;

	private ConversationsAdapter mAdapter;

	private RelativeLayout mEmptyView;

	private Comparator<? super Conversation> mConversationsComparator;

	private GestureDetector mSwipeGestureListener;

	private int mAmountToScrollAfterSwipeBack; /* the amount to scroll back to restore your position */

	private ViewAnimator mCurrentComposeView;

	private EditText mCurrentComposeText;

	private Map<String, Conversation> mConversationsByMSISDN;

	private Set<String> mConversationsAdded;

	/* the conversation that's currently being composed */
	private Conversation mCurrentConversation;

	private ComposeViewWatcher mComposeTextWatcher;

	private class SwipeGestureDetector extends SimpleOnGestureListener
	{
		final int swipeMinDistance;

		final int swipeThresholdVelocity;

		public SwipeGestureDetector()
		{
			final ViewConfiguration vc = ViewConfiguration.get(MessagesList.this);
			swipeMinDistance = vc.getScaledTouchSlop();
			swipeThresholdVelocity = vc.getScaledMinimumFlingVelocity();
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e)
		{
			if (mCurrentComposeView != null)
			{
				swipeBack(mCurrentComposeView, true);
				return true;
			}

			int pos = mConversationsView.pointToPosition((int) e.getX(), (int) e.getY());
			if (pos < 0)
			{
				return false;
			}
			else
			{
				selectConversation(pos);
				return true;
			}
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
		{
			if (Math.abs(velocityY) > 500)
			{
				Log.d("MessagesList", "Swipe ignored -- Y motion too fast " + velocityY);
				return false;
			}

			if (Math.abs(velocityX) < swipeThresholdVelocity)
			{
				Log.d("MessagesList", "Swipe ignored -- Too slow " + velocityX);
				/* too slow, ignore */
				return false;
			}

			if (Math.abs(e1.getY() - e2.getY()) > 200)
			{
				/* too much horizontal movement, ignore */
				Log.d("MessagesList", "Swipe ignore -- Too much Y movement " + (Math.abs(e1.getY() - e2.getY())) + " " + 200);
				return false;
			}

			if ((Math.abs(e1.getX() - e2.getX()) < swipeMinDistance))
			{
				/* too short, ignore */
				Log.d("MessagesList", "Swipe ignored -- Too short");
				return false;
			}

			Log.d("MessagesList", "Valid swipe detected");
			boolean swipeRight = e2.getX() > e1.getX();
			int pos = mConversationsView.pointToPosition((int) e1.getX(), (int) e1.getY());
			onSwipeDetected(pos, swipeRight);
			return true;
		}
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		Log.d("MESSAGE LIST", "Currently in pause state. .......");
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, null);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		Log.d("MESSAGE LIST", "Resumed .....");
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
				db.deleteConversation(ids.toArray(new Long[] {}));
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
			catch (UserError err)
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

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
		if (token == null)
		{
			/* This is to check if phone validation screen is reached by the user or not. */
			boolean phoneValidation = settings.getBoolean(HikeMessengerApp.PHONE_NUMBER_VALIDATION, false);
			if (phoneValidation)
			{
				startActivity(new Intent(this, SmsFallback.class));
				finish();
				return;
			}
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
		mSwipeGestureListener = new GestureDetector(new SwipeGestureDetector());
		View.OnTouchListener gestureListener = new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				if (mSwipeGestureListener.onTouchEvent(event))
				{
					/* handled by swipe */
					return true;
				}

				return mCurrentConversation != null;
			}
		};

		mConversationsView.setOnTouchListener(gestureListener);

		/*
		 * mSearchIconView = findViewById(R.id.search); mSearchIconView.setOnClickListener(this);
		 */

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
		HikeMessengerApp.getPubSub().addListener(HikePubSub.SERVER_RECEIVED_MSG, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_DELIVERED_READ, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_DELIVERED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_FAILED, this);

		HikeMessengerApp.getPubSub().addListener(HikePubSub.NEW_CONVERSATION, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_SENT, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MSG_READ, this);

		/* register for long-press's */
		registerForContextMenu(mConversationsView);
	}

	private void swipeBack(ViewAnimator viewAnimator, boolean animate)
	{
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mCurrentComposeText.getWindowToken(), 0);

		mCurrentConversation = null;
		mCurrentComposeText.removeTextChangedListener(mComposeTextWatcher);
		mComposeTextWatcher.uninit();
		mComposeTextWatcher = null;
		mCurrentComposeText = null;
		mCurrentComposeView = null;

		viewAnimator.setOutAnimation(animate ? Utils.outToLeftAnimation(this) : null);
		viewAnimator.setInAnimation(animate ? Utils.inFromRightAnimation(this) : null);

		viewAnimator.setDisplayedChild(0);
		View bottomBar = findViewById(R.id.bottom_nav_bar);
		bottomBar.setVisibility(View.VISIBLE);

		if (animate)
		{
			viewAnimator.getInAnimation().setAnimationListener(new AnimationListener()
			{
				@Override
				public void onAnimationStart(Animation animation)
				{}
				@Override
				public void onAnimationRepeat(Animation animation)
				{}

				@Override
				public void onAnimationEnd(Animation animation)
				{
					mConversationsView.postDelayed(new Runnable() {
						public void run()
						{
							int delta = mAmountToScrollAfterSwipeBack;
							if (delta > 0 )
							{
								int scrollDistance = mConversationsView.getChildAt(0).getHeight() * delta;
								Log.d("MessagesList", "Should be scrolling to position " + delta + " " + scrollDistance);
								mConversationsView.scrollBy(0, -scrollDistance);
							}
							else if (delta < 0)
							{
								/* message was sent, jump to the top */
								mConversationsView.scrollTo(0, 0);
							}
							mAmountToScrollAfterSwipeBack = 0;
						}
					}, 150);
				}
			});
		}
		View overlay = findViewById(R.id.messages_list_overlay);
		overlay.setVisibility(View.GONE);
	}

	@Override
	public void onBackPressed()
	{
		if (mCurrentConversation != null)
		{
			swipeBack(mCurrentComposeView, true);
		}
		else
		{
			super.onBackPressed();
		}
	}

	private void onSwipeDetected(int pos, boolean swipeRight)
	{
		final int positionSelected = pos;

		int firstPosition = mConversationsView.getFirstVisiblePosition() - mConversationsView.getHeaderViewsCount(); // This is the same as child #0
		final int wantedPosition = pos - firstPosition;

		if ((wantedPosition < mConversationsView.getFirstVisiblePosition()) || (wantedPosition > mConversationsView.getLastVisiblePosition()))
		{
			Log.e("MessagesList", "Selected swipe view is outside visible range " + wantedPosition);
			return;
		}

		View wantedView = mConversationsView.getChildAt(wantedPosition);
		ViewAnimator viewAnimator = (ViewAnimator) wantedView.findViewById(R.id.conversation_flip);
		int currentChild = viewAnimator.getDisplayedChild();

		if (swipeRight && (currentChild == 0))
		{
			Log.d("MessagesList", "swipe forward");
			if (mCurrentComposeView != null)
			{
				Log.d("MessagesList", "swiping backing weird view");
				swipeBack(mCurrentComposeView, true);
				return;
			}

			mCurrentConversation = mAdapter.getItem(pos);
			setComposeView(viewAnimator);

			Log.d("MessagesList", "position is " + mAmountToScrollAfterSwipeBack);

			viewAnimator.setOutAnimation(Utils.outToRightAnimation(this));
			Animation inAnimation = Utils.inFromLeftAnimation(this);
			viewAnimator.setInAnimation(inAnimation);
			inAnimation.setAnimationListener(new AnimationListener(){

				@Override
				public void onAnimationEnd(Animation animation)
				{
//					mCurrentComposeText.requestFocus();

					mConversationsView.postDelayed(new Runnable() {
						public void run()
						{
							/* since this is a callback, the user may have unswiped already */
							if (mCurrentComposeView == null)
							{
								return;
							}

							mAmountToScrollAfterSwipeBack = positionSelected - mConversationsView.getFirstVisiblePosition();

							int[] loc = new int[2];
							mCurrentComposeView.getLocationOnScreen(loc);
							int scrollDistance = (int) (loc[1] - mCurrentComposeView.getHeight()*1.2);
							mConversationsView.scrollTo(0, scrollDistance);

							Display display = getWindowManager().getDefaultDisplay();
							View v = findViewById(R.id.messages_list_overlay);
							RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) v.getLayoutParams();
							View parent = (View) mCurrentComposeView.getParent();
							int[] location = new int[2];
							parent.getLocationOnScreen(location);

							lp.height = display.getHeight() - location[1] - parent.getHeight();
							v.setLayoutParams(lp);
							v.setVisibility(View.VISIBLE);

							InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
							imm.showSoftInput(mCurrentComposeText, InputMethodManager.SHOW_IMPLICIT);
							imm.showSoftInputFromInputMethod(mCurrentComposeText.getWindowToken(), InputMethodManager.SHOW_IMPLICIT);
						}
					}, (int) 150);
				}

				@Override
				public void onAnimationRepeat(Animation arg0)
				{
				}
				@Override
				public void onAnimationStart(Animation arg0)
				{
				}
			});

			viewAnimator.setDisplayedChild(1);
		}
		else if (currentChild == 1)
		{
			Log.d("MessagesList", "swipe back");
			swipeBack(viewAnimator, true);
		}
	}

	public void setComposeView(ViewAnimator viewAnimator)
	{
		viewAnimator.setTag(MessagesList.COMPOSE);
		mCurrentComposeText = (EditText) viewAnimator.findViewById(R.id.mini_compose);
		mCurrentComposeText.requestFocus();

		mCurrentComposeText.setOnEditorActionListener(this);
		mCurrentComposeView = viewAnimator;

		Button button = (Button) viewAnimator.findViewById(R.id.send_message);
		if (mComposeTextWatcher != null)
		{
			mComposeTextWatcher.uninit();
		}

		mComposeTextWatcher = new ComposeViewWatcher(mCurrentConversation, mCurrentComposeText, button,
									getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getInt(HikeMessengerApp.SMS_SETTING, 0));
		mComposeTextWatcher.init();
		mCurrentComposeText.addTextChangedListener(mComposeTextWatcher);
	}

	private void selectConversation(int position)
	{
		int firstPosition = mConversationsView.getFirstVisiblePosition() - mConversationsView.getHeaderViewsCount(); // This is the same as child #0
		int wantedPosition = position - firstPosition;

		if (mCurrentComposeView != null)
		{
			if (mConversationsView.getChildAt(wantedPosition) == mCurrentComposeView.getParent())
			{
				/* ignore the select since this conversation is currently swiped */
				return;
			}

			swipeBack(mCurrentComposeView, false);
		}

		Conversation conversation = (Conversation) mAdapter.getItem(position);
		Intent intent = createIntentForConversation(conversation);
		startActivity(intent);
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
			intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, conv.getLabel());
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
			return true;
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
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.SERVER_RECEIVED_MSG, this);
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.MESSAGE_DELIVERED_READ, this);
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.MESSAGE_DELIVERED, this);
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.MESSAGE_FAILED, this);
		if (mComposeTextWatcher != null)
		{
			mComposeTextWatcher.uninit();
		}
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
		if ((HikePubSub.MESSAGE_RECEIVED.equals(type)) || (HikePubSub.MESSAGE_SENT.equals(type)))
		{
			Log.d("MESSAGE LIST", "New msg event sent or received.");
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
			Log.d("MessagesList", "new message is " + message);
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
		else if (HikePubSub.MSG_READ.equals(type))
		{
			String msisdn = (String) object;
			Conversation conv = mConversationsByMSISDN.get(msisdn);
			ConvMessage msg = conv.getMessages().get(conv.getMessages().size() - 1);
			msg.setState(ConvMessage.State.RECEIVED_READ);
			conv.getMessages().set(conv.getMessages().size() - 1, msg);
			mAdapter.setNotifyOnChange(false);
			runOnUiThread(this);
		}
		else if (HikePubSub.SERVER_RECEIVED_MSG.equals(type))
		{
			long msgId = ((Long) object).longValue();
			ConvMessage msg = findMessageById(msgId);
			if (msg != null)
			{
				msg.setState(ConvMessage.State.SENT_CONFIRMED);
				runOnUiThread(this);
			}
		}
		else if (HikePubSub.MESSAGE_DELIVERED_READ.equals(type))
		{
			long[] ids = (long[]) object;
			// TODO we could keep a map of msgId -> conversation objects somewhere to make this faster
			for (int i = 0; i < ids.length; i++)
			{
				ConvMessage msg = findMessageById(ids[i]);
				if (msg != null)
				{
					msg.setState(ConvMessage.State.SENT_DELIVERED_READ);
				}
			}
			runOnUiThread(this);
		}
		else if (HikePubSub.MESSAGE_DELIVERED.equals(type))
		{
			long msgId = ((Long) object).longValue();
			ConvMessage msg = findMessageById(msgId);
			if (msg != null)
			{
				msg.setState(ConvMessage.State.SENT_DELIVERED);
				runOnUiThread(this);
			}
		}
		else if (HikePubSub.MESSAGE_FAILED.equals(type))
		{
			long msgId = ((Long) object).longValue();
			ConvMessage msg = findMessageById(msgId);
			if (msg != null)
			{
				msg.setState(ConvMessage.State.SENT_FAILED);
				runOnUiThread(this);
			}
		}
	}

	ConvMessage findMessageById(long msgId)
	{
		int count = mAdapter.getCount();
		for (int i = 0; i < count; ++i)
		{
			Conversation conversation = mAdapter.getItem(i);
			if (conversation == null)
			{
				continue;
			}
			List<ConvMessage> messages = conversation.getMessages();
			if (messages.isEmpty())
			{
				continue;
			}

			ConvMessage message = messages.get(messages.size() - 1);
			if (message.getMsgID() == msgId)
			{
				return message;
			}
		}

		return null;
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

	@Override
	public void afterTextChanged(Editable editable)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
	{
	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
	{
	}

	/* called when the send button in the swipe method is clicked */
	public void onSendClick(View unused)
	{
		String message = mCurrentComposeText.getText().toString();
		mCurrentComposeText.setText("");
		long time = (long) System.currentTimeMillis() / 1000;
		final ConvMessage convMessage = new ConvMessage(message, mCurrentConversation.getMsisdn(), time, ConvMessage.State.SENT_UNCONFIRMED);
		convMessage.setConversation(mCurrentConversation);
		Log.d("MessagesList", "Current Conversation is " + mCurrentConversation);
		mAmountToScrollAfterSwipeBack = - 1; /* signifies that we should jump to the top, not the last position */
		(new Handler()).postDelayed(new Runnable() {
			public void run()
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, convMessage);
			}
		}, 750);
		swipeBack(mCurrentComposeView, true);
	}

	@Override
	public boolean onEditorAction(TextView view, int actionId, KeyEvent keyEvent)
	{
		if ((view == mCurrentComposeText) &&
				(actionId == EditorInfo.IME_ACTION_SEND))
			{
				onSendClick(null);
				return true;
			}
			return false;
		}

	public Conversation getSelectedConversation()
	{
		return mCurrentConversation;
	}
}
