package com.bsb.hike.models;

import java.util.ArrayList;

public class NuxCustomMessage
{
	private String screenTitle;

	private String hint;

	private String buttext;

	private boolean togglescreen;

	private ArrayList<String> stickerList;

	public NuxCustomMessage(String screentitle, String smsmessage, String buttext, boolean togglecustommsg, ArrayList<String> stickerList)
	{
		super();
		this.screenTitle = screentitle;
		this.hint = smsmessage;
		this.buttext = buttext;
		this.togglescreen = togglecustommsg;
		this.stickerList = stickerList;
	}

	/**
	 * @return the screentitle
	 */
	public String getScreenTitle()
	{
		return screenTitle;
	}

	/**
	 * @return the smsmessage
	 */
	public String getSmsMessage()
	{
		return hint;
	}

	/**
	 * @return the buttext
	 */
	public String getButText()
	{
		return buttext;
	}

	/**
	 * @return the togglecustommsg
	 */
	public boolean isToggleCustomMsg()
	{
		return togglescreen;
	}

	public ArrayList<String> getStickerList()
	{
		return stickerList;
	}

}
