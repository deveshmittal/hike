package com.bsb.hike.chatthread;

public class OverFlowMenuItem {
	public String text;
	public int unreadCount;
	public int drawableId;
	public int uniqueness;

	public OverFlowMenuItem(String text, int unreadCount, int drawableId,
			int uniqueness) {
		this.text = text;
		this.unreadCount = unreadCount;
		this.drawableId = drawableId;
		this.uniqueness = uniqueness;
	}

}
