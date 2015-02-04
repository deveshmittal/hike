package com.bsb.hike.analytics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Calendar;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import twitter4j.internal.http.HttpResponseCode;
import android.content.Context;
import android.content.SharedPreferences;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * @author rajesh
 *
 */
public class AnalyticsSender  
{
	private Context context;
	
	private static AnalyticsSender _instance;
	
	private HttpClient httpClient;

	private int retryCount;
	
	private long retryDelay = DELAY_BEFORE_RETRY;
	
	/** maximum retry counts */
	private static final int MAX_RETRY_COUNT = 3;
	
	/** delay before making first retry request(in ms) */
	private static final long DELAY_BEFORE_RETRY = 5000;		

	/**
	 * parameterized constructor of the class
	 * @param context application context
	 */
	private AnalyticsSender(Context context)
	{
		this.context = context.getApplicationContext();
	}
	
	/**
	 * static constructor of AnalyticsSender class
	 * @param context application context
	 * @return singleton instance of AnalyticsSender
	 */
	public static AnalyticsSender getInstance(Context context)
	{
		if(_instance == null)
		{
			synchronized (AnalyticsSender.class)
			{
				if(_instance == null)
				{
					_instance = new AnalyticsSender(context.getApplicationContext());
				}
			}
		}
		return _instance;
	}
	
	/**
	 * Used to check if there is analytics data logged on client
	 * @return true if there is analytics data logged, false otherwise
	 */
	public boolean isAnalyticsUploadReady()
	{
		boolean isPossible = true;
		
		// get files absolute paths
		String[] fileNames = HAManager.getFileNames(context);
		
		if(fileNames == null)
			isPossible = false;	
		
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Checking if analytics data exists or user is offline :" + isPossible);
		return isPossible;
	}
	
	/**
	 * This method sends the event log files to the server. Returns if no file is present or there is no connectivity
	 * Use this method carefully as its not thread-safe
	 */
	public void sendData()
	{
		// get files absolute paths
		String[] fileNames = HAManager.getFileNames(this.context);
		
		if(fileNames == null || !(Utils.isUserOnline(this.context)))
			return;
		
		int size = fileNames.length;
		
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Number of files to send :" + Integer.toString(size));
		
		for(int i=0; i<size; i++)
		{
			upload(fileNames[i]);
		}		
	}
	
	/**
	 * Used to retry upload of the file to the server
	 * true if upload should be retried, false otherwise
	 */
	private boolean retryUpload()
	{	
		if(retryCount < MAX_RETRY_COUNT)
		{
			retryCount++;
			
			retryDelay = retryCount * retryDelay;
			
			try
			{
				Thread.sleep(retryDelay);
			}
			catch(InterruptedException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Interrupted exception while thread sleeping before retry upload");
			}
		}
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "RETRY NUMBER ::" + retryCount);

