package com.bsb.hike.models.Conversation;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.CountDownTimer;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class ConversationTip implements OnClickListener
{
	public static final int NO_TIP = -1;

	public static final int GROUP_CHAT_TIP = 1;

	public static final int STEALTH_FTUE_TIP = 2;

	public static final int RESET_STEALTH_TIP = 3;

	public static final int WELCOME_HIKE_TIP = 4;

	public static final int STEALTH_INFO_TIP = 5;

	public static final int STEALTH_UNREAD_TIP = 6;

	public static final int ATOMIC_PROFILE_PIC_TIP = 7;

	public static final int ATOMIC_FAVOURTITES_TIP = 8;

	public static final int ATOMIC_INVITE_TIP = 9;

	public static final int ATOMIC_STATUS_TIP = 10;

	public static final int ATOMIC_INFO_TIP = 11;

	public static final int ATOMIC_HTTP_TIP = 12;

	public static final int ATOMIC_APP_GENERIC_TIP = 13;

	private int tipType;

	private LayoutInflater inflater;

	private Context context;

	CountDownSetter countDownSetter;

	public interface ConversationTipClickedListener
	{
		public void closeTip(int whichTip);
	}

	private ConversationTipClickedListener mListener;

	public ConversationTip(Context context, ConversationTipClickedListener listener)
	{
		this.context = context;
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.mListener = listener;
	}

	public View getView(int whichTip)
	{
		if (whichTip == NO_TIP)
		{
			return null;
		}
		tipType = whichTip;

		View v;
		switch (tipType)
		{
		case STEALTH_FTUE_TIP:
			v = inflater.inflate(R.layout.stealth_ftue_conversation_tip, null, false);
			v.findViewById(R.id.close).setOnClickListener(this);
			// TODO Add animation Code here
			return v;

		case RESET_STEALTH_TIP:
			v = inflater.inflate(R.layout.stealth_ftue_conversation_tip, null, false);
			v.findViewById(R.id.close).setOnClickListener(this);
			TextView headerText = (TextView) v.findViewById(R.id.tip);
			long remainingTime = HikeConstants.RESET_COMPLETE_STEALTH_TIME_MS
					- (System.currentTimeMillis() - HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME, 0l));

			if (remainingTime <= 0)
			{
				headerText.setText(Html.fromHtml(context.getResources().getString(R.string.tap_to_reset_stealth_tip)));
			}
			else
			{
				if (countDownSetter == null)
				{
					countDownSetter = new CountDownSetter(headerText, remainingTime, 1000);
					countDownSetter.start();

					setTimeRemainingText(headerText, remainingTime);
				}
				else
				{
					countDownSetter.setTextView(headerText);
				}

			}
			return v;

		case WELCOME_HIKE_TIP:
			v = inflater.inflate(R.layout.welcome_hike_tip, null, false);
			((TextView) v.findViewById(R.id.tip_header)).setText(R.string.new_ui_welcome_tip_header);
			((TextView) v.findViewById(R.id.tip_msg)).setText(R.string.new_ui_welcome_tip_msg);
			v.findViewById(R.id.close_tip).setOnClickListener(this);
			return v;

		case STEALTH_INFO_TIP:
			v = inflater.inflate(R.layout.stealth_unread_tip, null, false);
			((TextView) v.findViewById(R.id.tip_header)).setText(R.string.stealth_info_tip_header);
			((TextView) v.findViewById(R.id.tip_msg)).setText(R.string.stealth_info_tip_subtext);
			v.findViewById(R.id.close_tip).setOnClickListener(this);
			return v;

		case STEALTH_UNREAD_TIP:
			v = inflater.inflate(R.layout.stealth_unread_tip, null, false);
			String headerTxt = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_UNREAD_TIP_HEADER, "");
			String msgTxt = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_UNREAD_TIP_MESSAGE, "");
			((TextView) v.findViewById(R.id.tip_header)).setText(headerTxt);
			((TextView) v.findViewById(R.id.tip_msg)).setText(msgTxt);
			v.findViewById(R.id.close_tip).setOnClickListener(this);
			return v;

		case ATOMIC_PROFILE_PIC_TIP:
			v = generateAtomicTipViews();
			((ImageView) v.findViewById(R.id.arrow_pointer)).setImageResource(R.drawable.ic_profile);
			return v;
		case ATOMIC_FAVOURTITES_TIP:
			v = generateAtomicTipViews();
			((ImageView) v.findViewById(R.id.arrow_pointer)).setImageResource(R.drawable.ic_favorites);
			return v;
		case ATOMIC_INVITE_TIP:
			v = generateAtomicTipViews();
			((ImageView) v.findViewById(R.id.arrow_pointer)).setImageResource(R.drawable.ic_rewards);
			return v;
		case ATOMIC_STATUS_TIP:
			v = generateAtomicTipViews();
			((ImageView) v.findViewById(R.id.arrow_pointer)).setImageResource(R.drawable.ic_status_tip);
			return v;
		case ATOMIC_INFO_TIP:
			v = generateAtomicTipViews();
			((ImageView) v.findViewById(R.id.arrow_pointer)).setImageResource(R.drawable.ic_information);
			return v;
		case ATOMIC_HTTP_TIP:
			v = generateAtomicTipViews();
			((ImageView) v.findViewById(R.id.arrow_pointer)).setImageResource(R.drawable.ic_profile);
			return v;
		case ATOMIC_APP_GENERIC_TIP:
			v = generateAtomicTipViews();
			((ImageView) v.findViewById(R.id.arrow_pointer)).setImageDrawable(null);
			return v;

		default:
			tipType = NO_TIP;
			return null;
		}
	}

	private View generateAtomicTipViews()
	{
		View v = inflater.inflate(R.layout.tip_left_arrow, null, false);
		TextView header = (TextView) v.findViewById(R.id.tip_header);
		TextView subText = (TextView) v.findViewById(R.id.tip_msg);
		HikeSharedPreferenceUtil pref = HikeSharedPreferenceUtil.getInstance();
		String headerTxt1 = pref.getData(HikeMessengerApp.ATOMIC_POP_UP_HEADER_MAIN, "");
		String message = pref.getData(HikeMessengerApp.ATOMIC_POP_UP_MESSAGE_MAIN, "");
		header.setText(headerTxt1);
		subText.setText(message);
		v.findViewById(R.id.close_tip).setOnClickListener(this);
		return v;

	}

	public int getTipType()
	{
		return tipType;
	}

	public void setTipType(int tipType)
	{
		this.tipType = tipType;
	}

	public boolean isGroupChatTip()
	{
		return tipType == GROUP_CHAT_TIP;
	}

	public boolean isStealthFtueTip()
	{
		return tipType == STEALTH_FTUE_TIP;
	}

	public boolean isResetStealthTip()
	{
		return tipType == RESET_STEALTH_TIP;
	}

	public boolean isWelcomeHikeTip()
	{
		return tipType == WELCOME_HIKE_TIP;
	}

	public boolean isStealthInfoTip()
	{
		return tipType == STEALTH_INFO_TIP;
	}

	public boolean isStealthUnreadTip()
	{
		return tipType == STEALTH_UNREAD_TIP;
	}

	private class CountDownSetter extends CountDownTimer
	{
		TextView textView;

		public CountDownSetter(TextView textView, long millisInFuture, long countDownInterval)
		{
			super(millisInFuture, countDownInterval);
			this.textView = textView;
		}

		@Override
		public void onFinish()
		{
			if (textView == null)
			{
				return;
			}
			textView.setText(Html.fromHtml(context.getResources().getString(R.string.tap_to_reset_stealth_tip)));
		}

		@Override
		public void onTick(long millisUntilFinished)
		{
			if (textView == null)
			{
				return;
			}

			setTimeRemainingText(textView, millisUntilFinished);
		}

		public void setTextView(TextView tv)
		{
			this.textView = tv;
		}
	}

	private void setTimeRemainingText(TextView textView, long millisUntilFinished)
	{
		long secondsUntilFinished = millisUntilFinished / 1000;
		int minutes = (int) (secondsUntilFinished / 60);
		int seconds = (int) (secondsUntilFinished % 60);
		String text = String.format("%1$02d:%2$02d", minutes, seconds);
		textView.setText(Html.fromHtml(context.getString(R.string.reset_stealth_tip, text)));
	}

	private void resetCountDownSetter()
	{
		if (countDownSetter == null)
		{
			return;
		}

		this.countDownSetter.cancel();
		this.countDownSetter = null;
	}

	@Override
	public void onClick(View v)
	{
		if (v.getId() == R.id.close_tip || v.getId() == R.id.close)
		{

			switch (tipType)
			{
			case RESET_STEALTH_TIP:
				resetCountDownSetter();
				Utils.cancelScheduledStealthReset();

				try
				{
					JSONObject metadata = new JSONObject();
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.RESET_STEALTH_CANCEL);
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				}
				catch (JSONException e)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}

				break;

			case WELCOME_HIKE_TIP:
				HikeMessengerApp.getPubSub().publish(HikePubSub.REMOVE_WELCOME_HIKE_TIP, null);
				break;

			case STEALTH_INFO_TIP:
				HikeMessengerApp.getPubSub().publish(HikePubSub.REMOVE_STEALTH_INFO_TIP, null);
				break;

			case STEALTH_UNREAD_TIP:
				HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_UNREAD_TIP_CLICKED, null);
				break;

			case ATOMIC_PROFILE_PIC_TIP:
			case ATOMIC_FAVOURTITES_TIP:
			case ATOMIC_INVITE_TIP:
			case ATOMIC_STATUS_TIP:
			case ATOMIC_INFO_TIP:
			case ATOMIC_HTTP_TIP:
			case ATOMIC_APP_GENERIC_TIP:
				HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.ATOMIC_POP_UP_TYPE_MAIN, "");
				break;
			default:
				break;
			}

			if (mListener != null)
			{
				mListener.closeTip(tipType);
			}

			tipType = NO_TIP;
		}
	}
}
