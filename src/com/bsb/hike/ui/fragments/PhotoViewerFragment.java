package com.bsb.hike.ui.fragments;

import java.util.ArrayList;

import android.os.Bundle;
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
	
	private String msisdn;
	
	private boolean fromChatThread = false;
	
	private String TAG = "PhotoViewerFragment";
	
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

		numColumns = Utils.getNumColumnsForGallery(getResources(), sizeOfImage);
		actualSize = Utils.getActualSizeForGallery(getResources(), sizeOfImage, numColumns);

		sharedMediaItems = getArguments().getParcelableArrayList(HikeConstants.Extras.SHARED_FILE_ITEMS);
		initialPosition = getArguments().getInt(HikeConstants.MEDIA_POSITION);
		msisdn = getArguments().getString(HikeConstants.Extras.MSISDN);
		
		if(getArguments().containsKey(HikeConstants.FROM_CHAT_THREAD))
			fromChatThread = getArguments().getBoolean(HikeConstants.FROM_CHAT_THREAD);
		
		setupActionBar();
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
		Logger.d(TAG, " Inside Photo Viewer fragment inside on Start  shared media size : " + sharedMediaItems.size() + " at Position : " + initialPosition);
		
		//Load media to the right and left of the view pager if this fragment is called from ChatThread.
		if(fromChatThread)
		{	Logger.d(TAG,  " MsgId : " + sharedMediaItems.get(0).getMsgId());
			smAdapter.loadItems(false,sharedMediaItems.get(0).getMsgId(),HikeConstants.MAX_MEDIA_ITEMS_TO_LOAD_INITIALLY/2,false, true, initialPosition);  //Left
			smAdapter.loadItems(false,sharedMediaItems.get(0).getMsgId(),HikeConstants.MAX_MEDIA_ITEMS_TO_LOAD_INITIALLY/2, true);         //Right
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
	public void onPageSelected(int arg0)
	{
		// TODO Auto-generated method stub
	}

	private void setSelection(int position)
	{
		selectedPager.setCurrentItem(position, false); 
	}
	
	private void setupActionBar()
	{
		ActionBar actionBar = getSherlockActivity().getSupportActionBar();
		actionBar.hide();
	}
}
