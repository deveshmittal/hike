package com.bsb.hike.chatthread;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.modules.animationModule.HikeAnimationFactory;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.HikeTip.TipType;
import com.bsb.hike.utils.Logger;

/**
 * This class is a helper class which contains exhaustive set of tips which can be shown in the chat thread. The tips include Atomic tips which are server triggered as well FTUE
 * tips. Every individual chat thread has the knowledge of its own set of tips, which it passes to this helper class.
 * 
 * It can call utility methods like {@link #showTip()}, {@link #closeTip(int)} etc to hide/show tips.
 * 
 * @author piyush
 * 
 */
public class ChatThreadTips implements OnClickListener, OnTouchListener
{
	/**
	 * Atomic Tips - Tips which are server triggered
	 */
	public static final int ATOMIC_ATTACHMENT_TIP = 1;

	public static final int ATOMIC_STICKER_TIP = 2;

	public static final int ATOMIC_CHAT_THEME_TIP = 3;

	/**
	 * FTUE Tips - Tips which introduce a new feature/functionality
	 */
	public static final int PIN_TIP = 4;

	public static final int STICKER_TIP = 5;

	/**
	 * Class members
	 */
	private Context mContext;

	private int tipId = -1;

	int[] mWhichTips;

	View mainView;

	View tipView;

	HikeSharedPreferenceUtil mPrefs;

	public ChatThreadTips(Context context, View view, int[] whichTipsToShow, HikeSharedPreferenceUtil prefs)
	{
		this.mContext = context;
		this.mainView = view;
		this.mWhichTips = whichTipsToShow;
		this.mPrefs = prefs;
	}

	public void showTip()
	{
		showFtueTips();
		// Is any tip open ?
		if (!isAnyTipOpen())
		{
			int newTipId = whichAtomicTipToShow();

			if (isPresentInArray(newTipId)) // Did the chat Thread pass the tip to be shown?
			{
				tipId = newTipId; // Resetting tipId

				switch (tipId)
				{
				case ATOMIC_ATTACHMENT_TIP:

					tipView = LayoutInflater.from(mContext).inflate(R.layout.tip_right_arrow, null);
					((ImageView) (tipView.findViewById(R.id.arrow_pointer))).setImageResource(R.drawable.ftue_up_arrow);
					setAtomicTipContent(tipView);
					((LinearLayout) mainView.findViewById(R.id.tipContainerTop)).addView(tipView, 0);
					break;

				case ATOMIC_CHAT_THEME_TIP:
					tipView = LayoutInflater.from(mContext).inflate(R.layout.tip_middle_arrow, null);
					((ImageView) (tipView.findViewById(R.id.arrow_pointer))).setImageResource(R.drawable.ftue_up_arrow);
					setAtomicTipContent(tipView);
					((LinearLayout) mainView.findViewById(R.id.tipContainerTop)).addView(tipView, 0);
					break;

				case ATOMIC_STICKER_TIP:
					tipView = LayoutInflater.from(mContext).inflate(R.layout.tip_left_arrow, null);
					((ImageView) (tipView.findViewById(R.id.arrow_pointer))).setImageResource(R.drawable.ftue_down_arrow);
					setAtomicTipContent(tipView);
					((LinearLayout) mainView.findViewById(R.id.tipContainerBottom)).addView(tipView, 0);
					break;
				}
			}
		}
	}

	private void setAtomicTipContent(View view)
	{
		((TextView) view.findViewById(R.id.tip_header)).setText(mPrefs.getData(HikeMessengerApp.ATOMIC_POP_UP_HEADER_CHAT, ""));
		((TextView) view.findViewById(R.id.tip_msg)).setText(mPrefs.getData(HikeMessengerApp.ATOMIC_POP_UP_MESSAGE_CHAT, ""));

		view.findViewById(R.id.close_tip).setOnClickListener(this);
	}

	private int whichAtomicTipToShow()
	{
		String key = mPrefs.getData(HikeMessengerApp.ATOMIC_POP_UP_TYPE_CHAT, "");
		switch (key)
		{
		case HikeMessengerApp.ATOMIC_POP_UP_ATTACHMENT:
			return ATOMIC_ATTACHMENT_TIP;

		case HikeMessengerApp.ATOMIC_POP_UP_STICKER:
			return ATOMIC_STICKER_TIP;

		case HikeMessengerApp.ATOMIC_POP_UP_THEME:
			return ATOMIC_CHAT_THEME_TIP;

		default:
			return -1;
		}
	}

	private void showFtueTips()
	{
		showStickerFtueTip();
		showPinFtueTip();
	}

	/**
	 * Utility method to show the pulsating dot animation on Stickers Icon
	 */
	public void showStickerFtueTip()
	{
		/**
		 * Proceed only if the calling class had passed in the StickerTip in the list
		 */
		if (filterTips(STICKER_TIP))
		{
			tipId = STICKER_TIP;
			setupStickerFTUETip();
		}
	}

	/**
	 * Utility method to show the Pin FTUE tip. If any other tip is showing, pin tip takes priority over it.
	 */
	private void showPinFtueTip()
	{
		/**
		 * Proceed only if the calling class had passed in the Pin Tip in the list
		 */
		if (filterTips(PIN_TIP))
		{
			tipId = PIN_TIP;
			tipView = mainView.findViewById(R.id.pin_tip);
			tipView.setVisibility(View.VISIBLE);
			tipView.setTag(TipType.PIN);
			tipView.findViewById(R.id.close_tip).setOnClickListener(this);
			tipView.setOnTouchListener(this);
		}
	}
	
