package com.bsb.hike.models;

import java.util.List;

public class Conversation implements Comparable {

	public String getMsisdn() {
		return msisdn;
	}

	public long getConvId() {
		return convId;
	}

	public String getContactId() {
		return contactId;
	}

	private String msisdn;
	private long convId;
	private String contactId;
	private List<ConvMessage> messages;

	public Conversation(String msisdn, long convId,
			String contactId) {
		this.msisdn = msisdn;
		this.convId = convId;
		this.contactId = contactId;
	}

	public void setMessages(List<ConvMessage> messages) {
		this.messages = messages;
	}

	public void addMessage(ConvMessage message) {
		this.messages.add(message);
	}

	@Override
	public int compareTo(Object obj) {
		Conversation rhs = (Conversation) obj;
		if (this.equals(rhs)) {
			return 0;
		}

		int ts = messages.isEmpty() ? 0 : messages.get(0).getTimestamp();
		int rhsTs = rhs.messages.get(0).getTimestamp();
		if (rhsTs != ts) {
			return (ts < rhsTs) ? -1 : 1;
		}

		int ret = msisdn.compareTo(rhs.msisdn);
		if (ret != 0) { return ret; }
			
		if (convId != rhs.convId) {
			return (convId < rhs.convId) ? -1 : 1;
		}

		String cId = (contactId != null) ? contactId : "";
		return cId.compareTo(rhs.contactId);
	}

	public List<ConvMessage> getMessages() {
		return messages;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((contactId == null) ? 0 : contactId.hashCode());
		result = prime * result + (int) (convId ^ (convId >>> 32));
		result = prime * result
				+ ((messages == null) ? 0 : messages.hashCode());
		result = prime * result + ((msisdn == null) ? 0 : msisdn.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Conversation other = (Conversation) obj;
		if (contactId == null) {
			if (other.contactId != null)
				return false;
		} else if (!contactId.equals(other.contactId))
			return false;
		if (convId != other.convId)
			return false;
		if (messages == null) {
			if (other.messages != null)
				return false;
		} else if (!messages.equals(other.messages))
			return false;
		if (msisdn == null) {
			if (other.msisdn != null)
				return false;
		} else if (!msisdn.equals(other.msisdn))
			return false;
		return true;
	}
}
