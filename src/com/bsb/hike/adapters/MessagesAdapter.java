package com.bsb.hike.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

public class MessagesAdapter extends BaseAdapter
{

	private enum ViewType
	{
		RECEIVE,
		SEND_SMS,
		SEND_HIKE,
		PARTICIPANT_INFO
	};

	private class ViewHolder
	{
		LinearLayout timestampContainer;
		TextView messageTextView;
		TextView timestampTextView;
		ImageView image;
		ViewGroup participantInfoContainer;
	}

	private Conversation conversation;
	private ArrayList<ConvMessage> convMessages;
	private Context context;

	public MessagesAdapter(Context context, ArrayList<ConvMessage> objects, Conversation conversation)
	{
		this.context = context;
		this.convMessages = objects;
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
		if (convMessage.getParticipantInfoState() != ParticipantInfoState.NO_INFO)
		{
			type = ViewType.PARTICIPANT_INFO;
		}
		else if (convMessage.isSent())
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
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		final ConvMessage convMessage = getItem(position);
		ViewHolder holder = null;
		View v = convertView;
		if (v == null)
		{
			holder = new ViewHolder();
			if(convMessage.getParticipantInfoState() != ParticipantInfoState.NO_INFO)
			{
				v = inflater.inflate(R.layout.message_item_receive, null);
				
				holder.image = (ImageView) v.findViewById(R.id.avatar);
				holder.messageTextView = (TextView) v.findViewById(R.id.message_receive);
				holder.timestampContainer = (LinearLayout) v.findViewById(R.id.timestamp_container);
				holder.timestampTextView = (TextView) v.findViewById(R.id.timestamp);
				holder.participantInfoContainer = (ViewGroup) v.findViewById(R.id.participant_info_container);

				holder.image.setVisibility(View.GONE);
				holder.messageTextView.setVisibility(View.GONE);
				v.setTag(holder);
			}
			else if (convMessage.isSent())
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
				holder.participantInfoContainer = (ViewGroup) v.findViewById(R.id.participant_info_container);

				holder.participantInfoContainer.setVisibility(View.GONE);
				v.setTag(holder);
			}
		}
		else
		{
			holder = (ViewHolder) v.getTag();
		}

		if (shouldDisplayTimestamp(position))
		{
			String dateFormatted = convMessage.getTimestampFormatted(false);
			holder.timestampTextView.setText(dateFormatted.toUpperCase());
			holder.timestampContainer.setVisibility(View.VISIBLE);
		}
		else
		{
			holder.timestampContainer.setVisibility(View.GONE);
		}

		if (convMessage.getParticipantInfoState() != ParticipantInfoState.NO_INFO)
		{
			((ViewGroup)holder.participantInfoContainer).removeAllViews();

			if (convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_JOINED) 
			{
				int left = (int) (0 * Utils.densityMultiplier);
				int top = (int) (0 * Utils.densityMultiplier);
				int right = (int) (0 * Utils.densityMultiplier);
				int bottom = (int) (6 * Utils.densityMultiplier);

				String msg = convMessage.getMessage();
				String[] participantInfoArray = msg.contains(", ") ? msg.split(", ") : new String[] {msg};

				for (int i = 0; i < participantInfoArray.length; i++) 
				{
					Log.d(getClass().getSimpleName(), "Joined: " + participantInfoArray[i]);

					TextView participantInfo = (TextView) inflater.inflate(
							R.layout.participant_info, null);

					LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

					if (i != participantInfoArray.length - 1) 
					{
						participantInfo.setText(Utils.getFormattedParticipantInfo(participantInfoArray[i] + " " + context.getString(R.string.joined_conversation)));
						lp.setMargins(left, top, right, bottom);
					}
					else
					{
						participantInfo.setText(Utils.getFormattedParticipantInfo(participantInfoArray[i]));
						lp.setMargins(left, top, right, 0);
					}
					participantInfo.setLayoutParams(lp);

					((ViewGroup) holder.participantInfoContainer).addView(participantInfo);
				}
			} 
			else 
			{
				Log.d(getClass().getSimpleName(), "Left: " + convMessage.getMessage());
				TextView participantInfo = (TextView) inflater.inflate(R.layout.participant_info, null);
				if (convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_LEFT) 
				{
					participantInfo.setText(Utils.getFormattedParticipantInfo(convMessage.getMessage()));
				}
				else
				{
					participantInfo.setText(convMessage.getMessage());
				}
				((ViewGroup) holder.participantInfoContainer).addView(participantInfo);
			}
			return v;
		}
			

