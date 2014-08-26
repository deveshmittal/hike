package com.bsb.hike.models;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import android.content.Context;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.Logger;
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
		setupIndexForSwapedCategories();
	}

	public Sticker(StickerCategory category, String stickerId)
	{
		this.category = category;
		this.stickerId = stickerId;
		setupStickerindex(category, stickerId);
	}

	public Sticker(String categoryName, String stickerId)
	{
		this.stickerId = stickerId;
		this.category = StickerManager.getInstance().getCategoryForName(categoryName);
		setupStickerindex(category, stickerId);
	}

	public Sticker()
	{

	}

	public Sticker(String categoryName, String stickerId, int stickerIdx)
	{
		this.stickerId = stickerId;
		this.category = StickerManager.getInstance().getCategoryForName(categoryName);
		this.stickerIndex = stickerIdx;
		setupIndexForSwapedCategories();
	}

	public boolean isUnknownSticker()
	{
		return category == null || (category.categoryId == StickerCategoryId.unknown);
	}

	private void setupStickerindex(StickerCategory category2, String stickerId2)

	{
		/*
		 * 
		 * Only set sticker index if the category is a local one
		 */
		String[] cat = null;

		if (category.categoryId == StickerCategoryId.humanoid)
		{
			cat = StickerManager.getInstance().LOCAL_STICKER_IDS_HUMANOID;
		}
		else if (category.categoryId == StickerCategoryId.expressions)
		{
			cat = StickerManager.getInstance().LOCAL_STICKER_IDS_EXPRESSIONS;
		}

		if (cat != null)
		{
			int count = cat.length;
			for (int i = 0; i < count; i++)

			{
				if (cat[i].equals(stickerId))

				{
					this.stickerIndex = i;
					break;

				}
			}
		}
	}

	public boolean isDefaultSticker()
	{
		// TODO : change this logic to make it much more robust as searching in array is not good

		if (category != null)
		{
			if (category.categoryId == StickerCategoryId.humanoid)
			{
				int count = StickerManager.getInstance().LOCAL_STICKER_IDS_HUMANOID.length;
				for (int i = 0; i < count; i++)
				{
					if (StickerManager.getInstance().LOCAL_STICKER_IDS_HUMANOID[i].equals(stickerId))
						return true;
				}
			}
			else if (category.categoryId == StickerCategoryId.expressions)
			{
				int count = StickerManager.getInstance().LOCAL_STICKER_IDS_EXPRESSIONS.length;
				for (int i = 0; i < count; i++)
				{
					if (StickerManager.getInstance().LOCAL_STICKER_IDS_EXPRESSIONS[i].equals(stickerId))
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
		final int BEFORE = -1;
		final int EQUAL = 0;
		final int AFTER = 1;
		if (this == rhs)
			return EQUAL;

		if (rhs == null)
			throw new NullPointerException();

		if (TextUtils.isEmpty(this.stickerId) && TextUtils.isEmpty(rhs.stickerId))
		{
			return (EQUAL);
		}
		else if (TextUtils.isEmpty(this.stickerId))
		{
			return AFTER;
		}
		else if (TextUtils.isEmpty(rhs.stickerId))
		{
			return BEFORE;
		}
		return (this.stickerId.toLowerCase().compareTo(rhs.stickerId.toLowerCase()));
	}

	/* Need to override equals and hashcode inorder to use them in recentStickers linkedhashset */
	@Override
	public boolean equals(Object object)
	{
		if (object == null)
			return false;
		if (object == this)
			return true;
		if (!(object instanceof Sticker))
			return false;
		Sticker st = (Sticker) object;
		return ((this.stickerId.equals(st.getStickerId())) && (this.category != null && st.getCategory() != null && this.category.categoryId == st.getCategory().categoryId));
	}

	@Override
	public int hashCode()
	{
		int hash = 3;
		if (category != null)
			hash = 7 * hash + this.category.categoryId.ordinal();
		hash = 7 * hash + this.stickerId.hashCode();
		return hash;
	}

	public void serializeObj(ObjectOutputStream out) throws IOException
	{
		out.writeInt(stickerIndex);
		out.writeUTF(stickerId);
		category.serializeObj(out);
	}

	public void deSerializeObj(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		stickerIndex = in.readInt();
		stickerId = in.readUTF();
		category = new StickerCategory();
		category.deSerializeObj(in);
		setupIndexForSwapedCategories();
	}

	/*
	 * We save sticker index -1 for all non-hardcoded stickers in message metadata. So when moving doggy to non-hardcoded and expressions to hardcoded. all doggy sticker will now
	 * be stickerIndex -1 and all hardcoded expressions will have a non negetive index value
	 */
	private void setupIndexForSwapedCategories()
	{
		if (category != null)
		{
			if (category.categoryId.equals(StickerCategoryId.doggy))
			{
				this.stickerIndex = -1;
				return;
			}
			if (category.categoryId.equals(StickerCategoryId.expressions)||category.categoryId.equals(StickerCategoryId.humanoid) && stickerIndex == -1)
			{
				setupStickerindex(category, stickerId);
				return;
			}
		}
	}

	public boolean isInAppSticker()
	{
		if(category != null)
		{
			if((category.categoryId == StickerCategoryId.humanoid || category.categoryId == StickerCategoryId.expressions) && stickerIndex >= 0)
				return true;
		}
		return false;
	}
}
