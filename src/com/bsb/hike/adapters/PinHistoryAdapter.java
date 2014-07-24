package com.bsb.hike.adapters;

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

import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

public class PinHistoryAdapter extends BaseAdapter
{
	private Activity context;
	
	private List<ConvMessage> textPins;
		
	private LayoutInflater inflater;
	
	private Conversation mConversation;
	
	private PinHistoryItemsListener mListener;
	
	public static interface PinHistoryItemsListener
	{
		public void onLastItemRequested();		
	}
	
	public PinHistoryAdapter(Activity context, List<ConvMessage> textPins, String userMsisdn, 
			long convId, Conversation conversation, PinHistoryItemsListener listener)
	{
		this.context = context;		
						
		this.mConversation = conversation;
		
		this.textPins = textPins;
		
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		setItemViewListener(listener);
	}
	
	private enum ViewType
	{
		TEXT
	}
		
	public void appendPinstoView(List<ConvMessage> list)
	{
		textPins.addAll(list);
		
		notifyDataSetChanged();
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
		return textPins.size();
	}

	@Override
	public ConvMessage getItem(int position) 
	{
		return textPins.get(position);
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
		return ViewType.TEXT.ordinal();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
		if(position == getCount() - 1 && mListener != null)
		{			
			mListener.onLastItemRequested();
		}
		ViewType viewType = ViewType.values()[getItemViewType(position)];

		final ConvMessage textPin = getItem(position);

		final ViewHolder viewHolder;
		
		if (convertView == null)
		{
			viewHolder = new ViewHolder();

			switch (viewType)
			{
				case TEXT:
				{
					convertView = inflater.inflate(R.layout.pin_history, null);
					viewHolder.sender = (TextView)convertView.findViewById(R.id.pin_header);
					viewHolder.detail = (TextView)convertView.findViewById(R.id.pin_detail);
					viewHolder.timestamp = (TextView)convertView.findViewById(R.id.last_pin_timestamp);
					viewHolder.parent = convertView.findViewById(R.id.main_content);					
				}		
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
				if(textPin.isSent())
				{
					viewHolder.sender.setText("You");
				}
				else
				{
					if (Utils.isGroupConversation(textPin.getMsisdn()))
					{
					GroupConversation gConv = (GroupConversation) mConversation;
					viewHolder.sender.setText(gConv.getGroupParticipantFirstName(textPin.getGroupParticipantMsisdn()));
					}
				}
				CharSequence markedUp= textPin.getMessage();
				SmileyParser smileyParser = SmileyParser.getInstance();
				markedUp = smileyParser.addSmileySpans(markedUp, false);
				
	 			viewHolder.detail.setText(markedUp);
	 			viewHolder.timestamp.setText(textPin.getTimestampFormatted(false, context));
	 			viewHolder.parent.setOnClickListener(pinOnClickListener);
				Linkify.addLinks(viewHolder.detail, Linkify.ALL);
			}
			break;
		}
		if (viewHolder.parent != null)
		{
			int bottomPadding;

			if (position == getCount() - 1)
			{
				bottomPadding = context.getResources().getDimensionPixelSize(R.dimen.updates_margin);
			}
			else
			{
				bottomPadding = 0;
			}

			viewHolder.parent.setPadding(0, 0, 0, bottomPadding);
		}

		return convertView;
	}

	private OnClickListener pinOnClickListener = new OnClickListener() 
	{		
		@Override
		public void onClick(View v) 
		{
		}
	};
}
