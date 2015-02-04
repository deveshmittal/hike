package com.bsb.hike.modules.stickerdownloadmgr;

import com.bsb.hike.utils.Logger;


class BaseStickerDownloadTask 
{
	
	private String taskId;
	
	public BaseStickerDownloadTask(String taskId)
	{
		this.taskId = taskId;
	}
	
	void onSuccess(Object result)
	{
		finish();
	}
	
	void onFailure(Exception e)
	{
		Logger.e(getClass().getCanonicalName(), "Stikcer task failed , task id : " + taskId, e);
		finish();
	}

	void finish()
	{
		StickerDownloadManager.getInstance().removeTask(taskId);
	}
	
}
