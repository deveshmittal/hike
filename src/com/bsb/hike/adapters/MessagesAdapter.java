package com.bsb.hike.adapters;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;

public class MessagesAdapter extends ArrayAdapter<ConvMessage> {

	public MessagesAdapter(Context context, List<ConvMessage> objects) {
		super(context, -1, objects);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Context context = getContext();
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		//TODO rather than re-use the cache (which could be for a slightly different type of view, let's just ignore it for now
		ConvMessage convMessage = getItem(position);
		View v = null;
		if (v == null) {
			if (convMessage.isSent()) {
				v = inflater.inflate(R.layout.message_item_send, parent, false);
			} else {
				v = inflater.inflate(R.layout.message_item_receive, parent, false);
			}
		}

		TextView messageView = (TextView) v.findViewById(R.id.conversation_id);
		messageView.setText(convMessage.getMessage());

		TextView timestampView = (TextView) v.findViewById(R.id.timestamp);
		int timestamp = convMessage.getTimestamp();
		SimpleDateFormat dfm = new SimpleDateFormat("HH:mm aa");
		Date date = new Date(timestamp * 1000);
		String dateFormatted = dfm.format(date);
		timestampView.setText(dateFormatted);

		return v;
	}
}
