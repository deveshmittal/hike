package com.bsb.hike.voip.view;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.voip.VoIPUtils;

public class CallRatePopup extends SherlockDialogFragment
{
	private static int rating = -1;
	
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		View view = inflater.inflate(R.layout.voip_call_rate_popup, container, false);
		getDialog().setCanceledOnTouchOutside(true);
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
				submitRating();
				dismiss();
			}
		});

		final LinearLayout starsContainer = (LinearLayout)view.findViewById(R.id.star_rate_container);
		int childCount = starsContainer.getChildCount();
		
		if(rating!=-1)
		{
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
		try
		{
			JSONObject data = new JSONObject();
			data.put(HikeConstants.SUB_TYPE, HikeConstants.UI_EVENT);

			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_TYPE, HikeConstants.LogEvent.CLICK);
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.VOIP_CALL_RATE_POPUP_SUBMIT);
			metadata.put(HikeConstants.VOIP_CALL_RATING, rating+1);
			metadata.put(HikeConstants.VOIP_CALL_ID, VoIPUtils.getLastCallId());

			data.put(HikeConstants.METADATA, metadata);

			Utils.sendLogEvent(data);
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
	}
	
}
