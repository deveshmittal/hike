package com.bsb.hike.adapters;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.util.Log;
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
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.tasks.DownloadFileTask;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.ui.ChatThread;
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
			try 
			{
				int positiveMargin = (int) (8 * Utils.densityMultiplier);
				int left = 0;
				int top = 0;
				int right = 0;
				int bottom = positiveMargin;

				if (convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_JOINED) 
				{
					JSONObject gcjPacket = new JSONObject(convMessage.getMetadata().serialize());
					JSONArray participantInfoArray = gcjPacket.getJSONArray(HikeConstants.DATA);

					if(!gcjPacket.optBoolean(HikeConstants.NEW_GROUP))
					{
						for (int i = 0; i < participantInfoArray.length(); i++) 
						{
							JSONObject nameMsisdn = participantInfoArray.getJSONObject(i);
							Log.d(getClass().getSimpleName(), "Joined: " + participantInfoArray.getString(i));

							TextView participantInfo = (TextView) inflater.inflate(
									R.layout.participant_info, null);

							LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

							GroupParticipant participant = ((GroupConversation)conversation).getGroupParticipant(nameMsisdn.getString(HikeConstants.MSISDN));

							String participantName = participant.getContactInfo().getFirstName();
							String baseMessage = context.getString(participant.getContactInfo().isOnhike() ? R.string.joined_conversation : R.string.invited_to_gc);

							setTextAndIconForSystemMessages(
									participantInfo, 
									Utils.getFormattedParticipantInfo(String.format(baseMessage, participantName)), 
									participant.getContactInfo().isOnhike() ? 
											R.drawable.ic_hike_user : R.drawable.ic_sms_user
									);
							if (i != participantInfoArray.length() - 1) 
							{
								lp.setMargins(left, top, right, bottom);
							}
							else
							{
								lp.setMargins(left, top, right, 0);
							}
							participantInfo.setLayoutParams(lp);

							((ViewGroup) holder.participantInfoContainer).addView(participantInfo);
						}
					}
					else
					{
						TextView participantInfo = (TextView) inflater.inflate(
								R.layout.participant_info, null);

						int count = participantInfoArray.length();
						String message = String.format(context.getString(R.string.new_group_message), count);

						SpannableStringBuilder ssb = new SpannableStringBuilder(message);
						ssb.setSpan(new ForegroundColorSpan(0xff666666), message.indexOf(""+count), message.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

						setTextAndIconForSystemMessages(participantInfo, ssb, R.drawable.ic_hike_user);

						((ViewGroup) holder.participantInfoContainer).addView(participantInfo);
					}
				} 
				else if(convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_LEFT || convMessage.getParticipantInfoState() == ParticipantInfoState.GROUP_END)
				{
					TextView participantInfo = (TextView) inflater.inflate(R.layout.participant_info, null);

					CharSequence message;
					if (convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_LEFT) 
					{
						String participantMsisdn = new JSONObject(convMessage.getMetadata().serialize()).optString(HikeConstants.DATA);
						message = Utils.getFormattedParticipantInfo(
										String.format(
												context.getString(R.string.left_conversation), 
												((GroupConversation) conversation).getGroupParticipant(participantMsisdn).getContactInfo().getFirstName()));
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
					if(TextUtils.isEmpty(convMessage.getMessage()))
					{
						convMessage.setMessage(String.format(context.getString(R.string.joined_hike), Utils.getFirstName(conversation.getLabel())));
					}
					TextView mainMessage = (TextView) inflater.inflate(R.layout.participant_info, null);
					setTextAndIconForSystemMessages(
														mainMessage, 
														convMessage.getMessage(), 
														convMessage.getParticipantInfoState() == ParticipantInfoState.USER_JOIN ? 
																R.drawable.ic_hike_user : R.drawable.ic_opt_in
													);

					TextView creditsMessage = null;
					if(convMessage.getMetadata().getJSON().getJSONObject(HikeConstants.DATA).has(HikeConstants.CREDITS))
					{
						creditsMessage = (TextView) inflater.inflate(R.layout.participant_info, null);
						int credits = convMessage.getMetadata().getJSON().optJSONObject(HikeConstants.DATA).optInt(HikeConstants.CREDITS);
						setTextAndIconForSystemMessages(creditsMessage, String.format(context.getString(R.string.earned_credits), credits), R.drawable.ic_got_credits);

						LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
						lp.setMargins(left, top, right, 0);
						creditsMessage.setLayoutParams(lp);
					}
					LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
					lp.setMargins(left, top, right, creditsMessage != null ? bottom : 0);
					mainMessage.setLayoutParams(lp);
					
					((ViewGroup) holder.participantInfoContainer).addView(mainMessage);
					if(creditsMessage != null)
					{
						((ViewGroup) holder.participantInfoContainer).addView(creditsMessage);
					}
				}
				else if(convMessage.getParticipantInfoState() == ParticipantInfoState.CHANGED_GROUP_NAME)
				{
					TextView mainMessage = (TextView) inflater.inflate(R.layout.participant_info, null);
					setTextAndIconForSystemMessages(mainMessage, Utils.getFormattedParticipantInfo(convMessage.getMessage()), R.drawable.ic_group_info);

					((ViewGroup) holder.participantInfoContainer).addView(mainMessage);
				}
				else if(convMessage.getParticipantInfoState() == ParticipantInfoState.BLOCK_INTERNATIONAL_SMS)
				{
					String info = convMessage.getMessage();
					String textToHighlight = context.getString(R.string.block_internation_sms_bold_text);

					SpannableStringBuilder ssb = new SpannableStringBuilder(info);
					ssb.setSpan(new ForegroundColorSpan(0xff666666), info.indexOf(textToHighlight), info.indexOf(textToHighlight) + textToHighlight.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

					TextView mainMessage = (TextView) inflater.inflate(R.layout.participant_info, null);
					setTextAndIconForSystemMessages(mainMessage, ssb, R.drawable.ic_no_int_sms);

					((ViewGroup) holder.participantInfoContainer).addView(mainMessage);
				}
				else
				{
					MessageMetadata metadata = convMessage.getMetadata();
					JSONArray dndNumbers = conversation instanceof GroupConversation ? metadata.getJSON().optJSONArray(HikeConstants.DND_USERS) : convMessage.getMetadata().getDndNumbers();
					if(conversation instanceof GroupConversation)
					{
						JSONArray nonDndNumbers = metadata.getJSON().optJSONArray(HikeConstants.NON_DND_USERS);
						if(nonDndNumbers != null && nonDndNumbers.length()>0)
						{
							for(int i = 0; i<nonDndNumbers.length(); i++)
							{
								TextView participantInfo = (TextView) inflater.inflate(
										R.layout.participant_info, null);

								GroupParticipant participant = ((GroupConversation)conversation).getGroupParticipant(nonDndNumbers.getString(i));
								String participantName = participant.getContactInfo().getFirstName();

								setTextAndIconForSystemMessages(
																participantInfo, 
																Utils.getFormattedParticipantInfo(
																		String.format(context.getString(R.string.joined_conversation), participantName)), 
																R.drawable.ic_opt_in);

								LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
								if(i != nonDndNumbers.length() - 1)
								{
									lp.setMargins(left, top, right, bottom);
								}
								else if(dndNumbers == null || dndNumbers.length()==0)
								{
									lp.setMargins(left, top, right, 0);
								}
								participantInfo.setLayoutParams(lp);

								((ViewGroup) holder.participantInfoContainer).addView(participantInfo);
							}
						}
						
					}
					TextView dndMessage = (TextView) inflater.inflate(R.layout.participant_info, null);

					if(dndNumbers != null && dndNumbers.length()>0)
					{
						StringBuilder dndNamesSB = new StringBuilder(); 
						for(int i=0; i<dndNumbers.length(); i++)
						{
							String name = conversation instanceof GroupConversation ? 
									((GroupConversation)conversation).getGroupParticipant(dndNumbers.getString(i)).getContactInfo().getFirstName() : Utils.getFirstName(conversation.getLabel());
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
			} 
			catch (JSONException e) 
			{
				Log.e(getClass().getSimpleName(), "Invalid JSON", e);
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

			holder.fileThumb.setBackgroundDrawable(
					showThumbnail ? 
							hikeFile.getThumbnail() : 
								context.getResources().getDrawable(
										hikeFile.getHikeFileType() == HikeFileType.IMAGE ? 
												R.drawable.ic_default_img : hikeFile.getHikeFileType() == HikeFileType.VIDEO ? 
														R.drawable.ic_default_mov : R.drawable.ic_default_audio));

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
		else if (metadata != null)
		{
			Spannable spannable = metadata.getMessage(context, convMessage, true);
			convMessage.setMessage(spannable.toString());
			/*
			 *  This is being done so that if the user chooses to forward or copy this message, 
			 *  he see's the metadata message and not the original one sent from the server.
			 */
			holder.messageTextView.setText(spannable);
			holder.messageTextView.setMovementMethod(LinkMovementMethod.getInstance());
		}
		else
		{
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
			AsyncTask<?, ?, ?> fileTransferTask = ChatThread.fileTransferTaskMap.get(convMessage.getMsgID());
			holder.circularProgress.setVisibility(View.VISIBLE);
			holder.circularProgress.setProgressAngle(fileTransferTask instanceof HikeHTTPTask ? ((HikeHTTPTask)fileTransferTask).getProgressFileTransfer() : ((DownloadFileTask)fileTransferTask).getProgressFileTransfer());
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