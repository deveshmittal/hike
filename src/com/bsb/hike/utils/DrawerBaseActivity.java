package com.bsb.hike.utils;

import java.io.File;
import java.net.URI;
import java.util.Calendar;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager.BadTokenException;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MoodAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.HikeHttpCallback;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.tasks.DownloadImageTask;
import com.bsb.hike.tasks.DownloadImageTask.ImageDownloadResult;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.ui.CentralTimeline;
import com.bsb.hike.ui.MessagesList;
import com.bsb.hike.ui.TwitterAuthActivity;
import com.bsb.hike.view.DrawerLayout;
import com.bsb.hike.view.DrawerLayout.CurrentState;
import com.bsb.hike.view.StatusDialog;

public class DrawerBaseActivity extends AuthSocialAccountBaseActivity implements
		DrawerLayout.Listener, HikePubSub.Listener {

	private static final int IMAGE_PICK_CODE = 1991;

	public DrawerLayout parentLayout;
	private String userMsisdn;
	private ActivityTask mActivityTask;
	private Dialog statusDialog;
	private ProgressDialog progressDialog;
	private SharedPreferences preferences;

	private String[] leftDrawerPubSubListeners = {
			HikePubSub.PROFILE_PIC_CHANGED, HikePubSub.FREE_SMS_TOGGLED,
			HikePubSub.TOGGLE_REWARDS, HikePubSub.SMS_CREDIT_CHANGED,
			HikePubSub.TALK_TIME_CHANGED };

	private String[] rightDrawerPubSubListeners = { HikePubSub.ICON_CHANGED,
			HikePubSub.RECENT_CONTACTS_UPDATED, HikePubSub.FAVORITE_TOGGLED,
			HikePubSub.USER_JOINED, HikePubSub.USER_LEFT,
			HikePubSub.CONTACT_ADDED, HikePubSub.REFRESH_FAVORITES,
			HikePubSub.REFRESH_RECENTS, HikePubSub.SHOW_STATUS_DIALOG,
			HikePubSub.MY_STATUS_CHANGED, HikePubSub.FRIEND_REQUEST_ACCEPTED,
			HikePubSub.REJECT_FRIEND_REQUEST, HikePubSub.SOCIAL_AUTH_COMPLETED,
			HikePubSub.SOCIAL_AUTH_FAILED };

	private ImageView pageSelected;

	private class ActivityTask {
		boolean showingStatusDialog = false;
		int moodId = -1;
		String filePath = null;
		DownloadImageTask downloadPicasaImageTask = null;
		Bitmap filePreview = null;
		HikeHTTPTask hikeHTTPTask = null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// This does not apply to pre-Honeycomb devices,
		if (Build.VERSION.SDK_INT >= 11) {
			getWindow().setFlags(HikeConstants.FLAG_HARDWARE_ACCELERATED,
					HikeConstants.FLAG_HARDWARE_ACCELERATED);
		}
		preferences = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
				MODE_PRIVATE);
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return mActivityTask;
	}

	public void afterSetContentView(Bundle savedInstanceState) {
		afterSetContentView(savedInstanceState, true);
	}

	public void afterSetContentView(Bundle savedInstanceState,
			boolean showButtons) {
		parentLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		parentLayout.setListener(this);
		parentLayout.setUpLeftDrawerView();

		if (showButtons) {
			findViewById(R.id.topbar_menu).setVisibility(View.VISIBLE);
			findViewById(R.id.menu_bar).setVisibility(View.VISIBLE);
		}

		HikeMessengerApp.getPubSub().addListeners(this,
				leftDrawerPubSubListeners);

		/*
		 * Only show the favorites drawer in the Messages list screen
		 */
		if ((this instanceof MessagesList) || (this instanceof CentralTimeline)) {
			parentLayout.setUpRightDrawerView(this);

			if (showButtons) {
				ImageButton rightFavoriteBtn = (ImageButton) findViewById(R.id.title_image_btn2);
				rightFavoriteBtn.setVisibility(View.VISIBLE);
				rightFavoriteBtn.setImageResource(R.drawable.ic_favorites);

				findViewById(R.id.title_image_btn2_container).setVisibility(
						View.VISIBLE);

				findViewById(R.id.button_bar3).setVisibility(View.VISIBLE);
			}
			HikeMessengerApp.getPubSub().addListeners(this,
					rightDrawerPubSubListeners);
		}

		if (savedInstanceState != null) {
			if (savedInstanceState
					.getBoolean(HikeConstants.Extras.IS_LEFT_DRAWER_VISIBLE)) {
				parentLayout.toggleSidebar(true, true);
			} else if (savedInstanceState
					.getBoolean(HikeConstants.Extras.IS_RIGHT_DRAWER_VISIBLE)) {
				parentLayout.toggleSidebar(true, false);
			}
		}

		userMsisdn = preferences.getString(HikeMessengerApp.MSISDN_SETTING, "");

		Object o = getLastNonConfigurationInstance();

		if (o instanceof ActivityTask) {
			mActivityTask = (ActivityTask) o;
			if (mActivityTask.downloadPicasaImageTask != null) {
				progressDialog = ProgressDialog.show(this, null, getResources()
						.getString(R.string.downloading_image));
			}
			if (mActivityTask.showingStatusDialog) {
				showStatusDialog(mActivityTask.filePath != null);
			}
			if (mActivityTask.hikeHTTPTask != null) {
				progressDialog = ProgressDialog.show(this, null, getResources()
						.getString(R.string.updating_status));
			}
		} else {
			mActivityTask = new ActivityTask();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		HikeMessengerApp.getPubSub().removeListeners(this,
				leftDrawerPubSubListeners);
		if ((this instanceof MessagesList)) {
			HikeMessengerApp.getPubSub().removeListeners(this,
					rightDrawerPubSubListeners);
		}
		if (progressDialog != null) {
			progressDialog.dismiss();
			progressDialog = null;
		}
		if (statusDialog != null) {
			statusDialog.dismiss();
			statusDialog = null;
			mActivityTask.moodId = -1;
		}
	}

	public void onToggleLeftSideBarClicked(View v) {
		Utils.logEvent(this, HikeConstants.LogEvent.DRAWER_BUTTON);
		parentLayout.toggleSidebar(false, true);
	}

	public void onTitleIconClick(View v) {
		Utils.logEvent(this, HikeConstants.LogEvent.DRAWER_BUTTON);
		parentLayout.toggleSidebar(false, false);
	}

	public void onEmptySpaceClicked(View v) {
		parentLayout.closeSidebar(false);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(HikeConstants.Extras.IS_LEFT_DRAWER_VISIBLE,
				this.parentLayout != null
						&& parentLayout.getCurrentState() == CurrentState.LEFT);
		outState.putBoolean(HikeConstants.Extras.IS_RIGHT_DRAWER_VISIBLE,
				this.parentLayout != null
						&& parentLayout.getCurrentState() == CurrentState.RIGHT);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onBackPressed() {
		if (parentLayout.getCurrentState() != CurrentState.NONE) {
			parentLayout.closeSidebar(false);
		} else {
			if (!(this instanceof MessagesList)
					&& !getIntent().getBooleanExtra(
							HikeConstants.Extras.FROM_CENTRAL_TIMELINE, false)) {
				Intent intent = new Intent(this, MessagesList.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				if (this instanceof CentralTimeline) {
					finish();
					overridePendingTransition(R.anim.no_animation,
							R.anim.slide_down_noalpha);
				} else {
					overridePendingTransition(R.anim.alpha_in,
							R.anim.slide_out_right_noalpha);
				}
			} else {
				finish();
			}
		}
	}

	@Override
	public boolean onContentTouchedWhenOpeningLeftSidebar() {
		parentLayout.closeSidebar(false);
		return true;
	}

	@Override
	public boolean onContentTouchedWhenOpeningRightSidebar() {
		parentLayout.closeSidebar(false);
		return true;
	}

	@Override
	public void rightSidebarOpened() {
	}

	@Override
	public void onEventReceived(final String type, final Object object) {
		if (HikePubSub.SMS_CREDIT_CHANGED.equals(type)) {
			final int credits = (Integer) object;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					parentLayout.updateCredits(credits);
				}
			});
		} else if (HikePubSub.PROFILE_PIC_CHANGED.equals(type)) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					parentLayout.setProfileImage();
				}
			});
		} else if (HikePubSub.FREE_SMS_TOGGLED.equals(type)) {
			final boolean freeSMSOn = (Boolean) object;
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					parentLayout.renderLeftDrawerItems(freeSMSOn);
					parentLayout.freeSMSToggled(freeSMSOn);
				}
			});
		} else if (HikePubSub.TOGGLE_REWARDS.equals(type)) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					parentLayout.setUpLeftDrawerView();
				}
			});
		} else if (HikePubSub.TALK_TIME_CHANGED.equals(type)) {
			final int talkTime = (Integer) object;
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					parentLayout.updateTalkTime(talkTime);
				}
			});
		} else if (HikePubSub.ICON_CHANGED.equals(type)) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					parentLayout.refreshFavoritesDrawer();
				}
			});
		} else if (HikePubSub.RECENT_CONTACTS_UPDATED.equals(type)
				|| HikePubSub.USER_JOINED.equals(type)
				|| HikePubSub.USER_LEFT.equals(type)) {
			final ContactInfo contactInfo = HikeUserDatabase.getInstance()
					.getContactInfoFromMSISDN((String) object, true);

			if (contactInfo == null
					|| (HikePubSub.RECENT_CONTACTS_UPDATED.equals(type) && contactInfo
							.isOnhike())) {
				return;
			}
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					parentLayout.updateRecentContacts(contactInfo);
				}
			});
		} else if (HikePubSub.FAVORITE_TOGGLED.equals(type)
				|| HikePubSub.FRIEND_REQUEST_ACCEPTED.equals(type)
				|| HikePubSub.REJECT_FRIEND_REQUEST.equals(type)) {
			final Pair<ContactInfo, FavoriteType> favoriteToggle = (Pair<ContactInfo, FavoriteType>) object;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					FavoriteType favoriteType = favoriteToggle.second;
					ContactInfo contactInfo = favoriteToggle.first;
					contactInfo.setFavoriteType(favoriteType);
					if ((favoriteType == FavoriteType.FRIEND)
							|| (favoriteType == FavoriteType.REQUEST_SENT)) {
						parentLayout.addToFavorite(contactInfo);
					} else if (favoriteType == FavoriteType.NOT_FRIEND
							|| favoriteType == FavoriteType.REQUEST_RECEIVED_REJECTED) {
						parentLayout.removeFromFavorite(contactInfo);
					}
				}
			});
		} else if (HikePubSub.CONTACT_ADDED.equals(type)) {
			final ContactInfo contactInfo = (ContactInfo) object;

			if (contactInfo == null) {
				return;
			}

			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if ((contactInfo.getFavoriteType() != FavoriteType.FRIEND)
							&& (contactInfo.getFavoriteType() != FavoriteType.REQUEST_SENT)) {
						parentLayout.updateRecentContacts(contactInfo);
					} else {
						parentLayout.addToFavorite(contactInfo);
					}
				}
			});
		} else if (HikePubSub.REFRESH_FAVORITES.equals(type)) {
			String myMsisdn = preferences.getString(
					HikeMessengerApp.MSISDN_SETTING, "");

			HikeUserDatabase hikeUserDatabase = HikeUserDatabase.getInstance();

			final List<ContactInfo> favoriteList = hikeUserDatabase
					.getContactsOfFavoriteType(FavoriteType.FRIEND,
							HikeConstants.BOTH_VALUE, myMsisdn);
			favoriteList.addAll(hikeUserDatabase.getContactsOfFavoriteType(
					FavoriteType.REQUEST_SENT, HikeConstants.BOTH_VALUE,
					myMsisdn));
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					parentLayout.refreshFavorites(favoriteList);
				}
			});
		} else if (HikePubSub.REFRESH_RECENTS.equals(type)) {
			final List<ContactInfo> recentList = HikeUserDatabase.getInstance()
					.getNonHikeRecentContacts(-1,
							HikeMessengerApp.isIndianUser(),
							FavoriteType.NOT_FRIEND);
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					parentLayout.refreshRecents(recentList);
				}
			});
		} else if (HikePubSub.SHOW_STATUS_DIALOG.equals(type)) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					showStatusDialog(false);
				}
			});
		} else if (HikePubSub.MY_STATUS_CHANGED.equals(type)) {
			final String status = (String) object;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					parentLayout.updateStatus(status);
				}
			});
		} else if (HikePubSub.SOCIAL_AUTH_COMPLETED.equals(type)
				|| HikePubSub.SOCIAL_AUTH_FAILED.equals(type)) {
			final boolean facebook = (Boolean) object;
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					setSelectionSocialButton(facebook,
							HikePubSub.SOCIAL_AUTH_COMPLETED.equals(type));
				}
			});
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (v.getId() != R.id.favorite_list) {
			return;
		}

		/* enable resend options on failed messages */
		AdapterView.AdapterContextMenuInfo adapterInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
		parentLayout.onCreateFavoritesContextMenu(this, menu,
				adapterInfo.position);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		return parentLayout.onFavoritesContextItemSelected(item);
	}

	public void showStatusDialog(boolean hasSelectedFile) {
		statusDialog = new StatusDialog(this, R.style.Theme_CustomDialog_Status);
		statusDialog.setContentView(R.layout.status_dialog);
		LayoutParams dialogParams = statusDialog.getWindow().getAttributes();
		dialogParams.gravity = Gravity.TOP;
		statusDialog.getWindow().setAttributes(dialogParams);

		ViewGroup parent = (ViewGroup) statusDialog
				.findViewById(R.id.parent_layout);

		int screenHeight = getResources().getDisplayMetrics().heightPixels;
		int dialogHeight = (int) (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? screenHeight / 2
				: FrameLayout.LayoutParams.MATCH_PARENT);

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			dialogHeight += ((int) 20 * Utils.densityMultiplier);
		}

		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.MATCH_PARENT, dialogHeight);
		lp.topMargin = (int) (5 * Utils.densityMultiplier);
		lp.bottomMargin = (int) (5 * Utils.densityMultiplier);
		lp.leftMargin = (int) (5 * Utils.densityMultiplier);
		lp.rightMargin = (int) (5 * Utils.densityMultiplier);
		parent.setLayoutParams(lp);

		final Button titleBtn = (Button) statusDialog
				.findViewById(R.id.title_icon);
		titleBtn.setText(R.string.post);
		titleBtn.setEnabled(false);
		titleBtn.setVisibility(View.VISIBLE);

		statusDialog.findViewById(R.id.button_bar_2)
				.setVisibility(View.VISIBLE);

		final TextView mTitleView = (TextView) statusDialog.findViewById(R.id.title);
		mTitleView.setText(R.string.status);

		ImageView avatar = (ImageView) statusDialog.findViewById(R.id.avatar);
		avatar.setImageDrawable(IconCacheManager.getInstance()
				.getIconForMSISDN(userMsisdn));

		// ImageButton insertImgBtn = (ImageButton) statusDialog
		// .findViewById(R.id.insert_img_btn);
		ImageButton fbPostBtn = (ImageButton) statusDialog
				.findViewById(R.id.post_fb_btn);
		ImageButton twitterPostBtn = (ImageButton) statusDialog
				.findViewById(R.id.post_twitter_btn);
		ImageButton moodBtn = (ImageButton) statusDialog
				.findViewById(R.id.mood_btn);

		final TextView charCounter = (TextView) statusDialog
				.findViewById(R.id.char_counter);

		final EditText statusTxt = (EditText) statusDialog
				.findViewById(R.id.status_txt);

		String statusHint = getString(R.string.whats_up_user,
				Utils.getFirstName(preferences.getString(
						HikeMessengerApp.NAME_SETTING, "")));

		statusTxt.setHint(statusHint);
		statusTxt.setText("");

		charCounter.setText(Integer
				.toString(HikeConstants.MAX_TWITTER_POST_LENGTH
						- statusTxt.length()));

		statusTxt.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				toggleEnablePostButton();
				charCounter.setText(Integer
						.toString(HikeConstants.MAX_TWITTER_POST_LENGTH
								- s.length()));
			}
		});

		/*
		 * The app would randomly crash here. We catch that particular
		 * exception. Behavior of the app remains the same even after catching
		 * this exception
		 */
		try {
			statusDialog.show();
		} catch (BadTokenException e) {
		}
		mActivityTask.showingStatusDialog = true;

		statusDialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				mActivityTask = new ActivityTask();
			}
		});

		OnClickListener statusDialogListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(getClass().getSimpleName(), "Onclick event");
				switch (v.getId()) {
				case R.id.post_fb_btn:
					v.setSelected(!v.isSelected());
					if (!v.isSelected()
							|| preferences.getBoolean(
									HikeMessengerApp.FACEBOOK_AUTH_COMPLETE,
									false)) {
						return;
					}
					startFBAuth(false);
					break;
				case R.id.post_twitter_btn:
					setSelectionSocialButton(false, !v.isSelected());

					if (!v.isSelected()
							|| preferences.getBoolean(
									HikeMessengerApp.TWITTER_AUTH_COMPLETE,
									false)) {
						return;
					}
					startActivity(new Intent(DrawerBaseActivity.this,
							TwitterAuthActivity.class));
					break;
				case R.id.title_icon:
					if ((statusDialog.findViewById(R.id.mood_parent)
							.getVisibility() == View.VISIBLE)) {
						statusDialog.onBackPressed();
						mTitleView.setText(R.string.status_update);
					} else {
						postStatus();
					}
					break;
				case R.id.mood_btn:
					showMoodSelector();
					break;
				}
			}
		};

		// insertImgBtn.setOnClickListener(statusDialogListener);
		fbPostBtn.setOnClickListener(statusDialogListener);
		twitterPostBtn.setOnClickListener(statusDialogListener);
		titleBtn.setOnClickListener(statusDialogListener);
		moodBtn.setOnClickListener(statusDialogListener);

		if (hasSelectedFile) {
			showFilePreview();
		}
		toggleEnablePostButton();
	}

	private void postStatus() {
		final EditText statusTxt = (EditText) statusDialog
				.findViewById(R.id.status_txt);
		HikeHttpRequest hikeHttpRequest = new HikeHttpRequest("/user/status",
				RequestType.STATUS_UPDATE, new HikeHttpCallback() {

					@Override
					public void onSuccess(JSONObject response) {
						if (progressDialog != null) {
							progressDialog.dismiss();
							progressDialog = null;
						}
						JSONObject data = response.optJSONObject("data");

						String mappedId = data
								.optString(HikeConstants.STATUS_ID);
						String text = data
								.optString(HikeConstants.STATUS_MESSAGE);
						int moodId = data.optInt(HikeConstants.MOOD) - 1;
						int timeOfDay = data.optInt(HikeConstants.TIME_OF_DAY);
						String msisdn = preferences.getString(
								HikeMessengerApp.MSISDN_SETTING, "");
						String name = preferences.getString(
								HikeMessengerApp.NAME_SETTING, "");
						long time = (long) System.currentTimeMillis() / 1000;

						StatusMessage statusMessage = new StatusMessage(0,
								mappedId, msisdn, name, text,
								StatusMessageType.TEXT, time, moodId, timeOfDay);
						HikeConversationsDatabase.getInstance()
								.addStatusMessage(statusMessage, true);

						int unseenUserStatusCount = preferences.getInt(
								HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, 0);
						Editor editor = preferences.edit();
						editor.putString(HikeMessengerApp.LAST_STATUS, text);
						editor.putInt(
								HikeMessengerApp.UNSEEN_USER_STATUS_COUNT,
								++unseenUserStatusCount);
						editor.commit();

						HikeMessengerApp.getPubSub().publish(
								HikePubSub.MY_STATUS_CHANGED, text);
						statusDialog.cancel();

						/*
						 * This would happen in the case where the user has
						 * added a self contact and received an mqtt message
						 * before saving this to the db.
						 */
						if (statusMessage.getId() != -1) {
							HikeMessengerApp.getPubSub().publish(
									HikePubSub.STATUS_MESSAGE_RECEIVED,
									statusMessage);
							HikeMessengerApp.getPubSub().publish(
									HikePubSub.TIMELINE_UPDATE_RECIEVED,
									statusMessage);
						}
						statusTxt.setText("");
					}

					@Override
					public void onFailure() {
						if (progressDialog != null) {
							progressDialog.dismiss();
							progressDialog = null;
						}
						Toast.makeText(getApplicationContext(),
								R.string.update_status_fail, Toast.LENGTH_SHORT)
								.show();
						mActivityTask.hikeHTTPTask = null;
					}

				});
		String status = null;
		/*
		 * If the text box is empty, the we take the hint text which is a
		 * prefill for moods.
		 */
		if (TextUtils.isEmpty(statusTxt.getText())) {
			status = statusTxt.getHint().toString();
		} else {
			status = statusTxt.getText().toString();
		}

		boolean facebook = statusDialog.findViewById(R.id.post_fb_btn)
				.isSelected();
		boolean twitter = statusDialog.findViewById(R.id.post_twitter_btn)
				.isSelected();

		Log.d(getClass().getSimpleName(), "Status: " + status);
		JSONObject data = new JSONObject();
		try {
			data.put(HikeConstants.STATUS_MESSAGE_2, status);
			data.put(HikeConstants.FACEBOOK_STATUS, facebook);
			data.put(HikeConstants.TWITTER_STATUS, twitter);
			if (mActivityTask.moodId != -1) {
				data.put(HikeConstants.MOOD, mActivityTask.moodId + 1);
				data.put(HikeConstants.TIME_OF_DAY, getTimeOfDay());
			}
		} catch (JSONException e) {
			Log.w(getClass().getSimpleName(), "Invalid JSON", e);
		}
		Log.d(getClass().getSimpleName(), "JSON: " + data);

		hikeHttpRequest.setJSONData(data);
		mActivityTask.hikeHTTPTask = new HikeHTTPTask(null, 0);
		mActivityTask.hikeHTTPTask.execute(hikeHttpRequest);

		progressDialog = ProgressDialog.show(DrawerBaseActivity.this, null,
				getResources().getString(R.string.updating_status));
	}

	private void setSelectionSocialButton(boolean facebook, boolean selection) {
		if (statusDialog == null || !statusDialog.isShowing()) {
			return;
		}
		View v = statusDialog.findViewById(facebook ? R.id.post_fb_btn
				: R.id.post_twitter_btn);
		v.setSelected(selection);
		if (!facebook) {
			setCharCountForStatus(statusDialog.findViewById(R.id.char_counter),
					(EditText) statusDialog.findViewById(R.id.status_txt),
					v.isSelected());
		}
	}

	private void setCharCountForStatus(View charCounter, EditText statusTxt,
			boolean isSelected) {
		charCounter.setVisibility(isSelected ? View.VISIBLE : View.GONE);

		if (isSelected) {
			statusTxt
					.setFilters(new InputFilter[] { new InputFilter.LengthFilter(
							HikeConstants.MAX_TWITTER_POST_LENGTH) });
		} else {
			statusTxt.setFilters(new InputFilter[] {});
		}
	}

	private void showMoodSelector() {
		if (statusDialog == null || !statusDialog.isShowing()) {
			return;
		}
		showCancelButton(true);

		ViewGroup moodParent = (ViewGroup) statusDialog
				.findViewById(R.id.mood_parent);
		final ViewGroup pageIndicatorContainer = (ViewGroup) statusDialog
				.findViewById(R.id.mood_page_indicator_container);
		ViewPager moodPager = (ViewPager) statusDialog
				.findViewById(R.id.mood_pager);

		moodParent.setVisibility(View.VISIBLE);

		MoodAdapter moodAdapter = new MoodAdapter(this);
		moodPager.setAdapter(moodAdapter);

		int indicatorCount = moodAdapter.getCount();
		addPageIndicators(pageIndicatorContainer, indicatorCount);

		moodPager.setOnPageChangeListener(new IndicatorChanger(
				pageIndicatorContainer));
	}

	private class IndicatorChanger implements OnPageChangeListener {

		private View pageIndicatorContainer;

		public IndicatorChanger(View v) {
			this.pageIndicatorContainer = v;
		}

		@Override
		public void onPageSelected(int pageNum) {
			ImageView iv = (ImageView) pageIndicatorContainer
					.findViewById(pageNum);

			iv.setImageResource(R.drawable.mood_page_indicator_selected);
			pageSelected
					.setImageResource(R.drawable.mood_page_indicator_unselected);

			pageSelected = iv;
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		@Override
		public void onPageScrollStateChanged(int arg0) {
		}
	}

	public void setMood(int moodId) {
		if (statusDialog == null || moodId == -1) {
			return;
		}
		mActivityTask.moodId = moodId;

		EditText statusTxt = (EditText) statusDialog
				.findViewById(R.id.status_txt);
		ImageView avatar = (ImageView) statusDialog.findViewById(R.id.avatar);
		avatar.setImageResource(EmoticonConstants.MOOD_RES_IDS[moodId]);

		int timeOfDay = getTimeOfDay();
		String[] prefillTextArray;
		if (timeOfDay == 1) {
			prefillTextArray = getResources().getStringArray(
					R.array.mood_prefills_morning);
		} else if (timeOfDay == 2) {
			prefillTextArray = getResources().getStringArray(
					R.array.mood_prefills_day);
		} else {
			prefillTextArray = getResources().getStringArray(
					R.array.mood_prefills_night);
		}

		if (moodId < prefillTextArray.length) {
			statusTxt.setHint(prefillTextArray[moodId]);
		}
		toggleEnablePostButton();
		statusDialog.onBackPressed();
	}

	private void addPageIndicators(ViewGroup pageIndicatorContainer,
			int indicatorCount) {
		int rightMargin = (int) (5 * Utils.densityMultiplier);
		int indicatorSize = (int) (5 * Utils.densityMultiplier);

		pageIndicatorContainer.removeAllViews();

		for (int i = 0; i < indicatorCount; i++) {
			ImageView iv = new ImageView(this);
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
					indicatorSize, indicatorSize);

			if (i != indicatorCount - 1) {
				layoutParams.rightMargin = rightMargin;
			}

			iv.setLayoutParams(layoutParams);
			iv.setImageResource(i == 0 ? R.drawable.mood_page_indicator_selected
					: R.drawable.mood_page_indicator_unselected);

			if (i == 0) {
				pageSelected = iv;
			}

			iv.setId(i);
			pageIndicatorContainer.addView(iv);
		}
	}

	private void showCancelButton(boolean moodLayout) {
		Button titleBtn = (Button) statusDialog.findViewById(R.id.title_icon);
		titleBtn.setText(R.string.cancel);
		titleBtn.setEnabled(true);
		if (moodLayout) {
			TextView mTitleView = (TextView) statusDialog
					.findViewById(R.id.title);
			mTitleView.setText(R.string.moods);
		}
	}

	private int getTimeOfDay() {
		int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		if (hour >= 4 && hour < 12) {
			return 1;
		} else if (hour >= 12 && hour < 8) {
			return 2;
		}
		return 3;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if ((requestCode == IMAGE_PICK_CODE) && resultCode == RESULT_OK) {
			File selectedFile = null;

			if (requestCode == IMAGE_PICK_CODE) {
				selectedFile = new File(preferences.getString(
						HikeMessengerApp.FILE_PATH, ""));

				clearTempData();
			}
			if (data == null
					&& (selectedFile == null || !selectedFile.exists())) {
				Toast.makeText(getApplicationContext(), R.string.error_capture,
						Toast.LENGTH_SHORT).show();
				return;
			}

			String filePath = null;
			if (data == null || data.getData() == null) {
				filePath = selectedFile.getAbsolutePath();
			} else {
				Uri selectedFileUri = Utils.makePicasaUri(data.getData());

				if (Utils.isPicasaUri(selectedFileUri.toString())) {
					final File destFile = selectedFile;
					// Picasa image
					mActivityTask.downloadPicasaImageTask = new DownloadImageTask(
							getApplicationContext(), destFile, selectedFileUri,
							new ImageDownloadResult() {
								@Override
								public void downloadFinished(boolean result) {
									mActivityTask.downloadPicasaImageTask = null;
									if (result) {
										mActivityTask.filePath = destFile
												.getPath();
										showFilePreview();
									} else {
										Toast.makeText(DrawerBaseActivity.this,
												R.string.error_download,
												Toast.LENGTH_SHORT).show();
									}
								}
							});
					progressDialog = ProgressDialog.show(this, null,
							getResources()
									.getString(R.string.downloading_image));
					return;
				} else {
					String fileUriStart = "file://";
					String fileUriString = selectedFileUri.toString();
					if (fileUriString.startsWith(fileUriStart)) {
						selectedFile = new File(URI.create(fileUriString));
						/*
						 * Done to fix the issue in a few Sony devices.
						 */
						filePath = selectedFile.getAbsolutePath();
					} else {
						filePath = Utils.getRealPathFromUri(selectedFileUri,
								this);
					}
					Log.d(getClass().getSimpleName(), "File path: " + filePath);
				}
			}

			mActivityTask.filePath = filePath;
			showFilePreview();
		} else if (resultCode == RESULT_CANCELED) {
			if (requestCode == AuthSocialAccountBaseActivity.FB_AUTH_REQUEST_CODE) {
				setSelectionSocialButton(true, false);
			} else {
				clearTempData();
			}
		}
	}

	private void showFilePreview() {
		if (statusDialog == null) {
			return;
		}
		mActivityTask.filePreview = Utils.scaleDownImage(
				mActivityTask.filePath, HikeConstants.PROFILE_IMAGE_DIMENSIONS,
				true);

		final ImageView filePreview = (ImageView) statusDialog
				.findViewById(R.id.img_inserted);
		final ImageView removeImgBtn = (ImageView) statusDialog
				.findViewById(R.id.remove_img);

		filePreview.setImageBitmap(mActivityTask.filePreview);

		filePreview.setVisibility(View.VISIBLE);
		removeImgBtn.setVisibility(View.VISIBLE);

		filePreview.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				removeImgBtn.setVisibility(View.GONE);
				filePreview.setVisibility(View.GONE);

				mActivityTask.filePath = null;
				mActivityTask.filePreview = null;

				toggleEnablePostButton();
			}
		});
		toggleEnablePostButton();
	}

	private void clearTempData() {
		Editor editor = preferences.edit();
		editor.remove(HikeMessengerApp.TEMP_NAME);
		editor.remove(HikeMessengerApp.TEMP_NUM);
		editor.commit();
	}

	private void toggleEnablePostButton() {
		if (statusDialog == null) {
			return;
		}
		Button titleBtn = (Button) statusDialog.findViewById(R.id.title_icon);
		EditText statusTxt = (EditText) statusDialog
				.findViewById(R.id.status_txt);

		/*
		 * Enabling if the text length is > 0 or if the user has selected a mood
		 * with some prefilled text.
		 */
		titleBtn.setEnabled((mActivityTask.moodId >= 0 && mActivityTask.moodId < 11)
				|| (statusTxt.length() > 0));
	}
}
