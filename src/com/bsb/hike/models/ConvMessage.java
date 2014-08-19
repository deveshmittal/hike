package com.bsb.hike.models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ocpsoft.prettytime.PrettyTime;

import android.content.Context;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class ConvMessage
{
	private boolean isBlockAddHeader;

	private long msgID; // this corresponds to msgID stored in sender's DB

	private long mappedMsgId; // this corresponds to msgID stored in receiver's
								// DB

	private Conversation mConversation;

	private String mMessage;

	private String mMsisdn;

	private long mTimestamp;

	private boolean mIsSent;

	private boolean mIsSMS;

	private State mState;

	private boolean mInvite;

	private MessageMetadata metadata;

	private String groupParticipantMsisdn;

	private ParticipantInfoState participantInfoState;

	private boolean isFileTransferMessage;

	private boolean isStickerMessage;

	private TypingNotification typingNotification;

	private JSONArray readByArray;

	private boolean shouldShowPush = true;

	private boolean isTickSoundPlayed = false;
	
	private int  hashMessage= HikeConstants.HASH_MESSAGE_TYPE.DEFAULT_MESSAGE;

	public int getHashMessage()
	{
		return hashMessage;
	}

	public void setHashMessage(int hashMessage)
	{
		this.hashMessage = hashMessage;
	}

	private int unreadCount = -1;
	private int messageType = HikeConstants.MESSAGE_TYPE.PLAIN_TEXT;
	// private boolean showResumeButton = true;
	
	public int getMessageType()
	{
		return messageType;
	}

	public void setMessageType(int messageType)
	{
		this.messageType = messageType;
	}

	public boolean isInvite()
	{
		return mInvite;
	}

	public void setInvite(boolean mIsInvite)
	{
		this.mInvite = mIsInvite;
	}

	public boolean isFileTransferMessage()
	{
		return isFileTransferMessage;
	}

	public void setIsFileTranferMessage(boolean isFileTransferMessage)
	{
		this.isFileTransferMessage = isFileTransferMessage;
	}

	public boolean isStickerMessage()
	{
		return isStickerMessage;
	}

	public void setIsStickerMessage(boolean isStickerMessage)
	{
		this.isStickerMessage = isStickerMessage;
	}

	// public void setResumeButtonVisibility(boolean visible)
	// {
	// showResumeButton = visible;
	// }
	//
	// public boolean getResumeButtonVisibility()
	// {
	// return showResumeButton;
	// }

	/* Adding entries to the beginning of this list is not backwards compatible */
	public static enum State
	{
		SENT_UNCONFIRMED, /* message sent to server */
		SENT_FAILED, /* message could not be sent, manually retry */
		SENT_CONFIRMED, /* message received by server */
		SENT_DELIVERED, /* message delivered to client device */
		SENT_DELIVERED_READ, /* message viewed by recipient */
		RECEIVED_UNREAD, /* message received, but currently unread */
		RECEIVED_READ, /* message received and read */
		UNKNOWN
	};
	

	public static enum ParticipantInfoState
	{
		NO_INFO, // This is a normal message
		PARTICIPANT_LEFT, // The participant has left
		PARTICIPANT_JOINED, // The participant has joined
		GROUP_END, // Group chat has ended
		USER_OPT_IN, DND_USER, USER_JOIN, CHANGED_GROUP_NAME, CHANGED_GROUP_IMAGE, BLOCK_INTERNATIONAL_SMS, INTRO_MESSAGE, STATUS_MESSAGE, CHAT_BACKGROUND;

		public static ParticipantInfoState fromJSON(JSONObject obj)
		{
			String type = obj.optString(HikeConstants.TYPE);
			if (HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN.equals(type))
			{
				return ParticipantInfoState.PARTICIPANT_JOINED;
			}
			else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_LEAVE.equals(type))
			{
				return ParticipantInfoState.PARTICIPANT_LEFT;
			}
			else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_END.equals(type))
			{
				return ParticipantInfoState.GROUP_END;
			}
			else if (HikeConstants.MqttMessageTypes.USER_JOINED.equals(type))
			{
				return USER_JOIN;
			}
			else if (HikeConstants.MqttMessageTypes.USER_OPT_IN.equals(type))
			{
				return USER_OPT_IN;
			}
			else if (HikeConstants.DND.equals(type))
			{
				return DND_USER;
			}
			else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_NAME.equals(type))
			{
				return CHANGED_GROUP_NAME;
			}
			else if (HikeConstants.MqttMessageTypes.DISPLAY_PIC.equals(type))
			{
				return CHANGED_GROUP_IMAGE;
			}
			else if (HikeConstants.MqttMessageTypes.BLOCK_INTERNATIONAL_SMS.equals(type))
			{
				return BLOCK_INTERNATIONAL_SMS;
			}
			else if (HikeConstants.INTRO_MESSAGE.equals(type))
			{
				return ParticipantInfoState.INTRO_MESSAGE;
			}
			else if (HikeConstants.MqttMessageTypes.STATUS_UPDATE.equals(type))
			{
				return STATUS_MESSAGE;
			}
			else if (HikeConstants.MqttMessageTypes.CHAT_BACKGROUD.equals(type))
			{
				return CHAT_BACKGROUND;
			}
			return NO_INFO;
		}
	}

	public ConvMessage(int unreadCount, long timestamp, long msgId)
	{
		this.unreadCount = unreadCount;
		this.mTimestamp = timestamp;
		this.msgID = msgId;
	}
	
	public ConvMessage(TypingNotification typingNotification)
	{
		this.typingNotification = typingNotification;
	}

	public ConvMessage(String message, String msisdn, long timestamp, State msgState)
	{
		this(message, msisdn, timestamp, msgState, -1, -1);
	}

	public ConvMessage(String message, String msisdn, long timestamp, State msgState, long msgid, long mappedMsgId)
	{
		this(message, msisdn, timestamp, msgState, msgid, mappedMsgId, null);
	}

	public ConvMessage(String message, String msisdn, long timestamp, State msgState, long msgid, long mappedMsgId, String groupParticipantMsisdn)
	{
		this(message, msisdn, timestamp, msgState, msgid, mappedMsgId, groupParticipantMsisdn, false,HikeConstants.MESSAGE_TYPE.PLAIN_TEXT);
	}
	public ConvMessage(String message, String msisdn, long timestamp, State msgState, long msgid, long mappedMsgId, String groupParticipantMsisdn, int type)
	{
		this(message, msisdn, timestamp, msgState, msgid, mappedMsgId, groupParticipantMsisdn, false, type);
	}
	public ConvMessage(String message, String msisdn, long timestamp, State msgState, long msgid, long mappedMsgId, String groupParticipantMsisdn, boolean isSMS, int type)
	{
		this(message, msisdn, timestamp, msgState, msgid, mappedMsgId, groupParticipantMsisdn, isSMS, ParticipantInfoState.NO_INFO, type);
	}

	public ConvMessage(String message, String msisdn, long timestamp, State msgState, long msgid, long mappedMsgId, String groupParticipantMsisdn, boolean isSMS,
			ParticipantInfoState participantInfoState, int type)
	{
		assert (msisdn != null);
		this.mMsisdn = msisdn;
		this.mMessage = message;
		this.mTimestamp = timestamp;
		this.msgID = msgid;
		this.mappedMsgId = mappedMsgId;
		mIsSent = (msgState == State.SENT_UNCONFIRMED || msgState == State.SENT_CONFIRMED || msgState == State.SENT_DELIVERED || msgState == State.SENT_DELIVERED_READ || msgState == State.SENT_FAILED);
		this.groupParticipantMsisdn = groupParticipantMsisdn;
		this.mIsSMS = isSMS;
		this.messageType= type;
		setState(msgState);
		if(msgState.ordinal() >= State.SENT_CONFIRMED.ordinal())
		{
			setTickSoundPlayed(true);
		}
		this.participantInfoState = participantInfoState;
	}

	public ConvMessage(JSONObject obj) throws JSONException
	{
		this.mMsisdn = obj.getString(obj.has(HikeConstants.TO) ? HikeConstants.TO : HikeConstants.FROM); /*
																										 * represents msg is coming from another client
																										 */
		this.groupParticipantMsisdn = obj.has(HikeConstants.TO) && obj.has(HikeConstants.FROM) ? obj.getString(HikeConstants.FROM) : null;
		JSONObject data = obj.getJSONObject(HikeConstants.DATA);
		if (data.has(HikeConstants.SMS_MESSAGE))
		{
			this.mMessage = data.getString(HikeConstants.SMS_MESSAGE);
			mIsSMS = true;
		}
		else
		{
			this.mMessage = data.getString(HikeConstants.HIKE_MESSAGE);
			mIsSMS = false;
		}
		
		this.mTimestamp = data.getLong(HikeConstants.TIMESTAMP);
		/* prevent us from receiving a message from the future */
		long now = System.currentTimeMillis() / 1000;
		this.mTimestamp = (this.mTimestamp > now) ? now : this.mTimestamp;
		/* if we're deserialized an object from json, it's always unread */
		setState(State.RECEIVED_UNREAD);
		msgID = -1;
		String mappedMsgID = data.getString(HikeConstants.MESSAGE_ID);
		try
		{
			this.mappedMsgId = Long.parseLong(mappedMsgID);
		}
		catch (NumberFormatException e)
		{
			Logger.e("CONVMESSAGE", "Exception occured while parsing msgId. Exception : " + e);
			this.mappedMsgId = -1;
			throw new JSONException("Problem in JSON while parsing msgID.");
		}
		this.participantInfoState = ParticipantInfoState.NO_INFO;
		if (data.optBoolean(HikeConstants.POKE))
		{
			JSONObject md = data.has(HikeConstants.METADATA) ? data.getJSONObject(HikeConstants.METADATA) : new JSONObject();
			md.put(HikeConstants.POKE, true);
			data.put(HikeConstants.METADATA, md);
		}
		if (data.has(HikeConstants.METADATA))
		{
			JSONObject mdata = data.getJSONObject(HikeConstants.METADATA);
			if (mdata.has(HikeConstants.PIN_MESSAGE))
			{
				this.messageType = mdata.getInt(HikeConstants.PIN_MESSAGE);
			}
			setMetadata(data.getJSONObject(HikeConstants.METADATA));
		}
		this.isStickerMessage = HikeConstants.STICKER.equals(obj.optString(HikeConstants.SUB_TYPE));
		/**
		 * This is to specifically handle the hike bot cases for now but can be generically used to control which messages have push enabled
		 */
		if (data.has(HikeConstants.PUSH))
		{
			this.shouldShowPush = data.optBoolean(HikeConstants.PUSH, true);
		}
	}

	public ConvMessage(JSONObject obj, Conversation conversation, Context context, boolean isSelfGenerated) throws JSONException
	{
		setMetadata(obj);

		if (participantInfoState != ParticipantInfoState.USER_JOIN || conversation != null)
		{
			this.mMsisdn = conversation != null ? conversation.getMsisdn() : obj.has(HikeConstants.TO) ? obj.getString(HikeConstants.TO) : obj.getString(HikeConstants.FROM);
			this.groupParticipantMsisdn = obj.has(HikeConstants.TO) && obj.has(HikeConstants.FROM) ? obj.getString(HikeConstants.FROM) : null;
		}
		else
		{
			this.mMsisdn = obj.getJSONObject(HikeConstants.DATA).getString(HikeConstants.MSISDN);
		}

		this.mMessage = "";
		this.mTimestamp = System.currentTimeMillis() / 1000;
		switch (this.participantInfoState)
		{
		case PARTICIPANT_JOINED:
			JSONArray arr = metadata.getGcjParticipantInfo();
			this.mMessage = context.getString(metadata.isNewGroup() ? R.string.new_group_message : R.string.add_to_group_message,
					Utils.getGroupJoinHighlightText(arr, (GroupConversation) conversation));
			break;
		case PARTICIPANT_LEFT:
			this.mMessage = String.format(context.getString(R.string.left_conversation), ((GroupConversation) conversation).getGroupParticipantFirstName(metadata.getMsisdn()));
			break;
		case GROUP_END:
			this.mMessage = context.getString(R.string.group_chat_end);
			break;
		case USER_JOIN:
			if (conversation != null)
			{
				String name;
				if (conversation instanceof GroupConversation)
				{
					name = ((GroupConversation) conversation).getGroupParticipantFirstName(metadata.getMsisdn());
				}
				else
				{
					name = Utils.getFirstName(conversation.getLabel());
				}
				this.mMessage = String.format(context.getString(metadata.isOldUser() ? R.string.user_back_on_hike : R.string.joined_hike_new), name);
			}
			break;
		case USER_OPT_IN:
			String name;
			if (conversation instanceof GroupConversation)
			{
				name = ((GroupConversation) conversation).getGroupParticipantFirstName(metadata.getMsisdn());
			}
			else
			{
				name = Utils.getFirstName(conversation.getLabel());
			}
			this.mMessage = String.format(context.getString(conversation instanceof GroupConversation ? R.string.joined_conversation : R.string.optin_one_to_one), name);
			break;
		case CHANGED_GROUP_NAME:
		case CHANGED_GROUP_IMAGE:
			String msisdn = metadata.getMsisdn();
			String userMsisdn = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.MSISDN_SETTING, "");

			String participantName = userMsisdn.equals(msisdn) ? context.getString(R.string.you) : ((GroupConversation) conversation).getGroupParticipantFirstName(msisdn);
			this.mMessage = String.format(
					context.getString(participantInfoState == ParticipantInfoState.CHANGED_GROUP_NAME ? R.string.change_group_name : R.string.change_group_image), participantName);
			break;
		case BLOCK_INTERNATIONAL_SMS:
			this.mMessage = context.getString(R.string.block_internation_sms);
			break;
		case STATUS_MESSAGE:
			this.mTimestamp = metadata.getStatusMessage().getTimeStamp();
			String msg;
			if (metadata.getStatusMessage().getStatusMessageType() == StatusMessageType.PROFILE_PIC)
			{
				msg = context.getString(R.string.changed_profile);
			}
			else
			{
				msg = metadata.getStatusMessage().getText();
			}
			this.mMessage = "\"" + msg + "\"";
			/*
			 * We want all status message state to be read by default.
			 */
			isSelfGenerated = true;
			break;
		case CHAT_BACKGROUND:
			if (conversation != null)
			{

				String nameString;
				if (conversation instanceof GroupConversation)
				{
					nameString = ((GroupConversation) conversation).getGroupParticipantFirstName(metadata.getMsisdn());
				}
				else
				{
					nameString = Utils.getFirstName(conversation.getLabel());
				}
				this.mMessage = context.getString(R.string.chat_bg_changed, nameString);
				;
			}
			break;
		}
		this.mConversation = conversation;
		setState(isSelfGenerated ? State.RECEIVED_READ : State.RECEIVED_UNREAD);
	}

	public void setMetadata(JSONObject metadata) throws JSONException
	{
		if (metadata != null)
		{
			this.metadata = new MessageMetadata(metadata, mIsSent);

			isFileTransferMessage = this.metadata.getHikeFiles() != null;

			participantInfoState = this.metadata.getParticipantInfoState();

			isStickerMessage = this.metadata.getSticker() != null;
			
			
		}
	}

	public void setMetadata(String metadataString) throws JSONException
	{
		if (!TextUtils.isEmpty(metadataString))
		{
			JSONObject metadata = new JSONObject(metadataString);
			setMetadata(metadata);
		}
	}

	public ParticipantInfoState getParticipantInfoState()
	{
		return participantInfoState;
	}

	public void setParticipantInfoState(ParticipantInfoState participantInfoState)
	{
		this.participantInfoState = participantInfoState;
	}

	public MessageMetadata getMetadata()
	{
		return this.metadata;
	}

	public void setMessage(String mMessage)
	{
		this.mMessage = mMessage;
	}

	public String getMessage()
	{
		return mMessage;
	}

	public boolean isSent()
	{
		return mIsSent;
	}

	public void setTimestamp(long timeStamp)
	{
		this.mTimestamp = timeStamp;
	}

	public long getTimestamp()
	{
		return this.mTimestamp;
	}

	public State getState()
	{
		return mState;
	}

	public String getMsisdn()
	{
		return mMsisdn;
	}

	public String getGroupParticipantMsisdn()
	{
		return groupParticipantMsisdn;
	}

	@Override
	public String toString()
	{
		String convId = mConversation == null ? "null" : Long.toString(mConversation.getConvId());
		return "ConvMessage [mConversation=" + convId + ", mMessage=" + mMessage + ", mMsisdn=" + mMsisdn + ", mTimestamp=" + mTimestamp + ", mIsSent=" + mIsSent + ", mState="
				+ mState + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (mIsSent ? 1231 : 1237);
		result = prime * result + ((mMessage == null) ? 0 : mMessage.hashCode());
		result = prime * result + ((mMsisdn == null) ? 0 : mMsisdn.hashCode());
		result = prime * result + ((mState == null) ? 0 : mState.hashCode());
		result = prime * result + (int) (mTimestamp ^ (mTimestamp >>> 32));
		result = prime * result + (int) (msgID ^ (msgID >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConvMessage other = (ConvMessage) obj;

		if (msgID != other.msgID)
		{
			return false;
		}
		if (mIsSent != other.mIsSent)
			return false;
		if (mMessage == null)
		{
			if (other.mMessage != null)
				return false;
		}
		else if (!mMessage.equals(other.mMessage))
			return false;
		if (mMsisdn == null)
		{
			if (other.mMsisdn != null)
				return false;
		}
		else if (!mMsisdn.equals(other.mMsisdn))
			return false;
		if (mState != other.mState)
			return false;
		if (mTimestamp != other.mTimestamp)
			return false;
		return true;
	}

	public JSONObject serialize()
	{
		return serialize(false);
	}

	public JSONObject serialize(boolean sendNativeInvite)
	{
		JSONObject object = new JSONObject();
		JSONObject data = new JSONObject();
		JSONObject md = null;
		try
		{
			if (participantInfoState == ParticipantInfoState.CHAT_BACKGROUND)
			{
				object = metadata.getJSON();
			}
			else
			{
				if (metadata != null)
				{
					if (isFileTransferMessage || isStickerMessage)
					{
						md = metadata.getJSON();
						data.put(HikeConstants.METADATA, md);
					}else if(messageType!=HikeConstants.MESSAGE_TYPE.PLAIN_TEXT)
					{
						md = metadata.getJSON();
						data.put(HikeConstants.METADATA, md);
				    }
					else if (metadata.isPokeMessage())
					{
						data.put(HikeConstants.POKE, true);
					}
				}
				data.put(!mIsSMS ? HikeConstants.HIKE_MESSAGE : HikeConstants.SMS_MESSAGE, mMessage);
				
				data.put(HikeConstants.TIMESTAMP, mTimestamp);

				if (mInvite)
				{
					data.put(HikeConstants.MESSAGE_ID, System.currentTimeMillis());
				}
				else
				{
					data.put(HikeConstants.MESSAGE_ID, msgID);

					if(mConversation.isStealth() && isSent())
					{
						data.put(HikeConstants.STEALTH, true);
					}
				}

				object.put(HikeConstants.TO, mMsisdn);
				object.put(HikeConstants.DATA, data);
				if (isStickerMessage)
				{
					object.put(HikeConstants.SUB_TYPE, HikeConstants.STICKER);
				}

				if (sendNativeInvite && mInvite)
				{
					object.put(HikeConstants.SUB_TYPE, HikeConstants.NO_SMS);
				}

				object.put(HikeConstants.TYPE, mInvite ? HikeConstants.MqttMessageTypes.INVITE : HikeConstants.MqttMessageTypes.MESSAGE);
			}
		}
		catch (JSONException e)
		{
			Logger.e("ConvMessage", "invalid json message", e);
		}
		return object;
	}

	public void setConversation(Conversation conversation)
	{
		this.mConversation = conversation;
	}

	public Conversation getConversation()
	{
		return mConversation;
	}

	public String getTimestampFormatted(boolean pretty, Context context)
	{
		Date date = new Date(mTimestamp * 1000);
		if (pretty)
		{
			PrettyTime p = new PrettyTime();
			return p.format(date);
		}
		else
		{
			String format;
			if (android.text.format.DateFormat.is24HourFormat(context))
			{
				format = "HH:mm";
			}
			else
			{
				format = "h:mm aaa";
			}

			DateFormat df = new SimpleDateFormat(format);
			return df.format(date);
		}
	}

	public String getMessageDate(Context context)
	{
		Date date = new Date(mTimestamp * 1000);
		String format;
		if (android.text.format.DateFormat.is24HourFormat(context))
		{
			format = "d MMM ''yy";
		}
		else
		{
			format = "d MMM ''yy";
		}

		DateFormat df = new SimpleDateFormat(format);
		return df.format(date);
	}

	public void setMsgID(long msgID)
	{
		this.msgID = msgID;
	}

	public long getMsgID()
	{
		return msgID;
	}

	public void setMappedMsgID(long msgID)
	{
		this.mappedMsgId = msgID;
	}

	public long getMappedMsgID()
	{
		return mappedMsgId;
	}
	
	public int getUnreadCount()
	{
		return unreadCount;
	}

	public static State stateValue(int val)
	{
		return State.values()[val];
	}

	public void setState(State state)
	{
		/* only allow the state to increase */
		if (((mState != null) ? mState.ordinal() : 0) <= state.ordinal())
		{
			mState = state;
		}

		/*
		 * We have a bug where a message is flipping from sent to received add this assert to track down when/where it's happening assert(mIsSent == (mState ==
		 * State.SENT_UNCONFIRMED || mState == State.SENT_CONFIRMED || mState == State.SENT_DELIVERED || mState == State.SENT_DELIVERED_READ || mState == State.SENT_FAILED));
		 */
	}

	public JSONObject serializeDeliveryReportRead()
	{
		JSONObject object = new JSONObject();
		JSONArray ids = new JSONArray();
		try
		{
			ids.put(String.valueOf(mappedMsgId));
			object.put(HikeConstants.DATA, ids);
			object.put(HikeConstants.MESSAGE_ID, Long.toString(System.currentTimeMillis()/1000));
			object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.MESSAGE_READ);
			object.put(HikeConstants.TO, mMsisdn);
		}
		catch (JSONException e)
		{
			Logger.e("ConvMessage", "invalid json message", e);
		}
		return object;
	}

	public void setSMS(boolean isSMS)
	{
		this.mIsSMS = isSMS;
	}

	public boolean isSMS()
	{
		return mIsSMS;
	}

	public TypingNotification getTypingNotification()
	{
		return typingNotification;
	}

	public void setTypingNotification(TypingNotification typingNotification)
	{
		this.typingNotification = typingNotification;
	}

	public JSONArray getReadByArray()
	{
		return readByArray;
	}

	public void setReadByArray(String readBy)
	{
		if (TextUtils.isEmpty(readBy))
		{
			return;
		}
		try
		{
			this.readByArray = new JSONArray(readBy);
		}
		catch (JSONException e)
		{
			Logger.w(getClass().getSimpleName(), "Invalid JSON");
		}
	}

	public int getImageState()
	{
		/* received messages have no img */
		if (!isSent())
		{
			return -1;
		}

		/* failed is handled separately, since it's applicable to SMS messages */
		if (mState == State.SENT_FAILED)
		{
			return R.drawable.ic_failed;
		}

		switch (mState)
		{
		case SENT_DELIVERED:
			return R.drawable.ic_delivered;
		case SENT_DELIVERED_READ:
			return R.drawable.ic_read;
		case SENT_CONFIRMED:
			return R.drawable.ic_sent;
		case SENT_UNCONFIRMED:
			return R.drawable.ic_retry_sending;
		default:
			return R.drawable.ic_blank;
		}
	}

	public boolean isGroupChat()
	{
		return Utils.isGroupConversation(this.mMsisdn);
	}

	/**
	 * @return the shouldShowPush
	 */
	public boolean isShouldShowPush()
	{
		return shouldShowPush;
	}

	/**
	 * @param shouldShowPush
	 *            the shouldShowPush to set
	 */
	public void setShouldShowPush(boolean shouldShowPush)
	{
		this.shouldShowPush = shouldShowPush;
	}

	public void setBlockAddHeader(boolean isBlockAddHeader)
	{
		this.isBlockAddHeader = isBlockAddHeader;
	}

	public boolean isBlockAddHeader()
	{
		return isBlockAddHeader;
	}
	
	public boolean isTickSoundPlayed()
	{
		return isTickSoundPlayed;
	}

	public void setTickSoundPlayed(boolean isTickSoundPlayed)
	{
		this.isTickSoundPlayed = isTickSoundPlayed;
	}

}
