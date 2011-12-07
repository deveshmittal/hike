package com.bsb.hike.ui;

import java.util.List;

import com.bsb.hike.R;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

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
		TextView messageView = (TextView) v.findViewById(R.id.conversation_id);
		messageView.setText(conversation.getMessage());
//		timestampView.setText(conversation.getTimestampFormatted());
		RelativeLayout.LayoutParams params = (LayoutParams) messageView.getLayoutParams();		
		if (conversation.isSent()) {
			params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
		} else {
			params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
		}
		messageView.setLayoutParams(params);

		return v;
	}
}
