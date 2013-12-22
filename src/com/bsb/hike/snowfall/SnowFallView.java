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

public class SnowFallView extends View {
	private int snow_flake_count = 10;
	private final List<Drawable> drawables = new ArrayList<Drawable>();
	private int[][] coords;
	private final Drawable snow_flake;
	
	public SnowFallView(Context context) {
		super(context);
		setFocusable(true);
		setFocusableInTouchMode(true);

		snow_flake = context.getResources().getDrawable(R.drawable.snow_flake);
		snow_flake.setBounds(0, 0, snow_flake.getIntrinsicWidth(), snow_flake
		        .getIntrinsicHeight());
	}

	@Override
	protected void onSizeChanged(int width, int height, int oldw, int oldh) {
		super.onSizeChanged(width, height, oldw, oldh);
		Random random = new Random();
		Interpolator interpolator = new LinearInterpolator();

		snow_flake_count = Math.max(width, height) / 12;
		coords = new int[snow_flake_count][];
		drawables.clear();
		int durationMultiplier = (int)(6/Utils.densityMultiplier);
		
		for (int i = 0; i < snow_flake_count; i++) {
			//coordinates where snow will be generated
			coords[i] = new int[] { random.nextInt(width), random.nextInt(height-300) };
			/* 
			 * x and y displacement of particles
			 * negative x => particle will go in left and vice versa
			 * negative y => particle will up and vice versa
			 */
			int toXDelta = 0;
			int toYDelta = 0;
			if(coords[i][0]>width/2){
				toXDelta = 200-random.nextInt(width/2);
			} else{
				toXDelta = -200+random.nextInt(width/2);
			}
			
			toYDelta = -400+random.nextInt(height);
			Animation animation = new TranslateAnimation(0,toXDelta , 0, toYDelta);
			/* 
			 * duration of the particle animation ==> this will decide the speed
			 * ideally it should be based on actual displacement otherwise some 
			 * particle would be very slow
			 */
			animation.setDuration(height+ random.nextInt((int)(height)));
			animation.setRepeatCount(Animation.INFINITE);
			animation.initialize(10, 10, 10, 10);
			animation.setInterpolator(interpolator);

			drawables.add(new AnimateDrawable(snow_flake, animation));
			animation.setFillBefore(false);
			
			animation.startNow();
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		for (int i = 0; i < snow_flake_count; i++) {
			Drawable drawable = drawables.get(i);
			canvas.save();
			canvas.translate(coords[i][0], coords[i][1]);
			drawable.draw(canvas);
			canvas.restore();
		}
		invalidate();
	}

} 