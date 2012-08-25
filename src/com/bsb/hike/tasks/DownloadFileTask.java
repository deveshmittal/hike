package com.bsb.hike.tasks;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.utils.Utils;

public class DownloadFileTask extends AsyncTask<Void, Integer, Boolean>
{
	private File destinationFile;
	private String fileKey;
	private Context context;
	private int progressFileTransfer;
	private long msgId;
	private boolean freeSpaceError = false;

	public DownloadFileTask(Context context, File destinationFile, String fileKey, long msgId) 
	{
		this.destinationFile = destinationFile;
		this.fileKey = fileKey;
		this.context = context;
		this.msgId = msgId;
	}

	@Override
	protected Boolean doInBackground(Void... params) 
	{
		FileOutputStream fos = null;
		InputStream is = null;
		try 
		{
			URL url = new URL(HikeConstants.FILE_TRANSFER_BASE_URL + fileKey);
			URLConnection urlConnection = url.openConnection();

			int length = urlConnection.getContentLength();

			if(length > Utils.getFreeSpace())
			{
				freeSpaceError = true;
				return Boolean.FALSE;
			}

			is = new BufferedInputStream(url.openConnection().getInputStream());

			fos = new FileOutputStream(destinationFile);

			byte[] buffer = new byte[HikeConstants.MAX_BUFFER_SIZE_KB * 1024];
			int len = 0;
			int totProg = 0;
			int progress;

			while((len = is.read(buffer)) != -1)
			{
				totProg += len;
				fos.write(buffer, 0, len);
				progress = (int) ((totProg*100/length));
				publishProgress(progress);
			}
			Log.d(getClass().getSimpleName(), "Done downloading file");
		} 
		catch (MalformedURLException e)
		{
			Log.e(getClass().getSimpleName(), "Invalid URL", e);
			return Boolean.FALSE;
		} 
		catch (IOException e)
		{
			Log.e(getClass().getSimpleName(), "Error while downloding file", e);
			return Boolean.FALSE;
		}
		finally
		{
			try 
			{
				if(fos != null)
				{
					fos.close();
				}
				if(is != null)
				{
					is.close();
				}
			} 
			catch (IOException e) 
			{
				Log.e(getClass().getSimpleName(), "Error while closing file", e);
				return Boolean.FALSE;
			}
		}
		return Boolean.TRUE;
	}

	@Override
	protected void onProgressUpdate(Integer... values) 
	{
		progressFileTransfer = values[0];
		HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
	}

	@Override
	protected void onPostExecute(Boolean result) 
	{
		if(result)
		{
			HikeConversationsDatabase.getInstance().addFile(fileKey, destinationFile.getName());
		}
		else
		{
			Toast.makeText(context, freeSpaceError ? R.string.not_enough_space : R.string.download_failed, Toast.LENGTH_SHORT).show();
			Log.d(getClass().getSimpleName(), "File not downloaded " + progressFileTransfer);
			destinationFile.delete();
		}

		ChatThread.fileTransferTaskMap.remove(msgId);
		HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
	}

	public int getProgressFileTransfer()
	{
		return progressFileTransfer;
	}
}