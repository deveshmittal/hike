package com.bsb.hike.adapters;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.util.Log;
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
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.tasks.DownloadFileTask;
import com.bsb.hike.tasks.DownloadSingleStickerTask;
import com.bsb.hike.tasks.UploadContactOrLocationTask;
import com.bsb.hike.tasks.UploadFileTask;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.FileTransferTaskBase;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;
import com.bsb.hike.view.CircularProgress;

public class MessagesAdapter extends BaseAdapter implements OnClickListener,
		OnLongClickListener, OnCheckedChangeListener {

	public static final int LAST_READ_CONV_MESSAGE_ID = -911;

	private enum ViewType {
		RECEIVE, SEND_SMS, SEND_HIKE, PARTICIPANT_INFO, FILE_TRANSFER_SEND, FILE_TRANSFER_RECEIVE, TYPING, LAST_READ, STATUS_MESSAGE, SMS_TOGGLE
	};

	private class ViewHolder {
		LinearLayout timestampContainer;
		TextView messageTextView;
		TextView timestampTextView;
		ImageView image;
		ViewGroup container;
		ImageView fileThumb;
		ImageView showFileBtn;
		CircularProgress circularProgress;
		View marginView;
		TextView participantNameFT;
		View loadingThumb;
		ImageView poke;
		View messageContainer;
		TextView undeliveredMsgTextView;
		CheckBox smsToggle;
		TextView hikeSmsText;
		TextView regularSmsText;
		View stickerPlaceholder;
		ProgressBar stickerLoader;
		TextView stickerParticipantName;
		ImageView stickerImage;
		View bubbleContainer;
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

	public MessagesAdapter(Context context, ArrayList<ConvMessage> objects,
			Conversation conversation, ChatThread chatThread) {
		this.context = context;
		this.convMessages = objects;
		this.conversation = conversation;
		this.chatThread = chatThread;
		this.voiceMessagePlayer = new VoiceMessagePlayer();
		this.preferences = context.getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		setLastSentMessagePosition();
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
		new AsyncTask<Void, Void, Void>() {

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
		}.execute();
	}

	/**
	 * Returns what type of View this item is going to result in * @return an
	 * integer
	 */
	@Override
	public int getItemViewType(int position) {
		ConvMessage convMessage = getItem(position);
		ViewType type;
		if (convMessage == null) {
			type = ViewType.TYPING;
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
			case TYPING:
				v = inflater.inflate(R.layout.typing_layout, null);
				break;
			case LAST_READ:
				v = inflater.inflate(R.layout.last_read_line, null);
				break;
			case STATUS_MESSAGE:
				v = inflater.inflate(R.layout.profile_timeline_item, null);

				holder.image = (ImageView) v.findViewById(R.id.status_type);
				holder.messageTextView = (TextView) v
						.findViewById(R.id.status_text);
				holder.fileThumb = (ImageView) v.findViewById(R.id.status_pic);
				holder.timestampTextView = (TextView) v.findViewById(R.id.time);
				holder.marginView = v.findViewById(R.id.empty_view);
				holder.container = (ViewGroup) v
						.findViewById(R.id.content_container);
				break;
			case PARTICIPANT_INFO:
				v = inflater.inflate(R.layout.message_item_receive, null);

				holder.image = (ImageView) v.findViewById(R.id.avatar);
				holder.timestampContainer = (LinearLayout) v
						.findViewById(R.id.timestamp_container);
				holder.timestampTextView = (TextView) v
						.findViewById(R.id.timestamp);
				holder.container = (ViewGroup) v
						.findViewById(R.id.participant_info_container);

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

				showFileTransferElements(holder);
			case SEND_HIKE:
			case SEND_SMS:
				if (v == null) {
					v = inflater.inflate(R.layout.message_item_send, parent,
							false);
				}

				holder.image = (ImageView) v
						.findViewById(R.id.msg_status_indicator);
				holder.timestampContainer = (LinearLayout) v
						.findViewById(R.id.timestamp_container);
				holder.timestampTextView = (TextView) v
						.findViewById(R.id.timestamp);
				holder.poke = (ImageView) v.findViewById(R.id.poke_sent);
				holder.messageContainer = v
						.findViewById(R.id.sent_message_container);

				holder.messageTextView = (TextView) v
						.findViewById(R.id.message_send);

				holder.undeliveredMsgTextView = (TextView) v
						.findViewById(R.id.msg_not_sent);

				holder.stickerPlaceholder = v
						.findViewById(R.id.sticker_placeholder);
				holder.stickerLoader = (ProgressBar) v
						.findViewById(R.id.loading_progress);
				holder.stickerParticipantName = (TextView) v
						.findViewById(R.id.participant_name);
				holder.stickerImage = (ImageView) v
						.findViewById(R.id.sticker_image);
				holder.bubbleContainer = v.findViewById(R.id.bubble_container);
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
				if (holder.messageTextView == null) {
					holder.messageTextView = (TextView) v
							.findViewById(R.id.message_receive);
				}
				holder.poke = (ImageView) v.findViewById(R.id.poke_receive);
				holder.messageContainer = v
						.findViewById(R.id.receive_message_container);
				holder.timestampContainer = (LinearLayout) v
						.findViewById(R.id.timestamp_container);
				holder.timestampTextView = (TextView) v
						.findViewById(R.id.timestamp);
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

		if (convMessage == null
				|| (convMessage.getMsgID() == LAST_READ_CONV_MESSAGE_ID)) {
			return v;
		}

		if (viewType == ViewType.SMS_TOGGLE) {
			smsToggleSubtext = holder.messageTextView;
			hikeSmsText = holder.hikeSmsText;
			regularSmsText = holder.regularSmsText;

			boolean smsToggleOn = PreferenceManager
					.getDefaultSharedPreferences(context).getBoolean(
							HikeConstants.SEND_SMS_PREF, false);
			holder.smsToggle.setChecked(smsToggleOn);
			setSmsToggleSubtext(smsToggleOn);

			holder.smsToggle.setOnCheckedChangeListener(this);
			return v;
		}

		if (shouldDisplayTimestamp(position)) {
			String dateFormatted = convMessage.getTimestampFormatted(false,
					context);
			holder.timestampTextView.setText(dateFormatted.toUpperCase());
			holder.timestampContainer.setVisibility(View.VISIBLE);
		} else {
			if (holder.timestampContainer != null) {
				holder.timestampContainer.setVisibility(View.GONE);
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

			MessageMetadata metadata = convMessage.getMetadata();
			if (infoState == ParticipantInfoState.PARTICIPANT_JOINED) {
				JSONArray participantInfoArray = metadata
						.getGcjParticipantInfo();

				TextView participantInfo = (TextView) inflater.inflate(
						R.layout.participant_info, null);

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
						R.drawable.ic_hike_user);

				((ViewGroup) holder.container).addView(participantInfo);
			} else if (infoState == ParticipantInfoState.PARTICIPANT_LEFT
					|| infoState == ParticipantInfoState.GROUP_END) {
				TextView participantInfo = (TextView) inflater.inflate(
						R.layout.participant_info, null);

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
								R.layout.participant_info, null);
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
							.getGroupParticipant(participantMsisdn)
							.getContactInfo().getFirstName();
					message = Utils.getFormattedParticipantInfo(
							String.format(context
									.getString(R.string.left_conversation),
									name), name);
				} else {
					message = context.getString(R.string.group_chat_end);
				}
				setTextAndIconForSystemMessages(participantInfo, message,
						R.drawable.ic_left_chat);

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
							.getGroupParticipant(participantMsisdn)
							.getContactInfo().getFirstName();
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

				TextView mainMessage = (TextView) inflater.inflate(
						R.layout.participant_info, null);
				setTextAndIconForSystemMessages(
						mainMessage,
						Utils.getFormattedParticipantInfo(message, name),
						infoState == ParticipantInfoState.USER_JOIN ? R.drawable.ic_hike_user
								: R.drawable.ic_opt_in);

				TextView creditsMessageView = null;
				if (metadata.getCredits() != -1) {
					int credits = metadata.getCredits();
					String creditsMessage = String.format(context.getString(
							R.string.earned_credits, credits));
					String highlight = String.format(context.getString(
							R.string.earned_credits_highlight, credits));

					creditsMessageView = (TextView) inflater.inflate(
							R.layout.participant_info, null);
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
								.getGroupParticipant(msisdn).getContactInfo()
								.getFirstName();
				String message = String
						.format(context.getString(convMessage
								.getParticipantInfoState() == ParticipantInfoState.CHANGED_GROUP_NAME ? R.string.change_group_name
								: R.string.change_group_image), participantName);

				TextView mainMessage = (TextView) inflater.inflate(
						R.layout.participant_info, null);
				setTextAndIconForSystemMessages(mainMessage,
						Utils.getFormattedParticipantInfo(message,
								participantName), R.drawable.ic_group_info);

				((ViewGroup) holder.container).addView(mainMessage);
			} else if (infoState == ParticipantInfoState.BLOCK_INTERNATIONAL_SMS) {
				String info = context.getString(R.string.block_internation_sms);
				String textToHighlight = context
						.getString(R.string.block_internation_sms_bold_text);

				TextView mainMessage = (TextView) inflater.inflate(
						R.layout.participant_info, null);
				setTextAndIconForSystemMessages(
						mainMessage,
						Utils.getFormattedParticipantInfo(info, textToHighlight),
						R.drawable.ic_no_int_sms);

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

				TextView mainMessage = (TextView) inflater.inflate(
						R.layout.participant_info, null);
				setTextAndIconForSystemMessages(mainMessage,
						Utils.getFormattedParticipantInfo(message, name),
						conversation.isOnhike() ? R.drawable.ic_hike_user
								: R.drawable.ic_sms_user);

				((ViewGroup) holder.container).addView(mainMessage);
			} else if (infoState == ParticipantInfoState.DND_USER) {
				JSONArray dndNumbers = metadata.getDndNumbers();

				TextView dndMessage = (TextView) inflater.inflate(
						R.layout.participant_info, null);

				if (dndNumbers != null && dndNumbers.length() > 0) {
					StringBuilder dndNamesSB = new StringBuilder();
					for (int i = 0; i < dndNumbers.length(); i++) {
						String name = conversation instanceof GroupConversation ? ((GroupConversation) conversation)
								.getGroupParticipant(dndNumbers.optString(i))
								.getContactInfo().getFirstName()
								: Utils.getFirstName(conversation.getLabel());
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
						ssb.setSpan(new ForegroundColorSpan(0xff666666),
								convMessage.getMessage().indexOf(dndNames),
								convMessage.getMessage().indexOf(dndNames)
										+ dndNames.length(),
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					} else {
						ssb = new SpannableStringBuilder(
								convMessage.getMessage());
						ssb.setSpan(new ForegroundColorSpan(0xff666666),
								convMessage.getMessage().indexOf(dndNames),
								convMessage.getMessage().indexOf(dndNames)
										+ dndNames.length(),
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						ssb.setSpan(new ForegroundColorSpan(0xff666666),
								convMessage.getMessage().lastIndexOf(dndNames),
								convMessage.getMessage().lastIndexOf(dndNames)
										+ dndNames.length(),
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}

					setTextAndIconForSystemMessages(dndMessage, ssb,
							R.drawable.ic_waiting_dnd);
					LayoutParams lp = new LayoutParams(
							LayoutParams.MATCH_PARENT,
							LayoutParams.WRAP_CONTENT);
					lp.setMargins(left, top, right, 0);
					dndMessage.setLayoutParams(lp);

					((ViewGroup) holder.container).addView(dndMessage);
				}
			}
			return v;
		} else if (infoState == ParticipantInfoState.STATUS_MESSAGE) {
			v.findViewById(R.id.div1).setVisibility(View.GONE);
			v.findViewById(R.id.div2).setVisibility(View.GONE);
			v.findViewById(R.id.div3).setVisibility(View.GONE);

			holder.container
					.setBackgroundResource(R.drawable.bg_status_chat_thread);

			StatusMessage statusMessage = convMessage.getMetadata()
					.getStatusMessage();
			if (statusMessage.getStatusMessageType() == StatusMessageType.TEXT) {
				holder.image.setImageResource(R.drawable.ic_text_status);
				SmileyParser smileyParser = SmileyParser.getInstance();
				holder.messageTextView.setText(smileyParser.addSmileySpans(
						statusMessage.getText(), true));
				Linkify.addLinks(holder.messageTextView, Linkify.ALL);

			} else if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC) {
				holder.image.setImageResource(R.drawable.ic_profile_pic_status);
				holder.messageTextView.setText(R.string.changed_profile);
			}
			if (statusMessage.hasMood()) {
				holder.image.setBackgroundDrawable(null);
				holder.image
						.setImageResource(Utils.getMoodsResource()[statusMessage
								.getMoodId()]);
			} else {
				holder.image.setBackgroundResource(R.drawable.bg_status_type);
			}
			holder.timestampTextView.setText(convMessage.getTimestampFormatted(
					true, context));
			holder.fileThumb.setVisibility(View.GONE);

			int padding = (int) (10 * Utils.densityMultiplier);
			holder.container.setPadding(padding, padding, padding, padding);

			holder.marginView.setVisibility(View.VISIBLE);

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
							&& statusIdForTip.equals(statusMessage
									.getMappedId())) {
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

		holder.stickerPlaceholder.setVisibility(View.GONE);
		MessageMetadata metadata = convMessage.getMetadata();
		if (convMessage.isFileTransferMessage()) {
			HikeFile hikeFile = metadata.getHikeFiles().get(0);

			boolean showThumbnail = ((convMessage.isSent())
					|| (conversation instanceof GroupConversation)
					|| (!TextUtils.isEmpty(conversation.getContactName())) || (hikeFile
					.wasFileDownloaded() && !ChatThread.fileTransferTaskMap
					.containsKey(convMessage.getMsgID())))
					&& (hikeFile.getThumbnail() != null)
					&& (hikeFile.getHikeFileType() != HikeFileType.UNKNOWN);

			if (convMessage.isSent()
					&& (hikeFile.getHikeFileType() == HikeFileType.IMAGE || hikeFile
							.getHikeFileType() == HikeFileType.LOCATION)
					&& !showThumbnail) {
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
					holder.fileThumb.setBackgroundDrawable(hikeFile
							.getThumbnail());
				} else {
					switch (hikeFile.getHikeFileType()) {
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

			LayoutParams fileThumbParams = (LayoutParams) holder.fileThumb
					.getLayoutParams();
			fileThumbParams.width = (int) (showThumbnail ? (100 * Utils.densityMultiplier)
					: LayoutParams.WRAP_CONTENT);
			fileThumbParams.height = (int) (showThumbnail ? (100 * Utils.densityMultiplier)
					: LayoutParams.WRAP_CONTENT);
			holder.fileThumb.setLayoutParams(fileThumbParams);

			holder.fileThumb
					.setImageResource(((hikeFile.getHikeFileType() == HikeFileType.VIDEO) && (showThumbnail)) ? R.drawable.ic_video_play
							: 0);

			holder.messageTextView.setVisibility(!showThumbnail ? View.VISIBLE
					: View.GONE);

			if (hikeFile.getHikeFileType() == HikeFileType.AUDIO_RECORDING) {
				Utils.setupFormattedTime(holder.messageTextView,
						hikeFile.getRecordingDuration());
			} else if (hikeFile.getHikeFileType() == HikeFileType.UNKNOWN) {
				holder.messageTextView.setText(context
						.getString(R.string.unknown_msg));
			} else {
				holder.messageTextView.setText(hikeFile.getFileName());
			}

			if (holder.showFileBtn != null) {
				if (hikeFile.wasFileDownloaded()
						&& hikeFile.getThumbnail() != null
						&& !ChatThread.fileTransferTaskMap
								.containsKey(convMessage.getMsgID())) {
					holder.showFileBtn.setVisibility(View.GONE);

				} else {
					LayoutParams lp = (LayoutParams) holder.showFileBtn
							.getLayoutParams();
					lp.gravity = !showThumbnail ? Gravity.CENTER_VERTICAL
							: Gravity.BOTTOM;
					holder.showFileBtn.setLayoutParams(lp);

					if (hikeFile.getHikeFileType() == HikeFileType.AUDIO_RECORDING) {
						holder.showFileBtn.setVisibility(View.VISIBLE);
						holder.showFileBtn.setBackgroundResource(0);
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
							holder.messageTextView
									.setTag(hikeFile.getFileKey());
							voiceMessagePlayer
									.setDurationTxt(holder.messageTextView);
						} else {
							if (!convMessage.isSent()
									|| !ChatThread.fileTransferTaskMap
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
			if (!convMessage.isSent()
					&& (conversation instanceof GroupConversation)) {
				holder.participantNameFT
						.setText(((GroupConversation) conversation)
								.getGroupParticipant(
										convMessage.getGroupParticipantMsisdn())
								.getContactInfo().getFirstName()
								+ HikeConstants.SEPARATOR);
				holder.participantNameFT.setVisibility(View.VISIBLE);
			}
			holder.messageContainer.setTag(convMessage);
			holder.messageContainer.setOnClickListener(this);
			holder.messageContainer.setOnLongClickListener(this);
		} else if (metadata != null && metadata.isPokeMessage()) {
			holder.messageContainer.setVisibility(View.GONE);
			holder.poke.setVisibility(View.VISIBLE);
			holder.poke
					.setImageResource(convMessage.isSent() ? R.drawable.ic_nudge_hike_sent
							: R.drawable.ic_nudge_hike_received);
		} else if (convMessage.isStickerMessage()) {
			holder.messageContainer.setVisibility(View.GONE);
			holder.poke.setVisibility(View.GONE);
			holder.stickerPlaceholder.setVisibility(View.VISIBLE);
			holder.stickerPlaceholder.setBackgroundResource(0);

			holder.stickerImage.setVisibility(View.GONE);
			holder.stickerLoader.setVisibility(View.GONE);
			holder.stickerParticipantName.setVisibility(View.GONE);

			Sticker sticker = metadata.getSticker();

			if (convMessage.isGroupChat() && !convMessage.isSent()
					&& convMessage.getGroupParticipantMsisdn() != null) {
				holder.stickerParticipantName.setVisibility(View.VISIBLE);
				holder.stickerParticipantName
						.setText(((GroupConversation) conversation)
								.getGroupParticipant(
										convMessage.getGroupParticipantMsisdn())
								.getContactInfo().getFirstName()
								+ HikeConstants.SEPARATOR);
			}
			/*
			 * If this is the first category, then the sticker are a part of the
			 * app bundle itself
			 */
			if (sticker.getStickerIndex() != -1) {
				holder.stickerImage.setVisibility(View.VISIBLE);
				holder.stickerImage
						.setImageResource(EmoticonConstants.LOCAL_STICKER_RES_IDS[sticker
								.getStickerIndex()]);
			} else {
				String categoryId = sticker.getCategoryId();
				String stickerId = sticker.getStickerId();

				String categoryDirPath = Utils
						.getStickerDirectoryForCategoryId(context, categoryId)
						+ HikeConstants.LARGE_STICKER_ROOT;
				File stickerImage = new File(categoryDirPath, stickerId);

				String key = categoryId + stickerId;
				boolean downloadingSticker = ChatThread.stickerTaskMap
						.containsKey(key);

				if (stickerImage.exists() && !downloadingSticker) {
					holder.stickerImage.setVisibility(View.VISIBLE);
					holder.stickerImage.setImageBitmap(BitmapFactory
							.decodeFile(stickerImage.getPath()));
				} else {
					holder.stickerLoader.setVisibility(View.VISIBLE);
					holder.stickerPlaceholder
							.setBackgroundResource(R.drawable.bg_sticker_placeholder);

					/*
					 * Download the sticker if not already downoading.
					 */
					if (!downloadingSticker) {
						DownloadSingleStickerTask downloadSingleStickerTask = new DownloadSingleStickerTask(
								context, categoryId, stickerId);
						ChatThread.stickerTaskMap.put(key,
								downloadSingleStickerTask);
						downloadSingleStickerTask.execute();
					}
				}
			}

		} else {
			holder.stickerPlaceholder.setVisibility(View.GONE);
			holder.messageContainer.setVisibility(View.VISIBLE);
			holder.poke.setVisibility(View.GONE);

			CharSequence markedUp = convMessage.getMessage();
			// Fix for bug where if a participant leaves the group chat, the
			// participant's name is never shown
			if (convMessage.isGroupChat() && !convMessage.isSent()
					&& convMessage.getGroupParticipantMsisdn() != null) {
				markedUp = Utils
						.addContactName(
								((GroupConversation) conversation)
										.getGroupParticipant(
												convMessage
														.getGroupParticipantMsisdn())
										.getContactInfo().getFirstName(),
								markedUp);
			}
			SmileyParser smileyParser = SmileyParser.getInstance();
			markedUp = smileyParser.addSmileySpans(markedUp, false);
			holder.messageTextView.setText(markedUp);
			Linkify.addLinks(holder.messageTextView, Linkify.ALL);
			Linkify.addLinks(holder.messageTextView, Utils.shortCodeRegex,
					"tel:");
		}

		if (convMessage.isFileTransferMessage()
				&& ChatThread.fileTransferTaskMap.containsKey(convMessage
						.getMsgID())) {
			FileTransferTaskBase fileTransferTask = ChatThread.fileTransferTaskMap
					.get(convMessage.getMsgID());
			holder.circularProgress.setVisibility(View.VISIBLE);
			holder.circularProgress.setProgressAngle(fileTransferTask
					.getProgress());
			if (convMessage.isSent()) {
				holder.image.setVisibility(View.INVISIBLE);
			}
		} else if (convMessage.isFileTransferMessage()
				&& convMessage.isSent()
				&& TextUtils.isEmpty(metadata.getHikeFiles().get(0)
						.getFileKey())) {
			if (holder.circularProgress != null) {
				holder.circularProgress.setVisibility(View.INVISIBLE);
			}
			holder.image.setVisibility(View.VISIBLE);
			holder.image.setImageResource(R.drawable.ic_download_failed);
		} else {
			if (holder.circularProgress != null) {
				holder.circularProgress.setVisibility(View.INVISIBLE);
			}
			/*
			 * set the image resource, getImageState returns -1 if this is a
			 * received image
			 */
			int resId = convMessage.getImageState();
			if (resId > 0) {
				if (convMessage.getState() == State.SENT_UNCONFIRMED) {
					showTryingAgainIcon(holder.image,
							convMessage.getTimestamp());
				} else {
					holder.image.setImageResource(resId);
					holder.image.setAnimation(null);
					holder.image.setVisibility(View.VISIBLE);
				}
				if (position == lastSentMessagePosition) {
					if (isMessageUndelivered(convMessage)) {
						View container = holder.bubbleContainer;

						scheduleUndeliveredText(holder.undeliveredMsgTextView,
								container, convMessage.getTimestamp());
					} else {
						holder.undeliveredMsgTextView.setVisibility(View.GONE);
					}
				} else {
					holder.undeliveredMsgTextView.setVisibility(View.GONE);
				}
			} else if (convMessage.isSent()) {
				holder.image.setImageResource(0);
			} else {
				holder.image
						.setImageDrawable(convMessage.isGroupChat() ? IconCacheManager
								.getInstance()
								.getIconForMSISDN(
										convMessage.getGroupParticipantMsisdn())
								: IconCacheManager.getInstance()
										.getIconForMSISDN(
												convMessage.getMsisdn()));
			}
		}

		if (convMessage.isSent() && holder.messageContainer != null) {
			/* label outgoing hike conversations in green */
			holder.messageContainer
					.setBackgroundResource(!convMessage.isSMS() ? R.drawable.ic_bubble_blue_selector
							: R.drawable.ic_bubble_green_selector);
		}

		return v;
	}

	private void setFileButtonResource(ImageView button,
			ConvMessage convMessage, HikeFile hikeFile) {
		if (ChatThread.fileTransferTaskMap.containsKey(convMessage.getMsgID())) {
			button.setImageResource(R.drawable.ic_open_file_disabled);
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

	private void scheduleUndeliveredText(TextView tv, View container, long ts) {
		if (showUndeliveredMessage != null) {
			handler.removeCallbacks(showUndeliveredMessage);
		}

		long diff = (((long) System.currentTimeMillis() / 1000) - ts);

		if (Utils.isUserOnline(context)
				&& diff < HikeConstants.DEFAULT_UNDELIVERED_WAIT_TIME) {
			showUndeliveredMessage = new ShowUndeliveredMessage(tv, container);
			handler.postDelayed(showUndeliveredMessage,
					(HikeConstants.DEFAULT_UNDELIVERED_WAIT_TIME - diff) * 1000);
		} else {
			showUndeliveredTextAndSetClick(tv, container, true);
		}
	}

	private void showFileTransferElements(ViewHolder holder) {
		holder.fileThumb.setVisibility(View.VISIBLE);
	}

	private boolean shouldDisplayTimestamp(int position) {
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
		return (current.getTimestamp() - previous.getTimestamp() > 60 * 5);
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
			if (convMessage.isSent()
					&& convMessage.equals(convMessages
							.get(lastSentMessagePosition))
					&& isMessageUndelivered(convMessage)) {
				long diff = (((long) System.currentTimeMillis() / 1000) - convMessage
						.getTimestamp());
				/*
				 * Only show fallback if the message has not been sent for our
				 * max wait time.
				 */
				if (diff >= HikeConstants.DEFAULT_UNDELIVERED_WAIT_TIME) {

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
						sendAllUnsentMessagesAsSMS(PreferenceManager
								.getDefaultSharedPreferences(context)
								.getBoolean(HikeConstants.SEND_SMS_PREF, false));
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
							&& !ChatThread.fileTransferTaskMap
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
						uploadTask.execute();
						ChatThread.fileTransferTaskMap.put(
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
					if (!ChatThread.fileTransferTaskMap.containsKey(convMessage
							.getMsgID())
							&& ((hikeFile.getHikeFileType() == HikeFileType.LOCATION)
									|| (hikeFile.getHikeFileType() == HikeFileType.CONTACT) || receivedFile
										.exists())) {
						openFile(hikeFile, convMessage, v);
					} else if (!ChatThread.fileTransferTaskMap
							.containsKey(convMessage.getMsgID())) {
						Log.d(getClass().getSimpleName(),
								"HIKEFILE: NAME: " + hikeFile.getFileName()
										+ " KEY: " + hikeFile.getFileKey()
										+ " TYPE: "
										+ hikeFile.getFileTypeString());
						DownloadFileTask downloadFile = new DownloadFileTask(
								context, receivedFile, hikeFile.getFileKey(),
								convMessage.getMsgID(),
								hikeFile.getHikeFileType());
						downloadFile.execute();
						ChatThread.fileTransferTaskMap.put(
								convMessage.getMsgID(), downloadFile);
						notifyDataSetChanged();
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
					R.color.sms_choice_unselected));
			regularSmsText.setTextColor(context.getResources().getColor(
					R.color.sms_choice_selected));
		} else {
			hikeSmsText.setTextColor(context.getResources().getColor(
					R.color.sms_choice_selected));
			regularSmsText.setTextColor(context.getResources().getColor(
					R.color.sms_choice_unselected));
		}
		smsToggleSubtext.setText(ssb);
	}

	private class ShowUndeliveredMessage implements Runnable {

		TextView tv;
		View container;

		public ShowUndeliveredMessage(TextView tv, View container) {
			this.tv = tv;
			this.container = container;
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
				showUndeliveredTextAndSetClick(tv, container, true);
			}
		}
	}

	private void showUndeliveredTextAndSetClick(TextView tv, View container,
			boolean fromHandler) {
		tv.setVisibility(View.VISIBLE);
		tv.setText(getUndeliveredTextRes());

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
			res = conversation.isOnhike()
					&& !(conversation instanceof GroupConversation) ? R.string.msg_unsent
					: R.string.sms_undelivered;
		} else {
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

		View hikeSMS1 = dialog.findViewById(R.id.hike_sms_container1);
		View hikeSMS2 = dialog.findViewById(R.id.hike_sms_container2);
		View nativeSMS1 = dialog.findViewById(R.id.native_sms_container1);
		View nativeSMS2 = dialog.findViewById(R.id.native_sms_container2);
		View orContainer = dialog.findViewById(R.id.or_container);

		hikeSMS1.setVisibility(nativeOnly ? View.GONE : View.VISIBLE);
		hikeSMS2.setVisibility(nativeOnly ? View.GONE : View.VISIBLE);
		orContainer.setVisibility(nativeOnly ? View.GONE : View.VISIBLE);

		if (conversation instanceof GroupConversation) {
			nativeSMS1.setVisibility(View.GONE);
			nativeSMS2.setVisibility(View.GONE);
			orContainer.setVisibility(View.GONE);
		}

		TextView hikeSmsText = (TextView) dialog
				.findViewById(R.id.hike_sms_text);
		final CheckBox sendHike = (CheckBox) dialog
				.findViewById(R.id.hike_sms_checkbox);

		TextView nativeSmsTextHead = (TextView) dialog
				.findViewById(R.id.native_sms_head_text);
		TextView nativeSmsText = (TextView) dialog
				.findViewById(R.id.native_sms_text);
		final CheckBox sendNative = (CheckBox) dialog
				.findViewById(R.id.native_sms_checkbox);
		ImageView avatar = (ImageView) dialog.findViewById(R.id.avatar);
		TextView nativeSMSInfo = (TextView) dialog
				.findViewById(R.id.native_sms_info);

		SharedPreferences prefs = preferences;

		String userMsisdn = prefs
				.getString(HikeMessengerApp.MSISDN_SETTING, "");
		avatar.setImageDrawable(IconCacheManager.getInstance()
				.getIconForMSISDN(userMsisdn));

		final Button sendBtn = (Button) dialog.findViewById(R.id.btn_send);
		sendBtn.setEnabled(false);

		String username = prefs.getString(HikeMessengerApp.NAME_SETTING, "");

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

		nativeSmsTextHead.setText(username);

		int numUnsentMessages = getAllUnsentMessages().size();
		if (numUnsentMessages == 1) {
			nativeSMSInfo.setText(R.string.native_sms_info1);
		} else {
			nativeSMSInfo.setText(context.getString(
					R.string.native_sms_info1_multiple,
					Integer.toString(numUnsentMessages)));
		}

		ConvMessage convMessage = convMessages.get(lastSentMessagePosition);
		hikeSmsText.setText(Utils.getMessageDisplayText(convMessage, context));
		nativeSmsText
				.setText(Utils.getMessageDisplayText(convMessage, context));

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

	private List<ConvMessage> getAllUnsentMessages() {
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
			if (convMessage.getState().ordinal() < State.SENT_CONFIRMED
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
		List<ConvMessage> unsentMessages = getAllUnsentMessages();
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
}