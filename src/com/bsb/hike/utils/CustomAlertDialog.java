package com.bsb.hike.utils;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.bsb.hike.R;

public class CustomAlertDialog extends Dialog{

	TextView header;
	TextView body;
	Button btnOk;
	Button btnCancel;
		
	public CustomAlertDialog(Context context) {
		super(context,  R.style.Theme_CustomDialog);
		this.setContentView(R.layout.operator_alert_popup);
		this.setCancelable(true);
		
		header = (TextView) this.findViewById(R.id.header);
		body = (TextView) this.findViewById(R.id.body_text);
		btnOk = (Button) this.findViewById(R.id.btn_ok);
		btnCancel = (Button) this.findViewById(R.id.btn_cancel);
		
		findViewById(R.id.body_checkbox).setVisibility(View.GONE);
	}
	
	public void setHeader(String headerText){
		header.setText(headerText);
	}

	public void setHeader(int headerTextResId){
		header.setText(headerTextResId);
	}

	public void setBody(String bodyText){
		body.setText(bodyText);
	}

	public void setBody(int bodyTextResId){
		body.setText(bodyTextResId);
	}

	public void setOkButton(String ok, View.OnClickListener l){
		btnOk.setText(ok);
		btnOk.setOnClickListener(l);
	}

	public void setOkButton(int okResId, View.OnClickListener l){
		btnOk.setText(okResId);
		btnOk.setOnClickListener(l);
	}
	
	public void setCancelButton(String cancel, View.OnClickListener l){
		btnCancel.setText(cancel);
		btnCancel.setOnClickListener(l);
	}

	public void setCancelButton(int cancelResId, View.OnClickListener l){
		btnCancel.setText(cancelResId);
		btnCancel.setOnClickListener(l);
	}
	
	public void setCancelButton(String cancel){
		btnCancel.setText(cancel);
		btnCancel.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				CustomAlertDialog.this.dismiss();
			}
		});
	}
	
	public void setCancelButton(int cancelResId){
		btnCancel.setText(cancelResId);
		btnCancel.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				CustomAlertDialog.this.dismiss();
			}
		});
	}


}
