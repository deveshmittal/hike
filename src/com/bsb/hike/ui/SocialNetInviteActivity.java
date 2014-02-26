package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import twitter4j.IDs;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.SocialNetInviteAdapter;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.HikeHttpCallback;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.models.SocialNetFriendInfo;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;
import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.FacebookRequestError;
import com.facebook.Request;
import com.facebook.Request.GraphUserListCallback;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphUser;
import com.facebook.widget.WebDialog;
import com.facebook.widget.WebDialog.OnCompleteListener;

public class SocialNetInviteActivity extends HikeAppStateBaseFragmentActivity implements OnItemClickListener, FinishableEvent
{
	private ListView listView;

	private ArrayList<Pair<AtomicBoolean, SocialNetFriendInfo>> list;

	private SocialNetInviteAdapter adapter;

	private List<GraphUser> friends;

	private EditText input;

	private Set<String> selectedFriends;

	private boolean isFacebook;

	private Twitter twitter;

	private SharedPreferences settings;

	private HikeHTTPTask mTwitterInviteTask;

	private Dialog mDialog;

	private Menu mMenu;

	private int MAX_INVITE_LIMIT = 10;

	private ViewGroup doneContainer;

	private TextView doneText;

	private TextView title;

	private ImageView backIcon;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hikelistactivity);

		settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
		if (savedInstanceState == null)
		{
			isFacebook = getIntent().getExtras().getBoolean(HikeConstants.Extras.IS_FACEBOOK);
		}
		else
		{
			isFacebook = savedInstanceState.getBoolean(HikeConstants.Extras.IS_FACEBOOK);
		}
		selectedFriends = new HashSet<String>();

		listView = (ListView) findViewById(R.id.contact_list);
		listView.setTextFilterEnabled(true);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setOnItemClickListener(this);

		list = new ArrayList<Pair<AtomicBoolean, SocialNetFriendInfo>>();

		input = (EditText) findViewById(R.id.input_number);

		findViewById(R.id.input_number_container).setVisibility(View.GONE);
		findViewById(R.id.contact_list).setVisibility(View.GONE);
		findViewById(R.id.progress_container).setVisibility(View.VISIBLE);
		if (isFacebook)
		{
			getFriends();
		}
		else
		{
			Utils.executeStringResultTask(new GetTwitterFollowers());
		}
		mTwitterInviteTask = (HikeHTTPTask) getLastCustomNonConfigurationInstance();
		if (mTwitterInviteTask != null)
		{
			mDialog = ProgressDialog.show(this, null, getString(R.string.posting_update_twitter));
		}
		setupActionBar();
	}

	private void init()
	{
		selectedFriends.clear();
		doneContainer.setVisibility(View.GONE);
		backIcon.setImageResource(R.drawable.ic_back);
		getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_header));
		setLabel();
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		backIcon = (ImageView) actionBarView.findViewById(R.id.abs__up);

		doneContainer = (ViewGroup) actionBarView.findViewById(R.id.done_container);
		doneText = (TextView) actionBarView.findViewById(R.id.done_text);

		title = (TextView) actionBarView.findViewById(R.id.title);

		backContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(SocialNetInviteActivity.this, TellAFriend.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);

			}
		});

		doneContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				sendInvite();
			}
		});

		actionBar.setCustomView(actionBarView);

		init();
	}

	private void setLabel()
	{
		if (isFacebook)
		{
			title.setText(R.string.invite_via_facebook);
		}
		else
		{
			title.setText(R.string.invite_via_twitter);
		}
	}

	protected void onSaveInstanceState(Bundle outState)
	{

		outState.putBoolean(HikeConstants.Extras.IS_FACEBOOK, isFacebook);

		super.onSaveInstanceState(outState);
	}

	/* store the task so we can keep keep the progress dialog going */
	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		Log.d("SocialNetInviteActivity", "onRetainNonConfigurationinstance");
		return mTwitterInviteTask;
	}

	private void getFriends()
	{
		Session activeSession = Session.getActiveSession();
		Log.d("INFO", activeSession.getPermissions().toString());
		if (activeSession != null && activeSession.getState().isOpened())
		{
			Log.d("SocialNetInviteActivity", "active session is opened quering for friends");
			Request friendRequest = Request.newMyFriendsRequest(activeSession, new GraphUserListCallback()
			{
				@Override
				public void onCompleted(List<GraphUser> users, Response response)
				{
					try
					{
						FacebookRequestError error = response.getError();
						if (error != null)
						{
							Log.i("friend Request Response", "session is invalid");
							Log.i("friend Request Response", "Do not have permissions");
						}
						else
						{
							Log.d("SocialNetInviteActivity", "got the friends object from facebook calling getFriends");
							Log.d("SocialNetInviteActivity", response.toString());
							friends = users;
							Utils.executeStringResultTask(new GetFriends());
						}
					}
					catch (NullPointerException e)
					{
						Log.e(this.getClass().getName(), "Unable to Connect to Internet", e);
						Toast toast = Toast.makeText(SocialNetInviteActivity.this, getString(R.string.social_invite_network_error), Toast.LENGTH_LONG);
						toast.show();
						finish();
					}
				}
			});
			Bundle bundleParams = new Bundle();
			bundleParams.putString("fields", "id,name,picture");
			friendRequest.setParameters(bundleParams);
			friendRequest.executeAsync();
		}
	}

	private class GetTwitterFollowers extends AsyncTask<Void, Void, String>
	{
		@Override
		protected String doInBackground(Void... params)
		{

			String str = "test";
			try
			{
				twitter = HikeMessengerApp.getTwitterInstance(settings.getString(HikeMessengerApp.TWITTER_TOKEN, "nullToken"),
						settings.getString(HikeMessengerApp.TWITTER_TOKEN_SECRET, "nullTokenSecret"));
				if (twitter != null)
				{
					long cursor = -1;
					IDs ids;
					System.out.println("Listing followers's ids.");
					do
					{
						ids = twitter.getFollowersIDs(twitter.getId(), cursor);
						for (long id : ids.getIDs())
						{
							System.out.println(id);
							User user = twitter.showUser(id);
							SocialNetFriendInfo socialFriend = new SocialNetFriendInfo();
							socialFriend.setId(user.getScreenName());
							socialFriend.setName(user.getName());
							socialFriend.setImageUrl(user.getMiniProfileImageURL());
							System.out.println(user.getName());
							list.add(new Pair<AtomicBoolean, SocialNetFriendInfo>(new AtomicBoolean(false), socialFriend));
						}
					}
					while ((cursor = ids.getNextCursor()) != 0);
				}
			}
			catch (TwitterException e)
			{
				Log.w(getClass().getSimpleName(), e);
			}
			return str;

		}

		// process data retrieved from doInBackground
		protected void onPostExecute(String result)
		{
			adapter = new SocialNetInviteAdapter(SocialNetInviteActivity.this, -1, list);
			input.addTextChangedListener(adapter);
			listView.setAdapter(adapter);
			listView.setOnScrollListener(scrollListener);
			findViewById(R.id.input_number_container).setVisibility(View.VISIBLE);
			findViewById(R.id.contact_list).setVisibility(View.VISIBLE);
			findViewById(R.id.progress_container).setVisibility(View.GONE);

		}
	}

	private class GetFriends extends AsyncTask<Void, Void, String>
	{
		@Override
		protected String doInBackground(Void... params)
		{
			String str = "test";
			try
			{
				for (int i = 0; i < friends.size(); i++)
				{
					SocialNetFriendInfo socialFriend = new SocialNetFriendInfo();
					socialFriend.setId(friends.get(i).getId());
					socialFriend.setName(friends.get(i).getName());
					socialFriend.setImageUrl(friends.get(i).getInnerJSONObject().getJSONObject("picture").getJSONObject("data").getString("url"));

					list.add(new Pair<AtomicBoolean, SocialNetFriendInfo>(new AtomicBoolean(false), socialFriend));
				}
				Collections.sort(list, new Comparator<Pair<AtomicBoolean, SocialNetFriendInfo>>()
				{
					@Override
					public int compare(Pair<AtomicBoolean, SocialNetFriendInfo> lhs, Pair<AtomicBoolean, SocialNetFriendInfo> rhs)
					{
						return lhs.second.compareTo(rhs.second);
					}
				});

			}
			catch (JSONException e)
			{
				Log.w(getClass().getSimpleName(), "Invalid JSON");
			}
			return str;
		}

		// process data retrieved from doInBackground
		protected void onPostExecute(String result)
		{

			adapter = new SocialNetInviteAdapter(SocialNetInviteActivity.this, -1, list);
			input.addTextChangedListener(adapter);

			listView.setAdapter(adapter);
			listView.setOnScrollListener(scrollListener);

			adapter.notifyDataSetChanged();
			findViewById(R.id.input_number_container).setVisibility(View.VISIBLE);
			findViewById(R.id.contact_list).setVisibility(View.VISIBLE);
			findViewById(R.id.progress_container).setVisibility(View.GONE);

		}
	}

	OnScrollListener scrollListener = new OnScrollListener()
	{
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState)
		{
			adapter.setIsListFlinging(scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING);
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
		{
		}
	};

	public void sendInvite()
	{
		String selectedFriendsIds = "";
		selectedFriendsIds = TextUtils.join(",", selectedFriends);
		Log.d("selectedFriendsIds", selectedFriendsIds);

		if (isFacebook && !selectedFriendsIds.equals(""))
			sendRequestDialog(selectedFriendsIds);
		else
		{
			JSONArray inviteesArray = new JSONArray();

			for (String id : selectedFriends)
			{
				inviteesArray.put(id);
			}
			try
			{
				sendTwitterInvite(new JSONObject().put("invitees", inviteesArray));
			}
			catch (JSONException e)
			{
				Log.e("SocialNetInviteActivity", "Creating a JSONObject payload for http Twitter Invite request", e);
			}
		}
	}

	public void sendTwitterInvite(JSONObject data)
	{
		HikeHttpRequest hikeHttpRequest = new HikeHttpRequest("/invite/twitter", RequestType.OTHER, new HikeHttpCallback()
		{

			@Override
			public void onSuccess(JSONObject response)
			{
				Toast.makeText(SocialNetInviteActivity.this, getString(R.string.posted_update), Toast.LENGTH_SHORT).show();
				finish();
			}

			@Override
			public void onFailure()
			{
				Toast.makeText(SocialNetInviteActivity.this, R.string.posting_update_fail, Toast.LENGTH_SHORT).show();
			}

		});
		hikeHttpRequest.setJSONData(data);
		mTwitterInviteTask = new HikeHTTPTask(this, R.string.posting_update_fail);

		Utils.executeHttpTask(mTwitterInviteTask, hikeHttpRequest);
		mDialog = ProgressDialog.show(this, null, getString(R.string.posting_update_twitter));

		return;
	}

	private void sendRequestDialog(final String selectedUserIds)
	{
		Session session = Session.getActiveSession();
		if (session != null)
		{

			Bundle params = new Bundle();
			params.putString("to", selectedUserIds);
			params.putString("message", getString(R.string.facebook_msg));

			WebDialog requestsDialog = (new WebDialog.RequestsDialogBuilder(SocialNetInviteActivity.this, Session.getActiveSession(), params)).setOnCompleteListener(
					new OnCompleteListener()
					{

						@Override
						public void onComplete(Bundle values, FacebookException error)
						{
							if (error != null)
							{
								if (error instanceof FacebookOperationCanceledException)
								{
									Toast.makeText(SocialNetInviteActivity.this, getString(R.string.fb_invite_failed), Toast.LENGTH_SHORT).show();
								}
								else
								{
									Toast.makeText(SocialNetInviteActivity.this, getString(R.string.social_invite_network_error), Toast.LENGTH_SHORT).show();
									selectedFriends.clear();
									finish();
								}

							}
							else
							{
								final String requestId = values.getString("request");
								if (requestId != null)
								{
									Toast.makeText(SocialNetInviteActivity.this, getString(R.string.fb_invite_success), Toast.LENGTH_SHORT).show();

									try
									{
										JSONObject data = new JSONObject();
										data.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.FACEBOOK);

										JSONArray nativeinvites = new JSONArray(selectedFriends);
										JSONObject d = new JSONObject();
										d.put(HikeConstants.NATIVE_INVITES, nativeinvites);
										data.put(HikeConstants.DATA, d);
										data.put(HikeConstants.TIMESTAMP, System.currentTimeMillis() / 1000);
										HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, data);
										Log.d("SocialNetInviteActivity", "fb packet" + data.toString());

										// sendFacebookInviteIds(data);
									}
									catch (JSONException e)
									{
										// TODO Auto-generated catch block
										Log.e("SocialNetInviteActivity", "while preparing JSON For Fb", e);
									}

									String alreadyInvited = settings.getString(HikeMessengerApp.INVITED_FACEBOOK_FRIENDS_IDS, "");
									String[] alreadyInvitedArray = alreadyInvited.split(",");
									for (int i = 0; i < alreadyInvitedArray.length; i++)
										selectedFriends.add(alreadyInvitedArray[i]);
									settings.edit().putString(HikeMessengerApp.INVITED_FACEBOOK_FRIENDS_IDS, TextUtils.join(",", selectedFriends)).commit();

									Log.d("invited ids", settings.getString(HikeMessengerApp.INVITED_FACEBOOK_FRIENDS_IDS, ""));

									selectedFriends.clear();
									finish();
								}
								else
								{
									Toast.makeText(SocialNetInviteActivity.this, getString(R.string.fb_invite_failed), Toast.LENGTH_SHORT).show();
								}
							}

						}

					}).build();

			requestsDialog.show();
		}
	}

	@SuppressWarnings("unchecked")
	public void onItemClick(AdapterView<?> parent, final View view, int position, long id)
	{
		Pair<AtomicBoolean, SocialNetFriendInfo> socialFriend = (Pair<AtomicBoolean, SocialNetFriendInfo>) parent.getItemAtPosition(position);
		CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkbox);
		if (selectedFriends.contains(socialFriend.second.getId()))
		{
			selectedFriends.remove(socialFriend.second.getId());
		}
		else
		{
			if (selectedFriends.size() != MAX_INVITE_LIMIT)
			{
				selectedFriends.add(socialFriend.second.getId());
			}
			else
			{
				Toast.makeText(SocialNetInviteActivity.this, getString(R.string.limited_requests), Toast.LENGTH_LONG).show();
				return;
			}
		}

		if (!selectedFriends.isEmpty())
		{
			doneContainer.setVisibility(View.VISIBLE);
			doneText.setText(Integer.toString(selectedFriends.size()));
			getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_header_compose));

			// title.setText(R.string.invite);
			// backIcon.setImageResource(R.drawable.ic_cancel);
		}
		else
		{
			init();
		}

		socialFriend.first.set(!socialFriend.first.get());
		checkbox.setChecked(socialFriend.first.get());
		/*
		 * mMenu.findItem(R.id.sendInvite).setTitle( getString(R.string.send_invite, selectedFriends.size())); if (selectedFriends.size() > 0)
		 * mMenu.findItem(R.id.sendInvite).setEnabled(true); else { mMenu.findItem(R.id.sendInvite).setEnabled(false); }
		 */
	}

	@Override
	protected void onDestroy()
	{
		if (mDialog != null)
		{
			mDialog.dismiss();
			mDialog = null;
		}
		mTwitterInviteTask = null;
		super.onDestroy();
	}

	@Override
	public void onFinish(boolean success)
	{
		if (mDialog != null)
		{
			mDialog.dismiss();
			mDialog = null;
		}
		mTwitterInviteTask = null;

	}

}
