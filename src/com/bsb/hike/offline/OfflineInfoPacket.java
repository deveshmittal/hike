package com.bsb.hike.offline;

public class OfflineInfoPacket {
	private String filePath ;
	private boolean isText;
	private String msgText ;
	private String host ;
	private int  type ;

	public OfflineInfoPacket(String filePath , boolean isText,
			String msg,   String host ,  int type) {
		this.filePath = filePath;
		this.isText = isText;
		this.msgText = msg;
		this.host = host ;
		this.type = type; 
	}
	
	public String getFilePath()
	{
		return filePath;
	}
	
	public boolean getIsText()
	{
		return isText;
	}
	
	public String getMsgText()
	{
		return msgText;
	}

	public String getHost()
	{
		return host ;
	}
	
	public int getType()
	{
		return type;
	}
	
}
