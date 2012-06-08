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
import com.bsb.hike.utils.SmileyParser;

public class EmoticonAdapter extends PagerAdapter implements OnItemClickListener{

	private static final int EMOTICON_COUNT_PER_PAGE = 14;

	private LayoutInflater inflater;
	private Context context;
	private EditText composeBox;

	public EmoticonAdapter(Context context, EditText composeBox) {
		this.inflater = LayoutInflater.from(context);
		this.context = context;
		this.composeBox = composeBox;
	}

	public void setComposeBox(EditText composeBox)
	{
		this.composeBox = composeBox;
	}

	@Override
	public int getCount() {
		return (int) (Math.ceil(SmileyParser.DEFAULT_SMILEY_RES_IDS.length/EMOTICON_COUNT_PER_PAGE)) + 1;
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
		emoticonPage.setAdapter(new EmoticonPageAdapter(position));
		emoticonPage.setOnItemClickListener(this);
		
		((ViewPager) container).addView(emoticonPage);
		return emoticonPage;
	}

	private class EmoticonPageAdapter extends BaseAdapter {

		int currentPage;
		LayoutInflater inflater;

		public EmoticonPageAdapter(int currentPage) {
			this.currentPage = currentPage;
			this.inflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return (SmileyParser.DEFAULT_SMILEY_RES_IDS.length - (currentPage * EMOTICON_COUNT_PER_PAGE)) >= EMOTICON_COUNT_PER_PAGE 
					? EMOTICON_COUNT_PER_PAGE : (SmileyParser.DEFAULT_SMILEY_RES_IDS.length % EMOTICON_COUNT_PER_PAGE);
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
			convertView.setTag(new Integer((currentPage * EMOTICON_COUNT_PER_PAGE) + position));
			((ImageView) convertView).setImageResource(SmileyParser.DEFAULT_SMILEY_RES_IDS[(currentPage * EMOTICON_COUNT_PER_PAGE) + position]);
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
