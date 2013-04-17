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

	public final int MAX_EMOTICONS_PER_PAGE;
	public final int MAX_EMOTICONS_PER_ROW;

	public static final int MAX_EMOTICONS_PER_PAGE_PORTRAIT = 21;
	public static final int MAX_EMOTICONS_PER_ROW_PORTRAIT = 7;

	public static final int MAX_EMOTICONS_PER_PAGE_LANDSCAPE = 20;
	public static final int MAX_EMOTICONS_PER_ROW_LANDSCAPE = 10;

	public static final int RECENTS_SUBCATEGORY_INDEX = -1;

	private int EMOTICON_NUM_PAGES;

	private LayoutInflater inflater;
	private Activity activity;
	private EditText composeBox;
	private int offset;
	private int whichSubCategory;
	private int[] recentEmoticons;
	private int[] emoticonResIds;
	private int[] emoticonSubCategories;
	private int idOffset;

	private final int EMOTICON_SIZE = (int) (27 * Utils.densityMultiplier);

	public EmoticonAdapter(Activity activity, EditText composeBox,
			EmoticonType emoticonType, int whichSubCategory, boolean isPortrait) {
		MAX_EMOTICONS_PER_PAGE = isPortrait ? MAX_EMOTICONS_PER_PAGE_PORTRAIT
				: MAX_EMOTICONS_PER_PAGE_LANDSCAPE;
		MAX_EMOTICONS_PER_ROW = isPortrait ? MAX_EMOTICONS_PER_ROW_PORTRAIT
				: MAX_EMOTICONS_PER_ROW_LANDSCAPE;

		this.inflater = LayoutInflater.from(activity);
		this.activity = activity;
		this.composeBox = composeBox;

		// We want the value to be -1 to signify Recent Emoticons tab
		this.whichSubCategory = whichSubCategory - 1;
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
		if (this.whichSubCategory != RECENTS_SUBCATEGORY_INDEX) {
			EMOTICON_NUM_PAGES = calculateNumPages(emoticonSubCategories[this.whichSubCategory]);
			setOffset();
		} else {
			int startOffset = idOffset + this.offset;
			int endOffset = startOffset + emoticonResIds.length;
			recentEmoticons = HikeConversationsDatabase.getInstance()
					.fetchEmoticonsOfType(emoticonType, startOffset, endOffset,
							MAX_EMOTICONS_PER_PAGE * 2);
			EMOTICON_NUM_PAGES = recentEmoticons.length == 0 ? 1
					: calculateNumPages(recentEmoticons.length);
		}
	}

	private int calculateNumPages(int numEmoticons) {
		int numPages = ((int) (numEmoticons / MAX_EMOTICONS_PER_PAGE)) + 1;
		// Doing this to prevent an empty page when the numerator is a multiple
		// of the denominator
		if (numEmoticons % MAX_EMOTICONS_PER_PAGE == 0) {
			return numPages - 1;
		}
		return numPages;
	}

	private void setOffset() {
		if (whichSubCategory == 0 || emoticonSubCategories == null) {
			return;
		}
		for (int i = 0; i <= whichSubCategory - 1; i++) {
			offset += emoticonSubCategories[i];
		}
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

		setRecentTextVisibility((TextView) emoticonPage
				.findViewById(R.id.recent_use_txt));

		((ViewPager) container).addView(emoticonPage);
		return emoticonPage;
	}

	private void setRecentTextVisibility(TextView recentUsedTxt) {
		if (whichSubCategory != RECENTS_SUBCATEGORY_INDEX) {
			recentUsedTxt.setVisibility(View.GONE);
			return;
		}
		recentUsedTxt.setVisibility(recentEmoticons.length > 0 ? View.GONE
				: View.VISIBLE);
	}

	private class EmoticonPageAdapter extends BaseAdapter {

		int currentPage;
		LayoutInflater inflater;

		public EmoticonPageAdapter(int currentPage) {
			this.currentPage = currentPage;
			this.inflater = LayoutInflater.from(activity);
		}

		@Override
		public int getCount() {
			if (whichSubCategory == RECENTS_SUBCATEGORY_INDEX) {
				if (currentPage == EMOTICON_NUM_PAGES - 1) {
					return recentEmoticons.length % MAX_EMOTICONS_PER_PAGE;
				}
				return MAX_EMOTICONS_PER_PAGE;
			} else {
				if (currentPage == EMOTICON_NUM_PAGES - 1) {
					return emoticonSubCategories[whichSubCategory]
							% MAX_EMOTICONS_PER_PAGE == 0 ? MAX_EMOTICONS_PER_PAGE
							: emoticonSubCategories[whichSubCategory]
									% MAX_EMOTICONS_PER_PAGE;
				}
				return MAX_EMOTICONS_PER_PAGE;
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
			LayoutParams lp = new LayoutParams(EMOTICON_SIZE, EMOTICON_SIZE);
			convertView.setLayoutParams(lp);
			convertView
					.setTag(Integer
							.valueOf(whichSubCategory != RECENTS_SUBCATEGORY_INDEX ? (offset
									+ ((currentPage) * MAX_EMOTICONS_PER_PAGE)
									+ position + idOffset)
									: (recentEmoticons[((currentPage) * MAX_EMOTICONS_PER_PAGE)
											+ position] + idOffset)));
			((ImageView) convertView)
					.setImageResource(emoticonResIds[whichSubCategory != RECENTS_SUBCATEGORY_INDEX ? offset
							+ ((currentPage) * MAX_EMOTICONS_PER_PAGE)
							+ position
							: recentEmoticons[((currentPage) * MAX_EMOTICONS_PER_PAGE)
									+ position]]);
			return convertView;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		int emoticonIndex = (Integer) arg1.getTag();
		HikeConversationsDatabase.getInstance().updateRecencyOfEmoticon(
				emoticonIndex, System.currentTimeMillis());
		((ChatThread) activity).onEmoticonBtnClicked(null, 0);
		// We don't add an emoticon if the compose box is near its maximum
		// length of characters
		if (composeBox.length() >= activity.getResources().getInteger(
				R.integer.max_length_message) - 20) {
			return;
		}
		SmileyParser.getInstance().addSmiley(composeBox, emoticonIndex);
	}
}
