package com.bsb.hike.adapters;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;

public class ConversationsAdapter extends ArrayAdapter<Conversation> {

	private int mResourceId;

	public ConversationsAdapter(Context context, int textViewResourceId,
			List<Conversation> objects) {
		super(context, textViewResourceId, objects);
		this.mResourceId = textViewResourceId;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Context context = getContext();
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = convertView;
		if (v == null) {
			v = inflater.inflate(mResourceId, parent, false);
		}

		Conversation conversation = getItem(position);
		System.out.println("ConversationsAdapter.getView() -- " + conversation);
		TextView contactView = (TextView) v.findViewById(R.id.contact);
		String name = conversation.getContactName();
		if (name == null) {
			name = conversation.getMsisdn();
		}

		contactView.setText(name);
		List<ConvMessage> messages = conversation.getMessages();
		if (!messages.isEmpty()) {
			ConvMessage message = messages.get(messages.size() - 1);
			TextView messageView = (TextView) v.findViewById(R.id.last_message);
			messageView.setText(message.getMessage());
			TextView tsView = (TextView) v.findViewById(R.id.last_message_timestamp);
			tsView.setText(message.getTimestampFormatted());
		}
		return v;
	}
}
