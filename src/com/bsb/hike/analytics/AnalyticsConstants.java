package com.bsb.hike.analytics;

/**
 * @author rajesh
 *
 */
public class AnalyticsConstants 
{	
	public static final long ONE_DAY = 24 * 60 * 60 * 1000;
	
	public static final long ONE_HOUR =  60 * 60 * 1000;
	
	public static long MAX_FILE_SIZE = 10 * 1024;
	
	public static long MAX_ANALYTICS_SIZE = 500 * 1024;
	
	/** time of first attempt to send analytics data */ 
	public static int HOUR_OF_DAY_TO_SEND = 8;
	
	/** number of times upload should be tried in one day */
	public static int ANALYTICS_UPLOAD_FREQUENCY = 3;
	
	/** try sending analytics data every 4 hours */
	public static int UPLOAD_TIME_MULTIPLE = 4;
	
	public static boolean IS_ANALYTICS_ENABLED = true;
	
	public static final int MAX_EVENTS_IN_MEMORY = 50;
	
	public static final String HTTP_UPLOAD_URL = "http://staging.im.hike.in/v1/logs/analytics"; 
	
	public static final String EVENT_FILE_DIR = "/Analytics";
	
	public static final String NEW_LINE = "\n";
		
	public static final String NORMAL_EVENT_FILE_NAME = "normaldata";
	
	public static final String IMP_EVENT_FILE_NAME = "impdata";
	
	public static final String FILE_EXTENSION = ".txt";
	
	public static final String ANALYTICS_TAG = "hikeAnalytics";
	
	public static String TYPE = "t";

	public static String ANALYTICS_EVENT = "le_android";
	
	public static String EVENT_PRIORITY = "ep";

	public static String DATA = "d";
	
	public static String METADATA = "md";

	public static String UI_EVENT = "uiEvent";

	public static String NON_UI_EVENT = "nonUiEvent";

	public static String CLICK_EVENT = "click";
				
	public static String SUB_TYPE = "st";
	
	public static String EVENT_TYPE = "et";		
	
	public static String EVENT_KEY = "ek";
	
	public static String TO = "to";	
	
	public static final String ANALYTICS = "analytics";
	
	public static final String ANALYTICS_FILESIZE = "analyticsfs";
	
	public static final String ANALYTICS_TOTAL_SIZE = "totalfs";
	
	public static final String EVENT_SUB_TYPE = "st";
	
	public static final String EVENT_TAG = "tag";
	
	public static final String CURRENT_TIME_STAMP = "cts"; 
	
	public static final String EVENT_TAG_MOB = "mob";
	
	public static final String EVENT_TAG_CBS = "cbs";	
	
	public static final String DEVICE_DETAILS = "devicedetails";
	
	public static final String DEVICE_STATS = "devicestats";
	
	public static final String FILE_TRANSFER = "filetransfer";	
	
	public static final String EXIT_FROM_GALLERY = "exitFromGallery";
	
	public static final String HIKE_SDK_INSTALL_ACCEPT = "hikeSDKInstallAccept";
	
	public static final String HIKE_SDK_INSTALL_DECLINE = "hikeSDKInstallDecline";
}
