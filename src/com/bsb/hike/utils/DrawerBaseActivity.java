package com.bsb.hike.utils;

import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.ui.CentralTimeline;
import com.bsb.hike.ui.MessagesList;
import com.bsb.hike.ui.StatusUpdate;
import com.bsb.hike.view.DrawerLayout;
import com.bsb.hike.view.DrawerLayout.CurrentState;

public class DrawerBaseActivity extends AuthSocialAccountBaseActivity implements
		DrawerLayout.Listener, HikePubSub.Listener {

	public DrawerLayout parentLayout;
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
			HikePubSub.REJECT_FRIEND_REQUEST, HikePubSub.BLOCK_USER,
			HikePubSub.UNBLOCK_USER };

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

	public void afterSetContentView(Bundle savedInstanceState) {
		afterSetContentView(savedInstanceState, true);
	}

	public void afterSetContentView(Bundle savedInstanceState,
			boolean showButtons) {
		parentLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		parentLayout.setListener(this);
		parentLayout.setActivity(this);
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

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		HikeMessengerApp.getPubSub().removeListeners(this,
				leftDrawerPubSubListeners);
		if ((this instanceof MessagesList) || (this instanceof CentralTimeline)) {
			HikeMessengerApp.getPubSub().removeListeners(this,
					rightDrawerPubSubListeners);
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
			HikeMessengerApp.getPubSub().publish(HikePubSub.REFRESH_RECENTS, null);
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
			boolean freeSMSOn = PreferenceManager.getDefaultSharedPreferences(
					this).getBoolean(HikeConstants.FREE_SMS_PREF, false);
			final List<ContactInfo> recentList = HikeUserDatabase.getInstance()
					.getRecentContacts(-1, false, FavoriteType.NOT_FRIEND,
							freeSMSOn ? 1 : 0);
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
					startActivity(new Intent(DrawerBaseActivity.this,
							StatusUpdate.class));
				}
			});
		} else if (HikePubSub.MY_STATUS_CHANGED.equals(type)) {
			final String status = (String) object;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					parentLayout.updateStatus(status,
							preferences.getInt(HikeMessengerApp.LAST_MOOD, -1));
				}
			});
		} else if (HikePubSub.BLOCK_USER.equals(type)
				|| HikePubSub.UNBLOCK_USER.equals(type)) {
			String msisdn = (String) object;
			final ContactInfo contactInfo = HikeUserDatabase.getInstance()
					.getContactInfoFromMSISDN(msisdn, true);
			final boolean blocked = HikePubSub.BLOCK_USER.equals(type);
			if (contactInfo == null) {
				return;
			}
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (blocked) {
						parentLayout.removeContact(contactInfo);
					} else {
						parentLayout.updateRecentContacts(contactInfo);
					}
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

}
