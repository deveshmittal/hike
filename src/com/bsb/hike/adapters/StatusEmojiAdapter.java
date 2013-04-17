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
import com.bsb.hike.ui.StatusUpdate;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

public class StatusEmojiAdapter extends PagerAdapter implements
		OnItemClickListener {

	public static final int RECENTS_SUBCATEGORY_INDEX = -1;

	public static final int MAX_EMOTICONS_PER_ROW = 7;

	private static final int MAX_RECENT_EMOJI = 40;

	private int EMOTICON_NUM_PAGES;

	private LayoutInflater inflater;
	private Activity activity;
	private EditText composeBox;
	private int[] recentEmoticons;
	private int[] emoticonResIds;
	private int[] emoticonSubCategories;
	private int idOffset;

	private final int EMOTICON_SIZE = (int) (27 * Utils.densityMultiplier);

	public StatusEmojiAdapter(Activity activity, EditText composeBox) {
		this.inflater = LayoutInflater.from(activity);
		this.activity = activity;
		this.composeBox = composeBox;

		emoticonResIds = EmoticonConstants.EMOJI_RES_IDS;
		emoticonSubCategories = SmileyParser.EMOJI_SUBCATEGORIES;
		idOffset = EmoticonConstants.DEFAULT_SMILEY_RES_IDS.length;

		EMOTICON_NUM_PAGES = SmileyParser.EMOJI_SUBCATEGORIES.length + 1;

		int startOffset = idOffset;
		int endOffset = startOffset + emoticonResIds.length;

		recentEmoticons = HikeConversationsDatabase.getInstance()
				.fetchEmoticonsOfType(EmoticonAdapter.EmoticonType.EMOJI,
						startOffset, endOffset, MAX_RECENT_EMOJI);
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
		emoticonGrid.setVerticalScrollBarEnabled(true);
		emoticonGrid.setHorizontalScrollBarEnabled(false);
		emoticonGrid.setAdapter(new EmoticonPageAdapter(position));
		emoticonGrid.setOnItemClickListener(this);

		setRecentTextVisibility(
				(TextView) emoticonPage.findViewById(R.id.recent_use_txt),
				position);

		((ViewPager) container).addView(emoticonPage);
		return emoticonPage;
	}

	private void setRecentTextVisibility(TextView recentUsedTxt, int position) {
		if (position != 0) {
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
			if (currentPage == 0) {
				return Math.min(recentEmoticons.length, MAX_RECENT_EMOJI);
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
			LayoutParams lp = new LayoutParams(EMOTICON_SIZE, EMOTICON_SIZE);
			convertView.setLayoutParams(lp);
			convertView.setTag(Integer
					.valueOf(currentPage != 0 ? (getIndex(currentPage)
							+ position + idOffset)
							: (recentEmoticons[position] + idOffset)));
			((ImageView) convertView)
					.setImageResource(emoticonResIds[currentPage != 0 ? getIndex(currentPage)
							+ position
							: recentEmoticons[position]]);
			return convertView;
		}

		private int getIndex(int currentPage) {
			currentPage--;
			if (currentPage == 0 || currentPage == -1) {
				return 0;
			}
			int offset = 0;
			for (int i = 0; i <= currentPage - 1; i++) {
				offset += emoticonSubCategories[i];
			}
			return offset;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		int emoticonIndex = (Integer) arg1.getTag();

		HikeConversationsDatabase.getInstance().updateRecencyOfEmoticon(
				emoticonIndex, System.currentTimeMillis());

		((StatusUpdate) activity).hideEmoticonSelector();

		// We don't add an emoticon if the compose box is near its maximum
		// length of characters
		if (composeBox.length() >= activity.getResources().getInteger(
				R.integer.max_length_message) - 20) {
			return;
		}
		SmileyParser.getInstance().addSmiley(composeBox, emoticonIndex);
	}
}
