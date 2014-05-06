package com.bsb.hike.adapters;

import java.util.List;
import java.util.Set;

import org.json.JSONArray;

import android.content.Context;
import android.os.CountDownTimer;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
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
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

public class ConversationsAdapter extends ArrayAdapter<Conversation>
{

	private IconLoader iconLoader;

	private int mResourceId;

	private int mIconImageSize;

	private CountDownSetter countDownSetter;
	
	private SparseBooleanArray itemsToAnimat;
	
	private boolean stealthFtueTipAnimated = false;
	
	private boolean resetStealthTipAnimated = false;

	private enum ViewType
	{
		CONVERSATION, GROUP_CHAT_TIP, STEALTH_FTUE_TIP_VIEW, RESET_STEALTH_TIP
	}

	public ConversationsAdapter(Context context, int textViewResourceId, List<Conversation> objects)
	{
		super(context, textViewResourceId, objects);
		this.mResourceId = textViewResourceId;
		mIconImageSize = context.getResources().getDimensionPixelSize(R.dimen.icon_picture_size);
		iconLoader = new IconLoader(context, mIconImageSize);
		iconLoader.setDefaultAvatarIfNoCustomIcon(true);
		itemsToAnimat = new SparseBooleanArray();
	}

	@Override
	public int getViewTypeCount()
	{
		return ViewType.values().length;
	}

	@Override
	public int getItemViewType(int position)
	{
		Conversation conversation = getItem(position);
		if (conversation instanceof ConversationTip)
		{
			switch (((ConversationTip) conversation).getTipType())
			{
			case ConversationTip.GROUP_CHAT_TIP:
				return ViewType.GROUP_CHAT_TIP.ordinal();
			case ConversationTip.STEALTH_FTUE_TIP:
				return ViewType.STEALTH_FTUE_TIP_VIEW.ordinal();
			case ConversationTip.RESET_STEALTH_TIP:
				return ViewType.RESET_STEALTH_TIP.ordinal();
			}
		}
		return ViewType.CONVERSATION.ordinal();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		Context context = getContext();
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final Conversation conversation = getItem(position);

		ViewType viewType = ViewType.values()[getItemViewType(position)];

		View v = convertView;
		if (v == null)
		{
			switch (viewType)
			{
			case CONVERSATION:
				v = inflater.inflate(mResourceId, parent, false);
				break;
			case GROUP_CHAT_TIP:
				v = inflater.inflate(R.layout.group_chat_tip, parent, false);
				break;
			case STEALTH_FTUE_TIP_VIEW:
			case RESET_STEALTH_TIP:
				v = inflater.inflate(R.layout.stealth_ftue_conversation_tip, parent, false);
				break;
			default:
				break;
			}
		}

		if (viewType == ViewType.GROUP_CHAT_TIP)
		{
			TextView tip = (TextView) v.findViewById(R.id.tip);

			String tipString = context.getString(R.string.tap_top_right_group_chat);
			String tipReplaceString = "*";

			SpannableStringBuilder ssb = new SpannableStringBuilder(tipString);
			ssb.setSpan(new ImageSpan(context, R.drawable.ic_group_tip_menu), tipString.indexOf(tipReplaceString), tipString.indexOf(tipReplaceString) + tipReplaceString.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			tip.setText(ssb);

			View close = v.findViewById(R.id.close);
			close.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View view)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.DISMISS_GROUP_CHAT_TIP, null);
				}
			});

			return v;
		}
		else if (viewType == ViewType.STEALTH_FTUE_TIP_VIEW)
		{
			View close = v.findViewById(R.id.close);
			final int pos = position;
			close.setOnClickListener(new OnClickListener()
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
			View close = v.findViewById(R.id.close);
			TextView tipText = (TextView) v.findViewById(R.id.tip);

			long remainingTime = HikeConstants.RESET_COMPLETE_STEALTH_TIME_MS
					- (System.currentTimeMillis() - HikeSharedPreferenceUtil.getInstance(getContext()).getData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME, 0l));

			if (remainingTime <= 0)
			{
				tipText.setText(Html.fromHtml(getContext().getResources().getString(R.string.tap_to_reset_stealth_tip)));
			}
			else
			{
				if (countDownSetter == null)
				{
					countDownSetter = new CountDownSetter(tipText, remainingTime, 1000);
					countDownSetter.start();

					setTimeRemainingText(tipText, remainingTime);
				}
				else
				{
					countDownSetter.setTextView(tipText);
				}
			}

			close.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View view)
				{
					resetStealthTipAnimated = false;
					resetCountDownSetter();

					remove(conversation);
					notifyDataSetChanged();

					Utils.cancelScheduledStealthReset(getContext());
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

		TextView contactView = (TextView) v.findViewById(R.id.contact);
		if(itemToBeAnimated(conversation))
		{
			final Animation animation = AnimationUtils.loadAnimation(context,
		            R.anim.slide_in_from_left);
			v.startAnimation(animation);
			setItemAnimated(conversation);
		}
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
		List<ConvMessage> messages = conversation.getMessages();
		if (!messages.isEmpty())
		{
			ConvMessage message = messages.get(messages.size() - 1);

			ImageView avatarframe = (ImageView) v.findViewById(R.id.avatar_frame);

			ImageView imgStatus = (ImageView) v.findViewById(R.id.msg_status_indicator);

			TextView unreadIndicator = (TextView) v.findViewById(R.id.unread_indicator);
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
					avatarframe.setImageDrawable(null);
					imgStatus.setImageResource(resId);
					imgStatus.setVisibility(View.VISIBLE);
				}
				else if (message.getState() == ConvMessage.State.RECEIVED_UNREAD && (message.getTypingNotification() == null))
				{
					avatarframe.setImageResource(R.drawable.frame_avatar_highlight);
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
					avatarframe.setImageDrawable(null);
				}
			}
			else
			{
				avatarframe.setImageDrawable(null);
			}

			TextView messageView = (TextView) v.findViewById(R.id.last_message);

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
			messageView.setVisibility(View.VISIBLE);
			messageView.setText(markedUp);
			TextView tsView = (TextView) v.findViewById(R.id.last_message_timestamp);
			tsView.setText(message.getTimestampFormatted(true, context));
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

		ImageView avatarView = (ImageView) v.findViewById(R.id.avatar);
		iconLoader.loadImage(conversation.getMsisdn(), true, avatarView, true);
		return v;
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
			textView.setText(Html.fromHtml(getContext().getResources().getString(R.string.tap_to_reset_stealth_tip)));
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
		textView.setText(Html.fromHtml(getContext().getString(R.string.reset_stealth_tip, text)));

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
}
