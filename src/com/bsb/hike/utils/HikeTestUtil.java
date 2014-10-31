package com.bsb.hike.utils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class HikeTestUtil
{
	public static final String TEST_LOGS_FILE_PATH = Environment.getExternalStorageDirectory() + "/Hike/TestReport.txt";

	public static final String TEST_CONN_FILE_PATH = Environment.getExternalStorageDirectory() + "/Hike/ConnReport.txt";

	public static final String TEST_DATA_DIR = "/sdcard/Hike/TestFiles/";
	
	public static final String LOGS_RECEIVER_EMAIL = "fieldtest@hike.in";
	
	public static final int HIKE_MESSAGE_COUNTER_DEFAULT = 25;
	
	public static final int HIKE_MESSAGE_DELAY_DEFAULT = 1000; 

	public static final int HIKE_MESSAGE_DELAY_MAX = 2000; 

	public static final int HIKE_MESSAGE_DELAY_MIN = 100; 

	public static final int HIKE_MESSAGE_COUNTER_MAX = 50;	
	
	private static int msg_delay = HIKE_MESSAGE_DELAY_DEFAULT; 
	
	private volatile static HikeTestUtil mTestUtil = null;
	
	private File dataFile = null;
		
	private FileOutputStream d_fos = null;
	
	private OutputStreamWriter d_osw = null;
	
	private File connFile = null;

	private FileOutputStream conn_fos = null;
	
	private OutputStreamWriter conn_osw = null;

	private Map<Long, Long> msgidTimeMap = null;
	
	Context context = null;

	/**
	 * constructor
	 */
	private HikeTestUtil(Context appContext)
	{
		context = appContext;
		msgidTimeMap = new ConcurrentHashMap<Long, Long>();
		File testData = new File(TEST_DATA_DIR);
		testData.mkdirs();
		createNewDataFile();
		createNewConnFile();
	}

	/**
	 * singleton class
	 */
	public static HikeTestUtil getInstance(Context appContext)
	{
		if (mTestUtil == null)
		{
			mTestUtil = new HikeTestUtil(appContext);
		}
		return mTestUtil;
	}

	/**
	 * returns the Hashmap
	 */
	public Map<Long, Long> getMsgIdTimeMap()
	{
		return msgidTimeMap;
	}
	
	/**
	 * adds a K,V pair to the map
	 */
	public void setMsgidTimestampPair(Long msgId, Long timestamp)
	{
		msgidTimeMap.put(msgId, timestamp);
	}
	
	/**
	 * returns timestamp corresponding to a msgId
	 */
	public Long getTimestampForMsgid(Long msgId)
	{
		return msgidTimeMap.get(msgId);
	}
	
	/*
	 * deletes the old file and creates a new one
	 */
	public void deleteFile()
	{
		String filename = TEST_LOGS_FILE_PATH;
		dataFile = new File(filename);	

		if(dataFile.exists())
		{
			Log.d("HikeTestUtil", "test report exists");
			
			if(dataFile.delete())
			{
				Log.d("HikeTestUtil", "old file deleted");
				createNewDataFile();
			}
			else
			{
				Log.d("HikeTestUtil", "old file deletion failed!");
			}
		}
		else
		{
			createNewDataFile();
		}
	}
	
	/*
	 * creates a new file 
	 */
	public void createNewDataFile()
	{
		String filename = TEST_LOGS_FILE_PATH;
		dataFile = null;
		d_fos = null;
		d_osw = null;
		dataFile = new File(filename);	
		
		try
		{
			d_fos = new FileOutputStream(dataFile, true);
			d_osw = new OutputStreamWriter(d_fos);

			if (!dataFile.exists())
			{
				if (dataFile.createNewFile())
				{
					Log.d("HikeTestUtil", "new file got created!");
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}		
	}

	/*
	 * creates a new connection file 
	 */
	public void createNewConnFile()
	{
		String filename = TEST_CONN_FILE_PATH;
		connFile = null;
		conn_fos = null;
		conn_osw = null;
		connFile = new File(filename);	
		
		try
		{
			conn_fos = new FileOutputStream(connFile, true);
			conn_osw = new OutputStreamWriter(conn_fos);

			if (!connFile.exists())
			{
				if (connFile.createNewFile())
				{
					Log.d("HikeTestUtil", "new connection file got created!");
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}		
	}

	/**
	 * writes the log to the file
	 */
	public void writeDataToFile(String log)
	{
		Log.d("HikeTestUtil", "data to write :" + log);
		
		try
		{
			Log.d("HikeTestUtil", "Inside try block");			
 			d_osw.append(log);
 			d_osw.append("\n");
 			d_osw.flush();
 			d_fos.flush();
		}
		catch (IOException e)
		{
			Log.d("HikeTestUtil", "There was exception in writing data to file!");
			e.printStackTrace();
		}
	}

	/**
	 * writes connection logs to the file
	 */
	public void writeConnLogsToFile(String log)
	{
		Log.d("HikeTestUtil", "data to write :" + log);
		
		try
		{
			Log.d("HikeTestUtil", "Inside try block");			
 			conn_osw.append(log);
 			conn_osw.append("\n");
 			conn_osw.flush();
 			conn_fos.flush();
		}
		catch (IOException e)
		{
			Log.d("HikeTestUtil", "There was exception in writing data to conn file!");
			e.printStackTrace();
		}
	}

	/**
	 * close the file
	 */
	@SuppressLint("NewApi")
	public void closeDataFile()
	{		
		try
		{
			dataFile.setReadOnly();
			dataFile.setWritable(false);
			d_osw.close();
			d_fos.close();		
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * close the connection log file
	 */
	@SuppressLint("NewApi")
	public void closeConnectionFile()
	{		
		try
		{
			connFile.setReadOnly();
			connFile.setWritable(false);
			conn_osw.close();
			conn_fos.close();		
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * formatter for current time upto 3 digits in milliseconds
	 */
	public static String getCurrentTimeInMilliseconds()
	{
		Calendar c = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("dd:MMMM:yyyy HH:mm:ss.SSS");
		String strDate = sdf.format(c.getTime());
		return strDate;
	}

	/**
	 * formats current time in date and time
	 */
	public static String getCurrentDateTime()
	{
		Calendar c = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("dd:MMMM:yyyy HH:mm");
		String strDate = sdf.format(c.getTime());
		return strDate;
	}

	/*
	 * set message delay in milliseconds
	 */
	public void setMessageDelay(int delay)
	{
		msg_delay = delay;
	}
	
	/*
	 * get current message delay in milliseconds
	 */
	public int getMessageDelay()
	{
		return msg_delay;
	}
	
	/**
	 * send log file to server 
	 * Not in use right now
	 */	
	public static int uploadFile(String sourceFileUri) 
	{
		int serverResponseCode = 0;	        
	    String upLoadServerUri = null;
	     
	    /**********  File Path *************/
	    final String uploadFileName = "HikeTest.txt";
	 
        String fileName = sourceFileUri;
        HttpURLConnection conn = null;
        DataOutputStream dos = null;  
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024; 
        File sourceFile = new File(sourceFileUri); 
         
        if (!sourceFile.isFile()) 
        {                           
             Log.e("uploadFile", "Source File not exist : " + sourceFileUri);                                               
             return 0;          
        }
        else
        {
             try 
             {                   
                 // open a URL connection to the Servlet
                 FileInputStream fileInputStream = new FileInputStream(sourceFile);
                 URL url = new URL(upLoadServerUri);
                  
                 // Open a HTTP  connection to  the URL
                 conn = (HttpURLConnection) url.openConnection(); 
                 conn.setDoInput(true); // Allow Inputs
                 conn.setDoOutput(true); // Allow Outputs
                 conn.setUseCaches(false); // Don't use a Cached Copy
                 conn.setRequestMethod("POST");
                 conn.setRequestProperty("Connection", "Keep-Alive");
                 conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                 conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                 conn.setRequestProperty("uploaded_file", fileName); 
                  
                 dos = new DataOutputStream(conn.getOutputStream());        
                 dos.writeBytes(twoHyphens + boundary + lineEnd); 
                 dos.writeBytes("Content-Disposition: form-data; name=" + "uploaded_file;filename="
                         + fileName + "" + lineEnd);
                  
                 dos.writeBytes(lineEnd);
        
                 // create a buffer of  maximum size
                 bytesAvailable = fileInputStream.available(); 
        
                 bufferSize = Math.min(bytesAvailable, maxBufferSize);
                 buffer = new byte[bufferSize];
        
                 // read file and write it into form...
                 bytesRead = fileInputStream.read(buffer, 0, bufferSize);  
                    
                 while (bytesRead > 0) 
                 {                      
                	 dos.write(buffer, 0, bufferSize);
                	 bytesAvailable = fileInputStream.available();
                	 bufferSize = Math.min(bytesAvailable, maxBufferSize);
                	 bytesRead = fileInputStream.read(buffer, 0, bufferSize);                       
                 }
        
                 // send multipart form data necesssary after file data...
                 dos.writeBytes(lineEnd);
                 dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
        
                 // Responses from the server (code and message)
                 serverResponseCode = conn.getResponseCode();
                 String serverResponseMessage = conn.getResponseMessage();
                   
                 Log.i("uploadFile", "HTTP Response is : " + serverResponseMessage + ": " + serverResponseCode);
                  
                 if(serverResponseCode == 200)
                 {                      
                      String msg = "File Upload Completed. "+uploadFileName;
                 }    
                  
                 //close the streams //
                 fileInputStream.close();
                 dos.flush();
                 dos.close();                   
            }
            catch (MalformedURLException ex) 
            {                 
                ex.printStackTrace();                 
                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);  
            } 
            catch (Exception e) 
            {                 
	            e.printStackTrace();
                Log.e("Upload file to server Exception", "Exception : "+ e.getMessage(), e);                                                   
            }
            return serverResponseCode; 
             
         }
       }
	
		public Context getContext()
		{
			return context;
		}
	}