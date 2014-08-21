package com.bsb.hike.ui.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.adapters.SharedMediaAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class PhotoViewerFragment extends SherlockFragment implements OnPageChangeListener
{
	private SharedMediaAdapter smAdapter;

	private ViewPager selectedPager;

	private ArrayList<HikeSharedFile> sharedMediaItems;

	private int numColumns;

	private int actualSize;

	private int sizeOfImage;
	
	private int initialPosition;
	
	private int prevPosition = 0;
	
	private String msisdn;
	
	private boolean fromChatThread = false;
	
	private boolean reachedEndLeft = false;
	
	private boolean reachedEndRight = false;
	
	private boolean loadingMoreItems = false;
	
	private long minMsgId;
	
	private long maxMsgId;
	
	private boolean applyOffset = false;
	
	private String TAG = "PhotoViewerFragment";
	
	private int PAGER_LIMIT = 3;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View parent = inflater.inflate(R.layout.shared_media_viewer, null);
		selectedPager = (ViewPager) parent.findViewById(R.id.selection_pager);
		sizeOfImage = getResources().getDimensionPixelSize(R.dimen.gallery_selection_item_size);
		setupActionBar();
		numColumns = Utils.getNumColumnsForGallery(getResources(), sizeOfImage);
		actualSize = Utils.getActualSizeForGallery(getResources(), sizeOfImage, numColumns);
		
		sharedMediaItems = getArguments().getParcelableArrayList(HikeConstants.Extras.SHARED_FILE_ITEMS);
		initialPosition = getArguments().getInt(HikeConstants.MEDIA_POSITION);
		msisdn = getArguments().getString(HikeConstants.Extras.MSISDN);
		minMsgId = sharedMediaItems.get(0).getMsgId();
		maxMsgId = sharedMediaItems.get(getCount()-1).getMsgId();
		
		if(getArguments().containsKey(HikeConstants.FROM_CHAT_THREAD))
			fromChatThread = getArguments().getBoolean(HikeConstants.FROM_CHAT_THREAD);
		
		smAdapter = new SharedMediaAdapter(getActivity(), actualSize, sharedMediaItems, msisdn, selectedPager);
		selectedPager.setAdapter(smAdapter);
		selectedPager.setOnPageChangeListener(this);
		
		selectedPager.setPageTransformer(false, new ViewPager.PageTransformer()
		{
			//Adding some sleek animations on transforming pages.
			@Override
			public void transformPage(View page, float position)
			{
				// TODO Auto-generated method stub
				 final float normalizedposition = Math.abs(Math.abs(position) - 1);
				    page.setAlpha(normalizedposition);
				    page.setScaleX(normalizedposition / 2 + 0.5f);
				    page.setScaleY(normalizedposition / 2 + 0.5f);
			}
		});
		
		//Load media to the right and left of the view pager if this fragment is called from ChatThread.
		if(fromChatThread)
		{	Logger.d(TAG,  " MsgId : " + sharedMediaItems.get(0).getMsgId());
			loadItems(false,sharedMediaItems.get(0).getMsgId(),HikeConstants.MAX_MEDIA_ITEMS_TO_LOAD_INITIALLY/2,false, true, initialPosition);  //Left
			loadItems(false,sharedMediaItems.get(0).getMsgId(),HikeConstants.MAX_MEDIA_ITEMS_TO_LOAD_INITIALLY/2, true);         //Right
		}
		else
		{
			setSelection(initialPosition);  //Opened from the gallery perhaps, hence set the view pager to the required position
		}
		
		return parent;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onStop()
	{	super.onStop();
		
	}
	
	@Override
	public void  onSaveInstanceState(Bundle outState)
	{	
		super.onSaveInstanceState(outState);
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
	public void onPageSelected(int position)
	{
		// TODO Auto-generated method stub
		if (!reachedEndRight && !loadingMoreItems && position == (getCount() - PAGER_LIMIT))
		{
			loadingMoreItems = true;
			//Logger.d(TAG,"loading items from right : " + maxMsgId);
			loadItems(reachedEndRight, maxMsgId, HikeConstants.MAX_MEDIA_ITEMS_TO_LOAD_INITIALLY, true);
		}

		if (!reachedEndLeft && !loadingMoreItems && position == PAGER_LIMIT)
		{
			loadingMoreItems = true;
			//Logger.d(TAG, "loading items from left : " + minMsgId);
			loadItems(reachedEndLeft, minMsgId, HikeConstants.MAX_MEDIA_ITEMS_TO_LOAD_INITIALLY, false, true, position);

		}
	}

	private void setSelection(int position)
	{
		selectedPager.setCurrentItem(position, false); 
	}
	
	private void setupActionBar()
	{
		ActionBar actionBar = getSherlockActivity().getSupportActionBar();
		actionBar.hide(); //More items related to actionbar will be added
	}
	
	public static void openPhoto (int resId, Context context, Bundle arguments)
	{
		PhotoViewerFragment photoViewerFragment = new PhotoViewerFragment();
		photoViewerFragment.setArguments(arguments);

		FragmentTransaction fragmentTransaction = ((FragmentActivity) context).getSupportFragmentManager().beginTransaction();
		fragmentTransaction.add(resId, photoViewerFragment, HikeConstants.IMAGE_FRAGMENT_TAG);
		fragmentTransaction.commitAllowingStateLoss();
		
	}
	
	public static void onPhotoBack(Fragment fragment, FragmentTransaction fragmentTransaction, ActionBar actionBar)
	{
		if (fragment != null && fragment.isVisible() && fragment instanceof PhotoViewerFragment)
		{	
			fragmentTransaction.remove(fragment);
			fragmentTransaction.commitAllowingStateLoss();
			actionBar.show();
			return;
		}
	}
	
	public void loadItems(boolean reachedEnd, long maxMsgId, int limit, boolean itemsToRight)
	{
		loadItems(reachedEnd, maxMsgId, limit, itemsToRight, false, 0);
	}
	//function called to load items to the left of viewpager
	public void loadItems(boolean reachedEnd, long msgId, int limit, boolean itemsToRight, boolean applyOffset, int prevPosition)
	{
		if(applyOffset)
		{
			this.applyOffset = applyOffset;
			this.prevPosition = prevPosition;
		}
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
					sharedMediaItems.addAll(getCount() , result);
				}
				else				//Loading them to the left
				{	Collections.reverse(result);
					sharedMediaItems.addAll(0, result);
					
				}
			   //Recalculating the min and Max msgIds, for further loading
				smAdapter.notifyDataSetChanged();
				minMsgId = sharedMediaItems.get(0).getMsgId();
				maxMsgId = sharedMediaItems.get(getCount()-1).getMsgId();

				if(applyOffset && !itemsToRight)   //Offset needed to correctly display current item in viewpager, if more items are loaded to left
				{
					selectedPager.setCurrentItem(result.size() + prevPosition , false); 
					applyOffset = false;
				}
			}
			
			else
			{
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
	
	private int getCount()
	{
		return sharedMediaItems.size();
	}
}
