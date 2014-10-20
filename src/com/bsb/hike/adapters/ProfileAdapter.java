package com.bsb.hike.adapters;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.ImageViewerInfo;
import com.bsb.hike.models.ProfileItem;
import com.bsb.hike.models.ProfileItem.ProfileGroupItem;
import com.bsb.hike.models.ProfileItem.ProfileStatusItem;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.smartImageLoader.ProfilePicImageLoader;
import com.bsb.hike.smartImageLoader.TimelineImageLoader;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

public class ProfileAdapter extends ArrayAdapter<ProfileItem>
{

	public static final String PROFILE_PIC_SUFFIX = "pp";

	private static enum ViewType
	{
		HEADER, STATUS, PROFILE_PIC_UPDATE, GROUP_PARTICIPANT, EMPTY_STATUS, REQUEST
	}

	private Context context;

	private ProfileActivity profileActivity;

	private GroupConversation groupConversation;

	private ContactInfo mContactInfo;

	private Bitmap profilePreview;

	private boolean groupProfile;

	private boolean myProfile;

	private boolean isContactBlocked;

	private boolean lastSeenPref;

	private IconLoader iconLoader;

	private TimelineImageLoader bigPicImageLoader;
	
	private ProfilePicImageLoader profileImageLoader;

	private int mIconImageSize;

	public ProfileAdapter(ProfileActivity profileActivity, List<ProfileItem> itemList, GroupConversation groupConversation, ContactInfo contactInfo, boolean myProfile)
	{
		this(profileActivity, itemList, groupConversation, contactInfo, myProfile, false);
	}

	public ProfileAdapter(ProfileActivity profileActivity, List<ProfileItem> itemList, GroupConversation groupConversation, ContactInfo contactInfo, boolean myProfile,
			boolean isContactBlocked)
	{
		super(profileActivity, -1, itemList);
		this.context = profileActivity;
		this.profileActivity = profileActivity;
		this.groupProfile = groupConversation != null;
		this.mContactInfo = contactInfo;
		this.groupConversation = groupConversation;
		this.myProfile = myProfile;
		this.isContactBlocked = isContactBlocked;
		this.lastSeenPref = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.LAST_SEEN_PREF, true);
		mIconImageSize = context.getResources().getDimensionPixelSize(R.dimen.icon_picture_size);
		int mBigImageSize = context.getResources().getDimensionPixelSize(R.dimen.timeine_big_picture_size);

		this.bigPicImageLoader = new TimelineImageLoader(context, mBigImageSize);

		this.profileImageLoader = new ProfilePicImageLoader(context, mBigImageSize);
		profileImageLoader.setDefaultAvatarIfNoCustomIcon(true);
		profileImageLoader.setHiResDefaultAvatar(true);

