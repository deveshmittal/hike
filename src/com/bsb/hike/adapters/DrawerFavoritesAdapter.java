package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
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
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.DrawerLayout;

public class DrawerFavoritesAdapter extends BaseAdapter implements
		OnClickListener {
	private List<ContactInfo> completeList;
	private LayoutInflater layoutInflater;
	private Context context;

	private List<ContactInfo> recommendedFavoriteList;
	private List<ContactInfo> favoriteList;
	private List<ContactInfo> onHikeList;
	private List<ContactInfo> recentList;

	private boolean freeSMSOn;
	private ContactInfo recentSection;
	private ContactInfo emptyFavorites;

	public static final String SECTION_ID = "-911";
	public static final String EMPTY_FAVORITES_ID = "-913";

	public static final int IMAGE_BOUNDS = (int) (40 * Utils.densityMultiplier);

	public static final int ITEM_HEIGHT = (int) (55 * Utils.densityMultiplier);

	public static enum FavoriteAdapterViewType {
		SECTION, FAVORITE, EMPTY_FAVORITE, RECENT, RECOMMENDED_FAVORITE
	}

	public DrawerFavoritesAdapter(Context context,
			final DrawerLayout drawerLayout) {
		completeList = new ArrayList<ContactInfo>();
		recommendedFavoriteList = new ArrayList<ContactInfo>(0);
		favoriteList = new ArrayList<ContactInfo>(0);
		onHikeList = new ArrayList<ContactInfo>(0);
		recentList = new ArrayList<ContactInfo>(0);
		freeSMSOn = PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(HikeConstants.FREE_SMS_PREF, false);
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				HikeUserDatabase hikeUserDatabase = HikeUserDatabase
						.getInstance();

				recommendedFavoriteList = hikeUserDatabase
						.getContactsOfFavoriteType(
								FavoriteType.AUTO_RECOMMENDED_FAVORITE,
								HikeConstants.BOTH_VALUE);
				recommendedFavoriteList.addAll(hikeUserDatabase
						.getContactsOfFavoriteType(
								FavoriteType.RECOMMENDED_FAVORITE,
								HikeConstants.BOTH_VALUE));

				favoriteList = hikeUserDatabase.getContactsOfFavoriteType(
						FavoriteType.FAVORITE, HikeConstants.BOTH_VALUE);
				Collections.sort(favoriteList);

				onHikeList = hikeUserDatabase.getContactsOfFavoriteType(
						FavoriteType.NOT_FAVORITE, HikeConstants.ON_HIKE_VALUE);

				recentList = hikeUserDatabase.getNonHikeRecentContacts(-1,
						HikeMessengerApp.isIndianUser(),
						FavoriteType.NOT_FAVORITE);

				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				makeCompleteList();
				drawerLayout.updatePendingRequests();
			}

		}.execute();

		this.context = context;
		this.layoutInflater = LayoutInflater.from(context);
	}

	private void makeCompleteList() {
		completeList.clear();

		// Contact for "Favorite Section"
		completeList.add(new ContactInfo(DrawerFavoritesAdapter.SECTION_ID,
				null, context.getString(R.string.favorites), null));

		/*
		 * If favorite list is empty, we add an element to show the empty view
		 * in the listview.
		 */
		if (favoriteList.isEmpty() && recommendedFavoriteList.isEmpty()) {
			emptyFavorites = new ContactInfo(
					DrawerFavoritesAdapter.EMPTY_FAVORITES_ID, null, null, null);
			completeList.add(emptyFavorites);
		} else {
			completeList.addAll(recommendedFavoriteList);
			completeList.addAll(favoriteList);
		}

		// Contact for "On Hike Section"
		completeList.add(new ContactInfo(DrawerFavoritesAdapter.SECTION_ID,
				null, context.getString(R.string.friends_on_hike), null));
		completeList.addAll(onHikeList);

		// Contact for "Recent Section"
		recentSection = new ContactInfo(DrawerFavoritesAdapter.SECTION_ID,
				null,
				(freeSMSOn && HikeMessengerApp.isIndianUser()) ? context
						.getString(R.string.recent) : context
						.getString(R.string.invite_friends), null);
		completeList.add(recentSection);

		int recentListLastElement = recentList.size() > HikeConstants.RECENT_COUNT_IN_FAVORITE ? HikeConstants.RECENT_COUNT_IN_FAVORITE
				: recentList.size();
		completeList.addAll(recentList.subList(0, recentListLastElement));
		notifyDataSetChanged();
	}

	public void addFavoriteItem(ContactInfo contactInfo) {
		/*
		 * We first check if we are showing the empty favorites item. If we are,
		 * we remove it before adding the new favorite.
		 */
		if (favoriteList.isEmpty() && recommendedFavoriteList.isEmpty()) {
			completeList.remove(emptyFavorites);
		}
		// Remove from the other lists.
		removeContactFromListByMatchingMsisdn(recentList, contactInfo);
		removeContactFromListByMatchingMsisdn(recommendedFavoriteList,
				contactInfo);
		removeContactFromListByMatchingMsisdn(onHikeList, contactInfo);
		removeContactFromListByMatchingMsisdn(favoriteList, contactInfo);

		favoriteList.add(0, contactInfo);
		Collections.sort(favoriteList);

		makeCompleteList();
	}

	public void addRecommendedFavoriteItem(ContactInfo contactInfo) {
		/*
		 * We first check if we are showing the empty favorites item. If we are,
		 * we remove it before adding the new favorite.
		 */
		if (favoriteList.isEmpty() && recommendedFavoriteList.isEmpty()) {
			completeList.remove(emptyFavorites);
		}
		// Remove from the hike list.
		removeContactFromListByMatchingMsisdn(onHikeList, contactInfo);
		// Remove from the recents list.
		removeContactFromListByMatchingMsisdn(recentList, contactInfo);
		recommendedFavoriteList.add(0, contactInfo);

		makeCompleteList();
	}

	public void addAutoRecommendedFavorites(List<ContactInfo> contactInfoList) {
		recommendedFavoriteList.addAll(contactInfoList);

		recentList.removeAll(contactInfoList);
		onHikeList.removeAll(contactInfoList);

		makeCompleteList();
	}

	public void removeFavoriteItem(ContactInfo contactInfo) {
		removeContactFromListByMatchingMsisdn(recommendedFavoriteList,
				contactInfo);
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
				|| (contactInfo.getFavoriteType() == FavoriteType.FAVORITE)
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

		recommendedFavoriteList.removeAll(favoriteList);
		onHikeList.removeAll(favoriteList);
		recentList.removeAll(favoriteList);

		makeCompleteList();
	}

	public void refreshRecommendedFavorites(
			List<ContactInfo> recommendedFavoriteList) {
		this.recommendedFavoriteList = recommendedFavoriteList;
		Collections.sort(recommendedFavoriteList);

		favoriteList.removeAll(recommendedFavoriteList);
		onHikeList.removeAll(recommendedFavoriteList);
		recentList.removeAll(recommendedFavoriteList);

		makeCompleteList();
	}

	public void freeSMSToggled(boolean freeSMS) {
		this.freeSMSOn = freeSMS;
		recentSection
				.setName((freeSMSOn && HikeMessengerApp.isIndianUser()) ? context
						.getString(R.string.recent) : context
						.getString(R.string.invite_friends));
		notifyDataSetChanged();
	}

	public int getPendingRequests() {
		return recommendedFavoriteList.size();
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public int getItemViewType(int position) {
		ContactInfo contactInfo = getItem(position);
		if (SECTION_ID.equals(contactInfo.getId())) {
			return FavoriteAdapterViewType.SECTION.ordinal();
		} else if (EMPTY_FAVORITES_ID.equals(contactInfo.getId())) {
			return FavoriteAdapterViewType.EMPTY_FAVORITE.ordinal();
		} else if (contactInfo.getFavoriteType() == FavoriteType.NOT_FAVORITE) {
			return FavoriteAdapterViewType.RECENT.ordinal();
		} else if (contactInfo.getFavoriteType() == FavoriteType.RECOMMENDED_FAVORITE
				|| contactInfo.getFavoriteType() == FavoriteType.AUTO_RECOMMENDED_FAVORITE) {
			return FavoriteAdapterViewType.RECOMMENDED_FAVORITE.ordinal();
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
				|| viewType == FavoriteAdapterViewType.SECTION) {
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
			case RECENT:
			case FAVORITE:
				convertView = layoutInflater
						.inflate(R.layout.drawer_item, null);

				viewHolder.addImg = (ImageView) convertView
						.findViewById(R.id.add_fav);
				viewHolder.avatarImg = (ImageView) convertView
						.findViewById(R.id.item_icon);
				viewHolder.name = (TextView) convertView
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

				viewHolder.name = (TextView) convertView
						.findViewById(R.id.item_txt);
				break;

			case RECOMMENDED_FAVORITE:
				convertView = layoutInflater.inflate(
						R.layout.recommended_favorite_item, null);

				viewHolder.avatarImg = (ImageView) convertView
						.findViewById(R.id.item_icon);
				viewHolder.name = (TextView) convertView
						.findViewById(R.id.item_msg);
				viewHolder.addToFav = (Button) convertView
						.findViewById(R.id.add);
				viewHolder.notNow = (Button) convertView
						.findViewById(R.id.not_now);
				break;
			}
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		switch (viewType) {
		case RECENT:
			viewHolder.hikeImg
					.setVisibility(contactInfo.isOnhike() ? View.VISIBLE
							: View.GONE);
		case FAVORITE:
			if (viewType == FavoriteAdapterViewType.RECENT) {
				if ((!HikeMessengerApp.isIndianUser() && !contactInfo
						.isOnhike())
						|| (HikeMessengerApp.isIndianUser()
								&& !contactInfo.isOnhike() && !freeSMSOn)) {
					viewHolder.addImg.setVisibility(View.GONE);
					viewHolder.invite.setVisibility(View.VISIBLE);
					viewHolder.invite.setOnClickListener(this);
					viewHolder.invite.setTag(contactInfo);

					LayoutParams lp = (LayoutParams) viewHolder.name
							.getLayoutParams();
					lp.addRule(RelativeLayout.LEFT_OF, R.id.invite_fav);
				} else {
					viewHolder.addImg.setOnClickListener(this);
					viewHolder.addImg.setTag(contactInfo);
					viewHolder.addImg.setVisibility(View.VISIBLE);
					viewHolder.invite.setVisibility(View.GONE);
					viewHolder.addImg.setImageResource(R.drawable.add_fav);

					LayoutParams lp = (LayoutParams) viewHolder.name
							.getLayoutParams();
					lp.addRule(RelativeLayout.LEFT_OF, R.id.add_fav);
				}
			} else {
				viewHolder.addImg.setVisibility(View.GONE);
			}
			viewHolder.avatarImg.setImageDrawable(IconCacheManager
					.getInstance().getIconForMSISDN(contactInfo.getMsisdn()));
			viewHolder.name
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
			String text = viewHolder.name.getText().toString();
			String replace = context.getString(R.string.plus);
			SpannableString spannableString = new SpannableString(text);

			Drawable drawable = context.getResources().getDrawable(
					R.drawable.ic_add_favorite);
			int height = (int) ((7 * drawable.getIntrinsicHeight()) / 10);
			int width = (int) ((7 * drawable.getIntrinsicWidth()) / 10);
			drawable.setBounds(0, 0, width, height);

			spannableString.setSpan(new ImageSpan(drawable),
					text.indexOf(replace),
					text.indexOf(replace) + replace.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			viewHolder.name.setText(spannableString);
			break;

		case RECOMMENDED_FAVORITE:
			String name = contactInfo.getFirstName();
			String msg = context
					.getString(
							contactInfo.getFavoriteType() == FavoriteType.RECOMMENDED_FAVORITE ? R.string.recommended_favorite
									: R.string.auto_recommended_favorite, name);
			SpannableStringBuilder message = new SpannableStringBuilder(msg);
			message.setSpan(new ForegroundColorSpan(context.getResources()
					.getColor(R.color.drawer_text)),
					msg.indexOf(name) + name.length(), msg.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			viewHolder.avatarImg.setImageDrawable(IconCacheManager
					.getInstance().getIconForMSISDN(contactInfo.getMsisdn()));
			viewHolder.name.setText(message);

			viewHolder.addToFav.setTag(contactInfo);
			viewHolder.addToFav.setOnClickListener(this);

			viewHolder.notNow.setTag(contactInfo);
			viewHolder.notNow.setOnClickListener(this);
			break;
		}
		return convertView;
	}

	private class ViewHolder {
		ImageView avatarImg;
		ImageView hikeImg;
		Button invite;
		TextView name;
		ImageView addImg;
		Button addToFav;
		Button notNow;
	}

	@Override
	public void onClick(View v) {
		ContactInfo contactInfo = (ContactInfo) v.getTag();
		if (v.getId() == R.id.add_fav) {
			if (contactInfo.getFavoriteType() == FavoriteType.FAVORITE) {
				Intent callIntent = new Intent(Intent.ACTION_CALL);
				callIntent.setData(Uri.parse("tel:" + contactInfo.getMsisdn()));
				context.startActivity(callIntent);
			} else {
				Pair<ContactInfo, FavoriteType> favoriteAdded = new Pair<ContactInfo, FavoriteType>(
						contactInfo, FavoriteType.FAVORITE);
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
					contactInfo, FavoriteType.FAVORITE);
			HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_TOGGLED,
					favoriteAdded);
		} else if (v.getId() == R.id.not_now) {
			Pair<ContactInfo, FavoriteType> favoriteRemoved = new Pair<ContactInfo, FavoriteType>(
					contactInfo, FavoriteType.NOT_FAVORITE);
			HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_TOGGLED,
					favoriteRemoved);
		}
	}
}
