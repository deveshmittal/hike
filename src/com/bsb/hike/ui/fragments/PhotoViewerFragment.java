package com.bsb.hike.ui.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.SharedMediaAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.ui.ComposeChatActivity;
import com.bsb.hike.ui.HikeSharedFilesActivity;
import com.bsb.hike.ui.utils.DepthPageTransformer;
import com.bsb.hike.utils.CustomAlertDialog;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class PhotoViewerFragment extends SherlockFragment implements OnPageChangeListener
{
	private View mParent;
	
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
	
	private Map<String, String> msisdnToNameMap;
	
	private long minMsgId;
	
	private long maxMsgId;
	
	private boolean applyOffset = false;
	
	private TextView senderName;
	
	private TextView itemTimeStamp;
	
	private boolean isGroup = false;
	
	private String conversationName;
	
	private String TAG = "PhotoViewerFragment";
	
	private int PAGER_LIMIT = 3;
	
	String[] msisdnArray;
	
	String[] nameArray;

	private ImageView gallaryButton;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		mParent = inflater.inflate(R.layout.shared_media_viewer, null);
		
		initializeViews(mParent);
		
		readArguments();
		
		if(savedInstanceState != null)
		{
			initialPosition = savedInstanceState.getInt(HikeConstants.Extras.CURRENT_POSITION, initialPosition);
		}
		
		return mParent;
	}
	
	private void initializeViews(View parent)
	{
		selectedPager = (ViewPager) parent.findViewById(R.id.selection_pager);
		senderName = (TextView) parent.findViewById(R.id.sender_name);
		itemTimeStamp = (TextView) parent.findViewById(R.id.item_time_stamp);
		gallaryButton  = (ImageView) parent.findViewById(R.id.gallary_button);
	}

	private void readArguments()
	{
		sharedMediaItems = getArguments().getParcelableArrayList(HikeConstants.Extras.SHARED_FILE_ITEMS);
		initialPosition = getArguments().getInt(HikeConstants.MEDIA_POSITION);
		msisdn = getArguments().getString(HikeConstants.Extras.MSISDN);
		isGroup = getArguments().getBoolean(HikeConstants.Extras.IS_GROUP_CONVERSATION, false);
		conversationName = getArguments().getString(HikeConstants.Extras.CONVERSATION_NAME);
		
		if(isGroup)
		{
			msisdnArray = getArguments().getStringArray(HikeConstants.Extras.PARTICIPANT_MSISDN_ARRAY);
			nameArray = getArguments().getStringArray(HikeConstants.Extras.PARTICIPANT_NAME_ARRAY);
			msisdnToNameMap = new HashMap<String, String>(msisdnArray.length);
			for(int i=0; i<msisdnArray.length; i++)
			{
				msisdnToNameMap.put(msisdnArray[i], nameArray[i]);
			}
		}
	}
	
	private void intialiazeViewPager()
	{
		int screenWidth = getResources().getDisplayMetrics().widthPixels;
		int screenHeight = getResources().getDisplayMetrics().heightPixels;
		sizeOfImage = screenWidth < screenHeight ? screenWidth : screenHeight;
		numColumns = Utils.getNumColumnsForGallery(getResources(), sizeOfImage);
		actualSize = Utils.getActualSizeForGallery(getResources(), sizeOfImage, numColumns);

		minMsgId = sharedMediaItems.get(0).getMsgId();
		maxMsgId = sharedMediaItems.get(getCount()-1).getMsgId();
		
		if(getArguments().containsKey(HikeConstants.FROM_CHAT_THREAD))
			fromChatThread = getArguments().getBoolean(HikeConstants.FROM_CHAT_THREAD);
		
		smAdapter = new SharedMediaAdapter(getActivity(), actualSize, sharedMediaItems, msisdn, selectedPager, this);
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
		
		if(Utils.isHoneycombOrHigher())  //The method setPageTransformer works only on API 11+. For lower devices, we can add margin to the view pager to show gap between adjacent views. 
			selectedPager.setPageTransformer(true, new DepthPageTransformer());
		else
			selectedPager.setPageMargin((int) getResources().getDimension(R.dimen.horizontal_page_margin));
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		/*
		 * We post execute setupActionBar in ChatThread; So to handle action bar of media viewer on rotation we need to do its action bar setup after activity is created and post
		 * UI this runnable
		 */
		(new Handler()).post(new Runnable()
		{

			@Override
			public void run()
			{
				setupActionBar();
			}
		});
		
		intialiazeViewPager();
		
		// Load media to the right and left of the view pager if this fragment is called from ChatThread.
		if (fromChatThread)
		{
			Logger.d(TAG, " MsgId : " + sharedMediaItems.get(0).getMsgId());
			loadItems(false, sharedMediaItems.get(0).getMsgId(), HikeConstants.MAX_MEDIA_ITEMS_TO_LOAD_INITIALLY / 2, false, true, initialPosition); // Left
			loadItems(false, sharedMediaItems.get(0).getMsgId(), HikeConstants.MAX_MEDIA_ITEMS_TO_LOAD_INITIALLY / 2, true); // Right
			setSenderDetails(0);
		}
		else
		{
			setSelection(initialPosition); // Opened from the gallery perhaps, hence set the view pager to the required position
		}
		
		gallaryButton.setOnClickListener(new OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				startActivity(HikeSharedFilesActivity.getHikeSharedFilesActivityIntent(getSherlockActivity(), isGroup, conversationName, msisdnArray, nameArray, msisdn));
			}
		});
		
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onStop()
	{	super.onStop();
		
	}
	
	@Override
	public void onPause()
	{
		// TODO Auto-generated method stub
		super.onPause();
		if(smAdapter != null)
		{
			smAdapter.getSharedFileImageLoader().setExitTasksEarly(true);
		}
	}

	@Override
	public void  onSaveInstanceState(Bundle outState)
	{	
		outState.putInt(HikeConstants.Extras.CURRENT_POSITION, selectedPager.getCurrentItem());
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
		
		setSenderDetails(position);
	}

	private void setSenderDetails(int position)
	{
		senderName.setText(getSenderName(position));
		long timeStamp = sharedMediaItems.get(position).getTimeStamp();
		String date = Utils.getFormattedDate(getSherlockActivity(), timeStamp);
		String time = Utils.getFormattedTime(false, getSherlockActivity(), timeStamp);
		itemTimeStamp.setText(date+", "+time);
	}

	private String getSenderName(int position)
	{
		HikeSharedFile hsf = sharedMediaItems.get(position);
		if(hsf.isSent())
		{
			return getString(R.string.you);
		}
		else if (isGroup)
		{
			return msisdnToNameMap.get(hsf.getGroupParticipantMsisdn());
		}
		else 
		{
			return conversationName;
		}
	}

	private void setSelection(int position)
	{
		selectedPager.setCurrentItem(position, false); 
	}
	
	public void setupActionBar()
	{
		if (getSherlockActivity() == null)
		{
			return;
		}
		/*
		 * else part
		 * */
		ActionBar actionBar = getSherlockActivity().getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = getSherlockActivity().getLayoutInflater().inflate(R.layout.compose_action_bar, null);
		actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_header_photo_viewer));

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(isGroup ? conversationName : Utils.getFirstName(conversationName));

		TextView subText = (TextView) actionBarView.findViewById(R.id.subtext);
		subText.setVisibility(View.GONE);

		actionBarView.findViewById(R.id.seprator).setVisibility(View.GONE);

		backContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				finish();
			}
		});

		actionBar.setCustomView(actionBarView);
	}
	
	private void finish()
	{
		getSherlockActivity().onBackPressed();
	}

	public static void openPhoto(int resId, Context context, ArrayList<HikeSharedFile> hikeSharedFiles, boolean fromChatThread, Conversation conversation)
	{
		Pair<String[], String[]> msisdnAndNameArrays = Utils.getMsisdnToNameArray(conversation);
		openPhoto(resId, context, hikeSharedFiles, fromChatThread, hikeSharedFiles.size() - 1, conversation.getMsisdn(), conversation.getLabel(),
				conversation instanceof GroupConversation, msisdnAndNameArrays.first, msisdnAndNameArrays.second);
	}

	public static void openPhoto(int resId, Context context, ArrayList<HikeSharedFile> hikeSharedFiles, boolean fromChatThread, int mediaPosition, String fromMsisdn,
			String convName, boolean isGroup, String[] msisdnArray, String[] nameArray)
	{
		PhotoViewerFragment photoViewerFragment = new PhotoViewerFragment();
		Bundle arguments = new Bundle();
		arguments.putInt(HikeConstants.MEDIA_POSITION, mediaPosition);
		arguments.putBoolean(HikeConstants.FROM_CHAT_THREAD, fromChatThread);
		arguments.putString(HikeConstants.Extras.MSISDN, fromMsisdn);
		arguments.putString(HikeConstants.Extras.CONVERSATION_NAME, convName);
		arguments.putParcelableArrayList(HikeConstants.Extras.SHARED_FILE_ITEMS, hikeSharedFiles);
		arguments.putBoolean(HikeConstants.Extras.IS_GROUP_CONVERSATION, isGroup);
		if (isGroup)
		{
			arguments.putStringArray(HikeConstants.Extras.PARTICIPANT_MSISDN_ARRAY, msisdnArray);
			arguments.putStringArray(HikeConstants.Extras.PARTICIPANT_NAME_ARRAY, nameArray);
		}

		photoViewerFragment.setArguments(arguments);

		FragmentTransaction fragmentTransaction = ((FragmentActivity) context).getSupportFragmentManager().beginTransaction();
		fragmentTransaction.add(resId, photoViewerFragment, HikeConstants.IMAGE_FRAGMENT_TAG);
		fragmentTransaction.commitAllowingStateLoss();
		
	}
	
	/**
	 * used to open photo/video from a 1:1 conversation
	 */
	public static void openPhoto(int resId, Context context, ArrayList<HikeSharedFile> hikeSharedFiles, boolean fromChatThread, 
			int mediaPosition, String fromMsisdn, String convName)
	{
		openPhoto(resId, context, hikeSharedFiles, fromChatThread, mediaPosition, fromMsisdn, convName, false, null, null);
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
			return (List<HikeSharedFile>) HikeConversationsDatabase.getInstance().getSharedMedia(msisdn, limit, msgId, true, itemsToRight);
		}

		@Override
		protected void onPostExecute(List<HikeSharedFile> result)
		{
			if(getSherlockActivity() == null)
			{
				return ;
			}
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
	
	public HikeSharedFile getCurrentSelectedItem()
	{
		return sharedMediaItems.get(selectedPager.getCurrentItem());
	}
	
	public void removeCurrentSelectedItem()
	{
		sharedMediaItems.remove(selectedPager.getCurrentItem());
		if(sharedMediaItems.isEmpty())
		{
			//if list is empty close the fragment
			finish();
		}
		smAdapter.notifyDataSetChanged();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		//deletes current selected item from viewpager 
		case R.id.delete_msgs:
			final CustomAlertDialog deleteConfirmDialog = new CustomAlertDialog(getSherlockActivity());
			deleteConfirmDialog.setHeader(R.string.confirm_delete_msg_header);
			deleteConfirmDialog.setBody(R.string.confirm_delete_msg);
			
			View.OnClickListener dialogOkClickListener = new View.OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					ArrayList<Long> msgIds = new ArrayList<Long>(1);
					msgIds.add(getCurrentSelectedItem().getMsgId());
					
					Bundle bundle = new Bundle();
					bundle.putString(HikeConstants.Extras.MSISDN, msisdn);
					bundle.putInt(HikeConstants.Extras.DELETED_MESSAGE_TYPE, HikeConstants.SHARED_MEDIA_TYPE);
					HikeMessengerApp.getPubSub().publish(HikePubSub.DELETE_MESSAGE, new Pair<ArrayList<Long>, Bundle>(msgIds, bundle));
					
					// if delete media from phone is checked
					if(deleteConfirmDialog.isChecked() && getCurrentSelectedItem() != null)
					{
						getCurrentSelectedItem().delete(getActivity().getApplicationContext());
					}
					if(!fromChatThread)
					{
						HikeMessengerApp.getPubSub().publish(HikePubSub.HIKE_SHARED_FILE_DELETED, getCurrentSelectedItem());
					}
					removeCurrentSelectedItem();
					deleteConfirmDialog.dismiss();
				}
			};

			deleteConfirmDialog.setCheckBox(R.string.delete_media_from_sdcard);
			deleteConfirmDialog.setOkButton(R.string.delete, dialogOkClickListener);
			deleteConfirmDialog.setCancelButton(R.string.cancel);
			deleteConfirmDialog.show();
			return true;
		case R.id.forward_msgs:
			Intent intent = new Intent(getSherlockActivity(), ComposeChatActivity.class);
			intent.putExtra(HikeConstants.Extras.FORWARD_MESSAGE, true);
			JSONArray multipleMsgArray = new JSONArray();
			try
			{
				JSONObject multiMsgFwdObject = new JSONObject();
				Utils.handleFileForwardObject(multiMsgFwdObject, getCurrentSelectedItem());
				multipleMsgArray.put(multiMsgFwdObject);
			}
			catch (JSONException e)
			{
				Logger.e(getClass().getSimpleName(), "Invalid JSON", e);
			}
			intent.putExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT, multipleMsgArray.toString());
			intent.putExtra(HikeConstants.Extras.PREV_MSISDN, msisdn);
			startActivity(intent);
			return true;
		case R.id.share_msgs:
			getCurrentSelectedItem().shareFile(getSherlockActivity());
			return true;
		}
		return false;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		menu.clear();
		inflater.inflate(R.menu.photo_viewer_option_menu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public void onResume()
	{
		if(!getSherlockActivity().getSupportActionBar().isShowing())
		{
			toggleViewsVisibility();
		}
		if(smAdapter != null)
		{
			smAdapter.getSharedFileImageLoader().setExitTasksEarly(false);
			smAdapter.notifyDataSetChanged();
		}
		super.onResume();
	}

	public void toggleViewsVisibility()
	{
		if (getSherlockActivity() != null)
		{
			ActionBar actionbar = getSherlockActivity().getSupportActionBar();
			Animation animation;
			if (!actionbar.isShowing())
			{
				actionbar.show();
				animation = AnimationUtils.loadAnimation(getSherlockActivity(), R.anim.fade_in_animation);
			}
			else
			{
				actionbar.hide();
				animation = AnimationUtils.loadAnimation(getSherlockActivity(), R.anim.fade_out_animation);
			}
			animation.setDuration(300);
			animation.setFillAfter(true);
			mParent.findViewById(R.id.info_group).startAnimation(animation);
			mParent.findViewById(R.id.gradient).startAnimation(animation);
		}
	}
}
