package com.bsb.hike.adapters;

import java.util.List;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView.FindListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.models.ImageViewerInfo;
import com.bsb.hike.models.ProfileItem;
import com.bsb.hike.models.ProfileItem.ProfileContactItem;
import com.bsb.hike.models.ProfileItem.ProfileGroupItem;
import com.bsb.hike.models.ProfileItem.ProfileSharedContent;
import com.bsb.hike.models.ProfileItem.ProfileSharedMedia;
import com.bsb.hike.models.ProfileItem.ProfileStatusItem;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.smartImageLoader.ProfilePicImageLoader;
import com.bsb.hike.smartImageLoader.SharedFileImageLoader;
import com.bsb.hike.smartImageLoader.TimelineImageLoader;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomFontTextView;

public class ProfileAdapter extends ArrayAdapter<ProfileItem>
{

	public static final String PROFILE_PIC_SUFFIX = "pp";
	
	public static final String PROFILE_ROUND_SUFFIX = "round";
	
	public static final String OPEN_GALLERY = "OpenGallery";
	
	public static final String IMAGE_TAG = "image";
	
	private static enum ViewType
	{
		HEADER, HEADER_PROFILE, HEADER_GROUP, SHARED_MEDIA, SHARED_CONTENT, STATUS, PROFILE_PIC_UPDATE, GROUP_PARTICIPANT, EMPTY_STATUS, REQUEST, MEMBERS, ADD_MEMBERS, PHONE_NUMBER
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
	
	private SharedFileImageLoader thumbnailLoader;

	private int mIconImageSize;

	private static final int SHOW_CONTACTS_STATUS = 0;
	
	private static final int NOT_A_FRIEND = 1;

	private static final int UNKNOWN_ON_HIKE = 2;

	private static final int REQUEST_RECEIVED = 3;

