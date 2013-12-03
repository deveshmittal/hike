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

import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.filetransfer.FileTransferBase.FTState;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile.HikeFileType;

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
	private final int THREAD_POOL_SIZE = 10;

	private final short KEEP_ALIVE_TIME = 60; // in seconds

	private ExecutorService pool;

	public static FileTransferManager _instance = null;

	private SharedPreferences settings;

	private Handler handler;

	public static String FT_CANCEL = "ft_cancel";

	public static String READ_FAIL = "read_fail";

	public static String UPLOAD_FAILED = "upload_failed";

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
		}
	}

	private FileTransferManager(Context ctx)
	{
		BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>();
		fileTaskMap = new ConcurrentHashMap<Long, FutureTask<FTResult>>();
		// here choosing TimeUnit in seconds as minutes are added after api level 9
		pool = new ThreadPoolExecutor(THREAD_POOL_SIZE, THREAD_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS, workQueue, new MyThreadFactory());
		context = ctx;
		HIKE_TEMP_DIR = context.getExternalFilesDir(HIKE_TEMP_DIR_NAME);
		handler = new Handler(context.getMainLooper());
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
		return fileTaskMap.contains(msgId);
	}

	public void downloadFile(File destinationFile, String fileKey, long msgId, HikeFileType hikeFileType, Object userContext, boolean showToast)
	{
		DownloadFileTask task = new DownloadFileTask(handler, fileTaskMap, context, destinationFile, fileKey, msgId, hikeFileType, userContext, showToast);
		task.setState(FTState.IN_PROGRESS);
		try
		{
			MyFutureTask ft = new MyFutureTask(task);
			pool.execute(ft); // this future is used to cancel pause the task
			fileTaskMap.put(msgId, ft);
		}
		catch (RejectedExecutionException rjEx)
		{
			// handle this properly
		}

	}

	public void uploadFile(String msisdn, File destinationFile, String fileType, HikeFileType hikeFileType, boolean isRec, boolean isForwardMsg, boolean isRecipientOnHike,
			long recordingDuration)
	{
		settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
		String uId = settings.getString(HikeMessengerApp.UID_SETTING, null);
		UploadFileTask task = new UploadFileTask(handler, fileTaskMap, context, token, uId, msisdn, destinationFile, fileType, hikeFileType, isRec, isForwardMsg,
				isRecipientOnHike, recordingDuration);
		MyFutureTask ft = new MyFutureTask(task);
		task.setFutureTask(ft);
		pool.execute(ft);
	}

	public void uploadFile(ConvMessage convMessage, boolean isRecipientOnHike)
	{
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
			((MyFutureTask) obj).getTask().setState(FTState.PAUSED);
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
		for (File f : HIKE_TEMP_DIR.listFiles())
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

	// this function gives the state of downloading for a file
	public FileSavedState getDownloadFileState(long msgId, File mFile)
	{
		FutureTask<FTResult> obj = fileTaskMap.get(msgId);
		if (obj != null)
		{
			return new FileSavedState(((MyFutureTask) obj).getTask()._state, ((MyFutureTask) obj).getTask()._totalSize, ((MyFutureTask) obj).getTask()._bytesTransferred);
			// return new FileSavedState(FTState.IN_PROGRESS, ((MyFutureTask) obj).getTask()._totalSize, ((MyFutureTask) obj).getTask()._bytesTransferred);
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
		FutureTask<FTResult> obj = fileTaskMap.get(msgId);
		if (obj != null)
		{
			return new FileSavedState(((MyFutureTask) obj).getTask()._state, ((MyFutureTask) obj).getTask()._totalSize, ((MyFutureTask) obj).getTask()._bytesTransferred);
			// return new FileSavedState(FTState.IN_PROGRESS, ((MyFutureTask) obj).getTask()._totalSize, ((MyFutureTask) obj).getTask()._bytesTransferred);
		}
		else
			return getUploadFileState(mFile, msgId);
	}

	public FileSavedState getUploadFileState(File mFile, long msgId)
	{
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
		if (sent)
			fss = getUploadFileState(msgId, mFile);
		else
			fss = getDownloadFileState(msgId, mFile);

		if (fss.getFTState() == FTState.IN_PROGRESS || fss.getFTState() == FTState.PAUSED || fss.getFTState() == FTState.ERROR)
		{
			if (fss.getTotalSize() > 0)
				return (int) ((fss.getTransferredSize() * 100) / fss.getTotalSize());
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
}
