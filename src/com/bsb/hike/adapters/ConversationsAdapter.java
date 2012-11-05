package com.bsb.hike.adapters;

import java.util.List;

import org.json.JSONArray;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.MessagesList;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

public class ConversationsAdapter extends ArrayAdapter<Conversation>
{

	private int mResourceId;
	private MessagesList mMessagesList;

	public ConversationsAdapter(MessagesList messagesList, int textViewResourceId, List<Conversation> objects)
	{
		super(messagesList, textViewResourceId, objects);
		this.mResourceId = textViewResourceId;
		mMessagesList = messagesList;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		Context context = getContext();
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		Conversation conversation = getItem(position);

		View v = convertView;
		if (v == null)
		{
			v = inflater.inflate(mResourceId, parent, false);
		}

		TextView contactView = (TextView) v.findViewById(R.id.contact);
		String name = conversation.getLabel();

		contactView.setText(name);
		Log.d(getClass().getSimpleName(), "Contact Name = " + name);
		List<ConvMessage> messages = conversation.getMessages();
		if (!messages.isEmpty())
		{
			ConvMessage message = messages.get(messages.size() - 1);

			ImageView imgStatus = (ImageView) v.findViewById(R.id.msg_status_indicator);
			int resId = message.getImageState();
			if (resId > 0)
			{
				imgStatus.setImageResource(resId);
				imgStatus.setVisibility(View.VISIBLE);
			}
			else if (message.getState() == ConvMessage.State.RECEIVED_UNREAD && (message.getMsgID() > -1 || message.getMappedMsgID() > -1))
			{
				imgStatus.setImageResource(R.drawable.ic_unread);
				imgStatus.setVisibility(View.VISIBLE);
			}
			else
			{
				imgStatus.setImageResource(0);
				imgStatus.setVisibility(View.GONE);
			}

			TextView messageView = (TextView) v.findViewById(R.id.last_message);

			MessageMetadata metadata = message.getMetadata();

			CharSequence markedUp = null;
			if (message.isFileTransferMessage())
			{
				markedUp = HikeFileType.toProperString(metadata.getHikeFiles().get(0).getHikeFileType());
				if((conversation instanceof GroupConversation) && !message.isSent())
				{
					markedUp = Utils.addContactName(((GroupConversation)conversation).getGroupParticipant(message.getGroupParticipantMsisdn()).getContactInfo().getFirstName(), markedUp);
				}
				imgStatus.setVisibility(ChatThread.fileTransferTaskMap != null && ChatThread.fileTransferTaskMap.containsKey(message.getMsgID()) ? View.GONE : View.VISIBLE);
			}
			else if (message.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_JOINED)
			{
				JSONArray participantInfoArray = metadata.getGcjParticipantInfo();

				String highlight = Utils.getGroupJoinHighlightText(participantInfoArray, (GroupConversation) conversation);

				if(metadata.isNewGroup())
				{
					markedUp = String.format(context.getString(R.string.new_group_message), highlight);
				}
				else
				{
					markedUp = String.format(context.getString(R.string.add_to_group_message), highlight);
				}
			}
			else if (message.getParticipantInfoState() == ParticipantInfoState.DND_USER)
			{
				JSONArray dndNumbers = metadata.getDndNumbers();
				if(dndNumbers != null && dndNumbers.length() > 0)
				{
					StringBuilder dndNames = new StringBuilder(); 
					for(int i=0; i<dndNumbers.length(); i++)
					{
						String dndName;
						dndName = conversation instanceof GroupConversation ? 
								((GroupConversation)conversation).getGroupParticipant(dndNumbers.optString(i)).getContactInfo().getFirstName() : Utils.getFirstName(conversation.getLabel());
								if(i < dndNumbers.length() - 2)
								{
									dndNames.append(dndName + ", ");
								}
								else if(i < dndNumbers.length() - 1)
								{
									dndNames.append(dndName + " and ");
								}
								else
								{
									dndNames.append(dndName);
								}
					}
					markedUp = String.format(context.getString(conversation instanceof GroupConversation ? R.string.dnd_msg_gc : R.string.dnd_one_to_one), dndNames.toString());
				}
			}
			else if (message.getParticipantInfoState() == ParticipantInfoState.INTRO_MESSAGE)
			{
				markedUp = String.format(context.getString(conversation.isOnhike() ? R.string.intro_hike_thread : R.string.intro_sms_thread), Utils.getFirstName(conversation.getLabel()));
			}
			else if (message.getParticipantInfoState() == ParticipantInfoState.USER_JOIN)
			{
				markedUp = TextUtils.isEmpty(message.getMessage()) ? 
						String.format(context.getString(R.string.joined_hike), Utils.getFirstName(conversation.getLabel())) : message.getMessage();
			}
			else
			{
				markedUp = message.getMessage();
				// For showing the name of the contact that sent the message in a group chat
				if(conversation instanceof GroupConversation && !TextUtils.isEmpty(message.getGroupParticipantMsisdn()) && message.getParticipantInfoState() == ParticipantInfoState.NO_INFO)
				{
					markedUp = Utils.addContactName(((GroupConversation)conversation).getGroupParticipant(message.getGroupParticipantMsisdn()).getContactInfo().getFirstName(), markedUp);
				}
				SmileyParser smileyParser = SmileyParser.getInstance();
				markedUp = smileyParser.addSmileySpans(markedUp, true);
			}
			messageView.setVisibility(View.VISIBLE);
			messageView.setText(markedUp);
			TextView tsView = (TextView) v.findViewById(R.id.last_message_timestamp);
			tsView.setText(message.getTimestampFormatted(true));
			if (message.getState() == ConvMessage.State.RECEIVED_UNREAD)
			{
				/* set unread messages to BLUE */
				messageView.setTextColor(mMessagesList.getResources().getColor(R.color.unread_message_blue));
			}
			else
			{
				messageView.setTextColor(mMessagesList.getResources().getColor(R.color.grey));
			}
		}

		ImageView avatarView = (ImageView) v.findViewById(R.id.avatar);
		avatarView.setImageDrawable(IconCacheManager.getInstance().getIconForMSISDN(conversation.getMsisdn()));

		return v;
	}
}
