package com.bsb.hike.analytics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;

import org.json.JSONObject;

import android.content.Context;

import com.bsb.hike.utils.Logger;

/**
 * @author rajesh
 *
 */
class AnalyticsStore
{
	private Context context;
	
	private static AnalyticsStore _instance; 
		
	private File eventFile;
	
	private String currentFileName;
				
	/**
	 * Constructor
	 * @param context application context
	 */
	private AnalyticsStore(Context context)
	{
		this.context = context.getApplicationContext();
				
		try 
		{
			eventFile = createNewEventFile();
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
	private String generateNewEventFileName()
	{
		 return AnalyticsConstants.EVENT_FILE_NAME + Long.toString(System.currentTimeMillis()) + 
				 AnalyticsConstants.FILE_EXTENSION;
	}
	
	/**
	 * gets the size of the event file whose priority is given
	 * @return the size of file in bytes
	 */
	protected long getFileSize()
	{
		long fileLength = 0;
		
		if(eventFile != null)
		{
			fileLength = eventFile.length();
		}
		return fileLength;
	}
	
	/**
	 * creates a new plain events(text) file 
	 */
	private File createNewEventFile() throws IOException
	{
		String fileName = generateNewEventFileName();

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

		currentFileName = fileName;
		
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
				
				try
				{					
					if(!eventFileExists())
					{
						createNewEventFile();
					}

					Logger.d(AnalyticsConstants.ANALYTICS_TAG, currentFileName);
					
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "file was written!");

					if(getFileSize() >= AnalyticsConstants.MAX_FILE_SIZE)
					{
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "current file size reached its limit!");
						createNewEventFile();
					}

					fileWriter = new FileWriter(eventFile, true);
					
					for(JSONObject object : eventJsons)
					{						
						fileWriter.write(object.toString() + AnalyticsConstants.NEW_LINE);
					}
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "events written to the file!");
				}
				catch (IOException e)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "io exception while writing events to file");
				}
				catch(ConcurrentModificationException ex)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "ConcurrentModificationException exception while writing events to file");			
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
	private boolean eventFileExists()
	{		
		return eventFile != null && eventFile.exists();
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
}
