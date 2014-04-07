package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.AddFriendAdapter;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.FtueContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;

public class AddFriendsActivity extends HikeAppStateBaseFragmentActivity implements OnItemClickListener {
	private ListView listview;
	private AddFriendAdapter mAdapter;
	private TextView nextBtn;
	private int hikeContactsCount = 0;

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
		listview.setOnItemClickListener(this);
		View header = LayoutInflater.from(this).inflate(
				R.layout.addfriends_listview_header, null);
		header.setOnClickListener(null);
		TextView headerTextView = (TextView) header.findViewById(R.id.header_txt);
		setColorSpannedText(headerTextView);
		listview.addHeaderView(header);
		listview.setAdapter(mAdapter);
		setupActionBar();
	}

	private void setColorSpannedText(TextView headerTextView)
	{
		String headerTextString = getResources().getString(R.string.add_friends_listview_header, hikeContactsCount);
		Spannable headerTextStringSpan = new SpannableString(headerTextString);  
		
		String numContactsString = getResources().getString(R.string.num_contacts, hikeContactsCount);
		int startSpan = headerTextString.indexOf(numContactsString);
		int endSpan = startSpan + numContactsString.length();
		if(startSpan>=0)
		{
			headerTextStringSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.blue_color_span)), startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		
		headerTextView.setText(headerTextStringSpan);
		
	}

	private AddFriendAdapter createAdapter()
	{
		HashMap<Integer, List<ContactInfo>> sectionsData = new HashMap<Integer, List<ContactInfo>>();
		HikeUserDatabase hikeUserDatabase = HikeUserDatabase.getInstance();
		SharedPreferences settings = getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String msisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING,
				"");
		
		List<ContactInfo>  friendsList = hikeUserDatabase.getContactsOfFavoriteType(FavoriteType.FRIEND, HikeConstants.BOTH_VALUE, msisdn, false);
		friendsList.addAll(hikeUserDatabase.getContactsOfFavoriteType(FavoriteType.REQUEST_SENT, HikeConstants.BOTH_VALUE, msisdn, false));
		friendsList.addAll(hikeUserDatabase.getContactsOfFavoriteType(FavoriteType.REQUEST_SENT_REJECTED, HikeConstants.BOTH_VALUE, msisdn, false));
		
		
		String recommendedContactsSelection = Utils.getServerRecommendedContactsSelection(settings.getString(HikeMessengerApp.SERVER_RECOMMENDED_CONTACTS, null), msisdn);
		List<ContactInfo> recommendedContacts = new ArrayList<ContactInfo>();
		if (!TextUtils.isEmpty(recommendedContactsSelection))
		{
			recommendedContacts = HikeUserDatabase.getInstance().getHikeContacts(-1, recommendedContactsSelection, null, msisdn);
			recommendedContacts.removeAll(friendsList);
			sectionsData.put(0, recommendedContacts);
		}
		
		List<ContactInfo> hikeContacts = hikeUserDatabase.getContactsOfFavoriteType(FavoriteType.NOT_FRIEND, HikeConstants.ON_HIKE_VALUE, msisdn, false);
		hikeContacts.addAll(hikeUserDatabase.getContactsOfFavoriteType(FavoriteType.REQUEST_RECEIVED_REJECTED, HikeConstants.ON_HIKE_VALUE, msisdn, false, true));
		hikeContacts.addAll(hikeUserDatabase.getContactsOfFavoriteType(FavoriteType.REQUEST_RECEIVED, HikeConstants.BOTH_VALUE, msisdn, false, true));
		hikeContactsCount = hikeContacts.size();
		hikeContacts.removeAll(recommendedContacts);
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
					sendFriendRequest(mAdapter.getSelectedFriends());
				}
				finish();
			}
		});
		actionBar.setCustomView(actionBarView);
	}

	private void sendFriendRequest(Set<ContactInfo> contacts)
	{
		for (ContactInfo contactInfo : contacts)
		{
			FavoriteType favoriteType;
			if (contactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED)
			{
				favoriteType = FavoriteType.FRIEND;
			}
			else
			{
				favoriteType = FavoriteType.REQUEST_SENT;
			}
			
			FtueContactInfo ftueContactInfo = new FtueContactInfo(contactInfo);
			ftueContactInfo.setFromFtue(true);
			Pair<ContactInfo, FavoriteType> favoriteToggle = new Pair<ContactInfo, FavoriteType>(ftueContactInfo, favoriteType);
			HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_TOGGLED, favoriteToggle);					
		}
		if(contacts.size() > 1)
		{
			Toast.makeText(this, R.string.friend_requests_sent, Toast.LENGTH_SHORT).show();
		}
		else
		{
			Toast.makeText(this, R.string.friend_request_sent, Toast.LENGTH_SHORT).show();
		}

	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position,
			long arg3) {
		position = position-listview.getHeaderViewsCount();
		ContactInfo contact = (ContactInfo) mAdapter.getItem(mAdapter.getSectionForPosition(position), mAdapter.getPositionInSectionForPosition(position));
		if (mAdapter.getSelectedFriends().contains(contact)) {
			mAdapter.unSelectItem(contact);
		} else {
			mAdapter.selectItem(contact);
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
