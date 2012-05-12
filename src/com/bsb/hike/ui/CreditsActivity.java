package com.bsb.hike.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.bsb.hike.R;
import com.bsb.hike.adapters.HikeInviteAdapter;

public class CreditsActivity extends Activity 
{
	private LinearLayout creditItemContainer;
	private TextView mTitleView;
	private TextView creditsNum;
	private Button inviteFriendsBtn;
	private SharedPreferences settings;
	private int numHike;
	private TextView impTxt;
	private TextView friendsNumTxt;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.credits);
		
		numHike = 0;
		
		mTitleView = (TextView) findViewById(R.id.title);
		creditItemContainer = (LinearLayout) findViewById(R.id.credit_item_container);
		creditsNum = (TextView) findViewById(R.id.credit_no);
		inviteFriendsBtn = (Button) findViewById(R.id.invite_now);
		impTxt = (TextView) findViewById(R.id.imp_txt);
		friendsNumTxt = (TextView) findViewById(R.id.friends_num);

		String imp = getString(R.string.important);
		String dnd = getString(R.string.dnd);
		SpannableString s = new SpannableString(imp + " " + dnd);

		s.setSpan(new ForegroundColorSpan(0xffff3333), 0, imp.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		int ind = dnd.indexOf("10 SMS");
		int l = new String("10 SMS").length();
		s.setSpan(new StyleSpan(Typeface.BOLD), imp.length() + ind, imp.length() + ind + l + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		impTxt.setText(s);

		String formatString = getResources().getString(R.string.friends_on_hike_0);
		String num = Integer.toString(numHike);
		String formatted = String.format(formatString, num);
		
		SpannableString str = new SpannableString(formatted);
		int start = formatString.indexOf("%1$s");
		str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start,
				start + num.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		friendsNumTxt.setText(str);
		
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

			friendNum.setText(i + "");
			if (i == 5) 
			{
				friendNum.setText(i + "+");
				if(numHike >= 5)
				{
					v.setBackgroundResource(R.drawable.credit_item_bckg_selected);
				}
			}
			else if(i == numHike)
			{
				v.setBackgroundResource(R.drawable.credit_item_bckg_selected);
			}
			
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
	}
}
