package com.bsb.hike.models;

public class ContactInfoData
{

	public static enum DataType
	{
		PHONE_NUMBER, EMAIL, ADDRESS, EVENT
	}

	private DataType dataType;

	private String data;

	private String dataSubType;

	public ContactInfoData(DataType dataType, String data, String dataSubType)
	{
		this.dataType = dataType;
		this.data = data;
		this.dataSubType = dataSubType;
	}

	public DataType getDataType()
	{
		return dataType;
	}

	public String getData()
	{
		return data;
	}

	public String getDataSubType()
	{
		return dataSubType;
	}

}
