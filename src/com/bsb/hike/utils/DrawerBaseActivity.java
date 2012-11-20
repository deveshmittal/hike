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
			HikePubSub.PROFILE_NAME_CHANGED };

	private String[] rightDrawerPubSubListeners = { HikePubSub.ICON_CHANGED,
			HikePubSub.RECENT_CONTACTS_UPDATED, HikePubSub.FAVORITE_TOGGLED,
			HikePubSub.AUTO_RECOMMENDED_FAVORITES_ADDED };

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
		parentLayout.setUpRightDrawerView(this);

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

		ImageButton rightFavoriteBtn = (ImageButton) findViewById(R.id.title_image_btn2);
		rightFavoriteBtn.setVisibility(View.VISIBLE);
		rightFavoriteBtn.setImageResource(R.drawable.ic_favorites);

		findViewById(R.id.button_bar3).setVisibility(View.VISIBLE);

		HikeMessengerApp.getPubSub().addListeners(this,
				leftDrawerPubSubListeners);

		/*
		 * Since we have a static adapter for the right drawer, we should only
		 * add its listeners once. MessagesList activity is the entry point for
		 * accessing favorites so we only add the listeners when we come to this
		 * activity
		 */
		if (this instanceof MessagesList) {
			HikeMessengerApp.getPubSub().addListeners(this,
					rightDrawerPubSubListeners);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		HikeMessengerApp.getPubSub().removeListeners(this,
				leftDrawerPubSubListeners);
		/*
		 * Only remove the listeners if the MessagesList activity is being
		 * destroyed.
		 */
		if (this instanceof MessagesList) {
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
		} else if (HikePubSub.ICON_CHANGED.equals(type)) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					parentLayout.refreshFavoritesDrawer();
				}
			});
		} else if (HikePubSub.RECENT_CONTACTS_UPDATED.equals(type)) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					parentLayout.updateRecentContacts((String) object);
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
							HikeConstants.BOTH_VALUE, true, false);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					parentLayout.addAutoRecommendedFavoritesList(autoRecommendedFavorites);
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
