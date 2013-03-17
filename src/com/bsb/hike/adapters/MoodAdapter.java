package com.bsb.hike.adapters;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.utils.DrawerBaseActivity;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.Utils;

public class MoodAdapter extends PagerAdapter implements OnItemClickListener {

	private static final int MOOD_PER_PAGE = 12;

	private int moodCount;
	private int numPages;
	private String[] moodHeadings;
	private LayoutInflater inflater;
	private Context context;

	private final int moodHeight = (int) (65 * Utils.densityMultiplier);
	private final int moodWidth = (int) (70 * Utils.densityMultiplier);

	public MoodAdapter(Context context) {
		this.inflater = LayoutInflater.from(context);
		this.context = context;
		this.moodHeadings = context.getResources().getStringArray(
				R.array.mood_headings);
		this.moodCount = EmoticonConstants.MOOD_RES_IDS.length;
		calculateNumPages();
	}

	private void calculateNumPages() {
		numPages = ((int) (moodCount / MOOD_PER_PAGE)) + 1;
		// Doing this to prevent an empty page when the numerator is a multiple
		// of the denominator
		if (moodCount % MOOD_PER_PAGE == 0) {
			this.numPages = numPages - 1;
		}
	}

	@Override
	public int getCount() {
		return numPages;
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
		View moodPage = inflater.inflate(R.layout.mood_page, null);

		GridView moodGrid = (GridView) moodPage.findViewById(R.id.mood_grid);
		moodGrid.setAdapter(new MoodPageAdapter(position));
		moodGrid.setOnItemClickListener(this);

		((ViewPager) container).addView(moodPage);
		return moodPage;
	}

	private class MoodPageAdapter extends BaseAdapter {

		int currentPage;
		LayoutInflater inflater;

		public MoodPageAdapter(int currentPage) {
			this.currentPage = currentPage;
			this.inflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			if (currentPage == numPages - 1) {
				return moodCount % MOOD_PER_PAGE == 0 ? MOOD_PER_PAGE
						: moodCount % MOOD_PER_PAGE;
			}
			return MOOD_PER_PAGE;
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
				convertView = inflater.inflate(R.layout.mood_item, null);
			}

			LayoutParams lp = new LayoutParams(moodWidth, moodHeight);
			convertView.setLayoutParams(lp);

			convertView.setTag(Integer.valueOf(((currentPage) * MOOD_PER_PAGE)
					+ position));

			ImageView moodImage = (ImageView) convertView
					.findViewById(R.id.mood);
			TextView moodText = (TextView) convertView
					.findViewById(R.id.mood_text);

			moodImage
					.setImageResource(EmoticonConstants.MOOD_RES_IDS[(currentPage)
							* MOOD_PER_PAGE + position]);
			moodText.setText(moodHeadings[(currentPage) * MOOD_PER_PAGE
					+ position]);

			return convertView;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view,
			int position, long id) {
		int moodId = (Integer) view.getTag();
		((DrawerBaseActivity) context).setMood(moodId);
	}
}
