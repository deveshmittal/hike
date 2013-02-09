package com.bsb.hike.models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.ocpsoft.pretty.time.PrettyTime;

public class StatusMessage {

	public static enum StatusMessageType {
		TEXT, IMAGE, TEXT_IMAGE, PROFILE_PIC, FRIEND_REQUEST, FRIEND_REQUEST_ACCEPTED, NO_STATUS, USER_ACCEPTED_FRIEND_REQUEST
	}

	private long id;
	private String mappedId;
	private String msisdn;
	private String name;
	private String text;
	private StatusMessageType statusMessageType;
	private boolean statusSeen;
	private long timeStamp;

	public StatusMessage(JSONObject statusMessageJson) {
		this.msisdn = statusMessageJson.optString(HikeConstants.FROM);
		this.timeStamp = statusMessageJson.optLong(HikeConstants.TIMESTAMP,
				System.currentTimeMillis()) / 1000;

		JSONObject data = statusMessageJson.optJSONObject(HikeConstants.DATA);

		this.mappedId = data.optString(HikeConstants.STATUS_ID);

		if (data.optBoolean(HikeConstants.PROFILE)) {
			this.statusMessageType = StatusMessageType.PROFILE_PIC;
			this.text = "";
		} else if (data.has(HikeConstants.STATUS_MESSAGE)) {
			this.statusMessageType = StatusMessageType.TEXT;
			this.text = data.optString(HikeConstants.STATUS_MESSAGE);
		}
	}

	public StatusMessage(long id, String mappedId, String msisdn, String name,
			String text, StatusMessageType statusMessageType, long timeStamp) {
		this(id, mappedId, msisdn, name, text, statusMessageType, timeStamp,
				false);
	}

	public StatusMessage(long id, String mappedId, String msisdn, String name,
			String text, StatusMessageType statusMessageType, long timeStamp,
			boolean statusSeen) {
		this.id = id;
		this.mappedId = mappedId;
		this.msisdn = msisdn;
		this.name = name;
		this.text = text;
		this.statusMessageType = statusMessageType;
		this.timeStamp = timeStamp;
		this.statusSeen = statusSeen;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public String getMappedId() {
		return mappedId;
	}

	public String getMsisdn() {
		return msisdn;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getText() {
		return text;
	}

	public StatusMessageType getStatusMessageType() {
		return statusMessageType;
	}

	public boolean isStatusSeen() {
		return statusSeen;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public String getTimestampFormatted(boolean pretty) {
		Date date = new Date(timeStamp * 1000);
		if (pretty) {
			PrettyTime p = new PrettyTime();
			return p.format(date);
		} else {
			String format = "d MMM ''yy 'AT' h:mm aaa";
			DateFormat df = new SimpleDateFormat(format);
			return df.format(date);
		}
	}
}
