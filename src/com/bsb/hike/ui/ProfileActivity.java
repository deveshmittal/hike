package com.bsb.hike.ui;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
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
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.cropimage.CropImage;
import com.bsb.hike.cropimage.Util;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.ProfileItem;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.utils.DrawerBaseActivity;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;
import com.fiksu.asotracking.FiksuTrackingManager;

public class ProfileActivity extends DrawerBaseActivity implements
		FinishableEvent, android.content.DialogInterface.OnClickListener,
		Listener {
	/* dialog IDs */
	private static final int PROFILE_PICTURE_FROM_CAMERA = 0;
	private static final int PROFILE_PICTURE_FROM_GALLERY = 1;

	/* activityForResult IDs */
	private static final int CAMERA_RESULT = 0;
	private static final int GALLERY_RESULT = 1;
	private static final int CROP_RESULT = 2;

	private ImageView mIconView;
	private EditText mNameEdit;

	private View currentSelection;

	private Dialog mDialog;
	private String mLocalMSISDN = null;

	private ActivityState mActivityState; /* config state of this activity */
	private String nameTxt;
	private boolean isBackPressed = false;
	private EditText mEmailEdit;
	private String emailTxt;
	private Map<String, GroupParticipant> participantList;

	private ProfileType profileType;
	private String httpRequestURL;
	private String groupOwner;

	private boolean shouldShowInviteAllButton = false;
	private TextView mNameDisplay;
	private ViewGroup participantNameContainer;
	private ProfileItem[] items;
	private int lastSavedGender;

	private SharedPreferences preferences;

	private String[] groupInfoPubSubListeners = { HikePubSub.ICON_CHANGED,
			HikePubSub.GROUP_NAME_CHANGED, HikePubSub.GROUP_END,
			HikePubSub.PARTICIPANT_JOINED_GROUP,
			HikePubSub.PARTICIPANT_LEFT_GROUP };

	private String[] contactInfoPubSubListeners = { HikePubSub.ICON_CHANGED,
			HikePubSub.CONTACT_ADDED, HikePubSub.USER_JOINED,
			HikePubSub.USER_LEFT };
	private GroupConversation groupConversation;
	private ImageButton topBarBtn;
	private ContactInfo contactInfo;
	private boolean isBlocked;

	private static enum ProfileType {
		USER_PROFILE, // The user profile screen
		USER_PROFILE_EDIT, // The user profile edit screen
		GROUP_INFO, // The group info screen
		CONTACT_INFO // Contact info screen
	};

	private class ActivityState {
		public HikeHTTPTask task; /* the task to update the global profile */
		public DownloadPicasaImageTask downloadPicasaImageTask; /*
																 * the task to
																 * download the
																 * picasa image
																 */

		public Bitmap newBitmap = null; /* the bitmap before the user saves it */
		public int genderType;
	}

	public File selectedFileIcon; /*
								 * the selected file that we'll store the
								 * profile camera picture
								 */

	/* store the task so we can keep keep the progress dialog going */
	@Override
	public Object onRetainNonConfigurationInstance() {
		Log.d("ProfileActivity", "onRetainNonConfigurationinstance");
		return mActivityState;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mDialog != null) {
			mDialog.dismiss();
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
		}
		mActivityState = null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (Utils.requireAuth(this)) {
			return;
		}

		preferences = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
				MODE_PRIVATE);

		Object o = getLastNonConfigurationInstance();
		if (o instanceof ActivityState) {
			mActivityState = (ActivityState) o;
			if (mActivityState.task != null) {
				/* we're currently executing a task, so show the progress dialog */
				mActivityState.task.setActivity(this);
				mDialog = ProgressDialog.show(this, null, getResources()
						.getString(R.string.updating_profile));
			} else if (mActivityState.downloadPicasaImageTask != null) {
				mDialog = ProgressDialog.show(this, null, getResources()
						.getString(R.string.downloading_image));
			}
		} else {
			mActivityState = new ActivityState();
		}

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
				setupProfileScreen(savedInstanceState);
			}
		}
	}

	private void setupContactProfileScreen() {
		setContentView(R.layout.contact_info);

		boolean canCall = getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_TELEPHONY);

		this.mLocalMSISDN = getIntent().getStringExtra(
				HikeConstants.Extras.CONTACT_INFO);

		contactInfo = HikeUserDatabase.getInstance().getContactInfoFromMSISDN(
				mLocalMSISDN, false);

		findViewById(R.id.button_bar3).setVisibility(View.VISIBLE);
		topBarBtn = (ImageButton) findViewById(R.id.title_image_btn2);

		if (!contactInfo.isOnhike()) {
			contactInfo.setOnhike(getIntent().getBooleanExtra(
					HikeConstants.Extras.ON_HIKE, false));
		}

		topBarBtn
				.setImageResource(contactInfo.getFavoriteType() == FavoriteType.FAVORITE ? R.drawable.ic_favorite
						: R.drawable.ic_not_favorite);
		topBarBtn.setVisibility(View.VISIBLE);
		findViewById(R.id.title_image_btn2_container).setVisibility(
				View.VISIBLE);

		findViewById(R.id.add_to_contacts).setVisibility(
				!TextUtils.isEmpty(contactInfo.getName()) ? View.GONE
						: View.VISIBLE);
		findViewById(R.id.invite_to_hike_btn).setVisibility(
				contactInfo.isOnhike() ? View.GONE : View.VISIBLE);
		findViewById(R.id.call_btn).setVisibility(
				canCall ? View.VISIBLE : View.GONE);

		TextView mTitleView = (TextView) findViewById(R.id.title);
		mTitleView.setText(R.string.user_info);

		mIconView = (ImageView) findViewById(R.id.profile);
		mIconView.setImageDrawable(IconCacheManager.getInstance()
				.getIconForMSISDN(mLocalMSISDN));

		isBlocked = HikeUserDatabase.getInstance().isBlocked(mLocalMSISDN);
		((TextView) findViewById(R.id.block_user_btn))
				.setText(!isBlocked ? R.string.block_user
						: R.string.unblock_user);

		((TextView) findViewById(R.id.name_current)).setText(TextUtils
				.isEmpty(contactInfo.getName()) ? mLocalMSISDN : contactInfo
				.getName());
	}

	private void setupGroupProfileScreen() {
		setContentView(R.layout.group_info);

		findViewById(R.id.button_bar3).setVisibility(View.VISIBLE);

		topBarBtn = (ImageButton) findViewById(R.id.title_image_btn2);

		ViewGroup addParticipantsLayout = (ViewGroup) findViewById(R.id.add_participants_layout);
		TextView mTitleView = (TextView) findViewById(R.id.title);
		View groupOwnerItem = (View) findViewById(R.id.group_owner);
		mNameEdit = (EditText) findViewById(R.id.name_input);
		mNameDisplay = (TextView) findViewById(R.id.name_display);
		mIconView = (ImageView) findViewById(R.id.profile);

		addParticipantsLayout.setFocusable(true);
		addParticipantsLayout
				.setBackgroundResource(R.drawable.profile_bottom_item_selector);

		this.mLocalMSISDN = getIntent().getStringExtra(
				HikeConstants.Extras.EXISTING_GROUP_CHAT);

		HikeConversationsDatabase hCDB = HikeConversationsDatabase
				.getInstance();
		groupConversation = (GroupConversation) hCDB.getConversation(
				mLocalMSISDN, 0);

		participantList = groupConversation.getGroupParticipantList();
		httpRequestURL = "/group/" + groupConversation.getMsisdn();

		participantNameContainer = (ViewGroup) findViewById(R.id.group_participant_container);

		topBarBtn
				.setImageResource(groupConversation.isMuted() ? R.drawable.ic_group_muted
						: R.drawable.ic_group_not_muted);
		topBarBtn.setVisibility(View.VISIBLE);
		findViewById(R.id.title_image_btn2_container).setVisibility(
				View.VISIBLE);

		int left = (int) (0 * Utils.densityMultiplier);
		int top = (int) (0 * Utils.densityMultiplier);
		int right = (int) (0 * Utils.densityMultiplier);
		int bottom = (int) (6 * Utils.densityMultiplier);

		GroupParticipant userInfo = new GroupParticipant(
				Utils.getUserContactInfo(preferences));
		participantList.put(userInfo.getContactInfo().getMsisdn(), userInfo);

		groupOwner = groupConversation.getGroupOwner();

		isBlocked = HikeUserDatabase.getInstance().isBlocked(groupOwner);

		Set<String> activeParticipants = new HashSet<String>();
		activeParticipants.add(groupOwner);
		for (Entry<String, GroupParticipant> participant : participantList
				.entrySet()) {
			ContactInfo contactInfo = participant.getValue().getContactInfo();
			if (participant.getKey().equals(groupOwner)) {
				TextView groupOwnerTextView = (TextView) groupOwnerItem
						.findViewById(R.id.participant_name);
				groupOwnerTextView.setText(participant.getValue()
						.getContactInfo().getFirstName());
				continue;
			}
			if (!contactInfo.isOnhike() && !participant.getValue().hasLeft()) {
				shouldShowInviteAllButton = true;
			}
			// Dont show participant that has left group
			if (participant.getValue().hasLeft()) {
				continue;
			}
			activeParticipants.add(participant.getKey());
			View participantNameItem = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE))
					.inflate(R.layout.participant_name_item, null);

			TextView participantName = (TextView) participantNameItem
					.findViewById(R.id.participant_name);
			participantName.setText(Utils.ellipsizeName(contactInfo
					.getFirstName()));
			participantName.setTextColor(getResources().getColor(
					contactInfo.isOnhike() ? R.color.contact_blue
							: R.color.contact_green));
			participantName
					.setBackgroundResource(contactInfo.isOnhike() ? R.drawable.hike_contact_bg
							: R.drawable.sms_contact_bg);

			ImageView dndImg = (ImageView) participantNameItem
					.findViewById(R.id.dnd_img);
			dndImg.setVisibility(!contactInfo.isOnhike()
					&& participant.getValue().onDnd() ? View.VISIBLE
					: View.INVISIBLE);

			LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT);
			lp.setMargins(left, top, right, bottom);
			participantNameItem.setLayoutParams(lp);
			participantNameItem.setId(participant.getKey().hashCode());

			participantNameContainer.addView(participantNameItem);
		}
		Button blockGroupOwner = (Button) findViewById(R.id.block_owner_btn);
		if (groupOwner.equals(userInfo.getContactInfo().getMsisdn())) {
			blockGroupOwner.setVisibility(View.GONE);
			findViewById(R.id.empty_horizontal_space).setVisibility(View.GONE);
		} else {
			blockGroupOwner.setVisibility(View.VISIBLE);
			findViewById(R.id.empty_horizontal_space).setVisibility(
					View.VISIBLE);
			blockGroupOwner.setText(isBlocked ? R.string.unblock_owner
					: R.string.block_owner);
		}
		// Disable the add participants item
		if (activeParticipants.size() == HikeConstants.MAX_CONTACTS_IN_GROUP) {
			addParticipantsLayout.setEnabled(false);
			((TextView) findViewById(R.id.add_participants_txt))
					.setTextColor(getResources().getColor(R.color.lightgrey));
		}
		participantList.remove(userInfo.getContactInfo().getMsisdn());

		findViewById(R.id.invite_all_btn).setVisibility(
				shouldShowInviteAllButton ? View.VISIBLE : View.GONE);

		nameTxt = groupConversation.getLabel();

		// Make sure that the group name text does not exceed the permitted
		// length
		int maxLength = getResources().getInteger(
				R.integer.max_length_group_name);
		if (nameTxt.length() > maxLength) {
			nameTxt = nameTxt.substring(0, maxLength);
		}

		Drawable drawable = IconCacheManager.getInstance().getIconForMSISDN(
				groupConversation.getMsisdn());

		if (mActivityState.newBitmap == null) {
			mIconView.setImageDrawable(drawable);
		} else {
			mIconView.setImageBitmap(mActivityState.newBitmap);
		}
		mNameEdit.setText(nameTxt);
		mNameDisplay.setText(nameTxt);
		mTitleView.setText(R.string.group_info);

		mNameEdit.setVisibility(View.GONE);
		mNameDisplay.setVisibility(View.VISIBLE);
	}

	public void onTitleIconClick(View v) {
		if (profileType == ProfileType.USER_PROFILE) {
			super.onTitleIconClick(v);
			return;
		}
		if (v.getId() == R.id.title_image_btn2) {
			if (profileType == ProfileType.GROUP_INFO) {
				groupConversation.setIsMuted(!groupConversation.isMuted());
				topBarBtn
						.setImageResource(groupConversation.isMuted() ? R.drawable.ic_group_muted
								: R.drawable.ic_group_not_muted);

				HikeMessengerApp.getPubSub().publish(
						HikePubSub.MUTE_CONVERSATION_TOGGLED,
						new Pair<String, Boolean>(
								groupConversation.getMsisdn(),
								groupConversation.isMuted()));
			} else if (profileType == ProfileType.CONTACT_INFO) {
				contactInfo
						.setFavoriteType(contactInfo.getFavoriteType() == FavoriteType.FAVORITE ? FavoriteType.NOT_FAVORITE
								: FavoriteType.FAVORITE);

				((ImageView) v)
						.setImageResource(contactInfo.getFavoriteType() == FavoriteType.FAVORITE ? R.drawable.ic_favorite
								: R.drawable.ic_not_favorite);
				Pair<ContactInfo, FavoriteType> favoriteToggle = new Pair<ContactInfo, FavoriteType>(
						contactInfo, contactInfo.getFavoriteType());
				HikeMessengerApp.getPubSub().publish(
						HikePubSub.FAVORITE_TOGGLED, favoriteToggle);
			}
		}
	}

	private void setupEditScreen() {
		setContentView(R.layout.profile_edit);

		TextView mTitleView = (TextView) findViewById(R.id.title);

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

		mTitleView.setText(getResources().getString(R.string.edit_profile));
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
		afterSetContentView(savedInstanceState);

		TextView mTitleView = (TextView) findViewById(R.id.title_centered);
		TextView mNameView = (TextView) findViewById(R.id.name_current);

		ViewGroup myInfo = (ViewGroup) findViewById(R.id.my_info);
		ViewGroup notifications = (ViewGroup) findViewById(R.id.notifications);
		ViewGroup privacy = (ViewGroup) findViewById(R.id.privacy);

		myInfo.setBackgroundResource(R.drawable.profile_single_item_selector);
		notifications
				.setBackgroundResource(R.drawable.profile_top_item_selector);
		privacy.setBackgroundResource(R.drawable.profile_bottom_item_selector);

		mIconView = (ImageView) findViewById(R.id.profile);

		ViewGroup[] itemLayouts = new ViewGroup[] { notifications, privacy };

		items = new ProfileItem[] {
				new ProfileItem.ProfilePreferenceItem("Notifications",
						R.drawable.ic_notifications,
						R.xml.notification_preferences),
				new ProfileItem.ProfilePreferenceItem("Privacy",
						R.drawable.ic_privacy, R.xml.privacy_preferences) };

		for (int i = 0; i < items.length; i++) {
			items[i].createViewHolder(itemLayouts[i], items[i]);
			items[i].bindView(ProfileActivity.this, itemLayouts[i]);
		}

		notifications.findViewById(R.id.divider).setVisibility(View.GONE);

		mTitleView.setText(getResources().getString(R.string.profile_title));
		mNameView.setText(nameTxt);
		Drawable drawable = IconCacheManager.getInstance().getIconForMSISDN(
				getLargerIconId());
		mIconView.setImageDrawable(drawable);

		myInfo.setFocusable(true);
		notifications.setFocusable(true);
		privacy.setFocusable(true);
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
		if (this.profileType == ProfileType.USER_PROFILE_EDIT
				|| this.profileType == ProfileType.GROUP_INFO) {
			isBackPressed = true;
			saveChanges();
			overridePendingTransition(R.anim.slide_in_left_noalpha,
					R.anim.slide_out_right_noalpha);
		} else {
			if (this.profileType == ProfileType.USER_PROFILE) {
				super.onBackPressed();
			} else {
				finish();
			}
		}
	}

	public void onProfileItemClick(View v) {
		ProfileItem item = (ProfileItem) v.getTag(R.id.profile);
		Intent intent = item.getIntent(ProfileActivity.this);
		if (intent != null) {
			startActivity(intent);
		}
	}

	public void saveChanges() {
		ArrayList<HikeHttpRequest> requests = new ArrayList<HikeHttpRequest>();

		if (this.profileType == ProfileType.USER_PROFILE_EDIT
				&& !TextUtils.isEmpty(mEmailEdit.getText())) {
			if (!Utils.isValidEmail(mEmailEdit.getText())) {
				Toast.makeText(this,
						getResources().getString(R.string.invalid_email),
						Toast.LENGTH_LONG).show();
				return;
			}
		}

		if (mNameEdit != null && !TextUtils.isEmpty(mNameEdit.getText())
				&& !nameTxt.equals(mNameEdit.getText().toString())) {
			/* user edited the text, so update the profile */
			HikeHttpRequest request = new HikeHttpRequest(httpRequestURL
					+ "/name", new HikeHttpRequest.HikeHttpCallback() {
				public void onFailure() {
					if (isBackPressed) {
						finishEditing();
					}
				}

				public void onSuccess(JSONObject response) {
					if (ProfileActivity.this.profileType != ProfileType.GROUP_INFO) {
						/*
						 * if the request was successful, update the shared
						 * preferences and the UI
						 */
						String name = mNameEdit.getText().toString();
						Editor editor = preferences.edit();
						editor.putString(HikeMessengerApp.NAME_SETTING, name);
						editor.commit();
						HikeMessengerApp.getPubSub().publish(
								HikePubSub.PROFILE_NAME_CHANGED, null);
					} else {
						HikeConversationsDatabase hCDB = HikeConversationsDatabase
								.getInstance();
						hCDB.setGroupName(ProfileActivity.this.mLocalMSISDN,
								mNameEdit.getText().toString());
					}
					if (isBackPressed) {
						finishEditing();
					}
				}
			});

			JSONObject json = new JSONObject();
			try {
				json.put("name", mNameEdit.getText().toString());
				request.setJSONData(json);
			} catch (JSONException e) {
				Log.e("ProfileActivity", "Could not set name", e);
			}
			requests.add(request);
		}

		if (mActivityState.newBitmap != null) {
			/* the server only needs a smaller version */
			final Bitmap smallerBitmap = Util.transform(new Matrix(),
					mActivityState.newBitmap,
					HikeConstants.PROFILE_IMAGE_DIMENSIONS,
					HikeConstants.PROFILE_IMAGE_DIMENSIONS, false);
			final byte[] bytes = Utils.bitmapToBytes(smallerBitmap,
					Bitmap.CompressFormat.JPEG);

			final byte[] larger_bytes;
			if (this.profileType != ProfileType.GROUP_INFO) {
				larger_bytes = Utils.bitmapToBytes(mActivityState.newBitmap,
						Bitmap.CompressFormat.JPEG);
			} else {
				larger_bytes = null;
			}

			HikeHttpRequest request = new HikeHttpRequest(httpRequestURL
					+ "/avatar", new HikeHttpRequest.HikeHttpCallback() {
				public void onFailure() {
					Log.d("ProfileActivity", "resetting image");
					mActivityState.newBitmap = null;
					if (mIconView != null) {
						/* reset the image */
						mIconView
								.setImageDrawable(IconCacheManager
										.getInstance()
										.getIconForMSISDN(
												ProfileActivity.this.profileType != ProfileType.GROUP_INFO ? getLargerIconId()
														: mLocalMSISDN));
					}
					if (isBackPressed) {
						finishEditing();
					}
				}

				public void onSuccess(JSONObject response) {
					HikeUserDatabase db = HikeUserDatabase.getInstance();
					db.setIcon(mLocalMSISDN, bytes, false);
					if (ProfileActivity.this.profileType != ProfileType.GROUP_INFO) {
						db.setIcon(getLargerIconId(), larger_bytes, true);
						HikeMessengerApp.getPubSub().publish(
								HikePubSub.PROFILE_PIC_CHANGED, null);
					}
					if (isBackPressed) {
						finishEditing();
					}
				}
			});

			request.setPostData(bytes);
			requests.add(request);
		}

		if (this.profileType == ProfileType.USER_PROFILE_EDIT
				&& ((!emailTxt.equals(mEmailEdit.getText().toString())) || ((mActivityState.genderType != lastSavedGender)))) {
			HikeHttpRequest request = new HikeHttpRequest(httpRequestURL
					+ "/profile", new HikeHttpRequest.HikeHttpCallback() {
				public void onFailure() {
					if (isBackPressed) {
						finishEditing();
					}
				}

				public void onSuccess(JSONObject response) {
					Editor editor = preferences.edit();
					if (Utils.isValidEmail(mEmailEdit.getText())) {
						editor.putString(HikeConstants.Extras.EMAIL, mEmailEdit
								.getText().toString());
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
		if (this.profileType != ProfileType.GROUP_INFO) {
			Intent i = new Intent(this, ProfileActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
		}
		finish();
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

		mActivityState = new ActivityState();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		String path = null;
		if (resultCode != RESULT_OK) {
			return;
		}

		switch (requestCode) {
		case CAMERA_RESULT:
			/* fall-through on purpose */
		case GALLERY_RESULT:
			Log.d("ProfileActivity", "The activity is " + this);
			if (requestCode == CAMERA_RESULT) {
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
			if (requestCode == CAMERA_RESULT && !selectedFileIcon.exists()) {
				Toast.makeText(getApplicationContext(), R.string.error_capture,
						Toast.LENGTH_SHORT).show();
				return;
			}
			boolean isPicasaImage = false;
			Uri selectedFileUri = null;
			if (requestCode == CAMERA_RESULT) {
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
				startCropActivity(path);
			} else {
				mActivityState.downloadPicasaImageTask = new DownloadPicasaImageTask(
						new File(path), selectedFileUri);
				mActivityState.downloadPicasaImageTask.execute();
				mDialog = ProgressDialog.show(this, null, getResources()
						.getString(R.string.downloading_image));
			}
			break;
		case CROP_RESULT:
			mActivityState.newBitmap = data
					.getParcelableExtra(HikeConstants.Extras.BITMAP);
			if (mIconView != null) {
				mIconView.setImageBitmap(mActivityState.newBitmap);
			}
			if (this.profileType == ProfileType.USER_PROFILE) {
				saveChanges();
			}
			break;
		}
	}

	private void startCropActivity(String path) {
		/* Crop the image */
		Intent intent = new Intent(this, CropImage.class);
		intent.putExtra(HikeConstants.Extras.IMAGE_PATH, path);
		intent.putExtra(HikeConstants.Extras.SCALE, true);
		intent.putExtra(HikeConstants.Extras.OUTPUT_X, 80);
		intent.putExtra(HikeConstants.Extras.OUTPUT_Y, 80);
		intent.putExtra(HikeConstants.Extras.ASPECT_X, 1);
		intent.putExtra(HikeConstants.Extras.ASPECT_Y, 1);
		startActivityForResult(intent, CROP_RESULT);
	}

	@Override
	public void onClick(DialogInterface dialog, int item) {
		Intent intent = null;
		switch (item) {
		case PROFILE_PICTURE_FROM_CAMERA:
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

				startActivityForResult(intent, CAMERA_RESULT);
				overridePendingTransition(R.anim.slide_in_right_noalpha,
						R.anim.slide_out_left_noalpha);
			} else {
				Toast.makeText(this, getString(R.string.no_sd_card),
						Toast.LENGTH_LONG).show();
			}
			break;
		case PROFILE_PICTURE_FROM_GALLERY:
			if (Utils.getExternalStorageState() == ExternalStorageState.NONE) {
				Toast.makeText(getApplicationContext(),
						R.string.no_external_storage, Toast.LENGTH_SHORT)
						.show();
				return;
			}
			intent = new Intent(Intent.ACTION_PICK);
			intent.setType("image/*");
			startActivityForResult(intent, GALLERY_RESULT);
			overridePendingTransition(R.anim.slide_in_right_noalpha,
					R.anim.slide_out_left_noalpha);
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

	public void onChangeImageClicked(View v) {
		/*
		 * The wants to change their profile picture. Open a dialog to allow
		 * them pick Camera or Gallery
		 */
		final CharSequence[] items = { "Camera", "Gallery" };/*
															 * TODO externalize
															 * these
															 */
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Choose a picture");
		builder.setItems(items, this);
		mDialog = builder.show();
	}

	public void onEditProfileClicked(View v) {
		Utils.logEvent(ProfileActivity.this,
				HikeConstants.LogEvent.EDIT_PROFILE);
		Intent i = new Intent(ProfileActivity.this, ProfileActivity.class);
		i.putExtra(HikeConstants.Extras.EDIT_PROFILE, true);
		startActivity(i);
		finish();
	}

	public void onInviteAllClicked(View v) {
		for (Entry<String, GroupParticipant> participant : participantList
				.entrySet()) {
			if (!participant.getValue().getContactInfo().isOnhike()
					&& !participant.getValue().hasLeft()) {
				HikeMessengerApp.getPubSub().publish(
						HikePubSub.MQTT_PUBLISH,
						Utils.makeHike2SMSInviteMessage(participant.getKey(),
								this).serialize());
			}
		}
		Toast toast = Toast.makeText(ProfileActivity.this,
				R.string.invite_sent, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.BOTTOM, 0, 0);
		toast.show();
	}

	public void onAddNewParticipantsClicked(View v) {
		Utils.logEvent(ProfileActivity.this,
				HikeConstants.LogEvent.ADD_PARTICIPANT);

		Intent intent = new Intent(ProfileActivity.this, ChatThread.class);
		intent.putExtra(HikeConstants.Extras.GROUP_CHAT, true);
		intent.putExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT, mLocalMSISDN);
		startActivity(intent);

		overridePendingTransition(R.anim.slide_in_right_noalpha,
				R.anim.slide_out_left_noalpha);
	}

	public void onLeaveGroupClicked(View v) {
		Intent intent = new Intent(this, MessagesList.class);
		intent.putExtra(HikeConstants.Extras.GROUP_LEFT, mLocalMSISDN);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		finish();
		overridePendingTransition(R.anim.slide_in_left_noalpha,
				R.anim.slide_out_right_noalpha);
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

	public void onEditGroupNameClicked(View v) {
		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,
				InputMethodManager.HIDE_IMPLICIT_ONLY);
		Log.d(getClass().getSimpleName(), "ONSHOWN");
		mNameDisplay.setVisibility(View.GONE);
		mNameEdit.setVisibility(View.VISIBLE);
		mNameEdit.setSelection(mNameEdit.length());
		mNameEdit.requestFocus();
	}

	public void onAddToContactClicked(View v) {
		Utils.logEvent(this, HikeConstants.LogEvent.MENU_ADD_TO_CONTACTS);
		Intent i = new Intent(Intent.ACTION_INSERT_OR_EDIT);
		i.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
		i.putExtra(Insert.PHONE, mLocalMSISDN);
		startActivity(i);
	}

	public void onInviteToHikeClicked(View v) {
		FiksuTrackingManager.uploadPurchaseEvent(this, HikeConstants.INVITE,
				HikeConstants.INVITE_SENT, HikeConstants.CURRENCY);
		HikeMessengerApp.getPubSub().publish(
				HikePubSub.MQTT_PUBLISH,
				Utils.makeHike2SMSInviteMessage(contactInfo.getMsisdn(), this)
						.serialize());

		Toast toast = Toast.makeText(ProfileActivity.this,
				R.string.invite_sent, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.BOTTOM, 0, 0);
		toast.show();
	}

	public void onCallClicked(View v) {
		Utils.logEvent(this, HikeConstants.LogEvent.MENU_CALL);
		Intent callIntent = new Intent(Intent.ACTION_CALL);
		callIntent.setData(Uri.parse("tel:" + mLocalMSISDN));
		startActivity(callIntent);
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

	@Override
	public void onEventReceived(String type, Object object) {
		// Only execute the super class method if we are in a drawer activity
		if (profileType == ProfileType.USER_PROFILE) {
			super.onEventReceived(type, object);
		}
		if (mLocalMSISDN == null
				|| (profileType != ProfileType.GROUP_INFO && profileType != ProfileType.CONTACT_INFO)) {
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

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mNameEdit.setText(nameTxt);
						mNameDisplay.setText(nameTxt);
					}
				});
			}
		} else if (HikePubSub.ICON_CHANGED.equals(type)) {
			if (mLocalMSISDN.equals((String) object)) {
				final Drawable drawable = IconCacheManager.getInstance()
						.getIconForMSISDN(mLocalMSISDN);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mIconView.setImageDrawable(drawable);
					}
				});
			}
		}

		if (HikePubSub.PARTICIPANT_LEFT_GROUP.equals(type)) {
			if (mLocalMSISDN.equals(((JSONObject) object)
					.optString(HikeConstants.TO))) {
				final JSONObject obj = (JSONObject) object;
				this.participantList.remove(object);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						participantNameContainer
								.removeView(participantNameContainer
										.findViewById(obj.optString(
												HikeConstants.DATA).hashCode()));
						participantNameContainer.requestLayout();
					}
				});
			}
		} else if (HikePubSub.PARTICIPANT_JOINED_GROUP.equals(type)) {
			if (mLocalMSISDN.equals(((JSONObject) object)
					.optString(HikeConstants.TO))) {
				final JSONObject obj = (JSONObject) object;
				final JSONArray participants = obj
						.optJSONArray(HikeConstants.DATA);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						for (int i = 0; i < participants.length(); i++) {
							String msisdn = participants.optJSONObject(i)
									.optString(HikeConstants.MSISDN);

							HikeUserDatabase hUDB = HikeUserDatabase
									.getInstance();
							ContactInfo participant = hUDB
									.getContactInfoFromMSISDN(msisdn, false);

							if (TextUtils.isEmpty(participant.getName())) {
								HikeConversationsDatabase hCDB = HikeConversationsDatabase
										.getInstance();
								participant.setName(hCDB.getParticipantName(
										mLocalMSISDN, msisdn));
							}

							if (!participant.isOnhike()) {
								shouldShowInviteAllButton = true;
								findViewById(R.id.invite_all_btn)
										.setVisibility(View.VISIBLE);
							}

							View participantNameItem = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE))
									.inflate(R.layout.participant_name_item,
											null);
							TextView participantName = (TextView) participantNameItem
									.findViewById(R.id.participant_name);
							participantName.setText(participant.getFirstName());
							participantName
									.setTextColor(getResources()
											.getColor(
													participant.isOnhike() ? R.color.contact_blue
															: R.color.contact_green));
							participantName.setBackgroundResource(participant
									.isOnhike() ? R.drawable.hike_contact_bg
									: R.drawable.sms_contact_bg);

							LayoutParams lp = new LayoutParams(
									LayoutParams.WRAP_CONTENT,
									LayoutParams.WRAP_CONTENT);
							lp.setMargins(0, 0, 0,
									(int) (6 * Utils.densityMultiplier));
							participantNameItem.setLayoutParams(lp);

							participantNameItem.setId(msisdn.hashCode());
							participantNameContainer
									.addView(participantNameItem);

							participantList.put(msisdn, new GroupParticipant(
									participant));
						}

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
			if (!this.mLocalMSISDN.equals(contact.getMsisdn())) {
				return;
			}
			this.contactInfo = contact;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					((TextView) findViewById(R.id.name_current))
							.setText(ProfileActivity.this.contactInfo.getName());
					findViewById(R.id.add_to_contacts).setVisibility(View.GONE);

					findViewById(R.id.button_bar3).setVisibility(View.VISIBLE);
					topBarBtn.setVisibility(View.VISIBLE);
				}
			});
		} else if (HikePubSub.USER_JOINED.equals(type)
				|| HikePubSub.USER_LEFT.equals(type)) {
			if (!mLocalMSISDN.equals((String) object)) {
				return;
			}
			final boolean userJoin = HikePubSub.USER_JOINED.equals(type);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					findViewById(R.id.invite_to_hike_btn).setVisibility(
							userJoin ? View.GONE : View.VISIBLE);
				}
			});
		}
	}

	private class DownloadPicasaImageTask extends
			AsyncTask<Void, Void, Boolean> {
		private File destFile;
		private Uri picasaUri;

		public DownloadPicasaImageTask(File destFile, Uri picasaUri) {
			this.destFile = destFile;
			this.picasaUri = picasaUri;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			try {
				Utils.downloadPicasaFile(ProfileActivity.this, destFile,
						picasaUri);
				return Boolean.TRUE;
			} catch (Exception e) {
				Log.e(getClass().getSimpleName(), "Error while fetching image",
						e);
				return Boolean.FALSE;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (mDialog != null) {
				mDialog.dismiss();
				mDialog = null;
			}
			mActivityState = new ActivityState();
			if (!result) {
				Toast.makeText(getApplicationContext(),
						R.string.error_download, Toast.LENGTH_SHORT).show();
			} else {
				startCropActivity(destFile.getAbsolutePath());
			}
		}

	}
}
