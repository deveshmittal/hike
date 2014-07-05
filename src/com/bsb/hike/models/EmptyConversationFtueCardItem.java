package com.bsb.hike.models;

public class EmptyConversationFtueCardItem extends EmptyConversationItem
{
	int headerTxtResId;
	
	int subTxtResId;
	
	int imgResId;
	
	int imgBgColor;
	
	boolean isClickable;
	
	int clickableTxtResId;
	
	int clickableTxtColor;
	
	public EmptyConversationFtueCardItem(int type, int imgResId, int imgBgColor,int headerTxtResId, int subTxtResId, int clickableTxtResId, int clickableTxtColor)
	{
		super(type);
		
		this.headerTxtResId = headerTxtResId;
		
		this.subTxtResId = subTxtResId;
		
		this.imgResId = imgResId;
		
		this.imgBgColor = imgBgColor;
		
		this.clickableTxtResId = clickableTxtResId;
		
		this.clickableTxtColor = clickableTxtColor;
	}

	public int getHeaderTxtResId()
	{
		return headerTxtResId;
	}

	public void setHeaderTxtResId(int headerTxtResId)
	{
		this.headerTxtResId = headerTxtResId;
	}

	public int getSubTxtResId()
	{
		return subTxtResId;
	}

	public void setSubTxtResId(int subTxtResId)
	{
		this.subTxtResId = subTxtResId;
	}

	public int getImgResId()
	{
		return imgResId;
	}

	public void setImgResId(int imgResId)
	{
		this.imgResId = imgResId;
	}

	public int getImgBgColor()
	{
		return imgBgColor;
	}

	public void setImgBgColor(int imgBgColor)
	{
		this.imgBgColor = imgBgColor;
	}

	public boolean isClickable()
	{
		return isClickable;
	}

	public void setClickable(boolean isClickable, int clickableTxtColor)
	{
		this.isClickable = isClickable;
		this.clickableTxtColor = clickableTxtColor;
	}

	public int getClickableTxtResId()
	{
		return clickableTxtResId;
	}
	
	public int getClickableTxtColor()
	{
		return clickableTxtColor;
	}

	public void setClickableTxtColor(int clickableTxtColor)
	{
		this.clickableTxtColor = clickableTxtColor;
	}
}