package com.bsb.hike.ui;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.AddFriendAdapter;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.utils.HikeAppStateBaseActivity;
import com.bsb.hike.utils.Utils;

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
		AddFriendAdapter adapter = createAdapter();
		View header = LayoutInflater.from(this).inflate(
				R.layout.addfriends_listview_header, null);
		listview.addHeaderView(header);
		listview.setAdapter(adapter);
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

}
