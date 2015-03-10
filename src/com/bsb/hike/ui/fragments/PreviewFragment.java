package com.bsb.hike.ui.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.photos.HikePhotosUtils;
import com.bsb.hike.photos.HikePhotosUtils.FilterTools.FilterList;
import com.bsb.hike.photos.HikePhotosUtils.MenuType;
import com.bsb.hike.photos.views.DoodleEffectItemLinearLayout;
import com.bsb.hike.photos.views.FilterEffectItemLinearLayout;
import com.bsb.hike.ui.PictureEditer.EditorClickListener;
import com.jess.ui.TwoWayAbsListView;
import com.jess.ui.TwoWayGridView;

public final class PreviewFragment extends Fragment
{

	private MenuType myType;

	private EditorClickListener handler;

	private Bitmap mOriginalBitmap;

	private ImageAdapter mAdapter;

	public PreviewFragment(MenuType type, EditorClickListener adapter, Bitmap bitmap)
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

		LinearLayout layout = (LinearLayout) LayoutInflater.from(getActivity()).inflate(R.layout.photos_pager_layout, container, false);

		
		int height = container.getMeasuredHeight();
		
		TwoWayGridView gridView = (TwoWayGridView) layout.findViewById(R.id.HorizontalGridView);
		//gridView.setLayoutParams(new TwoWayAbsListView.LayoutParams(TwoWayAbsListView.LayoutParams.MATCH_PARENT, HikePhotosUtils.dpToPx(getActivity().getApplicationContext(), height)));
		gridView.setColumnWidth(GridView.AUTO_FIT);
		gridView.setRowHeight(height);
		mAdapter = new ImageAdapter(getActivity(), myType, handler);
		gridView.setAdapter(mAdapter);
		ViewStub adjuster = (ViewStub) layout.findViewById(R.id.sizeBarStub);
		switch (myType)
		{
		case Doodle:
			layout.setWeightSum(HikeConstants.HikePhotos.PHOTOS_PAGER_DOODLE_WEIGHT_SUM);
			RelativeLayout sizeBar = (RelativeLayout) adjuster.inflate();
			sizeBar.findViewById(R.id.plusWidth).setOnClickListener(handler);
			sizeBar.findViewById(R.id.minusWidth).setOnClickListener(handler);
			ViewStub stub = (ViewStub) sizeBar.findViewById(R.id.viewStubPreview);
			DoodleEffectItemLinearLayout inflated = (DoodleEffectItemLinearLayout) stub.inflate();
			inflated.setRingColor(HikeConstants.HikePhotos.DOODLE_SELECTED_RING_COLOR);
			inflated.setBrushColor(HikePhotosUtils.DoodleColors[0]);
			inflated.setBrushWidth(HikePhotosUtils.dpToPx(getActivity().getApplicationContext(), HikeConstants.HikePhotos.DEFAULT_BRUSH_WIDTH));
			inflated.refresh();
			inflated.setPadding(0, 0, 0, 0);
			inflated.invalidate();
			handler.setDoodlePreview(inflated);
			break;
		case Effects:
			layout.setWeightSum(HikeConstants.HikePhotos.PHOTOS_PAGER_FILTER_WEIGHT_SUM);
			adjuster.setVisibility(View.GONE);
			break;
		case Border:
			break;
		case Quality:
			break;
		case Text:
			break;
		default:
			break;
		}
		layout.invalidate();
		HikePhotosUtils.FilterTools.setSelectedColor(HikePhotosUtils.DoodleColors[0]);
		HikePhotosUtils.FilterTools.setSelectedFilter(FilterList.getHikeEffects().filters.get(0));
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

		private EditorClickListener clickListener;

		public ImageAdapter(Context context, MenuType type, EditorClickListener Adapter)
		{
			mContext = context;
			clickListener = Adapter;
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
				count = HikePhotosUtils.DoodleColors.length;
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
		@Override
		public int getItemViewType(int position)
		{
			switch (myType)
			{
			case Effects:
				return 0;
			case Doodle:
				return 1;
			}
			return 0;
		}

		// Convert DP to PX
		// Source: http://stackoverflow.com/a/8490361

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			switch (myType)
			{
			case Effects:
				if (convertView == null)
				{
					convertView = LayoutInflater.from(mContext).inflate(R.layout.filter_preview_item, parent, false);
				}
				FilterList myFilters = FilterList.getHikeEffects();
				String filterName = myFilters.names.get(position);
				Object tagFilterName = convertView.getTag();
				if (tagFilterName != null && ((String) tagFilterName).equals(filterName))
				{
					return convertView;
				}
				else
				{
					if (HikePhotosUtils.FilterTools.getCurrentFilterItem() != null)
					{
						String existingTag = ((String) HikePhotosUtils.FilterTools.getCurrentFilterItem().getTag());

						if (existingTag != null && existingTag.equals(filterName))
						{
							return HikePhotosUtils.FilterTools.getCurrentFilterItem();
						}
					}
					FilterEffectItemLinearLayout filterPreviewView = (FilterEffectItemLinearLayout) convertView;
					filterPreviewView.init(mOriginalBitmap, myFilters.names.get(position));
					filterPreviewView.setFilter(mContext, myFilters.filters.get(position), myFilters.names.get(position));
					filterPreviewView.setOnClickListener(clickListener);
					convertView.setTag(filterName);
				}

				return convertView;
			case Border:
				break;
			case Text:
				break;
			case Doodle:
				if (convertView == null)
				{
					convertView = LayoutInflater.from(mContext).inflate(R.layout.doodle_preview_item, parent, false);
				}
				int currentPosColor = HikePhotosUtils.DoodleColors[position];
				Object tagColor = convertView.getTag();
				if (tagColor != null && ((Integer) tagColor) == currentPosColor)
				{
					return convertView;
				}
				else
				{
					if (HikePhotosUtils.FilterTools.getCurrentDoodleItem() != null)
					{
						Integer existingColor = ((Integer) HikePhotosUtils.FilterTools.getCurrentDoodleItem().getTag());

						if (existingColor != null && existingColor == currentPosColor)
						{
							return HikePhotosUtils.FilterTools.getCurrentDoodleItem();
						}
					}
					
					DoodleEffectItemLinearLayout doodleItem = (DoodleEffectItemLinearLayout) convertView;
					doodleItem.setBrushColor(currentPosColor);
					doodleItem.refresh();
					doodleItem.setOnClickListener(clickListener);
					convertView.setTag(currentPosColor);
				}
				break;
			case Quality:
				break;
			}

			return convertView;

		}
	}
}
