package com.bsb.hike.filetransfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.Thread.State;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.filetransfer.FileTransferBase.FTState;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/* 
 * This manager will manage the upload and download (File Transfers).
 * A general thread pool is maintained which will be used for both downloads and uploads.
 * The manager will run on main thread hence an executor is used to delegate task to thread pool threads.
 */
public class FileTransferManager extends BroadcastReceiver
{
	private Context context;

	private ConcurrentHashMap<Long, FutureTask<FTResult>> fileTaskMap;

	private String HIKE_TEMP_DIR_NAME = "hikeTmp";

	private File HIKE_TEMP_DIR;

	// Constant variables
	private final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

	private final int CORE_POOL_SIZE = CPU_COUNT + 1;

	private final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;

	private final short KEEP_ALIVE_TIME = 60; // in seconds

	private static int minChunkSize = 8 * 1024;

	private static int maxChunkSize = 128 * 1024;
	
	private final int taskLimit;
	
	private final int TASK_OVERFLOW_LIMIT = 90;

	private ExecutorService pool;

	public static FileTransferManager _instance = null;

	private SharedPreferences settings;

	private Handler handler;

	public static String FT_CANCEL = "ft_cancel";

	public static String READ_FAIL = "read_fail";

	public static String UPLOAD_FAILED = "upload_failed";

	public static String UNABLE_TO_DOWNLOAD = "unable_to_download";

	public enum NetworkType
	{
		WIFI
		{
			@Override
			public int getMaxChunkSize()
			{
				return 1024 * 1024;
			}

			@Override
			public int getMinChunkSize()
			{
				return 256 * 1024;
			}
		},
		FOUR_G
		{
			@Override
			public int getMaxChunkSize()
			{
				return 512 * 1024;
			}

			@Override
			public int getMinChunkSize()
			{
				return 128 * 1024;
			}
		},
		THREE_G
		{
			@Override
			public int getMaxChunkSize()
			{
				return 256 * 1024;
			}

			@Override
			public int getMinChunkSize()
			{
				return 128 * 1024;
			}
		},
		TWO_G
		{
			@Override
			public int getMaxChunkSize()
			{
				return 32 * 1024;
			}

			@Override
			public int getMinChunkSize()
			{
				return 16 * 1024;
			}
		},
		NO_NETWORK
		{
			@Override
			public int getMaxChunkSize()
			{
				return 2 * 1024;
			}

			@Override
			public int getMinChunkSize()
			{
				return 1 * 1024;
			}
		};

		public abstract int getMaxChunkSize();

		public abstract int getMinChunkSize();
	};

	private FileTransferManager()
	{
		taskLimit = context.getResources().getInteger(R.integer.ft_limit);
	}

	private class MyThreadFactory implements ThreadFactory
	{
		private final AtomicInteger threadNumber = new AtomicInteger(1);

		@Override
		public Thread newThread(Runnable r)
		{
			int threadCount = threadNumber.getAndIncrement();
			Thread t = new Thread(r);
			// This approach reduces resource competition between the Runnable object's thread and the UI thread.
			t.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
			t.setName("FT Thread-" + threadCount);
			Logger.d(getClass().getSimpleName(), "Running FT thread : " + t.getName());
			return t;
		}
	}

	private class MyFutureTask extends FutureTask<FTResult>
	{
		private FileTransferBase task;

		public MyFutureTask(FileTransferBase callable)
		{
			super(callable);
			this.task = callable;
		}

		private FileTransferBase getTask()
		{
			return task;
		}

		@Override
		public void run()
		{
			Logger.d(getClass().getSimpleName(), "TimeCheck: Starting time : " + System.currentTimeMillis());
			super.run();
		}

		@Override
		protected void done()
		{
			super.done();
			FTResult result = FTResult.UPLOAD_FAILED;
			try
			{
				result = this.get();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			catch (ExecutionException e)
			{
				e.printStackTrace();
			}

			if (task instanceof DownloadFileTask)
				((DownloadFileTask) task).postExecute(result);
			else if (task instanceof UploadFileTask)
				((UploadFileTask) task).postExecute(result);
			else
				((UploadContactOrLocationTask) task).postExecute(result);

			Logger.d(getClass().getSimpleName(), "TimeCheck: Exiting  time : " + System.currentTimeMillis());
		}
	}

