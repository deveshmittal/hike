package com.bsb.hike.utils;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.ui.ComposeActivity;
import com.bsb.hike.ui.CreditsActivity;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.SettingsActivity;
import com.bsb.hike.ui.StatusUpdate;
import com.bsb.hike.ui.TellAFriend;
import com.bsb.hike.ui.WebViewActivity;

public class HomeBaseFragment extends SherlockListFragment {

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent = null;

		switch (item.getItemId()) {
		case R.id.new_conversation:
			intent = new Intent(getActivity(), ComposeActivity.class);
			intent.putExtra(HikeConstants.Extras.EDIT, true);
			break;
		case R.id.new_update:
			intent = new Intent(getActivity(), StatusUpdate.class);
			intent.putExtra(HikeConstants.Extras.FROM_CONVERSATIONS_SCREEN,
					true);
			break;
		case R.id.settings:
			intent = new Intent(getActivity(), SettingsActivity.class);
			break;
		case R.id.invite:
			intent = new Intent(getActivity(), TellAFriend.class);
			break;
		case R.id.free_sms:
			intent = new Intent(getActivity(), CreditsActivity.class);
			break;
		case R.id.my_profile:
			intent = new Intent(getActivity(), ProfileActivity.class);
			break;
		case R.id.rewards:
			intent = getRewardsIntent();
			break;
		case R.id.games:
			intent = getGamingIntent();
			break;
		case R.id.mute_notification:
			toggleMute();
			return true;
		}

		if (intent != null) {
			startActivity(intent);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		MenuItem gamesItem = menu.findItem(R.id.games);
		MenuItem rewardsItem = menu.findItem(R.id.rewards);
		MenuItem muteItem = menu.findItem(R.id.mute_notification);

		SharedPreferences prefs = getSherlockActivity().getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		if (rewardsItem != null) {
			rewardsItem.setVisible(prefs.getBoolean(
					HikeMessengerApp.SHOW_REWARDS, false));
		}

		if (gamesItem != null) {
			gamesItem.setVisible(prefs.getBoolean(HikeMessengerApp.SHOW_GAMES,
					false));
		}

		if (muteItem != null) {
			int preference = PreferenceManager.getDefaultSharedPreferences(
					getActivity()).getInt(HikeConstants.STATUS_PREF, 0);
			muteItem.setTitle(preference == 0 ? R.string.mute_notifications
					: R.string.unmute_notifications);
		}
		super.onPrepareOptionsMenu(menu);
	}

	private Intent getGamingIntent() {
		Intent intent = new Intent(this.getSherlockActivity()
				.getApplicationContext(), WebViewActivity.class);
		intent.putExtra(HikeConstants.Extras.GAMES_PAGE, true);
		/*
		 * using the same token as rewards token, as per DK sir's mail
		 */
		intent.putExtra(
				HikeConstants.Extras.URL_TO_LOAD,
				AccountUtils.gamesUrl
						+ getActivity().getSharedPreferences(
								HikeConstants.GAMES, Context.MODE_PRIVATE)
								.getString(HikeMessengerApp.REWARDS_TOKEN, ""));
		intent.putExtra(HikeConstants.Extras.TITLE, getSherlockActivity()
				.getString(R.string.new_string));
		return intent;
	}

	private Intent getRewardsIntent() {
		Intent intent = new Intent(this.getSherlockActivity()
				.getApplicationContext(), WebViewActivity.class);
		intent.putExtra(
				HikeConstants.Extras.URL_TO_LOAD,
				AccountUtils.rewardsUrl
						+ getActivity().getSharedPreferences(
								HikeConstants.GAMES, Context.MODE_PRIVATE)
								.getString(HikeMessengerApp.REWARDS_TOKEN, ""));
		intent.putExtra(HikeConstants.Extras.TITLE, getSherlockActivity()
				.getString(R.string.new_string));
		return intent;
	}

	private void toggleMute() {
		SharedPreferences settingPref = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		int preference = settingPref.getInt(HikeConstants.STATUS_PREF, 0);

		int newValue;

		Editor editor = settingPref.edit();
		if (preference == 0) {
			newValue = -1;
			editor.putInt(HikeConstants.STATUS_PREF, newValue);
		} else {
			newValue = 0;
			editor.putInt(HikeConstants.STATUS_PREF, newValue);
		}
		editor.commit();

		try {
			JSONObject jsonObject = new JSONObject();
			JSONObject data = new JSONObject();
			data.put(HikeConstants.PUSH_SU, newValue);
			jsonObject.put(HikeConstants.DATA, data);
			jsonObject.put(HikeConstants.TYPE,
					HikeConstants.MqttMessageTypes.ACCOUNT_CONFIG);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH,
					jsonObject);

			Toast.makeText(
					getActivity(),
					newValue == 0 ? R.string.status_notification_on
							: R.string.status_notification_off,
					Toast.LENGTH_SHORT).show();
		} catch (JSONException e) {
			Log.w(getClass().getSimpleName(), e);
		}
	}
}
