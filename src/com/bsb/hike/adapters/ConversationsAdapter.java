package com.bsb.hike.adapters;

import java.util.List;

import org.json.JSONArray;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

public class ConversationsAdapter extends ArrayAdapter<Conversation> {

	private int mResourceId;

	public ConversationsAdapter(Context context, int textViewResourceId,
			List<Conversation> objects) {
		super(context, textViewResourceId, objects);
		this.mResourceId = textViewResourceId;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Context context = getContext();
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		Conversation conversation = getItem(position);

		View v = convertView;
		if (v == null) {
			v = inflater.inflate(mResourceId, parent, false);
		}

		TextView contactView = (TextView) v.findViewById(R.id.contact);
		String name = conversation.getLabel();

		contactView.setText(name);
		if (conversation instanceof GroupConversation) {
			contactView.setCompoundDrawablesWithIntrinsicBounds(
					R.drawable.ic_group, 0, 0, 0);
		} else {
			contactView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
		}
		List<ConvMessage> messages = conversation.getMessages();
		if (!messages.isEmpty()) {
			ConvMessage message = messages.get(messages.size() - 1);

			ImageView avatarframe = (ImageView) v
					.findViewById(R.id.avatar_frame);

			ImageView imgStatus = (ImageView) v
					.findViewById(R.id.msg_status_indicator);

			TextView unreadIndicator = (TextView) v
					.findViewById(R.id.unread_indicator);
			unreadIndicator.setVisibility(View.GONE);
			imgStatus.setVisibility(View.GONE);
			/*
			 * If the message is a status message, we only show an indicator if
			 * the status of the message is unread.
			 */
			if (message.getParticipantInfoState() != ParticipantInfoState.STATUS_MESSAGE
					|| message.getState() == State.RECEIVED_UNREAD) {
				int resId = message.getImageState();
				if (resId > 0) {
					avatarframe
							.setImageResource(R.drawable.frame_avatar_large_selector);
					imgStatus.setImageResource(resId);
					imgStatus.setVisibility(View.VISIBLE);
				} else if (message.getState() == ConvMessage.State.RECEIVED_UNREAD
						&& (message.getMsgID() > -1 || message.getMappedMsgID() > -1)) {
					avatarframe
							.setImageResource(R.drawable.frame_avatar_large_highlight_selector);
					unreadIndicator.setVisibility(View.VISIBLE);

					if (conversation.getUnreadCount() == 0) {
						unreadIndicator.setText("");
					} else {
						unreadIndicator.setText(Integer.toString(conversation
								.getUnreadCount()));
					}
				} else {
					avatarframe
							.setImageResource(R.drawable.frame_avatar_large_selector);
				}
			}

			TextView messageView = (TextView) v.findViewById(R.id.last_message);

			MessageMetadata metadata = message.getMetadata();

			CharSequence markedUp = null;
			if (message.isFileTransferMessage()) {
				markedUp = HikeFileType.getFileTypeMessage(context, metadata
						.getHikeFiles().get(0).getHikeFileType(),
						message.isSent());
				if ((conversation instanceof GroupConversation)
						&& !message.isSent()) {
					markedUp = Utils.addContactName(
							((GroupConversation) conversation)
									.getGroupParticipantFirstName(message
											.getGroupParticipantMsisdn()),
							markedUp);
				}
			} else if (message.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_JOINED) {
				JSONArray participantInfoArray = metadata
						.getGcjParticipantInfo();

				String highlight = Utils.getGroupJoinHighlightText(
						participantInfoArray, (GroupConversation) conversation);

				if (metadata.isNewGroup()) {
					markedUp = String.format(
							context.getString(R.string.new_group_message),
							highlight);
				} else {
					markedUp = String.format(
							context.getString(R.string.add_to_group_message),
							highlight);
				}
			} else if (message.getParticipantInfoState() == ParticipantInfoState.DND_USER) {
				JSONArray dndNumbers = metadata.getDndNumbers();
				if (dndNumbers != null && dndNumbers.length() > 0) {
					StringBuilder dndNames = new StringBuilder();
					for (int i = 0; i < dndNumbers.length(); i++) {
						String dndName;
						dndName = conversation instanceof GroupConversation ? ((GroupConversation) conversation)
								.getGroupParticipantFirstName(dndNumbers
										.optString(i)) : Utils
								.getFirstName(conversation.getLabel());
						if (i < dndNumbers.length() - 2) {
							dndNames.append(dndName + ", ");
						} else if (i < dndNumbers.length() - 1) {
							dndNames.append(dndName + " and ");
						} else {
							dndNames.append(dndName);
						}
					}
					markedUp = String
							.format(context
									.getString(conversation instanceof GroupConversation ? R.string.dnd_msg_gc
											: R.string.dnd_one_to_one),
									dndNames.toString());
				}
			} else if (message.getParticipantInfoState() == ParticipantInfoState.INTRO_MESSAGE) {
				if (conversation.isOnhike()) {
					boolean firstIntro = conversation.getMsisdn().hashCode() % 2 == 0;
					markedUp = String.format(context
							.getString(firstIntro ? R.string.start_thread1
									: R.string.start_thread1), Utils
							.getFirstName(conversation.getLabel()));
				} else {
					markedUp = String.format(
							context.getString(R.string.intro_sms_thread),
							Utils.getFirstName(conversation.getLabel()));
				}
			} else if (message.getParticipantInfoState() == ParticipantInfoState.USER_JOIN) {
				String participantName;
				if (conversation instanceof GroupConversation) {
					String participantMsisdn = metadata.getMsisdn();
					participantName = ((GroupConversation) conversation)
							.getGroupParticipantFirstName(participantMsisdn);
				} else {
					participantName = Utils.getFirstName(conversation
							.getLabel());
				}
				markedUp = context.getString(
						metadata.isOldUser() ? R.string.user_back_on_hike
								: R.string.joined_hike_new, participantName);

			} else if (message.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_LEFT
					|| message.getParticipantInfoState() == ParticipantInfoState.GROUP_END) {

				if (message.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_LEFT) {
					// Showing the block internation sms message if the user was
					// booted because of that reason
					String participantMsisdn = metadata.getMsisdn();
					String participantName = ((GroupConversation) conversation)
							.getGroupParticipantFirstName(participantMsisdn);
					markedUp = String.format(
							context.getString(R.string.left_conversation),
							participantName);
				} else {
					markedUp = context.getString(R.string.group_chat_end);
				}
			} else if (message.getParticipantInfoState() == ParticipantInfoState.CHANGED_GROUP_NAME) {
				String msisdn = metadata.getMsisdn();

				String userMsisdn = context.getSharedPreferences(
						HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(
						HikeMessengerApp.MSISDN_SETTING, "");

				String participantName = userMsisdn.equals(msisdn) ? context
						.getString(R.string.you)
						: ((GroupConversation) conversation)
								.getGroupParticipantFirstName(msisdn);

				markedUp = String.format(
						context.getString(R.string.change_group_name),
						participantName);
			} else if (message.getParticipantInfoState() == ParticipantInfoState.BLOCK_INTERNATIONAL_SMS) {
				markedUp = context.getString(R.string.block_internation_sms);
			} else if (message.getParticipantInfoState() == ParticipantInfoState.CHAT_BACKGROUND) {
				String msisdn = metadata.getMsisdn();
				String userMsisdn = context.getSharedPreferences(
						HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(
						HikeMessengerApp.MSISDN_SETTING, "");

				String nameString;
				if (conversation instanceof GroupConversation) {
					nameString = userMsisdn.equals(msisdn) ? context
							.getString(R.string.you)
							: ((GroupConversation) conversation)
									.getGroupParticipantFirstName(msisdn);
				} else {
					nameString = userMsisdn.equals(msisdn) ? context
							.getString(R.string.you) : Utils
							.getFirstName(conversation.getLabel());
				}

				markedUp = context.getString(R.string.chat_bg_changed,
						nameString);
			} else {
				String msg = message.getMessage();
				markedUp = msg.substring(0, Math.min(msg.length(),
						HikeConstants.MAX_MESSAGE_PREVIEW_LENGTH));
				// For showing the name of the contact that sent the message in
				// a group chat
				if (conversation instanceof GroupConversation
						&& !TextUtils.isEmpty(message
								.getGroupParticipantMsisdn())
						&& message.getParticipantInfoState() == ParticipantInfoState.NO_INFO) {
					markedUp = Utils.addContactName(
							((GroupConversation) conversation)
									.getGroupParticipantFirstName(message
											.getGroupParticipantMsisdn()),
							markedUp);
				}
				SmileyParser smileyParser = SmileyParser.getInstance();
				markedUp = smileyParser.addSmileySpans(markedUp, true);
			}
			messageView.setVisibility(View.VISIBLE);
			messageView.setText(markedUp);
			TextView tsView = (TextView) v
					.findViewById(R.id.last_message_timestamp);
			tsView.setText(message.getTimestampFormatted(true, context));
			if (message.getState() == ConvMessage.State.RECEIVED_UNREAD) {
				/* set unread messages to BLUE */
				messageView.setTextColor(context.getResources().getColor(
						R.color.unread_message));
			} else {
				messageView.setTextColor(context.getResources().getColor(
						R.color.list_item_header));
			}
		}

		ImageView avatarView = (ImageView) v.findViewById(R.id.avatar);
		avatarView.setImageDrawable(IconCacheManager.getInstance()
				.getIconForMSISDN(conversation.getMsisdn(), true));

		return v;
	}

}
