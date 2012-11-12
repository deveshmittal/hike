package com.bsb.hike.view;

import java.util.Calendar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
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
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.CreditsActivity;
import com.bsb.hike.ui.MessagesList;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.Tutorial;
import com.bsb.hike.utils.Utils;

public class DrawerLayout extends RelativeLayout implements
		View.OnClickListener, OnItemClickListener {

	public final static int DURATION = 250;

	private boolean mLeftOpened;
	private boolean mRightOpened;

	private View mLeftSidebar;
	private View mRightSidebar;

	private View mContent;
	private int mSidebarWidth;
	private int mSidebarOffsetForAnimation;
	private int topBarButtonWidth;

	// Left drawer animations
	private Animation contentAnimationLeftOut;
	private Animation leftSidebarTranslateAnimationIn;
	private Animation contentAnimationRightIn;
	private Animation leftSidebarTranslateAnimationOut;

	// Right drawer animations
	private Animation contentAnimationRightOut;
	private Animation rightSidebarTranslateAnimationIn;
	private Animation contentAnimationLeftIn;
	private Animation rightSidebarTranslateAnimationOut;

	private OpenListener mLeftOpenListener;
	private CloseListener mLeftCloseListener;

	private OpenListener mRightOpenListener;
	private CloseListener mRightCloseListener;

	private Listener mListener;

	private boolean mPressed = false;

	private TextView creditsNum;

	private SharedPreferences accountPrefs;

	private Intent intent;

	private Activity activity;

	private Handler handler;

	private ImageView profileImg;

	private TextView profileName;

	private ContactInfo me;

	private BitmapDrawable sideBarBackground;

	/*
	 * Making this static in order to prevent having multiple lists of contacts
	 * since we show favorites in multiple screens.
	 */
	private static DrawerFavoritesAdapter drawerFavoritesAdapter;

	private TimeOfDay time;

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
		mSidebarWidth = (int) ((isPortrait ? context.getResources()
				.getDisplayMetrics().widthPixels : context.getResources()
				.getDisplayMetrics().heightPixels) - topBarButtonWidth);
		mSidebarOffsetForAnimation = (int) (80 * Utils.densityMultiplier);

		initializeLeftDrawerAnimations();
		initializeRightDrawerAnimations();

		/*
		 * Fix for android v2.3 and below specific bug where the bitmap is not
		 * tiled and gets stretched instead if we use the xml. So we're creating
		 * it via code.
		 * http://stackoverflow.com/questions/7586209/xml-drawable-bitmap
		 * -tilemode-bug
		 */
		sideBarBackground = new BitmapDrawable(BitmapFactory.decodeResource(
				getResources(), R.drawable.bg_drawer));
		sideBarBackground.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
	}

	private void initializeLeftDrawerAnimations() {
		/* Close Animations */
		contentAnimationRightIn = new TranslateAnimation(0, -mSidebarWidth, 0,
				0);
		contentAnimationRightIn.setFillAfter(true);
		contentAnimationRightIn.setFillEnabled(true);

		leftSidebarTranslateAnimationOut = new TranslateAnimation(0,
				-mSidebarOffsetForAnimation, 0, 0);

		/* Open Animations */
		contentAnimationLeftOut = new TranslateAnimation(0, mSidebarWidth, 0, 0);
		contentAnimationLeftOut.setFillAfter(true);
		contentAnimationLeftOut.setFillEnabled(true);

		leftSidebarTranslateAnimationIn = new TranslateAnimation(
				-mSidebarOffsetForAnimation, 0, 0, 0);
	}

	private void initializeRightDrawerAnimations() {
		/* Close Animations */
		contentAnimationLeftIn = new TranslateAnimation(0, mSidebarWidth, 0, 0);
		contentAnimationLeftIn.setFillAfter(true);
		contentAnimationLeftIn.setFillEnabled(true);

		rightSidebarTranslateAnimationOut = new TranslateAnimation(0,
				mSidebarOffsetForAnimation, 0, 0);

		/* Open Animations */
		contentAnimationRightOut = new TranslateAnimation(0, -mSidebarWidth, 0,
				0);
		contentAnimationRightOut.setFillAfter(true);
		contentAnimationRightOut.setFillEnabled(true);

		rightSidebarTranslateAnimationIn = new TranslateAnimation(
				mSidebarOffsetForAnimation, 0, 0, 0);
	}

	public void setUpRightDrawerView(Activity activity) {
		ListView favoriteListView = (ListView) findViewById(R.id.favorite_list);

		if (drawerFavoritesAdapter == null) {
			Log.d(getClass().getSimpleName(), "Initialising favorites adapter");
			drawerFavoritesAdapter = new DrawerFavoritesAdapter(getContext());
		}
		favoriteListView.setAdapter(drawerFavoritesAdapter);

		favoriteListView.setOnItemClickListener(this);
		activity.registerForContextMenu(favoriteListView);

		setStatus();
	}

	private void setStatus() {
		ViewGroup background = (ViewGroup) findViewById(R.id.time_base_status);

		int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		if (hour >= 6 && hour < 12) // Morning
		{
			if (time == TimeOfDay.MORNING) {
				return;
			}
			time = TimeOfDay.MORNING;
			background.setBackgroundResource(R.drawable.morning);
		} else if (hour >= 12 && hour < 21) // Day
		{
			if (time == TimeOfDay.DAY) {
				return;
			}
			time = TimeOfDay.DAY;
			background.setBackgroundResource(R.drawable.day);
		} else if (hour >= 21 || hour < 6) // Night
		{
			if (time == TimeOfDay.NIGHT) {
				return;
			}
			time = TimeOfDay.NIGHT;
			background.setBackgroundResource(R.drawable.night);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position,
			long id) {
		Intent intent = Utils
				.createIntentFromContactInfo(drawerFavoritesAdapter
						.getItem(position));
		intent.setClass(getContext(), ChatThread.class);
		getContext().startActivity(intent);
	}

	public void removeFromFavorite(ContactInfo contactInfo) {
		drawerFavoritesAdapter.removeFavoriteItem(contactInfo);
	}

	public void addToRecommended(ContactInfo contactInfo) {
		drawerFavoritesAdapter.addRecommendedFavoriteItem(contactInfo);
	}

	public void addToFavorite(ContactInfo contactInfo) {
		drawerFavoritesAdapter.addFavoriteItem(contactInfo);
	}

	public void refreshFavoritesDrawer() {
		if (drawerFavoritesAdapter != null) {
			drawerFavoritesAdapter.notifyDataSetChanged();
		}
	}

	public void updateRecentContacts(String msisdn) {
		Log.d(getClass().getSimpleName(), "Update Recent List");
		ContactInfo contactInfo = HikeUserDatabase.getInstance()
				.getContactInfoFromMSISDN(msisdn, true);
		drawerFavoritesAdapter.updateRecentContactsList(contactInfo);
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
		LayoutInflater layoutInflater = LayoutInflater.from(getContext());

		profileImg = (ImageView) findViewById(R.id.profile_image);
		profileName = (TextView) findViewById(R.id.name);

		setProfileName();
		setProfileImage();

		int[] parentIds = { R.id.top_half_items_container,
				R.id.bottom_half_items_container };
		int itemNumber = 0;
		for (int i = 0; i < parentIds.length; i++) {
			String[] itemTexts = i == 0 ? getResources().getStringArray(
					R.array.top_half_drawer_text) : getResources()
					.getStringArray(R.array.bottom_half_drawer_text);
			int[] itemIcons = i == 0 ? new int[] { R.drawable.ic_drawer_home,
					R.drawable.ic_drawer_group_chat,
					R.drawable.ic_drawer_invite } : new int[] {
					R.drawable.ic_drawer_free_sms,
					R.drawable.ic_drawer_profile, R.drawable.ic_drawer_help };

			ViewGroup parentView = (ViewGroup) findViewById(parentIds[i]);

			for (int j = 0; j < itemTexts.length; j++) {
				View itemView = layoutInflater.inflate(R.layout.drawer_item,
						null);
				itemView.setFocusable(true);
				itemView.setClickable(true);

				TextView itemTxt = (TextView) itemView
						.findViewById(R.id.item_name);
				ImageView itemImg = (ImageView) itemView
						.findViewById(R.id.item_icon);

				itemTxt.setText(itemTexts[j]);
				itemImg.setImageResource(itemIcons[j]);

				if (itemTexts[j].equals(getContext().getString(
						R.string.free_sms_txt))) {
					creditsNum = (TextView) itemView
							.findViewById(R.id.credit_num);
					creditsNum.setVisibility(View.VISIBLE);
					creditsNum.setText(Integer.toString(accountPrefs.getInt(
							HikeMessengerApp.SMS_SETTING, 0)));
				}
				if (j == 0) {
					itemView.findViewById(R.id.divider).setVisibility(
							View.VISIBLE);
					itemView.setBackgroundResource(R.drawable.drawer_top_item_selector);
				} else if (j == itemTexts.length - 1) {
					itemView.findViewById(R.id.divider)
							.setVisibility(View.GONE);
					itemView.setBackgroundResource(R.drawable.drawer_bottom_item_selector);
				} else {
					itemView.findViewById(R.id.divider).setVisibility(
							View.VISIBLE);
					itemView.setBackgroundResource(R.drawable.drawer_center_item_selector);
				}
				itemView.setFocusable(true);
				int id = LeftDrawerItems.values()[itemNumber++].ordinal();
				switch (LeftDrawerItems.values()[id]) {
				case HOME:
					if (activity instanceof MessagesList) {
						itemView.setBackgroundResource(R.drawable.drawer_top_item_pressed);
					}
					break;
				case GROUP_CHAT:
					if (activity instanceof ChatThread) {
						itemView.setBackgroundResource(R.drawable.drawer_center_item_pressed);
					}
					break;
				case FREE_SMS:
					if (activity instanceof CreditsActivity) {
						itemView.setBackgroundResource(R.drawable.drawer_top_item_pressed);
					}
					break;
				case PROFILE:
					if (activity instanceof ProfileActivity) {
						itemView.setBackgroundResource(R.drawable.drawer_center_item_pressed);
					}
					break;
				case HELP:
					if (activity instanceof Tutorial) {
						itemView.setBackgroundResource(R.drawable.drawer_bottom_item_pressed);
					}
					break;
				}
				itemView.setId(id);
				itemView.setOnClickListener(this);
				parentView.addView(itemView);
			}
			parentView.setFocusable(true);
		}
	}

	public void onClick(View v) {
		Log.d(getClass().getSimpleName(), "Drawer item clicked: " + v.getId());
		intent = null;
		boolean goingBackToHome = false;
		switch (LeftDrawerItems.values()[v.getId()]) {
		case HOME:
			Utils.logEvent(getContext(), HikeConstants.LogEvent.DRAWER_HOME);
			intent = activity instanceof MessagesList ? null : new Intent(
					getContext(), MessagesList.class);
			goingBackToHome = true;
			break;
		case GROUP_CHAT:
			Utils.logEvent(getContext(),
					HikeConstants.LogEvent.DRAWER_GROUP_CHAT);
			intent = activity instanceof ChatThread ? null : new Intent(
					getContext(), ChatThread.class);
			if (intent != null) {
				intent.putExtra(HikeConstants.Extras.GROUP_CHAT, true);
			}
			handler.postDelayed(resetSidebar, 400);
			break;
		case TELL_A_FRIEND:
			Utils.logEvent(getContext(), HikeConstants.LogEvent.DRAWER_INVITE);
			Utils.startShareIntent(getContext(),
					Utils.getInviteMessage(getContext()));
			break;
		case FREE_SMS:
			Utils.logEvent(getContext(), HikeConstants.LogEvent.DRAWER_CREDITS);
			intent = activity instanceof CreditsActivity ? null : new Intent(
					getContext(), CreditsActivity.class);
			break;
		case PROFILE:
			Utils.logEvent(getContext(), HikeConstants.LogEvent.DRAWER_PROFILE);
			intent = activity instanceof ProfileActivity ? null : new Intent(
					getContext(), ProfileActivity.class);
			break;
		case HELP:
			Utils.logEvent(getContext(), HikeConstants.LogEvent.DRAWER_HELP);
			intent = activity instanceof Tutorial ? null : new Intent(
					getContext(), Tutorial.class);
			if (intent != null) {
				intent.putExtra(HikeConstants.Extras.HELP_PAGE, true);
			}
			break;
		}
		if (intent != null) {
			intent.putExtra(HikeConstants.Extras.GOING_BACK_TO_HOME,
					goingBackToHome);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			activity.startActivity(intent);
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

	public void updateCredits(int credits) {
		creditsNum.setText(Integer.toString(credits));
	}

	public void setProfileImage() {
		profileImg.setImageDrawable(IconCacheManager.getInstance()
				.getIconForMSISDN(me.getMsisdn()));
	}

	public void setProfileName() {
		me = Utils.getUserContactInfo(accountPrefs);
		profileName.setText(me.getName());
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
		leftLp.width = mSidebarWidth;
		mLeftSidebar.setLayoutParams(leftLp);

		LayoutParams rightLp = (LayoutParams) mRightSidebar.getLayoutParams();
		rightLp.width = mSidebarWidth;
		mRightSidebar.setLayoutParams(rightLp);

		mLeftSidebar.setBackgroundDrawable(sideBarBackground);
		mRightSidebar.setBackgroundDrawable(sideBarBackground);

		mLeftOpenListener = new OpenListener(mLeftSidebar, mContent, true);
		mLeftCloseListener = new CloseListener(mLeftSidebar, mContent, true);

		mRightOpenListener = new OpenListener(mRightSidebar, mContent, false);
		mRightCloseListener = new CloseListener(mRightSidebar, mContent, false);
	}

	@Override
	public void onLayout(boolean changed, int l, int t, int r, int b) {
		/* the title bar assign top padding, drop it */
		mLeftSidebar.layout(l, 0, l + mSidebarWidth,
				0 + mLeftSidebar.getMeasuredHeight());

		mRightSidebar.layout(r - mSidebarWidth, 0, r,
				0 + mRightSidebar.getMeasuredHeight());

		if (mLeftOpened) {
			mContent.layout(l + mSidebarWidth, 0, r + mSidebarWidth, b);
		} else if (mRightOpened) {
			mContent.layout(l - mSidebarWidth, 0, r - mSidebarWidth, b);
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
		if (mContent.getAnimation() != null) {
			return;
		}

		if (leftSidebar) {
			if (mLeftOpened) {
				/* opened, make close animation */
				animateLayouts(mLeftSidebar, leftSidebarTranslateAnimationOut,
						contentAnimationRightIn, mLeftCloseListener,
						noAnimation);
			} else {
				animateLayouts(mLeftSidebar, leftSidebarTranslateAnimationIn,
						contentAnimationLeftOut, mLeftOpenListener, noAnimation);
			}
		} else {
			if (mRightOpened) {
				animateLayouts(mRightSidebar,
						rightSidebarTranslateAnimationOut,
						contentAnimationLeftIn, mRightCloseListener,
						noAnimation);
			} else {
				setStatus();
				animateLayouts(mRightSidebar, rightSidebarTranslateAnimationIn,
						contentAnimationRightOut, mRightOpenListener,
						noAnimation);
			}
		}
	}

	private void animateLayouts(View sidebar, Animation sidebarAnim,
			Animation contentAnim, AnimationListener listener,
			boolean noAnimation) {
		contentAnim.setDuration(noAnimation ? 0 : DURATION);
		sidebarAnim.setDuration(noAnimation ? 0 : DURATION);
		/* opened, make close animation */
		sidebar.startAnimation(sidebarAnim);
		mContent.startAnimation(contentAnim);
		contentAnim.setAnimationListener(listener);
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

	class OpenListener implements Animation.AnimationListener {
		View iSidebar;
		View iContent;
		boolean iLeftSidebar;

		OpenListener(View sidebar, View content, boolean leftSidebar) {
			iSidebar = sidebar;
			iContent = content;
			iLeftSidebar = leftSidebar;
		}

		public void onAnimationRepeat(Animation animation) {
		}

		public void onAnimationStart(Animation animation) {
			iSidebar.setVisibility(View.VISIBLE);
		}

		public void onAnimationEnd(Animation animation) {
			iContent.clearAnimation();
			iSidebar.clearAnimation();
			if (iLeftSidebar) {
				mLeftOpened = !mLeftOpened;
			} else {
				mRightOpened = !mRightOpened;
			}
			requestLayout();
		}
	}

	class CloseListener implements Animation.AnimationListener {
		View iSidebar;
		View iContent;
		boolean iLeftSidebar;

		CloseListener(View sidebar, View content, boolean leftSidebar) {
			iSidebar = sidebar;
			iContent = content;
			iLeftSidebar = leftSidebar;
		}

		public void onAnimationRepeat(Animation animation) {
		}

		public void onAnimationStart(Animation animation) {
		}

		public void onAnimationEnd(Animation animation) {
			iContent.clearAnimation();
			iSidebar.clearAnimation();
			iSidebar.setVisibility(View.INVISIBLE);
			if (iLeftSidebar) {
				mLeftOpened = !mLeftOpened;
			} else {
				mRightOpened = !mRightOpened;
			}
			requestLayout();
		}
	}

	public interface Listener {
		public boolean onContentTouchedWhenOpeningLeftSidebar();

		public boolean onContentTouchedWhenOpeningRightSidebar();
	}
}
