package com.bsb.hike.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;

public class EmoticonPageAdapter extends BaseAdapter {

	LayoutInflater inflater;
	int currentPage;
	int offset;

	private int[] recentEmoticons;
	private int[] emoticonSubCategories;
	private int[] emoticonResIds;
	private int idOffset;

	public EmoticonPageAdapter(Context context, int[] emoticonSubCategories,
			int[] emoticonResIds, int currentPage, int idOffset) {
		this.currentPage = currentPage;
		this.inflater = LayoutInflater.from(context);
		this.emoticonSubCategories = emoticonSubCategories;
		this.emoticonResIds = emoticonResIds;
		this.idOffset = idOffset;

		/*
		 * There will be a positive offset for subcategories having a greater
		 * than 1 index.
		 */
		if (currentPage > 1) {
			for (int i = 0; i <= currentPage - 2; i++) {
				this.offset += emoticonSubCategories[i];
			}
		} else if (currentPage == 0) {
			int startOffset = idOffset;
			int endOffset = startOffset + emoticonResIds.length;

			recentEmoticons = HikeConversationsDatabase.getInstance()
					.fetchEmoticonsOfType(startOffset, endOffset, -1);
		}
	}

	@Override
	public int getCount() {
		if (currentPage == 0) {
			return recentEmoticons.length;
		} else {
			return emoticonSubCategories[currentPage - 1];
		}
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.emoticon_item, parent,
					false);
		}

		if (currentPage == 0) {
			convertView.setTag(Integer.valueOf(recentEmoticons[position]));
			((ImageView) convertView)
					.setImageResource(emoticonResIds[recentEmoticons[position]
							- idOffset]);
		} else {
			convertView.setTag(Integer.valueOf(idOffset + offset + position));
			((ImageView) convertView).setImageResource(emoticonResIds[offset
					+ position]);
		}
		return convertView;
	}
}
