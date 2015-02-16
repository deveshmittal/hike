package com.bsb.hike.ui.fragments;

import android.app.Activity;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.Utils;

public class DataConsumptionFragment extends SherlockFragment
{

	TextView dataInBytesView;
	int appId = HikeMessengerApp.getInstance().getApplicationInfo().uid;
	private Handler mHandler = new Handler();
	private long baseDataRecieved = HikeMessengerApp.getInstance().getBaseDataRecieved();
	private long baseDataSent = HikeMessengerApp.getInstance().getBaseDataTransmitted();
	
	private long lastDataRecieved = 0;
	private long lastDataSent = 0;
	
	Runnable fetchDataConsumedRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			long totalDataRecieved = (Utils.getTotalDataRecieved(appId) - baseDataRecieved);
			long totalDataSent = (Utils.getTotalDataSent(appId) - baseDataSent);
			long totalDataConsumed = totalDataRecieved + totalDataSent;

			long justRecieved = 0;
			long justSent = 0;

			if(lastDataRecieved>0)
			{
				justRecieved = totalDataRecieved - lastDataRecieved;
			}
			if(lastDataSent > 0)
			{
				justSent = totalDataSent - lastDataSent;
			}
			
			lastDataRecieved = totalDataRecieved;
			lastDataSent = totalDataSent;
			
			String string = "Total : " + totalDataConsumed + "\nRecieved : " + totalDataRecieved + "\nSent : " + totalDataSent;
			string += "\n\n Just recieved : " + justRecieved + "\nJust sent : " + justSent;
			
			dataInBytesView.setText(string);
			startRecordingDataConsumption(2000);
		}
	};
	
	public static DataConsumptionFragment newInstance() 
	{
	    return new DataConsumptionFragment();
	}
	
	public DataConsumptionFragment()
	{
	}
	
	@Override
	public void onAttach(Activity activity)
	{
		// TODO Auto-generated method stub
		super.onAttach(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		setRetainInstance(true);
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		View parent = inflater.inflate(R.layout.data_consumption, null);
		dataInBytesView = (TextView) parent.findViewById(R.id.data_bytes);
		View pauseBtn = parent.findViewById(R.id.pause_data);
		pauseBtn.setTag(true);
		
		pauseBtn.setOnClickListener(new OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				Boolean puased = (Boolean)(v.getTag());
				ImageView btn = (ImageView) v;
				if(puased)
				{
					//start data showing
					startRecordingDataConsumption(0);
					btn.setImageResource(R.drawable.emo_im_02_happy);
				}
				else
				{
					//pause data update
					btn.setImageResource(R.drawable.emo_im_108_unhappy);
					stopRecordingDataConsumption();
				}
				btn.setTag(!puased);
			}
		});
		return parent;
	}
	
	@Override
	public void onResume()
	{
		// TODO Auto-generated method stub
		startRecordingDataConsumption(0);
		super.onResume();
	}
	
	@Override
	public void onPause()
	{
		// TODO Auto-generated method stub
		stopRecordingDataConsumption();
		super.onPause();
	}
	
	private void startRecordingDataConsumption(int delay)
	{
		
		mHandler.postDelayed(fetchDataConsumedRunnable, delay);
	}
	
	private void stopRecordingDataConsumption()
	{
		mHandler.removeCallbacks(fetchDataConsumedRunnable);
	}
	
}