		this.iconLoader = new IconLoader(context, mIconImageSize);
		iconLoader.setDefaultAvatarIfNoCustomIcon(true);
	}
	

	@Override
	public int getItemViewType(int position)
	{
		ViewType viewType;
		ProfileItem profileItem = getItem(position);
		int itemId = profileItem.getItemId();
		if (ProfileItem.HEADER_ID == itemId)
		{
			viewType = ViewType.HEADER;
		}
		else if (ProfileItem.EMPTY_ID == itemId)
		{
			viewType = ViewType.EMPTY_STATUS;
		}
		else if (ProfileItem.REQUEST_ID == itemId)
		{
			viewType = ViewType.REQUEST;
		}
		else
		{
			if (groupProfile)
			{
				viewType = ViewType.GROUP_PARTICIPANT;
			}
			else
			{
				StatusMessage statusMessage = ((ProfileStatusItem) profileItem).getStatusMessage();
				if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC)
				{
					viewType = ViewType.PROFILE_PIC_UPDATE;
				}
				else
				{
					viewType = ViewType.STATUS;
				}
			}
		}
		return viewType.ordinal();
	}

	@Override
	public int getViewTypeCount()
	{
		return ViewType.values().length;
	}

	@Override
	public boolean areAllItemsEnabled()
	{
		return false;
	}

	@Override
	public boolean isEnabled(int position)
	{
		ViewType viewType = ViewType.values()[getItemViewType(position)];
		if (viewType == ViewType.HEADER)
		{
			return false;
		}
		return true;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		ViewType viewType = ViewType.values()[getItemViewType(position)];

		ProfileItem profileItem = getItem(position);

		ViewHolder viewHolder = null;
		View v = convertView;

		if (v == null)
		{
			viewHolder = new ViewHolder();

			switch (viewType)
			{
			case HEADER:
				v = inflater.inflate(R.layout.profile_header, null);

				viewHolder.text = (TextView) v.findViewById(R.id.name);
				viewHolder.subText = (TextView) v.findViewById(R.id.info);

				viewHolder.image = (ImageView) v.findViewById(R.id.profile);
				viewHolder.icon = (ImageView) v.findViewById(R.id.change_profile);
				break;

			case GROUP_PARTICIPANT:
				v = new LinearLayout(context);
				break;

			case STATUS:
				v = inflater.inflate(R.layout.profile_timeline_item, null);

				viewHolder.icon = (ImageView) v.findViewById(R.id.avatar);
				viewHolder.iconFrame = (ImageView) v.findViewById(R.id.avatar_frame);

				viewHolder.text = (TextView) v.findViewById(R.id.name);
				viewHolder.subText = (TextView) v.findViewById(R.id.main_info);
				viewHolder.timeStamp = (TextView) v.findViewById(R.id.timestamp);
				viewHolder.parent = v.findViewById(R.id.main_content);
				break;

			case PROFILE_PIC_UPDATE:
				v = inflater.inflate(R.layout.profile_pic_timeline_item, null);

				viewHolder.icon = (ImageView) v.findViewById(R.id.avatar);

				viewHolder.text = (TextView) v.findViewById(R.id.name);
				viewHolder.subText = (TextView) v.findViewById(R.id.main_info);
				viewHolder.image = (ImageView) v.findViewById(R.id.profile_pic);
				viewHolder.timeStamp = (TextView) v.findViewById(R.id.timestamp);
				viewHolder.infoContainer = v.findViewById(R.id.info_container);
				viewHolder.parent = v.findViewById(R.id.main_content);
				break;

			case EMPTY_STATUS:
				v = inflater.inflate(R.layout.profile_timeline_negative_item, null);

				viewHolder.text = (TextView) v.findViewById(R.id.info);
				viewHolder.icon = (ImageView) v.findViewById(R.id.icon);
				viewHolder.btn1 = (Button) v.findViewById(R.id.btn);
				viewHolder.btn2 = (Button) v.findViewById(R.id.add_sms_friend_btn);
				break;

			case REQUEST:
				v = inflater.inflate(R.layout.profile_friend_request_item, null);

				viewHolder.icon = (ImageView) v.findViewById(R.id.avatar);

				viewHolder.text = (TextView) v.findViewById(R.id.name);
				viewHolder.subText = (TextView) v.findViewById(R.id.info);
				viewHolder.extraInfo = (TextView) v.findViewById(R.id.extra_info);

				viewHolder.infoContainer = v.findViewById(R.id.btn_container);
				viewHolder.imageBtn1 = (ImageButton) v.findViewById(R.id.yes_btn);
				viewHolder.imageBtn2 = (ImageButton) v.findViewById(R.id.no_btn);

				viewHolder.btn1 = (Button) v.findViewById(R.id.text_btn);
				viewHolder.parent = v.findViewById(R.id.main_content);
			}

			v.setTag(viewHolder);
		}
		else
		{
			viewHolder = (ViewHolder) v.getTag();
		}

		switch (viewType)
		{
		case HEADER:
			String msisdn;
			String name;

			if (groupProfile)
			{
				msisdn = groupConversation.getMsisdn();
				name = groupConversation.getLabel();
			}
			else
			{
				msisdn = mContactInfo.getMsisdn();
				name = TextUtils.isEmpty(mContactInfo.getName()) ? mContactInfo.getMsisdn() : mContactInfo.getName();
			}

			viewHolder.text.setText(name);

			String mappedId = msisdn + PROFILE_PIC_SUFFIX;
			ImageViewerInfo imageViewerInfo = new ImageViewerInfo(mappedId, null, false, !HikeUserDatabase.getInstance().hasIcon(msisdn));
			viewHolder.image.setTag(imageViewerInfo);
			if (profilePreview == null)
			{
				profileImageLoader.loadImage(mappedId, viewHolder.image, isListFlinging);
			}
			else
			{
				viewHolder.image.setImageBitmap(profilePreview);
			}
			viewHolder.icon.setVisibility(View.VISIBLE);
			if (myProfile || groupProfile)
			{
				viewHolder.icon.setImageResource(R.drawable.ic_change_profile_pic);
			}
			else
			{
				viewHolder.icon.setImageResource(R.drawable.ic_new_conversation);
			}

			if (mContactInfo != null)
			{
				if (mContactInfo.getMsisdn().equals(mContactInfo.getId()))
				{
					viewHolder.subText.setVisibility(View.VISIBLE);
					viewHolder.subText.setText(R.string.tap_to_save);
				}
				else
				{
					viewHolder.subText.setVisibility(View.VISIBLE);
					viewHolder.subText.setText(mContactInfo.getMsisdn());
					if (!TextUtils.isEmpty(mContactInfo.getMsisdnType()))
					{
						viewHolder.subText.append(" (" + mContactInfo.getMsisdnType() + ")");
					}
				}
			}
			else if (groupProfile)
			{
				/*
				 * Adding one to count self.
				 */
				viewHolder.subText.setText(context.getString(R.string.num_people, (groupConversation.getGroupMemberAliveCount() + 1)));
			}

			break;

		case GROUP_PARTICIPANT:
			LinearLayout parentView = (LinearLayout) v;
			parentView.removeAllViews();

			GroupParticipant[] groupParticipants = ((ProfileGroupItem) profileItem).getGroupParticipants();

			for (int i = 0; i < groupParticipants.length; i++)
			{
				GroupParticipant groupParticipant = groupParticipants[i];

				View groupParticipantParentView = inflater.inflate(R.layout.group_profile_item, parentView, false);

				TextView nameTextView = (TextView) groupParticipantParentView.findViewById(R.id.name);
				TextView mainInfo = (TextView) groupParticipantParentView.findViewById(R.id.main_info);

				if (groupParticipant == null)
				{
					/*
					 * if the second element is null, we just make it invisible.
					 */
					if (i == 1)
					{
						groupParticipantParentView.setVisibility(View.INVISIBLE);
					}

					View avatarContainer = groupParticipantParentView.findViewById(R.id.avatar_container);

					View addParticipantView = groupParticipantParentView.findViewById(R.id.add_participant);

					avatarContainer.setVisibility(View.GONE);
					addParticipantView.setVisibility(View.VISIBLE);
					mainInfo.setVisibility(View.GONE);

					nameTextView.setText(R.string.add_people);
				}
				else
				{
					ImageView avatar = (ImageView) groupParticipantParentView.findViewById(R.id.avatar);
					ImageView avatarFrame = (ImageView) groupParticipantParentView.findViewById(R.id.avatar_frame);
					View ownerIndicator = groupParticipantParentView.findViewById(R.id.owner_indicator);

					ContactInfo contactInfo = groupParticipant.getContactInfo();

					if (contactInfo.getMsisdn().equals(groupConversation.getGroupOwner()))
					{
						ownerIndicator.setVisibility(View.VISIBLE);
					}
					else
					{
						ownerIndicator.setVisibility(View.GONE);
					}

					int offline = contactInfo.getOffline();

					String lastSeenString = null;
					boolean showingLastSeen = false;
					if (lastSeenPref && contactInfo.getFavoriteType() == FavoriteType.FRIEND && !contactInfo.getMsisdn().equals(contactInfo.getId()))
					{
						lastSeenString = Utils.getLastSeenTimeAsString(context, contactInfo.getLastSeenTime(), offline, true);
						showingLastSeen = !TextUtils.isEmpty(lastSeenString);
					}

					nameTextView.setText(contactInfo.getFirstNameAndSurname());
					if (!showingLastSeen)
					{
						mainInfo.setText(contactInfo.isOnhike() ? R.string.on_hike : R.string.on_sms);
					}
					else
					{
						mainInfo.setText(lastSeenString);
					}

					if (showingLastSeen && offline == 0)
					{
						mainInfo.setTextColor(context.getResources().getColor(R.color.unread_message));
						avatarFrame.setImageResource(R.drawable.frame_avatar_highlight);
					}
					else
					{
						mainInfo.setTextColor(context.getResources().getColor(R.color.participant_last_seen));
						avatarFrame.setImageDrawable(null);
					}
					setAvatar(contactInfo.getMsisdn(), avatar);

					groupParticipantParentView.setOnLongClickListener(profileActivity);
				}

				LayoutParams layoutParams = (LayoutParams) groupParticipantParentView.getLayoutParams();
				int margin = context.getResources().getDimensionPixelSize(R.dimen.updates_margin);

				layoutParams.leftMargin = margin;
				layoutParams.topMargin = margin;
				if (i == groupParticipants.length - 1)
				{
					layoutParams.rightMargin = margin;
				}

				if (position == getCount() - 1)
				{
					layoutParams.bottomMargin = margin;
				}
				groupParticipantParentView.setTag(groupParticipant);

				groupParticipantParentView.setOnClickListener(profileActivity);

				parentView.addView(groupParticipantParentView);
			}
			break;

		case STATUS:
			StatusMessage statusMessage = ((ProfileStatusItem) profileItem).getStatusMessage();
			viewHolder.text.setText(myProfile ? context.getString(R.string.me) : statusMessage.getNotNullName());

			if(statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST_ACCEPTED || statusMessage.getStatusMessageType() == StatusMessageType.USER_ACCEPTED_FRIEND_REQUEST)
			{
				boolean friendRequestAccepted = statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST_ACCEPTED;

				viewHolder.subText.setText(context.getString(friendRequestAccepted ? R.string.accepted_your_favorite_request_details
						: R.string.you_accepted_favorite_request_details, Utils.getFirstName(statusMessage.getNotNullName())));
			}
			else
			{
				SmileyParser smileyParser = SmileyParser.getInstance();
				viewHolder.subText.setText(smileyParser.addSmileySpans(statusMessage.getText(), true));
			}

			Linkify.addLinks(viewHolder.text, Linkify.ALL);
			viewHolder.text.setMovementMethod(null);

			viewHolder.timeStamp.setText(statusMessage.getTimestampFormatted(true, context));

			if (statusMessage.hasMood())
			{
				viewHolder.icon.setImageResource(EmoticonConstants.moodMapping.get(statusMessage.getMoodId()));
				viewHolder.iconFrame.setVisibility(View.GONE);
			}
			else
			{
				setAvatar(statusMessage.getMsisdn(), viewHolder.icon);
				viewHolder.iconFrame.setVisibility(View.VISIBLE);
			}
			break;

		case PROFILE_PIC_UPDATE:
			StatusMessage profilePicStatusUpdate = ((ProfileStatusItem) profileItem).getStatusMessage();
			viewHolder.text.setText(myProfile ? context.getString(R.string.me) : profilePicStatusUpdate.getNotNullName());

			viewHolder.subText.setText(R.string.status_profile_pic_notification);
			setAvatar(profilePicStatusUpdate.getMsisdn(), viewHolder.icon);

			ImageViewerInfo imageViewerInfo2 = new ImageViewerInfo(profilePicStatusUpdate.getMappedId(), null, true);

			viewHolder.image.setTag(imageViewerInfo2);

			bigPicImageLoader.loadImage(profilePicStatusUpdate.getMappedId(), viewHolder.image, isListFlinging);

			viewHolder.timeStamp.setText(profilePicStatusUpdate.getTimestampFormatted(true, context));

			viewHolder.infoContainer.setTag(profilePicStatusUpdate);
			viewHolder.infoContainer.setOnLongClickListener(profileActivity);
			break;

		case EMPTY_STATUS:
			String contactName = mContactInfo.getFirstName();

			if (!isContactBlocked)
			{
				if (mContactInfo.isOnhike())
				{
					viewHolder.btn2.setVisibility(View.GONE);
					viewHolder.icon.setImageResource(R.drawable.ic_not_friend);
					if (mContactInfo.getFavoriteType() == FavoriteType.REQUEST_SENT)
					{
						viewHolder.text.setText(context.getString(R.string.waiting_for_accept, contactName));
						viewHolder.btn1.setVisibility(View.GONE);
					}
					else
					{
						viewHolder.text.setText(context.getString(R.string.add_as_friend_info, contactName));
						viewHolder.btn1.setText(R.string.add_as_friend);
						viewHolder.btn1.setVisibility(View.VISIBLE);
					}
				}
				else
				{
					viewHolder.icon.setImageResource(R.drawable.ic_not_on_hike);
					viewHolder.text.setText(context.getString(R.string.not_on_hike, contactName));
					viewHolder.btn1.setText(R.string.invite_to_hike);
					viewHolder.btn2.setVisibility(mContactInfo.getFavoriteType() == FavoriteType.NOT_FRIEND ? View.VISIBLE : View.GONE);
				}
			}
			else
			{
				viewHolder.icon.setImageResource(R.drawable.ic_block_profile);
				viewHolder.text.setText(context.getString(R.string.user_blocked, contactName));
				viewHolder.btn1.setText(R.string.unblock_title);
				viewHolder.btn2.setVisibility(View.GONE);
			}

			break;

		case REQUEST:
			String contactFirstName = mContactInfo.getFirstName();

			setAvatar(mContactInfo.getMsisdn(), viewHolder.icon);

			viewHolder.text.setText(contactFirstName);

			viewHolder.infoContainer.setVisibility(View.GONE);
			if (mContactInfo.isOnhike())
			{
				switch (mContactInfo.getFavoriteType())
				{
				case NOT_FRIEND:
				case REQUEST_SENT_REJECTED:
				case REQUEST_RECEIVED_REJECTED:
					viewHolder.subText.setText(mContactInfo.getMsisdn());

					viewHolder.imageBtn1.setVisibility(View.GONE);
					viewHolder.imageBtn2.setVisibility(View.GONE);

					viewHolder.btn1.setVisibility(View.VISIBLE);
					viewHolder.btn1.setText(R.string.add);
					viewHolder.btn1.setBackgroundResource(R.drawable.bg_blue_btn_selector);

					viewHolder.extraInfo.setVisibility(View.VISIBLE);
					viewHolder.extraInfo.setText(context.getString(R.string.add_as_favorites_profile, contactFirstName));
					break;
				case REQUEST_RECEIVED:
					viewHolder.infoContainer.setVisibility(View.VISIBLE);

					viewHolder.subText.setText(context.getString(R.string.sent_you_favorite_request_detailed, mContactInfo.getFirstName()));

					viewHolder.imageBtn1.setVisibility(View.VISIBLE);
					viewHolder.imageBtn2.setVisibility(View.VISIBLE);

					viewHolder.btn1.setVisibility(View.GONE);

					viewHolder.extraInfo.setVisibility(View.GONE);
					break;

				case REQUEST_SENT:
					viewHolder.subText.setText(R.string.favorite_request_pending);

					viewHolder.imageBtn1.setVisibility(View.GONE);
					viewHolder.imageBtn2.setVisibility(View.GONE);
					viewHolder.btn1.setVisibility(View.GONE);
					viewHolder.extraInfo.setVisibility(View.GONE);
					break;
				}
			}
			else
			{
				if (mContactInfo.getMsisdn().equals(mContactInfo.getId()))
				{
					viewHolder.subText.setText(R.string.on_sms);
				}
				else
				{
					viewHolder.subText.setText(mContactInfo.getMsisdn());
				}

				viewHolder.imageBtn1.setVisibility(View.GONE);
				viewHolder.imageBtn2.setVisibility(View.GONE);

				viewHolder.btn1.setVisibility(View.VISIBLE);
				viewHolder.btn1.setText(R.string.invite_1);
				viewHolder.btn1.setBackgroundResource(R.drawable.bg_green_btn_selector);

				viewHolder.extraInfo.setVisibility(View.VISIBLE);
				viewHolder.extraInfo.setText(R.string.invite_to_hike);
			}
			break;
		}

		if (viewHolder.parent != null)
		{
			int bottomPadding;

			if (position == getCount() - 1)
			{
				bottomPadding = context.getResources().getDimensionPixelSize(R.dimen.updates_margin);
			}
			else
			{
				bottomPadding = 0;
			}

			viewHolder.parent.setPadding(0, 0, 0, bottomPadding);
		}

		return v;
	}

	private void setAvatar(String msisdn, ImageView avatarView)
	{
		iconLoader.loadImage(msisdn, true, avatarView, true);
	}

	private class ViewHolder
	{
		TextView text;

		TextView subText;

		TextView extraInfo;

		ImageView image;

		ImageView icon;

		ImageView iconFrame;

		Button btn1;

		Button btn2;

		ImageButton imageBtn1;

		ImageButton imageBtn2;

		TextView timeStamp;

		View infoContainer;

		View parent;
	}

	public void setProfilePreview(Bitmap preview)
	{
		this.profilePreview = preview;
		notifyDataSetChanged();
	}

	public void updateGroupConversation(GroupConversation groupConversation)
	{
		this.groupConversation = groupConversation;
		notifyDataSetChanged();
	}

	public void updateContactInfo(ContactInfo contactInfo)
	{
		this.mContactInfo = contactInfo;
		notifyDataSetChanged();
	}

	public void setIsContactBlocked(boolean b)
	{
		isContactBlocked = b;
	}

	public boolean isContactBlocked()
	{
		return isContactBlocked;
	}

	public TimelineImageLoader getTimelineImageLoader()
	{
		return bigPicImageLoader;
	}

	public IconLoader getIconImageLoader()
	{
		return iconLoader;
	}

	private boolean isListFlinging;

	public void setIsListFlinging(boolean b)
	{
		boolean notify = b != isListFlinging;

		isListFlinging = b;
		bigPicImageLoader.setPauseWork(isListFlinging);
		iconLoader.setPauseWork(isListFlinging);

		if (notify && !isListFlinging)
		{
			notifyDataSetChanged();
		}
	}
}