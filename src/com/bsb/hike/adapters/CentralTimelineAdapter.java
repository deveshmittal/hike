package com.bsb.hike.adapters;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.models.Protip;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.ui.StatusUpdate;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

public class CentralTimelineAdapter extends BaseAdapter {

	public static final long EMPTY_STATUS_NO_STATUS_ID = -3;
	public static final long EMPTY_STATUS_NO_STATUS_RECENTLY_ID = -5;

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

	}

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

			viewHolder.yesBtn = (TextView) convertView
					.findViewById(R.id.yes_btn);
			viewHolder.noBtn = (TextView) convertView.findViewById(R.id.no_btn);

			viewHolder.avatarFrame = (ImageView) convertView
					.findViewById(R.id.avatar_frame);

			viewHolder.statusImg = (ImageView) convertView
					.findViewById(R.id.status_pic);

			viewHolder.buttonDivider = convertView
					.findViewById(R.id.button_divider);

			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		if (statusMessage.getStatusMessageType() == StatusMessageType.PROTIP) {
			viewHolder.avatar.setImageResource(R.drawable.ic_protip);
		} else if (statusMessage.hasMood()) {
			viewHolder.avatar
					.setImageResource(Utils.getMoodsResource()[statusMessage
							.getMoodId()]);
		} else {
			viewHolder.avatar.setImageDrawable(IconCacheManager.getInstance()
					.getIconForMSISDN(statusMessage.getMsisdn()));
		}
		viewHolder.name
				.setText(userMsisdn.equals(statusMessage.getMsisdn()) ? "Me"
						: statusMessage.getNotNullName());

		viewHolder.mainInfo.setText(statusMessage.getText());

		viewHolder.timeStamp.setText(statusMessage.getTimestampFormatted(true,
				context));

		viewHolder.statusImg.setVisibility(View.GONE);

		viewHolder.buttonDivider.setVisibility(View.VISIBLE);

		int padding = context.getResources().getDimensionPixelSize(
				R.dimen.status_btn_padding);
		viewHolder.noBtn.setPadding(padding, viewHolder.noBtn.getPaddingTop(),
				padding, viewHolder.noBtn.getPaddingTop());
		viewHolder.noBtn.setText(R.string.not_now);

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
					Utils.getFirstName(statusMessage.getNotNullName())));
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
			Linkify.addLinks(viewHolder.mainInfo, Linkify.ALL);
			viewHolder.mainInfo.setMovementMethod(null);
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
			viewHolder.extraInfo.setVisibility(View.GONE);

			viewHolder.mainInfo.setText(context.getString(
					R.string.friend_request_accepted_name,
					Utils.getFirstName(statusMessage.getNotNullName())));
			break;
		case PROTIP:
			Protip protip = statusMessage.getProtip();

			viewHolder.yesBtn.setVisibility(View.GONE);
			viewHolder.buttonDivider.setVisibility(View.GONE);
			viewHolder.timeStamp.setVisibility(View.GONE);

			viewHolder.noBtn.setVisibility(View.VISIBLE);
			viewHolder.noBtn.setText(R.string.dismiss);

			int btnPadding = context.getResources().getDimensionPixelSize(
					R.dimen.protip_btn_padding);
			viewHolder.noBtn.setPadding(btnPadding,
					viewHolder.noBtn.getPaddingTop(), btnPadding,
					viewHolder.noBtn.getPaddingTop());

			if (!TextUtils.isEmpty(protip.getText())) {
				viewHolder.extraInfo.setVisibility(View.VISIBLE);
				viewHolder.extraInfo.setText(protip.getText());
			} else {
				viewHolder.extraInfo.setVisibility(View.GONE);
			}

			if (!TextUtils.isEmpty(protip.getImageURL())) {
				viewHolder.statusImg.setImageDrawable(IconCacheManager
						.getInstance().getIconForMSISDN(protip.getMappedId()));
				viewHolder.statusImg.setVisibility(View.VISIBLE);
			} else {
				viewHolder.statusImg.setVisibility(View.GONE);
			}

			Linkify.addLinks(viewHolder.mainInfo, Linkify.ALL);
			viewHolder.mainInfo.setMovementMethod(null);

			Linkify.addLinks(viewHolder.extraInfo, Linkify.ALL);
			viewHolder.mainInfo.setMovementMethod(null);
			break;
		}

		int avatarDimension;
		if (position < unseenCount) {
			avatarDimension = context.getResources().getDimensionPixelSize(
					R.dimen.medium_avatar);

			viewHolder.avatarFrame
					.setImageResource(R.drawable.frame_avatar_medium_highlight_selector);
		} else {
			avatarDimension = context.getResources().getDimensionPixelSize(
					R.dimen.small_avatar);

			viewHolder.avatarFrame
					.setImageResource(R.drawable.frame_avatar_small_selector);
		}

		LayoutParams layoutParams = (LayoutParams) viewHolder.avatar
				.getLayoutParams();
		layoutParams.height = avatarDimension;
		layoutParams.width = avatarDimension;

		viewHolder.avatar.setLayoutParams(layoutParams);
		viewHolder.avatar.setTag(statusMessage);

		viewHolder.yesBtn.setTag(statusMessage);
		viewHolder.yesBtn.setOnClickListener(yesBtnClickListener);

		viewHolder.noBtn.setTag(statusMessage);
		viewHolder.noBtn.setOnClickListener(noBtnClickListener);

		viewHolder.statusImg.setTag(statusMessage);
		viewHolder.statusImg.setOnClickListener(imageClickListener);

		return convertView;
	}

	private class ViewHolder {
		ImageView avatar;
		TextView name;
		TextView mainInfo;
		TextView extraInfo;
		TextView timeStamp;
		TextView yesBtn;
		TextView noBtn;
		ImageView avatarFrame;
		ImageView statusImg;
		View buttonDivider;
	}

	private OnClickListener imageClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			StatusMessage statusMessage = (StatusMessage) v.getTag();
			String mappedId = statusMessage.getMappedId();
			String url = null;
			if (statusMessage.getStatusMessageType() == StatusMessageType.PROTIP) {
				url = statusMessage.getProtip().getImageURL();
			}

			Bundle arguments = new Bundle();
			arguments.putString(HikeConstants.Extras.MAPPED_ID, mappedId);
			arguments.putString(HikeConstants.Extras.URL, url);
			arguments.putBoolean(HikeConstants.Extras.IS_STATUS_IMAGE, true);

			HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_IMAGE,
					arguments);

		}
	};

	private OnClickListener yesBtnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			StatusMessage statusMessage = (StatusMessage) v.getTag();

			if (CentralTimelineAdapter.EMPTY_STATUS_NO_STATUS_ID == statusMessage
					.getId()
					|| CentralTimelineAdapter.EMPTY_STATUS_NO_STATUS_RECENTLY_ID == statusMessage
							.getId()) {
				context.startActivity(new Intent(context, StatusUpdate.class));
			}
		}
	};

	private OnClickListener noBtnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			StatusMessage statusMessage = (StatusMessage) v.getTag();
			if (statusMessage.getStatusMessageType() == StatusMessageType.PROTIP) {
				/*
				 * Removing the protip
				 */
				statusMessages.remove(0);

				notifyDataSetChanged();

				Editor editor = context.getSharedPreferences(
						HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
				editor.putLong(HikeMessengerApp.PROTIP_DISMISS_TIME,
						System.currentTimeMillis() / 1000);
				editor.putLong(HikeMessengerApp.CURRENT_PROTIP, -1);
				editor.commit();

				HikeMessengerApp.getPubSub().publish(HikePubSub.REMOVE_PROTIP,
						statusMessage.getProtip().getMappedId());
			}
		}
	};

}
