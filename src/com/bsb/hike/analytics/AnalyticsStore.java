package com.bsb.hike.analytics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.utils.Logger;

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
				 AnalyticsConstants.FILE_EXTENSION;
		}
		else if(priority == EventPriority.HIGH)
		{
			fileName = AnalyticsConstants.IMP_EVENT_FILE_NAME + Long.toString(System.currentTimeMillis()) + 
					 AnalyticsConstants.FILE_EXTENSION;			
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

		String dirName = this.context.getFilesDir().toString() + AnalyticsConstants.EVENT_FILE_DIR;
		File dir = new File(dirName);

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
	 * writes the event json to the file
	 */
	protected void dumpEvents(final ArrayList<JSONObject> eventJsons)
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
							normalPriorityEventFile = createNewEventFile(EventPriority.NORMAL);
						}

						fileWriter = new FileWriter(normalPriorityEventFile, true);
						fileWriter.write(normal.toString());

						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "events written to the file!");
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
							highPriorityEventFile = createNewEventFile(EventPriority.HIGH);
						}
						fileWriter = new FileWriter(normalPriorityEventFile, true);
						fileWriter.write(normal.toString());
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
			}
		}).start();						
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
		
		File dir = new File(context.getFilesDir().toString() + AnalyticsConstants.EVENT_FILE_DIR + File.separator);

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
		File dir = new File(context.getFilesDir().toString() + AnalyticsConstants.EVENT_FILE_DIR + File.separator);

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
}