	private FileTransferManager(Context ctx)
	{
		BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>();
		fileTaskMap = new ConcurrentHashMap<Long, FutureTask<FTResult>>();
		// here choosing TimeUnit in seconds as minutes are added after api level 9
		pool = new ThreadPoolExecutor(2, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS, workQueue, new MyThreadFactory());
		context = ctx;
		HIKE_TEMP_DIR = context.getExternalFilesDir(HIKE_TEMP_DIR_NAME);
		handler = new Handler(context.getMainLooper());
		IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		context.registerReceiver(this, filter);
		taskLimit = context.getResources().getInteger(R.integer.ft_limit);
	}
	

	public static FileTransferManager getInstance(Context context)
	{
		if (_instance == null)
		{
			synchronized (FileTransferManager.class)
			{
				if (_instance == null)
					_instance = new FileTransferManager(context.getApplicationContext());
			}
		}
		return _instance;
	}

	public boolean isFileTaskExist(long msgId)
	{
		return fileTaskMap.containsKey(msgId);
	}

	public ConvMessage getMessage(long msgId)
	{
		FutureTask<FTResult> obj = fileTaskMap.get(msgId);
		if (obj != null)
		{
			Object msg = ((MyFutureTask) obj).getTask().getUserContext();
			if (msg != null)
			{
				return ((ConvMessage) msg);
			}
		}
		return null;
	}

	public void downloadFile(File destinationFile, String fileKey, long msgId, HikeFileType hikeFileType, Object userContext, boolean showToast)
	{
		if (isFileTaskExist(msgId)){
			validateFilePauseState(msgId);
			return;
		}
		if(taskOverflowLimitAchieved())
			return;
		
		settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
		String uId = settings.getString(HikeMessengerApp.UID_SETTING, null);
		DownloadFileTask task = new DownloadFileTask(handler, fileTaskMap, context, destinationFile, fileKey, msgId, hikeFileType, userContext, showToast, token, uId);
		try
		{
			MyFutureTask ft = new MyFutureTask(task);
			fileTaskMap.put(msgId, ft);
			pool.execute(ft); // this future is used to cancel pause the task
		}
		catch (RejectedExecutionException rjEx)
		{
			// handle this properly
		}

	}

	public void uploadFile(String msisdn, File sourceFile, String fileKey, String fileType, HikeFileType hikeFileType, boolean isRec, boolean isForwardMsg, boolean isRecipientOnHike,
			long recordingDuration)
	{
		if(taskOverflowLimitAchieved())
			return;
		
		settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
		String uId = settings.getString(HikeMessengerApp.UID_SETTING, null);
		UploadFileTask task = new UploadFileTask(handler, fileTaskMap, context, token, uId, msisdn, sourceFile, fileKey, fileType, hikeFileType, isRec, isForwardMsg, isRecipientOnHike,
				recordingDuration);
		// UploadFileTask task = new UploadFileTask(handler, fileTaskMap, context, token, uId, convMessage, isRecipientOnHike);
		MyFutureTask ft = new MyFutureTask(task);
		task.setFutureTask(ft);
		pool.execute(ft);
	}

	public void uploadFile(ConvMessage convMessage, boolean isRecipientOnHike)
	{
		if (isFileTaskExist(convMessage.getMsgID())){
			validateFilePauseState(convMessage.getMsgID());
			return;
		}
		if(taskOverflowLimitAchieved())
			return;
		
		settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
		String uId = settings.getString(HikeMessengerApp.UID_SETTING, null);
		UploadFileTask task = new UploadFileTask(handler, fileTaskMap, context, token, uId, convMessage, isRecipientOnHike);
		MyFutureTask ft = new MyFutureTask(task);
		task.setFutureTask(ft);
		pool.execute(ft);
	}

	public void uploadFile(Uri picasaUri, HikeFileType hikeFileType, String msisdn, boolean isRecipientOnHike)
	{
		if(taskOverflowLimitAchieved())
			return;
		if(hikeFileType != HikeFileType.IMAGE)
		{
			handler.post(new Runnable()
			{
				@Override
				public void run()
				{
					Toast.makeText(context, R.string.unknown_msg, Toast.LENGTH_SHORT).show();
				}
			});
			return;
		}
		
		settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
		String uId = settings.getString(HikeMessengerApp.UID_SETTING, null);
		UploadFileTask task = new UploadFileTask(handler, fileTaskMap, context, token, uId, picasaUri, hikeFileType, msisdn, isRecipientOnHike);
		MyFutureTask ft = new MyFutureTask(task);
		task.setFutureTask(ft);
		pool.execute(ft);
	}

