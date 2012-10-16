package com.bsb.hike.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.utils.DrawerBaseActivity;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.GaugeView;
import com.bsb.hike.view.RotatableImageView;

public class CreditsActivity extends DrawerBaseActivity implements Listener
{
	private TextView mTitleView;
	private ViewGroup creditsContainer;
	private ImageButton inviteFriendsBtn;
	private SharedPreferences settings;
	private GaugeView creditsGaugeView;
	private RotatableImageView creditsPointer;
	private TextView freeSms;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.credits);
		afterSetContentView(savedInstanceState);

		settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		Editor editor = settings.edit();
		editor.putBoolean(HikeMessengerApp.INVITE_TOOLTIP_DISMISSED, true);
		editor.commit();

		mTitleView = (TextView) findViewById(R.id.title_centered);
		creditsGaugeView = (GaugeView) findViewById(R.id.gauge);
		creditsContainer = (ViewGroup) findViewById(R.id.credits_container);
		inviteFriendsBtn = (ImageButton) findViewById(R.id.invite_now);
		creditsPointer = (RotatableImageView) findViewById(R.id.pointer);
		freeSms = (TextView) findViewById(R.id.free_sms);

		String freeSmsString = getString(R.string.invite_friend_free_sms);
		String textToColor = "50 free SMS";
		int startIndex = freeSmsString.indexOf(textToColor);

		SpannableStringBuilder ssb = new SpannableStringBuilder(freeSmsString);
		ssb.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.lightblack)), startIndex, startIndex + textToColor.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		freeSms.setText(ssb);

		mTitleView.setText("Free SMS");

		updateCredits();
		updateTotalCredits();

		inviteFriendsBtn.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				Utils.logEvent(CreditsActivity.this, HikeConstants.LogEvent.INVITE_BUTTON_CLICKED);
				startActivity(new Intent(CreditsActivity.this, HikeListActivity.class));
			}
		});

		HikeMessengerApp.getPubSub().addListener(HikePubSub.SMS_CREDIT_CHANGED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.INVITEE_NUM_CHANGED, this);
	}

	@Override
	protected void onDestroy() 
	{
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.SMS_CREDIT_CHANGED, this);
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.INVITEE_NUM_CHANGED, this);
		super.onDestroy();
	}

	@Override
	public void onEventReceived(String type, Object object) 
	{
		super.onEventReceived(type, object);
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
					updateTotalCredits();
				}
			});
		}
	}

	private void updateCredits()
	{
		int credits = settings.getInt(HikeMessengerApp.SMS_SETTING, 0);
		creditsPointer.setCredits(credits);

		creditsContainer.removeAllViews();
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

		String creditsString = String.valueOf(credits);

		for(int i = 0; i<creditsString.length(); i++)
		{
			TextView creditNum = (TextView) inflater.inflate(R.layout.credit_number, null);
			creditNum.setText(creditsString.charAt(i) + "");
			LayoutParams lp = new LayoutParams(getResources().getDimensionPixelSize(R.dimen.credits_num_width), getResources().getDimensionPixelSize(R.dimen.credits_num_height));
			if(i == 0)
			{
				lp.leftMargin = (int) (5 * Utils.densityMultiplier);
			}
			lp.rightMargin = (int) (5 * Utils.densityMultiplier);
			creditNum.setLayoutParams(lp);
			creditsContainer.addView(creditNum);
		}
	}

	private void updateTotalCredits()
	{
		creditsGaugeView.setMaxCreditsAngle(Integer.parseInt(settings.getString(HikeMessengerApp.TOTAL_CREDITS_PER_MONTH, "0")));
	}
}