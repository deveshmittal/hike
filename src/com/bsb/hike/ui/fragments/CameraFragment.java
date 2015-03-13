package com.bsb.hike.ui.fragments;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.photos.HikeCameraHost;
import com.bsb.hike.photos.HikePhotosListener;
import com.bsb.hike.ui.HikeCameraActivity;
import com.bsb.hike.ui.PictureEditer;
import com.commonsware.cwac.camera.CameraView;
import com.commonsware.cwac.camera.PictureTransaction;
import com.commonsware.cwac.camera.ZoomTransaction;

public class CameraFragment extends SherlockFragment
{
	private static final String KEY_USE_FFC = "av1ku";

	private CameraView cameraView = null;

	private HikeCameraHost host = null;

	private boolean useFFC;

	public static CameraFragment newInstance(boolean useFFC)
	{
		CameraFragment f = new CameraFragment();

		Bundle args = new Bundle();

		args.putBoolean(KEY_USE_FFC, useFFC);

		f.setArguments(args);

		return (f);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Fragment#onCreateView(android.view. LayoutInflater, android.view.ViewGroup, android.os.Bundle)
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		cameraView = new CameraView(getActivity());
		useFFC = getArguments().getBoolean(KEY_USE_FFC);
		cameraView.setHost(getHost());
		return (cameraView);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Fragment#onResume()
	 */
	@Override
	public void onResume()
	{
		super.onResume();

		cameraView.onResume();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Fragment#onPause()
	 */
	@Override
	public void onPause()
	{
		if (isRecording())
		{
			try
			{
				stopRecording();
			}
			catch (IOException e)
			{
				// TODO: get to developers
				Log.e(getClass().getSimpleName(), "Exception stopping recording in onPause()", e);
			}
		}

		cameraView.onPause();

		super.onPause();
	}

	/**
	 * Use this if you are overriding onCreateView() and are inflating a layout containing your CameraView, to tell the fragment the CameraView, so the fragment can help manage it.
	 * You do not need to call this if you are allowing the fragment to create its own CameraView instance.
	 * 
	 * @param cameraView
	 *            the CameraView from your inflated layout
	 */
	protected void setCameraView(CameraView cameraView)
	{
		this.cameraView = cameraView;
	}

	/**
	 * @return the CameraHost instance you want to use for this fragment, where the default is an instance of the stock SimpleCameraHost.
	 */
	public HikeCameraHost getHost()
	{
		if (host == null)
		{
			host = HikeCameraHost.getInstance(useFFC);
			host.setOnImageSavedListener(photoListener);
		}

		return (host);
	}

	private HikePhotosListener photoListener = new HikePhotosListener()
	{
		@Override
		public void onFailure()
		{

		}

		@Override
		public void onComplete(final File f)
		{

		}

		@Override
		public void onComplete(final Bitmap bmp)
		{
			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if (getActivity() instanceof HikeCameraActivity)
					{
						Bitmap bimp = ((HikeCameraActivity) getActivity()).processSquareBitmap(bmp);

						OutputStream outStream = null;

						File file = getHost().getPhotoPath();
						try
						{
							// make a new bitmap from your file
							outStream = new FileOutputStream(file);
							bimp.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
							outStream.flush();
							outStream.close();
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
						Log.e("file", "" + file);

						final String filePath = file.getAbsolutePath();

						new Handler().postDelayed(new Runnable()
						{

							@Override
							public void run()
							{
								Intent i = new Intent(getActivity(), PictureEditer.class);
								i.putExtra(HikeConstants.HikePhotos.FILENAME, filePath);
								getActivity().startActivity(i);
							}
						}, 100);

					}
				}
			});

		}
	};

	private String flashMode = Camera.Parameters.FLASH_MODE_AUTO;

	/**
	 * Call this (or override getHost()) to supply the CameraHost used for most of the detailed interaction with the camera.
	 * 
	 * @param host
	 *            a CameraHost instance, such as a subclass of SimpleCameraHost
	 */
	public void setHost(HikeCameraHost host)
	{
		this.host = host;
	}

