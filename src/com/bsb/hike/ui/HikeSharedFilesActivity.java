package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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
import com.actionbarsherlock.view.Menu;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.adapters.HikeSharedFileAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
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
						/*
						 * This signifies that we've reached the end. No need to query the db anymore unless we add a new message.
						 */
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

		Intent intent;
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
			intent = new Intent();
			intent.putParcelableArrayListExtra(HikeConstants.Extras.SHARED_FILE_ITEMS, new ArrayList<HikeSharedFile>(selectedSharedFileItems.values()));
			// TODO send this intent to photo viewer
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
}
