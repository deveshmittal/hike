package com.bsb.hike.modules.stickerdownloadmgr;


public interface IStickerResultListener
{
	
	public void onSuccess(Object result);
	
	
	public void onFailure(Object result, Throwable exception);
	
	
	public void onProgressUpdated(double percentage);
	
}
