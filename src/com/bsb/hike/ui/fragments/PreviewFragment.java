package com.bsb.hike.ui.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;

import com.bsb.hike.R;
import com.bsb.hike.photos.PhotoEditerTools;
import com.bsb.hike.photos.FilterTools.FilterList;
import com.bsb.hike.photos.PhotoEditerTools.MenuType;
import com.bsb.hike.photos.view.DoodleEffectItem;
import com.bsb.hike.photos.view.FilterEffectItem;
import com.bsb.hike.ui.PictureEditer.EffectItemAdapter;
import com.jess.ui.TwoWayAbsListView;
import com.jess.ui.TwoWayGridView;

public final class PreviewFragment extends Fragment
{

	private MenuType myType;

	private EffectItemAdapter handler;

	private Bitmap mOriginalBitmap;

	public PreviewFragment(MenuType type, EffectItemAdapter adapter, Bitmap bitmap)
	{

		myType = type;
		handler = adapter;
		mOriginalBitmap = bitmap;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{

		LinearLayout layout = new LinearLayout(getActivity());
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
		layout.setGravity(Gravity.CENTER);

		TwoWayGridView gridView = new TwoWayGridView(getActivity());
		gridView.setLayoutParams(new TwoWayAbsListView.LayoutParams(TwoWayAbsListView.LayoutParams.MATCH_PARENT, TwoWayAbsListView.LayoutParams.WRAP_CONTENT));
		gridView.setGravity(Gravity.CENTER);
		gridView.setColumnWidth(GridView.AUTO_FIT);
		gridView.setRowHeight(GridView.AUTO_FIT);
		gridView.setStretchMode(0);
		gridView.setAdapter(new ImageAdapter(getActivity(), myType, handler));

		switch (myType)
		{
		case Doodle:
			LinearLayout adjuster = (LinearLayout) LayoutInflater.from(getActivity()).inflate(R.layout.doodle_brush_size, container, false);
			adjuster.findViewById(R.id.plusWidth).setOnClickListener(handler);
			adjuster.findViewById(R.id.minusWidth).setOnClickListener(handler);
			ViewStub stub = (ViewStub) adjuster.findViewById(R.id.viewStub1);
			DoodleEffectItem inflated = (DoodleEffectItem) stub.inflate();
			inflated.setRingColor(0xFFFFD700);
			handler.setDoodlePreview(inflated);
			layout.addView(adjuster);
			break;
		case Effects:
			gridView.setPadding(0, PhotoEditerTools.dpToPx(getActivity(), 15), 0, 0);
			break;
		}

		layout.addView(gridView);
		return layout;
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
	}

	class ImageAdapter extends BaseAdapter
	{

		private Context mContext;

		private MenuType itemType;

		private EffectItemAdapter adapter;

		public ImageAdapter(Context context, MenuType type, EffectItemAdapter Adapter)
		{
			mContext = context;
			itemType = type;
			adapter = Adapter;
		}

		@Override
		public int getCount()
		{
			int count = 0;
			switch (myType)
			{
			case Effects:
				count = FilterList.getHikeEffects().filters.size();
				break;
			case Doodle:
				count = PhotoEditerTools.DoodleColors.length;
				break;
			}
			return count;
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

		// Convert DP to PX
		// Source: http://stackoverflow.com/a/8490361

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{

			// Want the width/height of the items
			// to be 120dp

			switch (myType)
			{
			case Effects:
				if (convertView == null)
				{
					convertView = LayoutInflater.from(mContext).inflate(R.layout.filter_preview_item, parent, false);
				}
				FilterList myFilters = FilterList.getHikeEffects();
				FilterEffectItem temp = (FilterEffectItem) convertView;
				temp.init(mOriginalBitmap, myFilters.names.get(position));
				temp.setFilter(mContext, myFilters.filters.get(position));
				temp.setOnClickListener(adapter);
				return temp;
			case Border:
				break;
			case Text:
				break;
			case Doodle:
				if (convertView == null)
				{
					convertView = LayoutInflater.from(mContext).inflate(R.layout.doodle_preview_item, parent, false);
				}
				DoodleEffectItem temp3 = (DoodleEffectItem) convertView;
				temp3.setBrushColor(PhotoEditerTools.DoodleColors[position]);
				temp3.Refresh();
				temp3.setOnClickListener(adapter);
				break;
			case Quality:

				break;
			}

			return convertView;

		}
	}
}
