package com.bsb.hike.adapters;

import java.util.ArrayList;

import org.json.JSONArray;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.utils.FileTransferTaskBase;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CircularProgress;

public class MessagesAdapter extends BaseAdapter
{

	private enum ViewType
	{
		RECEIVE,
		SEND_SMS,
		SEND_HIKE,
		PARTICIPANT_INFO,
		FILE_TRANSFER_SEND,
		FILE_TRANSFER_RECEIVE,
		TYPING
	};

	private class ViewHolder
	{
		LinearLayout timestampContainer;
		TextView messageTextView;
		TextView timestampTextView;
		ImageView image;
		ViewGroup participantInfoContainer;
		ImageView fileThumb;
		ImageView showFileBtn;
		CircularProgress circularProgress;
		View marginView;
		TextView participantNameFT;
		View loadingThumb;
		View poke;
		View messageContainer;
	}

	private Conversation conversation;
	private ArrayList<ConvMessage> convMessages;
	private Context context;

	public MessagesAdapter(Context context, ArrayList<ConvMessage> objects, Conversation conversation)
	{
		this.context = context;
		this.convMessages = objects;
		this.conversation = conversation;
	}

	/**
	 * Returns what type of View this item is going to result in	 * @return an integer 
	 */
	@Override
	public int getItemViewType(int position)
	{
		ConvMessage convMessage = getItem(position);
		ViewType type;
		if(convMessage == null)
		{
			type = ViewType.TYPING;
		}
		else if (convMessage.isFileTransferMessage())
		{
			type = convMessage.isSent() ? ViewType.FILE_TRANSFER_SEND : ViewType.FILE_TRANSFER_RECEIVE;
		}
		else if (convMessage.getParticipantInfoState() != ParticipantInfoState.NO_INFO)
		{
			type = ViewType.PARTICIPANT_INFO;
		}
		else if (convMessage.isSent())
		{
			type = conversation.isOnhike() ? ViewType.SEND_HIKE : ViewType.SEND_SMS;
		}
		else
		{
			type = ViewType.RECEIVE;
		}

		return type.ordinal();
	}

