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
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.models.ImageViewerInfo;
import com.bsb.hike.models.ProfileItem;
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
		HEADER, SHARED_MEDIA, SHARED_CONTENT, STATUS, PROFILE_PIC_UPDATE, GROUP_PARTICIPANT, EMPTY_STATUS, REQUEST, MEMBERS, ADD_MEMBERS, PHONE_NUMBER
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

			case SHARED_MEDIA:
				v = inflater.inflate(R.layout.shared_media, null);
				viewHolder.text = (TextView) v.findViewById(R.id.name);
				viewHolder.subText = (TextView) v.findViewById(R.id.count);
				viewHolder.infoContainer = v.findViewById(R.id.shared_media_items);
				viewHolder.extraInfo = (TextView) v.findViewById(R.id.sm_emptystate);
				
				List<HikeSharedFile> sharedMedia = (List<HikeSharedFile>) ((ProfileSharedMedia) profileItem).getSharedFileList();
				LinearLayout layout = (LinearLayout) viewHolder.infoContainer;
				layout.removeAllViews();
				int smSize = ((ProfileSharedMedia) profileItem).getSharedMediaCount();
				viewHolder.subText.setText(Integer.toString(smSize));
				
				if(sharedMedia != null)
				{
					for (HikeSharedFile galleryItem : sharedMedia)
					{
						View image_thumb = inflater.inflate(R.layout.thumbnail_layout, layout, false);
						layout.addView(image_thumb);
					}
					
					if(sharedMedia.size() < smSize )
					{
						// Add Arrow Icon
						View image_thumb = inflater.inflate(R.layout.thumbnail_layout, layout, false);
						layout.addView(image_thumb);
					}
				}
				
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
				viewHolder.parent = inflater.inflate(R.layout.group_profile_item, (LinearLayout) v, false);
				viewHolder.text = (TextView) viewHolder.parent.findViewById(R.id.name);
				viewHolder.extraInfo  = (TextView) viewHolder.parent.findViewById(R.id.main_info);
				viewHolder.icon  = (ImageView) viewHolder.parent.findViewById(R.id.avatar);
				viewHolder.iconFrame = (ImageView) viewHolder.parent.findViewById(R.id.avatar_frame);
				viewHolder.infoContainer = viewHolder.parent.findViewById(R.id.owner_indicator);
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
				viewHolder.phoneNumView = inflater.inflate(R.layout.phone_num_layout, (ViewGroup) viewHolder.parent, false);
				viewHolder.extraInfo = (TextView) viewHolder.phoneNumView.findViewById(R.id.name);
				viewHolder.groupOrPins = (TextView) viewHolder.phoneNumView.findViewById(R.id.main_info);
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
			LayoutParams layoutParams;
			ImageView image;
			int smSize = ((ProfileSharedMedia) profileItem).getSharedMediaCount();
			viewHolder.subText.setText(Integer.toString(smSize));
			
			if(sharedMedia != null)
			{
				int i = 0;
				for (i = 0; i < sharedMedia.size(); i++)
				{
					HikeSharedFile galleryItem = sharedMedia.get(i);
					View image_thumb = layout.getChildAt(i);
					View image_duration = image_thumb.findViewById(R.id.vid_time_layout);
					View fileMissing = image_thumb.findViewById(R.id.file_missing_layout);
					if(galleryItem.getHikeFileType() == HikeFileType.VIDEO)
					{
						image_duration.setVisibility(View.VISIBLE);
					}
					
					image = (ImageView) image_thumb.findViewById(R.id.thumbnail);
					if(galleryItem.getFileFromExactFilePath().exists())
					{
						thumbnailLoader.loadImage(galleryItem.getImageLoaderKey(false), image);
						fileMissing.setVisibility(View.GONE);
						
						if (galleryItem.getHikeFileType() == HikeFileType.VIDEO)
						{
							image_duration.setVisibility(View.VISIBLE);
						}
						else
						{
							image_duration.setVisibility(View.GONE);
						}
					}
					else
					{
						image_duration.setVisibility(View.GONE);
						fileMissing.setVisibility(View.VISIBLE);
					}
					
					image_thumb.setTag(galleryItem);
					image_thumb.setOnClickListener(profileActivity);
				}
				if(sharedMedia.size() < smSize )
				{
					// Add Arrow Icon
					View image_thumb = layout.getChildAt(i);
					image = (ImageView) image_thumb.findViewById(R.id.thumbnail);
					image.setTag(OPEN_GALLERY);
					image.setOnClickListener(profileActivity);
					image.setImageDrawable((context.getResources().getDrawable(R.drawable.ic_arrow)));
					image.setScaleType(ScaleType.CENTER);
				}
			}
			else
			{		//Empty State
				layout.removeAllViews();
				viewHolder.extraInfo.setVisibility(View.VISIBLE);
				layoutParams = (LayoutParams) viewHolder.extraInfo.getLayoutParams();
				layoutParams.width = LayoutParams.MATCH_PARENT;
				layoutParams.height = LayoutParams.WRAP_CONTENT;
				layout.addView(viewHolder.extraInfo);
				layout.setLayoutParams(layoutParams);
			}
			
			break;

		case SHARED_CONTENT:
			viewHolder.infoContainer.setVisibility(View.VISIBLE);
			String heading = ((ProfileSharedContent)profileItem).getText();
			viewHolder.text.setText(heading);
			viewHolder.sharedFilesCount.setText(Integer.toString(((ProfileSharedContent) profileItem).getSharedFilesCount()));
			viewHolder.extraInfo.setText(Integer.toString(((ProfileSharedContent) profileItem).getSharedPinsCount())); //PinCount
			int totalfiles = ((ProfileSharedContent) profileItem).getSharedFilesCount() + ((ProfileSharedContent) profileItem).getSharedPinsCount();
			viewHolder.subText.setText(Integer.toString(totalfiles)); 

			if(groupProfile)
			{	
				/*
				 * We will remove these two lines when we will be add groups for 1:1
				 */
				((LinearLayout) viewHolder.infoContainer).getChildAt(1).setVisibility(View.VISIBLE);
				((LinearLayout) viewHolder.infoContainer).findViewById(R.id.shared_content_seprator).setVisibility(View.VISIBLE);
				
				viewHolder.groupOrPins.setText(context.getResources().getString(R.string.pins));
				viewHolder.icon.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_pin_2));
			}
			else
			{
				/*
				 * We will remove these two lines when we will be add groups for 1:1
				 */
				
				((LinearLayout) viewHolder.infoContainer).getChildAt(1).setVisibility(View.GONE);
				((LinearLayout) viewHolder.infoContainer).findViewById(R.id.shared_content_seprator).setVisibility(View.GONE);
				
				viewHolder.groupOrPins.setText(context.getResources().getString(R.string.groups));
				viewHolder.icon.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_group_2));
			}
			break;

		case PHONE_NUMBER:
			LinearLayout parentll = (LinearLayout) viewHolder.parent;
			parentll.removeAllViews();
			parentll.setVisibility(View.VISIBLE);
			String head = context.getResources().getString(R.string.phone_pa);
			viewHolder.text.setText(head);
			viewHolder.subText.setVisibility(View.GONE);
			viewHolder.extraInfo.setText(mContactInfo.getMsisdn());
			if(mContactInfo.getMsisdnType().length()>0)
				viewHolder.groupOrPins.setText(mContactInfo.getMsisdnType());
			else
				viewHolder.groupOrPins.setVisibility(View.GONE);
			
			parentll.addView(viewHolder.phoneNumView);
			
			break;

		case MEMBERS:
			viewHolder.text.setText(context.getResources().getString(R.string.members));
			viewHolder.subText.setText(Integer.toString(((ProfileGroupItem)profileItem).getTotalMembers()));
			break;

		case GROUP_PARTICIPANT:
			LinearLayout parentView = (LinearLayout) v;
			parentView.removeAllViews();
			PairModified<GroupParticipant, String> groupParticipants = ((ProfileGroupItem) profileItem).getGroupParticipant();
			parentView.setBackgroundColor(Color.WHITE);
			GroupParticipant groupParticipant = groupParticipants.getFirst();
			
			ContactInfo contactInfo = groupParticipant.getContactInfo();
			if (contactInfo.getMsisdn().equals(groupConversation.getGroupOwner()))
			{
				viewHolder.infoContainer.setVisibility(View.VISIBLE);
			}
			else
			{
				viewHolder.infoContainer.setVisibility(View.GONE);
			}

			int offline = contactInfo.getOffline();
			String lastSeenString = null;
			boolean showingLastSeen = false;
			if (lastSeenPref && contactInfo.getFavoriteType() == FavoriteType.FRIEND && !contactInfo.getMsisdn().equals(contactInfo.getId()))
			{
				lastSeenString = Utils.getLastSeenTimeAsString(context, contactInfo.getLastSeenTime(), offline, true);
				showingLastSeen = !TextUtils.isEmpty(lastSeenString);
			}
			String groupParticipantName = groupParticipants.getSecond();
			if (null == groupParticipantName)
			{
				groupParticipantName = contactInfo.getFirstNameAndSurname();
			}
			viewHolder.text.setText(groupParticipantName);
			if (!showingLastSeen)
			{
				viewHolder.extraInfo.setText(contactInfo.isOnhike() ? R.string.on_hike : R.string.on_sms);
			}
			else
			{
				viewHolder.extraInfo.setText(lastSeenString);
			}
			if (showingLastSeen && offline == 0)
			{
				viewHolder.extraInfo.setTextColor(context.getResources().getColor(R.color.unread_message));
				viewHolder.iconFrame.setImageResource(R.drawable.frame_avatar_highlight);
			}
			else
			{
				viewHolder.extraInfo.setTextColor(context.getResources().getColor(R.color.participant_last_seen));
				viewHolder.iconFrame.setImageDrawable(null);
			}
			setAvatar(contactInfo.getMsisdn(), viewHolder.icon);
			viewHolder.parent.setOnLongClickListener(profileActivity);
			viewHolder.parent.setTag(groupParticipant);

			viewHolder.parent.setOnClickListener(profileActivity);

			parentView.addView(viewHolder.parent);
			
			break;

		case ADD_MEMBERS:
			LinearLayout addMemberLayout = (LinearLayout) v;
			addMemberLayout.removeAllViews();
			View groupParticipantParentView_mem = inflater.inflate(R.layout.group_profile_item, addMemberLayout, false);
			View avatarContainer = groupParticipantParentView_mem.findViewById(R.id.avatar_container);
			avatarContainer.setVisibility(View.GONE);
			TextView nameTextView_mem = (TextView) groupParticipantParentView_mem.findViewById(R.id.name);
			TextView mainInfo_mem = (TextView) groupParticipantParentView_mem.findViewById(R.id.main_info);
			ImageView avatar_mem = (ImageView) groupParticipantParentView_mem.findViewById(R.id.add_participant);
			avatar_mem.setVisibility(View.VISIBLE);
			nameTextView_mem.setText(R.string.add_people);
			nameTextView_mem.setTextColor(context.getResources().getColor(R.color.blue_hike));
			mainInfo_mem.setVisibility(View.GONE);
			groupParticipantParentView_mem.setTag(null);
			groupParticipantParentView_mem.setOnClickListener(profileActivity);
			addMemberLayout.addView(groupParticipantParentView_mem);

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
		
		View phoneNumView;
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
	
	public ProfilePicImageLoader getProfilePicImageLoader()
	{
		return profileImageLoader;
	}

	private boolean isListFlinging;

	public void setIsListFlinging(boolean b)
	{
		boolean notify = b != isListFlinging;

		isListFlinging = b;

		if (notify && !isListFlinging)
		{
			notifyDataSetChanged();
		}
	}
}