package com.bsb.hike.adapters;

import java.util.List;
import java.util.Set;

import org.json.JSONArray;

import android.content.Context;
import android.os.CountDownTimer;
import android.text.Html;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.ConversationTip;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

public class ConversationsAdapter extends BaseAdapter
{

	private IconLoader iconLoader;

	private int mIconImageSize;

	private CountDownSetter countDownSetter;
	
	private SparseBooleanArray itemsToAnimat;
	
	private boolean stealthFtueTipAnimated = false;
	
	private boolean resetStealthTipAnimated = false;

	private List<Conversation> conversationList;

	private Context context;

	private enum ViewType
	{
		CONVERSATION, STEALTH_FTUE_TIP_VIEW, RESET_STEALTH_TIP, WELCOME_HIKE_TIP
	}

	private class ViewHolder
	{
		String msisdn;

		TextView headerText;

		View closeTip;

		TextView subText;

		ImageView imageStatus;

		TextView unreadIndicator;

		TextView timeStamp;

		ImageView avatar;
	}

	public ConversationsAdapter(Context context, List<Conversation> objects)
	{
		this.context = context;
		this.conversationList = objects;
		mIconImageSize = context.getResources().getDimensionPixelSize(R.dimen.icon_picture_size);
		iconLoader = new IconLoader(context, mIconImageSize);
		iconLoader.setDefaultAvatarIfNoCustomIcon(true);
		itemsToAnimat = new SparseBooleanArray();
	}

	@Override
	public int getCount()
	{
		return conversationList.size();
	}

	@Override
	public Conversation getItem(int position)
	{
		return conversationList.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}
	@Override
	public int getViewTypeCount()
	{
		return ViewType.values().length;
	}

	public void remove(Conversation conversation)
	{
		conversationList.remove(conversation);
	}

	public void clear()
	{
		conversationList.clear();
	}

