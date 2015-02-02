package com.bsb.hike.voip.view;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.voip.VoIPConstants;

public class CallIssuesPopup extends SherlockDialogFragment
{
	public CallIssuesPopup(){
	}

	private final String TAG = "CallIssuesPopup";

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_CustomDialog);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) 
	{
		View view = inflater.inflate(R.layout.call_issues_popup, container, false);
		setCancelable(true);

		OnClickListener issueListener = new OnClickListener() {
			
			@Override
			public void onClick(View view) {
				view.setSelected(!view.isSelected());
			}
		};

		TableLayout issuesContainer = (TableLayout) view.findViewById(R.id.issues_container);
		int i, rowCount = issuesContainer.getChildCount();
		for(i=0; i<rowCount; i++)
		{
			TableRow row = (TableRow) issuesContainer.getChildAt(i);
			int j, colCount = row.getChildCount();
			for(j=0; j<colCount; j++)
			{
				row.getChildAt(j).setOnClickListener(issueListener);
			}
		}

		view.findViewById(R.id.call_issues_submit).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				submitIssues();
				dismiss();
			}
		});
		
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) 
	{
		super.onSaveInstanceState(outState);
	}

	private void submitIssues()
	{
		Bundle bundle = getArguments();
		int isCallInitiator = -1, callId = -1, rating = -1;
		if(bundle!=null)
		{
			isCallInitiator = bundle.getInt(VoIPConstants.IS_CALL_INITIATOR);
			callId = bundle.getInt(VoIPConstants.CALL_ID);
			rating = bundle.getInt(VoIPConstants.CALL_RATING);
		}

		try
		{
			JSONObject data = new JSONObject();
			data.put(HikeConstants.SUB_TYPE, HikeConstants.UI_EVENT);

			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_TYPE, HikeConstants.LogEvent.VOIP);
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.VOIP_CALL_RATE_POPUP_SUBMIT);
			metadata.put(VoIPConstants.Analytics.CALL_RATING, rating);
			metadata.put(VoIPConstants.Analytics.CALL_ID, callId);
			metadata.put(VoIPConstants.Analytics.IS_CALLER, isCallInitiator);

			TableLayout issuesContainer = (TableLayout) getView().findViewById(R.id.issues_container);
			int i, rowCount = issuesContainer.getChildCount();
			for(i=0; i<rowCount; i++)
			{
				TableRow row = (TableRow) issuesContainer.getChildAt(i);
				int j, colCount = row.getChildCount();
				for(j=0; j<colCount; j++)
				{
					View v = row.getChildAt(j);
					String tag = (String) v.getTag();
					if(v.isSelected() && tag!=null)
					{
						metadata.put(tag, 1);
					}
				}
			}

			data.put(HikeConstants.METADATA, metadata);

			Utils.sendLogEvent(data);
		}
		catch (JSONException e)
		{
			Logger.w(TAG, "Invalid json");
		}
	}
}
