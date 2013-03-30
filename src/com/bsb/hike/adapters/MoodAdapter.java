package com.bsb.hike.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.ui.StatusUpdate;
import com.bsb.hike.utils.Utils;

public class MoodAdapter extends BaseAdapter implements OnItemClickListener {

	private int moodCount;
	private String[] moodHeadings;
	private LayoutInflater inflater;
	private Context context;

	private final int moodHeight;
	private final int moodWidth;

	private int[] moods;

	public MoodAdapter(Context context) {
		this.inflater = LayoutInflater.from(context);
		this.context = context;
		this.moodHeadings = context.getResources().getStringArray(
				R.array.mood_headings);
		this.inflater = LayoutInflater.from(context);

		int width = context.getResources().getDisplayMetrics().widthPixels;
		moodWidth = (int) (width / 3);
		moodHeight = moodWidth;

		this.moods = Utils.getMoodsResource();
		this.moodCount = moods.length;
	}

	@Override
	public int getCount() {
		return moodCount;
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

		convertView.setTag(Integer.valueOf(position));

		ImageView moodImage = (ImageView) convertView.findViewById(R.id.mood);
		TextView moodText = (TextView) convertView.findViewById(R.id.mood_text);

		moodImage.setImageResource(moods[position]);
		moodText.setText(moodHeadings[position]);

		return convertView;
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view,
			int position, long id) {
		int moodId = (Integer) view.getTag();
		((StatusUpdate) context).setMood(moodId);
	}
}