	public void uploadLocation(String msisdn, double latitude, double longitude, int zoomLevel, boolean isRecipientOnhike)
	{
		if(taskOverflowLimitAchieved())
			return;
		
		settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
		String uId = settings.getString(HikeMessengerApp.UID_SETTING, null);
		UploadContactOrLocationTask task = new UploadContactOrLocationTask(handler, fileTaskMap, context, msisdn, latitude, longitude, zoomLevel, isRecipientOnhike, token, uId);
		MyFutureTask ft = new MyFutureTask(task);
		task.setFutureTask(ft);
		pool.execute(ft);
	}

	public void uploadContact(String msisdn, JSONObject contactJson, boolean isRecipientOnhike)
	{
		if(taskOverflowLimitAchieved())
			return;
		
		settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
		String uId = settings.getString(HikeMessengerApp.UID_SETTING, null);
		UploadContactOrLocationTask task = new UploadContactOrLocationTask(handler, fileTaskMap, context, msisdn, contactJson, isRecipientOnhike, token, uId);
		MyFutureTask ft = new MyFutureTask(task);
		task.setFutureTask(ft);
		pool.execute(ft);
	}

	public void uploadContactOrLocation(ConvMessage convMessage, boolean uploadingContact, boolean isRecipientOnhike)
	{
		if (isFileTaskExist(convMessage.getMsgID())){
			validateFilePauseState(convMessage.getMsgID());
			return;
		}
		if(taskOverflowLimitAchieved())
			return;
		
		settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
		String uId = settings.getString(HikeMessengerApp.UID_SETTING, null);
		UploadContactOrLocationTask task = new UploadContactOrLocationTask(handler, fileTaskMap, context, convMessage, uploadingContact, isRecipientOnhike, token, uId);
		MyFutureTask ft = new MyFutureTask(task);
		task.setFutureTask(ft);
		pool.execute(ft);
	}

	public void removeTask(long msgId)
	{
		fileTaskMap.remove(msgId);
	}

	/*
	 * This function will close down the executor service, and usually be called after unlink or delete account
	 */
	public void shutDownAll()
	{
		fileTaskMap.clear();
		pool.shutdown();
		deleteAllFTRFiles();
		_instance = null;
	}

	public void cancelTask(long msgId, File mFile, boolean sent)
	{
		FileSavedState fss;
		if (sent)
			fss = getUploadFileState(msgId, mFile);
		else
			fss = getDownloadFileState(msgId, mFile);

		if (fss.getFTState() == FTState.IN_PROGRESS || fss.getFTState() == FTState.PAUSED || fss.getFTState() == FTState.INITIALIZED)
		{
			FutureTask<FTResult> obj = fileTaskMap.get(msgId);
			if (obj != null)
			{
				((MyFutureTask) obj).getTask().setState(FTState.CANCELLED);
			}
			Logger.d(getClass().getSimpleName(), "deleting state file" + msgId);
			deleteStateFile(msgId, mFile);
			if (!sent)
			{
				Logger.d(getClass().getSimpleName(), "deleting tempFile" + msgId);
				File tempDownloadedFile = new File(getHikeTempDir(), mFile.getName() + ".part");
				if (tempDownloadedFile != null && tempDownloadedFile.exists())
					tempDownloadedFile.delete();

			}
		}
	}

