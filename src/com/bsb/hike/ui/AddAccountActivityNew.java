package com.bsb.hike.ui;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.AccountUtils.AccountInfo;
import com.bsb.hike.utils.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.add_account);
		nextbtn = (Button) findViewById(R.id.addAccountButton);
		num = (EditText) findViewById(R.id.addAccountNum);
		finishbtn = (Button) findViewById(R.id.addAccountFinishButton);
		
//		final String msisdn;
		nextbtn.setOnClickListener(new OnClickListener() 
		
		{
			
			@Override
			public void onClick(View arg0) {
				msisdn = num.getText().toString();
//				Thread validateThread = new Thread (new Runnable() {
//					
//					@Override
//					public void run() {
//
//						AccountUtils.validateNumber(num.getText().toString());
//						nextbtn.setVisibility(View.GONE);
//						finishbtn.setVisibility(View.VISIBLE);
//						
//					}
//				});
//				validateThread.run();
				new ExtraAccountNumberValidateTask().execute(msisdn);
				
				
			}
		});
		
		
		finishbtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
			
			settings = AddAccountActivityNew.this.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE);	
//			Thread validateThread = new Thread (new Runnable() {
//				
//				@Override
//				public void run() {
//
//					Utils.addAccountCredentials(AddAccountActivityNew.this, newAcc, settings, settings.edit());
//					
//				}
//			});
//			validateThread.run();
//			
//			finish();
//				
//			}
//		});
			new AddExtraCredential().execute();
			}
		});
		
	}
	
	
	private class ExtraAccountNumberValidateTask extends AsyncTask<String, Void, Void> {


		@Override
		protected Void doInBackground(String... params) {
			// TODO Auto-generated method stub
			AccountUtils.validateNumber(params[0]);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void v) {
			nextbtn.setVisibility(View.GONE);
			finishbtn.setVisibility(View.VISIBLE);
			num.setText("");
			num.setHint("PIN");
			
		}

	}
	
	private class AddExtraCredential extends AsyncTask<Void, Void, Void>{

		@Override
		protected Void doInBackground(Void... params) {
			newAcc = AccountUtils.addExtraAccount(AddAccountActivityNew.this, num.getText().toString(), msisdn);
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void v) {
			Utils.addAccountCredentials(AddAccountActivityNew.this, newAcc, settings, settings.edit());
			android.os.Process.killProcess(android.os.Process.myPid());
//			AddAccountActivityNew.this.finish();
			
		}
	}

}
