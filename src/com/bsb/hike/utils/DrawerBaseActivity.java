package com.bsb.hike.utils;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
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
import com.bsb.hike.ui.MessagesList;
import com.bsb.hike.view.DrawerLayout;

public class DrawerBaseActivity extends Activity implements
		DrawerLayout.Listener, HikePubSub.Listener {

	private DrawerLayout parentLayout;

	private String[] leftDrawerPubSubListeners = {
			HikePubSub.SMS_CREDIT_CHANGED, HikePubSub.PROFILE_PIC_CHANGED,
			HikePubSub.PROFILE_NAME_CHANGED, HikePubSub.FREE_SMS_TOGGLED };

	private String[] rightDrawerPubSubListeners = { HikePubSub.ICON_CHANGED,
			HikePubSub.RECENT_CONTACTS_UPDATED, HikePubSub.FAVORITE_TOGGLED,
			HikePubSub.AUTO_RECOMMENDED_FAVORITES_ADDED,
			HikePubSub.USER_JOINED, HikePubSub.USER_LEFT,
			HikePubSub.CONTACT_ADDED };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// This does not apply to pre-Honeycomb devices,
		if (Build.VERSION.SDK_INT >= 11) {
			getWindow().setFlags(HikeConstants.FLAG_HARDWARE_ACCELERATED,
					HikeConstants.FLAG_HARDWARE_ACCELERATED);
		}
	}

	public void afterSetContentView(Bundle savedInstanceState) {
		parentLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		parentLayout.setListener(this);
		parentLayout.setUpLeftDrawerView();

		findViewById(R.id.topbar_menu).setVisibility(View.VISIBLE);
		findViewById(R.id.menu_bar).setVisibility(View.VISIBLE);
		if (savedInstanceState != null) {
			if (savedInstanceState
					.getBoolean(HikeConstants.Extras.IS_LEFT_DRAWER_VISIBLE)) {
				parentLayout.toggleSidebar(true, true);
			} else if (savedInstanceState
					.getBoolean(HikeConstants.Extras.IS_RIGHT_DRAWER_VISIBLE)) {
				parentLayout.toggleSidebar(true, false);
			}
		}

		HikeMessengerApp.getPubSub().addListeners(this,
				leftDrawerPubSubListeners);

		/*
		 * Only show the favorites drawer in the Messages list screen
		 */
		if ((this instanceof MessagesList)) {
			parentLayout.setUpRightDrawerView(this);

			ImageButton rightFavoriteBtn = (ImageButton) findViewById(R.id.title_image_btn2);
			rightFavoriteBtn.setVisibility(View.VISIBLE);
			rightFavoriteBtn.setImageResource(R.drawable.ic_favorites);

			findViewById(R.id.button_bar3).setVisibility(View.VISIBLE);
			HikeMessengerApp.getPubSub().addListeners(this,
					rightDrawerPubSubListeners);
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
				overridePendingTransition(R.anim.alpha_in,
						R.anim.slide_out_right_noalpha);
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
	public void onEventReceived(String type, final Object object) {
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
		} else if (HikePubSub.PROFILE_NAME_CHANGED.equals(type)) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					parentLayout.setProfileName();
				}
			});
		} else if (HikePubSub.FREE_SMS_TOGGLED.equals(type)) {
			final boolean freeSMSOn = (Boolean) object;
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					parentLayout.renderLeftDrawerItems(freeSMSOn);
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
			/*
			 * If the contact is already a part of the favorites list, we don't
			 * need to do anything.
			 */
			if(contactInfo.getFavoriteType() != FavoriteType.NOT_FAVORITE) {
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
					} else if (favoriteType == FavoriteType.RECOMMENDED_FAVORITE) {
						contactInfo
								.setFavoriteType(FavoriteType.RECOMMENDED_FAVORITE);
						parentLayout.addToRecommended(contactInfo);
					}
				}
			});
		} else if (HikePubSub.AUTO_RECOMMENDED_FAVORITES_ADDED.equals(type)) {
			final List<ContactInfo> autoRecommendedFavorites = HikeUserDatabase
					.getInstance().getContactsOrderedByLastMessaged(-1,
							FavoriteType.AUTO_RECOMMENDED_FAVORITE,
							HikeConstants.BOTH_VALUE, true, false, -1);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					parentLayout
							.addAutoRecommendedFavoritesList(autoRecommendedFavorites);
				}
			});
		} else if (HikePubSub.CONTACT_ADDED.equals(type)) {
			final ContactInfo contactInfo = (ContactInfo) object;
			/*
			 * If the contact is already a part of the favorites list, we don't
			 * need to do anything.
			 */
			if (contactInfo == null) {
				return;
			}
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					parentLayout.updateRecentContacts(contactInfo);
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
		/*
		 * Accounting for the header view.
		 */
		int pos = adapterInfo.position - 1;
		parentLayout.onCreateFavoritesContextMenu(this, menu, pos);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		return parentLayout.onFavoritesContextItemSelected(item);
	}
}
