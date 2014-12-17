package com.bsb.hike.analytics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;

import org.json.JSONObject;

import android.content.Context;
import android.os.FileObserver;

import com.bsb.hike.analytics.Event.EventPriority;
import com.bsb.hike.utils.Logger;

/**
 * @author rajesh
 *
 */
class AnalyticsStore implements Runnable
{
	private Context context = null;
	
	private ArrayList<Event> eventList = null;
	
	private boolean hasMaxFileSizeReached = false;
	
	private FileObserver fileObserver = null;
		
	private File eventFile = null;
	
	private FileWriter fileWriter = null;
	
	private String currentFileName;
				
	/**
	 * Constructor
	 * @param context application context
	 */
	public AnalyticsStore(Context context)
	{
		this.context = context.getApplicationContext();
								
		currentFileName = getNewFileName();
	}
	
	/**
	 * Returns the file name which is a concatenation of filename and current system time
	 * @return name of the file
	 */
	private String getNewFileName()
	{
		 return currentFileName = AnalyticsConstants.EVENT_FILE_NAME + Long.toString(System.currentTimeMillis()) + 
				 AnalyticsConstants.FILE_EXTENSION;
	}
	
	/**
	 * deletes the event file whose priority is given
	 * @param priority priority of the events to be deleted
	 */
	public void deleteEvents(EventPriority priority)
	{
		//TODO:: Implementation of events deletion goes here. 
	}
	
	/**
	 * gets the size of the event file whose priority is given
	 * @param priority priority of the events 
	 * @return the size of file in bytes
	 */
	public long getFileSize(EventPriority priority)
	{
		return eventFile.length();
	}
	
	/**
	 * creates a new plain events(text) file 
	 */
	private void createNewEventFile(String fileName)
	{
		try 
		{			
			String dirName = this.context.getFilesDir().toString() + AnalyticsConstants.EVENT_FILE_DIR;
			File dir = new File(dirName);
			dir.mkdirs();
			eventFile = new File(dir, currentFileName);
			
			if(!eventFile.exists())
				eventFile.createNewFile();
			
			fileWriter = new FileWriter(eventFile, true);
		}
		catch (IOException e) 
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "io exception while creating file writer");
		}
	}
	
	/**
	 * writes the event json to the file
	 */
	public synchronized void dumpEvents()
	{		
		try
		{
			createNewEventFile(currentFileName);

			Logger.d(AnalyticsConstants.ANALYTICS_TAG, currentFileName);
			
			long size = eventFile.length();
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "file was written!");

			if(size >= AnalyticsConstants.MAX_FILE_SIZE)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "current file size reached its limit!");
				setMaxFileSizeReached(true);
				closeCurrentFile();				
				createNewEventFile(getNewFileName());
			}

			for(Event e : eventList)
			{
				JSONObject json = Event.toJson(e);
				
				if(fileWriter != null)					
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
			eventList.clear();

			if(fileWriter != null)	
			{
				closeCurrentFile();
			}
		}
	}

	/**
	 * checks if the event file has reached its max size
	 * @return true if event file has reached its max size, false otherwise 
	 */
	private boolean isMaxFileSizeReached()
	{
		return hasMaxFileSizeReached;
	}
	
	/**
	 * sets the event file size
	 * @param value true if file size has reached its max size, false otherwise
	 */
	private void setMaxFileSizeReached(boolean value)
	{
		hasMaxFileSizeReached = true;
	}
	
	/**
	 * Sets the events to be written to the file
	 * @param events ArrayList of events
	 */
	public void setEventsToDump(ArrayList<Event> events)
	{
		this.eventList = (ArrayList<Event>)events.clone();
	}
	
	/**
	 * Returns the events to be written to the file
	 * @return ArrayList of events 
	 */
	public ArrayList<Event> getEventsToDump()
	{
		return this.eventList;
	}
	
	/**
	 * closes the currently opened event file
	 */
	private void closeCurrentFile()
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
