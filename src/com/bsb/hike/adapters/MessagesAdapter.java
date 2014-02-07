package com.bsb.hike.adapters;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.TipType;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfoData;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupTypingNotification;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.smartImageLoader.StickerLoader;
import com.bsb.hike.tasks.DownloadFileTask;
import com.bsb.hike.tasks.DownloadSingleStickerTask;
import com.bsb.hike.tasks.UploadContactOrLocationTask;
import com.bsb.hike.tasks.UploadFileTask;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.FileTransferTaskBase;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.StickerManager.StickerCategoryId;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;
import com.bsb.hike.view.CircularProgress;

public class MessagesAdapter extends BaseAdapter implements OnClickListener,
		OnLongClickListener, OnCheckedChangeListener {

	public static final int LAST_READ_CONV_MESSAGE_ID = -911;

	private enum ViewType {
		RECEIVE, SEND_SMS, SEND_HIKE, PARTICIPANT_INFO, FILE_TRANSFER_SEND, FILE_TRANSFER_RECEIVE, LAST_READ, STATUS_MESSAGE, SMS_TOGGLE
	};

	private class ViewHolder {
		LinearLayout dayContainer;
		TextView messageTextView;
		TextView dayTextView;
		ImageView image;
		ImageView avatarFrame;
		ViewGroup container;
		ImageView fileThumb;
		ImageView showFileBtn;
		CircularProgress circularProgress;
		View marginView;
		TextView participantNameFT;
		View loadingThumb;
		ImageView poke;
		View messageContainer;
		TextView messageInfo;
		CheckBox smsToggle;
		TextView hikeSmsText;
		TextView regularSmsText;
		View stickerPlaceholder;
		ProgressBar stickerLoader;
		TextView stickerParticipantName;
		ImageView stickerImage;
		View bubbleContainer;
		ImageView sending;
		ImageView typing;
		ViewGroup avatarContainer;
		ViewGroup typingAvatarContainer;
		View dayLeft;
		View dayRight;
		ImageView pokeCustom;
	}

	private Conversation conversation;
	private ArrayList<ConvMessage> convMessages;
	private Context context;
	private ChatThread chatThread;
	private TextView smsToggleSubtext;
	private TextView hikeSmsText;
	private TextView regularSmsText;
	private ShowUndeliveredMessage showUndeliveredMessage;
	private int lastSentMessagePosition = -1;
	private VoiceMessagePlayer voiceMessagePlayer;
	private String statusIdForTip;
	private SharedPreferences preferences;
	private boolean isGroupChat;
	private ChatTheme chatTheme;
	private boolean isDefaultTheme = true;
	private IconLoader iconLoader;
	private StickerLoader largeStickerLoader;
	private int mIconImageSize;

	public MessagesAdapter(Context context, ArrayList<ConvMessage> objects,
			Conversation conversation, ChatThread chatThread) {
		mIconImageSize = context.getResources().getDimensionPixelSize(R.dimen.icon_picture_size);
		this.largeStickerLoader = new StickerLoader(context);
		this.iconLoader = new IconLoader(context,mIconImageSize);
		this.context = context;
		this.convMessages = objects;
		this.conversation = conversation;
		this.chatThread = chatThread;
		this.voiceMessagePlayer = new VoiceMessagePlayer();
		this.preferences = context.getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		this.isGroupChat = Utils.isGroupConversation(conversation.getMsisdn());
		this.chatTheme = ChatTheme.DEFAULT;
		setLastSentMessagePosition();
	}

	public void setChatTheme(ChatTheme theme) {
		chatTheme = theme;
		isDefaultTheme = chatTheme == ChatTheme.DEFAULT;
		notifyDataSetChanged();
	}

	public boolean isDefaultTheme() {
		return isDefaultTheme;
	}

	public void addMessage(ConvMessage convMessage) {
		convMessages.add(convMessage);
		if (convMessage != null && convMessage.isSent()) {
			lastSentMessagePosition = convMessages.size() - 1;
		}
	}

	public void addMessage(ConvMessage convMessage, int index) {
		convMessages.add(index, convMessage);
		if (index > lastSentMessagePosition) {
			if (convMessage != null && convMessage.isSent()) {
				lastSentMessagePosition = index;
			}
		} else {
			lastSentMessagePosition++;
		}
	}

	public void addMessages(List<ConvMessage> oldConvMessages, int index) {
		convMessages.addAll(index, oldConvMessages);

		if (lastSentMessagePosition >= index) {
			lastSentMessagePosition += oldConvMessages.size();
		}
	}

	public void removeMessage(ConvMessage convMessage) {
		int index = convMessages.indexOf(convMessage);
		convMessages.remove(convMessage);
		/*
		 * We need to update the last sent position
		 */
		if (index == lastSentMessagePosition) {
			setLastSentMessagePosition();
		} else if (index < lastSentMessagePosition) {
			lastSentMessagePosition--;
		}
	}

	public void removeMessage(int index) {
		convMessages.remove(index);
		/*
		 * We need to update the last sent position
		 */
		if (index == lastSentMessagePosition) {
			setLastSentMessagePosition();
		} else if (index < lastSentMessagePosition) {
			lastSentMessagePosition--;
		}
	}

	private void setLastSentMessagePosition() {
		AsyncTask<Void, Void, Void> getLastSentMessagePositionTask = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				lastSentMessagePosition = -1;
				for (int i = convMessages.size() - 1; i >= 0; i--) {
					ConvMessage convMessage = convMessages.get(i);
					if (convMessage == null) {
						continue;
					}
					if (convMessage.isSent()) {
						lastSentMessagePosition = i;
						break;
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				Log.d(getClass().getSimpleName(), "Last Postion: "
						+ lastSentMessagePosition);
				if (lastSentMessagePosition == -1) {
					return;
				}
				notifyDataSetChanged();
			}
		};
		Utils.executeAsyncTask(getLastSentMessagePositionTask);
	}

	/**
	 * Returns what type of View this item is going to result in * @return an
	 * integer
	 */
	@Override
	public int getItemViewType(int position) {
		ConvMessage convMessage = getItem(position);
		ViewType type;
		if (convMessage.getTypingNotification() != null) {
			type = ViewType.RECEIVE;
		} else if (convMessage.getMsgID() == ConvMessage.SMS_TOGGLE_ID) {
			type = ViewType.SMS_TOGGLE;
		} else if (convMessage.getMsgID() == LAST_READ_CONV_MESSAGE_ID) {
			type = ViewType.LAST_READ;
		} else if (convMessage.isFileTransferMessage()) {
			type = convMessage.isSent() ? ViewType.FILE_TRANSFER_SEND
					: ViewType.FILE_TRANSFER_RECEIVE;
		} else if (convMessage.getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE) {
			type = ViewType.STATUS_MESSAGE;
		} else if (convMessage.getParticipantInfoState() != ParticipantInfoState.NO_INFO) {
			type = ViewType.PARTICIPANT_INFO;
		} else if (convMessage.isSent()) {
			type = conversation.isOnhike() ? ViewType.SEND_HIKE
					: ViewType.SEND_SMS;
		} else {
			type = ViewType.RECEIVE;
		}

		return type.ordinal();
	}

	/**
	 * Returns how many distinct types of views this adapter creates. This is
	 * used to reuse the view (via convertView in getView)
	 * 
	 * @return how many distinct views this adapter will create
	 */
	@Override
	public int getViewTypeCount() {
		return ViewType.values().length;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ViewType viewType = ViewType.values()[getItemViewType(position)];

		final ConvMessage convMessage = getItem(position);
		ViewHolder holder = null;
		View v = convertView;
		if (v == null) {
			holder = new ViewHolder();

			switch (viewType) {
			case LAST_READ:
				v = inflater.inflate(R.layout.last_read_line, null);
				break;
			case STATUS_MESSAGE:
				v = inflater.inflate(R.layout.in_thread_status_update, null);

				holder.image = (ImageView) v.findViewById(R.id.avatar);
				holder.avatarFrame = (ImageView) v
						.findViewById(R.id.avatar_frame);
				holder.messageTextView = (TextView) v
						.findViewById(R.id.status_text);
				holder.dayTextView = (TextView) v
						.findViewById(R.id.status_info);
				holder.container = (ViewGroup) v
						.findViewById(R.id.content_container);
				holder.messageInfo = (TextView) v.findViewById(R.id.timestamp);
				break;
			case PARTICIPANT_INFO:
				v = inflater.inflate(R.layout.message_item_receive, null);

				holder.image = (ImageView) v.findViewById(R.id.avatar);
				holder.dayContainer = (LinearLayout) v
						.findViewById(R.id.day_container);
				holder.dayTextView = (TextView) v.findViewById(R.id.day);
				holder.container = (ViewGroup) v
						.findViewById(R.id.participant_info_container);
				holder.dayLeft = v.findViewById(R.id.day_left);
				holder.dayRight = v.findViewById(R.id.day_right);

				holder.image.setVisibility(View.GONE);
				v.findViewById(R.id.receive_message_container).setVisibility(
						View.GONE);
				break;

			case FILE_TRANSFER_SEND:
				v = inflater.inflate(R.layout.message_item_send, parent, false);

				holder.fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
				holder.circularProgress = (CircularProgress) v
						.findViewById(R.id.file_transfer_progress);
				holder.marginView = v.findViewById(R.id.margin_view);
				holder.loadingThumb = v.findViewById(R.id.loading_thumb);
				holder.showFileBtn = (ImageView) v
						.findViewById(R.id.btn_open_file);
				holder.image = (ImageView) v
						.findViewById(R.id.msg_status_indicator);

				showFileTransferElements(holder);
			case SEND_HIKE:
			case SEND_SMS:
				if (v == null) {
					v = inflater.inflate(R.layout.message_item_send, parent,
							false);
				}

				holder.dayContainer = (LinearLayout) v
						.findViewById(R.id.day_container);
				holder.dayLeft = v.findViewById(R.id.day_left);
				holder.dayRight = v.findViewById(R.id.day_right);
				holder.dayTextView = (TextView) v.findViewById(R.id.day);
				holder.poke = (ImageView) v.findViewById(R.id.poke_sent);
				holder.pokeCustom = (ImageView) v
						.findViewById(R.id.poke_sent_custom);
				holder.messageContainer = v
						.findViewById(R.id.sent_message_container);

				holder.messageTextView = (TextView) v
						.findViewById(R.id.message_send);

				holder.messageInfo = (TextView) v.findViewById(R.id.msg_info);

				holder.stickerPlaceholder = v
						.findViewById(R.id.sticker_placeholder);
				holder.stickerLoader = (ProgressBar) v
						.findViewById(R.id.loading_progress);
				holder.stickerParticipantName = (TextView) v
						.findViewById(R.id.participant_name);
				holder.stickerImage = (ImageView) v
						.findViewById(R.id.sticker_image);
				holder.bubbleContainer = v.findViewById(R.id.bubble_container);
				holder.sending = (ImageView) v.findViewById(R.id.sending_anim);
				break;

			case FILE_TRANSFER_RECEIVE:
				v = inflater.inflate(R.layout.message_item_receive, parent,
						false);

				holder.fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
				holder.showFileBtn = (ImageView) v
						.findViewById(R.id.btn_open_file);
				holder.circularProgress = (CircularProgress) v
						.findViewById(R.id.file_transfer_progress);
				holder.messageTextView = (TextView) v
						.findViewById(R.id.message_receive_ft);
				holder.messageTextView.setVisibility(View.VISIBLE);

				holder.circularProgress.setVisibility(View.INVISIBLE);
				showFileTransferElements(holder);

				v.findViewById(R.id.message_receive).setVisibility(View.GONE);
			case RECEIVE:
				if (v == null) {
					v = inflater.inflate(R.layout.message_item_receive, parent,
							false);
				}

				holder.participantNameFT = (TextView) v
						.findViewById(R.id.participant_name_ft);
				holder.image = (ImageView) v.findViewById(R.id.avatar);
				holder.avatarContainer = (ViewGroup) v
						.findViewById(R.id.avatar_container);
				if (holder.messageTextView == null) {
					holder.messageTextView = (TextView) v
							.findViewById(R.id.message_receive);
				}
				holder.poke = (ImageView) v.findViewById(R.id.poke_receive);
				holder.pokeCustom = (ImageView) v
						.findViewById(R.id.poke_receive_custom);
				holder.messageContainer = v
						.findViewById(R.id.receive_message_container);
				holder.dayContainer = (LinearLayout) v
						.findViewById(R.id.day_container);
				holder.dayTextView = (TextView) v.findViewById(R.id.day);
				holder.dayLeft = v.findViewById(R.id.day_left);
				holder.dayRight = v.findViewById(R.id.day_right);
				holder.container = (ViewGroup) v
						.findViewById(R.id.participant_info_container);
				holder.stickerPlaceholder = v
						.findViewById(R.id.sticker_placeholder);
				holder.stickerLoader = (ProgressBar) v
						.findViewById(R.id.loading_progress);
				holder.stickerParticipantName = (TextView) v
						.findViewById(R.id.participant_name);
				holder.stickerImage = (ImageView) v
						.findViewById(R.id.sticker_image);
				holder.bubbleContainer = v.findViewById(R.id.bubble_container);

				holder.messageInfo = (TextView) v.findViewById(R.id.msg_info);

				holder.typing = (ImageView) v.findViewById(R.id.typing);

				holder.typingAvatarContainer = (ViewGroup) v
						.findViewById(R.id.typing_avatar_container);

				holder.container.setVisibility(View.GONE);

				break;
			case SMS_TOGGLE:
				v = inflater.inflate(R.layout.sms_toggle_item, parent, false);

				holder.messageTextView = (TextView) v
						.findViewById(R.id.sms_toggle_subtext);
				holder.smsToggle = (CheckBox) v.findViewById(R.id.checkbox);
				holder.hikeSmsText = (TextView) v.findViewById(R.id.hike_text);
				holder.regularSmsText = (TextView) v
						.findViewById(R.id.sms_text);
			}
			v.setTag(holder);
		} else {
			holder = (ViewHolder) v.getTag();
		}

		if (viewType == ViewType.RECEIVE) {
			holder.typing.setVisibility(View.GONE);
			holder.typingAvatarContainer.setVisibility(View.GONE);
		}
		if (convMessage.getTypingNotification() != null) {
			holder.image.setVisibility(View.GONE);
			holder.avatarContainer.setVisibility(View.GONE);
			holder.typing.setVisibility(View.VISIBLE);
			holder.messageContainer.setVisibility(View.GONE);
			holder.dayContainer.setVisibility(View.GONE);
			holder.stickerPlaceholder.setVisibility(View.GONE);
			holder.poke.setVisibility(View.GONE);
			holder.pokeCustom.setVisibility(View.GONE);

			holder.messageInfo.setVisibility(View.INVISIBLE);

			AnimationDrawable ad = (AnimationDrawable) holder.typing
					.getDrawable();
			ad.setCallback(holder.typing);
			ad.setVisible(true, true);
			ad.start();

			if (isGroupChat) {
				holder.typingAvatarContainer.setVisibility(View.VISIBLE);

				GroupTypingNotification groupTypingNotification = (GroupTypingNotification) convMessage
						.getTypingNotification();
				List<String> participantList = groupTypingNotification
						.getGroupParticipantList();

				holder.typingAvatarContainer.removeAllViews();
				for (int i = participantList.size() - 1; i >= 0; i--) {
					View avatarContainer = inflater.inflate(
							R.layout.small_avatar_container,
							holder.typingAvatarContainer, false);

					ImageView imageView = (ImageView) avatarContainer
							.findViewById(R.id.avatar);
					/*
					 * Catching OOB here since the participant list can be
					 * altered by another thread. In that case an OOB will be
					 * thrown here. The only impact that will have is that the
					 * image which has been removed will be skipped.
					 */
					try {
						iconLoader.loadImage(participantList.get(i),true ,imageView,true);
						holder.typingAvatarContainer.addView(avatarContainer);
					} catch (IndexOutOfBoundsException e) {

					}
				}
			}
			return v;
		}

		if (viewType == ViewType.SMS_TOGGLE) {
			smsToggleSubtext = holder.messageTextView;
			hikeSmsText = holder.hikeSmsText;
			regularSmsText = holder.regularSmsText;

			if (isDefaultTheme) {
				holder.hikeSmsText.setTextColor(context.getResources()
						.getColor(R.color.sms_choice_unselected));
				holder.regularSmsText.setTextColor(context.getResources()
						.getColor(R.color.sms_choice_unselected));
				holder.messageTextView.setTextColor(context.getResources()
						.getColor(R.color.sms_choice_unselected));
				holder.smsToggle
						.setButtonDrawable(R.drawable.sms_checkbox);
				v.setBackgroundResource(R.drawable.bg_sms_toggle);
			} else {
				holder.hikeSmsText.setTextColor(context.getResources()
						.getColor(R.color.white));
				holder.regularSmsText.setTextColor(context.getResources()
						.getColor(R.color.white));
				holder.messageTextView.setTextColor(context.getResources()
						.getColor(R.color.white));
				holder.smsToggle
						.setButtonDrawable(R.drawable.sms_checkbox_custom_theme);
				v.setBackgroundResource(R.drawable.bg_sms_toggle_custom_theme);
			}

			boolean smsToggleOn = Utils.getSendSmsPref(context);
			holder.smsToggle.setChecked(smsToggleOn);
			setSmsToggleSubtext(smsToggleOn);

			holder.smsToggle.setOnCheckedChangeListener(this);
			return v;
		}

		if (showDayIndicator(position)) {
			String dateFormatted = convMessage.getMessageDate(context);
			holder.dayTextView.setText(dateFormatted.toUpperCase());
			holder.dayContainer.setVisibility(View.VISIBLE);

			if (isDefaultTheme) {
				holder.dayTextView.setTextColor(context.getResources()
						.getColor(R.color.list_item_header));
				holder.dayLeft.setBackgroundColor(context.getResources()
						.getColor(R.color.day_line));
				holder.dayRight.setBackgroundColor(context.getResources()
						.getColor(R.color.day_line));
			} else {
				holder.dayTextView.setTextColor(context.getResources()
						.getColor(R.color.white));
				holder.dayLeft.setBackgroundColor(context.getResources()
						.getColor(R.color.white));
				holder.dayRight.setBackgroundColor(context.getResources()
						.getColor(R.color.white));
			}
		} else {
			if (holder.dayContainer != null) {
				holder.dayContainer.setVisibility(View.GONE);
			}
		}

		ParticipantInfoState infoState = convMessage.getParticipantInfoState();
		if ((infoState != ParticipantInfoState.NO_INFO)
				&& (infoState != ParticipantInfoState.STATUS_MESSAGE)) {
			((ViewGroup) holder.container).removeAllViews();
			int positiveMargin = (int) (8 * Utils.densityMultiplier);
			int left = 0;
			int top = 0;
			int right = 0;
			int bottom = positiveMargin;

			int layoutRes = isDefaultTheme ? R.layout.participant_info
					: R.layout.participant_info_custom;
			MessageMetadata metadata = convMessage.getMetadata();
			if (infoState == ParticipantInfoState.PARTICIPANT_JOINED) {
				JSONArray participantInfoArray = metadata
						.getGcjParticipantInfo();

				TextView participantInfo = (TextView) inflater.inflate(
						layoutRes, null);

				String message;
				String highlight = Utils.getGroupJoinHighlightText(
						participantInfoArray, (GroupConversation) conversation);

				if (metadata.isNewGroup()) {
					message = String.format(
							context.getString(R.string.new_group_message),
							highlight);
				} else {
					message = String.format(
							context.getString(R.string.add_to_group_message),
							highlight);
				}

				setTextAndIconForSystemMessages(participantInfo,
						Utils.getFormattedParticipantInfo(message, highlight),
						isDefaultTheme ? R.drawable.ic_joined_chat
								: R.drawable.ic_joined_chat_custom);

				((ViewGroup) holder.container).addView(participantInfo);
			} else if (infoState == ParticipantInfoState.PARTICIPANT_LEFT
					|| infoState == ParticipantInfoState.GROUP_END) {
				TextView participantInfo = (TextView) inflater.inflate(
						layoutRes, null);

				CharSequence message;
				if (infoState == ParticipantInfoState.PARTICIPANT_LEFT) {
					// Showing the block internation sms message if the user was
					// booted because of that reason
					if (metadata.isShowBIS()) {
						String info = context
								.getString(R.string.block_internation_sms);
						String textToHighlight = context
								.getString(R.string.block_internation_sms_bold_text);

						TextView mainMessage = (TextView) inflater.inflate(
								layoutRes, null);
						setTextAndIconForSystemMessages(mainMessage,
								Utils.getFormattedParticipantInfo(info,
										textToHighlight),
								R.drawable.ic_no_int_sms);

						LayoutParams lp = new LayoutParams(
								LayoutParams.MATCH_PARENT,
								LayoutParams.WRAP_CONTENT);
						lp.setMargins(left, top, right, bottom);
						mainMessage.setLayoutParams(lp);

						((ViewGroup) holder.container).addView(mainMessage);
					}
					String participantMsisdn = metadata.getMsisdn();
					String name = ((GroupConversation) conversation)
							.getGroupParticipantFirstName(participantMsisdn);
					message = Utils.getFormattedParticipantInfo(
							String.format(context
									.getString(R.string.left_conversation),
									name), name);
				} else {
					message = context.getString(R.string.group_chat_end);
				}
				setTextAndIconForSystemMessages(participantInfo, message,
						isDefaultTheme ? R.drawable.ic_left_chat
								: R.drawable.ic_left_chat_custom);

				LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,
						LayoutParams.WRAP_CONTENT);
				lp.setMargins(left, top, right, 0);
				participantInfo.setLayoutParams(lp);

				((ViewGroup) holder.container).addView(participantInfo);
			} else if (infoState == ParticipantInfoState.USER_JOIN
					|| infoState == ParticipantInfoState.USER_OPT_IN) {
				String name;
				String message;
				if (conversation instanceof GroupConversation) {
					String participantMsisdn = metadata.getMsisdn();
					name = ((GroupConversation) conversation)
							.getGroupParticipantFirstName(participantMsisdn);
					message = context
							.getString(
									infoState == ParticipantInfoState.USER_JOIN ? (metadata
											.isOldUser() ? R.string.user_back_on_hike
											: R.string.joined_hike_new)
											: R.string.joined_conversation,
									name);
				} else {
					name = Utils.getFirstName(conversation.getLabel());
					message = context
							.getString(
									infoState == ParticipantInfoState.USER_JOIN ? (metadata
											.isOldUser() ? R.string.user_back_on_hike
											: R.string.joined_hike_new)
											: R.string.optin_one_to_one, name);
				}

				int icRes;
				if (infoState == ParticipantInfoState.USER_JOIN) {
					icRes = isDefaultTheme ? R.drawable.ic_user_join
							: R.drawable.ic_user_join_custom;
				} else {
					icRes = isDefaultTheme ? R.drawable.ic_opt_in
							: R.drawable.ic_opt_in_custom;
				}

				TextView mainMessage = (TextView) inflater.inflate(layoutRes,
						null);
				setTextAndIconForSystemMessages(mainMessage,
						Utils.getFormattedParticipantInfo(message, name), icRes);

				TextView creditsMessageView = null;
				if (metadata.getCredits() != -1) {
					int credits = metadata.getCredits();
					String creditsMessage = String.format(context.getString(
							R.string.earned_credits, credits));
					String highlight = String.format(context.getString(
							R.string.earned_credits_highlight, credits));

					creditsMessageView = (TextView) inflater.inflate(layoutRes,
							null);
					setTextAndIconForSystemMessages(creditsMessageView,
							Utils.getFormattedParticipantInfo(creditsMessage,
									highlight), R.drawable.ic_got_credits);

					LayoutParams lp = new LayoutParams(
							LayoutParams.MATCH_PARENT,
							LayoutParams.WRAP_CONTENT);
					lp.setMargins(left, top, right, 0);
					creditsMessageView.setLayoutParams(lp);
				}
				LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,
						LayoutParams.WRAP_CONTENT);
				lp.setMargins(left, top, right,
						creditsMessageView != null ? bottom : 0);
				mainMessage.setLayoutParams(lp);

				((ViewGroup) holder.container).addView(mainMessage);
				if (creditsMessageView != null) {
					((ViewGroup) holder.container).addView(creditsMessageView);
				}
			} else if ((infoState == ParticipantInfoState.CHANGED_GROUP_NAME)
					|| (infoState == ParticipantInfoState.CHANGED_GROUP_IMAGE)) {
				String msisdn = metadata.getMsisdn();
				String userMsisdn = preferences.getString(
						HikeMessengerApp.MSISDN_SETTING, "");

				String participantName = userMsisdn.equals(msisdn) ? context
						.getString(R.string.you)
						: ((GroupConversation) conversation)
								.getGroupParticipantFirstName(msisdn);
				String message = String
						.format(context.getString(convMessage
								.getParticipantInfoState() == ParticipantInfoState.CHANGED_GROUP_NAME ? R.string.change_group_name
								: R.string.change_group_image), participantName);

				TextView mainMessage = (TextView) inflater.inflate(layoutRes,
						null);
				int icRes;
				if (infoState == ParticipantInfoState.CHANGED_GROUP_NAME) {
					icRes = isDefaultTheme ? R.drawable.ic_group_info
							: R.drawable.ic_group_info_custom;
				} else {
					icRes = isDefaultTheme ? R.drawable.ic_group_image
							: R.drawable.ic_group_image_custom;
				}
				setTextAndIconForSystemMessages(mainMessage,
						Utils.getFormattedParticipantInfo(message,
								participantName), icRes);

				((ViewGroup) holder.container).addView(mainMessage);
			} else if (infoState == ParticipantInfoState.BLOCK_INTERNATIONAL_SMS) {
				String info = context.getString(R.string.block_internation_sms);
				String textToHighlight = context
						.getString(R.string.block_internation_sms_bold_text);

				TextView mainMessage = (TextView) inflater.inflate(layoutRes,
						null);
				setTextAndIconForSystemMessages(
						mainMessage,
						Utils.getFormattedParticipantInfo(info, textToHighlight),
						isDefaultTheme ? R.drawable.ic_no_int_sms
								: R.drawable.ic_no_int_sms_custom);

				((ViewGroup) holder.container).addView(mainMessage);
			} else if (infoState == ParticipantInfoState.INTRO_MESSAGE) {
				String name = Utils.getFirstName(conversation.getLabel());
				String message;
				if (conversation.isOnhike()) {
					boolean firstIntro = conversation.getMsisdn().hashCode() % 2 == 0;
					message = String.format(context
							.getString(firstIntro ? R.string.start_thread1
									: R.string.start_thread1), name);
				} else {
					message = String.format(
							context.getString(R.string.intro_sms_thread), name);
				}

				int icRes;
				if (conversation.isOnhike()) {
					icRes = isDefaultTheme ? R.drawable.ic_user_join
							: R.drawable.ic_user_join_custom;
				} else {
					icRes = isDefaultTheme ? R.drawable.ic_sms_user_ct
							: R.drawable.ic_sms_user_ct_custom;
				}

				TextView mainMessage = (TextView) inflater.inflate(layoutRes,
						null);
				setTextAndIconForSystemMessages(mainMessage,
						Utils.getFormattedParticipantInfo(message, name), icRes);

				((ViewGroup) holder.container).addView(mainMessage);
			} else if (infoState == ParticipantInfoState.DND_USER) {
				JSONArray dndNumbers = metadata.getDndNumbers();

				TextView dndMessage = (TextView) inflater.inflate(layoutRes,
						null);

				if (dndNumbers != null && dndNumbers.length() > 0) {
					StringBuilder dndNamesSB = new StringBuilder();
					for (int i = 0; i < dndNumbers.length(); i++) {
						String name = conversation instanceof GroupConversation ? ((GroupConversation) conversation)
								.getGroupParticipantFirstName(dndNumbers
										.optString(i)) : Utils
								.getFirstName(conversation.getLabel());
						if (i < dndNumbers.length() - 2) {
							dndNamesSB.append(name + ", ");
						} else if (i < dndNumbers.length() - 1) {
							dndNamesSB.append(name + " and ");
						} else {
							dndNamesSB.append(name);
						}
					}
					String dndNames = dndNamesSB.toString();
					convMessage
							.setMessage(String.format(
									context.getString(conversation instanceof GroupConversation ? R.string.dnd_msg_gc
											: R.string.dnd_one_to_one),
									dndNames));

					SpannableStringBuilder ssb;
					if (conversation instanceof GroupConversation) {
						ssb = new SpannableStringBuilder(
								convMessage.getMessage());
						ssb.setSpan(new StyleSpan(Typeface.BOLD), convMessage
								.getMessage().indexOf(dndNames),
								convMessage.getMessage().indexOf(dndNames)
										+ dndNames.length(),
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

					} else {

						ssb = new SpannableStringBuilder(
								convMessage.getMessage());
						ssb.setSpan(new StyleSpan(Typeface.BOLD), convMessage
								.getMessage().indexOf(dndNames),
								convMessage.getMessage().indexOf(dndNames)
										+ dndNames.length(),
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						ssb.setSpan(new StyleSpan(Typeface.BOLD), convMessage
								.getMessage().lastIndexOf(dndNames),
								convMessage.getMessage().lastIndexOf(dndNames)
										+ dndNames.length(),
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}

					setTextAndIconForSystemMessages(dndMessage, ssb,
							isDefaultTheme ? R.drawable.ic_waiting_dnd
									: R.drawable.ic_waiting_dnd_custom);
					LayoutParams lp = new LayoutParams(
							LayoutParams.MATCH_PARENT,
							LayoutParams.WRAP_CONTENT);
					lp.setMargins(left, top, right, 0);
					dndMessage.setLayoutParams(lp);

					((ViewGroup) holder.container).addView(dndMessage);
				}
			} else if (infoState == ParticipantInfoState.CHAT_BACKGROUND) {
				TextView mainMessage = (TextView) inflater.inflate(layoutRes,
						null);

				String msisdn = metadata.getMsisdn();
				String userMsisdn = preferences.getString(
						HikeMessengerApp.MSISDN_SETTING, "");

				String name;
				if (isGroupChat) {
					name = userMsisdn.equals(msisdn) ? context
							.getString(R.string.you)
							: ((GroupConversation) conversation)
									.getGroupParticipantFirstName(msisdn);
				} else {
					name = userMsisdn.equals(msisdn) ? context
							.getString(R.string.you) : Utils
							.getFirstName(conversation.getLabel());
				}

				String message = context.getString(R.string.chat_bg_changed,
						name);

				setTextAndIconForSystemMessages(mainMessage,
						Utils.getFormattedParticipantInfo(message, name),
						isDefaultTheme ? R.drawable.ic_change_theme
								: R.drawable.ic_change_theme_custom);

				((ViewGroup) holder.container).addView(mainMessage);
			}
			return v;
		} else if (infoState == ParticipantInfoState.STATUS_MESSAGE) {
			if (isDefaultTheme) {
				holder.dayTextView.setTextColor(context.getResources()
						.getColor(R.color.list_item_subtext));
				holder.messageInfo.setTextColor(context.getResources()
						.getColor(R.color.timestampcolor));
				holder.messageTextView.setTextColor(context.getResources()
						.getColor(R.color.list_item_header));
				holder.container
						.setBackgroundResource(R.drawable.bg_status_chat_thread);
			} else {
				holder.dayTextView.setTextColor(context.getResources()
						.getColor(R.color.white));
				holder.messageInfo.setTextColor(context.getResources()
						.getColor(R.color.white));
				holder.messageTextView.setTextColor(context.getResources()
						.getColor(R.color.white));
				holder.container
						.setBackgroundResource(R.drawable.bg_status_chat_thread_custom_theme);
			}

			StatusMessage statusMessage = convMessage.getMetadata()
					.getStatusMessage();

			holder.dayTextView.setText(context.getString(
					R.string.xyz_posted_update,
					Utils.getFirstName(conversation.getLabel())));

			iconLoader.loadImage(conversation.getMsisdn(),true ,holder.image,true);

			holder.messageInfo.setText(statusMessage.getTimestampFormatted(
					true, context));

			if (statusMessage.getStatusMessageType() == StatusMessageType.TEXT) {
				SmileyParser smileyParser = SmileyParser.getInstance();
				holder.messageTextView.setText(smileyParser.addSmileySpans(
						statusMessage.getText(), true));
				Linkify.addLinks(holder.messageTextView, Linkify.ALL);

			} else if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC) {
				holder.messageTextView.setText(R.string.changed_profile);
			}

			if (statusMessage.hasMood()) {
				holder.image.setImageResource(EmoticonConstants.moodMapping
						.get(statusMessage.getMoodId()));
				holder.avatarFrame.setVisibility(View.GONE);
			} else {
				holder.avatarFrame.setVisibility(View.VISIBLE);
			}

			holder.container.setTag(convMessage);
			holder.container.setOnClickListener(this);

			boolean showTip = false;
			boolean shownStatusTip = preferences.getBoolean(
					HikeMessengerApp.SHOWN_STATUS_TIP, false);

			if (!shownStatusTip) {
				if (chatThread.tipView == null) {
					showTip = true;
				} else {
					TipType tipType = (TipType) chatThread.tipView.getTag();
					if (tipType == TipType.STATUS
							&& statusMessage.getMappedId().equals(
									statusIdForTip)) {
						showTip = true;
					}
				}
			}

			if (showTip) {
				chatThread.tipView = v.findViewById(R.id.status_tip);
				statusIdForTip = statusMessage.getMappedId();
				Utils.showTip(chatThread, TipType.STATUS, chatThread.tipView,
						Utils.getFirstName(conversation.getLabel()));
			} else {
				v.findViewById(R.id.status_tip).setVisibility(View.GONE);
			}

			return v;
		}

		if (holder.poke != null) {
			holder.poke.setVisibility(View.GONE);
			holder.pokeCustom.setVisibility(View.GONE);
		}

		boolean firstMessageFromParticipant = false;
		if (isGroupChat
				&& !TextUtils.isEmpty(convMessage.getGroupParticipantMsisdn())) {
			if (position != 0) {
				ConvMessage previous = getItem(position - 1);
				if (previous.getParticipantInfoState() != ParticipantInfoState.NO_INFO
						|| !convMessage.getGroupParticipantMsisdn().equals(
								previous.getGroupParticipantMsisdn())) {
					firstMessageFromParticipant = true;
				}
			} else {
				firstMessageFromParticipant = true;
			}
		}

		MessageMetadata metadata = convMessage.getMetadata();

		holder.stickerPlaceholder.setVisibility(View.GONE);

		if (convMessage.isFileTransferMessage()) {
			holder.circularProgress
					.setProgressColor(context
							.getResources()
							.getColor(
									isDefaultTheme ? R.color.progress_colour_default_theme
											: R.color.white));

			final HikeFile hikeFile = metadata.getHikeFiles().get(0);
			HikeFileType hikeFileType = hikeFile.getHikeFileType();

			boolean showThumbnail = ((convMessage.isSent())
					|| (conversation instanceof GroupConversation)
					|| (!TextUtils.isEmpty(conversation.getContactName())) || (hikeFile
					.wasFileDownloaded() && !HikeMessengerApp.fileTransferTaskMap
					.containsKey(convMessage.getMsgID())))
					&& (hikeFile.getThumbnail() != null)
					&& (hikeFileType != HikeFileType.UNKNOWN);

			Drawable thumbnail = null;
			if (hikeFile.getHikeFileType() == HikeFileType.IMAGE
					|| hikeFile.getHikeFileType() == HikeFileType.VIDEO
					|| hikeFile.getHikeFileType() == HikeFileType.LOCATION) {
				if (hikeFile.getThumbnail() == null
						&& !TextUtils.isEmpty(hikeFile.getFileKey())) {
					thumbnail = HikeMessengerApp.getLruCache().getFileIconFromCache(hikeFile.getFileKey());
					
					if (thumbnail != null) {
						showThumbnail = true;
					}
				} else {
					thumbnail = hikeFile.getThumbnail();
				}
			}

			if (convMessage.isSent()
					&& (hikeFileType == HikeFileType.IMAGE || hikeFileType == HikeFileType.LOCATION)
					&& thumbnail == null) {
				/*
				 * This case should ideally only happen when downloading a
				 * picasa image or the thumbnail for a location. In that case we
				 * won't have a thumbnail initially while the image is being
				 * downloaded.
				 */
				holder.loadingThumb.setVisibility(View.VISIBLE);
				holder.fileThumb.setVisibility(View.GONE);
				showThumbnail = true;
			} else {
				if (holder.loadingThumb != null) {
					holder.loadingThumb.setVisibility(View.GONE);
					holder.fileThumb.setVisibility(View.VISIBLE);
				}

				if (showThumbnail) {
					holder.fileThumb.setBackgroundDrawable(thumbnail);
				} else {
					switch (hikeFileType) {
					case IMAGE:
						holder.fileThumb
								.setBackgroundResource(R.drawable.ic_default_img);
						break;
					case VIDEO:
						holder.fileThumb
								.setBackgroundResource(R.drawable.ic_default_mov);
						break;
					case AUDIO:
						holder.fileThumb
								.setBackgroundResource(R.drawable.ic_default_audio);
						break;
					case CONTACT:
						holder.fileThumb
								.setBackgroundResource(R.drawable.ic_default_contact);
						break;
					case AUDIO_RECORDING:
						holder.fileThumb
								.setBackgroundResource(R.drawable.ic_audio_msg_received);
						break;
					case UNKNOWN:
						holder.fileThumb
								.setBackgroundResource(R.drawable.ic_unknown_file);
						break;
					}
				}
			}

			holder.fileThumb
					.setImageResource(((hikeFileType == HikeFileType.VIDEO) && (showThumbnail)) ? R.drawable.ic_video_play
							: 0);

			LayoutParams fileThumbParams = (LayoutParams) holder.fileThumb
					.getLayoutParams();

			if (showThumbnail && thumbnail != null) {
				holder.fileThumb.setScaleType(ScaleType.CENTER);
				fileThumbParams.height = (int) (150 * Utils.densityMultiplier);
				fileThumbParams.width = (int) ((thumbnail.getIntrinsicWidth() * fileThumbParams.height) / thumbnail
						.getIntrinsicHeight());
				/*
				 * fixed the bug when imagethumbnail is very big. By specifying
				 * a maximum width for the thumbnail so that download button can
				 * also fit to the screen.
				 */
				int maxWidth = (int) (250 * Utils.densityMultiplier);
				fileThumbParams.width = Math.min(fileThumbParams.width,
						maxWidth);
			} else {
				holder.fileThumb.setScaleType(ScaleType.CENTER);
				fileThumbParams.height = LayoutParams.WRAP_CONTENT;
				fileThumbParams.width = LayoutParams.WRAP_CONTENT;
			}
			holder.fileThumb.setLayoutParams(fileThumbParams);

			holder.messageTextView.setVisibility(!showThumbnail ? View.VISIBLE
					: View.GONE);

			if (hikeFileType == HikeFileType.AUDIO_RECORDING) {
				Utils.setupFormattedTime(holder.messageTextView,
						hikeFile.getRecordingDuration());
			} else if (hikeFileType == HikeFileType.UNKNOWN) {
				holder.messageTextView.setText(context
						.getString(R.string.unknown_msg));
			} else {
				holder.messageTextView.setText(hikeFile.getFileName());
			}

			if (holder.showFileBtn != null) {
				if (hikeFile.wasFileDownloaded()
						&& showThumbnail
						&& !HikeMessengerApp.fileTransferTaskMap
								.containsKey(convMessage.getMsgID())) {
					holder.showFileBtn.setVisibility(View.GONE);

				} else {
					LayoutParams lp = (LayoutParams) holder.showFileBtn
							.getLayoutParams();
					lp.gravity = !showThumbnail ? Gravity.CENTER_VERTICAL
							: Gravity.BOTTOM;
					holder.showFileBtn.setLayoutParams(lp);

					if (hikeFileType == HikeFileType.AUDIO_RECORDING) {
						holder.showFileBtn.setVisibility(View.VISIBLE);
						holder.showFileBtn.setBackgroundResource(0);
						holder.showFileBtn.setImageResource(0);
						holder.showFileBtn
								.setScaleType(ScaleType.CENTER_INSIDE);
						if (hikeFile.getFileKey().equals(
								voiceMessagePlayer.getFileKey())) {

							if (voiceMessagePlayer.getPlayerState() == VoiceMessagePlayerState.PLAYING) {
								holder.showFileBtn
										.setImageResource(R.drawable.ic_pause_audio);
							} else {
								holder.showFileBtn
										.setImageResource(R.drawable.ic_open_received_file);
							}
							holder.showFileBtn
									.setBackgroundResource(R.drawable.bg_red_btn_selector);
							holder.messageTextView
									.setTag(hikeFile.getFileKey());
							voiceMessagePlayer
									.setDurationTxt(holder.messageTextView);
						} else {
							if (!convMessage.isSent()
									|| !HikeMessengerApp.fileTransferTaskMap
											.containsKey(convMessage.getMsgID())) {
								holder.showFileBtn.setVisibility(View.VISIBLE);
								setFileButtonResource(holder.showFileBtn,
										convMessage, hikeFile);
							} else {
								holder.showFileBtn.setVisibility(View.GONE);
							}
						}

					} else {
						holder.showFileBtn
								.setVisibility(convMessage.isSent() ? View.GONE
										: View.VISIBLE);
						holder.showFileBtn.setBackgroundResource(0);
						setFileButtonResource(holder.showFileBtn, convMessage,
								hikeFile);
					}
				}
			}
			if (holder.marginView != null) {
				holder.marginView.setVisibility(hikeFile.getThumbnail() == null
						&& !showThumbnail ? View.VISIBLE : View.GONE);
			}
			if (!convMessage.isSent()) {
				if (firstMessageFromParticipant) {
					holder.participantNameFT
							.setText(((GroupConversation) conversation)
									.getGroupParticipantFirstName(convMessage
											.getGroupParticipantMsisdn()));
					holder.participantNameFT.setVisibility(View.VISIBLE);
				} else {
					holder.participantNameFT.setVisibility(View.GONE);
				}
			}
			holder.messageContainer.setTag(convMessage);
			holder.messageContainer.setOnClickListener(this);
			holder.messageContainer.setOnLongClickListener(this);
		} else if (metadata != null && metadata.isPokeMessage()) {
			holder.messageTextView.setVisibility(View.GONE);
			holder.messageContainer.setVisibility(View.VISIBLE);
			if (!convMessage.isSent()) {
				if (firstMessageFromParticipant) {
					holder.participantNameFT.setVisibility(View.VISIBLE);
					holder.participantNameFT
							.setText(((GroupConversation) conversation)
									.getGroupParticipantFirstName(convMessage
											.getGroupParticipantMsisdn()));
				} else {
					holder.participantNameFT.setVisibility(View.GONE);
				}
			}
			if (isDefaultTheme) {
				holder.poke.setVisibility(View.VISIBLE);
				holder.poke
						.setImageResource(convMessage.isSent() ? R.drawable.ic_nudge_hike_sent
								: R.drawable.ic_nudge_hike_receive);
				holder.messageContainer.setVisibility(View.VISIBLE);
			} else {
				holder.pokeCustom.setVisibility(View.VISIBLE);
				holder.pokeCustom
						.setImageResource(convMessage.isSent() ? chatTheme
								.sentNudgeResId()
								: R.drawable.ic_nudge_receive_custom);
				holder.messageContainer.setVisibility(View.GONE);
			}
		} else if (convMessage.isStickerMessage()) {
			holder.messageContainer.setVisibility(View.GONE);
			holder.poke.setVisibility(View.GONE);
			holder.stickerPlaceholder.setVisibility(View.VISIBLE);
			holder.stickerPlaceholder.setBackgroundResource(0);

			holder.stickerImage.setVisibility(View.GONE);
			holder.stickerLoader.setVisibility(View.GONE);
			holder.stickerParticipantName.setVisibility(View.GONE);

			Sticker sticker = metadata.getSticker();

			if (!convMessage.isSent()) {
				if (firstMessageFromParticipant) {
					holder.stickerParticipantName.setVisibility(View.VISIBLE);
					holder.stickerParticipantName
							.setText(((GroupConversation) conversation)
									.getGroupParticipantFirstName(convMessage
											.getGroupParticipantMsisdn()));
				} else {
					holder.stickerParticipantName.setVisibility(View.GONE);
				}
			}
			/*
			 * If this is the default category, then the sticker are part of the
			 * app bundle itself
			 */
			if (sticker.getStickerIndex() != -1) {
				holder.stickerImage.setVisibility(View.VISIBLE);
				if (StickerCategoryId.doggy.equals(sticker.getCategory().categoryId)) {
					holder.stickerImage
							.setImageResource(StickerManager.getInstance().LOCAL_STICKER_RES_IDS_DOGGY[sticker
									.getStickerIndex()]);
				} else if (StickerCategoryId.humanoid.equals(sticker.getCategory().categoryId)) {
					holder.stickerImage
							.setImageResource(StickerManager.getInstance().LOCAL_STICKER_RES_IDS_HUMANOID[sticker
									.getStickerIndex()]);
				}
			} else {
				String categoryId = sticker.getCategory().categoryId.name();
				String stickerId = sticker.getStickerId();

				String categoryDirPath = StickerManager.getInstance()
						.getStickerDirectoryForCategoryId(context, categoryId)
						+ HikeConstants.LARGE_STICKER_ROOT;
				File stickerImage = null;
				if (categoryDirPath != null) {
					stickerImage = new File(categoryDirPath, stickerId);
				}

				String key = categoryId + stickerId;
				boolean downloadingSticker = StickerManager.getInstance().isStickerDownloading(key); 

				if (stickerImage != null && stickerImage.exists()
						&& !downloadingSticker) {
					holder.stickerImage.setVisibility(View.VISIBLE);
					largeStickerLoader.loadImage(stickerImage.getPath(), holder.stickerImage);
					//holder.stickerImage.setImageDrawable(HikeMessengerApp.getLruCache().getSticker(context,stickerImage.getPath()));
//					holder.stickerImage.setImageDrawable(IconCacheManager
//							.getInstance().getSticker(context,
//									stickerImage.getPath()));
				} else {
					holder.stickerLoader.setVisibility(View.VISIBLE);
					holder.stickerPlaceholder
							.setBackgroundResource(R.drawable.bg_sticker_placeholder);

					/*
					 * Download the sticker if not already downloading.
					 */
					if (!downloadingSticker) {
						DownloadSingleStickerTask downloadSingleStickerTask = new DownloadSingleStickerTask(
								context, categoryId, stickerId);
						StickerManager.getInstance().insertTask(key,
								downloadSingleStickerTask);
						Utils.executeFtResultAsyncTask(downloadSingleStickerTask);
					}
				}
			}

		} else {
			holder.messageTextView.setVisibility(View.VISIBLE);
			holder.stickerPlaceholder.setVisibility(View.GONE);
			holder.messageContainer.setVisibility(View.VISIBLE);
			holder.poke.setVisibility(View.GONE);

			CharSequence markedUp = convMessage.getMessage();
			// Fix for bug where if a participant leaves the group chat, the
			// participant's name is never shown
			if (!convMessage.isSent()) {
				if (firstMessageFromParticipant) {
					holder.participantNameFT.setVisibility(View.VISIBLE);
					holder.participantNameFT
							.setText(((GroupConversation) conversation)
									.getGroupParticipantFirstName(convMessage
											.getGroupParticipantMsisdn()));
				} else {
					holder.participantNameFT.setVisibility(View.GONE);
				}
			}
			SmileyParser smileyParser = SmileyParser.getInstance();
			markedUp = smileyParser.addSmileySpans(markedUp, false);
			holder.messageTextView.setText(markedUp);
			Linkify.addLinks(holder.messageTextView, Linkify.ALL);
			Linkify.addLinks(holder.messageTextView, Utils.shortCodeRegex,
					"tel:");
		}

		if (convMessage.isFileTransferMessage() && convMessage.isSent()) {
			holder.image.setVisibility(View.INVISIBLE);
		}

		if (convMessage.isFileTransferMessage()
				&& HikeMessengerApp.fileTransferTaskMap.containsKey(convMessage
						.getMsgID())) {
			FileTransferTaskBase fileTransferTask = HikeMessengerApp.fileTransferTaskMap
					.get(convMessage.getMsgID());
			holder.circularProgress.setVisibility(View.VISIBLE);
			holder.circularProgress.setProgressAngle(fileTransferTask
					.getProgress());

			if (holder.messageInfo != null) {
				holder.messageInfo.setVisibility(View.INVISIBLE);
			}
			if (holder.sending != null) {
				holder.sending.setVisibility(View.INVISIBLE);
			}
		} else if (convMessage.isFileTransferMessage()
				&& convMessage.isSent()
				&& TextUtils.isEmpty(metadata.getHikeFiles().get(0)
						.getFileKey())) {
			if (holder.circularProgress != null) {
				holder.circularProgress.setVisibility(View.INVISIBLE);
			}
			holder.image.setVisibility(View.VISIBLE);
			holder.image
					.setImageResource(isDefaultTheme ? R.drawable.ic_download_failed
							: R.drawable.ic_download_failed_custom);

			if (holder.messageInfo != null) {
				holder.messageInfo.setVisibility(View.INVISIBLE);
			}
			if (holder.sending != null) {
				holder.sending.setVisibility(View.INVISIBLE);
			}
		} else {
			if (holder.circularProgress != null) {
				holder.circularProgress.setVisibility(View.INVISIBLE);
			}
			if (!convMessage.isSent()) {
				if (firstMessageFromParticipant) {
					holder.image.setVisibility(View.VISIBLE);
					iconLoader.loadImage(convMessage.getGroupParticipantMsisdn(), true, holder.image,true);
					holder.avatarContainer.setVisibility(View.VISIBLE);
				} else {
					holder.avatarContainer
							.setVisibility(isGroupChat ? View.INVISIBLE
									: View.GONE);
				}
			}
			setSDRAndTimestamp(position, holder.messageInfo, holder.sending,
					holder.bubbleContainer);
		}

		if (convMessage.isSent() && holder.messageContainer != null) {
			/* label outgoing hike conversations in green */
			if (isDefaultTheme) {
				holder.messageContainer.setBackgroundResource(!convMessage
						.isSMS() ? R.drawable.ic_bubble_blue_selector
						: R.drawable.ic_bubble_green_selector);
			} else {
				holder.messageContainer.setBackgroundResource(chatTheme
						.bubbleResId());
			}
		}

		return v;
	}

	private void setFileButtonResource(ImageView button,
			ConvMessage convMessage, HikeFile hikeFile) {
		button.setBackgroundResource(R.drawable.bg_red_btn_selector);
		if (HikeMessengerApp.fileTransferTaskMap.containsKey(convMessage
				.getMsgID())) {
			button.setImageResource(R.drawable.ic_download_file);
			button.setBackgroundResource(R.drawable.bg_red_btn_disabled);
		} else if (hikeFile.wasFileDownloaded()
				&& hikeFile.getHikeFileType() != HikeFileType.CONTACT) {
			button.setImageResource(R.drawable.ic_open_received_file);
		} else {
			button.setImageResource(R.drawable.ic_download_file);
		}
	}

	private void setTextAndIconForSystemMessages(TextView textView,
			CharSequence text, int iconResId) {
		textView.setText(text);
		textView.setCompoundDrawablesWithIntrinsicBounds(iconResId, 0, 0, 0);
	}

	private void showTryingAgainIcon(ImageView iv, long ts) {
		/*
		 * We are checking this so that we can delay the try again icon from
		 * being shown immediately if the user just sent the msg. If it has been
		 * over 5 secs then the user will immediately see the icon though.
		 */
		if ((((long) System.currentTimeMillis() / 1000) - ts) < 3) {
			iv.setVisibility(View.INVISIBLE);

			Animation anim = AnimationUtils.loadAnimation(context,
					android.R.anim.fade_in);
			anim.setStartOffset(4000);
			anim.setDuration(1);

			iv.setAnimation(anim);
		}
		iv.setVisibility(View.VISIBLE);
		iv.setImageResource(R.drawable.ic_retry_sending);
	}

	Handler handler = new Handler();

	private void scheduleUndeliveredText(TextView tv, View container,
			ImageView iv, long ts) {
		if (showUndeliveredMessage != null) {
			handler.removeCallbacks(showUndeliveredMessage);
		}

		long diff = (((long) System.currentTimeMillis() / 1000) - ts);

		if (Utils.isUserOnline(context)
				&& diff < HikeConstants.DEFAULT_UNDELIVERED_WAIT_TIME) {
			showUndeliveredMessage = new ShowUndeliveredMessage(tv, container,
					iv);
			handler.postDelayed(showUndeliveredMessage,
					(HikeConstants.DEFAULT_UNDELIVERED_WAIT_TIME - diff) * 1000);
		} else {
			showUndeliveredTextAndSetClick(tv, container, iv, true);
		}
	}

	private void showFileTransferElements(ViewHolder holder) {
		holder.fileThumb.setVisibility(View.VISIBLE);
	}

	private boolean showDayIndicator(int position) {
		/*
		 * We show the time stamp in the status message separately so no need to
		 * show this time stamp.
		 */
		if (ViewType.values()[getItemViewType(position)] == ViewType.STATUS_MESSAGE) {
			return false;
		}
		/*
		 * only show the timestamp if the delta between this message and the
		 * previous one is greater than 10 minutes
		 */
		ConvMessage current = getItem(position);
		ConvMessage previous = position > 0 ? getItem(position - 1) : null;
		if (previous == null) {
			return true;
		} else if (previous.getMsgID() == LAST_READ_CONV_MESSAGE_ID) {
			return false;
		}

		Calendar currentMessageCalendar = Calendar.getInstance();
		currentMessageCalendar.setTimeInMillis(current.getTimestamp() * 1000);

		Calendar previousMessageCalendar = Calendar.getInstance();
		previousMessageCalendar.setTimeInMillis(previous.getTimestamp() * 1000);

		return (previousMessageCalendar.get(Calendar.DAY_OF_YEAR) != currentMessageCalendar
				.get(Calendar.DAY_OF_YEAR));
	}

	private void setSDRAndTimestamp(int position, TextView tv, ImageView iv,
			View container) {
		/*
		 * We show the time stamp in the status message separately so no need to
		 * show this time stamp.
		 */
		if (ViewType.values()[getItemViewType(position)] == ViewType.STATUS_MESSAGE) {
			return;
		}
		tv.setVisibility(View.VISIBLE);
		if (iv != null) {
			iv.setVisibility(View.GONE);
		}

		tv.setTextColor(context.getResources().getColor(
				isDefaultTheme ? R.color.list_item_subtext : R.color.white));

		ConvMessage current = getItem(position);
		if (current.isSent() && (position == lastSentMessagePosition)) {
			switch (current.getState()) {
			case SENT_UNCONFIRMED:
				tv.setVisibility(View.GONE);
				iv.setVisibility(View.VISIBLE);

				iv.setImageResource(isDefaultTheme ? R.drawable.sending
						: R.drawable.sending_custom);
				AnimationDrawable ad = (AnimationDrawable) iv.getDrawable();
				ad.setCallback(iv);
				ad.setVisible(true, true);
				ad.start();

				if (!current.isSMS()) {
					scheduleUndeliveredText(tv, container, iv,
							current.getTimestamp());
				}
				break;
			case SENT_CONFIRMED:
				tv.setText(context.getString(
						!current.isSMS() ? R.string.sent
								: R.string.sent_via_sms, current
								.getTimestampFormatted(false, context)));
				if (!current.isSMS()) {
					scheduleUndeliveredText(tv, container, iv,
							current.getTimestamp());
				}
				break;
			case SENT_DELIVERED:
				tv.setText(R.string.delivered);
				break;
			case SENT_DELIVERED_READ:
				if (!isGroupChat) {
					tv.setText(R.string.read);
				} else {
					setReadByForGroup(current, tv);
				}
				break;
			}
		} else {

			ConvMessage next = position == getCount() - 1 ? null
					: getItem(position + 1);

			if (next == null || (next.isSent() != current.isSent())
					|| (next.getTimestamp() - current.getTimestamp() > 2 * 60)) {
				tv.setText(current.getTimestampFormatted(false, context));
				return;
			}
			tv.setVisibility(View.GONE);
		}
	}

	private void setReadByForGroup(ConvMessage convMessage, TextView tv) {
		GroupConversation groupConversation = (GroupConversation) conversation;
		JSONArray readByArray = convMessage.getReadByArray();

		if (readByArray == null
				|| groupConversation.getGroupMemberAliveCount() == readByArray
						.length()) {
			tv.setText(R.string.read_by_everyone);
		} else {
			StringBuilder sb = new StringBuilder();

			int lastIndex = readByArray.length()
					- HikeConstants.MAX_READ_BY_NAMES;

			boolean moreNamesThanMaxCount = false;
			if (lastIndex < 0) {
				lastIndex = 0;
			} else if (lastIndex > 0) {
				moreNamesThanMaxCount = true;
			}

			for (int i = readByArray.length() - 1; i >= lastIndex; i--) {
				sb.append(groupConversation
						.getGroupParticipantFirstName(readByArray.optString(i)));
				if (i > lastIndex + 1) {
					sb.append(", ");
				} else if (i == lastIndex + 1) {
					if (moreNamesThanMaxCount) {
						sb.append(", ");
					} else {
						sb.append(" and ");
					}
				}
			}
			String readByString = sb.toString();
			if (moreNamesThanMaxCount) {
				tv.setText(context.getString(R.string.read_by_names_number,
						readByString, lastIndex));
			} else {
				tv.setText(context.getString(R.string.read_by_names_only,
						readByString));
			}
		}
	}

	@Override
	public int getCount() {
		return convMessages.size();
	}

	@Override
	public ConvMessage getItem(int position) {
		return convMessages.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public boolean isEmpty() {
		return getCount() == 0;
	}

	@Override
	public void onClick(View v) {
		try {
			ConvMessage convMessage = (ConvMessage) v.getTag();
			if (convMessage == null) {
				return;
			}
			Log.d(getClass().getSimpleName(), "OnCLICK");
			if (convMessage.isSent()
					&& convMessage.equals(convMessages
							.get(lastSentMessagePosition))
					&& isMessageUndelivered(convMessage)
					&& convMessage.getState() != State.SENT_UNCONFIRMED
					&& !chatThread.isContactOnline()) {
				long diff = (((long) System.currentTimeMillis() / 1000) - convMessage
						.getTimestamp());

				/*
				 * Only show fallback if the message has not been sent for our
				 * max wait time.
				 */
				if (diff >= HikeConstants.DEFAULT_UNDELIVERED_WAIT_TIME
						|| !Utils.isUserOnline(context)) {

					if (conversation.isOnhike()) {
						if (!Utils.isUserOnline(context)) {
							if (conversation instanceof GroupConversation) {
								Toast.makeText(context,
										R.string.gc_fallback_offline,
										Toast.LENGTH_LONG).show();
							} else {
								showSMSDialog(true);
							}
						} else {
							if (conversation instanceof GroupConversation) {
								showSMSDialog(false);
							} else {
								/*
								 * Only show the H2S fallback option if
								 * messaging indian numbers.
								 */
								showSMSDialog(!conversation
										.getMsisdn()
										.startsWith(
												HikeConstants.INDIA_COUNTRY_CODE));
							}
						}
					} else {
						sendAllUnsentMessagesAsSMS(Utils
								.getSendSmsPref(context));
					}
					return;
				}
			}
			if (convMessage.isFileTransferMessage()) {
				HikeFile hikeFile = convMessage.getMetadata().getHikeFiles()
						.get(0);
				if (Utils.getExternalStorageState() == ExternalStorageState.NONE
						&& hikeFile.getHikeFileType() != HikeFileType.CONTACT
						&& hikeFile.getHikeFileType() != HikeFileType.LOCATION) {
					Toast.makeText(context, R.string.no_external_storage,
							Toast.LENGTH_SHORT).show();
					return;
				}
				if (convMessage.isSent()) {
					Log.d(getClass().getSimpleName(),
							"Hike File name: " + hikeFile.getFileName()
									+ " File key: " + hikeFile.getFileKey());
					// If uploading failed then we try again.
					if (TextUtils.isEmpty(hikeFile.getFileKey())
							&& !HikeMessengerApp.fileTransferTaskMap
									.containsKey(convMessage.getMsgID())) {

						FileTransferTaskBase uploadTask;
						if ((hikeFile.getHikeFileType() != HikeFileType.LOCATION)
								&& (hikeFile.getHikeFileType() != HikeFileType.CONTACT)) {
							uploadTask = new UploadFileTask(convMessage,
									context, conversation);
						} else {
							uploadTask = new UploadContactOrLocationTask(
									convMessage,
									context,
									(hikeFile.getHikeFileType() == HikeFileType.CONTACT),
									conversation);
						}
						Utils.executeIntProgFtResultAsyncTask(uploadTask);
						HikeMessengerApp.fileTransferTaskMap.put(
								convMessage.getMsgID(), uploadTask);
						notifyDataSetChanged();
					}
					// Else we open it for the use to see
					else {
						openFile(hikeFile, convMessage, v);
					}
				} else {
					File receivedFile = hikeFile.getFile();
					if (hikeFile.getHikeFileType() == HikeFileType.UNKNOWN) {
						Toast.makeText(context, R.string.unknown_msg,
								Toast.LENGTH_SHORT).show();
						return;
					}
					if (!HikeMessengerApp.fileTransferTaskMap
							.containsKey(convMessage.getMsgID())
							&& ((hikeFile.getHikeFileType() == HikeFileType.LOCATION)
									|| (hikeFile.getHikeFileType() == HikeFileType.CONTACT) || receivedFile
										.exists())) {
						openFile(hikeFile, convMessage, v);
					} else if (!HikeMessengerApp.fileTransferTaskMap
							.containsKey(convMessage.getMsgID())) {
						Log.d(getClass().getSimpleName(),
								"HIKEFILE: NAME: " + hikeFile.getFileName()
										+ " KEY: " + hikeFile.getFileKey()
										+ " TYPE: "
										+ hikeFile.getFileTypeString());
						DownloadFileTask downloadFile = new DownloadFileTask(
								context, receivedFile, hikeFile.getFileKey(),
								convMessage, hikeFile.getHikeFileType(),
								convMessage.getMsgID());
						Utils.executeIntProgFtResultAsyncTask(downloadFile);
					}
				}
			} else if (convMessage.getMetadata() != null
					&& convMessage.getMetadata().getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE) {
				Intent intent = new Intent(context, ProfileActivity.class);
				intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE,
						true);
				intent.putExtra(HikeConstants.Extras.ON_HIKE,
						conversation.isOnhike());

				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				intent.putExtra(HikeConstants.Extras.CONTACT_INFO,
						convMessage.getMsisdn());
				context.startActivity(intent);
			}
		} catch (ActivityNotFoundException e) {
			Log.w(getClass().getSimpleName(),
					"Trying to open an unknown format", e);
			Toast.makeText(context, R.string.unknown_msg, Toast.LENGTH_SHORT)
					.show();
		}

	}

	private void openFile(HikeFile hikeFile, ConvMessage convMessage,
			View parent) {
		File receivedFile = hikeFile.getFile();
		Log.d(getClass().getSimpleName(), "Opening file");
		Intent openFile = new Intent(Intent.ACTION_VIEW);
		if (hikeFile.getHikeFileType() == HikeFileType.LOCATION) {
			String uri = String.format(Locale.US,
					"geo:%1$f,%2$f?z=%3$d&q=%1$f,%2$f", hikeFile.getLatitude(),
					hikeFile.getLongitude(), hikeFile.getZoomLevel());
			openFile.setData(Uri.parse(uri));
		} else if (hikeFile.getHikeFileType() == HikeFileType.CONTACT) {
			saveContact(hikeFile);
			return;
		} else if (hikeFile.getHikeFileType() == HikeFileType.AUDIO_RECORDING) {
			String fileKey = hikeFile.getFileKey();

			ImageView showFileBtn = (ImageView) parent
					.findViewById(R.id.btn_open_file);
			TextView durationTxt = (TextView) parent.findViewById(convMessage
					.isSent() ? R.id.message_send : R.id.message_receive_ft);

			if (fileKey.equals(voiceMessagePlayer.getFileKey())) {

				showFileBtn.setTag(fileKey);
				voiceMessagePlayer.setFileBtn(showFileBtn);
				durationTxt.setTag(fileKey);
				voiceMessagePlayer.setDurationTxt(durationTxt);

				if (voiceMessagePlayer.getPlayerState() == VoiceMessagePlayerState.PLAYING) {
					voiceMessagePlayer.pausePlayer();
				} else if (voiceMessagePlayer.getPlayerState() == VoiceMessagePlayerState.PAUSED) {
					voiceMessagePlayer.resumePlayer();
				} else if (voiceMessagePlayer.getPlayerState() == VoiceMessagePlayerState.STOPPED) {
					voiceMessagePlayer.playMessage(hikeFile);
				}
			} else {
				if (voiceMessagePlayer.getPlayerState() == VoiceMessagePlayerState.PLAYING
						|| voiceMessagePlayer.getPlayerState() == VoiceMessagePlayerState.PAUSED) {
					voiceMessagePlayer.resetPlayer();
				}

				showFileBtn.setTag(fileKey);
				voiceMessagePlayer.setFileBtn(showFileBtn);
				durationTxt.setTag(fileKey);
				voiceMessagePlayer.setDurationTxt(durationTxt);

				voiceMessagePlayer.playMessage(hikeFile);
			}
			return;
		} else if (hikeFile.getHikeFileType() == HikeFileType.UNKNOWN
				|| receivedFile == null) {
			Toast.makeText(context, R.string.unknown_msg, Toast.LENGTH_SHORT);
		} else {
			openFile.setDataAndType(Uri.fromFile(receivedFile),
					hikeFile.getFileTypeString());
		}
		context.startActivity(openFile);
	}

	private void saveContact(HikeFile hikeFile) {

		String name = hikeFile.getDisplayName();

		List<ContactInfoData> items = Utils
				.getContactDataFromHikeFile(hikeFile);

		chatThread.showContactDetails(items, name, null, true);
	}

	@Override
	public boolean onLongClick(View view) {
		return chatThread.showMessageContextMenu((ConvMessage) view.getTag());
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		Editor editor = prefs.edit();
		editor.putBoolean(HikeConstants.SEND_SMS_PREF, isChecked);
		editor.commit();

		setSmsToggleSubtext(isChecked);

		HikeMessengerApp.getPubSub().publish(HikePubSub.SEND_SMS_PREF_TOGGLED,
				null);

		Utils.sendNativeSmsLogEvent(isChecked);

		if (isChecked) {
			if (!preferences.getBoolean(
					HikeMessengerApp.SHOWN_NATIVE_INFO_POPUP, false)) {
				showSMSClientDialog(true, buttonView, true);
			} else if (!prefs.getBoolean(HikeConstants.RECEIVE_SMS_PREF, false)) {
				showSMSClientDialog(true, buttonView, false);
			}
		}
	}

	private void setSmsToggleSubtext(boolean isChecked) {
		String msisdn = preferences.getString(HikeMessengerApp.MSISDN_SETTING,
				"");

		String text = context.getString(
				isChecked ? R.string.messaging_my_number
						: R.string.messaging_hike_number, msisdn);
		SpannableStringBuilder ssb = new SpannableStringBuilder(text);

		if (isChecked) {
			ssb.setSpan(new StyleSpan(Typeface.BOLD), text.indexOf(msisdn),
					text.indexOf(msisdn) + msisdn.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			hikeSmsText.setTextColor(context.getResources().getColor(
					isDefaultTheme ? R.color.sms_choice_unselected
							: R.color.sms_choice_unselected_custom_theme));
			regularSmsText.setTextColor(context.getResources().getColor(
					isDefaultTheme ? R.color.sms_choice_selected
							: R.color.white));

		} else {
			hikeSmsText.setTextColor(context.getResources().getColor(
					isDefaultTheme ? R.color.sms_choice_selected
							: R.color.white));
			regularSmsText.setTextColor(context.getResources().getColor(
					isDefaultTheme ? R.color.sms_choice_unselected
							: R.color.sms_choice_unselected_custom_theme));

		}
		smsToggleSubtext.setText(ssb);
	}

	private class ShowUndeliveredMessage implements Runnable {

		TextView tv;
		ImageView iv;
		View container;

		public ShowUndeliveredMessage(TextView tv, View container, ImageView iv) {
			this.tv = tv;
			this.container = container;
			this.iv = iv;
		}

		@Override
		public void run() {
			if (lastSentMessagePosition >= convMessages.size()
					|| lastSentMessagePosition == -1) {
				return;
			}
			ConvMessage lastSentMessage = convMessages
					.get(lastSentMessagePosition);
			if (isMessageUndelivered(lastSentMessage)) {
				showUndeliveredTextAndSetClick(tv, container, iv, true);
			}
		}
	}

	private void showUndeliveredTextAndSetClick(TextView tv, View container,
			ImageView iv, boolean fromHandler) {
		String undeliveredText = getUndeliveredTextRes();
		if (!TextUtils.isEmpty(undeliveredText)) {
			iv.setVisibility(View.GONE);
			tv.setVisibility(View.VISIBLE);
			tv.setText(undeliveredText);
		}

		container.setTag(convMessages.get(lastSentMessagePosition));
		container.setOnClickListener(this);
		container.setOnLongClickListener(this);

		/*
		 * Make the list scroll to the end to show the text.
		 */
		if (fromHandler) {
			HikeMessengerApp.getPubSub().publish(
					HikePubSub.SHOWN_UNDELIVERED_MESSAGE, null);
		}
	}

	private String getUndeliveredTextRes() {
		ConvMessage convMessage = convMessages.get(lastSentMessagePosition);

		int res;
		if (convMessage.getState() == State.SENT_UNCONFIRMED) {
			/*
			 * We don't want to show the user as offline. So we return blank
			 * here.
			 */
			return "";
		} else {
			/*
			 * We don't show the contact as offline if the user is online in the
			 * last time.
			 */
			if (chatThread.isContactOnline()) {
				return "";
			}
			res = conversation.isOnhike()
					&& !(conversation instanceof GroupConversation) ? R.string.msg_undelivered
					: R.string.sms_undelivered;
		}
		return context.getString(res,
				Utils.getFirstName(conversation.getLabel()));
	}

	private void showSMSDialog(final boolean nativeOnly) {
		final Dialog dialog = new Dialog(chatThread, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.sms_undelivered_popup);
		dialog.setCancelable(true);

		View hikeSMS = dialog.findViewById(R.id.hike_sms_container);
		View nativeSMS = dialog.findViewById(R.id.native_sms_container);
		View divider = dialog.findViewById(R.id.divider);
		TextView nativeHeader = (TextView) dialog
				.findViewById(R.id.native_sms_header);

		hikeSMS.setVisibility(nativeOnly ? View.GONE : View.VISIBLE);
		divider.setVisibility(nativeOnly ? View.GONE : View.VISIBLE);

		if (conversation instanceof GroupConversation) {
			nativeSMS.setVisibility(View.GONE);
			divider.setVisibility(View.GONE);
		}

		final CheckBox sendHike = (CheckBox) dialog
				.findViewById(R.id.hike_sms_checkbox);

		final CheckBox sendNative = (CheckBox) dialog
				.findViewById(R.id.native_sms_checkbox);

		final Button sendBtn = (Button) dialog.findViewById(R.id.btn_send);
		sendBtn.setEnabled(false);

		if (PreferenceManager.getDefaultSharedPreferences(context).contains(
				HikeConstants.SEND_UNDELIVERED_AS_NATIVE_SMS_PREF)) {
			boolean nativeOn = PreferenceManager.getDefaultSharedPreferences(
					context).getBoolean(
					HikeConstants.SEND_UNDELIVERED_AS_NATIVE_SMS_PREF, false);
			if (nativeOn || nativeOnly) {
				sendNative.setChecked(true);
				sendBtn.setEnabled(true);
			} else if (!nativeOnly
					|| (conversation instanceof GroupConversation)) {
				sendHike.setChecked(true);
				sendBtn.setEnabled(true);
			}
		}

		int numUnsentMessages = getAllUnsentMessages(false).size();
		nativeHeader.setText(context.getString(R.string.x_regular_sms,
				numUnsentMessages));

		sendHike.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				sendHike.setChecked(true);
				sendNative.setChecked(false);
				sendBtn.setEnabled(true);
			}
		});

		sendNative.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				sendHike.setChecked(false);
				sendNative.setChecked(true);
				sendBtn.setEnabled(true);
			}
		});

		sendBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (sendHike.isChecked()) {
					Utils.setSendUndeliveredSmsSetting(context, false);
					sendAllUnsentMessagesAsSMS(false);
				} else {
					if (!PreferenceManager.getDefaultSharedPreferences(context)
							.getBoolean(HikeConstants.RECEIVE_SMS_PREF, false)) {
						showSMSClientDialog(false, null, false);
					} else {
						sendAllUnsentMessagesAsSMS(true);
						Utils.setSendUndeliveredSmsSetting(context, true);
					}
				}
				dialog.dismiss();
			}
		});

		dialog.show();
	}

	private void showSMSClientDialog(final boolean triggeredFromToggle,
			final CompoundButton checkBox, final boolean showingNativeInfoDialog) {
		final Dialog dialog = new Dialog(chatThread, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.enable_sms_client_popup);
		dialog.setCancelable(showingNativeInfoDialog);

		TextView header = (TextView) dialog.findViewById(R.id.header);
		TextView body = (TextView) dialog.findViewById(R.id.body);
		Button btnOk = (Button) dialog.findViewById(R.id.btn_ok);
		Button btnCancel = (Button) dialog.findViewById(R.id.btn_cancel);

		header.setText(showingNativeInfoDialog ? R.string.native_header
				: R.string.use_hike_for_sms);
		body.setText(showingNativeInfoDialog ? R.string.native_info
				: R.string.use_hike_for_sms_info);

		if (showingNativeInfoDialog) {
			btnCancel.setVisibility(View.GONE);
			btnOk.setText(R.string.continue_txt);
		} else {
			btnCancel.setText(R.string.cancel);
			btnOk.setText(R.string.allow);
		}

		btnOk.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (showingNativeInfoDialog) {
					if (!PreferenceManager.getDefaultSharedPreferences(context)
							.getBoolean(HikeConstants.RECEIVE_SMS_PREF, false)) {
						showSMSClientDialog(triggeredFromToggle, checkBox,
								false);
					}
				} else {
					Utils.setReceiveSmsSetting(context, true);
					if (!triggeredFromToggle) {
						sendAllUnsentMessagesAsSMS(true);
					}
					if (!preferences.getBoolean(
							HikeMessengerApp.SHOWN_SMS_SYNC_POPUP, false)) {
						HikeMessengerApp.getPubSub().publish(
								HikePubSub.SHOW_SMS_SYNC_DIALOG, null);
					}
				}
				if (showingNativeInfoDialog) {
					Editor editor = preferences.edit();
					editor.putBoolean(HikeMessengerApp.SHOWN_NATIVE_INFO_POPUP,
							true);
					editor.commit();
				}
				dialog.dismiss();
			}
		});

		dialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				if (showingNativeInfoDialog) {
					checkBox.setChecked(false);
				}
			}
		});

		btnCancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!showingNativeInfoDialog) {
					Utils.setReceiveSmsSetting(context, false);
				}
				dialog.dismiss();
				if (triggeredFromToggle) {
					checkBox.setChecked(false);
				}
			}
		});

		dialog.show();
	}

	private List<ConvMessage> getAllUnsentMessages(boolean resetTimestamp) {
		List<ConvMessage> unsentMessages = new ArrayList<ConvMessage>();
		int count = 0;
		for (int i = lastSentMessagePosition; i >= 0; i--) {
			ConvMessage convMessage = convMessages.get(i);
			if (!convMessage.isSent()) {
				break;
			}
			if (!isMessageUndelivered(convMessage)) {
				break;
			}
			if (resetTimestamp
					&& convMessage.getState().ordinal() < State.SENT_CONFIRMED
							.ordinal()) {
				convMessage.setTimestamp(System.currentTimeMillis() / 1000);
			}
			unsentMessages.add(convMessage);
			if (++count >= HikeConstants.MAX_FALLBACK_NATIVE_SMS) {
				break;
			}
		}
		return unsentMessages;
	}

	private void sendAllUnsentMessagesAsSMS(boolean nativeSMS) {
		List<ConvMessage> unsentMessages = getAllUnsentMessages(true);
		Log.d(getClass().getSimpleName(),
				"Unsent messages: " + unsentMessages.size());

		if (nativeSMS) {
			HikeMessengerApp.getPubSub().publish(
					HikePubSub.SEND_NATIVE_SMS_FALLBACK, unsentMessages);
		} else {
			if (conversation.isOnhike()) {
				HikeMessengerApp.getPubSub().publish(
						HikePubSub.SEND_HIKE_SMS_FALLBACK, unsentMessages);
			} else {
				for (ConvMessage convMessage : unsentMessages) {
					HikeMessengerApp.getPubSub().publish(
							HikePubSub.MQTT_PUBLISH, convMessage.serialize());
					convMessage.setTimestamp(System.currentTimeMillis() / 1000);
				}
				notifyDataSetChanged();
			}
		}
	}

	private boolean isMessageUndelivered(ConvMessage convMessage) {
		boolean fileUploaded = true;
		boolean isGroupChatInternational = false;
		if (convMessage.isFileTransferMessage()) {
			HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			fileUploaded = !TextUtils.isEmpty(hikeFile.getFileKey());
		}
		if (conversation instanceof GroupConversation) {
			isGroupChatInternational = !HikeMessengerApp.isIndianUser();
		}
		return ((!convMessage.isSMS() && convMessage.getState().ordinal() < State.SENT_DELIVERED
				.ordinal()) || (convMessage.isSMS() && convMessage.getState()
				.ordinal() < State.SENT_CONFIRMED.ordinal()))
				&& fileUploaded && !isGroupChatInternational;
	}

	enum VoiceMessagePlayerState {
		PLAYING, PAUSED, STOPPED
	};

	private class VoiceMessagePlayer {
		String fileKey;
		MediaPlayer mediaPlayer;
		ImageView fileBtn;
		TextView durationTxt;
		Handler handler;
		VoiceMessagePlayerState playerState;

		public VoiceMessagePlayer() {
			handler = new Handler();
		}

		public void playMessage(HikeFile hikeFile) {
			Utils.blockOrientationChange(chatThread);

			playerState = VoiceMessagePlayerState.PLAYING;
			fileKey = hikeFile.getFileKey();

			try {
				mediaPlayer = new MediaPlayer();
				mediaPlayer.setDataSource(hikeFile.getFilePath());
				mediaPlayer.prepare();
				mediaPlayer.start();

				setFileBtnResource();

				mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
					@Override
					public void onCompletion(MediaPlayer mp) {
						resetPlayer();
					}
				});
				handler.post(updateTimer);
			} catch (IllegalArgumentException e) {
				Log.w(getClass().getSimpleName(), e);
			} catch (IllegalStateException e) {
				Log.w(getClass().getSimpleName(), e);
			} catch (IOException e) {
				Log.w(getClass().getSimpleName(), e);
			}
		}

		public void pausePlayer() {
			Utils.unblockOrientationChange(chatThread);
			if (mediaPlayer == null) {
				return;
			}
			playerState = VoiceMessagePlayerState.PAUSED;
			mediaPlayer.pause();
			setTimer();
			setFileBtnResource();
		}

		public void resumePlayer() {
			if (mediaPlayer == null) {
				return;
			}
			Utils.blockOrientationChange(chatThread);
			playerState = VoiceMessagePlayerState.PLAYING;
			mediaPlayer.start();
			handler.post(updateTimer);
			setFileBtnResource();
		}

		public void resetPlayer() {
			Utils.unblockOrientationChange(chatThread);
			playerState = VoiceMessagePlayerState.STOPPED;

			setTimer();
			setFileBtnResource();

			if (mediaPlayer != null) {
				mediaPlayer.stop();
				mediaPlayer.reset();
				mediaPlayer.release();
				mediaPlayer = null;
			}
			fileBtn = null;
			durationTxt = null;
		}

		public String getFileKey() {
			return fileKey;
		}

		public VoiceMessagePlayerState getPlayerState() {
			return playerState;
		}

		public void setDurationTxt(TextView durationTxt) {
			this.durationTxt = durationTxt;
			setTimer();
		}

		public void setFileBtn(ImageView fileBtn) {
			this.fileBtn = fileBtn;
		}

		public void setFileBtnResource() {
			if (fileBtn == null) {
				return;
			}
			String btnFileKey = (String) fileBtn.getTag();
			if (!fileKey.equals(btnFileKey)) {
				return;
			}
			fileBtn.setImageResource(playerState != VoiceMessagePlayerState.PLAYING ? R.drawable.ic_open_received_file
					: R.drawable.ic_pause_audio);
			fileBtn.setBackgroundResource(R.drawable.bg_red_btn_selector);
		}

		Runnable updateTimer = new Runnable() {

			@Override
			public void run() {
				setTimer();
				if (playerState == VoiceMessagePlayerState.PLAYING) {
					handler.postDelayed(updateTimer, 500);
				}
			}
		};

		private void setTimer() {
			if (durationTxt == null || fileKey == null || mediaPlayer == null) {
				return;
			}
			String txtFileKey = (String) durationTxt.getTag();
			if (!fileKey.equals(txtFileKey)) {
				return;
			}
			try {
				switch (playerState) {
				case PLAYING:
				case PAUSED:
					Utils.setupFormattedTime(durationTxt,
							mediaPlayer.getCurrentPosition() / 1000);
					break;
				case STOPPED:
					Utils.setupFormattedTime(durationTxt,
							mediaPlayer.getDuration() / 1000);
					break;

				}
			} catch (IllegalStateException e) {
				/*
				 * This can be thrown if we try to get the duration of the media
				 * player when it has already stopped.
				 */
				Log.w(getClass().getSimpleName(), e);
			}
		}
	}

	public void resetPlayerIfRunning() {
		voiceMessagePlayer.resetPlayer();
	}
	
	public StickerLoader getStickerLoader()
	{
		return largeStickerLoader;
	}

	public IconLoader getIconImageLoader()
	{
		return iconLoader;
	}
}
