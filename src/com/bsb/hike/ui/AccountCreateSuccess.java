package com.bsb.hike.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;

public class AccountCreateSuccess extends Activity {
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.accountcreatesuccess);
        Intent intent = getIntent();
        String rendered;
        Resources res = getResources();

        /* previous attempt failed, signal user to try again? */
        if (intent.getBooleanExtra("failed", false)) {
            rendered = res.getString(R.string.address_book_failed);
        } else {
            /* format message using retrieved msisdn and carrier */
            SharedPreferences settings = getSharedPreferences(
                    HikeMessengerApp.ACCOUNT_SETTINGS, 0);
            String msisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING,
                    null);
            TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            String carrierName = manager.getNetworkOperatorName();

            rendered = String.format(
                    res.getString(R.string.we_got_you_on_carrier), msisdn,
                    carrierName);
        }

        TextView view = (TextView) findViewById(R.id.textWeGotYouOnCarrier);
        view.setText(rendered);
    }

    public void onClick(View v) {
        Log.d("AccountCreateSuccess", "View Clicked");
        startActivity(new Intent(this, ScanningAddressBook.class));
        finish();
    }
}
