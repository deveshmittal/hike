package com.bsb.hike.ui;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;

public class AccountCreateStep extends Activity implements OnClickListener, OnCheckedChangeListener {

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

        //attach event listeners
        mNameField = (EditText) findViewById(R.id.name_field);
        mCheckBox = (CheckBox) findViewById(R.id.checkbox_tc);
        mNameField.setOnClickListener(this);
        mCheckBox.setOnCheckedChangeListener(this);
	}

	private void updateNextButtonState() {
		mNextButton.setEnabled((mNameField.getText().length() > 0) && mCheckBox.isChecked());
//		mNextButton.setClickable((mNameField.getText().length() > 0) && mCheckBox.isChecked());
	}

	public void onClick(View v) {
		Log.d("AccountCreateStep", "onClick: " +v);
		updateNextButtonState();
	}

	@Override
	public void onCheckedChanged(CompoundButton btn, boolean val) {
		Log.d("AccountCreateStep", "onCheck: " +btn);
		updateNextButtonState();
	}

}
