package com.bsb.hike.models;

import com.bsb.hike.R;


public class StickerCategory {

	public String categoryId;
	public int categoryResId; // this is for showing small icons in sticker pallete, one those are below actual stickers
	public String downloadDialogPref;
	public int categoryPreviewResId;
	public boolean updateAvailable;

	public StickerCategory(String categoryId, int categoryResId,
			String downloadDialogPref, int categoryPreviewResId,
			boolean updateAvailable) {
		this.categoryId = categoryId;
		this.categoryResId = categoryResId;
		this.downloadDialogPref = downloadDialogPref;
		this.categoryPreviewResId = categoryPreviewResId;
		this.updateAvailable = updateAvailable;
	}

	// this is mostly used for recents stickers only
	public StickerCategory(String category)
	{
		this.categoryId = category;
		this.categoryResId = R.drawable.humanoid; // change this once you get icon for recents
		this.updateAvailable = false;
		this.downloadDialogPref = "recentDiagPref";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((categoryId == null) ? 0 : categoryId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StickerCategory other = (StickerCategory) obj;
		if (categoryId == null) {
			if (other.categoryId != null)
				return false;
		} else if (!categoryId.equals(other.categoryId)) {
			return false;
		}
		return true;
	}
}