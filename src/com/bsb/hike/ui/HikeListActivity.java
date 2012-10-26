package com.bsb.hike.ui;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.HikeArrayAdapter;
import com.bsb.hike.adapters.HikeInviteAdapter;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.utils.Utils;
import com.fiksu.asotracking.FiksuTrackingManager;

public class HikeListActivity extends Activity
{
	private HikeArrayAdapter adapter;
	private ListView listView;
	private TextView labelView;
	private Button titleBtn;
	private SparseBooleanArray checkedItems;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hikelistactivity);
		labelView = (TextView) findViewById(R.id.title);
		listView = (ListView) findViewById(R.id.contact_list);
		titleBtn = (Button) findViewById(R.id.title_icon);

		titleBtn.setText(R.string.send);
		titleBtn.setVisibility(View.VISIBLE);

		findViewById(R.id.button_bar_2).setVisibility(View.VISIBLE);

		listView.setTextFilterEnabled(true);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		checkedItems = listView.getCheckedItemPositions();
		
		adapter = new HikeInviteAdapter(this, -1, checkedItems);

		labelView.setText(R.string.invite_via_sms);

		listView.setAdapter(adapter);
	}

	@Override
	protected void onDestroy() 
	{
		super.onDestroy();
	}

	public void onTitleIconClick(View v) 
	{
		boolean noItemsChecked = true;
		for(int i=0; i<checkedItems.size(); i++)
		{
			if(checkedItems.valueAt(i))
			{
				ContactInfo contactInfo = (ContactInfo) adapter.getItem(checkedItems.keyAt(i));
				Log.d(getClass().getSimpleName(), "Inviting " + contactInfo.toString());
				FiksuTrackingManager.uploadPurchaseEvent(this, HikeConstants.INVITE, HikeConstants.INVITE_SENT, HikeConstants.CURRENCY);
				HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, Utils.makeHike2SMSInviteMessage(contactInfo.getMsisdn(), this).serialize());
				noItemsChecked = false;
			}
		}
		Toast.makeText(getApplicationContext(), noItemsChecked ? "Select the contacts you want to invite" : "Invites sent", Toast.LENGTH_SHORT).show();
		if(!noItemsChecked)
		{
			finish();
		}
	}
}
