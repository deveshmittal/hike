package com.bsb.hike.adapters;

import java.util.List;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Log;
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
import com.bsb.hike.models.ImageViewerInfo;
import com.bsb.hike.models.Protip;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.tasks.ImageLoader;
import com.bsb.hike.ui.StatusUpdate;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

public class CentralTimelineAdapter extends BaseAdapter {

	public static final long EMPTY_STATUS_NO_STATUS_ID = -3;
	public static final long EMPTY_STATUS_NO_STATUS_RECENTLY_ID = -5;

	private List<StatusMessage> statusMessages;
	private Context context;
	private String userMsisdn;
	private ImageLoader imageLoader;

	private enum ViewType {
		PROFILE_PIC_CHANGE, OTHER_UPDATE
	}

	public CentralTimelineAdapter(Context context,
			List<StatusMessage> statusMessages, String userMsisdn) {
		this.context = context;
		this.statusMessages = statusMessages;
		this.userMsisdn = userMsisdn;
		this.imageLoader = new ImageLoader(context);
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
	public int getViewTypeCount() {
		return ViewType.values().length;
	}

	@Override
	public int getItemViewType(int position) {
		StatusMessage message = getItem(position);
		if (message.getStatusMessageType() == StatusMessageType.PROFILE_PIC) {
			return ViewType.PROFILE_PIC_CHANGE.ordinal();
		}
		return ViewType.OTHER_UPDATE.ordinal();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		ViewType viewType = ViewType.values()[getItemViewType(position)];

		final StatusMessage statusMessage = getItem(position);

		final ViewHolder viewHolder;

		if (convertView == null) {
			viewHolder = new ViewHolder();

			switch (viewType) {

			case OTHER_UPDATE:
				convertView = inflater.inflate(R.layout.timeline_item, null);

				viewHolder.avatar = (ImageView) convertView
						.findViewById(R.id.avatar);
				viewHolder.avatarFrame = (ImageView) convertView
						.findViewById(R.id.avatar_frame);

				viewHolder.name = (TextView) convertView
						.findViewById(R.id.name);
				viewHolder.mainInfo = (TextView) convertView
						.findViewById(R.id.main_info);
				viewHolder.extraInfo = (TextView) convertView
						.findViewById(R.id.details);
				viewHolder.timeStamp = (TextView) convertView
						.findViewById(R.id.timestamp);

				viewHolder.yesBtn = (TextView) convertView
						.findViewById(R.id.yes_btn);
				viewHolder.noBtn = (TextView) convertView
						.findViewById(R.id.no_btn);

				viewHolder.statusImg = (ImageView) convertView
						.findViewById(R.id.status_pic);

				viewHolder.buttonDivider = convertView
						.findViewById(R.id.button_divider);
				break;

			case PROFILE_PIC_CHANGE:
				convertView = inflater.inflate(
						R.layout.profile_pic_timeline_item, null);

				viewHolder.avatar = (ImageView) convertView
						.findViewById(R.id.avatar);

				viewHolder.name = (TextView) convertView
						.findViewById(R.id.name);
				viewHolder.mainInfo = (TextView) convertView
						.findViewById(R.id.main_info);
				viewHolder.largeProfilePic = (ImageView) convertView
						.findViewById(R.id.profile_pic);
				viewHolder.timeStamp = (TextView) convertView
						.findViewById(R.id.timestamp);
				break;
			}
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		switch (viewType) {
		case OTHER_UPDATE:
			if (statusMessage.getStatusMessageType() == StatusMessageType.PROTIP) {
				viewHolder.avatar.setImageResource(R.drawable.ic_protip);
				viewHolder.avatarFrame.setVisibility(View.GONE);
			} else if (statusMessage.hasMood()) {
				viewHolder.avatar
						.setImageResource(EmoticonConstants.moodMapping
								.get(statusMessage.getMoodId()));
				viewHolder.avatarFrame.setVisibility(View.GONE);
			} else {
				viewHolder.avatar.setImageDrawable(IconCacheManager
						.getInstance().getIconForMSISDN(
								statusMessage.getMsisdn(), true));
				viewHolder.avatarFrame.setVisibility(View.VISIBLE);
			}
			viewHolder.name
					.setText(userMsisdn.equals(statusMessage.getMsisdn()) ? "Me"
							: statusMessage.getNotNullName());

			viewHolder.mainInfo.setText(statusMessage.getText());

			viewHolder.timeStamp.setText(statusMessage.getTimestampFormatted(
					true, context));

			viewHolder.statusImg.setVisibility(View.GONE);

			viewHolder.buttonDivider.setVisibility(View.VISIBLE);

			int padding = context.getResources().getDimensionPixelSize(
					R.dimen.status_btn_padding);
			viewHolder.noBtn.setPadding(padding,
					viewHolder.noBtn.getPaddingTop(), padding,
					viewHolder.noBtn.getPaddingTop());
			viewHolder.noBtn.setText(R.string.not_now);

			switch (statusMessage.getStatusMessageType()) {
			case NO_STATUS:
				viewHolder.extraInfo.setVisibility(View.VISIBLE);
				viewHolder.yesBtn.setVisibility(View.VISIBLE);
				viewHolder.noBtn.setVisibility(View.GONE);

				if (EMPTY_STATUS_NO_STATUS_ID == statusMessage.getId()) {
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

				viewHolder.buttonDivider.setVisibility(View.GONE);
				viewHolder.timeStamp.setVisibility(View.GONE);

				viewHolder.noBtn.setVisibility(View.VISIBLE);
				viewHolder.noBtn.setText(R.string.dismiss);
				viewHolder.yesBtn.setText(R.string.download);
				
				int btnPadding = context.getResources().getDimensionPixelSize(
						R.dimen.protip_btn_padding);
				viewHolder.noBtn.setPadding(btnPadding,
						viewHolder.noBtn.getPaddingTop(), btnPadding,
						viewHolder.noBtn.getPaddingTop());
				
				viewHolder.yesBtn.setPadding(btnPadding,
						viewHolder.yesBtn.getPaddingTop(), btnPadding,
						viewHolder.yesBtn.getPaddingTop());

				if (!TextUtils.isEmpty(protip.getText())) {
					viewHolder.extraInfo.setVisibility(View.VISIBLE);
					viewHolder.extraInfo.setText(protip.getText());

				} else {
					viewHolder.extraInfo.setVisibility(View.GONE);
				}

				if (!TextUtils.isEmpty(protip.getImageURL())) {
					viewHolder.statusImg.setImageDrawable(IconCacheManager
							.getInstance().getIconForMSISDN(
									protip.getMappedId()));
					viewHolder.statusImg.setVisibility(View.VISIBLE);

					ImageViewerInfo imageViewerInfo = new ImageViewerInfo(
							statusMessage.getMappedId(), protip.getImageURL(),
							true);
					viewHolder.statusImg.setTag(imageViewerInfo);
					viewHolder.statusImg.setOnClickListener(imageClickListener);
					if (!TextUtils.isEmpty(protip.getGameDownlodURL())) {
						viewHolder.yesBtn.setTag(protip);
						viewHolder.yesBtn.setVisibility(View.VISIBLE);
						viewHolder.yesBtn
								.setOnClickListener(yesBtnClickListener);
					}
				} else {
					viewHolder.statusImg.setVisibility(View.GONE);
				}

				Linkify.addLinks(viewHolder.mainInfo, Linkify.ALL);
				viewHolder.mainInfo.setMovementMethod(null);

				Linkify.addLinks(viewHolder.extraInfo, Linkify.ALL);
				viewHolder.mainInfo.setMovementMethod(null);
				break;
			}

			viewHolder.avatar.setTag(statusMessage);

			// viewHolder.yesBtn.setTag(statusMessage);
			// viewHolder.yesBtn.setOnClickListener(yesBtnClickListener);

			viewHolder.noBtn.setTag(statusMessage);
			viewHolder.noBtn.setOnClickListener(noBtnClickListener);

			break;

		case PROFILE_PIC_CHANGE:
			viewHolder.avatar.setImageDrawable(IconCacheManager.getInstance()
					.getIconForMSISDN(statusMessage.getMsisdn(), true));
			viewHolder.name
					.setText(userMsisdn.equals(statusMessage.getMsisdn()) ? "Me"
							: statusMessage.getNotNullName());
			viewHolder.mainInfo
					.setText(R.string.status_profile_pic_notification);

			ImageViewerInfo imageViewerInfo = new ImageViewerInfo(
					statusMessage.getMappedId(), null, true);

			viewHolder.largeProfilePic.setTag(imageViewerInfo);
			viewHolder.largeProfilePic.setOnClickListener(imageClickListener);

			/*
			 * Fetch larger image
			 */
			imageLoader.loadImage(statusMessage.getMappedId(),
					viewHolder.largeProfilePic);

			viewHolder.timeStamp.setText(statusMessage.getTimestampFormatted(
					true, context));
			break;
		}

		return convertView;
	}

	private class ViewHolder {
		ImageView avatar;
		ImageView avatarFrame;
		TextView name;
		TextView mainInfo;
		TextView extraInfo;
		TextView timeStamp;
		TextView yesBtn;
		TextView noBtn;
		ImageView statusImg;
		View buttonDivider;
		ImageView largeProfilePic;
	}

	private OnClickListener imageClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			ImageViewerInfo imageViewerInfo = (ImageViewerInfo) v.getTag();

			String mappedId = imageViewerInfo.mappedId;
			String url = imageViewerInfo.url;

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
			Protip protip = (Protip) v.getTag();
			String url = protip.getGameDownlodURL();
			Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY
					| Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			try {
				context.startActivity(marketIntent);
			} catch (ActivityNotFoundException e) {
				Log.e(CentralTimelineAdapter.class.getSimpleName(),
						"Unable to open market");
			}
			HikeMessengerApp.getPubSub().publish(HikePubSub.REMOVE_PROTIP,
					protip.getMappedId());
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

	public void stopImageLoaderThread() {
		if (imageLoader == null) {
			return;
		}
		imageLoader.interruptThread();
	}

	public void restartImageLoaderThread() {
		imageLoader = new ImageLoader(context);
	}
}
