package com.bsb.hike.models;

import com.bsb.hike.utils.ClearTypingNotification;

public class TypingNotification
{

	private String id;

	private ClearTypingNotification clearTypingNotification;

	public TypingNotification(String id)
	{
		this.id = id;
	}

	public TypingNotification(String id, ClearTypingNotification clearTypingNotification)
	{
		this.id = id;
		this.clearTypingNotification = clearTypingNotification;
	}

	public ClearTypingNotification getClearTypingNotification()
	{
		return clearTypingNotification;
	}

	public String getId()
	{
		return id;
	}
}
