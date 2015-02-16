package com.bsb.hike.voip.view;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.voip.VoIPConstants;

public class CallRatePopup extends SherlockDialogFragment
{
	private int rating = -1;
	
	private final String TAG = "CallRatePopup";

	public CallRatePopup(){
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_CustomDialog);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) 
	{
		View view = inflater.inflate(R.layout.voip_call_rate_popup, container, false);
		setCancelable(true);

		view.findViewById(R.id.call_rate_dismiss).setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					dismiss();
				}
			});

		view.findViewById(R.id.call_rate_submit).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(rating >= 3)
				{
					submitRating();
					dismiss();
				}
				else if(rating >= 0)
				{
					showCallIssuesFragment(getArguments());
					dismiss();
				}
			}
		});

		final LinearLayout starsContainer = (LinearLayout)view.findViewById(R.id.star_rate_container);
		int childCount = starsContainer.getChildCount();
		
		if(bundle!=null)
		{
			rating = bundle.getInt("rating");
			for(int i=0; i<=rating; i++)
			{
				starsContainer.getChildAt(i).setSelected(true);
			}
		}

		OnClickListener rateListener = new OnClickListener() {
			
			@Override
			public void onClick(View view) {
				int index = starsContainer.indexOfChild(view);
				if(index < rating)
				{
					for(int i=index+1; i<=rating; i++)
					{
						starsContainer.getChildAt(i).setSelected(false);
					}
				}
				else
				{
					for(int i=0; i<=index; i++)
					{
						starsContainer.getChildAt(i).setSelected(true);
					}
				}
				rating = index;
			}
		};

		for(int i=0;i<childCount;i++)
		{
			starsContainer.getChildAt(i).setOnClickListener(rateListener);
		}
		return view;
		 
	}

	private void submitRating()
	{
		Bundle bundle = getArguments();
		int isCallInitiator = -1, callId = -1, network = -1;
		String toMsisdn = "";
		if(bundle!=null)
		{
			isCallInitiator = bundle.getInt(VoIPConstants.IS_CALL_INITIATOR);
			callId = bundle.getInt(VoIPConstants.CALL_ID);
			network = bundle.getInt(VoIPConstants.CALL_NETWORK_TYPE);
			toMsisdn = bundle.getString(VoIPConstants.PARTNER_MSISDN);
		}

		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_TYPE, HikeConstants.LogEvent.VOIP);
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.VOIP_CALL_RATE_POPUP_SUBMIT);
			metadata.put(VoIPConstants.Analytics.CALL_RATING, rating+1);
			metadata.put(VoIPConstants.Analytics.CALL_ID, callId);
			metadata.put(VoIPConstants.Analytics.IS_CALLER, isCallInitiator);
			metadata.put(VoIPConstants.Analytics.NETWORK_TYPE, network);
			metadata.put(AnalyticsConstants.TO, toMsisdn);

			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
		}
		catch (JSONException e)
		{
			Logger.w(TAG, "Invalid json");
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) 
	{
		super.onSaveInstanceState(outState);
		outState.putInt("rating", rating);
	}

	private void showCallIssuesFragment(Bundle bundle)
	{
		bundle.putInt(VoIPConstants.CALL_RATING, rating+1);
		((IVoipCallListener)getSherlockActivity()).onVoipCallEnd(bundle, HikeConstants.VOIP_CALL_ISSUES_FRAGMENT_TAG);
	}
}
