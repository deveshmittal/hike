package com.bsb.hike.adapters;

import java.util.List;

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
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.utils.IconCacheManager;
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
			setImgStatus(message, imgStatus);

			TextView messageView = (TextView) v.findViewById(R.id.last_message);

			MessageMetadata metadata = message.getMetadata();
			final String dndMissedCalledNumber = metadata != null ? metadata.getDNDMissedCallNumber() : null;
			final boolean newUser = metadata != null ? metadata.getNewUser() : false;

			CharSequence markedUp;
			if (!TextUtils.isEmpty(dndMissedCalledNumber) || metadata != null)
			{
				markedUp = context.getString(
						!TextUtils.isEmpty(dndMissedCalledNumber) ? 
								R.string.dnd_message : !newUser ? 
										R.string.friend_joined_hike_no_creds : R.string.friend_joined_hike_with_creds, 
										message.getConversation().getLabel(), 
										dndMissedCalledNumber);
			}
			else
			{
				SmileyParser smileyParser = SmileyParser.getInstance();
				markedUp = smileyParser.addSmileySpans(message.getMessage());
				// For showing the name of the contact that sent the message in a group chat
				if(conversation instanceof GroupConversation && !TextUtils.isEmpty(message.getGroupParticipantMsisdn()) && message.getParticipantInfoState() == ParticipantInfoState.NO_INFO)
				{
					markedUp = Utils.addContactName(((GroupConversation)conversation).getGroupParticipant(message.getGroupParticipantMsisdn()).getContactInfo().getFirstName(), markedUp);
				}
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

	private void setImgStatus(ConvMessage message, ImageView imgStatus)
	{
		imgStatus.setVisibility(View.VISIBLE);
		switch (message.getState()) {
		case SENT_CONFIRMED:
			imgStatus.setImageResource(R.drawable.ic_sent_small);
			break;
		case SENT_DELIVERED:
			imgStatus.setImageResource(R.drawable.ic_delivered_small);
			break;
		case SENT_DELIVERED_READ:
			imgStatus.setImageResource(R.drawable.ic_read_small);
			break;
		case RECEIVED_UNREAD:
			imgStatus.setImageResource(R.drawable.ic_unread);
			break;
		case SENT_FAILED:
			imgStatus.setImageResource(R.drawable.ic_failed);
			break;
		case SENT_UNCONFIRMED:
			imgStatus.setImageResource(R.drawable.ic_tower_small);
			break;
		default:
			imgStatus.setImageResource(0);
			imgStatus.setVisibility(View.GONE);
		}
	}
}
