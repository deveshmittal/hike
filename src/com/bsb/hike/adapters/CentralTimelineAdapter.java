package com.bsb.hike.adapters;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.SmileyParser;

public class CentralTimelineAdapter extends BaseAdapter {

	public static final long EMPTY_STATUS_NO_FRIEND_ID = -1;
	public static final long FRIEND_REQUEST_ID = -2;
	public static final long EMPTY_STATUS_NO_STATUS_ID = -3;
	public static final long EMPTY_STATUS_NO_FRIEND_NO_STATUS_ID = -4;
	public static final long EMPTY_STATUS_NO_STATUS_RECENTLY_ID = -5;

	private List<StatusMessage> statusMessages;
	private Context context;
	private String userMsisdn;
	private int unseenCount;

	public CentralTimelineAdapter(Context context,
			List<StatusMessage> statusMessages, String userMsisdn,
			int unseenCount) {
		this.context = context;
		this.statusMessages = statusMessages;
		this.userMsisdn = userMsisdn;
		this.unseenCount = unseenCount;
	}

	@Override
	public int getCount() {
		return statusMessages.size();
	}

	@Override
	public StatusMessage getItem(int position) {
		return statusMessages.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		return true;
	}

	public void incrementUnseenCount() {
		unseenCount++;
	}

