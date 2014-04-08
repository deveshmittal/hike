package com.bsb.hike.ui.fragments;

import java.io.File;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.ProfileAdapter;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.smartImageLoader.ImageWorker;
import com.bsb.hike.tasks.ProfileImageLoader;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class ImageViewerFragment extends SherlockFragment implements LoaderCallbacks<Boolean>, OnClickListener
{

	ImageView imageView;

	private ProgressDialog mDialog;

	private String mappedId;

	private String key;

	private boolean isStatusImage;

	private String basePath;

	private boolean hasCustomImage;

	private String fileName;

	private String url;

	private IconLoader iconLoader;

	private int imageSize;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		iconLoader = new IconLoader(getActivity(), 180);
		imageSize = this.getActivity().getResources().getDimensionPixelSize(R.dimen.timeine_big_picture_size);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View parent = inflater.inflate(R.layout.image_viewer, null);
		imageView = (ImageView) parent.findViewById(R.id.image);
		imageView.setOnClickListener(this);

		return parent;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		mappedId = getArguments().getString(HikeConstants.Extras.MAPPED_ID);

		isStatusImage = getArguments().getBoolean(HikeConstants.Extras.IS_STATUS_IMAGE);

		url = getArguments().getString(HikeConstants.Extras.URL);

		basePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;

		hasCustomImage = true;
		key = mappedId;
		if (!isStatusImage)
		{
			int idx = key.indexOf(ProfileAdapter.PROFILE_PIC_SUFFIX);
			if (idx > 0)
				key = key.substring(0, idx);
			hasCustomImage = HikeUserDatabase.getInstance().hasIcon(key);
		}

		if (hasCustomImage)
		{
			fileName = Utils.getProfileImageFileName(key);

			File file = new File(basePath, fileName);

			boolean downloadImage = true;
			if (file.exists())
			{
				BitmapDrawable drawable = HikeMessengerApp.getLruCache().get(mappedId);
				if (drawable == null)
				{
					Bitmap b = ImageWorker.decodeSampledBitmapFromFile(basePath + "/" + fileName, imageSize, imageSize, HikeMessengerApp.getLruCache());
					if (b != null)
					{
						drawable = Utils.getBitmapDrawable(this.getActivity().getApplicationContext().getResources(), b);
						Logger.e(getClass().getSimpleName(), "Decode from file is returning null bitmap.");
					}
					else
					{
						// as bitmap is drawable, this means big image is either not downloaded or is corrupt or is currently in the downloading state,
						// till then show blurred image if present
						
						//TODO : Sid
					}
				}

				downloadImage = false;
				HikeMessengerApp.getLruCache().putInCache(mappedId, drawable);
				imageView.setImageDrawable(drawable);

			}
			if (downloadImage)
			{
				iconLoader.loadImage(mappedId, imageView);

				// imageView.setImageDrawable(IconCacheManager.getInstance()
				// .getIconForMSISDN(mappedId));

				getLoaderManager().initLoader(0, null, this);

				mDialog = ProgressDialog.show(getActivity(), null, getResources().getString(R.string.downloading_image));
				mDialog.setCancelable(true);
			}
		}
		else
		{
			imageView.setBackgroundResource(Utils.getDefaultAvatarResourceId(key, false));
			imageView.setImageResource(Utils.isGroupConversation(mappedId) ? R.drawable.ic_default_avatar_group_hires : R.drawable.ic_default_avatar_hires);
		}

	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		dismissProgressDialog();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		menu.clear();
	}

	@Override
	public Loader<Boolean> onCreateLoader(int id, Bundle arguments)
	{
		return new ProfileImageLoader(getActivity(), key, fileName, hasCustomImage, isStatusImage, url);
	}

	@Override
	public void onLoadFinished(Loader<Boolean> arg0, Boolean arg1)
	{
		dismissProgressDialog();

		if (!isAdded())
		{
			return;
		}

		File file = new File(basePath, fileName);

		BitmapDrawable drawable = null;
		if (file.exists())
		{
			drawable = Utils.getBitmapDrawable(this.getActivity().getApplicationContext().getResources(),
					ImageWorker.decodeSampledBitmapFromFile(basePath + "/" + fileName, imageSize, imageSize, HikeMessengerApp.getLruCache()));
			imageView.setImageDrawable(drawable);
		}

		Log.d(getClass().getSimpleName(), "Putting in cache mappedId : " + mappedId);
		/*
		 * Putting downloaded image bitmap in cache.
		 */
		if (drawable != null)
		{
			HikeMessengerApp.getLruCache().putInCache(mappedId, drawable);
		}

		if (isStatusImage)
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.LARGER_UPDATE_IMAGE_DOWNLOADED, null);
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.LARGER_IMAGE_DOWNLOADED, null);
	}

	@Override
	public void onLoaderReset(Loader<Boolean> arg0)
	{
		dismissProgressDialog();
	}

	private void dismissProgressDialog()
	{
		if (mDialog != null)
		{
			mDialog.dismiss();
			mDialog = null;
		}
	}

	@Override
	public void onClick(View v)
	{
		/*
		 * This object can become null, if the method is called when the fragment is not attached with the activity. In that case we do nothing and return.
		 */
		if (getActivity() == null)
		{
			return;
		}
		getActivity().onBackPressed();
	}
}
