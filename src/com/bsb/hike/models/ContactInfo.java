package com.bsb.hike.models;

public class ContactInfo
{
	@Override
	public String toString()
	{
		return "ContactInfo [name=" + name + ", number=" + number + ", id=" + id + ", onhike=" + onhike + "]";
	}

	public String name;

	public String number;

	public String id;

	public boolean onhike;

	public ContactInfo(String id, String number, String name)
	{
		this(id, number, name, false);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((number == null) ? 0 : number.hashCode());
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
		if (number == null)
		{
			if (other.number != null)
				return false;
		}
		else if (!number.equals(other.number))
			return false;
		return true;
	}

	public ContactInfo(String id, String number, String name, boolean onhike)
	{
		this.id = id;
		this.number = number;
		this.name = name;
		this.onhike = onhike;
	}
}
