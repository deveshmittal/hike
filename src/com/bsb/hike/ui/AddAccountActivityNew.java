package com.bsb.hike.ui;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.AccountUtils.AccountInfo;
import com.bsb.hike.utils.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class AddAccountActivityNew extends Activity {
	
	private Button nextbtn;
	private EditText num;
	private SharedPreferences accountPrefs;
	private Button finishbtn;
	private String msisdn;
	private SharedPreferences settings;
	protected AccountInfo newAcc;
	
	private class networkRunnable implements Runnable {
		public void run() {
			
		}
	} 
	
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.add_account);
		nextbtn = (Button) findViewById(R.id.addAccountButton);
		num = (EditText) findViewById(R.id.addAccountNum);
		finishbtn = (Button) findViewById(R.id.addAccountFinishButton);
		
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

		StrictMode.setThreadPolicy(policy); 
//		final String msisdn;
		nextbtn.setOnClickListener(new OnClickListener() 
		
		{
			
			@Override
			public void onClick(View arg0) {
				msisdn = num.getText().toString();
				Thread validateThread = new Thread (new Runnable() {
					
					@Override
					public void run() {

						AccountUtils.validateNumber(num.getText().toString());
						nextbtn.setVisibility(View.GONE);
						finishbtn.setVisibility(View.VISIBLE);
						
					}
				});
				validateThread.run();
				
				
			}
		});
		finishbtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
			newAcc = AccountUtils.addExtraAccount(AddAccountActivityNew.this, num.getText().toString(), msisdn);
			settings = AddAccountActivityNew.this.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE);	
			Thread validateThread = new Thread (new Runnable() {
				
				@Override
				public void run() {

					Utils.addAccountCredentials(AddAccountActivityNew.this, newAcc, settings, settings.edit());
					
				}
			});
			validateThread.run();
			
			finish();
				
			}
		});
		
		
	}

}
