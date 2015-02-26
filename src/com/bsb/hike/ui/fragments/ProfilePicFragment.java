package com.bsb.hike.ui.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.internal.nineoldandroids.animation.Animator;
import com.actionbarsherlock.internal.nineoldandroids.animation.AnimatorListenerAdapter;
import com.actionbarsherlock.internal.nineoldandroids.animation.FloatEvaluator;
import com.actionbarsherlock.internal.nineoldandroids.animation.ObjectAnimator;
import com.actionbarsherlock.internal.nineoldandroids.animation.ValueAnimator;
import com.actionbarsherlock.internal.nineoldandroids.animation.ValueAnimator.AnimatorUpdateListener;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.view.HoloCircularProgress;
import com.bsb.hike.view.RoundedImageView;

public class ProfilePicFragment extends SherlockFragment
{
	private View mFragmentView;

	private HoloCircularProgress mCircularProgress;

	float mCurrentProgress = 0;

	private TextView text1;

	private TextView text2;

	private Interpolator deceleratorInterp = new DecelerateInterpolator();

	private boolean isPaused;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		mFragmentView = inflater.inflate(R.layout.profile_pic_fragment, null);

		mCircularProgress = (HoloCircularProgress) mFragmentView.findViewById(R.id.circular_progress);

		mCircularProgress.setProgress(0);

		Bundle bundle = getArguments();

		String imagePath = bundle.getString(HikeConstants.HikePhotos.FILENAME);

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.RGB_565;
		Bitmap bmp = BitmapFactory.decodeFile(imagePath, options);
		if (bmp != null)
		{
			((RoundedImageView) mFragmentView.findViewById(R.id.circular_image_view)).setImageBitmap(bmp);
		}

		text1 = (TextView) mFragmentView.findViewById(R.id.text1);

		text2 = (TextView) mFragmentView.findViewById(R.id.text2);

		startUpload();

		return mFragmentView;
	}

	private void startUpload()
	{

		changeTextWithAnimation(text1, getString(R.string.photo_dp_saving));

		changeTextWithAnimation(text2, "");

		mCircularProgress.setProgressColor(getResources().getColor(R.color.photos_circular_progress_blue));

		mCircularProgress.resetProgress();

		mFragmentView.findViewById(R.id.retryButton).setVisibility(View.GONE);

		updateProgress(10f);
	}

	private void updateProgress(float i)
	{

		ValueAnimator mAnim = ObjectAnimator.ofFloat(mCurrentProgress, mCurrentProgress + i);
		mAnim.setInterpolator(deceleratorInterp);
		mAnim.setEvaluator(new FloatEvaluator());
		mAnim.setDuration(2000);
		mAnim.addUpdateListener(new AnimatorUpdateListener()
		{
			@Override
			public void onAnimationUpdate(ValueAnimator animation)
			{
				Float value = (Float) animation.getAnimatedValue();
				mCircularProgress.setProgress(value / 100f);
			}
		});
		mAnim.start();

		mCurrentProgress += i;

		if (mCurrentProgress < 100f)
		{
			HikeHandlerUtil.getInstance().postRunnableWithDelay(new Runnable()
			{
				@Override
				public void run()
				{
					ProfilePicFragment.this.getActivity().runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							updateProgress(10);
						}
					});
				}
			}, 2000);
		}
		else
		{
			changeTextWithAnimation(text1, getString(R.string.photo_dp_saved));

			changeTextWithAnimation(text2, getString(R.string.photo_dp_saved_sub));

			HikeHandlerUtil.getInstance().postRunnableWithDelay(new Runnable()
			{
				@Override
				public void run()
				{
					ProfilePicFragment.this.getActivity().runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							if (isPaused)
							{
								return;
							}

							changeTextWithAnimation(text1, getString(R.string.photo_dp_save_error));

							changeTextWithAnimation(text2, getString(R.string.photo_dp_save_error_sub));

							mCircularProgress.setProgressColor(getResources().getColor(R.color.photos_circular_progress_red));

							mFragmentView.findViewById(R.id.retryButton).setVisibility(View.VISIBLE);

							mFragmentView.findViewById(R.id.retryButton).setOnClickListener(new View.OnClickListener()
							{
								@Override
								public void onClick(View v)
								{
									mCurrentProgress = 0.0f;
									startUpload();
								}
							});
						}
					});
				}
			}, 5000);
		}
	}

	private void changeTextWithAnimation(final TextView tv, final String newText)
	{
		ObjectAnimator visToInvis = ObjectAnimator.ofFloat(tv, "alpha", 1f, 0.2f);
		visToInvis.setDuration(250);
		visToInvis.setInterpolator(deceleratorInterp);
		visToInvis.addListener(new AnimatorListenerAdapter()
		{
			@Override
			public void onAnimationEnd(Animator animation)
			{
				super.onAnimationEnd(animation);
				tv.setText(newText);
				ObjectAnimator invisToVis = ObjectAnimator.ofFloat(tv, "alpha", 0.2f, 1f);
				invisToVis.setDuration(250);
				invisToVis.setInterpolator(deceleratorInterp);
				invisToVis.start();
			}
		});
		visToInvis.start();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		isPaused = true;
	}

	@Override
	public void onResume()
	{
		super.onResume();
		isPaused = false;
	}
}
