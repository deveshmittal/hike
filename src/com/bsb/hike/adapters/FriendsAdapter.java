package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.WhichScreen;
import com.bsb.hike.view.PinnedSectionListView.PinnedSectionListAdapter;

public class FriendsAdapter extends BaseAdapter implements OnClickListener,
		PinnedSectionListAdapter {

	public static final int FRIEND_INDEX = 0;
	public static final int HIKE_INDEX = 1;
	public static final int SMS_INDEX = 2;

	public static final String EXTRA_ID = "-910";
	public static final String SECTION_ID = "-911";
	public static final String EMPTY_ID = "-912";
	public static final String REMOVE_SUGGESTIONS_ID = "-913";

	public static final String INVITE_MSISDN = "-123";
	public static final String GROUP_MSISDN = "-124";

	public static final String FRIEND_PHONE_NUM = "-125";
	public static final String CONTACT_PHONE_NUM = "--126";

	public enum ViewType {
		SECTION, FRIEND, NOT_FRIEND_HIKE, NOT_FRIEND_SMS, FRIEND_REQUEST, EXTRA, EMPTY, FTUE_CONTACT, REMOVE_SUGGESTIONS
	}

	private LayoutInflater layoutInflater;

	private List<ContactInfo> completeList;

	private List<ContactInfo> friendsList;
	private List<ContactInfo> hikeContactsList;
	private List<ContactInfo> smsContactsList;
	private List<ContactInfo> filteredFriendsList;
	private List<ContactInfo> filteredHikeContactsList;
	private List<ContactInfo> filteredSmsContactsList;

	private Context context;
	private ContactInfo friendsSection;
	private ContactInfo hikeContactsSection;
	private ContactInfo smsContactsSection;
	private ContactInfo inviteExtraItem;
	private ContactInfo groupExtraItem;
	private ContactFilter contactFilter;
	private String queryText;
	private boolean lastSeenPref;
	private boolean showSMSContacts;

	public FriendsAdapter(final Context context) {
		this.layoutInflater = LayoutInflater.from(context);
		this.context = context;
		this.contactFilter = new ContactFilter();
		this.lastSeenPref = PreferenceManager.getDefaultSharedPreferences(
				context).getBoolean(HikeConstants.LAST_SEEN_PREF, true);
		this.showSMSContacts = PreferenceManager.getDefaultSharedPreferences(
				context).getBoolean(HikeConstants.FREE_SMS_PREF, true)
				|| Utils.getSendSmsPref(context);

		completeList = new ArrayList<ContactInfo>();

		friendsList = new ArrayList<ContactInfo>(0);
		hikeContactsList = new ArrayList<ContactInfo>(0);
		smsContactsList = new ArrayList<ContactInfo>(0);

		filteredFriendsList = new ArrayList<ContactInfo>(0);
		filteredHikeContactsList = new ArrayList<ContactInfo>(0);
		filteredSmsContactsList = new ArrayList<ContactInfo>(0);

		FetchFriendsTask fetchFriendsTask = new FetchFriendsTask();
		Utils.executeAsyncTask(fetchFriendsTask);
	}

	private class FetchFriendsTask extends AsyncTask<Void, Void, Void> {

		List<ContactInfo> favoriteTaskList;
		List<ContactInfo> hikeTaskList;
		List<ContactInfo> smsTaskList;

		@Override
		protected Void doInBackground(Void... params) {
			String myMsisdn = context.getSharedPreferences(
					HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(
					HikeMessengerApp.MSISDN_SETTING, "");

			boolean nativeSMSOn = Utils.getSendSmsPref(context);

			HikeUserDatabase hikeUserDatabase = HikeUserDatabase.getInstance();

			favoriteTaskList = hikeUserDatabase.getContactsOfFavoriteType(
					FavoriteType.FRIEND, HikeConstants.BOTH_VALUE, myMsisdn,
					nativeSMSOn);
			favoriteTaskList.addAll(hikeUserDatabase.getContactsOfFavoriteType(
					FavoriteType.REQUEST_RECEIVED, HikeConstants.BOTH_VALUE,
					myMsisdn, nativeSMSOn, false));
			favoriteTaskList.addAll(hikeUserDatabase.getContactsOfFavoriteType(
					FavoriteType.REQUEST_SENT, HikeConstants.BOTH_VALUE,
					myMsisdn, nativeSMSOn));
			favoriteTaskList.addAll(hikeUserDatabase.getContactsOfFavoriteType(
					FavoriteType.REQUEST_SENT_REJECTED,
					HikeConstants.BOTH_VALUE, myMsisdn, nativeSMSOn));
			Collections.sort(favoriteTaskList,
					ContactInfo.lastSeenTimeComparator);

			hikeTaskList = hikeUserDatabase.getContactsOfFavoriteType(
					FavoriteType.NOT_FRIEND, HikeConstants.ON_HIKE_VALUE,
					myMsisdn, nativeSMSOn);
			hikeTaskList.addAll(hikeUserDatabase.getContactsOfFavoriteType(
					FavoriteType.REQUEST_RECEIVED_REJECTED,
					HikeConstants.ON_HIKE_VALUE, myMsisdn, nativeSMSOn, true));
			Collections.sort(hikeTaskList);

			smsTaskList = hikeUserDatabase.getContactsOfFavoriteType(
					FavoriteType.NOT_FRIEND, HikeConstants.NOT_ON_HIKE_VALUE,
					myMsisdn, nativeSMSOn);
			Collections.sort(smsTaskList);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			friendsList = favoriteTaskList;
			hikeContactsList = hikeTaskList;
			smsContactsList = smsTaskList;

			filteredFriendsList.addAll(favoriteTaskList);
			filteredHikeContactsList.addAll(hikeTaskList);
			filteredSmsContactsList.addAll(smsTaskList);
			makeCompleteList(true);
		}
	}

	public void onQueryChanged(String s) {
		queryText = s;
		contactFilter.filter(queryText);
	}

	private class ContactFilter extends Filter {
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();

			if (!TextUtils.isEmpty(constraint)) {

				String textToBeFiltered = constraint.toString().toLowerCase()
						.trim();

				List<ContactInfo> filteredFriendsList = new ArrayList<ContactInfo>();
				List<ContactInfo> filteredHikeContactsList = new ArrayList<ContactInfo>();
				List<ContactInfo> filteredSmsContactsList = new ArrayList<ContactInfo>();

				for (ContactInfo info : friendsList) {
					String name = info.getName();
					if (name != null) {
						name = name.toLowerCase();
						if (name.contains(textToBeFiltered)) {
							filteredFriendsList.add(info);
							continue;
						}
					}

					if (info.getMsisdn() != null) {
						if (info.getMsisdn().contains(textToBeFiltered)) {
							filteredFriendsList.add(info);
						}
					}
				}

				for (ContactInfo info : hikeContactsList) {
					String name = info.getName();
					if (name != null) {
						name = name.toLowerCase();
						if (name.contains(textToBeFiltered)) {
							filteredHikeContactsList.add(info);
							continue;
						}
					}

					if (info.getMsisdn() != null) {
						if (info.getMsisdn().contains(textToBeFiltered)) {
							filteredHikeContactsList.add(info);
						}
					}
				}

				for (ContactInfo info : smsContactsList) {
					String name = info.getName();
					if (name != null) {
						name = name.toLowerCase();
						if (name.contains(textToBeFiltered)) {
							filteredSmsContactsList.add(info);
							continue;
						}
					}

					if (info.getMsisdn() != null) {
						if (info.getMsisdn().contains(textToBeFiltered)) {
							filteredSmsContactsList.add(info);
						}
					}
				}

				List<List<ContactInfo>> resultList = new ArrayList<List<ContactInfo>>(
						3);
				resultList.add(filteredFriendsList);
				resultList.add(filteredHikeContactsList);
				resultList.add(filteredSmsContactsList);

				results.values = resultList;
			} else {
				List<List<ContactInfo>> resultList = new ArrayList<List<ContactInfo>>(
						3);
				resultList.add(friendsList);
				resultList.add(hikeContactsList);
				resultList.add(smsContactsList);

				results.values = resultList;
			}
			results.count = 1;
			return results;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results) {
			List<List<ContactInfo>> resultList = (List<List<ContactInfo>>) results.values;

			filteredFriendsList.clear();
			filteredFriendsList.addAll(resultList.get(0));

			filteredHikeContactsList.clear();
			filteredHikeContactsList.addAll(resultList.get(1));

			filteredSmsContactsList.clear();
			filteredSmsContactsList.addAll(resultList.get(2));

			makeCompleteList(true);
		}
	}

	public void makeCompleteList(boolean filtered) {
		if (!filtered) {
			contactFilter.filter(queryText);
			return;
		}

		if ((friendsList.isEmpty() && hikeContactsList.isEmpty() && smsContactsList
				.isEmpty())
				|| (filteredFriendsList.isEmpty()
						&& filteredHikeContactsList.isEmpty() && filteredSmsContactsList
							.isEmpty())) {
			return;
		}

		completeList.clear();

		if (TextUtils.isEmpty(queryText)) {
			inviteExtraItem = new ContactInfo(EXTRA_ID, INVITE_MSISDN,
					context.getString(R.string.invite_friends_hike), null);
			completeList.add(inviteExtraItem);

			groupExtraItem = new ContactInfo(EXTRA_ID, GROUP_MSISDN,
					context.getString(R.string.create_group), null);
			completeList.add(groupExtraItem);
		}

		friendsSection = new ContactInfo(SECTION_ID,
				Integer.toString(filteredFriendsList.size()),
				context.getString(R.string.friends), FRIEND_PHONE_NUM);
		completeList.add(friendsSection);

		boolean hideSuggestions = true;

		if (!HomeActivity.ftueList.isEmpty() && TextUtils.isEmpty(queryText)
				&& friendsList.size() < HikeConstants.FTUE_LIMIT) {
			SharedPreferences prefs = context.getSharedPreferences(
					HikeMessengerApp.ACCOUNT_SETTINGS, 0);

			hideSuggestions = prefs.getBoolean(
					HikeMessengerApp.HIDE_FTUE_SUGGESTIONS, false);

			if (!hideSuggestions) {
				long firstTimeViewFtueSuggestionsTs = prefs.getLong(
						HikeMessengerApp.FIRST_VIEW_FTUE_LIST_TIMESTAMP, 0);

				long currentTime = System.currentTimeMillis() / 1000;

				if (currentTime - firstTimeViewFtueSuggestionsTs > 60 * 60) {
					completeList.add(new ContactInfo(REMOVE_SUGGESTIONS_ID,
							null, null, null));
				}

				int limit = HikeConstants.FTUE_LIMIT - friendsList.size();

				int counter = 0;
				for (ContactInfo contactInfo : HomeActivity.ftueList) {
					FavoriteType favoriteType = contactInfo.getFavoriteType();
					if (favoriteType == FavoriteType.NOT_FRIEND
							|| favoriteType == FavoriteType.REQUEST_RECEIVED_REJECTED
							|| favoriteType == null) {
						completeList.add(contactInfo);
						if (++counter == limit) {
							break;
						}
					}
				}
				if (!friendsList.isEmpty()) {
					completeList.addAll(filteredFriendsList);
				}
			}
		}

		if (hideSuggestions) {
			if (friendsList.isEmpty()) {
				if (TextUtils.isEmpty(queryText)) {
					completeList
							.add(new ContactInfo(EMPTY_ID, null, null, null));
				}
			} else {
				completeList.addAll(filteredFriendsList);
			}
		}

		if (!hikeContactsList.isEmpty()) {
			hikeContactsSection = new ContactInfo(SECTION_ID,
					Integer.toString(filteredHikeContactsList.size()),
					context.getString(R.string.hike_contacts),
					CONTACT_PHONE_NUM);
			completeList.add(hikeContactsSection);

			completeList.addAll(filteredHikeContactsList);
		}

		if (showSMSContacts) {
			smsContactsSection = new ContactInfo(SECTION_ID,
					Integer.toString(filteredSmsContactsList.size()),
					context.getString(R.string.sms_contacts), CONTACT_PHONE_NUM);
			completeList.add(smsContactsSection);

			completeList.addAll(filteredSmsContactsList);
		}

		notifyDataSetChanged();
	}

	public void toggleShowSMSContacts(boolean showSMSOn) {
		this.showSMSContacts = showSMSOn;
		notifyDataSetChanged();
	}

	private void removeContactByMatchingMsisdn(List<ContactInfo> contactList,
			ContactInfo contactInfo) {
		for (int i = 0; i < contactList.size(); i++) {
			ContactInfo listContactInfo = contactList.get(i);
			if (listContactInfo.getMsisdn().equals(contactInfo.getMsisdn())) {
				contactList.remove(i);
				break;
			}
		}
	}

	public void removeContact(ContactInfo contactInfo,
			boolean remakeCompleteList) {
		removeContactByMatchingMsisdn(friendsList, contactInfo);

		removeContactByMatchingMsisdn(hikeContactsList, contactInfo);

		removeContactByMatchingMsisdn(smsContactsList, contactInfo);
		if (remakeCompleteList) {
			makeCompleteList(false);
		}
	}

	public void addToGroup(ContactInfo contactInfo, int groupIndex) {
		removeContact(contactInfo, false);

		if (getCount() == 0) {
			return;
		}

		switch (groupIndex) {
		case FRIEND_INDEX:
			friendsList.add(contactInfo);
			Collections.sort(friendsList, ContactInfo.lastSeenTimeComparator);
			break;
		case HIKE_INDEX:
			hikeContactsList.add(contactInfo);
			Collections.sort(hikeContactsList);
			break;
		case SMS_INDEX:
			smsContactsList.add(contactInfo);
			Collections.sort(smsContactsList);
			break;
		}

		makeCompleteList(false);
	}

	public void refreshGroupList(List<ContactInfo> newGroupList, int groupIndex) {
		List<ContactInfo> groupList = null;
		switch (groupIndex) {
		case FRIEND_INDEX:
			groupList = friendsList;
			break;
		case HIKE_INDEX:
			groupList = hikeContactsList;
			break;
		case SMS_INDEX:
			groupList = smsContactsList;
			break;
		}
		groupList.clear();

		groupList.addAll(newGroupList);

		makeCompleteList(false);
	}

	public void removeFromGroup(ContactInfo contactInfo, int groupIndex) {
		switch (groupIndex) {
		case FRIEND_INDEX:
			removeContactByMatchingMsisdn(friendsList, contactInfo);
			break;
		case HIKE_INDEX:
			removeContactByMatchingMsisdn(hikeContactsList, contactInfo);
			break;
		case SMS_INDEX:
			removeContactByMatchingMsisdn(smsContactsList, contactInfo);
			break;
		}
		makeCompleteList(false);
	}

	@Override
	public int getCount() {
		return completeList.size();
	}

	@Override
	public ContactInfo getItem(int position) {
		return completeList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getViewTypeCount() {
		return ViewType.values().length;
	}

	@Override
	public int getItemViewType(int position) {
		ContactInfo contactInfo = getItem(position);
		if (EMPTY_ID.equals(contactInfo.getId())) {
			return ViewType.EMPTY.ordinal();
		} else if (SECTION_ID.equals(contactInfo.getId())) {
			return ViewType.SECTION.ordinal();
		} else if (EXTRA_ID.equals(contactInfo.getId())) {
			return ViewType.EXTRA.ordinal();
		} else if (REMOVE_SUGGESTIONS_ID.equals(contactInfo.getId())) {
			return ViewType.REMOVE_SUGGESTIONS.ordinal();
		} else {
			FavoriteType favoriteType = contactInfo.getFavoriteType();
			if (favoriteType == FavoriteType.FRIEND
					|| favoriteType == FavoriteType.REQUEST_SENT
					|| favoriteType == FavoriteType.REQUEST_SENT_REJECTED) {
				return ViewType.FRIEND.ordinal();
			} else if (favoriteType == FavoriteType.REQUEST_RECEIVED) {
				return ViewType.FRIEND_REQUEST.ordinal();
			} else if (HikeConstants.FTUE_MSISDN_TYPE.equals(contactInfo
					.getMsisdnType())) {
				return ViewType.FTUE_CONTACT.ordinal();
			} else if (contactInfo.isOnhike()) {
				return ViewType.NOT_FRIEND_HIKE.ordinal();
			}
			return ViewType.NOT_FRIEND_SMS.ordinal();
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		ViewType viewType = ViewType.values()[getItemViewType(position)];

		ContactInfo contactInfo = getItem(position);

		if (convertView == null) {
			switch (viewType) {
			case FTUE_CONTACT:
			case FRIEND:
				convertView = layoutInflater.inflate(
						R.layout.friends_child_view, null);
				break;

			case NOT_FRIEND_HIKE:
				convertView = layoutInflater.inflate(
						R.layout.hike_contact_child_view, null);
				break;

			case NOT_FRIEND_SMS:
				convertView = layoutInflater.inflate(
						R.layout.sms_contact_child_view, null);
				break;

			case SECTION:
				convertView = layoutInflater.inflate(
						R.layout.friends_group_view, null);
				break;
			case FRIEND_REQUEST:
				convertView = layoutInflater.inflate(
						R.layout.friend_request_view, null);
				break;
			case EXTRA:
				convertView = layoutInflater.inflate(
						R.layout.friends_tab_extra_item, null);
				break;
			case EMPTY:
				convertView = layoutInflater.inflate(
						R.layout.friends_empty_view, parent, false);
				break;
			case REMOVE_SUGGESTIONS:
				convertView = layoutInflater.inflate(
						R.layout.remove_suggestions, parent, false);
			}
		}

		switch (viewType) {
		case FRIEND:
		case NOT_FRIEND_HIKE:
		case FRIEND_REQUEST:
		case NOT_FRIEND_SMS:
		case FTUE_CONTACT:

			ImageView avatar = (ImageView) convertView
					.findViewById(R.id.avatar);
			TextView name = (TextView) convertView.findViewById(R.id.contact);

			avatar.setImageDrawable(IconCacheManager.getInstance()
					.getIconForMSISDN(contactInfo.getMsisdn(), true));
			name.setText(TextUtils.isEmpty(contactInfo.getName()) ? contactInfo
					.getMsisdn() : contactInfo.getName());

			if (viewType == ViewType.FRIEND
					|| viewType == ViewType.FRIEND_REQUEST
					|| viewType == ViewType.FTUE_CONTACT) {
				TextView lastSeen = (TextView) convertView
						.findViewById(R.id.last_seen);
				ImageView avatarFrame = (ImageView) convertView
						.findViewById(R.id.avatar_frame);

				lastSeen.setTextColor(context.getResources().getColor(
						R.color.list_item_subtext));
				lastSeen.setVisibility(View.GONE);

				avatarFrame
						.setImageResource(R.drawable.frame_avatar_medium_selector);

				TextView inviteBtn = (TextView) convertView
						.findViewById(R.id.invite_btn);
				if (inviteBtn != null) {
					inviteBtn.setVisibility(View.GONE);
				}

				if (contactInfo.getFavoriteType() == FavoriteType.FRIEND
						&& lastSeenPref) {
					String lastSeenString = Utils.getLastSeenTimeAsString(
							context, contactInfo.getLastSeenTime(),
							contactInfo.getOffline());
					if (!TextUtils.isEmpty(lastSeenString)) {
						if (contactInfo.getOffline() == 0) {
							lastSeen.setTextColor(context.getResources()
									.getColor(R.color.action_bar_disabled_text));
							avatarFrame
									.setImageResource(R.drawable.frame_avatar_medium_highlight_selector);
						}
						lastSeen.setVisibility(View.VISIBLE);
						lastSeen.setText(lastSeenString);
					}
				} else {
					if (contactInfo.getFavoriteType() == FavoriteType.REQUEST_SENT) {
						lastSeen.setVisibility(View.VISIBLE);
						lastSeen.setText(R.string.request_pending);

						if (!contactInfo.isOnhike()) {
							setInviteButton(contactInfo, inviteBtn, null);
						}
					} else if (viewType == ViewType.FRIEND_REQUEST) {
						lastSeen.setVisibility(View.VISIBLE);
						lastSeen.setText(R.string.sent_friend_request);

						ImageView acceptBtn = (ImageView) convertView
								.findViewById(R.id.accept);
						ImageView rejectBtn = (ImageView) convertView
								.findViewById(R.id.reject);

						acceptBtn.setTag(contactInfo);
						rejectBtn.setTag(contactInfo);

						acceptBtn.setOnClickListener(acceptOnClickListener);
						rejectBtn.setOnClickListener(rejectOnClickListener);

					} else if (viewType == ViewType.FTUE_CONTACT) {
						lastSeen.setVisibility(View.VISIBLE);
						lastSeen.setText(R.string.ftue_friends_subtext);

						TextView addBtn = (TextView) convertView
								.findViewById(R.id.invite_btn);

						addBtn.setVisibility(View.VISIBLE);
						addBtn.setText(R.string.add);
						addBtn.setTag(contactInfo);
						addBtn.setOnClickListener(addOnClickListener);
					}
				}
			} else {
				TextView info = (TextView) convertView.findViewById(R.id.info);
				info.setText(contactInfo.isOnhike() ? R.string.tap_chat
						: R.string.tap_sms);
				if (viewType == ViewType.NOT_FRIEND_HIKE) {
					ImageView addFriend = (ImageView) convertView
							.findViewById(R.id.add_friend);

					addFriend.setTag(contactInfo);
					addFriend.setOnClickListener(this);
				} else {
					TextView inviteBtn = (TextView) convertView
							.findViewById(R.id.invite_btn);
					ImageView inviteIcon = (ImageView) convertView
							.findViewById(R.id.invite_icon);
					ViewGroup infoContainer = (ViewGroup) convertView
							.findViewById(R.id.info_container);

					setInviteButton(contactInfo, inviteBtn, inviteIcon);

					LayoutParams layoutParams = (LayoutParams) infoContainer
							.getLayoutParams();
					if (inviteIcon.getVisibility() == View.VISIBLE) {
						layoutParams.addRule(RelativeLayout.LEFT_OF,
								inviteIcon.getId());
					} else {
						layoutParams.addRule(RelativeLayout.LEFT_OF,
								inviteBtn.getId());
					}
				}
			}
			break;

		case SECTION:
			TextView headerName = (TextView) convertView
					.findViewById(R.id.name);
			TextView headerCount = (TextView) convertView
					.findViewById(R.id.count);
			ImageView icon = (ImageView) convertView.findViewById(R.id.icon);

			headerName.setText(contactInfo.getName());
			headerCount.setText(contactInfo.getMsisdn());
			icon.setImageResource(FRIEND_PHONE_NUM.equals(contactInfo
					.getPhoneNum()) ? R.drawable.ic_header_friends
					: R.drawable.ic_header_contacts);
			break;

		case EXTRA:
			TextView headerName2 = (TextView) convertView
					.findViewById(R.id.contact);
			ImageView headerIcon = (ImageView) convertView
					.findViewById(R.id.icon);

			if (contactInfo.getMsisdn().equals(INVITE_MSISDN)) {
				headerIcon.setImageResource(R.drawable.ic_invite_to_hike);
				headerName2.setText(R.string.invite_friends_hike);
			} else {
				headerIcon.setImageResource(R.drawable.ic_create_group);
				headerName2.setText(R.string.create_group);
			}
			break;

		case EMPTY:
			TextView emptyText = (TextView) convertView
					.findViewById(R.id.empty_text);

			String text = context.getString(R.string.tap_plus_add_friends);
			int index = text.indexOf("+");

			SpannableStringBuilder ssb = new SpannableStringBuilder(text);
			ssb.setSpan(new ImageSpan(context, R.drawable.ic_add_friend),
					index, index + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			emptyText.setText(ssb);
			break;
		}

		return convertView;
	}

	private void setInviteButton(ContactInfo contactInfo, TextView inviteBtn,
			ImageView inviteIcon) {

		inviteBtn.setEnabled(true);
		inviteBtn.setBackgroundResource(R.drawable.bg_green_btn_selector);
		inviteBtn.setTextColor(context.getResources().getColor(R.color.white));

		inviteBtn.setOnClickListener(inviteOnClickListener);
		inviteBtn.setTag(contactInfo);

		if (inviteIcon != null) {
			inviteIcon.setOnClickListener(inviteOnClickListener);
			inviteIcon.setTag(contactInfo);
		}

		if (contactInfo.getInviteTime() == 0) {
			if (inviteIcon != null) {
				inviteIcon.setVisibility(View.VISIBLE);
				inviteBtn.setVisibility(View.GONE);
			} else {
				inviteBtn.setVisibility(View.VISIBLE);
				inviteBtn.setText(R.string.invite_1);
			}
		} else {
			if (inviteIcon != null) {
				inviteIcon.setVisibility(View.GONE);
			}
			inviteBtn.setVisibility(View.VISIBLE);

			long inviteTime = contactInfo.getInviteTime();

			/*
			 * If the contact was invited more than an hour back, we give the
			 * option to remind this contact
			 */
			if ((System.currentTimeMillis() / 1000 - inviteTime) > 60 * 60) {
				inviteBtn.setText(R.string.remind);
			} else {
				inviteBtn.setEnabled(false);
				inviteBtn.setText(R.string.invited);
				inviteBtn.setBackgroundResource(0);
				inviteBtn.setTextColor(context.getResources().getColor(
						R.color.description_lightgrey));
			}
		}
	}

	@Override
	public void onClick(View v) {

		ContactInfo contactInfo = null;
		Object tag = v.getTag();

		if (tag instanceof ContactInfo) {
			contactInfo = (ContactInfo) tag;
		}

		FavoriteType favoriteType;
		if (contactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED) {
			favoriteType = FavoriteType.FRIEND;
		} else {
			favoriteType = FavoriteType.REQUEST_SENT;
			Toast.makeText(context, R.string.friend_request_sent,
					Toast.LENGTH_SHORT).show();
		}

		Pair<ContactInfo, FavoriteType> favoriteAdded = new Pair<ContactInfo, FavoriteType>(
				contactInfo, favoriteType);
		HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_TOGGLED,
				favoriteAdded);
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		ContactInfo contactInfo = getItem(position);
		if (SECTION_ID.equals(contactInfo.getId())) {
			return false;
		} else {
			return true;
		}
	}

	private OnClickListener acceptOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			ContactInfo contactInfo = (ContactInfo) v.getTag();
			respondToFriendRequest(contactInfo, true);
		}
	};

	private OnClickListener rejectOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			ContactInfo contactInfo = (ContactInfo) v.getTag();
			respondToFriendRequest(contactInfo, false);
		}
	};

	private OnClickListener inviteOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			ContactInfo contactInfo = (ContactInfo) v.getTag();
			Utils.sendInviteUtil(contactInfo, context,
					HikeConstants.SINGLE_INVITE_SMS_ALERT_CHECKED,
					context.getString(R.string.native_header),
					context.getString(R.string.native_info),
					WhichScreen.SMS_SECTION);
		}
	};

	private OnClickListener addOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			ContactInfo contactInfo = (ContactInfo) v.getTag();

			FavoriteType favoriteType;
			if (contactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED) {
				favoriteType = FavoriteType.FRIEND;
			} else {
				favoriteType = FavoriteType.REQUEST_SENT;
				Toast.makeText(context, R.string.friend_request_sent,
						Toast.LENGTH_SHORT).show();
			}

			/*
			 * Cloning the object since we don't want to send the ftue
			 * reference.
			 */
			ContactInfo contactInfo2 = new ContactInfo(contactInfo);

			Pair<ContactInfo, FavoriteType> favoriteAdded = new Pair<ContactInfo, FavoriteType>(
					contactInfo2, favoriteType);
			HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_TOGGLED,
					favoriteAdded);

			Utils.sendUILogEvent(HikeConstants.LogEvent.ADD_FRIENDS_CLICK, contactInfo2.getMsisdn());

			if (!contactInfo.isOnhike())
				Utils.sendInviteUtil(
						contactInfo2,
						context,
						HikeConstants.FTUE_ADD_SMS_ALERT_CHECKED,
						context.getString(R.string.ftue_add_prompt_invite_title),
						context.getString(R.string.ftue_add_prompt_invite),
						WhichScreen.FRIENDS_TAB);

		}
	};

	private void respondToFriendRequest(ContactInfo contactInfo, boolean accept) {
		FavoriteType favoriteType = accept ? FavoriteType.FRIEND
				: FavoriteType.REQUEST_RECEIVED_REJECTED;
		Pair<ContactInfo, FavoriteType> favoriteAdded = new Pair<ContactInfo, FavoriteType>(
				contactInfo, favoriteType);
		HikeMessengerApp
				.getPubSub()
				.publish(
						favoriteType == FavoriteType.FRIEND ? HikePubSub.FAVORITE_TOGGLED
								: HikePubSub.REJECT_FRIEND_REQUEST,
						favoriteAdded);

		removeFromGroup(contactInfo, FRIEND_INDEX);
	}

	@Override
	public boolean isItemViewTypePinned(int viewType) {
		return viewType == ViewType.SECTION.ordinal();
	}

	public List<ContactInfo> getCompleteList() {
		return completeList;
	}

	public void setCompleteList(List<ContactInfo> completeList) {
		this.completeList = completeList;
	}

	public List<ContactInfo> getFriendsList() {
		return friendsList;
	}

	public void setFriendsList(List<ContactInfo> friendsList) {
		this.friendsList = friendsList;
	}

	public List<ContactInfo> getFilteredFriendsList() {
		return filteredFriendsList;
	}

	public void setFilteredFriendsList(List<ContactInfo> filteredFriendsList) {
		this.filteredFriendsList = filteredFriendsList;
	}

}
