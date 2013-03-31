package com.bsb.hike.adapters;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

@SuppressWarnings("unchecked")
public class ProfileAdapter extends BaseAdapter {

	public static final int PROFILE_HEADER_ID = -1;
	public static final int PROFILE_BUTTON_ID = -2;
	public static final int PROFILE_EMPTY_ID = -3;

	public static final String GROUP_HEADER_ID = "-1";
	public static final String GROUP_BUTTON_ID = "-2";
	public static final String GROUP_LEAVE_BUTTON_ID = "-3";

	private static enum ViewType {
		HEADER, BUTTONS, STATUS, GROUP_PARTICIPANT, EMPTY_STATUS
	}

	private Context context;
	private List<GroupParticipant> groupParticipants;
	private List<StatusMessage> statusMessages;
	private GroupConversation groupConversation;
	private ContactInfo mContactInfo;
	private Bitmap profilePreview;
	private boolean groupProfile;
	private boolean myProfile;
	private int numParticipants;
	private boolean isContactBlocked;

	public ProfileAdapter(Context context, List<?> itemList,
			GroupConversation groupConversation, ContactInfo contactInfo,
			boolean myProfile) {
		this(context, itemList, groupConversation, contactInfo, myProfile,
				false);
	}

	public ProfileAdapter(Context context, List<?> itemList,
			GroupConversation groupConversation, ContactInfo contactInfo,
			boolean myProfile, boolean isContactBlocked) {
		this.context = context;
		this.groupProfile = groupConversation != null;
		if (groupProfile) {
			groupParticipants = (List<GroupParticipant>) itemList;
		} else {
			statusMessages = (List<StatusMessage>) itemList;
		}
		this.mContactInfo = contactInfo;
		this.groupConversation = groupConversation;
		this.myProfile = myProfile;
		this.isContactBlocked = isContactBlocked;
	}

	@Override
	public int getItemViewType(int position) {
		ViewType viewType;
		if (groupProfile) {
			ContactInfo contactInfo = groupParticipants.get(position)
					.getContactInfo();
			if (GROUP_HEADER_ID.equals(contactInfo.getId())) {
				viewType = ViewType.HEADER;
			} else if (GROUP_BUTTON_ID.equals(contactInfo.getId())
					|| GROUP_LEAVE_BUTTON_ID.equals(contactInfo.getId())) {
				viewType = ViewType.BUTTONS;
			} else {
				viewType = ViewType.GROUP_PARTICIPANT;
			}
		} else {
			StatusMessage statusMessage = statusMessages.get(position);
			if (PROFILE_HEADER_ID == statusMessage.getId()) {
				viewType = ViewType.HEADER;
			} else if (PROFILE_BUTTON_ID == statusMessage.getId()) {
				viewType = ViewType.BUTTONS;
			} else if (PROFILE_EMPTY_ID == statusMessage.getId()) {
				viewType = ViewType.EMPTY_STATUS;
			} else {
				viewType = ViewType.STATUS;
			}
		}
		return viewType.ordinal();
	}