	public void pauseTask(long msgId)
	{
		FutureTask<FTResult> obj = fileTaskMap.get(msgId);
		if (obj != null)
		{
			FileTransferBase task = ((MyFutureTask) obj).getTask();
			task.setPausedProgress(task._bytesTransferred);
			task.setState(FTState.PAUSED);
			LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED));
			Logger.d(getClass().getSimpleName(), "pausing the task....");
		}
	}

	// this will be used when user deletes corresponding chat bubble
	public void deleteStateFile(long msgId, File mFile)
	{
		String fName = mFile.getName() + ".bin." + msgId;
		File f = new File(HIKE_TEMP_DIR, fName);
		if (f != null)
			f.delete();
	}

	// this will be used when user deletes account or unlink account
	public void deleteAllFTRFiles()
	{
		if (HIKE_TEMP_DIR != null && HIKE_TEMP_DIR.listFiles() != null)
			for (File f : HIKE_TEMP_DIR.listFiles())
			{
				if (f != null)
				{
					try
					{
						f.delete();
					}
					catch (Exception e)
					{
						Logger.e(getClass().getSimpleName(), "Exception while deleting state file : ", e);
					}
				}
			}
	}

	// this function gives the state of downloading for a file
	public FileSavedState getDownloadFileState(long msgId, File mFile)
	{
		if (isFileTaskExist(msgId))
		{
			FutureTask<FTResult> obj = fileTaskMap.get(msgId);
			if (obj != null)
			{
				return new FileSavedState(((MyFutureTask) obj).getTask()._state, ((MyFutureTask) obj).getTask()._totalSize, ((MyFutureTask) obj).getTask()._bytesTransferred);
			}
			else
			{
				return new FileSavedState(FTState.IN_PROGRESS, 0, 0);
			}
		}
		else
			return getDownloadFileState(mFile, msgId);
	}

	/*
	 * here mFile is the file provided by the caller (original file) null : represents unhandled error and should be handled accordingly
	 */
	public FileSavedState getDownloadFileState(File mFile, long msgId)
	{
		if (mFile == null) // @GM only for now. Has to be handled properly
			return new FileSavedState();

		FileSavedState fss = null;
		if (mFile.exists())
		{
			fss = new FileSavedState(FTState.COMPLETED, 100, 100);
		}
		else
		{
			try
			{
				String fName = mFile.getName() + ".bin." + msgId;
				File f = new File(HIKE_TEMP_DIR, fName);
				if (!f.exists())
					return new FileSavedState();
				FileInputStream fileIn = new FileInputStream(f);
				ObjectInputStream in = new ObjectInputStream(fileIn);
				fss = (FileSavedState) in.readObject();
				in.close();
				fileIn.close();
			}
			catch (IOException i)
			{
				i.printStackTrace();
			}
			catch (ClassNotFoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return fss != null ? fss : new FileSavedState();
	}

	// this function gives the state of uploading for a file
	public FileSavedState getUploadFileState(long msgId, File mFile)
	{
		Logger.d(getClass().getSimpleName(), "Returning state for message ID : " + msgId);
		if (isFileTaskExist(msgId))
		{
			FutureTask<FTResult> obj = fileTaskMap.get(msgId);
			if (obj != null)
			{
				Logger.d(getClass().getSimpleName(), "Returning: " + ((MyFutureTask) obj).getTask()._state.toString());
				return new FileSavedState(((MyFutureTask) obj).getTask()._state, ((MyFutureTask) obj).getTask()._totalSize, ((MyFutureTask) obj).getTask()._bytesTransferred);
			}
			else
			{
				Logger.d(getClass().getSimpleName(), "Returning: in_prog");
				return new FileSavedState(FTState.IN_PROGRESS, 0, 0);
			}
		}
		else
			return getUploadFileState(mFile, msgId);
	}

	public FileSavedState getUploadFileState(File mFile, long msgId)
	{
		Logger.d(getClass().getSimpleName(), "Returning from second call");
		if (mFile == null) // @GM only for now. Has to be handled properly
			return new FileSavedState();

		FileSavedState fss = null;

		try
		{
			String fName = mFile.getName() + ".bin." + msgId;
			Logger.d(getClass().getSimpleName(), fName);
			File f = new File(HIKE_TEMP_DIR, fName);
			if (!f.exists())
			{
				return new FileSavedState();
			}
			FileInputStream fileIn = new FileInputStream(f);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			fss = (FileSavedState) in.readObject();
			in.close();
			fileIn.close();
		}
		catch (IOException i)
		{
			i.printStackTrace();
		}
		catch (ClassNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.e(getClass().getSimpleName(), "Exception while reading state file : ", e);
		}
		return fss != null ? fss : new FileSavedState();

	}

	// Fetches the type of internet connection the device is using
	public NetworkType getNetworkType()
	{
		int networkType = -1;
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		// Contains all the information about current connection
		NetworkInfo info = cm.getActiveNetworkInfo();
		if (info != null)
		{
			if (!info.isConnected())
				return NetworkType.NO_NETWORK;
			// If device is connected via WiFi
			if (info.getType() == ConnectivityManager.TYPE_WIFI)
				return NetworkType.WIFI; // return 1024 * 1024;
			else
				networkType = info.getSubtype();
		}

		// There are following types of mobile networks
		switch (networkType)
		{
		case TelephonyManager.NETWORK_TYPE_HSUPA: // ~ 1-23 Mbps
		case TelephonyManager.NETWORK_TYPE_LTE: // ~ 10+ Mbps // API level 11
		case TelephonyManager.NETWORK_TYPE_HSPAP: // ~ 10-20 Mbps // API level 13
		case TelephonyManager.NETWORK_TYPE_EVDO_B: // ~ 5 Mbps // API level 9
			return NetworkType.FOUR_G;
		case TelephonyManager.NETWORK_TYPE_EVDO_0: // ~ 400-1000 kbps
		case TelephonyManager.NETWORK_TYPE_EVDO_A: // ~ 600-1400 kbps
		case TelephonyManager.NETWORK_TYPE_HSDPA: // ~ 2-14 Mbps
		case TelephonyManager.NETWORK_TYPE_HSPA: // ~ 700-1700 kbps
		case TelephonyManager.NETWORK_TYPE_UMTS: // ~ 400-7000 kbps
		case TelephonyManager.NETWORK_TYPE_EHRPD: // ~ 1-2 Mbps // API level 11
			return NetworkType.THREE_G;
		case TelephonyManager.NETWORK_TYPE_1xRTT: // ~ 50-100 kbps
		case TelephonyManager.NETWORK_TYPE_CDMA: // ~ 14-64 kbps
		case TelephonyManager.NETWORK_TYPE_EDGE: // ~ 50-100 kbps
		case TelephonyManager.NETWORK_TYPE_GPRS: // ~ 100 kbps
		case TelephonyManager.NETWORK_TYPE_IDEN: // ~25 kbps // API level 8
		case TelephonyManager.NETWORK_TYPE_UNKNOWN:
		default:
			return NetworkType.TWO_G;
		}
	}

	private void setChunkSize(NetworkType networkType)
	{
		maxChunkSize = networkType.getMaxChunkSize();
		minChunkSize = networkType.getMinChunkSize();
	}

	private void resumeAllTasks()
	{
		for (Entry<Long, FutureTask<FTResult>> entry : fileTaskMap.entrySet())
		{
			if (entry != null)
			{
				FutureTask<FTResult> obj = entry.getValue();
				if (obj != null)
				{
					Thread t = ((MyFutureTask) obj).getTask().getThread();
					if (t != null)
					{
						if (t.getState() == State.TIMED_WAITING)
						{
							Logger.d(getClass().getSimpleName(), "interrupting the task: " + t.toString());
							t.interrupt();
						}
					}
				}
			}

		}
	}

	public int getMaxChunkSize()
	{
		return maxChunkSize;
	}

	public int getMinChunkSize()
	{
		return minChunkSize;
	}

	public File getHikeTempDir()
	{
		return HIKE_TEMP_DIR;
	}

	/**
	 * caller should handle the 0 return value
	 * */

	public int getFTProgress(long msgId, File mFile, boolean sent)
	{
		FileSavedState fss;
		if (isFileTaskExist(msgId))
		{
			FutureTask<FTResult> obj = fileTaskMap.get(msgId);
			if (obj != null)
			{
				return ((MyFutureTask) obj).getTask().progressPercentage;
			}
		}

		if (sent)
			fss = getUploadFileState(mFile, msgId);
		else
			fss = getDownloadFileState(mFile, msgId);

		if (fss.getFTState() == FTState.IN_PROGRESS || fss.getFTState() == FTState.PAUSED || fss.getFTState() == FTState.ERROR)
		{
			if (fss.getTotalSize() > 0)
			{
				long temp = fss.getTransferredSize();
				temp *= 100;
				temp /= fss.getTotalSize();
				return (int) temp;
			}

			else
				return 0;
		}
		else if (fss.getFTState() == FTState.COMPLETED)
		{
			return 100;
		}
		else
			return 0;
	}

	public int getChunkSize(long msgId)
	{
		if (isFileTaskExist(msgId))
		{
			FutureTask<FTResult> obj = fileTaskMap.get(msgId);
			if (obj != null)
			{
				return ((MyFutureTask) obj).getTask().chunkSize;
			}
		}
		return 0;
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
		{
			Logger.d(getClass().getSimpleName(), "Connectivity Change Occured");
			// if network available then proceed
			if (Utils.isUserOnline(context))
			{
				NetworkType networkType = getNetworkType();
				setChunkSize(networkType);
				resumeAllTasks();
			}
		}
	}
	
	public int remainingTransfers()
	{
		if(taskLimit > fileTaskMap.size())
			return (taskLimit - fileTaskMap.size());
		else
			return 0;
	}

	public int getTaskLimit()
	{
		return taskLimit;
	}

	public boolean taskOverflowLimitAchieved()
	{
		if(fileTaskMap.size() >= TASK_OVERFLOW_LIMIT)
			return true;
		else
			return false;
	}
	
	private void validateFilePauseState(long msgId){
		FutureTask<FTResult> obj = fileTaskMap.get(msgId);
		if (obj != null)
		{
			FileTransferBase task = ((MyFutureTask) obj).getTask();
			if(task.getPausedProgress() == task._bytesTransferred && task._state == FTState.PAUSED){
				task.setState(FTState.IN_PROGRESS);
				LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED));
			}
		}
	}
}