		if(retryCount >= MAX_RETRY_COUNT)
		{
			resetRetryParams();
			
			return false;
		}
		return true;
	}
	
	/**
	 * Used to reset retry parameters
	 */
	private void resetRetryParams()
	{
		retryCount = 0;
		retryDelay = DELAY_BEFORE_RETRY;
	}
	
	/**
	 * starts the analytics data upload to server as per the set alarm and schedules the next alarm
	 */
	public void startUploadAndScheduleNextAlarm()
	{
		if(!isAnalyticsUploadReady())
			return;
		
		HAManager instance = HAManager.getInstance();
		long nextSchedule = instance.getWhenToSend();

		if(!Utils.isUserOnline(context))
		{			
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "User is offline.....");

			int freq = instance.getAnalyticsUploadRetryCount();

			// set alarm for post 4 hours if there is no network
			if(freq < AnalyticsConstants.ANALYTICS_UPLOAD_FREQUENCY)
			{
				instance.incrementAnalyticsUploadRetryCount();
				nextSchedule = System.currentTimeMillis() + AnalyticsConstants.UPLOAD_TIME_MULTIPLE * AnalyticsConstants.ONE_HOUR;				
				
				freq = instance.getAnalyticsUploadRetryCount();				
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "UPLOAD RETRY COUNT :" + freq);
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Alarm set for next 4 hours from current time.");
				
				// don't remove! added for testing purpose. 
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(nextSchedule);								
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Next alarm Date :" + cal.get(Calendar.DATE));
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Next alarm time :" + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE));
			}
			else
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Retries finished. Set alarm as per frequency now.");
				instance.resetAnalyticsUploadRetryCount();
								
				// TODO :This condition will make client keep trying to upload data if frequency is <= 8 hours when user is offline even after 2nd retry
				nextSchedule += instance.getAnalyticsSendFrequency() * AnalyticsConstants.ONE_MINUTE;
				instance.setNextSendTimeToPrefs(nextSchedule);

				// don't remove! added for testing purpose. 
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(nextSchedule);
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Next alarm Date :" + cal.get(Calendar.DATE));
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Next alarm time :" + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE));
			}
		}
		// regular path will set alarm for same time next day
		else
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "User is online.....");

			nextSchedule += instance.getAnalyticsSendFrequency() * AnalyticsConstants.ONE_MINUTE;
			instance.setNextSendTimeToPrefs(nextSchedule);
			
			// don't remove! added for testing purpose. 
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(nextSchedule);							
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Next alarm Date :" + cal.get(Calendar.DATE));
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Next alarm time :" + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE));			
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "---UPLOADING FROM ALARM ROUTE---");
			
			instance.sendAnalyticsData(false);			
		}
		HikeAlarmManager.setAlarm(context, nextSchedule, HikeAlarmManager.REQUESTCODE_HIKE_ANALYTICS, false);		
	}
	
	/**
	 * This method actually uploads a specific event file to the server
	 * @param fileName name of the file to be sent to the server
	 */
	private void upload(String fileName)
	{
		while(true)
		{
			String absolutePath = HAManager.getInstance().getAnalyticsDirectory() + File.separator + fileName;
	
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "uploading file :" + absolutePath);		
			
			SharedPreferences settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
			String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
			String uId = settings.getString(HikeMessengerApp.UID_SETTING, null);
	
			httpClient = new DefaultHttpClient();
			
			HttpPost postCall = new HttpPost(AccountUtils.analyticsUploadUrl);
	
			HttpResponse response = null;
			
			try 
			{
				postCall.addHeader("Connection", "Keep-Alive");
				postCall.addHeader("Content-Name", fileName);
				postCall.addHeader("Content-Encoding", "gzip");
				postCall.addHeader("Cookie", "user=" + token + ";UID=" + uId);
				postCall.setEntity(new FileEntity(new File(absolutePath), "text/plain"));

				response = httpClient.execute(postCall);			
			}
			catch(SocketTimeoutException e)
			{			
				if(!retryUpload())
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Exiting upload process....");
					return;
				}
			}
			catch(ConnectTimeoutException e)
			{
				if(!retryUpload())
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Exiting upload process....");
					return;
				}
			}
			catch (FileNotFoundException e) 
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "File not found during upload.");
				return;
			}
			catch(ClientProtocolException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "ClientProtocol exception during upload.");
				return;
			}
			catch(IOException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "io exception during upload.");
				return;
			}
			if (response != null)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "http response :" + response.getStatusLine());
				switch (response.getStatusLine().getStatusCode())
				{
				case HttpResponseCode.OK:
				{
					resetRetryParams();

					new File(absolutePath).delete();

					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "deleted file :" + fileName);
				}
					return;

				case HttpResponseCode.GATEWAY_TIMEOUT:
				case HttpResponseCode.SERVICE_UNAVAILABLE:
				case HttpResponseCode.INTERNAL_SERVER_ERROR:
				case HttpResponseCode.NOT_FOUND:

					if (!retryUpload())
					{
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Exiting upload process....");
						return;
					}
				}
			}
			else
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "null response while uploading file to server.");
				
				if(!retryUpload())
					return;
			}
		}
	}		
}

//class NetworkListener extends BroadcastReceiver 
//{
//	Context context;
//		
//	public NetworkListener(Context context) 
//	{
//		this.context = context;
//		
//		this.context.registerReceiver(this, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));		
//	}
//
//	@Override
//	public void onReceive(Context context, Intent intent) 
//	{		
//		if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
//		{
//			if(Utils.isUserOnline(context))
//			{
//				NetworkType networkType = FileTransferManager.getInstance(this.context).getNetworkType();
//				
//				if(networkType == NetworkType.WIFI)
//				{
//					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Wifi connectivity changed!");
//
//					if(AnalyticsSender.getFileNames(context) != null && Utils.isUserOnline(context))
//					{
//						new Thread(AnalyticsSender.getInstance(context)).start();
//					}
//				}
//			}
//		}				
//	}
//}
