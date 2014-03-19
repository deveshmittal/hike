package com.bsb.hike.models;

public class OverFlowMenuItem
{
	private String name;

	private int key;

	public OverFlowMenuItem(String name, int key)
	{
		this.name = name;
		this.key = key;
	}

	public String getName()
	{
		return name;
	}

	public int getKey()
	{
		return key;
	}
}