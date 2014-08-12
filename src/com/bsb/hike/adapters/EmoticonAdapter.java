package com.bsb.hike.adapters;

import android.app.Activity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.GridView;

import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.view.StickerEmoticonIconPageIndicator.StickerEmoticonIconPagerAdapter;

public class EmoticonAdapter extends PagerAdapter implements StickerEmoticonIconPagerAdapter
{

	public final int MAX_EMOTICONS_PER_ROW;

	public static final int MAX_EMOTICONS_PER_ROW_PORTRAIT = 7;

	public static final int MAX_EMOTICONS_PER_ROW_LANDSCAPE = 10;

	public static final int RECENTS_SUBCATEGORY_INDEX = -1;

	private LayoutInflater inflater;

	private Activity activity;

	private EditText composeBox;

	private int[] emoticonResIds;

	private int[] emoticonSubCategories;

	private int[] categoryResIds;

	private int idOffset;

	public EmoticonAdapter(Activity activity, EditText composeBox, boolean isPortrait, int[] categoryResIds)
	{
		this(activity, composeBox, isPortrait, categoryResIds, false);
	}

	public EmoticonAdapter(Activity activity, EditText composeBox, boolean isPortrait, int[] categoryResIds, boolean emojiOnly)
	{
		MAX_EMOTICONS_PER_ROW = isPortrait ? MAX_EMOTICONS_PER_ROW_PORTRAIT : MAX_EMOTICONS_PER_ROW_LANDSCAPE;

		this.inflater = LayoutInflater.from(activity);
		this.activity = activity;
		this.composeBox = composeBox;
		this.categoryResIds = categoryResIds;

		emoticonResIds = emojiOnly ? EmoticonConstants.EMOJI_RES_IDS : EmoticonConstants.DEFAULT_SMILEY_RES_IDS;
		emoticonSubCategories = emojiOnly ? SmileyParser.EMOJI_SUBCATEGORIES : SmileyParser.EMOTICONS_SUBCATEGORIES;

		if (emojiOnly)
		{
			for (int i : SmileyParser.HIKE_SUBCATEGORIES)
			{
				idOffset += i;
			}
		}
	}

	@Override
	public int getCount()
	{
		return emoticonSubCategories.length + 1;
	}

	@Override
	public boolean isViewFromObject(View view, Object object)
	{
		return view == object;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object)
	{
		((ViewPager) container).removeView((View) object);
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position)
	{
		View emoticonPage = inflater.inflate(R.layout.emoticon_page, null);

		GridView emoticonGrid = (GridView) emoticonPage.findViewById(R.id.emoticon_grid);
		emoticonGrid.setNumColumns(MAX_EMOTICONS_PER_ROW);
		emoticonGrid.setVerticalScrollBarEnabled(false);
		emoticonGrid.setHorizontalScrollBarEnabled(false);
		emoticonGrid.setAdapter(new EmoticonPageAdapter(activity, emoticonSubCategories, emoticonResIds, position, idOffset, composeBox));

		((ViewPager) container).addView(emoticonPage);
		return emoticonPage;
	}

	@Override
	public int getIconResId(int index)
	{
		return categoryResIds[index];
	}

	@Override
	public boolean isUpdateAvailable(int index)
	{
		return false;
	}
}
