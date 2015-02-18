package com.bsb.hike.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.photos.HikePhotosUtils;
import com.bsb.hike.photos.HikePhotosUtils.MenuType;
import com.bsb.hike.photos.views.DoodleEffectItemLinearLayout;
import com.bsb.hike.photos.views.FilterEffectItemLinearLayout;
import com.bsb.hike.photos.views.PhotosEditerFrameLayoutView;
import com.bsb.hike.ui.fragments.PreviewFragment;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.viewpagerindicator.IconPagerAdapter;
import com.viewpagerindicator.TabPageIndicator;

public class PictureEditer extends HikeAppStateBaseFragmentActivity
{

	PhotosEditerFrameLayoutView editView;

	private int menuIcons[] = { R.drawable.filters, R.drawable.doodle };

	private EditorClickListener clickHandler;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// PhotoEditerTools.hideSystemUI(getWindow().getDecorView());

		setContentView(R.layout.fragment_picture_editer);

		clickHandler = new EditorClickListener(this);

		Intent intent = getIntent();
		String filename = intent.getStringExtra(HikeConstants.HikePhotos.FILENAME);
		editView = (PhotosEditerFrameLayoutView) findViewById(R.id.editer);
		editView.loadImageFromFile(filename);
		FragmentPagerAdapter adapter = new EffectsViewAdapter(getSupportFragmentManager(), clickHandler);

		ViewPager pager = (ViewPager) findViewById(R.id.pager);
		pager.setAdapter(adapter);

		TabPageIndicator indicator = (TabPageIndicator) findViewById(R.id.indicator);
		indicator.setViewPager(pager);

		// ((Button)findViewById(R.id.saveButton)).setOnClickListener(clickHandler);

		findViewById(R.id.back).setOnClickListener(clickHandler);

		TabPageIndicator tabs = (TabPageIndicator) findViewById(R.id.indicator);

		// getSupportActionBar().hide();

	}

	public class EffectsViewAdapter extends FragmentPagerAdapter implements IconPagerAdapter
	{

		private EditorClickListener mAdapter;

		public EffectsViewAdapter(FragmentManager fm, EditorClickListener adapter)
		{
			super(fm);
			mAdapter = adapter;
		}

		@Override
		public Fragment getItem(int position)
		{

			switch (position)
			{
			case HikeConstants.HikePhotos.FILTER_FRAGMENT_ID:
				return new PreviewFragment(MenuType.Effects, mAdapter, editView.getScaledImageOriginal());
			case HikeConstants.HikePhotos.DOODLE_FRAGMENT_ID:
				return new PreviewFragment(MenuType.Doodle, mAdapter, editView.getScaledImageOriginal());
			}
			return null;
		}

		@Override
		public CharSequence getPageTitle(int position)
		{
			return HikeConstants.HikePhotos.EMPTY_TAB_TITLE;
		}

		@Override
		public int getIconResId(int index)
		{
			return menuIcons[index];
		}

		@Override
		public int getCount()
		{
			return menuIcons.length;
		}
	}

	public class EditorClickListener implements OnClickListener
	{
		private DoodleEffectItemLinearLayout doodlePreview;

		private int doodleWidth;

		private Context mContext;

		public EditorClickListener(Context context)
		{
			// TODO Auto-generated constructor stub
			mContext = context;
			doodleWidth = HikeConstants.HikePhotos.DEFAULT_BRUSH_WIDTH;
		}

		public void setDoodlePreview(DoodleEffectItemLinearLayout view)
		{
			doodlePreview = view;
		}

		public void clearDoodleScreen()
		{

		}

		@Override
		public void onClick(View v)
		{
			// TODO Auto-generated method stub

			if (v.getClass() == FilterEffectItemLinearLayout.class)
			{
				editView.disableDoodling();
				FilterEffectItemLinearLayout me = (FilterEffectItemLinearLayout) v;
				editView.applyFilter(me.getFilter());
			}

			else if (v.getClass() == DoodleEffectItemLinearLayout.class)
			{
				DoodleEffectItemLinearLayout me = (DoodleEffectItemLinearLayout) v;
				editView.setBrushColor(me.getBrushColor());
				editView.enableDoodling();
				doodlePreview.setBrushColor(me.getBrushColor());
				doodlePreview.refresh();

			}
			else
			{
				switch (v.getId())
				{
				case R.id.plusWidth:
					if (doodleWidth + HikeConstants.HikePhotos.DELTA_BRUSH_WIDTH <= HikeConstants.HikePhotos.MAX_BRUSH_WIDTH)
						doodleWidth += HikeConstants.HikePhotos.DELTA_BRUSH_WIDTH;
					doodlePreview.setBrushWidth(HikePhotosUtils.dpToPx(mContext, doodleWidth));
					doodlePreview.refresh();
					editView.setBrushWidth(HikePhotosUtils.dpToPx(mContext, doodleWidth));
					break;
				case R.id.minusWidth:
					if (doodleWidth - HikeConstants.HikePhotos.DELTA_BRUSH_WIDTH >= HikeConstants.HikePhotos.Min_BRUSH_WIDTH)
						doodleWidth -= HikeConstants.HikePhotos.DELTA_BRUSH_WIDTH;
					doodlePreview.setBrushWidth(HikePhotosUtils.dpToPx(mContext, doodleWidth));
					doodlePreview.refresh();
					editView.setBrushWidth(HikePhotosUtils.dpToPx(mContext, doodleWidth));
					break;
				case R.id.back:
					editView.undoLastDoodleDraw();
					break;
				}
			}

		}

	}

}
