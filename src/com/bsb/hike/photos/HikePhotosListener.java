package com.bsb.hike.photos;

import java.io.File;

public interface HikePhotosListener
{
	void onComplete(File f);
	
	void onFailure();
}