	public void decrementUnseenCount() {
		if (unseenCount > 0) {
			unseenCount--;
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		StatusMessage statusMessage = getItem(position);

		ViewHolder viewHolder;

		if (convertView == null) {
			convertView = inflater.inflate(R.layout.timeline_item, null);

			viewHolder = new ViewHolder();

			viewHolder.avatar = (ImageView) convertView
					.findViewById(R.id.avatar);

			viewHolder.name = (TextView) convertView.findViewById(R.id.name);
			viewHolder.mainInfo = (TextView) convertView
					.findViewById(R.id.main_info);
			viewHolder.extraInfo = (TextView) convertView
					.findViewById(R.id.details);
			viewHolder.timeStamp = (TextView) convertView
					.findViewById(R.id.timestamp);

			viewHolder.detailsBtn = (ImageView) convertView
					.findViewById(R.id.details_btn);
			viewHolder.yesBtn = (TextView) convertView
					.findViewById(R.id.yes_btn);
			viewHolder.noBtn = (TextView) convertView.findViewById(R.id.no_btn);

			viewHolder.divider = (ImageView) convertView
					.findViewById(R.id.divider);

			viewHolder.content = (ViewGroup) convertView
					.findViewById(R.id.main_content);

			viewHolder.statusImg = (ImageView) convertView
					.findViewById(R.id.status_pic);

			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		if (statusMessage.hasMood()) {
			viewHolder.avatar
					.setImageResource(EmoticonConstants.MOOD_RES_IDS[statusMessage
							.getMoodId()]);
		} else {
			viewHolder.avatar.setImageDrawable(IconCacheManager.getInstance()
					.getIconForMSISDN(statusMessage.getMsisdn()));
		}
		viewHolder.name
				.setText(userMsisdn.equals(statusMessage.getMsisdn()) ? "Me"
						: statusMessage.getNotNullName());

		viewHolder.mainInfo.setText(statusMessage.getText());

		viewHolder.timeStamp.setText(statusMessage.getTimestampFormatted(true));

		viewHolder.statusImg.setVisibility(View.GONE);

		switch (statusMessage.getStatusMessageType()) {
		case NO_STATUS:
			viewHolder.extraInfo.setVisibility(View.VISIBLE);
			viewHolder.yesBtn.setVisibility(View.VISIBLE);
			viewHolder.noBtn.setVisibility(View.GONE);

			if (EMPTY_STATUS_NO_FRIEND_ID == statusMessage.getId()) {
				viewHolder.extraInfo.setText(R.string.add_friend_info);
				viewHolder.yesBtn.setText(R.string.add_hike_friend);
			} else if (EMPTY_STATUS_NO_STATUS_ID == statusMessage.getId()) {
				viewHolder.extraInfo.setText(R.string.no_status);
				viewHolder.yesBtn.setText(R.string.update_status);
			} else if (EMPTY_STATUS_NO_STATUS_RECENTLY_ID == statusMessage
					.getId()) {
				viewHolder.extraInfo.setText(R.string.no_status_recently);
				viewHolder.yesBtn.setText(R.string.update_status);
			}
			break;
		case FRIEND_REQUEST:
			viewHolder.extraInfo.setVisibility(View.VISIBLE);
			viewHolder.yesBtn.setVisibility(View.VISIBLE);
			viewHolder.noBtn.setVisibility(View.VISIBLE);

			viewHolder.extraInfo.setText(context.getString(
					R.string.added_as_hike_friend_info,
					statusMessage.getNotNullName()));
			viewHolder.yesBtn.setText(R.string.confirm);
			viewHolder.noBtn.setText(R.string.no_thanks);
			break;
		case TEXT:
			viewHolder.extraInfo.setVisibility(View.GONE);
			viewHolder.yesBtn.setVisibility(View.GONE);
			viewHolder.noBtn.setVisibility(View.GONE);

			SmileyParser smileyParser = SmileyParser.getInstance();
			viewHolder.mainInfo.setText(smileyParser.addSmileySpans(
					statusMessage.getText(), true));
			break;
		case PROFILE_PIC:
			viewHolder.extraInfo.setVisibility(View.GONE);
			viewHolder.yesBtn.setVisibility(View.GONE);
			viewHolder.noBtn.setVisibility(View.GONE);
			viewHolder.statusImg.setVisibility(View.VISIBLE);

			viewHolder.mainInfo.setText(R.string.changed_profile);
			viewHolder.statusImg.setImageDrawable(IconCacheManager
					.getInstance()
					.getIconForMSISDN(statusMessage.getMappedId()));
			viewHolder.statusImg.setId(position);
			break;
		case FRIEND_REQUEST_ACCEPTED:
		case USER_ACCEPTED_FRIEND_REQUEST:
			viewHolder.yesBtn.setVisibility(View.GONE);
			viewHolder.noBtn.setVisibility(View.GONE);
			viewHolder.extraInfo.setVisibility(View.VISIBLE);

			viewHolder.extraInfo
					.setText(context.getString(
							statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST_ACCEPTED ? R.string.confirmed_friend_info
									: R.string.accepted_friend_request_info,
							statusMessage.getNotNullName()));
			break;
		}

		viewHolder.content
				.setBackgroundResource(position >= unseenCount ? R.drawable.seen_timeline_selector
						: R.drawable.timeline_selector);

		viewHolder.divider
				.setBackgroundResource(isLastUnseenStatus(position) ? R.drawable.new_update_repeat
						: R.drawable.ic_thread_divider_profile);

		viewHolder.detailsBtn.setTag(statusMessage);
		viewHolder.yesBtn.setTag(statusMessage);
		viewHolder.noBtn.setTag(statusMessage);
		viewHolder.statusImg.setTag(statusMessage);
		viewHolder.avatar.setTag(statusMessage);

		return convertView;
	}

	private boolean isLastUnseenStatus(int position) {
		/*
		 * We show a different divider if the status message in the current
		 * position is unseen and the one after that has already been seen.
		 */
		if (position >= unseenCount) {
			return false;
		}
		if (position == statusMessages.size() - 1) {
			return true;
		} else if (position + 1 >= unseenCount) {
			return true;
		}
		return false;
	}

	private class ViewHolder {
		ImageView avatar;
		TextView name;
		TextView mainInfo;
		TextView extraInfo;
		TextView timeStamp;
		ImageView detailsBtn;
		TextView yesBtn;
		TextView noBtn;
		ImageView divider;
		ViewGroup content;
		ImageView statusImg;
	}
}
