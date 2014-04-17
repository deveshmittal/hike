package com.bsb.hike.models;

public class OverFlowMenuItem
{
	private String name;

	private int key;

	private int iconRes;

	public OverFlowMenuItem(String name, int key)
	{
		this(name, key, 0);
	}

	public OverFlowMenuItem(String name, int key, int iconRes)
	{
		this.name = name;
		this.key = key;
		this.iconRes = iconRes;
	}

	public String getName()
	{
		return name;
	}

	public int getKey()
	{
		return key;
	}

	public int getIconRes()
	{
		return iconRes;
	}
}