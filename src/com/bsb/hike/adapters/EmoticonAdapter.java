package com.bsb.hike.adapters;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;

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

	public static final int MAX_RECENT_EMOTICONS_TO_SHOW = 21; 

	private final int EMOTICON_TAB_NUMBER;

	private LayoutInflater inflater;
	private Context context;
	private EditText composeBox;
	private EmoticonType emoticonType;

	public EmoticonAdapter(Context context, EditText composeBox, EmoticonType emoticonType) {
		this.inflater = LayoutInflater.from(context);
		this.context = context;
		this.composeBox = composeBox;
		this.emoticonType = emoticonType;
		switch (emoticonType) 
		{
		// Incrementing these numbers to show a recents tab as well.
		case HIKE_EMOTICON:
			EMOTICON_TAB_NUMBER = SmileyParser.HIKE_EMOTICONS_SUBCATEGORIES.length + 1;
			break;
		case EMOJI:
			EMOTICON_TAB_NUMBER = SmileyParser.EMOJI_SUBCATEGORIES.length + 1;
			break;
		default:
			EMOTICON_TAB_NUMBER = 0;
		}
	}

	public void setComposeBox(EditText composeBox)
	{
		this.composeBox = composeBox;
	}

	@Override
	public int getCount() {
		return EMOTICON_TAB_NUMBER;
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
		GridView emoticonPage = (GridView) inflater.inflate(R.layout.emoticon_page, null);
		emoticonPage.setAdapter(new EmoticonPageAdapter(position, emoticonType));
		emoticonPage.setOnItemClickListener(this);
		
		((ViewPager) container).addView(emoticonPage);
		return emoticonPage;
	}

	private class EmoticonPageAdapter extends BaseAdapter {

		int currentPage;
		LayoutInflater inflater;
		int startIndex;
		final int[] emoticonSubCategories;
		final int[] emoticonResIds;
		int[] recentEmoticons;
		int idOffset;

		public EmoticonPageAdapter(int currentPage, EmoticonType emoticonType) {
			this.currentPage = currentPage;
			this.inflater = LayoutInflater.from(context);
			switch (emoticonType) 
			{
			case HIKE_EMOTICON:
				emoticonSubCategories = currentPage != 0 ? SmileyParser.HIKE_EMOTICONS_SUBCATEGORIES : null;
				emoticonResIds = EmoticonConstants.DEFAULT_SMILEY_RES_IDS;
				idOffset = 0;
				break;
			case EMOJI:
				emoticonSubCategories = currentPage != 0 ? SmileyParser.EMOJI_SUBCATEGORIES : null;
				emoticonResIds = EmoticonConstants.EMOJI_RES_IDS;
				idOffset = EmoticonConstants.DEFAULT_SMILEY_RES_IDS.length;
				break;
			default:
				emoticonSubCategories = null;
				emoticonResIds = null;
			}
			if(currentPage != 0)
			{
				for(int i=currentPage-2; i>=0; i--)
				{
					startIndex += emoticonSubCategories[i];
				}
			}
			else
			{
				recentEmoticons = HikeConversationsDatabase.getInstance().fetchEmoticonsOfType(emoticonType);
			}
		}

		@Override
		public int getCount() {
			return currentPage != 0 ? 
					emoticonSubCategories[currentPage - 1] : 
						(recentEmoticons.length > MAX_RECENT_EMOTICONS_TO_SHOW ? 
								MAX_RECENT_EMOTICONS_TO_SHOW : recentEmoticons.length);
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
			if(convertView == null)
			{
				convertView = inflater.inflate(R.layout.emoticon_item, null);
			}
			convertView.setTag(new Integer(currentPage != 0 ? startIndex + idOffset + position : recentEmoticons[position]));
			((ImageView) convertView).setImageResource(emoticonResIds[currentPage != 0 ? startIndex + position : recentEmoticons[position]]);
			return convertView;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) 
	{
		int emoticonIndex = (Integer) arg1.getTag();
		SmileyParser.getInstance().addSmiley(composeBox, emoticonIndex);
		HikeConversationsDatabase.getInstance().updateRecencyOfEmoticon(emoticonIndex, System.currentTimeMillis());
		((ChatThread)context).onEmoticonBtnClicked(null);
	}
}
