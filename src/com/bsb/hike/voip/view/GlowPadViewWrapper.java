package com.bsb.hike.voip.view;

import com.bsb.hike.R;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.voip.VoIPConstants;
import com.fima.glowpadview.GlowPadView;
import com.fima.glowpadview.GlowPadView.OnTriggerListener;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

public class GlowPadViewWrapper extends GlowPadView implements GlowPadView.OnTriggerListener
{
	private CallActions mCallActions;

	private String TAG = "VoIPGlowPad";

	public GlowPadViewWrapper(Context context) 
	{
		super(context);
	}

	public GlowPadViewWrapper(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
	}

	@Override
	protected void onFinishInflate() 
	{
		super.onFinishInflate();
		setOnTriggerListener(this);
	}

	public void setCallActionsListener(CallActions mCallActions)
	{
		this.mCallActions = mCallActions;
	}

	@Override
	public void onGrabbed(View v, int handle) {
		Logger.d(TAG,"Call glow pad view - Grabbed");
	}

	@Override
	public void onReleased(View v, int handle) 
	{
		Logger.d(TAG,"Call glow pad view - onRelease");
		ping();
	}

	@Override
	public void onTrigger(View v, int target) 
	{
		int resId = getResourceIdForTarget(target);
		if(resId == R.drawable.ic_item_call_hang)
		{
			mCallActions.declineCall();
		}
		else if(resId == R.drawable.ic_item_call_pick)
		{
			mCallActions.acceptCall();
		}
	}

	@Override
	public void onGrabbedStateChange(View v, int handle) {
		Logger.d(TAG,"Call glow pad view - Grabbed state changed");
	}

	@Override
	public void onFinishFinalAnimation() {
		Logger.d(TAG,"Call glow pad view - Finish final anim");
	}
}
