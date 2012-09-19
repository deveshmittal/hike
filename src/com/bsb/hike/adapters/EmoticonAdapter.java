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
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.SmileyParser;

public class EmoticonAdapter extends PagerAdapter implements OnItemClickListener{

	public enum EmoticonType
	{
		HIKE_EMOTICON,
		EMOJI
	}

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
		case HIKE_EMOTICON:
			EMOTICON_TAB_NUMBER = SmileyParser.HIKE_EMOTICONS_SUBCATEGORIES.length;
			break;
		case EMOJI:
			EMOTICON_TAB_NUMBER = SmileyParser.EMOJI_SUBCATEGORIES.length;
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
		int idOffset;

		public EmoticonPageAdapter(int currentPage, EmoticonType emoticonType) {
			this.currentPage = currentPage;
			this.inflater = LayoutInflater.from(context);
			switch (emoticonType) 
			{
			case HIKE_EMOTICON:
				emoticonSubCategories = SmileyParser.HIKE_EMOTICONS_SUBCATEGORIES;
				emoticonResIds = EmoticonConstants.DEFAULT_SMILEY_RES_IDS;
				idOffset = 0;
				break;
			case EMOJI:
				emoticonSubCategories = SmileyParser.EMOJI_SUBCATEGORIES;
				emoticonResIds = EmoticonConstants.EMOJI_RES_IDS;
				idOffset = EmoticonConstants.DEFAULT_SMILEY_RES_IDS.length;
				break;
			default:
				emoticonSubCategories = null;
				emoticonResIds = null;
			}
			for(int i=currentPage-1; i>=0; i--)
			{
				startIndex += emoticonSubCategories[i];
			}
		}

		@Override
		public int getCount() {
			return emoticonSubCategories[currentPage];
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
			convertView.setTag(new Integer(startIndex + idOffset + position));
			((ImageView) convertView).setImageResource(emoticonResIds[startIndex + position]);
			return convertView;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) 
	{
		SmileyParser.getInstance().addSmiley(composeBox, (Integer) arg1.getTag());
		((ChatThread)context).onEmoticonBtnClicked(null);
	}
}
