package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.List;
import com.bsb.hike.R;
import android.app.Activity;
import android.content.Context;
import android.text.util.Linkify;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class PinHistoryAdapter extends BaseAdapter
{
	private Activity context;
	
	private List<Pair<String, String>> pinMessages;
	
	private String userMSISDN;
	
	private LayoutInflater inflater;

	
	public PinHistoryAdapter(Activity context, List<Pair<String, String>> PinMessages, String userMsisdn)
	{
		this.context = context;
		this.pinMessages = getPins();
		this.userMSISDN = userMsisdn;
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	private enum ViewType
	{
		TEXT
	}
	
	private class ViewHolder
	{
		TextView sender;
		
		TextView detail;
		
		View parent;
	}
	
	@Override
	public int getCount() 
	{
		return pinMessages.size();
	}

	@Override
	public Pair<String, String> getItem(int position) 
	{
		return pinMessages.get(position);
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
		ViewType viewType = ViewType.values()[getItemViewType(position)];

		final Pair<String, String> pin = getItem(position);

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
	 			viewHolder.sender.setText(pin.first);
	 			viewHolder.detail.setText(pin.second);
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
	
	private List<Pair<String, String>> getPins()
	{
		Pair<String, String> pin = null;
		List<Pair<String, String>> pins = new ArrayList<Pair<String,String>>();
		
		for(int i=0; i<10; i++)
		{
			pin = Pair.create("Pathik " + i, "Let's go on a mass bunk tomorrow and watch movie! " + i);
			pins.add(pin);
		}
		return pins;
	}
}
