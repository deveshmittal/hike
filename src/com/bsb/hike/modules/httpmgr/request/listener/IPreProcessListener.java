package com.bsb.hike.modules.httpmgr.request.listener;

/**
 * This interface will be used for background processing other than the http call
 * 
 * @author anubhav & sidharth
 * 
 */
public interface IPreProcessListener
{
	/**
	 * Perform background tasks other than http call in this method
	 */
	public void doInBackground();
}