	private static final int UNKNOWN_NOT_ON_HIKE = 4;

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
		int thumbNailSize = context.getResources().getDimensionPixelSize(R.dimen.profile_shared_media_item_size);
		this.bigPicImageLoader = new TimelineImageLoader(context, mBigImageSize);
		thumbnailLoader = new SharedFileImageLoader(context, thumbNailSize);
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
		else if (ProfileItem.HEADER_ID_GROUP == itemId)
		{
			viewType = ViewType.HEADER_GROUP;
		}
		else if (ProfileItem.SHARED_MEDIA == itemId)
		{
			viewType = ViewType.SHARED_MEDIA;
		}
		else if (ProfileItem.SHARED_CONTENT == itemId)
		{
			viewType = ViewType.SHARED_CONTENT;
		}
		else if (ProfileItem.MEMBERS == itemId)
		{
			viewType = ViewType.MEMBERS;
		}
		else if (ProfileItem.GROUP_MEMBER == itemId)
		{
			viewType = ViewType.GROUP_PARTICIPANT;
		}
		else if (ProfileItem.ADD_MEMBERS == itemId)
		{
			viewType = ViewType.ADD_MEMBERS;
		}
		else if (ProfileItem.EMPTY_ID == itemId)
		{
			viewType = ViewType.EMPTY_STATUS;
		}
		else if (ProfileItem.REQUEST_ID == itemId)
		{
			viewType = ViewType.REQUEST;
		}
		else if (ProfileItem.HEADER_ID_PROFILE == itemId)
		{
			viewType = ViewType.HEADER_PROFILE;
		}
		else if (ProfileItem.PHONE_NUMBER == itemId)
		{
			viewType = ViewType.PHONE_NUMBER;
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

			case HEADER_PROFILE:
				v = inflater.inflate(R.layout.profile_header_other, null);
				viewHolder.text = (TextView) v.findViewById(R.id.name);
				viewHolder.subText = (TextView) v.findViewById(R.id.subtext);
				viewHolder.image = (ImageView) v.findViewById(R.id.profile_image);
				viewHolder.parent = v.findViewById(R.id.profile_header);
				viewHolder.extraInfo = (TextView) v.findViewById(R.id.add_fav_tv);
				viewHolder.icon = (ImageView) v.findViewById(R.id.add_fav_star);
				break;

			case HEADER_GROUP:
				v = inflater.inflate(R.layout.profile_header_group, null);
				viewHolder.editName = (EditText) v.findViewById(R.id.name_edit);
				viewHolder.text = (TextView) v.findViewById(R.id.name);
				viewHolder.subText = (TextView) v.findViewById(R.id.subtext);
				viewHolder.image = (ImageView) v.findViewById(R.id.group_profile_image);
				viewHolder.iconFrame = (ImageView) v.findViewById(R.id.change_profile);
				break;

			case SHARED_MEDIA:
				v = inflater.inflate(R.layout.shared_media, null);
				viewHolder.text = (TextView) v.findViewById(R.id.name);
				viewHolder.subText = (TextView) v.findViewById(R.id.count);
				viewHolder.infoContainer = v.findViewById(R.id.shared_media_items);
				viewHolder.extraInfo = (TextView) v.findViewById(R.id.sm_emptystate);
				break;

			case SHARED_CONTENT:
				v = inflater.inflate(R.layout.shared_content, null);
				viewHolder.parent = v.findViewById(R.id.shared_content);
				viewHolder.text = (TextView) viewHolder.parent.findViewById(R.id.name);
				viewHolder.subText = (TextView) viewHolder.parent.findViewById(R.id.count);
				viewHolder.extraInfo = (TextView) v.findViewById(R.id.count_pin);
				viewHolder.infoContainer = v.findViewById(R.id.shared_content_layout);
				viewHolder.groupOrPins = (TextView) v.findViewById(R.id.shared_pins);
				viewHolder.sharedFilesCount = (TextView) v.findViewById(R.id.count_sf);
				viewHolder.icon = (ImageView) v.findViewById(R.id.shared_pin_icon);
				break;

			case MEMBERS:
				v = inflater.inflate(R.layout.friends_group_view, null);
				viewHolder.text = (TextView) v.findViewById(R.id.name);
				viewHolder.subText = (TextView) v.findViewById(R.id.count);
				break;

			case GROUP_PARTICIPANT:
				v = new LinearLayout(context);
				break;

			case ADD_MEMBERS:
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

			case PHONE_NUMBER:
				v = inflater.inflate(R.layout.shared_content, null);
				viewHolder.infoContainer = v.findViewById(R.id.shared_content);
				viewHolder.text = (TextView) viewHolder.infoContainer.findViewById(R.id.name);
				viewHolder.subText = (TextView) viewHolder.infoContainer.findViewById(R.id.count);
				viewHolder.parent = v.findViewById(R.id.phone_num);
				break;
			}

			v.setTag(viewHolder);
		}
		else
		{
			viewHolder = (ViewHolder) v.getTag();
		}

