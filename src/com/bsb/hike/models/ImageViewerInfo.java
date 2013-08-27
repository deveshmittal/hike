package com.bsb.hike.models;

public class ImageViewerInfo {

	public String mappedId;
	public String url;
	public boolean isStatusMessage;

	public ImageViewerInfo(String mappedId, String url, boolean isStatusMessage) {
		this.mappedId = mappedId;
		this.url = url;
		this.isStatusMessage = isStatusMessage;
	}
}
