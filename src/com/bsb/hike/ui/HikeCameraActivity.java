package com.bsb.hike.ui;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;

import com.actionbarsherlock.internal.nineoldandroids.animation.Animator;
import com.actionbarsherlock.internal.nineoldandroids.animation.AnimatorListenerAdapter;
import com.actionbarsherlock.internal.nineoldandroids.animation.ObjectAnimator;
import com.bsb.hike.R;
import com.bsb.hike.ui.fragments.CameraFragment;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentManager;

public class HikeCameraActivity extends HikeAppStateBaseFragmentActivity implements OnClickListener
{
	private static final String FLASH_AUTO = "fauto";

	private static final String FLASH_ON = "fon";

	private static final String FLASH_OFF = "foff";

	private static final String TAG = "HikeCameraActivity";

	private static final int GALLERY_PICKER_REQUEST = 2;

	private CameraFragment cameraFragment;

	private boolean isUsingFFC = true;

	private View containerView;

	private Interpolator deceleratorInterp = new DecelerateInterpolator();
	
	private Interpolator overshootInterp = new OvershootInterpolator();

	private OrientationEventListener orientationListener;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		overridePendingTransition(R.anim.fade_in_animation, R.anim.fade_out_animation);

		super.onCreate(savedInstanceState);

		setContentView(R.layout.hike_camera_activity);

		cameraFragment = CameraFragment.newInstance(false);

		new Handler().postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				// replaceFragment(cameraFragment);
				flipCamera(containerView);
			}
		}, 1000);

		findViewById(R.id.btntakepic).setOnClickListener(HikeCameraActivity.this);

		ImageButton galleryBtn = (ImageButton) findViewById(R.id.btngallery);
		galleryBtn.setOnClickListener(HikeCameraActivity.this);

		ImageButton flipBtn = (ImageButton) findViewById(R.id.btnflip);
		flipBtn.setOnClickListener(HikeCameraActivity.this);

		findViewById(R.id.btntoggleflash).setOnClickListener(HikeCameraActivity.this);

		containerView = findViewById(R.id.container);

		orientationListener = new OrientationEventListener(HikeCameraActivity.this, SensorManager.SENSOR_DELAY_UI)
		{
			float state = 0;

			float stateLandscape1 = -90;

			float stateLandscape2 = 90;

			float statePortrait = 0;

			public void onOrientationChanged(int orientation)
			{
				if (orientation > 315 || orientation < 45)
				{
					if (state != statePortrait)
					{
						Log.d("Animating", "from" + state + "to" + 0);
						ObjectAnimator anim = ObjectAnimator.ofFloat(findViewById(R.id.btntakepic), "rotation", state, 0f);
						anim.setDuration(500); // Duration in milliseconds
						anim.setInterpolator(overshootInterp);
						anim.start();
						state = statePortrait;

					}
				}
				else if (orientation > 45 && orientation < 180)
				{
					if (state != stateLandscape1)
					{
						Log.d("Animating", "from" + state + "to" + stateLandscape1);
						ObjectAnimator anim = ObjectAnimator.ofFloat(findViewById(R.id.btntakepic), "rotation", state, stateLandscape1);
						anim.setInterpolator(overshootInterp);
						anim.setDuration(500); // Duration in milliseconds
						anim.start();
						state = stateLandscape1;

					}
				}
				else if (orientation > 180 && orientation < 315)
				{
					if (state != stateLandscape2)
					{
						Log.d("Animating", "from" + state + "to" + stateLandscape2);
						ObjectAnimator anim = ObjectAnimator.ofFloat(findViewById(R.id.btntakepic), "rotation", state, stateLandscape2);
						anim.setDuration(500); // Duration in milliseconds
						anim.setInterpolator(overshootInterp);
						anim.start();
						state = stateLandscape2;

					}
				}
			}
		};

	}

	@Override
	protected void onResume()
	{
		super.onResume();
		orientationListener.enable();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		orientationListener.disable();
	}

	@Override
	public void onClick(View v)
	{
		int viewId = v.getId();

		switch (viewId)
		{
		case R.id.btntakepic:
			cameraFragment.takePicture();
			break;
		case R.id.btngallery:
			// Open gallery
			Intent intent = new Intent();
			intent.setClass(HikeCameraActivity.this, PictureEditer.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(HikeCameraActivity.this, 0, intent, 0);
			Intent galleryPickerIntent = IntentManager.getHikeGalleryPickerIntent(HikeCameraActivity.this, false, pendingIntent);
			startActivityForResult(galleryPickerIntent, GALLERY_PICKER_REQUEST);
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

		// ObjectAnimator visToInvis = ObjectAnimator.ofFloat(containerView, "rotationY", 0f, -90f);
		ObjectAnimator visToInvis = ObjectAnimator.ofFloat(containerView, "alpha", 1f, 0f);
		visToInvis.setDuration(200);
		visToInvis.setInterpolator(deceleratorInterp);

		// final ObjectAnimator invisToVis = ObjectAnimator.ofFloat(containerView, "rotationY", 90f, 0f);
		final ObjectAnimator invisToVis = ObjectAnimator.ofFloat(containerView, "alpha", 0f, 1f);
		invisToVis.setDuration(1000);
		invisToVis.setInterpolator(deceleratorInterp);

		visToInvis.addListener(new AnimatorListenerAdapter()
		{
			@Override
			public void onAnimationEnd(Animator anim)
			{
				isUsingFFC = isUsingFFC ? false : true;
				cameraFragment = CameraFragment.newInstance(isUsingFFC);
				replaceFragment(cameraFragment);
				new Handler().postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						invisToVis.start();
					}
				}, 500);
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
	}

	private void replaceFragment(Fragment argFragment)
	{
		getSupportFragmentManager().beginTransaction().replace(R.id.container, argFragment).commit();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == GALLERY_PICKER_REQUEST)
		{
			if (resultCode == RESULT_OK)
			{
				// Access selected file
			}
		}
	}
}
