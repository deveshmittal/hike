package com.bsb.hike.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.bsb.hike.R;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.smartImageLoader.SharedFileImageLoader;
import com.bsb.hike.utils.Logger;

public class SharedMediaAdapter extends PagerAdapter
{
	private LayoutInflater layoutInflater;

	private SharedFileImageLoader sharedMediaLoader;

	private ArrayList<HikeSharedFile> sharedMediaItems;

	private Context context;

	private String TAG = "SharedMediaAdapter";
	
	private String IMAGE_TAG = "image";
	
	public SharedMediaAdapter(Context context, int size_image, ArrayList<HikeSharedFile> sharedMediaItems, String msisdn, ViewPager viewPager)
	{
		this.context = context;
		this.layoutInflater = LayoutInflater.from(this.context);
		this.sharedMediaLoader = new SharedFileImageLoader(context, size_image);
		this.sharedMediaItems = sharedMediaItems;
	}

	@Override
	public int getCount()
	{
		// TODO Auto-generated method stub
		return sharedMediaItems.size();
	}

	@Override
	public boolean isViewFromObject(View view, Object object)
	{
		// TODO Auto-generated method stub
		return view == object;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object)
	{
		
		((ViewPager) container).removeView((View) object);
	}

	@Override
	public int getItemPosition(Object object)
	{
		return POSITION_NONE;
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position)
	{
		View page = layoutInflater.inflate(R.layout.gallery_layout_item, container, false);
		final HikeSharedFile sharedMediaItem = sharedMediaItems.get(position);

		ImageView galleryImageView = (ImageView) page.findViewById(R.id.album_image);

		if (!sharedMediaItem.getFileTypeString().toString().contains(IMAGE_TAG))
		{
			Button playBtn = (Button) page.findViewById(R.id.play_media);
			playBtn.setVisibility(View.VISIBLE);
			playBtn.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					// TODO Auto-generated method stub
					Logger.d(TAG, "Opening video/other files");
					Intent openFile = new Intent(Intent.ACTION_VIEW);
					openFile.setDataAndType(Uri.fromFile(sharedMediaItem.getFile()), sharedMediaItem.getFileTypeString());
					context.startActivity(openFile);
				}
			});
		}

		galleryImageView.setScaleType(ScaleType.FIT_CENTER);
		sharedMediaLoader.loadImage(sharedMediaItem.getImageLoaderKey(), galleryImageView, false);
		
		((ViewPager) container).addView(page);
		return page;
	}

}