package com.bsb.hike.models;

public class SocialNetFriendInfo implements Comparable<SocialNetFriendInfo>
{
	private String name;

	private String id;

	private String imageUrl;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getImageUrl()
	{
		return imageUrl;
	}

	public void setImageUrl(String imageUrl)
	{
		this.imageUrl = imageUrl;
	}

	@Override
	public int compareTo(SocialNetFriendInfo another)
	{
		return this.name.compareTo(another.getName());
	}
}
