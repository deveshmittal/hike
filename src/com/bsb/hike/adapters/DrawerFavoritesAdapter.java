package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
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
import com.bsb.hike.ui.TellAFriend;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.DrawerLayout;

public class DrawerFavoritesAdapter extends BaseAdapter implements
		OnClickListener {
	private List<ContactInfo> completeList;
	private LayoutInflater layoutInflater;
	private Context context;

	private List<ContactInfo> favoriteList;
	private List<ContactInfo> onHikeList;
	private List<ContactInfo> recentList;

	private boolean freeSMSOn;
	private ContactInfo recentSection;
	private ContactInfo emptyFavorites;
	private ContactInfo friendsOnHikeSection;
	private ContactInfo emptyHike;
	private String status;
	private int statusDrawableResource;
	private boolean listLoaded;
	private boolean zeroContacts;

	public static final String SECTION_ID = "-911";
	public static final String EMPTY_FAVORITES_ID = "-913";
	public static final String EMPTY_HIKE_ID = "-914";

	public static final int IMAGE_BOUNDS = (int) (40 * Utils.densityMultiplier);

	public static final int ITEM_HEIGHT = (int) (55 * Utils.densityMultiplier);

	public static enum FavoriteAdapterViewType {
		SECTION, FAVORITE, EMPTY_FAVORITE, RECENT, STATUS, EMPTY_HIKE
	}

	public DrawerFavoritesAdapter(final Context context,
			final DrawerLayout drawerLayout) {
		this.context = context;
		this.layoutInflater = LayoutInflater.from(context);

		completeList = new ArrayList<ContactInfo>();
		favoriteList = new ArrayList<ContactInfo>(0);
		onHikeList = new ArrayList<ContactInfo>(0);
		recentList = new ArrayList<ContactInfo>(0);
		freeSMSOn = PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(HikeConstants.FREE_SMS_PREF, false);
		SharedPreferences preferences = context.getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		status = preferences.getString(HikeMessengerApp.LAST_STATUS,
				context.getString(R.string.default_status));
		int moodId = preferences.getInt(HikeMessengerApp.LAST_MOOD, -1);
		statusDrawableResource = moodId == -1
				|| moodId >= Utils.getMoodsResource().length ? R.drawable.ic_text_status
				: Utils.getMoodsResource()[moodId];

		/*
		 * Show the initial View with the loader icon.
		 */
		makeCompleteList();

		new AsyncTask<Void, Void, Void>() {

			List<ContactInfo> favoriteTaskList;
			List<ContactInfo> onHikeTaskList;
			List<ContactInfo> recentTaskList;

			@Override
			protected Void doInBackground(Void... params) {
				Log.d(getClass().getSimpleName(),
						"Favorite List started fetching");

				String myMsisdn = context.getSharedPreferences(
						HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(
						HikeMessengerApp.MSISDN_SETTING, "");

				HikeUserDatabase hikeUserDatabase = HikeUserDatabase
						.getInstance();

				favoriteTaskList = hikeUserDatabase
						.getContactsOfFavoriteType(FavoriteType.FRIEND,
								HikeConstants.BOTH_VALUE, myMsisdn);
				favoriteTaskList.addAll(hikeUserDatabase
						.getContactsOfFavoriteType(FavoriteType.REQUEST_SENT,
								HikeConstants.BOTH_VALUE, myMsisdn));
				favoriteTaskList.addAll(hikeUserDatabase
						.getContactsOfFavoriteType(
								FavoriteType.REQUEST_SENT_REJECTED,
								HikeConstants.BOTH_VALUE, myMsisdn));
				Collections.sort(favoriteTaskList);

				onHikeTaskList = hikeUserDatabase.getContactsOfFavoriteType(
						FavoriteType.NOT_FRIEND, HikeConstants.ON_HIKE_VALUE,
						myMsisdn);
				onHikeTaskList.addAll(hikeUserDatabase
						.getContactsOfFavoriteType(
								FavoriteType.REQUEST_RECEIVED,
								HikeConstants.ON_HIKE_VALUE, myMsisdn));
				onHikeTaskList.addAll(hikeUserDatabase
						.getContactsOfFavoriteType(
								FavoriteType.REQUEST_RECEIVED_REJECTED,
								HikeConstants.ON_HIKE_VALUE, myMsisdn));
				Collections.sort(onHikeTaskList);

				recentTaskList = hikeUserDatabase.getRecentContacts(-1, false,
						FavoriteType.NOT_FRIEND, freeSMSOn ? 1 : 0, myMsisdn);

				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				Log.d(getClass().getSimpleName(), "Favorite List fetched");
				listLoaded = true;
				drawerLayout.findViewById(R.id.loading_progress).setVisibility(
						View.GONE);

				favoriteList = favoriteTaskList;
				onHikeList = onHikeTaskList;
				recentList = recentTaskList;
				makeCompleteList();
			}

		}.execute();
	}

	private void makeCompleteList() {
		completeList.clear();

		// For the status item
		completeList.add(null);

		if (listLoaded) {

			if (favoriteList.isEmpty() && recentList.isEmpty()
					&& onHikeList.isEmpty()) {
				zeroContacts = true;
				completeList.add(new ContactInfo(
						DrawerFavoritesAdapter.SECTION_ID, null, context
								.getString(R.string.contacts_on_hike,
										onHikeList.size()), null));

				emptyFavorites = new ContactInfo(
						DrawerFavoritesAdapter.EMPTY_FAVORITES_ID, null, null,
						null);
				completeList.add(emptyFavorites);
			} else {
				// Contact for "Favorite Section"
				friendsOnHikeSection = new ContactInfo(
						DrawerFavoritesAdapter.SECTION_ID, null,
						context.getString(R.string.circle_friends,
								favoriteList.size()), null);
				completeList.add(friendsOnHikeSection);
				/*
				 * If favorite list is empty, we add an element to show the
				 * empty view in the listview.
				 */
				if (favoriteList.isEmpty()) {
					emptyFavorites = new ContactInfo(
							DrawerFavoritesAdapter.EMPTY_FAVORITES_ID, null,
							null, null);
					completeList.add(emptyFavorites);
				} else {
					completeList.addAll(favoriteList);
				}

				// Contact for "Recent Section"
				recentSection = new ContactInfo(
						DrawerFavoritesAdapter.SECTION_ID, null,
						context.getString(R.string.recent), null);
				completeList.add(recentSection);

				int recentListLastElement = recentList.size() > HikeConstants.RECENT_COUNT_IN_FAVORITE ? HikeConstants.RECENT_COUNT_IN_FAVORITE
						: recentList.size();
				completeList.addAll(recentList
						.subList(0, recentListLastElement));

				// Contact for "On Hike Section"
				completeList.add(new ContactInfo(
						DrawerFavoritesAdapter.SECTION_ID, null, context
								.getString(R.string.contacts_on_hike,
										onHikeList.size()), null));
				if (onHikeList.isEmpty()) {
					emptyHike = new ContactInfo(EMPTY_HIKE_ID, null, null, null);
					completeList.add(emptyHike);
				} else {
					completeList.addAll(onHikeList);
				}
			}
		} else {
			/*
			 * Showing the progress loading icon while the list is being fetched
			 * from the db.
			 */
			// Contact for "On Hike Section"
			completeList.add(new ContactInfo(DrawerFavoritesAdapter.SECTION_ID,
					null, context.getString(R.string.contacts_on_hike,
							onHikeList.size()), null));
		}

		notifyDataSetChanged();
	}

	public void addFavoriteItem(ContactInfo contactInfo) {
		/*
		 * We first check if we are showing the empty favorites item. If we are,
		 * we remove it before adding the new favorite.
		 */
		if (favoriteList.isEmpty()) {
			completeList.remove(emptyFavorites);
		}
		// Remove from the other lists.
		removeContactFromListByMatchingMsisdn(recentList, contactInfo);
		removeContactFromListByMatchingMsisdn(onHikeList, contactInfo);
		removeContactFromListByMatchingMsisdn(favoriteList, contactInfo);

		favoriteList.add(0, contactInfo);
		Collections.sort(favoriteList);

		makeCompleteList();
	}

	public void removeFavoriteItem(ContactInfo contactInfo) {
		removeContactFromListByMatchingMsisdn(favoriteList, contactInfo);

		if (TextUtils.isEmpty(contactInfo.getName())) {
			makeCompleteList();
			return;
		}
		/*
		 * Adding the contact back to the recents list based on whether the
		 * contact is on hike or not since the contact was removed from the
		 * favorites list.
		 */
		if (contactInfo.isOnhike()) {
			onHikeList.add(contactInfo);
			Collections.sort(onHikeList);
		} else {
			recentList.add(contactInfo);
			Collections.sort(recentList, new Comparator<ContactInfo>() {
				@Override
				public int compare(ContactInfo lhs, ContactInfo rhs) {
					if (lhs.getLastMessaged() != rhs.getLastMessaged()) {
						return -((Long) lhs.getLastMessaged()).compareTo(rhs
								.getLastMessaged());
					} else {
						return (lhs.getName().toLowerCase()).compareTo(rhs
								.getName().toLowerCase());
					}
				}
			});
		}

		makeCompleteList();
	}

	public void updateRecentContactsList(ContactInfo contactInfo) {
		/*
		 * Return if object is null or its a favorite contact or if the contact
		 * is a non hike non Indian contact and the user is Indian.
		 */

		if (contactInfo == null
				|| (contactInfo.getFavoriteType() == FavoriteType.FRIEND)
				|| (HikeMessengerApp.isIndianUser() && !contactInfo.isOnhike() && !contactInfo
						.getMsisdn().startsWith(
								HikeConstants.INDIA_COUNTRY_CODE))) {
			Log.d(getClass().getSimpleName(), "Null contact");
			return;
		}

		// Remove the contact if it already exists
		removeContactFromListByMatchingMsisdn(onHikeList, contactInfo);
		removeContactFromListByMatchingMsisdn(recentList, contactInfo);

		if (contactInfo.isOnhike()) {
			if (onHikeList.isEmpty()) {
				completeList.remove(emptyHike);
			}
			onHikeList.add(0, contactInfo);
			Collections.sort(onHikeList);
		} else {
			recentList.add(0, contactInfo);
			/*
			 * If we added a new contact then we delete the last item to
			 * maintain uniformity in size.
			 */
			if (recentList.size() > HikeConstants.RECENT_COUNT_IN_FAVORITE) {
				recentList.remove(recentList.size() - 1);
			}
		}
		makeCompleteList();
	}

	public void removeContact(ContactInfo contactInfo) {
		removeContactFromListByMatchingMsisdn(onHikeList, contactInfo);
		removeContactFromListByMatchingMsisdn(recentList, contactInfo);
		removeContactFromListByMatchingMsisdn(favoriteList, contactInfo);

		makeCompleteList();
	}

	private void removeContactFromListByMatchingMsisdn(
			List<ContactInfo> contactList, ContactInfo contactInfo) {

		List<ContactInfo> deleteContactList = new ArrayList<ContactInfo>();
		for (ContactInfo listContact : contactList) {
			if (listContact.getMsisdn().equals(contactInfo.getMsisdn())) {
				deleteContactList.add(listContact);
			}
		}
		contactList.removeAll(deleteContactList);
	}

	public void refreshFavoritesList(List<ContactInfo> favoriteList) {
		this.favoriteList = favoriteList;
		Collections.sort(favoriteList);

		onHikeList.removeAll(favoriteList);
		recentList.removeAll(favoriteList);

		makeCompleteList();
	}

	public void refreshRecents(List<ContactInfo> recents) {
		recentList = recents;
		makeCompleteList();
	}

	public void updateStatus(String status, int moodId) {
		this.status = status;
		this.statusDrawableResource = moodId == -1
				|| moodId >= Utils.getMoodsResource().length ? R.drawable.ic_text_status
				: Utils.getMoodsResource()[moodId];
		notifyDataSetChanged();
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public int getItemViewType(int position) {
		ContactInfo contactInfo = getItem(position);
		if (position == 0) {
			return FavoriteAdapterViewType.STATUS.ordinal();
		} else if (SECTION_ID.equals(contactInfo.getId())) {
			return FavoriteAdapterViewType.SECTION.ordinal();
		} else if (EMPTY_FAVORITES_ID.equals(contactInfo.getId())) {
			return FavoriteAdapterViewType.EMPTY_FAVORITE.ordinal();
		} else if (EMPTY_HIKE_ID.equals(contactInfo.getId())) {
			return FavoriteAdapterViewType.EMPTY_HIKE.ordinal();
		} else if (contactInfo.getFavoriteType() == FavoriteType.NOT_FRIEND
				|| contactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED
				|| contactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED_REJECTED) {
			return FavoriteAdapterViewType.RECENT.ordinal();
		}
		return FavoriteAdapterViewType.FAVORITE.ordinal();
	}

	@Override
	public int getViewTypeCount() {
		return FavoriteAdapterViewType.values().length;
	}

	@Override
	public boolean isEnabled(int position) {
		FavoriteAdapterViewType viewType = FavoriteAdapterViewType.values()[getItemViewType(position)];
		if (viewType == FavoriteAdapterViewType.EMPTY_FAVORITE
				|| viewType == FavoriteAdapterViewType.SECTION
				|| viewType == FavoriteAdapterViewType.STATUS
				|| viewType == FavoriteAdapterViewType.EMPTY_HIKE) {
			return false;
		}
		return true;
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
	public View getView(int position, View convertView, ViewGroup parent) {
		ContactInfo contactInfo = getItem(position);
		FavoriteAdapterViewType viewType = FavoriteAdapterViewType.values()[getItemViewType(position)];

		ViewHolder viewHolder;

		if (convertView == null) {
			viewHolder = new ViewHolder();

			switch (viewType) {
			case STATUS:
				convertView = layoutInflater
						.inflate(R.layout.status_item, null);
				viewHolder.text = (TextView) convertView
						.findViewById(R.id.status_text);
				viewHolder.textInfo = (TextView) convertView
						.findViewById(R.id.status_text_info);
				viewHolder.avatarImg = (ImageView) convertView
						.findViewById(R.id.avatar);
				break;
			case RECENT:
			case FAVORITE:
				convertView = layoutInflater
						.inflate(R.layout.drawer_item, null);

				viewHolder.addImg = (ImageView) convertView
						.findViewById(R.id.add_fav);
				viewHolder.avatarImg = (ImageView) convertView
						.findViewById(R.id.item_icon);
				viewHolder.text = (TextView) convertView
						.findViewById(R.id.item_name);
				viewHolder.hikeImg = (ImageView) convertView
						.findViewById(R.id.hike_icon);
				viewHolder.invite = (Button) convertView
						.findViewById(R.id.invite_fav);
				break;

			case SECTION:
				convertView = (TextView) layoutInflater.inflate(
						R.layout.drawer_section, null);
				break;

			case EMPTY_FAVORITE:
				convertView = layoutInflater.inflate(R.layout.empty_favorites,
						null);

				viewHolder.text = (TextView) convertView
						.findViewById(R.id.item_txt);
				viewHolder.invite = (Button) convertView
						.findViewById(R.id.invite_btn);
				break;
			case EMPTY_HIKE:
				convertView = layoutInflater.inflate(
						R.layout.no_contact_on_hike, null);
				viewHolder.invite = (Button) convertView
						.findViewById(R.id.invite_btn);
			}
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		switch (viewType) {
		case STATUS:
			if (context.getString(R.string.default_status).equals(status)) {
				viewHolder.textInfo.setVisibility(View.VISIBLE);
				String tapHere = context.getString(R.string.tap_here);

				SpannableStringBuilder ssb = new SpannableStringBuilder(status);
				if (status.indexOf(tapHere) != -1) {
					ssb.setSpan(new StyleSpan(Typeface.BOLD),
							status.indexOf(tapHere), status.indexOf(tapHere)
									+ tapHere.length(),
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}

				viewHolder.text.setText(ssb);
			} else {
				viewHolder.textInfo.setVisibility(View.GONE);
				viewHolder.text.setText(SmileyParser.getInstance()
						.addSmileySpans(status, true));
			}
			viewHolder.avatarImg.setImageResource(statusDrawableResource);
			viewHolder.avatarImg
					.setBackgroundResource(statusDrawableResource == R.drawable.ic_text_status ? R.drawable.bg_status_type
							: 0);
			convertView.setOnClickListener(this);

			int statusHeight = (int) (75 * Utils.densityMultiplier);
			AbsListView.LayoutParams statusLp = new AbsListView.LayoutParams(
					AbsListView.LayoutParams.MATCH_PARENT, statusHeight);
			convertView.setLayoutParams(statusLp);
			break;
		case RECENT:
			viewHolder.hikeImg.setVisibility(View.GONE);
		case FAVORITE:
			if (viewType == FavoriteAdapterViewType.RECENT) {
				viewHolder.addImg.setOnClickListener(this);
				viewHolder.addImg.setTag(contactInfo);
				viewHolder.addImg.setVisibility(View.VISIBLE);
				viewHolder.invite.setVisibility(View.GONE);
				viewHolder.addImg.setImageResource(R.drawable.add_fav);

				LayoutParams lp = (LayoutParams) viewHolder.text
						.getLayoutParams();
				lp.addRule(RelativeLayout.LEFT_OF, R.id.add_fav);
			} else {
				viewHolder.addImg.setVisibility(View.GONE);
			}
			viewHolder.avatarImg.setImageDrawable(IconCacheManager
					.getInstance().getIconForMSISDN(contactInfo.getMsisdn()));
			viewHolder.text
					.setText(TextUtils.isEmpty(contactInfo.getName()) ? contactInfo
							.getMsisdn() : contactInfo.getName());

			LayoutParams lp = (LayoutParams) viewHolder.avatarImg
					.getLayoutParams();
			lp.height = lp.width = IMAGE_BOUNDS;
			viewHolder.avatarImg.setLayoutParams(lp);

			android.widget.AbsListView.LayoutParams layoutParams = new android.widget.AbsListView.LayoutParams(
					LayoutParams.MATCH_PARENT, ITEM_HEIGHT);
			convertView.setLayoutParams(layoutParams);

			break;

		case SECTION:
			((TextView) convertView).setText(contactInfo.getName());
			break;

		case EMPTY_FAVORITE:
			if (zeroContacts) {
				viewHolder.text.setText(R.string.no_contacts_hike);
				viewHolder.invite.setVisibility(View.VISIBLE);
				viewHolder.invite.setOnClickListener(this);
			} else {
				viewHolder.invite.setVisibility(View.GONE);

				String text = context.getString(R.string.no_friends);
				String replace = context.getString(R.string.plus);
				SpannableString spannableString = new SpannableString(text);

				Drawable drawable = context.getResources().getDrawable(
						R.drawable.ic_add_fav);
				int height = drawable.getIntrinsicHeight();
				int width = drawable.getIntrinsicWidth();
				drawable.setBounds(0, 0, width, height);

				spannableString.setSpan(new ImageSpan(drawable),
						text.indexOf(replace),
						text.indexOf(replace) + replace.length(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				viewHolder.text.setText(spannableString);
			}
			break;

		case EMPTY_HIKE:
			viewHolder.invite.setOnClickListener(this);

			android.widget.AbsListView.LayoutParams lParams = new android.widget.AbsListView.LayoutParams(
					LayoutParams.MATCH_PARENT, ITEM_HEIGHT);
			convertView.setLayoutParams(lParams);
		}
		return convertView;
	}

	private class ViewHolder {
		ImageView avatarImg;
		ImageView hikeImg;
		Button invite;
		TextView text;
		ImageView addImg;
		TextView textInfo;
	}

	@Override
	public void onClick(View v) {

		ContactInfo contactInfo = null;
		Object tag = v.getTag();

		if (tag instanceof ContactInfo) {
			contactInfo = (ContactInfo) tag;
		}

		if (v.getId() == R.id.add_fav) {
			if (contactInfo.getFavoriteType() == FavoriteType.FRIEND) {
				Intent callIntent = new Intent(Intent.ACTION_CALL);
				callIntent.setData(Uri.parse("tel:" + contactInfo.getMsisdn()));
				context.startActivity(callIntent);
			} else {
				FavoriteType favoriteType;
				if (contactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED) {
					favoriteType = FavoriteType.FRIEND;
				} else {
					favoriteType = FavoriteType.REQUEST_SENT;
				}
				Pair<ContactInfo, FavoriteType> favoriteAdded = new Pair<ContactInfo, FavoriteType>(
						contactInfo, favoriteType);
				HikeMessengerApp.getPubSub().publish(
						HikePubSub.FAVORITE_TOGGLED, favoriteAdded);
			}
		} else if (v.getId() == R.id.invite_fav) {
			HikeMessengerApp.getPubSub().publish(
					HikePubSub.MQTT_PUBLISH,
					Utils.makeHike2SMSInviteMessage(contactInfo.getMsisdn(),
							context).serialize());
			Toast.makeText(context, R.string.invite_sent, Toast.LENGTH_SHORT)
					.show();
		} else if (v.getId() == R.id.add) {
			Pair<ContactInfo, FavoriteType> favoriteAdded = new Pair<ContactInfo, FavoriteType>(
					contactInfo, FavoriteType.FRIEND);
			HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_TOGGLED,
					favoriteAdded);
		} else if (v.getId() == R.id.not_now) {
			Pair<ContactInfo, FavoriteType> favoriteRemoved = new Pair<ContactInfo, FavoriteType>(
					contactInfo, FavoriteType.NOT_FRIEND);
			HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_TOGGLED,
					favoriteRemoved);
		} else if (v.getId() == R.id.status_item) {
			// show the status UI
			HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_STATUS_DIALOG,
					null);
		} else if (v.getId() == R.id.invite_btn) {
			context.startActivity(new Intent(context, TellAFriend.class));
		}
	}
}
