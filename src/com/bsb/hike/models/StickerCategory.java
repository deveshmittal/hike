package com.bsb.hike.models;

import com.bsb.hike.R;

public class StickerCategory {

	public static String BACK_CATEGORY_ID = "back";
	public static int BACK_CATEGORY_RES_ID = R.drawable.ic_sticker_back;

	public String categoryId;
	public int categoryResId;

	public StickerCategory(String categoryId, int categoryResId) {
		this.categoryId = categoryId;
		this.categoryResId = categoryResId;
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