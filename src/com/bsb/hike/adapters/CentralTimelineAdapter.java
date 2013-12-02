package com.bsb.hike.adapters;

import java.util.List;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ImageViewerInfo;
import com.bsb.hike.models.Protip;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.tasks.ImageLoader;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.StatusUpdate;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.WhichScreen;

public class CentralTimelineAdapter extends BaseAdapter {

	public static final long EMPTY_STATUS_NO_STATUS_ID = -3;
	public static final long EMPTY_STATUS_NO_STATUS_RECENTLY_ID = -5;
	public static final long FTUE_ITEM_ID = -6;
	private int protipIndex;
	private List<StatusMessage> statusMessages;
	private Context context;
	private String userMsisdn;
	private ImageLoader imageLoader;
	private LayoutInflater inflater;

	private int[] moodsRow1 = { R.drawable.mood_09_chilling,
			R.drawable.mood_35_partying_hard, R.drawable.mood_14_boozing,
			R.drawable.mood_01_happy };

	private int[] moodsRow2 = { R.drawable.mood_15_movie,
			R.drawable.mood_34_music, R.drawable.mood_37_eating,
			R.drawable.mood_03_in_love };

	private int[] moodsRowLand = { R.drawable.mood_09_chilling,
			R.drawable.mood_35_partying_hard, R.drawable.mood_14_boozing,
			R.drawable.mood_01_happy, R.drawable.mood_15_movie,
			R.drawable.mood_34_music, R.drawable.mood_37_eating };

	private enum ViewType {
		PROFILE_PIC_CHANGE, OTHER_UPDATE, FTUE_ITEM
	}

