package com.bsb.hike.modules.stickerdownloadmgr;

public class StickerConstants
{
	
	public enum STState
	{
		NOT_STARTED, INITIALIZED, IN_PROGRESS, PAUSED, CANCELLED, COMPLETED, ERROR
	}
	
	public enum DownloadType
	{
		NEW_CATEGORY, UPDATE, MORE_STICKERS
	}
	
	public enum StickerRequestType
	{
		SINGLE(0, "ss"),
		MULTIPLE(1, "sm"),
		PREVIEW(2, "sp"),
		ENABLE_DISABLE(3, "sed"),
		METADATA(4,"smd"),
		SIZE(5, "ssz");
		
		private final int type;
		private final String label;
		
		private StickerRequestType(int type, String label)
		{
			this.type = type;
			this.label = label;
		}
		
		public int getType()
		{
			return this.type;
		}
		public String getLabel()
		{
			return this.label;
		}
	};
}
