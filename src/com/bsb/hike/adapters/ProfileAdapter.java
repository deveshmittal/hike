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
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.utils.IconCacheManager;

@SuppressWarnings("unchecked")
public class ProfileAdapter extends BaseAdapter {

	public static final int PROFILE_HEADER_ID = -1;
	public static final int PROFILE_BUTTON_ID = -2;
	public static final int PROFILE_EMPTY_ID = -3;

	public static final String GROUP_HEADER_ID = "-1";
	public static final String GROUP_BUTTON_ID = "-2";

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

	public ProfileAdapter(Context context, List<?> itemList,
			GroupConversation groupConversation, ContactInfo contactInfo,
			boolean myProfile) {
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
	}

	@Override
	public int getItemViewType(int position) {
		ViewType viewType;
		if (groupProfile) {
			ContactInfo contactInfo = groupParticipants.get(position)
					.getContactInfo();
			if (GROUP_HEADER_ID.equals(contactInfo.getId())) {
				viewType = ViewType.HEADER;
			} else if (GROUP_BUTTON_ID.equals(contactInfo.getId())) {
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
		ViewType viewType = ViewType.values()[getItemViewType(position)];
		if (viewType == ViewType.HEADER) {
			return false;
		} else if (viewType == ViewType.BUTTONS) {
			return !groupProfile;
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
				break;

			case BUTTONS:
				v = inflater.inflate(R.layout.profile_btns_item, null);

				viewHolder.btn1 = (Button) v.findViewById(R.id.btn1);
				viewHolder.btn2 = (Button) v.findViewById(R.id.btn2);
				viewHolder.btn3 = (Button) v.findViewById(R.id.btn3);

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
				break;

			case EMPTY_STATUS:
				v = inflater.inflate(R.layout.profile_timeline_negative_item,
						null);

				viewHolder.text = (TextView) v.findViewById(R.id.info);
				viewHolder.icon = (ImageView) v.findViewById(R.id.icon);
				viewHolder.btn1 = (Button) v.findViewById(R.id.btn);
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
			viewHolder.subText.setVisibility(View.GONE);
			viewHolder.icon
					.setVisibility((myProfile || groupConversation != null) ? View.VISIBLE
							: View.GONE);
			break;

		case BUTTONS:
			if (groupParticipant != null) {
				viewHolder.btn1.setText(R.string.add_member);
				viewHolder.btn2.setText(R.string.leave_group);
				viewHolder.btn3.setText(R.string.invite_all_members);

				viewHolder.btn1.setCompoundDrawablesWithIntrinsicBounds(
						R.drawable.ic_add_member, 0, 0, 0);
				viewHolder.btn2.setCompoundDrawablesWithIntrinsicBounds(
						R.drawable.ic_leave_group, 0, 0, 0);
				viewHolder.btn1.setCompoundDrawablesWithIntrinsicBounds(
						R.drawable.ic_invite_all_members, 0, 0, 0);

				v.setBackgroundResource(R.drawable.thatch_repeat);
			} else {
				viewHolder.btn1.setVisibility(View.GONE);
				viewHolder.btn2.setVisibility(View.GONE);
				viewHolder.container.setVisibility(View.GONE);

				viewHolder.btn3.setText(myProfile ? R.string.post_status
						: R.string.send_message);
				viewHolder.btn3.setCompoundDrawablesWithIntrinsicBounds(
						myProfile ? R.drawable.ic_post_status
								: R.drawable.ic_msg_small, 0, 0, 0);
			}
			break;

		case GROUP_PARTICIPANT:
			ContactInfo contactInfo = groupParticipant.getContactInfo();

			viewHolder.image.setImageDrawable(IconCacheManager.getInstance()
					.getIconForMSISDN(contactInfo.getMsisdn()));

			viewHolder.text.setText(contactInfo.getName());
			if (contactInfo.getMsisdn().equals(contactInfo.getId())) {
				viewHolder.text.append(" (" + contactInfo.getMsisdn() + ")");
			}

			if (groupConversation.getGroupOwner().equals(
					contactInfo.getMsisdn())) {
				viewHolder.subText.setText(R.string.owner);
			} else if (!contactInfo.isOnhike()) {
				viewHolder.subText.setText(R.string.on_sms);
			} else {
				viewHolder.subText.setVisibility(View.GONE);
			}

			viewHolder.icon.setVisibility(View.GONE);
			break;

		case STATUS:
			viewHolder.text.setText(statusMessage.getText());
			viewHolder.image.setVisibility(View.GONE);
			viewHolder.subText.setText(statusMessage
					.getTimestampFormatted(true));
			viewHolder.icon.setImageResource(R.drawable.ic_text_status);
			break;

		case EMPTY_STATUS:
			String contactName = TextUtils.isEmpty(mContactInfo.getName()) ? mContactInfo
					.getMsisdn() : mContactInfo.getName();

			viewHolder.text.setText(context.getString(
					mContactInfo.isOnhike() ? R.string.add_as_friend_info
							: R.string.not_on_hike, contactName));

			viewHolder.icon
					.setImageResource(mContactInfo.isOnhike() ? R.drawable.ic_not_friend
							: R.drawable.ic_not_on_hike);

			viewHolder.btn1
					.setText(mContactInfo.isOnhike() ? R.string.add_as_friend
							: R.string.invite_to_hike);
			break;
		}

		return v;
	}

	private class ViewHolder {
		TextView text;
		TextView subText;
		ImageView image;
		ImageView icon;
		Button btn1;
		Button btn2;
		Button btn3;
		ViewGroup container;
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
}