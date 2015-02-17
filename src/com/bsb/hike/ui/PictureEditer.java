package com.bsb.hike.ui;

import java.io.File;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.photos.PhotoEditerTools;
import com.bsb.hike.photos.PhotoEditerTools.MenuType;
import com.bsb.hike.photos.view.DoodleEffectItem;
import com.bsb.hike.photos.view.FilterEffectItem;
import com.bsb.hike.photos.view.PictureEditerView;
import com.bsb.hike.ui.fragments.PreviewFragment;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.Utils;
import com.viewpagerindicator.IconPagerAdapter;
import com.viewpagerindicator.TabPageIndicator;

public class PictureEditer extends HikeAppStateBaseFragmentActivity
{

	PictureEditerView editView;

	private int menuIcons[] = { R.drawable.filters, R.drawable.doodle };

	private EffectsClickListener effectsClickListener;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.fragment_picture_editer);

		effectsClickListener = new EffectsClickListener(this);

		Intent intent = getIntent();
		String filename = intent.getStringExtra("FilePath");
		editView = (PictureEditerView) findViewById(R.id.editer);
		editView.loadImageFromFile(filename);
		FragmentPagerAdapter adapter = new PhotoEditViewPagerAdapter(getSupportFragmentManager(), effectsClickListener);

		ViewPager pager = (ViewPager) findViewById(R.id.pager);
		pager.setAdapter(adapter);

		TabPageIndicator indicator = (TabPageIndicator) findViewById(R.id.indicator);
		indicator.setViewPager(pager);

		findViewById(R.id.done_btn).setOnClickListener(effectsClickListener);

		TabPageIndicator tabs = (TabPageIndicator) findViewById(R.id.indicator);

		getSupportActionBar().hide();

	}

	public class PhotoEditViewPagerAdapter extends FragmentPagerAdapter implements IconPagerAdapter
	{

		private EffectsClickListener mItemClickListener;

		public PhotoEditViewPagerAdapter(FragmentManager fm, EffectsClickListener argItemClickListener)
		{
			super(fm);
			mItemClickListener = argItemClickListener;
		}

		@Override
		public Fragment getItem(int position)
		{

			switch (position)
			{
			case 0:
				return new PreviewFragment(MenuType.Effects, mItemClickListener, editView.getImageOriginal());
			case 1:
				return new PreviewFragment(MenuType.Doodle, mItemClickListener, editView.getImageOriginal());
			}
			return null;
		}

		@Override
		public CharSequence getPageTitle(int position)
		{
			return "";
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

	public class EffectsClickListener implements OnClickListener
	{
		private DoodleEffectItem doodlePreview;

		private int doodleWidth = 30;

		private Context mContext;

		public EffectsClickListener(Context context)
		{
			// TODO Auto-generated constructor stub
			mContext = context;
		}

		public void setDoodlePreview(DoodleEffectItem view)
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

			if (v.getClass() == FilterEffectItem.class)
			{
				editView.disableDoodling();
				FilterEffectItem me = (FilterEffectItem) v;
				editView.applyFilter(me.getFilter());
			}

			else if (v.getClass() == DoodleEffectItem.class)
			{
				DoodleEffectItem me = (DoodleEffectItem) v;
				editView.setBrushColor(me.getBrushColor());
				editView.enableDoodling();
				doodlePreview.setBrushColor(me.getBrushColor());
				doodlePreview.Refresh();

			}
			else
			{
				switch (v.getId())
				{
				case R.id.plusWidth:
					if (doodleWidth + 10 <= 80)
						doodleWidth += 10;
					doodlePreview.setBrushWidth(PhotoEditerTools.dpToPx(mContext, doodleWidth));
					doodlePreview.Refresh();
					editView.setBrushWidth(PhotoEditerTools.dpToPx(mContext, doodleWidth));
					break;
				case R.id.minusWidth:
					if (doodleWidth - 10 >= 10)
						doodleWidth -= 10;
					doodlePreview.setBrushWidth(PhotoEditerTools.dpToPx(mContext, doodleWidth));
					doodlePreview.Refresh();
					editView.setBrushWidth(PhotoEditerTools.dpToPx(mContext, doodleWidth));
					break;
				case R.id.done_btn:
					File savedImage = editView.saveImage();
					Intent forwardIntent = IntentManager.getForwardImageIntent(mContext, savedImage);
					startActivity(forwardIntent);
					break;
				}
			}
		}

	}

}
