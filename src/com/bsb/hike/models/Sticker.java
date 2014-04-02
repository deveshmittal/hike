package com.bsb.hike.models;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.StickerManager.StickerCategoryId;

public class Sticker implements Serializable, Comparable<Sticker>
{

	/*
	 * Used for the local stickers. Will be -1 for non local stickers
	 */
	private int stickerIndex = -1;

	private String stickerId;

	private StickerCategory category;

	public Sticker(StickerCategory category, String stickerId, int stickerIndex)
	{
		this.stickerId = stickerId;
		this.stickerIndex = stickerIndex;
		this.category = category;
	}

	public Sticker(StickerCategory category, String stickerId)
	{
		this.category = category;
		this.stickerId = stickerId;

		/*
		 * Only set sticker index if the category is a local one
		 */
		if (category != null && category.categoryId.equals(StickerCategoryId.humanoid) || category.categoryId.equals(StickerCategoryId.doggy))
		{
			/*
			 * Making sure there is an '_' character in the sticker name.
			 */
			int idx = stickerId.indexOf("_");
			if (idx != -1)
			{
				int stickerNumber = -1;
				try
				{
					stickerNumber = Integer.valueOf(stickerId.substring(0, idx));
				}
				catch (NumberFormatException e)
				{
					Log.wtf(getClass().getSimpleName(), "Server sent wrong sticker id : " + stickerId);
				}

				if ((category != null && category.categoryId.equals(StickerCategoryId.doggy) && stickerNumber <= StickerManager.getInstance().LOCAL_STICKER_RES_IDS_DOGGY.length)
						|| (category.categoryId.equals(StickerCategoryId.humanoid) && stickerNumber <= StickerManager.getInstance().LOCAL_STICKER_RES_IDS_HUMANOID.length))
				{
					this.stickerIndex = stickerNumber - 1;
				}
			}
		}

	}

	public Sticker(String categoryName, String stickerId)
	{
		this.stickerId = stickerId;
		this.category = StickerManager.getInstance().getCategoryForName(categoryName);
	}

	public Sticker()
	{

	}

	public Sticker(String categoryName, String stickerId, int stickerIdx)
	{
		this.stickerId = stickerId;
		this.category = StickerManager.getInstance().getCategoryForName(categoryName);
		this.stickerIndex = stickerIdx;
	}

	public boolean isDefaultSticker()
	{
		//TODO : change this logic to make it much more robust as searching in array is not good
		
		if (category != null)
		{
			if (category.categoryId == StickerCategoryId.humanoid)
			{
				int count = StickerManager.getInstance().LOCAL_STICKER_IDS_HUMANOID.length;
				for (int i = 0; i < count; i++)
				{
					if(StickerManager.getInstance().LOCAL_STICKER_IDS_HUMANOID[i].equals(stickerId))
						return true;
				}
			}
			else if (category.categoryId == StickerCategoryId.doggy)
			{
				int count = StickerManager.getInstance().LOCAL_STICKER_IDS_DOGGY.length;
				for (int i = 0; i < count; i++)
				{
					if(StickerManager.getInstance().LOCAL_STICKER_IDS_DOGGY[i].equals(stickerId))
						return true;
				}
			}
			return false;
		}
		return false;

	}

	/**
	 * If sticker is default sticker then its not disabled Else if sticker small image does'nt exist then also its disabled
	 * 
	 * @param sticker
	 * @return
	 */
	public boolean isDisabled(Sticker sticker, Context ctx)
	{
		if (sticker.isDefaultSticker())
			return false;
		File f = new File(sticker.getSmallStickerPath(ctx));
		return !f.exists();
	}

	public int getStickerIndex()
	{
		return stickerIndex;
	}

	public String getStickerId()
	{
		return stickerId;
	}

	public StickerCategory getCategory()
	{
		return category;
	}

	public String getStickerPath(Context context)
	{
		String rootPath = category.categoryId == StickerCategoryId.unknown ? null : StickerManager.getInstance().getStickerDirectoryForCategoryId(context,
				category.categoryId.name());
		if (rootPath == null)
		{
			return null;
		}
		return rootPath + HikeConstants.LARGE_STICKER_ROOT + "/" + stickerId;
	}

	public String getSmallStickerPath(Context context)
	{
		return StickerManager.getInstance().getStickerDirectoryForCategoryId(context, category.categoryId.name()) + HikeConstants.SMALL_STICKER_ROOT + "/" + stickerId;
	}

	@Override
	public int compareTo(Sticker rhs)
	{
		if (TextUtils.isEmpty(this.stickerId) && TextUtils.isEmpty(rhs.stickerId))
		{
			return (0);
		}
		else if (TextUtils.isEmpty(this.stickerId))
		{
			return 1;
		}
		else if (TextUtils.isEmpty(rhs.stickerId))
		{
			return -1;
		}
		return (this.stickerId.toLowerCase().compareTo(rhs.stickerId.toLowerCase()));
	}

	/* Need to override equals and hashcode inorder to use them in recentStickers linkedhashset */
	@Override
	public boolean equals(Object object)
	{
		boolean result = false;
		if (object == null || object.getClass() != getClass())
		{
			result = false;
		}
		else
		{
			Sticker st = (Sticker) object;
			if (this.category != null && this.category.categoryId.equals(st.getCategory().categoryId) && this.stickerId.equals(st.getStickerId()))
			{
				result = true;
			}
		}
		return result;
	}

	@Override
	public int hashCode()
	{
		int hash = 3;
		if (category != null)
			hash = 7 * hash + this.category.categoryId.hashCode();
		hash = 7 * hash + this.stickerId.hashCode();
		return hash;
	}

	public void serializeObj(ObjectOutputStream out)
	{
		try
		{
			out.writeInt(stickerIndex);
			out.writeUTF(stickerId);
			category.serializeObj(out);
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void deSerializeObj(ObjectInputStream in)
	{
		try
		{
			stickerIndex = in.readInt();
			stickerId = in.readUTF();
			category = new StickerCategory();
			category.deSerializeObj(in);
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
