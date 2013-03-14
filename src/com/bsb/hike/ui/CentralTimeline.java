package com.bsb.hike.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.adapters.CentralTimelineAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.tasks.DownloadProfileImageTask;
import com.bsb.hike.utils.DrawerBaseActivity;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;

public class CentralTimeline extends DrawerBaseActivity implements
		OnItemClickListener, Listener {

	private CentralTimelineAdapter centralTimelineAdapter;
	private ListView timelineContent;
	private List<StatusMessage> statusMessages;
	private int friendRequests;
	private StatusMessage noStatusMessage;
	private StatusMessage noFriendMessage;
	private int unseenCount;
	private SharedPreferences prefs;

	private String[] pubSubListeners = new String[] {
			HikePubSub.FAVORITE_TOGGLED, HikePubSub.STATUS_MESSAGE_RECEIVED,
			HikePubSub.PROFILE_IMAGE_DOWNLOADED,
			HikePubSub.PROFILE_IMAGE_NOT_DOWNLOADED };
	private String userMsisdn;
	private ActivityState mActivityState;
	private ProgressDialog mDialog;

	private class ActivityState {
		public DownloadProfileImageTask downloadProfileImageTask;
		public boolean viewingProfileImage = false;
		public boolean animatedProfileImage = false;
		public int imageViewId = -1;
		public String mappedId = null;
	}

	@Override
	protected void onPause() {
		super.onPause();
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, null);
	}

	@Override
	protected void onResume() {
		super.onResume();
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, this);
		if (prefs.getInt(HikeMessengerApp.UNSEEN_STATUS_COUNT, 0) > 0
				|| prefs.getInt(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, 0) > 0) {
			resetUnseenStatusCount();
			HikeMessengerApp.getPubSub().publish(
					HikePubSub.RESET_NOTIFICATION_COUNTER, null);
		}
	}

	private void resetUnseenStatusCount() {
		Editor editor = prefs.edit();
		editor.putInt(HikeMessengerApp.UNSEEN_STATUS_COUNT, 0);
		editor.putInt(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, 0);
		editor.commit();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return mActivityState;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.setDensityMultiplier(this);
		setContentView(R.layout.central_timeline);
		afterSetContentView(savedInstanceState, false);

		prefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
				MODE_PRIVATE);

		unseenCount = Utils.getNotificationCount(prefs, true);

		Object o = getLastNonConfigurationInstance();
		if (o instanceof ActivityState) {
			mActivityState = (ActivityState) o;
			if (mActivityState.downloadProfileImageTask != null) {
				mDialog = ProgressDialog.show(this, null, getResources()
						.getString(R.string.downloading_image));
				if (mActivityState.downloadProfileImageTask != null) {
					mDialog.setCancelable(true);
					setDialogOnCancelListener();
				}
			}
		} else {
			mActivityState = new ActivityState();
		}

		TextView titleTV = (TextView) findViewById(R.id.title);
		titleTV.setText(R.string.recent_updates);

		timelineContent = (ListView) findViewById(R.id.timeline_content);

		userMsisdn = prefs.getString(HikeMessengerApp.MSISDN_SETTING, "");

		List<ContactInfo> friendRequestList = HikeUserDatabase.getInstance()
				.getContactsOfFavoriteType(FavoriteType.REQUEST_RECEIVED,
						HikeConstants.BOTH_VALUE, userMsisdn);

		List<ContactInfo> friendsList = HikeUserDatabase.getInstance()
				.getContactsOfFavoriteType(FavoriteType.FRIEND,
						HikeConstants.BOTH_VALUE, userMsisdn);

		friendRequests = friendRequestList.size();

		int friendMsisdnLength = friendRequestList.size() + friendsList.size();

		ArrayList<String> msisdnList = new ArrayList<String>();

		for (ContactInfo contactInfo : friendRequestList) {
			/*
			 * We don't show status updates from unknown contacts
			 */
			if (contactInfo.isUnknownContact()
					|| TextUtils.isEmpty(contactInfo.getMsisdn())) {
				continue;
			}
			msisdnList.add(contactInfo.getMsisdn());
		}
		for (ContactInfo contactInfo : friendsList) {
			/*
			 * We don't show status updates from unknown contacts
			 */
			if (contactInfo.isUnknownContact()
					|| TextUtils.isEmpty(contactInfo.getMsisdn())) {
				continue;
			}
			msisdnList.add(contactInfo.getMsisdn());
		}
		msisdnList.add(userMsisdn);

		String[] friendMsisdns = new String[msisdnList.size()];
		msisdnList.toArray(friendMsisdns);
		statusMessages = HikeConversationsDatabase.getInstance()
				.getStatusMessages(friendMsisdns);

		for (ContactInfo contactInfo : friendRequestList) {
			statusMessages
					.add(0,
							new StatusMessage(
									CentralTimelineAdapter.FRIEND_REQUEST_ID,
									null,
									contactInfo.getMsisdn(),
									TextUtils.isEmpty(contactInfo.getName()) ? contactInfo
											.getMsisdn() : contactInfo
											.getName(),
									getString(R.string.added_as_hike_friend),
									StatusMessageType.FRIEND_REQUEST, System
											.currentTimeMillis() / 1000));
		}

		String name = prefs.getString(HikeMessengerApp.NAME_SETTING, null);
		String lastStatus = prefs.getString(HikeMessengerApp.LAST_STATUS, "");

		/*
		 * If we already have a few status messages in the timeline, no need to
		 * prompt the user to post his/her own message.
		 */
		if (statusMessages.size() < HikeConstants.MIN_STATUS_COUNT) {
			if (TextUtils.isEmpty(lastStatus)) {
				noStatusMessage = new StatusMessage(
						CentralTimelineAdapter.EMPTY_STATUS_NO_STATUS_ID, null,
						"12345", getString(R.string.team_hike), getString(
								R.string.hey_name, name),
						StatusMessageType.NO_STATUS,
						System.currentTimeMillis() / 1000);
				statusMessages.add(0, noStatusMessage);
			}
		}
		if (friendMsisdnLength == 0) {
			noFriendMessage = new StatusMessage(
					CentralTimelineAdapter.EMPTY_STATUS_NO_FRIEND_ID, null,
					"12345", getString(R.string.team_hike), getString(
							R.string.hey_name, name),
					StatusMessageType.NO_STATUS,
					System.currentTimeMillis() / 1000);
			statusMessages.add(0, noFriendMessage);
		}

		centralTimelineAdapter = new CentralTimelineAdapter(this,
				statusMessages, userMsisdn, unseenCount);
		timelineContent.setAdapter(centralTimelineAdapter);
		timelineContent.setOnItemClickListener(this);

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		for (int i = 0; i < friendRequests; i++) {
			notificationManager.cancel(statusMessages.get(i).getMsisdn()
					.hashCode());
		}

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);

		if (mActivityState.viewingProfileImage) {
			downloadOrShowProfileImage(false, false, mActivityState.mappedId,
					mActivityState.imageViewId);
		}
	}

	public void onBackPressed() {
		if (mActivityState.viewingProfileImage) {
			View profileContainer = findViewById(R.id.profile_image_container);

			animateProfileImage(false, mActivityState.imageViewId);
			profileContainer.setVisibility(View.GONE);

			mActivityState = new ActivityState();
			expandedWithoutAnimation = false;
			return;
		}
		super.onBackPressed();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view,
			int position, long id) {
		StatusMessage statusMessage = centralTimelineAdapter.getItem(position);
		if ((statusMessage.getStatusMessageType() == StatusMessageType.NO_STATUS)
				|| (statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST)) {
			return;
		} else if (userMsisdn.equals(statusMessage.getMsisdn())) {
			Intent intent = new Intent(this, ProfileActivity.class);
			intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
			startActivity(intent);
			return;
		}

		Intent intent = Utils.createIntentFromContactInfo(new ContactInfo(null,
				statusMessage.getMsisdn(), statusMessage.getName(),
				statusMessage.getMsisdn()));
		intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
		intent.setClass(this, ChatThread.class);
		startActivity(intent);
	}

	public void onYesBtnClick(View v) {
		StatusMessage statusMessage = (StatusMessage) v.getTag();

		if (CentralTimelineAdapter.EMPTY_STATUS_NO_FRIEND_ID == statusMessage
				.getId()) {
			parentLayout.openRightSidebar();
		} else if (CentralTimelineAdapter.EMPTY_STATUS_NO_STATUS_ID == statusMessage
				.getId()) {
			showStatusDialog(false);
		} else {
			toggleFavoriteAndRemoveTimelineItem(statusMessage,
					FavoriteType.FRIEND);
		}
	}

	public void onNoBtnClick(View v) {
		StatusMessage statusMessage = (StatusMessage) v.getTag();
		toggleFavoriteAndRemoveTimelineItem(statusMessage,
				FavoriteType.NOT_FRIEND);
	}

	private void toggleFavoriteAndRemoveTimelineItem(
			StatusMessage statusMessage, FavoriteType favoriteType) {
		ContactInfo contactInfo = HikeUserDatabase.getInstance()
				.getContactInfoFromMSISDN(statusMessage.getMsisdn(), false);

		Pair<ContactInfo, FavoriteType> favoriteAdded = new Pair<ContactInfo, FavoriteType>(
				contactInfo, favoriteType);
		HikeMessengerApp
				.getPubSub()
				.publish(
						favoriteType == FavoriteType.FRIEND ? HikePubSub.FAVORITE_TOGGLED
								: HikePubSub.REJECT_FRIEND_REQUEST,
						favoriteAdded);

		for (StatusMessage listItem : statusMessages) {
			if (listItem.getMsisdn().equals(statusMessage.getMsisdn())
					&& listItem.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST) {
				statusMessages.remove(listItem);
				friendRequests--;
				break;
			}
		}
		centralTimelineAdapter.notifyDataSetChanged();
		HikeMessengerApp.getPubSub().publish(
				HikePubSub.DECREMENT_NOTIFICATION_COUNTER, null);
	}

	public void onDetailsBtnClick(View v) {
		StatusMessage statusMessage = (StatusMessage) v.getTag();

		Intent intent = new Intent(this, ProfileActivity.class);
		intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);

		if ((statusMessage.getStatusMessageType() == StatusMessageType.NO_STATUS)
				|| (statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST)) {
			return;
		} else if (userMsisdn.equals(statusMessage.getMsisdn())) {
			startActivity(intent);
			return;
		}

		intent.setClass(this, ProfileActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(HikeConstants.Extras.CONTACT_INFO,
				statusMessage.getMsisdn());
		startActivity(intent);
	}

	@Override
	public void onEventReceived(String type, Object object) {
		super.onEventReceived(type, object);
		if (HikePubSub.FAVORITE_TOGGLED.equals(type)) {
			final Pair<ContactInfo, FavoriteType> favoriteToggle = (Pair<ContactInfo, FavoriteType>) object;
			ContactInfo contactInfo = favoriteToggle.first;
			if (favoriteToggle.second != FavoriteType.REQUEST_RECEIVED) {
				return;
			}
			int startIndex = getStartIndex();
			statusMessages
					.add(startIndex,
							new StatusMessage(
									CentralTimelineAdapter.FRIEND_REQUEST_ID,
									null,
									contactInfo.getMsisdn(),
									TextUtils.isEmpty(contactInfo.getName()) ? contactInfo
											.getMsisdn() : contactInfo
											.getName(),
									getString(R.string.added_as_hike_friend),
									StatusMessageType.FRIEND_REQUEST, System
											.currentTimeMillis() / 1000));
			friendRequests++;
			if (noFriendMessage != null) {
				statusMessages.remove(noFriendMessage);
				noFriendMessage = null;
			}
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					centralTimelineAdapter.notifyDataSetChanged();
				}
			});

		} else if (HikePubSub.STATUS_MESSAGE_RECEIVED.equals(type)) {
			StatusMessage statusMessage = (StatusMessage) object;
			ContactInfo contactInfo = HikeUserDatabase.getInstance()
					.getContactInfoFromMSISDN(statusMessage.getMsisdn(), false);
			statusMessage.setName(contactInfo.getName());

			int startIndex = getStartIndex();

			statusMessages.add(friendRequests + startIndex, statusMessage);
			resetUnseenStatusCount();
			if (noStatusMessage != null
					&& statusMessages.size() >= HikeConstants.MIN_STATUS_COUNT) {
				statusMessages.remove(noStatusMessage);
				noStatusMessage = null;
			}
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					centralTimelineAdapter.notifyDataSetChanged();
				}
			});
			HikeMessengerApp.getPubSub().publish(
					HikePubSub.RESET_NOTIFICATION_COUNTER, null);
		} else if (HikePubSub.PROFILE_IMAGE_DOWNLOADED.equals(type)
				|| HikePubSub.PROFILE_IMAGE_NOT_DOWNLOADED.equals(type)) {
			final String msisdn = (String) object;
			if (!msisdn.equals(mActivityState.mappedId)) {
				return;
			}
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mActivityState.animatedProfileImage = true;
					downloadOrShowProfileImage(false, true, msisdn,
							mActivityState.imageViewId);

					if (mDialog != null) {
						mDialog.dismiss();
						mDialog = null;
					}
				}
			});
		}
	}

	private int getStartIndex() {
		int startIndex = 0;
		if (noFriendMessage != null) {
			startIndex++;
		}
		if (noStatusMessage != null) {
			startIndex++;
		}
		return startIndex;
	}

	public void onViewImageClicked(View v) {
		StatusMessage statusMessage = (StatusMessage) v.getTag();
		mActivityState.imageViewId = v.getId();
		mActivityState.mappedId = statusMessage.getMappedId();
		downloadOrShowProfileImage(true, false, mActivityState.mappedId,
				v.getId());
	}

	private void downloadOrShowProfileImage(boolean startNewDownload,
			boolean justDownloaded, String mappedId, int viewId) {
		if (Utils.getExternalStorageState() == ExternalStorageState.NONE) {
			Toast.makeText(getApplicationContext(),
					R.string.no_external_storage, Toast.LENGTH_SHORT).show();
			return;
		}
		/*
		 * The id should not be null if the image was just downloaded.
		 */
		if (justDownloaded && mActivityState.mappedId == null) {
			return;
		}

		String basePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT
				+ HikeConstants.PROFILE_ROOT;

		boolean hasCustomImage = HikeUserDatabase.getInstance().hasIcon(
				mappedId);

		String fileName = hasCustomImage ? Utils
				.getProfileImageFileName(mappedId) : Utils
				.getDefaultAvatarServerName(this, mappedId);

		File file = new File(basePath, fileName);

		if (file.exists()) {
			showLargerImage(
					BitmapDrawable.createFromPath(basePath + "/" + fileName),
					justDownloaded, viewId);
		} else {
			showLargerImage(
					IconCacheManager.getInstance().getIconForMSISDN(mappedId),
					justDownloaded, viewId);
			if (startNewDownload) {
				mActivityState.downloadProfileImageTask = new DownloadProfileImageTask(
						getApplicationContext(), mappedId, fileName, true, true);
				mActivityState.downloadProfileImageTask.execute();

				mDialog = ProgressDialog.show(this, null, getResources()
						.getString(R.string.downloading_image));
				mDialog.setCancelable(true);
				setDialogOnCancelListener();
			}
		}
	}

	private void setDialogOnCancelListener() {
		mDialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				if (mActivityState.downloadProfileImageTask != null) {
					mActivityState.downloadProfileImageTask.cancel(true);
				}
				onBackPressed();
			}
		});
	}

	public void hideLargerImage(View v) {
		onBackPressed();
	}

	private boolean expandedWithoutAnimation = false;

	private void showLargerImage(Drawable image, boolean justDownloaded,
			int viewId) {
		mActivityState.viewingProfileImage = true;

		ViewGroup profileImageContainer = (ViewGroup) findViewById(R.id.profile_image_container);
		profileImageContainer.setVisibility(View.VISIBLE);

		ImageView profileImageLarge = (ImageView) findViewById(R.id.profile_image_large);

		if (!justDownloaded) {
			if (!mActivityState.animatedProfileImage) {
				mActivityState.animatedProfileImage = true;
				animateProfileImage(true, viewId);
			} else {
				expandedWithoutAnimation = true;
				((LinearLayout) profileImageContainer)
						.setGravity(Gravity.CENTER);
				LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,
						LayoutParams.MATCH_PARENT);
				profileImageLarge.setLayoutParams(lp);
			}
		}

		((ImageView) findViewById(R.id.profile_image_large))
				.setImageDrawable(image);
	}

	private void animateProfileImage(boolean expand, int viewId) {
		ViewGroup profileImageContainer = (ViewGroup) findViewById(R.id.profile_image_container);
		((LinearLayout) profileImageContainer).setGravity(Gravity.NO_GRAVITY);
		ImageView profileImageLarge = (ImageView) findViewById(R.id.profile_image_large);
		ImageView profileImageSmall = (ImageView) findViewById(viewId);

		if (profileImageSmall != null) {
			int maxWidth = (expand || !expandedWithoutAnimation) ? getResources()
					.getDisplayMetrics().widthPixels : profileImageSmall
					.getMeasuredHeight();
			int maxHeight = (expand || !expandedWithoutAnimation) ? getResources()
					.getDisplayMetrics().heightPixels : profileImageSmall
					.getMeasuredHeight();

			int screenHeight = getResources().getDisplayMetrics().heightPixels;

			int startWidth = (int) ((expand || !expandedWithoutAnimation) ? getResources()
					.getDimension(R.dimen.timeline_pic) : profileImageLarge
					.getWidth());
			int startHeight = (int) ((expand || !expandedWithoutAnimation) ? getResources()
					.getDimension(R.dimen.timeline_pic) : profileImageLarge
					.getHeight());

			int[] startLocations = new int[2];
			if (expand || mActivityState.animatedProfileImage) {
				profileImageSmall.getLocationOnScreen(startLocations);
			} else {
				profileImageLarge.getLocationOnScreen(startLocations);
			}

			int statusBarHeight = screenHeight
					- profileImageContainer.getHeight();

			int startLocX = startLocations[0];
			int startLocY = startLocations[1] - statusBarHeight;

			LayoutParams startLp = new LayoutParams(startWidth, startHeight);
			startLp.setMargins(startLocX, startLocY, 0, 0);

			profileImageLarge.setLayoutParams(startLp);

			float multiplier;
			if (maxWidth > maxHeight) {
				multiplier = maxHeight / startHeight;
			} else {
				multiplier = maxWidth / startWidth;
			}

			ScaleAnimation scaleAnimation = (expand || expandedWithoutAnimation) ? new ScaleAnimation(
					1.0f, multiplier, 1.0f, multiplier) : new ScaleAnimation(
					multiplier, 1.0f, multiplier, 1.0f);

			int xDest;
			int yDest;
			if (expand || !expandedWithoutAnimation) {
				xDest = maxWidth / 2;
				xDest -= (((int) (startWidth * multiplier)) / 2) + startLocX;
				yDest = maxHeight / 2;
				yDest -= (((int) (startHeight * multiplier)) / 2) + startLocY;
			} else {
				int[] endLocations = new int[2];
				profileImageSmall.getLocationInWindow(endLocations);
				xDest = endLocations[0];
				yDest = endLocations[1];
			}

			TranslateAnimation translateAnimation = (expand || expandedWithoutAnimation) ? new TranslateAnimation(
					0, xDest, 0, yDest) : new TranslateAnimation(xDest, 0,
					yDest, 0);

			AnimationSet animationSet = new AnimationSet(true);
			animationSet.addAnimation(scaleAnimation);
			animationSet.addAnimation(translateAnimation);
			animationSet.setFillAfter(true);
			animationSet.setDuration(350);
			animationSet.setStartOffset(expand ? 150 : 0);

			profileImageLarge.startAnimation(animationSet);
		}
		AlphaAnimation alphaAnimation = expand ? new AlphaAnimation(0.0f, 1.0f)
				: new AlphaAnimation(1.0f, 0.0f);
		alphaAnimation.setDuration(200);
		alphaAnimation.setStartOffset(expand ? 0 : 200);
		profileImageContainer.startAnimation(alphaAnimation);
	}

}