		switch (viewType)
		{
		case HEADER_PROFILE:
		case HEADER_GROUP:
			if (groupProfile)
				viewHolder.editName.setText(groupConversation.getLabel());

			String msisdn;
			String name;
			StatusMessage status;
			if (groupProfile)
			{
				msisdn = groupConversation.getMsisdn();
				name = groupConversation.getLabel();
				viewHolder.text.setText(name);
				viewHolder.subText.setText(context.getString(R.string.num_people, (groupConversation.getGroupMemberAliveCount() + 1)));
			}
			else
			{
				msisdn = mContactInfo.getMsisdn();
				name = TextUtils.isEmpty(mContactInfo.getName()) ? mContactInfo.getMsisdn() : mContactInfo.getName();
				viewHolder.text.setText(name);

			}

			String mappedId = msisdn + PROFILE_ROUND_SUFFIX;
			ImageViewerInfo imageViewerInfo = new ImageViewerInfo(mappedId, null, false, !ContactManager.getInstance().hasIcon(msisdn));
			viewHolder.image.setTag(imageViewerInfo);
			if (profilePreview == null)
			{
				profileImageLoader.loadImage(mappedId, viewHolder.image, isListFlinging);
			}
			else
			{
				viewHolder.image.setImageBitmap(profilePreview);
			}

			if (mContactInfo != null)
			{
				int contactType = ((ProfileItem.ProfileContactItem) profileItem).getContactType();
				switch (contactType)
				{
				
				case REQUEST_RECEIVED:
					LinearLayout req_layout = (LinearLayout) viewHolder.parent.findViewById(R.id.remove_fav);
					req_layout.setVisibility(View.VISIBLE);
					
				case SHOW_CONTACTS_STATUS:
					status = ((ProfileItem.ProfileContactItem) profileItem).getContactStatus();
					if(contactType == SHOW_CONTACTS_STATUS)   //The layout wasn't becoming invisible, if the request was accepted from above case.
					{	LinearLayout req_layout_fav = (LinearLayout) viewHolder.parent.findViewById(R.id.remove_fav);
						req_layout_fav.setVisibility(View.GONE);
					}
					if (status.getStatusMessageType() == StatusMessageType.JOINED_HIKE)
					{
						if (status.getTimeStamp() == 0)
							viewHolder.subText.setText(status.getText());
						else
							viewHolder.subText.setText(status.getText() + " " + status.getTimestampFormatted(true, context));
					}
					else
					{
						SmileyParser smileyParser = SmileyParser.getInstance();
						viewHolder.subText.setText(smileyParser.addSmileySpans(status.getText(), true));
					}
					break;

				case UNKNOWN_ON_HIKE:
					viewHolder.subText.setText(context.getResources().getString(R.string.on_hike));
					viewHolder.parent.findViewById(R.id.add_fav_view).setVisibility(View.GONE);
					break;
					
				case NOT_A_FRIEND:
					LinearLayout fav_layout = (LinearLayout) viewHolder.parent.findViewById(R.id.add_fav_view);
					fav_layout.setVisibility(View.VISIBLE);
					viewHolder.subText.setText(context.getResources().getString(R.string.on_hike));
					viewHolder.extraInfo.setTextColor(context.getResources().getColor(R.color.add_fav));
					viewHolder.extraInfo.setText(context.getResources().getString(R.string.add_fav));
					viewHolder.icon.setImageResource(R.drawable.ic_add_friend);
					break;
					
				case UNKNOWN_NOT_ON_HIKE:
					LinearLayout invite_layout = (LinearLayout) viewHolder.parent.findViewById(R.id.add_fav_view);
					invite_layout.setVisibility(View.VISIBLE);
					viewHolder.subText.setText(context.getResources().getString(R.string.on_sms));
					viewHolder.extraInfo.setTextColor(context.getResources().getColor(R.color.blue_hike));
					viewHolder.extraInfo.setText(context.getResources().getString(R.string.ftue_add_prompt_invite_title));
					viewHolder.icon.setImageResource(R.drawable.ic_invite_to_hike);
					break;
				}
			}
			break;
		case HEADER:
			
			String contmsisdn = mContactInfo.getMsisdn();
			String contname = TextUtils.isEmpty(mContactInfo.getName()) ? mContactInfo.getMsisdn() : mContactInfo.getName();
			viewHolder.text.setText(contname);
			String mapedId = contmsisdn + PROFILE_PIC_SUFFIX;
			ImageViewerInfo imageViewerInf = new ImageViewerInfo(mapedId, null, false, !ContactManager.getInstance().hasIcon(contmsisdn));
			viewHolder.image.setTag(imageViewerInf);
			if (profilePreview == null)
			{
				profileImageLoader.loadImage(mapedId, viewHolder.image, isListFlinging);
			}
			else
			{
				viewHolder.image.setImageBitmap(profilePreview);
			}
			viewHolder.icon.setVisibility(View.VISIBLE);
			if (myProfile)
			{
				viewHolder.icon.setImageResource(R.drawable.ic_change_profile_pic);
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
			break;
		case SHARED_MEDIA:
			viewHolder.text.setText(context.getString(R.string.shared_med));
			List<HikeSharedFile> sharedMedia = (List<HikeSharedFile>) ((ProfileSharedMedia) profileItem).getSharedFileList();
			LinearLayout layout = (LinearLayout) viewHolder.infoContainer;
			layout.removeAllViews();
			LayoutParams layoutParams;
			ImageView image;
			int smSize = ((ProfileSharedMedia) profileItem).getSharedMediaCount();
			viewHolder.subText.setText("" + smSize);
			if(sharedMedia != null && sharedMedia.size() < smSize )
			{
				for (HikeSharedFile galleryItem : sharedMedia)
				{
					View image_thumb = inflater.inflate(R.layout.thumbnail_layout, layout, false);
					View image_duration = image_thumb.findViewById(R.id.vid_time_layout);
					if(!galleryItem.getFileTypeString().toString().contains(IMAGE_TAG))
					{
						image_duration.setVisibility(View.VISIBLE);
					}
					image = (ImageView) image_thumb.findViewById(R.id.thumbnail);
					image.setTag(galleryItem);
					thumbnailLoader.loadImage(galleryItem.getImageLoaderKey(false), image);
					image.setOnClickListener(profileActivity);
					layout.addView(image_thumb);
				}
				//Add Arrow Icon
				View image_thumb = inflater.inflate(R.layout.thumbnail_layout, layout, false);
				image = (ImageView) image_thumb.findViewById(R.id.thumbnail);
				image.setTag(OPEN_GALLERY);
				image.setOnClickListener(profileActivity);
				image.setImageDrawable((context.getResources().getDrawable(R.drawable.ic_arrow)));
				image.setScaleType(ScaleType.CENTER);
				layout.addView(image_thumb);
			}
			
			else if(sharedMedia != null && sharedMedia.size() >= smSize)
			{
				 
				for (HikeSharedFile galleryItem : sharedMedia)
				{
					View image_thumb = inflater.inflate(R.layout.thumbnail_layout, layout, false);
					image = (ImageView) image_thumb.findViewById(R.id.thumbnail);
					image.setTag(galleryItem);
					thumbnailLoader.loadImage(galleryItem.getImageLoaderKey(false), image);
					image.setOnClickListener(profileActivity);
					layout.addView(image_thumb);
				}
				
			}
			else
			{		//Empty State
				layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				viewHolder.extraInfo.setVisibility(View.VISIBLE);
				layout.addView(viewHolder.extraInfo);
				layout.setLayoutParams(layoutParams);
			}
			
			break;

		case SHARED_CONTENT:
			viewHolder.infoContainer.setVisibility(View.VISIBLE);
			String heading = ((ProfileSharedContent)profileItem).getText();
			viewHolder.text.setText(heading);
			viewHolder.sharedFilesCount.setText(""+ ((ProfileSharedContent) profileItem).getSharedFilesCount());
			viewHolder.extraInfo.setText(""+ ((ProfileSharedContent) profileItem).getSharedPinsCount()); //PinCount
			int totalfiles = ((ProfileSharedContent) profileItem).getSharedFilesCount() + ((ProfileSharedContent) profileItem).getSharedPinsCount();
			viewHolder.subText.setText("" +  totalfiles); 

			if(groupProfile)
			{	viewHolder.groupOrPins.setText(context.getResources().getString(R.string.pins));
				viewHolder.icon.setBackground(context.getResources().getDrawable(R.drawable.ic_pin_2));
			}
			else
			{
				viewHolder.groupOrPins.setText(context.getResources().getString(R.string.groups));
				viewHolder.icon.setBackground(context.getResources().getDrawable(R.drawable.ic_group_2));
			}
			break;

		case PHONE_NUMBER:
			LinearLayout parentll = (LinearLayout) viewHolder.parent;
			parentll.removeAllViews();
			parentll.setVisibility(View.VISIBLE);
			String head = context.getResources().getString(R.string.phone_pa);
			viewHolder.text.setText(head);
			viewHolder.subText.setVisibility(View.GONE);
			View phoneNumberView = inflater.inflate(R.layout.phone_num_layout, parentll, false);
			TextView phoneNum = (TextView) phoneNumberView.findViewById(R.id.name);
			phoneNum.setText(mContactInfo.getMsisdn());
			TextView phoneType = (TextView) phoneNumberView.findViewById(R.id.main_info);
			if(mContactInfo.getMsisdnType().length()>0)
				phoneType.setText(mContactInfo.getMsisdnType());
			else
				phoneType.setVisibility(View.GONE);
			
			parentll.addView(phoneNumberView);
			
			break;

		case MEMBERS:
			viewHolder.text.setText(context.getResources().getString(R.string.members));
			viewHolder.subText.setText("" + ((ProfileGroupItem)profileItem).getTotalMembers());
			break;

		case GROUP_PARTICIPANT:
			LinearLayout parentView = (LinearLayout) v;
			parentView.removeAllViews();

			List<PairModified<GroupParticipant, String>> groupParticipants = ((ProfileGroupItem) profileItem).getGroupParticipants();
			parentView.setBackgroundColor(Color.WHITE);
			for (PairModified<GroupParticipant, String> groupParticipantPair : groupParticipants)
			{

				GroupParticipant groupParticipant = groupParticipantPair.getFirst();

				View groupParticipantParentView = inflater.inflate(R.layout.group_profile_item, parentView, false);

				TextView nameTextView = (TextView) groupParticipantParentView.findViewById(R.id.name);
				TextView mainInfo = (TextView) groupParticipantParentView.findViewById(R.id.main_info);

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
				String groupParticipantName = groupParticipantPair.getSecond();
				if (null == groupParticipantName)
				{
					groupParticipantName = contactInfo.getFirstNameAndSurname();
				}
				nameTextView.setText(groupParticipantName);
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
				groupParticipantParentView.setTag(groupParticipant);

				groupParticipantParentView.setOnClickListener(profileActivity);

				parentView.addView(groupParticipantParentView);

			}
			break;

		case ADD_MEMBERS:
			LinearLayout addMemberLayout = (LinearLayout) v;
			addMemberLayout.removeAllViews();
			View groupParticipantParentView = inflater.inflate(R.layout.group_profile_item, addMemberLayout, false);
			View avatarContainer = groupParticipantParentView.findViewById(R.id.avatar_container);
			avatarContainer.setVisibility(View.GONE);
			TextView nameTextView = (TextView) groupParticipantParentView.findViewById(R.id.name);
			TextView mainInfo = (TextView) groupParticipantParentView.findViewById(R.id.main_info);
			ImageView avatar = (ImageView) groupParticipantParentView.findViewById(R.id.add_participant);
			avatar.setVisibility(View.VISIBLE);
			nameTextView.setText(R.string.add_people);
			nameTextView.setTextColor(context.getResources().getColor(R.color.blue_hike));
			mainInfo.setVisibility(View.GONE);
			groupParticipantParentView.setTag(null);
			groupParticipantParentView.setOnClickListener(profileActivity);
			addMemberLayout.addView(groupParticipantParentView);

			break;

		case STATUS:
			StatusMessage statusMessage = ((ProfileStatusItem) profileItem).getStatusMessage();
			viewHolder.text.setText(myProfile ? context.getString(R.string.me) : statusMessage.getNotNullName());

			if (statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST_ACCEPTED
					|| statusMessage.getStatusMessageType() == StatusMessageType.USER_ACCEPTED_FRIEND_REQUEST)
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

		EditText editName;

		TextView subText;

		TextView extraInfo;
		
		TextView sharedFilesCount;
		
		TextView groupOrPins;

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