	/**
	 * Returns how many distinct types of views this adapter creates.
	 * This is used to reuse the view (via convertView in getView)
	 * @return how many distinct views this adapter will create
	 */
	@Override
	public int getViewTypeCount()
	{
		return ViewType.values().length;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		final ConvMessage convMessage = getItem(position);
		ViewHolder holder = null;
		View v = convertView;
		if (v == null)
		{
			holder = new ViewHolder();

			switch(ViewType.values()[getItemViewType(position)])
			{
			case TYPING:
				v = inflater.inflate(R.layout.typing_layout, null);
				break;
			case PARTICIPANT_INFO:
				v = inflater.inflate(R.layout.message_item_receive, null);

				holder.image = (ImageView) v.findViewById(R.id.avatar);
				holder.timestampContainer = (LinearLayout) v.findViewById(R.id.timestamp_container);
				holder.timestampTextView = (TextView) v.findViewById(R.id.timestamp);
				holder.participantInfoContainer = (ViewGroup) v.findViewById(R.id.participant_info_container);

				holder.image.setVisibility(View.GONE);
				v.findViewById(R.id.receive_message_container).setVisibility(View.GONE);
				break;

			case FILE_TRANSFER_SEND:
				v = inflater.inflate(R.layout.message_item_send, parent, false);

				holder.fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
				holder.circularProgress = (CircularProgress) v.findViewById(R.id.file_transfer_progress);
				holder.marginView = v.findViewById(R.id.margin_view);
				holder.loadingThumb = v.findViewById(R.id.loading_thumb);

				showFileTransferElements(holder, v, true);
			case SEND_HIKE:
			case SEND_SMS:
				if(v == null)
				{
					v = inflater.inflate(R.layout.message_item_send, parent, false);
				}

				holder.image = (ImageView) v.findViewById(R.id.msg_status_indicator);
				holder.timestampContainer = (LinearLayout) v.findViewById(R.id.timestamp_container);
				holder.timestampTextView = (TextView) v.findViewById(R.id.timestamp);
				holder.poke = v.findViewById(R.id.poke_sent);
				holder.messageContainer = v.findViewById(R.id.sent_message_container);

				holder.messageTextView = (TextView) v.findViewById(R.id.message_send);
				/* label outgoing hike conversations in green */
				v.findViewById(R.id.sent_message_container).setBackgroundResource(conversation.isOnhike() ? R.drawable.ic_bubble_blue_selector : R.drawable.ic_bubble_green_selector);
				break;

			case FILE_TRANSFER_RECEIVE:
				v = inflater.inflate(R.layout.message_item_receive, parent, false);

				holder.fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
				holder.showFileBtn = (ImageView) v.findViewById(R.id.btn_open_file);
				holder.circularProgress = (CircularProgress) v.findViewById(R.id.file_transfer_progress);
				holder.messageTextView = (TextView) v.findViewById(R.id.message_receive_ft);
				holder.messageTextView.setVisibility(View.VISIBLE);

				holder.circularProgress.setVisibility(View.INVISIBLE);
				showFileTransferElements(holder, v, false);

				v.findViewById(R.id.message_receive).setVisibility(View.GONE);
			case RECEIVE:
			default:
				if(v == null)
				{
					v = inflater.inflate(R.layout.message_item_receive, parent, false);
				}

				holder.participantNameFT = (TextView) v.findViewById(R.id.participant_name_ft);
				holder.image = (ImageView) v.findViewById(R.id.avatar);
				if(holder.messageTextView == null)
				{
					holder.messageTextView = (TextView) v.findViewById(R.id.message_receive);
				}
				holder.poke = v.findViewById(R.id.poke_receive);
				holder.messageContainer = v.findViewById(R.id.receive_message_container);
				holder.timestampContainer = (LinearLayout) v.findViewById(R.id.timestamp_container);
				holder.timestampTextView = (TextView) v.findViewById(R.id.timestamp);
				holder.participantInfoContainer = (ViewGroup) v.findViewById(R.id.participant_info_container);

				holder.participantInfoContainer.setVisibility(View.GONE);
				break;
			}
			v.setTag(holder);
		}
		else
		{
			holder = (ViewHolder) v.getTag();
		}

		if (convMessage == null)
		{
			return v;
		}
		if (shouldDisplayTimestamp(position))
		{
			String dateFormatted = convMessage.getTimestampFormatted(false);
			holder.timestampTextView.setText(dateFormatted.toUpperCase());
			holder.timestampContainer.setVisibility(View.VISIBLE);
		}
		else
		{
			holder.timestampContainer.setVisibility(View.GONE);
		}

		if (convMessage.getParticipantInfoState() != ParticipantInfoState.NO_INFO)
		{
			((ViewGroup)holder.participantInfoContainer).removeAllViews();
			int positiveMargin = (int) (8 * Utils.densityMultiplier);
			int left = 0;
			int top = 0;
			int right = 0;
			int bottom = positiveMargin;

			MessageMetadata metadata = convMessage.getMetadata();
			if (convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_JOINED) 
			{
				JSONArray participantInfoArray = metadata.getGcjParticipantInfo();

				TextView participantInfo = (TextView) inflater.inflate(
						R.layout.participant_info, null);

				String message;
				String highlight = Utils.getGroupJoinHighlightText(participantInfoArray, (GroupConversation) conversation);

				if(metadata.isNewGroup())
				{
					message = String.format(context.getString(R.string.new_group_message), highlight);
				}
				else
				{
					message = String.format(context.getString(R.string.add_to_group_message), highlight);
				}

				setTextAndIconForSystemMessages(participantInfo, Utils.getFormattedParticipantInfo(message, highlight), R.drawable.ic_hike_user);

				((ViewGroup) holder.participantInfoContainer).addView(participantInfo);
			} 
			else if(convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_LEFT || convMessage.getParticipantInfoState() == ParticipantInfoState.GROUP_END)
			{
				TextView participantInfo = (TextView) inflater.inflate(R.layout.participant_info, null);

				CharSequence message;
				if (convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_LEFT) 
				{
					// Showing the block internation sms message if the user was booted because of that reason
					if(metadata.isShowBIS())
					{
						String info = context.getString(R.string.block_internation_sms);
						String textToHighlight = context.getString(R.string.block_internation_sms_bold_text);

						TextView mainMessage = (TextView) inflater.inflate(R.layout.participant_info, null);
						setTextAndIconForSystemMessages(mainMessage, Utils.getFormattedParticipantInfo(info, textToHighlight), R.drawable.ic_no_int_sms);

						LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
						lp.setMargins(left, top, right, bottom);
						mainMessage.setLayoutParams(lp);

						((ViewGroup) holder.participantInfoContainer).addView(mainMessage);
					}
					String participantMsisdn = metadata.getMsisdn();
					String name = ((GroupConversation) conversation).getGroupParticipant(participantMsisdn).getContactInfo().getFirstName();
					message = Utils.getFormattedParticipantInfo(String.format(context.getString(R.string.left_conversation), name), name);
				}
				else
				{
					message = context.getString(R.string.group_chat_end);
				}
				setTextAndIconForSystemMessages(participantInfo, message, R.drawable.ic_left_chat);

				LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				lp.setMargins(left, top, right, 0);
				participantInfo.setLayoutParams(lp);

				((ViewGroup) holder.participantInfoContainer).addView(participantInfo);
			}
			else if(convMessage.getParticipantInfoState() == ParticipantInfoState.USER_JOIN || convMessage.getParticipantInfoState() == ParticipantInfoState.USER_OPT_IN)
			{
				String name;
				String message;
				if(conversation instanceof GroupConversation)
				{
					String participantMsisdn = metadata.getMsisdn();
					name = ((GroupConversation) conversation).getGroupParticipant(participantMsisdn).getContactInfo().getFirstName();
					message = context.getString(
							convMessage.getParticipantInfoState() == ParticipantInfoState.USER_JOIN ? R.string.joined_hike : R.string.joined_conversation, name);
				}
				else
				{
					name = Utils.getFirstName(conversation.getLabel());
					message = context.getString(
							convMessage.getParticipantInfoState() == ParticipantInfoState.USER_JOIN ? R.string.joined_hike : R.string.optin_one_to_one, name);
				}

				TextView mainMessage = (TextView) inflater.inflate(R.layout.participant_info, null);
				setTextAndIconForSystemMessages(
						mainMessage, 
						Utils.getFormattedParticipantInfo(message, name), 
						convMessage.getParticipantInfoState() == ParticipantInfoState.USER_JOIN ? 
								R.drawable.ic_hike_user : R.drawable.ic_opt_in
						);

				TextView creditsMessageView = null;
				if(metadata.getCredits() != -1)
				{
					int credits = metadata.getCredits();
					String creditsMessage = String.format(context.getString(R.string.earned_credits, credits));
					String highlight = String.format(context.getString(R.string.earned_credits_highlight, credits));

					creditsMessageView = (TextView) inflater.inflate(R.layout.participant_info, null);
					setTextAndIconForSystemMessages(creditsMessageView, Utils.getFormattedParticipantInfo(creditsMessage, highlight), R.drawable.ic_got_credits);

					LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
					lp.setMargins(left, top, right, 0);
					creditsMessageView.setLayoutParams(lp);
				}
				LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				lp.setMargins(left, top, right, creditsMessageView != null ? bottom : 0);
				mainMessage.setLayoutParams(lp);

				((ViewGroup) holder.participantInfoContainer).addView(mainMessage);
				if(creditsMessageView != null)
				{
					((ViewGroup) holder.participantInfoContainer).addView(creditsMessageView);
				}
			}
			else if(convMessage.getParticipantInfoState() == ParticipantInfoState.CHANGED_GROUP_NAME)
			{
				String participantName = ((GroupConversation)conversation).getGroupParticipant(metadata.getMsisdn()).getContactInfo().getFirstName();
				String message = String.format(context.getString(R.string.change_group_name), participantName);

				TextView mainMessage = (TextView) inflater.inflate(R.layout.participant_info, null);
				setTextAndIconForSystemMessages(mainMessage, Utils.getFormattedParticipantInfo(message, participantName), R.drawable.ic_group_info);

				((ViewGroup) holder.participantInfoContainer).addView(mainMessage);
			}
			else if(convMessage.getParticipantInfoState() == ParticipantInfoState.BLOCK_INTERNATIONAL_SMS)
			{
				String info = convMessage.getMessage();
				String textToHighlight = context.getString(R.string.block_internation_sms_bold_text);

				TextView mainMessage = (TextView) inflater.inflate(R.layout.participant_info, null);
				setTextAndIconForSystemMessages(mainMessage, Utils.getFormattedParticipantInfo(info, textToHighlight), R.drawable.ic_no_int_sms);

				((ViewGroup) holder.participantInfoContainer).addView(mainMessage);
			}
			else if(convMessage.getParticipantInfoState() == ParticipantInfoState.INTRO_MESSAGE)
			{
				String name = Utils.getFirstName(conversation.getLabel());
				String message = context.getString(conversation.isOnhike() ? R.string.intro_hike_thread : R.string.intro_sms_thread, name);

				TextView mainMessage = (TextView) inflater.inflate(R.layout.participant_info, null);
				setTextAndIconForSystemMessages(
						mainMessage, 
						Utils.getFormattedParticipantInfo(message, name), 
						conversation.isOnhike() ? R.drawable.ic_hike_user : R.drawable.ic_sms_user);

				((ViewGroup) holder.participantInfoContainer).addView(mainMessage);
			}
			else
			{
				JSONArray dndNumbers = metadata.getDndNumbers();

				TextView dndMessage = (TextView) inflater.inflate(R.layout.participant_info, null);

				if(dndNumbers != null && dndNumbers.length()>0)
				{
					StringBuilder dndNamesSB = new StringBuilder(); 
					for(int i=0; i<dndNumbers.length(); i++)
					{
						String name = conversation instanceof GroupConversation ? 
								((GroupConversation)conversation).getGroupParticipant(dndNumbers.optString(i)).getContactInfo().getFirstName() : 
									Utils.getFirstName(conversation.getLabel());
								if(i < dndNumbers.length() - 2)
								{
									dndNamesSB.append(name + ", ");
								}
								else if(i < dndNumbers.length() - 1)
								{
									dndNamesSB.append(name + " and ");
								}
								else
								{
									dndNamesSB.append(name);
								}
					}
					String dndNames = dndNamesSB.toString();
					convMessage.setMessage(String.format(context.getString(conversation instanceof GroupConversation ? R.string.dnd_msg_gc : R.string.dnd_one_to_one), dndNames));

					SpannableStringBuilder ssb;
					if(conversation instanceof GroupConversation)
					{
						ssb = new SpannableStringBuilder(convMessage.getMessage());
						ssb.setSpan(new ForegroundColorSpan(0xff666666), convMessage.getMessage().indexOf(dndNames), convMessage.getMessage().indexOf(dndNames) + dndNames.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
					else
					{
						ssb = new SpannableStringBuilder(convMessage.getMessage());
						ssb.setSpan(new ForegroundColorSpan(0xff666666), convMessage.getMessage().indexOf(dndNames), convMessage.getMessage().indexOf(dndNames) + dndNames.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						ssb.setSpan(new ForegroundColorSpan(0xff666666), convMessage.getMessage().lastIndexOf(dndNames), convMessage.getMessage().lastIndexOf(dndNames) + dndNames.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}

					setTextAndIconForSystemMessages(dndMessage, ssb, R.drawable.ic_waiting_dnd);
					LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
					lp.setMargins(left, top, right, 0);
					dndMessage.setLayoutParams(lp);

					((ViewGroup) holder.participantInfoContainer).addView(dndMessage);
				}
			}
			return v;
		}
			

		MessageMetadata metadata = convMessage.getMetadata();
		if(convMessage.isFileTransferMessage())
		{
			HikeFile hikeFile = metadata.getHikeFiles().get(0);

			boolean showThumbnail = (
					(convMessage.isSent()) || 
					(conversation instanceof GroupConversation) ||
					(!TextUtils.isEmpty(conversation.getContactName())) || 
					(hikeFile.wasFileDownloaded() && 
							!ChatThread.fileTransferTaskMap.containsKey(convMessage.getMsgID()))) && 
							(hikeFile.getThumbnail() != null);

			if(convMessage.isSent() && 
					(hikeFile.getHikeFileType() == HikeFileType.IMAGE || hikeFile.getHikeFileType() == HikeFileType.LOCATION) && 
					!showThumbnail)
			{
				/*
				 *  This case should ideally only happen when downloading a picasa image or the thumbnail for a location. In that case we
				 *  won't have a thumbnail initially while the image is being downloaded.
				 */
				holder.loadingThumb.setVisibility(View.VISIBLE);
				holder.fileThumb.setVisibility(View.GONE);
			}
			else
			{
				if(holder.loadingThumb != null)
				{
					holder.loadingThumb.setVisibility(View.GONE);
					holder.fileThumb.setVisibility(View.VISIBLE);
				}

				holder.fileThumb.setBackgroundDrawable(
						showThumbnail ? 
								hikeFile.getThumbnail() : 
									context.getResources().getDrawable(
											hikeFile.getHikeFileType() == HikeFileType.IMAGE ? 
													R.drawable.ic_default_img : hikeFile.getHikeFileType() == HikeFileType.VIDEO ? 
															R.drawable.ic_default_mov : R.drawable.ic_default_audio));
			}

			LayoutParams fileThumbParams = (LayoutParams) holder.fileThumb.getLayoutParams();
			fileThumbParams.width = (int) (showThumbnail ? (100 * Utils.densityMultiplier) : LayoutParams.WRAP_CONTENT);
			fileThumbParams.height = (int) (showThumbnail ? (100 * Utils.densityMultiplier) : LayoutParams.WRAP_CONTENT);
			holder.fileThumb.setLayoutParams(fileThumbParams);

			holder.fileThumb.setImageResource(((hikeFile.getHikeFileType() == HikeFileType.VIDEO) && (showThumbnail)) ? R.drawable.ic_video_play : 0);

			holder.messageTextView.setVisibility(!showThumbnail ? View.VISIBLE : View.GONE);
			holder.messageTextView.setText(hikeFile.getFileName());

			if(holder.showFileBtn != null)
			{
				if(hikeFile.wasFileDownloaded() && hikeFile.getThumbnail() != null && !ChatThread.fileTransferTaskMap.containsKey(convMessage.getMsgID()))
				{
					holder.showFileBtn.setVisibility(View.GONE);
				}
				else
				{
					holder.showFileBtn.setVisibility(View.VISIBLE);
					LayoutParams lp = (LayoutParams) holder.showFileBtn.getLayoutParams();
					lp.gravity = !showThumbnail ? Gravity.CENTER_VERTICAL : Gravity.BOTTOM;
					holder.showFileBtn.setLayoutParams(lp);
					holder.showFileBtn.setImageResource(ChatThread.fileTransferTaskMap.containsKey(convMessage.getMsgID()) ?
							R.drawable.ic_open_file_disabled : 
								hikeFile.wasFileDownloaded() ? 
										R.drawable.ic_open_received_file : 
											R.drawable.ic_download_file);
				}
			}
			if(holder.marginView != null)
			{
				holder.marginView.setVisibility(hikeFile.getThumbnail() == null ? View.VISIBLE : View.GONE);
			}
			if(!convMessage.isSent() && (conversation instanceof GroupConversation))
			{
				holder.participantNameFT.setText(
						((GroupConversation) conversation).getGroupParticipant(convMessage.getGroupParticipantMsisdn()).getContactInfo().getFirstName() + HikeConstants.SEPARATOR);
				holder.participantNameFT.setVisibility(View.VISIBLE);
			}
		}
		else if (metadata != null && metadata.isPokeMessage())
		{
			holder.messageContainer.setVisibility(View.GONE);
			holder.poke.setVisibility(View.VISIBLE);
		}
		else
		{
			holder.messageContainer.setVisibility(View.VISIBLE);
			holder.poke.setVisibility(View.GONE);

			CharSequence markedUp = convMessage.getMessage();
			// Fix for bug where if a participant leaves the group chat, the participant's name is never shown 
			if(convMessage.isGroupChat() && !convMessage.isSent() && convMessage.getGroupParticipantMsisdn() != null)
			{
				markedUp = Utils.addContactName(((GroupConversation) conversation).getGroupParticipant(convMessage.getGroupParticipantMsisdn()).getContactInfo().getFirstName(), markedUp);
			}
			SmileyParser smileyParser = SmileyParser.getInstance();
			markedUp = smileyParser.addSmileySpans(markedUp, false);
			holder.messageTextView.setText(markedUp);
			Linkify.addLinks(holder.messageTextView, Linkify.ALL);
			Linkify.addLinks(holder.messageTextView, Utils.shortCodeRegex, "tel:");
		}

		if(convMessage.isFileTransferMessage() && ChatThread.fileTransferTaskMap.containsKey(convMessage.getMsgID()))
		{
			FileTransferTaskBase fileTransferTask = ChatThread.fileTransferTaskMap.get(convMessage.getMsgID());
			holder.circularProgress.setVisibility(View.VISIBLE);
			holder.circularProgress.setProgressAngle(fileTransferTask.getProgress());
			if(convMessage.isSent())
			{
				holder.image.setVisibility(View.INVISIBLE);
			}
		}
		else if(convMessage.isFileTransferMessage() && convMessage.isSent() && TextUtils.isEmpty(metadata.getHikeFiles().get(0).getFileKey()))
		{
			if(holder.circularProgress != null)
			{
				holder.circularProgress.setVisibility(View.INVISIBLE);
			}
			holder.image.setVisibility(View.VISIBLE);
			holder.image.setImageResource(R.drawable.ic_download_failed);
		}
		else
		{
			if(holder.circularProgress != null)
			{
				holder.circularProgress.setVisibility(View.INVISIBLE);
			}
			/* set the image resource, getImageState returns -1 if this is a received image */
			int resId = convMessage.getImageState();
			if (resId > 0)
			{
				if (convMessage.getState() == State.SENT_UNCONFIRMED) 
				{
					showTryingAgainIcon(holder.image, convMessage.getTimestamp());
				}
				else
				{
					holder.image.setImageResource(resId);
					holder.image.setAnimation(null);
					holder.image.setVisibility(View.VISIBLE);
				}
			}
			else if (convMessage.isSent())
			{
				holder.image.setImageResource(0);
			}
			else
			{
				holder.image.setImageDrawable(convMessage.isGroupChat() ? IconCacheManager.getInstance().getIconForMSISDN(convMessage.getGroupParticipantMsisdn()) : IconCacheManager.getInstance().getIconForMSISDN(convMessage.getMsisdn()));
			}
		}

		return v;
	}

	private void setTextAndIconForSystemMessages(TextView textView, CharSequence text, int iconResId)
	{
		textView.setText(text);
		textView.setCompoundDrawablesWithIntrinsicBounds(iconResId, 0, 0, 0);
	}

	private void showTryingAgainIcon(ImageView iv, long ts)
	{
		/* 
		 * We are checking this so that we can delay the try again icon from being shown immediately if the user 
		 * just sent the msg. If it has been over 5 secs then the user will immediately see the icon though. 
		 */
		if ((((long)System.currentTimeMillis()/1000) - ts) < 3) 
		{
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

	private void showFileTransferElements(ViewHolder holder, View v, boolean isSentMessage)
	{
		holder.fileThumb.setVisibility(View.VISIBLE);
		if(holder.showFileBtn != null)
		{
			holder.showFileBtn.setVisibility(View.VISIBLE);
			holder.showFileBtn.setImageResource(isSentMessage ? R.drawable.ic_open_sent_file : R.drawable.ic_open_received_file);
		}
	}

	private boolean shouldDisplayTimestamp(int position)
	{
		/* 
		 * only show the timestamp if the delta between
		 * this message and the previous one is greater than 
		 * 10 minutes
		 */
		ConvMessage current = getItem(position);
		ConvMessage previous = position > 0 ? getItem(position - 1) : null;
		if (previous == null)
		{
			return true;
		}
		return (current.getTimestamp() - previous.getTimestamp() > 60*5);
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

	public boolean isEmpty()
	{
		return getCount() == 0;
	}
}