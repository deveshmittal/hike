/*
 * Copyright (C) 2012 0xlab - http://0xlab.org/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authored by Julian Chu <walkingice AT 0xlab.org>
 */

package com.bsb.hike.view;

// update the package name to match your app
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

import com.bsb.hike.R;
import com.bsb.hike.utils.Utils;

public class DrawerLayout extends ViewGroup {

	public final static int DURATION = 250;

	private boolean mOpened;
	private View mSidebar;
	private View mContent;
	private int mSidebarWidth;
	private int mSidebarOffsetForAnimation;

	private Animation contentAnimationIn;
	private Animation sidebarTranslateAnimationIn;
	private Animation contentAnimationOut;
	private Animation sidebarTranslateAnimationOut;

	private OpenListener mOpenListener;
	private CloseListener mCloseListener;
	private Listener mListener;

	private boolean mPressed = false;

	public DrawerLayout(Context context) {
		this(context, null, 0);
	}

	public DrawerLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public DrawerLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mSidebarWidth = (int) (272 * Utils.densityMultiplier);
		mSidebarOffsetForAnimation = (int) (80 * Utils.densityMultiplier);

		/* Close Animations */
		contentAnimationOut = new TranslateAnimation(0, -mSidebarWidth, 0, 0);
		contentAnimationOut.setFillAfter(true);
		contentAnimationOut.setFillEnabled(true);

		sidebarTranslateAnimationOut = new TranslateAnimation(0, -mSidebarOffsetForAnimation, 0, 0);

		/* Open Animations */
		contentAnimationIn = new TranslateAnimation(0, mSidebarWidth, 0, 0);
		contentAnimationIn.setFillAfter(true);
		contentAnimationIn.setFillEnabled(true);

		sidebarTranslateAnimationIn = new TranslateAnimation(-mSidebarOffsetForAnimation, 0, 0, 0);
	}

	@Override
	public void onFinishInflate() {
		super.onFinishInflate();
		mSidebar = findViewById(R.id.animation_layout_sidebar);
		mContent = findViewById(R.id.animation_layout_content);

		if (mSidebar == null) {
			throw new NullPointerException("no view id = animation_sidebar");
		}

		if (mContent == null) {
			throw new NullPointerException("no view id = animation_content");
		}

		mOpenListener = new OpenListener(mSidebar, mContent);
		mCloseListener = new CloseListener(mSidebar, mContent);
	}

	@Override
	public void onLayout(boolean changed, int l, int t, int r, int b) {
		/* the title bar assign top padding, drop it */
		mSidebar.layout(l, 0, l + mSidebarWidth,
				0 + mSidebar.getMeasuredHeight());
		if (mOpened) {
			mContent.layout(l + mSidebarWidth, 0, r + mSidebarWidth, b);
		} else {
			mContent.layout(l, 0, r, b);
		}
	}

	@Override
	public void onMeasure(int w, int h) {
		super.onMeasure(w, h);
		super.measureChildren(w, h);
		mSidebarWidth = mSidebar.getMeasuredWidth();
	}

	@Override
	protected void measureChild(View child, int parentWSpec, int parentHSpec) {
		/* the max width of Sidebar is 90% of Parent */
		if (child == mSidebar) {
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
		if (!isOpening()) {
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
				&& mContent.getTop() < y && mContent.getBottom() > y) {
			if (action == MotionEvent.ACTION_DOWN) {
				mPressed = true;
			}

			if (mPressed && action == MotionEvent.ACTION_UP
					&& mListener != null) {
				mPressed = false;
				return mListener.onContentTouchedWhenOpening();
			}
		} else {
			mPressed = false;
		}

		return false;
	}

	public void setListener(Listener l) {
		mListener = l;
	}

	/* to see if the Sidebar is visible */
	public boolean isOpening() {
		return mOpened;
	}

	public void toggleSidebar(boolean noAnimation) {
		if (mContent.getAnimation() != null) {
			return;
		}

		if (mOpened) {
			contentAnimationOut.setDuration(noAnimation ? 0 : DURATION);
			sidebarTranslateAnimationOut.setDuration(noAnimation ? 0 : DURATION);
			/* opened, make close animation */
			mSidebar.startAnimation(sidebarTranslateAnimationOut);
			mContent.startAnimation(contentAnimationOut);
			contentAnimationOut.setAnimationListener(mCloseListener);
		} else {
			contentAnimationIn.setDuration(noAnimation ? 0 : DURATION);
			sidebarTranslateAnimationIn.setDuration(noAnimation ? 0 : DURATION);
			/* not opened, make open animation */
			mSidebar.startAnimation(sidebarTranslateAnimationIn);
			mContent.startAnimation(contentAnimationIn);
			contentAnimationIn.setAnimationListener(mOpenListener);
		}
	}

	public void openSidebar() {
		if (!mOpened) {
			toggleSidebar(false);
		}
	}

	public void closeSidebar() {
		if (mOpened) {
			toggleSidebar(false);
		}
	}

	class OpenListener implements Animation.AnimationListener {
		View iSidebar;
		View iContent;

		OpenListener(View sidebar, View content) {
			iSidebar = sidebar;
			iContent = content;
		}

		public void onAnimationRepeat(Animation animation) {
		}

		public void onAnimationStart(Animation animation) {
			iSidebar.setVisibility(View.VISIBLE);
		}

		public void onAnimationEnd(Animation animation) {
			iContent.clearAnimation();
			iSidebar.clearAnimation();
			mOpened = !mOpened;
			requestLayout();
			if (mListener != null) {
				mListener.onSidebarOpened();
			}
		}
	}

	class CloseListener implements Animation.AnimationListener {
		View iSidebar;
		View iContent;

		CloseListener(View sidebar, View content) {
			iSidebar = sidebar;
			iContent = content;
		}

		public void onAnimationRepeat(Animation animation) {
		}

		public void onAnimationStart(Animation animation) {
		}

		public void onAnimationEnd(Animation animation) {
			iContent.clearAnimation();
			iSidebar.clearAnimation();
			iSidebar.setVisibility(View.INVISIBLE);
			mOpened = !mOpened;
			requestLayout();
			if (mListener != null) {
				mListener.onSidebarClosed();
			}
		}
	}

	public interface Listener {
		public void onSidebarOpened();

		public void onSidebarClosed();

		public boolean onContentTouchedWhenOpening();
	}
}