	@Override
	public int getViewTypeCount() {
		return ViewType.values().length;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		/*
		 * We got an IndexOutOfBoundsException here.
		 */
		if (position >= getCount()) {
			return false;
		}
		ViewType viewType = ViewType.values()[getItemViewType(position)];
		if (viewType == ViewType.HEADER) {
			return false;
		} else if (viewType == ViewType.BUTTONS) {
			return false;
		}
		return true;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		Object item = getItem(position);
		ViewType viewType = ViewType.values()[getItemViewType(position)];

		GroupParticipant groupParticipant = null;
		StatusMessage statusMessage = null;

		if (item instanceof GroupParticipant) {
			groupParticipant = (GroupParticipant) item;
		} else {
			statusMessage = (StatusMessage) item;
		}

		ViewHolder viewHolder = null;
		View v = convertView;

		if (v == null) {
			viewHolder = new ViewHolder();

			switch (viewType) {
			case HEADER:
				v = inflater.inflate(R.layout.profile_header, null);

				viewHolder.text = (TextView) v.findViewById(R.id.name);
				viewHolder.subText = (TextView) v.findViewById(R.id.info);

				viewHolder.image = (ImageView) v.findViewById(R.id.profile);
				viewHolder.icon = (ImageView) v
						.findViewById(R.id.change_profile);
				viewHolder.requestLayout = (ViewGroup) v
						.findViewById(R.id.request_layout);

				viewHolder.requestMain = (TextView) v
						.findViewById(R.id.req_main);
				viewHolder.requestInfo = (TextView) v
						.findViewById(R.id.req_info);

				viewHolder.btn1 = (Button) v.findViewById(R.id.yes_btn);
				viewHolder.btn2 = (Button) v.findViewById(R.id.no_btn);
				break;

			case BUTTONS:
				v = inflater.inflate(R.layout.profile_btns_item, null);

				viewHolder.btnContainer1 = v.findViewById(R.id.btn1);
				viewHolder.btnContainer2 = v.findViewById(R.id.btn2);
				viewHolder.btnContainer3 = v.findViewById(R.id.btn3);

				viewHolder.btnText1 = (TextView) v.findViewById(R.id.btn1_txt);
				viewHolder.btnText2 = (TextView) v.findViewById(R.id.btn2_txt);
				viewHolder.btnText3 = (TextView) v.findViewById(R.id.btn3_txt);

				viewHolder.btnImage1 = (ImageView) v
						.findViewById(R.id.btn1_img);
				viewHolder.btnImage2 = (ImageView) v
						.findViewById(R.id.btn2_img);
				viewHolder.btnImage3 = (ImageView) v
						.findViewById(R.id.btn3_img);

				viewHolder.divContainer1 = (ViewGroup) v
						.findViewById(R.id.div_container_1);
				viewHolder.divContainer2 = (ViewGroup) v
						.findViewById(R.id.div_container_2);
				viewHolder.btnDivider = v.findViewById(R.id.btn_divider);
				viewHolder.marginView = v.findViewById(R.id.margin_view);

				viewHolder.container = (ViewGroup) v
						.findViewById(R.id.btn_container);
				break;

			case GROUP_PARTICIPANT:
				v = inflater.inflate(R.layout.group_profile_item, null);

				viewHolder.text = (TextView) v.findViewById(R.id.name);
				viewHolder.subText = (TextView) v.findViewById(R.id.info_txt);

				viewHolder.image = (ImageView) v.findViewById(R.id.avatar);
				viewHolder.icon = (ImageView) v
						.findViewById(R.id.unknown_contact);
				break;

			case STATUS:
				v = inflater.inflate(R.layout.profile_timeline_item, null);

				viewHolder.text = (TextView) v.findViewById(R.id.status_text);
				viewHolder.subText = (TextView) v.findViewById(R.id.time);

				viewHolder.image = (ImageView) v.findViewById(R.id.status_pic);
				viewHolder.icon = (ImageView) v.findViewById(R.id.status_type);

				viewHolder.container = (ViewGroup) v.findViewById(R.id.content);
				viewHolder.contentContainer = (ViewGroup) v
						.findViewById(R.id.content_container);
				break;

			case EMPTY_STATUS:
				v = inflater.inflate(R.layout.profile_timeline_negative_item,
						null);

				viewHolder.text = (TextView) v.findViewById(R.id.info);
				viewHolder.icon = (ImageView) v.findViewById(R.id.icon);
				viewHolder.btn1 = (Button) v.findViewById(R.id.btn);
				viewHolder.btn2 = (Button) v
						.findViewById(R.id.add_sms_friend_btn);
				break;
			}

			v.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) v.getTag();
		}

