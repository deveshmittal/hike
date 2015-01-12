package com.bsb.hike.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.NUXConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.NuxCustomMessage;
import com.bsb.hike.models.NuxInviteFriends;
import com.bsb.hike.ui.utils.RecyclingImageView;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.NUXManager;
import com.bsb.hike.utils.Utils;

public class NuxSendCustomMessageActivity extends HikeAppStateBaseFragmentActivity 
{

	private TextView tapToChangeView;

	private EditText textMessageView;
	
	private HorizontalFriendsFragment newFragment = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if (Utils.requireAuth(this))
		{
			return;
		}

		getSupportActionBar().hide();

		HikeMessengerApp app = (HikeMessengerApp) getApplication();

		app.connectToService();

		setContentView(R.layout.nux_send_custom_message);

		bindViews(savedInstanceState);

		//bindListeners();

		processViewElemets();

	}

	private void bindViews(Bundle savedInstanceState)
	{
		
		textMessageView = (EditText) findViewById(R.id.multiforward_text_message);
		tapToChangeView = (TextView) findViewById(R.id.tap_to_write);
		
		if (savedInstanceState == null) {
           
			
			FragmentManager fm = getSupportFragmentManager();
			newFragment = (HorizontalFriendsFragment) fm.findFragmentByTag("chatFragment");
			if(newFragment == null){
				Logger.d("UmangX","Give me red");
				newFragment = new HorizontalFriendsFragment();
			}
			
            FragmentTransaction ft = fm.beginTransaction();
            ft.add(R.id.horizontal_friends_placeholder, newFragment, "chatFragment").commit();
        }
	}

//	private void bindListeners()
//	{
//		tapToChangeView.setOnClickListener(this);
//		
//	}

	private void processViewElemets()
	{
		NUXManager mmNuxManager = NUXManager.getInstance();
		NuxCustomMessage mmCustomMessage = mmNuxManager.getNuxCustomMessagePojo();

		if (!TextUtils.isEmpty(mmCustomMessage.getSmsMessage()))
		{
			textMessageView.setHint(mmCustomMessage.getSmsMessage());
		}
	}
	
	public String getCustomMessage(){
		if(TextUtils.isEmpty(textMessageView.getText())){
			return NUXManager.getInstance().getNuxCustomMessagePojo().getSmsMessage();
		} else {
			return textMessageView.getText().toString();
		}
	}
//	@Override
//	public void onClick(View v)
//	{
//		switch (v.getId())
//		{
//		
//		
//
//		case R.id.tap_to_write :
//			
//			//IntentManager.openNuxFriendSelector(this);
//			Toast.makeText(this, "NUX started NOW", Toast.LENGTH_LONG).show();
//			break;
//		}
//
//	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
	}
}
