package com.bsb.hike.modules.stickerdownloadmgr.retry;

public interface IRetryPolicy
{
	
	int getRetryCount();
	
	boolean retry();
	
	void reset();
}
