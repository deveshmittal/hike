package com.bsb.hike.voip.view;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.voip.VoIPConstants;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.WindowManager;

public class CallRateActivity extends HikeAppStateBaseFragmentActivity implements IVoipCallRateListener
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		setContentView(R.layout.call_rate_activity);
		showCallRateFragment(new Bundle());
	}

	private void showCallRateFragment(final Bundle bundle) 
	{
		if(!isFragmentAdded(HikeConstants.VOIP_CALL_RATE_FRAGMENT_TAG) && !isFragmentAdded(HikeConstants.VOIP_CALL_ISSUES_FRAGMENT_TAG))
		{
			CallRateDialogFragment callRatePopup = new CallRateDialogFragment();
			callRatePopup.setArguments(bundle);

			FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
			fragmentTransaction.add(callRatePopup, HikeConstants.VOIP_CALL_RATE_FRAGMENT_TAG);
			fragmentTransaction.commitAllowingStateLoss();
		}
	}

	@Override
	public void showCallIssuesFragment(final Bundle bundle)
	{
		if(!isFragmentAdded(HikeConstants.VOIP_CALL_ISSUES_FRAGMENT_TAG))
		{
			CallIssuesDialogFragment callIssuesPopup = new CallIssuesDialogFragment();
			callIssuesPopup.setArguments(bundle);

			FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
			fragmentTransaction.add(callIssuesPopup, HikeConstants.VOIP_CALL_ISSUES_FRAGMENT_TAG);
			fragmentTransaction.commitAllowingStateLoss();
		}
	}
}
