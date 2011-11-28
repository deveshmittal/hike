package com.bsb.hike.ui;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;

public class AccountCreateStep extends Activity implements OnCheckedChangeListener {

	private EditText mNameField;
	private CheckBox mCheckBox;
	private Button mNextButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

 	   setContentView(R.layout.accountcreatestep);

       mNameField = (EditText) findViewById(R.id.name_field);
       mCheckBox = (CheckBox) findViewById(R.id.checkbox_tc);
       mNextButton = (Button) findViewById(R.id.next);

       SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
       String msisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING, null);
       String carrierName = settings.getString(HikeMessengerApp.CARRIER_SETTING, null);

        //format the label
        Resources res = getResources();
        String text = String.format(res.getString(R.string.enter_name_message), carrierName, msisdn);
        Log.d("AccountCreateStep", "Formatted text is" + text);
        TextView label = (TextView) findViewById(R.id.enter_name_message);
        label.setText(text);

        mNameField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
        	@Override
        	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        		if (actionId == EditorInfo.IME_ACTION_DONE) {
        			Log.d("create", "called");
        			updateNextButtonState();
        			return false;
        		}
        		return false;
        	}
        });

        mCheckBox.setOnCheckedChangeListener(this);
	}

	private void updateNextButtonState() {
		mNextButton.setEnabled((mNameField.getText().length() > 0) && mCheckBox.isChecked());
	}

	@Override
	public void onCheckedChanged(CompoundButton btn, boolean val) {
		Log.d("AccountCreateStep", "onCheck: " +btn);
		updateNextButtonState();
	}

}
