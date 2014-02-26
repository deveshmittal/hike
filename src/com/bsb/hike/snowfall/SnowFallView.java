package com.bsb.hike.snowfall;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;

import com.bsb.hike.R;
import com.bsb.hike.utils.Utils;

public class SnowFallView extends View
{
	private int snow_flake_count = 10;

	private final List<Drawable> drawables = new ArrayList<Drawable>();

	private int[][] coords;

	private final Drawable snow_flake;

	private final Drawable snow_flake_large;

	private final Drawable snow_flake_trans;

	public SnowFallView(Context context)
	{
		super(context);
		setFocusable(true);
		setFocusableInTouchMode(true);

		snow_flake = context.getResources().getDrawable(R.drawable.snow_flake);
		snow_flake.setBounds(0, 0, snow_flake.getIntrinsicWidth(), snow_flake.getIntrinsicHeight());

		snow_flake_large = context.getResources().getDrawable(R.drawable.snow_flake_large);
		snow_flake_large.setBounds(0, 0, snow_flake_large.getIntrinsicWidth(), snow_flake_large.getIntrinsicHeight());

		snow_flake_trans = context.getResources().getDrawable(R.drawable.snow_flake_trans);
		snow_flake_trans.setBounds(0, 0, snow_flake_trans.getIntrinsicWidth(), snow_flake_trans.getIntrinsicHeight());
	}

	@Override
	protected void onSizeChanged(int width, int height, int oldw, int oldh)
	{
		super.onSizeChanged(width, height, oldw, oldh);
		Random random = new Random();
		Interpolator interpolator = new LinearInterpolator();

		snow_flake_count = Math.max(width, height) / 8;
		coords = new int[snow_flake_count][];
		drawables.clear();
		int durationMultiplier = (int) (6 / Utils.densityMultiplier);
		for (int i = 0; i < snow_flake_count; i++)
		{
			Animation animation = new TranslateAnimation(0, height / 10 - random.nextInt(height / 5), 0, height + 50);
			animation.setDuration(durationMultiplier * height + random.nextInt((int) (durationMultiplier * height)));
			animation.setRepeatCount(Animation.INFINITE);
			animation.initialize(10, 10, 10, 10);
			animation.setInterpolator(interpolator);

			int startYDisp = (int) (45 * Utils.densityMultiplier);
			coords[i] = new int[] { random.nextInt(width), -startYDisp };
			drawables.add(new AnimateDrawable(snow_flake, snow_flake_large, snow_flake_trans, animation));
			animation.setStartOffset(random.nextInt(8 * height));
			animation.setFillBefore(false);

			animation.startNow();
		}
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		for (int i = 0; i < snow_flake_count; i++)
		{
			Drawable drawable = drawables.get(i);
			canvas.save();
			canvas.translate(coords[i][0], coords[i][1]);
			drawable.draw(canvas);
			canvas.restore();
		}
		invalidate();
	}

}