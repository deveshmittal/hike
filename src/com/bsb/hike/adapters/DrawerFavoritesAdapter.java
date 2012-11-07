package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
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
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.utils.Utils;

public class DrawerFavoritesAdapter extends BaseAdapter implements
		OnClickListener {
	private List<ContactInfo> completeList;
	private LayoutInflater layoutInflater;
	private Context context;

	private List<ContactInfo> favoriteList;
	private List<ContactInfo> recommendedFavoriteList;
	private List<ContactInfo> recentList;

	public static final String FAVORITES_SECTION_ID = "-911";
	public static final String RECENTS_SECTION_ID = "-912";
	public static final String EMPTY_FAVORITES_ID = "-913";

	public static final int IMAGE_BOUNDS = (int) (40 * Utils.densityMultiplier);

	public static enum FavoriteAdapterViewType {
		SECTION, FAVORITE, EMPTY_FAVORITE, RECENT, RECOMMENDED_FAVORITE
	}

	public DrawerFavoritesAdapter(Context context) {
		HikeUserDatabase hikeUserDatabase = HikeUserDatabase.getInstance();

		recommendedFavoriteList = hikeUserDatabase
				.getContactsOrderedByLastMessaged(-1,
						FavoriteType.RECOMMENDED_FAVORITE, false, true);
		recommendedFavoriteList.addAll(hikeUserDatabase
				.getContactsOrderedByLastMessaged(-1,
						FavoriteType.AUTO_RECOMMENDED_FAVORITE, false, true));
		favoriteList = hikeUserDatabase.getContactsOrderedByLastMessaged(-1,
				FavoriteType.FAVORITE, false, true);
		recentList = hikeUserDatabase.getContactsOrderedByLastMessaged(
				HikeConstants.RECENT_COUNT_IN_FAVORITE,
				FavoriteType.NOT_FAVORITE, false, true);

		completeList = new ArrayList<ContactInfo>();
		makeCompleteList();

		this.context = context;
		this.layoutInflater = LayoutInflater.from(context);
	}

	private void makeCompleteList() {
		completeList.clear();

		// Contact for "Favorite Section"
		completeList.add(new ContactInfo(
				DrawerFavoritesAdapter.FAVORITES_SECTION_ID, null,
				HikeConstants.FAVORITES, null));

		/*
		 * If favorite list is empty, we add an element to show the empty view
		 * in the listview.
		 */
		if (favoriteList.isEmpty() && recommendedFavoriteList.isEmpty()) {
			completeList
					.add(new ContactInfo(
							DrawerFavoritesAdapter.EMPTY_FAVORITES_ID, null,
							null, null));
		} else {
			completeList.addAll(recommendedFavoriteList);
			completeList.addAll(favoriteList);
		}

		// Contact for "Recent Section"
		completeList.add(new ContactInfo(
				DrawerFavoritesAdapter.RECENTS_SECTION_ID, null,
				HikeConstants.RECENT, null));

		completeList.addAll(recentList);
		notifyDataSetChanged();
	}

	public void addFavoriteItem(ContactInfo contactInfo) {
		/*
		 * We first check if we are showing the empty favorites item. If we are,
		 * we remove it before adding the new favorite.
		 */
		if (favoriteList.isEmpty() && recommendedFavoriteList.isEmpty()) {
			completeList.remove(1);
		}
		// Remove from the other lists.
		recentList.remove(contactInfo);
		recommendedFavoriteList.remove(contactInfo);

		favoriteList.add(0, contactInfo);

		makeCompleteList();
	}

	public void addRecommendedFavoriteItem(ContactInfo contactInfo) {
		/*
		 * We first check if we are showing the empty favorites item. If we are,
		 * we remove it before adding the new favorite.
		 */
		if (favoriteList.isEmpty() && recommendedFavoriteList.isEmpty()) {
			completeList.remove(1);
		}
		// Remove from the recents list.
		recentList.remove(contactInfo);
		recommendedFavoriteList.add(0, contactInfo);

		makeCompleteList();
	}

	public void removeFavoriteItem(ContactInfo contactInfo) {
		recommendedFavoriteList.remove(contactInfo);
		favoriteList.remove(contactInfo);

		makeCompleteList();
	}

	public void updateRecentContactsList(ContactInfo contactInfo) {
		// Return if object is null
		if (contactInfo == null) {
			Log.d(getClass().getSimpleName(), "Null contact");
			return;
		}

		if (contactInfo.getFavoriteType() == FavoriteType.FAVORITE) {
			Log.d(getClass().getSimpleName(), "contact already a favorite");
			return;
		}
		// Remove the contact if it already exists
		recentList.remove(contactInfo);

		recentList.add(0, contactInfo);
		/*
		 * If we added a new contact then we delete the last item to maintain
		 * uniformity in size.
		 */
		if (recentList.size() > HikeConstants.RECENT_COUNT_IN_FAVORITE) {
			recentList.remove(recentList.size() - 1);
		}
		makeCompleteList();
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public int getItemViewType(int position) {
		ContactInfo contactInfo = getItem(position);
		if (FAVORITES_SECTION_ID.equals(contactInfo.getId())
				|| RECENTS_SECTION_ID.equals(contactInfo.getId())) {
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
			viewHolder.addImg.setVisibility(View.VISIBLE);
			viewHolder.addImg.setTag(contactInfo);
			viewHolder.addImg.setOnClickListener(this);
		case FAVORITE:
			viewHolder.avatarImg.setImageDrawable(IconCacheManager
					.getInstance().getIconForMSISDN(contactInfo.getMsisdn()));
			viewHolder.name.setText(contactInfo.getName());

			LayoutParams lp = (LayoutParams) viewHolder.avatarImg
					.getLayoutParams();
			lp.height = lp.width = IMAGE_BOUNDS;
			viewHolder.avatarImg.setLayoutParams(lp);

			break;

		case SECTION:
			((TextView) convertView).setText(contactInfo.getName());
			break;

		case EMPTY_FAVORITE:
			String text = viewHolder.name.getText().toString();
			String replace = "plus";
			SpannableString spannableString = new SpannableString(text);
			spannableString.setSpan(new ImageSpan(context,
					R.drawable.ic_add_favorite), text.indexOf(replace),
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
		TextView name;
		ImageView addImg;
		Button addToFav;
		Button notNow;
	}

	@Override
	public void onClick(View v) {
		ContactInfo contactInfo = (ContactInfo) v.getTag();
		if (v.getId() == R.id.add_fav) {
			Pair<ContactInfo, FavoriteType> favoriteAdded = new Pair<ContactInfo, FavoriteType>(
					contactInfo, FavoriteType.FAVORITE);
			HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_TOGGLED,
					favoriteAdded);
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
