package com.bsb.hike.filetransfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.filetransfer.FileTransferBase.FTState;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.utils.FileTransferCancelledException;
import com.bsb.hike.utils.Utils;

/* 
 * This manager will manage the upload and download (File Transfers).
 * A general thread pool is maintained which will be used for both downloads and uploads.
 * The manager will run on main thread hence an executor is used to delegate task to thread pool threads.
 */
public class FileTransferManager
{
	private Context context;

	private ConcurrentHashMap<Long, FutureTask<FTResult>> fileTaskMap;

	private String HIKE_TEMP_DIR_NAME = "hikeTmp";

	private File HIKE_TEMP_DIR;

	// Constant variables
	private int THREAD_POOL_SIZE = 10;

	private final short KEEP_ALIVE_TIME = 60; // in seconds

	private static int minChunkSize = 10 * 1024;

	private static int maxChunkSize = 100 * 1024;

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
				return 64 * 1024;
			}

			@Override
			public int getMinChunkSize()
			{
				return 32 * 1024;
			}
		};

		public abstract int getMaxChunkSize();

		public abstract int getMinChunkSize();
	};

	private FileTransferManager()
	{
	}

	private class MyThreadFactory implements ThreadFactory
	{
		private final AtomicInteger threadNumber = new AtomicInteger(1);

		@Override
		public Thread newThread(Runnable r)
		{
			Thread t = new Thread(r);
			// This approach reduces resource competition between the Runnable object's thread and the UI thread.
			t.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
			t.setName("FT Thread-" + threadNumber.getAndIncrement());
			Log.d(getClass().getSimpleName(), "Running FT thread : " + t.getName());
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
			Log.d(getClass().getSimpleName(), "TimeCheck: Starting time : " + System.currentTimeMillis());
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

			Log.d(getClass().getSimpleName(), "TimeCheck: Exiting  time : " + System.currentTimeMillis());
		}
	}

	private FileTransferManager(Context ctx)
	{
		BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>();
		fileTaskMap = new ConcurrentHashMap<Long, FutureTask<FTResult>>();
		// here choosing TimeUnit in seconds as minutes are added after api level 9
		THREAD_POOL_SIZE = (Runtime.getRuntime().availableProcessors()) * 2;
		if (THREAD_POOL_SIZE < 2)
			THREAD_POOL_SIZE = 2;
		pool = new ThreadPoolExecutor(2, THREAD_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS, workQueue, new MyThreadFactory());
		context = ctx;
		HIKE_TEMP_DIR = context.getExternalFilesDir(HIKE_TEMP_DIR_NAME);
		handler = new Handler(context.getMainLooper());
		setChunkSize();
	}

	public static FileTransferManager getInstance(Context context)
	{
		if (_instance == null)
		{
			synchronized (FileTransferManager.class)
			{
				if (_instance == null)
					_instance = new FileTransferManager(context);
			}
		}
		return _instance;
	}

	public boolean isFileTaskExist(long msgId)
	{
		return fileTaskMap.containsKey(msgId);
	}

	public void downloadFile(File destinationFile, String fileKey, long msgId, HikeFileType hikeFileType, Object userContext, boolean showToast)
	{
		if (isFileTaskExist(msgId))
			return;
		DownloadFileTask task = new DownloadFileTask(handler, fileTaskMap, context, destinationFile, fileKey, msgId, hikeFileType, userContext, showToast);
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

	public void uploadFile(String msisdn, File sourceFile, String fileType, HikeFileType hikeFileType, boolean isRec, boolean isForwardMsg, boolean isRecipientOnHike,
			long recordingDuration)
	{
		settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
		String uId = settings.getString(HikeMessengerApp.UID_SETTING, null);
		UploadFileTask task = new UploadFileTask(handler, fileTaskMap, context, token, uId, msisdn, sourceFile, fileType, hikeFileType, isRec, isForwardMsg, isRecipientOnHike,
				recordingDuration);
		// UploadFileTask task = new UploadFileTask(handler, fileTaskMap, context, token, uId, convMessage, isRecipientOnHike);
		MyFutureTask ft = new MyFutureTask(task);
		task.setFutureTask(ft);
		pool.execute(ft);
	}

	public void uploadFile(ConvMessage convMessage, boolean isRecipientOnHike)
	{
		if (isFileTaskExist(convMessage.getMsgID()))
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
		UploadContactOrLocationTask task = new UploadContactOrLocationTask(handler, fileTaskMap, context, msisdn, latitude, longitude, zoomLevel, isRecipientOnhike);
		MyFutureTask ft = new MyFutureTask(task);
		task.setFutureTask(ft);
		pool.execute(ft);
	}

	public void uploadContact(String msisdn, JSONObject contactJson, boolean isRecipientOnhike)
	{
		UploadContactOrLocationTask task = new UploadContactOrLocationTask(handler, fileTaskMap, context, msisdn, contactJson, isRecipientOnhike);
		MyFutureTask ft = new MyFutureTask(task);
		task.setFutureTask(ft);
		pool.execute(ft);
	}

	public void uploadContactOrLocation(ConvMessage convMessage, boolean uploadingContact, boolean isRecipientOnhike)
	{
		UploadContactOrLocationTask task = new UploadContactOrLocationTask(handler, fileTaskMap, context, convMessage, uploadingContact, isRecipientOnhike);
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

		if (fss.getFTState() == FTState.IN_PROGRESS || fss.getFTState() == FTState.PAUSED)
		{
			FutureTask<FTResult> obj = fileTaskMap.get(msgId);
			if (obj != null)
			{
				((MyFutureTask) obj).getTask().setState(FTState.CANCELLED);
			}
			Log.d(getClass().getSimpleName(), "deleting state file" + msgId);
			deleteStateFile(msgId, mFile);
			if (!sent)
			{
				Log.d(getClass().getSimpleName(), "deleting tempFile" + msgId);
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
			((MyFutureTask) obj).getTask().setState(FTState.PAUSING);
			Log.d(getClass().getSimpleName(), "pausing the task....");
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
		if (HIKE_TEMP_DIR != null)
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
						Log.e(getClass().getSimpleName(), "Exception while deleting state file : ", e);
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
		return fss;
	}

	// this function gives the state of uploading for a file
	public FileSavedState getUploadFileState(long msgId, File mFile)
	{
		Log.d(getClass().getSimpleName(), "Returning state for message ID : " + msgId);
		if (isFileTaskExist(msgId))
		{
			FutureTask<FTResult> obj = fileTaskMap.get(msgId);
			if (obj != null)
			{
				Log.d(getClass().getSimpleName(), "Returning: " + ((MyFutureTask) obj).getTask()._state.toString());
				return new FileSavedState(((MyFutureTask) obj).getTask()._state, ((MyFutureTask) obj).getTask()._totalSize, ((MyFutureTask) obj).getTask()._bytesTransferred);
			}
			else
			{
				Log.d(getClass().getSimpleName(), "Returning: in_prog");
				return new FileSavedState(FTState.IN_PROGRESS, 0, 0);
			}
		}
		else
			return getUploadFileState(mFile, msgId);
	}

	public FileSavedState getUploadFileState(File mFile, long msgId)
	{
		Log.d(getClass().getSimpleName(), "Returning from second call");
		if (mFile == null) // @GM only for now. Has to be handled properly
			return new FileSavedState();

		FileSavedState fss = null;

		try
		{
			String fName = mFile.getName() + ".bin." + msgId;
			Log.d(getClass().getSimpleName(), fName);
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
			Log.e(getClass().getSimpleName(), "Exception while reading state file : ", e);
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

	// Set the limits of chunk sizes of files to transfer
	public void setChunkSize()
	{
		NetworkType networkType = getNetworkType();
		maxChunkSize = networkType.getMaxChunkSize();
		minChunkSize = networkType.getMinChunkSize();
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

		if (fss.getFTState() == FTState.IN_PROGRESS || fss.getFTState() == FTState.PAUSING || fss.getFTState() == FTState.PAUSED || fss.getFTState() == FTState.ERROR)
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
}
