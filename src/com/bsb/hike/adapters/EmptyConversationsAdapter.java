package com.bsb.hike.adapters;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.EmptyConversationItem;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.ui.ComposeChatActivity;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.utils.Utils;

public class EmptyConversationsAdapter extends ArrayAdapter<EmptyConversationItem>
{

	private Context context;

	private int mIconImageSize;

	private IconLoader iconLoader;

	private LayoutInflater inflater;

	private class ViewHolder
	{
		View parent;

		ViewGroup contactsContainer;

		TextView name;

		TextView mainInfo;

		TextView seeAll;

	}

	public EmptyConversationsAdapter(Context context, int textViewResourceId, List<EmptyConversationItem> objects)
	{
		super(context, textViewResourceId, objects);

		this.context = context;
		mIconImageSize = context.getResources().getDimensionPixelSize(R.dimen.icon_picture_size);
		iconLoader = new IconLoader(context, mIconImageSize);
		iconLoader.setDefaultAvatarIfNoCustomIcon(true);
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View v, ViewGroup parent)
	{
		EmptyConversationItem item = getItem(position);

		ViewHolder viewHolder;
		if (v == null)
		{
			viewHolder = new ViewHolder();
			switch (item.getType())
			{
			case EmptyConversationItem.HIKE_CONTACTS:
			case EmptyConversationItem.SMS_CONTACTS:

				v = inflater.inflate(R.layout.ftue_updates_item, parent, false);

				viewHolder.name = (TextView) v.findViewById(R.id.name);
				viewHolder.mainInfo = (TextView) v.findViewById(R.id.main_info);

				viewHolder.contactsContainer = (ViewGroup) v.findViewById(R.id.contacts_container);
				viewHolder.parent = v.findViewById(R.id.main_content);

				viewHolder.seeAll = (TextView) v.findViewById(R.id.see_all);
				
				viewHolder.contactsContainer.removeAllViews();
				int limit = HikeConstants.FTUE_LIMIT;
				for (int i=0; i< item.getContactList().size(); i++)
				{
					View parentView = inflater.inflate(R.layout.ftue_recommended_list_item, parent, false);
					viewHolder.contactsContainer.addView(parentView);
					
					if (--limit == 0)
					{
						break;
					}
				}

				break;

			default:
				break;
			}
			v.setTag(viewHolder);
		}
		else
		{
			viewHolder = (ViewHolder) v.getTag();
		}

		if (item.getType() == EmptyConversationItem.HIKE_CONTACTS || item.getType() == EmptyConversationItem.SMS_CONTACTS)
		{
			viewHolder.name.setText(item.getHeader());
			viewHolder.mainInfo.setVisibility(View.GONE);

			int limit = HikeConstants.FTUE_LIMIT;
			View parentView = null;
			for (int i=0; i< item.getContactList().size(); i++)
			{
				ContactInfo contactInfo = item.getContactList().get(i);
				parentView = viewHolder.contactsContainer.getChildAt(i);

				ImageView avatar = (ImageView) parentView.findViewById(R.id.avatar);
				TextView name = (TextView) parentView.findViewById(R.id.contact);
				TextView status = (TextView) parentView.findViewById(R.id.info);

				iconLoader.loadImage(contactInfo.getMsisdn(), true, avatar, true);

				name.setText(contactInfo.getName());
				status.setText(contactInfo.getMsisdn());

				parentView.setTag(contactInfo);
				parentView.setOnClickListener(ftueListItemClickListener);

				if (--limit == 0)
				{
					break;
				}
			}
			if (parentView != null)
			{
				parentView.findViewById(R.id.divider).setVisibility(View.GONE);
			}

			switch (item.getType())
			{
			case EmptyConversationItem.HIKE_CONTACTS:
				if (HomeActivity.ftueContactsData.getTotalHikeContactsCount() > HikeConstants.FTUE_LIMIT)
				{
					setUpSeeAllButton(viewHolder.seeAll);
				}
				else
				{
					viewHolder.seeAll.setVisibility(View.GONE);
				}
				break;
			case EmptyConversationItem.SMS_CONTACTS:
				if (HomeActivity.ftueContactsData.getTotalSmsContactsCount() > HomeActivity.ftueContactsData.getSmsContacts().size())
				{
					setUpSeeAllButton(viewHolder.seeAll);
				}
				else
				{
					viewHolder.seeAll.setVisibility(View.GONE);
				}
				break;

			}
		}
		return v;
	}

	private void setUpSeeAllButton(TextView seeAllView)
	{
		seeAllView.setVisibility(View.VISIBLE);
		seeAllView.setText(R.string.see_all_upper_caps);
		seeAllView.setOnClickListener(seeAllBtnClickListener);
	}

	@Override
	public boolean isEnabled(int position)
	{
		EmptyConversationItem item = (EmptyConversationItem) getItem(position);
		switch (item.getType())
		{
		case EmptyConversationItem.HIKE_CONTACTS:
		case EmptyConversationItem.SMS_CONTACTS:
			return false;

		default:
			break;
		}
		return super.isEnabled(position);
	}

	private OnClickListener ftueListItemClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			ContactInfo contactInfo = (ContactInfo) v.getTag();

			Utils.startChatThread(context, contactInfo);

		}
	};

	private OnClickListener seeAllBtnClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(context, ComposeChatActivity.class);
			context.startActivity(intent);
		}
	};

}
