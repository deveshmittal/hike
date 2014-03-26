package com.bsb.hike.ui;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.AddFriendAdapter;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;

public class AddFriendsActivity extends HikeAppStateBaseFragmentActivity implements OnItemClickListener {
	private ListView listview;
	private AddFriendAdapter mAdapter;
	private TextView nextBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.addfriends);
		initializeViewComponents();
		init();
	}

	private void init() {
		mAdapter = createAdapter();
		View header = LayoutInflater.from(this).inflate(
				R.layout.addfriends_listview_header, null);
		listview.addHeaderView(header);
		listview.setAdapter(mAdapter);
		listview.setOnItemClickListener(this);
		setupActionBar();
	}

	private AddFriendAdapter createAdapter()
	{
		HashMap<Integer, List<ContactInfo>> sectionsData = new HashMap<Integer, List<ContactInfo>>();
		HikeUserDatabase hikeUserDatabase = HikeUserDatabase.getInstance();
		SharedPreferences settings = getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String msisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING,
				"");
		
		String recommendedContactsSelection = Utils.getServerRecommendedContactsSelection(settings.getString(HikeMessengerApp.SERVER_RECOMMENDED_CONTACTS, null), msisdn);
		List<ContactInfo> recommendedContacts;
		if (!TextUtils.isEmpty(recommendedContactsSelection))
		{
			recommendedContacts = HikeUserDatabase.getInstance().getHikeContacts(100, recommendedContactsSelection, null, msisdn);
			sectionsData.put(0, recommendedContacts);
		}
		
		List<ContactInfo> hikeContacts = hikeUserDatabase.getContactsOfFavoriteType(FavoriteType.NOT_FRIEND, HikeConstants.ON_HIKE_VALUE, msisdn, false);
		hikeContacts.addAll(hikeUserDatabase.getContactsOfFavoriteType(FavoriteType.REQUEST_RECEIVED_REJECTED, HikeConstants.ON_HIKE_VALUE, msisdn, false, true));
		Collections.sort(hikeContacts);
		sectionsData.put(sectionsData.size(), hikeContacts);
		
		return new AddFriendAdapter(this, -1, sectionsData,listview);
	}

	private void initializeViewComponents() {
		listview = (ListView) findViewById(R.id.addfriend_listview);
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		actionBar.setIcon(R.drawable.hike_logo_top_bar);
		
		View actionBarView = LayoutInflater.from(this).inflate(R.layout.signup_activity_action_bar, null);
		
		TextView actionBarTitle = (TextView) actionBarView.findViewById(R.id.title);
		nextBtn = (TextView) actionBarView.findViewById(R.id.next_btn);

		actionBarTitle.setText(R.string.friends_ftue_item_label);
		nextBtn.setText(R.string.skip);
		
		nextBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				if(mAdapter.getSelectedFriendsCount()>0)
				{
					
				}
				else
				{
					finish();
				}
			}
		});
		actionBar.setCustomView(actionBarView);
	}

	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position,
			long arg3) {
		position = position-listview.getHeaderViewsCount();
		ContactInfo contact = (ContactInfo) mAdapter.getItem(mAdapter.getSectionForPosition(position), mAdapter.getPositionInSectionForPosition(position));
		String msisdn = contact.getMsisdn();
		if (mAdapter.getSelectedFriends().contains(msisdn)) {
			mAdapter.unSelectItem(msisdn);
		} else {
			mAdapter.selectItem(msisdn);
		}
		
		if(mAdapter.getSelectedFriendsCount()>0)
		{
			nextBtn.setText(getResources().getString(R.string.add_count_btn, mAdapter.getSelectedFriendsCount()));
		}
		else
		{
			nextBtn.setText(R.string.skip);
		}
		mAdapter.notifyDataSetChanged();
	}
	
	@Override
	public void onBackPressed()
	{
		if(mAdapter.getSelectedFriendsCount()>0)
		{
			mAdapter.unSelectAllFriends();
			nextBtn.setText(R.string.skip);
			return;
		}
		super.onBackPressed();
	}

}
