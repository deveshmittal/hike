package com.bsb.hike.adapters;

import java.util.List;

import android.content.Context;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

public class MessagesAdapter extends ArrayAdapter<ConvMessage>
{

	private enum ViewType
	{
		RECEIVE,
		SEND_SMS,
		SEND_HIKE
	};

	private class ViewHolder
	{
		LinearLayout timestampContainer;
		TextView messageTextView;
		TextView timestampTextView;
		ImageView image;
	}

	private Conversation conversation;

	public MessagesAdapter(Context context, List<ConvMessage> objects, Conversation conversation)
	{
		super(context, -1, objects);
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
		if (convMessage.isSent())
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
		Context context = getContext();
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		ConvMessage convMessage = getItem(position);
		ViewHolder holder = null;
		View v = convertView;
		if (v == null)
		{
			holder = new ViewHolder();
			if (convMessage.isSent())
			{
				v = inflater.inflate(R.layout.message_item_send, parent, false);

				holder.image = (ImageView) v.findViewById(R.id.msg_status_indicator);
				holder.timestampContainer = (LinearLayout) v.findViewById(R.id.timestamp_container);
				holder.timestampTextView = (TextView) v.findViewById(R.id.timestamp);
				v.setTag(holder);

				/* label outgoing hike conversations in green */
				if (conversation.isOnhike())
				{
					holder.messageTextView = (TextView) v.findViewById(R.id.message_hike);
					holder.messageTextView.setVisibility(View.VISIBLE);
					v.findViewById(R.id.message_sms).setVisibility(View.GONE);
				}
				else
				{
					holder.messageTextView = (TextView) v.findViewById(R.id.message_sms);
					holder.messageTextView.setVisibility(View.VISIBLE);
					v.findViewById(R.id.message_hike).setVisibility(View.GONE);
				}
			}
			else
			{
				v = inflater.inflate(R.layout.message_item_receive, parent, false);

				holder.image = (ImageView) v.findViewById(R.id.avatar);
				holder.messageTextView = (TextView) v.findViewById(R.id.message_receive);
				holder.timestampContainer = (LinearLayout) v.findViewById(R.id.timestamp_container);
				holder.timestampTextView = (TextView) v.findViewById(R.id.timestamp);
				v.setTag(holder);
			}
		}
		else
		{
			holder = (ViewHolder) v.getTag();
		}

		SmileyParser smileyParser = SmileyParser.getInstance();
		CharSequence markedUp = smileyParser.addSmileySpans(convMessage.getMessage());
		holder.messageTextView.setText(markedUp);

		Linkify.addLinks(holder.messageTextView, Linkify.ALL);
		Linkify.addLinks(holder.messageTextView, Utils.shortCodeRegex, "tel:");

		if (shouldDisplayTimestamp(position))
		{
			String dateFormatted = convMessage.getTimestampFormatted(false);
			holder.timestampTextView.setText(dateFormatted);
			holder.timestampContainer.setVisibility(View.VISIBLE);
		}
		else
		{
			holder.timestampContainer.setVisibility(View.GONE);
		}

		/* set the image resource, getImageState returns -1 if this is a received image */
		int resId = convMessage.getImageState();
		if (resId > 0)
		{
			holder.image.setImageResource(resId);
		}
		else if (convMessage.isSent())
		{
			holder.image.setImageResource(0);
		}
		else
		{
			holder.image.setImageDrawable(IconCacheManager.getInstance().getIconForMSISDN(convMessage.getMsisdn()));
		}

		return v;
	}

	private boolean shouldDisplayTimestamp(int position)
	{
		/* always show the timestamp for the first element */
		if (position == 0)
		{
			return true;
		}

		/* 
		 * otherwise, only show the timestamp if the delta between
		 * this message and the previous one is greater than 
		 * 10 minutes
		 */
		ConvMessage current = getItem(position);
		ConvMessage previous = getItem(position - 1);
		return (current.getTimestamp() - previous.getTimestamp() > 60*10);
	}
}
