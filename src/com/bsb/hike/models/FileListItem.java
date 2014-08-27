package com.bsb.hike.models;

import java.io.File;

public class FileListItem
{
	private int icon;

	private String title;

	private String subtitle = "";

	private String extension = "";

	private String mimeType;

	private boolean showThumbnail;

	private File file;
	
	public FileListItem(int icon, String title, String subtitle, String extension, String mimeType, boolean showThumbnail, File file)
	{
		this.icon = icon;
		
		this.title = title;
		
		this.subtitle = subtitle;
		
		this.extension = extension;
		
		this.mimeType = mimeType;
		
		this.showThumbnail = showThumbnail;
		
		this.file = file;
	}
	
	public FileListItem()
	{
		// TODO Auto-generated constructor stub
	}

	public int getIcon()
	{
		return icon;
	}

	public void setIcon(int icon)
	{
		this.icon = icon;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getSubtitle()
	{
		return subtitle;
	}

	public void setSubtitle(String subtitle)
	{
		this.subtitle = subtitle;
	}

	public String getExtension()
	{
		return extension;
	}

	public void setExtension(String extension)
	{
		this.extension = extension;
	}

	public String getMimeType()
	{
		return mimeType;
	}

	public void setMimeType(String mimeType)
	{
		this.mimeType = mimeType;
	}

	public boolean isShowThumbnail()
	{
		return showThumbnail;
	}

	public void setShowThumbnail(boolean showThumbnail)
	{
		this.showThumbnail = showThumbnail;
	}

	public File getFile()
	{
		return file;
	}

	public void setFile(File file)
	{
		this.file = file;
	}
}