	public CentralTimelineAdapter(Context context,
			List<StatusMessage> statusMessages, String userMsisdn) {
		this.context = context;
		this.statusMessages = statusMessages;
		this.userMsisdn = userMsisdn;
		this.imageLoader = new ImageLoader(context);
		this.inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.protipIndex = -1;
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
		ViewType viewType = ViewType.values()[getItemViewType(position)];
		if (viewType == ViewType.FTUE_ITEM) {
			return false;
		} else if (viewType == ViewType.OTHER_UPDATE) {
			StatusMessage statusMessage = getItem(position);
			if (EMPTY_STATUS_NO_STATUS_ID == statusMessage.getId()
					|| EMPTY_STATUS_NO_STATUS_RECENTLY_ID == statusMessage
							.getId()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int getViewTypeCount() {
		return ViewType.values().length;
	}

	@Override
	public int getItemViewType(int position) {
		StatusMessage message = getItem(position);
		if (message.getId() == FTUE_ITEM_ID) {
			return ViewType.FTUE_ITEM.ordinal();
		} else if (message.getStatusMessageType() == StatusMessageType.PROFILE_PIC) {
			return ViewType.PROFILE_PIC_CHANGE.ordinal();
		}
		return ViewType.OTHER_UPDATE.ordinal();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
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

				viewHolder.infoContainer = convertView
						.findViewById(R.id.btn_container);

				viewHolder.moodsContainer = (ViewGroup) convertView
						.findViewById(R.id.moods_container);

				viewHolder.parent = convertView.findViewById(R.id.main_content);
				break;

			case FTUE_ITEM:
				convertView = inflater
						.inflate(R.layout.ftue_updates_item, null);

				viewHolder.name = (TextView) convertView
						.findViewById(R.id.name);
				viewHolder.mainInfo = (TextView) convertView
						.findViewById(R.id.main_info);

				viewHolder.contactsContainer = (ViewGroup) convertView
						.findViewById(R.id.contacts_container);
				viewHolder.parent = convertView.findViewById(R.id.main_content);

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
				viewHolder.infoContainer = convertView
						.findViewById(R.id.info_container);
				viewHolder.parent = convertView.findViewById(R.id.main_content);
				break;
			}
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		switch (viewType) {
		case OTHER_UPDATE:

			viewHolder.avatar.setScaleType(ScaleType.FIT_CENTER);
			viewHolder.avatar.setBackgroundResource(0);

			if (EMPTY_STATUS_NO_STATUS_ID == statusMessage.getId()
					|| EMPTY_STATUS_NO_STATUS_RECENTLY_ID == statusMessage
							.getId()) {
				viewHolder.avatar.setScaleType(ScaleType.CENTER_INSIDE);
				viewHolder.avatar
						.setImageResource(R.drawable.ic_ftue_moods_tip);
				viewHolder.avatar
						.setBackgroundResource(R.drawable.bg_ftue_updates_tip);
				viewHolder.avatarFrame.setVisibility(View.GONE);
			} else if (statusMessage.getStatusMessageType() == StatusMessageType.PROTIP) {
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

			viewHolder.timeStamp.setVisibility(View.VISIBLE);
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

			viewHolder.infoContainer.setVisibility(View.GONE);
			viewHolder.moodsContainer.setVisibility(View.GONE);

			switch (statusMessage.getStatusMessageType()) {
			case NO_STATUS:
				viewHolder.infoContainer.setVisibility(View.VISIBLE);
				viewHolder.extraInfo.setVisibility(View.VISIBLE);
				viewHolder.yesBtn.setVisibility(View.VISIBLE);
				viewHolder.noBtn.setVisibility(View.GONE);

				if (EMPTY_STATUS_NO_STATUS_ID == statusMessage.getId()
						|| EMPTY_STATUS_NO_STATUS_RECENTLY_ID == statusMessage
								.getId()) {
					viewHolder.timeStamp.setVisibility(View.GONE);

					viewHolder.extraInfo.setText(R.string.no_status);
					viewHolder.yesBtn.setText(R.string.post_a_mood);

					viewHolder.moodsContainer.setVisibility(View.VISIBLE);

					ViewGroup container1 = (ViewGroup) viewHolder.moodsContainer
							.findViewById(R.id.container1);
					ViewGroup container2 = (ViewGroup) viewHolder.moodsContainer
							.findViewById(R.id.container2);

					if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
						addMoods(container1, moodsRow1);
						addMoods(container2, moodsRow2);
					} else {
						addMoods(container1, moodsRowLand);
					}
				}
				viewHolder.yesBtn.setTag(statusMessage);
				viewHolder.yesBtn.setOnClickListener(yesBtnClickListener);
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

				viewHolder.infoContainer.setVisibility(View.VISIBLE);

				viewHolder.buttonDivider.setVisibility(View.GONE);
				viewHolder.timeStamp.setVisibility(View.GONE);

				viewHolder.noBtn.setVisibility(View.VISIBLE);
				viewHolder.noBtn.setText(R.string.dismiss);
				viewHolder.yesBtn.setText(R.string.download);

				if (!TextUtils.isEmpty(protip.getText())) {
					viewHolder.extraInfo.setVisibility(View.VISIBLE);
					viewHolder.extraInfo.setText(protip.getText());

				} else {
					viewHolder.extraInfo.setVisibility(View.GONE);
				}

				if (!TextUtils.isEmpty(protip.getImageURL())) {

					ImageViewerInfo imageViewerInfo = new ImageViewerInfo(
							statusMessage.getMappedId(), protip.getImageURL(),
							true);
					viewHolder.statusImg.setTag(imageViewerInfo);
					viewHolder.statusImg.setOnClickListener(imageClickListener);
					imageLoader.loadImage(protip.getMappedId(),
							viewHolder.statusImg);
					viewHolder.statusImg.setVisibility(View.VISIBLE);
				} else {
					viewHolder.statusImg.setVisibility(View.GONE);
				}
				if (!TextUtils.isEmpty(protip.getGameDownlodURL())) {
					viewHolder.yesBtn.setTag(statusMessage);
					viewHolder.yesBtn.setVisibility(View.VISIBLE);
					viewHolder.buttonDivider.setVisibility(View.VISIBLE);
					viewHolder.yesBtn.setOnClickListener(yesBtnClickListener);
				} else {
					viewHolder.yesBtn.setVisibility(View.GONE);
				}

				Linkify.addLinks(viewHolder.mainInfo, Linkify.ALL);
				viewHolder.mainInfo.setMovementMethod(null);

				Linkify.addLinks(viewHolder.extraInfo, Linkify.ALL);
				viewHolder.mainInfo.setMovementMethod(null);
				break;
			}

			viewHolder.avatar.setTag(statusMessage);

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

			viewHolder.infoContainer.setTag(statusMessage);
			viewHolder.infoContainer
					.setOnClickListener(onProfileInfoClickListener);
			break;

		case FTUE_ITEM:
			viewHolder.name.setText(R.string.friends_ftue_item_label);
			viewHolder.mainInfo.setText(R.string.updates_are_fun_with_friends);

			viewHolder.contactsContainer.removeAllViews();

			int limit = HikeConstants.FTUE_LIMIT;

			for (ContactInfo contactInfo : HomeActivity.ftueList) {
				FavoriteType favoriteType = contactInfo.getFavoriteType();
				if (favoriteType == FavoriteType.FRIEND
						|| favoriteType == FavoriteType.REQUEST_SENT
						|| favoriteType == FavoriteType.REQUEST_SENT_REJECTED
						|| favoriteType == FavoriteType.REQUEST_RECEIVED) {
					continue;
				}

				View parentView = inflater.inflate(
						R.layout.ftue_updates_contact_item, parent, false);

				ImageView avatar = (ImageView) parentView
						.findViewById(R.id.avatar);
				TextView name = (TextView) parentView
						.findViewById(R.id.contact);
				TextView addBtn = (TextView) parentView
						.findViewById(R.id.invite_btn);

				avatar.setImageDrawable(IconCacheManager.getInstance()
						.getIconForMSISDN(contactInfo.getMsisdn(), true));
				name.setText(contactInfo.getName());

				addBtn.setTag(contactInfo);
				addBtn.setOnClickListener(addOnClickListener);

				viewHolder.contactsContainer.addView(parentView);

				if (--limit == 0) {
					break;
				}
			}
			break;
		}

		if (viewHolder.parent != null) {
			int bottomPadding;

			if (position == getCount() - 1) {
				bottomPadding = context.getResources().getDimensionPixelSize(
						R.dimen.updates_margin);
			} else {
				bottomPadding = 0;
			}

			viewHolder.parent.setPadding(0, 0, 0, bottomPadding);
		}

		return convertView;
	}

	private void addMoods(ViewGroup container, int[] moods) {
		container.removeAllViews();

		for (int moodRes : moods) {
			ImageView imageView = (ImageView) inflater.inflate(
					R.layout.ftue_mood_item, container, false);
			imageView.setImageResource(moodRes);

			container.addView(imageView);
		}
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
		View infoContainer;
		View parent;
		ViewGroup contactsContainer;
		ViewGroup moodsContainer;
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
			StatusMessage statusMessage = (StatusMessage) v.getTag();
			if (EMPTY_STATUS_NO_STATUS_ID == statusMessage.getId()
					|| EMPTY_STATUS_NO_STATUS_RECENTLY_ID == statusMessage
							.getId()) {
				Intent intent = new Intent(context, StatusUpdate.class);
				context.startActivity(intent);

				Utils.sendUILogEvent(HikeConstants.LogEvent.POST_UPDATE_FROM_CARD);
			} else if (statusMessage.getStatusMessageType() == StatusMessageType.PROTIP) {
				Protip protip = statusMessage.getProtip();
				String url = protip.getGameDownlodURL();
				Intent marketIntent = new Intent(Intent.ACTION_VIEW,
						Uri.parse(url));
				marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY
						| Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				try {
					context.startActivity(marketIntent);
				} catch (ActivityNotFoundException e) {
					Log.e(CentralTimelineAdapter.class.getSimpleName(),
							"Unable to open market");
				}
				HikeMessengerApp.getPubSub().publish(
						HikePubSub.GAMING_PROTIP_DOWNLOADED, protip);
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
				try {
					statusMessages.remove(getProtipIndex());
					notifyDataSetChanged();

					Editor editor = context.getSharedPreferences(
							HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
					editor.putLong(HikeMessengerApp.CURRENT_PROTIP, -1);
					editor.commit();

					HikeMessengerApp.getPubSub().publish(
							HikePubSub.REMOVE_PROTIP,
							statusMessage.getProtip().getMappedId());
				} catch (IndexOutOfBoundsException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
	};

	private OnClickListener onProfileInfoClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			StatusMessage statusMessage = (StatusMessage) v.getTag();
			if ((statusMessage.getStatusMessageType() == StatusMessageType.NO_STATUS)
					|| (statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST)
					|| (statusMessage.getStatusMessageType() == StatusMessageType.PROTIP)) {
				return;
			} else if (userMsisdn.equals(statusMessage.getMsisdn())) {
				Intent intent = new Intent(context, ProfileActivity.class);
				intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE,
						true);
				context.startActivity(intent);
				return;
			}

			Intent intent = Utils.createIntentFromContactInfo(
					new ContactInfo(null, statusMessage.getMsisdn(),
							statusMessage.getNotNullName(), statusMessage
									.getMsisdn()), true);
			intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
			intent.setClass(context, ChatThread.class);
			context.startActivity(intent);
		}
	};

	private OnClickListener addOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			ContactInfo contactInfo = (ContactInfo) v.getTag();

			FavoriteType favoriteType;
			if (contactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED) {
				favoriteType = FavoriteType.FRIEND;
			} else {
				favoriteType = FavoriteType.REQUEST_SENT;
				Toast.makeText(context, R.string.friend_request_sent,
						Toast.LENGTH_SHORT).show();
			}

			/*
			 * Cloning the object since we don't want to send the ftue
			 * reference.
			 */
			ContactInfo contactInfo2 = new ContactInfo(contactInfo);

			Pair<ContactInfo, FavoriteType> favoriteAdded = new Pair<ContactInfo, FavoriteType>(
					contactInfo2, favoriteType);
			HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_TOGGLED,
					favoriteAdded);

			Utils.sendUILogEvent(HikeConstants.LogEvent.ADD_UPDATES_CLICK,
					contactInfo2.getMsisdn());

			if (!contactInfo.isOnhike())
				Utils.sendInviteUtil(
						contactInfo2,
						context,
						HikeConstants.FTUE_ADD_SMS_ALERT_CHECKED,
						context.getString(R.string.ftue_add_prompt_invite_title),
						context.getString(R.string.ftue_add_prompt_invite),
						WhichScreen.UPDATES_TAB);
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

	/**
	 * @return the protipIndex
	 */
	public int getProtipIndex() {
		return protipIndex;
	}

	/**
	 * @param protipIndex
	 *            the protipIndex to set
	 */
	public void setProtipIndex(int protipIndex) {
		this.protipIndex = protipIndex;
	}
}
