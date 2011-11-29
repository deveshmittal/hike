package com.bsb.hike.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.AccountUtils;

public class PhoneNumberFindActivity extends Activity {
	private PhoneNumberFindActivity mThis;
	private String mCarrierName;

	private class RegisterTask extends AsyncTask<Void, Integer, String> {
		protected String doInBackground(Void... none) {
			Log.w("RegisterTask", "Trying to get MSISDN");
			String msisdn = null;
			try {
				msisdn = AccountUtils.getMSISDN();
			} catch (Exception e) {
				Log.e("RegisterTask", "Got an exception");
			}

			if (msisdn != null) {
				SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
				SharedPreferences.Editor editor = settings.edit();
				editor.putString(HikeMessengerApp.MSISDN_SETTING, msisdn);
				editor.putString(HikeMessengerApp.CARRIER_SETTING, mCarrierName);
				editor.commit();
			}

			return msisdn;
		}

		protected void onPostExecute(String msisdn) {
			Log.d("RegisterTask", "Something called me: " + msisdn);
			if (msisdn != null) {
				startActivity(new Intent(mThis, AccountCreateStep.class));
				finish();
			} else {
				//startActivity(new Intent(mThis, SMSAuthTokenEnterStep.class));
				finish();
			}
		}
	}

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	mThis = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phonenumberfind);
        Resources res = getResources();
        TelephonyManager manager = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
        mCarrierName = manager.getNetworkOperatorName();
        String text = String.format(res.getString(R.string.we_got_you_on_carrier), mCarrierName);
        TextView label = (TextView) findViewById(R.id.msisdn_label);
        label.setText(text);
        RegisterTask registerTask = new RegisterTask();
        registerTask.execute();
    }
}