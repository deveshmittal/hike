package com.bsb.hike.models;

import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;

public class MessageMetadata
{
	private String dndMissedCallNumber;
	private JSONObject json;
	private ParticipantInfoState participantInfoState = ParticipantInfoState.NO_INFO;

	public MessageMetadata(JSONObject metadata)
	{
		this.dndMissedCallNumber = metadata.optString(HikeConstants.METADATA_DND);
		this.participantInfoState = ParticipantInfoState.fromJSON(metadata);
		this.json = metadata;
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
}
