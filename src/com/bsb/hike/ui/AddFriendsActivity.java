package com.bsb.hike.ui;

import java.util.List;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ListView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.AddFriendAdapter;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.utils.HikeAppStateBaseActivity;

public class AddFriendsActivity extends HikeAppStateBaseActivity {
	private ListView listview;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.addfriends);
		initializeViewComponents();
		init();
	}

	private void init() {
		SharedPreferences settings = getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String msisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING,
				"");
		List<ContactInfo> contacts = HikeUserDatabase.getInstance()
				.getHikeContacts(100, null, null, msisdn);
		AddFriendAdapter adapter = new AddFriendAdapter(this, -1, contacts,
				listview);
		View header = LayoutInflater.from(this).inflate(
				R.layout.addfriends_listview_header, null);
		listview.addHeaderView(header);
		listview.setAdapter(adapter);
	}

	private void initializeViewComponents() {
		listview = (ListView) findViewById(R.id.addfriend_listview);
	}

	public void onClickAddFriendButton(View v) {
		View popUp = findViewById(R.id.addfriend_popup);
		popUp.startAnimation(AnimationUtils.loadAnimation(this,
				R.anim.fade_out_animation));
		popUp.setVisibility(View.GONE);
	}

}
