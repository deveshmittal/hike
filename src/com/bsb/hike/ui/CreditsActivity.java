package com.bsb.hike.ui;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.utils.DrawerBaseActivity;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.GaugeView;

public class CreditsActivity extends DrawerBaseActivity implements Listener
{
	private TextView mTitleView;
	private TextView creditsNum;
	private ImageButton inviteFriendsBtn;
	private SharedPreferences settings;
	private GaugeView creditsGaugeView;
	private TextView validityTxt;

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
		creditsNum = (TextView) findViewById(R.id.sms_num);
		inviteFriendsBtn = (ImageButton) findViewById(R.id.invite_now);
		validityTxt = (TextView) findViewById(R.id.validity);
		validityTxt.setVisibility(View.INVISIBLE);

		mTitleView.setText("Free SMS");

		updateCredits();
		updateTotalCredits();

		inviteFriendsBtn.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				Utils.logEvent(CreditsActivity.this, HikeConstants.LogEvent.INVITE_BUTTON_CLICKED);
				Utils.startShareIntent(CreditsActivity.this, Utils.getInviteMessage(CreditsActivity.this));
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
		creditsNum.setText(credits + "");
		creditsGaugeView.setActualCreditsAngle(credits);
		creditsGaugeView.invalidate();
	}

	private void updateTotalCredits()
	{
		creditsGaugeView.setMaxCreditsAngle(Integer.parseInt(settings.getString(HikeMessengerApp.TOTAL_CREDITS_PER_MONTH, "0")));
		creditsGaugeView.invalidate();
	}
}