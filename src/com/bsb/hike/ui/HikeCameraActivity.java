package com.bsb.hike.ui;

import java.io.File;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageButton;

import com.actionbarsherlock.internal.nineoldandroids.animation.Animator;
import com.actionbarsherlock.internal.nineoldandroids.animation.AnimatorListenerAdapter;
import com.actionbarsherlock.internal.nineoldandroids.animation.ObjectAnimator;
import com.bsb.hike.R;
import com.bsb.hike.photos.PictureEditer;
import com.bsb.hike.ui.fragments.CameraFragment;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;

@SuppressWarnings("deprecation")
public class HikeCameraActivity extends HikeAppStateBaseFragmentActivity implements OnClickListener
{
	private static final String FLASH_AUTO = "fauto";

	private static final String FLASH_ON = "fon";

	private static final String FLASH_OFF = "foff";

	private CameraFragment cameraFragment;

	private boolean isUsingFFC;

	private View containerView;

	private Interpolator decelerator = new DecelerateInterpolator();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		getSupportActionBar().hide();

		setContentView(R.layout.hike_camera_activity);

		cameraFragment = CameraFragment.newInstance(false);

		getSupportFragmentManager().beginTransaction().replace(R.id.container, cameraFragment).commit();

		findViewById(R.id.btntakepic).setOnClickListener(HikeCameraActivity.this);

		ImageButton galleryBtn = (ImageButton) findViewById(R.id.btngallery);
		galleryBtn.setOnClickListener(HikeCameraActivity.this);

		ImageButton flipBtn = (ImageButton) findViewById(R.id.btnflip);
		flipBtn.setOnClickListener(HikeCameraActivity.this);

		findViewById(R.id.btntoggleflash).setOnClickListener(HikeCameraActivity.this);

		containerView = findViewById(R.id.container);
	}

	@Override
	public void onClick(View v)
	{
		int viewId = v.getId();

		switch (viewId)
		{
		case R.id.btntakepic:
			cameraFragment.takePicture();
			File savedImage = cameraFragment.getHost().getLastSavedFile();
			// Start editor activity here
			Intent i = new Intent(HikeCameraActivity.this, PictureEditer.class);   
			i.putExtra("FilePath", savedImage.getAbsolutePath());
			break;
		case R.id.btngallery:
			// Open gallery
			break;
		case R.id.btnflip:
			flipCamera(v);
			break;
		case R.id.btntoggleflash:

			ImageButton btnFlash = (ImageButton) v;

			String tag = (String) btnFlash.getTag();

			if (tag.equals(FLASH_AUTO))
			{
				btnFlash.setTag(FLASH_OFF);
				btnFlash.setImageDrawable(getResources().getDrawable(R.drawable.flashoff));
				cameraFragment.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			}
			else if (tag.equals(FLASH_OFF))
			{
				btnFlash.setTag(FLASH_ON);
				btnFlash.setImageDrawable(getResources().getDrawable(R.drawable.flashon));
				cameraFragment.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
			}
			else if (tag.equals(FLASH_ON))
			{
				btnFlash.setTag(FLASH_AUTO);
				btnFlash.setImageDrawable(getResources().getDrawable(R.drawable.flashauto));
				cameraFragment.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
			}

			break;

		}
	}

	private void flipCamera(final View v)
	{
		v.setEnabled(false);

		ObjectAnimator visToInvis = ObjectAnimator.ofFloat(containerView, "rotationY", 0f, -90f);
		visToInvis.setDuration(200);
		visToInvis.setInterpolator(decelerator);

		final ObjectAnimator invisToVis = ObjectAnimator.ofFloat(containerView, "rotationY", 90f, 0f);
		invisToVis.setDuration(200);
		invisToVis.setInterpolator(decelerator);

		visToInvis.addListener(new AnimatorListenerAdapter()
		{
			@Override
			public void onAnimationEnd(Animator anim)
			{
				isUsingFFC = isUsingFFC ? false : true;
				cameraFragment = CameraFragment.newInstance(isUsingFFC);
				getSupportFragmentManager().beginTransaction().replace(R.id.container, cameraFragment).commit();
				new Handler().postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						invisToVis.start();
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
						{
							containerView.animate().alpha(1f).setDuration(200).start();
						}
					}
				}, 0);
			}
		});

		invisToVis.addListener(new AnimatorListenerAdapter()
		{

			@Override
			public void onAnimationEnd(Animator animation)
			{
				v.setEnabled(true);
			}

		});

		visToInvis.start();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
		{
			containerView.animate().alpha(0.0f).setDuration(150).start();
		}
	}
}
