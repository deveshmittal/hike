package com.bsb.hike.ui;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Intents.Insert;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.adapters.ProfileAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.HikeHttpCallback;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.ImageViewerInfo;
import com.bsb.hike.models.ProfileItem;
import com.bsb.hike.models.ProfileItem.ProfileStatusItem;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.tasks.DownloadImageTask;
import com.bsb.hike.tasks.DownloadImageTask.ImageDownloadResult;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;

public class ProfileActivity extends HikeAppStateBaseFragmentActivity implements
		FinishableEvent, android.content.DialogInterface.OnClickListener,
		Listener, OnLongClickListener, OnItemLongClickListener,
		OnScrollListener, View.OnClickListener {

	private EditText mNameEdit;

	private View currentSelection;

	private Dialog mDialog;
	private String mLocalMSISDN = null;

	private ActivityState mActivityState; /* config state of this activity */
	private String nameTxt;
	private boolean isBackPressed = false;
	private EditText mEmailEdit;
	private String emailTxt;
	private Map<String, GroupParticipant> participantMap;

	private ProfileType profileType;
	private String httpRequestURL;
	private String groupOwner;

	private int lastSavedGender;

	private SharedPreferences preferences;

	private String[] groupInfoPubSubListeners = { HikePubSub.ICON_CHANGED,
			HikePubSub.GROUP_NAME_CHANGED, HikePubSub.GROUP_END,
			HikePubSub.PARTICIPANT_JOINED_GROUP,
			HikePubSub.PARTICIPANT_LEFT_GROUP, HikePubSub.USER_JOINED,
			HikePubSub.USER_LEFT };

	private String[] contactInfoPubSubListeners = { HikePubSub.ICON_CHANGED,
			HikePubSub.CONTACT_ADDED, HikePubSub.USER_JOINED,
			HikePubSub.USER_LEFT, HikePubSub.STATUS_MESSAGE_RECEIVED,
			HikePubSub.FAVORITE_TOGGLED, HikePubSub.FRIEND_REQUEST_ACCEPTED,
			HikePubSub.REJECT_FRIEND_REQUEST,
			HikePubSub.HIKE_JOIN_TIME_OBTAINED,
			HikePubSub.LAST_SEEN_TIME_UPDATED };

	private String[] profilePubSubListeners = {
			HikePubSub.STATUS_MESSAGE_RECEIVED,
			HikePubSub.USER_JOIN_TIME_OBTAINED };

	private GroupConversation groupConversation;
	private ImageButton topBarBtn;
	private ContactInfo contactInfo;
	private boolean isBlocked;
	private Dialog groupEditDialog;
	private boolean showingRequestItem = false;

	private static enum ProfileType {
		USER_PROFILE, // The user profile screen
		USER_PROFILE_EDIT, // The user profile edit screen
		GROUP_INFO, // The group info screen
		CONTACT_INFO // Contact info screen
	};

	private class ActivityState {
		public HikeHTTPTask task; /* the task to update the global profile */
		public DownloadImageTask downloadPicasaImageTask; /*
														 * the task to download
														 * the picasa image
														 */
		public HikeHTTPTask getHikeJoinTimeTask;

		public String destFilePath = null; /*
											 * the bitmap before the user saves
											 * it
											 */
		public int genderType;
		public boolean groupEditDialogShowing = false;
		public String edittedGroupName = null;
	}

	public File selectedFileIcon;
	private ListView profileContent;
	private ProfileAdapter profileAdapter;

	private List<ProfileItem> profileItems;

	private boolean isGroupOwner;

	/* store the task so we can keep keep the progress dialog going */
	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		Log.d("ProfileActivity", "onRetainNonConfigurationinstance");
		return mActivityState;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mDialog != null) {
			mDialog.dismiss();
			mDialog = null;
		}
		if (groupEditDialog != null) {
			if (mNameEdit != null) {
				mActivityState.edittedGroupName = mNameEdit.getText()
						.toString();
			}
			groupEditDialog.dismiss();
			groupEditDialog = null;
		}
		if ((mActivityState != null) && (mActivityState.task != null)) {
			mActivityState.task.setActivity(null);
		}
		if (profileType == ProfileType.GROUP_INFO) {
			HikeMessengerApp.getPubSub().removeListeners(this,
					groupInfoPubSubListeners);
		} else if (profileType == ProfileType.CONTACT_INFO) {
			HikeMessengerApp.getPubSub().removeListeners(this,
					contactInfoPubSubListeners);
		} else if (profileType == ProfileType.USER_PROFILE) {
			HikeMessengerApp.getPubSub().removeListeners(this,
					profilePubSubListeners);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (Utils.requireAuth(this)) {
			return;
		}

		preferences = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
				MODE_PRIVATE);

		Object o = getLastCustomNonConfigurationInstance();
		if (o instanceof ActivityState) {
			mActivityState = (ActivityState) o;
			if (mActivityState.task != null) {
				/* we're currently executing a task, so show the progress dialog */
				mActivityState.task.setActivity(this);
				mDialog = ProgressDialog.show(this, null, getResources()
						.getString(R.string.updating_profile));
			}
		} else {
			mActivityState = new ActivityState();
		}

		setContentView(R.layout.profile);

		if (getIntent().hasExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT)) {
			this.profileType = ProfileType.GROUP_INFO;
			HikeMessengerApp.getPubSub().addListeners(this,
					groupInfoPubSubListeners);
			setupGroupProfileScreen();
		} else if (getIntent().hasExtra(HikeConstants.Extras.CONTACT_INFO)) {
			this.profileType = ProfileType.CONTACT_INFO;
			HikeMessengerApp.getPubSub().addListeners(this,
					contactInfoPubSubListeners);
			setupContactProfileScreen();
		} else {
			httpRequestURL = "/account";
			fetchPersistentData();

			if (getIntent().getBooleanExtra(HikeConstants.Extras.EDIT_PROFILE,
					false)) {
				this.profileType = ProfileType.USER_PROFILE_EDIT;
				setupEditScreen();
			} else {
				this.profileType = ProfileType.USER_PROFILE;
				HikeMessengerApp.getPubSub().addListeners(this,
						profilePubSubListeners);
				setupProfileScreen(savedInstanceState);
			}
		}
		if (mActivityState.groupEditDialogShowing) {
			onEditGroupNameClick(null);
		}
		setupActionBar();
	}

	private void setupActionBar() {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(
				R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		switch (profileType) {
		case CONTACT_INFO:
			title.setText(R.string.profile_title);
			break;
		case USER_PROFILE:
			title.setText(R.string.me);
			break;
		case GROUP_INFO:
			title.setText(R.string.group);
			break;
		}

		backContainer.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		actionBar.setCustomView(actionBarView);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (profileAdapter != null) {
			profileAdapter.restartImageLoaderThread();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (profileAdapter != null) {
			profileAdapter.stopImageLoaderThread();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return super.onCreateOptionsMenu(menu);
	}

	private void setupContactProfileScreen() {
		boolean canCall = getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_TELEPHONY);

		this.mLocalMSISDN = getIntent().getStringExtra(
				HikeConstants.Extras.CONTACT_INFO);

		contactInfo = HikeUserDatabase.getInstance().getContactInfoFromMSISDN(
				mLocalMSISDN, false);

		if (!contactInfo.isOnhike()) {
			contactInfo.setOnhike(getIntent().getBooleanExtra(
					HikeConstants.Extras.ON_HIKE, false));
		}

		profileItems = new ArrayList<ProfileItem>();
		setupContactProfileList();

		profileAdapter = new ProfileAdapter(this, profileItems, null,
				contactInfo, false, HikeUserDatabase.getInstance().isBlocked(
						mLocalMSISDN));
		profileContent = (ListView) findViewById(R.id.profile_content);
		profileContent.setAdapter(profileAdapter);
		profileContent.setOnScrollListener(this);

		/*
		 * if the hike join time for a known hike contact is 0, we request the
		 * server for the hike join time.
		 */
		if (contactInfo.isOnhike() && contactInfo.getHikeJoinTime() == 0) {
			HikeHttpRequest hikeHttpRequest = new HikeHttpRequest(
					"/account/profile/" + mLocalMSISDN,
					RequestType.HIKE_JOIN_TIME, new HikeHttpCallback() {
						@Override
						public void onSuccess(JSONObject response) {
							Log.d(getClass().getSimpleName(), "Response: "
									+ response.toString());
							try {
								JSONObject profile = response
										.getJSONObject(HikeConstants.PROFILE);
								long hikeJoinTime = profile.optLong(
										HikeConstants.JOIN_TIME, 0);
								hikeJoinTime = Utils.applyServerTimeOffset(
										ProfileActivity.this, hikeJoinTime);

								HikeMessengerApp.getPubSub().publish(
										HikePubSub.HIKE_JOIN_TIME_OBTAINED,
										new Pair<String, Long>(mLocalMSISDN,
												hikeJoinTime));
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
					});
			mActivityState.getHikeJoinTimeTask = new HikeHTTPTask(null, -1);
			mActivityState.getHikeJoinTimeTask.execute(hikeHttpRequest);
		}
	}

	private void setupContactProfileList() {
		profileItems.clear();
		// Adding an item for the header
		profileItems.add(new ProfileItem.ProfileStatusItem(
				ProfileItem.HEADER_ID));

		showingRequestItem = false;
		if (!contactInfo.isOnhike()
				|| contactInfo.getFavoriteType() != FavoriteType.FRIEND) {
			profileItems.add(new ProfileItem.ProfileStatusItem(
					ProfileItem.REQUEST_ID));
			showingRequestItem = true;
		}

		if (showContactsUpdates(contactInfo)) {

			addStatusMessageAsProfileItems(HikeConversationsDatabase
					.getInstance().getStatusMessages(false,
							HikeConstants.MAX_STATUSES_TO_LOAD_INITIALLY, -1,
							mLocalMSISDN));

			if (contactInfo.isOnhike() && contactInfo.getHikeJoinTime() > 0) {
				profileItems.add(getJoinedHikeStatus(contactInfo));
			}
		}
	}

	private void addStatusMessageAsProfileItems(
			List<StatusMessage> statusMessages) {
		for (StatusMessage statusMessage : statusMessages) {
			profileItems.add(new ProfileItem.ProfileStatusItem(statusMessage));
		}
	}

	private boolean showContactsUpdates(ContactInfo contactInfo) {
		return (contactInfo.getFavoriteType() != FavoriteType.NOT_FRIEND)
				&& (contactInfo.getFavoriteType() != FavoriteType.REQUEST_SENT)
				&& (contactInfo.getFavoriteType() != FavoriteType.REQUEST_SENT_REJECTED)
				&& (contactInfo.isOnhike());
	}

	private void setupGroupProfileScreen() {
		this.mLocalMSISDN = getIntent().getStringExtra(
				HikeConstants.Extras.EXISTING_GROUP_CHAT);

		HikeConversationsDatabase hCDB = HikeConversationsDatabase
				.getInstance();
		groupConversation = (GroupConversation) hCDB.getConversation(
				mLocalMSISDN, 0);

		participantMap = groupConversation.getGroupParticipantList();
		List<String> inactiveMsisdns = new ArrayList<String>();
		/*
		 * Removing inactive participants
		 */
		for (Entry<String, GroupParticipant> participantEntry : participantMap
				.entrySet()) {
			GroupParticipant groupParticipant = participantEntry.getValue();
			if (groupParticipant.hasLeft()) {
				inactiveMsisdns.add(participantEntry.getKey());
			}
		}
		for (String msisdn : inactiveMsisdns) {
			participantMap.remove(msisdn);
		}

		httpRequestURL = "/group/" + groupConversation.getMsisdn();

		profileItems = new ArrayList<ProfileItem>();
		setupGroupProfileList();

		profileAdapter = new ProfileAdapter(this, profileItems,
				groupConversation, null, false);

		profileContent = (ListView) findViewById(R.id.profile_content);
		profileContent.setAdapter(profileAdapter);
		profileContent.setDivider(null);

		nameTxt = groupConversation.getLabel();
	}

	private void setupGroupProfileList() {
		GroupParticipant userInfo = new GroupParticipant(
				Utils.getUserContactInfo(preferences, true));

		profileItems.clear();
		// Adding an item for the header
		profileItems
				.add(new ProfileItem.ProfileGroupItem(ProfileItem.HEADER_ID));

		List<GroupParticipant> participants = new ArrayList<GroupParticipant>(
				participantMap.values());

		if (!participantMap.containsKey(userInfo.getContactInfo().getMsisdn())) {
			participants.add(userInfo);
		}

		Collections.sort(participants, GroupParticipant.lastSeenTimeComparator);

		/*
		 * Adding an element to for the 'add participant' element.
		 */
		participants.add(0, null);

		int loopCount = participants.size() / 2;
		if (participants.size() % 2 != 0) {
			loopCount++;
		}

		for (int i = 0; i < loopCount; i++) {
			int index1 = 2 * i;
			int index2 = 2 * i + 1;

			GroupParticipant[] groupParticipants = new GroupParticipant[2];
			groupParticipants[0] = participants.get(index1);

			if (index2 < participants.size()) {
				groupParticipants[1] = participants.get(index2);
			}

			profileItems
					.add(new ProfileItem.ProfileGroupItem(groupParticipants));
		}

		isGroupOwner = userInfo.getContactInfo().getMsisdn()
				.equals(groupConversation.getGroupOwner());
	}

	private void setupEditScreen() {
		ViewGroup name = (ViewGroup) findViewById(R.id.name);
		ViewGroup phone = (ViewGroup) findViewById(R.id.phone);
		ViewGroup email = (ViewGroup) findViewById(R.id.email);
		ViewGroup gender = (ViewGroup) findViewById(R.id.gender);
		ViewGroup picture = (ViewGroup) findViewById(R.id.photo);

		mNameEdit = (EditText) name.findViewById(R.id.name_input);
		mEmailEdit = (EditText) email.findViewById(R.id.email_input);

		((TextView) name.findViewById(R.id.name_edit_field))
				.setText(R.string.name);
		((TextView) phone.findViewById(R.id.phone_edit_field))
				.setText(R.string.phone_num);
		((TextView) email.findViewById(R.id.email_edit_field))
				.setText(R.string.email);
		((TextView) gender.findViewById(R.id.gender_edit_field))
				.setText(R.string.gender);
		((TextView) picture.findViewById(R.id.photo_edit_field))
				.setText(R.string.edit_picture);

		picture.setBackgroundResource(R.drawable.profile_bottom_item_selector);
		picture.setFocusable(true);

		((EditText) phone.findViewById(R.id.phone_input)).setText(mLocalMSISDN);
		((EditText) phone.findViewById(R.id.phone_input)).setEnabled(false);

		// Make sure that the name text does not exceed the permitted length
		int maxLength = getResources().getInteger(R.integer.max_length_name);
		if (nameTxt.length() > maxLength) {
			nameTxt = nameTxt.substring(0, maxLength);
		}

		mNameEdit.setText(nameTxt);
		mEmailEdit.setText(emailTxt);

		mNameEdit.setSelection(nameTxt.length());
		mEmailEdit.setSelection(emailTxt.length());

		onEmoticonClick(mActivityState.genderType == 0 ? null
				: mActivityState.genderType == 1 ? gender
						.findViewById(R.id.guy) : gender
						.findViewById(R.id.girl));

		// Hide the cursor initially
		Utils.hideCursor(mNameEdit, getResources());
	}

	private void setupProfileScreen(Bundle savedInstanceState) {
		setContentView(R.layout.profile);

		contactInfo = Utils.getUserContactInfo(preferences);

		profileItems = new ArrayList<ProfileItem>();

		// Adding an item for the header
		profileItems.add(new ProfileItem.ProfileStatusItem(
				ProfileItem.HEADER_ID));

		addStatusMessageAsProfileItems(HikeConversationsDatabase.getInstance()
				.getStatusMessages(false,
						HikeConstants.MAX_STATUSES_TO_LOAD_INITIALLY, -1,
						mLocalMSISDN));

		if (contactInfo.isOnhike() && contactInfo.getHikeJoinTime() > 0) {
			profileItems.add(getJoinedHikeStatus(contactInfo));
		}

		profileAdapter = new ProfileAdapter(this, profileItems, null,
				contactInfo, true);

		profileContent = (ListView) findViewById(R.id.profile_content);
		profileContent.setAdapter(profileAdapter);
		profileContent.setOnItemLongClickListener(this);
		profileContent.setOnScrollListener(this);

		if (contactInfo.isOnhike() && contactInfo.getHikeJoinTime() == 0) {
			HikeHttpRequest hikeHttpRequest = new HikeHttpRequest(
					"/account/profile/" + mLocalMSISDN,
					RequestType.HIKE_JOIN_TIME, new HikeHttpCallback() {
						@Override
						public void onSuccess(JSONObject response) {
							Log.d(getClass().getSimpleName(), "Response: "
									+ response.toString());
							try {
								JSONObject profile = response
										.getJSONObject(HikeConstants.PROFILE);
								long hikeJoinTime = profile.optLong(
										HikeConstants.JOIN_TIME, 0);

								Editor editor = preferences.edit();
								editor.putLong(HikeMessengerApp.USER_JOIN_TIME,
										hikeJoinTime);
								editor.commit();

								HikeMessengerApp.getPubSub().publish(
										HikePubSub.USER_JOIN_TIME_OBTAINED,
										new Pair<String, Long>(mLocalMSISDN,
												hikeJoinTime));
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
					});
			mActivityState.getHikeJoinTimeTask = new HikeHTTPTask(null, -1);
			mActivityState.getHikeJoinTimeTask.execute(hikeHttpRequest);
		}
	}

	boolean reachedEnd;
	boolean loadingMoreMessages;

	@Override
	public void onScroll(AbsListView view, final int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		if (!reachedEnd
				&& !loadingMoreMessages
				&& !profileItems.isEmpty()
				&& (firstVisibleItem + visibleItemCount) >= (profileItems
						.size() - HikeConstants.MIN_INDEX_TO_LOAD_MORE_MESSAGES)) {

			Log.d(getClass().getSimpleName(), "Loading more items");
			loadingMoreMessages = true;

			new AsyncTask<Void, Void, List<StatusMessage>>() {

				@Override
				protected List<StatusMessage> doInBackground(Void... params) {
					StatusMessage statusMessage = ((ProfileStatusItem) profileItems
							.get(profileItems.size() - 1)).getStatusMessage();

					if (statusMessage == null) {
						return new ArrayList<StatusMessage>();
					}
					List<StatusMessage> olderMessages = HikeConversationsDatabase
							.getInstance()
							.getStatusMessages(
									true,
									HikeConstants.MAX_OLDER_STATUSES_TO_LOAD_EACH_TIME,
									(int) statusMessage.getId(), mLocalMSISDN);
					return olderMessages;
				}

				@Override
				protected void onPostExecute(List<StatusMessage> olderMessages) {
					if (!olderMessages.isEmpty()) {
						addStatusMessageAsProfileItems(olderMessages);
						profileAdapter.notifyDataSetChanged();
						profileContent.setSelection(firstVisibleItem);
					} else {
						/*
						 * This signifies that we've reached the end. No need to
						 * query the db anymore unless we add a new message.
						 */
						reachedEnd = true;
					}

					loadingMoreMessages = false;
				}

			}.execute();

		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}

	private void fetchPersistentData() {
		nameTxt = preferences.getString(HikeMessengerApp.NAME, "Set a name!");
		mLocalMSISDN = preferences.getString(HikeMessengerApp.MSISDN_SETTING,
				null);
		emailTxt = preferences.getString(HikeConstants.Extras.EMAIL, "");
		lastSavedGender = preferences.getInt(HikeConstants.Extras.GENDER, 0);
		mActivityState.genderType = mActivityState.genderType == 0 ? lastSavedGender
				: mActivityState.genderType;
	}

	public void onBackPressed() {
		if (this.profileType == ProfileType.USER_PROFILE_EDIT) {
			isBackPressed = true;
			saveChanges();
		} else {
			super.onBackPressed();
		}
	}

	public void saveChanges() {
		ArrayList<HikeHttpRequest> requests = new ArrayList<HikeHttpRequest>();

		if ((this.profileType == ProfileType.USER_PROFILE_EDIT)
				&& !TextUtils.isEmpty(mEmailEdit.getText())) {
			if (!Utils.isValidEmail(mEmailEdit.getText())) {
				Toast.makeText(this,
						getResources().getString(R.string.invalid_email),
						Toast.LENGTH_LONG).show();
				return;
			}
		}

		if (mNameEdit != null) {
			final String newName = mNameEdit.getText().toString().trim();
			if (!TextUtils.isEmpty(newName) && !nameTxt.equals(newName)) {
				/* user edited the text, so update the profile */
				HikeHttpRequest request = new HikeHttpRequest(httpRequestURL
						+ "/name", RequestType.OTHER,
						new HikeHttpRequest.HikeHttpCallback() {
							public void onFailure() {
								if (isBackPressed) {
									finishEditing();
								}
							}

							public void onSuccess(JSONObject response) {
								if (ProfileActivity.this.profileType != ProfileType.GROUP_INFO) {
									/*
									 * if the request was successful, update the
									 * shared preferences and the UI
									 */
									String name = newName;
									Editor editor = preferences.edit();
									editor.putString(
											HikeMessengerApp.NAME_SETTING, name);
									editor.commit();
									HikeMessengerApp.getPubSub().publish(
											HikePubSub.PROFILE_NAME_CHANGED,
											null);
								}
								if (isBackPressed) {
									finishEditing();
								}
							}
						});

				JSONObject json = new JSONObject();
				try {
					json.put("name", newName);
					request.setJSONData(json);
				} catch (JSONException e) {
					Log.e("ProfileActivity", "Could not set name", e);
				}
				requests.add(request);
			}
		}

		if (mActivityState.destFilePath != null) {
			/* the server only needs a smaller version */
			final Bitmap smallerBitmap = Utils.scaleDownImage(
					mActivityState.destFilePath,
					HikeConstants.PROFILE_IMAGE_DIMENSIONS, true);

			final byte[] bytes = Utils.bitmapToBytes(smallerBitmap,
					Bitmap.CompressFormat.JPEG, 100);

			if (profileAdapter != null) {
				profileAdapter.setProfilePreview(smallerBitmap);
			}

			HikeHttpRequest request = new HikeHttpRequest(httpRequestURL
					+ "/avatar", RequestType.PROFILE_PIC,
					new HikeHttpRequest.HikeHttpCallback() {
						public void onFailure() {
							Log.d("ProfileActivity", "resetting image");
							Utils.removeTempProfileImage(mLocalMSISDN);
							mActivityState.destFilePath = null;
							if (profileAdapter != null) {
								profileAdapter.notifyDataSetChanged();
							}
							if (isBackPressed) {
								finishEditing();
							}
						}

						public void onSuccess(JSONObject response) {
							mActivityState.destFilePath = null;
							HikeUserDatabase db = HikeUserDatabase
									.getInstance();
							db.setIcon(mLocalMSISDN, bytes, false);

							Utils.renameTempProfileImage(mLocalMSISDN);

							if (profileType == ProfileType.USER_PROFILE
									|| profileType == ProfileType.USER_PROFILE_EDIT) {
								HikeMessengerApp.getPubSub().publish(
										HikePubSub.PROFILE_PIC_CHANGED, null);

								/*
								 * Making the profile pic change a status
								 * message.
								 */
								JSONObject data = response
										.optJSONObject("status");

								if (data == null) {
									return;
								}

								String mappedId = data
										.optString(HikeConstants.STATUS_ID);
								String msisdn = preferences.getString(
										HikeMessengerApp.MSISDN_SETTING, "");
								String name = preferences.getString(
										HikeMessengerApp.NAME_SETTING, "");
								long time = (long) System.currentTimeMillis() / 1000;

								StatusMessage statusMessage = new StatusMessage(
										0, mappedId, msisdn, name, "",
										StatusMessageType.PROFILE_PIC, time,
										-1, 0);
								HikeConversationsDatabase.getInstance()
										.addStatusMessage(statusMessage, true);

								HikeUserDatabase.getInstance().setIcon(
										statusMessage.getMappedId(), bytes,
										false);

								int unseenUserStatusCount = preferences
										.getInt(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT,
												0);
								Editor editor = preferences.edit();
								editor.putInt(
										HikeMessengerApp.UNSEEN_USER_STATUS_COUNT,
										++unseenUserStatusCount);
								editor.commit();
								/*
								 * This would happen in the case where the user
								 * has added a self contact and received an mqtt
								 * message before saving this to the db.
								 */
								if (statusMessage.getId() != -1) {
									HikeMessengerApp.getPubSub().publish(
											HikePubSub.STATUS_MESSAGE_RECEIVED,
											statusMessage);
									HikeMessengerApp
											.getPubSub()
											.publish(
													HikePubSub.TIMELINE_UPDATE_RECIEVED,
													statusMessage);
								}
							}
							if (isBackPressed) {
								finishEditing();
							}
						}
					});

			request.setFilePath(mActivityState.destFilePath);
			requests.add(request);
		}

		if ((this.profileType == ProfileType.USER_PROFILE_EDIT)
				&& ((!emailTxt.equals(mEmailEdit.getText().toString())) || ((mActivityState.genderType != lastSavedGender)))) {
			HikeHttpRequest request = new HikeHttpRequest(httpRequestURL
					+ "/profile", RequestType.OTHER,
					new HikeHttpRequest.HikeHttpCallback() {
						public void onFailure() {
							if (isBackPressed) {
								finishEditing();
							}
						}

						public void onSuccess(JSONObject response) {
							Editor editor = preferences.edit();
							if (Utils.isValidEmail(mEmailEdit.getText())) {
								editor.putString(HikeConstants.Extras.EMAIL,
										mEmailEdit.getText().toString());
							}
							editor.putInt(
									HikeConstants.Extras.GENDER,
									currentSelection != null ? (currentSelection
											.getId() == R.id.guy ? 1 : 2) : 0);
							editor.commit();
							if (isBackPressed) {
								finishEditing();
							}
						}
					});
			JSONObject obj = new JSONObject();
			try {
				Log.d(getClass().getSimpleName(), "Profile details Email: "
						+ mEmailEdit.getText() + " Gender: "
						+ mActivityState.genderType);
				if (!emailTxt.equals(mEmailEdit.getText().toString())) {
					obj.put(HikeConstants.EMAIL, mEmailEdit.getText());
				}
				if (mActivityState.genderType != lastSavedGender) {
					obj.put(HikeConstants.GENDER,
							mActivityState.genderType == 1 ? "m"
									: mActivityState.genderType == 2 ? "f" : "");
				}
				Log.d(getClass().getSimpleName(),
						"JSON to be sent is: " + obj.toString());
				request.setJSONData(obj);
			} catch (JSONException e) {
				Log.e("ProfileActivity", "Could not set email or gender", e);
			}
			requests.add(request);
		}

		if (!requests.isEmpty()) {
			mDialog = ProgressDialog.show(this, null,
					getResources().getString(R.string.updating_profile));
			mActivityState.task = new HikeHTTPTask(this,
					R.string.update_profile_failed);
			HikeHttpRequest[] r = new HikeHttpRequest[requests.size()];
			requests.toArray(r);
			mActivityState.task.execute(r);
		} else if (isBackPressed) {
			finishEditing();
		}
	}

	private void finishEditing() {
		if (this.profileType != ProfileType.GROUP_INFO
				&& this.profileType != ProfileType.USER_PROFILE) {
			Intent i = new Intent(this, ProfileActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
		} else if (this.profileType == ProfileType.USER_PROFILE) {
			super.onBackPressed();
			return;
		}
		super.onBackPressed();
	}

	protected String getLargerIconId() {
		return mLocalMSISDN + "::large";
	}

	@Override
	public void onFinish(boolean success) {
		if (mDialog != null) {
			mDialog.dismiss();
			mDialog = null;
		}

		mActivityState.task = null;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		String path = null;
		if (resultCode != RESULT_OK) {
			return;
		}

		String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT
				+ HikeConstants.PROFILE_ROOT;
		/*
		 * Making sure the directory exists before setting a profile image
		 */
		File dir = new File(directory);
		if (!dir.exists()) {
			dir.mkdirs();
		}

		String fileName = Utils.getTempProfileImageFileName(mLocalMSISDN);
		final String destFilePath = directory + "/" + fileName;

		switch (requestCode) {
		case HikeConstants.CAMERA_RESULT:
			/* fall-through on purpose */
		case HikeConstants.GALLERY_RESULT:
			Log.d("ProfileActivity", "The activity is " + this);
			if (requestCode == HikeConstants.CAMERA_RESULT) {
				String filePath = preferences.getString(
						HikeMessengerApp.FILE_PATH, "");
				selectedFileIcon = new File(filePath);

				/*
				 * Removing this key. We no longer need this.
				 */
				Editor editor = preferences.edit();
				editor.remove(HikeMessengerApp.FILE_PATH);
				editor.commit();
			}
			if (requestCode == HikeConstants.CAMERA_RESULT
					&& !selectedFileIcon.exists()) {
				Toast.makeText(getApplicationContext(), R.string.error_capture,
						Toast.LENGTH_SHORT).show();
				return;
			}
			boolean isPicasaImage = false;
			Uri selectedFileUri = null;
			if (requestCode == HikeConstants.CAMERA_RESULT) {
				path = selectedFileIcon.getAbsolutePath();
			} else {
				selectedFileUri = data.getData();
				if (Utils.isPicasaUri(selectedFileUri.toString())) {
					isPicasaImage = true;
					path = Utils.getOutputMediaFile(HikeFileType.PROFILE, null,
							null).getAbsolutePath();
				} else {
					String fileUriStart = "file://";
					String fileUriString = selectedFileUri.toString();
					if (fileUriString.startsWith(fileUriStart)) {
						selectedFileIcon = new File(URI.create(fileUriString));
						/*
						 * Done to fix the issue in a few Sony devices.
						 */
						path = selectedFileIcon.getAbsolutePath();
					} else {
						path = Utils.getRealPathFromUri(selectedFileUri, this);
					}
				}
			}
			if (TextUtils.isEmpty(path)) {
				Toast.makeText(getApplicationContext(), R.string.error_capture,
						Toast.LENGTH_SHORT).show();
				return;
			}
			if (!isPicasaImage) {
				Utils.startCropActivity(this, path, destFilePath);
			} else {
				final File destFile = new File(path);
				mActivityState.downloadPicasaImageTask = new DownloadImageTask(
						getApplicationContext(), destFile, selectedFileUri,
						new ImageDownloadResult() {

							@Override
							public void downloadFinished(boolean result) {
								if (mDialog != null) {
									mDialog.dismiss();
									mDialog = null;
								}
								mActivityState.downloadPicasaImageTask = null;
								if (!result) {
									Toast.makeText(getApplicationContext(),
											R.string.error_download,
											Toast.LENGTH_SHORT).show();
								} else {
									Utils.startCropActivity(
											ProfileActivity.this,
											destFile.getAbsolutePath(),
											destFilePath);
								}
							}
						});
				mActivityState.downloadPicasaImageTask.execute();
				mDialog = ProgressDialog.show(this, null, getResources()
						.getString(R.string.downloading_image));
			}
			break;
		case HikeConstants.CROP_RESULT:
			mActivityState.destFilePath = data
					.getStringExtra(MediaStore.EXTRA_OUTPUT);
			if (mActivityState.destFilePath == null) {
				Toast.makeText(getApplicationContext(),
						R.string.error_setting_profile, Toast.LENGTH_SHORT)
						.show();
				return;
			}
			if ((this.profileType == ProfileType.USER_PROFILE)
					|| (this.profileType == ProfileType.GROUP_INFO)) {
				saveChanges();
			}
			break;
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int item) {
		Intent intent = null;
		switch (item) {
		case HikeConstants.PROFILE_PICTURE_FROM_CAMERA:
			if (Utils.getExternalStorageState() != ExternalStorageState.WRITEABLE) {
				Toast.makeText(getApplicationContext(),
						R.string.no_external_storage, Toast.LENGTH_SHORT)
						.show();
				return;
			}
			intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			selectedFileIcon = Utils.getOutputMediaFile(HikeFileType.PROFILE,
					null, null); // create a file to save the image
			if (selectedFileIcon != null) {
				intent.putExtra(MediaStore.EXTRA_OUTPUT,
						Uri.fromFile(selectedFileIcon));

				/*
				 * Saving the file path. Will use this to get the file once the
				 * image has been captured.
				 */
				Editor editor = preferences.edit();
				editor.putString(HikeMessengerApp.FILE_PATH,
						selectedFileIcon.getAbsolutePath());
				editor.commit();

				startActivityForResult(intent, HikeConstants.CAMERA_RESULT);
			} else {
				Toast.makeText(this, getString(R.string.no_sd_card),
						Toast.LENGTH_LONG).show();
			}
			break;
		case HikeConstants.PROFILE_PICTURE_FROM_GALLERY:
			if (Utils.getExternalStorageState() == ExternalStorageState.NONE) {
				Toast.makeText(getApplicationContext(),
						R.string.no_external_storage, Toast.LENGTH_SHORT)
						.show();
				return;
			}
			intent = new Intent(Intent.ACTION_PICK);
			intent.setType("image/*");
			startActivityForResult(intent, HikeConstants.GALLERY_RESULT);
			break;
		}
	}

	public void onEmoticonClick(View v) {
		if (v != null) {
			if (currentSelection != null) {
				currentSelection.setSelected(false);
			}
			v.setSelected(currentSelection != v);
			currentSelection = v == currentSelection ? null : v;
			if (currentSelection != null) {
				mActivityState.genderType = currentSelection.getId() == R.id.guy ? 1
						: 2;
				return;
			}
			mActivityState.genderType = 0;
		}
	}

	public void onViewImageClicked(View v) {
		ImageViewerInfo imageViewerInfo = (ImageViewerInfo) v.getTag();

		String mappedId = imageViewerInfo.mappedId;

		String url = imageViewerInfo.url;

		Bundle arguments = new Bundle();
		arguments.putString(HikeConstants.Extras.MAPPED_ID, mappedId);
		arguments.putString(HikeConstants.Extras.URL, url);
		arguments.putBoolean(HikeConstants.Extras.IS_STATUS_IMAGE,
				imageViewerInfo.isStatusMessage);

		HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_IMAGE, arguments);
	}

	public void onYesBtnClick(View v) {
		ContactInfo contactInfo = (ContactInfo) v.getTag();
		respondToFriendRequest(contactInfo, true);
	}

	public void onNoBtnClick(View v) {
		ContactInfo contactInfo = (ContactInfo) v.getTag();
		respondToFriendRequest(contactInfo, false);
	}

	private void respondToFriendRequest(ContactInfo contactInfo,
			boolean accepted) {
		FavoriteType favoriteType = accepted ? FavoriteType.FRIEND
				: FavoriteType.REQUEST_RECEIVED_REJECTED;
		contactInfo.setFavoriteType(favoriteType);
		Pair<ContactInfo, FavoriteType> favoriteToggle = new Pair<ContactInfo, ContactInfo.FavoriteType>(
				contactInfo, favoriteType);
		HikeMessengerApp.getPubSub().publish(
				accepted ? HikePubSub.FAVORITE_TOGGLED
						: HikePubSub.REJECT_FRIEND_REQUEST, favoriteToggle);
	}

	public void onTextButtonClick(View v) {
		if (contactInfo.isOnhike()) {
			contactInfo.setFavoriteType(FavoriteType.REQUEST_SENT);

			Pair<ContactInfo, FavoriteType> favoriteToggle = new Pair<ContactInfo, FavoriteType>(
					contactInfo, contactInfo.getFavoriteType());
			HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_TOGGLED,
					favoriteToggle);
		} else {
			inviteToHike(contactInfo.getMsisdn());
		}
	}

	public void onHeaderButtonClicked(View v) {
		if (profileType == ProfileType.USER_PROFILE
				|| profileType == ProfileType.GROUP_INFO) {
			/*
			 * The wants to change their profile picture. Open a dialog to allow
			 * them pick Camera or Gallery
			 */
			final CharSequence[] items = getResources().getStringArray(
					R.array.profile_pic_dialog);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.choose_picture);
			builder.setItems(items, this);
			mDialog = builder.show();
		} else if (profileType == ProfileType.CONTACT_INFO) {
			openChatThread(contactInfo);
		}
	}

	public void onEditProfileClicked(View v) {
		Utils.logEvent(ProfileActivity.this,
				HikeConstants.LogEvent.EDIT_PROFILE);
		Intent i = new Intent(ProfileActivity.this, ProfileActivity.class);
		i.putExtra(HikeConstants.Extras.EDIT_PROFILE, true);
		startActivity(i);
		finish();
	}

	public void onProfileLargeBtnClick(View v) {
		switch (profileType) {
		case GROUP_INFO:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.leave_group_confirm);
			builder.setPositiveButton(R.string.yes,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							Intent intent = new Intent(ProfileActivity.this,
									HomeActivity.class);
							intent.putExtra(HikeConstants.Extras.GROUP_LEFT,
									mLocalMSISDN);
							intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							startActivity(intent);
							finish();
						}
					});
			builder.setNegativeButton(R.string.no,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					});
			builder.setCancelable(true);
			AlertDialog alertDialog = builder.create();
			alertDialog.show();
			break;
		case CONTACT_INFO:
			openChatThread(contactInfo);
			break;
		case USER_PROFILE:
			startActivity(new Intent(this, StatusUpdate.class));
			break;
		}
	}

	private void openChatThread(ContactInfo contactInfo) {
		Intent intent = Utils.createIntentFromContactInfo(contactInfo);
		intent.setClass(this, ChatThread.class);
		if (!getIntent().getBooleanExtra(
				HikeConstants.Extras.FROM_CENTRAL_TIMELINE, false)) {
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		} else {
			intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
		}
		startActivity(intent);
	}

	public void onProfileSmallLeftBtnClick(View v) {
		Utils.logEvent(ProfileActivity.this,
				HikeConstants.LogEvent.ADD_PARTICIPANT);

		Intent intent = new Intent(ProfileActivity.this, ChatThread.class);
		intent.putExtra(HikeConstants.Extras.GROUP_CHAT, true);
		intent.putExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT, mLocalMSISDN);
		startActivity(intent);

	}

	public void onProfileSmallRightBtnClick(View v) {
		groupConversation.setIsMuted(!groupConversation.isMuted());

		((TextView) v.findViewById(R.id.btn2_txt)).setText(groupConversation
				.isMuted() ? R.string.unmute_group : R.string.mute_group);
		((ImageView) v.findViewById(R.id.btn2_img))
				.setImageResource(groupConversation.isMuted() ? R.drawable.ic_unmute
						: R.drawable.ic_mute);

		HikeMessengerApp.getPubSub().publish(
				HikePubSub.MUTE_CONVERSATION_TOGGLED,
				new Pair<String, Boolean>(groupConversation.getMsisdn(),
						groupConversation.isMuted()));
	}

	public void onProfileBtn1Click(View v) {
		if (profileAdapter.isContactBlocked()) {
			HikeMessengerApp.getPubSub().publish(HikePubSub.UNBLOCK_USER,
					mLocalMSISDN);
			profileAdapter.setIsContactBlocked(false);
			profileAdapter.notifyDataSetChanged();
		} else {
			if (contactInfo.isOnhike()) {
				contactInfo.setFavoriteType(FavoriteType.REQUEST_SENT);

				Pair<ContactInfo, FavoriteType> favoriteToggle = new Pair<ContactInfo, FavoriteType>(
						contactInfo, contactInfo.getFavoriteType());
				HikeMessengerApp.getPubSub().publish(
						HikePubSub.FAVORITE_TOGGLED, favoriteToggle);
			} else {
				inviteToHike(contactInfo.getMsisdn());
			}
		}
	}

	public void onProfileBtn2Click(View v) {
		contactInfo.setFavoriteType(FavoriteType.REQUEST_SENT);

		Pair<ContactInfo, FavoriteType> favoriteToggle = new Pair<ContactInfo, FavoriteType>(
				contactInfo, contactInfo.getFavoriteType());
		HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_TOGGLED,
				favoriteToggle);
	}

	public void onEditGroupNameClick(View v) {
		if (groupConversation == null) {
			return;
		}
		groupEditDialog = new Dialog(this, R.style.Theme_CustomDialog_Keyboard);
		groupEditDialog.setContentView(R.layout.group_name_change_dialog);
		groupEditDialog.setCancelable(true);
		groupEditDialog.getWindow().setGravity(Gravity.CENTER);

		mNameEdit = (EditText) groupEditDialog
				.findViewById(R.id.group_name_edit);

		mNameEdit
				.setText(TextUtils.isEmpty(mActivityState.edittedGroupName) ? groupConversation
						.getLabel() : mActivityState.edittedGroupName);

		mNameEdit.setSelection(mNameEdit.length());

		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		imm.showSoftInput(mNameEdit, InputMethodManager.SHOW_IMPLICIT);

		mActivityState.groupEditDialogShowing = true;

		Button okBtn = (Button) groupEditDialog.findViewById(R.id.btn_ok);
		Button cancelBtn = (Button) groupEditDialog
				.findViewById(R.id.btn_cancel);

		okBtn.setText(R.string.save);
		okBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Utils.hideSoftKeyboard(ProfileActivity.this, mNameEdit);
				groupEditDialog.cancel();
				saveChanges();
			}
		});
		cancelBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Utils.hideSoftKeyboard(ProfileActivity.this, mNameEdit);
				groupEditDialog.cancel();
			}
		});

		groupEditDialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				mActivityState.groupEditDialogShowing = false;
				mActivityState.edittedGroupName = null;
			}
		});
		groupEditDialog.show();
	}

	public void onBlockGroupOwnerClicked(View v) {
		Button blockBtn = (Button) v;
		HikeMessengerApp.getPubSub().publish(
				isBlocked ? HikePubSub.UNBLOCK_USER : HikePubSub.BLOCK_USER,
				this.groupOwner);
		isBlocked = !isBlocked;
		blockBtn.setText(!isBlocked ? R.string.block_owner
				: R.string.unblock_owner);
	}

	public void onAddToContactClicked(View v) {
		if (profileType != ProfileType.CONTACT_INFO) {
			return;
		}
		if (!contactInfo.getMsisdn().equals(contactInfo.getId())) {
			return;
		}
		Utils.logEvent(this, HikeConstants.LogEvent.MENU_ADD_TO_CONTACTS);
		addToContacts(mLocalMSISDN);
	}

	private void addToContacts(String msisdn) {
		Intent i = new Intent(Intent.ACTION_INSERT_OR_EDIT);
		i.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
		i.putExtra(Insert.PHONE, msisdn);
		startActivity(i);
	}

	public void onInviteToHikeClicked(View v) {
		inviteToHike(contactInfo.getMsisdn());
	}

	private void inviteToHike(String msisdn) {
		HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH,
				Utils.makeHike2SMSInviteMessage(msisdn, this).serialize());

		Toast toast = Toast.makeText(ProfileActivity.this,
				R.string.invite_sent, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.BOTTOM, 0, 0);
		toast.show();
	}

	public void onCallClicked(View v) {
		Builder builder = new Builder(this);
		builder.setTitle(R.string.call_not_free_head);
		builder.setMessage(R.string.call_not_free_body);
		builder.setPositiveButton(R.string.call, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Utils.logEvent(ProfileActivity.this,
						HikeConstants.LogEvent.MENU_CALL);
				Intent callIntent = new Intent(Intent.ACTION_CALL);
				callIntent.setData(Uri.parse("tel:" + mLocalMSISDN));
				startActivity(callIntent);
			}
		});
		builder.setNegativeButton(R.string.cancel, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.show();
	}

	public void onBlockUserClicked(View v) {
		Button blockBtn = (Button) v;
		HikeMessengerApp.getPubSub().publish(
				isBlocked ? HikePubSub.UNBLOCK_USER : HikePubSub.BLOCK_USER,
				this.mLocalMSISDN);
		isBlocked = !isBlocked;
		blockBtn.setText(!isBlocked ? R.string.block_user
				: R.string.unblock_user);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onEventReceived(final String type, Object object) {
		super.onEventReceived(type, object);

		if (mLocalMSISDN == null) {
			Log.w(getClass().getSimpleName(),
					"The msisdn is null, we are doing something wrong.."
							+ object);
			return;
		}
		if (HikePubSub.GROUP_NAME_CHANGED.equals(type)) {
			if (mLocalMSISDN.equals((String) object)) {
				HikeConversationsDatabase db = HikeConversationsDatabase
						.getInstance();
				nameTxt = db.getGroupName(mLocalMSISDN);
				groupConversation.setContactName(nameTxt);

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						profileAdapter
								.updateGroupConversation(groupConversation);
					}
				});
			}
		} else if (HikePubSub.ICON_CHANGED.equals(type)) {
			if (mLocalMSISDN.equals((String) object)) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						profileAdapter.notifyDataSetChanged();
					}
				});
			}
		}

		if (HikePubSub.PARTICIPANT_LEFT_GROUP.equals(type)) {
			if (mLocalMSISDN.equals(((JSONObject) object)
					.optString(HikeConstants.TO))) {
				String msisdn = ((JSONObject) object)
						.optString(HikeConstants.DATA);
				this.participantMap.remove(msisdn);

				setupGroupProfileList();

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						profileAdapter.notifyDataSetChanged();
					}
				});
			}
		} else if (HikePubSub.PARTICIPANT_JOINED_GROUP.equals(type)) {
			if (mLocalMSISDN.equals(((JSONObject) object)
					.optString(HikeConstants.TO))) {
				final JSONObject obj = (JSONObject) object;
				final JSONArray participants = obj
						.optJSONArray(HikeConstants.DATA);
				for (int i = 0; i < participants.length(); i++) {
					String msisdn = participants.optJSONObject(i).optString(
							HikeConstants.MSISDN);

					HikeUserDatabase hUDB = HikeUserDatabase.getInstance();
					ContactInfo participant = hUDB.getContactInfoFromMSISDN(
							msisdn, false);

					if (TextUtils.isEmpty(participant.getName())) {
						HikeConversationsDatabase hCDB = HikeConversationsDatabase
								.getInstance();
						participant.setName(hCDB.getParticipantName(
								mLocalMSISDN, msisdn));
					}

					participantMap.put(msisdn,
							new GroupParticipant(participant));
				}
				setupGroupProfileList();
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						profileAdapter.notifyDataSetChanged();
					}
				});
			}
		} else if (HikePubSub.GROUP_END.equals(type)) {
			mLocalMSISDN.equals(((JSONObject) object)
					.optString(HikeConstants.TO));
			{
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						ProfileActivity.this.finish();
					}
				});
			}
		} else if (HikePubSub.CONTACT_ADDED.equals(type)) {
			final ContactInfo contact = (ContactInfo) object;
			if (contactInfo == null) {
				return;
			}

			if (!this.mLocalMSISDN.equals(contact.getMsisdn())) {
				return;
			}
			this.contactInfo = contact;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					profileAdapter.updateContactInfo(contactInfo);
					if (topBarBtn != null) {
						topBarBtn.setImageResource(R.drawable.ic_call_top);
					}
				}
			});
		} else if (HikePubSub.USER_JOINED.equals(type)
				|| HikePubSub.USER_LEFT.equals(type)) {
			String msisdn = (String) object;
			if (!mLocalMSISDN.equals(msisdn)
					&& profileType != ProfileType.GROUP_INFO) {
				return;
			} else if (profileType == ProfileType.GROUP_INFO) {
				GroupParticipant groupParticipant = groupConversation
						.getGroupParticipant(msisdn);
				if (groupParticipant == null) {
					return;
				}
				groupParticipant.getContactInfo().setOnhike(
						HikePubSub.USER_JOINED.equals(type));
			}

			if (profileType == ProfileType.GROUP_INFO) {
				setupGroupProfileList();
			} else {
				setupContactProfileList();
			}
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					profileAdapter.notifyDataSetChanged();
				}
			});
		} else if (HikePubSub.STATUS_MESSAGE_RECEIVED.equals(type)) {
			StatusMessage statusMessage = (StatusMessage) object;
			if (!mLocalMSISDN.equals(statusMessage.getMsisdn())
					|| statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST_ACCEPTED
					|| statusMessage.getStatusMessageType() == StatusMessageType.USER_ACCEPTED_FRIEND_REQUEST) {
				return;
			}
			profileItems.add(showingRequestItem ? 2 : 1,
					new ProfileItem.ProfileStatusItem(statusMessage));
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					profileAdapter.notifyDataSetChanged();
				}
			});
		} else if (HikePubSub.FAVORITE_TOGGLED.equals(type)
				|| HikePubSub.FRIEND_REQUEST_ACCEPTED.equals(type)
				|| HikePubSub.REJECT_FRIEND_REQUEST.equals(type)) {
			final Pair<ContactInfo, FavoriteType> favoriteToggle = (Pair<ContactInfo, FavoriteType>) object;

			ContactInfo contactInfo = favoriteToggle.first;
			FavoriteType favoriteType = favoriteToggle.second;

			if (!mLocalMSISDN.equals(contactInfo.getMsisdn())) {
				return;
			}
			this.contactInfo.setFavoriteType(favoriteType);
			setupContactProfileList();
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					profileAdapter.notifyDataSetChanged();
				}
			});
		} else if (HikePubSub.HIKE_JOIN_TIME_OBTAINED.equals(type)
				|| HikePubSub.USER_JOIN_TIME_OBTAINED.equals(type)) {
			Pair<String, Long> msisdnHikeJoinTimePair = (Pair<String, Long>) object;

			String msisdn = msisdnHikeJoinTimePair.first;
			long hikeJoinTime = msisdnHikeJoinTimePair.second;

			if (!msisdn.equals(mLocalMSISDN)) {
				return;
			}

			contactInfo.setHikeJoinTime(hikeJoinTime);
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (showContactsUpdates(contactInfo)) {
						profileItems.add(getJoinedHikeStatus(contactInfo));
					}

					profileAdapter.notifyDataSetChanged();
				}
			});
		} else if (HikePubSub.LAST_SEEN_TIME_UPDATED.equals(type)) {
			final ContactInfo contactInfo = (ContactInfo) object;

			if (profileType != ProfileType.CONTACT_INFO) {
				return;
			}

			if (!mLocalMSISDN.equals(contactInfo.getMsisdn())
					|| (contactInfo.getFavoriteType() != FavoriteType.FRIEND
							&& contactInfo.getFavoriteType() != FavoriteType.REQUEST_RECEIVED && contactInfo
							.getFavoriteType() != FavoriteType.REQUEST_RECEIVED_REJECTED)) {
				return;
			}

			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					profileAdapter.updateContactInfo(contactInfo);
				}

			});
		}
	}

	private ProfileItem getJoinedHikeStatus(ContactInfo contactInfo) {
		return new ProfileItem.ProfileStatusItem(new StatusMessage(0, null,
				contactInfo.getMsisdn(), contactInfo.getName(),
				getString(R.string.joined_hike_update),
				StatusMessageType.JOINED_HIKE, contactInfo.getHikeJoinTime()));
	}

	@Override
	public boolean onLongClick(View view) {
		GroupParticipant groupParticipant = (GroupParticipant) view.getTag();

		ArrayList<String> optionsList = new ArrayList<String>();
		ArrayList<Integer> optionImagesList = new ArrayList<Integer>();

		ContactInfo tempContactInfo = null;

		if (groupParticipant == null) {
			return false;
		}

		String myMsisdn = preferences.getString(
				HikeMessengerApp.MSISDN_SETTING, "");

		tempContactInfo = groupParticipant.getContactInfo();
		if (myMsisdn.equals(tempContactInfo.getMsisdn())) {
			return false;
		}

		final ContactInfo contactInfo = tempContactInfo;

		optionsList.add(getString(R.string.send_message));
		optionImagesList.add(R.drawable.ic_send_message);
		if (!tempContactInfo.isOnhike()) {
			optionsList.add(getString(R.string.invite_to_hike));
			optionImagesList.add(R.drawable.ic_invite_single);
		}
		if (tempContactInfo.isUnknownContact()) {
			optionsList.add(getString(R.string.add_to_contacts));
			optionImagesList.add(R.drawable.ic_add_to_contacts);
		}
		if (isGroupOwner) {
			optionsList.add(getString(R.string.remove_from_group));
			optionImagesList.add(R.drawable.ic_remove_from_group);
		}

		final String[] options = new String[optionsList.size()];
		optionsList.toArray(options);

		final Integer[] optionIcons = new Integer[optionImagesList.size()];
		optionImagesList.toArray(optionIcons);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		ListAdapter dialogAdapter = new ArrayAdapter<CharSequence>(this,
				R.layout.alert_item, R.id.item, options) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = super.getView(position, convertView, parent);
				if (optionIcons.length > 0) {
					TextView tv = (TextView) v.findViewById(R.id.item);
					tv.setCompoundDrawablesWithIntrinsicBounds(
							optionIcons[position], 0, 0, 0);
				}
				return v;
			}

		};

		builder.setAdapter(dialogAdapter,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String option = options[which];
						if (getString(R.string.send_message).equals(option)) {
							openChatThread(contactInfo);
						} else if (getString(R.string.invite_to_hike).equals(
								option)) {
							inviteToHike(contactInfo.getMsisdn());
						} else if (getString(R.string.add_to_contacts).equals(
								option)) {
							addToContacts(contactInfo.getMsisdn());
						} else if (getString(R.string.remove_from_group)
								.equals(option)) {
							removeFromGroup(contactInfo);
						}
					}
				});

		AlertDialog alertDialog = builder.show();
		alertDialog.getListView().setDivider(
				getResources()
						.getDrawable(R.drawable.ic_thread_divider_profile));
		return true;
	}

	private void removeFromGroup(final ContactInfo contactInfo) {
		AlertDialog.Builder builder = new AlertDialog.Builder(
				ProfileActivity.this);

		String message = getString(R.string.remove_confirm,
				contactInfo.getFirstName());

		builder.setMessage(message);
		builder.setPositiveButton(R.string.yes,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						JSONObject object = new JSONObject();
						try {
							object.put(HikeConstants.TO,
									groupConversation.getMsisdn());
							object.put(
									HikeConstants.TYPE,
									HikeConstants.MqttMessageTypes.GROUP_CHAT_KICK);

							JSONObject data = new JSONObject();

							JSONArray msisdns = new JSONArray();
							msisdns.put(contactInfo.getMsisdn());

							data.put(HikeConstants.MSISDNS, msisdns);

							object.put(HikeConstants.DATA, data);
						} catch (JSONException e) {
							Log.e(getClass().getSimpleName(), "Invalid JSON", e);
						}
						HikeMessengerApp.getPubSub().publish(
								HikePubSub.MQTT_PUBLISH, object);
					}
				});
		builder.setNegativeButton(R.string.no,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});
		builder.setCancelable(true);
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view,
			int position, long id) {

		ArrayList<String> optionsList = new ArrayList<String>();

		StatusMessage tempStatusMessage = null;

		ProfileItem profileItem = profileAdapter.getItem(position);
		tempStatusMessage = ((ProfileStatusItem) profileItem)
				.getStatusMessage();

		if (tempStatusMessage == null
				|| (tempStatusMessage.getStatusMessageType() != StatusMessageType.TEXT && tempStatusMessage
						.getStatusMessageType() != StatusMessageType.PROFILE_PIC)) {
			return false;
		}

		optionsList.add(getString(R.string.delete_status));

		final StatusMessage statusMessage = tempStatusMessage;

		final String[] options = new String[optionsList.size()];
		optionsList.toArray(options);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		ListAdapter dialogAdapter = new ArrayAdapter<CharSequence>(this,
				R.layout.alert_item, R.id.item, options);

		builder.setAdapter(dialogAdapter,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						showDeleteStatusConfirmationDialog(statusMessage
								.getMappedId());
					}
				});

		AlertDialog alertDialog = builder.show();
		alertDialog.getListView().setDivider(
				getResources()
						.getDrawable(R.drawable.ic_thread_divider_profile));
		return true;
	}

	private void showDeleteStatusConfirmationDialog(final String statusId) {
		AlertDialog.Builder builder = new Builder(this);
		builder.setMessage(R.string.delete_status_confirmation);

		builder.setNegativeButton(R.string.no, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});

		builder.setPositiveButton(R.string.yes, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				deleteStatus(statusId);
			}
		});
		builder.show();
	}

	private void deleteStatus(final String statusId) {
		HikeHttpRequest hikeHttpRequest = new HikeHttpRequest("/user/status/"
				+ statusId, RequestType.DELETE_STATUS, new HikeHttpCallback() {

			@Override
			public void onSuccess(JSONObject response) {
				HikeMessengerApp.getPubSub().publish(HikePubSub.DELETE_STATUS,
						statusId);
				for (ProfileItem profileItem : profileItems) {
					StatusMessage message = ((ProfileStatusItem) profileItem)
							.getStatusMessage();

					if (statusId.equals(message.getMappedId())) {
						profileItems.remove(message);
						break;
					}
				}
				profileAdapter.notifyDataSetChanged();
			}

		});
		mActivityState.task = new HikeHTTPTask(this,
				R.string.delete_status_error);
		mActivityState.task.execute(hikeHttpRequest);
		mDialog = ProgressDialog.show(this, null,
				getString(R.string.deleting_status));
	}

	@Override
	public void onClick(View v) {
		GroupParticipant groupParticipant = (GroupParticipant) v.getTag();
		if (groupParticipant == null) {
			Intent intent = new Intent(ProfileActivity.this,
					ComposeActivity.class);
			intent.putExtra(HikeConstants.Extras.GROUP_CHAT, true);
			intent.putExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT,
					mLocalMSISDN);
			startActivity(intent);

		} else {

			ContactInfo contactInfo = groupParticipant.getContactInfo();

			String myMsisdn = preferences.getString(
					HikeMessengerApp.MSISDN_SETTING, "");

			Intent intent = new Intent(this, ProfileActivity.class);

			intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
			if (myMsisdn.equals(contactInfo.getMsisdn())) {
				startActivity(intent);
				return;
			}

			intent.setClass(this, ProfileActivity.class);
			intent.putExtra(HikeConstants.Extras.CONTACT_INFO,
					contactInfo.getMsisdn());
			intent.putExtra(HikeConstants.Extras.ON_HIKE,
					contactInfo.isOnhike());
			startActivity(intent);
		}
	}
}
