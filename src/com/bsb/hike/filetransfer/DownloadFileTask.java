package com.bsb.hike.filetransfer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;

import javax.net.ssl.HttpsURLConnection;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSSLUtil;
import com.bsb.hike.utils.Utils;

public class DownloadFileTask extends FileTransferBase
{
	private URL mUrl;

	private File tempDownloadedFile;

	private boolean showToast;

	private int num = 0;

	protected DownloadFileTask(Handler handler,ConcurrentHashMap<Long, FutureTask<FTResult>> fileTaskMap, Context ctx, File destinationFile, String fileKey, long msgId, HikeFileType hikeFileType,
			Object userContext,boolean showToast)
	{
		super(handler,fileTaskMap, ctx, destinationFile, msgId, hikeFileType);
		tempDownloadedFile = new File(FileTransferManager.getInstance(context).getHikeTempDir(), mFile.getName() + ".part");
		stateFile = new File(FileTransferManager.getInstance(context).getHikeTempDir(), mFile.getName() + ".bin." + msgId);
		this.fileKey = fileKey;
		this.showToast = showToast;
		this.userContext = userContext;
	}

	@Override
	public FTResult call()
	{
		Log.d(getClass().getSimpleName(), "Instantiating download ....");
		RandomAccessFile raf = null;
		try
		{
			mUrl = new URL(AccountUtils.fileTransferBaseDownloadUrl + fileKey);

			FileSavedState fst = FileTransferManager.getInstance(context).getDownloadFileState(mFile, msgId);
			/* represents this file is either not started or unrecovered error has happened */
			if (fst.getFTState().equals(FTState.NOT_STARTED) || fst.getFTState().equals(FTState.CANCELLED))
			{
				Log.d(getClass().getSimpleName(), "File state : " + fst.getFTState());
				raf = new RandomAccessFile(tempDownloadedFile, "rw");
				// TransferredBytes should always be set. It might be need for calculating percentage
				setBytesTransferred(0);
				return downloadFile(0, raf, AccountUtils.ssl);
			}
			else if (fst.getFTState().equals(FTState.PAUSED) || fst.getFTState().equals(FTState.ERROR))
			{
				Log.d(getClass().getSimpleName(), "File state : " + fst.getFTState());
				raf = new RandomAccessFile(tempDownloadedFile, "rw");
				// Restoring the bytes transferred(downloaded) previously.
				setBytesTransferred((int) raf.length());
				//Bug Fix: 13029
				setFileTotalSize(fst.getTotalSize());
				return downloadFile(raf.length(), raf, AccountUtils.ssl);
			}
		}
		catch (MalformedURLException e)
		{
			Log.e(getClass().getSimpleName(), "Invalid URL", e);
			return FTResult.DOWNLOAD_FAILED;
		}
		catch (FileNotFoundException e)
		{
			Log.e(getClass().getSimpleName(), "File Expired", e);
			return FTResult.FILE_EXPIRED;
		}
		catch (IOException e)
		{
			Log.e(getClass().getSimpleName(), "Error while downloding file", e);
			return FTResult.DOWNLOAD_FAILED;
		}
		return FTResult.DOWNLOAD_FAILED;
	}

	private URLConnection initConn() throws IOException
	{
		URLConnection conn = (HttpURLConnection) mUrl.openConnection();
		if (AccountUtils.ssl)
		{
			((HttpsURLConnection) conn).setSSLSocketFactory(HikeSSLUtil.getSSLSocketFactory());
		}
		AccountUtils.addUserAgent(conn);
		return conn;
	}