		switch (viewType) {
		case HEADER:
			String msisdn;
			String name;

			if (groupParticipant != null) {
				msisdn = groupConversation.getMsisdn();
				name = groupConversation.getLabel();
			} else {
				msisdn = mContactInfo.getMsisdn();
				name = TextUtils.isEmpty(mContactInfo.getName()) ? mContactInfo
						.getMsisdn() : mContactInfo.getName();
			}

			viewHolder.text.setText(name);
			if (profilePreview == null) {
				viewHolder.image.setImageDrawable(IconCacheManager
						.getInstance().getIconForMSISDN(msisdn));
			} else {
				viewHolder.image.setImageBitmap(profilePreview);
			}
			viewHolder.image.setTag(statusMessage);
			viewHolder.subText.setVisibility(View.GONE);
			viewHolder.icon
					.setVisibility((myProfile || groupConversation != null) ? View.VISIBLE
							: View.GONE);
			if (mContactInfo != null) {
				if (mContactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED) {
					viewHolder.requestLayout.setVisibility(View.VISIBLE);
					viewHolder.requestInfo.setVisibility(View.VISIBLE);
					viewHolder.requestMain.setVisibility(View.VISIBLE);
					viewHolder.btn2.setVisibility(View.VISIBLE);

					viewHolder.requestMain.setText(context.getString(
							R.string.added_as_friend, name));
					viewHolder.requestInfo.setText(context.getString(
							R.string.added_as_hike_friend_info,
							mContactInfo.getFirstName()));

					viewHolder.btn1.setText(R.string.confirm);
					viewHolder.btn1.setTag(mContactInfo);
					viewHolder.btn2.setTag(mContactInfo);
				} else if (mContactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED_REJECTED) {
					viewHolder.requestLayout.setVisibility(View.VISIBLE);
					viewHolder.requestInfo.setVisibility(View.GONE);
					viewHolder.requestMain.setVisibility(View.GONE);
					viewHolder.btn2.setVisibility(View.GONE);

					viewHolder.btn1.setText(R.string.add_as_friend);
					viewHolder.btn1.setTag(mContactInfo);
				} else {
					viewHolder.requestLayout.setVisibility(View.GONE);
				}
			}
			if (mContactInfo != null) {
				if (mContactInfo.isOnhike()) {
					if (mContactInfo.getHikeJoinTime() > 0) {
						viewHolder.subText.setVisibility(View.VISIBLE);
						viewHolder.subText.setText(context.getString(
								R.string.on_hike_since,
								mContactInfo.getFormattedHikeJoinTime()));
					} else {
						viewHolder.subText.setVisibility(View.INVISIBLE);
					}
				} else {
					viewHolder.subText.setVisibility(View.VISIBLE);
					viewHolder.subText.setText(R.string.on_sms);
				}
			}

			break;

		case BUTTONS:
			if (groupParticipant != null) {
				if (groupParticipant.getContactInfo().getId()
						.equals(GROUP_BUTTON_ID)) {
					viewHolder.btnContainer1
							.setVisibility(numParticipants < HikeConstants.MAX_CONTACTS_IN_GROUP ? View.VISIBLE
									: View.GONE);
					viewHolder.btnContainer2.setVisibility(View.VISIBLE);
					viewHolder.btnContainer3.setVisibility(View.GONE);
					viewHolder.marginView.setVisibility(View.GONE);

					viewHolder.btnDivider
							.setVisibility(viewHolder.btnContainer1
									.getVisibility() == View.VISIBLE
									&& viewHolder.btnContainer2.getVisibility() == View.VISIBLE ? View.VISIBLE
									: View.GONE);

					viewHolder.btnText1.setText(R.string.add_member);
					viewHolder.btnText2
							.setText(groupConversation.isMuted() ? R.string.unmute_group
									: R.string.mute_group);

					viewHolder.btnImage1
							.setImageResource(R.drawable.ic_add_member);
					viewHolder.btnImage2.setImageResource(groupConversation
							.isMuted() ? R.drawable.ic_unmute
							: R.drawable.ic_mute);
				} else {
					viewHolder.btnContainer1.setVisibility(View.GONE);
					viewHolder.btnContainer2.setVisibility(View.GONE);
					viewHolder.btnContainer3.setVisibility(View.VISIBLE);
					viewHolder.marginView.setVisibility(View.GONE);

					viewHolder.btnDivider.setVisibility(View.GONE);

					viewHolder.btnText3.setText(R.string.leave_group);

					viewHolder.btnImage3
							.setImageResource(R.drawable.ic_leave_group);
				}
				v.setBackgroundResource(R.color.seen_timeline_item);
				viewHolder.divContainer1.setVisibility(View.VISIBLE);
				viewHolder.divContainer2.setVisibility(View.GONE);
			} else {
				viewHolder.btnContainer1.setVisibility(View.GONE);
				viewHolder.btnContainer2.setVisibility(View.GONE);
				viewHolder.container.setVisibility(View.GONE);

				viewHolder.marginView.setVisibility(View.GONE);

				viewHolder.btnContainer3.setVisibility(View.VISIBLE);
				viewHolder.btnText3.setText(myProfile ? R.string.post_status
						: R.string.send_message);
				viewHolder.btnImage3
						.setImageResource(myProfile ? R.drawable.ic_post_status
								: R.drawable.ic_msg_small);
				v.setBackgroundResource(R.color.seen_timeline_item);
				viewHolder.divContainer1.setVisibility(View.GONE);
				viewHolder.divContainer2.setVisibility(View.VISIBLE);
			}
			break;

		case GROUP_PARTICIPANT:
			ContactInfo contactInfo = groupParticipant.getContactInfo();

			viewHolder.image.setImageDrawable(IconCacheManager.getInstance()
					.getIconForMSISDN(contactInfo.getMsisdn()));

			viewHolder.text.setText(contactInfo.getName());
			if (contactInfo.isUnknownContact()) {
				viewHolder.text.append(" (" + contactInfo.getMsisdn() + ")");
			}

			viewHolder.subText.setVisibility(View.VISIBLE);
			if (groupConversation.getGroupOwner().equals(
					contactInfo.getMsisdn())) {
				viewHolder.subText.setText(R.string.owner);
			} else if (!contactInfo.isOnhike()) {
				viewHolder.subText
						.setText(groupParticipant.onDnd() ? R.string.on_dnd
								: R.string.on_sms);
			} else {
				viewHolder.subText.setVisibility(View.GONE);
			}

			viewHolder.icon.setVisibility(View.GONE);
			break;

		case STATUS:
			if (myProfile) {
				viewHolder.contentContainer
						.setBackgroundResource(R.drawable.seen_timeline_selector);
			}
			SmileyParser smileyParser = SmileyParser.getInstance();
			viewHolder.text.setText(smileyParser.addSmileySpans(
					statusMessage.getText(), true));
			viewHolder.image.setVisibility(View.GONE);
			viewHolder.subText.setText(statusMessage
					.getTimestampFormatted(true));
			viewHolder.icon.setImageResource(R.drawable.ic_text_status);
			if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC) {
				viewHolder.image.setVisibility(View.VISIBLE);
				viewHolder.image.setImageDrawable(IconCacheManager
						.getInstance().getIconForMSISDN(
								statusMessage.getMappedId()));
				viewHolder.icon
						.setImageResource(R.drawable.ic_profile_pic_status);
				viewHolder.image.setId(position);
				viewHolder.image.setTag(statusMessage);
				viewHolder.text.setText(R.string.changed_profile);
			} else if (statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST_ACCEPTED
					|| statusMessage.getStatusMessageType() == StatusMessageType.USER_ACCEPTED_FRIEND_REQUEST) {
				viewHolder.icon
						.setImageResource(R.drawable.ic_profile_pic_status);
			}
			if (statusMessage.hasMood()) {
				viewHolder.icon.setBackgroundDrawable(null);
				viewHolder.icon
						.setImageResource(Utils.getMoodsResource()[statusMessage
								.getMoodId()]);
			} else {
				viewHolder.icon
						.setBackgroundResource(R.drawable.bg_status_type);
			}
			LayoutParams lp = (LayoutParams) viewHolder.subText
					.getLayoutParams();
			if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC
					|| viewHolder.text.getLineCount() > 1) {
				lp.topMargin = 0;
				viewHolder.subText.setLayoutParams(lp);
			} else {
				lp.topMargin = (int) (5 * Utils.densityMultiplier);
				viewHolder.subText.setLayoutParams(lp);
			}
			break;

		case EMPTY_STATUS:
			String contactName = TextUtils.isEmpty(mContactInfo.getName()) ? mContactInfo
					.getMsisdn() : mContactInfo.getName();

			if (!isContactBlocked) {
				if (mContactInfo.isOnhike()) {
					viewHolder.btn2.setVisibility(View.GONE);
					viewHolder.icon.setImageResource(R.drawable.ic_not_friend);
					if (mContactInfo.getFavoriteType() == FavoriteType.REQUEST_SENT) {
						viewHolder.text.setText(context.getString(
								R.string.waiting_for_accept, contactName));
						viewHolder.btn1.setVisibility(View.GONE);
					} else {
						viewHolder.text.setText(context.getString(
								R.string.add_as_friend_info, contactName));
						viewHolder.btn1.setText(R.string.add_as_friend);
						viewHolder.btn1.setVisibility(View.VISIBLE);
					}
				} else {
					viewHolder.icon.setImageResource(R.drawable.ic_not_on_hike);
					viewHolder.text.setText(context.getString(
							R.string.not_on_hike, contactName));
					viewHolder.btn1.setText(R.string.invite_to_hike);
					viewHolder.btn2
							.setVisibility(mContactInfo.getFavoriteType() == FavoriteType.NOT_FRIEND ? View.VISIBLE
									: View.GONE);
				}
			} else {
				viewHolder.icon.setImageResource(R.drawable.ic_block_profile);
				viewHolder.text.setText(context.getString(
						R.string.user_blocked, contactName));
				viewHolder.btn1.setText(R.string.unblock_title);
				viewHolder.btn2.setVisibility(View.GONE);
			}

			break;
		}

		return v;
	}

	private class ViewHolder {
		TextView text;
		TextView subText;
		TextView requestMain;
		TextView requestInfo;
		ImageView image;
		ImageView icon;
		Button btn1;
		Button btn2;
		ViewGroup container;
		View btnDivider;
		ViewGroup requestLayout;
		View marginView;
		ViewGroup contentContainer;
		View btnContainer1;
		View btnContainer2;
		View btnContainer3;
		TextView btnText1;
		TextView btnText2;
		TextView btnText3;
		ImageView btnImage1;
		ImageView btnImage2;
		ImageView btnImage3;
		ViewGroup divContainer1;
		ViewGroup divContainer2;
	}

	@Override
	public int getCount() {
		return groupProfile ? groupParticipants.size() : statusMessages.size();
	}

	@Override
	public Object getItem(int position) {
		return groupProfile ? groupParticipants.get(position) : statusMessages
				.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	public void setProfilePreview(Bitmap preview) {
		this.profilePreview = preview;
		notifyDataSetChanged();
	}

	public void updateGroupConversation(GroupConversation groupConversation) {
		this.groupConversation = groupConversation;
		notifyDataSetChanged();
	}

	public void updateContactInfo(ContactInfo contactInfo) {
		this.mContactInfo = contactInfo;
		notifyDataSetChanged();
	}

	public void setNumParticipants(int numParticipants) {
		this.numParticipants = numParticipants;
	}

	public void setIsContactBlocked(boolean b) {
		isContactBlocked = b;
	}

	public boolean isContactBlocked() {
		return isContactBlocked;
	}
}