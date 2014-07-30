package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;
import com.google.android.gms.internal.co;

public class PinHistoryAdapter extends BaseAdapter
{
	private Activity context;

	private List<ConvMessage> textPins;

	private List<Object> listData;

	private LayoutInflater inflater;

	private Conversation mConversation;

	private PinHistoryItemsListener mListener;

	private boolean addDateInBetween;

	public static interface PinHistoryItemsListener
	{
		public void onLastItemRequested();
	}

	public PinHistoryAdapter(Activity context, List<ConvMessage> textPins, String userMsisdn, long convId, Conversation conversation, PinHistoryItemsListener listener,
			boolean addDateInbetween)
	{
		this.context = context;
		this.addDateInBetween = addDateInbetween;
		this.mConversation = conversation;

		this.textPins = textPins;
		addDateInBetween();
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		setItemViewListener(listener);
	}

	private enum ViewType
	{
		TEXT, DATE_SEP
	}

	public void appendPinstoView(List<ConvMessage> list)
	{
		textPins.addAll(list);
		addDateInBetween();
	}

	private void setItemViewListener(PinHistoryItemsListener listener)
	{
		this.mListener = listener;
	}

	public void removeItemViewListener()
	{
		this.mListener = null;
	}

	private class ViewHolder
	{
		TextView sender;

		TextView detail;

		TextView timestamp;

		View parent;
	}

	@Override
	public int getCount()
	{
		return listData.size();
	}

	@Override
	public Object getItem(int position)
	{
		return listData.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return 0;
	}

	@Override
	public int getViewTypeCount()
	{
		return ViewType.values().length;
	}

	@Override
	public boolean areAllItemsEnabled()
	{
		return false;
	}

	@Override
	public boolean isEnabled(int position)
	{
		return true;
	}

	@Override
	public int getItemViewType(int position)
	{
		Object obj = getItem(position);
		if (obj instanceof ConvMessage)
		{
			return ViewType.TEXT.ordinal();
		}
		return ViewType.DATE_SEP.ordinal();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		if (position == getCount() - 1 && mListener != null)
		{
			mListener.onLastItemRequested();
		}
		ViewType viewType = ViewType.values()[getItemViewType(position)];

		final Object data = getItem(position);

		final ViewHolder viewHolder;

		if (convertView == null)
		{
			viewHolder = new ViewHolder();

			switch (viewType)
			{
			case TEXT:
			{
				convertView = inflater.inflate(R.layout.pin_history, null);
				viewHolder.sender = (TextView) convertView.findViewById(R.id.sender);
				viewHolder.detail = (TextView) convertView.findViewById(R.id.text);
				viewHolder.timestamp = (TextView) convertView.findViewById(R.id.timestamp);
			}
				break;
			case DATE_SEP:
				convertView = inflater.inflate(R.layout.message_day_container, null);
				break;
			}
			convertView.setTag(viewHolder);
		}
		else
		{
			viewHolder = (ViewHolder) convertView.getTag();
		}

		switch (viewType)
		{
		case TEXT:
		{
			ConvMessage textPin = (ConvMessage) data;
			if (textPin.isSent())
			{
				viewHolder.sender.setText("You, ");
			}
			else
			{
				if (Utils.isGroupConversation(textPin.getMsisdn()))
				{
					GroupConversation gConv = (GroupConversation) mConversation;
					String number = null;					
					String name = gConv.getGroupParticipantFirstName(textPin.getGroupParticipantMsisdn());

					if (((GroupConversation) mConversation).getGroupParticipant(textPin.getGroupParticipantMsisdn()).getContactInfo().isUnknownContact())
					{
						number = textPin.getGroupParticipantMsisdn();
					}

					if (number != null)
					{
						viewHolder.sender.setText(number + " ~ " + name + ", ");
					}
					else
					{
						viewHolder.sender.setText(name + ", ");
					}
				}
			}
			CharSequence markedUp = textPin.getMessage();
			SmileyParser smileyParser = SmileyParser.getInstance();
			markedUp = smileyParser.addSmileySpans(markedUp, false);

			viewHolder.detail.setText(markedUp);
			viewHolder.timestamp.setText(textPin.getTimestampFormatted(false, context));
			Linkify.addLinks(viewHolder.detail, Linkify.ALL);
			Linkify.addLinks(viewHolder.detail, Utils.shortCodeRegex, "tel:");
		}
			break;
		case DATE_SEP:
			TextView tv = (TextView) convertView.findViewById(R.id.day);
			tv.setText((String) data);
			break;
		}

		return convertView;
	}

	private void addDateInBetween()
	{
		List<Object> newData = new ArrayList<Object>();
		if (textPins.size() > 0)
		{
			if (addDateInBetween)
			{
				long timeStamp = textPins.get(0).getTimestamp();
				Calendar c = Calendar.getInstance();
				c.setTimeInMillis(timeStamp);
				String timeToShow = textPins.get(0).getMessageDate(context);
				newData.add(timeToShow);
				newData.add(textPins.get(0));
				for (int i = 1; i < textPins.size(); i++)
				{
					Calendar newC = Calendar.getInstance();
					newC.setTimeInMillis(textPins.get(i).getTimestamp());
					if (c.get(Calendar.DAY_OF_MONTH) != newC.get(Calendar.DAY_OF_MONTH))
					{
						newData.add(textPins.get(i).getMessageDate(context));
						c = newC;
					}
					newData.add(textPins.get(i));
				}
			}
			else
			{
				for (ConvMessage con : textPins)
				{
					newData.add(con);
				}
			}
		}
		listData = newData;
		notifyDataSetChanged();
	}
}
