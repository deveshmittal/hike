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
class AnalyticsStore implements Runnable
{
	private Context context = null;
	
	private static AnalyticsStore _instance; 
	
	private ArrayList<Event> eventList = null;
	
	private File eventFile = null;
	
	private String currentFileName;
				
	/**
	 * Constructor
	 * @param context application context
	 */
	private AnalyticsStore(Context context)
	{
		this.context = context.getApplicationContext();
		
		eventList = new ArrayList<Event>();
		
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
				_instance = new AnalyticsStore(context.getApplicationContext());
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
			dir.mkdirs();
		}
		eventFile = new File(dir, fileName);

		eventFile.createNewFile();

		currentFileName = fileName;
		return eventFile;
	}
	
	/**
	 * writes the event json to the file
	 */
	private synchronized void dumpEvents()
	{		
		FileWriter fileWriter = null;
		try
		{
			ArrayList<Event> events = (ArrayList<Event>)eventList.clone();
			eventList.clear();
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
			
			for(Event e : events)
			{
				JSONObject json = Event.toJson(e);
				
				fileWriter.write(json + AnalyticsConstants.NEW_LINE);
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
			if(eventList != null)
			{
				eventList.clear();
			}

			if(fileWriter != null)	
			{
				closeCurrentFile(fileWriter);
			}
		}
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
	 * Sets the events to be written to the file
	 * @param events ArrayList of events
	 */
	protected void setEventsToDump(ArrayList<Event> events)
	{
		this.eventList.addAll(events);
	}
	
	/**
	 * Returns the events to be written to the file
	 * @return ArrayList of events 
	 */
	protected ArrayList<Event> getEventsToDump()
	{
		return this.eventList;
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
	 * writer thread's run implementation
	 */
	@Override
	public void run() 
	{
		dumpEvents();		
	}
}
