package com.bsb.hike.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.HikeInviteAdapter;

public class CreditsActivity extends Activity 
{
	private static final int INVITE_PICKER_RESULT = 1001;

	private LinearLayout creditItemContainer;
	private TextView mTitleView;
	private TextView creditsNum;
	private Button inviteFriendsBtn;
	private SharedPreferences settings;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.credits);

		mTitleView = (TextView) findViewById(R.id.title);
		creditItemContainer = (LinearLayout) findViewById(R.id.credit_item_container);
		creditsNum = (TextView) findViewById(R.id.credit_no);
		inviteFriendsBtn = (Button) findViewById(R.id.invite_now);
		
		settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		int credits = settings.getInt(HikeMessengerApp.SMS_SETTING, 0);

		creditsNum.setText(credits+"");
		mTitleView.setText("Free SMS");
		LayoutInflater layoutInflater = LayoutInflater.from(CreditsActivity.this);

		for(int i = 0; i<=5; i++)
		{
			View v = layoutInflater.inflate(R.layout.credits_item, null);
			TextView friendNum = (TextView) v.findViewById(R.id.friends_no);
			TextView smsNum = (TextView) v.findViewById(R.id.sms_no);
			
			int smsNo = 100 + (i*20);
			
			friendNum.setText(i+"");
			smsNum.setText(smsNo+"");
			creditItemContainer.addView(v);
		}
		inviteFriendsBtn.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				invite();
			}
		});
	}
	protected void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		if (resultCode == RESULT_OK)
		{
			switch (requestCode)
			{
			case INVITE_PICKER_RESULT:
				intent.setClass(this, ChatThread.class);
				startActivity(intent);
			}
		}
	}

	private void invite()
	{
		Intent intent = new Intent(this, HikeListActivity.class);
		intent.putExtra(HikeConstants.ADAPTER_NAME, HikeInviteAdapter.class.getName());
		startActivityForResult(intent, INVITE_PICKER_RESULT);		
	}
}
