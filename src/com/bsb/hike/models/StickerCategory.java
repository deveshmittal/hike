package com.bsb.hike.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;

import com.bsb.hike.utils.StickerManager.StickerCategoryId;

public class StickerCategory implements Serializable
{

	public StickerCategoryId categoryId;

	public boolean updateAvailable;

	private boolean reachedEnd = false;

	public StickerCategory(StickerCategoryId categoryId, boolean updateAvailable)
	{
		this.categoryId = categoryId;
		this.updateAvailable = updateAvailable;
	}

	public StickerCategory(StickerCategoryId categoryId, boolean updateAvailable, boolean hasreachedEnd)
	{
		this.categoryId = categoryId;
		this.updateAvailable = updateAvailable;
		this.reachedEnd = hasreachedEnd;
	}

	// this is mostly used for recents stickers only
	public StickerCategory(StickerCategoryId category)
	{
		this.categoryId = category;
		this.updateAvailable = false;
	}

	public StickerCategory()
	{

	}

	public void setReachedEnd(boolean reachedEnd)
	{
		this.reachedEnd = reachedEnd;
	}

	public boolean hasReachedEnd()
	{
		return reachedEnd;
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
		out.writeUTF(categoryId.name());
		out.writeBoolean(updateAvailable);
		out.writeBoolean(reachedEnd);
	}

	public void deSerializeObj(ObjectInputStream in) throws OptionalDataException, ClassNotFoundException, IOException
	{
		categoryId = StickerCategoryId.valueOf(in.readUTF());
		updateAvailable = in.readBoolean();
		reachedEnd = in.readBoolean();
	}
}