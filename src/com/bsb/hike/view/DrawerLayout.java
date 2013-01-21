package com.bsb.hike.view;

import java.util.List;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.DrawerFavoritesAdapter;
import com.bsb.hike.adapters.DrawerFavoritesAdapter.FavoriteAdapterViewType;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.CreditsActivity;
import com.bsb.hike.ui.MessagesList;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.TellAFriend;
import com.bsb.hike.ui.Tutorial;
import com.bsb.hike.ui.WebViewActivity;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.CustomInterpolator;
import com.bsb.hike.utils.Utils;

public class DrawerLayout extends RelativeLayout implements
		OnItemClickListener, View.OnClickListener {

	private final static int ANIMATION_STEPS = (int) (7 * Utils.densityMultiplier);

	private final static int DURATION_BETWEEN_EACH_STEP = 5;

	private boolean mLeftOpened;
	private boolean mRightOpened;

	private View mLeftSidebar;
	private View mRightSidebar;

	private View mContent;
	private int mRightSidebarWidth;
	private int mRightSidebarOffsetForAnimation;
	private int mLeftSidebarWidth;
	private int mLeftSidebarOffsetForAnimation;
	private int topBarButtonWidth;

	private Listener mListener;

	private boolean mPressed = false;

	private SharedPreferences accountPrefs;

	private Intent intent;

	private Activity activity;

	private Handler handler;

	private ImageView profileImg;

	private BitmapDrawable leftDrawerBg;

	private BitmapDrawable rightDrawerBg;

	private DrawerFavoritesAdapter drawerFavoritesAdapter;

	private boolean freeSMS;

	private TextView pendingRequests;

	private boolean isAnimating = false;

	private CustomInterpolator interpolator;

	public enum LeftDrawerItems {
		HOME, GROUP_CHAT, TELL_A_FRIEND, FREE_SMS, PROFILE, HELP
	}

	public enum TimeOfDay {
		MORNING, DAY, NIGHT
	}

	public DrawerLayout(Context context) {
		this(context, null, 0);
	}

	public DrawerLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DrawerLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		accountPrefs = getContext().getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		handler = new Handler();
		topBarButtonWidth = (int) (48 * Utils.densityMultiplier);
		boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
		mRightSidebarWidth = (int) ((isPortrait ? context.getResources()
				.getDisplayMetrics().widthPixels : context.getResources()
				.getDisplayMetrics().heightPixels) - topBarButtonWidth);
		mRightSidebarOffsetForAnimation = (int) (80 * Utils.densityMultiplier);

		mLeftSidebarWidth = (int) (72 * Utils.densityMultiplier);
		mLeftSidebarOffsetForAnimation = 10;
		/*
		 * Fix for android v2.3 and below specific bug where the bitmap is not
		 * tiled and gets stretched instead if we use the xml. So we're creating
		 * it via code.
		 * http://stackoverflow.com/questions/7586209/xml-drawable-bitmap
		 * -tilemode-bug
		 */
		rightDrawerBg = new BitmapDrawable(BitmapFactory.decodeResource(
				getResources(), R.drawable.bg_right_drawer));
		rightDrawerBg.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);

		leftDrawerBg = new BitmapDrawable(BitmapFactory.decodeResource(
				getResources(), R.drawable.bg_right_drawer));
		leftDrawerBg.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);

		freeSMS = PreferenceManager.getDefaultSharedPreferences(getContext())
				.getBoolean(HikeConstants.FREE_SMS_PREF, false);

		interpolator = new CustomInterpolator();
	}

	public void setUpRightDrawerView(Activity activity) {
		ListView favoriteListView = (ListView) findViewById(R.id.favorite_list);
		pendingRequests = (TextView) findViewById(R.id.fav_requests);

		drawerFavoritesAdapter = new DrawerFavoritesAdapter(getContext(), this);
		favoriteListView.setAdapter(drawerFavoritesAdapter);

		favoriteListView.setOnItemClickListener(this);
		activity.registerForContextMenu(favoriteListView);

		updatePendingRequests();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position,
			long id) {

		ContactInfo contactInfo = drawerFavoritesAdapter.getItem(position);

		/*
		 * If the user taps on a non hike contact and is not an Indian user, we
		 * do nothing.
		 */
		if (!contactInfo.isOnhike()
				&& (!HikeMessengerApp.isIndianUser() || !freeSMS)) {
			return;
		}

		Intent intent = Utils.createIntentFromContactInfo(contactInfo);
		intent.setClass(getContext(), ChatThread.class);
		getContext().startActivity(intent);
	}

	public void updatePendingRequests() {
		int pendingRequestsNum = drawerFavoritesAdapter.getPendingRequests();
		pendingRequests.setVisibility(pendingRequestsNum > 0 ? View.VISIBLE
				: View.GONE);
		pendingRequests.setText(pendingRequestsNum + "");
	}

	public void removeFromFavorite(ContactInfo contactInfo) {
		drawerFavoritesAdapter.removeFavoriteItem(contactInfo);
		updatePendingRequests();
	}

	public void addToRecommended(ContactInfo contactInfo) {
		drawerFavoritesAdapter.addRecommendedFavoriteItem(contactInfo);
		updatePendingRequests();
	}

	public void addToFavorite(ContactInfo contactInfo) {
		drawerFavoritesAdapter.addFavoriteItem(contactInfo);
		updatePendingRequests();
	}

	public void refreshFavoritesDrawer() {
		if (drawerFavoritesAdapter != null) {
			drawerFavoritesAdapter.notifyDataSetChanged();
			updatePendingRequests();
		}
	}

	public void addAutoRecommendedFavoritesList(
			List<ContactInfo> contactInfoList) {
		if (drawerFavoritesAdapter != null) {
			drawerFavoritesAdapter.addAutoRecommendedFavorites(contactInfoList);
			updatePendingRequests();
		}
	}

	public void freeSMSToggled(boolean freeSMS) {
		if (drawerFavoritesAdapter != null) {
			this.freeSMS = freeSMS;
			drawerFavoritesAdapter.freeSMSToggled(freeSMS);
		}
	}

	public void updateRecentContacts(ContactInfo contactInfo) {
		Log.d(getClass().getSimpleName(), "Update Recent List");
		drawerFavoritesAdapter.updateRecentContactsList(contactInfo);
		updatePendingRequests();
	}

	public void refreshFavorites(List<ContactInfo> favoriteList) {
		drawerFavoritesAdapter.refreshFavoritesList(favoriteList);
		updatePendingRequests();
	}

	public void refreshRecommendedFavorites(
			List<ContactInfo> recommendedFavoriteList) {
		drawerFavoritesAdapter
				.refreshRecommendedFavorites(recommendedFavoriteList);
		updatePendingRequests();
	}

	public void refreshRecents(List<ContactInfo> recents) {
		drawerFavoritesAdapter.refreshRecents(recents);
	}

	public void cancelFavoriteNotifications(
			NotificationManager notificationManager) {
		drawerFavoritesAdapter.cancelFavoriteNotifications(notificationManager);
	}

	public void onCreateFavoritesContextMenu(Activity activity, Menu menu,
			int position) {
		if (drawerFavoritesAdapter.getItemViewType(position) != FavoriteAdapterViewType.FAVORITE
				.ordinal()) {
			return;
		}
		MenuInflater menuInflater = activity.getMenuInflater();
		menuInflater.inflate(R.menu.favorites_menu, menu);
	}

	public boolean onFavoritesContextItemSelected(MenuItem menuItem) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuItem
				.getMenuInfo();
		ContactInfo contactInfo = drawerFavoritesAdapter.getItem((int) info.id);
		if (menuItem.getItemId() == R.id.remove_fav) {
			Pair<ContactInfo, FavoriteType> favoriteRemoved = new Pair<ContactInfo, FavoriteType>(
					contactInfo, FavoriteType.NOT_FAVORITE);
			HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_TOGGLED,
					favoriteRemoved);
			return true;
		}
		return false;
	}

	public void setUpLeftDrawerView() {

		profileImg = (ImageView) findViewById(R.id.profile_image);

		setProfileImage();
		renderLeftDrawerItems(PreferenceManager.getDefaultSharedPreferences(
				getContext()).getBoolean(HikeConstants.FREE_SMS_PREF, true));
	}

	public void renderLeftDrawerItems(boolean freeSMSOn) {

		boolean rewardsOn = accountPrefs.getBoolean(
				HikeMessengerApp.SHOW_REWARDS, false);

		int[] ids = { R.id.left_drawer_home, R.id.left_drawer_group_chat,
				R.id.left_drawer_tell_a_friend, R.id.left_drawer_free_sms,
				R.id.left_drawer_profile, R.id.left_drawer_rewards,
				R.id.left_drawer_help };

		for (int i = 0; i < ids.length; i++) {
			findViewById(ids[i]).setOnClickListener(this);
		}
		findViewById(R.id.left_drawer_free_sms).setVisibility(
				freeSMSOn ? VISIBLE : GONE);
		findViewById(R.id.left_credits_divider).setVisibility(
				freeSMSOn ? VISIBLE : GONE);

		findViewById(R.id.left_drawer_rewards).setVisibility(
				rewardsOn ? VISIBLE : GONE);
		findViewById(R.id.divider_rewards).setVisibility(
				rewardsOn ? VISIBLE : GONE);
	}

	@Override
	public void onClick(View v) {
		Log.d(getClass().getSimpleName(), "Drawer item clicked: " + v.getId());
		intent = null;
		boolean goingBackToHome = false;
		switch (v.getId()) {
		case R.id.left_drawer_home:
			Utils.logEvent(getContext(), HikeConstants.LogEvent.DRAWER_HOME);
			intent = activity instanceof MessagesList ? null : new Intent(
					getContext(), MessagesList.class);
			goingBackToHome = true;
			break;
		case R.id.left_drawer_group_chat:
			Utils.logEvent(getContext(),
					HikeConstants.LogEvent.DRAWER_GROUP_CHAT);
			intent = activity instanceof ChatThread ? null : new Intent(
					getContext(), ChatThread.class);
			if (intent != null) {
				intent.putExtra(HikeConstants.Extras.GROUP_CHAT, true);
			}
			handler.postDelayed(resetSidebar, 400);
			break;
		case R.id.left_drawer_tell_a_friend:
			Utils.logEvent(getContext(), HikeConstants.LogEvent.DRAWER_INVITE);
			intent = activity instanceof TellAFriend ? null : new Intent(
					getContext(), TellAFriend.class);
			break;
		case R.id.left_drawer_free_sms:
			Utils.logEvent(getContext(), HikeConstants.LogEvent.DRAWER_CREDITS);
			intent = activity instanceof CreditsActivity ? null : new Intent(
					getContext(), CreditsActivity.class);
			break;
		case R.id.left_drawer_profile:
			Utils.logEvent(getContext(), HikeConstants.LogEvent.DRAWER_PROFILE);
			intent = activity instanceof ProfileActivity ? null : new Intent(
					getContext(), ProfileActivity.class);
			break;
		case R.id.left_drawer_help:
			Utils.logEvent(getContext(), HikeConstants.LogEvent.DRAWER_HELP);
			intent = activity instanceof Tutorial ? null : new Intent(
					getContext(), Tutorial.class);
			if (intent != null) {
				intent.putExtra(HikeConstants.Extras.HELP_PAGE, true);
			}
			break;
		case R.id.left_drawer_rewards:
			intent = activity instanceof WebViewActivity ? null : new Intent(
					getContext(), WebViewActivity.class);
			if (intent != null) {
				intent.putExtra(HikeConstants.Extras.REWARDS_PAGE, true);
				intent.putExtra(
						HikeConstants.Extras.URL_TO_LOAD,
						AccountUtils.rewardsUrl
								+ accountPrefs.getString(
										HikeMessengerApp.REWARDS_TOKEN, ""));
				intent.putExtra(HikeConstants.Extras.TITLE, getContext()
						.getString(R.string.rewards));
			}
			break;
		}
		if (intent != null) {
			intent.putExtra(HikeConstants.Extras.GOING_BACK_TO_HOME,
					goingBackToHome);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			activity.startActivity(intent);
			/*
			 * We don't need this activity in the stack if it not the messages
			 * list screen.
			 */
			if (!(activity instanceof MessagesList)) {
				activity.finish();
			}
			if (!goingBackToHome) {
				activity.overridePendingTransition(
						R.anim.slide_in_right_noalpha, R.anim.alpha_out);
			} else {
				activity.overridePendingTransition(
						R.anim.slide_in_right_noalpha,
						R.anim.slide_out_left_noalpha);
			}
		} else {
			closeLeftSidebar(false);
		}
	}

	Runnable resetSidebar = new Runnable() {
		@Override
		public void run() {
			mContent.clearAnimation();
			closeLeftSidebar(true);
		}
	};

	public void setProfileImage() {
		String msisdn = accountPrefs.getString(HikeMessengerApp.MSISDN_SETTING,
				"");
		profileImg.setImageDrawable(IconCacheManager.getInstance()
				.getIconForMSISDN(msisdn));
	}

	@Override
	public void onFinishInflate() {
		super.onFinishInflate();
		mLeftSidebar = findViewById(R.id.animation_layout_left_sidebar);
		mRightSidebar = findViewById(R.id.animation_layout_right_sidebar);
		mContent = findViewById(R.id.animation_layout_content);

		if (mLeftSidebar == null) {
			throw new NullPointerException(
					"no view id = animation_left_sidebar");
		}

		if (mRightSidebar == null) {
			throw new NullPointerException(
					"no view id = animation_right_sidebar");
		}

		if (mContent == null) {
			throw new NullPointerException("no view id = animation_content");
		}

		LayoutParams leftLp = (LayoutParams) mLeftSidebar.getLayoutParams();
		leftLp.width = mLeftSidebarWidth;
		mLeftSidebar.setLayoutParams(leftLp);

		LayoutParams rightLp = (LayoutParams) mRightSidebar.getLayoutParams();
		rightLp.width = mRightSidebarWidth;
		mRightSidebar.setLayoutParams(rightLp);

		mLeftSidebar.setBackgroundDrawable(leftDrawerBg);
		mRightSidebar.setBackgroundDrawable(rightDrawerBg);
	}

	@Override
	public void onLayout(boolean changed, int l, int t, int r, int b) {
		/* the title bar assign top padding, drop it */
		mLeftSidebar.layout(l, 0, l + mLeftSidebarWidth,
				0 + mLeftSidebar.getMeasuredHeight());

		mRightSidebar.layout(r - mRightSidebarWidth, 0, r,
				0 + mRightSidebar.getMeasuredHeight());

		if (mLeftOpened) {
			mContent.layout(l + mLeftSidebarWidth, 0, r + mLeftSidebarWidth, b);
		} else if (mRightOpened) {
			mContent.layout(l - mRightSidebarWidth, 0, r - mRightSidebarWidth,
					b);
		} else {
			mContent.layout(l, 0, r, b);
		}
	}

	@Override
	public void onMeasure(int w, int h) {
		super.onMeasure(w, h);
		super.measureChildren(w, h);
	}

	@Override
	protected void measureChild(View child, int parentWSpec, int parentHSpec) {
		/* the max width of Sidebar is 90% of Parent */
		if (child == mLeftSidebar || child == mRightSidebar) {
			int mode = MeasureSpec.getMode(parentWSpec);
			int width = (int) (getMeasuredWidth() * 0.9);
			super.measureChild(child, MeasureSpec.makeMeasureSpec(width, mode),
					parentHSpec);
		} else {
			super.measureChild(child, parentWSpec, parentHSpec);
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (!isLeftOpening() && !isRightOpening()) {
			return false;
		}

		int action = ev.getAction();

		if (action != MotionEvent.ACTION_UP
				&& action != MotionEvent.ACTION_DOWN) {
			return false;
		}

		/*
		 * if user press and release both on Content while sidebar is opening,
		 * call listener. otherwise, pass the event to child.
		 */
		int x = (int) ev.getX();
		int y = (int) ev.getY();
		if (mContent.getLeft() < x && mContent.getRight() > x
				&& mContent.getTop() + topBarButtonWidth < y
				&& mContent.getBottom() > y) {
			if (action == MotionEvent.ACTION_DOWN) {
				mPressed = true;
			}

			if (mPressed && action == MotionEvent.ACTION_UP
					&& mListener != null) {
				mPressed = false;
				return mLeftOpened ? mListener
						.onContentTouchedWhenOpeningLeftSidebar() : mListener
						.onContentTouchedWhenOpeningRightSidebar();
			}
		} else {
			mPressed = false;
		}

		return false;
	}

	public void setListener(Listener l) {
		mListener = l;
		activity = (Activity) l;
	}

	/* to see if the Left Sidebar is visible */
	public boolean isLeftOpening() {
		return mLeftOpened;
	}

	/* to see if the Right Sidebar is visible */
	public boolean isRightOpening() {
		return mRightOpened;
	}

	public void toggleSidebar(boolean noAnimation, boolean leftSidebar) {
		if (isAnimating) {
			return;
		}
		isAnimating = true;

		if (leftSidebar) {
			if (mLeftOpened) {
				handler.post(new CloseDrawerAnimation(mLeftSidebar, mContent,
						noAnimation, true));
			} else {
				handler.post(new OpenDrawerAnimation(mLeftSidebar, mContent,
						noAnimation, true));
			}
		} else {
			if (mRightOpened) {
				handler.post(new CloseDrawerAnimation(mRightSidebar, mContent,
						noAnimation, false));
			} else {
				handler.post(new OpenDrawerAnimation(mRightSidebar, mContent,
						noAnimation, false));
			}
		}
	}

	public void openLeftSidebar() {
		if (!mLeftOpened) {
			toggleSidebar(false, true);
		}
	}

	public void closeLeftSidebar(boolean noAnimation) {
		if (mLeftOpened) {
			toggleSidebar(noAnimation, true);
		}
	}

	public void openRightSidebar() {
		if (!mRightOpened) {
			toggleSidebar(false, false);
		}
	}

	public void closeRightSidebar(boolean noAnimation) {
		if (mRightOpened) {
			toggleSidebar(noAnimation, false);
		}
	}

	public interface Listener {
		public boolean onContentTouchedWhenOpeningLeftSidebar();

		public boolean onContentTouchedWhenOpeningRightSidebar();

		public void rightSidebarOpened();
	}

	class OpenDrawerAnimation implements Runnable {

		View sidebar;
		View content;
		int contentPosition;
		int sidebarPosition;
		boolean noAnim;
		boolean leftDrawer;
		int sidebarOffset;
		int sidebarWidth;

		public OpenDrawerAnimation(View sidebar, View content, boolean noAnim,
				boolean leftDrawer) {
			this.leftDrawer = leftDrawer;

			this.contentPosition = 0;
			this.sidebarPosition = 0;

			this.sidebarOffset = leftDrawer ? mLeftSidebarOffsetForAnimation
					: mRightSidebarOffsetForAnimation;
			this.sidebarWidth = leftDrawer ? mLeftSidebarWidth
					: mRightSidebarWidth;

			this.content = content;

			this.sidebar = sidebar;
			this.sidebar.setVisibility(View.VISIBLE);
			this.sidebar.offsetLeftAndRight(leftDrawer ? -sidebarOffset
					: sidebarOffset);
			invalidate();

			this.noAnim = noAnim;
		}

		@Override
		public void run() {

			if (!noAnim) {
				float contentFactor = Math
						.max(0.1f,
								((float) (Math.abs(contentPosition) * 100 / sidebarWidth) / 100));

				float sidebarFactor = Math
						.max(0.1f,
								((float) (Math.abs(sidebarPosition) * 100 / sidebarOffset) / 100));

				int sidebarIncrements = (int) (((int) sidebarOffset / ANIMATION_STEPS) * interpolator
						.getInterpolation(sidebarFactor));
				int contentIncrements = (int) (((int) sidebarWidth / ANIMATION_STEPS) * interpolator
						.getInterpolation(contentFactor));

				sidebarIncrements = Math.min(sidebarIncrements, sidebarOffset
						- Math.abs(sidebarPosition));
				contentIncrements = Math.min(contentIncrements, sidebarWidth
						- Math.abs(contentPosition));

				sidebarIncrements = Math.max(sidebarIncrements, 1);
				contentIncrements = Math.max(contentIncrements, 1);

				if (!leftDrawer) {
					sidebarIncrements = -sidebarIncrements;
					contentIncrements = -contentIncrements;
				}

				if ((Math.abs(contentPosition) < sidebarWidth)
						|| (Math.abs(sidebarPosition) < sidebarOffset)) {
					if (Math.abs(contentPosition) < sidebarWidth) {
						contentPosition += contentIncrements;
						content.offsetLeftAndRight(contentIncrements);
					}

					if (Math.abs(sidebarPosition) < sidebarOffset) {
						sidebarPosition += sidebarIncrements;
						sidebar.offsetLeftAndRight(sidebarIncrements);
					} else {
						int offset = sidebarOffset
								- (leftDrawer ? sidebarPosition
										: -sidebarPosition);
						sidebarPosition = leftDrawer ? sidebarOffset
								: -sidebarOffset;
						sidebar.offsetLeftAndRight(offset);
					}

					handler.postDelayed(this, DURATION_BETWEEN_EACH_STEP);
				} else {
					finishAnim();
				}
			} else {
				finishAnim();
			}
			invalidate();
		}

		private void finishAnim() {
			isAnimating = false;
			if (leftDrawer) {
				mLeftOpened = true;
			} else {
				mListener.rightSidebarOpened();
				mRightOpened = true;
			}
		}
	}

	class CloseDrawerAnimation implements Runnable {

		View sidebar;
		View content;
		int contentPosition;
		int sidebarPosition;
		boolean noAnim;
		boolean leftDrawer;
		int sidebarOffset;
		int sidebarWidth;

		public CloseDrawerAnimation(View sidebar, View content, boolean noAnim,
				boolean leftDrawer) {
			this.leftDrawer = leftDrawer;

			this.sidebarOffset = leftDrawer ? mLeftSidebarOffsetForAnimation
					: mRightSidebarOffsetForAnimation;
			this.sidebarWidth = leftDrawer ? mLeftSidebarWidth
					: mRightSidebarWidth;

			this.contentPosition = leftDrawer ? sidebarWidth : -sidebarWidth;
			this.sidebarPosition = leftDrawer ? sidebarOffset : -sidebarOffset;

			this.content = content;
			this.sidebar = sidebar;

			this.noAnim = noAnim;
		}

		@Override
		public void run() {

			if (!noAnim) {
				float contentFactor = Math
						.max(0.1f,
								((float) ((sidebarWidth - Math
										.abs(contentPosition)) * 100 / sidebarWidth) / 100));

				float sidebarFactor = Math
						.max(0.1f,
								((float) ((sidebarOffset - Math
										.abs(sidebarPosition)) * 100 / sidebarOffset) / 100));

				int sidebarIncrements = (int) (((int) sidebarOffset / ANIMATION_STEPS) * interpolator
						.getInterpolation(sidebarFactor));
				int contentIncrements = (int) (((int) sidebarWidth / ANIMATION_STEPS) * interpolator
						.getInterpolation(contentFactor));

				sidebarIncrements = Math.min(sidebarIncrements, sidebarOffset
						+ Math.abs(sidebarPosition));
				contentIncrements = Math.min(contentIncrements, sidebarWidth
						+ Math.abs(contentPosition));

				sidebarIncrements = Math.max(sidebarIncrements, 1);
				contentIncrements = Math.max(contentIncrements, 1);

				if (leftDrawer) {
					sidebarIncrements = -sidebarIncrements;
					contentIncrements = -contentIncrements;
				}

				if ((Math.abs(contentPosition) > 0)
						|| (Math.abs(sidebarPosition) > 0)) {
					if (Math.abs(contentPosition) > 0) {
						contentPosition += contentIncrements;
						content.offsetLeftAndRight(contentIncrements);
					}

					if ((leftDrawer && sidebarPosition > 0)
							|| (!leftDrawer && sidebarPosition < 0)) {
						sidebarPosition += sidebarIncrements;
						sidebar.offsetLeftAndRight(sidebarIncrements);
					} else {
						int offset = -sidebarPosition;
						sidebarPosition = 0;
						sidebar.offsetLeftAndRight(offset);
					}

					handler.postDelayed(this, DURATION_BETWEEN_EACH_STEP);
				} else {
					finishAnim();
				}
			} else {
				finishAnim();
			}
			invalidate();
		}

		private void finishAnim() {
			isAnimating = false;
			sidebar.setVisibility(View.INVISIBLE);
			if (leftDrawer) {
				mLeftOpened = false;
				if (!noAnim) {
					sidebar.offsetLeftAndRight(sidebarOffset);
				} else {
					content.offsetLeftAndRight(-sidebarWidth);
				}
			} else {
				mRightOpened = false;
				if (!noAnim) {
					sidebar.offsetLeftAndRight(-sidebarOffset);
				} else {
					content.offsetLeftAndRight(sidebarWidth);
				}
			}
		}
	}
}