		MessageMetadata metadata = convMessage.getMetadata();
		final String dndMissedCalledNumber = metadata != null ? metadata.getDNDMissedCallNumber() : null;

		if (dndMissedCalledNumber != null)
		{
			String content = "tap here";
			String message = context.getString(R.string.dnd_message, convMessage.getConversation().getLabel(), dndMissedCalledNumber);
			Spannable spannable = Spannable.Factory.getInstance().newSpannable(message);
			int index = message.indexOf(content);
			spannable.setSpan(new ClickableSpan()
			{
				@Override
				public void onClick(View blah)
				{
					Intent smsIntent = new Intent(Intent.ACTION_VIEW);
					smsIntent.setData(Uri.parse("sms:" + convMessage.getMsisdn()));
					smsIntent.putExtra("sms_body", context.getString(R.string.dnd_invite_message, dndMissedCalledNumber));
					context.startActivity(smsIntent);
				}
			}, index, index + content.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			holder.messageTextView.setText(spannable);
			holder.messageTextView.setMovementMethod(LinkMovementMethod.getInstance());
		}
		else
		{
			SmileyParser smileyParser = SmileyParser.getInstance();
			CharSequence markedUp = smileyParser.addSmileySpans(convMessage.getMessage());
			if(convMessage.isGroupChat() && !convMessage.isSent() && convMessage.getGroupParticipantMsisdn() != null)
			{
				markedUp = Utils.addContactName(this.conversation.getGroupParticipants(), convMessage.getGroupParticipantMsisdn(), markedUp);
			}
			holder.messageTextView.setText(markedUp);
			Linkify.addLinks(holder.messageTextView, Linkify.ALL);
			Linkify.addLinks(holder.messageTextView, Utils.shortCodeRegex, "tel:");
		}

		/* set the image resource, getImageState returns -1 if this is a received image */
		int resId = convMessage.getImageState();
		if (resId > 0)
		{
			if (convMessage.getState() == State.SENT_UNCONFIRMED) 
			{
				showTryingAgainIcon(holder.image, convMessage.getTimestamp());
			}
			else
			{
				holder.image.setImageResource(resId);
				holder.image.setAnimation(null);
				holder.image.setVisibility(View.VISIBLE);
			}
		}
		else if (convMessage.isSent())
		{
			holder.image.setImageResource(0);
		}
		else
		{
			holder.image.setImageDrawable(convMessage.isGroupChat() ? IconCacheManager.getInstance().getIconForMSISDN(convMessage.getGroupParticipantMsisdn()) : IconCacheManager.getInstance().getIconForMSISDN(convMessage.getMsisdn()));
		}

		return v;
	}

	private void showTryingAgainIcon(ImageView iv, long ts)
	{
		/* 
		 * We are checking this so that we can delay the try again icon from being shown immediately if the user 
		 * just sent the msg. If it has been over 5 secs then the user will immediately see the icon though. 
		 */
		if ((((long)System.currentTimeMillis()/1000) - ts) < 5) 
		{
			iv.setVisibility(View.INVISIBLE);

			Animation anim = AnimationUtils.loadAnimation(context,
					android.R.anim.fade_in);
			anim.setStartOffset(4000);
			anim.setDuration(1);

			iv.setAnimation(anim);
		}
		iv.setVisibility(View.VISIBLE);
		
		if(!(iv.getDrawable() instanceof AnimationDrawable))
		{
			AnimationDrawable ad = new AnimationDrawable();
			ad.addFrame(context.getResources()
					.getDrawable(R.drawable.ic_tower0), 600);
			ad.addFrame(context.getResources()
					.getDrawable(R.drawable.ic_tower1), 600);
			ad.addFrame(context.getResources()
					.getDrawable(R.drawable.ic_tower2), 600);
			ad.setOneShot(false);
			ad.setVisible(true, true);
			iv.setImageDrawable(ad);
			ad.start();
		}
	}

	private boolean shouldDisplayTimestamp(int position)
	{
		/* 
		 * only show the timestamp if the delta between
		 * this message and the previous one is greater than 
		 * 10 minutes
		 */
		ConvMessage current = getItem(position);
		ConvMessage previous = position > 0 ? getItem(position - 1) : null;
		if (previous == null)
		{
			return true;
		}
		return (current.getTimestamp() - previous.getTimestamp() > 60*10);
	}

	@Override
	public int getCount() {
		return convMessages.size();
	}

	@Override
	public ConvMessage getItem(int position) {
		return convMessages.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public boolean isEmpty()
	{
		return getCount() == 0;
	}

}