	/**
	 * Used to set up pulsating dot views
	 */
	private void setupStickerFTUETip()
	{
		ViewStub pulsatingDot = (ViewStub) mainView.findViewById(R.id.pulsatingDotViewStub);
		
		if(pulsatingDot != null)
		{
			pulsatingDot.setOnInflateListener(new ViewStub.OnInflateListener()
			{

				@Override
				public void onInflate(ViewStub stub, View inflated)
				{
					tipView = inflated;
					startPulsatingDotAnimation(tipView);
				}
			});
			
			pulsatingDot.inflate();
		}
	}
	
	/**
	 * Used to start pulsating dot animation for stickers
	 * 
	 * @param view
	 */
	private void startPulsatingDotAnimation(View view)
	{
		new Handler().postDelayed(getPulsatingRunnable(view, R.id.ring1), 0);
		new Handler().postDelayed(getPulsatingRunnable(view, R.id.ring2), 1500);
	}

	private Runnable getPulsatingRunnable(final View view, final int viewId)
	{
		return new Runnable()
		{
			@Override
			public void run()
			{
				ImageView ringView = (ImageView) view.findViewById(viewId);
				ringView.startAnimation(HikeAnimationFactory.getPulsatingDotAnimation(0));
			}
		};
	}	
	
	private boolean filterTips(int whichTip)
	{
		return isPresentInArray(whichTip) && !(seenTip(whichTip));
	}

	/**
	 * Have we seen a particular kind of tip before ?
	 * 
	 * @param whichTip
	 * @return
	 */
	private boolean seenTip(int whichTip)
	{
		switch (whichTip)
		{
		case PIN_TIP:
			return mPrefs.getData(HikeMessengerApp.SHOWN_PIN_TIP, false);
		case STICKER_TIP:
			return mPrefs.getData(HikeMessengerApp.SHOWN_EMOTICON_TIP, false);
		default:
			return false;
		}
	}

	private boolean isPresentInArray(int whichTip)
	{
		for (int i : mWhichTips)
		{
			if (i == whichTip)
			{
				return true;
			}
			continue;
		}
		return false;
	}

	/**
	 * Closes any other open tips
	 */
	private void closeTip()
	{
		if (tipView != null)
		{
			tipId = -1;
			tipView.setVisibility(View.GONE);
			tipView = null;
		}
	}

	/**
	 * Used to temporarily hide any open tips
	 */
	public void hideTip()
	{
		if (tipView != null && tipView.getVisibility() == View.VISIBLE && shouldHideTip())
		{
			tipView.setVisibility(View.INVISIBLE);
		}
	}

	public void hideTip(int whichTip)
	{
		if (tipId == whichTip && tipView != null && tipView.getVisibility() == View.VISIBLE && shouldHideTip())
		{
			tipView.setVisibility(View.INVISIBLE);
		}
	}
	
	/**
	 * There could be certain tips which do not interfere with any UI components. Hence if such a tip is showing we should not hide it.
	 * eg : Sticker_tip. This method is future safe, if let's say we need to show pulsating dots on VoIP buttons or Pin buttons.
	 * @return
	 */
	private boolean shouldHideTip()
	{
		return tipId != STICKER_TIP;
	}

	public void showHiddenTip()
	{
		if (tipView != null && tipView.getVisibility() == View.INVISIBLE)
		{
			tipView.setVisibility(View.VISIBLE);
		}
	}

	public void showHiddenTip(int whichTip)
	{
		if (tipId == whichTip && tipView != null && tipView.getVisibility() == View.INVISIBLE)
		{
			tipView.setVisibility(View.VISIBLE);
		}
	}

	public boolean isAnyTipOpen()
	{
		return tipId != -1;
	}
	
	public boolean isGivenTipShowing(int whichTip)
	{
		return (tipView != null && tipId == whichTip);
	}

	@Override
	public void onClick(View v)
	{
		switch (tipId)
		{
		case ATOMIC_ATTACHMENT_TIP:
		case ATOMIC_CHAT_THEME_TIP:
		case ATOMIC_STICKER_TIP:
			setTipSeen(tipId);
			break;

		case PIN_TIP:
			setTipSeen(tipId);
			break;

		default:
			break;
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		/**
		 * This is being done to eat the double tap to nudge events when one presses the tips
		 */
		return true;
	}

	/**
	 * Function to mark the tip as seen
	 * @param whichTip
	 */
	public void setTipSeen(int whichTip)
	{
		/**
		 * Proceeding only if we are showing the current tip indicated by whichTip
		 */
		if (tipId == whichTip)
		{
			switch (whichTip)
			{
			case PIN_TIP:
				mPrefs.saveData(HikeMessengerApp.SHOWN_PIN_TIP, true);
				closeTip();
				break;
				
			case STICKER_TIP:
				mPrefs.saveData(HikeMessengerApp.SHOWN_EMOTICON_TIP, true);
				closeTip();
				break;

			case ATOMIC_ATTACHMENT_TIP:
			case ATOMIC_CHAT_THEME_TIP:
			case ATOMIC_STICKER_TIP:
				mPrefs.saveData(HikeMessengerApp.ATOMIC_POP_UP_TYPE_CHAT, "");
				/**
				 * Recording click on sticker tip
				 */
				if (whichTip == ATOMIC_STICKER_TIP)   
				{
					ChatThreadUtils.recordStickerFTUEClick();
				}
				closeTip();
				break;

			}
		}
	}
}