package com.bsb.hike.adapters;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
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

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
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
			try 
			{
				if (convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_JOINED) 
				{
					int left = (int) (0 * Utils.densityMultiplier);
					int top = (int) (0 * Utils.densityMultiplier);
					int right = (int) (0 * Utils.densityMultiplier);
					int bottom = (int) (6 * Utils.densityMultiplier);

					JSONArray participantInfoArray = new JSONObject(convMessage.getMetadata().serialize()).getJSONArray(HikeConstants.DATA);

					for (int i = 0; i < participantInfoArray.length(); i++) 
					{
						JSONObject nameMsisdn = participantInfoArray.getJSONObject(i);
						Log.d(getClass().getSimpleName(), "Joined: " + participantInfoArray.getString(i));

						TextView participantInfo = (TextView) inflater.inflate(
								R.layout.participant_info, null);

						LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

						participantInfo.setText(
								Utils.getFormattedParticipantInfo(
										((GroupConversation)conversation).getGroupParticipant(nameMsisdn.getString(HikeConstants.MSISDN)).getContactInfo().getFirstName() + " " 
												+ context.getString(R.string.joined_conversation)));
						if (i != participantInfoArray.length() - 1) 
						{
							lp.setMargins(left, top, right, bottom);
						}
						else
						{
							lp.setMargins(left, top, right, 0);
						}
						participantInfo.setLayoutParams(lp);

						((ViewGroup) holder.participantInfoContainer).addView(participantInfo);
					}
				} 
				else 
				{
					TextView participantInfo = (TextView) inflater.inflate(R.layout.participant_info, null);

					if (convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_LEFT) 
					{
						String participantMsisdn = new JSONObject(convMessage.getMetadata().serialize()).optString(HikeConstants.DATA);
						participantInfo.setText(
								Utils.getFormattedParticipantInfo(
										((GroupConversation) conversation).getGroupParticipant(participantMsisdn).getContactInfo().getFirstName() + " " 
												+ context.getString(R.string.left_conversation)));
					}
					else
					{
						participantInfo.setText(R.string.group_chat_end);
					}
					((ViewGroup) holder.participantInfoContainer).addView(participantInfo);
				}
			} 
			catch (JSONException e) 
			{
				Log.e(getClass().getSimpleName(), "Invalid JSON", e);
			}
			return v;
		}
			

		MessageMetadata metadata = convMessage.getMetadata();
		if (metadata != null)
		{
			Spannable spannable = metadata.getMessage(context, convMessage, true);
			holder.messageTextView.setText(spannable);
			holder.messageTextView.setMovementMethod(LinkMovementMethod.getInstance());
		}
		else
		{
			CharSequence markedUp = convMessage.getMessage();
			// Fix for bug where if a participant leaves the group chat, the participant's name is never shown 
			if(convMessage.isGroupChat() && !convMessage.isSent() && convMessage.getGroupParticipantMsisdn() != null)
			{
				markedUp = Utils.addContactName(((GroupConversation) conversation).getGroupParticipant(convMessage.getGroupParticipantMsisdn()).getContactInfo().getFirstName(), markedUp);
			}
			SmileyParser smileyParser = SmileyParser.getInstance();
			markedUp = smileyParser.addSmileySpans(markedUp);
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
