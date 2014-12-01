package com.bsb.hike.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.View;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.ui.CreditsActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class MessageMetadata
{

	public static enum NudgeAnimationType
	{
		NONE, SINGLE
	}

	private String dndMissedCallNumber;

	private boolean newUser;

	private JSONObject json;

	private JSONArray dndNumbers;

	private ParticipantInfoState participantInfoState = ParticipantInfoState.NO_INFO;

	private List<HikeFile> hikeFileList;

	private JSONArray gcjParticipantInfo;

	private boolean newGroup;

	private String msisdn;

	private boolean showBIS;

	private int credits;

	private boolean isPokeMessage;

	private StatusMessage statusMessage;

	private Sticker sticker;
	
	private boolean oldUser;
	
	private boolean isGhostMessage;

	public boolean isGhostMessage()
	{
		return isGhostMessage;
	}

	private NudgeAnimationType nudgeAnimationType = NudgeAnimationType.NONE;

	private int pinMessage =0 ;

	public int getPinMessage()
	{
		return pinMessage;
	}

	public void setPinMessage(int pinMessage)
	{
		this.pinMessage = pinMessage;
	}

	public MessageMetadata(String jsonString,boolean isSent) throws JSONException{
		this(new JSONObject(jsonString),isSent);
	}
	public MessageMetadata(JSONObject metadata, boolean isSent) throws JSONException
	{
		this.participantInfoState = metadata.has(HikeConstants.DND_USERS) || metadata.has(HikeConstants.DND_NUMBERS) ? ParticipantInfoState.DND_USER : ParticipantInfoState
				.fromJSON(metadata);
		switch (this.participantInfoState)
		{
		case CHANGED_GROUP_NAME:
			this.msisdn = metadata.getString(HikeConstants.FROM);
			break;

		case DND_USER:
			this.dndNumbers = metadata.has(HikeConstants.DND_USERS) ? metadata.getJSONArray(HikeConstants.DND_USERS) : metadata.getJSONArray(HikeConstants.DND_NUMBERS);
			break;

		case PARTICIPANT_JOINED:
			this.gcjParticipantInfo = metadata.getJSONArray(HikeConstants.DATA);
			this.newGroup = metadata.optBoolean(HikeConstants.NEW_GROUP);
			break;

		case PARTICIPANT_LEFT:
			this.msisdn = metadata.getString(HikeConstants.DATA);
			this.showBIS = HikeConstants.MqttMessageTypes.BLOCK_INTERNATIONAL_SMS.equals(metadata.optString(HikeConstants.SUB_TYPE));
			break;

		case USER_JOIN:
		case USER_OPT_IN:
			this.msisdn = metadata.getJSONObject(HikeConstants.DATA).getString(HikeConstants.MSISDN);
			this.credits = metadata.getJSONObject(HikeConstants.DATA).optInt(HikeConstants.CREDITS, -1);
			this.oldUser = HikeConstants.RETURNING_USER.equals(metadata.optString(HikeConstants.SUB_TYPE));
			break;

		case CHANGED_GROUP_IMAGE:
			this.msisdn = metadata.getString(HikeConstants.FROM);
			break;
		case STATUS_MESSAGE:
			this.statusMessage = new StatusMessage(metadata);
			break;
		case CHAT_BACKGROUND:
			this.msisdn = metadata.optString(HikeConstants.FROM);
			break;
		}
		this.newUser = metadata.optString(HikeConstants.NEW_USER).equals("true");
		this.dndMissedCallNumber = metadata.optString(HikeConstants.METADATA_DND);
		this.hikeFileList = getHikeFileListFromJSONArray(metadata.optJSONArray(HikeConstants.FILES), isSent);
		if (HikeConstants.LOCATION_CONTENT_TYPE.equals(metadata.optString(HikeConstants.CONTENT_TYPE)))
		{
			this.hikeFileList = new ArrayList<HikeFile>();
			this.hikeFileList.add(new HikeFile(metadata, isSent));
		}
		this.isPokeMessage = metadata.optBoolean(HikeConstants.POKE);
		this.pinMessage  = metadata.optInt(HikeConstants.PIN_MESSAGE);
		this.json = metadata;
		if (metadata.has(StickerManager.STICKER_ID))
		{
			String cat = metadata.optString(StickerManager.CATEGORY_ID);
			this.sticker = new Sticker(cat, metadata.optString(StickerManager.STICKER_ID));
		}
	}

	/**
	 * Returns the sticker category. Used only for cases where the category is an unknown one.
	 * 
	 * @return
	 */
	public String getUnknownStickerCategory()
	{
		return json.optString(StickerManager.CATEGORY_ID);
	}

	private List<HikeFile> getHikeFileListFromJSONArray(JSONArray fileList, boolean isSent)
	{
		if (fileList == null)
		{
			return null;
		}
		List<HikeFile> hikeFileList = new ArrayList<HikeFile>();
		for (int i = 0; i < fileList.length(); i++)
		{
			hikeFileList.add(new HikeFile(fileList.optJSONObject(i), isSent));
		}
		return hikeFileList;
	}

	public JSONArray getGcjParticipantInfo()
	{
		return gcjParticipantInfo;
	}

	public boolean isNewGroup()
	{
		return newGroup;
	}

	public String getMsisdn()
	{
		return msisdn;
	}

	public boolean isShowBIS()
	{
		return showBIS;
	}

	public int getCredits()
	{
		return credits;
	}

	public JSONArray getDndNumbers()
	{
		return dndNumbers;
	}

	public List<HikeFile> getHikeFiles()
	{
		return hikeFileList;
	}

	public String getDNDMissedCallNumber()
	{
		return dndMissedCallNumber;
	}

	public ParticipantInfoState getParticipantInfoState()
	{
		return participantInfoState;
	}

	public String serialize()
	{
		return this.json.toString();
	}

	public boolean getNewUser()
	{
		return newUser;
	}

	public boolean isPokeMessage()
	{
		return isPokeMessage;
	}

	public JSONObject getJSON()
	{
		return json;
	}

	public StatusMessage getStatusMessage()
	{
		return statusMessage;
	}

	public Sticker getSticker()
	{
		return sticker;
	}
	
	public boolean isOldUser()
	{
		return oldUser;
	}

	public NudgeAnimationType getNudgeAnimationType()
	{
		return nudgeAnimationType;
	}

	public void setNudgeAnimationType(NudgeAnimationType type)
	{
		this.nudgeAnimationType = type;
	}
}
