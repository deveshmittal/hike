package com.bsb.hike.adapters;

import android.app.Activity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

public class EmoticonAdapter extends PagerAdapter implements
		OnItemClickListener {

	public enum EmoticonType {
		HIKE_EMOTICON, EMOJI
	}

	public final int MAX_EMOTICONS_PER_ROW;

	public static final int MAX_EMOTICONS_PER_ROW_PORTRAIT = 7;

	public static final int MAX_EMOTICONS_PER_ROW_LANDSCAPE = 10;

	public static final int RECENTS_SUBCATEGORY_INDEX = -1;

	private int EMOTICON_NUM_PAGES;

	private LayoutInflater inflater;
	private Activity activity;
	private EditText composeBox;
	private int[] recentEmoticons;
	private int[] emoticonResIds;
	private int[] emoticonSubCategories;
	private EmoticonType emoticonType;
	private int idOffset;

	private final int EMOTICON_SIZE = (int) (27 * Utils.densityMultiplier);

	public EmoticonAdapter(Activity activity, EditText composeBox,
			EmoticonType emoticonType, boolean isPortrait) {
		MAX_EMOTICONS_PER_ROW = isPortrait ? MAX_EMOTICONS_PER_ROW_PORTRAIT
				: MAX_EMOTICONS_PER_ROW_LANDSCAPE;

		this.inflater = LayoutInflater.from(activity);
		this.activity = activity;
		this.composeBox = composeBox;
		this.emoticonType = emoticonType;

		switch (emoticonType) {
		// Incrementing these numbers to show a recents tab as well.
		case HIKE_EMOTICON:
			emoticonResIds = EmoticonConstants.DEFAULT_SMILEY_RES_IDS;
			emoticonSubCategories = SmileyParser.HIKE_EMOTICONS_SUBCATEGORIES;
			idOffset = 0;
			break;
		case EMOJI:
			emoticonResIds = EmoticonConstants.EMOJI_RES_IDS;
			emoticonSubCategories = SmileyParser.EMOJI_SUBCATEGORIES;
			idOffset = EmoticonConstants.DEFAULT_SMILEY_RES_IDS.length;
			break;
		}

		EMOTICON_NUM_PAGES = calculateNumPages(emoticonType);
	}

	private int calculateNumPages(EmoticonType emoticonType) {
		switch (emoticonType) {
		case EMOJI:
			return SmileyParser.EMOJI_SUBCATEGORIES.length + 1;
		case HIKE_EMOTICON:
			return SmileyParser.HIKE_EMOTICONS_SUBCATEGORIES.length + 1;
		}
		return 0;
	}

	@Override
	public int getCount() {
		return EMOTICON_NUM_PAGES;
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == object;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		((ViewPager) container).removeView((View) object);
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		View emoticonPage = inflater.inflate(R.layout.emoticon_page, null);

		GridView emoticonGrid = (GridView) emoticonPage
				.findViewById(R.id.emoticon_grid);
		emoticonGrid.setNumColumns(MAX_EMOTICONS_PER_ROW);
		emoticonGrid.setVerticalScrollBarEnabled(false);
		emoticonGrid.setHorizontalScrollBarEnabled(false);
		emoticonGrid.setAdapter(new EmoticonPageAdapter(position));
		emoticonGrid.setOnItemClickListener(this);

		((ViewPager) container).addView(emoticonPage);
		return emoticonPage;
	}

	private class EmoticonPageAdapter extends BaseAdapter {

		int currentPage;
		int offset;
		LayoutInflater inflater;

		public EmoticonPageAdapter(int currentPage) {
			this.currentPage = currentPage;
			this.inflater = LayoutInflater.from(activity);

			/*
			 * There will be a positive offset for subcategories having a
			 * greater than 1 index.
			 */
			if (currentPage > 1) {
				for (int i = 0; i <= currentPage - 2; i++) {
					this.offset += emoticonSubCategories[i];
				}
			} else if (currentPage == 0) {
				int startOffset = idOffset;
				int endOffset = startOffset + emoticonResIds.length;

				recentEmoticons = HikeConversationsDatabase.getInstance()
						.fetchEmoticonsOfType(emoticonType, startOffset,
								endOffset, -1);
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
				convertView = inflater.inflate(R.layout.emoticon_item, null);
			}

			LayoutParams lp;

			lp = new LayoutParams(EMOTICON_SIZE, EMOTICON_SIZE);

			convertView.setLayoutParams(lp);
			if (currentPage == 0) {
				convertView.setTag(Integer.valueOf(idOffset
						+ recentEmoticons[position]));
				((ImageView) convertView)
						.setImageResource(emoticonResIds[recentEmoticons[position]]);
			} else {
				convertView.setTag(Integer
						.valueOf(idOffset + offset + position));
				((ImageView) convertView)
						.setImageResource(emoticonResIds[offset + position]);
			}
			return convertView;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		int emoticonIndex = (Integer) arg1.getTag();
		HikeConversationsDatabase.getInstance().updateRecencyOfEmoticon(
				emoticonIndex, System.currentTimeMillis());
		// We don't add an emoticon if the compose box is near its maximum
		// length of characters
		if (composeBox.length() >= activity.getResources().getInteger(
				R.integer.max_length_message) - 20) {
			return;
		}
		SmileyParser.getInstance().addSmiley(composeBox, emoticonIndex);
	}
}