	// we can extend this later to multiple download threads (if required)
	private FTResult downloadFile(long mStartByte, RandomAccessFile raf, boolean ssl)
	{
		long mStart = mStartByte;
		FTResult res = FTResult.SUCCESS;
		BufferedInputStream in = null;
		URLConnection conn = null;
		while (shouldRetry())
		{
			try
			{
				conn = initConn();
				// set the range of byte to download
				String byteRange = mStart + "-";
				try
				{
					conn.setRequestProperty("Range", "bytes=" + byteRange);
					conn.setConnectTimeout(10000);
				}
				catch (Exception e)
				{

				}
				conn.connect();
				int resCode = ssl ? ((HttpsURLConnection) conn).getResponseCode() : ((HttpURLConnection) conn).getResponseCode();
				// Make sure the response code is in the 200 range.
				if (resCode / 100 != 2)
				{
					Log.d(getClass().getSimpleName(), "Server response code is not in 200 range");
					error();
					res = FTResult.SERVER_ERROR;
				}
				else
				// everything is fine till this point
				{
					retry = true;
					reconnectTime = 0;
					retryAttempts = 0;
					// Check for valid content length.
					int contentLength = conn.getContentLength();
					if ((contentLength - raf.length()) > Utils.getFreeSpace())
					{
						closeStreams(raf, in);
						return FTResult.FILE_TOO_LARGE;
					}
					Log.d(getClass().getSimpleName(), "bytes=" + byteRange);
					setFileTotalSize(contentLength + (int) mStart);

					progressPercentage = (int) ((_bytesTransferred * 100) / _totalSize);
					num = (int) (progressPercentage / 10);
					// get the input stream
					in = new BufferedInputStream(conn.getInputStream());

					// open the output file and seek to the start location
					raf.seek(mStart);

					int chunkSize = FileTransferManager.getInstance(context).getMinChunkSize();
					byte data[] = new byte[chunkSize];
					int numRead;
					while ((_state == FTState.IN_PROGRESS) && ((numRead = in.read(data, 0, chunkSize)) != -1))
					{
						// write to buffer
						try
						{
							raf.write(data, 0, numRead);
						}
						catch(IOException e)
						{
							Log.e(getClass().getSimpleName(), "Exception", e);
							return FTResult.CARD_UNMOUNT;
						}
						Log.d(getClass().getSimpleName(),"ChunkSize : " + chunkSize + "Bytes");
						// ChunkSize is increased within the limits
						chunkSize *= 2;
						if(chunkSize > FileTransferManager.getInstance(context).getMaxChunkSize())
							chunkSize = FileTransferManager.getInstance(context).getMaxChunkSize();
						else if (chunkSize < FileTransferManager.getInstance(context).getMinChunkSize())
							chunkSize = FileTransferManager.getInstance(context).getMinChunkSize();
						int maxMemory = (int) Runtime.getRuntime().maxMemory();
						if( chunkSize > (maxMemory / 8) )
							chunkSize = maxMemory / 8 ;
						// change buffer size
						data = new byte[chunkSize];
						// increase the startByte for resume later
						mStart += numRead;
						// increase the downloaded size
						incrementBytesTransferred(numRead);
						progressPercentage = (int) ((_bytesTransferred * 100) / _totalSize);
						showButton();
						//if (shouldSendProgress())
						sendProgress();
					}

					switch (_state)
					{
					case CANCELLED:
						Log.d(getClass().getSimpleName(), "FT Cancelled");
						deleteTempFile();
						deleteStateFile();
						closeStreams(raf, in);
						return FTResult.CANCELLED;
					case IN_PROGRESS:
						String md5Hash = AccountUtils.crcValue(fileKey);
						Log.d(getClass().getSimpleName(),"Server md5 : " + md5Hash);
						if (md5Hash != null)
						{
							String file_md5Hash = Utils.fileToMD5(tempDownloadedFile.getPath());
							Log.d(getClass().getSimpleName(),"Phone's md5 : " + file_md5Hash);
							if (!md5Hash.equals(file_md5Hash))
							{
								Log.d(getClass().getSimpleName(),"The md5's are not equal...Deleting the files...");
								deleteTempFile();
								deleteStateFile();
								return FTResult.FAILED_UNRECOVERABLE;
							}
								
						}
						else
						{
							deleteTempFile();
							deleteStateFile();
							return FTResult.FAILED_UNRECOVERABLE;
						}
						if (!tempDownloadedFile.renameTo(mFile)) // if failed
						{
							Log.d(getClass().getSimpleName(), "FT failed");
							error();
							closeStreams(raf, in);
							return FTResult.READ_FAIL;
						}
						else
						{
							Log.d(getClass().getSimpleName(), "FT Completed");
							// temp file is already deleted
							_state = FTState.COMPLETED;
							deleteStateFile();
							retry = false;
						}
						break;
					case PAUSED:
						Log.d(getClass().getSimpleName(), "FT PAUSED");
						saveFileState();
						retry = false;
						break;
					default:
						break;
					}
				}
			}
			catch (IOException e)
			{
				Log.e(getClass().getSimpleName(), "FT error : " + e.getMessage());
				if (e.getMessage() != null && (e.getMessage().contains(NETWORK_ERROR_1) || e.getMessage().contains(NETWORK_ERROR_2)))
				{
					// here we should retry
					mStart = _bytesTransferred;
					// Is case id the task quits after making MAX attempts
					// the file state is saved
					if(retryAttempts >= MAX_RETRY_ATTEMPTS)
						error();
				}
				else
				{
					error();
					res = FTResult.DOWNLOAD_FAILED;
					retry = false;
				}
			}
			catch (Exception e)
			{
				Log.e(getClass().getSimpleName(), "FT error : " + e.getMessage());
			}
		}
		res = closeStreams(raf, in);
		return res;
	}

