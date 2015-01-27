package com.bsb.hike.analytics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.zip.GZIPOutputStream;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.net.ConnectivityManager;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * @author rajesh
 *
 */
public class AnalyticsStore
{
	private Context context;
	
	private static AnalyticsStore _instance; 
		
	private File normalPriorityEventFile;
	
	private File highPriorityEventFile;
		
	/**
	 * Constructor
	 * @param context application context
	 */
	private AnalyticsStore(Context context)
	{
		this.context = context.getApplicationContext();
						
		try 
		{
			normalPriorityEventFile = createNewEventFile(EventPriority.NORMAL);
			
			highPriorityEventFile = createNewEventFile(EventPriority.HIGH);
		}
		catch (IOException e) 
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "IO exception while creating new event file");
		}
	}
	
	/**
	 * static constructor of AnalyticsStore
	 * @param context application context
	 * @return singleton instance of AnalyticsStore 
	 */
	public static AnalyticsStore getInstance(Context context)
	{
		if(_instance == null)
		{
			synchronized (AnalyticsStore.class) 
			{
				if(_instance == null)
				{
					_instance = new AnalyticsStore(context.getApplicationContext());
				}
			}
		}
		return _instance;
	}
	
	/**
	 * Returns the file name which is a concatenation of filename and current system time
	 * @return name of the file
	 */
	private String generateNewEventFileName(EventPriority priority)	
	{
		String fileName = null;
		
		if(priority == EventPriority.NORMAL)
		{
			fileName = AnalyticsConstants.NORMAL_EVENT_FILE_NAME + Long.toString(System.currentTimeMillis()) + 
				 AnalyticsConstants.SRC_FILE_EXTENSION;
		}
		else if(priority == EventPriority.HIGH)
		{
			fileName = AnalyticsConstants.IMP_EVENT_FILE_NAME + Long.toString(System.currentTimeMillis()) + 
					 AnalyticsConstants.SRC_FILE_EXTENSION;			
		}
		return fileName;
	}
	
	/**
	 * gets the size of the event file whose priority is given
	 * @return the size of file in bytes
	 */
	protected long getFileSize(EventPriority priority)
	{
		long fileLength = 0;
		
		if(priority == EventPriority.NORMAL)
		{
			if(normalPriorityEventFile != null)
			{
				fileLength = normalPriorityEventFile.length();
			}
		}
		else if(priority == EventPriority.HIGH)
		{
			if(highPriorityEventFile != null)
			{
				fileLength = highPriorityEventFile.length();
			}
		}		
		return fileLength;
	}
	
	/**
	 * creates a new plain events(text) file 
	 */
	private File createNewEventFile(EventPriority priority) throws IOException
	{
		File eventFile = null;
		
		String fileName = generateNewEventFileName(priority);
		
		File dir = new File(HAManager.getInstance().getAnalyticsDirectory());

		if(!dir.exists())
		{
			boolean ret = dir.mkdirs();
			
			if(!ret)
			{				
				throw new IOException("Failed to create Analytics directory");
			}
		}
		eventFile = new File(dir, fileName);

		boolean val = eventFile.createNewFile();
		
		if(!val)
		{
			throw new IOException("Failed to create event file");
		}

		return eventFile;
	}

	/**
	 * Used to write event json to the file
	 * @param eventJsons ArrayList of json events to be written to the file
	 * @param sendToServer true if data should be sent to the server, false otherwise
	 * @param isOnDemandFromServer true if server has sent action packet to get data from client, false otherwise
	 */
	protected void dumpEvents(final ArrayList<JSONObject> eventJsons, final boolean sendToServer, final boolean isOnDemandFromServer)
	{		
		new Thread(new Runnable() 
		{			
			@Override
			public void run() 
			{			
				FileWriter fileWriter = null;
				StringBuilder normal = new StringBuilder();
				StringBuilder high = new StringBuilder();

				try
				{
					for(JSONObject object : eventJsons)
					{
						JSONObject json = object.getJSONObject(HikeConstants.DATA);

						if(json.has(AnalyticsConstants.EVENT_PRIORITY))
						{
							EventPriority priority = (EventPriority) json.get(AnalyticsConstants.EVENT_PRIORITY);

							if(priority == EventPriority.NORMAL)
							{
								normal.append(json);
								normal.append(AnalyticsConstants.NEW_LINE);
							}
							else if(priority == EventPriority.HIGH)
							{
								high.append(json);
								high.append(AnalyticsConstants.NEW_LINE);								
							}
						}
					}					
					if(normal.length() > 0)
					{
						if(!eventFileExists(EventPriority.NORMAL))
						{
							normalPriorityEventFile = createNewEventFile(EventPriority.NORMAL);
						}

						if(getFileSize(EventPriority.NORMAL) >= AnalyticsConstants.MAX_FILE_SIZE)
						{
							Logger.d(AnalyticsConstants.ANALYTICS_TAG, "normal priority file size reached its limit! " + normalPriorityEventFile.getName());
							compressAndDeleteOriginalFile(normalPriorityEventFile.getAbsolutePath());
							normalPriorityEventFile = createNewEventFile(EventPriority.NORMAL);
						}
						fileWriter = new FileWriter(normalPriorityEventFile, true);
						fileWriter.write(normal.toString());
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "events written to normal file! Size now :" + normalPriorityEventFile.length() + "bytes");
					}
					if(sendToServer)
					{
						compressAndDeleteOriginalFile(normalPriorityEventFile.getAbsolutePath());
					}

					if(high.length() > 0)
					{
						if(!eventFileExists(EventPriority.HIGH))
						{
							highPriorityEventFile = createNewEventFile(EventPriority.HIGH);
						}

						if(getFileSize(EventPriority.HIGH) >= AnalyticsConstants.MAX_FILE_SIZE)
						{
							Logger.d(AnalyticsConstants.ANALYTICS_TAG, "high priority file size reached its limit! " + highPriorityEventFile.getName());
							compressAndDeleteOriginalFile(highPriorityEventFile.getAbsolutePath());
							highPriorityEventFile = createNewEventFile(EventPriority.HIGH);
						}
						fileWriter = new FileWriter(highPriorityEventFile, true);
						fileWriter.write(normal.toString());
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "events written to imp file! Size now :" + highPriorityEventFile.length() + "bytes");
					}	
					if(sendToServer)
					{
						compressAndDeleteOriginalFile(highPriorityEventFile.getAbsolutePath());
					}
				}
				catch (IOException e)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "io exception while writing events to file");
				}
				catch(ConcurrentModificationException ex)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "ConcurrentModificationException exception while writing events to file");			
				}
				catch(JSONException e)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "json error");
				}
				finally
				{	
					if(fileWriter != null)	
					{
						closeCurrentFile(fileWriter);
					}
				}
				// SEND ANALYTICS FROM HERE
				if(sendToServer)
				{
					if(Utils.isUserOnline(context))
					{			
						if(!isOnDemandFromServer)
						{
							// if total logged data is less than threshold value or wifi is available, try sending all the data else delete normal priority data
							if(!((Utils.getNetworkType(context) == ConnectivityManager.TYPE_WIFI) || 
									(AnalyticsStore.getInstance(context).getTotalAnalyticsSize() <= HAManager.getInstance().getMaxAnalyticsSizeOnClient())))
							{
								AnalyticsStore.getInstance(context).deleteNormalPriorityData();
							}
						}
						AnalyticsSender.getInstance(context).sendData();
					}
				}
			}
		}, AnalyticsConstants.ANALYTICS_THREAD_WRITER).start();						
	}
	
	/**
	 * Checks if the event file exists or not
	 * @return true if the file exists, false otherwise
	 */
	private boolean eventFileExists(EventPriority priority)
	{	
		boolean isExist = false;
		
		if(priority == EventPriority.NORMAL)
		{
			isExist = normalPriorityEventFile != null && normalPriorityEventFile.exists();
		}
		else if(priority == EventPriority.HIGH)
		{
			isExist = highPriorityEventFile != null && highPriorityEventFile.exists();			
		}
		return isExist;
	}
	
	/**
	 * closes the currently opened event file
	 */
	private void closeCurrentFile(FileWriter fileWriter)
	{		
		try 
		{
			fileWriter.flush();
			fileWriter.close();
		}
		catch (IOException e) 
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "io exception while file connection closing!");
		}
	}	
	
	/**
	 * Used to get the total size of the logged analytics data
	 * @return size of the logged data in bytes
	 */
	public long getTotalAnalyticsSize()
	{
		long dirSize = 0;
		
		File dir = new File(HAManager.getInstance().getAnalyticsDirectory() + File.separator);

		File[] file = dir.listFiles();

		if(file == null)
			return 0;
		
		int size = file.length;
		
		for(int i=0; i<size; i++)
		{
			dirSize += file[i].length();
		}
		return dirSize;
	}
	
	/**
	 * Used to delete the analytics log files having NORMAL priority
	 */
	public void deleteNormalPriorityData()
	{
		File dir = new File(HAManager.getInstance().getAnalyticsDirectory() + File.separator);

		File[] file = dir.listFiles();

		if(file == null)
			return;
		
		int size = file.length;
		
		for(int i=0; i<size; i++)
		{
			if(file[i].exists() && file[i].getName().startsWith(AnalyticsConstants.NORMAL_EVENT_FILE_NAME, 0))
			{
				file[i].delete();
			}
		}
	}
	
	/**
	 * Used to compress the text file to gzip file
	 * @param fileUrl is the file path to be compressed
	 * @throws IOException 
	 */
	private void gzipFile(String srcFileUrl) throws IOException
	{	
		String destFileUrl = srcFileUrl.replace(AnalyticsConstants.SRC_FILE_EXTENSION, AnalyticsConstants.DEST_FILE_EXTENSION);

		byte[] buffer = new byte[4096];
		
		GZIPOutputStream gzos = null;
		FileInputStream fis = null;
		try
		{
			gzos = new GZIPOutputStream(new FileOutputStream(destFileUrl));

			fis = new FileInputStream(srcFileUrl);

			int len;

			while ((len = fis.read(buffer)) > 0) 
			{
				gzos.write(buffer, 0, len);
			}	 
		}
		catch(FileNotFoundException ex)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "ioExcepion while compressing file");
		}
		finally
		{
			if(fis != null)
			{
				fis.close();
			}
			if(gzos != null)
			{
				gzos.finish();
				gzos.close();
			}
		}
	}
	
	/**
	 * Used to compress file with a given path in gzip format and then deletes it
	 * @param filePath of the file to be compressed to gzip
	 * @throws IOException thrown by gzipFile() method
	 */
	private void compressAndDeleteOriginalFile(String filePath) throws IOException
	{
		File tempFile = new File(filePath);

		if(tempFile.length() == 0)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "File was empty! Deleted :" + filePath);
			tempFile.delete();			
			return;
		}
		else
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "File compressed :" + filePath);
			gzipFile(filePath);
			tempFile.delete();
		}
	}
}
