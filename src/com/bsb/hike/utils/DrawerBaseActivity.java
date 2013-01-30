package com.bsb.hike.utils;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.HikeHttpCallback;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.tasks.DownloadPicasaImageTask;
import com.bsb.hike.tasks.DownloadPicasaImageTask.PicasaDownloadResult;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.ui.CentralTimeline;
import com.bsb.hike.ui.MessagesList;
import com.bsb.hike.view.DrawerLayout;

public class DrawerBaseActivity extends Activity implements
		DrawerLayout.Listener, HikePubSub.Listener {

	private static final long DELAY_BEFORE_ENABLE_ANIMATION = 1000;

	private static final int IMAGE_PICK_CODE = 1991;

	public DrawerLayout parentLayout;
	private long waitTime;
	private String userMsisdn;
	private ActivityTask mActivityTask;
	private Dialog statusDialog;
	private ProgressDialog progressDialog;

	private String[] leftDrawerPubSubListeners = {
			HikePubSub.PROFILE_PIC_CHANGED, HikePubSub.FREE_SMS_TOGGLED,
			HikePubSub.TOGGLE_REWARDS };

	private String[] rightDrawerPubSubListeners = { HikePubSub.ICON_CHANGED,
			HikePubSub.RECENT_CONTACTS_UPDATED, HikePubSub.FAVORITE_TOGGLED,
			HikePubSub.USER_JOINED, HikePubSub.USER_LEFT,
			HikePubSub.CONTACT_ADDED, HikePubSub.REFRESH_FAVORITES,
			HikePubSub.REFRESH_RECENTS, HikePubSub.SHOW_STATUS_DIALOG,
			HikePubSub.MY_STATUS_CHANGED };

	private class ActivityTask {
		boolean showingStatusDialog = false;
		String filePath = null;
		DownloadPicasaImageTask downloadPicasaImageTask = null;
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
	}

	@Override
	protected void onStart() {
		super.onStart();
		waitTime = System.currentTimeMillis();
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

		userMsisdn = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
				MODE_PRIVATE).getString(HikeMessengerApp.MSISDN_SETTING, "");

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
		}
	}

	public void onToggleLeftSideBarClicked(View v) {
		/*
		 * Adding a delay before we enable the animation. Otherwise the
		 * animation would randomly over-shoot or under-shoot
		 */
		if (System.currentTimeMillis() - waitTime <= DELAY_BEFORE_ENABLE_ANIMATION) {
			return;
		}
		Utils.logEvent(this, HikeConstants.LogEvent.DRAWER_BUTTON);
		parentLayout.toggleSidebar(false, true);
	}

	public void onTitleIconClick(View v) {
		/*
		 * Adding a delay before we enable the animation. Otherwise the
		 * animation would randomly over-shoot or under-shoot
		 */
		if (System.currentTimeMillis() - waitTime <= DELAY_BEFORE_ENABLE_ANIMATION) {
			return;
		}
		Utils.logEvent(this, HikeConstants.LogEvent.DRAWER_BUTTON);
		parentLayout.toggleSidebar(false, false);
	}

	public void onEmptySpaceClicked(View v) {
		parentLayout.closeLeftSidebar(false);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(HikeConstants.Extras.IS_LEFT_DRAWER_VISIBLE,
				this.parentLayout != null && this.parentLayout.isLeftOpening());
		outState.putBoolean(HikeConstants.Extras.IS_RIGHT_DRAWER_VISIBLE,
				this.parentLayout != null && this.parentLayout.isRightOpening());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onBackPressed() {
		if (parentLayout.isLeftOpening()) {
			parentLayout.closeLeftSidebar(false);
		} else if (parentLayout.isRightOpening()) {
			parentLayout.closeRightSidebar(false);
		} else {
			if (!(this instanceof MessagesList)) {
				Intent intent = new Intent(this, MessagesList.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				if (this instanceof CentralTimeline) {
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
		parentLayout.closeLeftSidebar(false);
		return true;
	}

	@Override
	public boolean onContentTouchedWhenOpeningRightSidebar() {
		parentLayout.closeRightSidebar(false);
		return true;
	}

	@Override
	public void rightSidebarOpened() {
	}

	@Override
	public void onEventReceived(String type, final Object object) {
		if (HikePubSub.PROFILE_PIC_CHANGED.equals(type)) {
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
		} else if (HikePubSub.FAVORITE_TOGGLED.equals(type)) {
			final Pair<ContactInfo, FavoriteType> favoriteToggle = (Pair<ContactInfo, FavoriteType>) object;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					FavoriteType favoriteType = favoriteToggle.second;
					ContactInfo contactInfo = favoriteToggle.first;
					if (favoriteType == FavoriteType.FAVORITE) {
						contactInfo.setFavoriteType(FavoriteType.FAVORITE);
						parentLayout.addToFavorite(contactInfo);
					} else if (favoriteType == FavoriteType.NOT_FAVORITE) {
						contactInfo.setFavoriteType(FavoriteType.NOT_FAVORITE);
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
					if (contactInfo.getFavoriteType() != FavoriteType.FAVORITE) {
						parentLayout.updateRecentContacts(contactInfo);
					} else {
						parentLayout.addToFavorite(contactInfo);
					}
				}
			});
		} else if (HikePubSub.REFRESH_FAVORITES.equals(type)) {
			HikeUserDatabase hikeUserDatabase = HikeUserDatabase.getInstance();

			final List<ContactInfo> favoriteList = hikeUserDatabase
					.getContactsOfFavoriteType(FavoriteType.FAVORITE,
							HikeConstants.BOTH_VALUE);
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
							FavoriteType.NOT_FAVORITE);
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
		statusDialog = new Dialog(DrawerBaseActivity.this,
				R.style.Theme_CustomDialog_Status);
		statusDialog.setContentView(R.layout.status_dialog);

		final Button titleBtn = (Button) statusDialog
				.findViewById(R.id.title_icon);
		titleBtn.setText("Post");
		titleBtn.setEnabled(false);
		titleBtn.setVisibility(View.VISIBLE);

		statusDialog.findViewById(R.id.button_bar_2)
				.setVisibility(View.VISIBLE);

		TextView mTitleView = (TextView) statusDialog.findViewById(R.id.title);
		mTitleView.setText("Status");

		ImageView avatar = (ImageView) statusDialog.findViewById(R.id.avatar);
		avatar.setImageDrawable(IconCacheManager.getInstance()
				.getIconForMSISDN(userMsisdn));

		ImageButton insertImgBtn = (ImageButton) statusDialog
				.findViewById(R.id.insert_img_btn);
		ImageButton fbPostBtn = (ImageButton) statusDialog
				.findViewById(R.id.post_fb_btn);
		ImageButton twitterPostBtn = (ImageButton) statusDialog
				.findViewById(R.id.post_twitter_btn);

		final EditText statusTxt = (EditText) statusDialog
				.findViewById(R.id.status_txt);
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
				titleBtn.setEnabled((mActivityTask.filePath != null)
						|| (s.length() > 0));
			}
		});

		statusDialog.show();
		mActivityTask.showingStatusDialog = true;

		statusDialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				mActivityTask.showingStatusDialog = false;
			}
		});

		OnClickListener statusDialogListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(getClass().getSimpleName(), "Onclick event");
				switch (v.getId()) {
				case R.id.insert_img_btn:

					// if (Utils.getExternalStorageState() ==
					// ExternalStorageState.NONE) {
					// Toast.makeText(getApplicationContext(),
					// R.string.no_external_storage,
					// Toast.LENGTH_SHORT).show();
					// return;
					// }
					//
					// SharedPreferences prefs = getSharedPreferences(
					// HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
					//
					// Intent pickIntent = new Intent();
					// pickIntent.setType("image/*");
					// pickIntent.setAction(Intent.ACTION_PICK);
					//
					// Intent newMediaFileIntent = new Intent(
					// MediaStore.ACTION_IMAGE_CAPTURE);
					//
					// File selectedFile = Utils.getOutputMediaFile(
					// HikeFileType.IMAGE, null, null);
					//
					// Editor editor = prefs.edit();
					// editor.putString(HikeMessengerApp.FILE_PATH,
					// selectedFile.getAbsolutePath());
					// editor.commit();
					//
					// Intent chooserIntent = Intent.createChooser(pickIntent,
					// "");
					//
					// newMediaFileIntent.putExtra(MediaStore.EXTRA_OUTPUT,
					// Uri.fromFile(selectedFile));
					//
					// chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
					// new Intent[] { newMediaFileIntent });
					//
					// startActivityForResult(chooserIntent, IMAGE_PICK_CODE);
					break;
				case R.id.post_fb_btn:
				case R.id.post_twitter_btn:
					v.setSelected(!v.isSelected());
					break;
				case R.id.title_icon:
					HikeHttpRequest hikeHttpRequest = new HikeHttpRequest(
							"/user/status", new HikeHttpCallback() {

								@Override
								public void onSuccess(JSONObject response) {
									if (progressDialog != null) {
										progressDialog.dismiss();
										progressDialog = null;
									}
									JSONObject data = response
											.optJSONObject("data");

									String mappedId = data
											.optString(HikeConstants.STATUS_ID);
									String text = data
											.optString(HikeConstants.STATUS_MESSAGE);
									SharedPreferences prefs = getSharedPreferences(
											HikeMessengerApp.ACCOUNT_SETTINGS,
											MODE_PRIVATE);
									String msisdn = prefs
											.getString(
													HikeMessengerApp.MSISDN_SETTING,
													"");
									String name = prefs.getString(
											HikeMessengerApp.NAME_SETTING, "");
									long time = (long) System
											.currentTimeMillis() / 1000;

									StatusMessage statusMessage = new StatusMessage(
											0, mappedId, msisdn, name, text,
											StatusMessageType.TEXT, time);
									HikeConversationsDatabase.getInstance()
											.addStatusMessage(statusMessage);

									Editor editor = prefs.edit();
									editor.putString(
											HikeMessengerApp.LAST_STATUS, text);
									editor.commit();

									HikeMessengerApp.getPubSub().publish(
											HikePubSub.MY_STATUS_CHANGED, text);
									statusDialog.cancel();

									/*
									 * This would happen in the case where the
									 * user has added a self contact and
									 * received an mqtt message before saving
									 * this to the db.
									 */
									if (statusMessage.getId() != -1) {
										HikeMessengerApp
												.getPubSub()
												.publish(
														HikePubSub.STATUS_MESSAGE_RECEIVED,
														statusMessage);
									}
									mActivityTask.showingStatusDialog = false;
								}

								@Override
								public void onFailure() {
									if (progressDialog != null) {
										progressDialog.dismiss();
										progressDialog = null;
									}
									mActivityTask.showingStatusDialog = false;
									Toast.makeText(getApplicationContext(),
											R.string.update_status_fail,
											Toast.LENGTH_SHORT).show();
								}

							});
					hikeHttpRequest.setStatusMessage(statusTxt.getText()
							.toString());
					mActivityTask.hikeHTTPTask = new HikeHTTPTask(null, 0);
					mActivityTask.hikeHTTPTask.execute(hikeHttpRequest);

					progressDialog = ProgressDialog.show(
							DrawerBaseActivity.this, null, getResources()
									.getString(R.string.updating_status));

					break;
				}
			}
		};

		insertImgBtn.setOnClickListener(statusDialogListener);
		fbPostBtn.setOnClickListener(statusDialogListener);
		twitterPostBtn.setOnClickListener(statusDialogListener);
		titleBtn.setOnClickListener(statusDialogListener);

		if (hasSelectedFile) {
			showFilePreview();
		}
		toggleEnablePostButton();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if ((requestCode == IMAGE_PICK_CODE) && resultCode == RESULT_OK) {
			File selectedFile = null;
			SharedPreferences prefs = getSharedPreferences(
					HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);

			if (requestCode == IMAGE_PICK_CODE) {
				selectedFile = new File(prefs.getString(
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
					mActivityTask.downloadPicasaImageTask = new DownloadPicasaImageTask(
							getApplicationContext(), destFile, selectedFileUri,
							new PicasaDownloadResult() {
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
			clearTempData();
			Log.d(getClass().getSimpleName(), "File transfer Cancelled");
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
		SharedPreferences prefs = getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
		Editor editor = prefs.edit();
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

		titleBtn.setEnabled((mActivityTask.filePath != null)
				|| (statusTxt.length() > 0));
	}
}
