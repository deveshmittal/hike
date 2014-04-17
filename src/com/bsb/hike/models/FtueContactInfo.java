package com.bsb.hike.models;

public class FtueContactInfo extends ContactInfo
{
	private boolean isFromFtue = false;

	public FtueContactInfo(ContactInfo contactInfo)
	{
		super(contactInfo);
	}

	public boolean isFromFtue()
	{
		return isFromFtue;
	}

	public void setFromFtue(boolean isFromFtue)
	{
		this.isFromFtue = isFromFtue;
	}

}
