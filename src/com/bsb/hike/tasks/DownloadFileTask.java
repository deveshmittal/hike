package com.bsb.hike.tasks;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.os.AsyncTask;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.ui.ChatThread;

public class DownloadFileTask extends AsyncTask<Void, Integer, Boolean>
{
	private File destinationFile;
	private String fileKey;
	private ChatThread chatThread;
	private int progressFileTransfer;
	private long msgId;

	public DownloadFileTask(ChatThread activity, File destinationFile, String fileKey, long msgId) 
	{
		this.destinationFile = destinationFile;
		this.fileKey = fileKey;
		this.chatThread = activity;
		this.msgId = msgId;
	}

	public void setChatThread(ChatThread activity)
	{
		this.chatThread = activity;
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
		chatThread.mUpdateAdapter.run();
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
			Log.d(getClass().getSimpleName(), "File not downloaded " + progressFileTransfer);
			destinationFile.delete();
		}

		ChatThread.fileTransferTaskMap.remove(msgId);
		chatThread.mUpdateAdapter.run();
	}

	public int getProgressFileTransfer()
	{
		return progressFileTransfer;
	}
}