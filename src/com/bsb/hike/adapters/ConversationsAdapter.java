package com.bsb.hike.adapters;

import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.ui.MessagesList;

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
		String name = conversation.getContactName();
		if (name == null)
		{
			name = conversation.getMsisdn();
		}

		contactView.setText(name);
		List<ConvMessage> messages = conversation.getMessages();
		if (!messages.isEmpty())
		{
			ConvMessage message = messages.get(messages.size() - 1);
			TextView messageView = (TextView) v.findViewById(R.id.last_message);
			messageView.setText(message.getMessage());
			TextView tsView = (TextView) v.findViewById(R.id.last_message_timestamp);
			tsView.setText(message.getTimestampFormatted());
			Typeface tf = messageView.getTypeface();
			if (message.getState() == ConvMessage.State.RECEIVED_UNREAD)
			{
				/* set unread messages to BOLD */
				messageView.setTypeface(tf, Typeface.BOLD);
				tf = tsView.getTypeface();
				tsView.setTypeface(tf, Typeface.BOLD);

				tf = contactView.getTypeface();
				contactView.setTypeface(tf, Typeface.BOLD);
			}
			else
			{
				messageView.setTypeface(Typeface.DEFAULT);
				tf = tsView.getTypeface();
				tsView.setTypeface(Typeface.DEFAULT);

				tf = contactView.getTypeface();
				contactView.setTypeface(Typeface.DEFAULT);
			}

			ImageView imgStatus = (ImageView) v.findViewById(R.id.msg_status_indicator);
			int resId = message.getImageState();
			if (resId > 0)
			{
				imgStatus.setImageResource(resId);
				imgStatus.setVisibility(View.VISIBLE);
			}
			else
			{
				imgStatus.setImageResource(0);
			}
		}

		ImageView avatarView = (ImageView) v.findViewById(R.id.avatar);
		avatarView.setImageDrawable(IconCacheManager.getInstance().getIconForMSISDN(conversation.getMsisdn()));

		if (mMessagesList.getSelectedConversation() == conversation)
		{
			ViewAnimator animator = (ViewAnimator) v.findViewById(R.id.conversation_flip);
			animator.setInAnimation(null);
			animator.setOutAnimation(null);
			animator.setDisplayedChild(1);
			mMessagesList.setComposeView(animator);
		}

		return v;
	}
}
