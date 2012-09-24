package com.bsb.hike.adapters;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class EmoticonAdapter extends PagerAdapter implements OnItemClickListener{

	public enum EmoticonType
	{
		HIKE_EMOTICON,
		EMOJI
	}

	public final int MAX_EMOTICONS_PER_PAGE; 
	public final int MAX_EMOTICONS_PER_ROW;

	public static final int MAX_EMOTICONS_PER_PAGE_PORTRAIT = 21; 
	public static final int MAX_EMOTICONS_PER_ROW_PORTRAIT = 7;

	public static final int MAX_EMOTICONS_PER_PAGE_LANDSCAPE = 20; 
	public static final int MAX_EMOTICONS_PER_ROW_LANDSCAPE = 10;

	private int EMOTICON_NUM_PAGES;

	private LayoutInflater inflater;
	private Context context;
	private EditText composeBox;
	private int offset;
	private int whichSubCategory;
	private int[] recentEmoticons;
	private int[] emoticonResIds;
	private int[] emoticonSubCategories;
	private int idOffset;

	public EmoticonAdapter(Context context, EditText composeBox, EmoticonType emoticonType, int whichSubCategory, boolean isPortrait) 
	{
		MAX_EMOTICONS_PER_PAGE = isPortrait ? MAX_EMOTICONS_PER_PAGE_PORTRAIT : MAX_EMOTICONS_PER_PAGE_LANDSCAPE;
		MAX_EMOTICONS_PER_ROW = isPortrait ? MAX_EMOTICONS_PER_ROW_PORTRAIT : MAX_EMOTICONS_PER_ROW_LANDSCAPE;

		this.inflater = LayoutInflater.from(context);
		this.context = context;
		this.composeBox = composeBox;
		this.whichSubCategory = whichSubCategory;
		switch (emoticonType) 
		{
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
		EMOTICON_NUM_PAGES = ((int)(emoticonSubCategories[whichSubCategory]/MAX_EMOTICONS_PER_PAGE)) + 2;
		// Doing this to prevent an empty page when the numerator is a multiple of the denominator
		if(emoticonSubCategories[whichSubCategory]%MAX_EMOTICONS_PER_PAGE == 0)
		{
			EMOTICON_NUM_PAGES--;
		}

		setOffset(emoticonSubCategories, whichSubCategory);

		int startOffset = idOffset + this.offset;
		int endOffset = startOffset + emoticonSubCategories[whichSubCategory];
		recentEmoticons = HikeConversationsDatabase.getInstance().fetchEmoticonsOfType
				(emoticonType, startOffset, endOffset, isPortrait ? MAX_EMOTICONS_PER_PAGE : MAX_EMOTICONS_PER_PAGE_LANDSCAPE);
	}

	private void setOffset(int[] subCategories, int whichSubCategory)
	{
		if(whichSubCategory == 0 || emoticonSubCategories == null)
		{
			return;
		}
		for(int i = 0; i<=whichSubCategory-1; i++)
		{
			offset += subCategories[i];
		}
	}

	public int getRecentEmoticonsLength()
	{
		return recentEmoticons.length;
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
	public Object instantiateItem(ViewGroup container, int position) 
	{
		View emoticonPage = inflater.inflate(R.layout.emoticon_page, null);

		GridView emoticonGrid = (GridView) emoticonPage.findViewById(R.id.emoticon_grid);
		emoticonGrid.setNumColumns(MAX_EMOTICONS_PER_ROW);
		emoticonGrid.setAdapter(new EmoticonPageAdapter(position));
		emoticonGrid.setOnItemClickListener(this);

		setRecentTextVisibility(position, (TextView)emoticonPage.findViewById(R.id.recent_use_txt));
		
		((ViewPager) container).addView(emoticonPage);
		return emoticonPage;
	}

	private void setRecentTextVisibility(int pageNum, TextView recentUsedTxt)
	{
		if(pageNum > 0)
		{
			recentUsedTxt.setVisibility(View.GONE);
			return;
		}
		int numLines = (int) (recentEmoticons.length*10)/(MAX_EMOTICONS_PER_ROW);
		int maxLines = MAX_EMOTICONS_PER_PAGE/MAX_EMOTICONS_PER_ROW;

		if(numLines <= ((maxLines-2)*10))
		{
			recentUsedTxt.setVisibility(View.VISIBLE);
			recentUsedTxt.setGravity(Gravity.CENTER);
		}
		else if(numLines <= ((maxLines-1)*10))
		{
			recentUsedTxt.setVisibility(View.VISIBLE);
			recentUsedTxt.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
		}
		else
		{
			recentUsedTxt.setVisibility(View.GONE);
		}
	}

	private class EmoticonPageAdapter extends BaseAdapter 
	{

		int currentPage;
		LayoutInflater inflater;

		public EmoticonPageAdapter(int currentPage) 
		{
			this.currentPage = currentPage;
			this.inflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() 
		{
			if(currentPage == EMOTICON_NUM_PAGES - 1)
			{
				return emoticonSubCategories[whichSubCategory] % MAX_EMOTICONS_PER_PAGE == 0 ?
						MAX_EMOTICONS_PER_PAGE : emoticonSubCategories[whichSubCategory] % MAX_EMOTICONS_PER_PAGE;
			}
			else if(currentPage == 0)
			{
				return recentEmoticons.length;
			}
			return MAX_EMOTICONS_PER_PAGE;
		}

		@Override
		public Object getItem(int position) 
		{
			return null;
		}

		@Override
		public long getItemId(int position) 
		{
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) 
		{
			if(convertView == null)
			{
				convertView = inflater.inflate(R.layout.emoticon_item, null);
			}
			convertView.setTag(Integer.valueOf(currentPage > 0 ? 
					(offset + ((currentPage - 1) * MAX_EMOTICONS_PER_PAGE) + position + idOffset) : 
						(recentEmoticons[position] + idOffset)));
			((ImageView) convertView).setImageResource(emoticonResIds[currentPage > 0 ? 
					offset + ((currentPage - 1) * MAX_EMOTICONS_PER_PAGE) + position : 
						recentEmoticons[position]]);
			return convertView;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) 
	{
		int emoticonIndex = (Integer) arg1.getTag();
		HikeConversationsDatabase.getInstance().updateRecencyOfEmoticon(emoticonIndex, System.currentTimeMillis());
		((ChatThread)context).onEmoticonBtnClicked(null, 0);
		// We don't add an emoticon if the compose box is near its maximum length of characters
		if(composeBox.length() >= context.getResources().getInteger(R.integer.max_length_message) - 20)
		{
			return;
		}
		SmileyParser.getInstance().addSmiley(composeBox, emoticonIndex);
	}
}
