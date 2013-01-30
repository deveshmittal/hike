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

public class CentralTimelineAdapter extends BaseAdapter {

	public static final long EMPTY_STATUS_ID = -1;
	public static final long FRIEND_REQUEST_ID = -2;

	private List<StatusMessage> statusMessages;
	private Context context;
	private String userMsisdn;

	public CentralTimelineAdapter(Context context,
			List<StatusMessage> statusMessages, String userMsisdn) {
		this.context = context;
		this.statusMessages = statusMessages;
		this.userMsisdn = userMsisdn;
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
			viewHolder.statusType = (ImageView) convertView
					.findViewById(R.id.status_type);

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

			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		viewHolder.avatar.setImageDrawable(IconCacheManager.getInstance()
				.getIconForMSISDN(statusMessage.getMsisdn()));
		viewHolder.name
				.setText(userMsisdn.equals(statusMessage.getMsisdn()) ? "Me"
						: statusMessage.getName());

		viewHolder.mainInfo.setText(statusMessage.getText());

		viewHolder.timeStamp.setText(statusMessage.getTimestampFormatted(true));

		if (statusMessage.getId() == EMPTY_STATUS_ID) {
			viewHolder.extraInfo.setVisibility(View.VISIBLE);
			viewHolder.yesBtn.setVisibility(View.VISIBLE);
			viewHolder.noBtn.setVisibility(View.GONE);

			viewHolder.extraInfo.setText(R.string.add_friend_info);
			viewHolder.yesBtn.setText(R.string.add_hike_friend);
			viewHolder.statusType.setImageResource(R.drawable.ic_no_status);
			viewHolder.statusType.setBackgroundDrawable(null);
		} else if (statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST) {
			viewHolder.extraInfo.setVisibility(View.VISIBLE);
			viewHolder.yesBtn.setVisibility(View.VISIBLE);
			viewHolder.noBtn.setVisibility(View.VISIBLE);

			viewHolder.extraInfo
					.setText(context.getString(
							R.string.added_as_hike_friend_info,
							statusMessage.getName()));
			viewHolder.yesBtn.setText(R.string.confirm);
			viewHolder.noBtn.setText(R.string.no_thanks);
			viewHolder.statusType
					.setImageResource(R.drawable.ic_profile_pic_status);
			viewHolder.statusType
					.setBackgroundResource(R.drawable.bg_status_type);
		} else {
			viewHolder.extraInfo.setVisibility(View.GONE);
			viewHolder.yesBtn.setVisibility(View.GONE);
			viewHolder.noBtn.setVisibility(View.GONE);
			viewHolder.extraInfo.setVisibility(View.GONE);

			viewHolder.statusType.setImageResource(R.drawable.ic_text_status);
			viewHolder.statusType
					.setBackgroundResource(R.drawable.bg_status_type);
		}
		viewHolder.detailsBtn.setTag(statusMessage);
		viewHolder.yesBtn.setTag(statusMessage);
		viewHolder.noBtn.setTag(statusMessage);

		return convertView;
	}

	private class ViewHolder {
		ImageView avatar;
		ImageView statusType;
		TextView name;
		TextView mainInfo;
		TextView extraInfo;
		TextView timeStamp;
		ImageView detailsBtn;
		TextView yesBtn;
		TextView noBtn;
	}
}
