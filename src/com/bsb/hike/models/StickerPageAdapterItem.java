package com.bsb.hike.models;

public class StickerPageAdapterItem
{
	private int id;

	private Sticker item;

	public static final int STICKER = 1;

	public static final int UPDATE = 2;

	public static final int DOWNLOADING = 3;

	public static final int RETRY = 4;

	public static final int DONE = 5;

	public static final int PLACE_HOLDER = 6;

	public StickerPageAdapterItem(int id, Sticker item)
	{
		this.id = id;
		this.item = item;
	}

	public StickerPageAdapterItem(int id)
	{
		this.id = id;
	}

	public Sticker getSticker()
	{
		return item;
	}

	public int getStickerPageAdapterItemId()
	{
		return id;
	}

	/* Need to override equals and hashcode inorder to use them in recentStickers linkedhashset */
	@Override
	public boolean equals(Object object)
	{
		if (object == null)
			return false;
		if (object == this)
			return true;
		if (!(object instanceof StickerPageAdapterItem))
			return false;
		if (((StickerPageAdapterItem) object).getStickerPageAdapterItemId() != this.getStickerPageAdapterItemId())
		{
			return false;
		}
		if (((StickerPageAdapterItem) object).getStickerPageAdapterItemId() == this.getStickerPageAdapterItemId())
		{
			if (this.getStickerPageAdapterItemId() == STICKER)
			{
				return (this.getSticker().equals(((StickerPageAdapterItem) object).getSticker()));
			}
			else
			{
				return true;
			}
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		int hash = 37;
		if (this.item != null)
		{
			hash += 7 * hash + this.item.hashCode();
		}
		hash += 7 * hash + this.id;

		return hash;
	}

}
