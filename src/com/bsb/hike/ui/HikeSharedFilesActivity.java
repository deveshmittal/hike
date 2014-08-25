package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.os.Parcelable;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.HikeSharedFileAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.ui.fragments.PhotoViewerFragment;
import com.bsb.hike.utils.CustomAlertDialog;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class HikeSharedFilesActivity extends HikeAppStateBaseFragmentActivity implements OnScrollListener, OnItemClickListener, OnItemLongClickListener
{

	private List<HikeSharedFile> sharedFilesList;

	private HikeSharedFileAdapter adapter;

	private String msisdn;

	private boolean multiSelectMode;

	private Map<Long, HikeSharedFile> selectedSharedFileItems;

	private TextView multiSelectTitle;

	private int previousFirstVisibleItem;

	private long previousEventTime;

	private int velocity;
	
	private boolean reachedEnd = false;
	
	private boolean loadingMoreItems = false;
	
	private String TAG = "HikeSharedFilesActivity";
	
	public boolean isMultiSelectMode()
	{
		return multiSelectMode;
	}

	public void setMultiSelectMode(boolean multiSelectMode)
	{
		this.multiSelectMode = multiSelectMode;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);  //Making the action bar overlay on top of the current view
		setContentView(R.layout.gallery);

		selectedSharedFileItems = new HashMap<Long, HikeSharedFile>();
		sharedFilesList = new ArrayList<HikeSharedFile>();

		Bundle data;
		if (savedInstanceState != null)
		{
			data = savedInstanceState;
		}
		else
		{
			data = getIntent().getExtras();
		}

		msisdn = data.getString(HikeConstants.Extras.MSISDN);

		GridView gridView = (GridView) findViewById(R.id.gallery);

		int sizeOfImage = getResources().getDimensionPixelSize(R.dimen.gallery_album_item_size);

		int numColumns = Utils.getNumColumnsForGallery(getResources(), sizeOfImage);
		int actualSize = Utils.getActualSizeForGallery(getResources(), sizeOfImage, numColumns);

		sharedFilesList = HikeConversationsDatabase.getInstance().getSharedMedia(msisdn, HikeConstants.MAX_MEDIA_ITEMS_TO_LOAD_INITIALLY, -1, true);
		adapter = new HikeSharedFileAdapter(this, sharedFilesList, actualSize, selectedSharedFileItems, false);

		gridView.setNumColumns(numColumns);
		gridView.setAdapter(adapter);
		gridView.setOnScrollListener(this);
		gridView.setOnItemClickListener(this);
		gridView.setOnItemLongClickListener(this);
		setupActionBar();
	}

	@Override
	public void onBackPressed()
	{
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(HikeConstants.IMAGE_FRAGMENT_TAG);
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
		
		if (fragment != null)
		{	
			PhotoViewerFragment.onPhotoBack(fragment, fragmentTransaction, getSupportActionBar());
			return;
		}
		if (multiSelectMode)
		{
			destroyActionMode();
		}
		else
		{
			super.onBackPressed();
		}
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(getString(R.string.shared_media));

		TextView subText = (TextView) actionBarView.findViewById(R.id.subtext);
		subText.setVisibility(View.GONE);

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

	private void setupMultiSelectActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.action_mode_action_bar, null);

		View closeBtn = actionBarView.findViewById(R.id.close_action_mode);
		multiSelectTitle = (TextView) actionBarView.findViewById(R.id.title);
		ViewGroup closeContainer = (ViewGroup) actionBarView.findViewById(R.id.close_container);

		closeContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});
		actionBar.setCustomView(actionBarView);

		Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left_noalpha);
		slideIn.setInterpolator(new AccelerateDecelerateInterpolator());
		slideIn.setDuration(200);
		closeBtn.startAnimation(slideIn);

		initializeActionMode();
	}

	public boolean initializeActionMode()
	{
		setMultiSelectMode(true);
		if (selectedSharedFileItems.size() > 0)
		{
			setActionModeTitle(selectedSharedFileItems.size());
		}
		invalidateOptionsMenu();
		return true;
	}

	public void destroyActionMode()
	{
		selectedSharedFileItems.clear();
		adapter.notifyDataSetChanged();
		setMultiSelectMode(false);
		setupActionBar();
		invalidateOptionsMenu();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if (multiSelectMode)
		{
			getSupportMenuInflater().inflate(R.menu.multi_select_chat_menu, menu);
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		if(multiSelectMode)
		{
			if (selectedSharedFileItems.size() > FileTransferManager.getInstance(this).remainingTransfers())
			{
				menu.findItem(R.id.forward_msgs).setVisible(false);
			}
			else
			{
				menu.findItem(R.id.forward_msgs).setVisible(true);
			}
		}
		return super.onPrepareOptionsMenu(menu);
	}
	
	private void setActionModeTitle(int count)
	{
		if (multiSelectTitle != null)
		{
			multiSelectTitle.setText(getString(R.string.selected_count, count));
		}
	}

	
	@Override
	public void onScroll(AbsListView view, final int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{

		if (previousFirstVisibleItem != firstVisibleItem)
		{
			long currTime = System.currentTimeMillis();
			long timeToScrollOneElement = currTime - previousEventTime;
			velocity = (int) (((double) 1 / timeToScrollOneElement) * 1000);

			previousFirstVisibleItem = firstVisibleItem;
			previousEventTime = currTime;
		}
	
		if (!reachedEnd && !loadingMoreItems && sharedFilesList != null && !sharedFilesList.isEmpty() && (firstVisibleItem + visibleItemCount)  <= totalItemCount - 9)
		{
			loadingMoreItems = true;

			final long lastItemId = sharedFilesList.get(sharedFilesList.size()-1).getMsgId();

			AsyncTask<Void, Void, List<HikeSharedFile>> asyncTask = new AsyncTask<Void, Void, List<HikeSharedFile>>()
			{

				@Override
				protected List<HikeSharedFile> doInBackground(Void... params)
				{
					return  HikeConversationsDatabase.getInstance().getSharedMedia(msisdn, HikeConstants.MAX_MEDIA_ITEMS_TO_LOAD_INITIALLY, lastItemId, true);
				}

				@Override
				protected void onPostExecute(List<HikeSharedFile> result)
				{
					if (!result.isEmpty())
					{
						sharedFilesList.addAll(result);
						adapter.notifyDataSetChanged();
					}
					else
					{
						Logger.d(TAG,"No more items found in Db");
						reachedEnd = true;
					}
					loadingMoreItems = false;
				}
			};

			if (Utils.isHoneycombOrHigher())
			{
				asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
			else
			{
				asyncTask.execute();
			}
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState)
	{
		adapter.setIsListFlinging(velocity > HikeConstants.MAX_VELOCITY_FOR_LOADING_IMAGES && scrollState == OnScrollListener.SCROLL_STATE_FLING);
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
	{
		HikeSharedFile sharedFileItem = sharedFilesList.get(position);

		if (multiSelectMode)
		{
			if (selectedSharedFileItems.containsKey(sharedFileItem.getMsgId()))
			{
				selectedSharedFileItems.remove(sharedFileItem.getMsgId());
				if (selectedSharedFileItems.isEmpty())
				{
					destroyActionMode();
				}
				else
				{
					setMultiSelectTitle();
				}

				if (selectedSharedFileItems.size() == FileTransferManager.getInstance(this).remainingTransfers())
				{
					invalidateOptionsMenu();
				}
			}
			else
			{
				selectedSharedFileItems.put(sharedFileItem.getMsgId(), sharedFileItem);
				setMultiSelectTitle();
				if (selectedSharedFileItems.size() > FileTransferManager.getInstance(this).remainingTransfers())
				{
					invalidateOptionsMenu();
				}
			}
			
			adapter.notifyDataSetChanged();
		}
		else
		{
			// TODO send this intent to photo viewer
			Bundle arguments = new Bundle();
			arguments.putParcelableArrayList(HikeConstants.Extras.SHARED_FILE_ITEMS, (ArrayList<? extends Parcelable>) sharedFilesList);
			arguments.putInt(HikeConstants.MEDIA_POSITION, position);
			arguments.putString(HikeConstants.Extras.MSISDN, msisdn);
			PhotoViewerFragment.openPhoto(R.id.parent_layout, HikeSharedFilesActivity.this, arguments);
		}

	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id)
	{
		if (!multiSelectMode)
		{
			setupMultiSelectActionBar();
		}

		if (selectedSharedFileItems.size() >= FileTransferManager.getInstance(this).remainingTransfers())
		{
			Toast.makeText(this, getString(R.string.max_num_files_reached, FileTransferManager.getInstance(this).getTaskLimit()), Toast.LENGTH_SHORT).show();
			return false;
		}

		HikeSharedFile sharedFileItem = sharedFilesList.get(position);

		selectedSharedFileItems.put(sharedFileItem.getMsgId(), sharedFileItem);

		adapter.notifyDataSetChanged();

		setMultiSelectTitle();

		return true;
	}

	private void setMultiSelectTitle()
	{
		if (multiSelectTitle == null)
		{
			return;
		}
		multiSelectTitle.setText(getString(R.string.gallery_num_selected, selectedSharedFileItems.size()));
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (multiSelectMode)
		{
			onActionModeItemClicked(item);
		}
		return super.onOptionsItemSelected(item);
	}

	public boolean onActionModeItemClicked(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.delete_msgs:
			final CustomAlertDialog deleteConfirmDialog = new CustomAlertDialog(HikeSharedFilesActivity.this);
			if (selectedSharedFileItems.size() == 1)
			{
				deleteConfirmDialog.setHeader(R.string.confirm_delete_msg_header);
				deleteConfirmDialog.setBody(R.string.confirm_delete_msg);
			}
			else
			{
				deleteConfirmDialog.setHeader(R.string.confirm_delete_msgs_header);
				deleteConfirmDialog.setBody(getString(R.string.confirm_delete_msgs, selectedSharedFileItems.size()));
			}
			View.OnClickListener dialogOkClickListener = new View.OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					ArrayList<Long> msgIds = new ArrayList<Long>(selectedSharedFileItems.size());
					for (HikeSharedFile file : selectedSharedFileItems.values())
					{
						msgIds.add(file.getMsgId());
					}
					HikeMessengerApp.getPubSub().publish(HikePubSub.REMOVE_MESSAGE_FROM_CHAT_THREAD, msgIds);
					sharedFilesList.removeAll(selectedSharedFileItems.values());
					destroyActionMode();
					deleteConfirmDialog.dismiss();
				}
			};

			deleteConfirmDialog.setOkButton(R.string.delete, dialogOkClickListener);
			deleteConfirmDialog.setCancelButton(R.string.cancel);
			deleteConfirmDialog.show();
			return true;
		case R.id.forward_msgs:
			ArrayList<Long> selectedMsgIds = new ArrayList<Long>(selectedSharedFileItems.keySet());
			Collections.sort(selectedMsgIds);
			Intent intent = new Intent(HikeSharedFilesActivity.this, ComposeChatActivity.class);
			intent.putExtra(HikeConstants.Extras.FORWARD_MESSAGE, true);
			JSONArray multipleMsgArray = new JSONArray();
			try
			{
				for (int i = 0; i < selectedMsgIds.size(); i++)
				{
					HikeSharedFile hikeFile = selectedSharedFileItems.get(selectedMsgIds.get(i));
					JSONObject multiMsgFwdObject = new JSONObject();
					multiMsgFwdObject.putOpt(HikeConstants.Extras.FILE_KEY, hikeFile.getFileKey());

					multiMsgFwdObject.putOpt(HikeConstants.Extras.FILE_PATH, hikeFile.getFilePath());
					multiMsgFwdObject.putOpt(HikeConstants.Extras.FILE_TYPE, hikeFile.getFileTypeString());
					if (hikeFile.getHikeFileType() == HikeFileType.AUDIO_RECORDING)
					{
						multiMsgFwdObject.putOpt(HikeConstants.Extras.RECORDING_TIME, hikeFile.getRecordingDuration());
					}

					multipleMsgArray.put(multiMsgFwdObject);
				}
			}
			catch (JSONException e)
			{
				Logger.e(getClass().getSimpleName(), "Invalid JSON", e);
			}
			intent.putExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT, multipleMsgArray.toString());
			intent.putExtra(HikeConstants.Extras.PREV_MSISDN, msisdn);
			startActivity(intent);
			return true;
		}
		return false;
	}
}
