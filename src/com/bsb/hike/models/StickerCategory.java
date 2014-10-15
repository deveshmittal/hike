package com.bsb.hike.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;

public class StickerCategory implements Serializable
{

	private String categoryId;

	private boolean updateAvailable;

	private boolean reachedEnd = false;
	
	private String categoryName;
	
	private boolean isVisible;
	
	private boolean isCustom;
	
	private boolean isAdded;
	
	private int catIndex;
	
	private String metadata;
	
	private int totalStickers;
	
	private int timeStamp;


	public StickerCategory(String categoryId, String categoryName, boolean updateAvailable, boolean hasreachedEnd, boolean isVisible, boolean isCustom, boolean isAdded,
			int catIndex, String metadata, int totalStickers, int timeStamp)
	{
		this.categoryId = categoryId;
		this.updateAvailable = updateAvailable;
		this.reachedEnd = hasreachedEnd;
		this.categoryName = categoryName;
		this.isVisible = isVisible;
		this.isCustom = isCustom;
		this.isAdded = isAdded;
		this.catIndex = catIndex;
		this.metadata = metadata;
		this.totalStickers = totalStickers;
		this.timeStamp = timeStamp;
	}

	// this is mostly used for recents stickers only
	public StickerCategory(String category)
	{
		this.categoryId = category;
		this.updateAvailable = false;
	}

	public StickerCategory()
	{

	}

	public String getCategoryId()
	{
		return categoryId;
	}

	public void setCategoryId(String categoryId)
	{
		this.categoryId = categoryId;
	}

	public boolean isUpdateAvailable()
	{
		return updateAvailable;
	}

	public void setUpdateAvailable(boolean updateAvailable)
	{
		this.updateAvailable = updateAvailable;
	}

	public String getCategoryName()
	{
		return categoryName;
	}

	public void setCategoryName(String categoryName)
	{
		this.categoryName = categoryName;
	}

	public boolean isVisible()
	{
		return isVisible;
	}

	public void setVisible(boolean isVisible)
	{
		this.isVisible = isVisible;
	}

	public boolean isCustom()
	{
		return isCustom;
	}

	public void setCustom(boolean isCustom)
	{
		this.isCustom = isCustom;
	}

	public boolean isAdded()
	{
		return isAdded;
	}

	public void setAdded(boolean isAdded)
	{
		this.isAdded = isAdded;
	}

	public int isCatIndex()
	{
		return catIndex;
	}

	public void setCatIndex(int catIndex)
	{
		this.catIndex = catIndex;
	}

	public String getMetadata()
	{
		return metadata;
	}

	public void setMetadata(String metadata)
	{
		this.metadata = metadata;
	}

	public int getTotalStickers()
	{
		return totalStickers;
	}

	public void setTotalStickers(int totalStickers)
	{
		this.totalStickers = totalStickers;
	}

	public int getTimeStamp()
	{
		return timeStamp;
	}

	public void setTimeStamp(int timeStamp)
	{
		this.timeStamp = timeStamp;
	}
	
	public boolean hasReachedEnd()
	{
		return reachedEnd;
	}

	public void setReachedEnd(boolean reachedEnd)
	{
		this.reachedEnd = reachedEnd;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((categoryId == null) ? 0 : categoryId.hashCode());
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
		StickerCategory other = (StickerCategory) obj;
		if (categoryId == null)
		{
			if (other.categoryId != null)
				return false;
		}
		else if (!categoryId.equals(other.categoryId))
		{
			return false;
		}
		return true;
	}

	public void serializeObj(ObjectOutputStream out) throws IOException
	{
		out.writeUTF(categoryId);
		out.writeBoolean(updateAvailable);
		out.writeBoolean(reachedEnd);
	}

	public void deSerializeObj(ObjectInputStream in) throws OptionalDataException, ClassNotFoundException, IOException
	{
		categoryId = in.readUTF();
		updateAvailable = in.readBoolean();
		reachedEnd = in.readBoolean();
	}
}