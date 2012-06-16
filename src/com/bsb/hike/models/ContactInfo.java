package com.bsb.hike.models;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.models.utils.JSONSerializable;

public class ContactInfo implements JSONSerializable, Comparable<ContactInfo>
{
	@Override
	public String toString()
	{
		return name;
	}

	private String name;

	private String msisdn;

	private String id;

	private boolean onhike;
	
	private boolean hasCustomPhoto;
	

	private String phoneNum;

	public String getName()
	{
		return name;
	}

	public String getFirstName()
	{
		if (TextUtils.isEmpty(name))
		{
			return this.msisdn;
		}

		return this.name.split(" ", 2)[0];
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getMsisdn()
	{
		return msisdn;
	}

	public void setMsisdn(String msisdn)
	{
		this.msisdn = msisdn;
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public boolean isOnhike()
	{
		return onhike;
	}

	public void setOnhike(boolean onhike)
	{
		this.onhike = onhike;
	}

	public String getPhoneNum()
	{
		return phoneNum;
	}

	public void setPhoneNum(String phoneNum)
	{
		this.phoneNum = phoneNum;
	}

	public boolean hasCustomPhoto() {
		return hasCustomPhoto;
	}

	public void setHasCustomPhoto(boolean hasCustomPhoto) {
		this.hasCustomPhoto = hasCustomPhoto;
	}

	public ContactInfo(String id, String number, String name,String phoneNum)
	{
		this(id, number, name, phoneNum, false, false);
	}

	public ContactInfo(String id, String number, String name, String phoneNum, boolean onHike)
	{
		this(id, number, name, phoneNum, onHike, false);
	}

	public ContactInfo(String id, String msisdn, String name, String phoneNum, boolean onhike, boolean hasCustomPhoto)
	{
		this.id = id;
		this.msisdn = msisdn;
		this.name = name;
		this.onhike = onhike;
		this.phoneNum = phoneNum;
		this.hasCustomPhoto = hasCustomPhoto;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((phoneNum == null) ? 0 : phoneNum.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ContactInfo other = (ContactInfo) obj;
		if (id == null)
		{
			if (other.id != null)
				return false;
		}
		else if (!id.equals(other.id))
			return false;
		if (name == null)
		{
			if (other.name != null)
				return false;
		}
		else if (!name.equals(other.name))
			return false;
		if (phoneNum == null)
		{
			if (other.phoneNum != null)
				return false;
		}
		else if (!phoneNum.equals(other.phoneNum))
			return false;
		return true;
	}


	public JSONObject toJSON() throws JSONException
	{
		JSONObject json = new JSONObject();
		json.put("phone_no", this.phoneNum);
		json.put("name", this.name);
		json.put("id", this.id);
		return json;
	}

	@Override
	public int compareTo(ContactInfo rhs)
	{
		return (this.name.toLowerCase().compareTo( ((ContactInfo) rhs).name.toLowerCase()));
	}
}
