package com.bsb.hike.ui;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.utils.Utils;

public class CreditsActivity extends Activity implements Listener
{
	private TextView mTitleView;
	private TextView creditsNum;
	private Button inviteFriendsBtn;
	private TextView friendsNumTxt;
	private SharedPreferences settings;
	private TextView everyMonthTxt;
	private TextView joinedNumTxt;
	private boolean firstTimeUser = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.credits);

		settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		Editor editor = settings.edit();
		editor.putBoolean(HikeMessengerApp.INVITE_TOOLTIP_DISMISSED, true);
		editor.commit();

		mTitleView = (TextView) findViewById(R.id.title);
		creditsNum = (TextView) findViewById(R.id.credit_no);
		inviteFriendsBtn = (Button) findViewById(R.id.invite_now);
		friendsNumTxt = (TextView) findViewById(R.id.friends_num);
		everyMonthTxt = (TextView) findViewById(R.id.every_month_text);
		joinedNumTxt = (TextView) findViewById(R.id.joined_num);

		String everyMonth = getString(R.string.every_month);
		SpannableString everyMonthSpan = new SpannableString(everyMonth);
		String stringToBeFormatted = getString(R.string.string_to_be_formatted);
		String stringToBeFormatted2 = getString(R.string.string_to_be_formatted2);
		everyMonthSpan.setSpan(
								new StyleSpan(Typeface.BOLD), 
								everyMonth.indexOf(stringToBeFormatted), 
								everyMonth.indexOf(stringToBeFormatted) + stringToBeFormatted.length() + 1,
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
								);
		everyMonthSpan.setSpan(
								new ForegroundColorSpan(getResources().getColor(R.color.lightblack)), 
								everyMonth.indexOf(stringToBeFormatted), 
								everyMonth.indexOf(stringToBeFormatted) + stringToBeFormatted.length() + 1,
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
								);
		everyMonthSpan.setSpan(
				new StyleSpan(Typeface.BOLD), 
				everyMonth.indexOf(stringToBeFormatted2), 
				everyMonth.indexOf(stringToBeFormatted2) + stringToBeFormatted2.length() + 1,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
				);
		everyMonthSpan.setSpan(
				new ForegroundColorSpan(getResources().getColor(R.color.lightblack)), 
				everyMonth.indexOf(stringToBeFormatted2), 
				everyMonth.indexOf(stringToBeFormatted2) + stringToBeFormatted2.length() + 1,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
				);
		everyMonthTxt.setText(everyMonthSpan);

		mTitleView.setText("Free SMS");

		updateCredits();

		updateInviteeNum();

		inviteFriendsBtn.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				Utils.logEvent(CreditsActivity.this, HikeConstants.LogEvent.INVITE_BUTTON_CLICKED);
				Utils.startInviteShareIntent(CreditsActivity.this);
			}
		});

		HikeMessengerApp.getPubSub().addListener(HikePubSub.SMS_CREDIT_CHANGED, this);
	}

	@Override
	protected void onDestroy() 
	{
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.SMS_CREDIT_CHANGED, this);
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.INVITEE_NUM_CHANGED, this);
		super.onDestroy();
	}

	public void onTitleIconClick(View v)
	{
		onBackPressed();
	}

	@Override
	public void onBackPressed() {
		if(firstTimeUser)
		{
			Intent intent = new Intent(CreditsActivity.this, MessagesList.class);
			intent.putExtra(HikeConstants.Extras.APP_STARTED_FIRST_TIME, true);
			startActivity(intent);
			finish();
		}
		super.onBackPressed();
	}

	@Override
	public void onEventReceived(String type, Object object) 
	{
		if(HikePubSub.SMS_CREDIT_CHANGED.equals(type))
		{
			runOnUiThread(new Runnable() 
			{
				@Override
				public void run() 
				{
					updateCredits();
				}
			});
		}
		else if(HikePubSub.INVITEE_NUM_CHANGED.equals(type))
		{
			runOnUiThread(new Runnable() 
			{
				@Override
				public void run() 
				{
					updateInviteeNum();
				}
			});
		}
	}

	private void updateCredits()
	{
		creditsNum.setText(settings.getInt(HikeMessengerApp.SMS_SETTING, 0) + "");
	}

	private void updateInviteeNum()
	{
		int numInvited = settings.getInt(HikeMessengerApp.INVITED, 0);
		int numHike = settings.getInt(HikeMessengerApp.INVITED_JOINED, 0);

		String formatString = getResources().getString(R.string.friends_on_hike_0);
		String num = Integer.toString(numInvited);
		String formatted = String.format(formatString, num);

		SpannableString str = new SpannableString(formatted);
		int start = formatString.indexOf("%1$s");
		str.setSpan(new StyleSpan(Typeface.BOLD), start,
				start + num.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		str.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.lightblack)), start, start + num.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		friendsNumTxt.setText(str);

		String joinedNum = ((numHike != 1) ? (numHike + " friends have "):(numHike + " friend has ")) + "joined hike because of you.";
		int idx = joinedNum.indexOf("friend");
		SpannableString ss = new SpannableString(joinedNum);
		ss.setSpan(new StyleSpan(Typeface.BOLD), 0, idx, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		ss.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.lightblack)), 0, idx, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		joinedNumTxt.setText(ss);
	}
}
