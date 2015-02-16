package com.bsb.hike.photos;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;

import com.bsb.hike.R;
import com.bsb.hike.photos.PhotoEditerTools.MenuType;
import com.viewpagerindicator.IconPagerAdapter;
import com.viewpagerindicator.TabPageIndicator;

public class PictureEditer extends FragmentActivity {

	PictureEditerView editView;

	private int menuIcons[]={R.drawable.filters,R.drawable.doodle};
	
	private EffectItemAdapter clickHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_picture_editer);

		clickHandler=new EffectItemAdapter(this);

		Intent intent = getIntent();
		String filename = intent.getStringExtra("FilePath");
		editView=(PictureEditerView)findViewById(R.id.editer) ;
		editView.loadImageFromFile(filename);
		FragmentPagerAdapter adapter = new EffectsViewAdapter(getSupportFragmentManager(),clickHandler);

		ViewPager pager = (ViewPager)findViewById(R.id.pager);
		pager.setAdapter(adapter);

		TabPageIndicator indicator = (TabPageIndicator)findViewById(R.id.indicator);
		indicator.setViewPager(pager);

	//	((Button)findViewById(R.id.saveButton)).setOnClickListener(clickHandler);

		TabPageIndicator tabs=(TabPageIndicator)findViewById(R.id.indicator);
		

	}


	class EffectsViewAdapter extends FragmentPagerAdapter implements IconPagerAdapter {

		private EffectItemAdapter mAdapter;

		public EffectsViewAdapter(FragmentManager fm,EffectItemAdapter adapter) {
			super(fm);
			mAdapter=adapter;
		}

		@Override
		public Fragment getItem(int position) {

			switch(position)
			{
			case 0:
				return new PreviewFragment(MenuType.Effects,mAdapter);  
			case 1:
				return new PreviewFragment(MenuType.Doodle,mAdapter); 
			}
			return null;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return "";		}

		@Override public int getIconResId(int index) {
			return menuIcons[index];
		}

		@Override
		public int getCount() {
			return menuIcons.length;
		}
	}

	class EffectItemAdapter implements OnClickListener 
	{
		private DoodleEffectItem doodlePreview;
		private int doodleWidth=30;
		private Context mContext;
		
		
		public  EffectItemAdapter(Context context) {
			// TODO Auto-generated constructor stub
			mContext=context;
		}

		public void setDoodlePreview(DoodleEffectItem view)
		{
			doodlePreview=view;
		}
		
		public void clearDoodleScreen()
		{
			
		}
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub

			if(v.getClass()==FilterEffectItem.class)
			{
				editView.disableDoodling();
				FilterEffectItem me=(FilterEffectItem)v;
				editView.applyFilter(me.getFilter());
			}

			else if(v.getClass()==DoodleEffectItem.class)
			{
				DoodleEffectItem me=(DoodleEffectItem)v;
				editView.setBrushColor(me.getBrushColor());
				editView.enableDoodling();
				doodlePreview.setBrushColor(me.getBrushColor());
				doodlePreview.Refresh();  
				

			}
			else
			{
				switch(v.getId())
				{
				case R.id.plusWidth:
					if(doodleWidth+10<=80)
						doodleWidth+=10 ;
					doodlePreview.setBrushWidth(PhotoEditerTools.dpToPx(mContext, doodleWidth));
					doodlePreview.Refresh();
					editView.setBrushWidth(PhotoEditerTools.dpToPx(mContext, doodleWidth) );
					break;
				case R.id.minusWidth:
					if(doodleWidth-10>=10)
						doodleWidth-=10 ;
					doodlePreview.setBrushWidth(PhotoEditerTools.dpToPx(mContext, doodleWidth));
					doodlePreview.Refresh();
					editView.setBrushWidth(PhotoEditerTools.dpToPx(mContext, doodleWidth) );
					break;
				}
			}
//			else if(v.getId()==R.id.saveButton)
//			{
//				editView.saveImage();
//			}


		}


	}



}
