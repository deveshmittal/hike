package com.bsb.hike.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView.ScaleType;

import com.bsb.hike.R;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.smartImageLoader.SharedFileImageLoader;
import com.bsb.hike.ui.fragments.PhotoViewerFragment;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.view.TouchImageView;

public class SharedMediaAdapter extends PagerAdapter implements OnClickListener
{
	private LayoutInflater layoutInflater;

	private SharedFileImageLoader sharedMediaLoader;

	private ArrayList<HikeSharedFile> sharedMediaItems;

	private Context context;

	private String TAG = "SharedMediaAdapter";
	
	private String IMAGE_TAG = "image";
	
	private PhotoViewerFragment photoViewerFragment;
	
	public SharedMediaAdapter(Context context, int size_image, ArrayList<HikeSharedFile> sharedMediaItems, String msisdn, ViewPager viewPager, PhotoViewerFragment photoViewerFragment)
	{
		this.context = context;
		this.layoutInflater = LayoutInflater.from(this.context);
		this.sharedMediaLoader = new SharedFileImageLoader(context, size_image);
		this.sharedMediaItems = sharedMediaItems;
		this.photoViewerFragment = photoViewerFragment;
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

		TouchImageView galleryImageView = (TouchImageView) page.findViewById(R.id.album_image);
		galleryImageView.setZoom(1.0f);
		galleryImageView.setScaleType(ScaleType.FIT_CENTER);
		sharedMediaLoader.loadImage(sharedMediaItem.getImageLoaderKey(true), galleryImageView, false);
		if (sharedMediaItem.getHikeFileType() == HikeFileType.VIDEO)
		{
			page.findViewById(R.id.progress_bar).setVisibility(View.GONE);
			page.findViewById(R.id.play_media).setVisibility(View.VISIBLE);
		}
		else
		{
			page.findViewById(R.id.play_media).setVisibility(View.GONE);
		}
		
		galleryImageView.setTag(sharedMediaItem);
		galleryImageView.setOnClickListener(this);
		((ViewPager) container).addView(page);
		return page;
	}

	@Override
	public void onClick(View v)
	{
		HikeSharedFile sharedMediaItem = (HikeSharedFile) v.getTag();
		switch (sharedMediaItem.getHikeFileType())
		{
		case IMAGE:
			photoViewerFragment.toggleViewsVisibility();
			break;
		case VIDEO:
			Intent openFile = new Intent(Intent.ACTION_VIEW);
			openFile.setDataAndType(Uri.fromFile(sharedMediaItem.getFile()), sharedMediaItem.getFileTypeString());
			context.startActivity(openFile);
			break;
		default:
			break;
		}
	}
}