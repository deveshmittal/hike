package com.bsb.hike.utils;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikeMessengerApp.CurrentState;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.fragments.ImageViewerFragment;

public class HikeAppStateBaseFragmentActivity extends SherlockFragmentActivity
		implements Listener {

	private static final String IMAGE_FRAGMENT_TAG = "imageFragmentTag";
	private static final String TAG = "HikeAppState";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (HikeMessengerApp.currentState == CurrentState.BACKGROUNDED
				|| HikeMessengerApp.currentState == CurrentState.CLOSED) {
			Log.d(TAG + getClass().getSimpleName(), "App was opened");
			HikeMessengerApp.currentState = CurrentState.OPENED;
			Utils.sendAppState(this);
		}
		super.onCreate(savedInstanceState);

	}

	@Override
	protected void onResume() {
		super.onResume();
		com.facebook.Settings.publishInstallAsync(this,
				HikeConstants.APP_FACEBOOK_ID);
	}

	@Override
	protected void onStart() {
		if (HikeMessengerApp.currentState == CurrentState.BACKGROUNDED
				|| HikeMessengerApp.currentState == CurrentState.CLOSED) {
			Log.d(TAG + getClass().getSimpleName(), "App was resumed");
			HikeMessengerApp.currentState = CurrentState.RESUMED;
			Utils.sendAppState(this);
		}
		super.onStart();
		HikeMessengerApp.getPubSub().addListener(HikePubSub.SHOW_IMAGE, this);
	}

	@Override
	public void onBackPressed() {
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(
				IMAGE_FRAGMENT_TAG);
		if (fragment != null && fragment.isVisible()) {
			FragmentTransaction fragmentTransaction = getSupportFragmentManager()
					.beginTransaction();
			fragmentTransaction.remove(fragment);
			fragmentTransaction.commitAllowingStateLoss();
		} else {
			HikeMessengerApp.currentState = CurrentState.BACK_PRESSED;
			super.onBackPressed();
		}
	}

	protected void onSaveInstanceState(Bundle outState) {
		// first saving my state, so the bundle wont be empty.
		// http://code.google.com/p/android/issues/detail?id=19917
		outState.putString("WORKAROUND_FOR_BUG_19917_KEY",
				"WORKAROUND_FOR_BUG_19917_VALUE");
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onStop() {
		Log.d(TAG + getClass().getSimpleName(), "OnStop");
		if (HikeMessengerApp.currentState == CurrentState.NEW_ACTIVITY) {
			Log.d(TAG + getClass().getSimpleName(),
					"App was going to another activity");
			HikeMessengerApp.currentState = CurrentState.RESUMED;
		} else if (HikeMessengerApp.currentState == CurrentState.BACK_PRESSED) {
			if (this instanceof HomeActivity) {
				Log.d(TAG + getClass().getSimpleName(), "App was closed");
				HikeMessengerApp.currentState = CurrentState.CLOSED;
				Utils.sendAppState(this);
			} else {
				HikeMessengerApp.currentState = CurrentState.RESUMED;
			}
		} else {
			Log.d(TAG + getClass().getSimpleName(), "App was backgrounded");
			HikeMessengerApp.currentState = CurrentState.BACKGROUNDED;
			Utils.sendAppState(this);
		}
		super.onStop();
		HikeMessengerApp.getPubSub()
				.removeListener(HikePubSub.SHOW_IMAGE, this);
	}

	@Override
	public void finish() {
		HikeMessengerApp.currentState = CurrentState.BACK_PRESSED;
		super.finish();
	}

	@Override
	public void startActivityFromFragment(Fragment fragment, Intent intent,
			int requestCode) {
		HikeMessengerApp.currentState = requestCode == -1
				|| requestCode == HikeConstants.SHARE_LOCATION_CODE
				|| requestCode == HikeConstants.CROP_RESULT ? CurrentState.NEW_ACTIVITY
				: CurrentState.BACKGROUNDED;
		super.startActivityFromFragment(fragment, intent, requestCode);
	}

	@Override
	public void startActivityForResult(Intent intent, int requestCode) {
		HikeMessengerApp.currentState = requestCode == -1
				|| requestCode == HikeConstants.SHARE_LOCATION_CODE
				|| requestCode == HikeConstants.CROP_RESULT ? CurrentState.NEW_ACTIVITY
				: CurrentState.BACKGROUNDED;
		super.startActivityForResult(intent, requestCode);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (HikeMessengerApp.currentState == CurrentState.BACKGROUNDED) {
			Log.d(TAG + getClass().getSimpleName(),
					"App returning from activity with result");
			HikeMessengerApp.currentState = CurrentState.RESUMED;
			Utils.sendAppState(this);
		}
	}

	@Override
	public void onEventReceived(String type, final Object object) {
		if (HikePubSub.SHOW_IMAGE.equals(type)) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Bundle arguments = (Bundle) object;

					ImageViewerFragment imageViewerFragment = new ImageViewerFragment();
					imageViewerFragment.setArguments(arguments);

					FragmentTransaction fragmentTransaction = getSupportFragmentManager()
							.beginTransaction();
					fragmentTransaction.add(R.id.parent_layout,
							imageViewerFragment, IMAGE_FRAGMENT_TAG);
					fragmentTransaction.commitAllowingStateLoss();
				}
			});
		}
	}
}
