package com.bsb.hike.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.bsb.hike.utils.Utils;

public class GaugeView extends View
{
	private Paint actualCreditsGauge;
	private Paint maxCreditsOuterGauge;
	private Paint maxCreditsInnerGauge;
	private int actualCreditsAngle;
	private int maxCreditsAngle;
	RectF actualCreditsArea;
	RectF maxCreditsOuterArea;
	RectF maxCreditsInnerArea;

	private static final int START_ANGLE = 139;

	private static final int OFFSET = 7;

	private static final int MAX_CREDIT_TO_SHOW = 600;

	private static final int MAX_ANGLE = 262;

	private static final int ACTUAL_CREDIT_GAUGE_INCREMENT = 4;

	private static final int MAX_CREDIT_GAUGE_INCREMENT = 8;

	public GaugeView(Context context, AttributeSet attrs, int defStyle) 
	{
		super(context, attrs, defStyle);

		actualCreditsGauge = new Paint();
		actualCreditsGauge.setColor(0xff76b952);
		actualCreditsGauge.setStyle(Paint.Style.STROKE);
		actualCreditsGauge.setStrokeWidth(22.5f * Utils.densityMultiplier);
		actualCreditsGauge.setAntiAlias(true);

		maxCreditsOuterGauge = new Paint();
		maxCreditsOuterGauge.setColor(0xff76b952);
		maxCreditsOuterGauge.setStyle(Paint.Style.STROKE);
		maxCreditsOuterGauge.setStrokeWidth(2.67f * Utils.densityMultiplier);
		maxCreditsOuterGauge.setAntiAlias(true);

		maxCreditsInnerGauge = new Paint();
		maxCreditsInnerGauge.setColor(0xff76b952);
		maxCreditsInnerGauge.setStyle(Paint.Style.STROKE);
		maxCreditsInnerGauge.setStrokeWidth(2.67f * Utils.densityMultiplier);
		maxCreditsInnerGauge.setAntiAlias(true);

		actualCreditsArea = new RectF((int)(15 * Utils.densityMultiplier), (int)(27 * Utils.densityMultiplier), (int)(214 * Utils.densityMultiplier), (int)(225 * Utils.densityMultiplier));
		maxCreditsOuterArea = new RectF((int)(6 * Utils.densityMultiplier), (int)(18 * Utils.densityMultiplier), (int)(224 * Utils.densityMultiplier), (int)(235 * Utils.densityMultiplier));
		maxCreditsInnerArea = new RectF((int)(25 * Utils.densityMultiplier), (int)(37 * Utils.densityMultiplier), (int)(205 * Utils.densityMultiplier), (int)(217 * Utils.densityMultiplier));
	}

	public GaugeView(Context context, AttributeSet attrs) 
	{
		this(context, attrs, 0);
	}

	public void setActualCreditsAngle(int actualCredits)
	{
		this.actualCreditsAngle = creditsToAngle(actualCredits);
	}

	public void setMaxCreditsAngle(int maxCredits)
	{
		this.maxCreditsAngle = creditsToAngle(maxCredits);
	}

	public GaugeView(Context context) 
	{
		this(context, null, 0);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if(isInEditMode())
		{
			return;
		}

		canvas.drawArc(actualCreditsArea, START_ANGLE - OFFSET, actualCreditsAngle + OFFSET, false, actualCreditsGauge);
		canvas.drawArc(maxCreditsOuterArea, START_ANGLE, maxCreditsAngle, false, maxCreditsOuterGauge);
		canvas.drawArc(maxCreditsInnerArea, START_ANGLE, maxCreditsAngle, false, maxCreditsInnerGauge);

		super.onDraw(canvas);
	}

	private int creditsToAngle(int credits)
	{
		if(credits >= MAX_CREDIT_TO_SHOW)
		{
			return MAX_ANGLE;
		}

		float ratio = (credits * 100)/MAX_CREDIT_TO_SHOW;
		int angle = (int) (ratio * MAX_ANGLE)/100;
		return angle;
	}
}