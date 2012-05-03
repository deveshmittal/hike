package com.bsb.hike.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.bsb.hike.R;

public class TermsAndConditionsActivity extends Activity {

	private TextView mTitleView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.terms_conditions);
		
		mTitleView = (TextView) findViewById(R.id.title);
		mTitleView.setText(R.string.terms_privacy);
	}
	
}