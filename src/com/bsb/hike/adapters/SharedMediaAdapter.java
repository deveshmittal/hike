package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.smartImageLoader.SharedFileImageLoader;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class SharedMediaAdapter extends PagerAdapter
{
	private LayoutInflater layoutInflater;

	private SharedFileImageLoader sharedMediaLoader;

	private ArrayList<HikeSharedFile> sharedMediaItems;

	private boolean reachedEndLeft = false;
	
	private boolean reachedEndRight = false;
	
	private boolean loadingMoreItems = false;

	private Context context;

	private String msisdn;
	
	private long minMsgId;
	
	private long maxMsgId;
	
	private boolean applyOffset = false;
	
	private ViewPager selectedPager;
	
	private int prevPosition = 0;
	
	private String TAG = "SharedMediaAdapter";
	
	private String IMAGE_TAG = "image";
	
	public SharedMediaAdapter(Context context, int size_image, ArrayList<HikeSharedFile> sharedMediaItems, String msisdn, ViewPager viewPager)
	{
		this.context = context;
		this.layoutInflater = LayoutInflater.from(this.context);
		this.sharedMediaLoader = new SharedFileImageLoader(context, size_image);
		this.sharedMediaItems = sharedMediaItems;
		this.msisdn = msisdn;
		this.minMsgId = sharedMediaItems.get(0).getMsgId();
		this.maxMsgId = sharedMediaItems.get(sharedMediaItems.size()-1).getMsgId();
		this.selectedPager = viewPager;
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
		sharedMediaLoader.loadImage(sharedMediaItem.getFilePath(), galleryImageView, false);
		((ViewPager) container).addView(page);
		Logger.d(TAG, "Position : " + position + " : " + getCount() + " : " + loadingMoreItems + " Right End ? : " + reachedEndRight + "Left End ? : " + reachedEndLeft);
		
		if (!reachedEndRight && !loadingMoreItems && position == (getCount() - 3))
		{
			loadingMoreItems = true;
			Logger.d(TAG,"loading items from right : " + maxMsgId);
			loadItems(reachedEndRight, maxMsgId, HikeConstants.MAX_MEDIA_ITEMS_TO_LOAD_INITIALLY, true);
		}

		if (!reachedEndLeft && !loadingMoreItems && position == 3)
		{
			loadingMoreItems = true;
			Logger.d(TAG, "loading items from left : " + minMsgId);
			loadItems(reachedEndLeft, minMsgId, HikeConstants.MAX_MEDIA_ITEMS_TO_LOAD_INITIALLY, false, true, position);

		}
		return page;
	}

	public void loadItems(boolean reachedEnd, long maxMsgId, int limit, boolean itemsToRight)
	{
		if (Utils.isHoneycombOrHigher())
		{
			new GetMoreItemsTask(reachedEnd, maxMsgId, limit, itemsToRight).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		else
		{
			 new GetMoreItemsTask(reachedEnd, maxMsgId, limit, itemsToRight).execute();
		}
	}
	//function called to load items to the left of viewpager
	public void loadItems(boolean reachedEnd, long msgId, int limit, boolean itemsToRight, boolean applyOffset, int prevPosition)
	{
		this.applyOffset = applyOffset;
		this.prevPosition = prevPosition;
		if (Utils.isHoneycombOrHigher())
		{
			new GetMoreItemsTask(reachedEnd, msgId, limit, itemsToRight).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		else
		{
			 new GetMoreItemsTask(reachedEnd, msgId, limit, itemsToRight).execute();
		}
	}
	/*
	 * AsyncTask to load more media in the background thread
	 */
	public class GetMoreItemsTask extends AsyncTask<Void, Void, List<HikeSharedFile>>
	{
		private boolean itemsToRight;

		private Long msgId;

		private int limit;

		public GetMoreItemsTask(boolean reachEnd, long msgId, int limit, boolean itemsToRight)
		{
			this.itemsToRight = itemsToRight;
			this.msgId = msgId;
			this.limit = limit;
		}

		@Override
		protected List<HikeSharedFile> doInBackground(Void... params)
		{
			return HikeConversationsDatabase.getInstance().getSharedMedia(msisdn, limit, msgId, true, itemsToRight);
		}

		@Override
		protected void onPostExecute(List<HikeSharedFile> result)
		{
			if (!result.isEmpty())
			{  
				if (itemsToRight)    //Loading items to the right of the viewpager
				{
					sharedMediaItems.addAll(sharedMediaItems.size() , result);
				}
				else				//Loading them to the left
				{	Collections.reverse(result);
					sharedMediaItems.addAll(0, result);
					
				}
			   //Recalculating the min and Max msgIds, for further loading
				notifyDataSetChanged();
				minMsgId = sharedMediaItems.get(0).getMsgId();
				maxMsgId = sharedMediaItems.get(sharedMediaItems.size()-1).getMsgId();

				if(applyOffset && !itemsToRight)   //Offset needed to correctly display current item in viewpager, if more items are loaded to left
				{
					selectedPager.setCurrentItem(result.size() + prevPosition , false); 
					applyOffset = false;
				}
			}
			
			else
			{
				/*
				 * This signifies that we've reached the end. No need to query the db anymore unless we find a new media item.
				 */
				if (itemsToRight)
				{
					reachedEndRight = true;
					Logger.d(TAG, "Reached right end");
				}
				else
				{
					reachedEndLeft = true;
					Logger.d(TAG, "Reached left end");
				}
			}
			
			loadingMoreItems = false;
		}
	}
}