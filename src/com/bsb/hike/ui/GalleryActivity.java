package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
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
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.adapters.GalleryAdapter;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.GalleryItem;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;

public class GalleryActivity extends HikeAppStateBaseFragmentActivity implements OnScrollListener, OnItemClickListener, OnItemLongClickListener
{

	private List<GalleryItem> galleryItemList;

	private GalleryAdapter adapter;

	private boolean isInsideAlbum;

	private String msisdn;

	private boolean multiSelectMode;

	private Map<Long, GalleryItem> selectedGalleryItems;

	private TextView multiSelectTitle;

	private String albumTitle;

	private int previousFirstVisibleItem;

	private long previousEventTime;

	private int velocity;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		setContentView(R.layout.gallery);

		selectedGalleryItems = new HashMap<Long, GalleryItem>();
		galleryItemList = new ArrayList<GalleryItem>();

		Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
		String[] projection = { MediaStore.Images.Media._ID, MediaStore.Images.Media.BUCKET_ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.DATA };

		String selection = null;
		String[] args = null;

		Bundle data;
		if (savedInstanceState != null)
		{
			data = savedInstanceState;
		}
		else
		{
			data = getIntent().getExtras();
		}

		GalleryItem selectedBucket = data.getParcelable(HikeConstants.Extras.SELECTED_BUCKET);
		msisdn = data.getString(HikeConstants.Extras.MSISDN);

		String sortBy;
		if (selectedBucket != null)
		{
			selection = MediaStore.Images.Media.BUCKET_ID + "=?";
			args = new String[] { selectedBucket.getBucketId() };

			isInsideAlbum = true;

			albumTitle = selectedBucket.getName();

			/*
			 * Adding the previously selected items.
			 */
			List<GalleryItem> prevSelectedItems = data.getParcelableArrayList(HikeConstants.Extras.GALLERY_SELECTIONS);
			if (prevSelectedItems != null && !prevSelectedItems.isEmpty())
			{
				for (GalleryItem galleryItem : prevSelectedItems)
				{
					selectedGalleryItems.put(galleryItem.getId(), galleryItem);
				}

				if (!multiSelectMode)
				{
					multiSelectMode = true;
					setupMultiSelectActionBar();
				}
				setMultiSelectTitle();
			}

			sortBy = MediaStore.Images.Media.DATE_MODIFIED + " DESC";
		}
		else
		{
			selection = "1) GROUP BY (" + MediaStore.Images.Media.BUCKET_ID;

			isInsideAlbum = false;

			sortBy = MediaStore.Images.Media.DATE_ADDED + " ASC";
		}

		Cursor cursor = getContentResolver().query(uri, projection, selection, args, sortBy);

