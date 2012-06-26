package com.bsb.hike.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.adapters.HikeInviteAdapter;

public class CreditsActivity extends Activity implements Listener
{
	private LinearLayout creditItemContainer;
	private TextView mTitleView;
	private TextView creditsNum;
	private Button inviteFriendsBtn;
	private int numHike;
	private int numInvited;
	private TextView impTxt;
	private TextView friendsNumTxt;
	private SharedPreferences settings;
	private TextView everyMonthTxt;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.credits);

		settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		Editor editor = settings.edit();
		editor.putBoolean(HikeMessengerApp.INVITE_TOOLTIP_DISMISSED, true);
		editor.commit();

		numHike = settings.getInt(HikeMessengerApp.INVITED_JOINED, 0);
		numInvited = settings.getInt(HikeMessengerApp.INVITED, 0);

		mTitleView = (TextView) findViewById(R.id.title);
		creditItemContainer = (LinearLayout) findViewById(R.id.credit_item_container);
		creditsNum = (TextView) findViewById(R.id.credit_no);
		inviteFriendsBtn = (Button) findViewById(R.id.invite_now);
		impTxt = (TextView) findViewById(R.id.imp_txt);
		friendsNumTxt = (TextView) findViewById(R.id.friends_num);
		everyMonthTxt = (TextView) findViewById(R.id.every_month_text);

		String everyMonth = getString(R.string.every_month);
		SpannableString everyMonthSpan = new SpannableString(everyMonth);
		String stringToBeFormatted = getString(R.string.string_to_be_formatted);
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
		everyMonthTxt.setText(everyMonthSpan);

		String imp = getString(R.string.important);
		String dnd = getString(R.string.dnd);
		SpannableString s = new SpannableString(imp + " " + dnd);

		s.setSpan(new StyleSpan(Typeface.BOLD), 0, imp.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		s.setSpan(new ForegroundColorSpan(0xffff3333), 0, imp.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		impTxt.setText(s);

		String formatString = getResources().getString(R.string.friends_on_hike_0);
		String num = Integer.toString(numInvited);
		String formatted = String.format(formatString, num);
		
		SpannableString str = new SpannableString(formatted);
		int start = formatString.indexOf("%1$s");
		str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start,
				start + num.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		str.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.lightblack)), start, start + num.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		friendsNumTxt.setText(str);

		updateCredits();

		mTitleView.setText("Free SMS");
		LayoutInflater layoutInflater = LayoutInflater.from(CreditsActivity.this);

		for(int i = 0; i<=2; i++)
		{
			View v = layoutInflater.inflate(R.layout.credits_item, null);
			TextView friendNum = (TextView) v.findViewById(R.id.friends_no);
			TextView smsNum = (TextView) v.findViewById(R.id.sms_no);

			int numToShow = numHike < 2 ? i : ((numHike + i) - 1);

			if(i == ((numHike < 2) ? numHike : 1))
			{
				v.setBackgroundResource(R.drawable.credit_item_bckg_selected);
			}

			int smsNo = HikeConstants.INITIAL_NUM_SMS + (numToShow * HikeConstants.NUM_SMS_PER_FRIEND);
			friendNum.setText(numToShow + "");
			smsNum.setText(smsNo+"");
			creditItemContainer.addView(v);
		}
		inviteFriendsBtn.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				Intent i = new Intent(CreditsActivity.this, HikeListActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				i.putExtra(HikeConstants.ADAPTER_NAME, HikeInviteAdapter.class.getName());
				startActivity(i);
			}
		});

		HikeMessengerApp.getPubSub().addListener(HikePubSub.SMS_CREDIT_CHANGED, this);
	}

	@Override
	protected void onDestroy() 
	{
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.SMS_CREDIT_CHANGED, this);
		super.onDestroy();
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
	}

	private void updateCredits()
	{
		creditsNum.setText(settings.getInt(HikeMessengerApp.SMS_SETTING, 0) + "");
	}
}
