
package com.bsb.hike.utils;

import android.content.Context;

import com.bsb.hike.HikeConstants;
import com.mobileapptracker.MobileAppTracker;

public class TrackerUtil {
	
	private  MobileAppTracker _mobileAppTracker = null;
	private  MatResponse _matResponse = null;
    private Context _ctx= null;
	private boolean DEBUG = false; //toggle this field to switch between test mode and live mode
    private static TrackerUtil _trackerUtil = null;
	private TrackerUtil() {
	 	
	}
    
	public static TrackerUtil getInstance() {
	    if(_trackerUtil ==null)
	    	return new TrackerUtil();
	    else
	    	return _trackerUtil;
	}

	public void init(Context ctx) {
		
		// initialize the MobileAppTracker framework
		this._ctx = ctx;
		_mobileAppTracker = new MobileAppTracker(this._ctx,HikeConstants.MA_TRACKER_AD_ID, HikeConstants.MA_TRACKER_KEY);
		_matResponse = new MatResponse();
		_mobileAppTracker.setMATResponse(_matResponse);
		_mobileAppTracker.setUserId(HikeConstants.MA_TRACKER_USERID);
		_mobileAppTracker.setRefId(HikeConstants.MA_TRACKER_REF_ID_PREFIX + System.currentTimeMillis());
	}
	
	
	public void setTrackOptions(boolean isNewInstall)
	{
	
		// Enable these options for debugging only
		if (DEBUG) {
			_mobileAppTracker.setAllowDuplicates(true);
			_mobileAppTracker.setDebugMode(true);
		}
		
		// check if the user is a new user or a returning user.
		// please note that even if this method is called multiple times, the mobile app tracker SDK will pass a no-op internally.
		// if there is no actual version update or new install. This is as per the email communication from mobileAppTracker team
		if (!isNewInstall) {
			// the SDK will record an update for all versions including the
			// current version
			_mobileAppTracker.trackUpdate();

		} else {
			// the SDK will record an install for this version and an update for
			// all subsequent versions.
			_mobileAppTracker.trackInstall();
		}
	}
}
