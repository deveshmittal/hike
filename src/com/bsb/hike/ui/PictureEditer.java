package com.bsb.hike.ui;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.GalleryItem;
import com.bsb.hike.photos.HikePhotosUtils;
import com.bsb.hike.photos.HikePhotosUtils.MenuType;
import com.bsb.hike.photos.views.CanvasImageView.OnDoodleStateChangeListener;
import com.bsb.hike.photos.views.DoodleEffectItemLinearLayout;
import com.bsb.hike.photos.views.FilterEffectItemLinearLayout;
import com.bsb.hike.photos.views.PhotosEditerFrameLayoutView;
import com.bsb.hike.ui.fragments.PreviewFragment;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentManager;
import com.viewpagerindicator.IconPagerAdapter;
import com.viewpagerindicator.TabPageIndicator;

public class PictureEditer extends HikeAppStateBaseFragmentActivity
{

	PhotosEditerFrameLayoutView editView;

	private int menuIcons[] = { R.drawable.filters, R.drawable.doodle_icon };

	private EditorClickListener clickHandler;

	private ImageView undoButton;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		overridePendingTransition(R.anim.fade_in_animation, R.anim.fade_out_animation);

		super.onCreate(savedInstanceState);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.fragment_picture_editer);

		editView = (PhotosEditerFrameLayoutView) findViewById(R.id.editer);

		clickHandler = new EditorClickListener(this);

		Intent intent = getIntent();
		String filename = intent.getStringExtra(HikeConstants.HikePhotos.FILENAME);
		if (filename == null)
		{
			ArrayList<GalleryItem> itemList = intent.getExtras().getParcelableArrayList(HikeConstants.Extras.GALLERY_SELECTIONS);

			if (itemList != null && !itemList.isEmpty())
			{
				filename = itemList.get(0).getFilePath();
			}
		}

		if (filename == null)
		{
			PictureEditer.this.finish();
			return;
		}
		editView = (PhotosEditerFrameLayoutView) findViewById(R.id.editer);
		editView.loadImageFromFile(filename);
		editView.setOnDoodlingStartListener(clickHandler);
		FragmentPagerAdapter adapter = new PhotoEditViewPagerAdapter(getSupportFragmentManager(), clickHandler);

		ViewPager pager = (ViewPager) findViewById(R.id.pager);
		pager.setAdapter(adapter);

		TabPageIndicator indicator = (TabPageIndicator) findViewById(R.id.indicator);
		indicator.setViewPager(pager);

		undoButton = (ImageView) findViewById(R.id.undo);
		undoButton.setOnClickListener(clickHandler);

		TabPageIndicator tabs = (TabPageIndicator) findViewById(R.id.indicator);
		tabs.setOnPageChangeListener(clickHandler);
		setupActionBar();

	}

	@Override
	protected void onResume()
	{
		overridePendingTransition(R.anim.fade_in_animation, R.anim.fade_out_animation);
		super.onResume();
	}

	@Override
	protected void onPause()
	{
		overridePendingTransition(R.anim.fade_in_animation, R.anim.fade_out_animation);
		super.onPause();
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.photos_action_bar, null);
		actionBarView.findViewById(R.id.back).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});

		actionBarView.findViewById(R.id.done_container).setOnClickListener(clickHandler);

		actionBar.setCustomView(actionBarView);
	}

	public class PhotoEditViewPagerAdapter extends FragmentPagerAdapter implements IconPagerAdapter
	{

		private EditorClickListener mItemClickListener;

		public PhotoEditViewPagerAdapter(FragmentManager fm, EditorClickListener argItemClickListener)
		{
			super(fm);
			mItemClickListener = argItemClickListener;
		}

		@Override
		public Fragment getItem(int position)
		{

			switch (position)
			{
			case HikeConstants.HikePhotos.FILTER_FRAGMENT_ID:
				return new PreviewFragment(MenuType.Effects, mItemClickListener, editView.getScaledImageOriginal());
			case HikeConstants.HikePhotos.DOODLE_FRAGMENT_ID:
				return new PreviewFragment(MenuType.Doodle, mItemClickListener, editView.getScaledImageOriginal());
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

	public class EditorClickListener implements OnClickListener, OnPageChangeListener, OnDoodleStateChangeListener
	{
		private DoodleEffectItemLinearLayout doodlePreview;

		private int doodleWidth;

		private Context mContext;

		private boolean doodleState;

		public EditorClickListener(Context context)
		{
			// TODO Auto-generated constructor stub
			mContext = context;
			doodleWidth = HikeConstants.HikePhotos.DEFAULT_BRUSH_WIDTH;
			doodleState = false;
		}

		public void setDoodlePreview(DoodleEffectItemLinearLayout view)
		{
			doodlePreview = view;
		}

		@Override
		public void onClick(View v)
		{
			// TODO Auto-generated method stub

			if (v.getClass() == FilterEffectItemLinearLayout.class)
			{
				FilterEffectItemLinearLayout prev = HikePhotosUtils.FilterTools.getCurrentFilterItem();
				FilterEffectItemLinearLayout me = (FilterEffectItemLinearLayout) v;
				editView.applyFilter(me.getFilter());
				me.select();
				if (prev != null && prev.getFilter() != me.getFilter())
				{
					prev.unSelect();
				}

			}

			else if (v.getClass() == DoodleEffectItemLinearLayout.class)
			{
				DoodleEffectItemLinearLayout me = (DoodleEffectItemLinearLayout) v;
				editView.setBrushColor(me.getBrushColor());
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
				case R.id.undo:
					editView.undoLastDoodleDraw();
					break;
				case R.id.done_container:
					File savedImage = editView.saveImage();
					Intent forwardIntent = IntentManager.getForwardImageIntent(mContext, savedImage);
					startActivity(forwardIntent);
					break;

				}
			}
		}

		@Override
		public void onPageScrollStateChanged(int arg0)
		{
			// TODO Auto-generated method stub

		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2)
		{
			// TODO Auto-generated method stub

		}

		@Override
		public void onPageSelected(int arg0)
		{

			// TODO Auto-generated method stub
			switch (arg0)
			{
			case HikeConstants.HikePhotos.FILTER_FRAGMENT_ID:
				editView.disableDoodling();
				undoButton.setVisibility(View.GONE);
				break;
			case HikeConstants.HikePhotos.DOODLE_FRAGMENT_ID:
				editView.enableDoodling();
				if (doodleState)
				{
					undoButton.setVisibility(View.VISIBLE);
				}
				break;
			}

		}

		@Override
		public void onDoodleStateChanged(boolean isCanvasEmpty)
		{
			// TODO Auto-generated method stub
			doodleState = !isCanvasEmpty;
			if (isCanvasEmpty)
			{
				undoButton.setVisibility(View.GONE);
			}
			else
			{
				undoButton.setVisibility(View.VISIBLE);
			}
		}

	}

}
