package com.bsb.hike.media;

import android.view.View;

public interface ShareablePopup
{
	public View getView();

	public int getViewId();

	public void releaseViewResources();

}