	/**
	 * Call this to take a picture and get access to a byte array of data as a result (e.g., to save or stream).
	 */
	public void takePicture()
	{
		final PictureTransaction xact = new PictureTransaction(getHost());
		xact.mirrorFFC(true);
		xact.useSingleShotMode(false);
		xact.needBitmap(true);
		xact.needByteArray(true);

		if (!useFFC)
			xact.flashMode(flashMode);

		try
		{
			takePicture(xact);
		}
		catch (IllegalStateException ise)
		{
			new Handler().postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						takePicture(xact);
					}
					catch (IllegalStateException ise2)
					{
						// Do nothing
					}
				}
			}, 500);
		}
	}

	/**
	 * Call this to take a picture.
	 * 
	 * @param needBitmap
	 *            true if you need to be passed a Bitmap result, false otherwise
	 * @param needByteArray
	 *            true if you need to be passed a byte array result, false otherwise
	 */
	public void takePicture(boolean needBitmap, boolean needByteArray)
	{
		cameraView.takePicture(needBitmap, needByteArray);
	}

	/**
	 * Call this to take a picture.
	 * 
	 * @param xact
	 *            PictureTransaction with configuration data for the picture to be taken
	 */
	public void takePicture(PictureTransaction xact)
	{
		cameraView.takePicture(xact);
	}

	/**
	 * @return true if we are recording video right now, false otherwise
	 */
	public boolean isRecording()
	{
		return (cameraView == null ? false : cameraView.isRecording());
	}

	/**
	 * Call this to begin recording video.
	 * 
	 * @throws Exception
	 *             all sorts of things could go wrong
	 */
	public void record() throws Exception
	{
		cameraView.record();
	}

	/**
	 * Call this to stop the recording triggered earlier by a call to record()
	 * 
	 * @throws Exception
	 *             all sorts of things could go wrong
	 */
	public void stopRecording() throws IOException
	{
		cameraView.stopRecording();
	}

	/**
	 * @return the orientation of the screen, in degrees (0-360)
	 */
	public int getDisplayOrientation()
	{
		return (cameraView.getDisplayOrientation());
	}

	/**
	 * Call this to lock the camera to landscape mode (with a parameter of true), regardless of what the actual screen orientation is.
	 * 
	 * @param enable
	 *            true to lock the camera to landscape, false to allow normal rotation
	 */
	public void lockToLandscape(boolean enable)
	{
		cameraView.lockToLandscape(enable);
	}

	/**
	 * Call this to begin an auto-focus operation (e.g., in response to the user tapping something to focus the camera).
	 */
	public void autoFocus()
	{
		if (cameraView != null)
			cameraView.autoFocus();
	}

	/**
	 * Call this to cancel an auto-focus operation that had been started via a call to autoFocus().
	 */
	public void cancelAutoFocus()
	{
		cameraView.cancelAutoFocus();
	}

	/**
	 * @return true if auto-focus is an option on this device, false otherwise
	 */
	public boolean isAutoFocusAvailable()
	{
		if(cameraView!=null)
		return (cameraView.isAutoFocusAvailable());
		else return false;
	}

	/**
	 * If you are in single-shot mode and are done processing a previous picture, call this to restart the camera preview.
	 */
	public void restartPreview()
	{
		cameraView.restartPreview();
	}

	/**
	 * @return the name of the current flash mode, as reported by Camera.Parameters
	 */
	public String getFlashMode()
	{
		return (cameraView.getFlashMode());
	}

	/**
	 * Call this to begin populating a ZoomTransaction, with the eventual goal of changing the camera's zoom level.
	 * 
	 * @param level
	 *            a value from 0 to getMaxZoom() (called on Camera.Parameters), to indicate how tight the zoom should be (0 indicates no zoom)
	 * @return a ZoomTransaction to configure further and eventually call go() to actually do the zooming
	 */
	public ZoomTransaction zoomTo(int level)
	{
		return (cameraView.zoomTo(level));
	}

	/**
	 * Calls startFaceDetection() on the CameraView, which in turn calls startFaceDetection() on the underlying camera.
	 */
	public void startFaceDetection()
	{
		cameraView.startFaceDetection();
	}

	/**
	 * Calls stopFaceDetection() on the CameraView, which in turn calls startFaceDetection() on the underlying camera.
	 */
	public void stopFaceDetection()
	{
		cameraView.stopFaceDetection();
	}

	public boolean doesZoomReallyWork()
	{
		return (cameraView.doesZoomReallyWork());
	}

	public void setFlashMode(String mode)
	{
		// cameraView.setFlashMode(mode);
		flashMode = mode;
	}

	public CameraView getCameraView()
	{
		return cameraView;
	}

}