	private FTResult closeStreams(RandomAccessFile raf, BufferedInputStream in)
	{
		if (raf != null)
		{
			try
			{
				raf.close();
			}
			catch (Exception e)
			{
				deleteTempFile();
				deleteStateFile();
				Log.e(getClass().getSimpleName(), "Error while closing file", e);
				return FTResult.DOWNLOAD_FAILED;
			}
		}

		if (in != null)
		{
			try
			{
				in.close();
			}
			catch (Exception e)
			{
			}
		}
		return FTResult.SUCCESS;
	}

	private boolean shouldSendProgress()
	{
		int x = progressPercentage / 10;
		if (x < num)
			return false;
		// @GM 'num++' will create a problem in future if with decide to increase "BUFFER_SIZE"(which we will)
		// num++;
		num = x + 1;
		return true;
	}

	private void sendProgress()
	{
		Log.d(getClass().getSimpleName(), "sending progress to publish...");
		Intent intent = new Intent(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}
	
	private void showButton()
	{
		Intent intent = new Intent(HikePubSub.RESUME_BUTTON_UPDATED);
		intent.putExtra("msgId", msgId);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
		
	}

	private void error()
	{
		_state = FTState.ERROR;
		saveFileState();
	}

	private void deleteTempFile()
	{
		if (tempDownloadedFile != null && tempDownloadedFile.exists())
			tempDownloadedFile.delete();
	}

	protected void postExecute(FTResult result)
	{
		FileTransferManager.getInstance(context).removeTask(msgId);
		if (result == FTResult.SUCCESS)
		{
			if (mFile != null)
			{
				if (mFile.exists() && hikeFileType != HikeFileType.AUDIO_RECORDING)
				{
					// this is to refresh media library
					context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(mFile)));
				}
				if (HikeFileType.IMAGE == hikeFileType)
					HikeMessengerApp.getPubSub().publish(HikePubSub.PUSH_FILE_DOWNLOADED, (ConvMessage) userContext);
			}
		}
		else if (result != FTResult.PAUSED) // if no PAUSE and no SUCCESS
		{
			final int errorStringId = result == FTResult.FILE_TOO_LARGE ? R.string.not_enough_space : result == FTResult.CANCELLED ? R.string.download_cancelled
					: result == FTResult.FILE_EXPIRED ? R.string.file_expire : result == FTResult.FAILED_UNRECOVERABLE ?
							R.string.download_failed_fatal : result == FTResult.CARD_UNMOUNT?
									R.string.card_unmount : R.string.download_failed;
			if (showToast)
			{
				handler.post(new Runnable()
				{
					@Override
					public void run()
					{
						Toast.makeText(context, errorStringId, Toast.LENGTH_SHORT).show();
					}
				});
			}
			if (mFile != null)
			{
				mFile.delete();
			}
		}
		showButton();
		sendProgress();
	}
}