		if (cursor != null)
		{
			try
			{
				int idIdx = cursor.getColumnIndex(MediaStore.Images.Media._ID);
				int bucketIdIdx = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID);
				int nameIdx = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
				int dataIdx = cursor.getColumnIndex(MediaStore.Images.Media.DATA);

				while (cursor.moveToNext())
				{
					GalleryItem galleryItem = new GalleryItem(cursor.getLong(idIdx), cursor.getString(bucketIdIdx), cursor.getString(nameIdx), cursor.getString(dataIdx));

					galleryItemList.add(galleryItem);
				}
			}
			finally
			{
				cursor.close();
			}
		}
		GridView gridView = (GridView) findViewById(R.id.gallery);

		int sizeOfImage = getResources().getDimensionPixelSize(isInsideAlbum ? R.dimen.gallery_album_item_size : R.dimen.gallery_cover_item_size);

		int numColumns = Utils.getNumColumnsForGallery(getResources(), sizeOfImage);
		int actualSize = Utils.getActualSizeForGallery(getResources(), sizeOfImage, numColumns);

		adapter = new GalleryAdapter(this, galleryItemList, isInsideAlbum, actualSize, selectedGalleryItems, false);

		gridView.setNumColumns(numColumns);
		gridView.setAdapter(adapter);
		gridView.setOnScrollListener(this);
		gridView.setOnItemClickListener(this);

		if (isInsideAlbum)
		{
			gridView.setOnItemLongClickListener(this);
		}

		if (!multiSelectMode)
		{
			setupActionBar(albumTitle);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putAll(getIntent().getExtras());
		outState.putParcelableArrayList(HikeConstants.Extras.GALLERY_SELECTIONS, new ArrayList<GalleryItem>(selectedGalleryItems.values()));

		super.onSaveInstanceState(outState);
	}

	@Override
	public void onBackPressed()
	{
		if (multiSelectMode)
		{
			selectedGalleryItems.clear();
			adapter.notifyDataSetChanged();

			setupActionBar(albumTitle);
			multiSelectMode = false;
		}
		else
		{
			super.onBackPressed();
		}
	}

	private void setupActionBar(String titleString)
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(titleString == null ? getString(R.string.gallery) : titleString);

		if (isInsideAlbum)
		{
			TextView subText = (TextView) actionBarView.findViewById(R.id.subtext);
			subText.setVisibility(View.VISIBLE);
			subText.setText(R.string.tap_hold_multi_select);
		}

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

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.chat_theme_action_bar, null);

		View sendBtn = actionBarView.findViewById(R.id.done_container);
		View closeBtn = actionBarView.findViewById(R.id.close_action_mode);
		ViewGroup closeContainer = (ViewGroup) actionBarView.findViewById(R.id.close_container);

		multiSelectTitle = (TextView) actionBarView.findViewById(R.id.title);
		multiSelectTitle.setText(getString(R.string.gallery_num_selected, 1));

		sendBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent();
				intent.putParcelableArrayListExtra(HikeConstants.Extras.GALLERY_SELECTIONS, new ArrayList<GalleryItem>(selectedGalleryItems.values()));

				sendGalleryIntent(intent);
			}
		});

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
		sendBtn.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_in));
	}

	private void sendGalleryIntent(Intent intent)
	{
		intent.setClass(this, GallerySelectionViewer.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(HikeConstants.Extras.MSISDN, msisdn);
		intent.putExtra(HikeConstants.Extras.ON_HIKE, getIntent().getBooleanExtra(HikeConstants.Extras.ON_HIKE, true));
		intent.putExtra(HikeConstants.Extras.SELECTED_BUCKET, getIntent().getParcelableExtra(HikeConstants.Extras.SELECTED_BUCKET));

		startActivity(intent);
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		if (previousFirstVisibleItem != firstVisibleItem)
		{
			long currTime = System.currentTimeMillis();
			long timeToScrollOneElement = currTime - previousEventTime;
			velocity = (int) (((double) 1 / timeToScrollOneElement) * 1000);

			previousFirstVisibleItem = firstVisibleItem;
			previousEventTime = currTime;
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
		GalleryItem galleryItem = galleryItemList.get(position);

		Intent intent;

		if (!isInsideAlbum)
		{
			intent = new Intent(this, GalleryActivity.class);
			intent.putExtra(HikeConstants.Extras.SELECTED_BUCKET, galleryItem);
			intent.putExtra(HikeConstants.Extras.MSISDN, msisdn);
			intent.putExtra(HikeConstants.Extras.ON_HIKE, getIntent().getBooleanExtra(HikeConstants.Extras.ON_HIKE, true));
			startActivity(intent);
		}
		else
		{
			if (multiSelectMode)
			{
				if (selectedGalleryItems.containsKey(galleryItem.getId()))
				{
					selectedGalleryItems.remove(galleryItem.getId());
					if (selectedGalleryItems.isEmpty())
					{
						setupActionBar(albumTitle);
						multiSelectMode = false;
					}
					else
					{
						setMultiSelectTitle();
					}
				}
				else
				{
					if (selectedGalleryItems.size() >= FileTransferManager.getInstance(this).remainingTransfers())
					{
						Toast.makeText(this, getString(R.string.max_num_files_reached, FileTransferManager.getInstance(this).getTaskLimit()), Toast.LENGTH_SHORT).show();
						return;
					}
					selectedGalleryItems.put(galleryItem.getId(), galleryItem);
					setMultiSelectTitle();
				}
				adapter.notifyDataSetChanged();
			}
			else
			{
				intent = new Intent();

				ArrayList<GalleryItem> item = new ArrayList<GalleryItem>(1);
				item.add(galleryItem);
				intent.putParcelableArrayListExtra(HikeConstants.Extras.GALLERY_SELECTIONS, item);
				sendGalleryIntent(intent);
			}
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id)
	{
		if (!multiSelectMode)
		{
			multiSelectMode = true;
			setupMultiSelectActionBar();
		}

		if (selectedGalleryItems.size() >= FileTransferManager.getInstance(this).remainingTransfers())
		{
			Toast.makeText(this, getString(R.string.max_num_files_reached, FileTransferManager.getInstance(this).getTaskLimit()), Toast.LENGTH_SHORT).show();
			return false;
		}

		GalleryItem galleryItem = galleryItemList.get(position);

		selectedGalleryItems.put(galleryItem.getId(), galleryItem);

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
		multiSelectTitle.setText(getString(R.string.gallery_num_selected, selectedGalleryItems.size()));
	}
}