	@Override
	public int getItemViewType(int position)
	{
		Conversation conversation = getItem(position);
		if (conversation instanceof ConversationTip)
		{
			switch (((ConversationTip) conversation).getTipType())
			{
			case ConversationTip.STEALTH_FTUE_TIP:
				return ViewType.STEALTH_FTUE_TIP_VIEW.ordinal();
			case ConversationTip.RESET_STEALTH_TIP:
				return ViewType.RESET_STEALTH_TIP.ordinal();
			case ConversationTip.WELCOME_HIKE_TIP:
				return ViewType.WELCOME_HIKE_TIP.ordinal();
			}
		}
		return ViewType.CONVERSATION.ordinal();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final Conversation conversation = getItem(position);

		ViewType viewType = ViewType.values()[getItemViewType(position)];

		View v = convertView;
		ViewHolder viewHolder = null;

		if (v == null)
		{
			viewHolder = new ViewHolder();
			switch (viewType)
			{
			case CONVERSATION:
				v = inflater.inflate(R.layout.conversation_item, parent, false);
				viewHolder.headerText = (TextView) v.findViewById(R.id.contact);
				viewHolder.imageStatus = (ImageView) v.findViewById(R.id.msg_status_indicator);
				viewHolder.unreadIndicator = (TextView) v.findViewById(R.id.unread_indicator);
				viewHolder.subText = (TextView) v.findViewById(R.id.last_message);
				viewHolder.timeStamp = (TextView) v.findViewById(R.id.last_message_timestamp);
				viewHolder.avatar = (ImageView) v.findViewById(R.id.avatar);
				break;
			case STEALTH_FTUE_TIP_VIEW:
			case RESET_STEALTH_TIP:
				v = inflater.inflate(R.layout.stealth_ftue_conversation_tip, parent, false);
				viewHolder.headerText = (TextView) v.findViewById(R.id.tip);
				viewHolder.closeTip = v.findViewById(R.id.close);
				break;
			case WELCOME_HIKE_TIP:
				v = inflater.inflate(R.layout.welcome_hike_tip, parent, false);
				break;	
			default:
				break;
			}

			v.setTag(viewHolder);
		}
		else
		{
			viewHolder = (ViewHolder) v.getTag();
		}

		if (viewType == ViewType.STEALTH_FTUE_TIP_VIEW)
		{
			final int pos = position;
			viewHolder.closeTip.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View view)
				{
					stealthFtueTipAnimated = false;
					HikeMessengerApp.getPubSub().publish(HikePubSub.DISMISS_STEALTH_FTUE_CONV_TIP, pos);
				}
			});
			if(!stealthFtueTipAnimated)
			{
				stealthFtueTipAnimated = true;
				final TranslateAnimation animation = new TranslateAnimation(0, 0, -70*Utils.densityMultiplier, 0);
				animation.setDuration(300);
				parent.startAnimation(animation);
			}
			return v;
		}
		else if (viewType == ViewType.RESET_STEALTH_TIP)
		{
			long remainingTime = HikeConstants.RESET_COMPLETE_STEALTH_TIME_MS
					- (System.currentTimeMillis() - HikeSharedPreferenceUtil.getInstance(context).getData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME, 0l));

			if (remainingTime <= 0)
			{
				viewHolder.headerText.setText(Html.fromHtml(context.getResources().getString(R.string.tap_to_reset_stealth_tip)));
			}
			else
			{
				if (countDownSetter == null)
				{
					countDownSetter = new CountDownSetter(viewHolder.headerText, remainingTime, 1000);
					countDownSetter.start();

					setTimeRemainingText(viewHolder.headerText, remainingTime);
				}
				else
				{
					countDownSetter.setTextView(viewHolder.headerText);
				}
			}

			viewHolder.closeTip.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View view)
				{
					resetStealthTipAnimated = false;
					resetCountDownSetter();

					remove(conversation);
					notifyDataSetChanged();

					Utils.cancelScheduledStealthReset(context);
					Utils.sendUILogEvent(HikeConstants.LogEvent.RESET_STEALTH_CANCEL);
				}
			});
			
			if(!resetStealthTipAnimated)
			{
				resetStealthTipAnimated = true;
				final TranslateAnimation animation = new TranslateAnimation(0, 0, -70*Utils.densityMultiplier, 0);
				animation.setDuration(300);
				parent.startAnimation(animation);
			}
			
			return v;
		}
		else if (viewType == ViewType.WELCOME_HIKE_TIP)
		{
			View close = v.findViewById(R.id.close_tip);
			((TextView)v.findViewById(R.id.tip_header)).setText(R.string.new_ui_welcome_tip_header);
			((TextView)v.findViewById(R.id.tip_msg)).setText(R.string.new_ui_welcome_tip_msg);
			close.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View view)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.REMOVE_WELCOME_HIKE_TIP, null);
				}
			});
			return v;
		}

		viewHolder.msisdn = conversation.getMsisdn();

		updateViewsRelatedToName(v, conversation);

		if (itemToBeAnimated(conversation))
		{
			final Animation animation = AnimationUtils.loadAnimation(context,
		            R.anim.slide_in_from_left);
			v.startAnimation(animation);
			setItemAnimated(conversation);
		}

		List<ConvMessage> messages = conversation.getMessages();
		if (!messages.isEmpty())
		{
			ConvMessage message = messages.get(messages.size() - 1);
			updateViewsRelatedToLastMessage(v, message, conversation);
		}

		updateViewsRelatedToAvatar(v, conversation);

		return v;
	}

	public void updateViewsRelatedToName(View parentView, Conversation conversation)
	{
		ViewHolder viewHolder = (ViewHolder) parentView.getTag();

		/*
		 * If the viewholder's msisdn is different from the converstion's msisdn, it means that the viewholder is currently being used for a different conversation.
		 * We don't need to do anything here then.
		 */
		if(!conversation.getMsisdn().equals(viewHolder.msisdn))
		{
			return;
		}

		TextView contactView = viewHolder.headerText;
		String name = conversation.getLabel();

		contactView.setText(name);
		if (conversation instanceof GroupConversation)
		{
			contactView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_group, 0, 0, 0);
		}
		else
		{
			contactView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
		}
	}

	public void updateViewsRelatedToAvatar(View parentView, Conversation conversation)
	{
		ViewHolder viewHolder = (ViewHolder) parentView.getTag();

		/*
		 * If the viewholder's msisdn is different from the converstion's msisdn, it means that the viewholder is currently being used for a different conversation.
		 * We don't need to do anything here then.
		 */
		if(!conversation.getMsisdn().equals(viewHolder.msisdn))
		{
			return;
		}

		ImageView avatarView = viewHolder.avatar;
		iconLoader.loadImage(conversation.getMsisdn(), true, avatarView, true);
	}

	public void updateViewsRelatedToLastMessage(View parentView, ConvMessage message, Conversation conversation)
	{
		ViewHolder viewHolder = (ViewHolder) parentView.getTag();

		/*
		 * If the viewholder's msisdn is different from the converstion's msisdn, it means that the viewholder is currently being used for a different conversation.
		 * We don't need to do anything here then.
		 */
		if(!conversation.getMsisdn().equals(viewHolder.msisdn))
		{
			return;
		}

		updateViewsRelatedToMessageState(parentView, message, conversation);

		TextView messageView = viewHolder.subText;

		CharSequence markedUp = getConversationText(conversation, message);
		
		messageView.setVisibility(View.VISIBLE);
		messageView.setText(markedUp);
		TextView tsView = viewHolder.timeStamp;
		tsView.setText(message.getTimestampFormatted(true, context));
	}

	public void updateViewsRelatedToMessageState(View parentView, ConvMessage message, Conversation conversation)
	{
		ViewHolder viewHolder = (ViewHolder) parentView.getTag();

		/*
		 * If the viewholder's msisdn is different from the converstion's msisdn, it means that the viewholder is currently being used for a different conversation.
		 * We don't need to do anything here then.
		 */
		if(!conversation.getMsisdn().equals(viewHolder.msisdn))
		{
			return;
		}

		ImageView imgStatus = viewHolder.imageStatus;

		TextView messageView = viewHolder.subText;

		TextView unreadIndicator = viewHolder.unreadIndicator;
		unreadIndicator.setVisibility(View.GONE);
		imgStatus.setVisibility(View.GONE);
		/*
		 * If the message is a status message, we only show an indicator if the status of the message is unread.
		 */
		if (message.getParticipantInfoState() != ParticipantInfoState.STATUS_MESSAGE || message.getState() == State.RECEIVED_UNREAD)
		{
			int resId = message.getImageState();
			if (resId > 0)
			{
				imgStatus.setImageResource(resId);
				imgStatus.setVisibility(View.VISIBLE);
			}
			else if (message.getState() == ConvMessage.State.RECEIVED_UNREAD && (message.getTypingNotification() == null))
			{
				unreadIndicator.setVisibility(View.VISIBLE);

				unreadIndicator.setBackgroundResource(conversation.isStealth() ? R.drawable.bg_unread_counter_stealth : R.drawable.bg_unread_counter);

				if (conversation.getUnreadCount() == 0)
				{
					unreadIndicator.setText("");
				}
				else
				{
					unreadIndicator.setText(Integer.toString(conversation.getUnreadCount()));
				}
			}
			else
			{
			}
		}

		if (message.getState() == ConvMessage.State.RECEIVED_UNREAD)
		{
			/* set unread messages to BLUE */
			messageView.setTextColor(context.getResources().getColor(R.color.unread_message));
		}
		else
		{
			messageView.setTextColor(context.getResources().getColor(R.color.list_item_header));
		}
	}

	private CharSequence getConversationText(Conversation conversation, ConvMessage message)
	{
		MessageMetadata metadata = message.getMetadata();
		CharSequence markedUp = null;

		if (message.isFileTransferMessage())
		{
			markedUp = HikeFileType.getFileTypeMessage(context, metadata.getHikeFiles().get(0).getHikeFileType(), message.isSent());
			if ((conversation instanceof GroupConversation) && !message.isSent())
			{
				markedUp = Utils.addContactName(((GroupConversation) conversation).getGroupParticipantFirstName(message.getGroupParticipantMsisdn()), markedUp);
			}
		}
		else if (message.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_JOINED)
		{
			JSONArray participantInfoArray = metadata.getGcjParticipantInfo();

			String highlight = Utils.getGroupJoinHighlightText(participantInfoArray, (GroupConversation) conversation);

			if (metadata.isNewGroup())
			{
				markedUp = String.format(context.getString(R.string.new_group_message), highlight);
			}
			else
			{
				markedUp = String.format(context.getString(R.string.add_to_group_message), highlight);
			}
		}
		else if (message.getParticipantInfoState() == ParticipantInfoState.DND_USER)
		{
			JSONArray dndNumbers = metadata.getDndNumbers();
			if (dndNumbers != null && dndNumbers.length() > 0)
			{
				StringBuilder dndNames = new StringBuilder();
				for (int i = 0; i < dndNumbers.length(); i++)
				{
					String dndName;
					dndName = conversation instanceof GroupConversation ? ((GroupConversation) conversation).getGroupParticipantFirstName(dndNumbers.optString(i)) : Utils
							.getFirstName(conversation.getLabel());
					if (i < dndNumbers.length() - 2)
					{
						dndNames.append(dndName + ", ");
					}
					else if (i < dndNumbers.length() - 1)
					{
						dndNames.append(dndName + " and ");
					}
					else
					{
						dndNames.append(dndName);
					}
				}
				markedUp = String.format(context.getString(conversation instanceof GroupConversation ? R.string.dnd_msg_gc : R.string.dnd_one_to_one), dndNames.toString());
			}
		}
		else if (message.getParticipantInfoState() == ParticipantInfoState.INTRO_MESSAGE)
		{
			if (conversation.isOnhike())
			{
				boolean firstIntro = conversation.getMsisdn().hashCode() % 2 == 0;
				markedUp = String.format(context.getString(firstIntro ? R.string.start_thread1 : R.string.start_thread1), Utils.getFirstName(conversation.getLabel()));
			}
			else
			{
				markedUp = String.format(context.getString(R.string.intro_sms_thread), Utils.getFirstName(conversation.getLabel()));
			}
		}
		else if (message.getParticipantInfoState() == ParticipantInfoState.USER_JOIN)
		{
			String participantName;
			if (conversation instanceof GroupConversation)
			{
				String participantMsisdn = metadata.getMsisdn();
				participantName = ((GroupConversation) conversation).getGroupParticipantFirstName(participantMsisdn);
			}
			else
			{
				participantName = Utils.getFirstName(conversation.getLabel());
			}
			markedUp = context.getString(metadata.isOldUser() ? R.string.user_back_on_hike : R.string.joined_hike_new, participantName);

		}
		else if (message.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_LEFT || message.getParticipantInfoState() == ParticipantInfoState.GROUP_END)
		{

			if (message.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_LEFT)
			{
				// Showing the block internation sms message if the user was
				// booted because of that reason
				String participantMsisdn = metadata.getMsisdn();
				String participantName = ((GroupConversation) conversation).getGroupParticipantFirstName(participantMsisdn);
				markedUp = String.format(context.getString(R.string.left_conversation), participantName);
			}
			else
			{
				markedUp = context.getString(R.string.group_chat_end);
			}
		}
		else if (message.getParticipantInfoState() == ParticipantInfoState.CHANGED_GROUP_NAME)
		{
			String msisdn = metadata.getMsisdn();

			String userMsisdn = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.MSISDN_SETTING, "");

			String participantName = userMsisdn.equals(msisdn) ? context.getString(R.string.you) : ((GroupConversation) conversation).getGroupParticipantFirstName(msisdn);

			markedUp = String.format(context.getString(R.string.change_group_name), participantName);
		}
		else if (message.getParticipantInfoState() == ParticipantInfoState.BLOCK_INTERNATIONAL_SMS)
		{
			markedUp = context.getString(R.string.block_internation_sms);
		}
		else if (message.getParticipantInfoState() == ParticipantInfoState.CHAT_BACKGROUND)
		{
			String msisdn = metadata.getMsisdn();
			String userMsisdn = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.MSISDN_SETTING, "");

			String nameString;
			if (conversation instanceof GroupConversation)
			{
				nameString = userMsisdn.equals(msisdn) ? context.getString(R.string.you) : ((GroupConversation) conversation).getGroupParticipantFirstName(msisdn);
			}
			else
			{
				nameString = userMsisdn.equals(msisdn) ? context.getString(R.string.you) : Utils.getFirstName(conversation.getLabel());
			}

			markedUp = context.getString(R.string.chat_bg_changed, nameString);
		}
		else
		{
			String msg = message.getMessage();
			/*
			 * Making sure this string is never null.
			 */
			if (msg == null)
			{
				msg = "";
			}
			markedUp = msg.substring(0, Math.min(msg.length(), HikeConstants.MAX_MESSAGE_PREVIEW_LENGTH));
			// For showing the name of the contact that sent the message in
			// a group chat
			if (conversation instanceof GroupConversation && !TextUtils.isEmpty(message.getGroupParticipantMsisdn())
					&& message.getParticipantInfoState() == ParticipantInfoState.NO_INFO)
			{
				markedUp = Utils.addContactName(((GroupConversation) conversation).getGroupParticipantFirstName(message.getGroupParticipantMsisdn()), markedUp);
			}
			SmileyParser smileyParser = SmileyParser.getInstance();
			markedUp = smileyParser.addSmileySpans(markedUp, true);
		}

		return markedUp;
	}

	private class CountDownSetter extends CountDownTimer
	{
		TextView textView;

		public CountDownSetter(TextView textView, long millisInFuture, long countDownInterval)
		{
			super(millisInFuture, countDownInterval);
			this.textView = textView;
		}

		@Override
		public void onFinish()
		{
			if (textView == null)
			{
				return;
			}
			textView.setText(Html.fromHtml(context.getResources().getString(R.string.tap_to_reset_stealth_tip)));
		}

		@Override
		public void onTick(long millisUntilFinished)
		{
			if (textView == null)
			{
				return;
			}

			setTimeRemainingText(textView, millisUntilFinished);
		}

		public void setTextView(TextView tv)
		{
			this.textView = tv;
		}
	}

	private void setTimeRemainingText(TextView textView, long millisUntilFinished)
	{
		long secondsUntilFinished = millisUntilFinished / 1000;
		int minutes = (int) (secondsUntilFinished / 60);
		int seconds = (int) (secondsUntilFinished % 60);
		String text = String.format("%1$02d:%2$02d", minutes, seconds);
		textView.setText(Html.fromHtml(context.getString(R.string.reset_stealth_tip, text)));

	}

	public void resetCountDownSetter()
	{
		if(countDownSetter == null)
		{
			return;
		}

		this.countDownSetter.cancel();
		this.countDownSetter = null;
	}

	public void addItemsToAnimat(Set<Conversation> stealthConversations)
	{
		for (Conversation conversation : stealthConversations)
		{
			itemsToAnimat.put(conversation.hashCode(), true);
		}
	}
	
	public void setItemAnimated(Conversation conv)
	{
		itemsToAnimat.delete(conv.hashCode());
	}
	
	public boolean itemToBeAnimated(Conversation conv)
	{
		return itemsToAnimat.get(conv.hashCode()) && conv.isStealth();
	}

	@Override
	public void notifyDataSetChanged()
	{
		Logger.d("TestList", "NotifyDataSetChanged called");
		super.notifyDataSetChanged();
	}
}
