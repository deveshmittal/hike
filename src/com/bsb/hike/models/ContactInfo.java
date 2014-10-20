package com.bsb.hike.models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.models.utils.JSONSerializable;
import com.bsb.hike.utils.LastSeenComparator;
import com.bsb.hike.utils.Utils;

public class ContactInfo implements JSONSerializable, Comparable<ContactInfo>
{
	public static enum FavoriteType
	{
		NOT_FRIEND, REQUEST_RECEIVED, FRIEND, AUTO_RECOMMENDED_FAVORITE, REQUEST_SENT, REQUEST_SENT_REJECTED, REQUEST_RECEIVED_REJECTED
	}

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

	private String msisdnType;

	private long lastMessaged;

	private String phoneNum;

	private FavoriteType favoriteType;

	private long hikeJoinTime;

	private long lastSeenTime;

	private int offline = 1;

	private long inviteTime;

	private boolean onGreenBlue = false;

	public String getName()
	{
		return name;
	}

	public String getNameOrMsisdn()
	{
		return name != null ? name : msisdn;
	}

	public String getFirstName()
	{
		if (TextUtils.isEmpty(name))
		{
			return this.msisdn;
		}

		return Utils.getFirstName(this.name);
	}
    
	public String getFirstNameAndSurname()
	{
		if(TextUtils.isEmpty(name))
		{
			return this.msisdn;
		}
		
		return Utils.getFirstNameAndSurname(this.name);
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

	public boolean hasCustomPhoto()
	{
		return hasCustomPhoto;
	}

	public void setHasCustomPhoto(boolean hasCustomPhoto)
	{
		this.hasCustomPhoto = hasCustomPhoto;
	}

	public String getMsisdnType()
	{
		return msisdnType;
	}

	public void setMsisdnType(String msisdnType)
	{
		this.msisdnType = msisdnType;
	}

	public long getLastMessaged()
	{
		return lastMessaged;
	}

	public void setLastMessaged(long lastMessaged)
	{
		this.lastMessaged = lastMessaged;
	}

	public FavoriteType getFavoriteType()
	{
		return favoriteType;
	}

	public void setFavoriteType(FavoriteType favoriteType)
	{
		this.favoriteType = favoriteType;
	}

	public long getHikeJoinTime()
	{
		return hikeJoinTime;
	}

	public void setHikeJoinTime(long hikeJoinTime)
	{
		this.hikeJoinTime = hikeJoinTime;
	}

	public boolean isUnknownContact()
	{
		if (msisdn == null)
		{
			return false;
		}
		/*
		 * For unknown contacts, we make the id and msisdn equal.
		 */
		return msisdn.equals(id);
	}

	public boolean isGroupConversationContact()
	{
		if (phoneNum == null)
		{
			return false;
		}
		/*
		 * For group conversations, we make the phone number and id equal.
		 */
		return phoneNum.equals(id);
	}

	public long getLastSeenTime()
	{
		return lastSeenTime;
	}

	public void setLastSeenTime(long lastSeenTime)
	{
		this.lastSeenTime = lastSeenTime;
	}

	public int getOffline()
	{
		return offline;
	}

	public void setOffline(int offline)
	{
		this.offline = offline;
	}

	public long getInviteTime()
	{
		return inviteTime;
	}

	public void setInviteTime(long inviteTime)
	{
		this.inviteTime = inviteTime;
	}

	public boolean isOnGreenBlue()
	{
		return onGreenBlue;
	}

	public void setOnGreenBlue(boolean onGreenBlue)
	{
		this.onGreenBlue = onGreenBlue;
	}

	public String getFormattedHikeJoinTime()
	{
		String format = "MMM ''yy";
		DateFormat df = new SimpleDateFormat(format);
		return df.format(new Date(hikeJoinTime * 1000));
	}

	public ContactInfo(String id, String msisdn, String name, String phoneNum)
	{
		this(id, msisdn, name, phoneNum, false, "", 0, false);
	}

	public ContactInfo(String id, String msisdn, String name, String phoneNum, boolean onHike)
	{
		this(id, msisdn, name, phoneNum, onHike, "", 0, false);
	}

	public ContactInfo(String id, String msisdn, String name, String phoneNum, boolean onhike, String msisdnType, long lastMessaged, boolean hasCustomPhoto)
	{
		this(id, msisdn, name, phoneNum, onhike, msisdnType, lastMessaged, hasCustomPhoto, 0);
	}

	public ContactInfo(String id, String msisdn, String name, String phoneNum, boolean onhike, String msisdnType, long lastMessaged, boolean hasCustomPhoto, long hikeJoinTime)
	{
		this.id = id;
		this.msisdn = msisdn;
		this.name = name;
		this.onhike = onhike;
		this.phoneNum = phoneNum;
		this.hasCustomPhoto = hasCustomPhoto;
		this.msisdnType = msisdnType;
		this.lastMessaged = lastMessaged;
		this.hikeJoinTime = hikeJoinTime;
	}

	public ContactInfo(ContactInfo contactInfo)
	{
		this(contactInfo.getId(), contactInfo.getMsisdn(), contactInfo.getName(), contactInfo.getPhoneNum(), contactInfo.isOnhike(), "", contactInfo.getLastMessaged(), contactInfo
				.hasCustomPhoto(), contactInfo.getHikeJoinTime());
		this.favoriteType = contactInfo.getFavoriteType();
		this.inviteTime = contactInfo.getInviteTime();
		this.lastSeenTime = contactInfo.getLastSeenTime();
		this.offline = contactInfo.getOffline();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((TextUtils.isEmpty(name)) ? 0 : name.hashCode());
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
		if (TextUtils.isEmpty(name))
		{
			if (!TextUtils.isEmpty(other.name))
				return false;
		}
		else if (!name.trim().equals(other.name.trim()))
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

	public static LastSeenComparator lastSeenTimeComparator = new LastSeenComparator(true);
	
	public static LastSeenComparator lastSeenTimeComparatorWithoutFav = new LastSeenComparator(false);

	@Override
	public int compareTo(ContactInfo rhs)
	{
		if (rhs == null)
		{
			return -1;
		}
		if (TextUtils.isEmpty(this.name) && TextUtils.isEmpty(rhs.name))
		{
			return (this.msisdn.toLowerCase().compareTo(((ContactInfo) rhs).msisdn.toLowerCase()));
		}
		else if (TextUtils.isEmpty(this.name))
		{
			return 1;
		}
		else if (TextUtils.isEmpty(rhs.name))
		{
			return -1;
		}
		else if (this.name.startsWith("+") && !rhs.name.startsWith("+"))
		{
			return 1;
		}
		else if (!this.name.startsWith("+") && rhs.name.startsWith("+"))
		{
			return -1;
		}
		return (this.name.toLowerCase().compareTo(((ContactInfo) rhs).name.toLowerCase()));
	}
}
