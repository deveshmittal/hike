package com.bsb.hike.ui.fragments;

import java.io.File;

import android.app.ProgressDialog;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
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
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.tasks.ProfileImageLoader;
import com.bsb.hike.utils.Utils;

public class ImageViewerFragment extends SherlockFragment implements LoaderCallbacks<Boolean>, OnClickListener
{

	ImageView imageView;

	private ProgressDialog mDialog;

	private String mappedId;

	private boolean isStatusImage;

	private String basePath;

	private boolean hasCustomImage;

	private String fileName;

	private String url;

	private IconLoader iconLoader;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		iconLoader = new IconLoader(getActivity(), 180);
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
		if (!isStatusImage)
		{
			hasCustomImage = HikeUserDatabase.getInstance().hasIcon(mappedId);
		}

		if (hasCustomImage)
		{
			fileName = Utils.getProfileImageFileName(mappedId);

			File file = new File(basePath, fileName);

			boolean downloadImage = true;
			if (file.exists())
			{
				Drawable drawable = BitmapDrawable.createFromPath(basePath + "/" + fileName);
				if (drawable != null)
				{
					downloadImage = false;
					imageView.setImageDrawable(drawable);
				}
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
			imageView.setBackgroundResource(Utils.getDefaultAvatarResourceId(mappedId, false));
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
		return new ProfileImageLoader(getActivity(), mappedId, fileName, hasCustomImage, isStatusImage, url);
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

		if (file.exists())
		{
			imageView.setImageDrawable(BitmapDrawable.createFromPath(basePath + "/" + fileName));
		}

		/*
		 * Removing the smaller icon in cache.
		 */
		HikeMessengerApp.getLruCache().remove(mappedId);

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
