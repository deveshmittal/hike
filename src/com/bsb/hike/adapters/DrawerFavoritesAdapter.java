package com.bsb.hike.adapters;

import java.util.List;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.utils.Utils;

public class DrawerFavoritesAdapter extends BaseAdapter 
{
	private List<ContactInfo> contactList;
	private LayoutInflater layoutInflater;
	private Context context;

	private int favoriteCount;

	public static final String FAVORITES_SECTION_ID = "-911";
	public static final String RECENTS_SECTION_ID = "-912";
	public static final String EMPTY_FAVORITES_ID = "-913";

	public static final int IMAGE_BOUNDS = (int) (30 * Utils.densityMultiplier);

	public static enum FavoriteAdapterViewType
	{
		SECTION,
		FAVORITE,
		EMPTY_FAVORITE,
		RECENT
	}

	public DrawerFavoritesAdapter(List<ContactInfo> contactList, int favoriteCount, Context context)
	{
		this.contactList = contactList;
		this.favoriteCount = favoriteCount;
		this.context = context;
		this.layoutInflater = LayoutInflater.from(context);
	}

	public void addFavoriteItem(ContactInfo contactInfo)
	{
		/*
		 *  We first check if we are showing the empty favorites item.
		 *  If we are, we remove it before adding the new favorite.
		 */
		if(EMPTY_FAVORITES_ID.equals(getItem(1).getId()))
		{
			contactList.remove(1);
		}
		// Remove from the recents list.
		contactList.remove(contactInfo);
		contactList.add(1, contactInfo);
		favoriteCount++;

		notifyDataSetChanged();
	}

	@Override
	public boolean areAllItemsEnabled() 
	{
		return false;
	}

	@Override
	public int getItemViewType(int position) 
	{
		ContactInfo contactInfo = getItem(position);
		if(FAVORITES_SECTION_ID.equals(contactInfo.getId()) || RECENTS_SECTION_ID.equals(contactInfo.getId()))
		{
			return FavoriteAdapterViewType.SECTION.ordinal();
		}
		else if(EMPTY_FAVORITES_ID.equals(contactInfo.getId()))
		{
			return FavoriteAdapterViewType.EMPTY_FAVORITE.ordinal();
		}
		/*
		 *  Recent items will be the last ones in the list so their position will start after favorites. We add the
		 *  two to include the 2 sections as well. If we have no favorites, we still add a 1 since we are showing an
		 *  extra item even if there are no favorites.
		 */
		else if(position > Math.max(1, favoriteCount) + 1)
		{
			return FavoriteAdapterViewType.RECENT.ordinal();
		}
		return FavoriteAdapterViewType.FAVORITE.ordinal();
	}

	@Override
	public int getViewTypeCount() 
	{
		return FavoriteAdapterViewType.values().length;
	}

	@Override
	public boolean isEnabled(int position) 
	{
		if(getItemViewType(position) == FavoriteAdapterViewType.SECTION.ordinal())
		{
			return false;
		}
		return true;
	}

	@Override
	public int getCount() 
	{
		return contactList.size();
	}

	@Override
	public ContactInfo getItem(int position) 
	{
		return contactList.get(position);
	}

	@Override
	public long getItemId(int position) 
	{
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
		ContactInfo contactInfo = getItem(position);
		FavoriteAdapterViewType viewType = FavoriteAdapterViewType.values()[getItemViewType(position)];

		ViewHolder viewHolder;

		if(convertView == null)
		{
			viewHolder = new ViewHolder();

			switch (viewType) 
			{
			case RECENT:
			case FAVORITE:
				convertView = layoutInflater.inflate(R.layout.drawer_item, null);

				viewHolder.addImg = (ImageView) convertView.findViewById(R.id.add_fav);
				viewHolder.avatarImg = (ImageView) convertView.findViewById(R.id.item_icon);
				viewHolder.name = (TextView) convertView.findViewById(R.id.item_name);
				break;

			case SECTION:
				convertView = (TextView) layoutInflater.inflate(R.layout.drawer_section, null);
				break;

			case EMPTY_FAVORITE:
				convertView = layoutInflater.inflate(R.layout.empty_favorites, null);

				viewHolder.name = (TextView) convertView.findViewById(R.id.item_txt);
			}
			convertView.setTag(viewHolder);
		}
		else
		{
			viewHolder = (ViewHolder) convertView.getTag();
		}
		
		switch (viewType) 
		{
		case RECENT:
			viewHolder.addImg.setVisibility(View.VISIBLE);
		case FAVORITE:
			viewHolder.avatarImg.setImageDrawable(IconCacheManager.getInstance().getIconForMSISDN(contactInfo.getMsisdn()));
			viewHolder.name.setText(contactInfo.getName());

			LayoutParams lp = (LayoutParams) viewHolder.avatarImg.getLayoutParams();
			lp.height = lp.width = IMAGE_BOUNDS;
			viewHolder.avatarImg.setLayoutParams(lp);

			break;

		case SECTION:
			((TextView)convertView).setText(contactInfo.getName());
			break;

		case EMPTY_FAVORITE:
			String text = viewHolder.name.getText().toString();
			String replace = "plus";
			SpannableString spannableString = new SpannableString(text);
			spannableString.setSpan(
					new ImageSpan(context, R.drawable.ic_add_favorite),
					text.indexOf(replace),
					text.indexOf(replace) + replace.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
									);
			viewHolder.name.setText(spannableString);
		}
		return convertView;
	}

	private class ViewHolder
	{
		ImageView avatarImg;
		TextView name;
		ImageView addImg;
	}
}
