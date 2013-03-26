package com.bsb.hike.view;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
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
import com.bsb.hike.ui.SettingsActivity;
import com.bsb.hike.ui.TellAFriend;
import com.bsb.hike.ui.WebViewActivity;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.CustomInterpolator;
import com.bsb.hike.utils.Utils;

public class DrawerLayout extends RelativeLayout implements
		OnItemClickListener, View.OnClickListener {

	private final int animationSteps;

	private final static int DURATION_BETWEEN_EACH_STEP = 16;

	public static enum CurrentState {
		NONE, LEFT, RIGHT
	}

	private CurrentState currentState;

	private View mLeftSidebar;
	private View mRightSidebar;

	private View mContent;
	private int mRightSidebarWidth;
	private int mRightSidebarOffsetForAnimation;
	private int mLeftSidebarWidth;
	private int mLeftSidebarOffsetForAnimation;
	private int topBarButtonWidth;

	private int contentCurrentPosition;
	private int contentFinalPosition;
	private int sidebarCurrentPosition;
	private int sidebarFinalPosition;

	private int rightSidebarFinalPosition;

	private Listener mListener;

	private boolean mPressed = false;

	private TextView creditsNum;

	private TextView talkTimeNum;

	private SharedPreferences accountPrefs;

	private Intent intent;

	private Activity activity;

	private Handler handler;

	private ImageView profileImg;

	private BitmapDrawable leftDrawerBg;

	private BitmapDrawable rightDrawerBg;

	private DrawerFavoritesAdapter drawerFavoritesAdapter;

	private boolean freeSMS;

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
		setChildrenDrawingOrderEnabled(true);
		currentState = CurrentState.NONE;
		accountPrefs = getContext().getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		handler = new Handler();
		animationSteps = Math.min((int) (8 * Utils.densityMultiplier), 10);
		topBarButtonWidth = (int) (48 * Utils.densityMultiplier);
		boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
		mRightSidebarWidth = (int) ((isPortrait ? context.getResources()
				.getDisplayMetrics().widthPixels : context.getResources()
				.getDisplayMetrics().heightPixels) - topBarButtonWidth);
		mRightSidebarOffsetForAnimation = (int) (100 * Utils.densityMultiplier);
		rightSidebarFinalPosition = context.getResources().getDisplayMetrics().widthPixels
				- mRightSidebarWidth;

		mLeftSidebarWidth = mRightSidebarWidth;
		mLeftSidebarOffsetForAnimation = mRightSidebarOffsetForAnimation;
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

		drawerFavoritesAdapter = new DrawerFavoritesAdapter(getContext());
		favoriteListView.setAdapter(drawerFavoritesAdapter);

		favoriteListView.setOnItemClickListener(this);
		activity.registerForContextMenu(favoriteListView);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position,
			long id) {

		ContactInfo contactInfo = drawerFavoritesAdapter.getItem(position);

		Intent intent = Utils.createIntentFromContactInfo(contactInfo);
		intent.setClass(getContext(), ChatThread.class);
		getContext().startActivity(intent);
	}

	public void removeFromFavorite(ContactInfo contactInfo) {
		drawerFavoritesAdapter.removeFavoriteItem(contactInfo);
	}

	public void addToFavorite(ContactInfo contactInfo) {
		drawerFavoritesAdapter.addFavoriteItem(contactInfo);
	}

	public void refreshFavoritesDrawer() {
		if (drawerFavoritesAdapter != null) {
			drawerFavoritesAdapter.notifyDataSetChanged();
		}
	}

	public void updateRecentContacts(ContactInfo contactInfo) {
		Log.d(getClass().getSimpleName(), "Update Recent List");
		drawerFavoritesAdapter.updateRecentContactsList(contactInfo);
	}

	public void refreshFavorites(List<ContactInfo> favoriteList) {
		drawerFavoritesAdapter.refreshFavoritesList(favoriteList);
	}

	public void refreshRecents(List<ContactInfo> recents) {
		drawerFavoritesAdapter.refreshRecents(recents);
	}

	public void removeContact(ContactInfo contactInfo) {
		drawerFavoritesAdapter.removeContact(contactInfo);
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

	public void updateStatus(String status, int moodId) {
		drawerFavoritesAdapter.updateStatus(status, moodId);
	}

	public boolean onFavoritesContextItemSelected(MenuItem menuItem) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuItem
				.getMenuInfo();
		ContactInfo contactInfo = drawerFavoritesAdapter.getItem((int) info.id);
		if (menuItem.getItemId() == R.id.remove_fav) {
			FavoriteType favoriteType;
			if (contactInfo.getFavoriteType() == FavoriteType.FRIEND) {
				favoriteType = FavoriteType.REQUEST_RECEIVED_REJECTED;
			} else {
				favoriteType = FavoriteType.NOT_FRIEND;
			}
			Pair<ContactInfo, FavoriteType> favoriteRemoved = new Pair<ContactInfo, FavoriteType>(
					contactInfo, favoriteType);
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

		int[] ids = { R.id.left_drawer_home, R.id.left_drawer_tell_a_friend,
				R.id.left_drawer_free_sms, R.id.left_drawer_profile,
				R.id.left_drawer_rewards, R.id.left_drawer_settings };

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

		creditsNum = (TextView) findViewById(R.id.credit_num);
		updateCredits(accountPrefs.getInt(HikeMessengerApp.SMS_SETTING, 0));

		talkTimeNum = (TextView) findViewById(R.id.talk_time_num);
		updateTalkTime(accountPrefs.getInt(HikeMessengerApp.TALK_TIME, 0));

		TextView withLoveTextView = (TextView) findViewById(R.id.made_with_love);

		String love = getContext().getString(R.string.love);
		String withLove = getContext().getString(R.string.with_love);

		Drawable drawable = getContext().getResources().getDrawable(
				R.drawable.ic_left_drawer_heart);
		drawable.setBounds(0, -10, drawable.getIntrinsicWidth(),
				drawable.getIntrinsicHeight() - 10);

		SpannableStringBuilder ssb = new SpannableStringBuilder(withLove);
		ssb.setSpan(new ImageSpan(drawable), withLove.indexOf(love),
				withLove.indexOf(love) + love.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		withLoveTextView.setText(ssb);
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
		case R.id.left_drawer_settings:
			Utils.logEvent(getContext(), HikeConstants.LogEvent.DRAWER_SETTINGS);
			intent = activity instanceof SettingsActivity ? null : new Intent(
					getContext(), SettingsActivity.class);
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
			closeSidebar(false);
		}
	}

	Runnable resetSidebar = new Runnable() {
		@Override
		public void run() {
			mContent.clearAnimation();
			closeSidebar(true);
		}
	};

	public void setProfileImage() {
		String msisdn = accountPrefs.getString(HikeMessengerApp.MSISDN_SETTING,
				"");
		profileImg.setImageDrawable(IconCacheManager.getInstance()
				.getIconForMSISDN(msisdn));
	}

	public void updateCredits(int credits) {
		if (creditsNum != null) {
			creditsNum.setText(Integer.toString(credits));
		}
	}

	public void updateTalkTime(int talkTime) {
		if (talkTimeNum != null) {
			talkTimeNum.setVisibility(talkTime > 0 ? View.VISIBLE
					: View.INVISIBLE);
			talkTimeNum.setText(Integer.toString(talkTime));
		}
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

		int width = r - l;
		int height = b - t;

		mContent.layout(0, 0, width, height);

		mRightSidebar.layout(rightSidebarFinalPosition, 0, width, height);

		mLeftSidebar.layout(0, 0, width, height);
	}

	@Override
	public void onMeasure(int w, int h) {

		int width = MeasureSpec.getSize(w);
		int height = MeasureSpec.getSize(h);

		super.setMeasuredDimension(width, height);

		if (mLeftSidebar != null) {
			measureChild(mLeftSidebar, MeasureSpec.makeMeasureSpec(
					mLeftSidebarWidth, MeasureSpec.EXACTLY), h);
		}
		if (mRightSidebar != null) {
			measureChild(mRightSidebar, MeasureSpec.makeMeasureSpec(
					mRightSidebarWidth, MeasureSpec.EXACTLY), h);
		}
		if (mContent != null) {
			measureChild(mContent,
					MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), h);
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (currentState == CurrentState.NONE) {
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
		if (((currentState == CurrentState.LEFT && mLeftSidebarWidth < x) || (currentState == CurrentState.RIGHT && topBarButtonWidth > x))
				&& (mContent.getTop() + topBarButtonWidth < y && mContent
						.getBottom() > y)) {
			if (action == MotionEvent.ACTION_DOWN) {
				mPressed = true;
			}

			if (mPressed && action == MotionEvent.ACTION_UP
					&& mListener != null) {
				mPressed = false;
				return currentState == CurrentState.LEFT ? mListener
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

	public void setActivity(Activity a) {
		activity = a;
	}

	public CurrentState getCurrentState() {
		return currentState;
	}

	public void toggleSidebar(boolean noAnimation, boolean leftSidebar) {
		if (isAnimating) {
			return;
		}
		isAnimating = true;

		if (leftSidebar) {
			if (currentState != CurrentState.NONE) {
				handler.post(new CloseDrawerAnimation(mLeftSidebar, mContent,
						noAnimation, true));
			} else {
				handler.post(new OpenDrawerAnimation(mLeftSidebar, mContent,
						noAnimation, true));
			}
		} else {
			if (currentState != CurrentState.NONE) {
				handler.post(new CloseDrawerAnimation(mRightSidebar, mContent,
						noAnimation, false));
			} else {
				handler.post(new OpenDrawerAnimation(mRightSidebar, mContent,
						noAnimation, false));
			}
		}
	}

	public void closeSidebar(boolean noAnimation) {
		toggleSidebar(noAnimation, currentState == CurrentState.LEFT ? true
				: false);
	}

	public void openLeftSidebar() {
		if (currentState == CurrentState.NONE) {
			toggleSidebar(false, true);
		}
	}

	public void openRightSidebar() {
		if (currentState == CurrentState.NONE) {
			toggleSidebar(false, false);
		}
	}

	public interface Listener {
		public boolean onContentTouchedWhenOpeningLeftSidebar();

		public boolean onContentTouchedWhenOpeningRightSidebar();
	}

	public boolean isAnimating() {
		return isAnimating;
	}

	class OpenDrawerAnimation implements Runnable {

		View sidebar;
		View content;
		boolean noAnim;
		boolean leftDrawer;
		int sidebarOffset;

		public OpenDrawerAnimation(View sidebar, View content, boolean noAnim,
				boolean leftDrawer) {
			this.leftDrawer = leftDrawer;

			contentCurrentPosition = 0;
			sidebarCurrentPosition = leftDrawer ? -mLeftSidebarOffsetForAnimation
					: mRightSidebarOffsetForAnimation;

			sidebarFinalPosition = 0;
			contentFinalPosition = leftDrawer ? mLeftSidebarWidth
					: mRightSidebarWidth;

			sidebarOffset = leftDrawer ? mLeftSidebarOffsetForAnimation
					: mRightSidebarOffsetForAnimation;

			this.content = content;

			this.sidebar = sidebar;
			this.sidebar.setVisibility(View.VISIBLE);
			this.sidebar.scrollTo(-sidebarCurrentPosition, 0);

			this.noAnim = noAnim;

			currentState = leftDrawer ? CurrentState.LEFT : CurrentState.RIGHT;
		}

		@Override
		public void run() {

			if (!noAnim) {
				float contentFactor = Math
						.max(0.1f,
								((float) (Math.abs(contentCurrentPosition) * 100 / contentFinalPosition) / 100));

				float sidebarFactor = Math
						.max(0.1f,
								((float) ((sidebarOffset - Math
										.abs(sidebarCurrentPosition)) * 100 / sidebarOffset) / 100));

				int sidebarIncrements = (int) (((int) sidebarOffset / (animationSteps)) * interpolator
						.getInterpolation(sidebarFactor));
				int contentIncrements = (int) (((int) contentFinalPosition / animationSteps) * interpolator
						.getInterpolation(contentFactor));

				sidebarIncrements = Math.min(sidebarIncrements,
						Math.abs(sidebarCurrentPosition));
				contentIncrements = Math
						.min(contentIncrements,
								contentFinalPosition
										- Math.abs(contentCurrentPosition));

				sidebarIncrements = Math.max(sidebarIncrements, 1);
				contentIncrements = Math.max(contentIncrements, 1);

				if (!leftDrawer) {
					sidebarIncrements = -sidebarIncrements;
					contentIncrements = -contentIncrements;
				}

				boolean sidebarAnimComplete = (currentState == CurrentState.LEFT && sidebarCurrentPosition <= 0)
						|| (currentState == CurrentState.RIGHT && sidebarCurrentPosition >= 0);
				if ((Math.abs(contentCurrentPosition) < contentFinalPosition)
						|| (sidebarAnimComplete)) {
					if (Math.abs(contentCurrentPosition) < contentFinalPosition) {
						contentCurrentPosition += contentIncrements;
						content.scrollTo(-contentCurrentPosition, 0);
					}

					if (sidebarAnimComplete) {
						sidebarCurrentPosition += sidebarIncrements;
						sidebar.scrollTo(-sidebarCurrentPosition, 0);
					}

					handler.postDelayed(this, DURATION_BETWEEN_EACH_STEP);
				} else {
					finishAnim();
				}
			} else {
				finishAnim();
			}
		}

		private void finishAnim() {
			isAnimating = false;
			sidebarCurrentPosition = sidebarFinalPosition;
			if (leftDrawer) {
				currentState = CurrentState.LEFT;
				contentCurrentPosition = contentFinalPosition;
			} else {
				currentState = CurrentState.RIGHT;
				contentCurrentPosition = -contentFinalPosition;
			}
			sidebarCurrentPosition = sidebarFinalPosition;

			content.scrollTo(-contentCurrentPosition, 0);
			sidebar.scrollTo(-sidebarCurrentPosition, 0);

		}
	}

	class CloseDrawerAnimation implements Runnable {

		View sidebar;
		View content;
		boolean noAnim;
		boolean leftDrawer;
		private int sidebarOffset;

		public CloseDrawerAnimation(View sidebar, View content, boolean noAnim,
				boolean leftDrawer) {
			this.leftDrawer = leftDrawer;

			this.content = content;
			this.sidebar = sidebar;

			this.noAnim = noAnim;

			sidebarOffset = leftDrawer ? mLeftSidebarOffsetForAnimation
					: mRightSidebarOffsetForAnimation;
			sidebarFinalPosition = leftDrawer ? -mLeftSidebarOffsetForAnimation
					: mRightSidebarOffsetForAnimation;
		}

		@Override
		public void run() {

			if (!noAnim) {
				float contentFactor = Math
						.max(0.1f,
								((float) ((contentFinalPosition - Math
										.abs(contentCurrentPosition)) * 100 / contentFinalPosition) / 100));

				float sidebarFactor = Math
						.max(0.1f,
								((float) ((Math.abs(sidebarCurrentPosition)) * 100 / sidebarOffset) / 100));

				int sidebarIncrements = (int) (((int) sidebarOffset / animationSteps) * interpolator
						.getInterpolation(sidebarFactor));
				int contentIncrements = (int) (((int) contentFinalPosition / animationSteps) * interpolator
						.getInterpolation(contentFactor));

				sidebarIncrements = Math.min(sidebarIncrements, sidebarOffset
						- Math.abs(sidebarCurrentPosition));
				contentIncrements = Math.min(contentIncrements,
						contentFinalPosition + Math.abs(contentFinalPosition));

				sidebarIncrements = Math.max(sidebarIncrements, 1);
				contentIncrements = Math.max(contentIncrements, 1);

				if (leftDrawer) {
					sidebarIncrements = -sidebarIncrements;
					contentIncrements = -contentIncrements;
				}

				boolean currentAnimationComplete = (leftDrawer && contentCurrentPosition > 0)
						|| (!leftDrawer && contentCurrentPosition < 0);
				if ((currentAnimationComplete)
						|| (Math.abs(sidebarCurrentPosition) < sidebarOffset)) {
					if (currentAnimationComplete) {
						contentCurrentPosition += contentIncrements;
						content.scrollTo(-contentCurrentPosition, 0);
					}

					if (Math.abs(sidebarCurrentPosition) < sidebarOffset) {
						sidebarCurrentPosition += sidebarIncrements;
						sidebar.scrollTo(-sidebarCurrentPosition, 0);
					}

					handler.postDelayed(this, DURATION_BETWEEN_EACH_STEP);
				} else {
					finishAnim();
				}
			} else {
				finishAnim();
			}
		}

		private void finishAnim() {
			isAnimating = false;
			sidebar.setVisibility(View.INVISIBLE);
			contentCurrentPosition = 0;
			sidebarCurrentPosition = sidebarFinalPosition;
			currentState = CurrentState.NONE;

			content.scrollTo(-contentCurrentPosition, 0);
			sidebar.scrollTo(-sidebarCurrentPosition, 0);
		}
	}
}
