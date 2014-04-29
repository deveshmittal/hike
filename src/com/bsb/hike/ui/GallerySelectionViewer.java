package com.bsb.hike.ui;

import java.util.ArrayList;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.GalleryAdapter;
import com.bsb.hike.models.GalleryItem;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.smartImageLoader.GalleryImageLoader;
import com.bsb.hike.tasks.InitiateMultiFileTransferTask;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;

public class GallerySelectionViewer extends HikeAppStateBaseFragmentActivity implements OnItemClickListener, OnScrollListener, OnPageChangeListener, HikePubSub.Listener
{
	private GalleryAdapter gridAdapter;

	private GalleryPagerAdapter pagerAdapter;

	private GridView selectedGrid;

	private ViewPager selectedPager;

	private ArrayList<GalleryItem> galleryItems;

	private ArrayList<GalleryItem> galleryGridItems;

	private volatile InitiateMultiFileTransferTask fileTransferTask;

	private ProgressDialog progressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gallery_selection_viewer);

		Object object = getLastCustomNonConfigurationInstance();

		if (object instanceof InitiateMultiFileTransferTask)
		{
			fileTransferTask = (InitiateMultiFileTransferTask) object;
			progressDialog = ProgressDialog.show(this, null, getResources().getString(R.string.multi_file_creation));
		}

		galleryItems = getIntent().getParcelableArrayListExtra(HikeConstants.Extras.GALLERY_SELECTIONS);

		/*
		 * Added one for the extra null item.
		 */
		galleryGridItems = new ArrayList<GalleryItem>(galleryItems.size() + 1);
		galleryGridItems.addAll(galleryItems);
		/*
		 * Adding an empty item which will be used to add more images.
		 */
		galleryGridItems.add(null);

		selectedGrid = (GridView) findViewById(R.id.selection_grid);
		selectedPager = (ViewPager) findViewById(R.id.selection_pager);

		int sizeOfImage = getResources().getDimensionPixelSize(R.dimen.gallery_selection_item_size);

		int numColumns = Utils.getNumColumnsForGallery(getResources(), sizeOfImage);
		int actualSize = Utils.getActualSizeForGallery(getResources(), sizeOfImage, numColumns);

		gridAdapter = new GalleryAdapter(this, galleryGridItems, true, actualSize, null, true);

		selectedGrid.setNumColumns(numColumns);
		selectedGrid.setAdapter(gridAdapter);
		selectedGrid.setOnScrollListener(this);
		selectedGrid.setOnItemClickListener(this);

		pagerAdapter = new GalleryPagerAdapter();
		selectedPager.setAdapter(pagerAdapter);
		selectedPager.setOnPageChangeListener(this);

		setSelection(galleryItems.size() - 1);
		setupActionBar();

		HikeMessengerApp.getPubSub().addListener(HikePubSub.MULTI_FILE_TASK_FINISHED, this);
	}

	@Override
	public void onBackPressed()
	{
		startAddMoreGalleryIntent();
		super.onBackPressed();
	}

	private void startAddMoreGalleryIntent()
	{

		Intent intent = new Intent(this, GalleryActivity.class);

		intent.putParcelableArrayListExtra(HikeConstants.Extras.GALLERY_SELECTIONS, galleryItems);
		intent.putExtra(HikeConstants.Extras.SELECTED_BUCKET, getIntent().getParcelableExtra(HikeConstants.Extras.SELECTED_BUCKET));
		intent.putExtra(HikeConstants.Extras.MSISDN, getIntent().getStringExtra(HikeConstants.Extras.MSISDN));
		intent.putExtra(HikeConstants.Extras.ON_HIKE, getIntent().getBooleanExtra(HikeConstants.Extras.ON_HIKE, true));

		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		startActivity(intent);
	}

	@Override
	protected void onDestroy()
	{
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.MULTI_FILE_TASK_FINISHED, this);

		if (progressDialog != null)
		{
			progressDialog.dismiss();
			progressDialog = null;
		}
		super.onDestroy();
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		View doneBtn = actionBarView.findViewById(R.id.done_container);
		TextView postText = (TextView) actionBarView.findViewById(R.id.post_btn);

		doneBtn.setVisibility(View.VISIBLE);
		postText.setText(R.string.send);

		title.setText(R.string.preview);

		backContainer.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});

		doneBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				ArrayList<Pair<String, String>> fileDetails = new ArrayList<Pair<String, String>>(galleryItems.size());
				for (GalleryItem galleryItem : galleryItems)
				{
					fileDetails.add(new Pair<String, String> (galleryItem.getFilePath(), HikeFileType.toString(HikeFileType.IMAGE)));
				}
				String msisdn = getIntent().getStringExtra(HikeConstants.Extras.MSISDN);
				boolean onHike = getIntent().getBooleanExtra(HikeConstants.Extras.ON_HIKE, true);

				fileTransferTask = new InitiateMultiFileTransferTask(getApplicationContext(), fileDetails, msisdn, onHike);
				Utils.executeAsyncTask(fileTransferTask);

				progressDialog = ProgressDialog.show(GallerySelectionViewer.this, null, getResources().getString(R.string.multi_file_creation));
			}
		});

		actionBar.setCustomView(actionBarView);
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		if (fileTransferTask != null)
		{
			return fileTransferTask;
		}
		else
		{
			return null;
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{

	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState)
	{
		gridAdapter.setIsListFlinging(scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING);
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
	{
		GalleryItem galleryItem = galleryGridItems.get(position);
		if (galleryItem == null)
		{
			startAddMoreGalleryIntent();
		}
		else
		{
			setSelection(position);
		}
	}

	@Override
	public void onPageScrollStateChanged(int scrollState)
	{
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2)
	{
	}

	@Override
	public void onPageSelected(int position)
	{
		setSelection(position);
	}

	private void setSelection(int position)
	{
		gridAdapter.setSelectedItemPosition(position);

		selectedPager.setCurrentItem(position);
		selectedGrid.smoothScrollToPosition(position);
	}

	private class GalleryPagerAdapter extends PagerAdapter
	{
		LayoutInflater layoutInflater;

		GalleryImageLoader galleryImageLoader;

		int viewerHeight;

		int viewerWidth;

		public GalleryPagerAdapter()
		{
			layoutInflater = LayoutInflater.from(GallerySelectionViewer.this);
			galleryImageLoader = new GalleryImageLoader(GallerySelectionViewer.this);

			int padding = 2 * getResources().getDimensionPixelSize(R.dimen.gallery_selection_padding);

			viewerWidth = getResources().getDisplayMetrics().widthPixels - padding;
			viewerHeight = getResources().getDisplayMetrics().heightPixels - getResources().getDimensionPixelSize(R.dimen.st__action_bar_default_height)
					- getResources().getDimensionPixelSize(R.dimen.notification_bar_height) - getResources().getDimensionPixelSize(R.dimen.gallery_selected_grid_height) - padding;
		}

		@Override
		public int getCount()
		{
			return galleryItems.size();
		}

		@Override
		public boolean isViewFromObject(View view, Object object)
		{
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
			View page = layoutInflater.inflate(R.layout.gallery_preview_item, container, false);

			GalleryItem galleryItem = galleryItems.get(position);

			ImageButton removeImage = (ImageButton) page.findViewById(R.id.remove_selection);

			ImageView galleryImageView = (ImageView) page.findViewById(R.id.album_image);
			galleryImageView.setScaleType(ScaleType.FIT_CENTER);

			galleryImageLoader.loadImage(GalleryImageLoader.GALLERY_KEY_PREFIX + galleryItem.getId(), galleryImageView, false, true);

			setupButtonSpacing(galleryImageView, removeImage);

			removeImage.setTag(position);
			removeImage.setOnClickListener(removeSelectionClickListener);

			((ViewPager) container).addView(page);
			return page;
		}

		private void setupButtonSpacing(ImageView galleryImageView, ImageButton removeImage)
		{

			int drawableHeight = galleryImageView.getDrawable().getIntrinsicHeight();
			int drawableWidth = galleryImageView.getDrawable().getIntrinsicWidth();

			int imageWidth;
			int imageHeight;

			if (viewerHeight / drawableHeight <= viewerWidth / drawableWidth)
			{
				imageWidth = drawableWidth * viewerHeight / drawableHeight;
				imageHeight = viewerHeight;
			}
			else
			{
				imageHeight = drawableHeight * viewerWidth / drawableWidth;
				imageWidth = viewerWidth;
			}

			LayoutParams layoutParams = (LayoutParams) removeImage.getLayoutParams();
			layoutParams.leftMargin = imageWidth;
			layoutParams.bottomMargin = imageHeight/2;
		}
	}

	OnClickListener removeSelectionClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			int postion = (Integer) v.getTag();
			galleryItems.remove(postion);
			galleryGridItems.remove(postion + 1);

			gridAdapter.notifyDataSetChanged();
			pagerAdapter.notifyDataSetChanged();

			if(galleryItems.isEmpty())
			{
				startAddMoreGalleryIntent();
			}

			GallerySelectionViewer.this.selectedPager.setCurrentItem(postion == 0 ? 0 : postion - 1);
		}
	};

	@Override
	public void onEventReceived(String type, Object object)
	{
		super.onEventReceived(type, object);

		if (HikePubSub.MULTI_FILE_TASK_FINISHED.equals(type))
		{
			fileTransferTask = null;

			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					Intent intent = new Intent(GallerySelectionViewer.this, ChatThread.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					intent.putExtra(HikeConstants.Extras.MSISDN, getIntent().getStringExtra(HikeConstants.Extras.MSISDN));
					startActivity(intent);

					if (progressDialog != null)
					{
						progressDialog.dismiss();
						progressDialog = null;
					}
				}
			});
		}
	}
}
