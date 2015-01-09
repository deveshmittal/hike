package com.bsb.hike.modules.httpmgr;

import com.bsb.hike.modules.httpmgr.response.Response;

/**
 * A util class for the header of the {@link Request} or {@link Response} class
 * 
 * @author sidharth
 * 
 */
public class Header
{
	private String name;

	private String value;

	public Header(String name, String value)
	{
		this.name = name;
		this.value = value;
	}

	public String getName()
	{
		return name;
	}

	public String getValue()
	{
		return value;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (other == null || getClass() != other.getClass())
		{
			return false;
		}

		Header header = (Header) other;
		if (name != null ? !name.equals(header.name) : header.name != null)
		{
			return false;
		}
		if (value != null ? !value.equals(header.value) : header.value != null)
		{
			return false;
		}
		return true;
	}

	@Override
	public int hashCode()
	{
		// TODO
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + (value != null ? value.hashCode() : 0);
		return result;
	}

	@Override
	public String toString()
	{
		return (name != null ? name : "") + ": " + (value != null ? value : "");
	}
}
