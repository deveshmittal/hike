package com.bsb.hike.adapters;

import java.util.List;

import android.content.Context;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

public class MessagesAdapter extends ArrayAdapter<ConvMessage>
{

	private Conversation conversation;

	public MessagesAdapter(Context context, List<ConvMessage> objects, Conversation conversation)
	{
		super(context, -1, objects);
		this.conversation = conversation;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		Context context = getContext();
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// TODO rather than re-use the cache (which could be for a slightly different type of view, let's just ignore it for now
		ConvMessage convMessage = getItem(position);
		View v = null;
		if (convMessage.isSent())
		{
			v = inflater.inflate(R.layout.message_item_send, parent, false);
			/* label outgoing hike conversations in green */
			if (conversation.isOnhike())
			{
				v.setBackgroundResource(R.color.blue);
			}

			ImageView imgStatus = (ImageView) v.findViewById(R.id.msg_status_indicator);
			int resId = conversation.isOnhike() ? convMessage.getImageState() : -1;
			if (resId > 0)
			{
				imgStatus.setImageResource(resId);
			}
		}
		else
		{
			v = inflater.inflate(R.layout.message_item_receive, parent, false);
		}

		TextView messageView = (TextView) v.findViewById(R.id.conversation_id);
		SmileyParser smileyParser = SmileyParser.getInstance();
		CharSequence markedUp = smileyParser.addSmileySpans(convMessage.getMessage());
		messageView.setText(markedUp);

		Linkify.addLinks(messageView, Linkify.ALL);
		Linkify.addLinks(messageView, Utils.shortCodeRegex, "tel:");
		TextView timestampView = (TextView) v.findViewById(R.id.timestamp);
		String dateFormatted = convMessage.getTimestampFormatted();
		timestampView.setText(dateFormatted);

		return v;
	}
}
